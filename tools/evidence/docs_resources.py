"""Offline docs resources on a real device (roadmap P3.5 optional item, P9 device evidence).

Repeats the P3.5 AVD evidence against a device: template and resource listing with the docs
catalog, the docs index, an HTML page and a binary page, error probes, and the docs catalog
disappearing and returning while the Offline Docs plugin is disabled and enabled again.

The report holds sizes, hashes, counts, timings and error codes only: no token, no page content.

    py tools/evidence/docs_resources.py --serial <serial>
"""
from __future__ import annotations

import argparse
import base64
import datetime
import hashlib
import json
import pathlib
import sys
import time

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

from device import HOST_PACKAGE, PLUGIN_PACKAGE, PLUGIN_TEST_RUNNER, Device, DeviceError, progress  # noqa: E402
from mcp_client import McpClient, McpError  # noqa: E402

DOCS_PACKAGE = "io.github.supermonster003.autojs6.plugin.offlinedocs"
DOCS_ROOT = "autojs6://docs/"
CLIENT_NAME = "autojs6-evidence-docs"
REPO = pathlib.Path(__file__).resolve().parents[2]


def content_facts(result: dict) -> dict:
    """Size, hash and type of a resources/read answer without its content."""
    item = result["contents"][0]
    data = item["text"].encode("utf-8") if "text" in item else base64.b64decode(item.get("blob", ""))
    return {
        "mimeType": item.get("mimeType"),
        "encoding": "text" if "text" in item else "blob",
        "bytes": len(data),
        "sha256": hashlib.sha256(data).hexdigest(),
        "meta": {k: v for k, v in (result.get("_meta") or {}).items() if not isinstance(v, (dict, list))},
    }


def index_facts(text: str) -> dict:
    """Scalar fields and list lengths of a JSON directory index (no names or paths)."""
    try:
        value = json.loads(text)
    except ValueError:
        return {"json": False}
    if not isinstance(value, dict):
        return {"json": True, "type": type(value).__name__}
    facts = {k: v for k, v in value.items() if isinstance(v, (str, int, float, bool)) and len(str(v)) <= 80}
    facts.update({f"{k}Count": len(v) for k, v in value.items() if isinstance(v, list)})
    return facts


def find_png(value) -> str | None:
    if isinstance(value, str):
        return value if value.lower().endswith(".png") else None
    items = value.values() if isinstance(value, dict) else value if isinstance(value, list) else []
    for item in items:
        found = find_png(item)
        if found:
            return found
    return None


def docs_listing(client: McpClient) -> dict:
    started = time.perf_counter()
    resources, pages, metas = client.list_all("resources/list", "resources")
    docs = [r for r in resources if r.get("uri", "").startswith(DOCS_ROOT)]
    status = next((m.get("docsCatalogStatus") for m in metas if isinstance(m, dict) and "docsCatalogStatus" in m), None)
    return {
        "resources": len(resources),
        "pages": len(pages),
        "docs": len(docs),
        "docsHtml": sum(1 for r in docs if (r.get("mimeType") or "").startswith("text/html")),
        "docsIndexListed": any(r.get("uri") == DOCS_ROOT for r in docs),
        "docsCatalogStatus": status,
        "elapsedMs": round((time.perf_counter() - started) * 1000),
        "_uris": [r["uri"] for r in docs],
    }


def template_uris(client: McpClient) -> list[str]:
    templates, _elapsed, _metas = client.list_all("resources/templates/list", "resourceTemplates")
    return [t.get("uriTemplate", "") for t in templates]


def read(client: McpClient, uri: str) -> tuple[dict, float]:
    result, ms = client.request("resources/read", {"uri": uri})
    return result, ms


def probe(client: McpClient, uri: str) -> dict:
    try:
        read(client, uri)
        return {"uri": uri, "error": None}
    except McpError as error:
        return {"uri": uri, "code": error.code, "messageBytes": len(error.message.encode("utf-8")),
                "messageMentionsDevicePath": any(p in error.message for p in ("/data/", "/storage/", "/sdcard"))}


def first_read(client: McpClient) -> tuple[dict, float]:
    """The first gated call; without the pre-pairing it fails with PAIRING_REQUIRED and a clear hint."""
    try:
        return read(client, DOCS_ROOT)
    except McpError as error:
        if error.pairing_fingerprint:
            raise DeviceError("the evidence client is not paired: allow it on the phone or install the androidTest APK")
        raise


def wait_listing(client: McpClient, predicate, timeout: float) -> tuple[dict, float]:
    started = time.perf_counter()
    while True:
        listing = docs_listing(client)
        if predicate(listing):
            return listing, (time.perf_counter() - started) * 1000
        if time.perf_counter() - started > timeout:
            raise DeviceError(f"docs listing did not change within {timeout} s (docs={listing['docs']})")
        time.sleep(0.5)


def run(device: Device, args) -> dict:
    report: dict = {
        "scenario": "p35-docs-resources",
        "startedAt": datetime.datetime.now().astimezone().isoformat(timespec="seconds"),
        "device": device.facts(),
        "packages": {name: device.package(pkg) for name, pkg in
                     (("host", HOST_PACKAGE), ("plugin", PLUGIN_PACKAGE), ("offlineDocs", DOCS_PACKAGE))},
    }
    if not report["packages"]["offlineDocs"] or not report["packages"]["offlineDocs"]["enabled"]:
        raise DeviceError("the Offline Docs plugin must be installed and enabled before the run")
    # The helpers restart the plugin package, so they run before the host session is held.
    was_running = device.plugin_state()["running"]
    progress("reading the token")
    token, source = device.read_token(args.token_source)
    report["tokenSource"] = source
    pairing = None
    if source != "env" and device.has_instrumentation(PLUGIN_TEST_RUNNER):
        progress(f"pre-pairing {CLIENT_NAME}")
        pairing = device.pre_pair(CLIENT_NAME)
        report["pairing"] = {"method": "EvidenceHelperTest#pairClient", "fingerprint": pairing}
    if was_running:
        progress("waiting for the host to rebind the restarted listener")
        device.wait_listener_with_host()
    try:
        run_session(device, args, token, report)
    finally:
        if pairing and not args.keep_pairing:
            progress("revoking the evidence pairing")
            device.revoke_pairing(pairing)
            report["pairing"]["revoked"] = True
    report["finishedAt"] = datetime.datetime.now().astimezone().isoformat(timespec="seconds")
    return report


def run_session(device: Device, args, token: str, report: dict) -> None:
    with device.host_session(args.hold_seconds) as session:
        report["hostSession"] = {"source": session["source"], "grantedMethods": session.get("grantedMethods")}
        progress(f"host session ready ({session['source']}), port {session['port']}")
        with device.forward(session["port"]) as local_port, McpClient(f"http://127.0.0.1:{local_port}/mcp", token, CLIENT_NAME) as client:
            init = client.initialize()
            report["initialize"] = {"protocolVersion": init.get("protocolVersion"), "serverInfo": init.get("serverInfo")}
            progress("listing templates and resources")
            index_result, index_ms = first_read(client)

            templates = template_uris(client)
            listing = docs_listing(client)
            uris = listing.pop("_uris")
            report["templates"] = {"count": len(templates), "docsTemplate": any(t.startswith(DOCS_ROOT) for t in templates)}
            report["listing"] = listing

            index_text = index_result["contents"][0].get("text", "")
            report["reads"] = {"index": {**content_facts(index_result), "elapsedMs": round(index_ms), "index": index_facts(index_text)}}
            for name in ("index.html", "app.html"):
                uri = next((u for u in uris if u.endswith("/" + name)), None)
                if uri:
                    result, ms = read(client, uri)
                    report["reads"][name] = {**content_facts(result), "elapsedMs": round(ms)}
            png = find_png(json.loads(index_text)) if index_text.startswith("{") else None
            if not png:
                images, _ms = read(client, DOCS_ROOT + "images/")
                png = find_png(json.loads(images["contents"][0].get("text", "{}")))
            if png:
                uri = png if png.startswith(DOCS_ROOT) else DOCS_ROOT + png.lstrip("/")
                result, ms = read(client, uri)
                report["reads"]["png"] = {**content_facts(result), "elapsedMs": round(ms)}

            progress("reading pages and probing errors")
            report["probes"] = [probe(client, DOCS_ROOT + suffix) for suffix in
                                ("does-not-exist.html", "images", "index.html/", "../index.html")]

            if args.toggle_docs:
                progress("disabling and enabling the Offline Docs plugin")
                toggle: dict = {}
                try:
                    device.set_package_enabled(DOCS_PACKAGE, False)
                    gone, ms = wait_listing(client, lambda item: item["docs"] == 0, timeout=30)
                    gone.pop("_uris")
                    root, _ms = read(client, DOCS_ROOT)
                    toggle["disabled"] = {**gone, "goneAfterMs": round(ms), "templates": len(template_uris(client)),
                                          "rootIndex": index_facts(root["contents"][0].get("text", "")),
                                          "pageRead": probe(client, DOCS_ROOT + "index.html")}
                finally:
                    device.set_package_enabled(DOCS_PACKAGE, True)
                back, ms = wait_listing(client, lambda item: item["docs"] == listing["docs"], timeout=60)
                back.pop("_uris")
                toggle["enabled"] = {**back, "backAfterMs": round(ms)}
                report["docsToggle"] = toggle


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--serial", required=True)
    parser.add_argument("--hold-seconds", type=int, default=240, help="host session hold when no session is running")
    parser.add_argument("--token-source", choices=("auto", "env", "instrumentation", "dumpsys"), default="auto")
    parser.add_argument("--no-toggle-docs", dest="toggle_docs", action="store_false",
                        help="skip disabling and enabling the Offline Docs plugin")
    parser.add_argument("--keep-pairing", action="store_true", help="leave the evidence client paired")
    parser.add_argument("--report-dir", default=str(REPO / "build" / "evidence"))
    args = parser.parse_args()
    device = Device(args.serial)
    report = run(device, args)
    out_dir = pathlib.Path(args.report_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    path = out_dir / f"{datetime.datetime.now():%Y%m%d-%H%M%S}-{args.serial}-docs.json"
    path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2, ensure_ascii=False))
    print(f"report: {path}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())

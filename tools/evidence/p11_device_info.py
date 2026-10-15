"""The plugin family section of device_info and autojs6://device/info on a real device (roadmap P11 D27).

On a host with `app.pluginCapabilities` in the MCP grant, `device_info` and the device resource carry
`plugins.families`: the seven families AutoJs6 reports (ocr, barcode, opencv, pinyin, pinyin4j, mediainfo,
docs), each with a known status, its tools, and a hint whenever it brings no tools; the families that
are available match the plugin-family tools in tools/list. On an older host (`--expect-no-plugins`) the
field is absent and the device part is unchanged.

The report keeps the status of each family, the tool names and whether hints are present; never the
device fields beyond the schema id.

    py tools/evidence/p11_device_info.py --serial <serial> [--expect-no-plugins]
"""
from __future__ import annotations

import argparse
import datetime
import json
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

from device import HOST_PACKAGE, PLUGIN_PACKAGE, Device, progress  # noqa: E402
from session import evidence_session  # noqa: E402
from tool_calls import call  # noqa: E402

CLIENT_NAME = "autojs6-evidence-p11-device-info"
REPO = pathlib.Path(__file__).resolve().parents[2]
FAMILIES = ("ocr", "barcode", "opencv", "pinyin", "pinyin4j", "mediainfo", "docs")
STATUSES = ("available", "not_installed", "disabled", "incompatible")


def run(device: Device, args) -> dict:
    report: dict = {
        "scenario": "p11-device-info-plugins",
        "startedAt": datetime.datetime.now().astimezone().isoformat(timespec="seconds"),
        "device": device.facts(),
        "packages": {name: device.package(pkg) for name, pkg in (("host", HOST_PACKAGE), ("plugin", PLUGIN_PACKAGE))},
        "checks": [],
    }

    def record(name: str, passed: bool, **facts):
        report["checks"].append({"check": name, "pass": bool(passed), **facts})
        progress(f"{'PASS' if passed else 'FAIL'} {name}")

    with evidence_session(device, report, CLIENT_NAME, hold_seconds=args.hold_seconds, token_source=args.token_source) as client:
        listed = {tool["name"] for tool in client.list_all("tools/list", "tools")[0]}
        ok, s, _t, ms = call(client, "device_info", {})
        resource, _ = client.request("resources/read", {"uri": "autojs6://device/info"})
        resource_json = json.loads(resource["contents"][0]["text"])
        record("device part unchanged", ok and s.get("schema") == "autojs6-bridge-device-info-v1"
               and resource_json.get("schema") == "autojs6-bridge-device-info-v1", ms=ms)
        if args.expect_no_plugins:
            record("an older host gets no plugins field", "plugins" not in s and "plugins" not in resource_json, toolCount=len(listed))
            return report
        families = {f.get("id"): f for f in (s.get("plugins") or {}).get("families") or []}
        report["families"] = {fid: {"status": f.get("status"), "tools": f.get("tools"), "hint": bool(f.get("hint")),
                                    "plugins": len(f.get("plugins") or [])} for fid, f in families.items()}
        record("the seven families with known statuses", set(FAMILIES) <= set(families) and all(f.get("status") in STATUSES for f in families.values()),
               families=sorted(families))
        missing_hints = [fid for fid, f in families.items() if f.get("status") != "available" and not f.get("hint")]
        record("a hint for every family without tools", not missing_hints, missing=missing_hints)
        expected_tools = {tool for f in families.values() if f.get("status") == "available" and f.get("toolGroupEnabled", True) for tool in f.get("tools") or []}
        plugin_tools = {name for f in families.values() for name in f.get("tools") or []}
        record("available families match the listed plugin-family tools", expected_tools == (listed & plugin_tools),
               expected=sorted(expected_tools), listed=sorted(listed & plugin_tools))
        resource_families = {f.get("id"): f.get("status") for f in (resource_json.get("plugins") or {}).get("families") or []}
        record("the device resource carries the same section", resource_families == {fid: f.get("status") for fid, f in families.items()})
    report["passed"] = sum(1 for c in report["checks"] if c["pass"])
    report["failed"] = sum(1 for c in report["checks"] if not c["pass"])
    report["finishedAt"] = datetime.datetime.now().astimezone().isoformat(timespec="seconds")
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--serial", required=True)
    parser.add_argument("--token-source", choices=("auto", "env", "instrumentation", "dumpsys"), default="auto")
    parser.add_argument("--hold-seconds", type=int, default=60)
    parser.add_argument("--expect-no-plugins", action="store_true", help="the host predates app.pluginCapabilities")
    parser.add_argument("--report-dir", default=str(REPO / "build" / "evidence"))
    args = parser.parse_args()
    report = run(Device(args.serial), args)
    report.setdefault("passed", sum(1 for c in report["checks"] if c["pass"]))
    report.setdefault("failed", sum(1 for c in report["checks"] if not c["pass"]))
    out_dir = pathlib.Path(args.report_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    path = out_dir / f"{datetime.datetime.now():%Y%m%d-%H%M%S}-{args.serial}-p11-device-info.json"
    path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    for check in report["checks"]:
        print(f"{'PASS' if check['pass'] else 'FAIL'}  {check['check']}")
    print(f"passed {report['passed']}, failed {report['failed']}; report: {path}")
    return 0 if report["failed"] == 0 else 1


if __name__ == "__main__":
    sys.exit(main())

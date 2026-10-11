"""Host contract version on a real device (roadmap P10): which transaction the installed host chose.

The plugin declares contract version 2 to hosts from build 5283 on and version 1 to older ones; the
host then opens the listener with `openServerV2` (shared host capability broker) or `openServer`.
With the drawer listener running, the scenario records the host and plugin versions, the contract
version of the session the host opened after the evidence helpers restarted the plugin (dumpsys
and the plugin's own log line), and a few tools that reach the host through that broker, including
a descriptor payload (`screen_capture`, only while the AutoJs6 accessibility service runs). The
report holds statuses, sizes and timings, never content.

Uses the listener the drawer switched on when it runs (the host reopens it after the helpers);
otherwise the host androidTest APK holds a session as in the other scenarios.

    py tools/evidence/contract_v2.py --serial <serial>
"""
from __future__ import annotations

import argparse
import datetime
import json
import pathlib
import re
import sys
import time

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

from device import HOST_PACKAGE, PLUGIN_PACKAGE, Device  # noqa: E402
from mcp_client import McpError  # noqa: E402
from session import evidence_session  # noqa: E402

CLIENT_NAME = "autojs6-evidence-contract"
REPO = pathlib.Path(__file__).resolve().parents[2]
V2_MIN_HOST_VERSION = 5283  # HostContractVersion.V2_MIN_HOST_VERSION
SESSION_LINE = re.compile(r"Session \S+ \(contract v(\d+)\) opened by uid \d+: broker (\S+) v(\d+), (\d+) granted methods")

CALLS = [
    ("device_info", {}),
    ("screen_state", {}),
    ("ui_current_window", {}),
    ("ui_dump", {"maxNodes": 60}),
    ("console_tail", {"lines": 5}),
]
# Returned through a descriptor. Called only when ui_dump worked: without the accessibility service the
# host falls back to MediaProjection, which shows a consent dialog on the phone.
CAPTURE = ("screen_capture", {"maxWidth": 64})


def call(client, name: str, arguments: dict) -> dict:
    started = time.perf_counter()
    try:
        result, _ = client.request("tools/call", {"name": name, "arguments": arguments}, timeout=130)
    except McpError as error:
        return {"tool": name, "ok": False, "rpcError": str(error)[:120], "elapsedMs": round((time.perf_counter() - started) * 1000)}
    structured = result.get("structuredContent") or {}
    content = result.get("content") or []
    return {
        "tool": name,
        "ok": not result.get("isError"),
        "code": (structured.get("error") or {}).get("code"),
        "elapsedMs": round((time.perf_counter() - started) * 1000),
        "contentTypes": sorted({item.get("type") for item in content}),
        "imageBytes": sum(len(item.get("data", "")) * 3 // 4 for item in content if item.get("type") == "image"),
        "textBytes": sum(len(item.get("text", "").encode("utf-8")) for item in content if item.get("type") == "text"),
    }


def last_session_line(device: Device) -> dict | None:
    out = device.run("logcat", "-d", "-s", "McpServerRuntime:I", check=False)
    matches = [m for m in map(SESSION_LINE.search, out.splitlines()) if m]
    if not matches:
        return None
    m = matches[-1]
    return {"contract": int(m.group(1)), "brokerId": m.group(2), "brokerVersion": int(m.group(3)), "grantedMethods": int(m.group(4))}


def run(device: Device, args) -> dict:
    packages = {name: device.package(pkg) for name, pkg in (("host", HOST_PACKAGE), ("plugin", PLUGIN_PACKAGE))}
    host_code = int((packages["host"] or {}).get("versionCode") or 0)
    report: dict = {
        "scenario": "p10-host-contract-version",
        "startedAt": datetime.datetime.now().astimezone().isoformat(timespec="seconds"),
        "device": device.facts(),
        "packages": packages,
        "expectedContract": 2 if host_code >= V2_MIN_HOST_VERSION else 1,
    }
    state = device.plugin_state()
    drawer = state["running"] and state.get("host") == "available"
    # Either way the host's own McpServerPluginHost picks the transaction from the declared version.
    report["listener"] = "drawer" if drawer else "held by the host McpServerPluginRoundTripTest"
    report["before"] = {"contract": state.get("contract"), "grantedMethods": state.get("grantedMethods")}
    device.run("logcat", "-c", check=False)
    with evidence_session(device, report, CLIENT_NAME, token_source=args.token_source, keep_pairing=args.keep_pairing) as client:
        state = device.plugin_state()
        report["session"] = {"contract": state.get("contract"), "grantedMethods": state.get("grantedMethods"), "log": last_session_line(device)}
        report["calls"] = [call(client, name, arguments) for name, arguments in CALLS]
        dump = next(c for c in report["calls"] if c["tool"] == "ui_dump")
        if dump["ok"]:
            report["calls"].append(call(client, *CAPTURE))
        else:
            report["calls"].append({"tool": CAPTURE[0], "skipped": f"ui_dump answered {dump['code']}; a capture would ask for screen recording consent"})
    report["matchesExpectation"] = report["session"]["contract"] == report["expectedContract"]
    report["finishedAt"] = datetime.datetime.now().astimezone().isoformat(timespec="seconds")
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--serial", required=True)
    parser.add_argument("--token-source", choices=("auto", "env", "instrumentation", "dumpsys"), default="auto")
    parser.add_argument("--keep-pairing", action="store_true", help="leave the evidence client paired")
    parser.add_argument("--report-dir", default=str(REPO / "build" / "evidence"))
    args = parser.parse_args()
    report = run(Device(args.serial), args)
    out_dir = pathlib.Path(args.report_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    path = out_dir / f"{datetime.datetime.now():%Y%m%d-%H%M%S}-{args.serial}-contract-v2.json"
    path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2, ensure_ascii=False))
    print(f"report: {path}", file=sys.stderr)
    return 0 if report["matchesExpectation"] else 1


if __name__ == "__main__":
    sys.exit(main())

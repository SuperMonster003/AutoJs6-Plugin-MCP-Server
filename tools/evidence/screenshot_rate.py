"""The screenshot rate limit on a real device (roadmap P6 / P9): 30 captures per sliding minute per client.

31 small `screen_capture` calls (maxWidth 64) in a row: the first 30 return images, the 31st
answers `RATE_LIMITED` with `retryAfterMs`; after waiting that long one more capture succeeds.
Before the burst the scenario waits a minute so that earlier captures of the same client are out of
the window. The report holds statuses, timings and the retry delay, never image data.

Needs the AutoJs6 accessibility service (Android 11 or later captures through it without consent)
and therefore a listener the drawer switched on, as ui_chain.py does.

    py tools/evidence/screenshot_rate.py --serial <serial>
"""
from __future__ import annotations

import argparse
import datetime
import json
import pathlib
import sys
import time

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

from device import HOST_PACKAGE, PLUGIN_PACKAGE, Device, DeviceError, progress  # noqa: E402
from session import evidence_session  # noqa: E402

CLIENT_NAME = "autojs6-evidence-rate"
REPO = pathlib.Path(__file__).resolve().parents[2]
LIMIT = 30


def capture(client) -> tuple[dict, dict, int]:
    started = time.perf_counter()
    result, _ = client.request("tools/call", {"name": "screen_capture", "arguments": {"maxWidth": 64}}, timeout=130)
    return result, result.get("structuredContent") or {}, round((time.perf_counter() - started) * 1000)


def run(device: Device, args) -> dict:
    report: dict = {
        "scenario": "p9-screenshot-rate-limit",
        "startedAt": datetime.datetime.now().astimezone().isoformat(timespec="seconds"),
        "device": device.facts(),
        "packages": {name: device.package(pkg) for name, pkg in (("host", HOST_PACKAGE), ("plugin", PLUGIN_PACKAGE))},
        "limit": LIMIT,
    }
    state = device.plugin_state()
    if not (state["running"] and state.get("host") == "available"):
        raise DeviceError("switch MCP Server on in the AutoJs6 drawer first (a held host session stops the accessibility service)")
    with evidence_session(device, report, CLIENT_NAME, token_source=args.token_source, keep_pairing=args.keep_pairing) as client:
        progress("waiting a minute so that no earlier capture of this client is in the window")
        time.sleep(61)
        burst_started = time.perf_counter()
        outcomes = []
        for _ in range(LIMIT + 1):
            result, structured, elapsed = capture(client)
            error = structured.get("error") or {}
            outcomes.append({"ok": not result.get("isError"), "code": error.get("code"), "retryAfterMs": error.get("retryAfterMs"),
                             "elapsedMs": elapsed, "source": structured.get("source")})
        report["burstMs"] = round((time.perf_counter() - burst_started) * 1000)
        report["succeeded"] = sum(1 for o in outcomes if o["ok"])
        report["firstFailure"] = next(({"index": i + 1, **o} for i, o in enumerate(outcomes) if not o["ok"]), None)
        report["sources"] = sorted({o["source"] for o in outcomes if o["source"]})
        retry = (report["firstFailure"] or {}).get("retryAfterMs")
        if retry:
            progress(f"waiting {retry} ms as the result asks")
            time.sleep(retry / 1000 + 0.5)
            result, structured, elapsed = capture(client)
            report["afterRetry"] = {"ok": not result.get("isError"), "elapsedMs": elapsed,
                                    "code": (structured.get("error") or {}).get("code")}
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
    path = out_dir / f"{datetime.datetime.now():%Y%m%d-%H%M%S}-{args.serial}-screenshot-rate.json"
    path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2, ensure_ascii=False))
    print(f"report: {path}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())

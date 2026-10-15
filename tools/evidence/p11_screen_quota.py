"""The shared screenshot window of roadmap P11 D39 on a real device: recognizing the screen counts like a capture.

After a minute without captures, 30 calls that each take a screenshot (screen_capture at maxWidth 64,
opencv_find_color and barcode_detect with image "screen", interleaved) all succeed; the 31st, a
screen recognition, answers RATE_LIMITED with retryAfterMs and names the shared limit, screen_capture
is refused as well, while a recognition call on a file is not counted (it reaches the host and fails
there with INVALID_ARGUMENTS for the missing file). After the wait, a screen recognition works again.

Needs the barcode and OpenCV plugins and the AutoJs6 accessibility service (rebound after the held
host session restarts AutoJs6). The report holds statuses, codes, delays and timings, never image data.

    py tools/evidence/p11_screen_quota.py --serial <serial>
"""
from __future__ import annotations

import argparse
import datetime
import json
import pathlib
import sys
import time

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

from device import HOST_PACKAGE, PLUGIN_PACKAGE, Device, progress  # noqa: E402
from session import evidence_session  # noqa: E402
from tool_calls import call, ensure_accessibility  # noqa: E402

CLIENT_NAME = "autojs6-evidence-p11-quota"
REPO = pathlib.Path(__file__).resolve().parents[2]
LIMIT = 30
SMALL = {"left": 0, "top": 0, "right": 64, "bottom": 64}
# One call per step, cycling: the three kinds of screenshot consumers share one window.
CYCLE = (
    ("screen_capture", {"maxWidth": 64}),
    ("opencv_find_color", {"image": "screen", "color": "#FFFFFF", "region": SMALL}),
    ("barcode_detect", {"image": "screen", "region": {"left": 0, "top": 0, "right": 256, "bottom": 256}}),
)


def outcome(name: str, ok: bool, structured: dict, ms: int) -> dict:
    error = structured.get("error") or {}
    return {"tool": name, "ok": ok, "code": error.get("code"), "retryAfterMs": error.get("retryAfterMs"), "ms": ms}


def run(device: Device, args) -> dict:
    report: dict = {
        "scenario": "p11-shared-screenshot-window",
        "startedAt": datetime.datetime.now().astimezone().isoformat(timespec="seconds"),
        "device": device.facts(),
        "packages": {name: device.package(pkg) for name, pkg in (("host", HOST_PACKAGE), ("plugin", PLUGIN_PACKAGE))},
        "limit": LIMIT,
        "checks": [],
    }

    def record(name: str, passed: bool, **facts):
        report["checks"].append({"check": name, "pass": bool(passed), **facts})
        progress(f"{'PASS' if passed else 'FAIL'} {name}")

    with evidence_session(device, report, CLIENT_NAME, hold_seconds=args.hold_seconds, token_source=args.token_source, keep_pairing=args.keep_pairing) as client:
        names = {tool["name"] for tool in client.list_all("tools/list", "tools")[0]}
        missing = [name for name, _ in CYCLE if name not in names]
        if missing or not ensure_accessibility(device, client, report):
            record("prerequisites", False, missing=missing, accessibility=report.get("accessibility"))
            return report
        progress("waiting a minute so that no earlier capture of this client is in the window")
        time.sleep(61)
        started = time.perf_counter()
        burst = []
        for index in range(LIMIT):
            name, arguments = CYCLE[index % len(CYCLE)]
            ok, structured, _text, ms = call(client, name, arguments, timeout=130)
            burst.append(outcome(name, ok, structured, ms))
        report["burstMs"] = round((time.perf_counter() - started) * 1000)
        report["burstByTool"] = {name: sum(1 for o in burst if o["tool"] == name and o["ok"]) for name, _ in CYCLE}
        record(f"{LIMIT} screenshot consumers within the window succeed", all(o["ok"] for o in burst),
               failures=[o for o in burst if not o["ok"]][:3], burstMs=report["burstMs"])

        ok, structured, _text, ms = call(client, "opencv_find_color", CYCLE[1][1])
        error = structured.get("error") or {}
        retry = error.get("retryAfterMs")
        record("the 31st, a screen recognition, is RATE_LIMITED", not ok and error.get("code") == "RATE_LIMITED" and (retry or 0) > 0
               and "together with screen_capture" in error.get("message", ""), retryAfterMs=retry, ms=ms)
        ok, structured, _text, ms = call(client, "screen_capture", CYCLE[0][1])
        record("screen_capture shares the exhausted window", not ok and (structured.get("error") or {}).get("code") == "RATE_LIMITED", ms=ms)
        ok, structured, _text, ms = call(client, "opencv_find_color", {"image": "mcp-evidence-p11-quota-missing.png", "color": "#FFFFFF"})
        code = (structured.get("error") or {}).get("code")
        record("a file is not counted", code not in (None, "RATE_LIMITED"), code=code, ms=ms)
        if retry:
            progress(f"waiting {retry} ms as the result asks")
            time.sleep(retry / 1000 + 1.0)
            ok, structured, _text, ms = call(client, "opencv_find_color", CYCLE[1][1])
            record("after the wait the screen can be read again", ok, code=(structured.get("error") or {}).get("code"), ms=ms)
    report["passed"] = sum(1 for c in report["checks"] if c["pass"])
    report["failed"] = sum(1 for c in report["checks"] if not c["pass"])
    report["finishedAt"] = datetime.datetime.now().astimezone().isoformat(timespec="seconds")
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--serial", required=True)
    parser.add_argument("--token-source", choices=("auto", "env", "instrumentation", "dumpsys"), default="auto")
    parser.add_argument("--keep-pairing", action="store_true", help="leave the evidence client paired")
    parser.add_argument("--hold-seconds", type=int, default=300)
    parser.add_argument("--report-dir", default=str(REPO / "build" / "evidence"))
    args = parser.parse_args()
    report = run(Device(args.serial), args)
    out_dir = pathlib.Path(args.report_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    path = out_dir / f"{datetime.datetime.now():%Y%m%d-%H%M%S}-{args.serial}-p11-screen-quota.json"
    path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    for check in report["checks"]:
        print(f"{'PASS' if check['pass'] else 'FAIL'}  {check['check']}")
    print(f"passed {report.get('passed', 0)}, failed {report.get('failed', 0)}; report: {path}")
    return 0 if report.get("failed", 1) == 0 else 1


if __name__ == "__main__":
    sys.exit(main())

"""The shared storage switch of roadmap P11 D26 on a real device, off and then on.

Switch off: absolute paths are refused by the plugin before any host call (`files_list`, `barcode_detect`,
`mediainfo_read` answer CAPABILITY_DENIED with the switch hint); a relative listing still works.
Switch on: fixtures pushed into `/sdcard/Download/autojs6-mcp-evidence` (an EAN-13, color squares, a WAV)
are listed with absolute paths and the shared storage root, read by `files_stat`, `barcode_detect`,
`opencv_find_color` and `mediainfo_read` through those paths; `Android/data` stays hidden in a listing and
refused by path, `files_read` stays in the working directory, and paths outside shared storage are refused.

Needs AutoJs6 with the P11 host changes and "All files access", the barcode, OpenCV and MediaInfo plugins,
and the plugin debug build with its androidTest APK (the switch is set through EvidenceHelperTest and
restored afterwards). The report keeps pass / fail per check, codes, counts and timings.

    py tools/evidence/p11_shared_storage.py --serial <serial>
"""
from __future__ import annotations

import argparse
import datetime
import json
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

import image_fixtures as fx  # noqa: E402
from device import HOST_PACKAGE, PLUGIN_PACKAGE, Device, progress  # noqa: E402
from p11_text_tools import wav_fixture  # noqa: E402
from session import evidence_session  # noqa: E402
from tool_calls import call  # noqa: E402

CLIENT_NAME = "autojs6-evidence-p11-shared"
REPO = pathlib.Path(__file__).resolve().parents[2]
SHARED_DIR = "/sdcard/Download/autojs6-mcp-evidence"


def error_code(structured: dict) -> str | None:
    return (structured.get("error") or {}).get("code")


def run(device: Device, args) -> dict:
    report: dict = {
        "scenario": "p11-shared-storage-switch",
        "startedAt": datetime.datetime.now().astimezone().isoformat(timespec="seconds"),
        "device": device.facts(),
        "packages": {name: device.package(pkg) for name, pkg in (("host", HOST_PACKAGE), ("plugin", PLUGIN_PACKAGE))},
        "checks": [],
    }

    def record(name: str, passed: bool, **facts):
        report["checks"].append({"check": name, "pass": bool(passed), **facts})
        progress(f"{'PASS' if passed else 'FAIL'} {name}")

    files = fx.fixtures()
    fixtures = {"ean13.png": files["ean13.png"]["png"], "colors.png": files["colors.png"]["png"], "tone.wav": wav_fixture()}
    previous = device.set_shared_storage_read(False)
    report["switchBefore"] = previous
    try:
        progress("phase 1: the switch is off")
        with evidence_session(device, report, CLIENT_NAME, hold_seconds=args.hold_seconds, token_source=args.token_source) as client:
            for name, arguments in (("files_list", {"path": "/sdcard/Download"}),
                                    ("barcode_detect", {"image": f"{SHARED_DIR}/ean13.png"}),
                                    ("mediainfo_read", {"path": f"{SHARED_DIR}/tone.wav"})):
                ok, s, _t, ms = call(client, name, arguments)
                hint = (s.get("error") or {}).get("hint", "")
                record(f"switch off: {name} refuses an absolute path", not ok and error_code(s) == "CAPABILITY_DENIED" and "Allow reading shared storage" in hint,
                       code=error_code(s), ms=ms)
            ok, s, _t, ms = call(client, "files_list", {"path": "."})
            record("switch off: a relative listing still works", ok, count=s.get("count"), ms=ms)

        progress("phase 2: the switch is on")
        device.set_shared_storage_read(True)
        local = REPO / "build" / "evidence" / "shared-fixtures"
        local.mkdir(parents=True, exist_ok=True)
        device.shell("mkdir", "-p", SHARED_DIR)
        for name, data in fixtures.items():
            (local / name).write_bytes(data)
            device.run("push", str(local / name), f"{SHARED_DIR}/{name}")
        with evidence_session(device, report, CLIENT_NAME, hold_seconds=args.hold_seconds, token_source=args.token_source) as client:
            ok, s, _t, ms = call(client, "files_list", {"path": SHARED_DIR})
            entries = {pathlib.PurePosixPath(e.get("path", "")).name: e for e in s.get("entries") or []}
            absolute = all(e.get("path", "").startswith("/") for e in entries.values())
            record("list a shared folder with absolute paths", ok and set(entries) == set(fixtures) and absolute and s.get("sharedStorage") is True,
                   count=s.get("count"), root=s.get("root"), ms=ms)
            ean = (entries.get("ean13.png") or {}).get("path", f"{SHARED_DIR}/ean13.png")
            colors = (entries.get("colors.png") or {}).get("path", f"{SHARED_DIR}/colors.png")
            tone = (entries.get("tone.wav") or {}).get("path", f"{SHARED_DIR}/tone.wav")

            ok, s, _t, ms = call(client, "files_stat", {"path": ean})
            record("stat a shared file", ok and s.get("exists") is True and s.get("size") == len(fixtures["ean13.png"]), size=s.get("size"), ms=ms)
            ok, s, _t, ms = call(client, "barcode_detect", {"image": ean})
            item = (s.get("items") or [{}])[0]
            record("barcode from a shared path", ok and item.get("rawValue") == fx.EAN13_VALUE, format=item.get("format"), code=error_code(s), ms=ms)
            ok, s, _t, ms = call(client, "opencv_find_color", {"image": colors, "color": "#FF0000"})
            point = s.get("point") and (s["point"]["x"], s["point"]["y"])
            record("color search on a shared path", ok and point == files["colors.png"]["red"], point=point, code=error_code(s), ms=ms)
            ok, s, _t, ms = call(client, "mediainfo_read", {"path": tone})
            audio = ((s.get("sections") or {}).get("audio") or [{}])[0]
            record("media information of a shared path", ok and "PCM" in audio.get("format", ""), audioFormat=audio.get("format"), code=error_code(s), ms=ms)

            ok, s, _t, ms = call(client, "files_list", {"path": "/sdcard/Android"})
            names = {pathlib.PurePosixPath(e.get("path", "")).name.lower() for e in s.get("entries") or []}
            record("Android/data and obb stay hidden in a listing", ok and not names & {"data", "obb"}, entries=len(names), ms=ms)
            ok, s, _t, ms = call(client, "files_stat", {"path": "/storage/emulated/0/Android/data"})
            record("Android/data is refused by path", not ok and error_code(s) == "INVALID_ARGUMENTS", code=error_code(s), ms=ms)
            ok, s, _t, ms = call(client, "files_read", {"path": ean})
            record("files_read stays in the working directory", not ok and error_code(s) == "INVALID_ARGUMENTS", code=error_code(s), ms=ms)
            ok, s, _t, ms = call(client, "files_list", {"path": "/data/local/tmp"})
            record("paths outside shared storage are refused", not ok and error_code(s) == "INVALID_ARGUMENTS", code=error_code(s), ms=ms)
    finally:
        device.shell("rm", "-rf", SHARED_DIR, check=False)
        restored = device.set_shared_storage_read(previous)
        report["switchRestored"] = previous
        report["switchBeforeRestore"] = restored
    report["passed"] = sum(1 for c in report["checks"] if c["pass"])
    report["failed"] = sum(1 for c in report["checks"] if not c["pass"])
    report["finishedAt"] = datetime.datetime.now().astimezone().isoformat(timespec="seconds")
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--serial", required=True)
    parser.add_argument("--token-source", choices=("auto", "env", "instrumentation", "dumpsys"), default="auto")
    parser.add_argument("--hold-seconds", type=int, default=90)
    parser.add_argument("--report-dir", default=str(REPO / "build" / "evidence"))
    args = parser.parse_args()
    report = run(Device(args.serial), args)
    out_dir = pathlib.Path(args.report_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    path = out_dir / f"{datetime.datetime.now():%Y%m%d-%H%M%S}-{args.serial}-p11-shared-storage.json"
    path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    for check in report["checks"]:
        print(f"{'PASS' if check['pass'] else 'FAIL'}  {check['check']}")
    print(f"passed {report['passed']}, failed {report['failed']}; report: {path}")
    return 0 if report["failed"] == 0 else 1


if __name__ == "__main__":
    sys.exit(main())

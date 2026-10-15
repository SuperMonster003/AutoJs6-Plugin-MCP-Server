"""The P11 text and media tools against a real AutoJs6 host (roadmap P11 D38).

`pinyin_convert` (Pinyin plugin) must read words in context ("银行" -> yín háng; "银行行长" with and without
segmentation is recorded), list
the readings of a heteronym and honor the styles; `pinyin4j_convert` (Pinyin4j plugin) must give tone
marks by default, Wade-Giles on request, every reading of a character with allReadings, and refuse
tone for a non-Hanyu romanization. `mediainfo_read` (MediaInfo plugin) reads a one-second WAV
fixture written into the working directory through `script_run` (the host owns the file, which needs
"All files access" on Android 11+; without it the media checks are skipped), with and without the
text report, and answers INVALID_ARGUMENTS for a missing file.

The report keeps pass / fail per check, counts, formats and timings; never the token or file content.

    py tools/evidence/p11_text_tools.py --serial <serial>
"""
from __future__ import annotations

import argparse
import datetime
import io
import json
import math
import pathlib
import struct
import sys
import wave

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

from device import HOST_PACKAGE, PLUGIN_PACKAGE, Device, DeviceError, progress  # noqa: E402
from session import evidence_session  # noqa: E402
from tool_calls import call, remove_dir, write_file  # noqa: E402

CLIENT_NAME = "autojs6-evidence-p11-text"
REPO = pathlib.Path(__file__).resolve().parents[2]
FIXTURE_DIR = "mcp-evidence-p11-text"
TOOLS = ("pinyin_convert", "pinyin4j_convert", "mediainfo_read")
PLUGINS = {
    "pinyin": "io.github.supermonster003.autojs6.plugin.pinyin",
    "pinyin4j": "io.github.supermonster003.autojs6.plugin.pinyin4j",
    "mediainfo": "io.github.supermonster003.autojs6.plugin.mediainfo",
}


def wav_fixture(seconds: float = 1.0, rate: int = 8000) -> bytes:
    """A mono 16-bit PCM sine at 440 Hz."""
    frames = b"".join(struct.pack("<h", int(12000 * math.sin(2 * math.pi * 440 * i / rate))) for i in range(int(seconds * rate)))
    buffer = io.BytesIO()
    with wave.open(buffer, "wb") as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(rate)
        out.writeframes(frames)
    return buffer.getvalue()


def text_checks(client) -> list[dict]:
    checks = []

    def record(name: str, passed: bool, ms: int, **facts):
        checks.append({"check": name, "pass": bool(passed), "ms": ms, **facts})
        progress(f"{'PASS' if passed else 'FAIL'} {name}")

    ok, s, text, ms = call(client, "pinyin_convert", {"text": "银行"})
    record("pinyin words in context", ok and text == "yín háng", ms, pinyin=text)
    # Recorded, not judged: how segmentation changes a phrase whose characters have several readings.
    on = call(client, "pinyin_convert", {"text": "银行行长"})
    off = call(client, "pinyin_convert", {"text": "银行行长", "segment": False})
    record("pinyin segmentation recorded", on[0] and off[0], on[3] + off[3], segmented=on[2], unsegmented=off[2])
    ok, s, text, ms = call(client, "pinyin_convert", {"text": "行", "heteronym": True})
    readings = (s.get("rows") or [[]])[0]
    record("pinyin heteronym lists readings", ok and len(readings) >= 2 and "|" in text, ms, readings=len(readings))
    ok, s, text, ms = call(client, "pinyin_convert", {"text": "中国", "style": "first_letter"})
    record("pinyin first letters", ok and text == "z g", ms, matches=text == "z g")

    ok, s, text, ms = call(client, "pinyin4j_convert", {"text": "中国"})
    record("pinyin4j tone marks by default", ok and text == "zhōng guó", ms, matches=text == "zhōng guó")
    ok, s, text, ms = call(client, "pinyin4j_convert", {"text": "中国", "romanization": "wade_giles", "separator": "-"})
    record("pinyin4j Wade-Giles", ok and text.startswith("chung") and "-kuo" in text, ms, text=text if ok else None)
    ok, s, text, ms = call(client, "pinyin4j_convert", {"text": "行", "allReadings": True, "tone": "number"})
    readings = (s.get("rows") or [[]])[0]
    record("pinyin4j all readings", ok and len(readings) >= 2 and all(r[-1:].isdigit() for r in readings), ms, readings=len(readings))
    ok, s, _text, ms = call(client, "pinyin4j_convert", {"text": "中", "romanization": "yale", "tone": "number"})
    record("pinyin4j refuses tone outside hanyu", not ok and (s.get("error") or {}).get("code") == "INVALID_ARGUMENTS", ms,
           code=(s.get("error") or {}).get("code"))
    return checks


def media_checks(client, wav: bytes) -> list[dict]:
    checks = []

    def record(name: str, passed: bool, ms: int, **facts):
        checks.append({"check": name, "pass": bool(passed), "ms": ms, **facts})
        progress(f"{'PASS' if passed else 'FAIL'} {name}")

    path = f"{FIXTURE_DIR}/tone.wav"
    ok, s, text, ms = call(client, "mediainfo_read", {"path": path})
    audio = ((s.get("sections") or {}).get("audio") or [{}])[0]
    general = ((s.get("sections") or {}).get("general") or [{}])[0]
    record("mediainfo reads a WAV", ok and (s.get("trackCounts") or {}).get("audio") == 1 and "PCM" in audio.get("format", "")
           and "8" in audio.get("samplingRate", "") and s.get("sizeBytes") == len(wav), ms,
           generalFormat=general.get("format"), audioFormat=audio.get("format"), samplingRate=audio.get("samplingRate"),
           channels=audio.get("channels"), sizeBytes=s.get("sizeBytes"), summaryLines=len(text.splitlines()), error=(s.get("error") or {}).get("code"))
    ok, s, text, ms = call(client, "mediainfo_read", {"path": path, "report": True})
    report = s.get("report") or ""
    record("mediainfo text report", ok and "Format" in report and s.get("reportTruncated") is False, ms, reportChars=len(report))
    ok, s, _text, ms = call(client, "mediainfo_read", {"path": f"{FIXTURE_DIR}/missing.wav"})
    record("mediainfo missing file", not ok and (s.get("error") or {}).get("code") == "INVALID_ARGUMENTS", ms, code=(s.get("error") or {}).get("code"))
    return checks


def run(device: Device, args) -> dict:
    report: dict = {
        "scenario": "p11-text-media-tools",
        "startedAt": datetime.datetime.now().astimezone().isoformat(timespec="seconds"),
        "device": device.facts(),
        "packages": {name: device.package(pkg) for name, pkg in (("host", HOST_PACKAGE), ("plugin", PLUGIN_PACKAGE), *PLUGINS.items())},
        "checks": [],
    }
    with evidence_session(device, report, CLIENT_NAME, hold_seconds=args.hold_seconds, token_source=args.token_source, keep_pairing=args.keep_pairing) as client:
        listed, _elapsed, _metas = client.list_all("tools/list", "tools")
        names = {tool["name"] for tool in listed}
        report["toolCount"] = len(names)
        report["listed"] = {name: name in names for name in TOOLS}
        if {"pinyin_convert", "pinyin4j_convert"} <= names:
            report["checks"] += text_checks(client)
        else:
            report["checks"].append({"check": "pinyin tools listed", "pass": False, "ms": 0})
        if "mediainfo_read" not in names:
            report["checks"].append({"check": "mediainfo_read listed", "pass": False, "ms": 0})
        elif not args.skip_media:
            wav = wav_fixture()
            try:
                write_file(client, f"{FIXTURE_DIR}/tone.wav", wav)
                report["fixture"] = {"tone.wav": len(wav)}
            except DeviceError as error:
                # Without "All files access" Android 11+ refuses AutoJs6 its own working directory.
                report["fixture"] = "not written: " + str(error)[:300]
                progress("the WAV fixture could not be written; skipping the media checks")
            if isinstance(report.get("fixture"), dict):
                try:
                    report["checks"] += media_checks(client, wav)
                finally:
                    report["fixtureRemoved"] = remove_dir(client, FIXTURE_DIR)
    report["passed"] = sum(1 for c in report["checks"] if c["pass"])
    report["failed"] = sum(1 for c in report["checks"] if not c["pass"])
    report["finishedAt"] = datetime.datetime.now().astimezone().isoformat(timespec="seconds")
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--serial", required=True)
    parser.add_argument("--token-source", choices=("auto", "env", "instrumentation", "dumpsys"), default="auto")
    parser.add_argument("--keep-pairing", action="store_true", help="leave the evidence client paired")
    parser.add_argument("--hold-seconds", type=int, default=180)
    parser.add_argument("--skip-media", action="store_true", help="only the pinyin checks")
    parser.add_argument("--report-dir", default=str(REPO / "build" / "evidence"))
    args = parser.parse_args()
    report = run(Device(args.serial), args)
    out_dir = pathlib.Path(args.report_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    path = out_dir / f"{datetime.datetime.now():%Y%m%d-%H%M%S}-{args.serial}-p11-text-tools.json"
    path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    for check in report["checks"]:
        print(f"{'PASS' if check['pass'] else 'FAIL'}  {check['check']}")
    print(f"passed {report['passed']}, failed {report['failed']}; report: {path}")
    return 0 if report["failed"] == 0 else 1


if __name__ == "__main__":
    sys.exit(main())

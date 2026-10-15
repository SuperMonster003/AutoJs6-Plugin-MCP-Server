"""The reasons AutoJs6 gives when its working directory does not exist (host follow-up of roadmap P11, 2026-10-15).

A freshly installed AutoJs6 that was never opened, or one whose working directory was deleted, keeps the
path but has no directory behind it. The scenario needs such a device (for example a read-only emulator
with a newly installed host) and refuses to run otherwise. It checks that `files_write`, `files_list` and
`script_run` name the missing directory and the way out instead of a generic error or a policy message, and
that none of them blames the grant. With `--create` it then creates the directory through adb shell (the
default state of an opened AutoJs6, so it stays) and checks that `files_write` and `script_run` work,
removing the probe file it wrote.

    py tools/evidence/workspace_missing.py --serial <serial> [--create]
"""
from __future__ import annotations

import argparse
import datetime
import json
import pathlib
import re
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

from device import HOST_PACKAGE, PLUGIN_PACKAGE, Device, progress  # noqa: E402
from session import evidence_session  # noqa: E402
from tool_calls import call, remove_file  # noqa: E402

CLIENT_NAME = "autojs6-evidence-workspace-missing"
REPO = pathlib.Path(__file__).resolve().parents[2]
MISSING = re.compile(r"the AutoJs6 working directory (\S+) does not exist")
PROBE = "mcp-evidence-workspace-probe.txt"


def reason(structured: dict, text: str) -> tuple[str | None, str]:
    # Tools with an output schema (script_run) carry the reason of an error in the text content only.
    error = structured.get("error") or {}
    return error.get("code"), error.get("message") or text


def run(device: Device, args) -> dict:
    report: dict = {
        "scenario": "workspace-missing",
        "startedAt": datetime.datetime.now().astimezone().isoformat(timespec="seconds"),
        "device": device.facts(),
        "packages": {name: device.package(pkg) for name, pkg in (("host", HOST_PACKAGE), ("plugin", PLUGIN_PACKAGE))},
        "checks": [],
    }

    def record(name: str, passed: bool, **facts):
        report["checks"].append({"check": name, "pass": bool(passed), **facts})
        progress(f"{'PASS' if passed else 'FAIL'} {name}")

    with evidence_session(device, report, CLIENT_NAME, hold_seconds=args.hold_seconds, token_source=args.token_source) as client:
        ok, s, t, ms = call(client, "files_write", {"path": PROBE, "content": "probe"})
        code, message = reason(s, t)
        match = MISSING.search(message or "")
        if ok:
            record("the working directory is missing on this device", False, hint="files_write succeeded; run on a host whose working directory does not exist")
            report["probeRemoved"] = remove_file(client, PROBE)
            return report
        report["workingDirectory"] = match.group(1) if match else None
        record("files_write names the missing working directory and the way out", match is not None and "Working directory path" in message,
               code=code, message=message, ms=ms)
        ok, s, t, ms = call(client, "files_list", {"path": "."})
        code, message = reason(s, t)
        record("files_list gives the same reason", not ok and MISSING.search(message or "") is not None, code=code, ms=ms)
        ok, s, t, ms = call(client, "script_run", {"source": "1 + 1", "timeoutMs": 10000})
        code, message = reason(s, t)
        record("script_run gives the same reason instead of a policy message", not ok and MISSING.search(message or "") is not None
               and "scoped cwd" not in message, code=code, message=message, ms=ms)
        record("no reason blames the grant", all(c.get("code") != "CAPABILITY_DENIED" for c in report["checks"]))

        if args.create and report["workingDirectory"]:
            # adb shell, not run-as: a run-as shell may not see shared storage at all (seen on an API 30 emulator).
            created = device.shell("mkdir", report["workingDirectory"], check=False)
            report["created"] = created.strip() or "ok"
            ok, s, t, ms = call(client, "files_write", {"path": PROBE, "content": "probe"})
            record("files_write works once the directory exists", ok, ms=ms)
            ok, s, t, ms = call(client, "script_run", {"source": "1 + 1", "timeoutMs": 10000})
            record("script_run works once the directory exists", ok and s.get("status") == "finished", status=s.get("status"), ms=ms)
            report["probeRemoved"] = remove_file(client, PROBE)
    report["passed"] = sum(1 for c in report["checks"] if c["pass"])
    report["failed"] = sum(1 for c in report["checks"] if not c["pass"])
    report["finishedAt"] = datetime.datetime.now().astimezone().isoformat(timespec="seconds")
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--serial", required=True)
    parser.add_argument("--token-source", choices=("auto", "env", "instrumentation", "dumpsys"), default="auto")
    parser.add_argument("--hold-seconds", type=int, default=90)
    parser.add_argument("--create", action="store_true", help="create the working directory through adb shell afterwards and check the tools again")
    parser.add_argument("--report-dir", default=str(REPO / "build" / "evidence"))
    args = parser.parse_args()
    report = run(Device(args.serial), args)
    report.setdefault("passed", sum(1 for c in report["checks"] if c["pass"]))
    report.setdefault("failed", sum(1 for c in report["checks"] if not c["pass"]))
    out_dir = pathlib.Path(args.report_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    path = out_dir / f"{datetime.datetime.now():%Y%m%d-%H%M%S}-{args.serial}-workspace-missing.json"
    path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    for check in report["checks"]:
        print(f"{'PASS' if check['pass'] else 'FAIL'}  {check['check']}")
    print(f"passed {report['passed']}, failed {report['failed']}; report: {path}")
    return 0 if report["failed"] == 0 else 1


if __name__ == "__main__":
    sys.exit(main())

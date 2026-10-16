"""The drawer listener after its plugin process died and wakes did not help (ColorOS, host PluginAutoWake.Rebind).

ColorOS refuses to bind a plugin while none of its processes runs, so the host wakes the plugin (its wake activity starts
the process) before it rebinds the listener the drawer owns. A wake after which the binding is still refused makes the
package back off for 60 s; a host that honors that backoff while it rebinds stops waking and gives the listener up after
its three rebind attempts (OnePlus OPD2413, host 5456, 2026-10-16), while a host that wakes on every rebind attempt brings
the listener back once the plugin can run again.

The scenario stops the plugin package with EvidenceHelperTest runs (each `am instrument` run stops it when it starts and
when it ends; the runs alternate `pairClient` and `revokePairing` for the evidence client, so no pairing is left behind
and no token is read), then runs `am kill` on the device in a loop for `--kill-seconds`, which ends the processes the
host's wakes start without stopping the package. With a loop longer than the whole rebind schedule (34 s) the host gives
the listener up whatever it does; `--expect-given-up` checks that and then waits for the drawer switch: the user turns it
on once the switch went off and the loop ended, and a wake logged after the given-up listener, less than 60 s after the
last wake that did not help, shows that the switch wakes the plugin during the backoff (an on-demand bind would not).

It needs a listener that the AutoJs6 drawer switched on (AutoJs6 in the foreground or allowed to display over other apps,
so that wakes can start) and the plugin debug build with its androidTest APK. The report keeps whether and when the
listener came back and how many host log lines tell of rebind attempts, wakes and a given-up listener; never log text.

    py tools/evidence/listener_rebind.py --serial <serial> [--kill-seconds 6] [--runs 2] [--timeout 120]
    py tools/evidence/listener_rebind.py --serial <serial> --kill-seconds 34 --expect-given-up
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

CLIENT_NAME = "autojs6-evidence-listener-rebind"
REPO = pathlib.Path(__file__).resolve().parents[2]
LOG_MARKERS = {
    "rebindAttemptsFailed": "MCP Server rebind attempt",
    "wakes": f"woke {PLUGIN_PACKAGE} after a refused binding",
    "wakesThatDidNotHelp": f"binding {PLUGIN_PACKAGE} was still refused after the wake",
    # The host publishes the given-up listener as a status without logging it; the third attempt is its last.
    "listenerGivenUp": "MCP Server rebind attempt 3 failed",
}


def run(device: Device, args) -> dict:
    report: dict = {
        "scenario": "listener-rebind",
        "startedAt": datetime.datetime.now().astimezone().isoformat(timespec="seconds"),
        "device": device.facts(),
        "packages": {name: device.package(pkg) for name, pkg in (("host", HOST_PACKAGE), ("plugin", PLUGIN_PACKAGE))},
        "runs": args.runs,
        "killSeconds": args.kill_seconds,
        "checks": [],
    }

    def record(name: str, passed: bool, **facts):
        report["checks"].append({"check": name, "pass": bool(passed), **facts})
        progress(f"{'PASS' if passed else 'FAIL'} {name}")

    state = device.plugin_state()
    record("drawer listener running with the host attached", state["running"] and state.get("host") == "available")
    if not report["checks"][-1]["pass"] or args.runs == 0:
        return finish(report)
    # `adb shell` joins the arguments into one device shell command, so a value with spaces is quoted.
    since = device.shell("date '+%m-%d %H:%M:%S.000'").strip()
    started = time.monotonic()
    fingerprint = None
    for index in range(args.runs):
        if fingerprint is None:
            progress(f"helper run {index + 1}: pairClient")
            fingerprint = device.pre_pair(CLIENT_NAME)
        else:
            progress(f"helper run {index + 1}: revokePairing")
            device.revoke_pairing(fingerprint)
            fingerprint = None
    if fingerprint is not None:
        device.revoke_pairing(fingerprint)
    if args.kill_seconds > 0:
        progress(f"am kill in a loop for {args.kill_seconds} s")
        out = device.shell(f"end=$(( $(date +%s) + {args.kill_seconds} )); n=0; "
                           f"while [ $(date +%s) -lt $end ]; do am kill {PLUGIN_PACKAGE}; n=$((n + 1)); done; echo $n",
                           timeout=args.kill_seconds + 60)
        report["kills"] = int(out.strip() or 0)
    stopped_ms = round((time.monotonic() - started) * 1000)
    # After a given-up listener only the drawer switch brings it back; the user turns it on once the loop ended.
    if args.expect_given_up:
        progress(f"turn the MCP Server switch on in the AutoJs6 drawer now; waiting up to {args.timeout} s")
    try:
        device.wait_listener_with_host(timeout=args.timeout)
        back_ms = round((time.monotonic() - started) * 1000)
    except DeviceError:
        back_ms = None
    # Unlike `adb shell`, `adb logcat` quotes its arguments itself.
    lines = device.run("logcat", "-d", "-T", since, timeout=60, check=False).splitlines()
    report["hostLog"] = counts = {key: sum(1 for line in lines if marker in line) for key, marker in LOG_MARKERS.items()}
    if args.expect_given_up:
        record("the host gave the listener up after a loop beyond its rebind schedule", counts["listenerGivenUp"] > 0, **counts)
        given_up = next((i for i, line in enumerate(lines) if LOG_MARKERS["listenerGivenUp"] in line), None)
        failed = [seconds_of(line) for line in lines[:given_up or 0] if LOG_MARKERS["wakesThatDidNotHelp"] in line]
        woke = [seconds_of(line) for line in lines[(given_up or 0) + 1:] if LOG_MARKERS["wakes"] in line] if given_up is not None else []
        within = round((woke[0] - failed[-1]) * 1000) if woke and failed else None
        # An on-demand bind during the 60 s backoff never wakes; only the switch the user turned on does.
        record("the drawer switch woke the plugin within the backoff", back_ms is not None and within is not None and within < 60_000,
               wakeAfterLastFailedWakeMs=within)
        return finish(report)
    record("listener back with the host attached", back_ms is not None, stoppedForMs=stopped_ms,
           backAfterMs=None if back_ms is None else back_ms - stopped_ms)
    record("the host did not give the listener up", counts["listenerGivenUp"] == 0, **counts)
    if args.kill_seconds > 0:
        record("the host woke the plugin again after a wake that did not help",
               counts["wakesThatDidNotHelp"] > 0 and counts["wakes"] > counts["wakesThatDidNotHelp"])
    return finish(report)


def seconds_of(line: str) -> float:
    """Seconds of the day of a `logcat` line ("MM-DD hh:mm:ss.mmm ...")."""
    hours, minutes, seconds = line.split()[1].split(":")
    return int(hours) * 3600 + int(minutes) * 60 + float(seconds)


def finish(report: dict) -> dict:
    report["passed"] = sum(1 for c in report["checks"] if c["pass"])
    report["failed"] = sum(1 for c in report["checks"] if not c["pass"])
    report["finishedAt"] = datetime.datetime.now().astimezone().isoformat(timespec="seconds")
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--serial", required=True)
    parser.add_argument("--runs", type=int, default=2, help="EvidenceHelperTest runs that stop the plugin package (0: only check the listener)")
    parser.add_argument("--kill-seconds", type=int, default=6, help="seconds of `am kill` in a loop after the runs (0: none)")
    parser.add_argument("--timeout", type=int, default=120, help="seconds to wait for the listener afterwards")
    parser.add_argument("--expect-given-up", action="store_true", help="the loop outlasts the rebind schedule: the host must give the listener up")
    parser.add_argument("--report-dir", default=str(REPO / "build" / "evidence"))
    args = parser.parse_args()
    report = run(Device(args.serial), args)
    out_dir = pathlib.Path(args.report_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    path = out_dir / f"{datetime.datetime.now():%Y%m%d-%H%M%S}-{args.serial}-listener-rebind.json"
    path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    for check in report["checks"]:
        extra = {k: v for k, v in check.items() if k not in ("check", "pass")}
        print(f"{'PASS' if check['pass'] else 'FAIL'}  {check['check']}{'  ' + json.dumps(extra) if extra else ''}")
    print(f"passed {report['passed']}, failed {report['failed']}; report: {path}")
    return 0 if report["failed"] == 0 else 1


if __name__ == "__main__":
    sys.exit(main())

"""The tool list follows plugin packages on a real host (roadmap P11 D28): tools/list_changed after a disable and an enable.

The scenario opens the standalone GET stream of the session, then disables the package of one plugin family
with `pm disable-user` (the Pinyin plugin by default; `--mode reinstall` uninstalls it instead and installs
the same APK again, pulled from the device beforehand) and checks that the server pushes
`notifications/tools/list_changed` without being asked, that the family's tool leaves tools/list, that a direct
call answers PLUGIN_UNAVAILABLE and that device_info no longer reports the family as available. It then
enables the package again (also when a check failed) and checks the push, the listing and a working call,
and that each change sent exactly one notification.

    py tools/evidence/p11_list_changed.py --serial <serial> [--family pinyin] [--mode disable|reinstall]
"""
from __future__ import annotations

import argparse
import datetime
import json
import pathlib
import sys
import threading
import time

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

from device import HOST_PACKAGE, PLUGIN_PACKAGE, Device, progress  # noqa: E402
from session import evidence_session  # noqa: E402
from tool_calls import call  # noqa: E402

CLIENT_NAME = "autojs6-evidence-p11-list-changed"
REPO = pathlib.Path(__file__).resolve().parents[2]
LIST_CHANGED = "notifications/tools/list_changed"
# One call per family that works with a short text argument.
FAMILIES = {
    "pinyin": ("pinyin_convert", {"text": "中文"}),
    "pinyin4j": ("pinyin4j_convert", {"text": "中文"}),
}


class Notifications:
    """Collects the notifications of the standalone stream on a daemon thread."""

    def __init__(self, client, timeout: float):
        self.opened = time.monotonic()
        self.stream = client.listen(timeout=timeout)
        self.received: list[tuple[float, str]] = []
        self.marks: dict[str, float] = {}
        self.error: str | None = None
        self._thread = threading.Thread(target=self._read, daemon=True)
        self._thread.start()

    def _read(self) -> None:
        try:
            while True:
                event = self.stream.next_event()
                if event is None:
                    return
                method = (event.message or {}).get("method")
                if method:
                    self.received.append((time.monotonic(), method))
        except Exception as error:  # noqa: BLE001 - the stream ends with the session
            self.error = f"{type(error).__name__}: {error}"

    def wait(self, method: str, since: float, timeout: float) -> float | None:
        """Seconds from `since` to the first `method` after it, or None."""
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            for at, name in list(self.received):
                if name == method and at >= since:
                    return round(at - since, 2)
            time.sleep(0.1)
        return None

    def close(self) -> None:
        try:
            self.stream.close()
        except OSError:
            pass


def family_entry(client, family: str) -> dict:
    ok, s, _t, _ms = call(client, "device_info", {})
    families = ((s.get("plugins") or {}).get("families") or []) if ok else []
    return next((f for f in families if f.get("id") == family), {})


def run(device: Device, args) -> dict:
    report: dict = {
        "scenario": "p11-list-changed",
        "startedAt": datetime.datetime.now().astimezone().isoformat(timespec="seconds"),
        "device": device.facts(),
        "packages": {name: device.package(pkg) for name, pkg in (("host", HOST_PACKAGE), ("plugin", PLUGIN_PACKAGE))},
        "family": args.family,
        "checks": [],
    }

    def record(name: str, passed: bool, **facts):
        report["checks"].append({"check": name, "pass": bool(passed), **facts})
        progress(f"{'PASS' if passed else 'FAIL'} {name}")

    tool, sample = FAMILIES[args.family]

    def listed() -> bool:
        return tool in {t["name"] for t in client.list_all("tools/list", "tools")[0]}

    with evidence_session(device, report, CLIENT_NAME, hold_seconds=args.hold_seconds, token_source=args.token_source) as client:
        entry = family_entry(client, args.family)
        package = next((p.get("packageName") for p in entry.get("plugins") or [] if p.get("enabled") is not False), None)
        report["pluginPackage"] = package
        record(f"{tool} is listed and the {args.family} family is available", listed() and entry.get("status") == "available" and package is not None,
               status=entry.get("status"))
        if not package:
            return report
        stash = None
        if args.mode == "reinstall":
            # The APKs of the installed plugin, so the scenario can put back exactly what it removed.
            stash = REPO / "build" / "evidence" / "stash" / package
            stash.mkdir(parents=True, exist_ok=True)
            paths = [line.removeprefix("package:").strip() for line in device.shell("pm", "path", package).splitlines() if line.startswith("package:")]
            apks = []
            for index, remote in enumerate(paths):
                local = stash / f"{index}-{pathlib.PurePosixPath(remote).name}"
                device.run("pull", remote, str(local))
                apks.append(str(local))
            report["stashedApks"] = len(apks)
        notifications = Notifications(client, timeout=args.hold_seconds + 30)
        disabled = False
        try:
            time.sleep(1)  # let the GET stream attach before the first change
            since = time.monotonic()
            notifications.marks["disable"] = round(since - notifications.opened, 2)
            if args.mode == "reinstall":
                out = device.shell("pm", "uninstall", package, check=False)
                disabled = out.strip() == "Success"
                report["uninstall"] = out.strip()
                record("pm uninstall removed the plugin package", disabled)
            else:
                out = device.shell("pm", "disable-user", "--user", "0", package, check=False)
                disabled = "disabled" in out
                report["disable"] = out.strip()
                record("pm disable-user changed the plugin package", disabled)
            delay = notifications.wait(LIST_CHANGED, since, args.wait)
            record(f"the server pushes tools/list_changed after the {'uninstall' if args.mode == 'reinstall' else 'disable'}", delay is not None, seconds=delay)
            record(f"{tool} leaves tools/list", not listed())
            ok, s, t, ms = call(client, tool, sample)
            code = (s.get("error") or {}).get("code") or (t.split(":", 1)[0] if t else None)
            record("a direct call answers PLUGIN_UNAVAILABLE", not ok and code == "PLUGIN_UNAVAILABLE", code=code, ms=ms)
            status = family_entry(client, args.family).get("status")
            record("device_info no longer reports the family as available", status not in (None, "available"), status=status)
        finally:
            since = time.monotonic()
            notifications.marks["enable"] = round(since - notifications.opened, 2)
            if args.mode == "reinstall":
                apks = sorted(str(path) for path in stash.iterdir()) if stash else []
                verb = "install-multiple" if len(apks) > 1 else "install"
                report["install"] = device.run(verb, "-r", *apks, check=False, timeout=300).strip().splitlines()[-1:] if apks else "no stashed APK"
            else:
                report["enable"] = device.shell("pm", "enable", package, check=False).strip()
        if disabled:
            delay = notifications.wait(LIST_CHANGED, since, args.wait)
            record(f"the server pushes tools/list_changed after the {'install' if args.mode == 'reinstall' else 'enable'}", delay is not None, seconds=delay)
            # One per change: the SDK used to send one per added or removed tool, a burst of about twice the catalog size.
            count = sum(1 for _at, name in notifications.received if name == LIST_CHANGED)
            record("each change sends exactly one tools/list_changed", count == 2, count=count)
        record(f"{tool} is listed again", listed())
        ok, s, t, ms = call(client, tool, sample)
        record(f"{tool} works again", ok, text=t[:40], ms=ms)
        notifications.close()
        # Seconds after the stream opened, next to the marks of the two package changes.
        report["notifications"] = {"marks": notifications.marks, "count": len(notifications.received),
                                   "first": [[round(at - notifications.opened, 2), name] for at, name in notifications.received[:20]]}
        if notifications.error:
            report["streamEnded"] = notifications.error
    report["passed"] = sum(1 for c in report["checks"] if c["pass"])
    report["failed"] = sum(1 for c in report["checks"] if not c["pass"])
    report["finishedAt"] = datetime.datetime.now().astimezone().isoformat(timespec="seconds")
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--serial", required=True)
    parser.add_argument("--family", choices=sorted(FAMILIES), default="pinyin")
    parser.add_argument("--mode", choices=("disable", "reinstall"), default="disable",
                        help="disable / enable the plugin package, or uninstall it and install the same APK again")
    parser.add_argument("--wait", type=float, default=20, help="seconds to wait for each tools/list_changed")
    parser.add_argument("--token-source", choices=("auto", "env", "instrumentation", "dumpsys"), default="auto")
    parser.add_argument("--hold-seconds", type=int, default=150)
    parser.add_argument("--report-dir", default=str(REPO / "build" / "evidence"))
    args = parser.parse_args()
    report = run(Device(args.serial), args)
    report.setdefault("passed", sum(1 for c in report["checks"] if c["pass"]))
    report.setdefault("failed", sum(1 for c in report["checks"] if not c["pass"]))
    out_dir = pathlib.Path(args.report_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    path = out_dir / f"{datetime.datetime.now():%Y%m%d-%H%M%S}-{args.serial}-p11-list-changed-{args.mode}.json"
    path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    for check in report["checks"]:
        print(f"{'PASS' if check['pass'] else 'FAIL'}  {check['check']}")
    print(f"passed {report['passed']}, failed {report['failed']}; report: {path}")
    return 0 if report["failed"] == 0 else 1


if __name__ == "__main__":
    sys.exit(main())

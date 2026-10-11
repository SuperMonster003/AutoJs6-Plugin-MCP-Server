"""Privileged paths on a real device (roadmap P3.4 / P9): Shizuku auto-enable of accessibility, Shell and root Shell.

1. `device_ensure_accessibility` while the AutoJs6 accessibility service is off: the host's
   automatic enable controller (Shizuku or the secure settings permission) has to switch it on,
   and `device_info` has to report it running afterwards.
2. `shell_exec` of `id` without and with `root: true`. Both need the Shell group, root also the
   "Allow root" switch of the plugin settings page and a root grant for AutoJs6; the scenario prints
   an `ACTION:` line and waits until `shell_exec` is listed.

Needs a listener the drawer switched on (a held host session would restart AutoJs6). The report
holds statuses, error codes, the reported uid names and timings, never other command output. The
accessibility setting is restored to its value before the run unless --keep-accessibility.

    py tools/evidence/privileged.py --serial <serial>
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

from device import HOST_PACKAGE, PLUGIN_PACKAGE, Device, DeviceError, progress  # noqa: E402
from mcp_client import McpClient  # noqa: E402
from session import evidence_session  # noqa: E402

CLIENT_NAME = "autojs6-evidence-privileged"
REPO = pathlib.Path(__file__).resolve().parents[2]
A11Y_KEY = "enabled_accessibility_services"


def call(client: McpClient, name: str, arguments: dict | None = None, timeout: float = 60) -> tuple[dict, dict]:
    started = time.perf_counter()
    result, _ = client.request("tools/call", {"name": name, "arguments": arguments or {}}, timeout=timeout)
    structured = result.get("structuredContent") or {}
    text = (result.get("content") or [{}])[0].get("text", "")
    step = {"ok": not result.get("isError"), "elapsedMs": round((time.perf_counter() - started) * 1000)}
    if result.get("isError"):
        step["code"] = text.split(":")[0]
        step["message"] = text[:160]
    return step, structured


def uid_name(stdout: str) -> str | None:
    match = re.search(r"uid=(\d+)\(([^)]*)\)", stdout or "")
    return f"{match.group(1)}({match.group(2)})" if match else None


def listed(client: McpClient, name: str) -> bool:
    result, _ = client.request("tools/list", {})
    return any(tool.get("name") == name for tool in result.get("tools", []))


def run(device: Device, args) -> dict:
    report: dict = {
        "scenario": "p9-privileged-paths",
        "startedAt": datetime.datetime.now().astimezone().isoformat(timespec="seconds"),
        "device": device.facts(),
        "packages": {name: device.package(pkg) for name, pkg in (("host", HOST_PACKAGE), ("plugin", PLUGIN_PACKAGE),
                                                                  ("shizuku", "moe.shizuku.privileged.api"))},
    }
    state = device.plugin_state()
    if not (state["running"] and state.get("host") == "available"):
        raise DeviceError("switch MCP Server on in the AutoJs6 drawer first")
    a11y_before = device.shell("settings", "get", "secure", A11Y_KEY).strip()
    report["autojs6AccessibilityEnabledBefore"] = f"{HOST_PACKAGE}/" in a11y_before
    try:
        with evidence_session(device, report, CLIENT_NAME, token_source=args.token_source, keep_pairing=args.keep_pairing) as client:
            step, info = call(client, "device_info")
            report["accessibilityBefore"] = info.get("accessibility")
            step, ensured = call(client, "device_ensure_accessibility", timeout=90)
            report["ensureAccessibility"] = {**step, "enabled": ensured.get("enabled")}
            time.sleep(1)
            step, info = call(client, "device_info")
            report["accessibilityAfter"] = info.get("accessibility")

            if not listed(client, "shell_exec"):
                print("ACTION: on the plugin settings page switch the Shell group on and allow root Shell commands", file=sys.stderr, flush=True)
                device.wait_for("shell_exec listed", lambda: listed(client, "shell_exec"), timeout=args.wait_seconds, interval=2)
            step, plain = call(client, "shell_exec", {"cmd": "id"})
            report["shell"] = {**step, "exitCode": plain.get("code"), "uid": uid_name(plain.get("stdout", ""))}
            step, rooted = call(client, "shell_exec", {"cmd": "id", "root": True}, timeout=90)
            if not step["ok"] and args.wait_seconds and "root" in step.get("message", "").lower():
                print("ACTION: allow root Shell commands on the plugin settings page (and grant root to AutoJs6 if asked)", file=sys.stderr, flush=True)
                until = time.monotonic() + args.wait_seconds
                while not step["ok"] and time.monotonic() < until:
                    time.sleep(3)
                    step, rooted = call(client, "shell_exec", {"cmd": "id", "root": True}, timeout=90)
            report["rootShell"] = {**step, "exitCode": rooted.get("code"), "uid": uid_name(rooted.get("stdout", ""))}
    finally:
        if not args.keep_accessibility:
            current = device.shell("settings", "get", "secure", A11Y_KEY).strip()
            if current != a11y_before:
                device.shell("settings", "put", "secure", A11Y_KEY, a11y_before)
                report["accessibilityRestored"] = device.shell("settings", "get", "secure", A11Y_KEY).strip() == a11y_before
    report["finishedAt"] = datetime.datetime.now().astimezone().isoformat(timespec="seconds")
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--serial", required=True)
    parser.add_argument("--wait-seconds", type=int, default=600, help="how long to wait for the on-screen steps")
    parser.add_argument("--keep-accessibility", action="store_true", help="leave the accessibility setting as the run left it")
    parser.add_argument("--token-source", choices=("auto", "env", "instrumentation", "dumpsys"), default="auto")
    parser.add_argument("--keep-pairing", action="store_true", help="leave the evidence client paired")
    parser.add_argument("--report-dir", default=str(REPO / "build" / "evidence"))
    args = parser.parse_args()
    report = run(Device(args.serial), args)
    out_dir = pathlib.Path(args.report_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    path = out_dir / f"{datetime.datetime.now():%Y%m%d-%H%M%S}-{args.serial}-privileged.json"
    path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2, ensure_ascii=False))
    print(f"report: {path}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())

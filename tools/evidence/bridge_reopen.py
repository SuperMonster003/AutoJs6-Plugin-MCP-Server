"""The stdio bridge across a listener restart on a real device (roadmap P9, PC bridge).

The phone's MCP session ends whenever its listener restarts (drawer switch, port or network change,
plugin update). The scenario drives `autojs6-mcp-bridge` over stdio the way a desktop client does:
`initialize`, `notifications/initialized` and `tools/call device_ping`; then it restarts the plugin
package, the host rebinds it and a new listener starts, and the same bridge process is asked for
`device_ping` twice more. A bridge that handles the ended session opens a new one with the client's
`initialize` and answers; 0.1.0 answered with a JSON-RPC error that blamed the endpoint path.

It needs a listener that the AutoJs6 drawer owns (running with the host attached): a session held
by the host instrumentation does not survive the restart. The restart is an `am instrument` run of
`EvidenceHelperTest#pairClient` for the already paired evidence client (idempotent), which restarts
the plugin package the way an update does, so the plugin androidTest APK is required. Neither
`am force-stop` nor `run-as kill` is used: the host treats a force-stop as the user's decision and
leaves the server off until the drawer switch is turned on again, and SELinux refuses `run-as`
signals to the app process on recent Android versions. Each `--bridge` command runs the case
once, so an old and a new bridge can be compared in one run. The token reaches the bridge through
its environment only. The report holds statuses, timings, error classes and what the bridge log
mentions, never the token, session ids or log lines.

    py tools/evidence/bridge_reopen.py --serial <serial> --bridge "node <bridge repo>/dist/cli.js"
"""
from __future__ import annotations

import argparse
import datetime
import json
import os
import pathlib
import queue
import re
import shlex
import subprocess
import sys
import threading
import time

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

from device import HOST_PACKAGE, PLUGIN_PACKAGE, PLUGIN_TEST_RUNNER, Device, DeviceError, progress, scrub  # noqa: E402

CLIENT_NAME = "autojs6-evidence-bridge"
PROTOCOL = "2025-11-25"
LISTENER_PROCESS = f"{PLUGIN_PACKAGE}:mcp_server"
REPO = pathlib.Path(__file__).resolve().parents[2]
SESSION_ID = re.compile(r"(session )[0-9A-Za-z_-]{8,}")


class StdioPeer:
    """The client side of a bridge process: newline-delimited JSON-RPC on stdin and stdout."""

    def __init__(self, command: list[str], env: dict):
        self.process = subprocess.Popen(command, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                        env=env, text=True, encoding="utf-8", errors="replace", bufsize=1)
        self.messages: queue.Queue = queue.Queue()
        self.log: list[str] = []
        threading.Thread(target=self._read_messages, daemon=True).start()
        threading.Thread(target=self._read_log, daemon=True).start()

    def _read_messages(self) -> None:
        for line in self.process.stdout:
            if line.strip():
                self.messages.put(json.loads(line))
        self.messages.put(None)

    def _read_log(self) -> None:
        for line in self.process.stderr:
            self.log.append(SESSION_ID.sub(r"\1<id>", line.rstrip("\n")))

    def send(self, message: dict) -> None:
        self.process.stdin.write(json.dumps(message) + "\n")
        self.process.stdin.flush()

    def request(self, request_id: int, method: str, params: dict | None = None, timeout: float = 90) -> tuple[dict, list[dict]]:
        """Returns the answer and the notifications that arrived before it."""
        self.send({"jsonrpc": "2.0", "id": request_id, "method": method, **({"params": params} if params else {})})
        notifications = []
        until = time.monotonic() + timeout
        while True:
            try:
                message = self.messages.get(timeout=max(0.1, until - time.monotonic()))
            except queue.Empty:
                raise DeviceError(f"no answer to {method} within {timeout} s") from None
            if message is None:
                raise DeviceError(f"the bridge exited before answering {method}")
            if message.get("id") == request_id and ("result" in message or "error" in message):
                return message, notifications
            notifications.append(message)

    def close(self, timeout: float = 30) -> int:
        self.process.stdin.close()
        try:
            return self.process.wait(timeout=timeout)
        except subprocess.TimeoutExpired:
            self.process.kill()
            return -1


def bridge_version(command: list[str]) -> str:
    out = subprocess.run([*command, "--version"], capture_output=True, text=True, timeout=60)
    return out.stdout.strip() or "unknown"


def listener_pid(device: Device) -> str:
    return device.shell("pidof", LISTENER_PROCESS, check=False).strip()


def restart_listener(device: Device) -> dict:
    """Restarts the plugin package; the host rebinds it and a new listener with no sessions starts."""
    before = listener_pid(device)
    if not before:
        raise DeviceError(f"no {LISTENER_PROCESS} process")
    started = time.perf_counter()
    device.pre_pair(CLIENT_NAME)
    device.wait_for("a new listener process with the host attached",
                    lambda: (pid := listener_pid(device)) and pid != before and device.plugin_state()["running"]
                    and device.plugin_state().get("host") == "available", timeout=90)
    return {"newProcess": True, "restartMs": round((time.perf_counter() - started) * 1000)}


def call_facts(answer: dict, notifications: list[dict], elapsed_ms: int) -> dict:
    facts: dict = {"elapsedMs": elapsed_ms}
    if "error" in answer:
        message = answer["error"].get("message", "")
        facts.update({
            "ok": False,
            "errorCode": answer["error"].get("code"),
            "errorBlamesEndpointPath": "endpoint path" in message,
            "errorNamesEndedSession": "session ended" in message or "no longer knows" in message,
        })
    else:
        result = answer.get("result") or {}
        facts.update({"ok": not result.get("isError"), "isError": bool(result.get("isError"))})
    facts["notifications"] = sorted({n.get("method") for n in notifications if n.get("method")})
    return facts


def timed_call(peer: StdioPeer, request_id: int) -> dict:
    started = time.perf_counter()
    answer, notifications = peer.request(request_id, "tools/call", {"name": "device_ping", "arguments": {}})
    return call_facts(answer, notifications, round((time.perf_counter() - started) * 1000))


def run_case(device: Device, command: list[str], url: str, token: str) -> dict:
    case: dict = {"bridge": bridge_version(command)}
    progress(f"case: {case['bridge']}")
    env = {**os.environ, "AUTOJS6_MCP_TOKEN": token}
    peer = StdioPeer([*command, "--url", url], env)
    try:
        answer, _ = peer.request(1, "initialize", {
            "protocolVersion": PROTOCOL,
            "capabilities": {},
            "clientInfo": {"name": CLIENT_NAME, "version": "1"},
        })
        result = answer.get("result") or {}
        capabilities = result.get("capabilities") or {}
        case["initialize"] = {
            "ok": "result" in answer,
            "protocolVersion": result.get("protocolVersion"),
            "serverVersion": (result.get("serverInfo") or {}).get("version"),
            "listChanged": sorted(k for k, v in capabilities.items() if isinstance(v, dict) and v.get("listChanged")),
        }
        peer.send({"jsonrpc": "2.0", "method": "notifications/initialized"})
        case["beforeRestart"] = timed_call(peer, 2)
        progress("restarting the plugin package (am instrument, the host rebinds)")
        case["restart"] = restart_listener(device)
        case["firstCallAfterRestart"] = timed_call(peer, 3)
        case["secondCallAfterRestart"] = timed_call(peer, 4)
    finally:
        case["exitCode"] = peer.close()
        for line in peer.log:
            progress(f"bridge: {scrub(line, token)}")
        case["log"] = {
            "opensNewSession": any("opened session" in line for line in peer.log),
            "blamesEndpointPath": any("endpoint path" in line for line in peer.log),
            "lines": len(peer.log),
        }
    return case


def run(device: Device, args) -> dict:
    report: dict = {
        "scenario": "p9-bridge-session-reopen",
        "startedAt": datetime.datetime.now().astimezone().isoformat(timespec="seconds"),
        "device": device.facts(),
        "packages": {name: device.package(pkg) for name, pkg in (("host", HOST_PACKAGE), ("plugin", PLUGIN_PACKAGE))},
        "protocol": PROTOCOL,
    }
    state = device.plugin_state()
    if not device.has_instrumentation(PLUGIN_TEST_RUNNER):
        raise DeviceError("the plugin androidTest APK is not installed (needed to restart the listener and to pair)")
    if not (state["running"] and state.get("host") == "available"):
        raise DeviceError("switch MCP Server on in the AutoJs6 drawer first: the scenario restarts a drawer-owned listener")
    progress("reading the token")
    token, source = device.read_token(args.token_source)
    report["tokenSource"] = source
    device.wait_listener_with_host()
    progress(f"pre-pairing {CLIENT_NAME}")
    pairing = device.pre_pair(CLIENT_NAME)
    report["pairing"] = {"method": "EvidenceHelperTest#pairClient", "fingerprint": pairing}
    try:
        port = device.wait_listener_with_host()["port"]
        with device.forward(port) as local_port:
            url = f"http://127.0.0.1:{local_port}/mcp"
            report["cases"] = [run_case(device, shlex.split(command), url, token) for command in args.bridge]
    finally:
        if not args.keep_pairing:
            progress("revoking the evidence pairing")
            device.revoke_pairing(pairing)
            report["pairing"]["revoked"] = True
        progress("waiting for the host to rebind the listener")
        try:
            device.wait_listener_with_host()
        except DeviceError:
            raise DeviceError("the listener did not come back; turn MCP Server on again in the AutoJs6 drawer") from None
    report["finishedAt"] = datetime.datetime.now().astimezone().isoformat(timespec="seconds")
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--serial", required=True)
    parser.add_argument("--bridge", action="append", required=True,
                        help='bridge command, repeatable, e.g. "node ../AutoJs6-MCP-Bridge/dist/cli.js"')
    parser.add_argument("--token-source", choices=("auto", "env", "instrumentation", "dumpsys"), default="auto")
    parser.add_argument("--keep-pairing", action="store_true", help="leave the evidence client paired")
    parser.add_argument("--report-dir", default=str(REPO / "build" / "evidence"))
    args = parser.parse_args()
    report = run(Device(args.serial), args)
    out_dir = pathlib.Path(args.report_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    path = out_dir / f"{datetime.datetime.now():%Y%m%d-%H%M%S}-{args.serial}-bridge-reopen.json"
    path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2, ensure_ascii=False))
    print(f"report: {path}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())

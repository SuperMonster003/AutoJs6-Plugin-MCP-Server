"""Lifecycle S5 on a real device (roadmap P6 / P9): the token is rotated while a client session is live.

The scenario opens a session with the current token and checks `device_ping`, then waits until the
token fingerprint in `dumpsys` changes: rotate the token on the plugin settings page meanwhile
(Token > Rotate token). Afterwards it expects what docs/dev/lifecycle-matrix.md lists for S5: the
old token is refused with 401 (JSON-RPC UNAUTHORIZED), the old session id with the new token gets
404 "Session not found", and a new session with the new token answers `device_ping` and
`device_info` without a pairing prompt (pairing is keyed by client name and address class).

Needs a listener the drawer switched on. Neither token is printed or stored; the report holds
fingerprints, statuses, error codes and timings.

    py tools/evidence/token_rotation.py --serial <serial>
"""
from __future__ import annotations

import argparse
import datetime
import json
import pathlib
import re
import sys
import time
import urllib.error
import urllib.request

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

from device import HOST_PACKAGE, PLUGIN_PACKAGE, PLUGIN_SERVICE, Device, DeviceError, progress  # noqa: E402
from mcp_client import McpClient  # noqa: E402

CLIENT_NAME = "autojs6-evidence-rotation"
REPO = pathlib.Path(__file__).resolve().parents[2]


def fingerprint(device: Device) -> str | None:
    out = device.shell("dumpsys", "activity", "service", PLUGIN_SERVICE, check=False)
    match = re.search(r"tokenFingerprint: (\w+)", out)
    return match.group(1) if match else None


def raw_post(url: str, token: str, session_id: str | None, body: dict) -> tuple[int, dict | None]:
    """One POST outside the client, to observe the HTTP status of a stale credential or session."""
    headers = {"Content-Type": "application/json", "Accept": "application/json, text/event-stream",
               "Authorization": f"Bearer {token}", "MCP-Protocol-Version": "2025-11-25"}
    if session_id:
        headers["Mcp-Session-Id"] = session_id
    request = urllib.request.Request(url, json.dumps(body).encode(), headers, method="POST")
    try:
        with urllib.request.urlopen(request, timeout=15) as response:
            return response.status, None
    except urllib.error.HTTPError as error:
        payload = error.read().decode("utf-8", "replace")
        try:
            return error.code, json.loads(payload)
        except ValueError:
            return error.code, None


def tool(client: McpClient, name: str) -> dict:
    started = time.perf_counter()
    result, _ = client.request("tools/call", {"name": name, "arguments": {}})
    text = (result.get("content") or [{}])[0].get("text", "")
    return {"ok": not result.get("isError"), "elapsedMs": round((time.perf_counter() - started) * 1000),
            "code": text.split(":")[0] if result.get("isError") else None}


def run(device: Device, args) -> dict:
    report: dict = {
        "scenario": "p9-lifecycle-s5-token-rotation",
        "startedAt": datetime.datetime.now().astimezone().isoformat(timespec="seconds"),
        "device": device.facts(),
        "packages": {name: device.package(pkg) for name, pkg in (("host", HOST_PACKAGE), ("plugin", PLUGIN_PACKAGE))},
    }
    if not device.plugin_state()["running"]:
        raise DeviceError("switch MCP Server on in the AutoJs6 drawer first")
    progress("reading the token and pre-pairing the evidence client")
    old_token, source = device.read_token(args.token_source)
    report["tokenSource"] = source
    pairing = device.pre_pair(CLIENT_NAME)
    report["pairing"] = {"fingerprint": pairing}
    try:
        port = device.wait_listener_with_host(timeout=180)["port"]
        with device.forward(port) as local_port:
            url = f"http://127.0.0.1:{local_port}/mcp"
            before = fingerprint(device)
            report["fingerprintBefore"] = before
            old = McpClient(url, old_token, CLIENT_NAME)
            old.initialize()
            old_session = old.session_id
            report["beforeRotation"] = tool(old, "device_ping")
            progress("rotate the token on the plugin settings page now (waiting up to "
                     f"{args.wait_seconds} s for the fingerprint to change)")
            device.wait_for("token fingerprint changed", lambda: (fp := fingerprint(device)) and fp != before and fp,
                            timeout=args.wait_seconds, interval=1)
            rotated_at = time.perf_counter()
            report["fingerprintAfter"] = fingerprint(device)
            device.wait_for("listener running with the host attached",
                            lambda: (s := device.plugin_state())["running"] and s.get("host") == "available", timeout=60)
            report["listenerBackMs"] = round((time.perf_counter() - rotated_at) * 1000)

            ping = {"jsonrpc": "2.0", "id": 90, "method": "tools/call", "params": {"name": "device_ping", "arguments": {}}}
            status, body = raw_post(url, old_token, old_session, ping)
            report["oldTokenOldSession"] = {"status": status, "rpcCode": ((body or {}).get("error") or {}).get("code"),
                                            "message": ((body or {}).get("error") or {}).get("message", "")[:80]}

            new_token, _ = device.read_token(args.token_source)
            device.wait_listener_with_host(timeout=180)
            status, body = raw_post(url, new_token, old_session, ping)
            report["newTokenOldSession"] = {"status": status, "rpcCode": ((body or {}).get("error") or {}).get("code"),
                                            "message": ((body or {}).get("error") or {}).get("message", "")[:80]}
            started = time.perf_counter()
            fresh = McpClient(url, new_token, CLIENT_NAME)
            init = fresh.initialize()
            report["newSession"] = {"initializeMs": round((time.perf_counter() - started) * 1000),
                                    "protocolVersion": init.get("protocolVersion"),
                                    "ping": tool(fresh, "device_ping"), "info": tool(fresh, "device_info")}
            report["pairedClientsKept"] = any(c["fingerprint"] == pairing for c in device.plugin_state()["pairedClients"])
            fresh.close()
            old_token = new_token = None  # noqa: F841 - drop the references early
    finally:
        if not args.keep_pairing:
            progress("revoking the evidence pairing")
            device.revoke_pairing(pairing)
            report["pairing"]["revoked"] = True
            device.wait_listener_with_host(timeout=180)
    report["finishedAt"] = datetime.datetime.now().astimezone().isoformat(timespec="seconds")
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--serial", required=True)
    parser.add_argument("--wait-seconds", type=int, default=600, help="how long to wait for the rotation")
    parser.add_argument("--token-source", choices=("auto", "env", "instrumentation", "dumpsys"), default="auto")
    parser.add_argument("--keep-pairing", action="store_true", help="leave the evidence client paired")
    parser.add_argument("--report-dir", default=str(REPO / "build" / "evidence"))
    args = parser.parse_args()
    report = run(Device(args.serial), args)
    out_dir = pathlib.Path(args.report_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    path = out_dir / f"{datetime.datetime.now():%Y%m%d-%H%M%S}-{args.serial}-token-rotation.json"
    path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2, ensure_ascii=False))
    print(f"report: {path}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())

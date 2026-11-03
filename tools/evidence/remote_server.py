"""Exercise device_start_remote_server over real MCP, preserving the host's original server state."""
from __future__ import annotations

import argparse
import datetime
import json
import pathlib
import socket
import sys
import time

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

from device import Device, DeviceError, HOST_PACKAGE, PLUGIN_PACKAGE
from session import evidence_session
from tool_calls import call, script

TOOL = "device_start_remote_server"
# The application-owned service getter is Kotlin internal (its JVM suffix depends on the module).
SERVICE = ("(function(){var a=org.autojs.autojs.AutoJs.getInstance();var ms=a.getClass().getMethods();"
           "for(var i=0;i<ms.length;i++){if(String(ms[i].getName()).indexOf('getDevPluginService')===0)"
           "return ms[i].invoke(a,[]);}throw Error('Host service getter unavailable');})()")
MARKER = "MCP_REMOTE_SERVER_STATE="


def state(client) -> dict:
    source = (f"var s = {SERVICE}; console.log('{MARKER}' + JSON.stringify({{"
              "running: s.isServerSocketConnected(), closed: s.isServerSocketNormallyClosed(), "
              "startedAt: Number(s.getServerStartedAt())}));")
    ok, result, text, _ = call(client, "script_run", {"source": source})
    if not ok or result.get("status") != "finished":
        raise DeviceError("Cannot read remote server state: " + json.dumps(result.get("exception", text), ensure_ascii=True)[:600])
    for line in reversed(result.get("console", [])):
        text = line.get("text", "")
        if MARKER in text:
            return json.loads(text.split(MARKER, 1)[1])
    raise DeviceError("The host did not report its remote server state")


def run(device: Device, args) -> dict:
    report = {"scenario": "remote-server", "device": device.facts(),
              "packages": {name: device.package(pkg) for name, pkg in (("host", HOST_PACKAGE), ("plugin", PLUGIN_PACKAGE))}}
    with evidence_session(device, report, "autojs6-evidence-remote-server", hold_seconds=45, token_source=args.token_source) as client:
        listed, _, _ = client.list_all("tools/list", "tools")
        report["listed"] = any(row["name"] == TOOL for row in listed)
        if args.expect_hidden:
            ok, _, text, _ = call(client, TOOL, {})
            report["hiddenRejected"] = not ok and text.startswith("CAPABILITY_DENIED:")
            if report["listed"] or not report["hiddenRejected"]:
                raise DeviceError("An older host did not hide and reject the new tool")
            return report
        if not report["listed"]:
            raise DeviceError("The host did not grant app.startRemoteServer")
        before = state(client)
        report["before"] = before
        try:
            ok, result, text, elapsed = call(client, TOOL, {})
            if not ok or result.get("enabled") is not True or result.get("port") != 7347:
                raise DeviceError(f"Remote server startup failed: {text[:300]}")
            if result["alreadyRunning"] != before["running"]:
                raise DeviceError("Incorrect alreadyRunning result")
            report["start"] = {**result, "elapsedMs": elapsed}
            with device.forward(7347) as port, socket.create_connection(("127.0.0.1", port), timeout=5) as peer:
                peer.settimeout(5)
                report["tcpHello"] = bool(peer.recv(4096))
                if not report["tcpHello"]:
                    raise DeviceError("The remote listener did not send a hello")
                started = state(client)
                ok, result, text, elapsed = call(client, TOOL, {})
                if not ok or result.get("alreadyRunning") is not True:
                    raise DeviceError(f"Repeated startup failed: {text[:300]}")
                report["repeat"] = {**result, "elapsedMs": elapsed}
                report["sameListener"] = state(client)["startedAt"] == started["startedAt"]
                if not report["sameListener"] or result.get("connectedClients", 0) < 1:
                    raise DeviceError("Repeated startup replaced the listener or its existing client")
                ok, _, text, _ = call(client, TOOL, {"port": 9637})
                report["invalidArgumentsRejected"] = not ok and text.startswith("INVALID_ARGUMENTS:")
                if not report["invalidArgumentsRejected"]:
                    raise DeviceError("The tool accepted unexpected arguments")
                client.close()
                with socket.create_connection(("127.0.0.1", port), timeout=5) as after_close:
                    after_close.settimeout(5)
                    report["survivesMcpSession"] = bool(after_close.recv(4096))
                if not report["survivesMcpSession"]:
                    raise DeviceError("The remote listener stopped with the MCP session")
                client.initialize()
        finally:
            if client.session_id is None:
                client.initialize()
            if not before["running"]:
                script(client, f"var s = {SERVICE}; s.disconnectJsonSocketServer(); "
                       f"s.setServerSocketNormallyClosed({str(before['closed']).lower()});")
                time.sleep(0.2)
            after = state(client)
            report["restored"] = all(after[key] == before[key] for key in ("running", "closed"))
            if not report["restored"]:
                raise DeviceError("Could not restore the original remote server state")
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--token-source", choices=("auto", "env", "instrumentation", "dumpsys"), default="auto")
    parser.add_argument("--expect-hidden", action="store_true")
    parser.add_argument("--report-dir", default=str(pathlib.Path(__file__).resolve().parents[2] / "build" / "evidence"))
    args = parser.parse_args()
    report = run(Device(args.serial), args)
    output = pathlib.Path(args.report_dir)
    output.mkdir(parents=True, exist_ok=True)
    path = output / f"{datetime.datetime.now():%Y%m%d-%H%M%S}-{args.serial}-remote-server.json"
    path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))
    print(f"report: {path}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())

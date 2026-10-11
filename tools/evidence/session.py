"""The common frame of a device scenario: token, evidence pairing, host session, forward, MCP session.

The EvidenceHelperTest helpers restart the plugin package (`am instrument` force-stops it), so they
run before the host session is held; a listener the drawer owns comes back on its own because the
host rebinds. The pairing recorded for the evidence client is revoked afterwards unless asked not to.
"""
from __future__ import annotations

from contextlib import contextmanager
from typing import Iterator

from device import PLUGIN_TEST_RUNNER, Device, progress
from mcp_client import McpClient


@contextmanager
def evidence_session(device: Device, report: dict, client_name: str, hold_seconds: int = 240,
                     token_source: str = "auto", keep_pairing: bool = False, rebind_timeout: float = 180) -> Iterator[McpClient]:
    was_running = device.plugin_state()["running"]
    progress("reading the token")
    token, source = device.read_token(token_source)
    report["tokenSource"] = source
    pairing = None
    if source != "env" and device.has_instrumentation(PLUGIN_TEST_RUNNER):
        progress(f"pre-pairing {client_name}")
        pairing = device.pre_pair(client_name)
        report["pairing"] = {"method": "EvidenceHelperTest#pairClient", "fingerprint": pairing}
    if was_running:
        # MIUI took more than a minute to let the host rebind after the helpers (2026-10-11).
        progress("waiting for the host to rebind the restarted listener")
        device.wait_listener_with_host(timeout=rebind_timeout)
    try:
        with device.host_session(hold_seconds) as session:
            report["hostSession"] = {"source": session["source"], "grantedMethods": session.get("grantedMethods")}
            progress(f"host session ready ({session['source']}), port {session['port']}")
            with device.forward(session["port"]) as local_port, \
                    McpClient(f"http://127.0.0.1:{local_port}/mcp", token, client_name) as client:
                init = client.initialize()
                report["initialize"] = {"protocolVersion": init.get("protocolVersion"), "serverInfo": init.get("serverInfo")}
                yield client
    finally:
        if pairing and not keep_pairing:
            progress("revoking the evidence pairing")
            device.revoke_pairing(pairing)
            report["pairing"]["revoked"] = True

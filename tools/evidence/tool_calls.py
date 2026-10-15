"""Tool calls shared by the P11 device scenarios: one `tools/call`, a host script, a file written by the host, the accessibility check."""
from __future__ import annotations

import base64
import time

from device import Device, DeviceError, progress

# Raw bytes per script_run call: the base64 text plus the script stays below the broker's 32 KiB argument limit.
WRITE_CHUNK = 18 * 1024


def call(client, name: str, arguments: dict, timeout: float = 120) -> tuple[bool, dict, str, int]:
    """(ok, structured content, first text content, elapsed ms) of one tools/call."""
    started = time.perf_counter()
    result, _ = client.request("tools/call", {"name": name, "arguments": arguments}, timeout=timeout)
    text = next((item.get("text", "") for item in result.get("content", []) if item.get("type") == "text"), "")
    return not result.get("isError"), result.get("structuredContent") or {}, text, round((time.perf_counter() - started) * 1000)


def script(client, source: str) -> None:
    ok, structured, text, _ms = call(client, "script_run", {"source": source, "timeoutMs": 30000})
    if not ok or structured.get("status") != "finished":
        # script_run has an output schema, so an error result carries its reason in the text content only.
        reason = structured.get("error") or structured.get("exception") or (structured.get("status") and f"status {structured['status']}") or text
        raise DeviceError(f"script_run failed: {str(reason)[:300]}")


def write_file(client, relative: str, data: bytes) -> None:
    """Writes `data` into the AutoJs6 working directory through script_run, so that the host owns the file.

    The broker caps engines.execScript arguments at 32 KiB, so the base64 text goes in chunks: the
    first call creates the file, the others append. Needs "All files access" on Android 11+.
    """
    for offset in range(0, max(len(data), 1), WRITE_CHUNK):
        encoded = base64.b64encode(data[offset:offset + WRITE_CHUNK]).decode("ascii")
        script(client, f'var p = files.path("{relative}"); files.createWithDirs(p); '
                       f'var out = new java.io.FileOutputStream(p, {"true" if offset else "false"}); '
                       f'try {{ out.write(android.util.Base64.decode("{encoded}", 0)); }} finally {{ out.close(); }}')


def remove_dir(client, relative: str) -> bool | str:
    try:
        script(client, f'files.removeDir(files.path("{relative}"));')
        return True
    except DeviceError as error:
        return str(error)


def remove_file(client, relative: str) -> bool | str:
    """Removes a file of the working directory through script_run (files_delete is off by default)."""
    try:
        script(client, f'files.remove(files.path("{relative}"));')
        return True
    except DeviceError as error:
        return str(error)


def ensure_accessibility(device: Device, client, report: dict) -> bool:
    def operational() -> bool:
        ok, structured, _text, _ms = call(client, "device_ensure_accessibility", {}, timeout=30)
        return ok and structured.get("enabled") is True

    if device.host_accessibility_crashed():
        # A held host session restarts the host process; rebinding the service is a toggle of the setting it already has.
        progress("rebinding the AutoJs6 accessibility service")
        report["accessibilityRebound"] = device.rebind_host_accessibility()
    for attempt in range(5):
        if operational():
            report["accessibility"] = f"operational (attempt {attempt + 1})"
            return True
        time.sleep(3)
    report["accessibility"] = "unavailable"
    return False

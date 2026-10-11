"""adb helpers for device evidence: forwarding, plugin state, token, pairing and a held host session.

Rules shared by every scenario (see README.md):
- The token is returned to the caller only; it is never printed, logged or written to a file, and
  adb output that may carry it is scrubbed before it appears in an exception.
- Every state change made here (forwards, held sessions, system properties, package states) has a
  matching restore step that the scenarios run in `finally` blocks.
- `am instrument` force-stops the whole plugin package when it starts and when it ends, so the
  EvidenceHelperTest helpers also stop a running listener. Scenarios call them before a host session
  is held; a session the drawer owns comes back on its own because the host rebinds.
"""
from __future__ import annotations

import os
import re
import subprocess
import sys
import time
from contextlib import contextmanager
from typing import Iterator

PLUGIN_PACKAGE = "io.github.supermonster003.autojs6.plugin.mcp.server"
PLUGIN_SERVICE = f"{PLUGIN_PACKAGE}/.McpServerService"
PLUGIN_TEST_RUNNER = f"{PLUGIN_PACKAGE}.test/androidx.test.runner.AndroidJUnitRunner"
EVIDENCE_HELPER = f"{PLUGIN_PACKAGE}.EvidenceHelperTest"
HOST_PACKAGE = "org.autojs.autojs6"
HOST_TEST_RUNNER = f"{HOST_PACKAGE}.test/androidx.test.runner.AndroidJUnitRunner"
HOST_ROUND_TRIP = "org.autojs.autojs.core.plugin.mcp.McpServerPluginRoundTripTest#openServerReachesRunningWithTheHostAttachedAndStopsCleanly"
HOST_HOLD_PROPERTY = "debug.autojs6.mcp.hold"
DEFAULT_PORT = 9637
TOKEN_PATTERN = re.compile(r"[A-Za-z0-9_-]{32,256}")
FINGERPRINT_PATTERN = re.compile(r"[0-9a-f]{24}")


class DeviceError(RuntimeError):
    pass


def progress(message: str) -> None:
    print(f"[{time.strftime('%H:%M:%S')}] {message}", file=sys.stderr, flush=True)


def scrub(text: str, secret: str | None = None) -> str:
    """Removes token-like values from adb output before it is shown anywhere."""
    text = re.sub(r"(mcpEvidenceToken=|token: )\S+", r"\1<redacted>", text)
    return text.replace(secret, "<redacted>") if secret else text


class Device:
    def __init__(self, serial: str, adb: str | None = None):
        self.serial = serial
        self.adb = adb or os.environ.get("ADB", "adb")

    def run(self, *args: str, timeout: float = 120, check: bool = True) -> str:
        result = subprocess.run([self.adb, "-s", self.serial, *args], capture_output=True, text=True,
                                encoding="utf-8", errors="replace", timeout=timeout)
        if check and result.returncode != 0:
            raise DeviceError(f"adb {' '.join(args[:3])} ... failed ({result.returncode}): {scrub(result.stderr.strip())}")
        return result.stdout

    def shell(self, *args: str, timeout: float = 120, check: bool = True) -> str:
        return self.run("shell", *args, timeout=timeout, check=check)

    # Facts -----------------------------------------------------------------------------------

    def getprop(self, name: str) -> str:
        return self.shell("getprop", name).strip()

    def facts(self) -> dict:
        return {
            "serial": self.serial,
            "model": self.getprop("ro.product.model"),
            "manufacturer": self.getprop("ro.product.manufacturer"),
            "sdk": int(self.getprop("ro.build.version.sdk") or 0),
            "release": self.getprop("ro.build.version.release"),
        }

    def package(self, package: str) -> dict | None:
        out = self.shell("dumpsys", "package", package, check=False)
        code = re.search(r"versionCode=(\d+)", out)
        if not code:
            return None
        name = re.search(r"versionName=(\S+)", out)
        flags = re.search(r"\bflags=\[([^\]]*)\]", out)
        return {
            "versionName": name.group(1) if name else None,
            "versionCode": int(code.group(1)),
            "debuggable": bool(flags and "DEBUGGABLE" in flags.group(1)),
            "enabled": self.is_enabled(package),
        }

    def is_enabled(self, package: str) -> bool:
        lines = self.shell("pm", "list", "packages", "-e", check=False).split()
        return f"package:{package}" in lines

    def has_instrumentation(self, runner: str) -> bool:
        return runner in self.shell("pm", "list", "instrumentation", check=False)

    # Plugin state -----------------------------------------------------------------------------

    def plugin_state(self) -> dict:
        """Parsed `dumpsys` of the listener service; the token line is dropped even in developer mode."""
        out = self.shell("dumpsys", "activity", "service", PLUGIN_SERVICE, check=False)
        state: dict = {"running": False, "endpoints": [], "pairedClients": []}
        for raw in out.splitlines():
            line = raw.strip()
            if line.startswith("token:"):
                continue
            if line.startswith("state:"):
                state["state"] = line.split(":", 1)[1].strip()
                state["running"] = state["state"] == "running"
            elif line.startswith("endpoint:"):
                state["endpoints"].append(line.split(":", 1)[1].strip())
            elif line.startswith("developerMode:"):
                state["developerMode"] = line.endswith("true")
            elif line.startswith("hostSession:"):
                host = re.search(r"host=(\S+)", line)
                methods = re.search(r"methods=(\d+)", line)
                contract = re.search(r"contract=v(\d+)", line)
                state["host"] = host.group(1) if host else "none"
                if contract:
                    state["contract"] = int(contract.group(1))
                state["grantedMethods"] = int(methods.group(1)) if methods else 0
            elif FINGERPRINT_PATTERN.match(line.split(" ")[0]) and '"' in line:
                fingerprint, rest = line.split(" ", 1)
                state["pairedClients"].append({"fingerprint": fingerprint, "name": rest.split('"')[1]})
        port = next((int(m.group(1)) for e in state["endpoints"] if (m := re.search(r"127\.0\.0\.1:(\d+)/", e))), None)
        state["port"] = port or DEFAULT_PORT
        return state

    def wait_for(self, description: str, condition, timeout: float = 60, interval: float = 0.5):
        until = time.monotonic() + timeout
        while True:
            value = condition()
            if value:
                return value
            if time.monotonic() >= until:
                raise DeviceError(f"timeout: {description}")
            time.sleep(interval)

    # Forwarding --------------------------------------------------------------------------------

    @contextmanager
    def forward(self, remote_port: int, local_port: int = 0) -> Iterator[int]:
        """`adb forward tcp:<local> tcp:<remote>`; local 0 lets adb pick a free port. Removed on exit."""
        out = self.run("forward", f"tcp:{local_port}", f"tcp:{remote_port}").strip()
        port = int(out) if out.isdigit() else local_port
        try:
            yield port
        finally:
            self.run("forward", "--remove", f"tcp:{port}", check=False)

    # Instrumentation helpers -------------------------------------------------------------------

    def _helper(self, method: str, argument: str, value: str) -> str:
        if not self.has_instrumentation(PLUGIN_TEST_RUNNER):
            raise DeviceError("the plugin androidTest APK is not installed (needed for EvidenceHelperTest)")
        out = self.shell("am", "instrument", "-w", "-r", "-e", argument, value, "-e", "class",
                         f"{EVIDENCE_HELPER}#{method}", PLUGIN_TEST_RUNNER, timeout=120)
        # INSTRUMENTATION_CODE is -1 even after a failure, so only the JUnit summary counts.
        if "OK (1 test)" not in out or "FAILURES" in out:
            raise DeviceError(f"EvidenceHelperTest#{method} failed: {scrub(out)[-400:]}")
        return out

    def read_token(self, method: str = "auto") -> tuple[str, str]:
        """Returns (token, source). Sources: environment, instrumentation helper, developer-mode dumpsys."""
        candidates = []
        if method in ("auto", "env") and os.environ.get("AUTOJS6_MCP_TOKEN"):
            candidates.append(("env", lambda: os.environ["AUTOJS6_MCP_TOKEN"]))
        if method in ("auto", "instrumentation") and self.has_instrumentation(PLUGIN_TEST_RUNNER):
            def from_helper() -> str | None:
                out = self._helper("reportToken", "mcpEvidenceToken", "true")
                match = re.search(r"mcpEvidenceToken=(\S+)", out)
                return match.group(1) if match else None
            candidates.append(("instrumentation", from_helper))
        if method in ("auto", "dumpsys"):
            def from_dumpsys() -> str | None:
                out = self.shell("dumpsys", "activity", "service", PLUGIN_SERVICE, check=False)
                match = re.search(r"^\s*token: (\S+)", out, re.M)
                return match.group(1) if match else None
            candidates.append(("dumpsys", from_dumpsys))
        for source, read in candidates:
            try:
                token = read()
            except DeviceError:
                continue
            if token and TOKEN_PATTERN.fullmatch(token):
                return token, source
        raise DeviceError("no token source worked: install the plugin androidTest APK, switch developer mode on, "
                          "or set AUTOJS6_MCP_TOKEN")

    def pre_pair(self, client_name: str) -> str:
        """Records `client_name` as paired over loopback and returns the fingerprint the gate derives."""
        out = self._helper("pairClient", "mcpEvidencePair", client_name)
        match = re.search(r"mcpEvidencePair=([0-9a-f]{24})", out)
        if not match:
            raise DeviceError("EvidenceHelperTest#pairClient reported no fingerprint")
        return match.group(1)

    def wait_listener_with_host(self, timeout: float = 60) -> dict:
        return self.wait_for("listener running with the host attached",
                             lambda: (s := self.plugin_state())["running"] and s.get("host") == "available" and s, timeout=timeout)

    def revoke_pairing(self, fingerprint: str) -> None:
        self._helper("revokePairing", "mcpEvidenceRevoke", fingerprint)

    # Host session ------------------------------------------------------------------------------

    @contextmanager
    def host_session(self, hold_seconds: int, wait_until_closed: bool = True) -> Iterator[dict]:
        """Keeps a real host session open through the host's McpServerPluginRoundTripTest.

        An already running listener with the host attached is reused as it is. Otherwise the host
        instrumentation opens a session on the default port, holds it for `hold_seconds`, then stops
        and closes it; the test also restores the plugin enable state it changed.
        """
        state = self.plugin_state()
        if state["running"] and state.get("host") == "available":
            yield {"source": "existing", **state}
            return
        if state["running"]:
            raise DeviceError("the listener runs without an attached host; stop it before holding a host session")
        if not self.has_instrumentation(HOST_TEST_RUNNER):
            raise DeviceError("the host androidTest APK is not installed (needed to hold a host session)")
        self.shell("setprop", HOST_HOLD_PROPERTY, str(hold_seconds))
        process = subprocess.Popen([self.adb, "-s", self.serial, "shell", "am", "instrument", "-w", "-e", "class",
                                    HOST_ROUND_TRIP, HOST_TEST_RUNNER], stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                   text=True, encoding="utf-8", errors="replace")
        try:
            def attached():
                if process.poll() is not None:
                    raise DeviceError(f"the host round trip ended early: {scrub(process.stdout.read())[-400:]}")
                current = self.plugin_state()
                return current if current["running"] and current.get("host") == "available" else None
            yield {"source": "host-instrumentation", **self.wait_for("host session attached", attached, timeout=90)}
        finally:
            if wait_until_closed:
                progress(f"waiting for the held host session to close (up to {hold_seconds} s)")
                try:
                    process.wait(timeout=hold_seconds + 120)
                except subprocess.TimeoutExpired:
                    process.kill()
            else:
                process.kill()
            self.shell("setprop", HOST_HOLD_PROPERTY, "0", check=False)

    # Package state -----------------------------------------------------------------------------

    def set_package_enabled(self, package: str, enabled: bool) -> None:
        if enabled:
            self.shell("pm", "enable", "--user", "0", package)
        else:
            self.shell("pm", "disable-user", "--user", "0", package)

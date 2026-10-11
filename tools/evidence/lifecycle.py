"""The P6 lifecycle matrix (S1 - S6) on a real device, driven from adb with a person at the phone.

The scenario kills and restarts processes and observes the listener (pid, state, host attachment,
an unauthenticated POST), the pairing notifications and a PC client's tool calls, as
docs/dev/lifecycle-matrix.md describes. Steps that need the phone's screen print a line starting
with `ACTION:` on stderr and wait: turning the drawer switch on and accepting the activation guide
(S4), rotating the token on the settings page (S5) and allowing pairing prompts (S6a / S6b).

Kills: the host with `am force-stop org.autojs.autojs6`; the listener process from inside the
plugin's UID through `McpServerLifecycleTest#killListenerProcess`; the plugin package with
`am force-stop` (S4). The host comes back through its launcher activity.

Needs a listener the drawer switched on, the plugin debug build and its androidTest APK. Tokens
are never printed; the report holds pids, states, statuses, codes, counts and timings. Every
evidence client's pairing is revoked at the end.

    py tools/evidence/lifecycle.py --serial <serial>
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

from device import (HOST_PACKAGE, PLUGIN_PACKAGE, PLUGIN_SERVICE, PLUGIN_TEST_RUNNER, Device, DeviceError,  # noqa: E402
                    progress)
from mcp_client import McpClient, McpError  # noqa: E402

CLIENT = "autojs6-evidence-lifecycle"
CLIENT_S6A = "autojs6-evidence-s6a"
CLIENT_S6B = "autojs6-evidence-s6b"
LISTENER = f"{PLUGIN_PACKAGE}:mcp_server"
KILL_HELPER = f"{PLUGIN_PACKAGE}.McpServerLifecycleTest#killListenerProcess"
REPO = pathlib.Path(__file__).resolve().parents[2]


class Phone:
    def __init__(self, device: Device):
        self.device = device

    def pid(self, name: str) -> str | None:
        return self.device.shell("pidof", name, check=False).strip() or None

    def state(self) -> dict:
        state = self.device.plugin_state()
        return {"listenerPid": self.pid(LISTENER), "hostPid": self.pid(HOST_PACKAGE), "state": state.get("state"),
                "host": state.get("host"), "pairingNotifications": self.pairing_notifications()}

    def pairing_notifications(self) -> int:
        """Active pairing notifications only: `dumpsys notification` also prints the archive and the channels."""
        out = self.device.shell("dumpsys", "notification", check=False)
        active = out.split("Notification List:", 1)[-1].split("mArchive=", 1)[0]
        return sum(1 for line in active.splitlines() if "NotificationRecord(" in line and f"{PLUGIN_PACKAGE}.pairing" in line)

    def await_pairing_notification(self, timeout: float = 5) -> int:
        """The prompt's notification follows its dialog; wait briefly so the count before a kill is meaningful."""
        until = time.monotonic() + timeout
        while (count := self.pairing_notifications()) == 0 and time.monotonic() < until:
            time.sleep(0.2)
        return count

    def fingerprint(self) -> str | None:
        match = re.search(r"tokenFingerprint: (\w+)", self.device.shell("dumpsys", "activity", "service", PLUGIN_SERVICE, check=False))
        return match.group(1) if match else None

    def kill_host(self) -> None:
        self.device.shell("am", "force-stop", HOST_PACKAGE)

    def start_host(self) -> None:
        self.device.shell("monkey", "-p", HOST_PACKAGE, "-c", "android.intent.category.LAUNCHER", "1")

    def kill_listener(self) -> None:
        out = self.device.shell("am", "instrument", "-w", "-r", "-e", "mcpKillListener", "true", "-e", "class", KILL_HELPER,
                                PLUGIN_TEST_RUNNER, timeout=120)
        if "OK (1 test)" not in out:
            raise DeviceError(f"killListenerProcess failed: {out[-300:]}")

    def force_stop_plugin(self) -> None:
        self.device.shell("am", "force-stop", PLUGIN_PACKAGE)

    def wait(self, description: str, condition, timeout: float = 60) -> int:
        started = time.perf_counter()
        self.device.wait_for(description, condition, timeout=timeout, interval=0.1)
        return round((time.perf_counter() - started) * 1000)

    def listening_with_host(self) -> bool:
        state = self.device.plugin_state()
        return state["running"] and state.get("host") == "available"

    def action(self, text: str) -> None:
        print(f"ACTION: {text}", file=sys.stderr, flush=True)


def endpoint_status(url: str) -> str:
    """An unauthenticated POST: 401 means listening."""
    try:
        urllib.request.urlopen(urllib.request.Request(url, b"{}", {"Content-Type": "application/json"}, method="POST"), timeout=5)
        return "200"
    except urllib.error.HTTPError as error:
        return str(error.code)
    except (urllib.error.URLError, ConnectionError, OSError) as error:
        return type(getattr(error, "reason", error)).__name__


def tools(client: McpClient) -> dict:
    out = {}
    for name in ("device_ping", "device_info"):
        try:
            result, elapsed = client.request("tools/call", {"name": name, "arguments": {}})
            text = (result.get("content") or [{}])[0].get("text", "")
            out[name] = "ok" if not result.get("isError") else text.split(":")[0]
        except McpError as error:
            out[name] = f"rpc {error.code}" if error.code is not None else f"http {error.http_status}"
        except OSError as error:
            out[name] = type(error).__name__
    return out


def fresh(url: str, token: str, name: str) -> tuple[McpClient, int]:
    started = time.perf_counter()
    client = McpClient(url, token, name)
    client.initialize()
    return client, round((time.perf_counter() - started) * 1000)


def scenarios(phone: Phone, url: str, token: str, args) -> dict:
    out: dict = {}
    client, _ = fresh(url, token, CLIENT)
    out["setup"] = {"state": phone.state(), "tools": tools(client)}

    if "S1" in args.only:
        progress("S1: host killed")
        before = phone.state()
        phone.kill_host()
        lost = phone.wait("host gone from the listener", lambda: phone.device.plugin_state().get("host") != "available")
        during = {"tools": tools(client), "state": phone.state()}
        phone.start_host()
        back = phone.wait("host reattached", phone.listening_with_host, timeout=90)
        out["S1"] = {"hostUnavailableMs": lost, "during": during, "hostBackMs": back,
                     "sameListenerPid": phone.state()["listenerPid"] == before["listenerPid"], "sameSessionTools": tools(client)}

    if "S2" in args.only:
        progress("S2: listener process killed")
        before = phone.state()
        phone.kill_listener()
        back = phone.wait("a new listener with the host", lambda: phone.pid(LISTENER) not in (None, before["listenerPid"])
                          and phone.listening_with_host(), timeout=90)
        old_session = tools(client)
        client, init_ms = fresh(url, token, CLIENT)
        out["S2"] = {"newListenerWithHostMs": back, "oldSession": old_session, "reinitializeMs": init_ms, "tools": tools(client),
                     "pairingNotifications": phone.pairing_notifications()}

    if "S3" in args.only:
        progress("S3: host and listener killed")
        phone.kill_listener()
        phone.kill_host()
        time.sleep(6)
        down = {"state": phone.state(), "endpoint": endpoint_status(url), "tools": tools(client)}
        phone.start_host()
        back = phone.wait("listener with the host after the host start", phone.listening_with_host, timeout=90)
        client, init_ms = fresh(url, token, CLIENT)
        out["S3"] = {"after6s": down, "backAfterHostStartMs": back, "reinitializeMs": init_ms, "tools": tools(client)}

    if "S4" in args.only:
        progress("S4: plugin force-stopped")
        phone.force_stop_plugin()
        samples = []
        for _ in range(20):
            time.sleep(1)
            samples.append(phone.pid(LISTENER) is not None)
        phone.action("open the AutoJs6 drawer, note the MCP Server subtitle, turn the switch on and accept the activation guide")
        started = time.perf_counter()
        phone.wait("listener with the host after activation", phone.listening_with_host, timeout=600)
        client, init_ms = fresh(url, token, CLIENT)
        out["S4"] = {"listenerAliveSamples": sum(samples), "samples": len(samples),
                     "backAfterActionMs": round((time.perf_counter() - started) * 1000), "reinitializeMs": init_ms, "tools": tools(client)}

    if "S5" in args.only:
        progress("S5: token rotated")
        before = phone.fingerprint()
        phone.action("rotate the token on the plugin settings page (Access token > Rotate token > OK)")
        phone.wait("token fingerprint changed", lambda: (fp := phone.fingerprint()) and fp != before, timeout=600)
        phone.wait("listener with the host", phone.listening_with_host, timeout=60)
        old_token = tools(client)
        new_token, _ = phone.device.read_token(args.token_source)
        phone.device.wait_listener_with_host(timeout=180)
        stale = McpClient(url, new_token, CLIENT)
        stale.session_id = client.session_id
        old_session_new_token = tools(stale)
        client, init_ms = fresh(url, new_token, CLIENT)
        token = new_token
        out["S5"] = {"fingerprintChanged": phone.fingerprint() != before, "oldToken": old_token,
                     "oldSessionNewToken": old_session_new_token, "reinitializeMs": init_ms, "tools": tools(client),
                     "pairingNotifications": phone.pairing_notifications()}

    if "S6" in args.only:
        progress("S6a: pairing pending, then the host is killed")
        pending, _ = fresh(url, token, CLIENT_S6A)
        first = tools(pending)
        notes_before = phone.await_pairing_notification()
        phone.kill_host()
        phone.wait("host gone", lambda: phone.device.plugin_state().get("host") != "available")
        notes_after = phone.pairing_notifications()
        phone.action(f"allow the pairing prompt of {CLIENT_S6A} on the phone")
        phone.wait("s6a paired", lambda: any(c["name"].startswith(CLIENT_S6A) for c in phone.device.plugin_state()["pairedClients"]), timeout=600)
        paired_host_dead = tools(pending)
        phone.start_host()
        phone.wait("host back", phone.listening_with_host, timeout=90)
        out["S6a"] = {"firstCall": first, "notificationsBeforeKill": notes_before, "notificationsAfterKill": notes_after,
                      "afterAllowHostDead": paired_host_dead, "afterHostBack": tools(pending)}

        progress("S6b: pairing pending, then the listener is killed")
        pending, _ = fresh(url, token, CLIENT_S6B)
        first = tools(pending)
        notes_before = phone.await_pairing_notification()
        old_pid = phone.pid(LISTENER)
        phone.kill_listener()
        back = phone.wait("new listener", lambda: phone.pid(LISTENER) not in (None, old_pid) and phone.listening_with_host(), timeout=90)
        time.sleep(1)
        notes_after = phone.pairing_notifications()
        pending, _ = fresh(url, token, CLIENT_S6B)
        again = tools(pending)
        phone.action(f"allow the pairing prompt of {CLIENT_S6B} on the phone")
        phone.wait("s6b paired", lambda: any(c["name"].startswith(CLIENT_S6B) for c in phone.device.plugin_state()["pairedClients"]), timeout=600)
        out["S6b"] = {"firstCall": first, "notificationsBeforeKill": notes_before, "newListenerMs": back,
                      "notificationsAfterNewListener": notes_after, "callAgain": again, "afterAllow": tools(pending)}
    return out


def run(device: Device, args) -> dict:
    report: dict = {
        "scenario": "p9-lifecycle-matrix",
        "startedAt": datetime.datetime.now().astimezone().isoformat(timespec="seconds"),
        "device": device.facts(),
        "packages": {name: device.package(pkg) for name, pkg in (("host", HOST_PACKAGE), ("plugin", PLUGIN_PACKAGE))},
    }
    phone = Phone(device)
    if not phone.listening_with_host():
        raise DeviceError("switch MCP Server on in the AutoJs6 drawer first")
    progress("reading the token and pre-pairing the evidence client")
    token, report["tokenSource"] = device.read_token(args.token_source)
    device.pre_pair(CLIENT)
    try:
        port = device.wait_listener_with_host(timeout=180)["port"]
        with device.forward(port) as local_port:
            report["results"] = scenarios(phone, f"http://127.0.0.1:{local_port}/mcp", token, args)
    finally:
        token = None  # noqa: F841
        if not args.keep_pairing:
            progress("revoking the evidence pairings")
            try:
                device.wait_listener_with_host(timeout=180)
            except DeviceError:
                pass
            for client in device.plugin_state()["pairedClients"]:
                if client["name"].startswith("autojs6-evidence-"):
                    device.revoke_pairing(client["fingerprint"])
    report["finishedAt"] = datetime.datetime.now().astimezone().isoformat(timespec="seconds")
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--serial", required=True)
    parser.add_argument("--only", nargs="*", default=["S1", "S2", "S3", "S4", "S5", "S6"], help="scenarios to run")
    parser.add_argument("--token-source", choices=("auto", "env", "instrumentation", "dumpsys"), default="auto")
    parser.add_argument("--keep-pairing", action="store_true", help="leave the evidence clients paired")
    parser.add_argument("--report-dir", default=str(REPO / "build" / "evidence"))
    args = parser.parse_args()
    report = run(Device(args.serial), args)
    out_dir = pathlib.Path(args.report_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    path = out_dir / f"{datetime.datetime.now():%Y%m%d-%H%M%S}-{args.serial}-lifecycle.json"
    path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2, ensure_ascii=False))
    print(f"report: {path}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())

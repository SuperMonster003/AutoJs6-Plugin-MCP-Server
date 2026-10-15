"""Offline checks of the evidence tools: `py -m unittest discover -s tools/evidence`."""
from __future__ import annotations

import json
import os
import pathlib
import struct
import sys
import unittest
import zlib
from contextlib import nullcontext
from unittest.mock import patch

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

from bridge_reopen import SESSION_ID, call_facts  # noqa: E402
from device import PLUGIN_PACKAGE, PLUGIN_TEST_RUNNER, Device, DeviceError, scrub  # noqa: E402
from docs_resources import content_facts, find_png, index_facts  # noqa: E402
import image_fixtures as fx  # noqa: E402
from mcp_client import PAIRING_REQUIRED, McpError, decode_messages, parse_sse  # noqa: E402
from privileged import uid_name  # noqa: E402
from session import evidence_session  # noqa: E402

TOKEN = "Abc_" + "x" * 39


class FakeDevice(Device):
    def __init__(self, dumpsys: str):
        super().__init__("FAKE")
        self.dumpsys = dumpsys

    def shell(self, *args, timeout=120, check=True):
        return self.dumpsys if args[:2] == ("dumpsys", "activity") else ""


class InstrumentationDevice(Device):
    def __init__(self, flags: str, test_installed: bool = True, app_installed: bool = True):
        super().__init__("FAKE")
        self.flags = flags
        self.test_installed = test_installed
        self.app_installed = app_installed
        self.commands = []

    def shell(self, *args, timeout=120, check=True):
        self.commands.append(args)
        if args == ("dumpsys", "package", PLUGIN_PACKAGE):
            return f"    versionCode=168\n    versionName=1.0.3\n    {self.flags}\n" if self.app_installed else ""
        if args[:3] == ("pm", "list", "packages"):
            return f"package:{PLUGIN_PACKAGE}\n"
        if args == ("pm", "list", "instrumentation"):
            return f"instrumentation:{PLUGIN_TEST_RUNNER} (target={PLUGIN_PACKAGE})" if self.test_installed else ""
        if args[:2] == ("dumpsys", "activity"):
            return f"    token: {TOKEN}\n"
        if args[:2] == ("am", "instrument"):
            return f"INSTRUMENTATION_STATUS: mcpEvidenceToken={TOKEN}\nOK (1 test)\n"
        raise AssertionError(f"unexpected adb command: {args}")

    def instrument_commands(self):
        return [args for args in self.commands if args[:2] == ("am", "instrument")]


class InstrumentationCompatibilityTest(unittest.TestCase):
    def test_package_flags_accept_both_android_formats_and_exact_flag_names(self):
        for flags, expected in [("pkgFlags=[ DEBUGGABLE HAS_CODE ]", True),
                                ("flags=[ HAS_CODE DEBUGGABLE ]", True),
                                ("pkgFlags=[ HAS_CODE ]", False),
                                ("pkgFlags=[ NOT_DEBUGGABLE HAS_CODE ]", False),
                                ("", False)]:
            with self.subTest(flags=flags):
                self.assertEqual(expected, InstrumentationDevice(flags).package(PLUGIN_PACKAGE)["debuggable"])

    def test_release_app_with_leftover_test_apk_is_rejected_before_instrumentation(self):
        device = InstrumentationDevice("pkgFlags=[ HAS_CODE ]")
        for call in [lambda: device.read_token("instrumentation"),
                     lambda: device.pre_pair("evidence-client"),
                     lambda: device.revoke_pairing("0" * 24)]:
            with self.assertRaisesRegex(DeviceError, "debug build and its matching androidTest APK"):
                call()
        self.assertEqual([], device.instrument_commands())

    def test_missing_app_or_test_apk_is_rejected_before_instrumentation(self):
        for installed in [{"app_installed": False}, {"test_installed": False}]:
            with self.subTest(installed=installed):
                device = InstrumentationDevice("pkgFlags=[ DEBUGGABLE HAS_CODE ]", **installed)
                with self.assertRaises(DeviceError):
                    device.read_token("instrumentation")
                self.assertEqual([], device.instrument_commands())

    def test_debug_app_still_reads_token_through_helper(self):
        device = InstrumentationDevice("pkgFlags=[ DEBUGGABLE HAS_CODE ]")
        self.assertEqual((TOKEN, "instrumentation"), device.read_token("instrumentation"))
        self.assertEqual(1, len(device.instrument_commands()))

    @patch.dict(os.environ, {}, clear=True)
    def test_auto_uses_dumpsys_on_release_without_starting_leftover_test_runner(self):
        device = InstrumentationDevice("pkgFlags=[ HAS_CODE ]")
        self.assertEqual((TOKEN, "dumpsys"), device.read_token())
        self.assertEqual([], device.instrument_commands())

    @patch.dict(os.environ, {"AUTOJS6_MCP_TOKEN": TOKEN}, clear=True)
    def test_auto_uses_environment_on_release_without_starting_test_runner(self):
        device = InstrumentationDevice("pkgFlags=[ HAS_CODE ]")
        self.assertEqual((TOKEN, "env"), device.read_token())
        self.assertEqual([], device.instrument_commands())

    @patch.dict(os.environ, {}, clear=True)
    def test_release_session_uses_phone_pairing_even_with_leftover_test_apk(self):
        device = InstrumentationDevice("pkgFlags=[ HAS_CODE ]")
        report = {}
        host = {"source": "existing", "port": 9637, "grantedMethods": 61}
        with patch.object(device, "host_session", return_value=nullcontext(host)), \
                patch.object(device, "forward", return_value=nullcontext(19637)), \
                patch("session.McpClient") as client_type:
            client = client_type.return_value.__enter__.return_value
            client.initialize.return_value = {"protocolVersion": "2025-11-25", "serverInfo": {"version": "1.0.3"}}
            with evidence_session(device, report, "evidence-client") as connected:
                self.assertIs(client, connected)
        self.assertEqual("dumpsys", report["tokenSource"])
        self.assertNotIn("pairing", report)
        self.assertEqual([], device.instrument_commands())


class EvidenceToolTest(unittest.TestCase):
    def test_sse_payloads_skip_comments_and_join_data_lines(self):
        stream = ": keep-alive\n\nid: 1\nretry: 1000\ndata: \n\nevent: message\ndata: {\"jsonrpc\":\"2.0\",\ndata: \"id\":3,\"result\":{}}\n\n" \
                 "data: {\"jsonrpc\":\"2.0\",\"method\":\"notifications/progress\"}\n"
        messages = parse_sse(stream)
        self.assertEqual([3, None], [m.get("id") for m in messages])
        self.assertEqual(messages, decode_messages("text/event-stream", stream.encode()))
        self.assertEqual([{"id": 1}], decode_messages("application/json", b'{"id": 1}'))
        self.assertEqual([], decode_messages("application/json", b""))

    def test_pairing_errors_expose_only_the_fingerprint(self):
        error = McpError(PAIRING_REQUIRED, "PAIRING_REQUIRED: confirm", {"fingerprint": "0123456789abcdef01234567"})
        self.assertEqual("0123456789abcdef01234567", error.pairing_fingerprint)
        self.assertIsNone(McpError(-32602, "bad", {"fingerprint": "x"}).pairing_fingerprint)

    def test_tokens_are_scrubbed_from_adb_output(self):
        out = f"INSTRUMENTATION_STATUS: mcpEvidenceToken={TOKEN}\n    token: {TOKEN}\nother {TOKEN}"
        cleaned = scrub(out, TOKEN)
        self.assertNotIn(TOKEN, cleaned)
        self.assertEqual(3, cleaned.count("<redacted>"))

    def test_plugin_state_drops_the_token_and_reads_sessions_and_clients(self):
        device = FakeDevice("\n".join([
            "SERVICE x/.McpServerService 1 pid=1 user=0",
            "  Client:",
            "    state: running",
            "    endpoint: http://127.0.0.1:9700/mcp",
            "    developerMode: true",
            f"    token: {TOKEN}",
            "    pairedClients: 1",
            '      764b6eef6d1edd433a6d1c53 "Visual Studio Code" loopback firstPairedAt=1 lastSeenAt=1',
            "    hostSession: plugin-1 uid=1 contract=v2 open=true host=available broker=b methods=61 calls=1",
        ]))
        state = device.plugin_state()
        self.assertNotIn(TOKEN, json.dumps(state))
        self.assertTrue(state["running"])
        self.assertEqual(9700, state["port"])
        self.assertEqual("available", state["host"])
        self.assertEqual(61, state["grantedMethods"])
        self.assertEqual(2, state["contract"])
        self.assertEqual([{"fingerprint": "764b6eef6d1edd433a6d1c53", "name": "Visual Studio Code"}], state["pairedClients"])
        self.assertEqual(9637, FakeDevice("hostSession: none").plugin_state()["port"])

    def test_reports_keep_sizes_and_hashes_but_not_content(self):
        facts = content_facts({"contents": [{"uri": "u", "mimeType": "text/html", "text": "<html>secret</html>"}], "_meta": {"truncated": False}})
        self.assertEqual(19, facts["bytes"])
        self.assertNotIn("secret", json.dumps(facts))
        blob = content_facts({"contents": [{"uri": "u", "mimeType": "image/png", "blob": "iVBORw0KGgo="}]})
        self.assertEqual((8, "blob"), (blob["bytes"], blob["encoding"]))
        index = index_facts(json.dumps({"fileCount": 185, "contentVersion": "6.8.0", "entries": [{"path": "a"}, {"path": "b"}]}))
        self.assertEqual({"fileCount": 185, "contentVersion": "6.8.0", "entriesCount": 2}, index)
        self.assertEqual("images/a.png", find_png({"entries": [{"path": "a.html"}, {"path": "images/a.png"}]}))

    def test_bridge_reports_classify_errors_and_hide_session_ids(self):
        ended = {"jsonrpc": "2.0", "id": 3, "error": {"code": -32000, "message": "The phone's MCP session ended (its listener "
                                                                                 "restarted or was switched off) and the bridge could not open a new one: x"}}
        facts = call_facts(ended, [], 12)
        self.assertEqual((False, -32000, True, False), (facts["ok"], facts["errorCode"], facts["errorNamesEndedSession"],
                                                        facts["errorBlamesEndpointPath"]))
        old = {"jsonrpc": "2.0", "id": 3, "error": {"code": -32000, "message": "404 Not Found from http://127.0.0.1:1/mcp: "
                                                                               "the endpoint path is wrong; the plugin serves /mcp."}}
        self.assertTrue(call_facts(old, [], 1)["errorBlamesEndpointPath"])
        ok = call_facts({"jsonrpc": "2.0", "id": 4, "result": {"content": []}},
                        [{"jsonrpc": "2.0", "method": "notifications/tools/list_changed"}], 5)
        self.assertEqual((True, ["notifications/tools/list_changed"]), (ok["ok"], ok["notifications"]))
        line = "the phone no longer knew session 6c1f0e7a-93b2-4d0e (its listener restarted); opened session 0b9d2c44-77aa-41f3"
        self.assertEqual("the phone no longer knew session <id> (its listener restarted); opened session <id>",
                         SESSION_ID.sub(r"\1<id>", line))
        self.assertEqual("The phone's MCP session ended", SESSION_ID.sub(r"\1<id>", "The phone's MCP session ended"))

    def test_privileged_reports_keep_only_the_uid_name(self):
        self.assertEqual("0(root)", uid_name("uid=0(root) gid=0(root) groups=0(root) context=u:r:magisk:s0\n"))
        self.assertEqual("10670(u0_a670)", uid_name("uid=10670(u0_a670) gid=10670(u0_a670) groups=3003(inet)"))
        self.assertIsNone(uid_name("permission denied"))
        self.assertIsNone(uid_name(None))


class ImageFixtureTest(unittest.TestCase):
    def test_png_round_trip_keeps_every_pixel(self):
        canvas = fx.Canvas(7, 5)
        canvas.fill(2, 1, 5, 4, fx.RED)
        canvas.fill(0, 4, 7, 5, fx.BLUE)
        width, height, rows = fx.decode_png(canvas.png())
        self.assertEqual((7, 5), (width, height))
        self.assertEqual(fx.RED, fx.pixel(rows, 2, 1))
        self.assertEqual(fx.WHITE, fx.pixel(rows, 5, 1))
        self.assertEqual(fx.BLUE, fx.pixel(rows, 6, 4))
        self.assertEqual("#FF0000", fx.hex_color(fx.pixel(rows, 4, 3)))

    def test_decoder_undoes_every_filter_type_of_an_rgba_png(self):
        # Rows of 2 RGBA pixels, each row stored with a different filter (0 none, 1 sub, 2 up, 3 average, 4 Paeth).
        source = [bytes([10 * y + x for x in range(8)]) for y in range(5)]

        def paeth(a, b, c):
            p = a + b - c
            pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
            return a if pa <= pb and pa <= pc else b if pb <= pc else c

        raw, previous = b"", bytes(8)
        for kind, row in enumerate(source):
            out = bytearray()
            for i, value in enumerate(row):
                left = row[i - 4] if i >= 4 else 0
                up = previous[i]
                up_left = previous[i - 4] if i >= 4 else 0
                predictor = [0, left, up, (left + up) // 2, paeth(left, up, up_left)][kind]
                out.append((value - predictor) & 0xFF)
            raw += bytes([kind]) + bytes(out)
            previous = row
        png = fx.encode_png(2, 5, b"")
        header = bytearray(png[:33])
        header[25] = 6  # color type RGBA
        header[29:33] = struct.pack(">I", zlib.crc32(bytes(header[12:29])) & 0xFFFFFFFF)
        idat = zlib.compress(raw)
        png = bytes(header) + struct.pack(">I", len(idat)) + b"IDAT" + idat + struct.pack(">I", zlib.crc32(b"IDAT" + idat) & 0xFFFFFFFF) + png[-12:]
        width, height, rows = fx.decode_png(png)
        self.assertEqual((2, 5), (width, height))
        for y, row in enumerate(source):
            self.assertEqual((row[0], row[1], row[2]), fx.pixel(rows, 0, y))
            self.assertEqual((row[4], row[5], row[6]), fx.pixel(rows, 1, y))

    def test_ean13_symbol_follows_the_module_tables(self):
        self.assertEqual(1, fx.ean13_check_digit("400638133393"))
        modules = fx.ean13_modules()
        self.assertEqual(95, len(modules))
        self.assertTrue(modules.startswith("101") and modules.endswith("101") and modules[45:50] == "01010")
        # First digit 4 selects the parity LGLLGG; digit 0 in L is 0001101, digit 0 in G is 0100111.
        self.assertEqual("0001101", modules[3:10])
        self.assertEqual("0100111", modules[10:17])
        with self.assertRaises(ValueError):
            fx.ean13_modules("4006381333932")
        self.assertEqual((95 + 22) * 4, fx.ean13(4, 10).width)

    def test_fixture_facts_point_at_the_drawn_content(self):
        files = fx.fixtures()
        _w, _h, rows = fx.decode_png(files["colors.png"]["png"])
        self.assertEqual(fx.RED, fx.pixel(rows, *files["colors.png"]["red"]))
        self.assertEqual(fx.BLUE, fx.pixel(rows, 620, 100))
        self.assertEqual((450, 150), files["ean13.png"]["center"])
        self.assertEqual((2468, 500), files["ean13-5000.png"]["center"])
        self.assertEqual((330, 140), files["scene.png"]["center"])
        self.assertTrue(all(len(f["png"]) < 48 * 1024 for f in files.values()), {k: len(f["png"]) for k, f in files.items()})


if __name__ == "__main__":
    unittest.main()

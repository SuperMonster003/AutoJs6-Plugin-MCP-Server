"""Offline checks of the evidence tools: `py -m unittest discover -s tools/evidence`."""
from __future__ import annotations

import json
import pathlib
import sys
import unittest

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

from device import Device, scrub  # noqa: E402
from docs_resources import content_facts, find_png, index_facts  # noqa: E402
from mcp_client import PAIRING_REQUIRED, McpError, decode_messages, parse_sse  # noqa: E402

TOKEN = "Abc_" + "x" * 39


class FakeDevice(Device):
    def __init__(self, dumpsys: str):
        super().__init__("FAKE")
        self.dumpsys = dumpsys

    def shell(self, *args, timeout=120, check=True):
        return self.dumpsys if args[:2] == ("dumpsys", "activity") else ""


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
            "    hostSession: plugin-1 uid=1 open=true host=available broker=b methods=61 calls=1",
        ]))
        state = device.plugin_state()
        self.assertNotIn(TOKEN, json.dumps(state))
        self.assertTrue(state["running"])
        self.assertEqual(9700, state["port"])
        self.assertEqual("available", state["host"])
        self.assertEqual(61, state["grantedMethods"])
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


if __name__ == "__main__":
    unittest.main()

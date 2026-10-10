"""Minimal Streamable HTTP MCP client for device evidence (Python standard library only).

It speaks the stateful protocol the plugin serves (roadmap D9): `initialize`, then
`notifications/initialized`, requests answered either as JSON or as an SSE stream, the
`Mcp-Session-Id` header and `DELETE` on close. The bearer token stays in memory: it is never
logged, `repr()` hides it and error messages never include request headers.
"""
from __future__ import annotations

import json
import time
import urllib.error
import urllib.request
from typing import Any

PROTOCOL_VERSION = "2025-11-25"
PAIRING_REQUIRED = -32002
PAIRING_DENIED = -32003
RATE_LIMITED = -32004
INVALID_PARAMS = -32602


class McpError(Exception):
    """A JSON-RPC error answer, or an HTTP failure without one (code None)."""

    def __init__(self, code: int | None, message: str, data: Any = None, http_status: int | None = None):
        super().__init__(f"{code}: {message}" if code is not None else message)
        self.code = code
        self.message = message
        self.data = data
        self.http_status = http_status

    @property
    def pairing_fingerprint(self) -> str | None:
        if self.code == PAIRING_REQUIRED and isinstance(self.data, dict):
            return self.data.get("fingerprint")
        return None


def parse_sse(text: str) -> list[dict]:
    """JSON payloads of the `data:` fields of an event stream; comments and empty events are skipped."""
    messages, data = [], []
    for line in text.splitlines() + [""]:
        if line == "":
            if data:
                messages.append(json.loads("\n".join(data)))
                data = []
        elif line.startswith(":"):
            continue
        elif line.startswith("data:"):
            data.append(line[5:].removeprefix(" "))
    return messages


def decode_messages(content_type: str, body: bytes) -> list[dict]:
    if not body:
        return []
    text = body.decode("utf-8")
    if "text/event-stream" in (content_type or ""):
        return parse_sse(text)
    value = json.loads(text)
    return value if isinstance(value, list) else [value]


class McpClient:
    def __init__(self, url: str, token: str, client_name: str = "autojs6-evidence", client_version: str = "1.0",
                 timeout: float = 60.0):
        self.url = url
        self._token = token
        self.client_name = client_name
        self.client_version = client_version
        self.timeout = timeout
        self.session_id: str | None = None
        self.protocol_version: str | None = None
        self.server_info: dict | None = None
        self._next_id = 1

    def __repr__(self) -> str:
        return f"McpClient(url={self.url!r}, client={self.client_name!r}, session={self.session_id!r})"

    def __enter__(self) -> "McpClient":
        return self

    def __exit__(self, *_exc) -> None:
        self.close()

    def _headers(self) -> dict[str, str]:
        headers = {
            "Content-Type": "application/json",
            "Accept": "application/json, text/event-stream",
            "Authorization": f"Bearer {self._token}",
        }
        if self.session_id:
            headers["Mcp-Session-Id"] = self.session_id
        if self.protocol_version:
            headers["MCP-Protocol-Version"] = self.protocol_version
        return headers

    def _send(self, payload: dict | None, method: str = "POST", timeout: float | None = None) -> tuple[int, dict, bytes]:
        data = json.dumps(payload).encode("utf-8") if payload is not None else None
        request = urllib.request.Request(self.url, data=data, method=method, headers=self._headers())
        try:
            with urllib.request.urlopen(request, timeout=timeout or self.timeout) as response:
                return response.status, dict(response.headers), response.read()
        except urllib.error.HTTPError as error:
            return error.code, dict(error.headers or {}), error.read()

    def request(self, method: str, params: dict | None = None, timeout: float | None = None) -> tuple[Any, float]:
        """Sends one request and returns (result, elapsed milliseconds); JSON-RPC errors raise McpError."""
        request_id = self._next_id
        self._next_id += 1
        payload = {"jsonrpc": "2.0", "id": request_id, "method": method}
        if params is not None:
            payload["params"] = params
        started = time.perf_counter()
        status, headers, body = self._send(payload, timeout=timeout)
        elapsed = (time.perf_counter() - started) * 1000
        content_type = next((v for k, v in headers.items() if k.lower() == "content-type"), "")
        session = next((v for k, v in headers.items() if k.lower() == "mcp-session-id"), None)
        if session and not self.session_id:
            self.session_id = session
        try:
            messages = decode_messages(content_type, body)
        except (ValueError, UnicodeDecodeError):
            raise McpError(None, f"HTTP {status} with an undecodable body ({len(body)} bytes)", http_status=status)
        answer = next((m for m in messages if m.get("id") == request_id), None)
        if answer is None:
            answer = next((m for m in messages if "error" in m and m.get("id") is None), None)
        if answer is None:
            raise McpError(None, f"HTTP {status} without an answer to request {request_id}", http_status=status)
        if "error" in answer:
            error = answer["error"]
            raise McpError(error.get("code"), error.get("message", ""), error.get("data"), http_status=status)
        return answer.get("result"), elapsed

    def notify(self, method: str, params: dict | None = None) -> int:
        payload = {"jsonrpc": "2.0", "method": method}
        if params is not None:
            payload["params"] = params
        status, _headers, _body = self._send(payload)
        return status

    def initialize(self) -> dict:
        result, _elapsed = self.request("initialize", {
            "protocolVersion": PROTOCOL_VERSION,
            "capabilities": {},
            "clientInfo": {"name": self.client_name, "version": self.client_version},
        })
        self.protocol_version = result.get("protocolVersion")
        self.server_info = result.get("serverInfo")
        self.notify("notifications/initialized")
        return result

    def list_all(self, method: str, key: str, timeout: float | None = None) -> tuple[list, list[float], list]:
        """Follows nextCursor; returns (items, elapsed per page, _meta per page)."""
        items, elapsed, metas, cursor = [], [], [], None
        while True:
            result, ms = self.request(method, {"cursor": cursor} if cursor else {}, timeout=timeout)
            items.extend(result.get(key, []))
            elapsed.append(ms)
            metas.append(result.get("_meta"))
            cursor = result.get("nextCursor")
            if not cursor:
                return items, elapsed, metas

    def close(self) -> None:
        if not self.session_id:
            return
        try:
            self._send(None, method="DELETE", timeout=10)
        except OSError:
            pass
        self.session_id = None

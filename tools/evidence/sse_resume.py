"""SSE resumption on a real device (roadmap P9, SEP-1699).

Three cases on one MCP session with protocol 2025-11-25:

- mid-run: a `script_run` of about six seconds sends progress every two seconds; after the priming
  event and the first progress notification the POST connection is dropped, and three seconds
  later a GET with `Last-Event-ID` resumes the stream and has to deliver the missed progress and
  the final result;
- after the end: the POST of a three-second script is dropped right after its priming event and
  resumed only after the script finished; the GET replays the kept events and ends;
- an unknown `Last-Event-ID` is refused with `400`.

The report holds ids, counts, timings and statuses only, not the scripts' console output.

    py tools/evidence/sse_resume.py --serial <serial>
"""
from __future__ import annotations

import argparse
import datetime
import json
import pathlib
import sys
import time

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

from device import HOST_PACKAGE, PLUGIN_PACKAGE, Device, progress  # noqa: E402
from mcp_client import EventStream, McpClient  # noqa: E402
from session import evidence_session  # noqa: E402

CLIENT_NAME = "autojs6-evidence-sse"
REPO = pathlib.Path(__file__).resolve().parents[2]
LONG_SCRIPT = "for (let i = 0; i < 6; i++) { console.log('step ' + i); sleep(1000); }"
SHORT_SCRIPT = "for (let i = 0; i < 3; i++) { console.log('step ' + i); sleep(1000); }"


def script_call(client: McpClient, source: str, token: str) -> EventStream:
    return client.stream("tools/call", {
        "name": "script_run",
        "arguments": {"source": source, "timeoutMs": 30000},
        "_meta": {"progressToken": token},
    })


def outcome(event) -> dict:
    result = (event.message or {}).get("result") or {}
    structured = result.get("structuredContent") or {}
    return {"isError": bool(result.get("isError")), "status": structured.get("status"), "durationMs": structured.get("durationMs")}


def stream_facts(events: list, after: int) -> dict:
    ids = [int(e.id) for e in events if e.id is not None]
    return {
        "events": len(events),
        "progress": sum(1 for e in events if (e.message or {}).get("method") == "notifications/progress"),
        "allIdsAfterResumePoint": all(i > after for i in ids),
        "idsIncreaseWithoutRepeats": ids == sorted(set(ids)),
    }


def mid_run(client: McpClient) -> dict:
    started = time.perf_counter()
    stream = script_call(client, LONG_SCRIPT, "mid-run")
    priming = stream.next_event()
    facts: dict = {"postStatus": stream.status, "primingId": priming.id, "retryMs": priming.retry,
                   "primingCarriesMessage": priming.message is not None}
    first = stream.next_event()
    while first is not None and (first.message or {}).get("method") != "notifications/progress":
        first = stream.next_event()
    last_id = int(first.id)
    facts["droppedAfterProgressId"] = last_id
    facts["droppedAtMs"] = round((time.perf_counter() - started) * 1000)
    stream.close()
    time.sleep(3)
    resumed = client.resume(str(last_id), request_id=stream.request_id)
    facts["resumeStatus"] = resumed.status
    facts["resumedAtMs"] = round((time.perf_counter() - started) * 1000)
    answer = resumed.until_response()
    facts["answeredAtMs"] = round((time.perf_counter() - started) * 1000)
    facts["resumedStream"] = stream_facts(resumed.events, last_id)
    facts["result"] = outcome(answer) if answer else None
    facts["getEndedAfterResult"] = resumed.next_event() is None
    resumed.close()
    return facts


def after_end(client: McpClient) -> dict:
    started = time.perf_counter()
    stream = script_call(client, SHORT_SCRIPT, "after-end")
    priming = stream.next_event()
    stream.close()
    time.sleep(6)
    resumed = client.resume(priming.id, request_id=stream.request_id)
    answer = resumed.until_response()
    facts = {
        "primingId": priming.id,
        "resumeStatus": resumed.status,
        "resumedAtMs": round((time.perf_counter() - started) * 1000),
        "replayed": stream_facts(resumed.events, int(priming.id)),
        "result": outcome(answer) if answer else None,
        "getEndedAfterReplay": resumed.next_event() is None,
    }
    resumed.close()
    return facts


def unknown_id(client: McpClient) -> dict:
    resumed = client.resume("999999999")
    body = resumed.read_body()
    resumed.close()
    try:
        message = json.loads(body.decode("utf-8")).get("error", {}).get("message", "")
    except ValueError:
        message = ""
    return {"status": resumed.status, "contentType": resumed.content_type, "message": message}


def run(device: Device, args) -> dict:
    report: dict = {
        "scenario": "p9-sse-resumption",
        "startedAt": datetime.datetime.now().astimezone().isoformat(timespec="seconds"),
        "device": device.facts(),
        "packages": {name: device.package(pkg) for name, pkg in (("host", HOST_PACKAGE), ("plugin", PLUGIN_PACKAGE))},
    }
    with evidence_session(device, report, CLIENT_NAME, args.hold_seconds, args.token_source, args.keep_pairing) as client:
        progress("mid-run resume")
        report["midRun"] = mid_run(client)
        progress("resume after the end")
        report["afterEnd"] = after_end(client)
        progress("unknown Last-Event-ID")
        report["unknownId"] = unknown_id(client)
    report["finishedAt"] = datetime.datetime.now().astimezone().isoformat(timespec="seconds")
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--serial", required=True)
    parser.add_argument("--hold-seconds", type=int, default=60, help="host session hold when no session is running")
    parser.add_argument("--token-source", choices=("auto", "env", "instrumentation", "dumpsys"), default="auto")
    parser.add_argument("--keep-pairing", action="store_true", help="leave the evidence client paired")
    parser.add_argument("--report-dir", default=str(REPO / "build" / "evidence"))
    args = parser.parse_args()
    report = run(Device(args.serial), args)
    out_dir = pathlib.Path(args.report_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    path = out_dir / f"{datetime.datetime.now():%Y%m%d-%H%M%S}-{args.serial}-sse-resume.json"
    path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2, ensure_ascii=False))
    print(f"report: {path}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())

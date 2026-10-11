"""The UI tools on a third-party app (roadmap P3.2 / P9): dump -> click -> set_text -> scroll -> back.

The app is the system Settings, opened with `android.settings.SETTINGS`: `ui_find` of the search
entry on its main page (by its text, description or id, then the innermost clickable node containing
it, since search bars often draw their hint in a non-clickable child), `ui_dump` to get that node's
`#n` reference, `ui_click` on it by reference (the host relocates it in its snapshot, roadmap P10),
the same reference once more after the screen changed (`NODE_REF_STALE` expected), `ui_wait_for`
an editable node,
`ui_set_text` of a one-letter query (many results), `ui_scroll` of the results (or, when they do
not scroll, of the main list after one back), then `ui_press_key back` until Settings is left. No setting is changed and nothing is submitted.

The AutoJs6 accessibility service must be running and the screen unlocked. A listener the drawer
owns is used as it is; a session held by the host instrumentation restarts the host process and
with it its accessibility service, so the scenario refuses to hold one. The report holds tool
names, statuses, error codes, timings, counts and window package names, never node text.

    py tools/evidence/ui_chain.py --serial <serial>
"""
from __future__ import annotations

import argparse
import datetime
import json
import pathlib
import re
import sys
import time

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

from device import HOST_PACKAGE, PLUGIN_PACKAGE, Device, DeviceError, progress  # noqa: E402
from mcp_client import McpClient  # noqa: E402
from session import evidence_session  # noqa: E402

CLIENT_NAME = "autojs6-evidence-ui"
REPO = pathlib.Path(__file__).resolve().parents[2]
QUERY = "a"
SEARCH_SELECTORS = [
    {"textContains": "搜索"},
    {"descContains": "搜索"},
    {"textContains": "Search"},
    {"descContains": "Search"},
    {"idMatches": ".*search.*"},
]
EDITABLE = {"className": "android.widget.EditText"}
ROW = re.compile(r"^#n(\d+) +(.*) (?:\[(-?\d+),(-?\d+)]\[(-?\d+),(-?\d+)]|c=\((-?\d+),(-?\d+)\))$")


def call(client: McpClient, name: str, arguments: dict | None = None, timeout: float = 60, text: list | None = None) -> tuple[dict, dict]:
    started = time.perf_counter()
    result, _ = client.request("tools/call", {"name": name, "arguments": arguments or {}}, timeout=timeout)
    structured = result.get("structuredContent") or {}
    step = {"tool": name, "ok": not result.get("isError"), "elapsedMs": round((time.perf_counter() - started) * 1000)}
    if result.get("isError"):
        step["code"] = ((structured.get("error") or {}).get("code")
                        or (result.get("content") or [{}])[0].get("text", "").split(":")[0])
    if text is not None:
        text.extend(item.get("text", "") for item in result.get("content") or [] if item.get("type") == "text")
    return step, structured


def ref_for(dump_text: str, bounds: dict) -> str | None:
    """The `#n` reference of the dump row at [bounds]: a container by its bounds, a clickable leaf by its centre."""
    edges = [bounds.get(key) for key in ("left", "top", "right", "bottom")]
    centre = ((edges[0] + edges[2]) // 2, (edges[1] + edges[3]) // 2)
    for line in dump_text.splitlines():
        match = ROW.match(line.rstrip())
        if not match:
            continue
        if match.group(3) is not None:
            if [int(match.group(i)) for i in (3, 4, 5, 6)] == edges:
                return f"#n{match.group(1)}"
        elif (int(match.group(7)), int(match.group(8))) == centre and "clickable" in match.group(2).split():
            return f"#n{match.group(1)}"
    return None


def window_package(client: McpClient) -> str | None:
    _, structured = call(client, "ui_current_window")
    window = structured.get("window") if isinstance(structured.get("window"), dict) else structured
    return window.get("packageName") or window.get("package")


def run_chain(client: McpClient, settings_package: str) -> dict:
    facts: dict = {"steps": []}
    steps = facts["steps"]
    facts["windowBefore"] = window_package(client)

    entry = None
    for selector in SEARCH_SELECTORS:
        step, found = call(client, "ui_find", {"selector": selector, "limit": 3})
        step["selector"] = list(selector)[0]
        step["count"] = found.get("count")
        steps.append(step)
        nodes = found.get("nodes") or []
        if nodes:
            entry = nodes[0]
            break
    if entry is None:
        raise DeviceError("no search entry found in Settings")
    target = entry
    if not entry.get("clickable"):
        # Pre-order lists ancestors first, so the last clickable node containing the entry is the innermost one.
        step, around = call(client, "ui_find", {"selector": {"clickable": True, "boundsContains": entry.get("bounds")}, "limit": 50})
        step["selector"] = "clickable+boundsContains"
        step["count"] = around.get("count")
        steps.append(step)
        containing = around.get("nodes") or []
        if not containing:
            raise DeviceError("the search entry has no clickable ancestor")
        target = containing[-1]

    dump_text: list = []
    step, dump = call(client, "ui_dump", {"maxNodes": 400}, text=dump_text)
    step["nodeCount"] = dump.get("nodeCount")
    step["snapshot"] = bool(dump.get("snapshotId"))
    steps.append(step)
    ref = ref_for("\n".join(dump_text), target.get("bounds") or {})
    facts["refFound"] = ref is not None
    if ref is None:
        raise DeviceError("the search entry is not a row of the dump")

    step, clicked = call(client, "ui_click", {"nodeRef": ref})
    step["via"] = clicked.get("via")
    steps.append(step)
    time.sleep(1.5)

    # The search page replaced the window the snapshot was taken in, so the host refuses the old reference.
    step, _ = call(client, "ui_click", {"nodeRef": ref})
    step["purpose"] = "stale reference after the screen changed"
    steps.append(step)
    facts["staleAfterChange"] = step.get("code") == "NODE_REF_STALE"

    step, waited = call(client, "ui_wait_for", {"selector": EDITABLE, "state": "appear", "timeoutMs": 8000}, timeout=30)
    step["satisfied"] = waited.get("satisfied")
    steps.append(step)

    step, typed = call(client, "ui_set_text", {"selector": EDITABLE, "text": QUERY})
    step["via"] = typed.get("via")
    step["textMatches"] = typed.get("text") == QUERY
    steps.append(step)
    time.sleep(1.5)

    step, ready = call(client, "ui_wait_for", {"selector": {"scrollable": True}, "state": "appear", "timeoutMs": 5000}, timeout=30)
    step["selector"] = "scrollable"
    steps.append(step)
    facts["scrollTarget"] = "search results"
    if not step["ok"]:
        back, _ = call(client, "ui_press_key", {"key": "back"})
        steps.append(back)
        time.sleep(1)
        facts["scrollTarget"] = "main list"
    step, scrolled = call(client, "ui_scroll", {"direction": "down", "times": 2})
    step["performed"] = scrolled.get("performed")
    step["atEnd"] = scrolled.get("atEnd")
    steps.append(step)

    backs = 0
    while backs < 5:
        step, _ = call(client, "ui_press_key", {"key": "back"})
        steps.append(step)
        backs += 1
        time.sleep(1)
        if window_package(client) != settings_package:
            break
    facts["backPresses"] = backs
    facts["windowAfter"] = window_package(client)
    facts["leftSettings"] = facts["windowAfter"] != settings_package
    return facts


def run(device: Device, args) -> dict:
    report: dict = {
        "scenario": "p9-ui-chain-third-party",
        "startedAt": datetime.datetime.now().astimezone().isoformat(timespec="seconds"),
        "device": device.facts(),
        "packages": {name: device.package(pkg) for name, pkg in (("host", HOST_PACKAGE), ("plugin", PLUGIN_PACKAGE))},
    }
    state = device.plugin_state()
    if not (state["running"] and state.get("host") == "available"):
        raise DeviceError("switch MCP Server on in the AutoJs6 drawer first: holding a host session would restart AutoJs6 and stop its accessibility service")
    device.shell("am", "start", "-a", "android.settings.SETTINGS")
    time.sleep(2.5)
    settings_package = device.shell("dumpsys", "window").split("mCurrentFocus=", 1)[-1].split("/", 1)[0].split()[-1]
    report["app"] = settings_package
    with evidence_session(device, report, CLIENT_NAME, token_source=args.token_source, keep_pairing=args.keep_pairing) as client:
        _, info = call(client, "device_info")
        report["accessibility"] = info.get("accessibility")
        if not (info.get("accessibility") or {}).get("serviceRunning"):
            raise DeviceError("the AutoJs6 accessibility service is not running")
        if window_package(client) != settings_package:
            # The helpers restarted the plugin; bring Settings to the front again.
            device.shell("am", "start", "-a", "android.settings.SETTINGS")
            time.sleep(2.5)
        report["chain"] = run_chain(client, settings_package)
    report["finishedAt"] = datetime.datetime.now().astimezone().isoformat(timespec="seconds")
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--serial", required=True)
    parser.add_argument("--token-source", choices=("auto", "env", "instrumentation", "dumpsys"), default="auto")
    parser.add_argument("--keep-pairing", action="store_true", help="leave the evidence client paired")
    parser.add_argument("--report-dir", default=str(REPO / "build" / "evidence"))
    args = parser.parse_args()
    report = run(Device(args.serial), args)
    out_dir = pathlib.Path(args.report_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    path = out_dir / f"{datetime.datetime.now():%Y%m%d-%H%M%S}-{args.serial}-ui-chain.json"
    path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2, ensure_ascii=False))
    print(f"report: {path}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())

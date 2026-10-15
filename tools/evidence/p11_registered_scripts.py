"""The registered-script tools on a real host (roadmap P11 D42): script_catalog and script_registered_run.

The scenario writes five small registered scripts into `mcp-evidence-p11-agent/` of the working directory
through `files_write` (an echo script, a second one with the same derived id in a subdirectory, a
sensitive one, one that throws and one that runs past its timeout), then checks: the catalog finds them
with their risk and confirm values; an id shared by two scripts is refused with both paths; a run by
path validates the arguments, fills defaults and returns the value the script reported with
`ai.agent.result`; extra arguments are refused; a sensitive script runs without a phone prompt (the
client confirms, decided 2026-10-15); a throwing script ends with status error and a slow one with
status timeout. The directory is removed afterwards.

With `--expect-hidden` it checks an AutoJs6 build whose MCP grant predates these methods instead: both
tools stay out of tools/list and a direct call answers CAPABILITY_DENIED naming the missing method.

Needs AutoJs6 with `agent.listScripts` / `readManifest` / `execRegistered` in the MCP grant (host commit
972e5dba6 or later) and the plugin debug build with its androidTest APK.

    py tools/evidence/p11_registered_scripts.py --serial <serial> [--expect-hidden]
"""
from __future__ import annotations

import argparse
import datetime
import json
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

from device import HOST_PACKAGE, PLUGIN_PACKAGE, Device, progress  # noqa: E402
from session import evidence_session  # noqa: E402
from tool_calls import call, remove_dir  # noqa: E402

CLIENT_NAME = "autojs6-evidence-p11-registered"
REPO = pathlib.Path(__file__).resolve().parents[2]
DIR = "mcp-evidence-p11-agent"
TOOLS = ("script_catalog", "script_registered_run")


def header(description: str, *tags: str) -> str:
    lines = ["/**", " * @agent", f" * @description MCP evidence: {description}"] + [f" * {tag}" for tag in tags] + [" * @tag mcp-evidence", " */"]
    return "\n".join(lines) + "\n"


SCRIPTS = {
    "echo.js": header("echo the word back", "@param {string} word Word to echo", "@param {integer} [times=2] Repetitions",
                      "@param {string=up|down} [letters=up] Letter case", "@result {object} { text: string, times: integer }",
                      "@risk readonly", "@timeout 20000") +
    "var p = ai.agent.context().parameters;\n"
    "var w = p.letters === 'up' ? p.word.toUpperCase() : p.word.toLowerCase();\n"
    "var text = new Array(p.times + 1).join(w);\n"
    "console.log('echo ' + text);\n"
    "ai.agent.result({ text: text, times: p.times });\n",
    "dup/echo.js": header("a second script whose derived id is also echo") + "ai.agent.result('dup');\n",
    "sensitive.js": header("a sensitive script that only reports", "@risk sensitive", "@timeout 20000") + "ai.agent.result({ ran: true });\n",
    "boom.js": header("a script that throws", "@timeout 20000") + "throw new Error('mcp evidence boom');\n",
    "slow.js": header("a script that outlives its timeout", "@timeout 4000") + "while (true) { sleep(200); }\n",
}


def run(device: Device, args) -> dict:
    report: dict = {
        "scenario": "p11-registered-scripts",
        "startedAt": datetime.datetime.now().astimezone().isoformat(timespec="seconds"),
        "device": device.facts(),
        "packages": {name: device.package(pkg) for name, pkg in (("host", HOST_PACKAGE), ("plugin", PLUGIN_PACKAGE))},
        "checks": [],
    }

    def record(name: str, passed: bool, **facts):
        report["checks"].append({"check": name, "pass": bool(passed), **facts})
        progress(f"{'PASS' if passed else 'FAIL'} {name}")

    def code(structured: dict) -> str | None:
        return (structured.get("error") or {}).get("code")

    with evidence_session(device, report, CLIENT_NAME, hold_seconds=args.hold_seconds, token_source=args.token_source) as client:
        names = {tool["name"] for tool in client.list_all("tools/list", "tools")[0]}
        report["listed"] = {name: name in names for name in TOOLS}
        if args.expect_hidden:
            record("an older host keeps the registered-script tools out of tools/list", not any(report["listed"].values()))
            ok, s, _t, ms = call(client, "script_catalog", {})
            message = (s.get("error") or {}).get("message", "")
            record("a direct call names the missing grant", not ok and code(s) == "CAPABILITY_DENIED" and "agent.listScripts" in message,
                   code=code(s), message=message, ms=ms)
            return report
        if not all(report["listed"].values()):
            record("registered-script tools listed", False, hint="the host grant lacks the agent methods (older AutoJs6)")
            return report
        try:
            for name, source in SCRIPTS.items():
                ok, s, _t, _ms = call(client, "files_write", {"path": f"{DIR}/{name}", "content": source})
                if not ok:
                    record(f"write {name}", False, code=code(s), message=(s.get("error") or {}).get("message"))
                    return report
            ok, s, _t, ms = call(client, "script_catalog", {"query": "MCP evidence"})
            scripts = {pathlib.PurePosixPath(x.get("path", "")).name + ("@dup" if "/dup/" in x.get("path", "") else ""): x for x in s.get("scripts") or []}
            report["catalog"] = {key: {"id": x.get("id"), "risk": x.get("risk"), "confirm": x.get("confirm"), "timeoutMs": x.get("timeoutMs")} for key, x in scripts.items()}
            record("the catalog finds the five scripts", ok and s.get("count") == 5, count=s.get("count"), ms=ms)
            sensitive = scripts.get("sensitive.js") or {}
            record("sensitive scripts are normalized to confirm before-run", sensitive.get("risk") == "sensitive" and sensitive.get("confirm") == "before-run")
            echo = scripts.get("echo.js") or {}
            record("the echo script lists its parameters with defaults", echo.get("risk") == "readonly"
                   and ((echo.get("parameters") or {}).get("properties") or {}).get("times", {}).get("default") == 2)

            ok, s, _t, ms = call(client, "script_registered_run", {"id": "echo", "arguments": {"word": "x"}})
            message = (s.get("error") or {}).get("message", "")
            record("an id two scripts share is refused with both paths", not ok and code(s) == "INVALID_ARGUMENTS" and "2 scripts register the id echo" in message, ms=ms)

            ok, s, _t, ms = call(client, "script_registered_run", {"path": echo.get("path", f"{DIR}/echo.js"), "arguments": {"word": "Ab", "letters": "down"}})
            result = s.get("result") or {}
            record("a run by path fills defaults and returns the reported result", ok and s.get("status") == "finished"
                   and result.get("text") == "abab" and result.get("times") == 2, status=s.get("status"), console=len(s.get("console") or []), ms=ms)
            ok, s, _t, ms = call(client, "script_registered_run", {"path": echo.get("path", f"{DIR}/echo.js"), "arguments": {"word": "x", "extra": 1}})
            record("extra arguments are refused", not ok and code(s) == "INVALID_ARGUMENTS", code=code(s), ms=ms)
            ok, s, _t, ms = call(client, "script_registered_run", {"id": "sensitive"})
            record("a sensitive script runs without a phone prompt", ok and s.get("status") == "finished" and (s.get("result") or {}).get("ran") is True
                   and s.get("confirm") == "before-run", ms=ms)
            ok, s, _t, ms = call(client, "script_registered_run", {"id": "boom"})
            record("a throwing script ends with status error", ok and s.get("status") == "error" and (s.get("error") or {}).get("code") == "SCRIPT_FAILED", ms=ms)
            ok, s, _t, ms = call(client, "script_registered_run", {"id": "slow"}, timeout=60)
            record("a script past its registered timeout is stopped", ok and s.get("status") == "timeout", status=s.get("status"), ms=ms)
        finally:
            report["fixturesRemoved"] = remove_dir(client, DIR)
    report["passed"] = sum(1 for c in report["checks"] if c["pass"])
    report["failed"] = sum(1 for c in report["checks"] if not c["pass"])
    report["finishedAt"] = datetime.datetime.now().astimezone().isoformat(timespec="seconds")
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--serial", required=True)
    parser.add_argument("--token-source", choices=("auto", "env", "instrumentation", "dumpsys"), default="auto")
    parser.add_argument("--hold-seconds", type=int, default=120)
    parser.add_argument("--expect-hidden", action="store_true", help="check an AutoJs6 build without the agent methods in its MCP grant")
    parser.add_argument("--report-dir", default=str(REPO / "build" / "evidence"))
    args = parser.parse_args()
    report = run(Device(args.serial), args)
    report.setdefault("passed", sum(1 for c in report["checks"] if c["pass"]))
    report.setdefault("failed", sum(1 for c in report["checks"] if not c["pass"]))
    out_dir = pathlib.Path(args.report_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    suffix = "-hidden" if args.expect_hidden else ""
    path = out_dir / f"{datetime.datetime.now():%Y%m%d-%H%M%S}-{args.serial}-p11-registered-scripts{suffix}.json"
    path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    for check in report["checks"]:
        print(f"{'PASS' if check['pass'] else 'FAIL'}  {check['check']}")
    print(f"passed {report['passed']}, failed {report['failed']}; report: {path}")
    return 0 if report["failed"] == 0 else 1


if __name__ == "__main__":
    sys.exit(main())

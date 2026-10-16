"""The self-described plugin tools against a real AutoJs6 host (roadmap P12, MCP integration).

With 3-Hanzi CC 2.1.0 or later installed (it declares `opencc_convert`), `tools/list` must show the tool with its
read-only annotation and its 32 configs, calls must convert through `plugin_tools.invoke` (the default config, a Taiwan
phrase config, a chained config), the host must refuse an unknown config and an oversized text with
INVALID_ARGUMENTS, and `device_info` must report the plugin in its `pluginTools` section. With `--expect-hidden`
(a host without `plugin_tools`, for example build 5352) the tool must not be listed and `device_info` must carry no
`pluginTools` section, while the built-in tools keep working.

The report keeps pass / fail per check, counts and timings; never the token or tool output beyond the short
conversions it checks.

    py tools/evidence/p12_plugin_tools.py --serial <serial> [--expect-hidden]
"""
from __future__ import annotations

import argparse
import datetime
import json
import pathlib
import io
import sys
import zipfile
import time

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

from device import HOST_PACKAGE, PLUGIN_PACKAGE, Device, progress  # noqa: E402
from session import evidence_session  # noqa: E402
from tool_calls import call, remove_dir, write_file  # noqa: E402
from image_fixtures import encode_png  # noqa: E402

CLIENT_NAME = "autojs6-evidence-p12-plugin-tools"
REPO = pathlib.Path(__file__).resolve().parents[2]
OPENCC_PACKAGE = "io.github.supermonster003.autojs6.plugin.three.hanzi.cc"
TOOL = "opencc_convert"
FAKE_PACKAGE = "org.autojs.plugin.tool.fake"
YOLO_PACKAGE = "io.github.supermonster003.autojs6.plugin.yolo.ncnn"
YOLO_MODEL = "models/yolo11n"
IMAGEQUANT_PACKAGE = "io.github.supermonster003.autojs6.plugin.imagequant"
IMAGEQUANT_GROUP = "imagequant"
IMAGEQUANT_DIR = "p12-imagequant-evidence"
ARCHIVE_PACKAGE = "io.github.supermonster003.autojs6.plugin.three.stack.archive"
ARCHIVE_GROUP = "archive"
ARCHIVE_DIR = "p12-archive-evidence"
FAKE_GROUP = "faketool"
FIXTURE_DIR = "mcp-evidence-p12"


def run(device: Device, args) -> dict:
    report: dict = {
        "scenario": "p12-plugin-tools",
        "startedAt": datetime.datetime.now().astimezone().isoformat(timespec="seconds"),
        "device": device.facts(),
        "packages": {name: device.package(pkg) for name, pkg in (("host", HOST_PACKAGE), ("plugin", PLUGIN_PACKAGE), ("opencc", OPENCC_PACKAGE))},
        "expectHidden": args.expect_hidden,
        "checks": [],
    }
    checks = report["checks"]

    def record(name: str, passed: bool, ms: int = 0, **facts):
        checks.append({"check": name, "pass": bool(passed), "ms": ms, **facts})
        progress(f"{'PASS' if passed else 'FAIL'} {name}")

    with evidence_session(device, report, CLIENT_NAME, hold_seconds=args.hold_seconds, token_source=args.token_source, keep_pairing=args.keep_pairing) as client:
        listed, _elapsed, _metas = client.list_all("tools/list", "tools")
        by_name = {tool["name"]: tool for tool in listed}
        report["toolCount"] = len(by_name)
        ok, s, _text, ms = call(client, "device_info", {})
        section = s.get("pluginTools") if ok else None
        if args.expect_hidden:
            record("plugin tool hidden on an older host", TOOL not in by_name, toolCount=len(by_name))
            record("device_info without pluginTools", ok and section is None, ms)
            ok, s, _text, ms = call(client, "script_list", {})
            record("built-in tools keep working", ok, ms)
            return finish(report)

        tool = by_name.get(TOOL)
        configs = ((tool or {}).get("inputSchema", {}).get("properties", {}).get("config", {}).get("enum") or [])
        record("opencc_convert listed", tool is not None and (tool.get("annotations") or {}).get("readOnlyHint") is True and len(configs) == 32,
               configs=len(configs), readOnly=(tool or {}).get("annotations", {}).get("readOnlyHint"))
        if tool is None:
            return finish(report)

        ok, s, text, ms = call(client, TOOL, {"text": "汉字"})
        record("default config s2t", ok and text == "漢字" and s.get("config") == "s2t", ms, config=s.get("config"))
        ok, s, text, ms = call(client, TOOL, {"text": "鼠标和打印机", "config": "s2twi"})
        record("Taiwan phrases with s2twi", ok and text == "滑鼠和印表機", ms, matches=text == "滑鼠和印表機")
        ok, s, text, ms = call(client, TOOL, {"text": "汉字", "config": "s2jp"})
        record("chained config s2jp", ok and bool(text) and s.get("config") == "s2jp", ms, chars=len(text or ""))
        ok, s, text, ms = call(client, TOOL, {"text": "汉字", "config": "S2T"})
        record("unknown config refused by the host", not ok and code_of(s, text) == "INVALID_ARGUMENTS", ms, code=code_of(s, text))
        ok, s, text, ms = call(client, TOOL, {"text": "字" * 16385})
        record("oversized text refused", not ok and code_of(s, text) in ("INVALID_ARGUMENTS", "LIMIT_EXCEEDED"), ms, code=code_of(s, text))

        plugins = {p.get("shortName"): p for p in ((section or {}).get("plugins") or [])}
        opencc = plugins.get("opencc") or {}
        record("device_info pluginTools reports opencc", opencc.get("status") == "available" and opencc.get("trust") == "official"
               and any(t.get("name") == TOOL and t.get("listed") for t in opencc.get("tools") or []), 0,
               providers=len(plugins), status=opencc.get("status"))
    if args.yolo:
        yolo_checks(device, args, report, record)
    if args.imagequant:
        imagequant_checks(device, args, report, record)
    if args.archive:
        archive_checks(device, args, report, record)
    if args.fake:
        fake_checks(device, args, report, record)
    return finish(report)


def yolo_checks(device: Device, args, report: dict, record) -> None:
    """The YOLO pilot (roadmap P12, D49): a model directory in the working directory, pushed beforehand with
    model.json, model.ncnn.param, model.ncnn.bin and bus.jpg (no model ships with the plugin)."""
    report["packages"]["yolo"] = device.package(YOLO_PACKAGE)
    with evidence_session(device, report, CLIENT_NAME, hold_seconds=args.hold_seconds, token_source=args.token_source, keep_pairing=args.keep_pairing) as client:
        listed, _elapsed, _metas = client.list_all("tools/list", "tools")
        tool = next((t for t in listed if t["name"] == "yolo_detect"), None)
        properties = (tool or {}).get("inputSchema", {}).get("properties", {})
        annotations = (tool or {}).get("annotations") or {}
        record("yolo_detect listed with image and model directory", tool is not None and annotations.get("readOnlyHint") is True
               and annotations.get("openWorldHint") is True and "directory path" in str(properties.get("model", {}).get("description", "")), 0)
        if tool is None:
            return
        arguments = {"image": f"{YOLO_MODEL}/bus.jpg", "model": YOLO_MODEL}
        ok, s, text, ms = call(client, "yolo_detect", arguments, timeout=180)
        labels = [d.get("label") for d in s.get("detections") or []]
        model = s.get("model") or {}
        record("detects with a model directory", ok and "bus" in labels and "person" in labels and (s.get("image") or {}).get("width") == 810, ms,
               count=s.get("count"), loadMs=model.get("loadMs"), cached=model.get("cached"), inferenceMs=s.get("inferenceMs"), code=code_of(s, text) if not ok else None)
        ok, s, text, ms = call(client, "yolo_detect", {**arguments, "labels": ["bus"], "confidence": 0.5}, timeout=180)
        model = s.get("model") or {}
        record("the second call reuses the loaded model", ok and model.get("cached") is True and s.get("count") == 1, ms,
               loadMs=model.get("loadMs"), inferenceMs=s.get("inferenceMs"))
        ok, s, text, ms = call(client, "yolo_detect", {"image": f"{YOLO_MODEL}/bus.jpg", "model": "models/nothing-here"})
        record("a missing model directory is refused", not ok and code_of(s, text) == "INVALID_ARGUMENTS", ms, code=code_of(s, text))


def imagequant_checks(device: Device, args, report: dict, record) -> None:
    """The image quantization pilot (roadmap P12, D51 / D52): an official tool with the write risk, so it is listed only
    once the writes switch of its group is on; the PNG comes back as an output at an automatic path or at outputPaths,
    with an image preview."""
    report["packages"]["imagequant"] = device.package(IMAGEQUANT_PACKAGE)
    with evidence_session(device, report, CLIENT_NAME, hold_seconds=args.hold_seconds, token_source=args.token_source, keep_pairing=args.keep_pairing) as client:
        listed, _elapsed, _metas = client.list_all("tools/list", "tools")
        names = {tool["name"] for tool in listed}
        ok, s, _text, ms = call(client, "device_info", {})
        plugin = next((p for p in ((s.get("pluginTools") or {}).get("plugins") or []) if p.get("shortName") == IMAGEQUANT_GROUP), {})
        record("official write tool hidden while the writes switch is off", plugin.get("status") == "available" and plugin.get("trust") == "official"
               and "imagequant_quantize" not in names, ms, status=plugin.get("status"), trust=plugin.get("trust"))
        ok, s, text, ms = call(client, "imagequant_quantize", {"image": f"{IMAGEQUANT_DIR}/gradient.png"})
        record("a hidden write tool answers TOOL_DISABLED", not ok and code_of(s, text) == "TOOL_DISABLED", ms, code=code_of(s, text))
    previous = device.set_plugin_group(IMAGEQUANT_GROUP, "true", "true")
    report["imagequantGroupBefore"] = previous
    try:
        with evidence_session(device, report, CLIENT_NAME, hold_seconds=args.hold_seconds, token_source=args.token_source, keep_pairing=args.keep_pairing) as client:
            listed, _elapsed, _metas = client.list_all("tools/list", "tools")
            tool = next((t for t in listed if t["name"] == "imagequant_quantize"), None)
            annotations = (tool or {}).get("annotations") or {}
            properties = (tool or {}).get("inputSchema", {}).get("properties", {})
            record("listed with the writes switch on, with outputPaths", tool is not None and annotations.get("readOnlyHint") is False
                   and annotations.get("destructiveHint") is False and annotations.get("openWorldHint") is True and "outputPaths" in properties, 0)
            if tool is None:
                return
            try:
                rows = b"".join(b"\x00" + b"".join(bytes((x * 255 // 160, y * 255 // 120, (x + y) * 255 // 280)) for x in range(160)) for y in range(120))
                write_file(client, f"{IMAGEQUANT_DIR}/gradient.png", encode_png(160, 120, rows))
                started = time.perf_counter()
                result, _ = client.request("tools/call", {"name": "imagequant_quantize", "arguments": {"image": f"{IMAGEQUANT_DIR}/gradient.png"}}, timeout=120)
                ms = round((time.perf_counter() - started) * 1000)
                s = result.get("structuredContent") or {}
                out = (s.get("outputs") or [{}])[0]
                kinds = [item.get("type") for item in result.get("content", [])]
                record("quantizes to an automatic path with a preview", not result.get("isError") and str(out.get("path", "")).startswith("plugin-outputs/imagequant/quantize-")
                       and str(out.get("path", "")).endswith(".png") and (out.get("bytes") or 0) > 0 and "image" in kinds, ms,
                       path=out.get("path"), bytes=out.get("bytes"), content=kinds)
                ok, s, text, ms = call(client, "imagequant_quantize", {"image": f"{IMAGEQUANT_DIR}/gradient.png", "maxColors": 16, "maxQuality": 80,
                                                                       "outputPaths": {"png": f"{IMAGEQUANT_DIR}/out.png"}})
                out = (s.get("outputs") or [{}])[0]
                result_fields = s.get("result") or s
                record("writes to outputPaths with the requested options", ok and out.get("path") == f"{IMAGEQUANT_DIR}/out.png" and (out.get("bytes") or 0) > 0, ms,
                       path=out.get("path"), bytes=out.get("bytes"), quality=result_fields.get("quality"))
                ok, s, text, ms = call(client, "imagequant_quantize", {"image": f"{IMAGEQUANT_DIR}/gradient.png", "minQuality": 100, "maxColors": 2})
                record("the pngquant quality floor fails the call", not ok and "QUALITY_TOO_LOW" in text, ms, code=code_of(s, text))
                ok, s, text, ms = call(client, "imagequant_quantize", {"image": f"{IMAGEQUANT_DIR}/gradient.png", "maxColors": 1})
                record("an out-of-range option is refused", not ok and code_of(s, text) == "INVALID_ARGUMENTS", ms, code=code_of(s, text))
            finally:
                report["fixtureRemoved"] = remove_dir(client, IMAGEQUANT_DIR)
                report["outputsRemoved"] = remove_dir(client, "plugin-outputs/imagequant")
    finally:
        device.set_plugin_group(IMAGEQUANT_GROUP, *previous)
        report["imagequantGroupRestored"] = True


def sample_zip() -> bytes:
    """readme.txt, docs/a.txt and docs/deep/b.txt, built in memory."""
    buffer = io.BytesIO()
    with zipfile.ZipFile(buffer, "w", zipfile.ZIP_DEFLATED) as archive:
        archive.writestr("readme.txt", "Read me first.")
        archive.writestr("docs/a.txt", "alpha")
        archive.writestr("docs/deep/b.txt", "beta")
    return buffer.getvalue()


def archive_checks(device: Device, args, report: dict, record) -> None:
    """The archive pilot (roadmap P12, D53 - D56): list and read are official read-only tools, listed at once; extract
    writes a directory output, so it is listed only once the writes switch of its group is on. An existing directory is
    merged and its files are kept."""
    report["packages"]["archive"] = device.package(ARCHIVE_PACKAGE)
    with evidence_session(device, report, CLIENT_NAME, hold_seconds=args.hold_seconds, token_source=args.token_source, keep_pairing=args.keep_pairing) as client:
        listed, _elapsed, _metas = client.list_all("tools/list", "tools")
        names = {tool["name"] for tool in listed}
        record("list and read listed, extract hidden while the writes switch is off",
               {"archive_list", "archive_read"} <= names and "archive_extract" not in names, 0, tools=sorted(n for n in names if n.startswith("archive_")))
        try:
            write_file(client, f"{ARCHIVE_DIR}/sample.zip", sample_zip())
            ok, s, text, ms = call(client, "archive_list", {"archive": f"{ARCHIVE_DIR}/sample.zip"})
            paths = [e.get("path") for e in s.get("entries") or []]
            record("lists the entries", ok and s.get("total") == 3 and "docs/deep/b.txt" in paths and s.get("format") == "zip", ms, total=s.get("total"))
            ok, s, text, ms = call(client, "archive_read", {"archive": f"{ARCHIVE_DIR}/sample.zip", "path": "docs/a.txt"})
            record("reads a file inside the archive", ok and s.get("text") == "alpha", ms)
            ok, s, text, ms = call(client, "archive_read", {"archive": f"{ARCHIVE_DIR}/sample.zip", "path": "missing.txt"})
            record("a missing entry is refused", not ok and code_of(s, text) == "INVALID_ARGUMENTS", ms, code=code_of(s, text))
            ok, s, text, ms = call(client, "archive_extract", {"archive": f"{ARCHIVE_DIR}/sample.zip"})
            record("a hidden write tool answers TOOL_DISABLED", not ok and code_of(s, text) == "TOOL_DISABLED", ms, code=code_of(s, text))
        finally:
            pass
    previous = device.set_plugin_group(ARCHIVE_GROUP, "true", "true")
    report["archiveGroupBefore"] = previous
    try:
        with evidence_session(device, report, CLIENT_NAME, hold_seconds=args.hold_seconds, token_source=args.token_source, keep_pairing=args.keep_pairing) as client:
            listed, _elapsed, _metas = client.list_all("tools/list", "tools")
            tool = next((t for t in listed if t["name"] == "archive_extract"), None)
            properties = (tool or {}).get("inputSchema", {}).get("properties", {})
            record("extract listed with the writes switch on, with outputPaths", tool is not None and "outputPaths" in properties
                   and "directory" in str(properties.get("outputPaths", {}).get("description", "")), 0)
            try:
                if tool is None:
                    return
                ok, s, text, ms = call(client, "archive_extract", {"archive": f"{ARCHIVE_DIR}/sample.zip"}, timeout=180)
                out = (s.get("outputs") or [{}])[0]
                record("extracts to an automatic directory", ok and str(out.get("path", "")).startswith("plugin-outputs/archive/extract-")
                       and out.get("kind") == "directory" and out.get("files") == 3, ms, path=out.get("path"), files=out.get("files"))
                write_file(client, f"{ARCHIVE_DIR}/out/readme.txt", b"mine")
                ok, s, text, ms = call(client, "archive_extract", {"archive": f"{ARCHIVE_DIR}/sample.zip", "outputPaths": {"files": f"{ARCHIVE_DIR}/out"}}, timeout=180)
                out = (s.get("outputs") or [{}])[0]
                result = s.get("result") or {}
                record("merges into an existing directory and keeps its files", ok and out.get("files") == 2 and result.get("kept") == 1
                       and result.get("keptPaths") == ["readme.txt"], ms, files=out.get("files"), kept=result.get("kept"))
            finally:
                report["fixtureRemoved"] = remove_dir(client, ARCHIVE_DIR)
                report["outputsRemoved"] = remove_dir(client, "plugin-outputs/archive")
    finally:
        device.set_plugin_group(ARCHIVE_GROUP, *previous)
        report["archiveGroupRestored"] = True


def fake_checks(device: Device, args, report: dict, record) -> None:
    """The user-granted provider (roadmap P12 D34 / D46 / D47): hidden by default, listed once its group and its writes
    switch are on, then files in and outputs out. The provider must be authorized in the AutoJs6 plugin center first."""
    report["packages"]["fake"] = device.package(FAKE_PACKAGE)
    with evidence_session(device, report, CLIENT_NAME, hold_seconds=args.hold_seconds, token_source=args.token_source, keep_pairing=args.keep_pairing) as client:
        listed, _elapsed, _metas = client.list_all("tools/list", "tools")
        names = {tool["name"] for tool in listed}
        ok, s, _text, ms = call(client, "device_info", {})
        fake = next((p for p in ((s.get("pluginTools") or {}).get("plugins") or []) if p.get("shortName") == FAKE_GROUP), {})
        record("user-granted provider reported and authorized", fake.get("status") == "available" and fake.get("trust") == "user_granted", ms,
               status=fake.get("status"), trust=fake.get("trust"), detail=fake.get("detail"))
        record("user-granted tools hidden by default", not any(n.startswith(FAKE_GROUP + "_") for n in names) and fake.get("toolGroupEnabled") is False,
               0, hint=bool(fake.get("hint")))
        ok, s, text, ms = call(client, "faketool_echo", {"text": "hidden"})
        record("a hidden user-granted tool answers TOOL_DISABLED", not ok and code_of(s, text) == "TOOL_DISABLED", ms, code=code_of(s, text))
    previous = device.set_plugin_group(FAKE_GROUP, "true", "true")
    report["fakeGroupBefore"] = previous
    try:
        with evidence_session(device, report, CLIENT_NAME, hold_seconds=args.hold_seconds, token_source=args.token_source, keep_pairing=args.keep_pairing) as client:
            listed, _elapsed, _metas = client.list_all("tools/list", "tools")
            names = sorted(n for n in (tool["name"] for tool in listed) if n.startswith(FAKE_GROUP + "_"))
            record("user-granted tools listed once switched on", {"faketool_echo", "faketool_emit", "faketool_inspect"} <= set(names), 0, tools=len(names))
            ok, s, text, ms = call(client, "faketool_echo", {"text": "from MCP"})
            record("user-granted tool runs", ok and text == "from MCP", ms)
            ok, s, text, ms = call(client, "faketool_emit", {"text": "written by MCP"})
            out = (s.get("outputs") or [{}])[0]
            record("output written to an automatic path", ok and str(out.get("path", "")).startswith("plugin-outputs/faketool/emit-") and out.get("bytes") == 14, ms,
                   path=out.get("path"), bytes=out.get("bytes"))
            ok, s, text, ms = call(client, "faketool_emit", {"text": "again", "outputPaths": {"data": f"{FIXTURE_DIR}/emit.txt"}})
            out = (s.get("outputs") or [{}])[0]
            record("output written to outputPaths", ok and out.get("path") == f"{FIXTURE_DIR}/emit.txt" and out.get("bytes") == 5, ms, path=out.get("path"))
            ok, s, text, ms = call(client, "faketool_emit", {"text": "third", "outputPaths": {"data": f"{FIXTURE_DIR}/emit.txt"}})
            record("an existing output is not replaced", not ok, ms, code=code_of(s, text))
            try:
                write_file(client, f"{FIXTURE_DIR}/input.txt", b"seven b")
                write_file(client, f"{FIXTURE_DIR}/pic.png", encode_png(16, 8, b"".join(b"\x00" + bytes((200, 30, 30)) * 16 for _ in range(8))))
                ok, s, text, ms = call(client, "faketool_inspect", {"image": f"{FIXTURE_DIR}/pic.png", "file": f"{FIXTURE_DIR}/input.txt"})
                inputs = {i.get("name"): i for i in (s.get("inputs") or [])}
                record("image and file paths reach the provider as descriptors", ok and inputs.get("file", {}).get("bytes") == 7
                       and inputs.get("image", {}).get("mimeType") == "image/png" and (inputs.get("image", {}).get("bytes") or 0) > 0, ms,
                       fileBytes=inputs.get("file", {}).get("bytes"), imageBytes=inputs.get("image", {}).get("bytes"))
            finally:
                report["fixtureRemoved"] = remove_dir(client, FIXTURE_DIR)
                report["outputsRemoved"] = remove_dir(client, "plugin-outputs/faketool")
    finally:
        device.set_plugin_group(FAKE_GROUP, *previous)
        report["fakeGroupRestored"] = True


def code_of(structured: dict, text: str) -> str | None:
    """The error code of a failed call; a tool with an output schema carries it in the text only (`CODE: message`)."""
    return (structured.get("error") or {}).get("code") or (text.split(":", 1)[0] if ":" in text else None)


def finish(report: dict) -> dict:
    report["passed"] = sum(1 for c in report["checks"] if c["pass"])
    report["failed"] = sum(1 for c in report["checks"] if not c["pass"])
    report["finishedAt"] = datetime.datetime.now().astimezone().isoformat(timespec="seconds")
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--serial", required=True)
    parser.add_argument("--token-source", choices=("auto", "env", "instrumentation", "dumpsys"), default="auto")
    parser.add_argument("--keep-pairing", action="store_true", help="leave the evidence client paired")
    parser.add_argument("--hold-seconds", type=int, default=180)
    parser.add_argument("--expect-hidden", action="store_true", help="an older host: the plugin tools must stay hidden")
    parser.add_argument("--yolo", action="store_true",
                        help=f"also check yolo_detect with the model pushed to <working directory>/{YOLO_MODEL}")
    parser.add_argument("--imagequant", action="store_true",
                        help="also check imagequant_quantize of the Image Quantization plugin 1.1.0 (its writes switch is turned on and restored)")
    parser.add_argument("--archive", action="store_true",
                        help="also check archive_list / archive_read / archive_extract of 3-Stack Archive 3.1.0 (its writes switch is turned on and restored)")
    parser.add_argument("--fake", action="store_true",
                        help="also check the user-granted test provider (org.autojs.plugin.tool.fake, authorized in the AutoJs6 plugin center)")
    parser.add_argument("--report-dir", default=str(REPO / "build" / "evidence"))
    args = parser.parse_args()
    report = run(Device(args.serial), args)
    out_dir = pathlib.Path(args.report_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    path = out_dir / f"{datetime.datetime.now():%Y%m%d-%H%M%S}-{args.serial}-p12-plugin-tools.json"
    path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    for check in report["checks"]:
        print(f"{'PASS' if check['pass'] else 'FAIL'}  {check['check']}")
    print(f"passed {report['passed']}, failed {report['failed']}; report: {path}")
    return 0 if report["failed"] == 0 else 1


if __name__ == "__main__":
    sys.exit(main())

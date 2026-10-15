"""The P11 barcode and OpenCV tools against a real AutoJs6 host (roadmap P11 D23 / D24 / D38 / D45).

File checks: the scenario writes test images into the AutoJs6 working directory through
`script_run` (the host owns the files, so no storage permission is involved), then runs
`barcode_detect`, `opencv_find_color`, `opencv_find_multi_colors` and `opencv_find_image` on
them, at normal size and at 5000 px, which AutoJs6 decodes at half size for model sessions; a
large image with a full-size template must be refused.

Screen checks: the system Settings app is opened, `ui_find` picks a text label, `screen_capture`
returns that label as a full-resolution PNG, which becomes the template of `opencv_find_image
image=screen`; its pixels feed `opencv_find_color` and `opencv_find_multi_colors` limited to
the label. `barcode_detect image=screen` must run without error. With `--show-barcode-with
<package>` the EAN-13 image is also opened full screen in that viewer and must be decoded from the
screen.

Needs AutoJs6 with the P11 host changes (6.8.0 from build 5352 with `app.pluginCapabilities` in the
MCP grant), the ML Kit Barcode and OpenCV plugins, and the plugin debug build with its androidTest
APK. The file checks also need "All files access" for AutoJs6 (Android 11+ refuses it its own
working directory otherwise; the scenario then skips them). The held host session restarts AutoJs6,
which leaves its accessibility service crashed; the scenario rebinds it by switching the setting
off and on again before the screen checks.

The report keeps pass / fail per check, coordinates, scores, scales and timings; never the token,
image data or recognized text beyond the fixture's own barcode value.

    py tools/evidence/p11_image_tools.py --serial <serial> [--show-barcode-with com.miui.gallery]
"""
from __future__ import annotations

import argparse
import base64
import datetime
import json
import pathlib
import sys
import time

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

import image_fixtures as fx  # noqa: E402
from device import HOST_PACKAGE, PLUGIN_PACKAGE, Device, DeviceError, progress  # noqa: E402
from session import evidence_session  # noqa: E402

CLIENT_NAME = "autojs6-evidence-p11-image"
REPO = pathlib.Path(__file__).resolve().parents[2]
FIXTURE_DIR = "mcp-evidence-p11"
TOOLS = ("barcode_detect", "opencv_find_image", "opencv_find_color", "opencv_find_multi_colors")
SHARED_BARCODE = "/sdcard/Pictures/autojs6-mcp-evidence-ean13.png"
# Raw bytes per script_run call: the base64 text plus the script stays below the broker's 32 KiB argument limit.
WRITE_CHUNK = 18 * 1024
# adb joins the shell arguments with spaces, so the SQL quotes need a shell quote around them.
SHARED_BARCODE_WHERE = "\"_display_name='autojs6-mcp-evidence-ean13.png'\""


def call(client, name: str, arguments: dict, timeout: float = 120) -> tuple[bool, dict, str, int]:
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
    # The host writes the file, so it owns it. The broker caps engines.execScript arguments at 32 KiB, so the
    # base64 text goes in chunks: the first call creates the file, the others append.
    for offset in range(0, max(len(data), 1), WRITE_CHUNK):
        encoded = base64.b64encode(data[offset:offset + WRITE_CHUNK]).decode("ascii")
        script(client, f'var p = files.path("{relative}"); files.createWithDirs(p); '
                       f'var out = new java.io.FileOutputStream(p, {"true" if offset else "false"}); '
                       f'try {{ out.write(android.util.Base64.decode("{encoded}", 0)); }} finally {{ out.close(); }}')


def near(actual, expected, tolerance: int) -> bool:
    return actual is not None and all(abs(a - e) <= tolerance for a, e in zip(actual, expected))


def center_of(bounds: dict | None):
    return None if not bounds else ((bounds["left"] + bounds["right"]) // 2, (bounds["top"] + bounds["bottom"]) // 2)


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


def file_checks(client, files: dict) -> list[dict]:
    checks = []

    def record(name: str, passed: bool, ms: int, **facts):
        checks.append({"check": name, "pass": bool(passed), "ms": ms, **facts})
        progress(f"{'PASS' if passed else 'FAIL'} {name}")

    for name, tolerance in (("ean13.png", 24), ("ean13-5000.png", 48)):
        ok, s, _t, ms = call(client, "barcode_detect", {"image": f"{FIXTURE_DIR}/{name}"})
        item = (s.get("items") or [{}])[0]
        center = center_of(item.get("bounds"))
        record(f"barcode file {name}", ok and item.get("format") == "EAN_13" and item.get("rawValue") == fx.EAN13_VALUE and near(center, files[name]["center"], tolerance),
               ms, count=s.get("count"), format=item.get("format"), valueMatches=item.get("rawValue") == fx.EAN13_VALUE,
               center=center, expected=files[name]["center"], scale=s.get("scale"), error=(s.get("error") or {}).get("code"))

    for name in ("colors.png", "colors-5000.png"):
        ok, s, _t, ms = call(client, "opencv_find_color", {"image": f"{FIXTURE_DIR}/{name}", "color": "#FF0000"})
        point = s.get("point") and (s["point"]["x"], s["point"]["y"])
        record(f"find color file {name}", ok and s.get("found") and near(point, files[name]["red"], 2), ms,
               point=point, expected=files[name]["red"], scale=s.get("scale"), error=(s.get("error") or {}).get("code"))

    ok, s, _t, ms = call(client, "opencv_find_multi_colors", {"image": f"{FIXTURE_DIR}/colors.png", "color": "#FF0000",
                                                             "points": [{"x": 20, "y": 0, "color": "#0000FF"}, {"x": 19, "y": 19, "color": "#FF0000"}]})
    point = s.get("point") and (s["point"]["x"], s["point"]["y"])
    record("find multi colors file colors.png", ok and s.get("found") and point == files["colors.png"]["red"], ms, point=point,
           error=(s.get("error") or {}).get("code"))
    ok, s, _t, ms = call(client, "opencv_find_multi_colors", {"image": f"{FIXTURE_DIR}/colors.png", "color": "#FF0000",
                                                             "points": [{"x": 20, "y": 0, "color": "#00FF00"}]})
    record("find multi colors file, wrong neighbor", ok and s.get("found") is False, ms, found=s.get("found"))

    ok, s, _t, ms = call(client, "opencv_find_image", {"image": f"{FIXTURE_DIR}/scene.png", "template": f"{FIXTURE_DIR}/pattern.png", "maxResults": 3})
    best = (s.get("matches") or [{}])[0]
    center = best.get("center") and (best["center"]["x"], best["center"]["y"])
    record("find image file scene.png", ok and near(center, files["scene.png"]["center"], 1) and (best.get("score") or 0) >= 0.95, ms,
           count=s.get("count"), center=center, expected=files["scene.png"]["center"], score=best.get("score"))
    ok, s, _t, ms = call(client, "opencv_find_image", {"image": f"{FIXTURE_DIR}/scene-5000.png", "template": f"{FIXTURE_DIR}/pattern.png"})
    error = s.get("error") or {}
    record("find image file at two scales is refused", not ok and error.get("code") == "INVALID_ARGUMENTS" and "one scale" in error.get("message", ""), ms,
           code=error.get("code"))
    return checks


def screen_checks(device: Device, client, args, writable: bool) -> list[dict]:
    checks = []

    def record(name: str, passed: bool, ms: int, **facts):
        checks.append({"check": name, "pass": bool(passed), "ms": ms, **facts})
        progress(f"{'PASS' if passed else 'FAIL'} {name}")

    device.shell("am", "start", "-a", "android.settings.SETTINGS")
    time.sleep(2.5)
    ok, s, _t, _ms = call(client, "ui_find", {"selector": {"className": "TextView"}, "limit": 30})
    nodes = [n for n in (s.get("nodes") or []) if n.get("bounds")]
    label = next((n["bounds"] for n in nodes
                  if 80 <= n["bounds"]["right"] - n["bounds"]["left"] <= 600 and 30 <= n["bounds"]["bottom"] - n["bounds"]["top"] <= 200), None)
    if not label:
        record("screen label for the template", False, 0, nodes=len(nodes))
        return checks
    started = time.perf_counter()
    raw, _elapsed = client.request("tools/call", {"name": "screen_capture", "arguments": {"region": label, "scale": 1, "format": "png"}}, timeout=130)
    ms = round((time.perf_counter() - started) * 1000)
    image = next((item for item in raw.get("content", []) if item.get("type") == "image"), None)
    if not image:
        record("screen crop as PNG", False, ms, error=((raw.get("structuredContent") or {}).get("error") or {}).get("code"))
        return checks
    crop = base64.b64decode(image["data"])
    width, height, rows = fx.decode_png(crop)
    record("screen crop as PNG", (width, height) == (label["right"] - label["left"], label["bottom"] - label["top"]), ms, size=(width, height))
    if writable:
        write_file(client, f"{FIXTURE_DIR}/screen-template.png", crop)
        expected = ((label["left"] + label["right"]) // 2, (label["top"] + label["bottom"]) // 2)
        ok, s, _t, ms = call(client, "opencv_find_image", {"image": "screen", "template": f"{FIXTURE_DIR}/screen-template.png", "maxResults": 3})
        best = (s.get("matches") or [{}])[0]
        center = best.get("center") and (best["center"]["x"], best["center"]["y"])
        record("find image screen", ok and near(center, expected, 3) and (best.get("score") or 0) >= 0.9, ms, count=s.get("count"),
               center=center, expected=expected, score=best.get("score"), captureSource=s.get("captureSource"), error=(s.get("error") or {}).get("code"))

    cx, cy = width // 2, height // 2
    color = fx.pixel(rows, cx, cy)
    ok, s, _t, ms = call(client, "opencv_find_color", {"image": "screen", "color": fx.hex_color(color), "region": label})
    point = s.get("point") and (s["point"]["x"], s["point"]["y"])
    inside = point is not None and label["left"] <= point[0] < label["right"] and label["top"] <= point[1] < label["bottom"]
    matches = inside and all(abs(a - b) <= 4 for a, b in zip(fx.pixel(rows, point[0] - label["left"], point[1] - label["top"]), color))
    record("find color screen", ok and s.get("found") and matches, ms, point=point, captureSource=s.get("captureSource"))

    dx, dy = min(10, width - 1 - cx), min(6, height - 1 - cy)
    points = [{"x": dx, "y": 0, "color": fx.hex_color(fx.pixel(rows, cx + dx, cy))}, {"x": 0, "y": dy, "color": fx.hex_color(fx.pixel(rows, cx, cy + dy))}]
    region = {"left": label["left"], "top": label["top"], "right": label["right"] - dx, "bottom": label["bottom"] - dy}
    ok, s, _t, ms = call(client, "opencv_find_multi_colors", {"image": "screen", "color": fx.hex_color(color), "points": points, "region": region})
    point = s.get("point") and (s["point"]["x"], s["point"]["y"])
    record("find multi colors screen", ok and s.get("found") and point is not None
           and region["left"] <= point[0] < region["right"] and region["top"] <= point[1] < region["bottom"], ms, point=point)

    ok, s, _t, ms = call(client, "barcode_detect", {"image": "screen"})
    record("barcode screen runs", ok and isinstance(s.get("count"), int), ms, count=s.get("count"), width=s.get("width"),
           height=s.get("height"), captureSource=s.get("captureSource"), error=(s.get("error") or {}).get("code"))

    if args.show_barcode_with:
        checks.extend(screen_barcode(device, client, args.show_barcode_with))
    device.shell("input", "keyevent", "KEYCODE_HOME", check=False)
    return checks


def screen_barcode(device: Device, client, viewer: str) -> list[dict]:
    """Opens the EAN-13 fixture full screen in [viewer] and decodes it from the screen."""
    local = REPO / "build" / "evidence" / "ean13-screen.png"
    local.parent.mkdir(parents=True, exist_ok=True)
    code = fx.ean13(6, 300)
    canvas = fx.Canvas(code.width + 120, 600)
    canvas.paste(code, 60, 150)
    local.write_bytes(canvas.png())
    try:
        device.run("push", str(local), SHARED_BARCODE)
        # Android 10+ ignores the scanner broadcast; MediaProvider's scan_file call indexes the file at once.
        device.shell("content", "call", "--uri", "content://media", "--method", "scan_file", "--arg", SHARED_BARCODE, check=False)
        uri = None
        for _ in range(10):
            out = device.shell("content", "query", "--uri", "content://media/external/images/media", "--projection", "_id",
                               "--where", SHARED_BARCODE_WHERE, check=False)
            uri = next((f"content://media/external/images/media/{line.split('_id=')[1].split(',')[0].strip()}" for line in out.splitlines() if "_id=" in line), None)
            if uri:
                break
            time.sleep(1)
        if not uri:
            return [{"check": "barcode screen in a viewer", "pass": False, "ms": 0, "reason": "the image was not indexed"}]
        device.shell("am", "start", "-a", "android.intent.action.VIEW", "-t", "image/png", "-d", uri, "-p", viewer)
        time.sleep(3)
        ok, s, _t, ms = call(client, "barcode_detect", {"image": "screen", "formats": ["EAN_13"]})
        item = (s.get("items") or [{}])[0]
        passed = ok and item.get("rawValue") == fx.EAN13_VALUE
        progress(f"{'PASS' if passed else 'FAIL'} barcode screen in a viewer")
        return [{"check": "barcode screen in a viewer", "pass": passed, "ms": ms, "count": s.get("count"), "format": item.get("format"),
                 "valueMatches": item.get("rawValue") == fx.EAN13_VALUE, "bounds": item.get("bounds"), "viewer": viewer}]
    finally:
        device.shell("input", "keyevent", "KEYCODE_BACK", check=False)
        device.shell("rm", "-f", SHARED_BARCODE, check=False)
        device.shell("content", "delete", "--uri", "content://media/external/images/media", "--where", SHARED_BARCODE_WHERE, check=False)


def run_checks(device: Device, client, args, files: dict, report: dict) -> None:
    writable = not args.skip_files
    try:
        if writable:
            progress("writing the fixtures through script_run")
            try:
                for name, fixture in files.items():
                    try:
                        write_file(client, f"{FIXTURE_DIR}/{name}", fixture["png"])
                    except DeviceError as error:
                        raise DeviceError(f"{name} ({len(fixture['png'])} bytes): {error}") from error
                report["fixtures"] = {name: len(fixture["png"]) for name, fixture in files.items()}
            except DeviceError as error:
                # Without "All files access" Android 11+ refuses AutoJs6 its own working directory (EPERM); the file checks cannot run there.
                writable = False
                report["fixtures"] = "not written: " + str(error)[:400]
                progress(f"the fixtures could not be written ({str(error)[:200]}); skipping the checks that need files")
        if writable:
            report["checks"] += file_checks(client, files)
        if not args.skip_screen:
            if ensure_accessibility(device, client, report):
                report["checks"] += screen_checks(device, client, args, writable)
            else:
                report["checks"].append({"check": "screen checks", "pass": False, "ms": 0, "reason": "accessibility unavailable"})
    finally:
        if isinstance(report.get("fixtures"), dict):
            try:
                script(client, f'files.removeDir(files.path("{FIXTURE_DIR}"));')
                report["fixturesRemoved"] = True
            except DeviceError as error:
                report["fixturesRemoved"] = str(error)


def run(device: Device, args) -> dict:
    report: dict = {
        "scenario": "p11-barcode-opencv-tools",
        "startedAt": datetime.datetime.now().astimezone().isoformat(timespec="seconds"),
        "device": device.facts(),
        "packages": {name: device.package(pkg) for name, pkg in (
            ("host", HOST_PACKAGE), ("plugin", PLUGIN_PACKAGE),
            ("barcode", "io.github.supermonster003.autojs6.plugin.mlkit.barcode"), ("opencv", "io.github.supermonster003.autojs6.plugin.opencv"))},
        "checks": [],
    }
    with evidence_session(device, report, CLIENT_NAME, hold_seconds=args.hold_seconds, token_source=args.token_source, keep_pairing=args.keep_pairing) as client:
        listed, _elapsed, _metas = client.list_all("tools/list", "tools")
        names = {tool["name"] for tool in listed}
        report["toolCount"] = len(names)
        report["listed"] = {name: name in names for name in TOOLS}
        missing = [name for name in TOOLS if name not in names]
        if missing:
            # A host without the P11 grant or without the plugins hides them on purpose (roadmap D27 / D28); nothing else to check.
            report["checks"].append({"check": "tools listed", "pass": False, "ms": 0, "missing": missing,
                                     "hint": "check the barcode and OpenCV plugins and the host grant (device_info)"})
        else:
            run_checks(device, client, args, fx.fixtures(), report)
    report["passed"] = sum(1 for c in report["checks"] if c["pass"])
    report["failed"] = sum(1 for c in report["checks"] if not c["pass"])
    report["finishedAt"] = datetime.datetime.now().astimezone().isoformat(timespec="seconds")
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--serial", required=True)
    parser.add_argument("--token-source", choices=("auto", "env", "instrumentation", "dumpsys"), default="auto")
    parser.add_argument("--keep-pairing", action="store_true", help="leave the evidence client paired")
    parser.add_argument("--hold-seconds", type=int, default=300)
    parser.add_argument("--skip-screen", action="store_true", help="only the file checks")
    parser.add_argument("--skip-files", action="store_true", help="only the screen checks (also chosen when the working directory is not writable)")
    parser.add_argument("--show-barcode-with", metavar="PACKAGE", help="an image viewer package that opens content:// images (e.g. com.miui.gallery)")
    parser.add_argument("--report-dir", default=str(REPO / "build" / "evidence"))
    args = parser.parse_args()
    report = run(Device(args.serial), args)
    out_dir = pathlib.Path(args.report_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    path = out_dir / f"{datetime.datetime.now():%Y%m%d-%H%M%S}-{args.serial}-p11-image-tools.json"
    path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    for check in report.get("checks", []):
        print(f"{'PASS' if check['pass'] else 'FAIL'}  {check['check']}")
    print(f"passed {report['passed']}, failed {report['failed']}; report: {path}")
    return 0 if report["failed"] == 0 else 1


if __name__ == "__main__":
    sys.exit(main())

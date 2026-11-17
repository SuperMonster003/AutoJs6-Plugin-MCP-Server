"""Portable icon recipe renderer (MPL-2.0).

Geometry/PNG encoding derive from AutoJs6 Optical geometry v1. This file is also
copied into managed repositories: only Python's standard library and Pillow are
required there. Preview and repository generation must use this exact code.
"""

from __future__ import annotations

import argparse
import decimal
import hashlib
import io
import itertools
import json
import math
import re
import struct
import xml.etree.ElementTree as ET
import zlib
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageOps

VERSION = "1.4.0"
# "1.3.1" was never released as a Studio version: the fixed background fix shipped
# in 1.4.0. 3-Block Compose already carries a recipe and kernel copy stamped by
# that interim build, so its recipes stay importable.
SUPPORTED_VERSIONS = {"1.0.0", "1.1.0", "1.2.0", "1.3.0", "1.3.1", VERSION}
FIXED_BACKGROUND_COLORS = {
    "day": "ic_icon_studio_background_day",
    "night": "ic_icon_studio_background_night",
}
APPLICATION_MANIFEST = "app/src/main/AndroidManifest.xml"
APPLICATION_ICON = "ic_icon_studio_application"
APPLICATION_OUTPUTS = {
    "app/src/main/res/mipmap/ic_icon_studio_application.png": "legacy-day",
    "app/src/main/res/mipmap-night/ic_icon_studio_application.png": "legacy-night",
    "app/src/main/res/mipmap/ic_icon_studio_application_foreground.png": "adaptive-day",
    "app/src/main/res/mipmap-night/ic_icon_studio_application_foreground.png": "adaptive-night",
    "app/src/main/res/mipmap/ic_icon_studio_application_monochrome.png": "mono",
    "app/src/main/res/mipmap-anydpi-v26/ic_icon_studio_application.xml": "application-adaptive",
    "app/src/main/res/mipmap-night-anydpi-v26/ic_icon_studio_application.xml": "application-adaptive",
    "app/src/main/res/raw/keep_icon_studio_application.xml": "application-keep",
}


def application_outputs(min_sdk: int = 24) -> dict[str, str]:
    return {
        path.replace("anydpi-v26/", "anydpi/") if min_sdk >= 26 else path: role
        for path, role in APPLICATION_OUTPUTS.items()
        if min_sdk < 26 or not role.startswith("legacy-")
    }
RECOMMENDED_SCALE = (0.80, 1.20)
VISUAL_TARGET = 0.52
RASTER_TOLERANCE = 0.006
THREE_BACKGROUNDS = {"day": "#fafafa", "night": "#212121"}
SIZE = 432
SUPERSAMPLE = 4
ASSET_ID = re.compile(r"[a-f0-9]{64}\Z")
KEEP_RESOURCE = b"""<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:tools="http://schemas.android.com/tools"
    tools:keep="@mipmap/ic_plugin_center" />
"""


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def canonical(value) -> bytes:
    return json.dumps(
        value,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
        allow_nan=False,
    ).encode()


def confined(root: Path, relative: str) -> Path:
    if (
        not isinstance(relative, str)
        or not relative
        or "\\" in relative
        or ":" in relative
    ):
        raise ValueError("无效的项目相对路径")
    part = Path(relative)
    if part.is_absolute() or ".." in part.parts:
        raise ValueError("文件路径不能越出项目目录")
    result = (root / part).resolve()
    if not result.is_relative_to(root.resolve()):
        raise ValueError("文件路径通过链接越出了项目目录")
    return result


def encode_png(image: Image.Image) -> bytes:
    rgba = image.convert("RGBA")
    pixels = rgba.tobytes()
    stride = rgba.width * 4
    rows = b"".join(
        b"\x00" + pixels[i : i + stride] for i in range(0, len(pixels), stride)
    )
    compressor = zlib.compressobj(level=9, strategy=zlib.Z_FIXED)
    compressed = compressor.compress(rows) + compressor.flush()

    def chunk(kind, data):
        return (
            struct.pack(">I", len(data))
            + kind
            + data
            + struct.pack(">I", zlib.crc32(kind + data))
        )

    return (
        b"\x89PNG\r\n\x1a\n"
        + chunk(
            b"IHDR", struct.pack(">IIBBBBB", rgba.width, rgba.height, 8, 6, 0, 0, 0)
        )
        + chunk(b"IDAT", compressed)
        + chunk(b"IEND", b"")
    )


def load_image(data: bytes) -> Image.Image:
    with Image.open(io.BytesIO(data)) as image:
        if image.format not in {"PNG", "JPEG", "WEBP"}:
            raise ValueError("仅支持 PNG、JPEG、WebP 或经导入转换后的 SVG")
        if getattr(image, "n_frames", 1) != 1:
            raise ValueError("请选择静态图稿，动画请先导出所需的一帧")
        if image.width * image.height > 32_000_000 or max(image.size) > 8192:
            raise ValueError(
                "图片过大，请使用边长不超过 8192 像素、总像素不超过 3200 万的源图"
            )
        return ImageOps.exif_transpose(image).convert("RGBA")


def visual_size(alpha: Image.Image) -> float:
    bounds = alpha.point(lambda a: 255 if a >= 16 else 0).getbbox()
    if not bounds:
        return 0.0
    area = (bounds[2] - bounds[0]) * (bounds[3] - bounds[1])
    ink = sum(value * count for value, count in enumerate(alpha.histogram())) / 255
    return math.sqrt((area + ink) / 2) / alpha.width


def maximum_radius(alpha: Image.Image) -> float:
    cx, cy = (alpha.width - 1) / 2, (alpha.height - 1) / 2
    raw = alpha.tobytes()
    maximum = 0.0
    for y in range(alpha.height):
        row = raw[y * alpha.width : (y + 1) * alpha.width]
        occupied = [x for x, value in enumerate(row) if value]
        if occupied:
            maximum = max(
                maximum,
                math.hypot(occupied[0] - cx, y - cy),
                math.hypot(occupied[-1] - cx, y - cy),
            )
    return maximum


def default_tone(mode="mask", foreground="#272727"):
    return {
        "mode": mode,
        "foreground": foreground,
        "brightness": 1.0,
        "contrast": 1.0,
        "gamma": 1.0,
        "threshold": 128,
        "invert": False,
    }


def is_three(recipe: dict) -> bool:
    return recipe.get("repository", "").startswith("AutoJs6-Plugin-Three-")


def backgrounds(recipe: dict) -> dict:
    if is_three(recipe):
        return dict(THREE_BACKGROUNDS)
    return recipe["params"].get("backgrounds", dict(THREE_BACKGROUNDS))


def rgba_color(value: str) -> tuple:
    return (
        (0, 0, 0, 0)
        if value == "transparent"
        else tuple(bytes.fromhex(value[1:])) + (255,)
    )


def validate_params(params: dict) -> None:
    expected = {"geometry", "day", "night", "sources"}
    if not isinstance(params, dict) or set(params) - {"backgrounds"} != expected:
        raise ValueError("图标参数结构不完整或包含未知字段")
    if "backgrounds" in params:
        colors = params["backgrounds"]
        if not isinstance(colors, dict) or set(colors) != {"day", "night"}:
            raise ValueError("背景参数须分别包含亮色和暗色")
        if any(
            not isinstance(c, str)
            or not re.fullmatch(r"transparent|#[0-9a-fA-F]{6}", c)
            for c in colors.values()
        ):
            raise ValueError("背景须使用 #RRGGBB 或 transparent")
    geometry = params["geometry"]
    if (
        not isinstance(geometry, dict)
        or set(geometry) != {"mode", "scale", "offsetX", "offsetY"}
        or not isinstance(geometry["mode"], str)
        or geometry["mode"] not in {"normalized", "preserve"}
    ):
        raise ValueError("几何参数无效")
    for key, low, high in (
        ("scale", 0.25, 2),
        ("offsetX", -0.5, 0.5),
        ("offsetY", -0.5, 0.5),
    ):
        value = geometry[key]
        if (
            isinstance(value, bool)
            or not isinstance(value, (int, float))
            or not math.isfinite(value)
            or not low <= value <= high
        ):
            raise ValueError(f"{key} 超出允许范围")
    for mode in ("day", "night"):
        tone = params[mode]
        if isinstance(tone, dict) and tone.get("mode") == "derived":
            continue  # Checked with the sources below.
        if (
            not isinstance(tone, dict)
            or set(tone) != set(default_tone())
            or not isinstance(tone["mode"], str)
            or tone["mode"] not in {"mask", "grayscale", "original", "binary"}
        ):
            raise ValueError("色调模式无效")
        if not isinstance(tone["foreground"], str) or not re.fullmatch(
            r"#[0-9a-fA-F]{6}", tone["foreground"]
        ):
            raise ValueError("前景颜色须使用 #RRGGBB 格式")
        for key, low, high in (
            ("brightness", 0.1, 3),
            ("contrast", 0, 3),
            ("gamma", 0.2, 3),
            ("threshold", 0, 255),
        ):
            value = tone[key]
            if (
                isinstance(value, bool)
                or not isinstance(value, (int, float))
                or not math.isfinite(value)
                or not low <= value <= high
            ):
                raise ValueError(f"{key} 超出允许范围")
        if not isinstance(tone["invert"], bool):
            raise ValueError("反色参数无效")
    if (
        not isinstance(params["sources"], dict)
        or set(params["sources"]) != {"day", "night", "alphaMode"}
        or not isinstance(params["sources"]["alphaMode"], str)
        or params["sources"]["alphaMode"] not in {"alpha", "dark-ink"}
    ):
        raise ValueError("源图参数无效")
    for mode in ("day", "night"):
        if not isinstance(params["sources"][mode], str) or not ASSET_ID.fullmatch(
            params["sources"][mode]
        ):
            raise ValueError("源图标识无效")
    for mode in ("day", "night"):
        tone = params[mode]
        if tone["mode"] != "derived":
            continue
        validate_derived_tone(tone)
        source = tone["from"]
        if source != mode:
            if params["sources"][mode] != params["sources"][source]:
                raise ValueError("派生图稿须与来源一侧使用同一张源图")
            if params[source]["mode"] == "derived" and params[source]["from"] == mode:
                raise ValueError("亮暗两侧不能互相派生")


def source_alpha(image: Image.Image, mode="alpha") -> Image.Image:
    alpha = image.getchannel("A")
    if mode == "dark-ink":
        alpha = ImageChops.multiply(alpha, ImageOps.invert(image.convert("L")))
    return alpha


def apply_tone(image: Image.Image, tone: dict) -> Image.Image:
    alpha = image.getchannel("A")
    mode = tone["mode"]
    if mode == "derived":
        # Bands are used as given; callers resolve contrast lifting first.
        table = band_table(tone["bands"])
        if "floor" in tone:  # Only rendered tones carry it, never recipes.
            level, dark = tone["floor"]
            table = [min(v, level) if dark else max(v, level) for v in table]
        gray = image.convert("L").point(table)
        result = Image.merge("RGBA", (gray, gray, gray, alpha))
        return result
    if mode == "mask":
        color = tuple(int(tone["foreground"][i : i + 2], 16) for i in (1, 3, 5))
        result = Image.new("RGB", image.size, color)
    elif mode in {"grayscale", "binary"}:
        result = ImageOps.grayscale(image).convert("RGB")
    else:
        result = image.convert("RGB")
    if (
        tone["brightness"] != 1
        or tone["contrast"] != 1
        or tone["gamma"] != 1
        or tone["invert"]
        or mode == "binary"
    ):
        table = []
        for value in range(256):
            value = max(
                0.0,
                min(
                    1.0,
                    ((value / 255 - 0.5) * tone["contrast"] + 0.5) * tone["brightness"],
                ),
            )
            value = value ** (1 / tone["gamma"])
            value = 1 - value if tone["invert"] else value
            value = round(value * 255)
            table.append(
                (255 if value >= tone["threshold"] else 0)
                if mode == "binary"
                else value
            )
        result = result.point(table * 3)
    result = result.convert("RGBA")
    result.putalpha(alpha)
    return result


# Derived tone: re-map the gray levels of one source into the other theme while
# keeping alpha. See docs/derived-tone.md. Analysis and rendering use integer and
# decimal arithmetic only, so Windows and Linux produce the same bytes.
DERIVED_KEYS = {
    "mode", "from", "anchor", "depth", "spread", "bands", "minContrast", "autoLift"
}
DERIVED_SPREADS = ("linear", "proportional", "source")
DERIVED_PRESETS = {"soft": 0.5, "balanced": 0.6, "deep": 0.7}
DERIVED_ANCHORS = {"day": "#272727", "night": "#d8d8d8"}
DEFAULT_MIN_CONTRAST = 3.0
BAND_VISIBLE_ALPHA = 128
BAND_LIMIT = 5
BAND_VALLEY_PERCENT = 8
BAND_MIN_SHARE_PERCENT = 1
BAND_TRIM_PERCENT = 2
FLAT_BAND_SPAN = 24
BAND_SLOT_FILL = 0.75
BAND_MIN_STEP = 12
_LINEAR_GRAY = []


def _number(value, low, high, message):
    if (
        isinstance(value, bool)
        or not isinstance(value, (int, float))
        or not math.isfinite(value)
        or not low <= value <= high
    ):
        raise ValueError(message)


def validate_derived_tone(tone: dict) -> None:
    """Check one derived tone on its own; validate_params adds the source rules."""
    if not isinstance(tone, dict) or set(tone) != DERIVED_KEYS or tone["mode"] != "derived":
        raise ValueError("派生色调参数结构无效")
    if tone["from"] not in ("day", "night"):
        raise ValueError("派生来源须为 day 或 night")
    anchor = tone["anchor"]
    if (
        not isinstance(anchor, str)
        or not re.fullmatch(r"#[0-9a-fA-F]{6}", anchor)
        or len({anchor[1:3].lower(), anchor[3:5].lower(), anchor[5:7].lower()}) != 1
    ):
        raise ValueError("派生锚点须为中性灰 #RRGGBB")
    _number(tone["depth"], 0.05, 1, "派生深度超出允许范围")
    if tone["spread"] not in DERIVED_SPREADS:
        raise ValueError("派生间距方式无效")
    _number(tone["minContrast"], 1, 21, "最低对比度超出允许范围")
    if not isinstance(tone["autoLift"], bool):
        raise ValueError("自动抬升参数无效")  # noqa: TRY004 - invalid parameters are ValueError
    bands = tone["bands"]
    if not isinstance(bands, list) or not 1 <= len(bands) <= BAND_LIMIT:
        raise ValueError(f"派生色阶须为 1 - {BAND_LIMIT} 个")
    previous = -1
    for band in bands:
        if not isinstance(band, dict) or set(band) != {"from", "to"}:
            raise ValueError("派生色阶结构无效")
        for key in ("from", "to"):
            pair = band[key]
            if (
                not isinstance(pair, list)
                or len(pair) != 2
                or any(isinstance(v, bool) or not isinstance(v, int) or not 0 <= v <= 255 for v in pair)
            ):
                raise ValueError("派生色阶须使用 0 - 255 的整数")
        low, high = band["from"]
        if low > high or low <= previous:
            raise ValueError("派生色阶须按亮度升序且互不重叠")
        if low == high and band["to"][0] != band["to"][1]:
            raise ValueError("单值色阶只能对应一个目标灰度")
        previous = high


def _interpolate(x0: int, y0: int, x1: int, y1: int, x: int) -> int:
    """Round-half-up linear interpolation in integers (x0 != x1)."""
    numerator = y0 * (x1 - x0) + (y1 - y0) * (x - x0)
    denominator = x1 - x0
    if denominator < 0:
        numerator, denominator = -numerator, -denominator
    return (2 * numerator + denominator) // (2 * denominator)


def band_table(bands: list) -> list[int]:
    """256-entry gray lookup: linear inside bands and gaps, extrapolated outside."""
    ordered = sorted(bands, key=lambda band: band["from"][0])

    def along(band, value):
        (low, high), (start, end) = band["from"], band["to"]
        return start if low == high else _interpolate(low, start, high, end, value)

    table = []
    for value in range(256):
        if value < ordered[0]["from"][0]:
            level = along(ordered[0], value)
        elif value > ordered[-1]["from"][1]:
            level = along(ordered[-1], value)
        else:
            level = None
            for index, band in enumerate(ordered):
                if band["from"][0] <= value <= band["from"][1]:
                    level = along(band, value)
                    break
                following = ordered[index + 1]
                if band["from"][1] < value < following["from"][0]:
                    level = _interpolate(
                        band["from"][1], band["to"][1], following["from"][0], following["to"][0], value
                    )
                    break
        table.append(max(0, min(255, level)))
    return table


def visible_histogram(image: Image.Image) -> list[int]:
    mask = image.getchannel("A").point(lambda a: 255 if a >= BAND_VISIBLE_ALPHA else 0)
    return image.convert("L").histogram(mask)


def _band_segments(histogram: list[int]) -> list[tuple[int, int]]:
    total = sum(histogram)
    levels = [value for value, count in enumerate(histogram) if count]
    if not levels:
        return []
    smooth = [sum(histogram[max(0, v - 2) : v + 3]) for v in range(256)]
    segments = [(levels[0], levels[-1])]
    while len(segments) < BAND_LIMIT:
        best = None
        for index, (low, high) in enumerate(segments):
            for value in range(low + 1, high):
                peak = min(max(smooth[low:value]), max(smooth[value + 1 : high + 1]))
                depth = smooth[value]
                if not peak or depth * 100 > BAND_VALLEY_PERCENT * peak:
                    continue
                left, right = sum(histogram[low:value]), sum(histogram[value : high + 1])
                if min(left, right) * 100 < BAND_MIN_SHARE_PERCENT * total:
                    continue
                # Deepest valley first; ties prefer the segment centre, then lower levels.
                centre = abs(2 * value - low - high)
                if best is None or (depth * best[1], centre, value) < (best[0] * peak, best[2], best[4]):
                    best = (depth, peak, centre, index, value)
        if best is None:
            break
        index, value = best[3], best[4]
        low, high = segments[index]
        left_end = max(v for v in range(low, value) if histogram[v])
        right_start = min(v for v in range(value, high + 1) if histogram[v])
        segments[index : index + 1] = [(low, left_end), (right_start, high)]
    return segments


def analyze_bands(image: Image.Image, polarity: str) -> dict:
    """Split visible gray levels into tone bands; deterministic for one image."""
    if polarity not in ("day", "night"):
        raise ValueError("源图模式须为 day 或 night")
    histogram = visible_histogram(image)
    total = sum(histogram)
    if not total:
        raise ValueError("源图没有可见内容")
    bands = []
    for low, high in _band_segments(histogram):
        count = sum(histogram[low : high + 1])
        cut = count * BAND_TRIM_PERCENT // 100
        start, end, seen = low, high, 0
        for value in range(low, high + 1):
            seen += histogram[value]
            if seen > cut:
                start = value
                break
        seen = 0
        for value in range(high, low - 1, -1):
            seen += histogram[value]
            if seen > cut:
                end = value
                break
        bands.append({
            "from": [start, end],
            "share": round(count / total, 4),
            "flat": end - start < FLAT_BAND_SPAN,
        })
    return {"polarity": polarity, "pixels": total, "bands": bands}


def _gray_of(color: str) -> int:
    red, green, blue = bytes.fromhex(color[1:])
    return (red * 299 + green * 587 + blue * 114 + 500) // 1000


def tone_table(tone: dict) -> list[int]:
    """Gray lookup equivalent to a non-derived tone, for "keep the source" re-tones."""
    if tone["mode"] == "derived":
        return band_table(tone["bands"])
    if tone["mode"] == "mask":
        return [_gray_of(tone["foreground"])] * 256
    ramp = Image.new("RGBA", (256, 1))
    ramp.putdata([(value, value, value, 255) for value in range(256)])
    return list(apply_tone(ramp, tone).convert("L").get_flattened_data())


def derive_bands(
    bands: list,
    source: str,
    target: str,
    *,
    anchor: str,
    depth: float,
    spread: str,
    background: str | None = None,
    current: dict | None = None,
) -> list[dict]:
    """Assign target gray levels to analysed bands, strongest band at the anchor."""
    ordered = sorted(bands, key=lambda band: band["from"][0])
    if spread == "source":
        table = tone_table(current) if current else list(range(256))
        return [
            {"from": list(b["from"]), "to": [table[b["from"][0]], table[b["from"][1]]]}
            for b in ordered
        ]
    level = _gray_of(anchor)
    if background in (None, "transparent"):
        background = THREE_BACKGROUNDS[target]
    paper = _gray_of(background)
    direction = 1 if paper >= level else -1
    room = (1 - depth) * abs(paper - level)
    # Strength: brighter is stronger in a night source, darker in a day source.
    strongest_first = list(reversed(ordered)) if source == "night" else ordered
    spans = [
        0 if b["from"][1] - b["from"][0] < FLAT_BAND_SPAN else b["from"][1] - b["from"][0]
        for b in strongest_first
    ]
    count = len(strongest_first)
    offsets = []
    if spread == "linear" or count == 1:
        width = room / count
        for rank, span in enumerate(spans):
            top = rank * width
            fill = width * (1 if rank == count - 1 else BAND_SLOT_FILL)
            offsets.append((top, top + (min(1.0, fill / span) * span if span else 0)))
    else:
        gaps = []
        for stronger, weaker in itertools.pairwise(strongest_first):
            if source == "night":
                gaps.append(max(BAND_MIN_STEP, stronger["from"][0] - weaker["from"][1]))
            else:
                gaps.append(max(BAND_MIN_STEP, weaker["from"][0] - stronger["from"][1]))
        demand = sum(spans) + sum(gaps)
        scale = min(1.0, room / demand) if demand else 0
        position = 0.0
        for rank, span in enumerate(spans):
            strong = position
            position += span * scale
            offsets.append((strong, position))
            if rank < count - 1:
                position += gaps[rank] * scale
    result = {}
    for band, (strong, weak) in zip(strongest_first, offsets):
        # to[i] belongs to from[i]: the weak end is the darker level of a night source.
        levels = [max(0, min(255, int(level + direction * x + 0.5))) for x in (weak, strong)]
        if source == "day":
            levels.reverse()
        result[band["from"][0]] = {"from": list(band["from"]), "to": levels}
    return [result[b["from"][0]] for b in ordered]


def derive_tone(
    bands: list,
    source: str,
    target: str,
    preset: str = "balanced",
    *,
    anchor: str | None = None,
    depth: float | None = None,
    spread: str | None = None,
    min_contrast: float = DEFAULT_MIN_CONTRAST,
    auto_lift: bool = True,
    background: str | None = None,
    current: dict | None = None,
) -> dict:
    """Build a recipe tone from analysed bands with a named preset or explicit values."""
    if preset not in (*DERIVED_PRESETS, "source"):
        raise ValueError("未知的派生预设")
    if preset == "source":
        spread, depth = "source", 1.0
    tone = {
        "mode": "derived",
        "from": source,
        "anchor": (anchor or DERIVED_ANCHORS[target]).lower(),
        "depth": float(DERIVED_PRESETS.get(preset, 1.0) if depth is None else depth),
        "spread": spread or "linear",
        "bands": [],
        "minContrast": float(min_contrast),
        "autoLift": bool(auto_lift),
    }
    tone["bands"] = derive_bands(
        bands,
        source,
        target,
        anchor=tone["anchor"],
        depth=tone["depth"],
        spread=tone["spread"],
        background=background,
        current=current,
    )
    validate_derived_tone(tone)
    return tone


def rederive_tone(tone: dict, target: str, background: str | None = None) -> dict:
    """Recompute target levels from the saved band snapshot after preset edits."""
    validate_derived_tone(tone)
    if tone["spread"] == "source":
        return {**tone, "bands": [dict(b) for b in tone["bands"]]}
    bands = derive_bands(
        tone["bands"],
        tone["from"],
        target,
        anchor=tone["anchor"],
        depth=tone["depth"],
        spread=tone["spread"],
        background=background,
    )
    return {**tone, "bands": bands}


def _linear_gray() -> list:
    if not _LINEAR_GRAY:
        context = decimal.Context(prec=34)
        for value in range(256):
            channel = context.divide(decimal.Decimal(value), decimal.Decimal(255))
            if channel <= decimal.Decimal("0.04045"):
                linear = context.divide(channel, decimal.Decimal("12.92"))
            else:
                linear = context.power(
                    context.divide(context.add(channel, decimal.Decimal("0.055")), decimal.Decimal("1.055")),
                    decimal.Decimal("2.4"),
                )
            _LINEAR_GRAY.append(linear)
    return _LINEAR_GRAY


def relative_luminance(color: str | int) -> decimal.Decimal:
    """WCAG 2 relative luminance of #RRGGBB or of a gray level."""
    table = _linear_gray()
    if isinstance(color, int):
        return table[color]
    red, green, blue = bytes.fromhex(color[1:])
    return (
        decimal.Decimal("0.2126") * table[red]
        + decimal.Decimal("0.7152") * table[green]
        + decimal.Decimal("0.0722") * table[blue]
    )


def contrast_ratio(first: decimal.Decimal, second: decimal.Decimal) -> decimal.Decimal:
    high, low = max(first, second), min(first, second)
    return (high + decimal.Decimal("0.05")) / (low + decimal.Decimal("0.05"))


def surface_color(mode: str, background: str | None) -> str:
    return THREE_BACKGROUNDS[mode] if background in (None, "transparent") else background


def _ink(background: str):
    """Strength of a gray level on this surface: away from the paper, toward ink."""
    # Black and white have equal contrast on a surface of luminance ~0.1791.
    dark = relative_luminance(background) > decimal.Decimal("0.1791")
    return (lambda gray: 255 - gray) if dark else (lambda gray: gray)


def contrast_floor(background: str, minimum: float) -> int | None:
    """Weakest gray level that reaches the minimum contrast on this surface."""
    paper = relative_luminance(background)
    strength = _ink(background)
    floor = decimal.Decimal(str(minimum))
    return next(
        (strength(s) for s in range(256)
         if contrast_ratio(relative_luminance(strength(s)), paper) >= floor),
        None,
    )


def lift_bands(bands: list, background: str, minimum: float) -> tuple[list, bool]:
    """Raise band end points below the contrast floor, keeping order and gradients."""
    strength = _ink(background)
    floor_level = contrast_floor(background, minimum)
    required = None if floor_level is None else strength(floor_level)
    points = [strength(level) for band in bands for level in band["to"]]
    if required is None or min(points) >= required:
        return [{"from": list(b["from"]), "to": list(b["to"])} for b in bands], False
    lowest, highest = min(points), max(points)
    # Levels at or above the strongest end of a failing band are kept exactly;
    # the range below it is mapped linearly onto [required, pivot].
    failing = [
        [strength(level) for level in band["to"]]
        for band in bands
        if min(strength(level) for level in band["to"]) < required
    ]
    pivot = min(max(max(max(ends) for ends in failing), required + BAND_MIN_STEP), highest)

    def lift(value):
        if pivot <= required:
            return min(255, value + required - lowest)
        if value >= pivot:
            return value
        return required + _interpolate(lowest, 0, pivot, pivot - required, value)

    lifted = [
        {"from": list(b["from"]), "to": [strength(lift(strength(level))) for level in b["to"]]}
        for b in bands
    ]
    return lifted, True


def effective_tone(tone: dict, mode: str, background: str | None) -> dict:
    """The tone actually rendered: derived bands lifted against the theme surface."""
    if tone["mode"] != "derived" or not tone["autoLift"]:
        return tone
    surface = surface_color(mode, background)
    bands, _ = lift_bands(tone["bands"], surface, tone["minContrast"])
    level = contrast_floor(surface, tone["minContrast"])
    if level is None:
        return {**tone, "bands": bands}
    # Trimmed tails extrapolate past the band ends; clamp them at the floor too.
    return {**tone, "bands": bands, "floor": (level, _ink(surface)(0) == 255)}


def flat_color(tone: dict) -> str | None:
    """A derived tone whose levels are all equal renders exactly like mask mode."""
    if tone["mode"] != "derived":
        return None
    levels = {level for band in tone["bands"] for level in band["to"]}
    if len(levels) != 1:
        return None
    return "#" + f"{levels.pop():02x}" * 3


def contrast_report(
    image: Image.Image, mode: str, background: str | None, tone: dict | None = None
) -> dict | None:
    """Per-band contrast of a rendered theme against its surface."""
    surface = surface_color(mode, background)
    paper = relative_luminance(surface)
    strength = _ink(surface)
    derived = tone is not None and tone["mode"] == "derived"
    minimum = tone["minContrast"] if derived else DEFAULT_MIN_CONTRAST
    floor = decimal.Decimal(str(minimum))
    rows = []
    if derived:
        for index, band in enumerate(effective_tone(tone, mode, background)["bands"]):
            weak = min(band["to"], key=strength)
            rows.append((index, sorted(band["to"]), None, weak))
    else:
        try:
            analysed = analyze_bands(image, mode)["bands"]
        except ValueError:
            return None  # Only faint edges remain; the size checks report that case.
        for index, band in enumerate(analysed):
            weak = min(band["from"], key=strength)
            rows.append((index, band["from"], band["share"], weak))
    bands = []
    for index, levels, share, weak in rows:
        ratio = contrast_ratio(relative_luminance(weak), paper)
        bands.append({
            "band": index,
            "range": list(levels),
            "share": share,
            "weakest": weak,
            "contrast": float(ratio.quantize(decimal.Decimal("0.01"))),
            "ok": ratio >= floor,
        })
    return {
        "background": surface,
        "threshold": minimum,
        "minimum": min(b["contrast"] for b in bands),
        "lifted": derived and tone["autoLift"] and effective_tone(tone, mode, background)["bands"] != tone["bands"],
        "bands": bands,
    }


def glyph(alpha, color):
    result = Image.new("RGBA", alpha.size, (*color[:3], 255))
    result.putalpha(alpha)
    return result


def place(alpha: Image.Image, ratio: float, x: float, y: float) -> Image.Image:
    side = SIZE * SUPERSAMPLE
    width = max(1, round(side * ratio))
    height = max(1, round(width * alpha.height / alpha.width))
    left = round((side - width) / 2 + x * width)
    top = round((side - height) / 2 + y * height)
    if left < 0 or top < 0 or left + width > side or top + height > side:
        raise ValueError("图案超出画布，请减小尺寸或偏移")
    canvas = Image.new("L", (side, side))
    canvas.paste(alpha.resize((width, height), Image.Resampling.LANCZOS), (left, top))
    return canvas.resize((SIZE, SIZE), Image.Resampling.LANCZOS)


def legacy(foreground: Image.Image, color) -> Image.Image:
    side = SIZE * SUPERSAMPLE
    alpha = Image.new("L", (side, side))
    ImageDraw.Draw(alpha).ellipse((0, 0, side - 1, side - 1), fill=255)
    background = glyph(alpha.resize((SIZE, SIZE), Image.Resampling.LANCZOS), color)
    return Image.alpha_composite(background, foreground)


def render_images(recipe: dict, loader) -> dict[str, Image.Image]:
    params = recipe["params"]
    validate_params(params)
    geometry = params["geometry"]
    sources = {
        mode: load_image(loader(params["sources"][mode])) for mode in ("day", "night")
    }
    alpha = source_alpha(sources["day"], params["sources"]["alphaMode"])
    bounds = alpha.getbbox()
    if bounds is None:
        raise ValueError("源图没有可见内容")
    raw_alpha = alpha.crop(bounds)
    normalized = geometry["mode"] == "normalized"
    ratio = (
        VISUAL_TARGET * geometry["scale"] / visual_size(raw_alpha)
        if normalized
        else geometry["scale"]
    )
    result = {}
    surfaces = backgrounds(recipe)
    for mode in ("day", "night"):
        tone = effective_tone(params[mode], mode, surfaces[mode])
        if flat_color(tone):
            tone = default_tone("mask", flat_color(tone))
        original = sources[mode]
        own_alpha = source_alpha(original, params["sources"]["alphaMode"])
        own_bounds = own_alpha.getbbox()
        if not own_bounds:
            raise ValueError("亮暗源图都必须包含可见内容")
        if normalized:
            original = original.crop(own_bounds)
        else:
            # Preserve the original composition on a square artboard, including its margins.
            width, height = original.size
            square = Image.new("RGBA", (max(width, height),) * 2)
            square.alpha_composite(
                original, ((square.width - width) // 2, (square.height - height) // 2)
            )
            original = square
        if normalized:
            crop_alpha = own_alpha.crop(own_bounds)
        else:
            crop_alpha = original.getchannel("A")
        for surface, factor in (("ui", 1.0), ("adaptive", 72 / 108)):
            target_ratio = (
                VISUAL_TARGET * geometry["scale"] / visual_size(crop_alpha)
                if normalized and recipe["profile"] != "neutral-v1"
                else ratio
            ) * factor
            placed = place(
                crop_alpha, target_ratio, geometry["offsetX"], geometry["offsetY"]
            )
            if tone["mode"] == "mask":
                toned = apply_tone(glyph(placed, (255, 255, 255)), tone)
            else:
                original_toned = apply_tone(original, tone)
                side = SIZE * SUPERSAMPLE
                width = max(1, round(side * target_ratio))
                height = max(1, round(width * crop_alpha.height / crop_alpha.width))
                left = round((side - width) / 2 + geometry["offsetX"] * width)
                top = round((side - height) / 2 + geometry["offsetY"] * height)
                canvas = Image.new("RGBA", (side, side))
                canvas.alpha_composite(
                    original_toned.resize((width, height), Image.Resampling.LANCZOS),
                    (left, top),
                )
                toned = canvas.resize((SIZE, SIZE), Image.Resampling.LANCZOS)
                toned.putalpha(placed)
            result[f"{surface}-{mode}"] = toned
    result["mono"] = glyph(result["adaptive-day"].getchannel("A"), (0, 0, 0))
    for mode, color in backgrounds(recipe).items():
        result[f"legacy-{mode}"] = (
            result[f"ui-{mode}"].copy()
            if color == "transparent"
            else legacy(result[f"ui-{mode}"], rgba_color(color)[:3])
        )
    return result


def inspect_images(recipe: dict, images: dict) -> dict:
    errors, warnings = [], []
    metrics = {}
    standard = recipe["profile"] == "neutral-v1"
    neutral = is_three(recipe) or (standard and not recipe.get("repository"))
    if (
        is_three(recipe)
        and recipe["params"].get("backgrounds", THREE_BACKGROUNDS) != THREE_BACKGROUNDS
    ):
        errors.append("Three 系列固定使用 #FAFAFA / #212121 背景")
    if standard:
        geometry = recipe["params"]["geometry"]
        if geometry["mode"] != "normalized":
            errors.append("规范图标必须使用统一的视觉归一化方式")
    alphas = []
    for mode in ("day", "night"):
        image = images[f"ui-{mode}"]
        alpha = image.getchannel("A")
        alphas.append(alpha.tobytes())
        pixels = image.get_flattened_data()
        colored = sum(1 for r, g, b, a in pixels if a > 0 and (r != g or g != b))
        opaque = [r for r, g, b, a in pixels if a == 255]
        mean = sum(opaque) / len(opaque) if opaque else None
        value = visual_size(alpha)
        bounds = alpha.point(lambda a: 255 if a >= 16 else 0).getbbox()
        radius = maximum_radius(alpha)
        adaptive_radius = maximum_radius(images[f"adaptive-{mode}"].getchannel("A"))
        metrics[mode] = {
            "visualSize": value,
            "bounds": bounds,
            "transparentRatio": alpha.histogram()[0] / (SIZE * SIZE),
            "radius": radius,
            "adaptiveRadius": adaptive_radius,
            "coloredPixels": colored,
        }
        tone = recipe["params"][mode]
        if neutral or tone["mode"] == "derived":
            # Contrast is advisory like optical size: it never blocks generation.
            report = contrast_report(
                image, mode, backgrounds(recipe)[mode], tone if tone["mode"] == "derived" else None
            )
            metrics[mode]["contrast"] = report
            failing = [band for band in report["bands"] if not band["ok"]] if report else []
            if failing:
                warnings.append(
                    f"{'浅色' if mode == 'day' else '深色'}图稿有 {len(failing)} 个色阶与背景的对比度"
                    f"低于 {report['threshold']:g}:1 (最低 {report['minimum']:.2f}:1), "
                    "可开启自动抬升或调整色阶"
                )
        if neutral:
            if mean is None:
                errors.append("图稿需要包含清晰的不透明主体")
            elif (mode == "day" and mean >= 128) or (mode == "night" and mean <= 128):
                errors.append(
                    "浅色图稿的主体应为深色，深色图稿的主体应为浅色；请调整前景、亮度或反色"
                )
            if colored:
                errors.append(
                    f"{'浅色' if mode == 'day' else '深色'}图稿包含彩色像素，请选择灰阶或单色填充"
                )
        if standard:
            if alpha.histogram()[0] <= SIZE * SIZE / 2:
                errors.append("透明区域须超过画布的一半")
            # Optical size is an art-direction guide. Cropping and safe circles
            # are independent constraints and remain blocking errors below.
            low, high = RECOMMENDED_SCALE
            if not (low <= geometry["scale"] <= high) or not (
                VISUAL_TARGET * low - RASTER_TOLERANCE
                <= value
                <= VISUAL_TARGET * high + RASTER_TOLERANCE
            ):
                warnings.append(
                    "尺寸偏离建议范围（缩放 80%–120%，视觉尺寸约 0.42–0.62）；可继续应用，请结合整组预览判断"
                )
            if (
                bounds
                and max(bounds[2] - bounds[0], bounds[3] - bounds[1]) > SIZE * 0.80
            ):
                warnings.append(
                    "图案最长边超过画布的 80%；可继续应用，请留意圆形边缘的视觉留白"
                )
            if radius > 216:
                errors.append("图案超出列表圆形边界")
            if recipe.get("launcher") and adaptive_radius > 132:
                errors.append("启动器前景超出 33 dp 安全圆")
        elif value > 0.8:
            warnings.append("当前图稿较满，可使用“按标准归一化”与其他插件比较")
    if standard and alphas[0] != alphas[1]:
        errors.append("亮暗图稿的透明轮廓必须一致")
    return {
        "metrics": metrics,
        "errors": list(dict.fromkeys(errors)),
        "warnings": list(dict.fromkeys(warnings)),
    }


def generated_files(recipe: dict, loader, *, strict=True) -> dict[str, bytes]:
    images = render_images(recipe, loader)
    report = inspect_images(recipe, images)
    if strict and report["errors"]:
        raise ValueError("；".join(report["errors"]))
    baseline = recipe.get("baseline", {})
    artwork = lambda params: {
        k: v for k, v in (params or {}).items() if k != "backgrounds"
    }
    unchanged = canonical(artwork(recipe["params"])) == canonical(
        artwork(baseline.get("params"))
    )
    background_changed = recipe["params"].get("backgrounds") != baseline.get(
        "params", {}
    ).get("backgrounds")
    result = {}
    fixed_backgrounds = set()
    for path, descriptor in recipe["outputs"].items():
        if not path.startswith("app/src/") or "/res/" not in path:
            raise ValueError("生成目标必须是项目资源文件")
        role = descriptor["role"]
        if role == "application-keep":
            result[path] = b'<resources xmlns:tools="http://schemas.android.com/tools" tools:keep="@mipmap/ic_launcher" />\n'
        elif role == "application-adaptive":
            result[path] = (
                '<?xml version="1.0" encoding="utf-8"?>\n'
                '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
                '    <background android:drawable="@color/ic_plugin_center_background" />\n'
                f'    <foreground android:drawable="@mipmap/{APPLICATION_ICON}_foreground" />\n'
                f'    <monochrome android:drawable="@mipmap/{APPLICATION_ICON}_monochrome" />\n'
                '</adaptive-icon>\n'
            ).encode()
        elif role == "background-keep":
            result[path] = (
                b'<resources xmlns:tools="http://schemas.android.com/tools" tools:keep="@color/ic_plugin_center_background" />\n'
            )
        elif role == "background-color":
            color = backgrounds(recipe)[descriptor["theme"]]
            value = "#00000000" if color == "transparent" else color.upper()
            result[path] = (
                '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <color name="ic_plugin_center_background">'
                + value
                + "</color>\n</resources>\n"
            ).encode()
        elif role == "adaptive-background":
            original = loader(descriptor["original"])
            background = ET.fromstring(original).find("background")
            if background is None:
                raise ValueError("自适应图标缺少背景图层")
            reference = background.get("{http://schemas.android.com/apk/res/android}drawable", "")
            # Drawable attributes require a resource reference, including when an
            # older renderer already saved an invalid literal as its input snapshot.
            if not background_changed and not reference.startswith("#"):
                result[path] = original
            else:
                theme = descriptor.get("theme")
                reference = "@color/" + (
                    FIXED_BACKGROUND_COLORS[theme]
                    if theme else "ic_plugin_center_background"
                )
                result[path], replacements = re.subn(
                    rb'(<background\b[^>]*\bandroid:drawable\s*=\s*)([\"\'])(.*?)\2',
                    lambda m: m[1] + m[2] + reference.encode() + m[2],
                    original,
                    flags=re.DOTALL,
                )
                if replacements != 1:
                    raise ValueError("无法定位自适应图标的背景资源引用")
            for mode, name in FIXED_BACKGROUND_COLORS.items():
                if reference == "@color/" + name:
                    fixed_backgrounds.add(mode)
        elif descriptor.get("original") and (
            role in {"keep", "keep-rule"}
            or (unchanged and not (background_changed and role.startswith("legacy-")))
        ):
            result[path] = loader(descriptor["original"])
        elif role == "keep":
            raise ValueError("缺少需要保留的原资源")
        elif role == "keep-rule":
            result[path] = KEEP_RESOURCE
        else:
            image = images[role]
            target = tuple(descriptor.get("size", (SIZE, SIZE)))
            if target != image.size:
                image = image.resize(target, Image.Resampling.LANCZOS)
            result[path] = encode_png(image)
    if fixed_backgrounds:
        default_colors = next(
            (path for path, descriptor in recipe["outputs"].items()
             if descriptor["role"] == "background-color" and descriptor["theme"] == "day"),
            None,
        )
        if default_colors is None:
            raise ValueError("缺少固定启动器背景色的资源输出")
        # Define fixed colors only in values/, so system night mode cannot change
        # the background of a light/dark launcher alias chosen by the user.
        extra = ""
        for mode, name in FIXED_BACKGROUND_COLORS.items():
            if mode in fixed_backgrounds:
                color = backgrounds(recipe)[mode]
                value = "#00000000" if color == "transparent" else color.upper()
                extra += f'    <color name="{name}">{value}</color>\n'
        result[default_colors] = result[default_colors].replace(
            b"</resources>", extra.encode() + b"</resources>"
        )
    return result


def application_manifest(data: bytes) -> bytes:
    """Change only the application's two icon references; preserve all components."""
    text = data.decode("utf-8")
    document = ET.fromstring(text)
    if len(document.findall("application")) != 1:
        raise ValueError("系统图标需要唯一的 application 节点")
    if not re.search(r'xmlns:android\s*=\s*[\"\']http://schemas.android.com/apk/res/android[\"\']', text):
        raise ValueError("无法识别 Manifest 的 android 命名空间")
    tags = list(re.finditer(r"<application(?=[\s/>])(?:[^\"'>]|\"[^\"]*\"|'[^']*')*?>", text))
    if len(tags) != 1:
        raise ValueError("无法安全定位 application 图标引用")
    match = tags[0]
    tag = match[0]
    for attribute in ("icon", "roundIcon"):
        pattern = rf"(\bandroid:{attribute}\s*=\s*)([\"'])(.*?)(\2)"
        value = "@mipmap/" + APPLICATION_ICON
        if re.search(pattern, tag, re.DOTALL):
            tag = re.sub(pattern, lambda m, value=value: m[1] + m[2] + value + m[4], tag, flags=re.DOTALL)
        else:
            closing = "/>" if tag.endswith("/>") else ">"
            head = tag[: -len(closing)]
            trimmed = head.rstrip()
            indentation = re.search(r"\n([ \t]+)\S", tag)
            separator = ("\r\n" if "\r\n" in tag else "\n") + indentation[1] if indentation else " "
            tag = trimmed + separator + f'android:{attribute}="{value}"' + head[len(trimmed) :] + closing
    return (text[: match.start()] + tag + text[match.end() :]).encode("utf-8")


def project_files(recipe: dict, loader, read, *, strict=True) -> dict[str, bytes | None]:
    outputs = generated_files(recipe, loader, strict=strict)
    if recipe.get("systemIcon"):
        outputs[APPLICATION_MANIFEST] = application_manifest(read(APPLICATION_MANIFEST))
        owned_paths = set(APPLICATION_OUTPUTS) | set(application_outputs(26))
        for path in recipe.get("systemIconObsolete", []):
            if path not in owned_paths or path in outputs:
                raise ValueError("不能移除不属于旧系统图标配置的文件: " + str(path))
            outputs[path] = None
        try:
            properties = read("version.properties")
        except FileNotFoundError:
            properties = None
        minimum = re.search(rb"(?m)^MIN_SDK_VERSION=(\d+)", properties or b"")
        if minimum and "systemIconMinSdk" in recipe and int(minimum[1]) != recipe["systemIconMinSdk"]:
            raise ValueError("最低 Android 版本已改变，请重新扫描并应用系统图标资源")
    return outputs


def remove_system_resource(root: Path, relative: str) -> None:
    if relative not in set(APPLICATION_OUTPUTS) | set(application_outputs(26)):
        raise ValueError("只能移除已列明的系统图标资源")
    path = confined(root, relative)
    if not path.is_relative_to((root / "app/src/main/res").resolve()):
        raise ValueError("系统图标删除目标越出资源目录")
    path.unlink()
    try:
        path.parent.rmdir()  # Only removes an empty resource folder; never recursive.
    except OSError:
        pass


def derive_cli(argv: list[str] | None = None) -> int:
    """Derive artwork for one theme from a grayscale source (a file, not a recipe)."""
    parser = argparse.ArgumentParser(
        prog="derive",
        description="Derive the other theme from one grayscale source by tone bands",
    )
    parser.add_argument("--source", type=Path, required=True, help="PNG, JPEG or WebP source")
    parser.add_argument(
        "--polarity", choices=("day", "night"), required=True,
        help="theme the source was drawn for (night: brighter is stronger)",
    )
    parser.add_argument("--target", choices=("day", "night"), help="default: the other theme")
    parser.add_argument("--preset", choices=(*DERIVED_PRESETS, "source"), default="balanced")
    parser.add_argument("--depth", type=float)
    parser.add_argument("--spread", choices=DERIVED_SPREADS)
    parser.add_argument("--anchor", help="neutral #RRGGBB for the strongest band")
    parser.add_argument("--background", help="#RRGGBB surface for the contrast guard")
    parser.add_argument("--min-contrast", type=float, default=DEFAULT_MIN_CONTRAST)
    parser.add_argument("--no-lift", action="store_true", help="report low contrast only")
    parser.add_argument("--out", type=Path, required=True, help="derived PNG to write")
    parser.add_argument("--tone-out", type=Path, help="also write the recipe tone as JSON")
    args = parser.parse_args(argv)
    if args.out.resolve() == args.source.resolve():
        parser.error("--out must not replace the source artwork")
    target = args.target or ("day" if args.polarity == "night" else "night")
    if args.background is not None and not re.fullmatch(r"#[0-9a-fA-F]{6}", args.background):
        parser.error("--background must be #RRGGBB")
    image = load_image(args.source.read_bytes())
    analysis = analyze_bands(image, args.polarity)
    try:
        tone = derive_tone(
            analysis["bands"], args.polarity, target, args.preset,
            anchor=args.anchor, depth=args.depth, spread=args.spread,
            min_contrast=args.min_contrast, auto_lift=not args.no_lift,
            background=args.background,
        )
    except ValueError as error:
        parser.error(str(error))
    rendered = effective_tone(tone, target, args.background)
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_bytes(encode_png(apply_tone(image, rendered)))
    if args.tone_out:
        args.tone_out.write_text(json.dumps(tone, indent=2) + "\n", encoding="utf-8")
    report = {
        "source": args.polarity,
        "target": target,
        "bands": analysis["bands"],
        "tone": tone,
        "rendered": rendered["bands"],
        "contrast": contrast_report(image, target, args.background, tone),
    }
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0


def main(root: Path | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="Generate resources from the committed Icon Studio recipe"
    )
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--root", type=Path)
    args = parser.parse_args()
    root = (args.root or root or Path(__file__).resolve().parents[1]).resolve()
    recipe = json.loads((root / ".icons/recipe.json").read_text(encoding="utf-8"))
    if (
        recipe.get("schemaVersion") != 1
        or recipe.get("generatorVersion") not in SUPPORTED_VERSIONS
    ):
        raise ValueError("图标配方与生成内核版本不匹配")

    def loader(asset_id):
        if not ASSET_ID.fullmatch(asset_id):
            raise ValueError("源图标识无效")
        data = confined(root, f".icons/assets/{asset_id}").read_bytes()
        if sha(data) != asset_id:
            raise ValueError("源图摘要不匹配")
        return data

    outputs = project_files(recipe, loader, lambda path: confined(root, path).read_bytes())
    if any(confined(root, p).exists() and not confined(root, p).is_file() for p in outputs):
        raise ValueError("生成目标被同名目录占用")
    changed = [
        (confined(root, p), data)
        for p, data in outputs.items()
        if (confined(root, p).read_bytes() if confined(root, p).is_file() else None) != data
    ]
    if args.check:
        if changed:
            raise SystemExit(
                "图标资源与配方不一致: "
                + ", ".join(str(p.relative_to(root)) for p, _ in changed)
            )
        print(f"Verified {len(outputs)} Icon Studio resources")
        return 0
    for path, data in changed:
        if data is None:
            remove_system_resource(root, path.relative_to(root).as_posix())
            print(f"Removed obsolete system resource {path.relative_to(root).as_posix()}")
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(data)
            print(f"Generated {path.relative_to(root).as_posix()}")
    return 0


if __name__ == "__main__":
    import sys

    if sys.argv[1:2] == ["derive"]:
        raise SystemExit(derive_cli(sys.argv[2:]))
    raise SystemExit(main())

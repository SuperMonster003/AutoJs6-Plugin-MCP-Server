"""Test images for the P11 image tools, built with the standard library only.

PNG encoding (8-bit RGB, filter 0) and decoding (8-bit gray / RGB / RGBA, every filter type, no
interlacing) are just enough for the fixtures written to the phone and the screen crops read back
from `screen_capture`. The EAN-13 symbol follows the GS1 module tables; ML Kit decodes it from a
plain rendering without a quiet-zone margin beyond 11 modules.
"""
from __future__ import annotations

import struct
import zlib

WHITE = (255, 255, 255)
BLACK = (0, 0, 0)
RED = (255, 0, 0)
GREEN = (0, 160, 0)
BLUE = (0, 0, 255)

EAN13_VALUE = "4006381333931"

_L = ["0001101", "0011001", "0010011", "0111101", "0100011", "0110001", "0101111", "0111011", "0110111", "0001011"]
_G = ["0100111", "0110011", "0011011", "0100001", "0011101", "0111001", "0000101", "0010001", "0001001", "0010111"]
_R = ["1110010", "1100110", "1101100", "1000010", "1011100", "1001110", "1010000", "1000100", "1001000", "1110100"]
_PARITY = ["LLLLLL", "LLGLGG", "LLGGLG", "LLGGGL", "LGLLGG", "LGGLLG", "LGGGLL", "LGLGLG", "LGLGGL", "LGGLGL"]
QUIET_MODULES = 11


class Canvas:
    """An RGB image as one bytearray, row by row."""

    def __init__(self, width: int, height: int, background: tuple[int, int, int] = WHITE):
        self.width = width
        self.height = height
        self.pixels = bytearray(bytes(background) * (width * height))

    def fill(self, left: int, top: int, right: int, bottom: int, color: tuple[int, int, int]) -> None:
        row = bytes(color) * (right - left)
        for y in range(top, bottom):
            start = (y * self.width + left) * 3
            self.pixels[start:start + len(row)] = row

    def paste(self, other: "Canvas", left: int, top: int) -> None:
        for y in range(other.height):
            src = y * other.width * 3
            dst = ((top + y) * self.width + left) * 3
            self.pixels[dst:dst + other.width * 3] = other.pixels[src:src + other.width * 3]

    def png(self) -> bytes:
        stride = self.width * 3
        raw = b"".join(b"\x00" + bytes(self.pixels[y * stride:(y + 1) * stride]) for y in range(self.height))
        return encode_png(self.width, self.height, raw)


def encode_png(width: int, height: int, filtered_rgb_rows: bytes) -> bytes:
    def chunk(kind: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)
    header = struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0)
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(b"IDAT", zlib.compress(filtered_rgb_rows, 9)) + chunk(b"IEND", b"")


def decode_png(data: bytes) -> tuple[int, int, list[bytearray]]:
    """(width, height, RGB rows) of an 8-bit gray, RGB or RGBA PNG without interlacing."""
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("not a PNG")
    pos, idat, width = 8, b"", 0
    height = depth = color_type = interlace = 0
    while pos < len(data):
        length, kind = struct.unpack(">I4s", data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + length]
        pos += 12 + length
        if kind == b"IHDR":
            width, height, depth, color_type, _c, _f, interlace = struct.unpack(">IIBBBBB", body)
        elif kind == b"IDAT":
            idat += body
        elif kind == b"IEND":
            break
    channels = {0: 1, 2: 3, 6: 4}.get(color_type)
    if depth != 8 or channels is None or interlace:
        raise ValueError(f"unsupported PNG (depth {depth}, color type {color_type}, interlace {interlace})")
    raw = zlib.decompress(idat)
    stride = width * channels
    rows: list[bytearray] = []
    previous = bytearray(stride)
    for y in range(height):
        kind = raw[y * (stride + 1)]
        line = bytearray(raw[y * (stride + 1) + 1:(y + 1) * (stride + 1)])
        for i in range(stride):
            left = line[i - channels] if i >= channels else 0
            up = previous[i]
            up_left = previous[i - channels] if i >= channels else 0
            if kind == 1:
                line[i] = (line[i] + left) & 0xFF
            elif kind == 2:
                line[i] = (line[i] + up) & 0xFF
            elif kind == 3:
                line[i] = (line[i] + (left + up) // 2) & 0xFF
            elif kind == 4:
                p = left + up - up_left
                pa, pb, pc = abs(p - left), abs(p - up), abs(p - up_left)
                line[i] = (line[i] + (left if pa <= pb and pa <= pc else up if pb <= pc else up_left)) & 0xFF
        previous = line
        if channels == 3:
            rows.append(line)
        else:
            rgb = bytearray()
            for x in range(width):
                pixel = line[x * channels:(x + 1) * channels]
                rgb += bytes(pixel[:1] * 3) if channels == 1 else bytes(pixel[:3])
            rows.append(rgb)
    return width, height, rows


def pixel(rows: list[bytearray], x: int, y: int) -> tuple[int, int, int]:
    row = rows[y]
    return row[x * 3], row[x * 3 + 1], row[x * 3 + 2]


def hex_color(color: tuple[int, int, int]) -> str:
    return "#%02X%02X%02X" % color


def ean13_check_digit(first12: str) -> int:
    total = sum(int(d) * (3 if i % 2 else 1) for i, d in enumerate(first12))
    return (10 - total % 10) % 10


def ean13_modules(value: str = EAN13_VALUE) -> str:
    """The 95 modules (1 = bar) of a 13-digit EAN; the check digit must be correct."""
    if len(value) != 13 or not value.isdigit() or ean13_check_digit(value[:12]) != int(value[12]):
        raise ValueError("not a valid EAN-13 value")
    parity = _PARITY[int(value[0])]
    left = "".join((_L if parity[i] == "L" else _G)[int(d)] for i, d in enumerate(value[1:7]))
    right = "".join(_R[int(d)] for d in value[7:])
    return "101" + left + "01010" + right + "101"


def ean13(module: int, bar_height: int, value: str = EAN13_VALUE) -> Canvas:
    """The symbol with its quiet zones; bars span the whole height."""
    modules = ean13_modules(value)
    canvas = Canvas((len(modules) + 2 * QUIET_MODULES) * module, bar_height)
    for index, bit in enumerate(modules):
        if bit == "1":
            left = (QUIET_MODULES + index) * module
            canvas.fill(left, 0, left + module, bar_height, BLACK)
    return canvas


# A 6 x 4 grid of 10 px cells; no row or column repeats, so the pattern matches in one place only.
PATTERN = ["KRWGKW", "WKGRWK", "GWKWRG", "RGWKKW"]
_CELL_COLORS = {"K": BLACK, "W": WHITE, "R": RED, "G": GREEN}
PATTERN_CELL = 10


def pattern() -> Canvas:
    canvas = Canvas(len(PATTERN[0]) * PATTERN_CELL, len(PATTERN) * PATTERN_CELL)
    for row, cells in enumerate(PATTERN):
        for column, cell in enumerate(cells):
            canvas.fill(column * PATTERN_CELL, row * PATTERN_CELL, (column + 1) * PATTERN_CELL, (row + 1) * PATTERN_CELL, _CELL_COLORS[cell])
    return canvas


def fixtures() -> dict:
    """name -> {png, facts}; the facts are what the device checks expect, in file pixels."""
    small_code = ean13(4, 200)
    small = Canvas(900, 300)
    small.paste(small_code, 216, 50)
    # 5000 px wide: AutoJs6 decodes it at half size for model sessions (roadmap D45).
    big_code = ean13(8, 400)
    big = Canvas(5000, 1000)
    big.paste(big_code, 2000, 300)

    colors = Canvas(900, 300)
    colors.fill(600, 100, 620, 120, RED)
    colors.fill(620, 100, 640, 120, BLUE)
    big_colors = Canvas(5000, 1000)
    big_colors.fill(3000, 500, 3040, 540, RED)

    template = pattern()
    scene = Canvas(900, 300)
    scene.paste(template, 300, 120)
    big_scene = Canvas(5000, 1000)
    big_scene.paste(template, 3000, 500)
    return {
        "ean13.png": {"png": small.png(), "center": (216 + small_code.width // 2, 50 + 100)},
        "ean13-5000.png": {"png": big.png(), "center": (2000 + big_code.width // 2, 300 + 200)},
        "colors.png": {"png": colors.png(), "red": (600, 100)},
        "colors-5000.png": {"png": big_colors.png(), "red": (3000, 500)},
        "pattern.png": {"png": template.png(), "size": (template.width, template.height)},
        "scene.png": {"png": scene.png(), "center": (300 + template.width // 2, 120 + template.height // 2)},
        "scene-5000.png": {"png": big_scene.png()},
    }

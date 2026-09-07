# -*- coding: utf-8 -*-
"""Render the MCP Server launcher icons.

Outputs (all RGBA PNG, regenerated deterministically from this script):
  app/src/main/res/mipmap/ic_launcher.png              432 x 432 legacy icon (rounded square)
  app/src/main/res/mipmap/ic_launcher_round.png        192 x 192 legacy round icon
  app/src/main/res/mipmap/ic_launcher_foreground.png   432 x 432 adaptive foreground (white glyph)
  app/src/main/res/mipmap/ic_launcher_monochrome.png   432 x 432 adaptive monochrome (black glyph)
  app/src/main/res/mipmap-night/*.png                  same set on the night background

The glyph is a rounded "node" frame with the letters MCP and three connector ports, so the
icon reads as "an MCP endpoint" at launcher and plugin-center sizes. Background colors match
values/ic_launcher_background.xml and values-night/ic_launcher_background.xml.

Usage: py .python/generate_launcher_icons.py
"""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app" / "src" / "main" / "res"
SIZE = 432
ROUND_SIZE = 192
SCALE = 4

DAY_BACKGROUND = (0x0F, 0x76, 0x6E, 255)
NIGHT_BACKGROUND = (0x11, 0x5E, 0x59, 255)
GLYPH_WHITE = (255, 255, 255, 255)
GLYPH_BLACK = (0, 0, 0, 255)
TRANSPARENT = (0, 0, 0, 0)

FONT_CANDIDATES = (
    "C:/Windows/Fonts/segoeuib.ttf",
    "C:/Windows/Fonts/arialbd.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
)


def load_font(size: int) -> ImageFont.FreeTypeFont:
    for candidate in FONT_CANDIDATES:
        if Path(candidate).is_file():
            return ImageFont.truetype(candidate, size)
    raise SystemExit("No bold TrueType font found; add a candidate path to FONT_CANDIDATES")


def draw_glyph(draw: ImageDraw.ImageDraw, center: tuple[float, float], box: float, color: tuple[int, int, int, int]) -> None:
    cx, cy = center
    half = box / 2
    stroke = box * 0.075
    radius = box * 0.2
    draw.rounded_rectangle(
        (cx - half, cy - half, cx + half, cy + half),
        radius=radius,
        outline=color,
        width=int(round(stroke)),
    )

    font = load_font(int(box * 0.34))
    text = "MCP"
    left, top, right, bottom = draw.textbbox((0, 0), text, font=font)
    text_width = right - left
    text_height = bottom - top
    text_x = cx - text_width / 2 - left
    text_y = cy - box * 0.09 - text_height / 2 - top
    draw.text((text_x, text_y), text, font=font, fill=color)

    port_radius = box * 0.045
    port_y = cy + box * 0.27
    for offset in (-box * 0.17, 0.0, box * 0.17):
        draw.ellipse(
            (cx + offset - port_radius, port_y - port_radius, cx + offset + port_radius, port_y + port_radius),
            fill=color,
        )


def render(
    size: int,
    background: tuple[int, int, int, int] | None,
    mask: str | None,
    glyph_color: tuple[int, int, int, int],
    glyph_ratio: float,
) -> Image.Image:
    scaled = size * SCALE
    canvas = Image.new("RGBA", (scaled, scaled), TRANSPARENT)
    draw = ImageDraw.Draw(canvas)
    if background is not None:
        if mask == "circle":
            draw.ellipse((0, 0, scaled - 1, scaled - 1), fill=background)
        elif mask == "rounded":
            draw.rounded_rectangle((0, 0, scaled - 1, scaled - 1), radius=scaled * 0.2, fill=background)
        else:
            draw.rectangle((0, 0, scaled - 1, scaled - 1), fill=background)
    draw_glyph(draw, (scaled / 2, scaled / 2), scaled * glyph_ratio, glyph_color)
    return canvas.resize((size, size), Image.LANCZOS)


def write(image: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, format="PNG", optimize=True)
    print(f"Generated {path.relative_to(ROOT).as_posix()} {image.size[0]}x{image.size[1]}")


def main() -> None:
    for directory, background in (("mipmap", DAY_BACKGROUND), ("mipmap-night", NIGHT_BACKGROUND)):
        target = RES / directory
        write(render(SIZE, background, "rounded", GLYPH_WHITE, 0.68), target / "ic_launcher.png")
        write(render(ROUND_SIZE, background, "circle", GLYPH_WHITE, 0.62), target / "ic_launcher_round.png")
        # Adaptive layers: the glyph stays inside the 66% safe zone of the 108 dp canvas.
        write(render(SIZE, None, None, GLYPH_WHITE, 0.54), target / "ic_launcher_foreground.png")
        write(render(SIZE, None, None, GLYPH_BLACK, 0.54), target / "ic_launcher_monochrome.png")


if __name__ == "__main__":
    main()

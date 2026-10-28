#!/usr/bin/env python3
"""Generate every RSETSparks icon, splash and store asset from the master logo.

Source of truth: branding/rsetsparks-logo.svg. Its 2048px render lives in
branding/rsetsparks-logo-2048.png; re-render it after changing the SVG with:

    NODE_PATH=$(npm root -g) node scripts/render_svg.js \
        branding/rsetsparks-logo.svg branding/rsetsparks-logo-2048.png 2048

Then run (needs Pillow):

    python3 scripts/generate_brand_assets.py
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
MASTER = ROOT / "branding" / "rsetsparks-logo-2048.png"
RES = ROOT / "app" / "src" / "main" / "res"
STORE = ROOT / "store" / "graphics"

CREAM = (250, 247, 242, 255)
BURGUNDY = (111, 10, 36, 255)
DEEP_WINE = (74, 5, 23, 255)
ROSE = (215, 72, 100, 255)

# The crest's outer ring is a circle centred in the artwork with radius ~0.473 * width.
CREST_RADIUS = 0.473

DENSITIES = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}


def load_crest() -> Image.Image:
    """Master logo with everything outside the outer ring made transparent."""
    src = Image.open(MASTER).convert("RGBA")
    size = src.width
    mask = Image.new("L", src.size, 0)
    r = CREST_RADIUS * size
    c = size / 2
    ImageDraw.Draw(mask).ellipse((c - r, c - r, c + r, c + r), fill=255)
    src.putalpha(mask)
    return src.crop(src.getbbox())


def fit(img: Image.Image, px: int) -> Image.Image:
    return img.resize((px, px), Image.LANCZOS)


def on_canvas(img: Image.Image, canvas_px: int, content_px: int, bg=(0, 0, 0, 0)) -> Image.Image:
    canvas = Image.new("RGBA", (canvas_px, canvas_px), bg)
    f = fit(img, content_px)
    canvas.alpha_composite(f, ((canvas_px - content_px) // 2, (canvas_px - content_px) // 2))
    return canvas


def monochrome(img: Image.Image) -> Image.Image:
    """White silhouette whose alpha follows the ink (non-cream) parts of the crest.

    Android tints the monochrome layer for themed icons, so only alpha matters.
    """
    rgba = img.convert("RGBA")
    out = Image.new("RGBA", rgba.size, (255, 255, 255, 0))
    src_px = rgba.load()
    out_px = out.load()
    for y in range(rgba.height):
        for x in range(rgba.width):
            r, g, b, a = src_px[x, y]
            # Distance from cream: cream -> 0, burgundy/rose -> ~255.
            ink = max(0, min(255, int((745 - (r + g + b)) * 255 / 420)))
            out_px[x, y] = (255, 255, 255, ink * a // 255)
    return out


def save_webp(img: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path, "WEBP", lossless=True, quality=100, method=6)


def android_assets(crest: Image.Image) -> None:
    mono_src = monochrome(fit(crest, 1024))
    for name, scale in DENSITIES.items():
        # Adaptive icon layers: 108dp canvas, crest at 70dp (fills the visible 72dp mask).
        canvas = round(108 * scale)
        content = round(70 * scale)
        save_webp(on_canvas(crest, canvas, content), RES / f"mipmap-{name}" / "ic_launcher_foreground.webp")
        save_webp(on_canvas(mono_src, canvas, content), RES / f"mipmap-{name}" / "ic_launcher_monochrome.webp")

        # Legacy (pre-API 26) icons: 48dp, crest on cream.
        legacy = round(48 * scale)
        square = Image.new("RGBA", (legacy, legacy), (0, 0, 0, 0))
        ImageDraw.Draw(square).rounded_rectangle(
            (0, 0, legacy - 1, legacy - 1), radius=round(legacy * 0.18), fill=CREAM
        )
        square.alpha_composite(fit(crest, round(legacy * 0.92)), (round(legacy * 0.04),) * 2)
        save_webp(square, RES / f"mipmap-{name}" / "ic_launcher.webp")
        round_icon = Image.new("RGBA", (legacy, legacy), (0, 0, 0, 0))
        ImageDraw.Draw(round_icon).ellipse((0, 0, legacy - 1, legacy - 1), fill=CREAM)
        round_icon.alpha_composite(fit(crest, legacy))
        save_webp(round_icon, RES / f"mipmap-{name}" / "ic_launcher_round.webp")

        # In-app crest (used at up to ~160dp).
        save_webp(fit(crest, round(160 * scale)), RES / f"drawable-{name}" / "ic_rset_sparks_logo.webp")

        # Android 12+ splash icon: 240dp canvas, content must sit inside a 160dp circle.
        save_webp(on_canvas(crest, round(240 * scale), round(156 * scale)), RES / f"drawable-{name}" / "splash_logo.webp")


def store_assets(crest: Image.Image) -> None:
    STORE.mkdir(parents=True, exist_ok=True)

    # Google Play hi-res icon: 512x512 32-bit PNG, full bleed (Play applies its own mask).
    icon = on_canvas(crest, 512, 472, CREAM)
    icon.save(STORE / "play-icon-512.png")

    # App Store icon: 1024x1024, no transparency allowed.
    on_canvas(crest, 1024, 944, CREAM).convert("RGB").save(STORE / "appstore-icon-1024.png")

    # Google Play feature graphic: 1024x500.
    fg = Image.new("RGBA", (1024, 500), BURGUNDY)
    d = ImageDraw.Draw(fg)
    for i in range(500):  # soft vertical gradient burgundy -> deep wine
        t = i / 499
        col = tuple(round(BURGUNDY[k] + (DEEP_WINE[k] - BURGUNDY[k]) * t) for k in range(3)) + (255,)
        d.line((0, i, 1024, i), fill=col)
    d.ellipse((50, 40, 470, 460), fill=CREAM)
    fg.alpha_composite(fit(crest, 400), (60, 50))
    serif_bold = "/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf"
    sans = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
    title = ImageFont.truetype(serif_bold, 70)
    tag = ImageFont.truetype(sans, 28)
    small = ImageFont.truetype(sans, 24)
    d.text((515, 150), "RSET", font=title, fill=CREAM)
    rset_w = d.textlength("RSET", font=title)
    d.text((515 + rset_w, 150), "Sparks", font=title, fill=ROSE)
    d.text((518, 248), "Learn • Connect • Excel", font=tag, fill=CREAM)
    d.text((518, 300), "Meet verified Rajagiri students.", font=small, fill=(243, 196, 205, 255))
    fg.convert("RGB").save(STORE / "play-feature-graphic-1024x500.png")


def main() -> None:
    crest = load_crest()
    android_assets(crest)
    store_assets(crest)
    print("Brand assets generated.")


if __name__ == "__main__":
    main()

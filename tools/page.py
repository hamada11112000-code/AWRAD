#!/usr/bin/env python3
"""Crop and zoom part of a scanned book page for close reading.

Usage: python3 tools/page.py PAGE [--from 0.0] [--to 1.0] [--scale 2]
--from/--to are vertical fractions of the page height (0 = top, 1 = bottom).
Prints the path of the zoomed PNG.
"""
import argparse
import os
import sys

from PIL import Image, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PAGES = os.path.join(ROOT, "content", "pages")
OUT = os.path.join(PAGES, "zoom")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("page", type=int)
    ap.add_argument("--from", dest="top", type=float, default=0.0)
    ap.add_argument("--to", dest="bottom", type=float, default=1.0)
    ap.add_argument("--scale", type=float, default=2.0)
    a = ap.parse_args()
    src = os.path.join(PAGES, f"p-{a.page:03d}.png")
    if not os.path.exists(src):
        sys.exit(f"no such page image: {src}")
    im = Image.open(src)
    w, h = im.size
    box = (0, int(h * max(0.0, a.top)), w, int(h * min(1.0, a.bottom)))
    crop = im.crop(box)
    crop = crop.resize((int(crop.width * a.scale), int(crop.height * a.scale)), Image.LANCZOS)
    crop = crop.filter(ImageFilter.SHARPEN)
    os.makedirs(OUT, exist_ok=True)
    dst = os.path.join(OUT, f"p-{a.page:03d}_{a.top:.2f}-{a.bottom:.2f}_x{a.scale:g}.png")
    crop.save(dst)
    print(dst)


if __name__ == "__main__":
    main()

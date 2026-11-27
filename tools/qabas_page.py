#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
يكبّر جزءًا من صفحة الطبعة الصغيرة المشكولة «قبس-مختار.pdf» ليُقرأ تشكيلها.

    python3 tools/qabas_page.py 81                    # الصفحة كاملة
    python3 tools/qabas_page.py 81 --from .4 --to .8  # الجزء الأوسط
    python3 tools/qabas_page.py 81 --scale 3          # تكبير أعلى

يطبع مسار الصورة، فاقرأها بأداة Read. الصور في `content/qabas/pages`
(رابط إلى مجلد المسودة، ومستثناة من git).
"""
import argparse, subprocess, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PDF = ROOT / "قبس-مختار.pdf"
OUT = ROOT / "content" / "qabas" / "pages"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("page", type=int, help="رقم صفحة الـPDF (١-١٩٢)")
    ap.add_argument("--from", dest="y0", type=float, default=0.0, help="بداية القص عموديًّا ٠-١")
    ap.add_argument("--to", dest="y1", type=float, default=1.0, help="نهايته ٠-١")
    ap.add_argument("--scale", type=float, default=2.0, help="مضاعف التكبير")
    ap.add_argument("--dpi", type=int, default=300)
    a = ap.parse_args()
    if not 1 <= a.page <= 192:
        sys.exit("رقم الصفحة خارج الكتاب (١-١٩٢).")
    OUT.mkdir(parents=True, exist_ok=True)
    base = OUT / f"hi-{a.page:03d}"
    src = Path(f"{base}-{a.page:03d}.png")
    if not src.exists():
        subprocess.run(["pdftoppm", "-r", str(a.dpi), "-gray", "-png",
                        "-f", str(a.page), "-l", str(a.page), str(PDF), str(base)],
                       check=True)
        cand = sorted(OUT.glob(f"hi-{a.page:03d}-*.png"))
        if not cand:
            sys.exit("فشل توليد الصورة.")
        src = cand[0]
    try:
        from PIL import Image
    except ImportError:
        print(src); return
    im = Image.open(src)
    w, h = im.size
    y0, y1 = max(0.0, a.y0), min(1.0, a.y1)
    if y1 <= y0:
        sys.exit("--to يجب أن تكون أكبر من --from")
    crop = im.crop((0, int(h * y0), w, int(h * y1)))
    if a.scale != 1.0:
        crop = crop.resize((int(crop.width * a.scale), int(crop.height * a.scale)))
    out = OUT / f"view-{a.page:03d}-{int(y0*100)}-{int(y1*100)}.png"
    crop.save(out)
    print(out)


if __name__ == "__main__":
    main()

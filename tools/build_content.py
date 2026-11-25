#!/usr/bin/env python3
"""Builds app/src/main/assets/azkar.json from the reviewed transcription.

Inputs
  content/raw/batch_*.json        verbatim transcription of the book (see content/GUIDELINES.md)
  content/editorial/titles.json   {"<book heading>": {"title": "<app title>", "featured": "<key>|null"}}
  content/editorial/items.json    {"<hadith num>": {"zikr": "...", "fadl": "...", "count": N}}

Safety rules enforced here (the build fails if any is broken):
  * every zikr and every fadl must be a verbatim excerpt of that hadith's matn/extra
    (compared letter by letter after only collapsing spaces and punctuation);
  * counts must be positive integers;
  * every editorial key must point at an existing heading / hadith.

Usage: python3 tools/build_content.py [--allow-missing-editorial]
"""
import glob
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW = os.path.join(ROOT, "content", "raw")
ED = os.path.join(ROOT, "content", "editorial")
OUT = os.path.join(ROOT, "app", "src", "main", "assets", "azkar.json")

HARAKAT = re.compile("[\u064B-\u0652\u0670]")
PUNCT = re.compile("[\\s«»﴿﴾()\\[\\]،.:؟؛!\\-…\"']+")


def letters(s):
    """Only the letters, in order: punctuation and spacing differences are ignored, nothing else."""
    return PUNCT.sub("", s)


def load(path, default):
    if os.path.exists(path):
        with open(path, encoding="utf-8") as f:
            return json.load(f)
    return default


def default_title(heading):
    t = heading
    if t.startswith("باب "):
        t = t[4:]
    return t.strip()


def main():
    allow_missing = "--allow-missing-editorial" in sys.argv
    titles = load(os.path.join(ED, "titles.json"), {})
    editorial = load(os.path.join(ED, "items.json"), {})
    errors = []

    batches = sorted(glob.glob(os.path.join(RAW, "batch_*.json")))
    if not batches:
        sys.exit("no raw batches found")

    categories = []
    cur = None
    sub = ""
    seen_headings = set()
    seen_nums = set()
    extra_counter = 0

    for path in batches:
        data = json.load(open(path, encoding="utf-8"))
        for sec in data["sections"]:
            heading = sec["heading"]
            if sec["level"] == 1:
                seen_headings.add(heading)
                if heading == "بين يدي الكتاب":
                    cur = None
                    continue
                if sec.get("continued") and cur is not None and cur["bookTitle"] == heading:
                    pass  # same chapter continues in this batch
                else:
                    meta = titles.get(heading, {})
                    if not meta and not allow_missing:
                        errors.append(f"no app title for chapter «{heading}» in content/editorial/titles.json")
                    cur = {
                        "id": f"c{len(categories) + 1:03d}",
                        "title": meta.get("title") or default_title(heading),
                        "bookTitle": heading,
                        "page": sec["page"],
                        "featured": meta.get("featured"),
                        "items": [],
                    }
                    categories.append(cur)
                sub = ""
            else:
                seen_headings.add(heading)
                sub = heading
            if cur is None:
                continue
            for e in sec["entries"]:
                t = e["type"]
                if t == "hadith":
                    num = e["num"]
                    seen_nums.add(str(num))
                    ed = editorial.get(str(num), {})
                    zikr = ed.get("zikr", "")
                    fadl = ed.get("fadl", "")
                    count = ed.get("count", 1 if zikr else 0)
                    source_text = letters(e.get("matn", "") + " " + e.get("extra", ""))
                    voc = ed.get("zikrVocalized", "")
                    if voc and letters(HARAKAT.sub("", voc)) != letters(zikr):
                        errors.append(f"#{num}: vocalized zikr changes letters, not only diacritics")
                    if zikr and letters(zikr) not in source_text:
                        errors.append(f"#{num}: zikr is not a verbatim excerpt of the matn: «{zikr[:60]}»")
                    for part in [p for p in fadl.split(" … ")] if fadl else []:
                        if letters(part) not in source_text:
                            errors.append(f"#{num}: fadl is not a verbatim excerpt of the matn: «{part[:60]}»")
                    if not isinstance(count, int) or count < 0 or (zikr and count < 1):
                        errors.append(f"#{num}: bad count {count!r}")
                    cur["items"].append({
                        "id": f"h{num}",
                        "type": "hadith",
                        "num": num,
                        "page": e["page"],
                        "subheading": sub,
                        "zikr": voc or zikr,
                        "count": count if zikr else 0,
                        "fadl": fadl,
                        "matn": e.get("matn", ""),
                        "isnad": e.get("isnad", ""),
                        "source": e.get("source", ""),
                        "extra": e.get("extra", ""),
                        "grade": e.get("grade", ""),
                        "takhrij": e.get("takhrij", ""),
                        "notes": e.get("notes", []),
                    })
                else:
                    extra_counter += 1
                    cur["items"].append({
                        "id": f"{'a' if t == 'ayah' else 't'}{extra_counter}",
                        "type": t,
                        "page": e["page"],
                        "subheading": sub,
                        "text": e.get("text", ""),
                        "ref": e.get("ref", ""),
                    })

    for h in titles:
        if h not in seen_headings:
            errors.append(f"titles.json names a chapter that is not in the transcription: «{h}»")
    for n in editorial:
        if n not in seen_nums:
            errors.append(f"items.json names hadith #{n}, which is not in the transcription")

    categories = [c for c in categories if c["items"]]
    if errors:
        print("\n".join("ERROR " + e for e in errors))
        sys.exit(1)

    lib = {
        "version": 1,
        "book": {"title": "الصحيح المسند من أذكار اليوم والليلة", "author": "مصطفى بن العدوي"},
        "categories": categories,
    }
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(lib, f, ensure_ascii=False, separators=(",", ":"))
    n_items = sum(len(c["items"]) for c in categories)
    n_zikr = sum(1 for c in categories for i in c["items"] if i.get("zikr"))
    print(f"wrote {OUT}: {len(categories)} chapters, {n_items} entries, {n_zikr} with a recitable zikr")


if __name__ == "__main__":
    main()

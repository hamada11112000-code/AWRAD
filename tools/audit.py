#!/usr/bin/env python3
"""Mechanical auditor for the transcribed book content.

Usage:
  python3 tools/audit.py content/raw/batch_06.json [more files...]
  python3 tools/audit.py --all            # every raw batch, plus cross-batch checks

It cannot judge whether a word matches the scan (the reviewing agents do that).
It catches everything a machine can: bad characters, diacritics, Latin digits,
punctuation spacing, unbalanced brackets, broken hadith numbering, page order,
missing fields and empty grades or takhrij.
Exit code 1 when any ERROR is found.
"""
import glob
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

HARAKAT = re.compile("[ً-ْٰٓ-ٟۖ-ۭ]")
TATWEEL = "ـ"
ALLOWED = re.compile(
    "^[ء-غف-ي"  # Arabic letters
    "٠-٩"                   # Arabic-Indic digits
    "ﷺ"                          # ﷺ
    " «»﴿﴾()\\[\\]،.:؟؛!\\-/…\"']*$"
)
LATIN = re.compile("[A-Za-z0-9]")
SPACE_BEFORE_PUNCT = re.compile(" [،.:؟؛!»)﴾\\]]")
SPACE_AFTER_OPEN = re.compile("[«(﴿\\[] ")
PUNCT_NO_SPACE = re.compile("[،:؛؟](?=[ء-ي«﴿(])")
PAIRS = [("«", "»"), ("﴿", "﴾"), ("(", ")"), ("[", "]")]
GRADES_OK = {
    "صحيح", "حسن", "صحيح لغيره", "حسن لغيره", "ضعيف", "صحيح موقوف", "حسن موقوف",
    "صحيح مرسل", "صحيح إلى", "إسناده صحيح", "إسناده حسن", "صحيح بشواهده", "حسن بشواهده",
}
TEXT_FIELDS = ["source", "isnad", "matn", "extra", "grade", "takhrij", "text", "ref", "heading"]

errors = 0
warnings = 0


def err(loc, msg):
    global errors
    errors += 1
    print(f"ERROR {loc}: {msg}")


def warn(loc, msg):
    global warnings
    warnings += 1
    print(f"WARN  {loc}: {msg}")


def check_text(loc, field, s):
    if not isinstance(s, str):
        err(loc, f"{field} is not a string")
        return
    if s != s.strip():
        err(loc, f"{field} has leading/trailing whitespace")
    if "  " in s:
        err(loc, f"{field} has a double space")
    if "\n" in s or "\t" in s:
        err(loc, f"{field} contains a newline/tab")
    if HARAKAT.search(s):
        bad = sorted({f"U+{ord(c):04X}" for c in HARAKAT.findall(s)})
        err(loc, f"{field} contains diacritics {bad} (raw text must be undiacritized)")
    if TATWEEL in s:
        err(loc, f"{field} contains tatweel")
    if LATIN.search(s):
        err(loc, f"{field} contains Latin letters/digits: {LATIN.findall(s)[:5]}")
    stripped = HARAKAT.sub("", s).replace(TATWEEL, "")
    if not ALLOWED.match(stripped):
        bad = sorted({c for c in stripped if not ALLOWED.match(c)})
        err(loc, f"{field} has unexpected characters {[f'{c} U+{ord(c):04X}' for c in bad]}")
    m = SPACE_BEFORE_PUNCT.search(s)
    if m:
        err(loc, f"{field} has a space before punctuation near «{s[max(0, m.start()-12):m.end()+4]}»")
    m = SPACE_AFTER_OPEN.search(s)
    if m:
        err(loc, f"{field} has a space after an opening bracket near «{s[max(0, m.start()-8):m.end()+12]}»")
    m = PUNCT_NO_SPACE.search(s)
    if m:
        warn(loc, f"{field} missing space after punctuation near «{s[max(0, m.start()-10):m.end()+10]}»")
    for o, c in PAIRS:
        if s.count(o) != s.count(c):
            err(loc, f"{field} unbalanced {o}{c}: {s.count(o)} vs {s.count(c)}")
    if "ﷺ" not in s and re.search("(النبي|رسول الله) صلى الله عليه وسلم", s):
        warn(loc, f"{field} writes the salutation in words; the book uses the glyph -> ﷺ (check the scan)")


def audit_file(path, state):
    try:
        data = json.load(open(path, encoding="utf-8"))
    except Exception as e:  # noqa: BLE001
        err(path, f"invalid JSON: {e}")
        return
    name = os.path.basename(path)
    pages = data.get("pages")
    if not (isinstance(pages, list) and len(pages) == 2):
        err(name, "missing 'pages': [first, last]")
        pages = [0, 10**6]
    lo, hi = pages
    last_page = 0
    secs = data.get("sections")
    if not isinstance(secs, list) or not secs:
        err(name, "no sections")
        return
    for si, sec in enumerate(secs):
        sloc = f"{name} section {si} «{sec.get('heading', '?')}»"
        for k in ("heading", "level", "page", "entries"):
            if k not in sec:
                err(sloc, f"missing '{k}'")
        check_text(sloc, "heading", sec.get("heading", ""))
        if sec.get("level") not in (1, 2):
            err(sloc, "level must be 1 or 2")
        if sec.get("level") == 1 and not sec.get("heading", "").startswith("باب") \
                and sec.get("heading") != "بين يدي الكتاب":
            warn(sloc, "level-1 heading that does not start with «باب»")
        if not sec.get("entries"):
            warn(sloc, "section has no entries")
        for ei, e in enumerate(sec.get("entries", [])):
            t = e.get("type")
            loc = f"{name} p{e.get('page', '?')} " + (f"#{e.get('num')}" if t == "hadith" else f"{t}[{si}.{ei}]")
            pg = e.get("page")
            if not isinstance(pg, int):
                err(loc, "missing integer page")
            else:
                if pg < lo or pg > hi + 1:
                    err(loc, f"page {pg} outside batch range {lo}-{hi}")
                if pg < last_page:
                    err(loc, f"page goes backwards ({last_page} -> {pg})")
                last_page = max(last_page, pg)
            for f in TEXT_FIELDS:
                if f in e:
                    check_text(loc, f, e[f])
            for i, n in enumerate(e.get("notes", []) or []):
                check_text(loc, f"notes[{i}]", n)
            for fl in e.get("flags", []) or []:
                state["flags"].append(f"{loc}: {fl}")
            if t == "hadith":
                for k in ("num", "source", "isnad", "matn", "grade", "takhrij"):
                    if k not in e:
                        err(loc, f"missing '{k}'")
                num = e.get("num")
                if not isinstance(num, int):
                    err(loc, "num must be an integer")
                else:
                    if num in state["nums"]:
                        err(loc, f"duplicate hadith number (also in {state['nums'][num]})")
                    state["nums"][num] = loc
                    if state["prev"] is not None and num != state["prev"] + 1:
                        err(loc, f"numbering jumps {state['prev']} -> {num}")
                    state["prev"] = num
                if not e.get("matn"):
                    err(loc, "empty matn")
                if not e.get("grade"):
                    warn(loc, "empty grade")
                elif e["grade"] not in GRADES_OK:
                    warn(loc, f"unusual grade «{e['grade']}» (verify against the scan)")
                if not e.get("takhrij"):
                    warn(loc, "empty takhrij")
                if e.get("source") and not e["source"].startswith(("قال", "وقال", "روى", "أخرج")):
                    warn(loc, f"source does not start with «قال»: «{e['source'][:30]}»")
            elif t == "ayah":
                txt = e.get("text", "")
                if not (txt.startswith("﴿") and txt.endswith("﴾")):
                    err(loc, "ayah text must be wrapped in ﴿ ﴾")
            elif t == "text":
                if not e.get("text"):
                    err(loc, "empty text")
            else:
                err(loc, f"unknown entry type {t!r}")


def main():
    args = sys.argv[1:]
    if not args:
        print(__doc__)
        sys.exit(2)
    if args == ["--all"]:
        args = sorted(glob.glob(os.path.join(ROOT, "content", "raw", "batch_*.json")))
    state = {"nums": {}, "prev": None, "flags": []}
    multi = len(args) > 1
    for p in args:
        if not multi:
            state["prev"] = None
        audit_file(p, state)
    if state["flags"]:
        print(f"\n{len(state['flags'])} flags for human/agent review:")
        for f in state["flags"]:
            print("  FLAG", f)
    nums = sorted(state["nums"])
    if nums:
        print(f"\nhadiths: {len(nums)} (#{nums[0]}..#{nums[-1]})")
    print(f"{errors} errors, {warnings} warnings")
    sys.exit(1 if errors else 0)


if __name__ == "__main__":
    main()

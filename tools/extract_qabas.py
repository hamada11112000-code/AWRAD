#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
استخراج آلي لنص «قبس مختار من صحيح الأذكار» من نسخة A4 (2abs_mokhtar.pdf).

بلا أي تكلفة ذكاء: pdftotext + تطبيع + تقسيم + إصلاح العيوب الآلية المؤكدة فقط.
كل ما ليس مؤكدًا يُسجَّل في flags ولا يُخمَّن بصمت (راجع content/GUIDELINES.md).

المخرجات:
  content/qabas/raw.json   الأبواب والأحاديث مع الحكم والرمز والصفحة والأعلام
  content/qabas/FLAGS.md   تقرير بكل ما يحتاج مطابقة بالصورة

التشغيل:
  python3 tools/extract_qabas.py
  python3 tools/extract_qabas.py --report      # ملخص على الشاشة بلا كتابة
"""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
import unicodedata
from dataclasses import dataclass, field, asdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PDF = ROOT / "2abs_mokhtar.pdf"
OUT_JSON = ROOT / "content" / "qabas" / "raw.json"
OUT_FLAGS = ROOT / "content" / "qabas" / "FLAGS.md"

INDEX_PAGES = range(2, 9)   # ٢-٨ فهرس العناوين
BODY_START = 9              # المتن من ٩ (ص٨ مقدمة المؤلف، وليست من الأذكار)

HARAKAT = "ً-ْٰۖ-ۭ"
RUNNING_HEAD = "قبس مختار من صحيح الأذكار"

# ───────────────────────── رموز المصادر ─────────────────────────
# ⚠ مقروءة من صورة صغيرة في ص٧ من الطبعة الصغيرة. تحقق منها قبل الاعتماد.
SOURCE_SYMBOLS = {
    "خ": "البخاري", "م": "مسلم", "ق": "متفق عليه", "د": "أبو داود",
    "ت": "الترمذي", "ن": "النسائي", "جه": "ابن ماجه", "حم": "أحمد",
    "دي": "الدارمي", "حب": "ابن حبان", "ط": "الطبراني", "ع": "أبو يعلى",
    "ك": "الحاكم", "بي": "البيهقي", "ج": "الجماعة",
}
SOURCE_SYMBOLS_VERIFIED = False   # يصير True بعد التحقق من ص٧ بالصورة


@dataclass
class Flag:
    kind: str
    page: int
    detail: str
    context: str = ""


@dataclass
class Entry:
    n: int
    chapter: str
    page: int
    matn: str
    grade: str = ""
    symbols: list = field(default_factory=list)
    sources: list = field(default_factory=list)
    unknown_symbols: list = field(default_factory=list)
    footnotes: list = field(default_factory=list)
    flags: list = field(default_factory=list)


# ───────────────────────── ١. الاستخراج ─────────────────────────
def extract_pages(pdf: Path) -> list[str]:
    try:
        raw = subprocess.run(
            ["pdftotext", "-enc", "UTF-8", str(pdf), "-"],
            check=True, capture_output=True,
        ).stdout.decode("utf-8")
    except FileNotFoundError:
        sys.exit("pdftotext غير مثبت (poppler-utils).")
    except subprocess.CalledProcessError as e:
        sys.exit(f"فشل pdftotext: {e.stderr.decode('utf-8', 'replace')[:300]}")
    return raw.split("\f")


# ───────────────────────── ٢. التطبيع ─────────────────────────
def normalize(text: str) -> str:
    """تطبيع آمن لا يغيّر المعنى: ترميز الخط والاتجاه والمسافات."""
    text = unicodedata.normalize("NFKC", text)
    text = re.sub("[‪-‮‎‏⁦-⁩]", "", text)
    # ترميز الخط: محارف فارسية/أردية من الخط لا من الكتاب
    text = text.replace("ی", "ي")   # ی → ي
    text = text.replace("ھ", "ه")   # ھ → ه
    text = text.replace("ک", "ك")   # ک → ك
    text = text.replace("ہ", "ه")   # ہ → ه
    # «االله» بألفين → «الله» (عيب ترميز مؤكد، ٦٧٢ موضعًا)
    text = text.replace("االله", "الله")
    # مسافة قبل علامة ترقيم → بعدها
    text = re.sub(r" +([،؛؟!\.:])", r"\1 ", text)
    # علامة الحاشية المقلوبة: «) (1» → «(1)»
    text = re.sub(r"\)\s*\((\d+)", r"(\1)", text)
    return re.sub(r"[ \t]+", " ", text)


def skeleton(s: str) -> str:
    """مفتاح مقارنة: بلا حركات ولا ترقيم، مع تطبيع الألف والياء والهاء."""
    s = re.sub(f"[{HARAKAT}ـ]", "", s)
    s = re.sub("[آأإا]", "ا", s)
    s = s.replace("ى", "ي").replace("ة", "ه")
    s = re.sub(r"[^ء-ي ]", " ", s)
    return re.sub(r"\s+", " ", s).strip()


# ───────────────────────── ٣. الفهرس ─────────────────────────
ORPHAN_TAIL = re.compile(f"\\s[{HARAKAT}]+$")


def repair_titles(lines: list[str]) -> tuple[list[str], set[str]]:
    """
    pdftotext يفصل الكلمة المنوَّنة عن عنوانها ويقدّمها سطرًا.
    مثال: «خاليا» ثم «فضل الذكر ً» صوابهما «فضل الذكر خاليا».
    نعيد ترتيب الكلمات فقط ونحذف التنوين المعلَّق،
    لأن التشكيل يؤخذ من الطبعة المشكولة ولا يُخترع.
    """
    stack: list[str] = []
    rebuilt: set[str] = set()
    for s in lines:
        touched = False
        while ORPHAN_TAIL.search(s) and stack:
            s = ORPHAN_TAIL.sub("", s).rstrip()
            s = f"{s} {stack.pop()}"
            touched = True
        s = ORPHAN_TAIL.sub("", s).rstrip()
        stack.append(s)
        if touched:
            rebuilt.add(s)
    return stack, {t for t in rebuilt if t in set(stack)}


def repair_matn_lines(lines: list[str]) -> tuple[list[str], list[dict]]:
    """
    نفس عيب الفهرس يقع في المتن: الكلمة المنوَّنة تُقدَّم سطرًا فيختلّ ترتيب
    الكلام. مثال ص١٠: «خاليا ففاضت عيناه» قبل «ظل إلا ظله ...ورجل ذكر الله ً»
    وصوابه «... ظل إلا ظله ... ورجل ذكر الله خاليا ففاضت عيناه».

    يُطبَّق فقط على سطر شبه مجرَّد من التشكيل (حركتان على الأكثر)، لأن آيات
    الطبعة مشكولة بالكامل وحركاتها تنتهي بها السطور طبعًا لا خللًا.
    """
    out: list[str] = []
    notes: list[dict] = []
    for ln in lines:
        if ORPHAN_TAIL.search(ln) and len(re.findall(f"[{HARAKAT}]", ln)) <= 2 and out:
            base = ORPHAN_TAIL.sub("", ln).rstrip()
            moved = out.pop()
            out.append(f"{base} {moved}")
            notes.append(asdict(Flag(
                "LINE_ORDER_REPAIRED", 0,
                "أُعيد ترتيب سطر قدّمه الاستخراج — طابِق الترتيب بالصورة",
                f"{moved[:50]} ⇠ {base[-50:]}")))
        else:
            if ORPHAN_TAIL.search(ln):
                notes.append(asdict(Flag(
                    "VOCALIZED_SCRAMBLE", 0,
                    "سطر مشكول اختلّ ترتيبه — لا يُصلَح آليًّا، طابِقه بالصورة",
                    ln[:70])))
            out.append(ln)
    return out, notes


def parse_index(pages: list[str]) -> tuple[list[str], set[str]]:
    lines: list[str] = []
    for pno in INDEX_PAGES:
        if pno - 1 >= len(pages):
            continue
        for line in normalize(pages[pno - 1]).split("\n"):
            s = line.strip().strip("·").strip()
            if not s:
                continue
            # نهاية الفهرس: الترويسة الجارية أو عنوان المقدمة
            if skeleton(s) == skeleton(RUNNING_HEAD) or skeleton(s) == skeleton("المقدمة"):
                return repair_titles(lines)
            lines.append(s)
    return repair_titles(lines)


# ───────────────────────── ٤. أعلام النص ─────────────────────────
PRIVATE_USE = re.compile(r"[-]")
HAMD_FIX = re.compile(r"(ال|وال)حمد الله")
LILLAH_SUSPECT = re.compile(r"(?<!ال)(?:إن|التحيات|مماتي|ومماتي)\s+الله")


def scan_flags(text: str, page: int) -> list[Flag]:
    out: list[Flag] = []
    for m in PRIVATE_USE.finditer(text):
        out.append(Flag("BROKEN_GLYPH", page,
                        f"محرف خط تالف U+{ord(m.group()):04X}",
                        text[max(0, m.start() - 30):m.start() + 30].strip()))
    for m in LILLAH_SUSPECT.finditer(text):
        out.append(Flag("ALLAH_OR_LILLAH", page,
                        "قد تكون «لله» لا «الله» — طابِقها بالصورة",
                        m.group().strip()))
    if "اللهم!" in text:
        out.append(Flag("EXCLAM_AFTER_ALLAHUMMA", page,
                        "«اللهم!» بعلامة تعجب كما في الأصل — تأكد من الصورة", ""))
    for m in re.finditer(r"[ء-ي] \)(?!\s*\()", text):
        out.append(Flag("HONORIFIC_GLYPH", page,
                        "«)» بعد اسم الصحابي = رمز الترضي، لا قوس",
                        text[max(0, m.start() - 25):m.start() + 20].strip()))
    for line in text.split("\n"):
        s = line.strip()
        if s and re.fullmatch(f"[{HARAKAT}\\s]+", s):
            out.append(Flag("ORPHAN_HARAKAT", page,
                            "حركات انفصلت على سطر وحدها — التشكيل مفقود هنا", repr(s)))
    return out


SALLA = re.compile(r"صلى\s+الله\s+عليه\s+و\s*سلم")
WEST_DIGITS = str.maketrans("0123456789", "\u0660\u0661\u0662\u0663\u0664\u0665\u0666\u0667\u0668\u0669")


def strip_harakat(text: str) -> str:
    """content/GUIDELINES.md: «بلا تشكيل» في النص الخام. التشكيل مرحلة لاحقة."""
    return re.sub(f"[{HARAKAT}\u0640]", "", text)


def apply_confirmed_fixes(text: str) -> tuple[str, list[dict]]:
    """القواعد المؤكدة وحدها، ويُسجَّل كل تطبيق."""
    applied: list[dict] = []

    def rec(kind, detail, ctx=""):
        applied.append(asdict(Flag(kind, 0, detail, ctx)))

    def _h(m):
        rec("FIXED_HAMD_LILLAH", "«الحمد الله» → «الحمد لله» (قاعدة مؤكدة)", m.group())
        return m.group(1) + "حمد لله"

    text = HAMD_FIX.sub(_h, text)

    # رمز الترضي الذي استُخرج قوسًا: يُحذف ويُسجَّل ليُعاد نصًّا من الصورة
    def _t(m):
        rec("HONORIFIC_DROPPED",
            "حُذف «)» رمز الترضي — أعِده نصًّا من الصورة", m.group().strip())
        return m.group(1) + " "
    text = re.sub(r"([\u0621-\u064a]) \)(?!\s*\()", _t, text)

    # علامة الصلاة المضغوطة تُكتب حرفًا واحدًا ﷺ (GUIDELINES)
    n = len(SALLA.findall(text))
    if n:
        text = SALLA.sub("\ufdfa", text)
        rec("SALLA_LIGATURE", f"«صلى الله عليه وسلم» → «ﷺ» في {n} موضعًا")

    # علامات الحواشي تُرفع من النص، ومحتواها في footnotes
    if re.search(r"\(\s*\d+\s*\)", text):
        text = re.sub(r"\s*\(\s*\d+\s*\)", " ", text)
        rec("FOOTNOTE_MARK_REMOVED", "رُفعت علامة حاشية من النص")

    # نقاط التعداد المتسرّبة داخل المتن
    if "\u00b7" in text:
        text = text.replace("\u00b7", " ")
        rec("STRAY_BULLET", "«·» متسرّبة داخل المتن")

    # علامات التنصيص المقلوبة: الافتتاح يُستخرج «":» والإغلاق «"» مجردة
    if '"' in text:
        text = re.sub(r'"\s*:\s*', ': \u00ab', text)   # افتتاح
        text = text.replace('"', '\u00bb')             # إغلاق
        text = re.sub(r"\s*\u00bb", "\u00bb", text)
        text = re.sub(r"\u00bb(?=[\u0621-\u064a])", "\u00bb ", text)
        rec("QUOTES_REPOSITIONED", "أُعيدت علامات التنصيص إلى موضعها «…»")

    text = strip_harakat(text)
    text = text.translate(WEST_DIGITS)              # لا أرقام لاتينية إطلاقًا
    text = text.replace(",", "\u060c")             # الفاصلة العربية
    text = re.sub(r" +([،؛؟\.:])", r"\1 ", text)
    text = re.sub(r"\.\s*\.\s*\.", "...", text)      # نقاط الحذف
    text = re.sub(r"\s+", " ", text).strip()
    return text, applied


# ───────── فصل الآيات والآثار عن متن الحديث ─────────
VERSE_START = re.compile(r"^(?:و?قال(?:ت)?\s+(?:الله|تعالى|سبحانه|عز\s+وجل)|"
                         r"و?قال\s+الله\s+(?:تعالى|سبحانه)|ق\s*ال\s+تعالى)")
HADITH_START = re.compile(
    r"^(?:و?عن\b|و?عند\b|و?في\s+الحديث|و?روى\b|و?سئل\b|و?كان\b|"
    r"و?أن\b|و?قال\s+رسول|و?حدثنا\b|و?قالت\s+عائشة)")


def split_preamble(lines: list[str]) -> tuple[list[str], list[str]]:
    """ما قبل أول علامة بدء حديث = آيات وآثار، لا متن."""
    for i, ln in enumerate(lines):
        s = ln.strip().strip("\u00b7").strip()
        if VERSE_START.match(s):
            continue
        if HADITH_START.match(s):
            return lines[:i], lines[i:]
    return [], lines


# ───────────────────────── ٥. سطر الحكم ─────────────────────────
GRADE_RE = re.compile(
    r"^\s*(?P<grade>(?:صحيح|حسن|ضعيف|متفق)(?:\s+(?:لغيره|عليه))?)\s*"
    r"[\)\(]\s*(?P<sym>[^\(\)]*?)\s*[\(\)]\s*[\.,]?\s*$"
)


def parse_grade(line: str):
    m = GRADE_RE.match(line.strip())
    if not m:
        return None
    grade = re.sub(f"[{HARAKAT}]", "", re.sub(r"\s+", " ", m.group("grade"))).strip()
    syms = [s for s in re.split(r"[\s,،]+|\bو\b", m.group("sym")) if s and s != "و"]
    syms = [s.lstrip("و") if len(s) > 1 and s.startswith("و") else s
            for s in syms]
    syms = [re.sub(f"[{HARAKAT}]", "", x) for x in syms]
    return grade, [s for s in syms if s]


# ───────────────────────── ٦. التقسيم ─────────────────────────
def segment(pages: list[str], titles: list[str]):
    by_skel = {skeleton(t): t for t in titles if len(skeleton(t)) > 4}
    entries: list[Entry] = []
    flags: list[Flag] = []
    ungraded: list[dict] = []

    chapter = "الحث على الذكر"
    buf: list[str] = []
    notes: list[str] = []
    in_footnote = False
    n = 0

    for pno in range(BODY_START, len(pages) + 1):
        if pno - 1 >= len(pages):
            break
        page = normalize(pages[pno - 1])
        flags.extend(scan_flags(page, pno))

        raw_lines = [l.strip() for l in page.split("\n")]
        raw_lines = [l for l in raw_lines if l]

        i = 0
        while i < len(raw_lines):
            s = raw_lines[i]

            if re.fullmatch(r"_{5,}", s):
                in_footnote = True
                i += 1
                continue
            if in_footnote:
                if re.match(r"^\(\d+\)", s):
                    notes.append(s)
                    i += 1
                    continue
                in_footnote = False

            if skeleton(s) == skeleton(RUNNING_HEAD):
                i += 1
                continue

            # عنوان باب: سطرًا واحدًا، أو سطرين التصق أحدهما بالآخر
            bare = s.strip("·").strip()
            hit = by_skel.get(skeleton(bare))
            step = 1
            if hit is None and i + 1 < len(raw_lines):
                two = f"{bare} {raw_lines[i + 1].strip('·').strip()}"
                hit = by_skel.get(skeleton(two))
                if hit is None:
                    two_r = f"{raw_lines[i + 1].strip('·').strip()} {bare}"
                    hit = by_skel.get(skeleton(two_r))
                if hit is not None:
                    step = 2
            if hit is not None:
                if buf:
                    utxt, _ = apply_confirmed_fixes(
                        re.sub(r"\s+", " ", " ".join(buf)).strip())
                    if utxt:
                        ungraded.append({"chapter": chapter, "page": pno,
                                         "kind": "آيات وآثار", "text": utxt})
                    buf = []
                chapter = hit
                i += step
                continue

            g = parse_grade(s)
            if g:
                grade, syms = g
                if not buf:
                    flags.append(Flag("GRADE_WITHOUT_MATN", pno,
                                      "سطر حكم بلا متن قبله", s))
                    i += 1
                    continue
                n += 1
                fixed_buf, order_notes = repair_matn_lines(buf)
                pre, body_lines = split_preamble(fixed_buf)
                if pre:
                    ptxt, _ = apply_confirmed_fixes(
                        re.sub(r"\s+", " ", " ".join(pre)).strip())
                    if ptxt:
                        ungraded.append({"chapter": chapter, "page": pno,
                                         "kind": "آيات وآثار", "text": ptxt})
                raw_matn = re.sub(r"\s+", " ", " ".join(body_lines)).strip()
                pre_flags = scan_flags(raw_matn, pno)
                matn, applied = apply_confirmed_fixes(raw_matn)
                applied = order_notes + applied
                unknown = [x for x in syms if x not in SOURCE_SYMBOLS]
                if unknown:
                    applied.append(asdict(Flag(
                        "UNKNOWN_SOURCE_SYMBOL", pno,
                        "رمز مصدر غير معروف", " ".join(unknown))))
                entries.append(Entry(
                    n=n, chapter=chapter, page=pno, matn=matn,
                    grade=grade, symbols=syms,
                    sources=[SOURCE_SYMBOLS[x] for x in syms if x in SOURCE_SYMBOLS],
                    unknown_symbols=unknown,
                    footnotes=notes[:],
                    flags=[asdict(f) for f in pre_flags] + applied,
                ))
                buf, notes = [], []
                i += 1
                continue

            buf.append(s)
            i += 1

    if buf:
        ungraded.append({"chapter": chapter, "page": len(pages),
                         "text": re.sub(r"\s+", " ", " ".join(buf)).strip()})
    return entries, flags, ungraded


# ───────────────────────── ٧. التقرير ─────────────────────────
MEANING = {
    "BROKEN_GLYPH": "محرف من خط تالف، النص هناك غير موثوق",
    "ALLAH_OR_LILLAH": "«الله» قد تكون «لله» — خطر على المعنى",
    "EXCLAM_AFTER_ALLAHUMMA": "«اللهم!» بعلامة تعجب",
    "HONORIFIC_GLYPH": "«)» رمز ترضٍّ لا قوس",
    "ORPHAN_HARAKAT": "حركات انفصلت، التشكيل مفقود هنا",
    "FIXED_HAMD_LILLAH": "صُحِّح آليًّا بقاعدة مؤكدة",
    "GRADE_WITHOUT_MATN": "سطر حكم لم يُلتقط متنه",
    "UNKNOWN_SOURCE_SYMBOL": "رمز مصدر ليس في جدول ص٧",
    "HONORIFIC_DROPPED": "حُذف رمز الترضي، أعِده نصًّا من الصورة",
    "SALLA_LIGATURE": "«صلى الله عليه وسلم» كُتبت «ﷺ»",
    "FOOTNOTE_MARK_REMOVED": "رُفعت علامة حاشية من المتن",
    "STRAY_BULLET": "«·» متسرّبة داخل المتن",
    "QUOTES_REPOSITIONED": "أُعيدت علامات التنصيص إلى موضعها",
    "LINE_ORDER_REPAIRED": "أُعيد ترتيب سطر قدّمه الاستخراج — يلزمه تأكيد بالصورة",
    "VOCALIZED_SCRAMBLE": "سطر مشكول مختلّ الترتيب لم يُصلَح",
}


def write_flags(flags, entries, ungraded, titles, rebuilt, missing):
    buckets: dict[str, list] = {}
    for f in flags:
        buckets.setdefault(f.kind, []).append(f)
    for e in entries:
        for d in e.flags:
            buckets.setdefault(d["kind"], []).append(Flag(**{**d, "page": e.page}))

    L = ["# أعلام الاستخراج الآلي — «قبس مختار» (نسخة A4)", "",
         "مولَّد بـ`tools/extract_qabas.py`. "
         "**لا يُعتمد شيء منه قبل المطابقة بصور الطبعة الصغيرة المشكولة.**", "",
         f"- عناوين الفهرس: **{len(titles)}** (منها **{len(rebuilt)}** أُعيد تركيب كلماتها)",
         f"- أحاديث مستخرجة: **{len(entries)}**",
         f"- مقاطع بلا حكم (آيات وآثار): **{len(ungraded)}**",
         f"- أبواب بلا حديث: **{len(missing)}**", ""]
    if not SOURCE_SYMBOLS_VERIFIED:
        L += ["> ⚠ **رموز المصادر لم تُتحقَّق بعد** من ص٧ من الطبعة الصغيرة، "
              "فكل حقل `sources` مبدئي.", ""]
    L += ["## ملخص الأعلام", "", "| النوع | العدد | المعنى |", "|---|---|---|"]
    for k in sorted(buckets, key=lambda x: -len(buckets[x])):
        L.append(f"| `{k}` | {len(buckets[k])} | {MEANING.get(k, '—')} |")
    L.append("")
    if rebuilt:
        L += ["## عناوين أُعيد تركيب كلماتها", "",
              "رُتِّبت كلماتها وحُذف التنوين المعلَّق. تأكد منها بالصورة:", ""]
        L += [f"- {t}" for t in sorted(rebuilt)]
        L.append("")
    if missing:
        L += ["## أبواب لم يُلتقط لها حديث", "",
              "إمّا فيها آيات وآثار بلا حكم، وإمّا لم يُتعرَّف على عنوانها في المتن:", ""]
        L += [f"- {t}" for t in missing]
        L.append("")
    for k in sorted(buckets, key=lambda x: -len(buckets[x])):
        L += [f"## {k} ({len(buckets[k])})", ""]
        for f in buckets[k][:40]:
            ctx = f.context.replace("|", "\\|")[:110]
            L.append(f"- ص{f.page}: {f.detail}" + (f" — `{ctx}`" if ctx else ""))
        if len(buckets[k]) > 40:
            L.append(f"- … و{len(buckets[k]) - 40} غيرها في `raw.json`")
        L.append("")
    OUT_FLAGS.write_text("\n".join(L), encoding="utf-8")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--pdf", default=str(PDF))
    ap.add_argument("--report", action="store_true", help="لا تكتب ملفات")
    a = ap.parse_args()

    pages = extract_pages(Path(a.pdf))
    titles, rebuilt = parse_index(pages)
    titles = [strip_harakat(t).translate(WEST_DIGITS) for t in titles]
    rebuilt = {strip_harakat(t).translate(WEST_DIGITS) for t in rebuilt}
    entries, flags, ungraded = segment(pages, titles)

    seen = {skeleton(e.chapter) for e in entries}
    missing = [t for t in titles if skeleton(t) not in seen]
    chapters = []
    for e in entries:
        if e.chapter not in chapters:
            chapters.append(e.chapter)

    print(f"صفحات: {len(pages)}")
    print(f"عناوين الفهرس: {len(titles)}  (أُعيد تركيب {len(rebuilt)})")
    print(f"أبواب فيها أحاديث: {len(chapters)}   بلا أحاديث: {len(missing)}")
    print(f"أحاديث: {len(entries)}")
    print(f"مقاطع بلا حكم: {len(ungraded)}")
    print(f"أعلام الصفحات: {len(flags)}")
    syms: dict[str, int] = {}
    for e in entries:
        for s in e.symbols:
            syms[s] = syms.get(s, 0) + 1
    print("الرموز:", ", ".join(f"{k}={v}" for k, v in
                               sorted(syms.items(), key=lambda x: -x[1])))
    unk = sorted({u for e in entries for u in e.unknown_symbols})
    if unk:
        print("رموز غير معروفة:", " ".join(unk))

    if a.report:
        return

    OUT_JSON.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "source": {
            "book": "قبس مختار من صحيح الأذكار",
            "author": "مصطفى بن العدوي",
            "pdf": Path(a.pdf).name,
            "note": "مستخرج آليًّا من نسخة A4. الصفحات صفحات ملف A4 لا الطبعة "
                    "الصغيرة. التشكيل يؤخذ من الطبعة المشكولة ولا يُخترع.",
            "source_symbols_verified": SOURCE_SYMBOLS_VERIFIED,
            "source_symbols": SOURCE_SYMBOLS,
        },
        "titles": titles,
        "titles_rebuilt": sorted(rebuilt),
        "chapters_with_entries": chapters,
        "chapters_without_entries": missing,
        "entries": [asdict(e) for e in entries],
        "ungraded_blocks": ungraded,
        "page_flags": [asdict(f) for f in flags],
    }
    OUT_JSON.write_text(
        json.dumps(payload, ensure_ascii=False, indent=1), encoding="utf-8")
    write_flags(flags, entries, ungraded, titles, rebuilt, missing)
    print(f"\nكُتب: {OUT_JSON.relative_to(ROOT)}")
    print(f"كُتب: {OUT_FLAGS.relative_to(ROOT)}")


if __name__ == "__main__":
    main()

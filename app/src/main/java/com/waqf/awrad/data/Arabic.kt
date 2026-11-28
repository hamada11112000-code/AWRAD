package com.waqf.awrad.data

private val diacritics = Regex("[ً-ٰٟۖ-ۭـ]")

/** Folds Arabic text for searching: drops diacritics/tatweel and unifies letter variants. */
fun String.normalizeArabic(): String = diacritics.replace(this, "")
    .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا').replace('ٱ', 'ا')
    .replace('ى', 'ي').replace('ئ', 'ي').replace('ؤ', 'و').replace('ة', 'ه')
    .lowercase()

/** Western digits to Arabic-Indic digits (١٢٣). */
fun String.arabicDigits(): String = buildString(length) {
    for (c in this@arabicDigits) append(if (c in '0'..'9') '٠' + (c - '0') else c)
}

fun Int.ar(): String = toString().arabicDigits()

/** «مرة واحدة»، «مرتان»، «٣ مرات»، «١٠٠ مرة» */
fun countLabel(n: Int): String = when {
    n <= 0 -> ""
    n == 1 -> "مرة واحدة"
    n == 2 -> "مرتان"
    n in 3..10 -> "${n.ar()} مرات"
    else -> "${n.ar()} مرة"
}

/** Unit word for a counter («مرة» / «مرات»). */
fun timesWord(n: Int): String = if (n in 3..10) "مرات" else "مرة"

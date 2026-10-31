package com.urdu.dub

object Roman {
    private val map = mapOf(
        'ا' to "a", 'آ' to "aa", 'ب' to "b", 'پ' to "p", 'ت' to "t", 'ٹ' to "tt",
        'ث' to "s", 'ج' to "j", 'چ' to "ch", 'ح' to "h", 'خ' to "kh", 'د' to "d",
        'ڈ' to "dd", 'ذ' to "z", 'ر' to "r", 'ڑ' to "rr", 'ز' to "z", 'ژ' to "zh",
        'س' to "s", 'ش' to "sh", 'ص' to "s", 'ض' to "z", 'ط' to "t", 'ظ' to "z",
        'ع' to "a", 'غ' to "gh", 'ف' to "f", 'ق' to "q", 'ک' to "k", 'گ' to "g",
        'ل' to "l", 'م' to "m", 'ن' to "n", 'ں' to "n", 'و' to "o", 'ہ' to "h",
        'ھ' to "h", 'ء' to "", 'ی' to "i", 'ے' to "e", 'ۓ' to "e", 'ئ' to "i",
        'ؤ' to "o", 'أ' to "a", 'إ' to "i", 'ٰ' to "a", '۔' to ".",
        'َ' to "a", 'ِ' to "i", 'ُ' to "u", 'ً' to "an", 'ٍ' to "in", 'ٌ' to "un",
        'ّ' to "", 'ْ' to "", 'ـ' to "",
        '۰' to "0", '۱' to "1", '۲' to "2", '۳' to "3", '۴' to "4",
        '۵' to "5", '۶' to "6", '۷' to "7", '۸' to "8", '۹' to "9"
    )

    fun toRoman(s: String): String {
        val sb = StringBuilder()
        for (c in s) {
            val r = map[c]
            if (r != null) sb.append(r) else sb.append(c)
        }
        return sb.toString()
    }
}

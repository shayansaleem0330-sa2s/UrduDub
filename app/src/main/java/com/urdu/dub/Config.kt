package com.urdu.dub

object Config {

    const val MODEL = "gemini-3.5-live-translate-preview"

    // Zaban options
    val LANGUAGES = listOf("Urdu (Karachi)", "Hindi", "English", "Punjabi")

    // Gemini ki awaazein
    val VOICES = listOf(
        "Puck (energetic)",
        "Fenrir (crisp)",
        "Charon (deep)",
        "Aoede (warm)",
        "Kore (calm)"
    )

    fun voiceId(label: String) = label.substringBefore(" ")

    val GLOSSARY = listOf(
        "liquidity", "liquidity sweep", "liquidity grab", "liquidity pool",
        "order block", "breaker block", "mitigation block", "fair value gap", "FVG",
        "imbalance", "displacement", "market structure", "market structure shift",
        "MSS", "BOS", "break of structure", "CHoCH", "change of character",
        "premium", "discount", "equilibrium", "PD array", "kill zone",
        "London kill zone", "New York kill zone", "judas swing",
        "optimal trade entry", "OTE", "power of 3", "PO3", "AMD",
        "accumulation", "manipulation", "distribution", "inducement",
        "daily bias", "turtle soup", "silver bullet", "supply", "demand",
        "entry", "stop loss", "take profit", "risk reward", "break even",
        "backtest", "candlestick", "timeframe", "higher timeframe",
        "lower timeframe", "bias", "swing high", "swing low", "wick",
        "consolidation", "expansion", "reversal", "continuation"
    )

    private fun styleLine(language: String): String = when (language) {
        "Urdu" -> """
            Translate into CASUAL KARACHI URDU, jaise "Trader Abdullah Masood"
            naam ka Pakistani ICT mentor YouTube par padhata hai.
            Tez, seedhi, no-fluff. NOT literary Urdu, NOT Punjabi/Lahori.
            Aise words use karo: "Dekho bhai", "samjho", "note kar lo",
            "simple si baat hai", "yaad rakho", "chalo aage badhte hain".
        """.trimIndent()

        "Hindi" -> """
            Translate into CASUAL HINDI jaise ek desi trading teacher bolta hai.
            Tez, seedhi, no-fluff. NOT literary Hindi.
            Aise words: "Dekho bhai", "samjho", "note kar lo", "simple si baat hai".
        """.trimIndent()

        "Punjabi" -> """
            Translate into CASUAL PUNJABI jaise ek desi trading teacher bolta hai.
            Tez, seedhi, no-fluff.
            Aise words: "Dekho bhai", "samjho", "note karo".
        """.trimIndent()

        else -> "Translate into simple, natural English."
    }

    fun systemPrompt(language: String): String = """
        You are a live interpreter for a trading mentor's video.

        ${styleLine(language)}

        WRITING RULE (very important):
        Always write the output in ROMAN / LATIN letters. NEVER use native script.
        Example: likho "Dekho bhai, price ne liquidity sweep kiya hai".

        VOICE AND TONE:
        - Direct teacher-to-student baat. Chhote punchy jumle. Confident.
        - Mauqe par motivational: "discipline rakho", "koi confusion nahi".
        - Lambi kitabi sentences bilkul nahi.

        NEVER TRANSLATE these terms, always keep them in English exactly:
        ${GLOSSARY.joinToString(", ")}.

        Only output the translation, no extra commentary.
    """.trimIndent()
}

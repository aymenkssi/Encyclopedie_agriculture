package org.agripedia.core.search

import java.text.Normalizer

/** Normalizes text for accent-, case- and script-variant-insensitive search (Latin and Arabic). */
object TextNormalizer {
    private val combiningMarks = Regex("\\p{Mn}+")
    // Arabic diacritics (tashkeel), tatweel and Quranic marks.
    private val arabicMarks = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]")
    private val nonAlnum = Regex("[^\\p{L}\\p{Nd}]+")

    fun normalize(input: String): String {
        var s = input.lowercase()
        s = arabicMarks.replace(s, "")
        s = s.replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا').replace('ٱ', 'ا')
            .replace('ة', 'ه').replace('ى', 'ي').replace('ؤ', 'و').replace('ئ', 'ي')
        s = Normalizer.normalize(s, Normalizer.Form.NFD)
        s = combiningMarks.replace(s, "")
        s = s.replace("œ", "oe").replace("æ", "ae").replace("ß", "ss")
        return nonAlnum.replace(s, " ").trim()
    }

    fun tokens(input: String): List<String> = normalize(input).split(' ').filter { it.isNotBlank() }
}

/** Optimal string alignment (restricted Damerau-Levenshtein) distance, with an early exit bound. */
object EditDistance {
    fun osa(a: String, b: String, maxDistance: Int = Int.MAX_VALUE): Int {
        if (a == b) return 0
        if (kotlin.math.abs(a.length - b.length) > maxDistance) return maxDistance + 1
        val n = a.length; val m = b.length
        if (n == 0) return m
        if (m == 0) return n
        var prev2 = IntArray(m + 1)
        var prev = IntArray(m + 1) { it }
        var cur = IntArray(m + 1)
        for (i in 1..n) {
            cur[0] = i
            var rowMin = cur[0]
            for (j in 1..m) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var v = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) v = minOf(v, prev2[j - 2] + 1)
                cur[j] = v
                if (v < rowMin) rowMin = v
            }
            if (rowMin > maxDistance) return maxDistance + 1
            val t = prev2; prev2 = prev; prev = cur; cur = t
        }
        return prev[m]
    }
}

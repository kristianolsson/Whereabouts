package com.kristianolsson.whereabouts

/**
 * Converts an ISO 3166-1 alpha-2 country code to its flag emoji.
 *
 * Uses Unicode Regional Indicator Symbols (U+1F1E6–U+1F1FF).
 * Matches the logic in macos/Sources/Whereabouts/FlagEmoji.swift:
 *   base = 127397 (U+1F1A5), then base + charValue for each letter.
 */
object FlagEmoji {
    // U+1F1A5 = 127397 = U+1F1E6 ('🇦') minus 'A' (65)
    // So: flagCodePoint = 127397 + charCodePoint
    // e.g. "US" → (127397+85)(127397+83) = U+1F1FA U+1F1F8 = 🇺🇸
    private const val REGIONAL_INDICATOR_BASE = 127397

    fun flagForCountryCode(code: String): String {
        val upper = code.uppercase()
        if (upper.length != 2) return "🏳"
        return buildString {
            for (ch in upper) {
                appendCodePoint(REGIONAL_INDICATOR_BASE + ch.code)
            }
        }
    }
}

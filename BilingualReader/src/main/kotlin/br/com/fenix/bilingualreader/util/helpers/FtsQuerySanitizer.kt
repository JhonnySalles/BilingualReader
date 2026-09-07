package br.com.fenix.bilingualreader.util.helpers

object FtsQuerySanitizer {

    private val REGEX_NON_WORD = Regex("[^a-z0-9áàâãéèêíïóôõöúçñ\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FAF\\u3005-\\u3007\\s]")
    private val REGEX_WHITESPACE = Regex("\\s+")

    val STOPWORDS_SET = setOf(
        // Portuguese
        "o", "a", "os", "as", "um", "uma", "uns", "umas", "de", "do", "da", "dos", "das",
        "em", "no", "na", "nos", "nas", "por", "pelo", "pela", "pelos", "pelas",
        "que", "e", "ou", "se", "para", "com", "como", "qual", "quais", "quem", "onde",
        "quando", "porque", "porquê", "por que", "é", "são", "era", "eram", "foi", "foram",
        // English
        "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with",
        "by", "from", "up", "about", "into", "through", "after", "is", "are", "was", "were",
        "what", "who", "where", "when", "why", "how",
        // Japanese particles / aux
        "は", "が", "の", "に", "を", "へ", "で", "と", "から", "まで", "より",
        "て", "た", "だ", "です", "ます", "これ", "それ", "あれ", "どれ", "何", "誰", "どこ"
    )

    /**
     * Converts raw query string to safe SQLite FTS4 MATCH query string.
     */
    fun sanitize(query: String): String {
        val normalized = TextPreprocessor.normalizeJapaneseWidth(query)
        val cleaned = normalized.lowercase()
            .replace(REGEX_NON_WORD, " ")
            .trim()

        val rawTokens = cleaned.split(REGEX_WHITESPACE).filter { it.isNotBlank() }
        val tokens = mutableListOf<String>()

        for (token in rawTokens) {
            if (isCjkWord(token)) {
                // For CJK words without spaces, split into 2-char n-grams if longer than 2 chars
                if (token.length <= 2) {
                    if (token !in STOPWORDS_SET) tokens.add(token)
                } else {
                    for (i in 0 until token.length - 1) {
                        val ngram = token.substring(i, i + 2)
                        if (ngram !in STOPWORDS_SET) tokens.add(ngram)
                    }
                }
            } else if (token.length > 1 && token !in STOPWORDS_SET) {
                tokens.add(token)
            }
        }

        if (tokens.isEmpty()) {
            if (rawTokens.isEmpty()) return "\"*\""
            return rawTokens.take(3).joinToString(" OR ") { "$it*" }
        }

        return tokens.distinct().take(5).joinToString(" OR ") { "$it*" }
    }

    private fun isCjkWord(text: String): Boolean {
        return text.any { c ->
            c in '\u3040'..'\u309F' || c in '\u30A0'..'\u30FF' || c in '\u4E00'..'\u9FAF' || c in '\u3005'..'\u3007'
        }
    }
}

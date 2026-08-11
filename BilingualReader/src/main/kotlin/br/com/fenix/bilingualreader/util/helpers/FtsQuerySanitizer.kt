package br.com.fenix.bilingualreader.util.helpers

object FtsQuerySanitizer {

    private val STOPWORDS = setOf(
        "o", "a", "os", "as", "um", "uma", "uns", "umas", "de", "do", "da", "dos", "das",
        "em", "no", "na", "nos", "nas", "por", "pelo", "pela", "pelos", "pelas",
        "que", "e", "ou", "se", "para", "com", "como", "qual", "quais", "quem", "onde",
        "quando", "porque", "porquê", "por que", "é", "são", "era", "eram", "foi", "foram",
        "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with",
        "by", "from", "up", "about", "into", "through", "after", "is", "are", "was", "were",
        "what", "who", "where", "when", "why", "how"
    )

    /**
     * Converts raw query string to safe SQLite FTS4 MATCH query string.
     */
    fun sanitize(query: String): String {
        val cleaned = query.lowercase()
            .replace(Regex("[^a-z0-9áàâãéèêíïóôõöúçñ\\s]"), " ")
            .trim()

        val tokens = cleaned.split(Regex("\\s+"))
            .filter { it.length > 1 && !STOPWORDS.contains(it) }

        if (tokens.isEmpty()) {
            val fallbackTokens = cleaned.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (fallbackTokens.isEmpty()) return "\"*\""
            return fallbackTokens.joinToString(" OR ") { "$it*" }
        }

        return tokens.joinToString(" OR ") { "$it*" }
    }
}

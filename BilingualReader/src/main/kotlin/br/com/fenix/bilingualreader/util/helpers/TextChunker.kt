package br.com.fenix.bilingualreader.util.helpers

object TextChunker {

    /**
     * Cleans noise such as duplicate line breaks, excess whitespace, etc.
     */
    fun cleanNoise(text: String): String {
        return text
            .replace(Regex("\r\n|\r"), "\n")
            .replace(Regex("\n{2,}"), "\n")
            .replace(Regex("[ \t]{2,}"), " ")
            .trim()
    }

    /**
     * Splits text into smaller chunks of ~targetWords words with overlapWords sentence/word overlap.
     */
    fun chunkText(
        text: String,
        targetWords: Int = 120,
        overlapWords: Int = 20
    ): List<String> {
        val cleaned = cleanNoise(text)
        if (cleaned.isBlank()) return emptyList()

        val words = cleaned.split(Regex("\\s+"))
        if (words.size <= targetWords) return listOf(cleaned)

        val chunks = mutableListOf<String>()
        var start = 0

        while (start < words.size) {
            val end = (start + targetWords).coerceAtMost(words.size)
            val chunkWords = words.subList(start, end)
            chunks.add(chunkWords.joinToString(" "))

            if (end == words.size) break
            start += (targetWords - overlapWords).coerceAtLeast(1)
        }

        return chunks
    }
}

package br.com.fenix.bilingualreader.util.helpers

object TextChunker {

    private val REGEX_LINE_BREAK = Regex("\r\n|\r")
    private val REGEX_MULTI_NEWLINE = Regex("\n{2,}")
    private val REGEX_MULTI_SPACE = Regex("[ \\t]{2,}")
    private val REGEX_WHITESPACE = Regex("\\s+")

    /**
     * Cleans noise such as duplicate line breaks, excess whitespace, etc.
     */
    fun cleanNoise(text: String): String {
        return text
            .replace(REGEX_LINE_BREAK, "\n")
            .replace(REGEX_MULTI_NEWLINE, "\n")
            .replace(REGEX_MULTI_SPACE, " ")
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

        val words = cleaned.split(REGEX_WHITESPACE).filter { it.isNotBlank() }
        if (words.size <= targetWords) return listOf(cleaned)

        val chunks = mutableListOf<String>()
        var start = 0

        while (start < words.size) {
            val end = (start + targetWords).coerceAtMost(words.size)
            var adjustedEnd = if (end == words.size) words.size else findSentenceBoundary(words, end, targetWords)
            
            if (adjustedEnd <= start) {
                adjustedEnd = (start + 1).coerceAtMost(words.size)
            }

            val chunkWords = words.subList(start, adjustedEnd)
            val chunk = chunkWords.joinToString(" ")
            if (chunk.isNotBlank()) chunks.add(chunk)

            if (adjustedEnd >= words.size) break
            start += (adjustedEnd - start - overlapWords).coerceAtLeast(1)
        }

        return chunks
    }

    private fun findSentenceBoundary(words: List<String>, position: Int, targetWords: Int): Int {
        val maxExtend = (targetWords * 0.3).toInt().coerceAtLeast(5)
        val limit = (position + maxExtend).coerceAtMost(words.size)

        for (i in position until limit) {
            val word = words[i]
            if (word.endsWith('.') || word.endsWith('!') || word.endsWith('?') ||
                word.endsWith('。') || word.endsWith('」')) {
                return i + 1
            }
        }

        for (i in (position - 1) downTo (position - maxExtend).coerceAtLeast(0)) {
            val word = words[i]
            if (word.endsWith('.') || word.endsWith('!') || word.endsWith('?') ||
                word.endsWith('。') || word.endsWith('」')) {
                return i + 1
            }
        }
        return position.coerceAtMost(words.size)
    }
}

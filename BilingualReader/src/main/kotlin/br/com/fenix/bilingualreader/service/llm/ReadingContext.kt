package br.com.fenix.bilingualreader.service.llm

import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.Type

enum class ContextSource {
    SUBTITLES,
    OCR,
    BOOK_TEXT,
    EMPTY
}

data class ContextChunk(
    val label: String,
    val text: String,
    val pageOrChapter: Int = -1
)

data class ReadingContext(
    val type: Type,
    val title: String,
    val sourceLanguage: Languages?,
    val userLanguage: Languages,
    val chunks: List<ContextChunk>,
    val source: ContextSource
) {
    fun joinedText(separator: String = "\n\n"): String =
        chunks.filter { it.text.isNotBlank() }
            .joinToString(separator) { chunk ->
                if (chunk.label.isNotBlank()) "${chunk.label}\n${chunk.text}" else chunk.text
            }
}

package br.com.fenix.bilingualreader.service.llm

import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.Type

import br.com.fenix.bilingualreader.util.helpers.TextPreprocessor

enum class ContextSource {
    SUBTITLES,
    OCR,
    BOOK_TEXT,
    EMPTY
}

data class ContextChunk(
    val label: String,
    val text: String,
    val pageOrChapter: Int = -1,
    val imageBase64: String? = null
)

data class ReadingContext(
    val type: Type,
    val title: String,
    val sourceLanguage: Languages?,
    val userLanguage: Languages,
    val chunks: List<ContextChunk>,
    val source: ContextSource
) {
    val joinedText: String by lazy {
        chunks.filter { it.text.isNotBlank() }
            .joinToString("\n\n") { chunk ->
                if (chunk.label.isNotBlank()) "${chunk.label}\n${chunk.text}" else chunk.text
            }
    }

    val wordCount: Int by lazy {
        TextPreprocessor.countWords(joinedText)
    }

    fun joinedText(separator: String = "\n\n"): String {
        if (separator == "\n\n") return joinedText
        return chunks.filter { it.text.isNotBlank() }
            .joinToString(separator) { chunk ->
                if (chunk.label.isNotBlank()) "${chunk.label}\n${chunk.text}" else chunk.text
            }
    }

    companion object {
        fun countWords(text: CharSequence): Int = TextPreprocessor.countWords(text)
    }
}

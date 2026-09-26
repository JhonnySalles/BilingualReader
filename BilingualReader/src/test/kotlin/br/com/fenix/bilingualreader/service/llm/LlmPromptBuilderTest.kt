package br.com.fenix.bilingualreader.service.llm

import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.Type
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LlmPromptBuilderTest {

    @Test
    fun truncateKeepsShortText() {
        assertEquals("abc", LlmPromptBuilder.truncate("abc", 10))
    }

    @Test
    fun truncateCutsLongText() {
        val result = LlmPromptBuilder.truncate("abcdefghij", 5)
        assertTrue(result.startsWith("abcde"))
        assertTrue(result.contains("…"))
    }

    @Test
    fun summaryPromptContainsTitleAndLanguage() {
        val prompt = LlmPromptBuilder.buildSummaryPrompt(
            title = "My Book",
            chaptersText = "Once upon a time",
            userLanguage = Languages.PORTUGUESE,
            maxChars = 1000
        )
        assertTrue(prompt.contains("My Book"))
        assertTrue(prompt.contains("Portuguese"))
        assertTrue(prompt.contains("Once upon a time"))
        assertTrue(prompt.contains("<start_of_turn>user"))
        assertTrue(prompt.contains("<end_of_turn>"))
        assertTrue(prompt.contains("<start_of_turn>model"))
    }

    @Test
    fun qaPromptContainsQuestion() {
        val context = ReadingContext(
            type = Type.BOOK,
            title = "Novel",
            sourceLanguage = Languages.ENGLISH,
            userLanguage = Languages.ENGLISH,
            chunks = listOf(ContextChunk("Ch1", "The hero met a dragon.")),
            source = ContextSource.BOOK_TEXT
        )
        val prompt = LlmPromptBuilder.buildQaPrompt(context, "Who met a dragon?", 1000)
        assertTrue(prompt.contains("Who met a dragon?"))
        assertTrue(prompt.contains("Novel"))
        assertTrue(prompt.contains("dragon"))
        assertTrue(prompt.startsWith("<start_of_turn>user"))
        assertTrue(prompt.endsWith("<start_of_turn>model\n") || prompt.endsWith("<start_of_turn>model"))
    }

    @Test
    fun wrapGemmaChatAppliesTemplate() {
        val wrapped = LlmPromptBuilder.wrapGemmaChat("Hello")
        assertEquals(
            "<start_of_turn>user\nHello\n<end_of_turn>\n<start_of_turn>model\n",
            wrapped
        )
    }
}
package br.com.fenix.bilingualreader.service.llm

import br.com.fenix.bilingualreader.model.enums.Languages

object LlmPromptBuilder {

    fun languageName(language: Languages): String = when (language) {
        Languages.PORTUGUESE, Languages.PORTUGUESE_GOOGLE -> "Portuguese"
        Languages.ENGLISH -> "English"
        Languages.JAPANESE -> "Japanese"
    }

    fun truncate(text: String, maxChars: Int): String {
        if (maxChars <= 0 || text.length <= maxChars) return text
        return text.take(maxChars) + "\n…"
    }

    fun wrapGemmaChat(userMessage: String): String {
        return "<start_of_turn>user\n${userMessage.trim()}\n<end_of_turn>\n<start_of_turn>model\n"
    }

    fun buildSummaryPrompt(
        title: String,
        chaptersText: String,
        userLanguage: Languages,
        maxChars: Int
    ): String {
        val lang = languageName(userLanguage)
        val body = truncate(chaptersText, maxChars)
        val userMessage = """
You are a reading assistant. Summarize the recent chapters below so the reader can refresh their memory before continuing.
Reply in $lang. Be concise (about 150-250 words). Cover key characters, plot points and unresolved threads. Do not invent facts.

Title: $title

Chapters:
$body
""".trimIndent()
        return wrapGemmaChat(userMessage)
    }

    fun buildQaPrompt(
        context: ReadingContext,
        question: String,
        maxChars: Int
    ): String {
        val lang = languageName(context.userLanguage)
        val body = truncate(context.joinedText(), maxChars)
        val userMessage = """
You are a reading assistant for the work "${context.title}".
Answer the user's question using ONLY the context below. If the answer is not in the context, say you do not know based on the available text.
Reply in $lang. Be concise and clear.

Context:
$body

Question: $question
""".trimIndent()
        return wrapGemmaChat(userMessage)
    }
}

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

    fun toGemmaPrompt(request: LlmChatRequest): String {
        val combined = "${request.system.trim()}\n\n${request.user.trim()}"
        return wrapGemmaChat(combined)
    }

    fun buildSummaryRequest(
        title: String,
        chaptersText: String,
        userLanguage: Languages,
        maxChars: Int
    ): LlmChatRequest {
        val lang = languageName(userLanguage)
        val body = truncate(chaptersText, maxChars)
        return LlmChatRequest(
            system = """
You are a reading assistant. Summarize the recent chapters below so the reader can refresh their memory before continuing.
Reply in $lang. Be concise (about 150-250 words). Cover key characters, plot points and unresolved threads. Do not invent facts.
""".trimIndent(),
            user = """
Title: $title

Chapters:
$body
""".trimIndent()
        )
    }

    fun buildQaRequest(
        context: ReadingContext,
        question: String,
        maxChars: Int
    ): LlmChatRequest {
        val lang = languageName(context.userLanguage)
        val body = truncate(context.joinedText(), maxChars)
        return LlmChatRequest(
            system = """
You are a reading assistant for the work "${context.title}".
Answer the user's question using ONLY the context below. If the answer is not in the context, say you do not know based on the available text.
Reply in $lang. Be concise and clear.
""".trimIndent(),
            user = """
Context:
$body

Question: $question
""".trimIndent()
        )
    }

    /** Kept for unit tests / Gemma-only callers. */
    fun buildSummaryPrompt(
        title: String,
        chaptersText: String,
        userLanguage: Languages,
        maxChars: Int
    ): String = toGemmaPrompt(buildSummaryRequest(title, chaptersText, userLanguage, maxChars))

    fun buildQaPrompt(
        context: ReadingContext,
        question: String,
        maxChars: Int
    ): String = toGemmaPrompt(buildQaRequest(context, question, maxChars))
}

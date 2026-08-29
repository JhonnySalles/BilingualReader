package br.com.fenix.bilingualreader.service.llm

import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.Type

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
        if (request.history.isEmpty()) {
            val combined = "${request.system.trim()}\n\n${request.user.trim()}"
            return wrapGemmaChat(combined)
        }
        val sb = StringBuilder()
        sb.append("<start_of_turn>user\n${request.system.trim()}\n<end_of_turn>\n")
        for (msg in request.history) {
            val role = if (msg.role.equals("user", ignoreCase = true)) "user" else "model"
            sb.append("<start_of_turn>$role\n${msg.text.trim()}\n<end_of_turn>\n")
        }
        sb.append("<start_of_turn>user\n${request.user.trim()}\n<end_of_turn>\n<start_of_turn>model\n")
        return sb.toString()
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
        maxChars: Int,
        history: List<LlmChatMessage> = emptyList()
    ): LlmChatRequest {
        val lang = languageName(context.userLanguage)
        val body = truncate(context.joinedText(), maxChars)
        val images = context.chunks.mapNotNull { it.imageBase64 }.filter { it.isNotBlank() }
        val systemPrompt = buildContextAwareSystemPrompt(context, lang)
        return LlmChatRequest(
            system = systemPrompt,
            user = """
Context:
$body

Question: $question
""".trimIndent(),
            history = history,
            imagesBase64 = images
        )
    }

    private fun buildContextAwareSystemPrompt(context: ReadingContext, lang: String): String {
        val base = """
You are a reading assistant for the work "${context.title}".
Answer the user's question using ONLY the context provided below.
Reply in $lang. Be concise and clear.
""".trimIndent()

        val noiseInstructions = """

IMPORTANT INSTRUCTIONS:
- The context may contain OCR artifacts, broken characters, or formatting noise. Ignore these and focus on extracting meaning from recognizable words and sentences.
- If the context is too noisy, garbled, or consists mostly of random characters to understand, respond clearly stating that the text could not be read properly.
- If the answer is not present in the context, say that you could not find the answer in the provided excerpt (translated to $lang).
- Do NOT invent information that is not in the context.
- Do NOT output raw OCR artifacts or meaningless character sequences in your response.
""".trimIndent()

        val typeSpecific = when (context.type) {
            Type.MANGA -> """
- You are analyzing manga/comic pages. The text may come from speech bubbles extracted via OCR and may be fragmented. Try to reconstruct dialogue flow.
- If images are provided, use visual context to complement text understanding.
""".trimIndent()
            Type.BOOK -> """
- You are analyzing an ebook/novel. The text should be mostly well-structured paragraphs.
- Pay attention to chapter titles and narrative flow when answering.
""".trimIndent()
        }

        return "$base\n$noiseInstructions\n$typeSpecific"
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

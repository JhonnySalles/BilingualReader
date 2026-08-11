package br.com.fenix.bilingualreader.service.llm

data class LlmChatMessage(
    val role: String,
    val text: String
)

data class LlmChatRequest(
    val system: String,
    val user: String,
    val history: List<LlmChatMessage> = emptyList()
)

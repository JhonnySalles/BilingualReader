package br.com.fenix.bilingualreader.service.llm

data class LlmChatRequest(
    val system: String,
    val user: String
)

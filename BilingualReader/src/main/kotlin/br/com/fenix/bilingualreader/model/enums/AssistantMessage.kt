package br.com.fenix.bilingualreader.model.enums

enum class AssistantMessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

data class AssistantMessage(
    val role: AssistantMessageRole,
    val text: String
)

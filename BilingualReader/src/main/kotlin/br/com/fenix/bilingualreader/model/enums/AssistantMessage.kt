package br.com.fenix.bilingualreader.model.enums

import java.util.UUID

enum class AssistantMessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

data class AssistantMessage(
    val role: AssistantMessageRole,
    val text: String,
    val id: String = UUID.randomUUID().toString()
)

package br.com.fenix.bilingualreader.service.llm.openrouter

data class OpenRouterMessage(
    val role: String,
    val content: String
)

data class OpenRouterChatRequest(
    val model: String,
    val messages: List<OpenRouterMessage>,
    val stream: Boolean = true,
    val max_tokens: Int = 1024,
    val temperature: Float? = null
)

data class OpenRouterDelta(
    val content: String? = null
)

data class OpenRouterChoice(
    val delta: OpenRouterDelta? = null,
    val message: OpenRouterMessage? = null,
    val finish_reason: String? = null
)

data class OpenRouterStreamChunk(
    val choices: List<OpenRouterChoice>? = null,
    val error: OpenRouterError? = null
)

data class OpenRouterError(
    val message: String? = null,
    val code: String? = null
)

data class OpenRouterModelInfo(
    val id: String,
    val name: String,
    val hasVision: Boolean = false
) {
    fun label(): String {
        val cleanName = name.replace(Regex("\\s*\\(.*?\\)"), "").trim()
        val displayName = if (cleanName.isNotBlank() && cleanName != id) cleanName else id
        return if (displayName.lowercase().endsWith("(free)")) displayName else "$displayName (free)"
    }
}

data class OpenRouterModelsResponse(
    val data: List<OpenRouterModelEntry>? = null
)

data class OpenRouterModelEntry(
    val id: String? = null,
    val name: String? = null,
    val pricing: OpenRouterPricing? = null,
    val architecture: OpenRouterArchitecture? = null
)

data class OpenRouterArchitecture(
    val modality: String? = null,
    val input_modalities: List<String>? = null
)

data class OpenRouterPricing(
    val prompt: String? = null,
    val completion: String? = null
)


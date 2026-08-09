package br.com.fenix.bilingualreader.service.llm

import kotlinx.coroutines.flow.Flow

interface LlmBackend {
    val isCloud: Boolean
    suspend fun ensureReady()
    fun generateStreaming(request: LlmChatRequest): Flow<Pair<String, Boolean>>
}

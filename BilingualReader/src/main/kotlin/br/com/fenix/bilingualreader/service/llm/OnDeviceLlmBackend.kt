package br.com.fenix.bilingualreader.service.llm

import android.content.Context
import kotlinx.coroutines.flow.Flow

class OnDeviceLlmBackend(private val context: Context) : LlmBackend {

    override val isCloud: Boolean = false

    override suspend fun ensureReady() {
        if (!LlmInferenceEngine.isNativeBackendAvailable()) {
            throw LlmUnsupportedDeviceException()
        }
        val manager = LlmModelManager.getInstance(context)
        if (!manager.isModelReady()) {
            manager.ensureModel()
        }
        LlmInferenceEngine.getInstance(context)
            .ensureLoaded(manager.getModelFile().absolutePath)
    }

    override fun generateStreaming(request: LlmChatRequest): Flow<Pair<String, Boolean>> {
        val prompt = LlmPromptBuilder.toGemmaPrompt(request)
        return LlmInferenceEngine.getInstance(context).generateStreamingTokens(prompt)
    }
}

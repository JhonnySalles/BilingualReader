package br.com.fenix.bilingualreader.service.llm

import android.content.Context
import br.com.fenix.bilingualreader.BuildConfig
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference

class LlmInferenceEngine(private val context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(LlmInferenceEngine::class.java)
    private val mutex = Mutex()
    private var llmInference: LlmInference? = null
    private var loadedModelPath: String? = null
    private val streamListener = AtomicReference<((String, Boolean) -> Unit)?>(null)

    suspend fun ensureLoaded(modelPath: String) = mutex.withLock {
        if (BuildConfig.USE_MOCK_LLM) return@withLock
        if (llmInference != null && loadedModelPath == modelPath) return@withLock
        closeLocked()
        withContext(Dispatchers.IO) {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(1024)
                .setMaxTopK(40)
                .setPreferredBackend(LlmInference.Backend.GPU)
                .setResultListener { partialResult, done ->
                    streamListener.get()?.invoke(partialResult, done)
                }
                .build()
            llmInference = LlmInference.createFromOptions(context, options)
            loadedModelPath = modelPath
            mLOGGER.info("LLM loaded from $modelPath")
        }
    }

    suspend fun generate(prompt: String): String = mutex.withLock {
        if (BuildConfig.USE_MOCK_LLM) return@withLock mockResponse(prompt)
        val engine = llmInference ?: throw IllegalStateException("LLM not loaded")
        withContext(Dispatchers.IO) { engine.generateResponse(prompt) }
    }

    fun generateStreamingTokens(prompt: String): Flow<Pair<String, Boolean>> = callbackFlow {
        if (BuildConfig.USE_MOCK_LLM) {
            val mock = mockResponse(prompt)
            val words = mock.split(" ")
            val builder = StringBuilder()
            for ((index, word) in words.withIndex()) {
                if (builder.isNotEmpty()) builder.append(' ')
                builder.append(word)
                trySend(builder.toString() to (index == words.lastIndex))
                delay(40)
            }
            close()
            return@callbackFlow
        }

        val engine = mutex.withLock {
            llmInference ?: throw IllegalStateException("LLM not loaded")
        }

        val accumulated = StringBuilder()
        streamListener.set { partialResult, done ->
            accumulated.append(partialResult)
            trySend(accumulated.toString() to done)
            if (done) close()
        }

        try {
            engine.generateResponseAsync(prompt)
        } catch (e: Exception) {
            streamListener.set(null)
            close(e)
        }

        awaitClose { streamListener.set(null) }
    }.flowOn(Dispatchers.IO)

    fun close() {
        runBlocking {
            mutex.withLock { closeLocked() }
        }
    }

    private fun closeLocked() {
        streamListener.set(null)
        try {
            llmInference?.close()
        } catch (e: Exception) {
            mLOGGER.warn("Error closing LLM: ${e.message}")
        }
        llmInference = null
        loadedModelPath = null
    }

    private fun mockResponse(prompt: String): String {
        val isSummary = prompt.contains("Summarize", ignoreCase = true)
        return if (isSummary) {
            "Resumo (mock): Nos capítulos recentes, os protagonistas enfrentam um conflito importante, novas pistas surgem e alguns mistérios permanecem em aberto para o próximo capítulo."
        } else {
            "Resposta (mock): Com base no contexto disponível, não há informação suficiente para responder com certeza. Continue a leitura para mais detalhes."
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: LlmInferenceEngine? = null

        fun getInstance(context: Context): LlmInferenceEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LlmInferenceEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

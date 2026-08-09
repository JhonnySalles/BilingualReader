package br.com.fenix.bilingualreader.service.llm

import android.content.Context
import android.os.Build
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.atomic.AtomicReference

class LlmUnsupportedDeviceException : IllegalStateException("LLM_UNSUPPORTED_DEVICE")

class LlmInferenceEngine(private val context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(LlmInferenceEngine::class.java)
    private val mutex = Mutex()
    private var llmInference: LlmInference? = null
    private var loadedModelPath: String? = null
    private val streamListener = AtomicReference<((String, Boolean) -> Unit)?>(null)

    suspend fun ensureLoaded(modelPath: String) = mutex.withLock {
        if (llmInference != null && loadedModelPath == modelPath) return@withLock

        // Check ABI before any reference to LlmInference (its <clinit> loads native .so).
        if (!isNativeBackendAvailable()) {
            throw LlmUnsupportedDeviceException()
        }

        val modelFile = File(modelPath)
        if (!modelFile.exists() || modelFile.length() < MIN_MODEL_BYTES) {
            throw IllegalStateException("LLM model file missing or invalid: $modelPath")
        }

        closeLocked()
        withContext(Dispatchers.IO) {
            try {
                llmInference = createInference(modelPath, LlmInference.Backend.GPU)
                    ?: createInference(modelPath, LlmInference.Backend.CPU)
                    ?: throw IllegalStateException("Failed to load LLM on GPU and CPU")
                loadedModelPath = modelPath
                mLOGGER.info("LLM loaded from $modelPath")
            } catch (e: LlmUnsupportedDeviceException) {
                throw e
            } catch (e: Throwable) {
                if (isNativeLinkFailure(e)) throw LlmUnsupportedDeviceException()
                throw if (e is Exception) e else IllegalStateException(e.message, e)
            }
        }
    }

    private fun createInference(modelPath: String, backend: LlmInference.Backend): LlmInference? {
        return try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(1024)
                .setMaxTopK(40)
                .setPreferredBackend(backend)
                .setResultListener { partialResult, done ->
                    streamListener.get()?.invoke(partialResult, done)
                }
                .build()
            LlmInference.createFromOptions(context, options).also {
                mLOGGER.info("LLM backend $backend ready")
            }
        } catch (e: Throwable) {
            mLOGGER.warn("LLM backend $backend failed: ${e.message}")
            if (isNativeLinkFailure(e)) throw LlmUnsupportedDeviceException()
            null
        }
    }

    suspend fun generate(prompt: String): String = mutex.withLock {
        val engine = llmInference ?: throw IllegalStateException("LLM not loaded")
        withContext(Dispatchers.IO) { engine.generateResponse(prompt) }
    }

    fun generateStreamingTokens(prompt: String): Flow<Pair<String, Boolean>> = callbackFlow {
        val engine = mutex.withLock {
            llmInference ?: throw IllegalStateException("LLM not loaded")
        }

        // MediaPipe may deliver either token deltas or the full accumulated string.
        // Prefer delta append; if a chunk already starts with the previous text, treat as full.
        var previous = ""
        streamListener.set { partialResult, done ->
            val text = when {
                previous.isEmpty() -> partialResult
                partialResult.startsWith(previous) -> partialResult
                else -> previous + partialResult
            }
            previous = text
            trySend(text to done)
            if (done) close()
        }

        try {
            engine.generateResponseAsync(prompt)
        } catch (e: Throwable) {
            streamListener.set(null)
            close(if (e is Exception) e else IllegalStateException(e.message, e))
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

    companion object {
        private const val MIN_MODEL_BYTES = 10L * 1024L * 1024L
        private const val REQUIRED_ABI = "arm64-v8a"

        @Volatile
        private var INSTANCE: LlmInferenceEngine? = null

        fun isNativeBackendAvailable(): Boolean {
            return Build.SUPPORTED_ABIS.any { it == REQUIRED_ABI }
        }

        fun isNativeLinkFailure(error: Throwable): Boolean {
            var current: Throwable? = error
            while (current != null) {
                if (current is UnsatisfiedLinkError) return true
                if (current.message?.contains("libllm_inference_engine_jni", ignoreCase = true) == true) {
                    return true
                }
                current = current.cause
            }
            return false
        }

        fun getInstance(context: Context): LlmInferenceEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LlmInferenceEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

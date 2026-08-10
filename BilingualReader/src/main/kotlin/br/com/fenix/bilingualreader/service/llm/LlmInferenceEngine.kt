package br.com.fenix.bilingualreader.service.llm

import android.content.Context
import android.os.Build
import br.com.fenix.bilingualreader.service.llm.LlmInferenceEngine.Companion.isNativeBackendAvailable
import br.com.fenix.bilingualreader.util.helpers.LlmSettings
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

/**
 * MediaPipe LlmInference is never referenced as a field type here: its <clinit>
 * calls System.loadLibrary and would crash before [isNativeBackendAvailable] can run.
 */
class LlmInferenceEngine(private val context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(LlmInferenceEngine::class.java)
    private val mutex = Mutex()
    private var loaded: MediapipeLlmBridge.LoadedEngine? = null
    private var loadedModelPath: String? = null
    private val streamListener = AtomicReference<((String, Boolean) -> Unit)?>(null)

    suspend fun ensureLoaded(modelPath: String) = mutex.withLock {
        if (loaded != null && loadedModelPath == modelPath) return@withLock

        // Check ABI before any reference to LlmInference (its <clinit> loads native .so).
        if (!isNativeBackendAvailable()) {
            throw LlmUnsupportedDeviceException()
        }

        val modelFile = File(modelPath)
        if (!modelFile.exists() || !isModelSizeValid(modelFile.length())) {
            throw IllegalStateException("LLM model file missing or invalid: $modelPath")
        }

        closeLocked()
        withContext(Dispatchers.IO) {
            try {
                loaded = MediapipeLlmBridge.create(context, modelPath)
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

    suspend fun generate(prompt: String): String = mutex.withLock {
        val engine = loaded ?: throw IllegalStateException("LLM not loaded")
        withContext(Dispatchers.IO) { MediapipeLlmBridge.generate(context, engine, prompt) }
    }

    fun generateStreamingTokens(prompt: String): Flow<Pair<String, Boolean>> = callbackFlow {
        val engine = mutex.withLock {
            loaded ?: throw IllegalStateException("LLM not loaded")
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
            MediapipeLlmBridge.generateAsync(context, engine, prompt) { partial, done ->
                streamListener.get()?.invoke(partial, done)
            }
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
            loaded?.let { MediapipeLlmBridge.close(it) }
        } catch (e: Exception) {
            mLOGGER.warn("Error closing LLM: ${e.message}")
        }
        loaded = null
        loadedModelPath = null
    }

    companion object {
        /** Gemma3-1B-IT-INT4 .task is ~555 MB; reject truncated or wrong copies. */
        const val MIN_MODEL_BYTES = 500L * 1024L * 1024L
        const val MAX_MODEL_BYTES = 600L * 1024L * 1024L
        private const val REQUIRED_ABI = "arm64-v8a"

        @Volatile
        private var INSTANCE: LlmInferenceEngine? = null

        fun isModelSizeValid(bytes: Long): Boolean =
            bytes in MIN_MODEL_BYTES..MAX_MODEL_BYTES

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

        fun isModelIncompatibleFailure(error: Throwable): Boolean {
            var current: Throwable? = error
            while (current != null) {
                val msg = current.message.orEmpty()
                if (msg.contains("CalculatorGraph", ignoreCase = true) ||
                    msg.contains("prefill_input", ignoreCase = true) ||
                    msg.contains("RET_CHECK", ignoreCase = true) ||
                    msg.contains("TfLitePrefillDecode", ignoreCase = true)
                ) {
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

/**
 * Isolates MediaPipe class references so [LlmInferenceEngine] can load without
 * triggering LlmInference <clinit> / System.loadLibrary.
 */
private object MediapipeLlmBridge {
    private val mLOGGER = LoggerFactory.getLogger(MediapipeLlmBridge::class.java)

    private const val MAX_TOKENS = 1024
    private const val TOP_K = 40

    class LoadedEngine(
        val inference: com.google.mediapipe.tasks.genai.llminference.LlmInference,
        @Volatile var session: com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
    )

    enum class Backend { GPU, CPU }

    fun create(context: Context, modelPath: String): LoadedEngine {
        var lastError: Throwable? = null
        for (backend in listOf(Backend.GPU, Backend.CPU)) {
            try {
                return createWithBackend(context, modelPath, backend).also {
                    mLOGGER.info("LLM backend $backend ready")
                }
            } catch (e: Throwable) {
                mLOGGER.warn("LLM backend $backend failed: ${e.message}", e)
                if (LlmInferenceEngine.isNativeLinkFailure(e)) throw LlmUnsupportedDeviceException()
                lastError = e
            }
        }
        val cause = lastError
        throw IllegalStateException(
            cause?.message ?: "Failed to load LLM on GPU and CPU",
            cause
        )
    }

    private fun createWithBackend(
        context: Context,
        modelPath: String,
        backend: Backend
    ): LoadedEngine {
        val llmBackend = when (backend) {
            Backend.GPU -> com.google.mediapipe.tasks.genai.llminference.LlmInference.Backend.GPU
            Backend.CPU -> com.google.mediapipe.tasks.genai.llminference.LlmInference.Backend.CPU
        }
        val options = com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(MAX_TOKENS)
            .setPreferredBackend(llmBackend)
            .build()
        val inference =
            com.google.mediapipe.tasks.genai.llminference.LlmInference.createFromOptions(context, options)
        val session = createSession(inference, LlmSettings.temperature(context))
        return LoadedEngine(inference, session)
    }

    private fun createSession(
        inference: com.google.mediapipe.tasks.genai.llminference.LlmInference,
        temperature: Float
    ): com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession {
        val sessionOptions =
            com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession.LlmInferenceSessionOptions.builder()
                .setTemperature(temperature)
                .setTopK(TOP_K)
                .build()
        return com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession.createFromOptions(
            inference,
            sessionOptions
        )
    }

    fun generate(context: Context, engine: LoadedEngine, prompt: String): String {
        recreateSession(context, engine)
        engine.session.addQueryChunk(prompt)
        return engine.session.generateResponse()
    }

    fun generateAsync(
        context: Context,
        engine: LoadedEngine,
        prompt: String,
        listener: (String, Boolean) -> Unit
    ) {
        recreateSession(context, engine)
        engine.session.addQueryChunk(prompt)
        engine.session.generateResponseAsync { partialResult, done ->
            listener(partialResult, done)
        }
    }

    private fun recreateSession(context: Context, engine: LoadedEngine) {
        try {
            engine.session.close()
        } catch (e: Exception) {
            mLOGGER.warn("Error closing LLM session before recreate: ${e.message}")
        }
        engine.session = createSession(engine.inference, LlmSettings.temperature(context))
    }

    fun close(engine: LoadedEngine) {
        try {
            engine.session.close()
        } catch (_: Exception) {
        }
        try {
            engine.inference.close()
        } catch (_: Exception) {
        }
    }
}

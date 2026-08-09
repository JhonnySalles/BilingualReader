package br.com.fenix.bilingualreader.service.llm

import android.content.Context
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.coroutineContext

sealed class ModelPrepareState {
    data object Idle : ModelPrepareState()
    data object Ready : ModelPrepareState()
    data class Extracting(val progress: Int, val bytesCopied: Long, val totalBytes: Long) : ModelPrepareState()
    data class Error(val message: String) : ModelPrepareState()
}

class LlmModelManager(private val context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(LlmModelManager::class.java)

    private val _state = MutableStateFlow<ModelPrepareState>(ModelPrepareState.Idle)
    val state: StateFlow<ModelPrepareState> = _state

    @Volatile
    private var cancelRequested = false

    fun getModelDir(): File {
        val dir = File(context.filesDir, GeneralConsts.CACHE_FOLDER.LLM)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getModelFile(): File {
        val prefs = GeneralConsts.getSharedPreferences(context)
        val custom = prefs.getString(GeneralConsts.KEYS.LLM.MODEL_PATH, null)
        if (!custom.isNullOrBlank()) {
            val file = File(custom)
            if (file.exists()) return file
        }
        val version = prefs.getString(
            GeneralConsts.KEYS.LLM.MODEL_VERSION,
            GeneralConsts.KEYS.LLM.DEFAULT_MODEL_VERSION
        ) ?: GeneralConsts.KEYS.LLM.DEFAULT_MODEL_VERSION
        return File(getModelDir(), "$version.task")
    }

    fun isModelReady(): Boolean {
        val file = getModelFile()
        return file.exists() && file.length() > MIN_MODEL_BYTES
    }

    fun getModelSizeBytes(): Long = if (isModelReady()) getModelFile().length() else 0L

    fun refreshState() {
        _state.value = if (isModelReady()) ModelPrepareState.Ready else ModelPrepareState.Idle
    }

    fun cancelExtract() {
        cancelRequested = true
    }

    fun deleteModel(): Boolean {
        return try {
            val file = getModelFile()
            val deleted = !file.exists() || file.delete()
            GeneralConsts.getSharedPreferences(context).edit()
                .putBoolean(GeneralConsts.KEYS.LLM.MODEL_EXTRACTED, false)
                .remove(GeneralConsts.KEYS.LLM.MODEL_PATH)
                .apply()
            _state.value = ModelPrepareState.Idle
            deleted
        } catch (e: Exception) {
            mLOGGER.error("Failed to delete LLM model copy", e)
            false
        }
    }

    suspend fun ensureModel(onProgress: ((Int) -> Unit)? = null): File = withContext(Dispatchers.IO) {
        if (isModelReady()) {
            _state.value = ModelPrepareState.Ready
            return@withContext getModelFile()
        }
        extractFromAssets(onProgress)
    }

    private suspend fun extractFromAssets(onProgress: ((Int) -> Unit)?): File {
        cancelRequested = false
        val prefs = GeneralConsts.getSharedPreferences(context)
        val assetPath = GeneralConsts.KEYS.LLM.ASSET_MODEL_PATH
        val target = getModelFile()
        val partial = File(target.parentFile, target.name + ".partial")

        mLOGGER.info("Extracting LLM model from assets/$assetPath to ${target.absolutePath}")
        _state.value = ModelPrepareState.Extracting(0, 0, 0)

        try {
            val assetFd = try {
                context.assets.openFd(assetPath)
            } catch (_: Exception) {
                null
            }
            val total = assetFd?.length ?: -1L
            assetFd?.close()

            context.assets.open(assetPath).use { input ->
                FileOutputStream(partial).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER)
                    var copied = 0L
                    var lastProgress = -1
                    while (true) {
                        coroutineContext.ensureActive()
                        if (cancelRequested) throw InterruptedException("Extract cancelled")
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        copied += read
                        val progress = if (total > 0) ((copied * 100) / total).toInt().coerceIn(0, 100) else 0
                        if (progress != lastProgress) {
                            lastProgress = progress
                            _state.value = ModelPrepareState.Extracting(progress, copied, total.coerceAtLeast(0))
                            onProgress?.invoke(progress)
                        }
                    }
                    output.flush()
                }
            }

            if (partial.length() < MIN_MODEL_BYTES) {
                partial.delete()
                throw IllegalStateException("Extracted model is too small")
            }

            if (target.exists()) target.delete()
            if (!partial.renameTo(target)) {
                partial.copyTo(target, overwrite = true)
                partial.delete()
            }

            prefs.edit()
                .putBoolean(GeneralConsts.KEYS.LLM.MODEL_EXTRACTED, true)
                .putString(GeneralConsts.KEYS.LLM.MODEL_PATH, target.absolutePath)
                .putString(
                    GeneralConsts.KEYS.LLM.MODEL_VERSION,
                    prefs.getString(
                        GeneralConsts.KEYS.LLM.MODEL_VERSION,
                        GeneralConsts.KEYS.LLM.DEFAULT_MODEL_VERSION
                    )
                )
                .apply()

            _state.value = ModelPrepareState.Ready
            return target
        } catch (e: Exception) {
            mLOGGER.error("LLM model extract failed: ${e.message}", e)
            partial.delete()
            _state.value = ModelPrepareState.Error(e.message ?: "Extract failed")
            throw e
        }
    }

    companion object {
        private const val MIN_MODEL_BYTES = 10L * 1024L * 1024L
        private const val DEFAULT_BUFFER = 64 * 1024

        @Volatile
        private var INSTANCE: LlmModelManager? = null

        fun getInstance(context: Context): LlmModelManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LlmModelManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

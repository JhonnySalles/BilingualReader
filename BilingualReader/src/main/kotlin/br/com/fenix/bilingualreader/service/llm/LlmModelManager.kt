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
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

sealed class ModelDownloadState {
    data object Idle : ModelDownloadState()
    data object Ready : ModelDownloadState()
    data class Downloading(val progress: Int, val bytesDownloaded: Long, val totalBytes: Long) : ModelDownloadState()
    data class Error(val message: String) : ModelDownloadState()
}

class LlmModelManager(private val context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(LlmModelManager::class.java)

    private val _state = MutableStateFlow<ModelDownloadState>(ModelDownloadState.Idle)
    val state: StateFlow<ModelDownloadState> = _state

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
        _state.value = if (isModelReady()) ModelDownloadState.Ready else ModelDownloadState.Idle
    }

    fun cancelDownload() {
        cancelRequested = true
    }

    fun deleteModel(): Boolean {
        return try {
            val file = getModelFile()
            val deleted = !file.exists() || file.delete()
            GeneralConsts.getSharedPreferences(context).edit()
                .putBoolean(GeneralConsts.KEYS.LLM.MODEL_DOWNLOADED, false)
                .remove(GeneralConsts.KEYS.LLM.MODEL_PATH)
                .apply()
            _state.value = ModelDownloadState.Idle
            deleted
        } catch (e: Exception) {
            mLOGGER.error("Failed to delete LLM model", e)
            false
        }
    }

    suspend fun ensureModel(onProgress: ((Int) -> Unit)? = null): File = withContext(Dispatchers.IO) {
        if (isModelReady()) {
            _state.value = ModelDownloadState.Ready
            return@withContext getModelFile()
        }
        downloadModel(onProgress)
    }

    private suspend fun downloadModel(onProgress: ((Int) -> Unit)?): File {
        cancelRequested = false
        val prefs = GeneralConsts.getSharedPreferences(context)
        val urlString = prefs.getString(
            GeneralConsts.KEYS.LLM.MODEL_URL,
            GeneralConsts.KEYS.LLM.DEFAULT_MODEL_URL
        ) ?: GeneralConsts.KEYS.LLM.DEFAULT_MODEL_URL

        val target = getModelFile()
        val partial = File(target.parentFile, target.name + ".partial")

        mLOGGER.info("Downloading LLM model from $urlString to ${target.absolutePath}")
        _state.value = ModelDownloadState.Downloading(0, 0, 0)

        var connection: HttpURLConnection? = null
        try {
            connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout = 60_000
                instanceFollowRedirects = true
                requestMethod = "GET"
                setRequestProperty("User-Agent", "BilingualReader")
            }
            connection.connect()
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code downloading model")
            }

            val total = connection.contentLengthLong.coerceAtLeast(0)
            connection.inputStream.use { input ->
                FileOutputStream(partial).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER)
                    var downloaded = 0L
                    var lastProgress = -1
                    while (true) {
                        coroutineContext.ensureActive()
                        if (cancelRequested) throw InterruptedException("Download cancelled")
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        val progress = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                        if (progress != lastProgress) {
                            lastProgress = progress
                            _state.value = ModelDownloadState.Downloading(progress, downloaded, total)
                            onProgress?.invoke(progress)
                        }
                    }
                    output.flush()
                }
            }

            if (partial.length() < MIN_MODEL_BYTES) {
                partial.delete()
                throw IllegalStateException("Downloaded model is too small")
            }

            if (target.exists()) target.delete()
            if (!partial.renameTo(target)) {
                partial.copyTo(target, overwrite = true)
                partial.delete()
            }

            prefs.edit()
                .putBoolean(GeneralConsts.KEYS.LLM.MODEL_DOWNLOADED, true)
                .putString(GeneralConsts.KEYS.LLM.MODEL_PATH, target.absolutePath)
                .putString(
                    GeneralConsts.KEYS.LLM.MODEL_VERSION,
                    prefs.getString(
                        GeneralConsts.KEYS.LLM.MODEL_VERSION,
                        GeneralConsts.KEYS.LLM.DEFAULT_MODEL_VERSION
                    )
                )
                .apply()

            _state.value = ModelDownloadState.Ready
            return target
        } catch (e: Exception) {
            mLOGGER.error("LLM model download failed: ${e.message}", e)
            partial.delete()
            _state.value = ModelDownloadState.Error(e.message ?: "Download failed")
            throw e
        } finally {
            connection?.disconnect()
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

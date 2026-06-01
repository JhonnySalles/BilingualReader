package br.com.fenix.bilingualreader.util.helpers

import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import org.slf4j.LoggerFactory
import br.com.fenix.bilingualreader.BuildConfig

object Telemetry {
    private val mLOGGER = LoggerFactory.getLogger(Telemetry::class.java)
    
    var isEnabled = true

    fun recordException(e: Throwable, message: String? = null) {
        if (BuildConfig.DEBUG)
            return

        if (!isEnabled) {
            mLOGGER.warn("Telemetry disabled. Exception ignored: ${e.message}")
            return
        }

        try {
            Firebase.crashlytics.apply {
                message?.let { setCustomKey("message", it) }
                recordException(e)
            }
        } catch (ex: Throwable) {
            mLOGGER.error("Telemetry: Failed to record exception: ${ex.message}")
        }
    }

    fun setCustomKey(key: String, value: String) {
        if (!isEnabled) return
        try {
            Firebase.crashlytics.setCustomKey(key, value)
        } catch (ex: Throwable) {
            mLOGGER.error("Telemetry: Failed to set custom key: ${ex.message}")
        }
    }
}

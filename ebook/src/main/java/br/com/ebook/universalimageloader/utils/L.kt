/*******************************************************************************
 * Copyright 2011-2014 Sergey Tarasevich
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package br.com.ebook.universalimageloader.utils

import android.util.Log
import br.com.ebook.universalimageloader.core.ImageLoader

/**
 * "Less-word" analog of Android [logger][android.util.Log]
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.6.4
 */
object L {
    private const val LOG_FORMAT = "%1\$s\n%2\$s"

    @Volatile
    private var writeDebugLogs = false

    @Volatile
    private var writeLogs = true

    /**
     * Enables logger (if [.disableLogging] was called before)
     *
     */
    @Deprecated("Use {@link #writeLogs(boolean) writeLogs(true)} instead")
    fun enableLogging() {
        writeLogs(true)
    }

    /**
     * Disables logger, no logs will be passed to LogCat, all log methods will do nothing
     *
     */
    @JvmStatic
	@Deprecated("Use {@link #writeLogs(boolean) writeLogs(false)} instead")
    fun disableLogging() {
        writeLogs(false)
    }

    @JvmStatic
	fun writeDebugLogs(writeDebugLogs: Boolean) {
        L.writeDebugLogs = writeDebugLogs
    }

    fun writeLogs(writeLogs: Boolean) {
        L.writeLogs = writeLogs
    }

    @JvmStatic
	fun d(message: String?, vararg args: Any?) {
        if (writeDebugLogs) {
            log(Log.DEBUG, null, message, *args)
        }
    }

    @JvmStatic
    fun i(message: String?, vararg args: Any?) {
        log(Log.INFO, null, message, *args)
    }

    @JvmStatic
	fun w(message: String?, vararg args: Any?) {
        log(Log.WARN, null, message, *args)
    }

    @JvmStatic
    fun e(ex: Throwable?) {
        log(Log.ERROR, ex, null)
    }

    @JvmStatic
    fun e(message: String?, vararg args: Any?) {
        log(Log.ERROR, null, message, *args)
    }

    @JvmStatic
    fun e(ex: Throwable?, message: String?, vararg args: Any?) {
        log(Log.ERROR, ex, message, *args)
    }

    private fun log(priority: Int, ex: Throwable?, message: String?, vararg args: Any?) {
        var message = message
        if (!writeLogs) return
        if (args.size > 0) {
            message = String.format(message!!, *args)
        }
        val log: String?
        log = if (ex == null) {
            message
        } else {
            val logMessage = message ?: ex.message
            val logBody = Log.getStackTraceString(ex)
            String.format(LOG_FORMAT, logMessage, logBody)
        }
        Log.println(priority, ImageLoader.TAG, log!!)
    }
}
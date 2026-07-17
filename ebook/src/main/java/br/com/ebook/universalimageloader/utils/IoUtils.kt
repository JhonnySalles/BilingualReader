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

import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Provides I/O operations
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.0.0
 */
object IoUtils {
    /** {@value}  */
    const val DEFAULT_BUFFER_SIZE = 32 * 1024 // 32 KB

    /** {@value}  */
    const val DEFAULT_IMAGE_TOTAL_SIZE = 500 * 1024 // 500 Kb

    /** {@value}  */
    const val CONTINUE_LOADING_PERCENTAGE = 75
    /**
     * Copies stream, fires progress events by listener, can be interrupted by listener.
     *
     * @param is         Input stream
     * @param os         Output stream
     * @param listener   null-ok; Listener of copying progress and controller of copying interrupting
     * @param bufferSize Buffer size for copying, also represents a step for firing progress listener callback, i.e.
     * progress event will be fired after every copied **bufferSize** bytes
     * @return **true** - if stream copied successfully; **false** - if copying was interrupted by listener
     * @throws IOException
     */
    /**
     * Copies stream, fires progress events by listener, can be interrupted by listener. Uses buffer size =
     * {@value #DEFAULT_BUFFER_SIZE} bytes.
     *
     * @param is       Input stream
     * @param os       Output stream
     * @param listener null-ok; Listener of copying progress and controller of copying interrupting
     * @return **true** - if stream copied successfully; **false** - if copying was interrupted by listener
     * @throws IOException
     */
    @JvmStatic
	@JvmOverloads
    @Throws(IOException::class)
    fun copyStream(`is`: InputStream, os: OutputStream, listener: CopyListener?, bufferSize: Int = DEFAULT_BUFFER_SIZE): Boolean {
        var current = 0
        var total = `is`.available()
        if (total <= 0) {
            total = DEFAULT_IMAGE_TOTAL_SIZE
        }
        val bytes = ByteArray(bufferSize)
        var count: Int
        if (shouldStopLoading(listener, current, total)) return false
        while (`is`.read(bytes, 0, bufferSize).also { count = it } != -1) {
            os.write(bytes, 0, count)
            current += count
            if (shouldStopLoading(listener, current, total)) return false
        }
        os.flush()
        return true
    }

    private fun shouldStopLoading(listener: CopyListener?, current: Int, total: Int): Boolean {
        if (listener != null) {
            val shouldContinue = listener.onBytesCopied(current, total)
            if (!shouldContinue) {
                if (100 * current / total < CONTINUE_LOADING_PERCENTAGE) {
                    return true // if loaded more than 75% then continue loading anyway
                }
            }
        }
        return false
    }

    /**
     * Reads all data from stream and close it silently
     *
     * @param is Input stream
     */
	@JvmStatic
	fun readAndCloseStream(`is`: InputStream) {
        val bytes = ByteArray(DEFAULT_BUFFER_SIZE)
        try {
            while (`is`.read(bytes, 0, DEFAULT_BUFFER_SIZE) != -1);
        } catch (ignored: IOException) {
        } finally {
            closeSilently(`is`)
        }
    }

    @JvmStatic
	fun closeSilently(closeable: Closeable?) {
        if (closeable != null) {
            try {
                closeable.close()
            } catch (ignored: Exception) {
            }
        }
    }

    /** Listener and controller for copy process  */
    interface CopyListener {
        /**
         * @param current Loaded bytes
         * @param total   Total bytes for loading
         * @return **true** - if copying should be continued; **false** - if copying should be interrupted
         */
        fun onBytesCopied(current: Int, total: Int): Boolean
    }
}
/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package br.com.ebook.universalimageloader.cache.disc.impl.ext

import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.Reader
import java.io.StringWriter
import java.nio.charset.Charset

/** Junk drawer of utility methods.  */
internal object Util {
    @JvmField
	val US_ASCII = Charset.forName("US-ASCII")
    @JvmField
	val UTF_8 = Charset.forName("UTF-8")
    @JvmStatic
	@Throws(IOException::class)
    fun readFully(reader: Reader): String {
        return try {
            val writer = StringWriter()
            val buffer = CharArray(1024)
            var count: Int
            while (reader.read(buffer).also { count = it } != -1) {
                writer.write(buffer, 0, count)
            }
            writer.toString()
        } finally {
            reader.close()
        }
    }

    /**
     * Deletes the contents of `dir`. Throws an IOException if any file
     * could not be deleted, or if `dir` is not a readable directory.
     */
    @JvmStatic
	@Throws(IOException::class)
    fun deleteContents(dir: File) {
        val files = dir.listFiles() ?: throw IOException("not a readable directory: $dir")
        for (file in files) {
            if (file.isDirectory) {
                deleteContents(file)
            }
            if (!file.delete()) {
                throw IOException("failed to delete file: $file")
            }
        }
    }

    @JvmStatic
	fun closeQuietly( /*Auto*/
                      closeable: Closeable?
    ) {
        if (closeable != null) {
            try {
                closeable.close()
            } catch (rethrown: RuntimeException) {
                throw rethrown
            } catch (ignored: Exception) {
            }
        }
    }
}
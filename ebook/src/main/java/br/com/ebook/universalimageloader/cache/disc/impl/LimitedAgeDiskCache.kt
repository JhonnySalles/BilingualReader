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
package br.com.ebook.universalimageloader.cache.disc.impl

import android.graphics.Bitmap
import br.com.ebook.universalimageloader.cache.disc.naming.FileNameGenerator
import br.com.ebook.universalimageloader.core.DefaultConfigurationFactory
import br.com.ebook.universalimageloader.utils.IoUtils.CopyListener
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.Collections

/**
 * Cache which deletes files which were loaded more than defined time. Cache size is unlimited.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.3.1
 */
class LimitedAgeDiskCache(cacheDir: File?, reserveCacheDir: File?, fileNameGenerator: FileNameGenerator?, maxAge: Long) :
    BaseDiskCache(cacheDir, reserveCacheDir, fileNameGenerator) {
    private val maxFileAge: Long
    private val loadingDates = Collections.synchronizedMap(HashMap<File, Long>())

    /**
     * @param cacheDir Directory for file caching
     * @param maxAge   Max file age (in seconds). If file age will exceed this value then it'll be removed on next
     * treatment (and therefore be reloaded).
     */
    constructor(cacheDir: File?, maxAge: Long) : this(cacheDir, null, DefaultConfigurationFactory.createFileNameGenerator(), maxAge) {}

    /**
     * @param cacheDir Directory for file caching
     * @param maxAge   Max file age (in seconds). If file age will exceed this value then it'll be removed on next
     * treatment (and therefore be reloaded).
     */
    constructor(cacheDir: File?, reserveCacheDir: File?, maxAge: Long) : this(
        cacheDir,
        reserveCacheDir,
        DefaultConfigurationFactory.createFileNameGenerator(),
        maxAge
    ) {
    }

    /**
     * @param cacheDir          Directory for file caching
     * @param reserveCacheDir   null-ok; Reserve directory for file caching. It's used when the primary directory isn't available.
     * @param fileNameGenerator Name generator for cached files
     * @param maxAge            Max file age (in seconds). If file age will exceed this value then it'll be removed on next
     * treatment (and therefore be reloaded).
     */
    init {
        maxFileAge = maxAge * 1000 // to milliseconds
    }

    override fun get(imageUri: String?): File? {
        val file = super.get(imageUri)
        if (file != null && file.exists()) {
            val cached: Boolean
            var loadingDate = loadingDates[file]
            if (loadingDate == null) {
                cached = false
                loadingDate = file.lastModified()
            } else {
                cached = true
            }
            if (System.currentTimeMillis() - loadingDate > maxFileAge) {
                file.delete()
                loadingDates.remove(file)
            } else if (!cached) {
                loadingDates[file] = loadingDate
            }
        }
        return file
    }

    @Throws(IOException::class)
    override fun save(imageUri: String?, imageStream: InputStream?, listener: CopyListener?): Boolean {
        val saved = super.save(imageUri, imageStream, listener)
        rememberUsage(imageUri)
        return saved
    }

    @Throws(IOException::class)
    override fun save(imageUri: String?, bitmap: Bitmap?): Boolean {
        val saved = super.save(imageUri, bitmap)
        rememberUsage(imageUri)
        return saved
    }

    override fun remove(imageUri: String?): Boolean {
        loadingDates.remove(getFile(imageUri))
        return super.remove(imageUri)
    }

    override fun clear() {
        super.clear()
        loadingDates.clear()
    }

    private fun rememberUsage(imageUri: String?) {
        val file = getFile(imageUri)
        val currentTime = System.currentTimeMillis()
        file.setLastModified(currentTime)
        loadingDates[file] = currentTime
    }
}
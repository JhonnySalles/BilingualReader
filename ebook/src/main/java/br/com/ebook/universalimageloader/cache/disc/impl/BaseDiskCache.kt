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
import br.com.ebook.universalimageloader.cache.disc.DiskCache
import br.com.ebook.universalimageloader.cache.disc.naming.FileNameGenerator
import br.com.ebook.universalimageloader.core.DefaultConfigurationFactory
import br.com.ebook.universalimageloader.utils.IoUtils.CopyListener
import br.com.ebook.universalimageloader.utils.IoUtils.closeSilently
import br.com.ebook.universalimageloader.utils.IoUtils.copyStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Base disk cache.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @see FileNameGenerator
 *
 * @since 1.0.0
 */
abstract class BaseDiskCache @JvmOverloads constructor(
    cacheDir: File?,
    reserveCacheDir: File? = null,
    fileNameGenerator: FileNameGenerator? = DefaultConfigurationFactory.createFileNameGenerator()
) : DiskCache {
    override val directory: File
    protected val reserveCacheDir: File?
    protected val fileNameGenerator: FileNameGenerator
    protected var bufferSize = DEFAULT_BUFFER_SIZE
    protected var compressFormat = DEFAULT_COMPRESS_FORMAT
    protected var compressQuality = DEFAULT_COMPRESS_QUALITY
    /**
     * @param cacheDir          Directory for file caching
     * @param reserveCacheDir   null-ok; Reserve directory for file caching. It's used when the primary directory isn't available.
     * @param fileNameGenerator [                          Name generator][FileNameGenerator] for cached files
     */
    /**
     * @param cacheDir        Directory for file caching
     * @param reserveCacheDir null-ok; Reserve directory for file caching. It's used when the primary directory isn't available.
     */
    /** @param cacheDir Directory for file caching
     */
    init {
        requireNotNull(cacheDir) { "cacheDir$ERROR_ARG_NULL" }
        requireNotNull(fileNameGenerator) { "fileNameGenerator$ERROR_ARG_NULL" }
        directory = cacheDir
        this.reserveCacheDir = reserveCacheDir
        this.fileNameGenerator = fileNameGenerator
    }

    override fun get(imageUri: String?): File? {
        return getFile(imageUri)
    }

    @Throws(IOException::class)
    override fun save(imageUri: String?, imageStream: InputStream?, listener: CopyListener?): Boolean {
        val imageFile = getFile(imageUri)
        val tmpFile = File(imageFile.absolutePath + TEMP_IMAGE_POSTFIX)
        var loaded = false
        try {
            val os: OutputStream = BufferedOutputStream(FileOutputStream(tmpFile), bufferSize)
            loaded = try {
                copyStream(imageStream!!, os, listener, bufferSize)
            } finally {
                closeSilently(os)
            }
        } finally {
            if (loaded && !tmpFile.renameTo(imageFile)) {
                loaded = false
            }
            if (!loaded) {
                tmpFile.delete()
            }
        }
        return loaded
    }

    @Throws(IOException::class)
    override fun save(imageUri: String?, bitmap: Bitmap?): Boolean {
        val imageFile = getFile(imageUri)
        val tmpFile = File(imageFile.absolutePath + TEMP_IMAGE_POSTFIX)
        val os: OutputStream = BufferedOutputStream(FileOutputStream(tmpFile), bufferSize)
        var savedSuccessfully = false
        try {
            savedSuccessfully = bitmap!!.compress(compressFormat, compressQuality, os)
        } finally {
            closeSilently(os)
            if (savedSuccessfully && !tmpFile.renameTo(imageFile)) {
                savedSuccessfully = false
            }
            if (!savedSuccessfully) {
                tmpFile.delete()
            }
        }
        bitmap!!.recycle()
        return savedSuccessfully
    }

    override fun remove(imageUri: String?): Boolean {
        return getFile(imageUri).delete()
    }

    override fun close() {
        // Nothing to do
    }

    override fun clear() {
        val files = directory.listFiles()
        if (files != null) {
            for (f in files) {
                f.delete()
            }
        }
    }

    /** Returns file object (not null) for incoming image URI. File object can reference to non-existing file.  */
    protected fun getFile(imageUri: String?): File {
        val fileName = fileNameGenerator.generate(imageUri)
        var dir: File? = directory
        if (!directory.exists() && !directory.mkdirs()) {
            if (reserveCacheDir != null && (reserveCacheDir.exists() || reserveCacheDir.mkdirs())) {
                dir = reserveCacheDir
            }
        }
        return File(dir, fileName)
    }

    companion object {
        /** {@value  */
        const val DEFAULT_BUFFER_SIZE = 32 * 1024 // 32 Kb

        /** {@value  */
        val DEFAULT_COMPRESS_FORMAT = Bitmap.CompressFormat.PNG

        /** {@value  */
        const val DEFAULT_COMPRESS_QUALITY = 100
        private const val ERROR_ARG_NULL = " argument must be not null"
        private const val TEMP_IMAGE_POSTFIX = ".tmp"
    }
}
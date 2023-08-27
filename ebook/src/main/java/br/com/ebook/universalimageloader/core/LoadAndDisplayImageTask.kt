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
package br.com.ebook.universalimageloader.core

import android.graphics.Bitmap
import android.os.Handler
import br.com.ebook.foobnix.pdf.info.wrapper.AppState
import br.com.ebook.universalimageloader.core.assist.FailReason
import br.com.ebook.universalimageloader.core.assist.FailReason.FailType
import br.com.ebook.universalimageloader.core.assist.ImageScaleType
import br.com.ebook.universalimageloader.core.assist.ImageSize
import br.com.ebook.universalimageloader.core.assist.LoadedFrom
import br.com.ebook.universalimageloader.core.assist.ViewScaleType
import br.com.ebook.universalimageloader.core.decode.ImageDecoder
import br.com.ebook.universalimageloader.core.decode.ImageDecodingInfo
import br.com.ebook.universalimageloader.core.download.ImageDownloader
import br.com.ebook.universalimageloader.core.imageaware.ImageAware
import br.com.ebook.universalimageloader.core.listener.ImageLoadingListener
import br.com.ebook.universalimageloader.core.listener.ImageLoadingProgressListener
import br.com.ebook.universalimageloader.utils.IoUtils.CopyListener
import br.com.ebook.universalimageloader.utils.IoUtils.closeSilently
import br.com.ebook.universalimageloader.utils.L.d
import br.com.ebook.universalimageloader.utils.L.e
import java.io.IOException

/**
 * Presents load'n'display image task. Used to load image from Internet or file
 * system, decode it to [Bitmap], and display it in
 * [ImageAware] using
 * [DisplayBitmapTask].
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @see ImageLoaderConfiguration
 *
 * @see ImageLoadingInfo
 *
 * @since 1.3.1
 */
internal class LoadAndDisplayImageTask(
    private val engine: ImageLoaderEngine,
    private val imageLoadingInfo: ImageLoadingInfo,
    private val handler: Handler
) : Runnable, CopyListener {
    // Helper references
    private val configuration: ImageLoaderConfiguration
    private val downloader: ImageDownloader?
    private val networkDeniedDownloader: ImageDownloader
    private val slowNetworkDownloader: ImageDownloader
    private val decoder: ImageDecoder?
    val loadingUri: String
    private val memoryCacheKey: String
    val imageAware: ImageAware
    private val targetSize: ImageSize
    val options: DisplayImageOptions
    val listener: ImageLoadingListener
    val progressListener: ImageLoadingProgressListener?
    private val syncLoading: Boolean

    // State vars
    private var loadedFrom = LoadedFrom.NETWORK

    init {
        configuration = engine.configuration
        downloader = configuration.downloader
        networkDeniedDownloader = configuration.networkDeniedDownloader
        slowNetworkDownloader = configuration.slowNetworkDownloader
        decoder = configuration.decoder
        loadingUri = imageLoadingInfo.uri
        memoryCacheKey = imageLoadingInfo.memoryCacheKey
        imageAware = imageLoadingInfo.imageAware
        targetSize = imageLoadingInfo.targetSize
        options = imageLoadingInfo.options
        listener = imageLoadingInfo.listener
        progressListener = imageLoadingInfo.progressListener
        syncLoading = options.isSyncLoading
    }

    override fun run() {
        if (waitIfPaused()) return
        if (delayIfNeed()) return
        val loadFromUriLock = imageLoadingInfo.loadFromUriLock
        d(LOG_START_DISPLAY_IMAGE_TASK, memoryCacheKey)
        if (loadFromUriLock.isLocked) {
            d(LOG_WAITING_FOR_IMAGE_LOADED, memoryCacheKey)
        }
        loadFromUriLock.lock()
        var bmp: Bitmap?
        try {
            checkTaskNotActual()
            bmp = configuration.memoryCache!![memoryCacheKey]
            if (bmp == null || bmp.isRecycled) {
                bmp = tryLoadBitmap()
                if (bmp == null) return  // listener callback already was fired
                checkTaskNotActual()
                checkTaskInterrupted()
                if (options.shouldPreProcess()) {
                    d(LOG_PREPROCESS_IMAGE, memoryCacheKey)
                    bmp = options.preProcessor!!.process(bmp)
                    if (bmp == null) {
                        e(ERROR_PRE_PROCESSOR_NULL, memoryCacheKey)
                    }
                }
                if (bmp != null && options.isCacheInMemory) {
                    d(LOG_CACHE_IMAGE_IN_MEMORY, memoryCacheKey)
                    configuration.memoryCache.put(memoryCacheKey, bmp)
                }
            } else {
                loadedFrom = LoadedFrom.MEMORY_CACHE
                d(LOG_GET_IMAGE_FROM_MEMORY_CACHE_AFTER_WAITING, memoryCacheKey)
            }
            if (bmp != null && options.shouldPostProcess()) {
                d(LOG_POSTPROCESS_IMAGE, memoryCacheKey)
                bmp = options.postProcessor!!.process(bmp)
                if (bmp == null) {
                    e(ERROR_POST_PROCESSOR_NULL, memoryCacheKey)
                }
            }
            checkTaskNotActual()
            checkTaskInterrupted()
        } catch (e: TaskCancelledException) {
            fireCancelEvent()
            return
        } finally {
            loadFromUriLock.unlock()
        }
        val displayBitmapTask = DisplayBitmapTask(bmp!!, imageLoadingInfo, engine, loadedFrom)
        runTask(displayBitmapTask, syncLoading, handler, engine)
    }

    /**
     * @return **true** - if task should be interrupted; **false** -
     * otherwise
     */
    private fun waitIfPaused(): Boolean {
        val pause = engine.pause
        if (pause.get()) {
            synchronized(engine.pauseLock) {
                if (pause.get()) {
                    d(LOG_WAITING_FOR_RESUME, memoryCacheKey)
                    try {
                        engine.pauseLock.wait()
                    } catch (e: InterruptedException) {
                        e(LOG_TASK_INTERRUPTED, memoryCacheKey)
                        return true
                    }
                    d(LOG_RESUME_AFTER_PAUSE, memoryCacheKey)
                }
            }
        }
        return isTaskNotActual
    }

    /**
     * @return **true** - if task should be interrupted; **false** -
     * otherwise
     */
    private fun delayIfNeed(): Boolean {
        if (options.shouldDelayBeforeLoading()) {
            d(LOG_DELAY_BEFORE_LOADING, options.delayBeforeLoading, memoryCacheKey)
            try {
                Thread.sleep(options.delayBeforeLoading.toLong())
            } catch (e: InterruptedException) {
                e(LOG_TASK_INTERRUPTED, memoryCacheKey)
                return true
            }
            return isTaskNotActual
        }
        return false
    }

    @Throws(TaskCancelledException::class)
    private fun tryLoadBitmap(): Bitmap? {
        var bitmap: Bitmap? = null
        try {
            var imageFile = configuration.diskCache!![loadingUri]
            if (imageFile != null && imageFile.exists() && imageFile.length() > 0) {
                d(LOG_LOAD_IMAGE_FROM_DISK_CACHE, memoryCacheKey)
                loadedFrom = LoadedFrom.DISC_CACHE
                checkTaskNotActual()
                bitmap = decodeImage(ImageDownloader.Scheme.FILE.wrap(imageFile.absolutePath))
            }
            if (bitmap == null || bitmap.width <= 0 || bitmap.height <= 0) {
                d(LOG_LOAD_IMAGE_FROM_NETWORK, memoryCacheKey)
                loadedFrom = LoadedFrom.NETWORK
                var imageUriForDecoding = loadingUri
                if (options.isCacheOnDisk && tryCacheImageOnDisk()) {
                    imageFile = configuration.diskCache[loadingUri]
                    if (imageFile != null) {
                        imageUriForDecoding = ImageDownloader.Scheme.FILE.wrap(imageFile.absolutePath)
                    }
                }
                checkTaskNotActual()
                bitmap = decodeImage(imageUriForDecoding)
                if (bitmap == null || bitmap.width <= 0 || bitmap.height <= 0) {
                    fireFailEvent(FailType.DECODING_ERROR, null)
                }
            }
        } catch (e: IllegalStateException) {
            fireFailEvent(FailType.NETWORK_DENIED, null)
        } catch (e: TaskCancelledException) {
            throw e
        } catch (e: IOException) {
            e(e)
            fireFailEvent(FailType.IO_ERROR, e)
        } catch (e: OutOfMemoryError) {
            e(e)
            AppState.get().pagesInMemory = 1
            fireFailEvent(FailType.OUT_OF_MEMORY, e)
        } catch (e: Throwable) {
            e(e)
            fireFailEvent(FailType.UNKNOWN, e)
        }
        return bitmap
    }

    @Throws(IOException::class)
    private fun decodeImage(imageUri: String): Bitmap? {
        val viewScaleType = imageAware.scaleType
        val decodingInfo = ImageDecodingInfo(memoryCacheKey, imageUri, loadingUri, targetSize, viewScaleType!!, getDownloader()!!, options)
        return decoder!!.decode(decodingInfo)
    }

    /**
     * @return **true** - if image was downloaded successfully; **false**
     * - otherwise
     */
    @Throws(TaskCancelledException::class)
    private fun tryCacheImageOnDisk(): Boolean {
        d(LOG_CACHE_IMAGE_ON_DISK, memoryCacheKey)
        var loaded: Boolean
        try {
            loaded = downloadImage()
            if (loaded) {
                val width = configuration.maxImageWidthForDiskCache
                val height = configuration.maxImageHeightForDiskCache
                if (width > 0 || height > 0) {
                    d(LOG_RESIZE_CACHED_IMAGE_FILE, memoryCacheKey)
                    resizeAndSaveImage(width, height) // TODO : process boolean
                    // result
                }
            }
        } catch (e: IOException) {
            e(e)
            loaded = false
        }
        return loaded
    }

    @Throws(IOException::class)
    private fun downloadImage(): Boolean {
        val `is` = getDownloader()!!.getStream(loadingUri, options.extraForDownloader)
        return if (`is` == null) {
            e(ERROR_NO_IMAGE_STREAM, memoryCacheKey)
            false
        } else {
            try {
                configuration.diskCache!!.save(loadingUri, `is`, this)
            } finally {
                closeSilently(`is`)
            }
        }
    }

    /** Decodes image file into Bitmap, resize it and save it back  */
    @Throws(IOException::class)
    private fun resizeAndSaveImage(maxWidth: Int, maxHeight: Int): Boolean {
        // Decode image file, compress and re-save it
        var saved = false
        val targetFile = configuration.diskCache!![loadingUri]
        if (targetFile != null && targetFile.exists()) {
            val targetImageSize = ImageSize(maxWidth, maxHeight)
            val specialOptions = DisplayImageOptions.Builder().cloneFrom(options).imageScaleType(ImageScaleType.IN_SAMPLE_INT).build()
            val decodingInfo = ImageDecodingInfo(
                memoryCacheKey,
                ImageDownloader.Scheme.FILE.wrap(targetFile.absolutePath),
                loadingUri,
                targetImageSize,
                ViewScaleType.FIT_INSIDE,
                getDownloader()!!,
                specialOptions
            )
            var bmp = decoder!!.decode(decodingInfo)
            if (bmp != null && configuration.processorForDiskCache != null) {
                d(LOG_PROCESS_IMAGE_BEFORE_CACHE_ON_DISK, memoryCacheKey)
                bmp = configuration.processorForDiskCache.process(bmp)
                if (bmp == null) {
                    e(ERROR_PROCESSOR_FOR_DISK_CACHE_NULL, memoryCacheKey)
                }
            }
            if (bmp != null) {
                saved = configuration.diskCache.save(loadingUri, bmp)
                bmp.recycle()
            }
        }
        return saved
    }

    override fun onBytesCopied(current: Int, total: Int): Boolean {
        return syncLoading || fireProgressEvent(current, total)
    }

    /**
     * @return **true** - if loading should be continued; **false** - if
     * loading should be interrupted
     */
    private fun fireProgressEvent(current: Int, total: Int): Boolean {
        if (isTaskInterrupted || isTaskNotActual) return false
        if (progressListener != null) {
            val r: Runnable = object : Runnable {
                override fun run() {
                    progressListener.onProgressUpdate(loadingUri, imageAware.wrappedView, current, total)
                }
            }
            runTask(r, false, handler, engine)
        }
        return true
    }

    private fun fireFailEvent(failType: FailType, failCause: Throwable?) {
        if (syncLoading || isTaskInterrupted || isTaskNotActual) return
        val r: Runnable = object : Runnable {
            override fun run() {
                if (options.shouldShowImageOnFail()) {
                    imageAware.setImageDrawable(options.getImageOnFail(configuration.resources))
                }
                listener.onLoadingFailed(loadingUri, imageAware.wrappedView, FailReason(failType, failCause!!))
            }
        }
        runTask(r, false, handler, engine)
    }

    private fun fireCancelEvent() {
        if (syncLoading || isTaskInterrupted) return
        val r: Runnable = object : Runnable {
            override fun run() {
                listener.onLoadingCancelled(loadingUri, imageAware.wrappedView)
            }
        }
        runTask(r, false, handler, engine)
    }

    private fun getDownloader(): ImageDownloader? {
        val d: ImageDownloader?
        d = if (engine.isNetworkDenied()) {
            networkDeniedDownloader
        } else if (engine.isSlowNetwork()) {
            slowNetworkDownloader
        } else {
            downloader
        }
        return d
    }

    /**
     * @throws TaskCancelledException
     * if task is not actual (target ImageAware is collected by GC
     * or the image URI of this task doesn't match to image URI
     * which is actual for current ImageAware at this moment)
     */
    @Throws(TaskCancelledException::class)
    private fun checkTaskNotActual() {
        checkViewCollected()
        checkViewReused()
    }

    /**
     * @return **true** - if task is not actual (target ImageAware is
     * collected by GC or the image URI of this task doesn't match to
     * image URI which is actual for current ImageAware at this
     * moment)); **false** - otherwise
     */
    private val isTaskNotActual: Boolean
        private get() = isViewCollected || isViewReused

    /**
     * @throws TaskCancelledException
     * if target ImageAware is collected
     */
    @Throws(TaskCancelledException::class)
    private fun checkViewCollected() {
        if (isViewCollected) {
            throw TaskCancelledException()
        }
    }

    /**
     * @return **true** - if target ImageAware is collected by GC;
     * **false** - otherwise
     */
    private val isViewCollected: Boolean
        private get() {
            if (imageAware.isCollected) {
                d(LOG_TASK_CANCELLED_IMAGEAWARE_COLLECTED, memoryCacheKey)
                return true
            }
            return false
        }

    /**
     * @throws TaskCancelledException
     * if target ImageAware is collected by GC
     */
    @Throws(TaskCancelledException::class)
    private fun checkViewReused() {
        if (isViewReused) {
            throw TaskCancelledException()
        }
    }// Check whether memory cache key (image URI) for current ImageAware is
    // actual.
    // If ImageAware is reused for another task then current task should be
    // cancelled.
    /**
     * @return **true** - if current ImageAware is reused for displaying
     * another image; **false** - otherwise
     */
    private val isViewReused: Boolean
        private get() {
            val currentCacheKey = engine.getLoadingUriForView(imageAware)
            // Check whether memory cache key (image URI) for current ImageAware is
            // actual.
            // If ImageAware is reused for another task then current task should be
            // cancelled.
            val imageAwareWasReused = memoryCacheKey != currentCacheKey
            if (imageAwareWasReused) {
                d(LOG_TASK_CANCELLED_IMAGEAWARE_REUSED, memoryCacheKey)
                return true
            }
            return false
        }

    /**
     * @throws TaskCancelledException
     * if current task was interrupted
     */
    @Throws(TaskCancelledException::class)
    private fun checkTaskInterrupted() {
        if (isTaskInterrupted) {
            throw TaskCancelledException()
        }
    }

    /**
     * @return **true** - if current task was interrupted; **false** -
     * otherwise
     */
    private val isTaskInterrupted: Boolean
        private get() {
            if (Thread.interrupted()) {
                d(LOG_TASK_INTERRUPTED, memoryCacheKey)
                return true
            }
            return false
        }

    /**
     * Exceptions for case when task is cancelled (thread is interrupted, image
     * view is reused for another task, view is collected by GC).
     *
     * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
     * @since 1.9.1
     */
    internal inner class TaskCancelledException : Exception()
    companion object {
        private const val LOG_WAITING_FOR_RESUME = "ImageLoader is paused. Waiting...  [%s]"
        private const val LOG_RESUME_AFTER_PAUSE = ".. Resume loading [%s]"
        private const val LOG_DELAY_BEFORE_LOADING = "Delay %d ms before loading...  [%s]"
        private const val LOG_START_DISPLAY_IMAGE_TASK = "Start display image task [%s]"
        private const val LOG_WAITING_FOR_IMAGE_LOADED = "Image already is loading. Waiting... [%s]"
        private const val LOG_GET_IMAGE_FROM_MEMORY_CACHE_AFTER_WAITING = "...Get cached bitmap from memory after waiting. [%s]"
        private const val LOG_LOAD_IMAGE_FROM_NETWORK = "Load image from network [%s]"
        private const val LOG_LOAD_IMAGE_FROM_DISK_CACHE = "Load image from disk cache [%s]"
        private const val LOG_RESIZE_CACHED_IMAGE_FILE = "Resize image in disk cache [%s]"
        private const val LOG_PREPROCESS_IMAGE = "PreProcess image before caching in memory [%s]"
        private const val LOG_POSTPROCESS_IMAGE = "PostProcess image before displaying [%s]"
        private const val LOG_CACHE_IMAGE_IN_MEMORY = "Cache image in memory [%s]"
        private const val LOG_CACHE_IMAGE_ON_DISK = "Cache image on disk [%s]"
        private const val LOG_PROCESS_IMAGE_BEFORE_CACHE_ON_DISK = "Process image before cache on disk [%s]"
        private const val LOG_TASK_CANCELLED_IMAGEAWARE_REUSED = "ImageAware is reused for another image. Task is cancelled. [%s]"
        private const val LOG_TASK_CANCELLED_IMAGEAWARE_COLLECTED = "ImageAware was collected by GC. Task is cancelled. [%s]"
        private const val LOG_TASK_INTERRUPTED = "Task was interrupted [%s]"
        private const val ERROR_NO_IMAGE_STREAM = "No stream for image [%s]"
        private const val ERROR_PRE_PROCESSOR_NULL = "Pre-processor returned null [%s]"
        private const val ERROR_POST_PROCESSOR_NULL = "Post-processor returned null [%s]"
        private const val ERROR_PROCESSOR_FOR_DISK_CACHE_NULL = "Bitmap processor for disk cache returned null [%s]"
        fun runTask(r: Runnable, sync: Boolean, handler: Handler?, engine: ImageLoaderEngine) {
            if (sync) {
                r.run()
            } else handler?.post(r) ?: engine.fireCallback(r)
        }
    }
}
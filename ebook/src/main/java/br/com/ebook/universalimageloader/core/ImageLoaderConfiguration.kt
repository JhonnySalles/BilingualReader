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

import android.content.Context
import android.content.res.Resources
import br.com.ebook.universalimageloader.cache.disc.DiskCache
import br.com.ebook.universalimageloader.cache.disc.naming.FileNameGenerator
import br.com.ebook.universalimageloader.cache.memory.MemoryCache
import br.com.ebook.universalimageloader.cache.memory.impl.FuzzyKeyMemoryCache
import br.com.ebook.universalimageloader.core.DefaultConfigurationFactory.createDiskCache
import br.com.ebook.universalimageloader.core.DefaultConfigurationFactory.createExecutor
import br.com.ebook.universalimageloader.core.DefaultConfigurationFactory.createFileNameGenerator
import br.com.ebook.universalimageloader.core.DefaultConfigurationFactory.createImageDecoder
import br.com.ebook.universalimageloader.core.DefaultConfigurationFactory.createImageDownloader
import br.com.ebook.universalimageloader.core.DefaultConfigurationFactory.createMemoryCache
import br.com.ebook.universalimageloader.core.DisplayImageOptions.Companion.createSimple
import br.com.ebook.universalimageloader.core.assist.FlushedInputStream
import br.com.ebook.universalimageloader.core.assist.ImageSize
import br.com.ebook.universalimageloader.core.assist.QueueProcessingType
import br.com.ebook.universalimageloader.core.decode.ImageDecoder
import br.com.ebook.universalimageloader.core.download.ImageDownloader
import br.com.ebook.universalimageloader.core.download.ImageDownloader.Scheme.Companion.ofUri
import br.com.ebook.universalimageloader.core.process.BitmapProcessor
import br.com.ebook.universalimageloader.utils.L
import br.com.ebook.universalimageloader.utils.L.w
import br.com.ebook.universalimageloader.utils.L.writeDebugLogs
import br.com.ebook.universalimageloader.utils.MemoryCacheUtils.createFuzzyKeyComparator
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.Executor

/**
 * Presents configuration for [ImageLoader]
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @see ImageLoader
 *
 * @see MemoryCache
 *
 * @see DiskCache
 *
 * @see DisplayImageOptions
 *
 * @see ImageDownloader
 *
 * @see FileNameGenerator
 *
 * @since 1.0.0
 */
class ImageLoaderConfiguration private constructor(builder: Builder) {
    @JvmField
	val resources: Resources
    val maxImageWidthForMemoryCache: Int
    val maxImageHeightForMemoryCache: Int
    @JvmField
	val maxImageWidthForDiskCache: Int
    @JvmField
	val maxImageHeightForDiskCache: Int
    @JvmField
	val processorForDiskCache: BitmapProcessor?
    val taskExecutor: Executor?
    val taskExecutorForCachedImages: Executor?
    val customExecutor: Boolean
    val customExecutorForCachedImages: Boolean
    val threadPoolSize: Int
    val threadPriority: Int
    val tasksProcessingType: QueueProcessingType
    @JvmField
	val memoryCache: MemoryCache?
    @JvmField
	val diskCache: DiskCache?
    @JvmField
	val downloader: ImageDownloader?
    @JvmField
	val decoder: ImageDecoder?
    @JvmField
	val defaultDisplayImageOptions: DisplayImageOptions?
    @JvmField
	val networkDeniedDownloader: ImageDownloader
    @JvmField
	val slowNetworkDownloader: ImageDownloader

    init {
        resources = builder.context.resources
        maxImageWidthForMemoryCache = builder.maxImageWidthForMemoryCache
        maxImageHeightForMemoryCache = builder.maxImageHeightForMemoryCache
        maxImageWidthForDiskCache = builder.maxImageWidthForDiskCache
        maxImageHeightForDiskCache = builder.maxImageHeightForDiskCache
        processorForDiskCache = builder.processorForDiskCache
        taskExecutor = builder.taskExecutor
        taskExecutorForCachedImages = builder.taskExecutorForCachedImages
        threadPoolSize = builder.threadPoolSize
        threadPriority = builder.threadPriority
        tasksProcessingType = builder.tasksProcessingType
        diskCache = builder.diskCache
        memoryCache = builder.memoryCache
        defaultDisplayImageOptions = builder.defaultDisplayImageOptions
        downloader = builder.downloader
        decoder = builder.decoder
        customExecutor = builder.customExecutor
        customExecutorForCachedImages = builder.customExecutorForCachedImages
        networkDeniedDownloader = NetworkDeniedImageDownloader(downloader)
        slowNetworkDownloader = SlowNetworkImageDownloader(downloader)
        writeDebugLogs(builder.writeLogs)
    }

    val maxImageSize: ImageSize
        get() {
            val displayMetrics = resources.displayMetrics
            var width = maxImageWidthForMemoryCache
            if (width <= 0) {
                width = displayMetrics.widthPixels
            }
            var height = maxImageHeightForMemoryCache
            if (height <= 0) {
                height = displayMetrics.heightPixels
            }
            return ImageSize(width, height)
        }

    /**
     * Builder for [ImageLoaderConfiguration]
     *
     * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
     */
    open class Builder(context: Context) {
        internal val context: Context
        internal var maxImageWidthForMemoryCache = 0
        internal var maxImageHeightForMemoryCache = 0
        internal var maxImageWidthForDiskCache = 0
        internal var maxImageHeightForDiskCache = 0
        internal var processorForDiskCache: BitmapProcessor? = null
        internal var taskExecutor: Executor? = null
        internal var taskExecutorForCachedImages: Executor? = null
        internal var customExecutor = false
        internal var customExecutorForCachedImages = false
        internal var threadPoolSize = DEFAULT_THREAD_POOL_SIZE
        internal var threadPriority = DEFAULT_THREAD_PRIORITY
        internal var denyCacheImageMultipleSizesInMemory = false
        internal var tasksProcessingType = DEFAULT_TASK_PROCESSING_TYPE
        internal var memoryCacheSize = 0
        internal var diskCacheSize: Long = 0
        internal var diskCacheFileCount = 0
        internal var memoryCache: MemoryCache? = null
        internal var diskCache: DiskCache? = null
        internal var diskCacheFileNameGenerator: FileNameGenerator? = null
        internal var downloader: ImageDownloader? = null
        internal var decoder: ImageDecoder? = null
        internal var defaultDisplayImageOptions: DisplayImageOptions? = null
        internal var writeLogs = false

        init {
            this.context = context.applicationContext
        }

        /**
         * Sets options for memory cache
         *
         * @param maxImageWidthForMemoryCache  Maximum image width which will be used for memory saving during decoding
         * an image to [Bitmap][android.graphics.Bitmap]. **Default value - device's screen width**
         * @param maxImageHeightForMemoryCache Maximum image height which will be used for memory saving during decoding
         * an image to [Bitmap][android.graphics.Bitmap]. **Default value** - device's screen height
         */
        fun memoryCacheExtraOptions(maxImageWidthForMemoryCache: Int, maxImageHeightForMemoryCache: Int): Builder {
            this.maxImageWidthForMemoryCache = maxImageWidthForMemoryCache
            this.maxImageHeightForMemoryCache = maxImageHeightForMemoryCache
            return this
        }

        @Deprecated(
            """Use
		  {@link #diskCacheExtraOptions(int, int, BitmapProcessor)}
		  instead"""
        )
        fun discCacheExtraOptions(
            maxImageWidthForDiskCache: Int, maxImageHeightForDiskCache: Int,
            processorForDiskCache: BitmapProcessor?
        ): Builder {
            return diskCacheExtraOptions(maxImageWidthForDiskCache, maxImageHeightForDiskCache, processorForDiskCache)
        }

        /**
         * Sets options for resizing/compressing of downloaded images before saving to disk cache.<br></br>
         * **NOTE: Use this option only when you have appropriate needs. It can make ImageLoader slower.**
         *
         * @param maxImageWidthForDiskCache  Maximum width of downloaded images for saving at disk cache
         * @param maxImageHeightForDiskCache Maximum height of downloaded images for saving at disk cache
         * @param processorForDiskCache      null-ok; [Bitmap processor][BitmapProcessor] which process images before saving them in disc cache
         */
        fun diskCacheExtraOptions(
            maxImageWidthForDiskCache: Int, maxImageHeightForDiskCache: Int,
            processorForDiskCache: BitmapProcessor?
        ): Builder {
            this.maxImageWidthForDiskCache = maxImageWidthForDiskCache
            this.maxImageHeightForDiskCache = maxImageHeightForDiskCache
            this.processorForDiskCache = processorForDiskCache
            return this
        }

        /**
         * Sets custom [executor][Executor] for tasks of loading and displaying images.<br></br>
         * <br></br>
         * **NOTE:** If you set custom executor then following configuration options will not be considered for this
         * executor:
         *
         *  * [.threadPoolSize]
         *  * [.threadPriority]
         *  * [.tasksProcessingOrder]
         *
         *
         * @see .taskExecutorForCachedImages
         */
        fun taskExecutor(executor: Executor?): Builder {
            if (threadPoolSize != DEFAULT_THREAD_POOL_SIZE || threadPriority != DEFAULT_THREAD_PRIORITY || tasksProcessingType !== DEFAULT_TASK_PROCESSING_TYPE) {
                w(WARNING_OVERLAP_EXECUTOR)
            }
            taskExecutor = executor
            return this
        }

        /**
         * Sets custom [executor][Executor] for tasks of displaying **cached on disk** images (these tasks
         * are executed quickly so UIL prefer to use separate executor for them).<br></br>
         * <br></br>
         * If you set the same executor for [general tasks][.taskExecutor] and
         * tasks about cached images (this method) then these tasks will be in the
         * same thread pool. So short-lived tasks can wait a long time for their turn.<br></br>
         * <br></br>
         * **NOTE:** If you set custom executor then following configuration options will not be considered for this
         * executor:
         *
         *  * [.threadPoolSize]
         *  * [.threadPriority]
         *  * [.tasksProcessingOrder]
         *
         *
         * @see .taskExecutor
         */
        fun taskExecutorForCachedImages(executorForCachedImages: Executor?): Builder {
            if (threadPoolSize != DEFAULT_THREAD_POOL_SIZE || threadPriority != DEFAULT_THREAD_PRIORITY || tasksProcessingType !== DEFAULT_TASK_PROCESSING_TYPE) {
                w(WARNING_OVERLAP_EXECUTOR)
            }
            taskExecutorForCachedImages = executorForCachedImages
            return this
        }

        /**
         * Sets thread pool size for image display tasks.<br></br>
         * Default value - [this][.DEFAULT_THREAD_POOL_SIZE]
         */
        fun threadPoolSize(threadPoolSize: Int): Builder {
            if (taskExecutor != null || taskExecutorForCachedImages != null) {
                w(WARNING_OVERLAP_EXECUTOR)
            }
            this.threadPoolSize = threadPoolSize
            return this
        }

        /**
         * Sets the priority for image loading threads. Should be **NOT** greater than [Thread.MAX_PRIORITY] or
         * less than [Thread.MIN_PRIORITY]<br></br>
         * Default value - [this][.DEFAULT_THREAD_PRIORITY]
         */
        fun threadPriority(threadPriority: Int): Builder {
            if (taskExecutor != null || taskExecutorForCachedImages != null) {
                w(WARNING_OVERLAP_EXECUTOR)
            }
            if (threadPriority < Thread.MIN_PRIORITY) {
                this.threadPriority = Thread.MIN_PRIORITY
            } else {
                if (threadPriority > Thread.MAX_PRIORITY) {
                    this.threadPriority = Thread.MAX_PRIORITY
                } else {
                    this.threadPriority = threadPriority
                }
            }
            return this
        }

        /**
         * When you display an image in a small [ImageView][android.widget.ImageView] and later you try to display
         * this image (from identical URI) in a larger [ImageView][android.widget.ImageView] so decoded image of
         * bigger size will be cached in memory as a previous decoded image of smaller size.<br></br>
         * So **the default behavior is to allow to cache multiple sizes of one image in memory**. You can
         * **deny** it by calling **this** method: so when some image will be cached in memory then previous
         * cached size of this image (if it exists) will be removed from memory cache before.
         */
        fun denyCacheImageMultipleSizesInMemory(): Builder {
            denyCacheImageMultipleSizesInMemory = true
            return this
        }

        /**
         * Sets type of queue processing for tasks for loading and displaying images.<br></br>
         * Default value - [QueueProcessingType.FIFO]
         */
        fun tasksProcessingOrder(tasksProcessingType: QueueProcessingType): Builder {
            if (taskExecutor != null || taskExecutorForCachedImages != null) {
                w(WARNING_OVERLAP_EXECUTOR)
            }
            this.tasksProcessingType = tasksProcessingType
            return this
        }

        /**
         * Sets maximum memory cache size for [bitmaps][android.graphics.Bitmap] (in bytes).<br></br>
         * Default value - 1/8 of available app memory.<br></br>
         * **NOTE:** If you use this method then
         * [LruMemoryCache] will be used as
         * memory cache. You can use [.memoryCache] method to set your own implementation of
         * [MemoryCache].
         */
        fun memoryCacheSize(memoryCacheSize: Int): Builder {
            require(memoryCacheSize > 0) { "memoryCacheSize must be a positive number" }
            if (memoryCache != null) {
                w(WARNING_OVERLAP_MEMORY_CACHE)
            }
            this.memoryCacheSize = memoryCacheSize
            return this
        }

        /**
         * Sets maximum memory cache size (in percent of available app memory) for [ bitmaps][android.graphics.Bitmap].<br></br>
         * Default value - 1/8 of available app memory.<br></br>
         * **NOTE:** If you use this method then
         * [LruMemoryCache] will be used as
         * memory cache. You can use [.memoryCache] method to set your own implementation of
         * [MemoryCache].
         */
        fun memoryCacheSizePercentage(availableMemoryPercent: Int): Builder {
            require(!(availableMemoryPercent <= 0 || availableMemoryPercent >= 100)) { "availableMemoryPercent must be in range (0 < % < 100)" }
            if (memoryCache != null) {
                w(WARNING_OVERLAP_MEMORY_CACHE)
            }
            val availableMemory = Runtime.getRuntime().maxMemory()
            memoryCacheSize = (availableMemory * (availableMemoryPercent / 100f)).toInt()
            return this
        }

        /**
         * Sets memory cache for [bitmaps][android.graphics.Bitmap].<br></br>
         * Default value - [LruMemoryCache]
         * with limited memory cache size (size = 1/8 of available app memory)<br></br>
         * <br></br>
         * **NOTE:** If you set custom memory cache then following configuration option will not be considered:
         *
         *  * [.memoryCacheSize]
         *
         */
        fun memoryCache(memoryCache: MemoryCache?): Builder {
            if (memoryCacheSize != 0) {
                w(WARNING_OVERLAP_MEMORY_CACHE)
            }
            this.memoryCache = memoryCache
            return this
        }

        @Deprecated("Use {@link #diskCacheSize(int)} instead ")
        fun discCacheSize(maxCacheSize: Int): Builder {
            return diskCacheSize(maxCacheSize)
        }

        /**
         * Sets maximum disk cache size for images (in bytes).<br></br>
         * By default: disk cache is unlimited.<br></br>
         * **NOTE:** If you use this method then
         * [LruDiskCache]
         * will be used as disk cache. You can use [.diskCache] method for introduction your own
         * implementation of [DiskCache]
         */
        fun diskCacheSize(maxCacheSize: Int): Builder {
            require(maxCacheSize > 0) { "maxCacheSize must be a positive number" }
            if (diskCache != null) {
                w(WARNING_OVERLAP_DISK_CACHE_PARAMS)
            }
            diskCacheSize = maxCacheSize.toLong()
            return this
        }

        @Deprecated("Use {@link #diskCacheFileCount(int)} instead ")
        fun discCacheFileCount(maxFileCount: Int): Builder {
            return diskCacheFileCount(maxFileCount)
        }

        /**
         * Sets maximum file count in disk cache directory.<br></br>
         * By default: disk cache is unlimited.<br></br>
         * **NOTE:** If you use this method then
         * [LruDiskCache]
         * will be used as disk cache. You can use [.diskCache] method for introduction your own
         * implementation of [DiskCache]
         */
        fun diskCacheFileCount(maxFileCount: Int): Builder {
            require(maxFileCount > 0) { "maxFileCount must be a positive number" }
            if (diskCache != null) {
                w(WARNING_OVERLAP_DISK_CACHE_PARAMS)
            }
            diskCacheFileCount = maxFileCount
            return this
        }

        @Deprecated("Use {@link #diskCacheFileNameGenerator(FileNameGenerator)} ")
        fun discCacheFileNameGenerator(fileNameGenerator: FileNameGenerator?): Builder {
            return diskCacheFileNameGenerator(fileNameGenerator)
        }

        /**
         * Sets name generator for files cached in disk cache.<br></br>
         * Default value -
         * [ DefaultConfigurationFactory.createFileNameGenerator()][DefaultConfigurationFactory.createFileNameGenerator]
         */
        fun diskCacheFileNameGenerator(fileNameGenerator: FileNameGenerator?): Builder {
            if (diskCache != null) {
                w(WARNING_OVERLAP_DISK_CACHE_NAME_GENERATOR)
            }
            diskCacheFileNameGenerator = fileNameGenerator
            return this
        }

        @Deprecated("Use {@link #diskCache(DiskCache)} ")
        fun discCache(diskCache: DiskCache?): Builder {
            return diskCache(diskCache)
        }

        /**
         * Sets disk cache for images.<br></br>
         * Default value - [ UnlimitedDiskCache][UnlimitedDiskCache]. Cache directory is defined by
         * [ StorageUtils.getCacheDirectory(Context)][StorageUtils.getCacheDirectory].<br></br>
         * <br></br>
         * **NOTE:** If you set custom disk cache then following configuration option will not be considered:
         *
         *  * [.diskCacheSize]
         *  * [.diskCacheFileCount]
         *  * [.diskCacheFileNameGenerator]
         *
         */
        fun diskCache(diskCache: DiskCache?): Builder {
            if (diskCacheSize > 0 || diskCacheFileCount > 0) {
                w(WARNING_OVERLAP_DISK_CACHE_PARAMS)
            }
            if (diskCacheFileNameGenerator != null) {
                w(WARNING_OVERLAP_DISK_CACHE_NAME_GENERATOR)
            }
            this.diskCache = diskCache
            return this
        }

        /**
         * Sets utility which will be responsible for downloading of image.<br></br>
         * Default value -
         * [ DefaultConfigurationFactory.createImageDownloader()][DefaultConfigurationFactory.createImageDownloader]
         */
        fun imageDownloader(imageDownloader: ImageDownloader?): Builder {
            downloader = imageDownloader
            return this
        }

        /**
         * Sets utility which will be responsible for decoding of image stream.<br></br>
         * Default value -
         * [ DefaultConfigurationFactory.createImageDecoder()][DefaultConfigurationFactory.createImageDecoder]
         */
        fun imageDecoder(imageDecoder: ImageDecoder?): Builder {
            decoder = imageDecoder
            return this
        }

        /**
         * Sets default [display image options][DisplayImageOptions] for image displaying. These options will
         * be used for every [image display call][ImageLoader.displayImage]
         * without passing custom [options][DisplayImageOptions]<br></br>
         * Default value - [Simple options][DisplayImageOptions.createSimple]
         */
        fun defaultDisplayImageOptions(defaultDisplayImageOptions: DisplayImageOptions?): Builder {
            this.defaultDisplayImageOptions = defaultDisplayImageOptions
            return this
        }

        /**
         * Enables detail logging of [ImageLoader] work. To prevent detail logs don't call this method.
         * Consider [L.disableLogging] to disable
         * ImageLoader logging completely (even error logs)
         */
        fun writeDebugLogs(): Builder {
            writeLogs = true
            return this
        }

        /** Builds configured [ImageLoaderConfiguration] object  */
        fun build(): ImageLoaderConfiguration {
            initEmptyFieldsWithDefaultValues()
            return ImageLoaderConfiguration(this)
        }

        private fun initEmptyFieldsWithDefaultValues() {
            if (taskExecutor == null) {
                taskExecutor = createExecutor(threadPoolSize, threadPriority, tasksProcessingType)
            } else {
                customExecutor = true
            }
            if (taskExecutorForCachedImages == null) {
                taskExecutorForCachedImages = createExecutor(threadPoolSize, threadPriority, tasksProcessingType)
            } else {
                customExecutorForCachedImages = true
            }
            if (diskCache == null) {
                if (diskCacheFileNameGenerator == null) {
                    diskCacheFileNameGenerator = createFileNameGenerator()
                }
                diskCache = createDiskCache(context, diskCacheFileNameGenerator, diskCacheSize, diskCacheFileCount)
            }
            if (memoryCache == null) {
                memoryCache = createMemoryCache(context, memoryCacheSize)
            }
            if (denyCacheImageMultipleSizesInMemory) {
                memoryCache = FuzzyKeyMemoryCache(memoryCache!!, createFuzzyKeyComparator())
            }
            if (downloader == null) {
                downloader = createImageDownloader(context)
            }
            if (decoder == null) {
                decoder = createImageDecoder(writeLogs)
            }
            if (defaultDisplayImageOptions == null) {
                defaultDisplayImageOptions = createSimple()
            }
        }

        companion object {
            private const val WARNING_OVERLAP_DISK_CACHE_PARAMS = "diskCache(), diskCacheSize() and diskCacheFileCount calls overlap each other"
            private const val WARNING_OVERLAP_DISK_CACHE_NAME_GENERATOR = "diskCache() and diskCacheFileNameGenerator() calls overlap each other"
            private const val WARNING_OVERLAP_MEMORY_CACHE = "memoryCache() and memoryCacheSize() calls overlap each other"
            private const val WARNING_OVERLAP_EXECUTOR = ("threadPoolSize(), threadPriority() and tasksProcessingOrder() calls "
                    + "can overlap taskExecutor() and taskExecutorForCachedImages() calls.")

            /** {@value}  */
            const val DEFAULT_THREAD_POOL_SIZE = 3

            /** {@value}  */
            const val DEFAULT_THREAD_PRIORITY = Thread.NORM_PRIORITY - 2

            /** {@value}  */
            val DEFAULT_TASK_PROCESSING_TYPE = QueueProcessingType.FIFO
        }
    }

    /**
     * Decorator. Prevents downloads from network (throws [exception][IllegalStateException]).<br></br>
     * In most cases this downloader shouldn't be used directly.
     *
     * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
     * @since 1.8.0
     */
    private class NetworkDeniedImageDownloader(private val wrappedDownloader: ImageDownloader?) : ImageDownloader {
        @Throws(IOException::class)
        override fun getStream(imageUri: String?, extra: Any?): InputStream? {
            return when (ofUri(imageUri)) {
                ImageDownloader.Scheme.HTTP, ImageDownloader.Scheme.HTTPS -> throw IllegalStateException()
                else -> wrappedDownloader!!.getStream(imageUri, extra)
            }
        }
    }

    /**
     * Decorator. Handles [this problem](http://code.google.com/p/android/issues/detail?id=6066) on slow networks
     * using [FlushedInputStream].
     *
     * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
     * @since 1.8.1
     */
    private class SlowNetworkImageDownloader(private val wrappedDownloader: ImageDownloader?) : ImageDownloader {
        @Throws(IOException::class)
        override fun getStream(imageUri: String?, extra: Any?): InputStream? {
            val imageStream = wrappedDownloader!!.getStream(imageUri, extra)
            return when (ofUri(imageUri)) {
                ImageDownloader.Scheme.HTTP, ImageDownloader.Scheme.HTTPS -> FlushedInputStream(
                    imageStream
                )

                else -> imageStream
            }
        }
    }

    companion object {
        /**
         * Creates default configuration for [ImageLoader] <br></br>
         * **Default values:**
         *
         *  * maxImageWidthForMemoryCache = device's screen width
         *  * maxImageHeightForMemoryCache = device's screen height
         *  * maxImageWidthForDikcCache = unlimited
         *  * maxImageHeightForDiskCache = unlimited
         *  * threadPoolSize = [this][Builder.DEFAULT_THREAD_POOL_SIZE]
         *  * threadPriority = [this][Builder.DEFAULT_THREAD_PRIORITY]
         *  * allow to cache different sizes of image in memory
         *  * memoryCache = [DefaultConfigurationFactory.createMemoryCache]
         *  * diskCache = [UnlimitedDiskCache]
         *  * imageDownloader = [DefaultConfigurationFactory.createImageDownloader]
         *  * imageDecoder = [DefaultConfigurationFactory.createImageDecoder]
         *  * diskCacheFileNameGenerator = [DefaultConfigurationFactory.createFileNameGenerator]
         *  * defaultDisplayImageOptions = [Simple options][DisplayImageOptions.createSimple]
         *  * tasksProcessingOrder = [QueueProcessingType.FIFO]
         *  * detailed logging disabled
         *
         */
        fun createDefault(context: Context): ImageLoaderConfiguration {
            return Builder(context).build()
        }
    }
}
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

import android.annotation.TargetApi
import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import br.com.ebook.universalimageloader.cache.disc.DiskCache
import br.com.ebook.universalimageloader.cache.disc.impl.UnlimitedDiskCache
import br.com.ebook.universalimageloader.cache.disc.impl.ext.LruDiskCache
import br.com.ebook.universalimageloader.cache.disc.naming.FileNameGenerator
import br.com.ebook.universalimageloader.cache.disc.naming.HashCodeFileNameGenerator
import br.com.ebook.universalimageloader.cache.memory.MemoryCache
import br.com.ebook.universalimageloader.cache.memory.impl.LruMemoryCache
import br.com.ebook.universalimageloader.core.assist.QueueProcessingType
import br.com.ebook.universalimageloader.core.assist.deque.LIFOLinkedBlockingDeque
import br.com.ebook.universalimageloader.core.decode.BaseImageDecoder
import br.com.ebook.universalimageloader.core.decode.ImageDecoder
import br.com.ebook.universalimageloader.core.display.BitmapDisplayer
import br.com.ebook.universalimageloader.core.display.SimpleBitmapDisplayer
import br.com.ebook.universalimageloader.core.download.BaseImageDownloader
import br.com.ebook.universalimageloader.core.download.ImageDownloader
import br.com.ebook.universalimageloader.utils.L.e
import br.com.ebook.universalimageloader.utils.StorageUtils.getCacheDirectory
import br.com.ebook.universalimageloader.utils.StorageUtils.getIndividualCacheDirectory
import java.io.File
import java.io.IOException
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Factory for providing of default options for [configuration][ImageLoaderConfiguration]
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.5.6
 */
object DefaultConfigurationFactory {
    /** Creates default implementation of task executor  */
	@JvmStatic
	fun createExecutor(
        threadPoolSize: Int, threadPriority: Int,
        tasksProcessingType: QueueProcessingType
    ): Executor {
        val lifo = tasksProcessingType === QueueProcessingType.LIFO
        val taskQueue: BlockingQueue<Runnable> = if (lifo) LIFOLinkedBlockingDeque() else LinkedBlockingQueue()
        return ThreadPoolExecutor(
            threadPoolSize, threadPoolSize, 0L, TimeUnit.MILLISECONDS, taskQueue,
            createThreadFactory(threadPriority, "uil-pool-")
        )
    }

    /** Creates default implementation of task distributor  */
	@JvmStatic
	fun createTaskDistributor(): Executor {
        return Executors.newCachedThreadPool(createThreadFactory(Thread.NORM_PRIORITY, "uil-pool-d-"))
    }

    /** Creates [default implementation][HashCodeFileNameGenerator] of FileNameGenerator  */
	@JvmStatic
	fun createFileNameGenerator(): FileNameGenerator {
        return HashCodeFileNameGenerator()
    }

    /**
     * Creates default implementation of [DiskCache] depends on incoming parameters
     */
	@JvmStatic
	fun createDiskCache(
        context: Context, diskCacheFileNameGenerator: FileNameGenerator?,
        diskCacheSize: Long, diskCacheFileCount: Int
    ): DiskCache {
        val reserveCacheDir = createReserveDiskCacheDir(context)
        if (diskCacheSize > 0 || diskCacheFileCount > 0) {
            val individualCacheDir = getIndividualCacheDirectory(context)
            try {
                return LruDiskCache(
                    individualCacheDir, reserveCacheDir, diskCacheFileNameGenerator, diskCacheSize,
                    diskCacheFileCount
                )
            } catch (e: IOException) {
                e(e)
                // continue and create unlimited cache
            }
        }
        val cacheDir = getCacheDirectory(context)
        return UnlimitedDiskCache(cacheDir, reserveCacheDir, diskCacheFileNameGenerator)
    }

    /** Creates reserve disk cache folder which will be used if primary disk cache folder becomes unavailable  */
    private fun createReserveDiskCacheDir(context: Context): File {
        var cacheDir = getCacheDirectory(context, false)
        val individualDir = File(cacheDir, "uil-images")
        if (individualDir.exists() || individualDir.mkdir()) {
            cacheDir = individualDir
        }
        return cacheDir
    }

    /**
     * Creates default implementation of [MemoryCache] - [LruMemoryCache]<br></br>
     * Default cache size = 1/8 of available app memory.
     */
	@JvmStatic
	fun createMemoryCache(context: Context, memoryCacheSize: Int): MemoryCache {
        var memoryCacheSize = memoryCacheSize
        if (memoryCacheSize == 0) {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            var memoryClass = am.memoryClass
            if (hasHoneycomb() && isLargeHeap(context)) {
                memoryClass = getLargeMemoryClass(am)
            }
            memoryCacheSize = 1024 * 1024 * memoryClass / 8
        }
        return LruMemoryCache(memoryCacheSize)
    }

    private fun hasHoneycomb(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private fun isLargeHeap(context: Context): Boolean {
        return context.applicationInfo.flags and ApplicationInfo.FLAG_LARGE_HEAP != 0
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private fun getLargeMemoryClass(am: ActivityManager): Int {
        return am.largeMemoryClass
    }

    /** Creates default implementation of [ImageDownloader] - [BaseImageDownloader]  */
	@JvmStatic
	fun createImageDownloader(context: Context?): ImageDownloader {
        return BaseImageDownloader(context!!)
    }

    /** Creates default implementation of [ImageDecoder] - [BaseImageDecoder]  */
	@JvmStatic
	fun createImageDecoder(loggingEnabled: Boolean): ImageDecoder {
        return BaseImageDecoder(loggingEnabled)
    }

    /** Creates default implementation of [BitmapDisplayer] - [SimpleBitmapDisplayer]  */
	@JvmStatic
	fun createBitmapDisplayer(): BitmapDisplayer {
        return SimpleBitmapDisplayer()
    }

    /** Creates default implementation of [thread factory][ThreadFactory] for task executor  */
    private fun createThreadFactory(threadPriority: Int, threadNamePrefix: String): ThreadFactory {
        return DefaultThreadFactory(threadPriority, threadNamePrefix)
    }

    private class DefaultThreadFactory internal constructor(private val threadPriority: Int, threadNamePrefix: String) : ThreadFactory {
        private val group: ThreadGroup
        private val threadNumber = AtomicInteger(1)
        private val namePrefix: String

        init {
            group = Thread.currentThread().threadGroup
            namePrefix = threadNamePrefix + poolNumber.getAndIncrement() + "-thread-"
        }

        override fun newThread(r: Runnable): Thread {
            val t = Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0)
            if (t.isDaemon) t.isDaemon = false
            t.priority = threadPriority
            return t
        }

        companion object {
            private val poolNumber = AtomicInteger(1)
        }
    }
}
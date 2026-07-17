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

import br.com.ebook.universalimageloader.core.DefaultConfigurationFactory.createExecutor
import br.com.ebook.universalimageloader.core.DefaultConfigurationFactory.createTaskDistributor
import br.com.ebook.universalimageloader.core.imageaware.ImageAware
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

/**
 * [ImageLoader] engine which responsible for [display task][LoadAndDisplayImageTask] execution.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.7.1
 */
internal class ImageLoaderEngine(val configuration: ImageLoaderConfiguration) {
    private var taskExecutor: Executor?
    private var taskExecutorForCachedImages: Executor?
    private val taskDistributor: Executor
    private val cacheKeysForImageAwares = Collections
        .synchronizedMap(HashMap<Int, String>())
    private val uriLocks: MutableMap<String, ReentrantLock> = WeakHashMap()
    val pause = AtomicBoolean(false)
    private val networkDenied = AtomicBoolean(false)
    private val slowNetwork = AtomicBoolean(false)
    val pauseLock = Object()

    init {
        taskExecutor = configuration.taskExecutor
        taskExecutorForCachedImages = configuration.taskExecutorForCachedImages
        taskDistributor = createTaskDistributor()
    }

    /** Submits task to execution pool  */
    fun submit(task: LoadAndDisplayImageTask) {
        taskDistributor.execute {
            val image = configuration.diskCache?.get(task.loadingUri)
            val isImageCachedOnDisk = image != null && image.exists()
            initExecutorsIfNeed()
            if (isImageCachedOnDisk) {
                taskExecutorForCachedImages!!.execute(task)
            } else {
                taskExecutor!!.execute(task)
            }
        }
    }

    /** Submits task to execution pool  */
    fun submit(task: ProcessAndDisplayImageTask?) {
        initExecutorsIfNeed()
        taskExecutorForCachedImages!!.execute(task)
    }

    private fun initExecutorsIfNeed() {
        if (!configuration.customExecutor && (taskExecutor as ExecutorService).isShutdown) {
            taskExecutor = createTaskExecutor()
        }
        if (!configuration.customExecutorForCachedImages && (taskExecutorForCachedImages as ExecutorService)
                .isShutdown
        ) {
            taskExecutorForCachedImages = createTaskExecutor()
        }
    }

    private fun createTaskExecutor(): Executor {
        return createExecutor(
            configuration.threadPoolSize, configuration.threadPriority,
            configuration.tasksProcessingType
        )
    }

    /**
     * Returns URI of image which is loading at this moment into passed [ImageAware]
     */
    fun getLoadingUriForView(imageAware: ImageAware): String? {
        return cacheKeysForImageAwares[imageAware.id]
    }

    /**
     * Associates **memoryCacheKey** with **imageAware**. Then it helps to define image URI is loaded into View at
     * exact moment.
     */
    fun prepareDisplayTaskFor(imageAware: ImageAware, memoryCacheKey: String) {
        cacheKeysForImageAwares[imageAware.id] = memoryCacheKey
    }

    /**
     * Cancels the task of loading and displaying image for incoming **imageAware**.
     *
     * @param imageAware [ImageAware] for which display task
     * will be cancelled
     */
    fun cancelDisplayTaskFor(imageAware: ImageAware) {
        cacheKeysForImageAwares.remove(imageAware.id)
    }

    fun cancelDisplayTaskFor(id: Int) {
        cacheKeysForImageAwares.remove(id)
    }

    /**
     * Denies or allows engine to download images from the network.<br></br> <br></br> If downloads are denied and if image
     * isn't cached then [ImageLoadingListener.onLoadingFailed] callback will be fired
     * with [FailReason.FailType.NETWORK_DENIED]
     *
     * @param denyNetworkDownloads pass **true** - to deny engine to download images from the network; **false** -
     * to allow engine to download images from network.
     */
    fun denyNetworkDownloads(denyNetworkDownloads: Boolean) {
        networkDenied.set(denyNetworkDownloads)
    }

    /**
     * Sets option whether ImageLoader will use [FlushedInputStream] for network downloads to handle [this known problem](http://code.google.com/p/android/issues/detail?id=6066) or not.
     *
     * @param handleSlowNetwork pass **true** - to use [FlushedInputStream] for network downloads; **false**
     * - otherwise.
     */
    fun handleSlowNetwork(handleSlowNetwork: Boolean) {
        slowNetwork.set(handleSlowNetwork)
    }

    /**
     * Pauses engine. All new "load&display" tasks won't be executed until ImageLoader is [resumed][.resume].<br></br> Already running tasks are not paused.
     */
    fun pause() {
        pause.set(true)
    }

    /** Resumes engine work. Paused "load&display" tasks will continue its work.  */
    fun resume() {
        pause.set(false)
        synchronized(pauseLock) { pauseLock.notifyAll() }
    }

    /**
     * Stops engine, cancels all running and scheduled display image tasks. Clears internal data.
     * <br></br>
     * **NOTE:** This method doesn't shutdown
     * [ custom task executors][ImageLoaderConfiguration.Builder.taskExecutor] if you set them.
     */
    fun stop() {
        if (!configuration.customExecutor) {
            (taskExecutor as ExecutorService).shutdownNow()
        }
        if (!configuration.customExecutorForCachedImages) {
            (taskExecutorForCachedImages as ExecutorService).shutdownNow()
        }
        cacheKeysForImageAwares.clear()
        uriLocks.clear()
    }

    fun clearAllTasks() {
        cacheKeysForImageAwares.clear()
        uriLocks.clear()
    }

    fun fireCallback(r: Runnable?) {
        taskDistributor.execute(r)
    }

    fun getLockForUri(uri: String): ReentrantLock {
        var lock = uriLocks[uri]
        if (lock == null) {
            lock = ReentrantLock()
            uriLocks[uri] = lock
        }
        return lock
    }

    fun isNetworkDenied(): Boolean {
        return networkDenied.get()
    }

    fun isSlowNetwork(): Boolean {
        return slowNetwork.get()
    }
}
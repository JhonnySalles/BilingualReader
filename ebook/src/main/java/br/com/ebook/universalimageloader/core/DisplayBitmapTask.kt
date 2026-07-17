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
import br.com.ebook.universalimageloader.core.assist.LoadedFrom
import br.com.ebook.universalimageloader.core.display.BitmapDisplayer
import br.com.ebook.universalimageloader.core.imageaware.ImageAware
import br.com.ebook.universalimageloader.core.listener.ImageLoadingListener
import br.com.ebook.universalimageloader.utils.L.d

/**
 * Displays bitmap in
 * [ImageAware]. Must be
 * called on UI thread.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @see ImageLoadingListener
 *
 * @see BitmapDisplayer
 *
 * @since 1.3.1
 */
internal class DisplayBitmapTask(private val bitmap: Bitmap, imageLoadingInfo: ImageLoadingInfo, engine: ImageLoaderEngine, loadedFrom: LoadedFrom) :
    Runnable {
    private val imageUri: String
    private val imageAware: ImageAware
    private val memoryCacheKey: String
    private val displayer: BitmapDisplayer
    private val listener: ImageLoadingListener
    private val engine: ImageLoaderEngine
    private val loadedFrom: LoadedFrom

    init {
        imageUri = imageLoadingInfo.uri
        imageAware = imageLoadingInfo.imageAware
        memoryCacheKey = imageLoadingInfo.memoryCacheKey
        displayer = imageLoadingInfo.options.displayer
        listener = imageLoadingInfo.listener
        this.engine = engine
        this.loadedFrom = loadedFrom
    }

    override fun run() {
        if (imageAware.isCollected) {
            d(LOG_TASK_CANCELLED_IMAGEAWARE_COLLECTED, memoryCacheKey)
            listener.onLoadingCancelled(imageUri, imageAware.wrappedView)
        } else if (isViewWasReused) {
            d(LOG_TASK_CANCELLED_IMAGEAWARE_REUSED, memoryCacheKey)
            listener.onLoadingCancelled(imageUri, imageAware.wrappedView)
            displayer.display(bitmap, imageAware, loadedFrom)
            engine.cancelDisplayTaskFor(imageAware)
        } else {
            d(LOG_DISPLAY_IMAGE_IN_IMAGEAWARE, loadedFrom, memoryCacheKey)
            displayer.display(bitmap, imageAware, loadedFrom)
            engine.cancelDisplayTaskFor(imageAware)
            listener.onLoadingComplete(imageUri, imageAware.wrappedView, bitmap)
        }
    }

    /**
     * Checks whether memory cache key (image URI) for current ImageAware is
     * actual
     */
    private val isViewWasReused: Boolean
        private get() {
            val currentCacheKey = engine.getLoadingUriForView(imageAware)
            return memoryCacheKey != currentCacheKey
        }

    companion object {
        private const val LOG_DISPLAY_IMAGE_IN_IMAGEAWARE = "Display image in ImageAware (loaded from %1\$s) [%2\$s]"
        private const val LOG_TASK_CANCELLED_IMAGEAWARE_REUSED = "ImageAware is reused for another image. Task is cancelled. [%s]"
        private const val LOG_TASK_CANCELLED_IMAGEAWARE_COLLECTED = "ImageAware was collected by GC. Task is cancelled. [%s]"
    }
}
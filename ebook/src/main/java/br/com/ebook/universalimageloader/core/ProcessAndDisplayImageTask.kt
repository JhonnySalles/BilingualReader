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
import br.com.ebook.universalimageloader.core.assist.LoadedFrom
import br.com.ebook.universalimageloader.utils.L.d

/**
 * Presents process'n'display image task. Processes image [Bitmap] and display it in [ImageView] using
 * [DisplayBitmapTask].
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.8.0
 */
internal class ProcessAndDisplayImageTask(
    private val engine: ImageLoaderEngine, private val bitmap: Bitmap, private val imageLoadingInfo: ImageLoadingInfo,
    private val handler: Handler
) : Runnable {
    override fun run() {
        d(LOG_POSTPROCESS_IMAGE, imageLoadingInfo.memoryCacheKey)
        val processor = imageLoadingInfo.options.postProcessor
        val processedBitmap = processor!!.process(bitmap)
        val displayBitmapTask = DisplayBitmapTask(
            processedBitmap!!, imageLoadingInfo, engine,
            LoadedFrom.MEMORY_CACHE
        )
        LoadAndDisplayImageTask.runTask(displayBitmapTask, imageLoadingInfo.options.isSyncLoading, handler, engine)
    }

    companion object {
        private const val LOG_POSTPROCESS_IMAGE = "PostProcess image before displaying [%s]"
    }
}
/*******************************************************************************
 * Copyright 2013-2014 Sergey Tarasevich
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
package br.com.ebook.universalimageloader.core.imageaware

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.view.View
import br.com.ebook.universalimageloader.core.assist.ImageSize
import br.com.ebook.universalimageloader.core.assist.ViewScaleType

/**
 * ImageAware which provides needed info for processing of original image but do nothing for displaying image. It's
 * used when user need just load and decode image and get it in [ ][ImageLoadingListener.onLoadingComplete].
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.9.0
 */
class NonViewAware(imageUri: String?, imageSize: ImageSize?, scaleType: ViewScaleType?) : ImageAware {
    protected val imageUri: String?
    protected val imageSize: ImageSize
    override val scaleType: ViewScaleType

    constructor(imageSize: ImageSize?, scaleType: ViewScaleType?) : this(null, imageSize, scaleType) {}

    init {
        requireNotNull(imageSize) { "imageSize must not be null" }
        requireNotNull(scaleType) { "scaleType must not be null" }
        this.imageUri = imageUri
        this.imageSize = imageSize
        this.scaleType = scaleType
    }

    override val width: Int
        get() = imageSize.width
    override val height: Int
        get() = imageSize.height
    override val wrappedView: View?
        get() = null
    override val isCollected: Boolean
        get() = false
    override val id: Int
        get() = if (TextUtils.isEmpty(imageUri)) super.hashCode() else imageUri.hashCode()

    override fun setImageDrawable(drawable: Drawable?): Boolean { // Do nothing
        return true
    }

    override fun setImageBitmap(bitmap: Bitmap?): Boolean { // Do nothing
        return true
    }
}
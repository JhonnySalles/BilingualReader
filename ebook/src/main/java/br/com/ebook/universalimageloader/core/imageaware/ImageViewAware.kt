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
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import br.com.ebook.universalimageloader.core.assist.ViewScaleType
import br.com.ebook.universalimageloader.core.assist.ViewScaleType.Companion.fromImageView
import br.com.ebook.universalimageloader.utils.L.e

/**
 * Wrapper for Android [ImageView][android.widget.ImageView]. Keeps weak reference of ImageView to prevent memory
 * leaks.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.9.0
 */
class ImageViewAware : ViewAware {
    /**
     * Constructor. <br></br>
     * References [ImageViewAware(imageView, true)][.ImageViewAware].
     *
     * @param imageView [ImageView][android.widget.ImageView] to work with
     */
    constructor(imageView: ImageView?) : super(imageView) {}

    /**
     * Constructor
     *
     * @param imageView           [ImageView][android.widget.ImageView] to work with
     * @param checkActualViewSize **true** - then [.getWidth] and [.getHeight] will check actual
     * size of ImageView. It can cause known issues like
     * [this](https://github.com/nostra13/Android-Universal-Image-Loader/issues/376).
     * But it helps to save memory because memory cache keeps bitmaps of actual (less in
     * general) size.
     *
     *
     * **false** - then [.getWidth] and [.getHeight] will **NOT**
     * consider actual size of ImageView, just layout parameters. <br></br> If you set 'false'
     * it's recommended 'android:layout_width' and 'android:layout_height' (or
     * 'android:maxWidth' and 'android:maxHeight') are set with concrete values. It helps to
     * save memory.
     *
     *
     */
    constructor(imageView: ImageView?, checkActualViewSize: Boolean) : super(imageView, checkActualViewSize) {}// Check maxWidth parameter

    /**
     * {@inheritDoc}
     * <br></br>
     * 3) Get **maxWidth**.
     */
    override val width: Int
        get() {
            var width = super.width
            if (width <= 0) {
                val imageView = viewRef.get() as ImageView?
                if (imageView != null) {
                    width = getImageViewFieldValue(imageView, "mMaxWidth") // Check maxWidth parameter
                }
            }
            return width
        }// Check maxHeight parameter

    /**
     * {@inheritDoc}
     * <br></br>
     * 3) Get **maxHeight**
     */
    override val height: Int
        get() {
            var height = super.height
            if (height <= 0) {
                val imageView = viewRef.get() as ImageView?
                if (imageView != null) {
                    height = getImageViewFieldValue(imageView, "mMaxHeight") // Check maxHeight parameter
                }
            }
            return height
        }
    override val scaleType: ViewScaleType
        get() {
            val imageView = viewRef.get() as ImageView?
            return if (imageView != null) {
                fromImageView(imageView)
            } else super.scaleType
        }
    override val wrappedView: ImageView?
        get() = super.wrappedView as ImageView?

    override fun setImageDrawableInto(drawable: Drawable?, view: View?) {
        (view as ImageView?)!!.setImageDrawable(drawable)
        if (drawable is AnimationDrawable) {
            drawable.start()
        }
    }

    override fun setImageBitmapInto(bitmap: Bitmap?, view: View?) {
        (view as ImageView?)!!.setImageBitmap(bitmap)
    }

    companion object {
        private fun getImageViewFieldValue(`object`: Any, fieldName: String): Int {
            var value = 0
            try {
                val field = ImageView::class.java.getDeclaredField(fieldName)
                field.isAccessible = true
                val fieldValue = field[`object`] as Int
                if (fieldValue > 0 && fieldValue < Int.MAX_VALUE) {
                    value = fieldValue
                }
            } catch (e: Exception) {
                e(e)
            }
            return value
        }
    }
}
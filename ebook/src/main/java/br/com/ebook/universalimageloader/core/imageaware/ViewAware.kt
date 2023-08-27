/*******************************************************************************
 * Copyright 2014 Sergey Tarasevich
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
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import br.com.ebook.universalimageloader.core.assist.ViewScaleType
import br.com.ebook.universalimageloader.utils.L.w
import java.lang.ref.Reference
import java.lang.ref.WeakReference

/**
 * Wrapper for Android [View][android.view.View]. Keeps weak reference of View to prevent memory leaks.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.9.2
 */
abstract class ViewAware @JvmOverloads constructor(view: View?, checkActualViewSize: Boolean = true) : ImageAware {
    @JvmField
    protected var viewRef: Reference<View?>
    protected var checkActualViewSize: Boolean
    /**
     * Constructor
     *
     * @param view                [View][android.view.View] to work with
     * @param checkActualViewSize **true** - then [.getWidth] and [.getHeight] will check actual
     * size of View. It can cause known issues like
     * [this](https://github.com/nostra13/Android-Universal-Image-Loader/issues/376).
     * But it helps to save memory because memory cache keeps bitmaps of actual (less in
     * general) size.
     *
     *
     * **false** - then [.getWidth] and [.getHeight] will **NOT**
     * consider actual size of View, just layout parameters. <br></br> If you set 'false'
     * it's recommended 'android:layout_width' and 'android:layout_height' (or
     * 'android:maxWidth' and 'android:maxHeight') are set with concrete values. It helps to
     * save memory.
     */
    /**
     * Constructor. <br></br>
     * References [ImageViewAware(imageView, true)][.ViewAware].
     *
     * @param view [View][android.view.View] to work with
     */
    init {
        requireNotNull(view) { "view must not be null" }
        viewRef = WeakReference(view)
        this.checkActualViewSize = checkActualViewSize
    }// Get actual image width
    // Get layout width parameter
    /**
     * {@inheritDoc}
     *
     *
     * Width is defined by target [view][android.view.View] parameters, configuration
     * parameters or device display dimensions.<br></br>
     * Size computing algorithm (go by steps until get non-zero value):<br></br>
     * 1) Get the actual drawn **getWidth()** of the View<br></br>
     * 2) Get **layout_width**
     */
    override val width: Int
        get() {
            val view = viewRef.get()
            if (view != null) {
                val params = view.layoutParams
                var width = 0
                if (checkActualViewSize && params != null && params.width != ViewGroup.LayoutParams.WRAP_CONTENT) {
                    width = view.width // Get actual image width
                }
                if (width <= 0 && params != null) width = params.width // Get layout width parameter
                return width
            }
            return 0
        }// Get actual image height
    // Get layout height parameter
    /**
     * {@inheritDoc}
     *
     *
     * Height is defined by target [view][android.view.View] parameters, configuration
     * parameters or device display dimensions.<br></br>
     * Size computing algorithm (go by steps until get non-zero value):<br></br>
     * 1) Get the actual drawn **getHeight()** of the View<br></br>
     * 2) Get **layout_height**
     */
    override val height: Int
        get() {
            val view = viewRef.get()
            if (view != null) {
                val params = view.layoutParams
                var height = 0
                if (checkActualViewSize && params != null && params.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
                    height = view.height // Get actual image height
                }
                if (height <= 0 && params != null) height = params.height // Get layout height parameter
                return height
            }
            return 0
        }
    override val scaleType: ViewScaleType
        get() = ViewScaleType.CROP
    override val wrappedView: View?
        get() = viewRef.get()
    override val isCollected: Boolean
        get() = viewRef.get() == null
    override val id: Int
        get() {
            val view = viewRef.get()
            return view?.hashCode() ?: super.hashCode()
        }

    override fun setImageDrawable(drawable: Drawable?): Boolean {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            val view = viewRef.get()
            if (view != null) {
                setImageDrawableInto(drawable, view)
                return true
            }
        } else {
            w(WARN_CANT_SET_DRAWABLE)
        }
        return false
    }

    override fun setImageBitmap(bitmap: Bitmap?): Boolean {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            val view = viewRef.get()
            if (view != null) {
                setImageBitmapInto(bitmap, view)
                return true
            }
        } else {
            w(WARN_CANT_SET_BITMAP)
        }
        return false
    }

    /**
     * Should set drawable into incoming view. Incoming view is guaranteed not null.<br></br>
     * This method is called on UI thread.
     */
    protected abstract fun setImageDrawableInto(drawable: Drawable?, view: View?)

    /**
     * Should set Bitmap into incoming view. Incoming view is guaranteed not null.< br />
     * This method is called on UI thread.
     */
    protected abstract fun setImageBitmapInto(bitmap: Bitmap?, view: View?)

    companion object {
        const val WARN_CANT_SET_DRAWABLE = "Can't set a drawable into view. You should call ImageLoader on UI thread for it."
        const val WARN_CANT_SET_BITMAP = "Can't set a bitmap into view. You should call ImageLoader on UI thread for it."
    }
}
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
import android.view.View
import br.com.ebook.universalimageloader.core.assist.ViewScaleType

/**
 * Represents image aware view which provides all needed properties and behavior for image processing and displaying
 * through [ImageLoader].
 * It can wrap any Android [View][android.view.View] which can be accessed by [.getWrappedView]. Wrapped
 * view is returned in [ImageLoadingListener]'s
 * callbacks.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @see ViewAware
 *
 * @see ImageViewAware
 *
 * @see NonViewAware
 *
 * @since 1.9.0
 */
interface ImageAware {
    /**
     * Returns width of image aware view. This value is used to define scale size for original image.
     * Can return 0 if width is undefined.<br></br>
     * Is called on UI thread if ImageLoader was called on UI thread. Otherwise - on background thread.
     */
    val width: Int

    /**
     * Returns height of image aware view. This value is used to define scale size for original image.
     * Can return 0 if height is undefined.<br></br>
     * Is called on UI thread if ImageLoader was called on UI thread. Otherwise - on background thread.
     */
    val height: Int

    /**
     * Returns [scale type][ViewScaleType] which is used for
     * scaling image for this image aware view. Must **NOT** return **null**.
     */
    val scaleType: ViewScaleType?

    /**
     * Returns wrapped Android [View][android.view.View]. Can return **null** if no view is wrapped or view was
     * collected by GC.<br></br>
     * Is called on UI thread if ImageLoader was called on UI thread. Otherwise - on background thread.
     */
    val wrappedView: View?

    /**
     * Returns a flag whether image aware view is collected by GC or whatsoever. If so then ImageLoader stop processing
     * of task for this image aware view and fires
     * [ImageLoadingListener#onLoadingCancelled(String, View)][ImageLoadingListener.onLoadingCancelled] callback.<br></br>
     * Mey be called on UI thread if ImageLoader was called on UI thread. Otherwise - on background thread.
     *
     * @return **true** - if view is collected by GC and ImageLoader should stop processing this image aware view;
     * **false** - otherwise
     */
    val isCollected: Boolean

    /**
     * Returns ID of image aware view. Point of ID is similar to Object's hashCode. This ID should be unique for every
     * image view instance and should be the same for same instances. This ID identifies processing task in ImageLoader
     * so ImageLoader won't process two image aware views with the same ID in one time. When ImageLoader get new task
     * it cancels old task with this ID (if any) and starts new task.
     *
     *
     * It's reasonable to return hash code of wrapped view (if any) to prevent displaying non-actual images in view
     * because of view re-using.
     */
    val id: Int

    /**
     * Sets image drawable into this image aware view.<br></br>
     * Displays drawable in this image aware view
     * [for empty Uri][DisplayImageOptions.Builder.showImageForEmptyUri],
     * [on loading][DisplayImageOptions.Builder.showImageOnLoading] or
     * [on loading fail][DisplayImageOptions.Builder.showImageOnFail]. These drawables can be specified in
     * [display options][DisplayImageOptions].<br></br>
     * Also can be called in [BitmapDisplayer].< br />
     * Is called on UI thread if ImageLoader was called on UI thread. Otherwise - on background thread.
     *
     * @return **true** if drawable was set successfully; **false** - otherwise
     */
    fun setImageDrawable(drawable: Drawable?): Boolean

    /**
     * Sets image bitmap into this image aware view.<br></br>
     * Displays loaded and decoded image [android.graphics.Bitmap] in this image view aware.
     * Actually it's used only in
     * [BitmapDisplayer].< br />
     * Is called on UI thread if ImageLoader was called on UI thread. Otherwise - on background thread.
     *
     * @return **true** if bitmap was set successfully; **false** - otherwise
     */
    fun setImageBitmap(bitmap: Bitmap?): Boolean
}
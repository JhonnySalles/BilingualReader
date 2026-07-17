/*******************************************************************************
 * Copyright 2011-2013 Sergey Tarasevich
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
package br.com.ebook.universalimageloader.core.listener

import android.graphics.Bitmap
import android.view.View
import br.com.ebook.universalimageloader.core.assist.FailReason

/**
 * Listener for image loading process.<br></br>
 * You can use [SimpleImageLoadingListener] for implementing only needed methods.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @see SimpleImageLoadingListener
 *
 * @see FailReason
 *
 * @since 1.0.0
 */
interface ImageLoadingListener {
    /**
     * Is called when image loading task was started
     *
     * @param imageUri Loading image URI
     * @param view     View for image
     */
    fun onLoadingStarted(imageUri: String?, view: View?)

    /**
     * Is called when an error was occurred during image loading
     *
     * @param imageUri   Loading image URI
     * @param view       View for image. Can be **null**.
     * @param failReason [The reason][FailReason] why image
     * loading was failed
     */
    fun onLoadingFailed(imageUri: String?, view: View?, failReason: FailReason?)

    /**
     * Is called when image is loaded successfully (and displayed in View if one was specified)
     *
     * @param imageUri    Loaded image URI
     * @param view        View for image. Can be **null**.
     * @param loadedImage Bitmap of loaded and decoded image
     */
    fun onLoadingComplete(imageUri: String?, view: View?, loadedImage: Bitmap?)

    /**
     * Is called when image loading task was cancelled because View for image was reused in newer task
     *
     * @param imageUri Loading image URI
     * @param view     View for image. Can be **null**.
     */
    fun onLoadingCancelled(imageUri: String?, view: View?)
}
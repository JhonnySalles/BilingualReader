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
 * A convenient class to extend when you only want to listen for a subset of all the image loading events. This
 * implements all methods in the [ImageLoadingListener] but does
 * nothing.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.4.0
 */
open class SimpleImageLoadingListener : ImageLoadingListener {
    override fun onLoadingStarted(imageUri: String?, view: View?) {
        // Empty implementation
    }

    override fun onLoadingFailed(imageUri: String?, view: View?, failReason: FailReason?) {
        // Empty implementation
    }

    override fun onLoadingComplete(imageUri: String?, view: View?, loadedImage: Bitmap?) {
        // Empty implementation
    }

    override fun onLoadingCancelled(imageUri: String?, view: View?) {
        // Empty implementation
    }
}
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
package br.com.ebook.universalimageloader.core.display

import android.graphics.Bitmap
import br.com.ebook.universalimageloader.core.assist.LoadedFrom
import br.com.ebook.universalimageloader.core.imageaware.ImageAware

/**
 * Displays [Bitmap] in [ImageAware]. Implementations can
 * apply some changes to Bitmap or any animation for displaying Bitmap.<br></br>
 * Implementations have to be thread-safe.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @see ImageAware
 *
 * @see LoadedFrom
 *
 * @since 1.5.6
 */
interface BitmapDisplayer {
    /**
     * Displays bitmap in [ImageAware].
     * **NOTE:** This method is called on UI thread so it's strongly recommended not to do any heavy work in it.
     *
     * @param bitmap     Source bitmap
     * @param imageAware [Image aware view][ImageAware] to
     * display Bitmap
     * @param loadedFrom Source of loaded image
     */
    fun display(bitmap: Bitmap?, imageAware: ImageAware?, loadedFrom: LoadedFrom?)
}
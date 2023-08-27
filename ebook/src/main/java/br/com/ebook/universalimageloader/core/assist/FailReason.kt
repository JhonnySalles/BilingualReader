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
package br.com.ebook.universalimageloader.core.assist

import br.com.ebook.universalimageloader.core.assist.FailReason.FailType

/**
 * Presents the reason why image loading and displaying was failed
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.0.0
 */
class FailReason(
    /** @return [Fail type][FailType]
     */
    val type: FailType,
    /** @return Thrown exception/error, can be **null**
     */
    val cause: Throwable
) {

    /** Presents type of fail while image loading  */
    enum class FailType {
        /** Input/output error. Can be caused by network communication fail or error while caching image on file system.  */
        IO_ERROR,

        /**
         * Error while
         * [ decode image to Bitmap][android.graphics.BitmapFactory.decodeStream]
         */
        DECODING_ERROR,

        /**
         * [Network][ImageLoader.denyNetworkDownloads] and requested image wasn't cached in disk cache before.
         */
        NETWORK_DENIED,

        /** Not enough memory to create needed Bitmap for image  */
        OUT_OF_MEMORY,

        /** Unknown error was occurred while loading image  */
        UNKNOWN
    }
}
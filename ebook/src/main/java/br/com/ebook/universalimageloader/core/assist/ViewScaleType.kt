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
package br.com.ebook.universalimageloader.core.assist

import android.widget.ImageView

/**
 * Simplify [ImageView&#39;s scale type][ScaleType] to 2 types: [.FIT_INSIDE] and [.CROP]
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.6.1
 */
enum class ViewScaleType {
    /**
     * Scale the image uniformly (maintain the image's aspect ratio) so that at least one dimension (width or height) of
     * the image will be equal to or less the corresponding dimension of the view.
     */
    FIT_INSIDE,

    /**
     * Scale the image uniformly (maintain the image's aspect ratio) so that both dimensions (width and height) of the
     * image will be equal to or larger than the corresponding dimension of the view.
     */
    CROP;

    companion object {
        /**
         * Defines scale type of ImageView.
         *
         * @param imageView [ImageView]
         * @return [.FIT_INSIDE] for
         *
         *  * [ScaleType.FIT_CENTER]
         *  * [ScaleType.FIT_XY]
         *  * [ScaleType.FIT_START]
         *  * [ScaleType.FIT_END]
         *  * [ScaleType.CENTER_INSIDE]
         *
         * [.CROP] for
         *
         *  * [ScaleType.CENTER]
         *  * [ScaleType.CENTER_CROP]
         *  * [ScaleType.MATRIX]
         *
         */
        @JvmStatic
        fun fromImageView(imageView: ImageView): ViewScaleType {
            return when (imageView.scaleType) {
                ImageView.ScaleType.FIT_CENTER, ImageView.ScaleType.FIT_XY, ImageView.ScaleType.FIT_START, ImageView.ScaleType.FIT_END, ImageView.ScaleType.CENTER_INSIDE -> FIT_INSIDE
                ImageView.ScaleType.MATRIX, ImageView.ScaleType.CENTER, ImageView.ScaleType.CENTER_CROP -> CROP
                else -> CROP
            }
        }
    }
}
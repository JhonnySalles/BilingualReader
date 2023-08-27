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

/**
 * Present width and height values
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.0.0
 */
class ImageSize {
    val width: Int
    val height: Int

    constructor(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    constructor(width: Int, height: Int, rotation: Int) {
        if (rotation % 180 == 0) {
            this.width = width
            this.height = height
        } else {
            this.width = height
            this.height = width
        }
    }

    /** Scales down dimensions in **sampleSize** times. Returns new object.  */
    fun scaleDown(sampleSize: Int): ImageSize {
        return ImageSize(width / sampleSize, height / sampleSize)
    }

    /** Scales dimensions according to incoming scale. Returns new object.  */
    fun scale(scale: Float): ImageSize {
        return ImageSize((width * scale).toInt(), (height * scale).toInt())
    }

    override fun toString(): String {
        return StringBuilder(TO_STRING_MAX_LENGHT).append(width).append(SEPARATOR).append(height).toString()
    }

    companion object {
        private const val TO_STRING_MAX_LENGHT = 9 // "9999x9999".length()
        private const val SEPARATOR = "x"
    }
}
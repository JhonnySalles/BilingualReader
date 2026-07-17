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

/**
 * Type of image scaling during decoding.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.5.0
 */
enum class ImageScaleType {
    /** Image won't be scaled  */
    NONE,

    /**
     * Image will be scaled down only if image size is greater than
     * [maximum acceptable texture size][javax.microedition.khronos.opengles.GL10.GL_MAX_TEXTURE_SIZE].
     * Usually it's 2048x2048.<br></br>
     * If Bitmap is expected to display than it must not exceed this size (otherwise you'll get the exception
     * "OpenGLRenderer: Bitmap too large to be uploaded into a texture".<br></br>
     * Image will be subsampled in an integer number of times (1, 2, 3, ...) to maximum texture size of device.
     */
    NONE_SAFE,

    /**
     * Image will be reduces 2-fold until next reduce step make image smaller target size.<br></br>
     * It's **fast** type and it's preferable for usage in lists/grids/galleries (and other
     * [adapter-views][android.widget.AdapterView]) .<br></br>
     * Relates to [android.graphics.BitmapFactory.Options.inSampleSize]<br></br>
     * Note: If original image size is smaller than target size then original image **won't** be scaled.
     */
    IN_SAMPLE_POWER_OF_2,

    /**
     * Image will be subsampled in an integer number of times (1, 2, 3, ...). Use it if memory economy is quite
     * important.<br></br>
     * Relates to [android.graphics.BitmapFactory.Options.inSampleSize]<br></br>
     * Note: If original image size is smaller than target size then original image **won't** be scaled.
     */
    IN_SAMPLE_INT,

    /**
     * Image will scaled-down exactly to target size (scaled width or height or both will be equal to target size;
     * depends on [ImageView&#39;s scale type][android.widget.ImageView.ScaleType]). Use it if memory economy is
     * critically important.<br></br>
     * **Note:** If original image size is smaller than target size then original image **won't** be scaled.<br></br>
     * <br></br>
     * **NOTE:** For creating result Bitmap (of exact size) additional Bitmap will be created with
     * [ Bitmap.createBitmap(...)][android.graphics.Bitmap.createBitmap].<br></br>
     * **Cons:** Saves memory by keeping smaller Bitmap in memory cache (comparing with IN_SAMPLE... scale types)<br></br>
     * **Pros:** Requires more memory in one time for creation of result Bitmap.
     */
    EXACTLY,

    /**
     * Image will scaled exactly to target size (scaled width or height or both will be equal to target size; depends on
     * [ImageView&#39;s scale type][android.widget.ImageView.ScaleType]). Use it if memory economy is critically
     * important.<br></br>
     * **Note:** If original image size is smaller than target size then original image **will be stretched** to
     * target size.<br></br>
     * <br></br>
     * **NOTE:** For creating result Bitmap (of exact size) additional Bitmap will be created with
     * [ Bitmap.createBitmap(...)][android.graphics.Bitmap.createBitmap].<br></br>
     * **Cons:** Saves memory by keeping smaller Bitmap in memory cache (comparing with IN_SAMPLE... scale types)<br></br>
     * **Pros:** Requires more memory in one time for creation of result Bitmap.
     */
    EXACTLY_STRETCHED
}
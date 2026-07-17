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
package br.com.ebook.universalimageloader.cache.disc

import android.graphics.Bitmap
import br.com.ebook.universalimageloader.utils.IoUtils.CopyListener
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Interface for disk cache
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.9.2
 */
interface DiskCache {
    /**
     * Returns root directory of disk cache
     *
     * @return Root directory of disk cache
     */
    val directory: File?

    /**
     * Returns file of cached image
     *
     * @param imageUri Original image URI
     * @return File of cached image or **null** if image wasn't cached
     */
    operator fun get(imageUri: String?): File?

    /**
     * Saves image stream in disk cache.
     * Incoming image stream shouldn't be closed in this method.
     *
     * @param imageUri    Original image URI
     * @param imageStream Input stream of image (shouldn't be closed in this method)
     * @param listener    Listener for saving progress, can be ignored if you don't use
     * [                    progress listener][ImageLoadingProgressListener] in ImageLoader calls
     * @return **true** - if image was saved successfully; **false** - if image wasn't saved in disk cache.
     * @throws java.io.IOException
     */
    @Throws(IOException::class)
    fun save(imageUri: String?, imageStream: InputStream?, listener: CopyListener?): Boolean

    /**
     * Saves image bitmap in disk cache.
     *
     * @param imageUri Original image URI
     * @param bitmap   Image bitmap
     * @return **true** - if bitmap was saved successfully; **false** - if bitmap wasn't saved in disk cache.
     * @throws IOException
     */
    @Throws(IOException::class)
    fun save(imageUri: String?, bitmap: Bitmap?): Boolean

    /**
     * Removes image file associated with incoming URI
     *
     * @param imageUri Image URI
     * @return **true** - if image file is deleted successfully; **false** - if image file doesn't exist for
     * incoming URI or image file can't be deleted.
     */
    fun remove(imageUri: String?): Boolean

    /** Closes disk cache, releases resources.  */
    fun close()

    /** Clears disk cache.  */
    fun clear()
}
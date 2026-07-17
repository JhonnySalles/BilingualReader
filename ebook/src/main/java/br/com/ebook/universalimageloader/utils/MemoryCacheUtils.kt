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
package br.com.ebook.universalimageloader.utils

import android.graphics.Bitmap
import br.com.ebook.universalimageloader.cache.memory.MemoryCache
import br.com.ebook.universalimageloader.core.assist.ImageSize

/**
 * Utility for generating of keys for memory cache, key comparing and other work with memory cache
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.6.3
 */
object MemoryCacheUtils {
    private const val URI_AND_SIZE_SEPARATOR = "_"
    private const val WIDTH_AND_HEIGHT_SEPARATOR = "x"

    /**
     * Generates key for memory cache for incoming image (URI + size).<br></br>
     * Pattern for cache key - **[imageUri]_[width]x[height]**.
     */
	@JvmStatic
	fun generateKey(imageUri: String?, targetSize: ImageSize): String {
        return StringBuilder(imageUri).append(URI_AND_SIZE_SEPARATOR).append(targetSize.width).append(WIDTH_AND_HEIGHT_SEPARATOR)
            .append(targetSize.height).toString()
    }

    @JvmStatic
	fun createFuzzyKeyComparator(): Comparator<String?> {
        return Comparator { key1, key2 ->
            val imageUri1 = key1!!.substring(0, key1.lastIndexOf(URI_AND_SIZE_SEPARATOR))
            val imageUri2 = key2!!.substring(0, key2.lastIndexOf(URI_AND_SIZE_SEPARATOR))
            imageUri1.compareTo(imageUri2)
        }
    }

    /**
     * Searches all bitmaps in memory cache which are corresponded to incoming URI.<br></br>
     * **Note:** Memory cache can contain multiple sizes of the same image if only you didn't set
     * [ denyCacheImageMultipleSizesInMemory()][ImageLoaderConfiguration.Builder.denyCacheImageMultipleSizesInMemory] option in [configuration][ImageLoaderConfiguration]
     */
    fun findCachedBitmapsForImageUri(imageUri: String?, memoryCache: MemoryCache): List<Bitmap?> {
        val values: MutableList<Bitmap?> = ArrayList()
        for (key in memoryCache.keys()) {
            if (key.startsWith(imageUri!!)) {
                values.add(memoryCache[key])
            }
        }
        return values
    }

    /**
     * Searches all keys in memory cache which are corresponded to incoming URI.<br></br>
     * **Note:** Memory cache can contain multiple sizes of the same image if only you didn't set
     * [ denyCacheImageMultipleSizesInMemory()][ImageLoaderConfiguration.Builder.denyCacheImageMultipleSizesInMemory] option in [configuration][ImageLoaderConfiguration]
     */
    fun findCacheKeysForImageUri(imageUri: String?, memoryCache: MemoryCache): List<String> {
        val values: MutableList<String> = ArrayList()
        for (key in memoryCache.keys()) {
            if (key.startsWith(imageUri!!)) {
                values.add(key)
            }
        }
        return values
    }

    /**
     * Removes from memory cache all images for incoming URI.<br></br>
     * **Note:** Memory cache can contain multiple sizes of the same image if only you didn't set
     * [ denyCacheImageMultipleSizesInMemory()][ImageLoaderConfiguration.Builder.denyCacheImageMultipleSizesInMemory] option in [configuration][ImageLoaderConfiguration]
     */
    fun removeFromCache(imageUri: String?, memoryCache: MemoryCache) {
        val keysToRemove: MutableList<String> = ArrayList()
        for (key in memoryCache.keys()) {
            if (key.startsWith(imageUri!!)) {
                keysToRemove.add(key)
            }
        }
        for (keyToRemove in keysToRemove) {
            memoryCache.remove(keyToRemove)
        }
    }
}
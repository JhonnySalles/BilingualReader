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
package br.com.ebook.universalimageloader.cache.memory

import android.graphics.Bitmap
import br.com.ebook.universalimageloader.utils.L.w
import java.util.Collections
import java.util.LinkedList
import java.util.concurrent.atomic.AtomicInteger

/**
 * Limited cache. Provides object storing. Size of all stored bitmaps will not to exceed size limit (
 * [.getSizeLimit]).<br></br>
 * <br></br>
 * **NOTE:** This cache uses strong and weak references for stored Bitmaps. Strong references - for limited count of
 * Bitmaps (depends on cache size), weak references - for all other cached Bitmaps.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @see BaseMemoryCache
 *
 * @since 1.0.0
 */
abstract class LimitedMemoryCache(protected val sizeLimit: Int) : BaseMemoryCache() {
    private val cacheSize: AtomicInteger

    /**
     * Contains strong references to stored objects. Each next object is added last. If hard cache size will exceed
     * limit then first object is deleted (but it continue exist at [.softMap] and can be collected by GC at any
     * time)
     */
    private val hardCache = Collections.synchronizedList(LinkedList<Bitmap>())

    /** @param sizeLimit Maximum size for cache (in bytes)
     */
    init {
        cacheSize = AtomicInteger()
        if (sizeLimit > MAX_NORMAL_CACHE_SIZE) {
            w("You set too large memory cache size (more than %1\$d Mb)", MAX_NORMAL_CACHE_SIZE_IN_MB)
        }
    }

    override fun put(key: String?, value: Bitmap?): Boolean {
        var putSuccessfully = false
        // Try to add value to hard cache
        val valueSize = getSize(value)
        val sizeLimit = sizeLimit
        var curCacheSize = cacheSize.get()
        if (valueSize < sizeLimit) {
            while (curCacheSize + valueSize > sizeLimit) {
                val removedValue = removeNext()
                if (hardCache.remove(removedValue)) {
                    curCacheSize = cacheSize.addAndGet(-getSize(removedValue))
                }
            }
            hardCache.add(value)
            cacheSize.addAndGet(valueSize)
            putSuccessfully = true
        }
        // Add value to soft cache
        super.put(key, value)
        return putSuccessfully
    }

    override fun remove(key: String?): Bitmap? {
        val value = super.get(key)
        if (value != null) {
            if (hardCache.remove(value)) {
                cacheSize.addAndGet(-getSize(value))
            }
        }
        return super.remove(key)
    }

    override fun clear() {
        hardCache.clear()
        cacheSize.set(0)
        super.clear()
    }

    protected abstract fun getSize(value: Bitmap?): Int
    protected abstract fun removeNext(): Bitmap

    companion object {
        private const val MAX_NORMAL_CACHE_SIZE_IN_MB = 16
        private const val MAX_NORMAL_CACHE_SIZE = MAX_NORMAL_CACHE_SIZE_IN_MB * 1024 * 1024
    }
}
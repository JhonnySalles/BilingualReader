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
package br.com.ebook.universalimageloader.cache.memory.impl

import android.graphics.Bitmap
import br.com.ebook.universalimageloader.cache.memory.MemoryCache

/**
 * Decorator for [MemoryCache]. Provides special feature for cache: some different keys are considered as
 * equals (using [comparator][Comparator]). And when you try to put some value into cache by key so entries with
 * "equals" keys will be removed from cache before.<br></br>
 * **NOTE:** Used for internal needs. Normally you don't need to use this class.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.0.0
 */
class FuzzyKeyMemoryCache(private val cache: MemoryCache, private val keyComparator: Comparator<String?>) : MemoryCache {
    override fun put(key: String?, value: Bitmap?): Boolean {
        // Search equal key and remove this entry
        synchronized(cache) {
            var keyToRemove: String? = null
            for (cacheKey in cache.keys()!!) {
                if (keyComparator.compare(key, cacheKey) == 0) {
                    keyToRemove = cacheKey
                    break
                }
            }
            if (keyToRemove != null) {
                cache.remove(keyToRemove)
            }
        }
        return cache.put(key, value)
    }

    override fun get(key: String?): Bitmap? {
        return cache[key]
    }

    override fun remove(key: String?): Bitmap? {
        return cache.remove(key)
    }

    override fun clear() {
        cache.clear()
    }

    override fun keys(): Collection<String> {
        return cache.keys()
    }
}
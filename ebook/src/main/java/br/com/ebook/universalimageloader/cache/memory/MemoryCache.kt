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
package br.com.ebook.universalimageloader.cache.memory

import android.graphics.Bitmap

/**
 * Interface for memory cache
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.9.2
 */
interface MemoryCache {
    /**
     * Puts value into cache by key
     *
     * @return **true** - if value was put into cache successfully, **false** - if value was **not** put into
     * cache
     */
    fun put(key: String?, value: Bitmap?): Boolean

    /** Returns value by key. If there is no value for key then null will be returned.  */
    operator fun get(key: String?): Bitmap?

    /** Removes item by key  */
    fun remove(key: String?): Bitmap?

    /** Returns all keys of cache  */
    fun keys(): Collection<String>

    /** Remove all items from cache  */
    fun clear()
}
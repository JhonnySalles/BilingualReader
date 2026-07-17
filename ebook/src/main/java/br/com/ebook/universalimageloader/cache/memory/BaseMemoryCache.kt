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
import java.lang.ref.Reference
import java.util.Collections

/**
 * Base memory cache. Implements common functionality for memory cache. Provides object references (
 * [not strong][Reference]) storing.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.0.0
 */
abstract class BaseMemoryCache : MemoryCache {
    /** Stores not strong references to objects  */
    private val softMap = Collections.synchronizedMap(HashMap<String, Reference<Bitmap>>())
    override fun get(key: String?): Bitmap? {
        var result: Bitmap? = null
        val reference = softMap[key]
        if (reference != null) {
            result = reference.get()
        }
        return result
    }

    override fun put(key: String?, value: Bitmap?): Boolean {
        softMap[key] = createReference(value)
        return true
    }

    override fun remove(key: String?): Bitmap? {
        val bmpRef = softMap.remove(key)
        return if (bmpRef == null) null else bmpRef.get()!!
    }

    override fun keys(): Collection<String> {
        synchronized(softMap) { return HashSet(softMap.keys) }
    }

    override fun clear() {
        softMap.clear()
    }

    /** Creates [not strong][Reference] reference of value  */
    protected abstract fun createReference(value: Bitmap?): Reference<Bitmap>
}
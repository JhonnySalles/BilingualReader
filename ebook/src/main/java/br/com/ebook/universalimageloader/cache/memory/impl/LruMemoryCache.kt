package br.com.ebook.universalimageloader.cache.memory.impl

import android.graphics.Bitmap
import br.com.ebook.universalimageloader.cache.memory.MemoryCache

/**
 * A cache that holds strong references to a limited number of Bitmaps. Each time a Bitmap is accessed, it is moved to
 * the head of a queue. When a Bitmap is added to a full cache, the Bitmap at the end of that queue is evicted and may
 * become eligible for garbage collection.<br></br>
 * <br></br>
 * **NOTE:** This cache uses only strong references for stored Bitmaps.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.8.1
 */
class LruMemoryCache(maxSize: Int) : MemoryCache {
    private val map: LinkedHashMap<String, Bitmap?>
    private val maxSize: Int

    /** Size of this cache in bytes  */
    private var size = 0

    /** @param maxSize Maximum sum of the sizes of the Bitmaps in this cache
     */
    init {
        require(maxSize > 0) { "maxSize <= 0" }
        this.maxSize = maxSize
        map = LinkedHashMap(0, 0.75f, true)
    }

    /**
     * Returns the Bitmap for `key` if it exists in the cache. If a Bitmap was returned, it is moved to the head
     * of the queue. This returns null if a Bitmap is not cached.
     */
    override fun get(key: String?): Bitmap? {
        if (key == null) {
            throw NullPointerException("key == null")
        }
        synchronized(this) { return map[key] }
    }

    /** Caches `Bitmap` for `key`. The Bitmap is moved to the head of the queue.  */
    override fun put(key: String?, value: Bitmap?): Boolean {
        if (key == null || value == null) {
            throw NullPointerException("key == null || value == null")
        }
        synchronized(this) {
            size += sizeOf(key, value)
            val previous = map.put(key, value)
            if (previous != null) {
                size -= sizeOf(key, previous)
            }
        }
        trimToSize(maxSize)
        return true
    }

    /**
     * Remove the eldest entries until the total of remaining entries is at or below the requested size.
     *
     * @param maxSize the maximum size of the cache before returning. May be -1 to evict even 0-sized elements.
     */
    private fun trimToSize(maxSize: Int) {
        var process = true
        while (true) {
            var key: String?
            var value: Bitmap?

            synchronized(this) {
                check(!(size < 0 || map.isEmpty() && size != 0)) { javaClass.name + ".sizeOf() is reporting inconsistent results!" }
                process = false
                if (size <= maxSize || map.isEmpty()) {
                    return@synchronized
                }
                val (key1, value1) = map.entries.iterator().next() ?: return@synchronized
                process = true
                key = key1
                value = value1
                map.remove(key)
                size -= sizeOf(key, value!!)
            }
        }
    }

    /** Removes the entry for `key` if it exists.  */
    override fun remove(key: String?): Bitmap? {
        if (key == null) {
            throw NullPointerException("key == null")
        }
        synchronized(this) {
            val previous = map.remove(key)
            if (previous != null) {
                size -= sizeOf(key, previous)
            }
            return previous
        }
    }

    override fun keys(): Collection<String> {
        synchronized(this) { return HashSet(map.keys) }
    }

    override fun clear() {
        trimToSize(-1) // -1 will evict 0-sized elements
    }

    /**
     * Returns the size `Bitmap` in bytes.
     *
     *
     * An entry's size must not change while it is in the cache.
     */
    private fun sizeOf(key: String?, value: Bitmap): Int {
        return value.rowBytes * value.height
    }

    @Synchronized
    override fun toString(): String {
        return String.format("LruCache[maxSize=%d]", maxSize)
    }
}
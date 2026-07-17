package br.com.ebook.universalimageloader.core.assist

import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

/**
 * Many streams obtained over slow connection show [this
 * problem](http://code.google.com/p/android/issues/detail?id=6066).
 */
class FlushedInputStream(inputStream: InputStream?) : FilterInputStream(inputStream) {
    @Throws(IOException::class)
    override fun skip(n: Long): Long {
        var totalBytesSkipped = 0L
        while (totalBytesSkipped < n) {
            var bytesSkipped = `in`.skip(n - totalBytesSkipped)
            if (bytesSkipped == 0L) {
                val by_te = read()
                bytesSkipped = if (by_te < 0) {
                    break // we reached EOF
                } else {
                    1 // we read one byte
                }
            }
            totalBytesSkipped += bytesSkipped
        }
        return totalBytesSkipped
    }
}
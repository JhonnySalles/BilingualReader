/*******************************************************************************
 * Copyright 2013-2014 Sergey Tarasevich
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

import java.io.IOException
import java.io.InputStream

/**
 * Decorator for [InputStream][java.io.InputStream]. Provides possibility to return defined stream length by
 * [.available] method.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com), Mariotaku
 * @since 1.9.1
 */
class ContentLengthInputStream(private val stream: InputStream, private val length: Int) : InputStream() {
    override fun available(): Int {
        return length
    }

    @Throws(IOException::class)
    override fun close() {
        stream.close()
    }

    override fun mark(readLimit: Int) {
        stream.mark(readLimit)
    }

    @Throws(IOException::class)
    override fun read(): Int {
        return stream.read()
    }

    @Throws(IOException::class)
    override fun read(buffer: ByteArray): Int {
        return stream.read(buffer)
    }

    @Throws(IOException::class)
    override fun read(buffer: ByteArray, byteOffset: Int, byteCount: Int): Int {
        return stream.read(buffer, byteOffset, byteCount)
    }

    @Throws(IOException::class)
    override fun reset() {
        stream.reset()
    }

    @Throws(IOException::class)
    override fun skip(byteCount: Long): Long {
        return stream.skip(byteCount)
    }

    override fun markSupported(): Boolean {
        return stream.markSupported()
    }
}
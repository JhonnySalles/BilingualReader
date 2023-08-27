/*******************************************************************************
 * Copyright 2011-2013 Sergey Tarasevich
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
package br.com.ebook.universalimageloader.core.download

import java.io.IOException
import java.io.InputStream

/**
 * Provides retrieving of [InputStream] of image by URI.<br></br>
 * Implementations have to be thread-safe.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.4.0
 */
interface ImageDownloader {
    /**
     * Retrieves [InputStream] of image by URI.
     *
     * @param imageUri Image URI
     * @param extra    Auxiliary object which was passed to [                 DisplayImageOptions.extraForDownloader(Object)][DisplayImageOptions.Builder.extraForDownloader]; can be null
     * @return [InputStream] of image
     * @throws IOException                   if some I/O error occurs during getting image stream
     * @throws UnsupportedOperationException if image URI has unsupported scheme(protocol)
     */
    @Throws(IOException::class)
    fun getStream(imageUri: String?, extra: Any?): InputStream?

    /** Represents supported schemes(protocols) of URI. Provides convenient methods for work with schemes and URIs.  */
    enum class Scheme(private val scheme: String) {
        HTTP("http"), HTTPS("https"), FILE("file"), CONTENT("content"), ASSETS("assets"), DRAWABLE("drawable"), UNKNOWN("");

        private val uriPrefix: String

        init {
            uriPrefix = "$scheme://"
        }

        private fun belongsTo(uri: String): Boolean {
            return uri.lowercase().startsWith(uriPrefix)
        }

        /** Appends scheme to incoming path  */
        fun wrap(path: String): String {
            return uriPrefix + path
        }

        /** Removed scheme part ("scheme://") from incoming URI  */
        fun crop(uri: String): String {
            require(belongsTo(uri)) { String.format("URI [%1\$s] doesn't have expected scheme [%2\$s]", uri, scheme) }
            return uri.substring(uriPrefix.length)
        }

        companion object {
            /**
             * Defines scheme of incoming URI
             *
             * @param uri URI for scheme detection
             * @return Scheme of incoming URI
             */
			@JvmStatic
			fun ofUri(uri: String?): Scheme {
                if (uri != null) {
                    for (s in values()) {
                        if (s.belongsTo(uri)) {
                            return s
                        }
                    }
                }
                return UNKNOWN
            }
        }
    }
}
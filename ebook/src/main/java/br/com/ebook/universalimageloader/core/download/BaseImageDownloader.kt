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
package br.com.ebook.universalimageloader.core.download

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import br.com.ebook.universalimageloader.core.assist.ContentLengthInputStream
import br.com.ebook.universalimageloader.core.download.ImageDownloader.Scheme.Companion.ofUri
import br.com.ebook.universalimageloader.utils.IoUtils.closeSilently
import br.com.ebook.universalimageloader.utils.IoUtils.readAndCloseStream
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Provides retrieving of [InputStream] of image by URI from network or file system or app resources.<br></br>
 * [URLConnection] is used to retrieve image stream from network.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.8.0
 */
class BaseImageDownloader @JvmOverloads constructor(
    context: Context,
    connectTimeout: Int = DEFAULT_HTTP_CONNECT_TIMEOUT,
    readTimeout: Int = DEFAULT_HTTP_READ_TIMEOUT
) : ImageDownloader {
    protected val context: Context
    protected val connectTimeout: Int
    protected val readTimeout: Int

    init {
        this.context = context.applicationContext
        this.connectTimeout = connectTimeout
        this.readTimeout = readTimeout
    }

    @Throws(IOException::class)
    override fun getStream(imageUri: String?, extra: Any?): InputStream? {
        return when (ofUri(imageUri)) {
            ImageDownloader.Scheme.HTTP, ImageDownloader.Scheme.HTTPS -> getStreamFromNetwork(
                imageUri,
                extra
            )

            ImageDownloader.Scheme.FILE -> getStreamFromFile(imageUri, extra)
            ImageDownloader.Scheme.CONTENT -> getStreamFromContent(imageUri, extra)
            ImageDownloader.Scheme.ASSETS -> getStreamFromAssets(imageUri, extra)
            ImageDownloader.Scheme.DRAWABLE -> getStreamFromDrawable(imageUri, extra)
            ImageDownloader.Scheme.UNKNOWN -> getStreamFromOtherSource(imageUri, extra)
            else -> getStreamFromOtherSource(imageUri, extra)
        }
    }

    /**
     * Retrieves [InputStream] of image by URI (image is located in the network).
     *
     * @param imageUri Image URI
     * @param extra    Auxiliary object which was passed to [                 DisplayImageOptions.extraForDownloader(Object)][DisplayImageOptions.Builder.extraForDownloader]; can be null
     * @return [InputStream] of image
     * @throws IOException if some I/O error occurs during network request or if no InputStream could be created for
     * URL.
     */
    @Throws(IOException::class)
    protected fun getStreamFromNetwork(imageUri: String?, extra: Any?): InputStream {
        var conn = createConnection(imageUri, extra)
        var redirectCount = 0
        while (conn.responseCode / 100 == 3 && redirectCount < MAX_REDIRECT_COUNT) {
            conn = createConnection(conn.getHeaderField("Location"), extra)
            redirectCount++
        }
        val imageStream: InputStream
        imageStream = try {
            conn.inputStream
        } catch (e: IOException) {
            // Read all data to allow reuse connection (http://bit.ly/1ad35PY)
            readAndCloseStream(conn.errorStream)
            throw e
        }
        if (!shouldBeProcessed(conn)) {
            closeSilently(imageStream)
            throw IOException("Image request failed with response code " + conn.responseCode)
        }
        return ContentLengthInputStream(BufferedInputStream(imageStream, BUFFER_SIZE), conn.contentLength)
    }

    /**
     * @param conn Opened request connection (response code is available)
     * @return **true** - if data from connection is correct and should be read and processed;
     * **false** - if response contains irrelevant data and shouldn't be processed
     * @throws IOException
     */
    @Throws(IOException::class)
    protected fun shouldBeProcessed(conn: HttpURLConnection): Boolean {
        return conn.responseCode == 200
    }

    /**
     * Create [HTTP connection][HttpURLConnection] for incoming URL
     *
     * @param url   URL to connect to
     * @param extra Auxiliary object which was passed to [              DisplayImageOptions.extraForDownloader(Object)][DisplayImageOptions.Builder.extraForDownloader]; can be null
     * @return [Connection][HttpURLConnection] for incoming URL. Connection isn't established so it still configurable.
     * @throws IOException if some I/O error occurs during network request or if no InputStream could be created for
     * URL.
     */
    @Throws(IOException::class)
    protected fun createConnection(url: String?, extra: Any?): HttpURLConnection {
        val encodedUrl = Uri.encode(url, ALLOWED_URI_CHARS)
        val conn = URL(encodedUrl).openConnection() as HttpURLConnection
        conn.connectTimeout = connectTimeout
        conn.readTimeout = readTimeout
        return conn
    }

    /**
     * Retrieves [InputStream] of image by URI (image is located on the local file system or SD card).
     *
     * @param imageUri Image URI
     * @param extra    Auxiliary object which was passed to [                 DisplayImageOptions.extraForDownloader(Object)][DisplayImageOptions.Builder.extraForDownloader]; can be null
     * @return [InputStream] of image
     * @throws IOException if some I/O error occurs reading from file system
     */
    @Throws(IOException::class)
    protected fun getStreamFromFile(imageUri: String?, extra: Any?): InputStream? {
        val filePath = ImageDownloader.Scheme.FILE.crop(imageUri!!)
        return if (isVideoFileUri(imageUri)) {
            getVideoThumbnailStream(filePath)
        } else {
            val imageStream = BufferedInputStream(FileInputStream(filePath), BUFFER_SIZE)
            ContentLengthInputStream(imageStream, File(filePath).length().toInt())
        }
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    private fun getVideoThumbnailStream(filePath: String): InputStream? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            val bitmap = ThumbnailUtils
                .createVideoThumbnail(filePath, MediaStore.Images.Thumbnails.FULL_SCREEN_KIND)
            if (bitmap != null) {
                val bos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos)
                return ByteArrayInputStream(bos.toByteArray())
            }
        }
        return null
    }

    /**
     * Retrieves [InputStream] of image by URI (image is accessed using [ContentResolver]).
     *
     * @param imageUri Image URI
     * @param extra    Auxiliary object which was passed to [                 DisplayImageOptions.extraForDownloader(Object)][DisplayImageOptions.Builder.extraForDownloader]; can be null
     * @return [InputStream] of image
     * @throws FileNotFoundException if the provided URI could not be opened
     */
    @Throws(FileNotFoundException::class)
    protected fun getStreamFromContent(imageUri: String?, extra: Any?): InputStream? {
        val res = context.contentResolver
        val uri = Uri.parse(imageUri)
        if (isVideoContentUri(uri)) { // video thumbnail
            val origId = java.lang.Long.valueOf(uri.lastPathSegment)
            val bitmap = MediaStore.Video.Thumbnails
                .getThumbnail(res, origId, MediaStore.Images.Thumbnails.MINI_KIND, null)
            if (bitmap != null) {
                val bos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos)
                return ByteArrayInputStream(bos.toByteArray())
            }
        } else if (imageUri!!.startsWith(CONTENT_CONTACTS_URI_PREFIX)) { // contacts photo
            return getContactPhotoStream(uri)
        }
        return res.openInputStream(uri)
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    protected fun getContactPhotoStream(uri: Uri?): InputStream {
        val res = context.contentResolver
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            ContactsContract.Contacts.openContactPhotoInputStream(res, uri, true)
        } else {
            ContactsContract.Contacts.openContactPhotoInputStream(res, uri)
        }
    }

    /**
     * Retrieves [InputStream] of image by URI (image is located in assets of application).
     *
     * @param imageUri Image URI
     * @param extra    Auxiliary object which was passed to [                 DisplayImageOptions.extraForDownloader(Object)][DisplayImageOptions.Builder.extraForDownloader]; can be null
     * @return [InputStream] of image
     * @throws IOException if some I/O error occurs file reading
     */
    @Throws(IOException::class)
    protected fun getStreamFromAssets(imageUri: String?, extra: Any?): InputStream {
        val filePath = ImageDownloader.Scheme.ASSETS.crop(imageUri!!)
        return context.assets.open(filePath)
    }

    /**
     * Retrieves [InputStream] of image by URI (image is located in drawable resources of application).
     *
     * @param imageUri Image URI
     * @param extra    Auxiliary object which was passed to [                 DisplayImageOptions.extraForDownloader(Object)][DisplayImageOptions.Builder.extraForDownloader]; can be null
     * @return [InputStream] of image
     */
    protected fun getStreamFromDrawable(imageUri: String?, extra: Any?): InputStream {
        val drawableIdString = ImageDownloader.Scheme.DRAWABLE.crop(imageUri!!)
        val drawableId = drawableIdString.toInt()
        return context.resources.openRawResource(drawableId)
    }

    /**
     * Retrieves [InputStream] of image by URI from other source with unsupported scheme. Should be overriden by
     * successors to implement image downloading from special sources.<br></br>
     * This method is called only if image URI has unsupported scheme. Throws [UnsupportedOperationException] by
     * default.
     *
     * @param imageUri Image URI
     * @param extra    Auxiliary object which was passed to [                 DisplayImageOptions.extraForDownloader(Object)][DisplayImageOptions.Builder.extraForDownloader]; can be null
     * @return [InputStream] of image
     * @throws IOException                   if some I/O error occurs
     * @throws UnsupportedOperationException if image URI has unsupported scheme(protocol)
     */
    @Throws(IOException::class)
    protected fun getStreamFromOtherSource(imageUri: String?, extra: Any?): InputStream {
        throw UnsupportedOperationException(String.format(ERROR_UNSUPPORTED_SCHEME, imageUri))
    }

    private fun isVideoContentUri(uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri)
        return mimeType != null && mimeType.startsWith("video/")
    }

    private fun isVideoFileUri(uri: String?): Boolean {
        val extension = MimeTypeMap.getFileExtensionFromUrl(uri)
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        return mimeType != null && mimeType.startsWith("video/")
    }

    companion object {
        /** {@value}  */
        const val DEFAULT_HTTP_CONNECT_TIMEOUT = 5 * 1000 // milliseconds

        /** {@value}  */
        const val DEFAULT_HTTP_READ_TIMEOUT = 20 * 1000 // milliseconds

        /** {@value}  */
        protected const val BUFFER_SIZE = 32 * 1024 // 32 Kb

        /** {@value}  */
        protected const val ALLOWED_URI_CHARS = "@#&=*+-_.,:!?()/~'%"
        protected const val MAX_REDIRECT_COUNT = 5
        protected const val CONTENT_CONTACTS_URI_PREFIX = "content://com.android.contacts/"
        private const val ERROR_UNSUPPORTED_SCHEME =
            "UIL doesn't support scheme(protocol) by default [%s]. " + "You should implement this support yourself (BaseImageDownloader.getStreamFromOtherSource(...))"
    }
}
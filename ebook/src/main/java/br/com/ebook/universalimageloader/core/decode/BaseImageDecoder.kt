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
package br.com.ebook.universalimageloader.core.decode

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import br.com.ebook.Config
import br.com.ebook.foobnix.sys.InputStreamBitmap
import br.com.ebook.universalimageloader.core.assist.ImageScaleType
import br.com.ebook.universalimageloader.core.assist.ImageSize
import br.com.ebook.universalimageloader.core.download.ImageDownloader
import br.com.ebook.universalimageloader.core.download.ImageDownloader.Scheme.Companion.ofUri
import br.com.ebook.universalimageloader.utils.ImageSizeUtils.computeImageSampleSize
import br.com.ebook.universalimageloader.utils.ImageSizeUtils.computeImageScale
import br.com.ebook.universalimageloader.utils.ImageSizeUtils.computeMinImageSampleSize
import br.com.ebook.universalimageloader.utils.IoUtils.closeSilently
import br.com.ebook.universalimageloader.utils.L.d
import br.com.ebook.universalimageloader.utils.L.e
import br.com.ebook.universalimageloader.utils.L.w
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream

/**
 * Decodes images to [Bitmap], scales them to needed size
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @see ImageDecodingInfo
 *
 * @since 1.8.3
 */
class BaseImageDecoder
/**
 * @param loggingEnabled
 * Whether debug logs will be written to LogCat. Usually should
 * match
 * [            ImageLoaderConfiguration.writeDebugLogs()][ImageLoaderConfiguration.Builder.writeDebugLogs]
 */(protected val loggingEnabled: Boolean) : ImageDecoder {

    private val mLOGGER = LoggerFactory.getLogger(BaseImageDecoder::class.java)

    /**
     * Decodes image from URI into [Bitmap]. Image is scaled close to
     * incoming [target size][ImageSize] during decoding (depend on
     * incoming parameters).
     *
     * @param decodingInfo
     * Needed data for decoding image
     * @return Decoded bitmap
     * @throws IOException
     * if some I/O exception occurs during image reading
     * @throws UnsupportedOperationException
     * if image URI has unsupported scheme(protocol)
     */
    @Throws(IOException::class)
    override fun decode(decodingInfo: ImageDecodingInfo?): Bitmap? {
        var decodedBitmap: Bitmap?
        val imageInfo: ImageFileInfo
        var imageStream = getImageStream(decodingInfo)
        if (imageStream == null) {
            e(ERROR_NO_IMAGE_STREAM, decodingInfo!!.imageKey)
            return null
        }
        if (imageStream is InputStreamBitmap) {
            decodedBitmap = imageStream.bitmap
            imageInfo = defineImageSizeAndRotation(imageStream, decodingInfo)
            decodedBitmap = considerExactScaleAndOrientatiton(decodedBitmap, decodingInfo, imageInfo.exif.rotation, imageInfo.exif.flipHorizontal)
            d("decode InputStreamBitmap", decodingInfo!!.imageKey)
            return decodedBitmap
        }
        try {
            imageInfo = defineImageSizeAndRotation(imageStream, decodingInfo)
            imageStream = resetStream(imageStream, decodingInfo)
            val decodingOptions = prepareDecodingOptions(imageInfo.imageSize, decodingInfo)
            decodedBitmap = BitmapFactory.decodeStream(imageStream, null, decodingOptions)
        } finally {
            closeSilently(imageStream)
        }
        if (decodedBitmap == null) {
            e(ERROR_CANT_DECODE_IMAGE, decodingInfo!!.imageKey)
        } else {
            decodedBitmap = considerExactScaleAndOrientatiton(decodedBitmap, decodingInfo, imageInfo.exif.rotation, imageInfo.exif.flipHorizontal)
        }
        return decodedBitmap
    }

    @Throws(IOException::class)
    protected fun getImageStream(decodingInfo: ImageDecodingInfo?): InputStream? {
        return decodingInfo!!.downloader.getStream(decodingInfo.imageUri, decodingInfo.extraForDownloader)
    }

    @Throws(IOException::class)
    protected fun defineImageSizeAndRotation(imageStream: InputStream?, decodingInfo: ImageDecodingInfo?): ImageFileInfo {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeStream(imageStream, null, options)
        val exif: ExifInfo
        val imageUri = decodingInfo!!.imageUri
        exif = if (decodingInfo.shouldConsiderExifParams() && canDefineExifParams(imageUri, options.outMimeType)) {
            defineExifOrientation(imageUri)
        } else {
            ExifInfo()
        }
        return ImageFileInfo(ImageSize(options.outWidth, options.outHeight, exif.rotation), exif)
    }

    private fun canDefineExifParams(imageUri: String, mimeType: String): Boolean {
        return "image/jpeg".equals(mimeType, ignoreCase = true) && ofUri(imageUri) === ImageDownloader.Scheme.FILE
    }

    protected fun defineExifOrientation(imageUri: String?): ExifInfo {
        var rotation = 0
        var flip = false
        try {
            val exif = ExifInterface(ImageDownloader.Scheme.FILE.crop(imageUri!!))
            val exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            when (exifOrientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                    flip = true
                    rotation = 0
                }

                ExifInterface.ORIENTATION_NORMAL -> rotation = 0
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    flip = true
                    rotation = 90
                }

                ExifInterface.ORIENTATION_ROTATE_90 -> rotation = 90
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                    flip = true
                    rotation = 180
                }

                ExifInterface.ORIENTATION_ROTATE_180 -> rotation = 180
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    flip = true
                    rotation = 270
                }

                ExifInterface.ORIENTATION_ROTATE_270 -> rotation = 270
            }
        } catch (e: IOException) {
            w("Can't read EXIF tags from file [%s]", imageUri)
        }
        return ExifInfo(rotation, flip)
    }

    protected fun prepareDecodingOptions(imageSize: ImageSize, decodingInfo: ImageDecodingInfo?): BitmapFactory.Options {
        val scaleType = decodingInfo!!.imageScaleType
        val scale: Int
        scale = if (scaleType === ImageScaleType.NONE) {
            1
        } else if (scaleType === ImageScaleType.NONE_SAFE) {
            computeMinImageSampleSize(imageSize)
        } else {
            val targetSize = decodingInfo.targetSize
            val powerOf2 = scaleType === ImageScaleType.IN_SAMPLE_POWER_OF_2
            computeImageSampleSize(imageSize, targetSize, decodingInfo.viewScaleType, powerOf2)
        }
        if (scale > 1 && loggingEnabled) {
            d(LOG_SUBSAMPLE_IMAGE, imageSize, imageSize.scaleDown(scale), scale, decodingInfo.imageKey)
        }
        val decodingOptions = decodingInfo.decodingOptions
        decodingOptions.inSampleSize = scale
        return decodingOptions
    }

    @Throws(IOException::class)
    protected fun resetStream(imageStream: InputStream, decodingInfo: ImageDecodingInfo?): InputStream? {
        if (Config.SHOW_LOG)
            mLOGGER.info("UIL resetStream")
        if (imageStream.markSupported()) {
            try {
                imageStream.reset()
                return imageStream
            } catch (ignored: IOException) {
            }
        }
        closeSilently(imageStream)
        return getImageStream(decodingInfo)
    }

    protected fun considerExactScaleAndOrientatiton(
        subsampledBitmap: Bitmap?,
        decodingInfo: ImageDecodingInfo?,
        rotation: Int,
        flipHorizontal: Boolean
    ): Bitmap {
        val m = Matrix()
        // Scale to exact size if need
        val scaleType = decodingInfo!!.imageScaleType
        if (scaleType === ImageScaleType.EXACTLY || scaleType === ImageScaleType.EXACTLY_STRETCHED) {
            val srcSize = ImageSize(subsampledBitmap!!.width, subsampledBitmap.height, rotation)
            val scale =
                computeImageScale(srcSize, decodingInfo.targetSize, decodingInfo.viewScaleType, scaleType === ImageScaleType.EXACTLY_STRETCHED)
            if (java.lang.Float.compare(scale, 1f) != 0) {
                m.setScale(scale, scale)
                if (loggingEnabled) {
                    d(LOG_SCALE_IMAGE, srcSize, srcSize.scale(scale), scale, decodingInfo.imageKey)
                }
            }
        }
        // Flip bitmap if need
        if (flipHorizontal) {
            m.postScale(-1f, 1f)
            if (loggingEnabled) d(LOG_FLIP_IMAGE, decodingInfo.imageKey)
        }
        // Rotate bitmap if need
        if (rotation != 0) {
            m.postRotate(rotation.toFloat())
            if (loggingEnabled) d(LOG_ROTATE_IMAGE, rotation, decodingInfo.imageKey)
        }
        val finalBitmap = Bitmap.createBitmap(subsampledBitmap!!, 0, 0, subsampledBitmap.width, subsampledBitmap.height, m, true)
        if (finalBitmap != subsampledBitmap) {
            subsampledBitmap.recycle()
        }
        return finalBitmap
    }

    protected class ExifInfo {
        val rotation: Int
        val flipHorizontal: Boolean

        constructor() {
            rotation = 0
            flipHorizontal = false
        }

        constructor(rotation: Int, flipHorizontal: Boolean) {
            this.rotation = rotation
            this.flipHorizontal = flipHorizontal
        }
    }

    protected class ImageFileInfo(val imageSize: ImageSize, val exif: ExifInfo)
    companion object {
        protected const val LOG_SUBSAMPLE_IMAGE = "Subsample original image (%1\$s) to %2\$s (scale = %3\$d) [%4\$s]"
        protected const val LOG_SCALE_IMAGE = "Scale subsampled image (%1\$s) to %2\$s (scale = %3$.5f) [%4\$s]"
        protected const val LOG_ROTATE_IMAGE = "Rotate image on %1\$d\u00B0 [%2\$s]"
        protected const val LOG_FLIP_IMAGE = "Flip image horizontally [%s]"
        protected const val ERROR_NO_IMAGE_STREAM = "No stream for image [%s]"
        protected const val ERROR_CANT_DECODE_IMAGE = "Image can't be decoded [%s]"
    }
}
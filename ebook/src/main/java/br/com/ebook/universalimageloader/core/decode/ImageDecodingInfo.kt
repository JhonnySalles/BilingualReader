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
package br.com.ebook.universalimageloader.core.decode

import android.annotation.TargetApi
import android.graphics.BitmapFactory
import android.os.Build
import br.com.ebook.universalimageloader.core.DisplayImageOptions
import br.com.ebook.universalimageloader.core.assist.ImageScaleType
import br.com.ebook.universalimageloader.core.assist.ImageSize
import br.com.ebook.universalimageloader.core.assist.ViewScaleType
import br.com.ebook.universalimageloader.core.download.ImageDownloader

/**
 * Contains needed information for decoding image to Bitmap
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.8.3
 */
class ImageDecodingInfo(
    /** @return Original [image key][MemoryCacheUtils.generateKey] (used in memory cache).
     */
    val imageKey: String,
    /** @return Image URI for decoding (usually image from disk cache)
     */
    val imageUri: String,
    /** @return The original image URI which was passed to ImageLoader
     */
    val originalImageUri: String,
    /**
     * @return Target size for image. Decoded bitmap should close to this size according to [ image scale type][ImageScaleType] and [view scale type][ViewScaleType].
     */
    val targetSize: ImageSize, viewScaleType: ViewScaleType,
    downloader: ImageDownloader, displayOptions: DisplayImageOptions
) {
    /**
     * @return [Scale type for image sampling and scaling][ImageScaleType]. This parameter affects result size
     * of decoded bitmap.
     */
    val imageScaleType: ImageScaleType

    /** @return [View scale type][ViewScaleType]. This parameter affects result size of decoded bitmap.
     */
    val viewScaleType: ViewScaleType

    /** @return Downloader for image loading
     */
    val downloader: ImageDownloader

    /** @return Auxiliary object for downloader
     */
    val extraForDownloader: Any?
    private val considerExifParams: Boolean

    /** @return Decoding options
     */
    val decodingOptions: BitmapFactory.Options

    init {
        imageScaleType = displayOptions.imageScaleType
        this.viewScaleType = viewScaleType
        this.downloader = downloader
        extraForDownloader = displayOptions.extraForDownloader
        considerExifParams = displayOptions.isConsiderExifParams
        decodingOptions = BitmapFactory.Options()
        copyOptions(displayOptions.decodingOptions, decodingOptions)
    }

    private fun copyOptions(srcOptions: BitmapFactory.Options, destOptions: BitmapFactory.Options) {
        destOptions.inDensity = srcOptions.inDensity
        destOptions.inDither = srcOptions.inDither
        destOptions.inInputShareable = srcOptions.inInputShareable
        destOptions.inJustDecodeBounds = srcOptions.inJustDecodeBounds
        destOptions.inPreferredConfig = srcOptions.inPreferredConfig
        destOptions.inPurgeable = srcOptions.inPurgeable
        destOptions.inSampleSize = srcOptions.inSampleSize
        destOptions.inScaled = srcOptions.inScaled
        destOptions.inScreenDensity = srcOptions.inScreenDensity
        destOptions.inTargetDensity = srcOptions.inTargetDensity
        destOptions.inTempStorage = srcOptions.inTempStorage
        if (Build.VERSION.SDK_INT >= 10) copyOptions10(srcOptions, destOptions)
        if (Build.VERSION.SDK_INT >= 11) copyOptions11(srcOptions, destOptions)
    }

    @TargetApi(10)
    private fun copyOptions10(srcOptions: BitmapFactory.Options, destOptions: BitmapFactory.Options) {
        destOptions.inPreferQualityOverSpeed = srcOptions.inPreferQualityOverSpeed
    }

    @TargetApi(11)
    private fun copyOptions11(srcOptions: BitmapFactory.Options, destOptions: BitmapFactory.Options) {
        destOptions.inBitmap = srcOptions.inBitmap
        destOptions.inMutable = srcOptions.inMutable
    }

    /** @return **true** - if EXIF params of image should be considered; **false** - otherwise
     */
    fun shouldConsiderExifParams(): Boolean {
        return considerExifParams
    }
}
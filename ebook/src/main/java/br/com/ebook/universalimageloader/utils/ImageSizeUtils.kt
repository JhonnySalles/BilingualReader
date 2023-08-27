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
package br.com.ebook.universalimageloader.utils

import android.opengl.GLES10
import br.com.ebook.universalimageloader.core.assist.ImageSize
import br.com.ebook.universalimageloader.core.assist.ViewScaleType
import br.com.ebook.universalimageloader.core.imageaware.ImageAware
import javax.microedition.khronos.opengles.GL10

/**
 * Provides calculations with image sizes, scales
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.8.3
 */
object ImageSizeUtils {
    private const val DEFAULT_MAX_BITMAP_DIMENSION = 2048
    private var maxBitmapSize: ImageSize? = null

    init {
        val maxTextureSize = IntArray(1)
        GLES10.glGetIntegerv(GL10.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0)
        val maxBitmapDimension = Math.max(maxTextureSize.get(0), DEFAULT_MAX_BITMAP_DIMENSION)
        maxBitmapSize = ImageSize(maxBitmapDimension, maxBitmapDimension)
    }

    /**
     * Defines target size for image aware view. Size is defined by target
     * [view][ImageAware] parameters, configuration
     * parameters or device display dimensions.<br></br>
     */
	@JvmStatic
	fun defineTargetSizeForView(imageAware: ImageAware, maxImageSize: ImageSize): ImageSize {
        var width = imageAware.width
        if (width <= 0) width = maxImageSize.width
        var height = imageAware.height
        if (height <= 0) height = maxImageSize.height
        return ImageSize(width, height)
    }

    /**
     * Computes sample size for downscaling image size (**srcSize**) to view size (**targetSize**). This sample
     * size is used during
     * [ decoding image][BitmapFactory.decodeStream] to bitmap.<br></br>
     * <br></br>
     * **Examples:**<br></br>
     *
     *
     * <pre>
     * srcSize(100x100), targetSize(10x10), powerOf2Scale = true -> sampleSize = 8
     * srcSize(100x100), targetSize(10x10), powerOf2Scale = false -> sampleSize = 10
     *
     * srcSize(100x100), targetSize(20x40), viewScaleType = FIT_INSIDE -> sampleSize = 5
     * srcSize(100x100), targetSize(20x40), viewScaleType = CROP       -> sampleSize = 2
    </pre> *
     *
     *
     * <br></br>
     * The sample size is the number of pixels in either dimension that correspond to a single pixel in the decoded
     * bitmap. For example, inSampleSize == 4 returns an image that is 1/4 the width/height of the original, and 1/16
     * the number of pixels. Any value <= 1 is treated the same as 1.
     *
     * @param srcSize       Original (image) size
     * @param targetSize    Target (view) size
     * @param viewScaleType [Scale type][ViewScaleType] for placing image in view
     * @param powerOf2Scale *true* - if sample size be a power of 2 (1, 2, 4, 8, ...)
     * @return Computed sample size
     */
	@JvmStatic
	fun computeImageSampleSize(
        srcSize: ImageSize, targetSize: ImageSize, viewScaleType: ViewScaleType?,
        powerOf2Scale: Boolean
    ): Int {
        val srcWidth = srcSize.width
        val srcHeight = srcSize.height
        val targetWidth = targetSize.width
        val targetHeight = targetSize.height
        var scale = 1
        when (viewScaleType) {
            ViewScaleType.FIT_INSIDE -> if (powerOf2Scale) {
                val halfWidth = srcWidth / 2
                val halfHeight = srcHeight / 2
                while (halfWidth / scale > targetWidth || halfHeight / scale > targetHeight) { // ||
                    scale *= 2
                }
            } else {
                scale = Math.max(srcWidth / targetWidth, srcHeight / targetHeight) // max
            }

            ViewScaleType.CROP -> if (powerOf2Scale) {
                val halfWidth = srcWidth / 2
                val halfHeight = srcHeight / 2
                while (halfWidth / scale > targetWidth && halfHeight / scale > targetHeight) { // &&
                    scale *= 2
                }
            } else {
                scale = Math.min(srcWidth / targetWidth, srcHeight / targetHeight) // min
            }

            else -> {}
        }
        if (scale < 1) {
            scale = 1
        }
        scale = considerMaxTextureSize(srcWidth, srcHeight, scale, powerOf2Scale)
        return scale
    }

    private fun considerMaxTextureSize(srcWidth: Int, srcHeight: Int, scale: Int, powerOf2: Boolean): Int {
        var scale = scale
        val maxWidth = maxBitmapSize!!.width
        val maxHeight = maxBitmapSize!!.height
        while (srcWidth / scale > maxWidth || srcHeight / scale > maxHeight) {
            if (powerOf2) {
                scale *= 2
            } else {
                scale++
            }
        }
        return scale
    }

    /**
     * Computes minimal sample size for downscaling image so result image size won't exceed max acceptable OpenGL
     * texture size.<br></br>
     * We can't create Bitmap in memory with size exceed max texture size (usually this is 2048x2048) so this method
     * calculate minimal sample size which should be applied to image to fit into these limits.
     *
     * @param srcSize Original image size
     * @return Minimal sample size
     */
	@JvmStatic
	fun computeMinImageSampleSize(srcSize: ImageSize): Int {
        val srcWidth = srcSize.width
        val srcHeight = srcSize.height
        val targetWidth = maxBitmapSize!!.width
        val targetHeight = maxBitmapSize!!.height
        val widthScale = Math.ceil((srcWidth.toFloat() / targetWidth).toDouble()).toInt()
        val heightScale = Math.ceil((srcHeight.toFloat() / targetHeight).toDouble()).toInt()
        return Math.max(widthScale, heightScale) // max
    }

    /**
     * Computes scale of target size (**targetSize**) to source size (**srcSize**).<br></br>
     * <br></br>
     * **Examples:**<br></br>
     *
     *
     * <pre>
     * srcSize(40x40), targetSize(10x10) -> scale = 0.25
     *
     * srcSize(10x10), targetSize(20x20), stretch = false -> scale = 1
     * srcSize(10x10), targetSize(20x20), stretch = true  -> scale = 2
     *
     * srcSize(100x100), targetSize(20x40), viewScaleType = FIT_INSIDE -> scale = 0.2
     * srcSize(100x100), targetSize(20x40), viewScaleType = CROP       -> scale = 0.4
    </pre> *
     *
     * @param srcSize       Source (image) size
     * @param targetSize    Target (view) size
     * @param viewScaleType [Scale type][ViewScaleType] for placing image in view
     * @param stretch       Whether source size should be stretched if target size is larger than source size. If **false**
     * then result scale value can't be greater than 1.
     * @return Computed scale
     */
	@JvmStatic
	fun computeImageScale(
        srcSize: ImageSize, targetSize: ImageSize, viewScaleType: ViewScaleType,
        stretch: Boolean
    ): Float {
        val srcWidth = srcSize.width
        val srcHeight = srcSize.height
        val targetWidth = targetSize.width
        val targetHeight = targetSize.height
        val widthScale = srcWidth.toFloat() / targetWidth
        val heightScale = srcHeight.toFloat() / targetHeight
        val destWidth: Int
        val destHeight: Int
        if ((viewScaleType == ViewScaleType.FIT_INSIDE && widthScale >= heightScale || viewScaleType == ViewScaleType.CROP) && widthScale < heightScale) {
            destWidth = targetWidth
            destHeight = (srcHeight / widthScale).toInt()
        } else {
            destWidth = (srcWidth / heightScale).toInt()
            destHeight = targetHeight
        }
        var scale = 1f
        if (!stretch && destWidth < srcWidth && destHeight < srcHeight || stretch && destWidth != srcWidth && destHeight != srcHeight) {
            scale = destWidth.toFloat() / srcWidth
        }
        return scale
    }
}
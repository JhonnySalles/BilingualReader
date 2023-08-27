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
package br.com.ebook.universalimageloader.core

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Handler
import br.com.ebook.universalimageloader.core.DefaultConfigurationFactory.createBitmapDisplayer
import br.com.ebook.universalimageloader.core.DisplayImageOptions.Builder
import br.com.ebook.universalimageloader.core.assist.ImageScaleType
import br.com.ebook.universalimageloader.core.display.BitmapDisplayer
import br.com.ebook.universalimageloader.core.process.BitmapProcessor

/**
 * Contains options for image display. Defines:
 *
 *  * whether stub image will be displayed in [ image aware view][ImageAware] during image loading
 *  * whether stub image will be displayed in [ image aware view][ImageAware] if empty URI is passed
 *  * whether stub image will be displayed in [ image aware view][ImageAware] if image loading fails
 *  * whether [image aware view][ImageAware] should be reset
 * before image loading start
 *  * whether loaded image will be cached in memory
 *  * whether loaded image will be cached on disk
 *  * image scale type
 *  * decoding options (including bitmap decoding configuration)
 *  * delay before loading of image
 *  * whether consider EXIF parameters of image
 *  * auxiliary object which will be passed to [ImageDownloader][ImageDownloader.getStream]
 *  * pre-processor for image Bitmap (before caching in memory)
 *  * post-processor for image Bitmap (after caching in memory, before displaying)
 *  * how decoded [Bitmap] will be displayed
 *
 *
 *
 * You can create instance:
 *
 *  * with [Builder]:<br></br>
 * **i.e.** :
 * `new [DisplayImageOptions].Builder().[cacheInMemory()][Builder.cacheInMemory].
 * [showImageOnLoading()][Builder.showImageOnLoading].[build()][Builder.build]`<br></br>
 *
 *  * or by static method: [.createSimple] <br></br>
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.0.0
 */
class DisplayImageOptions private constructor(builder: Builder) {
    private val imageResOnLoading: Int
    private val imageResForEmptyUri: Int
    private val imageResOnFail: Int
    private val imageOnLoading: Drawable?
    private val imageForEmptyUri: Drawable?
    private val imageOnFail: Drawable?
    val isResetViewBeforeLoading: Boolean
    val isCacheInMemory: Boolean
    val isCacheOnDisk: Boolean
    val imageScaleType: ImageScaleType
    val decodingOptions: BitmapFactory.Options
    val delayBeforeLoading: Int
    val isConsiderExifParams: Boolean
    val extraForDownloader: Any?
    val preProcessor: BitmapProcessor?
    val postProcessor: BitmapProcessor?
    val displayer: BitmapDisplayer
    val handler: Handler?
    val isSyncLoading: Boolean

    init {
        imageResOnLoading = builder.imageResOnLoading
        imageResForEmptyUri = builder.imageResForEmptyUri
        imageResOnFail = builder.imageResOnFail
        imageOnLoading = builder.imageOnLoading
        imageForEmptyUri = builder.imageForEmptyUri
        imageOnFail = builder.imageOnFail
        isResetViewBeforeLoading = builder.resetViewBeforeLoading
        isCacheInMemory = builder.cacheInMemory
        isCacheOnDisk = builder.cacheOnDisk
        imageScaleType = builder.imageScaleType
        decodingOptions = builder.decodingOptions
        delayBeforeLoading = builder.delayBeforeLoading
        isConsiderExifParams = builder.considerExifParams
        extraForDownloader = builder.extraForDownloader
        preProcessor = builder.preProcessor
        postProcessor = builder.postProcessor
        displayer = builder.displayer
        handler = builder.handler
        isSyncLoading = builder.isSyncLoading
    }

    fun shouldShowImageOnLoading(): Boolean {
        return imageOnLoading != null || imageResOnLoading != 0
    }

    fun shouldShowImageForEmptyUri(): Boolean {
        return imageForEmptyUri != null || imageResForEmptyUri != 0
    }

    fun shouldShowImageOnFail(): Boolean {
        return imageOnFail != null || imageResOnFail != 0
    }

    fun shouldPreProcess(): Boolean {
        return preProcessor != null
    }

    fun shouldPostProcess(): Boolean {
        return postProcessor != null
    }

    fun shouldDelayBeforeLoading(): Boolean {
        return delayBeforeLoading > 0
    }

    fun getImageOnLoading(res: Resources): Drawable {
        return if (imageResOnLoading != 0) res.getDrawable(imageResOnLoading) else imageOnLoading!!
    }

    fun getImageForEmptyUri(res: Resources): Drawable {
        return if (imageResForEmptyUri != 0) res.getDrawable(imageResForEmptyUri) else imageForEmptyUri!!
    }

    fun getImageOnFail(res: Resources): Drawable {
        return if (imageResOnFail != 0) res.getDrawable(imageResOnFail) else imageOnFail!!
    }

    /**
     * Builder for [DisplayImageOptions]
     *
     * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
     */
    class Builder {
        var imageResOnLoading = 0
        var imageResForEmptyUri = 0
        var imageResOnFail = 0
        var imageOnLoading: Drawable? = null
        var imageForEmptyUri: Drawable? = null
        var imageOnFail: Drawable? = null
        var resetViewBeforeLoading = false
        var cacheInMemory = false
        var cacheOnDisk = false
        var imageScaleType = ImageScaleType.IN_SAMPLE_POWER_OF_2
        var decodingOptions = BitmapFactory.Options()
        var delayBeforeLoading = 0
        var considerExifParams = false
        var extraForDownloader: Any? = null
        var preProcessor: BitmapProcessor? = null
        var postProcessor: BitmapProcessor? = null
        var displayer = createBitmapDisplayer()
        var handler: Handler? = null
        var isSyncLoading = false

        /**
         * Stub image will be displayed in [ image aware view][ImageAware] during image loading
         *
         * @param imageRes Stub image resource
         */
        @Deprecated("Use {@link #showImageOnLoading(int)} instead")
        fun showStubImage(imageRes: Int): Builder {
            imageResOnLoading = imageRes
            return this
        }

        /**
         * Incoming image will be displayed in [ image aware view][ImageAware] during image loading
         *
         * @param imageRes Image resource
         */
        fun showImageOnLoading(imageRes: Int): Builder {
            imageResOnLoading = imageRes
            return this
        }

        /**
         * Incoming drawable will be displayed in [ image aware view][ImageAware] during image loading.
         * This option will be ignored if [DisplayImageOptions.Builder.showImageOnLoading] is set.
         */
        fun showImageOnLoading(drawable: Drawable?): Builder {
            imageOnLoading = drawable
            return this
        }

        /**
         * Incoming image will be displayed in [ image aware view][ImageAware] if empty URI (null or empty
         * string) will be passed to **ImageLoader.displayImage(...)** method.
         *
         * @param imageRes Image resource
         */
        fun showImageForEmptyUri(imageRes: Int): Builder {
            imageResForEmptyUri = imageRes
            return this
        }

        /**
         * Incoming drawable will be displayed in [ image aware view][ImageAware] if empty URI (null or empty
         * string) will be passed to **ImageLoader.displayImage(...)** method.
         * This option will be ignored if [DisplayImageOptions.Builder.showImageForEmptyUri] is set.
         */
        fun showImageForEmptyUri(drawable: Drawable?): Builder {
            imageForEmptyUri = drawable
            return this
        }

        /**
         * Incoming image will be displayed in [ image aware view][ImageAware] if some error occurs during
         * requested image loading/decoding.
         *
         * @param imageRes Image resource
         */
        fun showImageOnFail(imageRes: Int): Builder {
            imageResOnFail = imageRes
            return this
        }

        /**
         * Incoming drawable will be displayed in [ image aware view][ImageAware] if some error occurs during
         * requested image loading/decoding.
         * This option will be ignored if [DisplayImageOptions.Builder.showImageOnFail] is set.
         */
        fun showImageOnFail(drawable: Drawable?): Builder {
            imageOnFail = drawable
            return this
        }

        /**
         * [ image aware view][ImageAware] will be reset (set **null**) before image loading start
         *
         */
        @Deprecated("Use {@link #resetViewBeforeLoading(boolean) resetViewBeforeLoading(true)} instead")
        fun resetViewBeforeLoading(): Builder {
            resetViewBeforeLoading = true
            return this
        }

        /**
         * Sets whether [ image aware view][ImageAware] will be reset (set **null**) before image loading start
         */
        fun resetViewBeforeLoading(resetViewBeforeLoading: Boolean): Builder {
            this.resetViewBeforeLoading = resetViewBeforeLoading
            return this
        }

        /**
         * Loaded image will be cached in memory
         *
         */
        @Deprecated("Use {@link #cacheInMemory(boolean) cacheInMemory(true)} instead")
        fun cacheInMemory(): Builder {
            cacheInMemory = true
            return this
        }

        /** Sets whether loaded image will be cached in memory  */
        fun cacheInMemory(cacheInMemory: Boolean): Builder {
            this.cacheInMemory = cacheInMemory
            return this
        }

        /**
         * Loaded image will be cached on disk
         *
         */
        @Deprecated("Use {@link #cacheOnDisk(boolean) cacheOnDisk(true)} instead")
        fun cacheOnDisc(): Builder {
            return cacheOnDisk(true)
        }

        /**
         * Sets whether loaded image will be cached on disk
         *
         */
        @Deprecated("Use {@link #cacheOnDisk(boolean)} instead")
        fun cacheOnDisc(cacheOnDisk: Boolean): Builder {
            return cacheOnDisk(cacheOnDisk)
        }

        /** Sets whether loaded image will be cached on disk  */
        fun cacheOnDisk(cacheOnDisk: Boolean): Builder {
            this.cacheOnDisk = cacheOnDisk
            return this
        }

        /**
         * Sets [scale type][ImageScaleType] for decoding image. This parameter is used while define scale
         * size for decoding image to Bitmap. Default value - [ImageScaleType.IN_SAMPLE_POWER_OF_2]
         */
        fun imageScaleType(imageScaleType: ImageScaleType): Builder {
            this.imageScaleType = imageScaleType
            return this
        }

        /** Sets [bitmap config][Bitmap.Config] for image decoding. Default value - [Bitmap.Config.ARGB_8888]  */
        fun bitmapConfig(bitmapConfig: Bitmap.Config?): Builder {
            requireNotNull(bitmapConfig) { "bitmapConfig can't be null" }
            decodingOptions.inPreferredConfig = bitmapConfig
            return this
        }

        /**
         * Sets options for image decoding.<br></br>
         * **NOTE:** [Options.inSampleSize] of incoming options will **NOT** be considered. Library
         * calculate the most appropriate sample size itself according yo [.imageScaleType]
         * options.<br></br>
         * **NOTE:** This option overlaps [bitmapConfig()][.bitmapConfig]
         * option.
         */
        fun decodingOptions(decodingOptions: BitmapFactory.Options?): Builder {
            requireNotNull(decodingOptions) { "decodingOptions can't be null" }
            this.decodingOptions = decodingOptions
            return this
        }

        /** Sets delay time before starting loading task. Default - no delay.  */
        fun delayBeforeLoading(delayInMillis: Int): Builder {
            delayBeforeLoading = delayInMillis
            return this
        }

        /** Sets auxiliary object which will be passed to [ImageDownloader.getStream]  */
        fun extraForDownloader(extra: Any?): Builder {
            extraForDownloader = extra
            return this
        }

        /** Sets whether ImageLoader will consider EXIF parameters of JPEG image (rotate, flip)  */
        fun considerExifParams(considerExifParams: Boolean): Builder {
            this.considerExifParams = considerExifParams
            return this
        }

        /**
         * Sets bitmap processor which will be process bitmaps before they will be cached in memory. So memory cache
         * will contain bitmap processed by incoming preProcessor.<br></br>
         * Image will be pre-processed even if caching in memory is disabled.
         */
        fun preProcessor(preProcessor: BitmapProcessor?): Builder {
            this.preProcessor = preProcessor
            return this
        }

        /**
         * Sets bitmap processor which will be process bitmaps before they will be displayed in
         * [image aware view][ImageAware] but
         * after they'll have been saved in memory cache.
         */
        fun postProcessor(postProcessor: BitmapProcessor?): Builder {
            this.postProcessor = postProcessor
            return this
        }

        /**
         * Sets custom [displayer][BitmapDisplayer] for image loading task. Default value -
         * [DefaultConfigurationFactory.createBitmapDisplayer]
         */
        fun displayer(displayer: BitmapDisplayer?): Builder {
            requireNotNull(displayer) { "displayer can't be null" }
            this.displayer = displayer
            return this
        }

        fun syncLoading(isSyncLoading: Boolean): Builder {
            this.isSyncLoading = isSyncLoading
            return this
        }

        /**
         * Sets custom [handler][Handler] for displaying images and firing [ listener][ImageLoadingListener] events.
         */
        fun handler(handler: Handler?): Builder {
            this.handler = handler
            return this
        }

        /** Sets all options equal to incoming options  */
        fun cloneFrom(options: DisplayImageOptions): Builder {
            imageResOnLoading = options.imageResOnLoading
            imageResForEmptyUri = options.imageResForEmptyUri
            imageResOnFail = options.imageResOnFail
            imageOnLoading = options.imageOnLoading
            imageForEmptyUri = options.imageForEmptyUri
            imageOnFail = options.imageOnFail
            resetViewBeforeLoading = options.isResetViewBeforeLoading
            cacheInMemory = options.isCacheInMemory
            cacheOnDisk = options.isCacheOnDisk
            imageScaleType = options.imageScaleType
            decodingOptions = options.decodingOptions
            delayBeforeLoading = options.delayBeforeLoading
            considerExifParams = options.isConsiderExifParams
            extraForDownloader = options.extraForDownloader
            preProcessor = options.preProcessor
            postProcessor = options.postProcessor
            displayer = options.displayer
            handler = options.handler
            isSyncLoading = options.isSyncLoading
            return this
        }

        /** Builds configured [DisplayImageOptions] object  */
        fun build(): DisplayImageOptions {
            return DisplayImageOptions(this)
        }
    }

    companion object {
        /**
         * Creates options appropriate for single displaying:
         *
         *  * View will **not** be reset before loading
         *  * Loaded image will **not** be cached in memory
         *  * Loaded image will **not** be cached on disk
         *  * [ImageScaleType.IN_SAMPLE_POWER_OF_2] decoding type will be used
         *  * [Bitmap.Config.ARGB_8888] bitmap config will be used for image decoding
         *  * [SimpleBitmapDisplayer] will be used for image displaying
         *
         *
         *
         * These option are appropriate for simple single-use image (from drawables or from Internet) displaying.
         */
		@JvmStatic
		fun createSimple(): DisplayImageOptions {
            return Builder().build()
        }
    }
}
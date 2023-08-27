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

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import br.com.ebook.universalimageloader.cache.disc.DiskCache
import br.com.ebook.universalimageloader.cache.memory.MemoryCache
import br.com.ebook.universalimageloader.core.assist.ImageSize
import br.com.ebook.universalimageloader.core.assist.LoadedFrom
import br.com.ebook.universalimageloader.core.assist.ViewScaleType
import br.com.ebook.universalimageloader.core.imageaware.ImageAware
import br.com.ebook.universalimageloader.core.imageaware.ImageViewAware
import br.com.ebook.universalimageloader.core.imageaware.NonViewAware
import br.com.ebook.universalimageloader.core.listener.ImageLoadingListener
import br.com.ebook.universalimageloader.core.listener.ImageLoadingProgressListener
import br.com.ebook.universalimageloader.core.listener.SimpleImageLoadingListener
import br.com.ebook.universalimageloader.utils.ImageSizeUtils.defineTargetSizeForView
import br.com.ebook.universalimageloader.utils.L.d
import br.com.ebook.universalimageloader.utils.L.w
import br.com.ebook.universalimageloader.utils.MemoryCacheUtils.generateKey

/**
 * Singletone for image loading and displaying at [ ImageViews][ImageView]<br></br>
 * **NOTE:** [.init] method must be called
 * before any other method.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.0.0
 */
class ImageLoader protected constructor() {
    private var configuration: ImageLoaderConfiguration? = null
    private var engine: ImageLoaderEngine? = null
    private var defaultListener: ImageLoadingListener = SimpleImageLoadingListener()

    /**
     * Initializes ImageLoader instance with configuration.<br></br>
     * If configurations was set before ( [.isInited] == true) then this
     * method does nothing.<br></br>
     * To force initialization with new configuration you should
     * [destroy ImageLoader][.destroy] at first.
     *
     * @param configuration
     * [ImageLoader][ImageLoaderConfiguration]
     * @throws IllegalArgumentException
     * if **configuration** parameter is null
     */
    @Synchronized
    fun init(configuration: ImageLoaderConfiguration?) {
        requireNotNull(configuration) { ERROR_INIT_CONFIG_WITH_NULL }
        if (this.configuration == null) {
            d(LOG_INIT_CONFIG)
            engine = ImageLoaderEngine(configuration)
            this.configuration = configuration
        } else {
            w(WARNING_RE_INIT_CONFIG)
        }
    }

    /**
     * Returns **true** - if ImageLoader
     * [is initialized with][.init]; **false** - otherwise
     */
    val isInited: Boolean
        get() = configuration != null

    /**
     * Adds display image task to execution pool. Image will be set to
     * ImageAware when it's turn.<br></br>
     * Default [display image options][DisplayImageOptions] from
     * [configuration][ImageLoaderConfiguration] will be used.<br></br>
     * **NOTE:** [.init] method must be
     * called before this method call
     *
     * @param uri
     * Image URI (i.e. "http://site.com/image.png",
     * "file:///mnt/sdcard/image.png")
     * @param imageAware
     * [            Image aware view][ImageAware] which should display image
     * @param listener
     * [Listener][ImageLoadingListener] for image loading
     * process. Listener fires events on UI thread if this method is
     * called on UI thread.
     * @throws IllegalStateException
     * if [.init] method wasn't
     * called before
     * @throws IllegalArgumentException
     * if passed **imageAware** is null
     */
    fun displayImage(uri: String?, imageAware: ImageAware?, listener: ImageLoadingListener?) {
        displayImage(uri, imageAware, null, listener, null)
    }
    /**
     * Adds display image task to execution pool. Image will be set to
     * ImageAware when it's turn.<br></br>
     * **NOTE:** [.init] method must be
     * called before this method call
     *
     * @param uri
     * Image URI (i.e. "http://site.com/image.png",
     * "file:///mnt/sdcard/image.png")
     * @param imageAware
     * [            Image aware view][ImageAware] which should display image
     * @param options
     * [            Options][DisplayImageOptions] for image decoding and displaying. If **null** -
     * default display image options
     * [            from configuration][ImageLoaderConfiguration.Builder.defaultDisplayImageOptions] will be used.
     * @param listener
     * [Listener][ImageLoadingListener] for image loading
     * process. Listener fires events on UI thread if this method is
     * called on UI thread.
     * @param progressListener
     * [            Listener][ImageLoadingProgressListener] for image loading progress. Listener fires events on
     * UI thread if this method is called on UI thread. Caching on
     * disk should be enabled in
     * [            options][DisplayImageOptions] to make this listener work.
     * @throws IllegalStateException
     * if [.init] method wasn't
     * called before
     * @throws IllegalArgumentException
     * if passed **imageAware** is null
     */
    /**
     * Adds display image task to execution pool. Image will be set to
     * ImageAware when it's turn. <br></br>
     * Default [display image options][DisplayImageOptions] from
     * [configuration][ImageLoaderConfiguration] will be used.<br></br>
     * **NOTE:** [.init] method must be
     * called before this method call
     *
     * @param uri
     * Image URI (i.e. "http://site.com/image.png",
     * "file:///mnt/sdcard/image.png")
     * @param imageAware
     * [            Image aware view][ImageAware] which should display image
     * @throws IllegalStateException
     * if [.init] method wasn't
     * called before
     * @throws IllegalArgumentException
     * if passed **imageAware** is null
     */
    /**
     * Adds display image task to execution pool. Image will be set to
     * ImageAware when it's turn.<br></br>
     * **NOTE:** [.init] method must be
     * called before this method call
     *
     * @param uri
     * Image URI (i.e. "http://site.com/image.png",
     * "file:///mnt/sdcard/image.png")
     * @param imageAware
     * [            Image aware view][ImageAware] which should display image
     * @param options
     * [            Options][DisplayImageOptions] for image decoding and displaying. If **null** -
     * default display image options
     * [            from configuration][ImageLoaderConfiguration.Builder.defaultDisplayImageOptions] will be used.
     * @throws IllegalStateException
     * if [.init] method wasn't
     * called before
     * @throws IllegalArgumentException
     * if passed **imageAware** is null
     */
    /**
     * Adds display image task to execution pool. Image will be set to
     * ImageAware when it's turn.<br></br>
     * **NOTE:** [.init] method must be
     * called before this method call
     *
     * @param uri
     * Image URI (i.e. "http://site.com/image.png",
     * "file:///mnt/sdcard/image.png")
     * @param imageAware
     * [            Image aware view][ImageAware] which should display image
     * @param options
     * [            Options][DisplayImageOptions] for image decoding and displaying. If **null** -
     * default display image options
     * [            from configuration][ImageLoaderConfiguration.Builder.defaultDisplayImageOptions] will be used.
     * @param listener
     * [Listener][ImageLoadingListener] for image loading
     * process. Listener fires events on UI thread if this method is
     * called on UI thread.
     * @throws IllegalStateException
     * if [.init] method wasn't
     * called before
     * @throws IllegalArgumentException
     * if passed **imageAware** is null
     */
    @JvmOverloads
    fun displayImage(
        uri: String?,
        imageAware: ImageAware?,
        options: DisplayImageOptions? = null,
        listener: ImageLoadingListener? = null,
        progressListener: ImageLoadingProgressListener? = null
    ) {
        displayImage(uri, imageAware, options, null, listener, progressListener)
    }

    /**
     * Adds display image task to execution pool. Image will be set to
     * ImageAware when it's turn.<br></br>
     * **NOTE:** [.init] method must be
     * called before this method call
     *
     * @param uri
     * Image URI (i.e. "http://site.com/image.png",
     * "file:///mnt/sdcard/image.png")
     * @param imageAware
     * [            Image aware view][ImageAware] which should display image
     * @param options
     * [            Options][DisplayImageOptions] for image decoding and displaying. If **null** -
     * default display image options
     * [            from configuration][ImageLoaderConfiguration.Builder.defaultDisplayImageOptions] will be used.
     * @param targetSize
     * [ImageSize] Image target size. If **null** -
     * size will depend on the view
     * @param listener
     * [Listener][ImageLoadingListener] for image loading
     * process. Listener fires events on UI thread if this method is
     * called on UI thread.
     * @param progressListener
     * [            Listener][ImageLoadingProgressListener] for image loading progress. Listener fires events on
     * UI thread if this method is called on UI thread. Caching on
     * disk should be enabled in
     * [            options][DisplayImageOptions] to make this listener work.
     * @throws IllegalStateException
     * if [.init] method wasn't
     * called before
     * @throws IllegalArgumentException
     * if passed **imageAware** is null
     */
    fun displayImage(
        uri: String?,
        imageAware: ImageAware?,
        options: DisplayImageOptions?,
        targetSize: ImageSize?,
        listener: ImageLoadingListener?,
        progressListener: ImageLoadingProgressListener?
    ) {
        var options = options
        var targetSize = targetSize
        var listener = listener
        checkConfiguration()
        requireNotNull(imageAware) { ERROR_WRONG_ARGUMENTS }
        if (listener == null) {
            listener = defaultListener
        }
        if (options == null) {
            options = configuration!!.defaultDisplayImageOptions
        }
        if (TextUtils.isEmpty(uri)) {
            engine!!.cancelDisplayTaskFor(imageAware)
            listener.onLoadingStarted(uri, imageAware.wrappedView)
            if (options!!.shouldShowImageForEmptyUri()) {
                imageAware.setImageDrawable(options.getImageForEmptyUri(configuration!!.resources))
            } else {
                imageAware.setImageDrawable(null)
            }
            listener.onLoadingComplete(uri, imageAware.wrappedView, null)
            return
        }
        if (targetSize == null) {
            targetSize = defineTargetSizeForView(imageAware, configuration!!.maxImageSize)
        }
        val memoryCacheKey = generateKey(uri, targetSize)
        engine!!.prepareDisplayTaskFor(imageAware, memoryCacheKey)
        listener.onLoadingStarted(uri, imageAware.wrappedView)
        val bmp = configuration!!.memoryCache!![memoryCacheKey]
        if (bmp != null && !bmp.isRecycled) {
            d(LOG_LOAD_IMAGE_FROM_MEMORY_CACHE, memoryCacheKey)
            if (options!!.shouldPostProcess()) {
                val imageLoadingInfo = ImageLoadingInfo(
                    uri!!, imageAware, targetSize, memoryCacheKey, options, listener, progressListener!!, engine!!.getLockForUri(
                        uri
                    )
                )
                val displayTask = ProcessAndDisplayImageTask(engine!!, bmp, imageLoadingInfo, defineHandler(options)!!)
                if (options.isSyncLoading) {
                    displayTask.run()
                } else {
                    engine!!.submit(displayTask)
                }
            } else {
                options.displayer.display(bmp, imageAware, LoadedFrom.MEMORY_CACHE)
                listener.onLoadingComplete(uri, imageAware.wrappedView, bmp)
            }
        } else {
            if (options!!.shouldShowImageOnLoading()) {
                imageAware.setImageDrawable(options.getImageOnLoading(configuration!!.resources))
            } else if (options.isResetViewBeforeLoading) {
                imageAware.setImageDrawable(null)
            }
            val imageLoadingInfo = ImageLoadingInfo(
                uri!!, imageAware, targetSize, memoryCacheKey, options, listener, progressListener!!, engine!!.getLockForUri(
                    uri
                )
            )
            val displayTask = LoadAndDisplayImageTask(engine!!, imageLoadingInfo, defineHandler(options)!!)
            if (options.isSyncLoading) {
                displayTask.run()
            } else {
                engine!!.submit(displayTask)
            }
        }
    }

    /**
     * Adds display image task to execution pool. Image will be set to ImageView
     * when it's turn. <br></br>
     * Default [display image options][DisplayImageOptions] from
     * [configuration][ImageLoaderConfiguration] will be used.<br></br>
     * **NOTE:** [.init] method must be
     * called before this method call
     *
     * @param uri
     * Image URI (i.e. "http://site.com/image.png",
     * "file:///mnt/sdcard/image.png")
     * @param imageView
     * [ImageView] which should display image
     * @throws IllegalStateException
     * if [.init] method wasn't
     * called before
     * @throws IllegalArgumentException
     * if passed **imageView** is null
     */
    fun displayImage(uri: String?, imageView: ImageView?) {
        displayImage(uri, ImageViewAware(imageView), null, null, null)
    }

    /**
     * Adds display image task to execution pool. Image will be set to ImageView
     * when it's turn. <br></br>
     * Default [display image options][DisplayImageOptions] from
     * [configuration][ImageLoaderConfiguration] will be used.<br></br>
     * **NOTE:** [.init] method must be
     * called before this method call
     *
     * @param uri
     * Image URI (i.e. "http://site.com/image.png",
     * "file:///mnt/sdcard/image.png")
     * @param imageView
     * [ImageView] which should display image
     * @throws IllegalStateException
     * if [.init] method wasn't
     * called before
     * @throws IllegalArgumentException
     * if passed **imageView** is null
     */
    fun displayImage(uri: String?, imageView: ImageView?, targetImageSize: ImageSize?) {
        displayImage(uri, ImageViewAware(imageView), null, targetImageSize, null, null)
    }

    /**
     * Adds display image task to execution pool. Image will be set to ImageView
     * when it's turn.<br></br>
     * **NOTE:** [.init] method must be
     * called before this method call
     *
     * @param uri
     * Image URI (i.e. "http://site.com/image.png",
     * "file:///mnt/sdcard/image.png")
     * @param imageView
     * [ImageView] which should display image
     * @param options
     * [            Options][DisplayImageOptions] for image decoding and displaying. If **null** -
     * default display image options
     * [            from configuration][ImageLoaderConfiguration.Builder.defaultDisplayImageOptions] will be used.
     * @throws IllegalStateException
     * if [.init] method wasn't
     * called before
     * @throws IllegalArgumentException
     * if passed **imageView** is null
     */
    fun displayImage(uri: String?, imageView: ImageView?, options: DisplayImageOptions?) {
        displayImage(uri, ImageViewAware(imageView), options, null, null)
    }

    /**
     * Adds display image task to execution pool. Image will be set to ImageView
     * when it's turn.<br></br>
     * Default [display image options][DisplayImageOptions] from
     * [configuration][ImageLoaderConfiguration] will be used.<br></br>
     * **NOTE:** [.init] method must be
     * called before this method call
     *
     * @param uri
     * Image URI (i.e. "http://site.com/image.png",
     * "file:///mnt/sdcard/image.png")
     * @param imageView
     * [ImageView] which should display image
     * @param listener
     * [Listener][ImageLoadingListener] for image loading
     * process. Listener fires events on UI thread if this method is
     * called on UI thread.
     * @throws IllegalStateException
     * if [.init] method wasn't
     * called before
     * @throws IllegalArgumentException
     * if passed **imageView** is null
     */
    fun displayImage(uri: String?, imageView: ImageView?, listener: ImageLoadingListener?) {
        displayImage(uri, ImageViewAware(imageView), null, listener, null)
    }
    /**
     * Adds display image task to execution pool. Image will be set to ImageView
     * when it's turn.<br></br>
     * **NOTE:** [.init] method must be
     * called before this method call
     *
     * @param uri
     * Image URI (i.e. "http://site.com/image.png",
     * "file:///mnt/sdcard/image.png")
     * @param imageView
     * [ImageView] which should display image
     * @param options
     * [            Options][DisplayImageOptions] for image decoding and displaying. If **null** -
     * default display image options
     * [            from configuration][ImageLoaderConfiguration.Builder.defaultDisplayImageOptions] will be used.
     * @param listener
     * [Listener][ImageLoadingListener] for image loading
     * process. Listener fires events on UI thread if this method is
     * called on UI thread.
     * @param progressListener
     * [            Listener][ImageLoadingProgressListener] for image loading progress. Listener fires events on
     * UI thread if this method is called on UI thread. Caching on
     * disk should be enabled in
     * [            options][DisplayImageOptions] to make this listener work.
     * @throws IllegalStateException
     * if [.init] method wasn't
     * called before
     * @throws IllegalArgumentException
     * if passed **imageView** is null
     */
    /**
     * Adds display image task to execution pool. Image will be set to ImageView
     * when it's turn.<br></br>
     * **NOTE:** [.init] method must be
     * called before this method call
     *
     * @param uri
     * Image URI (i.e. "http://site.com/image.png",
     * "file:///mnt/sdcard/image.png")
     * @param imageView
     * [ImageView] which should display image
     * @param options
     * [            Options][DisplayImageOptions] for image decoding and displaying. If **null** -
     * default display image options
     * [            from configuration][ImageLoaderConfiguration.Builder.defaultDisplayImageOptions] will be used.
     * @param listener
     * [Listener][ImageLoadingListener] for image loading
     * process. Listener fires events on UI thread if this method is
     * called on UI thread.
     * @throws IllegalStateException
     * if [.init] method wasn't
     * called before
     * @throws IllegalArgumentException
     * if passed **imageView** is null
     */
    @JvmOverloads
    fun displayImage(
        uri: String?,
        imageView: ImageView?,
        options: DisplayImageOptions?,
        listener: ImageLoadingListener?,
        progressListener: ImageLoadingProgressListener? = null
    ) {
        displayImage(uri, ImageViewAware(imageView), options, listener, progressListener)
    }

    /**
     * Adds load image task to execution pool. Image will be returned with
     * [ImageLoadingListener.onLoadingComplete]
     * callback}. <br></br>
     * **NOTE:** [.init] method must be
     * called before this method call
     *
     * @param uri
     * Image URI (i.e. "http://site.com/image.png",
     * "file:///mnt/sdcard/image.png")
     * @param listener
     * [Listener][ImageLoadingListener] for image loading
     * process. Listener fires events on UI thread if this method is
     * called on UI thread.
     * @throws IllegalStateException
     * if [.init] method wasn't
     * called before
     */
    fun loadImage(uri: String?, listener: ImageLoadingListener?) {
        loadImage(uri, null, null, listener, null)
    }

    /**
     * Adds load image task to execution pool. Image will be returned with
     * [ImageLoadingListener.onLoadingComplete]
     * callback}. <br></br>
     * **NOTE:** [.init] method must be
     * called before this method call
     *
     * @param uri
     * Image URI (i.e. "http://site.com/image.png",
     * "file:///mnt/sdcard/image.png")
     * @param targetImageSize
     * Minimal size for [Bitmap] which will be returned in
     * [ImageLoadingListener.onLoadingComplete]
     * callback}. Downloaded image will be decoded and scaled to
     * [Bitmap] of the size which is **equal or larger**
     * (usually a bit larger) than incoming targetImageSize.
     * @param listener
     * [Listener][ImageLoadingListener] for image loading
     * process. Listener fires events on UI thread if this method is
     * called on UI thread.
     * @throws IllegalStateException
     * if [.init] method wasn't
     * called before
     */
    fun loadImage(uri: String?, targetImageSize: ImageSize?, listener: ImageLoadingListener?) {
        loadImage(uri, targetImageSize, null, listener, null)
    }

    /**
     * Adds load image task to execution pool. Image will be returned with
     * [ImageLoadingListener.onLoadingComplete]
     * callback}. <br></br>
     * **NOTE:** [.init] method must be
     * called before this method call
     *
     * @param uri
     * Image URI (i.e. "http://site.com/image.png",
     * "file:///mnt/sdcard/image.png")
     * @param options
     * [            Options][DisplayImageOptions] for image decoding and displaying. If **null** -
     * default display image options
     * [            from configuration][ImageLoaderConfiguration.Builder.defaultDisplayImageOptions] will be used.<br></br>
     * @param listener
     * [Listener][ImageLoadingListener] for image loading
     * process. Listener fires events on UI thread if this method is
     * called on UI thread.
     * @throws IllegalStateException
     * if [.init] method wasn't
     * called before
     */
    fun loadImage(uri: String?, options: DisplayImageOptions?, listener: ImageLoadingListener?): Int {
        return loadImage(uri, null, options, listener, null)
    }

    /**
     * Adds load image task to execution pool. Image will be returned with
     * [ImageLoadingListener.onLoadingComplete]
     * callback}. <br></br>
     * **NOTE:** [.init] method must be
     * called before this method call
     *
     * @param uri
     * Image URI (i.e. "http://site.com/image.png",
     * "file:///mnt/sdcard/image.png")
     * @param targetImageSize
     * Minimal size for [Bitmap] which will be returned in
     * [ImageLoadingListener.onLoadingComplete]
     * callback}. Downloaded image will be decoded and scaled to
     * [Bitmap] of the size which is **equal or larger**
     * (usually a bit larger) than incoming targetImageSize.
     * @param options
     * [            Options][DisplayImageOptions] for image decoding and displaying. If **null** -
     * default display image options
     * [            from configuration][ImageLoaderConfiguration.Builder.defaultDisplayImageOptions] will be used.<br></br>
     * @param listener
     * [Listener][ImageLoadingListener] for image loading
     * process. Listener fires events on UI thread if this method is
     * called on UI thread.
     * @throws IllegalStateException
     * if [.init] method wasn't
     * called before
     */
    fun loadImage(uri: String?, targetImageSize: ImageSize?, options: DisplayImageOptions?, listener: ImageLoadingListener?) {
        loadImage(uri, targetImageSize, options, listener, null)
    }

    /**
     * Adds load image task to execution pool. Image will be returned with
     * [ImageLoadingListener.onLoadingComplete]
     * callback}. <br></br>
     * **NOTE:** [.init] method must be
     * called before this method call
     *
     * @param uri
     * Image URI (i.e. "http://site.com/image.png",
     * "file:///mnt/sdcard/image.png")
     * @param targetImageSize
     * Minimal size for [Bitmap] which will be returned in
     * [ImageLoadingListener.onLoadingComplete]
     * callback}. Downloaded image will be decoded and scaled to
     * [Bitmap] of the size which is **equal or larger**
     * (usually a bit larger) than incoming targetImageSize.
     * @param options
     * [            Options][DisplayImageOptions] for image decoding and displaying. If **null** -
     * default display image options
     * [            from configuration][ImageLoaderConfiguration.Builder.defaultDisplayImageOptions] will be used.<br></br>
     * @param listener
     * [Listener][ImageLoadingListener] for image loading
     * process. Listener fires events on UI thread if this method is
     * called on UI thread.
     * @param progressListener
     * [            Listener][ImageLoadingProgressListener] for image loading progress. Listener fires events on
     * UI thread if this method is called on UI thread. Caching on
     * disk should be enabled in
     * [            options][DisplayImageOptions] to make this listener work.
     * @throws IllegalStateException
     * if [.init] method wasn't
     * called before
     */
    fun loadImage(
        uri: String?,
        targetImageSize: ImageSize?,
        options: DisplayImageOptions?,
        listener: ImageLoadingListener?,
        progressListener: ImageLoadingProgressListener?
    ): Int {
        var targetImageSize = targetImageSize
        var options = options
        checkConfiguration()
        if (targetImageSize == null) {
            targetImageSize = configuration!!.maxImageSize
        }
        if (options == null) {
            options = configuration!!.defaultDisplayImageOptions
        }
        val imageAware = NonViewAware(uri, targetImageSize, ViewScaleType.CROP)
        displayImage(uri, imageAware, options, listener, progressListener)
        return imageAware.id
    }

    /**
     * Loads and decodes image synchronously.<br></br>
     * **NOTE:** [.init] method must be
     * called before this method call
     *
     * @param uri
     * Image URI (i.e. "http://site.com/image.png",
     * "file:///mnt/sdcard/image.png")
     * @param options
     * [            Options][DisplayImageOptions] for image decoding and scaling. If **null** -
     * default display image options
     * [            from configuration][ImageLoaderConfiguration.Builder.defaultDisplayImageOptions] will be used.
     * @return Result image Bitmap. Can be **null** if image loading/decoding
     * was failed or cancelled.
     * @throws IllegalStateException
     * if [.init] method wasn't
     * called before
     */
    fun loadImageSync(uri: String?, options: DisplayImageOptions?): Bitmap? {
        return loadImageSync(uri, null, options)
    }
    /**
     * Loads and decodes image synchronously.<br></br>
     * **NOTE:** [.init] method must be
     * called before this method call
     *
     * @param uri
     * Image URI (i.e. "http://site.com/image.png",
     * "file:///mnt/sdcard/image.png")
     * @param targetImageSize
     * Minimal size for [Bitmap] which will be returned.
     * Downloaded image will be decoded and scaled to [Bitmap]
     * of the size which is **equal or larger** (usually a bit
     * larger) than incoming targetImageSize.
     * @param options
     * [            Options][DisplayImageOptions] for image decoding and scaling. If **null** -
     * default display image options
     * [            from configuration][ImageLoaderConfiguration.Builder.defaultDisplayImageOptions] will be used.
     * @return Result image Bitmap. Can be **null** if image loading/decoding
     * was failed or cancelled.
     * @throws IllegalStateException
     * if [.init] method wasn't
     * called before
     */
    /**
     * Loads and decodes image synchronously.<br></br>
     * Default display image options
     * [ from configuration][ImageLoaderConfiguration.Builder.defaultDisplayImageOptions] will be used.<br></br>
     * **NOTE:** [.init] method must be
     * called before this method call
     *
     * @param uri
     * Image URI (i.e. "http://site.com/image.png",
     * "file:///mnt/sdcard/image.png")
     * @return Result image Bitmap. Can be **null** if image loading/decoding
     * was failed or cancelled.
     * @throws IllegalStateException
     * if [.init] method wasn't
     * called before
     */
    /**
     * Loads and decodes image synchronously.<br></br>
     * Default display image options
     * [ from configuration][ImageLoaderConfiguration.Builder.defaultDisplayImageOptions] will be used.<br></br>
     * **NOTE:** [.init] method must be
     * called before this method call
     *
     * @param uri
     * Image URI (i.e. "http://site.com/image.png",
     * "file:///mnt/sdcard/image.png")
     * @param targetImageSize
     * Minimal size for [Bitmap] which will be returned.
     * Downloaded image will be decoded and scaled to [Bitmap]
     * of the size which is **equal or larger** (usually a bit
     * larger) than incoming targetImageSize.
     * @return Result image Bitmap. Can be **null** if image loading/decoding
     * was failed or cancelled.
     * @throws IllegalStateException
     * if [.init] method wasn't
     * called before
     */
    @JvmOverloads
    fun loadImageSync(uri: String?, targetImageSize: ImageSize? = null, options: DisplayImageOptions? = null): Bitmap? {
        var options = options
        if (options == null) {
            options = configuration!!.defaultDisplayImageOptions
        }
        options = DisplayImageOptions.Builder().cloneFrom(options!!).syncLoading(true).build()
        val listener = SyncImageLoadingListener()
        loadImage(uri, targetImageSize, options, listener)
        return listener.loadedBitmap
    }

    /**
     * Checks if ImageLoader's configuration was initialized
     *
     * @throws IllegalStateException
     * if configuration wasn't initialized
     */
    private fun checkConfiguration() {
        checkNotNull(configuration) { ERROR_NOT_INIT }
    }

    /** Sets a default loading listener for all display and loading tasks.  */
    fun setDefaultLoadingListener(listener: ImageLoadingListener?) {
        defaultListener = listener ?: SimpleImageLoadingListener()
    }

    /**
     * Returns memory cache
     *
     * @throws IllegalStateException
     * if [.init] method wasn't
     * called before
     */
    val memoryCache: MemoryCache?
        get() {
            checkConfiguration()
            return configuration!!.memoryCache
        }

    /**
     * Clears memory cache
     *
     * @throws IllegalStateException
     * if [.init] method wasn't
     * called before
     */
    fun clearMemoryCache() {
        checkConfiguration()
        configuration!!.memoryCache!!.clear()
    }

    /**
     * Returns disk cache
     *
     * @throws IllegalStateException
     * if [.init] method wasn't
     * called before
     */
    @get:Deprecated("Use {@link #getDiskCache()} instead")
    val discCache: DiskCache?
        get() = diskCache

    /**
     * Returns disk cache
     *
     * @throws IllegalStateException
     * if [.init] method wasn't
     * called before
     */
    val diskCache: DiskCache?
        get() {
            checkConfiguration()
            return configuration!!.diskCache
        }

    /**
     * Clears disk cache.
     *
     * @throws IllegalStateException
     * if [.init] method wasn't
     * called before
     */
    @Deprecated("Use {@link #clearDiskCache()} instead")
    fun clearDiscCache() {
        clearDiskCache()
    }

    /**
     * Clears disk cache.
     *
     * @throws IllegalStateException
     * if [.init] method wasn't
     * called before
     */
    fun clearDiskCache() {
        checkConfiguration()
        configuration!!.diskCache!!.clear()
    }

    /**
     * Returns URI of image which is loading at this moment into passed
     * [ ImageAware][ImageAware]
     */
    fun getLoadingUriForView(imageAware: ImageAware?): String? {
        return engine!!.getLoadingUriForView(imageAware!!)
    }

    /**
     * Returns URI of image which is loading at this moment into passed
     * [ImageView][android.widget.ImageView]
     */
    fun getLoadingUriForView(imageView: ImageView?): String? {
        return engine!!.getLoadingUriForView(ImageViewAware(imageView))
    }

    /**
     * Cancel the task of loading and displaying image for passed
     * [ ImageAware][ImageAware].
     *
     * @param imageAware
     * [            ImageAware][ImageAware] for which display task will be cancelled
     */
    fun cancelDisplayTask(imageAware: ImageAware?) {
        engine!!.cancelDisplayTaskFor(imageAware!!)
    }

    fun cancelDisplayTaskForID(id: Int) {
        engine!!.cancelDisplayTaskFor(id)
    }

    /**
     * Cancel the task of loading and displaying image for passed
     * [ImageView][android.widget.ImageView].
     *
     * @param imageView
     * [ImageView][android.widget.ImageView] for which display
     * task will be cancelled
     */
    fun cancelDisplayTask(imageView: ImageView?) {
        engine!!.cancelDisplayTaskFor(ImageViewAware(imageView))
    }

    /**
     * Denies or allows ImageLoader to download images from the network.<br></br>
     * <br></br>
     * If downloads are denied and if image isn't cached then
     * [ImageLoadingListener.onLoadingFailed]
     * callback will be fired with [FailReason.FailType.NETWORK_DENIED]
     *
     * @param denyNetworkDownloads
     * pass **true** - to deny engine to download images from the
     * network; **false** - to allow engine to download images
     * from network.
     */
    fun denyNetworkDownloads(denyNetworkDownloads: Boolean) {
        engine!!.denyNetworkDownloads(denyNetworkDownloads)
    }

    /**
     * Sets option whether ImageLoader will use [FlushedInputStream] for
     * network downloads to handle
     * [this
 * known problem](http://code.google.com/p/android/issues/detail?id=6066) or not.
     *
     * @param handleSlowNetwork
     * pass **true** - to use [FlushedInputStream] for
     * network downloads; **false** - otherwise.
     */
    fun handleSlowNetwork(handleSlowNetwork: Boolean) {
        engine!!.handleSlowNetwork(handleSlowNetwork)
    }

    /**
     * Pause ImageLoader. All new "load&display" tasks won't be executed until
     * ImageLoader is [resumed][.resume]. <br></br>
     * Already running tasks are not paused.
     */
    fun pause() {
        engine!!.pause()
    }

    /** Resumes waiting "load&display" tasks  */
    fun resume() {
        engine!!.resume()
    }

    /**
     * Cancels all running and scheduled display image tasks.<br></br>
     * **NOTE:** This method doesn't shutdown
     * [ custom task executors][ImageLoaderConfiguration.Builder.taskExecutor] if you set them.<br></br>
     * ImageLoader still can be used after calling this method.
     */
    fun stop() {
        if (engine != null) {
            engine!!.stop()
        }
    }

    fun clearAllTasks() {
        engine!!.clearAllTasks()
    }

    /**
     * [Stops ImageLoader][.stop] and clears current configuration.
     * <br></br>
     * You can [init][.init] ImageLoader
     * with new configuration after calling this method.
     */
    fun destroy() {
        if (configuration != null) d(LOG_DESTROY)
        stop()
        configuration!!.diskCache!!.close()
        engine = null
        configuration = null
    }

    /**
     * Listener which is designed for synchronous image loading.
     *
     * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
     * @since 1.9.0
     */
    private class SyncImageLoadingListener : SimpleImageLoadingListener() {
        var loadedBitmap: Bitmap? = null
            private set

        override fun onLoadingComplete(imageUri: String?, view: View?, loadedImage: Bitmap?) {
            loadedBitmap = loadedImage
        }
    }

    companion object {
        val TAG = ImageLoader::class.java.simpleName
        const val LOG_INIT_CONFIG = "Initialize ImageLoader with configuration"
        const val LOG_DESTROY = "Destroy ImageLoader"
        const val LOG_LOAD_IMAGE_FROM_MEMORY_CACHE = "Load image from memory cache [%s]"
        private const val WARNING_RE_INIT_CONFIG =
            "Try to initialize ImageLoader which had already been initialized before. " + "To re-init ImageLoader with new configuration call ImageLoader.destroy() at first."
        private const val ERROR_WRONG_ARGUMENTS = "Wrong arguments were passed to displayImage() method (ImageView reference must not be null)"
        private const val ERROR_NOT_INIT = "ImageLoader must be init with configuration before using"
        private const val ERROR_INIT_CONFIG_WITH_NULL = "ImageLoader configuration can not be initialized with null"

        /** Returns singleton class instance  */
        @JvmStatic
        @Volatile
        var instance: ImageLoader? = null
            get() {
                if (field == null) {
                    synchronized(ImageLoader::class.java) {
                        if (field == null) {
                            field = ImageLoader()
                        }
                    }
                }
                return field
            }
            private set

        private fun defineHandler(options: DisplayImageOptions?): Handler? {
            var handler = options!!.handler
            if (options.isSyncLoading) {
                handler = null
            } else if (handler == null && Looper.myLooper() == Looper.getMainLooper()) {
                handler = Handler()
            }
            return handler
        }
    }
}
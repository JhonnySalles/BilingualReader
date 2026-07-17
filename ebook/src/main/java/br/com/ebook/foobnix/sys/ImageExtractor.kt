package br.com.ebook.foobnix.sys

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.util.Base64
import android.util.Pair
import br.com.ebook.util.BitmapUtils
import br.com.ebook.Config
import br.com.ebook.core.BookExtractorFactory
import br.com.ebook.foobnix.android.utils.Dips
import br.com.ebook.foobnix.android.utils.Safe
import br.com.ebook.foobnix.android.utils.TxtUtils
import br.com.ebook.foobnix.ext.CacheZipUtils
import br.com.ebook.foobnix.pdf.info.ExtUtils
import br.com.ebook.foobnix.pdf.info.IMG
import br.com.ebook.foobnix.pdf.info.PageUrl
import br.com.ebook.foobnix.pdf.info.wrapper.AppState
import br.com.ebook.foobnix.pdf.info.wrapper.MagicHelper
import br.com.ebook.universalimageloader.core.download.BaseImageDownloader
import br.com.ebook.universalimageloader.core.download.ImageDownloader
import kotlinx.coroutines.runBlocking
import org.ebookdroid.BookType
import org.ebookdroid.common.bitmaps.BitmapRef
import org.ebookdroid.common.bitmaps.RawBitmap
import org.ebookdroid.core.codec.CodecContext
import org.ebookdroid.core.codec.CodecDocument
import org.ebookdroid.core.codec.CodecPage
import org.ebookdroid.core.crop.PageCropper
import org.ebookdroid.droids.mupdf.codec.exceptions.MuPdfPasswordException
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.Locale

class ImageExtractor private constructor(private val context: Context) : ImageDownloader {
    
    private val baseImage = BaseImageDownloader(context)

    companion object {
        init {
            NativeLibLoader.loadLibrary("mypdf")
        }

        private val LOGGER = LoggerFactory.getLogger(ImageExtractor::class.java)

        const val COVER_PAGE_WITH_EFFECT = -3
        const val COVER_PAGE_NO_EFFECT = -2
        const val COVER_PAGE = -1

        @Volatile
        private var instance: ImageExtractor? = null
        
        @Volatile
        var sp: SharedPreferences? = null

        @JvmStatic
        @Synchronized
        fun getInstance(c: Context): ImageExtractor {
            if (instance == null) {
                instance = ImageExtractor(c.applicationContext)
            }
            sp = c.getSharedPreferences("Errors", Context.MODE_PRIVATE)
            return instance!!
        }

        @JvmStatic
        fun clearErrors() {
            sp?.edit()?.clear()?.apply()
        }

        // Cache estático encapsulado para evitar vazamentos de memória soltos
        object CodecCache {
            var codeCache: CodecDocument? = null
            var codecContex: CodecContext? = null
            var pathCache: String? = null
            var whCache: Int = -1
            var pageCount: Int = 0

            @Synchronized
            fun clear() {
                codeCache?.let {
                    it.recycle()
                    LOGGER.info("CodecCache: codeCache recycled")
                }
                codeCache = null
                pathCache = null
                codecContex?.let {
                    it.recycle()
                    LOGGER.info("CodecCache: codecContex recycled")
                }
                codecContex = null
                whCache = -1
                pageCount = 0
                TempHolder.get().clear()
            }
        }

        @JvmStatic
        fun clearCodeDocument() {
            CodecCache.clear()
        }

        @JvmStatic
        fun init(codec: CodecDocument?, path: String?) {
            CodecCache.clear()
            CodecCache.codeCache = codec
            CodecCache.pathCache = path
        }

        @JvmStatic
        @Synchronized
        fun singleCodecContext(path: String, passw: String, w: Int, h: Int): CodecDocument? {
            return try {
                val codecCtx = BookType.getCodecContextByPath(path)
                TempHolder.get().init(path)
                LOGGER.info("CodecContext: {}", codecCtx)
                if (codecCtx == null) return null
                TempHolder.get().loadingCancelled = false
                codecCtx.openDocument(path, passw)
            } catch (e: RuntimeException) {
                LOGGER.error("Error get single codec context: {}", e.message, e)
                null
            }
        }

        @JvmStatic
        @Synchronized
        fun getNewCodecContext(path: String, passw: String, w: Int, h: Int, font: Int): CodecDocument? {
            var width = w
            var height = h
            if (path == CodecCache.pathCache && CodecCache.codeCache != null && !CodecCache.codeCache!!.isRecycled) {
                LOGGER.info("getNewCodecContext cache: {}", path)
                return CodecCache.codeCache
            }

            LOGGER.info("getNewCodecContext new: {}", path)
            CodecCache.clear()

            if (width <= 0 || height <= 0) {
                width = Dips.screenWidth()
                height = Dips.screenHeight()
            }

            val ctx = BookType.getCodecContextByPath(path)
            TempHolder.get().init(path)
            LOGGER.info("CodecContext: {}", ctx)
            if (ctx == null) return null

            TempHolder.get().loadingCancelled = false
            val document = ctx.openDocument(path, passw) ?: return null

            CodecCache.codecContex = ctx
            CodecCache.codeCache = document
            CodecCache.pageCount = document.getPageCount(width, height, font)
            CodecCache.pathCache = path
            CodecCache.whCache = height + width

            return document
        }
    }

    fun processCoverPage(pageUrl: PageUrl): Bitmap? {
        val path = pageUrl.path
        if (pageUrl.height == 0) {
            pageUrl.height = (pageUrl.width * 1.5).toInt()
        }

        val ebookMeta = runBlocking(br.com.ebook.core.EbookDispatcher.dispatcher) {
            BookExtractorFactory.getMetadata(path)
        }

        val unZipPath = ebookMeta.unzipPath ?: path
        var cover: Bitmap? = null

        if (ebookMeta.coverImage != null) {
            cover = BitmapUtils.arrayToBitmap(ebookMeta.coverImage, pageUrl.width)
        } else {
            // Usamos a nova BookExtractorFactory em Kotlin
            val coverBytes = runBlocking(br.com.ebook.core.EbookDispatcher.dispatcher) {
                BookExtractorFactory.getExtractor(unZipPath)?.extractCover(unZipPath)?.getOrNull()
            }
            if (coverBytes != null) {
                cover = BitmapUtils.arrayToBitmap(coverBytes, pageUrl.width)
            } else if (BookType.PDF.`is`(unZipPath) || BookType.DJVU.`is`(unZipPath) || BookType.TIFF.`is`(unZipPath)) {
                cover = processOtherPage(pageUrl)
            } else if (ExtUtils.isFileArchive(unZipPath)) {
                val ext = ExtUtils.getFileExtension(unZipPath)
                cover = BitmapUtils.getBookCoverWithTitle("...", "  [" + ext.uppercase(Locale.getDefault()) + "]", true)
                pageUrl.tempWithWatermakr = true
            } else if (ExtUtils.isFontFile(unZipPath)) {
                cover = BitmapUtils.getBookCoverWithTitle("font", "", true)
                pageUrl.tempWithWatermakr = true
            }
        }

        if (cover == null) {
            cover = BitmapUtils.getBookCoverWithTitle(ebookMeta.author, ebookMeta.title, true)
            pageUrl.tempWithWatermakr = true
        }

        return cover
    }

    fun generalCoverWithEffect(pageUrl: PageUrl, cover: Bitmap): InputStream? {
        return try {
            val out = ByteArrayOutputStream()
            val res: Bitmap
            if (AppState.get().isBookCoverEffect || pageUrl.page == COVER_PAGE_WITH_EFFECT) {
                res = MagicHelper.scaleCenterCrop(cover, pageUrl.height, pageUrl.width, !pageUrl.tempWithWatermakr)
                res.compress(CompressFormat.PNG, 90, out)
            } else {
                res = cover
                res.compress(CompressFormat.JPEG, 90, out)
            }

            val byteArray = out.toByteArray()
            val stream = ByteArrayInputStream(byteArray)
            res.recycle()
            out.close()
            stream
        } catch (e: Exception) {
            LOGGER.error("Error get cover with effect: {}", e.message, e)
            null
        }
    }

    fun processOtherPage(pageUrl: PageUrl): Bitmap? {
        var page = pageUrl.page
        val path = pageUrl.path
        var isNeedDisableMagicInPDFDjvu = false

        if (pageUrl.page == COVER_PAGE || pageUrl.page == COVER_PAGE_NO_EFFECT || pageUrl.page == COVER_PAGE_WITH_EFFECT) {
            isNeedDisableMagicInPDFDjvu = true
        }

        if (page < 0) page = 0

        val codeCache = if (isNeedDisableMagicInPDFDjvu) {
            singleCodecContext(path, "", pageUrl.width, pageUrl.height)
        } else {
            getNewCodecContext(path, "", pageUrl.width, pageUrl.height, AppState.get().fontSizeSp)
        } ?: return null

        val pageInfo = codeCache.getPageInfo(page)
        val k = pageInfo.height.toFloat() / pageInfo.width
        val width = pageUrl.width
        val height = (width * k).toInt()

        val pageCodec = codeCache.getPage(page)
        var bitmap: Bitmap? = null
        var bitmapRef: BitmapRef?

        if (pageUrl.number == 0) {
            if (isNeedDisableMagicInPDFDjvu) MagicHelper.isNeedMagic = false
            bitmapRef = pageCodec.renderBitmap(width, height, RectF(0f, 0f, 1f, 1f))
            if (isNeedDisableMagicInPDFDjvu) MagicHelper.isNeedMagic = true
            bitmap = bitmapRef.bitmap
            if (pageUrl.isCrop) {
                bitmap = cropBitmap(bitmap)
            }
        } else if (pageUrl.number == 1) {
            val right = pageUrl.cutp.toFloat() / 100
            bitmapRef = pageCodec.renderBitmap((width * right).toInt(), height, RectF(0f, 0f, right, 1f))
            bitmap = bitmapRef.bitmap
            if (pageUrl.isCrop) bitmap = cropBitmap(bitmap)
        } else if (pageUrl.number == 2) {
            val right = pageUrl.cutp.toFloat() / 100
            bitmapRef = pageCodec.renderBitmap((width * (1 - right)).toInt(), height, RectF(right, 0f, 1f, 1f))
            bitmap = bitmapRef.bitmap
            if (pageUrl.isCrop) bitmap = cropBitmap(bitmap)
        }

        var currentBitmap = bitmap
        if (currentBitmap != null) {
            if (pageUrl.isInvert) {
                val bmp = RawBitmap(currentBitmap, Rect(0, 0, currentBitmap.width, currentBitmap.height))
                bmp.invert()
                currentBitmap.recycle()
                currentBitmap = bmp.toBitmap().bitmap
            }

            if (currentBitmap != null && pageUrl.rotate > 0) {
                val matrix = Matrix()
                matrix.postRotate(pageUrl.rotate.toFloat())
                val rotatedBitmap = Bitmap.createBitmap(currentBitmap, 0, 0, currentBitmap.width, currentBitmap.height, matrix, true)
                currentBitmap.recycle()
                currentBitmap = rotatedBitmap
            }
            bitmap = currentBitmap
        }

        if (!pageCodec.isRecycled) pageCodec.recycle()
        if (isNeedDisableMagicInPDFDjvu) codeCache.recycle()

        if (bitmap != null && !isNeedDisableMagicInPDFDjvu && MagicHelper.isNeedBookBackgroundImage()) {
            bitmap = MagicHelper.updateWithBackground(bitmap)
        }

        return bitmap
    }

    fun cropBitmap(bitmap: Bitmap): Bitmap {
        val rootRect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectCrop = PageCropper.getCropBounds(bitmap, rootRect, RectF(0f, 0f, 1f, 1f))
        val x = (bitmap.width * rectCrop.left).toInt()
        val y = (bitmap.height * rectCrop.top).toInt()
        val w = (bitmap.width * rectCrop.width()).toInt()
        val h = (bitmap.height * rectCrop.height()).toInt()
        val cropped = Bitmap.createBitmap(bitmap, x, y, w, h)
        bitmap.recycle()
        return cropped
    }

    override fun getStream(imageUri: String?, extra: Any?): InputStream? {
        return getStreamInner(imageUri ?: "")
    }

    fun getStreamInner(imageUri: String): InputStream? {
        LOGGER.info("url: {}", imageUri)

        if (imageUri.startsWith(Safe.TXT_SAFE_RUN)) {
            return baseImage.getStream("assets://opds/web111.png", null)
        }

        if (imageUri.startsWith("data:")) {
            var uri = imageUri
            uri = uri.substring(uri.indexOf(",") + 1)
            return ByteArrayInputStream(Base64.decode(uri, Base64.DEFAULT))
        }

        if (!imageUri.startsWith("{")) {
            return baseImage.getStream(imageUri, null)
        }

        if (sp?.contains(imageUri.hashCode().toString()) == true) {
            return messageFile("#crash", "")
        }

        val pageUrl = PageUrl.fromString(imageUri)
        val path = pageUrl.path
        val file = File(path)

        return try {
            if (ExtUtils.isImageFile(file)) {
                return BitmapUtils.decodeImage(path, IMG.getImageSize())
            }

            if (path.endsWith("json")) {
                return messageFile("#json", "")
            }

            if (!file.isFile) {
                return messageFile("#no file", "")
            }

            sp?.edit()?.putBoolean(imageUri.hashCode().toString(), true)?.apply()

            val page = pageUrl.page
            if (pageUrl.height == 0) {
                pageUrl.height = (pageUrl.width * 1.5).toInt()
            }

            val resultStream: InputStream?
            if (page == COVER_PAGE || page == COVER_PAGE_WITH_EFFECT) {
                try {
                    MagicHelper.isNeedBC = false
                    val processCoverPage = processCoverPage(pageUrl)
                    resultStream = processCoverPage?.let { generalCoverWithEffect(pageUrl, it) }
                } finally {
                    MagicHelper.isNeedBC = true
                }
            } else if (page == COVER_PAGE_NO_EFFECT) {
                resultStream = processCoverPage(pageUrl)?.let { bitmapToStream(it) }
            } else {
                if (pageUrl.isDouble) {
                    if (AppState.get().isDoubleCoverAlone) {
                        pageUrl.page = pageUrl.page - 1
                    }

                    val bitmap1 = processOtherPage(pageUrl) ?: throw IOException("bitmap1 rendering failed")
                    pageUrl.page = pageUrl.page + 1

                    var bitmap2: Bitmap?
                    if (pageUrl.page < CodecCache.pageCount) {
                        bitmap2 = processOtherPage(pageUrl)
                    } else {
                        bitmap2 = Bitmap.createBitmap(bitmap1)
                        val canvas = Canvas(bitmap2)
                        canvas.drawColor(Color.WHITE)
                    }

                    val maxH = Math.max(bitmap1.height, bitmap2!!.height)
                    val bitmap = Bitmap.createBitmap(bitmap1.width + bitmap2.width, maxH, Bitmap.Config.RGB_565)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(MagicHelper.getBgColor())

                    if (AppState.get().isCutRTL) {
                        canvas.drawBitmap(bitmap2, 0f, ((maxH - bitmap2.height) / 2).toFloat(), null)
                        canvas.drawBitmap(bitmap1, bitmap2.width.toFloat(), ((maxH - bitmap1.height) / 2).toFloat(), null)
                    } else {
                        canvas.drawBitmap(bitmap1, 0f, ((maxH - bitmap1.height) / 2).toFloat(), null)
                        canvas.drawBitmap(bitmap2, bitmap1.width.toFloat(), ((maxH - bitmap2.height) / 2).toFloat(), null)
                    }

                    bitmap1.recycle()
                    bitmap2.recycle()
                    resultStream = bitmapToStreamRAW(bitmap)
                } else {
                    resultStream = processOtherPage(pageUrl)?.let { bitmapToStreamRAW(it) }
                }
            }
            resultStream
        } catch (e: MuPdfPasswordException) {
            messageFile("#password", file.name)
        } catch (e: Exception) {
            LOGGER.error("Error get stream inner: {}", e.message, e)
            messageFile("#error", "")
        } catch (e: OutOfMemoryError) {
            AppState.get().pagesInMemory = 1
            messageFile("#error", "")
        } finally {
            sp?.edit()?.remove(imageUri.hashCode().toString())?.apply()
        }
    }

    private fun bitmapToStream(bitmap: Bitmap): ByteArrayInputStream? {
        return try {
            val os = ByteArrayOutputStream()
            val isJPG = AppState.get().imageFormat == AppState.JPG
            val format = if (isJPG) CompressFormat.JPEG else CompressFormat.PNG
            val quality = if (isJPG) 80 else 100
            bitmap.compress(format, quality, os)

            val byteArray = os.toByteArray()
            val stream = ByteArrayInputStream(byteArray)
            bitmap.recycle()
            os.close()
            stream
        } catch (e: Exception) {
            LOGGER.error("Error bitmap to stream: {}", e.message, e)
            null
        }
    }

    private fun bitmapToStreamRAW(bitmap: Bitmap): InputStream? {
        return try {
            InputStreamBitmap(bitmap)
        } catch (e: Exception) {
            LOGGER.error("Error bitmap to stream raw: {}", e.message, e)
            null
        }
    }

    private fun messageFile(msg: String, name: String): InputStream? {
        return BitmapUtils.getBookCoverWithTitle(msg, name, true)?.let { bitmapToStream(it) }
    }
}

package br.com.fenix.bilingualreader.service.parses.book

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.RectF
import br.com.ebook.foobnix.android.utils.Dips
import br.com.ebook.foobnix.ext.CacheZipUtils
import br.com.ebook.foobnix.pdf.info.ExtUtils
import br.com.ebook.foobnix.pdf.info.IMG
import br.com.ebook.foobnix.pdf.info.TintUtil
import br.com.ebook.foobnix.pdf.info.wrapper.AppState
import br.com.ebook.foobnix.sys.ImageExtractor
import br.com.ebook.foobnix.sys.TempHolder
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.exceptions.BookLoadException
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.FontUtil
import org.ebookdroid.common.cache.CacheManager
import org.ebookdroid.core.codec.CodecDocument
import org.ebookdroid.core.codec.CodecPage
import org.ebookdroid.core.codec.CodecPageInfo
import org.ebookdroid.core.codec.OutlineLink
import org.slf4j.LoggerFactory
import java.io.File

class DocumentParse(var context: Context, var path: String, var password: String = "", var fontSize: Float) : CodecDocument {

    private val mLOGGER = LoggerFactory.getLogger(DocumentParse::class.java)

    init {
        System.loadLibrary("mypdf")
        System.loadLibrary("mobi")
        init(context)

        open(path, password, fontSize)

        if (!isLoaded())
            throw BookLoadException(context.getString(R.string.reading_book_open_exception) + ": " + path)
    }

    companion object {
        fun init(context: Context) {
            val cacheDir = File(GeneralConsts.getCacheDir(context), GeneralConsts.CACHE_FOLDER.BOOKS + '/')

            Dips.init(context)
            AppState.get().load(context)
            CacheZipUtils.init(context, cacheDir)
            ExtUtils.init(context)
            IMG.init(context)
            TintUtil.init()
            CacheManager.init(context)
        }
    }


    private val mWidth: Int = Resources.getSystem().displayMetrics.widthPixels
    private val mHeight: Int = Resources.getSystem().displayMetrics.heightPixels

    private var isLoading = false
    private var isLoaded = false
    private var mCodecDocument: CodecDocument? = null


    fun getPageCount(fontSize: Int) : Int = mCodecDocument?.getPageCount(
        mWidth,
        mHeight,
        fontSize
    ) ?: 1

    fun open(path: String, password: String = "", fontSize: Float) : DocumentParse {
        try {
            clear()
            val metrics = Resources.getSystem().displayMetrics
            mCodecDocument = ImageExtractor.getNewCodecContext(
                path,
                password,
                metrics.widthPixels,
                metrics.heightPixels,
                FontUtil.pixelToDips(context, fontSize)
            )
            isLoaded = mCodecDocument != null
            return this
        } catch (e : Exception) {
            mLOGGER.error(e.message, e)
            throw BookLoadException(context.getString(R.string.reading_book_open_exception) + ": " + path)
        }
    }

    fun clear() {
        isLoaded = false
        if (mCodecDocument != null) {
            mCodecDocument!!.recycle()
            mCodecDocument = null
        }
    }

    fun isLoaded() : Boolean = isLoaded

    fun isLoading() : Boolean = isLoading
    fun isSearching() : Boolean = TempHolder.isSeaching
    fun isConverting() : Boolean = TempHolder.isConverting

    fun cancelOpen() {
        TempHolder.get().loadingCancelled
    }

    override fun getDocumentHandle(): Long {
        return mCodecDocument!!.documentHandle
    }

    override fun getPageCount(): Int {
        return mCodecDocument!!.pageCount
    }

    override fun getPageCount(w: Int, h: Int, fsize: Int): Int {
        return mCodecDocument!!.getPageCount(w, h, fsize)
    }

    override fun getPage(pageNuber: Int): CodecPage {
        return mCodecDocument!!.getPage(pageNuber)
    }

    override fun getPageInner(pageNuber: Int): CodecPage {
        return mCodecDocument!!.getPageInner(pageNuber)
    }

    override fun getUnifiedPageInfo(): CodecPageInfo {
        return mCodecDocument!!.unifiedPageInfo
    }

    override fun getPageInfo(pageNuber: Int): CodecPageInfo {
        return mCodecDocument!!.getPageInfo(pageNuber)
    }

    override fun searchText(pageNuber: Int, pattern: String?): MutableList<out RectF> {
        return mCodecDocument!!.searchText(pageNuber, pattern)
    }

    override fun getOutline(): MutableList<OutlineLink> {
        return mCodecDocument!!.outline
    }

    override fun getFootNotes(): MutableMap<String, String> {
        return mCodecDocument!!.footNotes
    }

    override fun getMediaAttachments(): MutableList<String> {
        return mCodecDocument!!.mediaAttachments
    }

    override fun recycle() {
        return mCodecDocument!!.recycle()
    }

    override fun isRecycled(): Boolean {
        return mCodecDocument!!.isRecycled
    }

    override fun getEmbeddedThumbnail(): Bitmap {
        return mCodecDocument!!.embeddedThumbnail
    }

    override fun hasChanges(): Boolean {
        return mCodecDocument!!.hasChanges()
    }

    override fun deleteAnnotation(pageNumber: Long, index: Int) {
        return mCodecDocument!!.deleteAnnotation(pageNumber, index)
    }

    override fun saveAnnotations(path: String?) {
        return mCodecDocument!!.saveAnnotations(path)
    }

    override fun documentToHtml(): String {
        return mCodecDocument!!.documentToHtml()
    }

    override fun getBookTitle(): String {
        return mCodecDocument!!.bookTitle
    }

    override fun getBookAuthor(): String {
        return mCodecDocument!!.bookAuthor
    }

    override fun getMeta(option: String?): String {
        return mCodecDocument!!.getMeta(option)
    }

}
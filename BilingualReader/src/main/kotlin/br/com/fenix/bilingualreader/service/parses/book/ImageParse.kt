package br.com.fenix.bilingualreader.service.parses.book

import android.content.Context
import android.graphics.Bitmap
import br.com.ebook.foobnix.ext.CacheZipUtils
import br.com.ebook.foobnix.pdf.info.ExtUtils
import br.com.ebook.foobnix.pdf.info.IMG
import br.com.ebook.foobnix.pdf.info.PageUrl
import br.com.ebook.foobnix.pdf.info.model.BookCSS
import br.com.ebook.foobnix.sys.ImageExtractor
import br.com.ebook.foobnix.sys.NativeLibLoader
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.constants.ReaderConsts
import org.slf4j.LoggerFactory
import java.io.File

class ImageParse(var context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(ImageParse::class.java)

    private val mImageExtractor: ImageExtractor

    init {
        NativeLibLoader.loadLibrary("mypdf")
        NativeLibLoader.loadLibrary("mobi")

        init(context)

        mImageExtractor = ImageExtractor.getInstance(context)
    }

    companion object {
        fun init(context: Context) {
            val baseCacheDir = GeneralConsts.getCacheDir(context)
            val cacheDir = File(baseCacheDir, GeneralConsts.CACHE_FOLDER.BOOKS + '/')

            CacheZipUtils.init(context, cacheDir)
            ExtUtils.init(context)
            IMG.init(context)
            BookCSS.get().load(context)
        }
    }


    fun getCoverPage(path: String, isCoverSize: Boolean): Bitmap? {
        val size = if (isCoverSize) ReaderConsts.COVER.BOOK_COVER_THUMBNAIL_WIDTH else ReaderConsts.COVER.BOOK_COVER_READER_WIDTH
        val pageHtml = PageUrl(path, 0, size, 0, false, true, 0)
        return mImageExtractor.proccessCoverPage(pageHtml)
    }
}
package br.com.fenix.bilingualreader.view.managers

import android.graphics.Bitmap
import android.net.Uri
import br.com.fenix.bilingualreader.service.parses.manga.Parse
import br.com.fenix.bilingualreader.util.constants.ReaderConsts
import br.com.fenix.bilingualreader.util.helpers.ImageUtil
import br.com.fenix.bilingualreader.util.helpers.Util

class MangaHandler(private var mParse: Parse) {
    private val HANDLER_URI = "localcomic"

    fun loadPage(pageNum: Int, targetWidth: Int = 0, targetHeight: Int = 0): Bitmap? {
        val reqWidth = if (targetWidth > 0) targetWidth else ReaderConsts.READER.MAX_PAGE_WIDTH
        val reqHeight = if (targetHeight > 0) targetHeight else ReaderConsts.READER.MAX_PAGE_HEIGHT

        val stream = mParse.getPage(pageNum) ?: return null
        val bitmap = ImageUtil.decodeInputStream(stream, reqWidth, reqHeight)
        Util.closeInputStream(stream)
        return bitmap
    }

    fun getPageUri(pageNum: Int): Uri? {
        return Uri.Builder()
            .scheme(HANDLER_URI)
            .authority("")
            .fragment(pageNum.toString())
            .build()
    }
}

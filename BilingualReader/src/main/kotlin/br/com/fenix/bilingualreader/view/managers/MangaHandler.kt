package br.com.fenix.bilingualreader.view.managers

import android.graphics.BitmapFactory
import android.net.Uri
import br.com.fenix.bilingualreader.service.parses.manga.Parse
import br.com.fenix.bilingualreader.util.constants.ReaderConsts
import br.com.fenix.bilingualreader.util.helpers.ImageUtil
import br.com.fenix.bilingualreader.util.helpers.Util
import com.squareup.picasso.Picasso
import com.squareup.picasso.Request
import com.squareup.picasso.RequestHandler

class MangaHandler(private var mParse: Parse) : RequestHandler() {
    private val HANDLER_URI = "localcomic"

    override fun canHandleRequest(request: Request): Boolean {
        return HANDLER_URI == request.uri.scheme
    }

    override fun load(request: Request, networkPolicy: Int): Result {
        val pageNum = request.uri.fragment!!.toInt()

        val reqWidth = if (request.targetWidth > 0) request.targetWidth else ReaderConsts.READER.MAX_PAGE_WIDTH
        val reqHeight = if (request.targetHeight > 0) request.targetHeight else ReaderConsts.READER.MAX_PAGE_HEIGHT

        val options = BitmapFactory.Options()

        options.inJustDecodeBounds = true
        var stream = mParse.getPage(pageNum)
        BitmapFactory.decodeStream(stream, null, options)
        Util.closeInputStream(stream)

        options.inSampleSize = ImageUtil.calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false

        stream = mParse.getPage(pageNum)
        val bitmap = BitmapFactory.decodeStream(stream, null, options)
        Util.closeInputStream(stream)

        return Result(bitmap, Picasso.LoadedFrom.DISK)
    }

    fun getPageUri(pageNum: Int): Uri? {
        return Uri.Builder()
            .scheme(HANDLER_URI)
            .authority("")
            .fragment(pageNum.toString())
            .build()
    }
}

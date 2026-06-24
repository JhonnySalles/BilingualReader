package br.com.fenix.bilingualreader.service.ocr

import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI

interface TesseractApi {
    fun init(datapath: String, language: String): Boolean
    fun setImage(bitmap: Bitmap)
    val utF8Text: String
    fun recycle()
}

class TesseractApiImpl : TesseractApi {
    private val api = TessBaseAPI()
    override fun init(datapath: String, language: String): Boolean = api.init(datapath, language)
    override fun setImage(bitmap: Bitmap) = api.setImage(bitmap)
    override val utF8Text: String get() = api.utF8Text
    override fun recycle() = api.recycle()
}

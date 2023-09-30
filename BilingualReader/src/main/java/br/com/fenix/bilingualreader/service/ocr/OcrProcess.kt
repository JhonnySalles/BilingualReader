package br.com.fenix.bilingualreader.service.ocr

import android.graphics.Bitmap
import br.com.fenix.bilingualreader.model.enums.Languages

interface OcrProcess {
    fun getImage(): Bitmap?
    fun getImage(x: Int, y: Int, width: Int, height: Int): Bitmap?
    fun getLanguage(): Languages?
    fun setText(text: String?)
    fun setText(text: ArrayList<String>)
    fun clearList()
}
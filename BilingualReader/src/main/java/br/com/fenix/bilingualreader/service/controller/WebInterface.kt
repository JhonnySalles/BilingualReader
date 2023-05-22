package br.com.fenix.bilingualreader.service.controller

import android.content.Context
import android.webkit.JavascriptInterface
import br.com.fenix.bilingualreader.service.japanese.Formatter

class WebInterface(var context: Context) {
    @JavascriptInterface
    fun showPopupVocabulary(id: Long) {
        System.out.println("Vocabulary click " + id)
        return
    }

    @JavascriptInterface
    fun showPopupKanji(kanji: String) {
        Formatter.getPopupKanji(context, kanji)
        return
    }
}
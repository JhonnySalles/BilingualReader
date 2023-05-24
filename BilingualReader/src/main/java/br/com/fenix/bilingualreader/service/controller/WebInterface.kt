package br.com.fenix.bilingualreader.service.controller

import android.app.Activity
import android.content.Context
import android.webkit.JavascriptInterface
import br.com.fenix.bilingualreader.service.japanese.Formatter

class WebInterface(val activity: Activity, val context: Context) {
    @JavascriptInterface
    fun showPopupVocabulary(id: Long) {
        activity.runOnUiThread{
            Formatter.getPopupVocabulary(context, id)
        }
    }

    @JavascriptInterface
    fun showPopupKanji(kanji: String) {
        activity.runOnUiThread {
            Formatter.getPopupKanji(context, kanji)
        }
    }
}
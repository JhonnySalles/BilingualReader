package br.com.fenix.bilingualreader.service.controller

import android.app.Activity
import android.content.Context
import android.webkit.JavascriptInterface
import br.com.fenix.bilingualreader.view.ui.popup.PopupKanji
import br.com.fenix.bilingualreader.view.ui.popup.PopupVocabulary

class WebInterface(val activity: Activity, context: Context) {
    private val mPopupKanji = PopupKanji(context)
    private val mPopupVocabulary = PopupVocabulary(context, false)

    @JavascriptInterface
    fun showPopupVocabulary(id: Long) {
        activity.runOnUiThread{
            mPopupVocabulary.getPopupVocabulary(id)
        }
    }

    @JavascriptInterface
    fun showPopupKanji(kanji: String) {
        activity.runOnUiThread {
            mPopupKanji.getPopupKanji(kanji)
        }
    }
}
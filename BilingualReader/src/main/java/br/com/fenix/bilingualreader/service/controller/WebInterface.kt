package br.com.fenix.bilingualreader.service.controller

import android.content.Context
import android.webkit.JavascriptInterface
import br.com.fenix.bilingualreader.model.entity.Vocabulary
import br.com.fenix.bilingualreader.service.kanji.Formatter
import br.com.fenix.bilingualreader.service.repository.VocabularyRepository

class WebInterface(var context: Context) {
    private val mRepository = VocabularyRepository(context)

    @JavascriptInterface
    fun formatJapanese(text: String): String = Formatter.generateHtmlText(text)

    @JavascriptInterface
    fun showPopupKanji(kanji: String) = Formatter.showPopupKanji(context, kanji)

    @JavascriptInterface
    fun getVocabulary(vocabulary: String): Vocabulary? {
        return mRepository.find(vocabulary)
    }
}
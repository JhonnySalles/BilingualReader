package br.com.fenix.bilingualreader.service.controller

import android.content.Context
import android.webkit.JavascriptInterface
import br.com.fenix.bilingualreader.model.entity.Vocabulary
import br.com.fenix.bilingualreader.service.repository.VocabularyRepository
import br.com.fenix.bilingualreader.service.vocabulary.FormatterVocabulary

class WebInterface(var context: Context) {
    private val mRepository = VocabularyRepository(context)

    @JavascriptInterface
    fun formatJapanese(text: String, withFurigana: Boolean): String = FormatterVocabulary.generateHtmlText(text, withFurigana)

    @JavascriptInterface
    fun getVocabulary(vocabulary: String): Vocabulary? {
        return mRepository.find(vocabulary)
    }
}
package br.com.fenix.bilingualreader.view.ui.popup

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ListView
import android.widget.TextView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Kanjax
import br.com.fenix.bilingualreader.model.entity.Vocabulary
import br.com.fenix.bilingualreader.service.japanese.Formatter
import br.com.fenix.bilingualreader.view.adapter.popup.VocabularyKanjiAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PopupVocabulary(val context: Context, isFormatterInitialize: Boolean = true) {

    init {
        if (isFormatterInitialize)
            Formatter.initializeAsync(context)
    }

    fun getPopupVocabulary(id: Long) {
        val vocabulary = Formatter.getVocabulary(id)
        val popup = createVocabularyPopup(context, LayoutInflater.from(context), vocabulary)
        MaterialAlertDialogBuilder(context, R.style.AppCompatMaterialAlertDialog)
            .setView(popup)
            .setCancelable(true)
            .setPositiveButton(R.string.action_neutral) { _, _ -> }
            .create()
            .show()
    }

    private fun createVocabularyPopup(
        context: Context,
        inflater: LayoutInflater,
        vocabulary: Vocabulary?
    ): View? {
        val root = inflater.inflate(R.layout.popup_vocabulary, null, false)

        vocabulary?.let {
            root.findViewById<TextView>(R.id.popup_vocabulary_title).text = it.word.map { w -> w }.joinToString(separator = "\n")
            root.findViewById<TextView>(R.id.popup_vocabulary_reading).text = it.reading?.map { w -> w }?.joinToString(separator = "\n") ?: ""

            root.findViewById<TextView>(R.id.popup_vocabulary_meaning_portuguese).text = it.portuguese
            root.findViewById<TextView>(R.id.popup_vocabulary_meaning_english).text = it.english

            val array = mutableListOf<Kanjax>()
            for (word in it.word)
                Formatter.getKanjax("$word")?.let { k -> array.add(k) }

            val list = root.findViewById<ListView>(R.id.popup_vocabulary_kanji_list)
            list.adapter = VocabularyKanjiAdapter(context, R.layout.line_card_book_search_history, array)
        }

        return root
    }
}
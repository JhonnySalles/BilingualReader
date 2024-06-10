package br.com.fenix.bilingualreader.view.ui.popup

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Kanjax
import br.com.fenix.bilingualreader.model.entity.Vocabulary
import br.com.fenix.bilingualreader.service.japanese.Formatter
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil.ThemeUtils.getColorFromAttr
import br.com.fenix.bilingualreader.view.adapter.popup.VocabularyKanjiAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.slf4j.LoggerFactory

class PopupVocabulary(val context: Context, isFormatterInitialize: Boolean = true) {

    private val mLOGGER = LoggerFactory.getLogger(PopupVocabulary::class.java)

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

    private fun createVocabularyPopup(context: Context, inflater: LayoutInflater, vocabulary: Vocabulary?): View? {
        val root = inflater.inflate(R.layout.popup_vocabulary, null, false)

        vocabulary?.let {
            when (it.jlpt) {
                1 -> {
                    root.findViewById<TextView>(R.id.popup_vocabulary_title).setTextColor(Formatter.COLOR_N1)
                    root.findViewById<TextView>(R.id.popup_vocabulary_jlpt).setTextColor(Formatter.COLOR_N1)
                }
                2 -> {
                    root.findViewById<TextView>(R.id.popup_vocabulary_title).setTextColor(Formatter.COLOR_N2)
                    root.findViewById<TextView>(R.id.popup_vocabulary_jlpt).setTextColor(Formatter.COLOR_N2)
                }
                3 -> {
                    root.findViewById<TextView>(R.id.popup_vocabulary_title).setTextColor(Formatter.COLOR_N3)
                    root.findViewById<TextView>(R.id.popup_vocabulary_jlpt).setTextColor(Formatter.COLOR_N3)
                }
                4 -> {
                    root.findViewById<TextView>(R.id.popup_vocabulary_title).setTextColor(Formatter.COLOR_N4)
                    root.findViewById<TextView>(R.id.popup_vocabulary_jlpt).setTextColor(Formatter.COLOR_N4)
                }
                5 -> {
                    root.findViewById<TextView>(R.id.popup_vocabulary_title).setTextColor(Formatter.COLOR_N5)
                    root.findViewById<TextView>(R.id.popup_vocabulary_jlpt).setTextColor(Formatter.COLOR_N5)
                }
                else -> {
                    val color = context.getColorFromAttr(R.attr.colorOnPrimary)
                    root.findViewById<TextView>(R.id.popup_vocabulary_title).setTextColor(color)
                    root.findViewById<TextView>(R.id.popup_vocabulary_jlpt).setTextColor(color)
                }
            }

            root.findViewById<TextView>(R.id.popup_vocabulary_title).text = it.word.map { w -> w }.joinToString(separator = "\n")

            root.findViewById<TextView>(R.id.popup_vocabulary_meaning_portuguese).text = it.portuguese
            root.findViewById<TextView>(R.id.popup_vocabulary_meaning_english).text = it.english

            root.findViewById<TextView>(R.id.popup_vocabulary_jlpt).text = context.getString(R.string.popup_vocabulary_jlpt, it.jlpt)
            root.findViewById<TextView>(R.id.popup_vocabulary_appear).text = context.getString(R.string.popup_vocabulary_appears, it.appears)
            root.findViewById<TextView>(R.id.popup_vocabulary_basic_form).text = context.getString(R.string.popup_vocabulary_basic_form, it.basicForm)
            root.findViewById<TextView>(R.id.popup_vocabulary_reading).text = context.getString(R.string.popup_vocabulary_reading, it.reading)

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
package br.com.fenix.bilingualreader.view.ui.popup

import android.content.Context
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.text.HtmlCompat
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Kanjax
import br.com.fenix.bilingualreader.service.japanese.Formatter
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PopupKanji(val context: Context, isFormatterInitialize: Boolean = true) {
    init {
        if (isFormatterInitialize)
            Formatter.initializeAsync(context)
    }

    fun getPopupKanji(kanji: String) {
        val kanjax = Formatter.getKanjax(kanji)
        val popup = createKanjiPopup(context, LayoutInflater.from(context), kanjax)
        MaterialAlertDialogBuilder(context, R.style.AppCompatMaterialAlertDialog)
            .setView(popup)
            .setCancelable(true)
            .setPositiveButton(R.string.action_neutral) { _, _ -> }
            .create()
            .show()
    }

    private fun createKanjiPopup(
        context: Context,
        inflater: LayoutInflater,
        kanjax: Kanjax?
    ): View? {
        val root = inflater.inflate(R.layout.popup_kanji, null, false)

        kanjax?.let {
            when (it.jlpt) {
                1 -> {
                    root.findViewById<TextView>(R.id.popup_kanji_jlpt).setTextColor(Formatter.COLOR_N1)
                    root.findViewById<LinearLayout>(R.id.popup_kanji_title_content).setBackgroundColor(
                        Formatter.COLOR_N1
                    )
                }
                2 -> {
                    root.findViewById<TextView>(R.id.popup_kanji_jlpt).setTextColor(Formatter.COLOR_N2)
                    root.findViewById<LinearLayout>(R.id.popup_kanji_title_content).setBackgroundColor(
                        Formatter.COLOR_N2
                    )
                }
                3 -> {
                    root.findViewById<TextView>(R.id.popup_kanji_jlpt).setTextColor(Formatter.COLOR_N3)
                    root.findViewById<LinearLayout>(R.id.popup_kanji_title_content).setBackgroundColor(
                        Formatter.COLOR_N3
                    )
                }
                4 -> {
                    root.findViewById<TextView>(R.id.popup_kanji_jlpt).setTextColor(Formatter.COLOR_N4)
                    root.findViewById<LinearLayout>(R.id.popup_kanji_title_content).setBackgroundColor(
                        Formatter.COLOR_N4
                    )
                }
                5 -> {
                    root.findViewById<TextView>(R.id.popup_kanji_jlpt).setTextColor(Formatter.COLOR_N5)
                    root.findViewById<LinearLayout>(R.id.popup_kanji_title_content).setBackgroundColor(
                        Formatter.COLOR_N5
                    )
                }
                else -> {
                    root.findViewById<TextView>(R.id.popup_kanji_jlpt).setTextColor(Formatter.COLOR_ANOTHER)
                    root.findViewById<LinearLayout>(R.id.popup_kanji_title_content).setBackgroundColor(
                        Formatter.COLOR_ANOTHER
                    )
                }
            }

            root.findViewById<TextView>(R.id.popup_kanji_title).text = it.kanji
            root.findViewById<TextView>(R.id.popup_kanji_meaning_portuguese).text = it.meaningPt
            root.findViewById<TextView>(R.id.popup_kanji_meaning_english).text = it.meaning
            root.findViewById<TextView>(R.id.popup_kanji_jlpt).text = context.getString(R.string.kanji_jlpt, it.jlpt)

            root.findViewById<TextView>(R.id.popup_kanji_grade).text = context.getString(R.string.kanji_grade, it.grade)
            root.findViewById<TextView>(R.id.popup_kanji_strokes).text = context.getString(R.string.kanji_strokes, it.strokes)
            root.findViewById<TextView>(R.id.popup_kanji_frequence).text = context.getString(R.string.kanji_frequency, it.frequence)
            root.findViewById<TextView>(R.id.popup_kanji_variant).text = context.getString(R.string.kanji_variants, it.variants)
            root.findViewById<TextView>(R.id.popup_kanji_parts).text = context.getString(R.string.kanji_parts, it.parts)
            root.findViewById<TextView>(R.id.popup_kanji_radical).text = context.getString(R.string.kanji_radical, it.radical)
            root.findViewById<TextView>(R.id.popup_kanji_memory_phrase_one).text = Html.fromHtml(it.koohii, HtmlCompat.FROM_HTML_MODE_LEGACY)
            root.findViewById<TextView>(R.id.popup_kanji_memory_phrase_two).text = Html.fromHtml(it.koohii2, HtmlCompat.FROM_HTML_MODE_LEGACY)
            root.findViewById<TextView>(R.id.popup_kanji_on_yomi_title).text = context.getString(
                R.string.kanji_onYomi, it.kunYomi)
            root.findViewById<TextView>(R.id.popup_kanji_on_yomi_list).text = Html.fromHtml(it.kunWords, HtmlCompat.FROM_HTML_MODE_LEGACY)
            root.findViewById<TextView>(R.id.popup_kanji_kun_yomi_title).text = context.getString(
                R.string.kanji_kunYomi, it.onYomi)
            root.findViewById<TextView>(R.id.popup_kanji_kun_yomi_list).text = Html.fromHtml(it.onWords, HtmlCompat.FROM_HTML_MODE_LEGACY)
        }

        return root
    }
}
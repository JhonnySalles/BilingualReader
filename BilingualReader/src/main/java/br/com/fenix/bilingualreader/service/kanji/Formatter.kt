package br.com.fenix.bilingualreader.service.kanji

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.text.*
import android.text.style.ClickableSpan
import android.text.style.RelativeSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.text.HtmlCompat
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Kanjax
import br.com.fenix.bilingualreader.service.repository.KanjaxRepository
import br.com.fenix.bilingualreader.service.repository.KanjiRepository
import br.com.fenix.bilingualreader.util.helpers.JapaneseCharacter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory


class Formatter {
    companion object KANJI {
        private val mLOGGER = LoggerFactory.getLogger(Formatter::class.java)
        private var mRepository: KanjaxRepository? = null
        private val mPattern = Regex(".*[\u4E00-\u9FFF].*")

        @TargetApi(26)
        var mSudachiTokenizer: com.worksap.nlp.sudachi.Tokenizer? = null
        var mKuromojiTokenizer: com.atilika.kuromoji.ipadic.Tokenizer? = null
        private var JLPT: Map<String, Int>? = null
        private var ANOTHER: Int = 0
        private var N1: Int = 0
        private var N2: Int = 0
        private var N3: Int = 0
        private var N4: Int = 0
        private var N5: Int = 0
        private var VOCABULARY: Int = 0

        fun initializeAsync(context: Context) =
            runBlocking { // this: CoroutineScope
                GlobalScope.async { // launch a new coroutine and continue
                    try {
                        mRepository = KanjaxRepository(context)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            mSudachiTokenizer = br.com.fenix.bilingualreader.service.tokenizers.SudachiTokenizer(context).tokenizer
                        else
                            mKuromojiTokenizer = com.atilika.kuromoji.ipadic.Tokenizer()

                        val repository = KanjiRepository(context)
                        JLPT = repository.getHashMap()

                        ANOTHER = context.getColor(R.color.JLPT0)
                        N1 = context.getColor(R.color.JLPT1)
                        N2 = context.getColor(R.color.JLPT2)
                        N3 = context.getColor(R.color.JLPT3)
                        N4 = context.getColor(R.color.JLPT4)
                        N5 = context.getColor(R.color.JLPT5)
                        VOCABULARY = context.getColor(R.color.VOCABULARY)
                    } catch (e: Exception) {
                        mLOGGER.warn("Error in open tokenizer file." + e.message, e)
                    }
                }
            }

        private fun getPopupKanjiAlert(kanji: String, setContentAlert: (SpannableString, SpannableString) -> (Unit)) {
            val kanjax = mRepository?.get(kanji)
            val title = SpannableString(kanji)
            title.setSpan(RelativeSizeSpan(3f), 0, kanji.length, 0)

            var middle = ""
            var bottom = ""
            var description = SpannableString(middle + bottom)
            if (kanjax != null) {
                middle = kanjax.keyword + "  -  " + kanjax.keywordPt + "\n\n" +
                        kanjax.meaning + " | " + kanjax.meaningPt + "\n"

                middle += "onYomi: " + kanjax.onYomi + " | nKunYomi: " + kanjax.kunYomi + "\n"

                bottom =
                    "jlpt: " + kanjax.jlpt + " grade: " + kanjax.grade + " frequency: " + kanjax.frequence + "\n"

                description = SpannableString(middle + bottom)
                description.setSpan(RelativeSizeSpan(1.2f), 0, middle.length, 0)
                description.setSpan(
                    RelativeSizeSpan(0.8f),
                    middle.length,
                    middle.length + bottom.length,
                    0
                )
            }
            setContentAlert(title, description)
        }

        private fun getPopupKanji(context: Context, kanji: String) {
            val kanjax = mRepository?.get(kanji)
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
            val root = inflater.inflate(R.layout.fragment_kanji, null, false)

            kanjax?.let {
                when (it.jlpt) {
                    1 -> {
                        root.findViewById<TextView>(R.id.kanji_jlpt).setTextColor(N1)
                        root.findViewById<LinearLayout>(R.id.kanji_title_content).setBackgroundColor(N1)
                    }
                    2 -> {
                        root.findViewById<TextView>(R.id.kanji_jlpt).setTextColor(N2)
                        root.findViewById<LinearLayout>(R.id.kanji_title_content).setBackgroundColor(N2)
                    }
                    3 -> {
                        root.findViewById<TextView>(R.id.kanji_jlpt).setTextColor(N3)
                        root.findViewById<LinearLayout>(R.id.kanji_title_content).setBackgroundColor(N3)
                    }
                    4 -> {
                        root.findViewById<TextView>(R.id.kanji_jlpt).setTextColor(N4)
                        root.findViewById<LinearLayout>(R.id.kanji_title_content).setBackgroundColor(N4)
                    }
                    5 -> {
                        root.findViewById<TextView>(R.id.kanji_jlpt).setTextColor(N5)
                        root.findViewById<LinearLayout>(R.id.kanji_title_content).setBackgroundColor(N5)
                    }
                    else -> {
                        root.findViewById<TextView>(R.id.kanji_jlpt).setTextColor(ANOTHER)
                        root.findViewById<LinearLayout>(R.id.kanji_title_content).setBackgroundColor(ANOTHER)
                    }
                }

                root.findViewById<TextView>(R.id.kanji_title).text = it.kanji
                root.findViewById<TextView>(R.id.kanji_meaning_portuguese).text = it.meaningPt
                root.findViewById<TextView>(R.id.kanji_meaning_english).text = it.meaning
                root.findViewById<TextView>(R.id.kanji_jlpt).text = context.getString(R.string.kanji_jlpt, it.jlpt)

                root.findViewById<TextView>(R.id.kanji_grade).text = context.getString(R.string.kanji_grade, it.grade)
                root.findViewById<TextView>(R.id.kanji_strokes).text = context.getString(R.string.kanji_strokes, it.strokes)
                root.findViewById<TextView>(R.id.kanji_frequence).text = context.getString(R.string.kanji_frequency, it.frequence)
                root.findViewById<TextView>(R.id.kanji_variant).text = context.getString(R.string.kanji_variants, it.variants)
                root.findViewById<TextView>(R.id.kanji_parts).text = context.getString(R.string.kanji_parts, it.parts)
                root.findViewById<TextView>(R.id.kanji_radical).text = context.getString(R.string.kanji_radical, it.radical)
                root.findViewById<TextView>(R.id.kanji_memory_phrase_one).text = Html.fromHtml(it.koohii, HtmlCompat.FROM_HTML_MODE_LEGACY)
                root.findViewById<TextView>(R.id.kanji_memory_phrase_two).text = Html.fromHtml(it.koohii2, HtmlCompat.FROM_HTML_MODE_LEGACY)
                root.findViewById<TextView>(R.id.kanji_on_yomi_title).text = context.getString(R.string.kanji_onYomi, it.kunYomi)
                root.findViewById<TextView>(R.id.kanji_on_yomi_list).text = Html.fromHtml(it.kunWords, HtmlCompat.FROM_HTML_MODE_LEGACY)
                root.findViewById<TextView>(R.id.kanji_kun_yomi_title).text = context.getString(R.string.kanji_kunYomi, it.onYomi)
                root.findViewById<TextView>(R.id.kanji_kun_yomi_list).text = Html.fromHtml(it.onWords, HtmlCompat.FROM_HTML_MODE_LEGACY)
            }

            return root
        }

        private fun generateFurigana(furigana: String): SpannableStringBuilder {
            val furiganaBuilder = SpannableStringBuilder(furigana)
            furiganaBuilder.setSpan(
                RelativeSizeSpan(0.75f),
                0, furiganaBuilder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            return furiganaBuilder
        }

        private fun generateClick(text: String, click: (String) -> (Unit)): ClickableSpan {
            return object : ClickableSpan() {
                override fun onClick(p0: View) {
                    click(text)
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false
                    ds.color = VOCABULARY
                }
            }
        }

        private fun kuromojiTokenizer(text: String, vocabularyClick: (String) -> (Unit)): SpannableStringBuilder {
            val textBuilder = SpannableStringBuilder()
            textBuilder.append(text)
            for (t in mKuromojiTokenizer!!.tokenize(text)) {
                if (t.surface.isNotEmpty() && t.surface.matches(mPattern)
                ) {
                    var furigana = ""
                    for (c in t.reading)
                        furigana += JapaneseCharacter.toHiragana(c)

                    textBuilder.setSpan(
                        SuperRubySpan(
                            generateFurigana(furigana),
                            SuperReplacementSpan.Alignment.CENTER,
                            SuperReplacementSpan.Alignment.CENTER
                        ),
                        t.position, t.position + t.surface.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    textBuilder.setSpan(
                        generateClick(t.baseForm, vocabularyClick),
                        t.position, t.position + t.surface.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            return textBuilder
        }

        @TargetApi(26)
        private fun sudachiTokenizer(text: String, vocabularyClick: (String) -> (Unit)): SpannableStringBuilder {
            val textBuilder = SpannableStringBuilder()
            textBuilder.append(text)
            if (mSudachiTokenizer != null) {
                for (t in mSudachiTokenizer!!.tokenize(com.worksap.nlp.sudachi.Tokenizer.SplitMode.C, text)) {
                    if (t.readingForm().isNotEmpty() && t.surface().matches(mPattern)
                    ) {
                        var furigana = ""
                        for (c in t.readingForm())
                            furigana += JapaneseCharacter.toHiragana(c)

                        textBuilder.setSpan(
                            SuperRubySpan(
                                generateFurigana(furigana),
                                SuperReplacementSpan.Alignment.CENTER,
                                SuperReplacementSpan.Alignment.CENTER
                            ),
                            t.begin(), t.end(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                        textBuilder.setSpan(
                            generateClick(t.dictionaryForm(), vocabularyClick),
                            t.begin(), t.end(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }
            }

            return textBuilder
        }

        fun generateFurigana(text: String, furigana: (CharSequence) -> (Unit), vocabularyClick: (String) -> (Unit)) {
            if (text.isEmpty()) {
                furigana(text)
                return
            }

            val textBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                sudachiTokenizer(text, vocabularyClick)
            else
                kuromojiTokenizer(text, vocabularyClick)

            furigana(textBuilder)
        }

        fun generateKanjiColor(context: Context, texts: ArrayList<String>): ArrayList<SpannableString> {
            val array = arrayListOf<SpannableString>()
            for (text in texts)
                generateKanjiColor(context, text) { array.add(it) }

            return array
        }

        fun generateKanjiColor(
            text: String,
            function: (SpannableString) -> (Unit),
            callAlert: (SpannableString, SpannableString) -> (Unit)
        ) {
            if (text.isEmpty()) {
                function(SpannableString(text))
                return
            }

            val ss = SpannableString(text)
            ss.forEachIndexed { index, element ->
                val kanji = element.toString()
                if (kanji.matches(mPattern)) {
                    val color = when (JLPT?.get(kanji)) {
                        1 -> N1
                        2 -> N2
                        3 -> N3
                        4 -> N4
                        5 -> N5
                        else -> ANOTHER
                    }

                    val cs = object : ClickableSpan() {
                        override fun onClick(p0: View) {
                            getPopupKanjiAlert(kanji, callAlert)
                        }

                        override fun updateDrawState(ds: TextPaint) {
                            super.updateDrawState(ds)
                            ds.isUnderlineText = false
                            ds.color = color
                        }
                    }
                    ss.setSpan(cs, index, index + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }

            function(ss)
        }

        fun generateKanjiColor(
            context: Context,
            text: String,
            function: (SpannableString) -> (Unit)
        ) {
            if (text.isEmpty()) {
                function(SpannableString(text))
                return
            }

            val ss = SpannableString(text)
            ss.forEachIndexed { index, element ->
                val kanji = element.toString()
                if (kanji.matches(mPattern)) {
                    val color = when (JLPT?.get(kanji)) {
                        1 -> N1
                        2 -> N2
                        3 -> N3
                        4 -> N4
                        5 -> N5
                        else -> ANOTHER
                    }

                    val cs = object : ClickableSpan() {
                        override fun onClick(p0: View) {
                            getPopupKanji(context, kanji)
                        }

                        override fun updateDrawState(ds: TextPaint) {
                            super.updateDrawState(ds)
                            ds.isUnderlineText = false
                            ds.color = color
                        }
                    }
                    ss.setSpan(cs, index, index + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }

            function(ss)
        }
    }
}
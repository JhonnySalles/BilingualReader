package br.com.fenix.bilingualreader.service.japanese

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
import br.com.fenix.bilingualreader.service.repository.VocabularyRepository
import br.com.fenix.bilingualreader.util.helpers.JapaneseCharacter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory


class Formatter {
    companion object JAPANESE {
        private val mLOGGER = LoggerFactory.getLogger(Formatter::class.java)
        private var mKanjaxRepository: KanjaxRepository? = null
        private var mVocabularyRepository: VocabularyRepository? = null
        private val mPatternKanji = Regex(".*[\u4E00-\u9FFF].*")

                                                        // Hiragana | katakana | kanji
        private val mPatternJapanese = Regex(".*[\u3040-\u309F|\u30A0-\u30FF|\u4E00-\u9FFF].*")

        @TargetApi(26)
        var mSudachiTokenizer: com.worksap.nlp.sudachi.Tokenizer? = null
        var mKuromojiTokenizer: com.atilika.kuromoji.ipadic.Tokenizer? = null

        // Kanji
        private var JLPT: Map<String, Int>? = null
        private var COLOR_ANOTHER: Int = 0
        private var COLOR_N1: Int = 0
        private var COLOR_N2: Int = 0
        private var COLOR_N3: Int = 0
        private var COLOR_N4: Int = 0
        private var COLOR_N5: Int = 0
        private var COLOR_VOCABULARY: Int = 0

        // Vocabulary
        private var HTML_ANOTHER: String = ""
        private var HTML_N1: String = ""
        private var HTML_N2: String = ""
        private var HTML_N3: String = ""
        private var HTML_N4: String = ""
        private var HTML_N5: String = ""

        fun initializeAsync(context: Context) =
            runBlocking { // this: CoroutineScope
                launch { // launch a new coroutine and continue
                    try {
                        mKanjaxRepository = KanjaxRepository(context)
                        mVocabularyRepository = VocabularyRepository(context)

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            mSudachiTokenizer = br.com.fenix.bilingualreader.service.tokenizers.SudachiTokenizer(context).tokenizer
                        else
                            mKuromojiTokenizer = com.atilika.kuromoji.ipadic.Tokenizer()

                        val repository = KanjiRepository(context)
                        JLPT = repository.getHashMap()

                        COLOR_ANOTHER = context.getColor(R.color.JLPT0)
                        COLOR_N1 = context.getColor(R.color.JLPT1)
                        COLOR_N2 = context.getColor(R.color.JLPT2)
                        COLOR_N3 = context.getColor(R.color.JLPT3)
                        COLOR_N4 = context.getColor(R.color.JLPT4)
                        COLOR_N5 = context.getColor(R.color.JLPT5)
                        COLOR_VOCABULARY = context.getColor(R.color.VOCABULARY)

                        HTML_ANOTHER = "#b3b3b3"
                        HTML_N1 = "#ff4d4d"
                        HTML_N2 = "#ffc266"
                        HTML_N3 = "#66ff66"
                        HTML_N4 = "#668cff"
                        HTML_N5 = "#b366ff"
                    } catch (e: Exception) {
                        mLOGGER.warn("Error in open tokenizer file." + e.message, e)
                    }
                }
            }

        // --------------------------------------------------------- Kanji ---------------------------------------------------------

        private fun getPopupKanjiAlert(kanji: String, setContentAlert: (SpannableString, SpannableString) -> (Unit)) {
            val kanjax = mKanjaxRepository?.get(kanji)
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

        fun getPopupKanji(context: Context, kanji: String) {
            val kanjax = mKanjaxRepository?.get(kanji)
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
                        root.findViewById<TextView>(R.id.kanji_jlpt).setTextColor(COLOR_N1)
                        root.findViewById<LinearLayout>(R.id.kanji_title_content).setBackgroundColor(COLOR_N1)
                    }
                    2 -> {
                        root.findViewById<TextView>(R.id.kanji_jlpt).setTextColor(COLOR_N2)
                        root.findViewById<LinearLayout>(R.id.kanji_title_content).setBackgroundColor(COLOR_N2)
                    }
                    3 -> {
                        root.findViewById<TextView>(R.id.kanji_jlpt).setTextColor(COLOR_N3)
                        root.findViewById<LinearLayout>(R.id.kanji_title_content).setBackgroundColor(COLOR_N3)
                    }
                    4 -> {
                        root.findViewById<TextView>(R.id.kanji_jlpt).setTextColor(COLOR_N4)
                        root.findViewById<LinearLayout>(R.id.kanji_title_content).setBackgroundColor(COLOR_N4)
                    }
                    5 -> {
                        root.findViewById<TextView>(R.id.kanji_jlpt).setTextColor(COLOR_N5)
                        root.findViewById<LinearLayout>(R.id.kanji_title_content).setBackgroundColor(COLOR_N5)
                    }
                    else -> {
                        root.findViewById<TextView>(R.id.kanji_jlpt).setTextColor(COLOR_ANOTHER)
                        root.findViewById<LinearLayout>(R.id.kanji_title_content).setBackgroundColor(COLOR_ANOTHER)
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
                    ds.color = COLOR_VOCABULARY
                }
            }
        }

        private fun kuromojiTokenizer(text: String, vocabularyClick: (String) -> (Unit)): SpannableStringBuilder {
            val textBuilder = SpannableStringBuilder()
            textBuilder.append(text)
            for (t in mKuromojiTokenizer!!.tokenize(text)) {
                if (t.surface.isNotEmpty() && t.surface.matches(mPatternKanji)) {
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
                    if (t.readingForm().isNotEmpty() && t.surface().matches(mPatternKanji)) {
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
                if (kanji.matches(mPatternKanji)) {
                    val color = when (JLPT?.get(kanji)) {
                        1 -> COLOR_N1
                        2 -> COLOR_N2
                        3 -> COLOR_N3
                        4 -> COLOR_N4
                        5 -> COLOR_N5
                        else -> COLOR_ANOTHER
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
                if (kanji.matches(mPatternKanji)) {
                    val color = when (JLPT?.get(kanji)) {
                        1 -> COLOR_N1
                        2 -> COLOR_N2
                        3 -> COLOR_N3
                        4 -> COLOR_N4
                        5 -> COLOR_N5
                        else -> COLOR_ANOTHER
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

        // --------------------------------------------------------- Vocabulary ---------------------------------------------------------

        fun getCss(): String {
            val colors = ".n0 {color: $HTML_ANOTHER } " +
                    ".n1 {color: $HTML_N1 } " +
                    ".n2 {color: $HTML_N2 } " +
                    ".n3 {color: $HTML_N3 } " +
                    ".n4 {color: $HTML_N4 } " +
                    ".n5 {color: $HTML_N5 } "
            return colors
        }

        private fun kuromojiTokenizerHtml(text: String, withFurigana: Boolean): String {
            if (mKuromojiTokenizer != null) {
                var textBuilder = ""
                for (t in mKuromojiTokenizer!!.tokenize(text)) {
                    if (t.surface.isNotEmpty() && t.surface.matches(mPatternKanji)) {
                        if (withFurigana) {
                            var furigana = ""
                            for (c in t.reading)
                                furigana += JapaneseCharacter.toHiragana(c)

                            var onClick = ""
                            val vocab = mVocabularyRepository!!.find(t.baseForm)

                            if (vocab != null)
                                onClick = "ondblclick=\"bilingualapp.showPopupVocabulary(${vocab.id}); return false\""

                            var kanji = ""
                            for (c in text.substring(t.position, t.position + t.surface.length))
                                kanji += if (onClick.isEmpty())
                                    "<span class=\"n${JLPT?.get(c.toString()) ?: 0}\" ondblclick=\"bilingualapp.showPopupVocabulary($c); return false\">$c</span>"
                                else
                                    "<span class=\"n${JLPT?.get(c.toString()) ?: 0}\">$c</span>"

                            textBuilder += "<span class=\"n${vocab?.jlpt?:0}\" $onClick><ruby>$kanji<rt>$furigana</rt></ruby></span>"
                        } else {
                            var onClick = ""
                            val vocab = mVocabularyRepository!!.find(t.baseForm)
                            if (vocab != null)
                                onClick = "onclick=\"bilingualapp.showPopupVocabulary(${vocab.id}); return false\""

                            var kanji = ""
                            for (c in text.substring(t.position, t.position + t.surface.length))
                                kanji += if (onClick.isEmpty())
                                    "<span class=\"n${JLPT?.get(c.toString()) ?: 0}\" ondblclick=\"bilingualapp.showPopupVocabulary($c); return false\">$c</span>"
                                else
                                    "<span class=\"n${JLPT?.get(c.toString()) ?: 0}\">$c</span>"

                            textBuilder += "<span class=\"n${vocab?.jlpt?:0}\" $onClick>$kanji</span>"
                        }
                    } else
                        textBuilder += text.substring(t.position, t.position + t.surface.length)
                }

                return textBuilder
            } else
                return text
        }

        @TargetApi(26)
        private fun sudachiTokenizerHtml(text: String, withFurigana: Boolean): String {
            if (mSudachiTokenizer != null) {
                var textBuilder = ""
                for (t in mSudachiTokenizer!!.tokenize(
                    com.worksap.nlp.sudachi.Tokenizer.SplitMode.C,
                    text
                )) {
                    if (t.readingForm().isNotEmpty() && t.surface().matches(mPatternKanji)) {
                        if (withFurigana) {
                            var furigana = ""
                            for (c in t.readingForm())
                                furigana += JapaneseCharacter.toHiragana(c)

                            var onClick = ""
                            val vocab = mVocabularyRepository!!.find(t.dictionaryForm())
                            if (vocab != null)
                                onClick = "ondblclick=\"bilingualapp.showPopupVocabulary(${vocab.id}); return false\""

                            var kanji = ""
                            for (c in text.substring(t.begin(), t.end()))
                                kanji += if (onClick.isEmpty())
                                    "<span class=\"n${JLPT?.get(c.toString()) ?: 0}\" ondblclick=\"bilingualapp.showPopupVocabulary($c); return false\">$c</span>"
                                else
                                    "<span class=\"n${JLPT?.get(c.toString()) ?: 0}\">$c</span>"

                            textBuilder += "<span class=\"n${vocab?.jlpt?:0}\" $onClick><ruby>$kanji<rt>$furigana</rt></ruby></span>"
                        } else {
                            var onClick = ""
                            val vocab = mVocabularyRepository!!.find(t.dictionaryForm())
                            if (vocab != null)
                                onClick = "ondblclick=\"bilingualapp.showPopupVocabulary(${vocab.id}); return false\""

                            var kanji = ""
                            for (c in text.substring(t.begin(), t.end()))
                                kanji += if (onClick.isEmpty())
                                    "<span class=\"n${JLPT?.get(c.toString()) ?: 0}\" ondblclick=\"bilingualapp.showPopupVocabulary($c); return false\">$c</span>"
                                else
                                    "<span class=\"n${JLPT?.get(c.toString()) ?: 0}\">$c</span>"

                            textBuilder += "<span class=\"n${vocab?.jlpt?:0}\" $onClick>$kanji</span>"
                        }
                    } else
                        textBuilder += text.substring(t.begin(), t.end())
                }
                return textBuilder
            } else
                return text
        }

        fun generateHtmlText(text: String, withFurigana: Boolean): String {
            if (text.isEmpty() || !text.contains(mPatternJapanese))
                return text

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                sudachiTokenizerHtml(text, withFurigana)
            else
                kuromojiTokenizerHtml(text, withFurigana)
        }
    }
}
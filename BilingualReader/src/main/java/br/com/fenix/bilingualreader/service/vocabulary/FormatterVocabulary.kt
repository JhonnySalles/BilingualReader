package br.com.fenix.bilingualreader.service.vocabulary

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.text.*
import br.com.fenix.bilingualreader.service.repository.KanjiRepository
import br.com.fenix.bilingualreader.service.repository.VocabularyRepository
import br.com.fenix.bilingualreader.service.tokenizers.SudachiTokenizer
import br.com.fenix.bilingualreader.util.helpers.JapaneseCharacter
import com.atilika.kuromoji.ipadic.Tokenizer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

class FormatterVocabulary {
    companion object VOCABULARY {
        private val mLOGGER = LoggerFactory.getLogger(FormatterVocabulary::class.java)
        private var mVocabularyRepository: VocabularyRepository? = null
        private val mPatternKanji = Regex(".*[\u4E00-\u9FFF].*")
                                                      // Hiragana | katakana | kanji
        private val mPatternJapanese = Regex(".*[\u3040-\u309F|\u30A0-\u30FF|\u4E00-\u9FFF].*")

        @TargetApi(26)
        var mSudachiTokenizer: com.worksap.nlp.sudachi.Tokenizer? = null
        var mKuromojiTokenizer: com.atilika.kuromoji.ipadic.Tokenizer? = null
        private var JLPT: Map<String, Int>? = null
        private var ANOTHER: String = ""
        private var N1: String = ""
        private var N2: String = ""
        private var N3: String = ""
        private var N4: String = ""
        private var N5: String = ""

        fun initializeAsync(context: Context) =
            runBlocking { // this: CoroutineScope
                launch { // launch a new coroutine and continue
                    try {
                        mVocabularyRepository = VocabularyRepository(context)

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            mSudachiTokenizer = SudachiTokenizer(context).tokenizer
                        else
                            mKuromojiTokenizer = Tokenizer()

                        val repository = KanjiRepository(context)
                        JLPT = repository.getHashMap()

                        ANOTHER = "#b3b3b3"
                        N1 = "#ff4d4d"
                        N2 = "#ffc266"
                        N3 = "#66ff66"
                        N4 = "#668cff"
                        N5 = "#b366ff"
                    } catch (e: Exception) {
                        mLOGGER.warn("Error in open tokenizer file." + e.message, e)
                    }
                }
            }

        fun getCss(): String {
            val colors = ".n0 {color: $ANOTHER } " +
                    ".n1 {color: $N1 } " +
                    ".n2 {color: $N2 } " +
                    ".n3 {color: $N3 } " +
                    ".n4 {color: $N4 } " +
                    ".n5 {color: $N5 } "
            return colors
        }

        // bilingualapp is define when set interface
        fun getScript(withFurigana: Boolean) : String {
            val furigana = if (withFurigana) "true" else "false"
            val textProcess = " async function process() {" +
                    "   var content = document.getElementById('text');" +
                    "   var html = content.innerHTML.split('<br>');" +
                    "   var processed = '';" +
                    "   for (i in html)" +
                    "     processed += bilingualapp.formatJapanese(html[i], $furigana) + '<br>';" +
                    "   content.innerHTML = processed;" +
                    " }" +
                    " process();"
            return "<script>" +
                    textProcess +
                    "</script>"
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

                            var kanji = ""
                            for (c in text.substring(t.position, t.position + t.surface.length))
                                kanji += "<span class=\"n${JLPT?.get(c.toString()) ?: 0}\">$c</span>"

                            val jlpt = mVocabularyRepository!!.findJlpt(t.reading)
                            textBuilder += "<span class=\"n$jlpt\"><ruby>$kanji<rt>$furigana</rt></ruby></span>"
                        } else {
                            var kanji = ""
                            for (c in text.substring(t.position, t.position + t.surface.length))
                                kanji += "<span class=\"n${JLPT?.get(c.toString()) ?: 0}\">$c</span>"

                            val jlpt = mVocabularyRepository!!.findJlpt(t.reading)
                            textBuilder += "<span class=\"n$jlpt\">$kanji</span>"
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

                            var kanji = ""
                            for (c in text.substring(t.begin(), t.end()))
                                kanji += "<span class=\"n${JLPT?.get(c.toString()) ?: 0}\">$c</span>"

                            val jlpt = mVocabularyRepository!!.findJlpt(t.readingForm())
                            textBuilder += "<span class=\"n$jlpt\"><ruby>$kanji<rt>$furigana</rt></ruby></span>"
                        } else {
                            var kanji = ""
                            for (c in text.substring(t.begin(), t.end()))
                                kanji += "<span class=\"n${JLPT?.get(c.toString()) ?: 0}\">$c</span>"

                            val jlpt = mVocabularyRepository!!.findJlpt(t.readingForm())
                            textBuilder += "<span class=\"n$jlpt\">$kanji</span>"
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
package br.com.fenix.bilingualreader.util.helpers

import android.text.Html
import org.jsoup.Jsoup

/**
 * High-performance text preprocessor for LLM context preparation,
 * cleaning OCR noise, book HTML artifacts, and normalizing Japanese full-width characters.
 */
object TextPreprocessor {

    private val REGEX_CONTROL_CHARS = Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]")
    private val REGEX_LINE_BREAK = Regex("\r\n|\r")
    private val REGEX_NBSP = Regex("\\u00A0")
    private val REGEX_MULTI_SPACE = Regex("[ \\t]{2,}")
    private val REGEX_MULTI_NEWLINE = Regex("\\n{3,}")
    private val REGEX_IMAGE_TAG = Regex("<image-begin>.*?<image-end>", RegexOption.DOT_MATCHES_ALL)
    private val REGEX_IMG_TAG = Regex("<img[^>]*>", RegexOption.IGNORE_CASE)

    // OCR noise patterns
    private val REGEX_EMPTY_BRACKETS = Regex("([\\(\\[\\{【（「『])[ \\t]*([\\)\\]\\}】）」』])")
    private val REGEX_ISOLATED_SYMBOLS = Regex("(?m)^[\\|丨｜~_\\^\\-\\=\\+\\*\\#\\/\\\\\\:]{1,3}$")
    private val REGEX_EXCESSIVE_DOTS = Regex("\\.{4,}")
    private val REGEX_EXCESSIVE_DASHES = Regex("-{3,}")

    /**
     * Normalizes full-width ASCII characters (0xFF01-0xFF5E) to half-width (0x21-0x7E)
     * and full-width ideographic space (0x3000) to standard ASCII space.
     */
    fun normalizeJapaneseWidth(text: String): String {
        if (text.isEmpty()) return ""
        val sb = StringBuilder(text.length)
        for (i in 0 until text.length) {
            val c = text[i]
            when {
                c == '\u3000' -> sb.append(' ')
                c in '\uff01'..'\uff5e' -> sb.append((c.code - 0xfee0).toChar())
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    /**
     * Cleans general noise such as control characters, duplicate line breaks, and excess whitespace.
     */
    fun cleanNoise(text: String): String {
        if (text.isBlank()) return ""
        return text
            .replace(REGEX_CONTROL_CHARS, "")
            .replace(REGEX_LINE_BREAK, "\n")
            .replace(REGEX_NBSP, " ")
            .replace(REGEX_MULTI_SPACE, " ")
            .replace(REGEX_MULTI_NEWLINE, "\n\n")
            .trim()
    }

    /**
     * Filters out OCR artifacts common in manga/comic recognition (isolated noise lines, empty brackets, etc.)
     */
    fun cleanOcrArtifacts(text: String): String {
        if (text.isBlank()) return ""
        var cleaned = text
            .replace(REGEX_EMPTY_BRACKETS, "")
            .replace(REGEX_EXCESSIVE_DOTS, "...")
            .replace(REGEX_EXCESSIVE_DASHES, "--")
            .replace(REGEX_ISOLATED_SYMBOLS, "")
            
        // Filter empty lines created by artifact removal
        cleaned = cleanNoise(cleaned)
        return cleaned
    }

    /**
     * Converts book HTML to cleaned plain text.
     */
    fun cleanBookHtml(html: String): String {
        if (html.isBlank()) return ""
        val withoutImages = html
            .replace(REGEX_IMAGE_TAG, "")
            .replace(REGEX_IMG_TAG, "")
        val rawText = try {
            Jsoup.parse(withoutImages).text()
        } catch (_: Exception) {
            Html.fromHtml(withoutImages, Html.FROM_HTML_MODE_COMPACT).toString()
        }
        return cleanNoise(normalizeJapaneseWidth(rawText))
    }

    /**
     * Full pre-processing pipeline before text is indexed in RAG or passed to LLM prompts.
     */
    fun prepareForLlm(text: String, isManga: Boolean = false): String {
        if (text.isBlank()) return ""
        var result = normalizeJapaneseWidth(text)
        result = cleanNoise(result)
        if (isManga) {
            result = cleanOcrArtifacts(result)
        }
        return result
    }

    /**
     * Fast O(N) word counting without string allocations.
     */
    fun countWords(text: CharSequence): Int {
        if (text.isEmpty()) return 0
        var count = 0
        var inWord = false
        for (i in 0 until text.length) {
            if (text[i].isWhitespace()) {
                inWord = false
            } else if (!inWord) {
                inWord = true
                count++
            }
        }
        return count
    }
}

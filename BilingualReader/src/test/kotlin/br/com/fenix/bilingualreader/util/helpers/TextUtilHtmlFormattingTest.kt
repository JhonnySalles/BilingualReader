package br.com.fenix.bilingualreader.util.helpers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextUtilHtmlFormattingTest {

    @Test
    fun formatHtml_withParagraphs_preservesPTagsAndDoesNotStripThem() {
        val input = "<p>First paragraph of the book.</p><p>Second paragraph of the book.</p>"
        val result = TextUtil.formatHtml(input)

        assertTrue("Expected <p> to be preserved", result.contains("<p>First paragraph of the book.</p>"))
        assertTrue("Expected second <p> to be preserved", result.contains("<p>Second paragraph of the book.</p>"))
        assertFalse("Expected </p> not to be deleted", result.replace("</p>", "").length == result.length)
    }

    @Test
    fun formatHtml_withHeadingsAndFormatting_preservesAllSemanticTags() {
        val input = "<h1>Chapter 1: The Beginning</h1><p>This is <b>bold</b> and <i>italic</i> and <u>underlined</u>.</p>"
        val result = TextUtil.formatHtml(input)

        assertTrue(result.contains("<h1>Chapter 1: The Beginning</h1>"))
        assertTrue(result.contains("<b>bold</b>"))
        assertTrue(result.contains("<i>italic</i>"))
        assertTrue(result.contains("<u>underlined</u>"))
    }

    @Test
    fun formatHtml_withCustomEndLine_replacesParagraphBreaksWithDelimiter() {
        val input = "<h1>Title</h1><p>First paragraph.</p><p>Second paragraph.<br/>Another line.</p>"
        val result = TextUtil.formatHtml(input, endLine = "||")

        val parts = result.split("||").map { it.trim() }.filter { it.isNotEmpty() }
        assertEquals(4, parts.size)
        assertEquals("Title", parts[0])
        assertEquals("First paragraph.", parts[1])
        assertEquals("Second paragraph.", parts[2])
        assertEquals("Another line.", parts[3])
    }

    @Test
    fun formatHtml_withMuPdfCoordinates_cleansCoordinatesWhilePreservingContent() {
        val input = "<p style=\"top:12.3pt;left:45.6pt;line-height:10.0pt\">Text inside block</p>"
        val result = TextUtil.formatHtml(input)

        assertFalse("Style with top/left coordinates should be stripped", result.contains("top:12.3pt"))
        assertTrue("Content must be preserved inside <p>", result.contains("<p>Text inside block</p>"))
    }

    @Test
    fun formatHtml_withHr_replacesWithSceneDividerParagraph() {
        val input = "<p>Scene 1 ended.</p><hr/><p>Scene 2 started.</p>"
        val result = TextUtil.formatHtml(input)

        assertTrue("Expected * * * divider paragraph", result.contains("* * *"))
        assertTrue("Expected align center on divider", result.contains("align=\"center\""))
    }

    @Test
    fun formatHtml_withNoiseTags_cleansPauseTag() {
        val input = "<p>Block 1</p><pause>\n<p>Block 2</p>"
        val result = TextUtil.formatHtml(input)

        assertFalse("Expected <pause> to be stripped", result.contains("<pause>"))
        assertTrue(result.contains("<p>Block 1</p>"))
        assertTrue(result.contains("<p>Block 2</p>"))
    }

    @Test
    fun formatHtml_withLegacyEndLine_convertsToBr() {
        val input = "<p>Line 1-<end-line>Line 2<end-line></p>"
        val result = TextUtil.formatHtml(input)

        assertFalse("Expected <end-line> to be converted", result.contains("<end-line>"))
        assertTrue("Expected <br/> conversion", result.contains("<br/>"))
    }

    @Test
    fun formatHtml_withEmptyInput_returnsEmptyString() {
        assertEquals("", TextUtil.formatHtml(""))
    }

    @Test
    fun replaceHtmlTTS_withHeadingsAndParagraphs_extractsSpeechTextCleanly() {
        val input = "<h1>Chapter 1</h1><p>First sentence. Second sentence.</p>"
        val result = TextUtil.replaceHtmlTTS(input)

        assertTrue(result.contains("Chapter 1"))
        assertTrue(result.contains("First sentence."))
        assertFalse(result.contains("<h1>"))
        assertFalse(result.contains("<p>"))
    }
}

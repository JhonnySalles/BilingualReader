package br.com.fenix.bilingualreader.util.helpers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextPreprocessorTest {

    @Test
    fun testNormalizeJapaneseWidth() {
        val input = "Ｈｅｌｌｏ　１２３！"
        val normalized = TextPreprocessor.normalizeJapaneseWidth(input)
        assertEquals("Hello 123!", normalized)
    }

    @Test
    fun testCleanNoise() {
        val input = "Line 1\r\n\r\n\r\nLine 2\u00A0with  spaces\n\n\n\nLine 3"
        val cleaned = TextPreprocessor.cleanNoise(input)
        assertEquals("Line 1\n\nLine 2 with spaces\n\nLine 3", cleaned)
    }

    @Test
    fun testCleanOcrArtifacts() {
        val input = "Hello 【】 world （ ）\n|\nValid line\n---"
        val cleaned = TextPreprocessor.cleanOcrArtifacts(input)
        assertTrue(cleaned.contains("Hello world"))
        assertTrue(cleaned.contains("Valid line"))
        assertTrue(!cleaned.contains("【】"))
    }

    @Test
    fun testCountWords() {
        assertEquals(0, TextPreprocessor.countWords(""))
        assertEquals(0, TextPreprocessor.countWords("   \n\t  "))
        assertEquals(1, TextPreprocessor.countWords("word"))
        assertEquals(4, TextPreprocessor.countWords("  The quick brown fox  "))
        assertEquals(4, TextPreprocessor.countWords("The\nquick\tbrown\nfox"))
    }

    @Test
    fun testPrepareForLlm() {
        val input = "１２３　ＡＢＣ\n\n\n【 】\n|\nValid manga line"
        val prepared = TextPreprocessor.prepareForLlm(input, isManga = true)
        assertTrue(prepared.contains("123 ABC"))
        assertTrue(prepared.contains("Valid manga line"))
    }
}

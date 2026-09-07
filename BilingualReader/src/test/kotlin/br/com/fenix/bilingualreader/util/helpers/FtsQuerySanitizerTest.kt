package br.com.fenix.bilingualreader.util.helpers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FtsQuerySanitizerTest {

    @Test
    fun testSanitizeEnglishQuery() {
        val query = "What is the secret of the castle?"
        val sanitized = FtsQuerySanitizer.sanitize(query)
        assertTrue(sanitized.contains("secret*"))
        assertTrue(sanitized.contains("castle*"))
        assertTrue(!sanitized.contains("the*"))
    }

    @Test
    fun testSanitizePortugueseQuery() {
        val query = "Qual é o nome do personagem principal?"
        val sanitized = FtsQuerySanitizer.sanitize(query)
        assertTrue(sanitized.contains("nome*"))
        assertTrue(sanitized.contains("personagem*"))
        assertTrue(sanitized.contains("principal*"))
        assertTrue(!sanitized.contains("qual*"))
    }

    @Test
    fun testSanitizeJapaneseQuery() {
        val query = "ルフィの夢は何ですか？"
        val sanitized = FtsQuerySanitizer.sanitize(query)
        assertTrue(sanitized.contains("ルフィ*") || sanitized.contains("ル*") || sanitized.contains("フィ*"))
        assertTrue(sanitized.contains("夢*"))
    }

    @Test
    fun testSanitizeEmptyQuery() {
        val query = "   "
        val sanitized = FtsQuerySanitizer.sanitize(query)
        assertEquals("\"*\"", sanitized)
    }
}

package br.com.fenix.bilingualreader.util.constants

import org.junit.Assert.assertEquals
import org.junit.Test

class DataBaseConstsTest {

    @Test
    fun `test Manga table and column constants`() {
        assertEquals("Manga", DataBaseConsts.MANGA.TABLE_NAME)
        assertEquals("id", DataBaseConsts.MANGA.COLUMNS.ID)
        assertEquals("title", DataBaseConsts.MANGA.COLUMNS.TITLE)
        assertEquals("pages", DataBaseConsts.MANGA.COLUMNS.PAGES)
        assertEquals("chapters", DataBaseConsts.MANGA.COLUMNS.CHAPTERS)
        assertEquals("path", DataBaseConsts.MANGA.COLUMNS.FILE_PATH)
        assertEquals("id_library", DataBaseConsts.MANGA.COLUMNS.FK_ID_LIBRARY)
    }

    @Test
    fun `test Book table and column constants`() {
        assertEquals("Book", DataBaseConsts.BOOK.TABLE_NAME)
        assertEquals("id", DataBaseConsts.BOOK.COLUMNS.ID)
        assertEquals("title", DataBaseConsts.BOOK.COLUMNS.TITLE)
        assertEquals("author", DataBaseConsts.BOOK.COLUMNS.AUTHOR)
        assertEquals("isbn", DataBaseConsts.BOOK.COLUMNS.ISBN)
        assertEquals("id_library", DataBaseConsts.BOOK.COLUMNS.FK_ID_LIBRARY)
    }

    @Test
    fun `test JLPT and Kanjax table constants`() {
        assertEquals("Jlpt", DataBaseConsts.JLPT.TABLE_NAME)
        assertEquals("kanji", DataBaseConsts.JLPT.COLUMNS.KANJI)

        assertEquals("Kanjax", DataBaseConsts.KANJAX.TABLE_NAME)
        assertEquals("meaning", DataBaseConsts.KANJAX.COLUMNS.MEANING)
    }

    @Test
    fun `test FileLink and PagesLink table constants`() {
        assertEquals("FileLink", DataBaseConsts.FILELINK.TABLE_NAME)
        assertEquals("id_manga", DataBaseConsts.FILELINK.COLUMNS.FK_ID_MANGA)

        assertEquals("PagesLink", DataBaseConsts.PAGESLINK.TABLE_NAME)
        assertEquals("id_file", DataBaseConsts.PAGESLINK.COLUMNS.FK_ID_FILE)
    }
}

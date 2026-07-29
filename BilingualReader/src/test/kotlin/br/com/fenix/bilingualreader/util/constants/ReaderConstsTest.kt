package br.com.fenix.bilingualreader.util.constants

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ReaderConstsTest {

    @Test
    fun `test reader dimension limits`() {
        assertEquals(2400, ReaderConsts.READER.MAX_PAGE_HEIGHT)
        assertEquals(3000, ReaderConsts.READER.MAX_PAGE_WIDTH)
        assertEquals(6, ReaderConsts.READER.MANGA_OFF_SCREEN_PAGE_LIMIT)
        assertEquals(6, ReaderConsts.READER.BOOK_OFF_SCREEN_PAGE_LIMIT)
        assertFalse(ReaderConsts.READER.BOOK_WEB_VIEW_MODE)
        assertFalse(ReaderConsts.READER.BOOK_NATIVE_POPUP_MENU_SELECT)
    }

    @Test
    fun `test cover dimensions`() {
        assertEquals(300, ReaderConsts.COVER.MANGA_COVER_THUMBNAIL_HEIGHT)
        assertEquals(200, ReaderConsts.COVER.MANGA_COVER_THUMBNAIL_WIDTH)
        assertEquals(300, ReaderConsts.COVER.BOOK_COVER_THUMBNAIL_WIDTH)
        assertEquals(1080, ReaderConsts.COVER.BOOK_COVER_READER_WIDTH)
    }

    @Test
    fun `test state and tokenizer constants`() {
        assertEquals("STATE_FULLSCREEN", ReaderConsts.STATES.STATE_FULLSCREEN)
        assertEquals("STATE_NEW_COMIC", ReaderConsts.STATES.STATE_NEW_COMIC)
        assertEquals("sudachi_smalldict.json", ReaderConsts.TOKENIZER.SUDACHI.DICTIONARY_NAME)
    }
}

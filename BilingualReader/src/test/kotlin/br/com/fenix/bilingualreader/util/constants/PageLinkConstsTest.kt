package br.com.fenix.bilingualreader.util.constants

import org.junit.Assert.assertEquals
import org.junit.Test

class PageLinkConstsTest {

    @Test
    fun `test PageLink values and messages constants`() {
        assertEquals(-1, PageLinkConsts.VALUES.PAGE_EMPTY)

        assertEquals(0, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_IMAGE_START)
        assertEquals(1, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_IMAGE_UPDATED)
        assertEquals(4, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_IMAGE_FINISHED)
        assertEquals(9, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ALL_IMAGES_LOADED)

        assertEquals(20, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_AUTO_PAGES_START)
        assertEquals(21, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_AUTO_PAGES_FINISHED)
        assertEquals(40, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_UNDO_LAST_CHANGE_START)
    }

    @Test
    fun `test PageLink tag and clipdata constants`() {
        assertEquals("page_link_right", PageLinkConsts.TAG.PAGE_LINK_RIGHT)
        assertEquals("page_link_left", PageLinkConsts.TAG.PAGE_LINK_LEFT)
        assertEquals("page_not_link", PageLinkConsts.TAG.PAGE_NOT_LINK)

        assertEquals(0, PageLinkConsts.CLIPDATA.PAGE_LINK)
        assertEquals(1, PageLinkConsts.CLIPDATA.PAGE_TYPE)
        assertEquals(2, PageLinkConsts.CLIPDATA.IMAGE_NAME)
    }
}

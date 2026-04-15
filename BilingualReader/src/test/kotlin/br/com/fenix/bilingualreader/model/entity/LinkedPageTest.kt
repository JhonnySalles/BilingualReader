package br.com.fenix.bilingualreader.model.entity

import android.graphics.Bitmap
import br.com.fenix.bilingualreader.util.constants.PageLinkConsts
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LinkedPageTest {

    private fun createBasePage(mangaPage: Int = 1): LinkedPage {
        return LinkedPage(
            id = 1L, idFile = 10L, mangaPage = mangaPage, mangaPages = 100,
            mangaPageName = "M1", mangaPagePath = "/m1"
        )
    }

    @Test
    fun `test merge copies link fields`() {
        val page = createBasePage()
        val other = createBasePage().apply {
            fileLinkLeftPage = 5
            fileLinkLeftPageName = "L5"
            isNotLinked = true
        }

        page.merge(other)

        assertEquals(5, page.fileLinkLeftPage)
        assertEquals("L5", page.fileLinkLeftPageName)
        assertTrue(page.isNotLinked)
    }

    @Test
    fun `test addLeftPageLink updates left part`() {
        val page = createBasePage()
        val bitmap = mockk<Bitmap>()
        
        page.addLeftPageLink(10, 50, "P10", "/p10", true, bitmap)

        assertEquals(10, page.fileLinkLeftPage)
        assertEquals(50, page.fileLinkLeftPages)
        assertEquals(bitmap, page.imageLeftFileLinkPage)
        assertTrue(page.isFileLeftDualPage)
    }

    @Test
    fun `test addRightPageLink fills left if empty`() {
        val page = createBasePage()
        
        page.addRightPageLink(20, "P20", "/p20", false, null)

        assertEquals("Should fill left if empty", 20, page.fileLinkLeftPage)
        assertEquals(PageLinkConsts.VALUES.PAGE_EMPTY, page.fileLinkRightPage)
    }

    @Test
    fun `test addRightPageLink fills right if left is busy`() {
        val page = createBasePage()
        page.fileLinkLeftPage = 10
        
        page.addRightPageLink(20, "P20", "/p20", false, null)

        assertEquals(10, page.fileLinkLeftPage)
        assertEquals(20, page.fileLinkRightPage)
        assertTrue("isDualImage should be true", page.isDualImage)
    }

    @Test
    fun `test movePageLinkRightToLeft`() {
        val page = createBasePage()
        page.fileLinkLeftPage = 1
        page.fileLinkRightPage = 2
        page.fileLinkRightPageName = "R2"
        page.isFileRightDualPage = true

        page.movePageLinkRightToLeft()

        assertEquals(2, page.fileLinkLeftPage)
        assertEquals("R2", page.fileLinkLeftPageName)
        assertTrue(page.isFileLeftDualPage)
        assertEquals(PageLinkConsts.VALUES.PAGE_EMPTY, page.fileLinkRightPage)
        assertFalse(page.isDualImage)
    }

    @Test
    fun `test clearLeftPageLink with move`() {
        val page = createBasePage()
        page.fileLinkLeftPage = 1
        page.fileLinkRightPage = 2
        page.fileLinkRightPageName = "R2"

        val moved = page.clearLeftPageLink(canMoved = true)

        assertTrue("Should return true because it moved", moved)
        assertEquals("Right page should be now in Left", 2, page.fileLinkLeftPage)
        assertEquals(PageLinkConsts.VALUES.PAGE_EMPTY, page.fileLinkRightPage)
    }

    @Test
    fun `test clearPageLink resets everything`() {
        val page = createBasePage()
        page.fileLinkLeftPage = 1
        page.fileLinkRightPage = 2
        page.isNotLinked = true
        
        page.clearPageLink()

        assertEquals(PageLinkConsts.VALUES.PAGE_EMPTY, page.fileLinkLeftPage)
        assertEquals(PageLinkConsts.VALUES.PAGE_EMPTY, page.fileLinkRightPage)
        assertFalse(page.isNotLinked)
        assertFalse(page.isDualImage)
    }
}

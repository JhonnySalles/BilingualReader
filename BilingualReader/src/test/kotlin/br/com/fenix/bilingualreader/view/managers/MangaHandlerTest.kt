package br.com.fenix.bilingualreader.view.managers

import br.com.fenix.bilingualreader.service.parses.manga.Parse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MangaHandlerTest {

    private lateinit var handler: MangaReaderHandler
    private val mockParse: Parse = mockk()

    @Before
    fun setUp() {
        handler = MangaReaderHandler(mockParse)
    }

    @Test
    fun `getPageUri should return correctly formatted uri`() {
        val pageNum = 5
        val uri = handler.getPageUri(pageNum)
        
        assertNotNull(uri)
        assertEquals("localcomic", uri?.scheme)
        assertEquals("5", uri?.fragment)
    }

    @Test
    fun `loadPage should fetch page stream from parse`() {
        val pageNum = 10
        val emptyStream = ByteArrayInputStream(byteArrayOf())
        every { mockParse.getPage(pageNum) } returns emptyStream

        handler.loadPage(pageNum)
        
        verify { mockParse.getPage(pageNum) }
    }
}

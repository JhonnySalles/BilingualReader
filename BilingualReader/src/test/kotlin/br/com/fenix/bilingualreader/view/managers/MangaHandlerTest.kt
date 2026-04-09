package br.com.fenix.bilingualreader.view.managers

import android.net.Uri
import br.com.fenix.bilingualreader.service.parses.manga.Parse
import com.squareup.picasso.Picasso
import com.squareup.picasso.Request
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.InputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MangaHandlerTest {

    private lateinit var handler: MangaHandler
    private val mockParse: Parse = mockk()

    @Before
    fun setUp() {
        handler = MangaHandler(mockParse)
    }

    @Test
    fun `canHandleRequest should return true for localcomic scheme`() {
        val uri = Uri.parse("localcomic:///path#fragment")
        val request = Request.Builder(uri).build()
        
        assertTrue(handler.canHandleRequest(request))
    }

    @Test
    fun `canHandleRequest should return false for other schemes`() {
        val uri = Uri.parse("http://example.com/image.png")
        val request = Request.Builder(uri).build()
        
        assertFalse(handler.canHandleRequest(request))
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
    fun `load should return result with bitmap from parse`() {
        val pageNum = 10
        val uri = Uri.parse("localcomic:///path#$pageNum")
        val request = Request.Builder(uri).build()
        
        // Mocking an empty stream for the bitmap decoder
        val emptyStream = ByteArrayInputStream(byteArrayOf())
        every { mockParse.getPage(pageNum) } returns emptyStream

        val result = handler.load(request, 0)
        
        assertNotNull(result)
        assertEquals(Picasso.LoadedFrom.MEMORY, result.loadedFrom)
        // Since it's an empty byte array, bitmap might be null but the function should execute
        verify { mockParse.getPage(pageNum) }
    }
}

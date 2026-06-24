package br.com.fenix.bilingualreader.service.update

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class AppVersionTest {

    @Test
    fun `test release creation and properties`() {
        val now = LocalDateTime.now()
        val note = ReleaseNote("Bug fixes")
        val release = Release(
            name = "v1.0.1",
            note = note,
            version = "1.0.1",
            build = "101",
            consoleUri = "http://console",
            testingUri = "http://test",
            downloadUri = "http://download",
            mean = now
        )
        
        assertEquals("v1.0.1", release.name)
        assertEquals("Bug fixes", release.note.note)
        assertEquals(now, release.mean)
    }

    @Test
    fun `test releases container`() {
        val release = Release("v1", ReleaseNote("N"), "1", "1", "C", "T", "D", LocalDateTime.now())
        val container = Releases(listOf(release), "token123")
        
        assertEquals(1, container.releases.size)
        assertEquals("token123", container.nextPage)
    }
}

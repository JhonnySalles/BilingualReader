package br.com.fenix.bilingualreader.service.tracker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.model.entity.Information
import br.com.fenix.bilingualreader.service.tracker.mal.*
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ParseInformationTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun createMalDetail(id: Int, title: String) = MalMangaDetail(
        id = id,
        title = title,
        mainPicture = MalPicture("large", "medium"),
        alternativeTitles = MalAlternativeTitles(listOf("syn"), "en", "ja"),
        startDate = "2023-01-01",
        endDate = null,
        synopsis = "Synopsis",
        mean = "8.5",
        rank = "1",
        popularity = 10,
        numListUsers = 100,
        numScoringUsers = 50,
        nsfw = NSFW.WHITE,
        genres = listOf(),
        createdAt = "2023-01-01",
        updatedAt = "2023-01-01",
        mediaType = MEDIA.MANGA,
        status = STATUS.FINISHED,
        myListStatus = listOf(),
        volumes = 10,
        chapters = 100,
        authors = listOf(),
        pictures = listOf()
    )

    @Test
    fun `getInformation with MalMangaDetail should map correctly`() {
        val detail = createMalDetail(123, "Test Title")
        
        val info = ParseInformation.getInformation(context, detail)
        
        assertEquals("Test Title", info.title)
        assertTrue(info.link.contains("123"))
        assertEquals(Information.MY_ANIME_LIST, info.origin)
    }

    @Test
    fun `getInformation with list should process all and remove empty titles`() {
        val list = listOf(
            createMalDetail(1, "Title 1"),
            createMalDetail(2, ""), // Should be removed
            createMalDetail(3, "Title 3")
        )
        
        val results = ParseInformation.getInformation(context, list)
        
        assertEquals(2, results.size)
        assertEquals("Title 1", results[0].title)
        assertEquals("Title 3", results[1].title)
    }

    @Test
    fun `getInformation with unknown type should return empty Information`() {
        val result = ParseInformation.getInformation(context, "Unknown Object")
        
        assertNotNull(result)
        assertEquals("", result.title)
    }
}

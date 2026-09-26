package br.com.fenix.bilingualreader.service.tracker

import br.com.fenix.bilingualreader.model.entity.ComicInfo
import br.com.fenix.bilingualreader.model.entity.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TrackerMatcherTest {

    @Test
    fun testParseFileName_variousFormats() {
        val f1 = TrackerMatcher.parseFileName("Naruto - Vol 01.rar")
        assertEquals("Naruto", f1.cleanTitle)
        assertEquals(1, f1.volume)

        val f2 = TrackerMatcher.parseFileName("One Piece - Volume 05.cbz")
        assertEquals("One Piece", f2.cleanTitle)
        assertEquals(5, f2.volume)

        val f3 = TrackerMatcher.parseFileName("Bleach, Vol. 12.zip")
        assertEquals("Bleach", f3.cleanTitle)
        assertEquals(12, f3.volume)

        val f4 = TrackerMatcher.parseFileName("Death Note Vol. 03.rar")
        assertEquals("Death Note", f4.cleanTitle)
        assertEquals(3, f4.volume)

        val f5 = TrackerMatcher.parseFileName("Solo Leveling [Scan] c15.cbz")
        assertEquals("Solo Leveling", f5.cleanTitle)
        assertEquals(15f, f5.chapter ?: 0f, 0.01f)
    }

    @Test
    fun testExtractMalIdFromComicInfo() {
        val comicInfoWithWeb = ComicInfo(web = "https://myanimelist.net/manga/13/One_Piece")
        assertEquals(13L, TrackerMatcher.extractMalIdFromComicInfo(comicInfoWithWeb))

        val comicInfoWithNotes = ComicInfo(notes = "mal: 4567")
        assertEquals(4567L, TrackerMatcher.extractMalIdFromComicInfo(comicInfoWithNotes))

        val comicInfoWithoutMal = ComicInfo(notes = "Some other notes")
        assertNull(TrackerMatcher.extractMalIdFromComicInfo(comicInfoWithoutMal))
    }

    @Test
    fun testMatchTrack_hierarchy() {
        val tracks = listOf(
            Track(id = 1L, malId = 13L, title = "One Piece", titleRegex = "One Piece", fkLibrary = 1L),
            Track(id = 2L, malId = 11L, title = "Naruto", titleRegex = "Naruto", fkLibrary = 1L)
        )

        // 1. Match por MAL ID no ComicInfo
        val comicWithMal = ComicInfo(web = "https://myanimelist.net/manga/13")
        val match1 = TrackerMatcher.matchTrack(tracks, comicWithMal, "Unknown Name.rar")
        assertNotNull(match1)
        assertEquals(1L, match1?.id)

        // 2. Match por título no ComicInfo
        val comicWithSeries = ComicInfo(series = "Naruto")
        val match2 = TrackerMatcher.matchTrack(tracks, comicWithSeries, "Random Name - Vol 01.rar")
        assertNotNull(match2)
        assertEquals(2L, match2?.id)

        // 3. Match por nome do arquivo
        val match3 = TrackerMatcher.matchTrack(tracks, null, "One Piece - Vol 10.zip")
        assertNotNull(match3)
        assertEquals(1L, match3?.id)
    }
}

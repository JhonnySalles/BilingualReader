package br.com.fenix.bilingualreader.model.dao

import br.com.fenix.bilingualreader.model.entity.mock.MangaMock
import br.com.fenix.bilingualreader.model.entity.mock.SubTitleMock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SubTitleDAOTest : DataBaseBaseTest() {

    @Test
    fun saveAndGetSubTitle() {
        val dao = db.getSubTitleDao()
        val subTitle = SubTitleMock.mockEntity(1L).apply {
            id_manga = 707L
            chapterKey = "ch1"
        }

        val id = dao.save(subTitle)
        assertEquals(1L, id)

        val result = dao.get(707L, 1L)
        assertNotNull(result)
        assertEquals("ch1", result.chapterKey)
    }

    @Test
    fun findByIdManga() {
        val dao = db.getSubTitleDao()
        val subTitle = SubTitleMock.mockEntity(1L).apply { id_manga = 707L }
        dao.save(subTitle)

        val resultByManga = dao.findByIdManga(707L)
        assertNotNull(resultByManga)
        assertEquals(1L, resultByManga.id)

        val listByManga = dao.listByIdManga(707L)
        assertEquals(1, listByManga.size)
    }

    @Test
    fun updateMangaHasSubtitle() {
        val mangaDao = db.getMangaDao()
        val subDao = db.getSubTitleDao()
        val manga = MangaMock.mockEntity(10L).apply { hasSubtitle = false }
        mangaDao.save(manga)

        subDao.updateHasSubtitle(10L, true)

        val updatedManga = mangaDao.get(10L)
        assertTrue(updatedManga?.hasSubtitle ?: false)
    }

    @Test
    fun deleteAllSubtitles() {
        val dao = db.getSubTitleDao()
        dao.save(SubTitleMock.mockEntity(1L).apply { id_manga = 707L })
        dao.save(SubTitleMock.mockEntity(2L).apply { id_manga = 707L })

        dao.deleteAll(707L)

        val list = dao.listByIdManga(707L)
        assertTrue(list.isEmpty())
    }
}

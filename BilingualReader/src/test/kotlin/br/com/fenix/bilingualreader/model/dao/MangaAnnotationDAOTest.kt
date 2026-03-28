package br.com.fenix.bilingualreader.model.dao

import br.com.fenix.bilingualreader.model.entity.mock.MangaAnnotationMock
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class MangaAnnotationDAOTest : DataBaseBaseTest() {

    @Test
    fun saveAndGetAnnotation() {
        val dao = db.getMangaAnnotation()
        val annotation = MangaAnnotationMock.mockEntity(1L).apply {
            idManga = 606L
            page = 5
            annotation = "Epic fight scene"
            alteration = LocalDateTime.now()
        }

        val id = dao.save(annotation)
        assertEquals(1L, id)

        val list = dao.findAllByManga(606L)
        assertEquals(1, list.size)
        MangaAnnotationMock.asserts(annotation, list[0])
    }

    @Test
    fun findByPage() {
        val dao = db.getMangaAnnotation()
        val annotation = MangaAnnotationMock.mockEntity(1L).apply { idManga = 606L; page = 5 }
        dao.save(annotation)

        val result = dao.findByPage(606L, 5)
        assertEquals(1, result.size)
        assertEquals(5, result[0].page)
    }

    @Test
    fun deleteAnnotation() {
        val dao = db.getMangaAnnotation()
        val annotation = MangaAnnotationMock.mockEntity(1L)
        dao.save(annotation)

        dao.delete(annotation)

        val list = dao.findAllByManga(annotation.idManga)
        assertTrue(list.isEmpty())
    }
}

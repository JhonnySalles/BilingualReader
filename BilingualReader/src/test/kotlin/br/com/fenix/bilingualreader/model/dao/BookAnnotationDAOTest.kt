package br.com.fenix.bilingualreader.model.dao

import br.com.fenix.bilingualreader.model.entity.mock.BookAnnotationMock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class BookAnnotationDAOTest : DataBaseBaseTest() {

    @Test
    fun saveAndGetAnnotation() {
        val dao = db.getBookAnnotation()
        val annotation = BookAnnotationMock.mockEntity(1L).copy(
            id_parent = 505L,
            page = 10,
            annotation = "Important note about chapter 1",
            alteration = LocalDateTime.now()
        )

        val id = dao.save(annotation)
        assertEquals(1L, id)

        val list = dao.findAllByBook(505L)
        assertEquals(1, list.size)
        BookAnnotationMock.asserts(annotation, list[0])
    }

    @Test
    fun findByPage() {
        val dao = db.getBookAnnotation()
        val annotation1 = BookAnnotationMock.mockEntity(1L).copy(id_parent = 505L, page = 10)
        val annotation2 = BookAnnotationMock.mockEntity(2L).copy(id_parent = 505L, page = 20)
        
        dao.save(annotation1)
        dao.save(annotation2)

        val result = dao.findByPage(505L, 10)
        assertEquals(1, result.size)
        assertEquals(10, result[0].page)
    }

    @Test
    fun deleteAnnotation() {
        val dao = db.getBookAnnotation()
        val annotation = BookAnnotationMock.mockEntity(1L)
        dao.save(annotation)

        dao.delete(annotation)

        val list = dao.findAllByBook(annotation.id_parent)
        assertTrue(list.isEmpty())
    }
}

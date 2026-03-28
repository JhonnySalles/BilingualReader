package br.com.fenix.bilingualreader.model.dao

import br.com.fenix.bilingualreader.model.entity.mock.TagsMock
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TagsDAOTest : DataBaseBaseTest() {

    @Test
    fun saveAndGetTag() {
        val dao = db.getTagsDao()
        val tag = TagsMock.mockEntity(1L).apply { name = "Historical" }

        val id = dao.save(tag)
        assertEquals(1L, id)

        val resultById = dao.get(1L)
        assertNotNull(resultById)
        assertEquals("Historical", resultById?.name)

        val resultByName = dao.get("Historical")
        assertNotNull(resultByName)
        assertEquals(1L, resultByName?.id)
    }

    @Test
    fun validTag() {
        val dao = db.getTagsDao()
        val tag = TagsMock.mockEntity(1L).apply { name = "Science Fiction" }
        dao.save(tag)

        val result = dao.valid("%Science%")
        assertNotNull(result)
        assertEquals(1L, result?.id)
    }

    @Test
    fun listTags() {
        val dao = db.getTagsDao()
        val tag1 = TagsMock.mockEntity(1L).apply { name = "Action", excluded = false }
        val tag2 = TagsMock.mockEntity(2L).apply { name = "Drama", excluded = false }
        val tag3 = TagsMock.mockEntity(3L).apply { name = "Deleted", excluded = true }

        dao.save(tag1)
        dao.save(tag2)
        dao.save(tag3)

        val list = dao.list()
        assertEquals(2, list?.size)
        assertEquals("Action", list?.get(0)?.name)
    }
}

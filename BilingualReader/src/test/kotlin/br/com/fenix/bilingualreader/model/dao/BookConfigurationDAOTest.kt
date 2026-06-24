package br.com.fenix.bilingualreader.model.dao

import br.com.fenix.bilingualreader.model.entity.mock.BookConfigurationMock
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookConfigurationDAOTest : DataBaseBaseTest() {

    @Test
    fun saveAndGetConfiguration() {
        val dao = db.getBookConfigurationDao()
        val config = BookConfigurationMock.mockEntity(1L).copy(idBook = 101L).apply {
            fontSize = 18.0f
        }

        val id = dao.save(config)
        assertEquals(1L, id)

        val result = dao.findByBook(101L)
        assertNotNull(result)
        BookConfigurationMock.asserts(config, result)
        assertEquals(18.0f, result?.fontSize)
    }

    @Test
    fun updateConfiguration() {
        val dao = db.getBookConfigurationDao()
        val config = BookConfigurationMock.mockEntity(1L).copy(idBook = 101L).apply { fontSize = 16.0f }
        dao.save(config)

        config.fontSize = 20.0f
        dao.update(config)

        val result = dao.findByBook(101L)
        assertEquals(20.0f, result?.fontSize)
    }
}

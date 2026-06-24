package br.com.fenix.bilingualreader.model.dao

import br.com.fenix.bilingualreader.model.entity.mock.KanjaxMock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KanjaxDAOTest : DataBaseBaseTest() {

    @Test
    fun saveAndGetKanjax() {
        val dao = db.getKanjaxDao()
        val kanjax = KanjaxMock.mockEntity(1L).apply { kanji = "一" }

        val id = dao.save(kanjax)
        assertEquals(1L, id)

        val resultById = dao.get(1L)
        assertNotNull(resultById)
        assertEquals("一", resultById?.kanji)

        val resultByKanji = dao.get("一")
        assertNotNull(resultByKanji)
        assertEquals(1L, resultByKanji?.id)
    }

    @Test
    fun listKanjax() {
        val dao = db.getKanjaxDao()
        dao.save(KanjaxMock.mockEntity(1L).apply { kanji = "一" })
        dao.save(KanjaxMock.mockEntity(2L).apply { kanji = "二" })

        val list = dao.list()
        assertEquals(2, list.size)
    }
}

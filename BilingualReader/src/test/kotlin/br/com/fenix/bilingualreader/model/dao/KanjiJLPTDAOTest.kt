package br.com.fenix.bilingualreader.model.dao

import br.com.fenix.bilingualreader.model.entity.mock.KanjiJLPTMock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KanjiJLPTDAOTest : DataBaseBaseTest() {

    @Test
    fun saveAndGetKanjiJLPT() {
        val dao = db.getKanjiJLPTDao()
        val kanji = KanjiJLPTMock.mockEntity(1L).apply { kanji = "日"; level = 5 }

        val id = dao.save(kanji)
        assertEquals(1L, id)

        val result = dao.get(1L)
        assertNotNull(result)
        assertEquals("日", result?.kanji)
        assertEquals(5, result?.level)
    }

    @Test
    fun listKanjiJLPT() {
        val dao = db.getKanjiJLPTDao()
        dao.save(KanjiJLPTMock.mockEntity(1L).apply { kanji = "日" })
        dao.save(KanjiJLPTMock.mockEntity(2L).apply { kanji = "月" })

        val list = dao.list()
        assertEquals(2, list.size)
    }
}

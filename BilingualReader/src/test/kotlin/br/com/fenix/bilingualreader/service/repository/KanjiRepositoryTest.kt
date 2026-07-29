package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.model.entity.KanjiJLPT
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class KanjiRepositoryTest {

    private lateinit var kanjiRepository: KanjiRepository
    private val kanjiDao = mockk<KanjiJLPTDAO>(relaxed = true)
    private val mockDb = mockk<DataBase>(relaxed = true)

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        every { mockDb.isOpen } returns true
        every { mockDb.getKanjiJLPTDao() } returns kanjiDao
        DataBase.setTestingInstance(mockDb)

        kanjiRepository = KanjiRepository(context)
    }

    @After
    fun tearDown() {
        DataBase.close()
        DataBase.setTestingInstance(null)
        unmockkAll()
    }

    @Test
    fun `list should return all records`() {
        val item1 = KanjiJLPT(id = 1L, kanji = "日", level = 5)
        val item2 = KanjiJLPT(id = 2L, kanji = "本", level = 5)
        every { kanjiDao.list() } returns listOf(item1, item2)

        val list = kanjiRepository.list()
        assertNotNull(list)
        assertEquals(2, list?.size)
    }

    @Test
    fun `get should return correct record`() {
        val item = KanjiJLPT(id = 1L, kanji = "日", level = 5)
        every { kanjiDao.get(1L) } returns item

        val found = kanjiRepository.get(1L)
        assertNotNull(found)
        assertEquals("日", found?.kanji)
    }

    @Test
    fun `getHashMap should return correct mapping`() {
        val item1 = KanjiJLPT(id = 1L, kanji = "日", level = 5)
        val item2 = KanjiJLPT(id = 2L, kanji = "本", level = 5)
        every { kanjiDao.list() } returns listOf(item1, item2)

        val map = kanjiRepository.getHashMap()
        assertNotNull(map)
        assertEquals(5, map?.get("日"))
        assertEquals(5, map?.get("本"))
    }
}


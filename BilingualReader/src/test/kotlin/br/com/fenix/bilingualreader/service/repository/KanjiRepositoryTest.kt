package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.model.entity.KanjiJLPT
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
    private lateinit var db: DataBase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DataBase::class.java)
            .allowMainThreadQueries()
            .build()
        DataBase.setTestingInstance(db)

        kanjiRepository = KanjiRepository(context)
    }

    @After
    fun tearDown() {
        db.close()
        unmockkAll()
    }

    @Test
    fun `list should return all records`() {
        val dao = db.getKanjiJLPTDao()
        dao.save(KanjiJLPT(kanji = "日", level = 5))
        dao.save(KanjiJLPT(kanji = "本", level = 5))

        val list = kanjiRepository.list()
        assertNotNull(list)
        assertEquals(2, list?.size)
    }

    @Test
    fun `get should return correct record`() {
        val dao = db.getKanjiJLPTDao()
        val id = dao.save(KanjiJLPT(kanji = "日", level = 5))

        val found = kanjiRepository.get(id)
        assertNotNull(found)
        assertEquals("日", found?.kanji)
    }

    @Test
    fun `getHashMap should return correct mapping`() {
        val dao = db.getKanjiJLPTDao()
        dao.save(KanjiJLPT(kanji = "日", level = 5))
        dao.save(KanjiJLPT(kanji = "本", level = 5))

        val map = kanjiRepository.getHashMap()
        assertNotNull(map)
        assertEquals(5, map?.get("日"))
        assertEquals(5, map?.get("本"))
    }
}

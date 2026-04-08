package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.model.entity.KanjiJLPT
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
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

    private lateinit var db: DataBase
    private lateinit var kanjiRepository: KanjiRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        mockkStatic(FirebaseCrashlytics::class)
        mockkStatic(Firebase::class)
        val mockCrashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
        every { Firebase.crashlytics } returns mockCrashlytics
        
        db = Room.inMemoryDatabaseBuilder(context, DataBase::class.java)
            .allowMainThreadQueries()
            .build()
            
        println("DB created: $db")
        val dao = db.getKanjiJLPTDao()
        println("DAO created: $dao")
        
        DataBase.setTestingInstance(db)
        kanjiRepository = KanjiRepository(context)
    }

    @After
    fun tearDown() {
        db.close()
        unmockkAll()
    }

    @Test
    fun `get should return correct kanji`() {
        val kanji = KanjiJLPT(id = 1L, kanji = "日", level = 5)
        db.getKanjiJLPTDao().save(kanji)

        val result = kanjiRepository.get(1L)
        assertNotNull(result)
        assertEquals("日", result?.kanji)
        assertEquals(5, result?.level)
    }

    @Test
    fun `list should return all kanjis`() {
        db.getKanjiJLPTDao().save(KanjiJLPT(id = 1L, kanji = "日", level = 5))
        db.getKanjiJLPTDao().save(KanjiJLPT(id = 2L, kanji = "月", level = 5))

        val result = kanjiRepository.list()
        assertEquals(2, result?.size)
    }

    @Test
    fun `getHashMap should return mapped kanjis`() {
        db.getKanjiJLPTDao().save(KanjiJLPT(id = 1L, kanji = "日", level = 5))
        db.getKanjiJLPTDao().save(KanjiJLPT(id = 2L, kanji = "月", level = 4))

        val map = kanjiRepository.getHashMap()
        assertEquals(2, map?.size)
        assertEquals(5, map!!["日"])
        assertEquals(4, map!!["月"])
    }
}

package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.Kanjax
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class KanjaxRepositoryTest {

    private val context = mockk<Context>()
    private val dataBase = mockk<DataBase>()
    private val kanjaxDao = mockk<KanjaxDAO>(relaxed = true)

    private lateinit var repository: KanjaxRepository

    private fun createKanjax(id: Long? = null, kanji: String = "漢") = Kanjax(
        id = id,
        kanji = kanji,
        keyword = "kanji",
        meaning = "meaning",
        koohii = "",
        koohii2 = "",
        onYomi = "",
        kunYomi = "",
        onWords = "",
        kunWords = "",
        jlpt = 0,
        grade = 0,
        frequence = 0,
        strokes = 0,
        variants = "",
        radical = "",
        parts = "",
        utf8 = "",
        sjis = "",
        keywordPt = "",
        meaningPt = ""
    )

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockkStatic(FirebaseCrashlytics::class)
        every { FirebaseCrashlytics.getInstance() } returns mockk(relaxed = true)

        mockkObject(DataBase)
        every { DataBase.getDataBase(any()) } returns dataBase
        every { dataBase.getKanjaxDao() } returns kanjaxDao
        
        repository = KanjaxRepository(context)
    }

    @org.junit.After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `get by kanji should return from DAO`() {
        val kanjax = createKanjax(1L, "漢")
        every { kanjaxDao.get("漢") } returns kanjax
        
        val result = repository.get("漢")
        
        assertEquals(kanjax, result)
        verify { kanjaxDao.get("漢") }
    }

    @Test
    fun `list should return all from DAO`() {
        val list = listOf(createKanjax(1L), createKanjax(2L))
        every { kanjaxDao.list() } returns list
        
        val result = repository.list()
        
        assertEquals(list, result)
        verify { kanjaxDao.list() }
    }
}

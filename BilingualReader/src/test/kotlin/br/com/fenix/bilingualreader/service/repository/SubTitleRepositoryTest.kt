package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.SubTitle
import br.com.fenix.bilingualreader.model.enums.Languages
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
class SubTitleRepositoryTest {

    private val context = mockk<Context>()
    private val dataBase = mockk<DataBase>()
    private val subTitleDao = mockk<SubTitleDAO>(relaxed = true)

    private lateinit var repository: SubTitleRepository

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockkStatic(FirebaseCrashlytics::class)
        every { FirebaseCrashlytics.getInstance() } returns mockk(relaxed = true)

        mockkObject(DataBase)
        every { DataBase.getDataBase(any()) } returns dataBase
        every { dataBase.getSubTitleDao() } returns subTitleDao
        
        repository = SubTitleRepository(context)
    }

    @org.junit.After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `save should delete old subtitles and save new one`() {
        val subtitle = SubTitle(null, 10L, Languages.JAPANESE, "Chapter", "Page", 1, "Path")
        every { subTitleDao.save(subtitle) } returns 101L
        
        val id = repository.save(subtitle)
        
        assertEquals(101L, id)
        verify { subTitleDao.deleteAll(10L) }
        verify { subTitleDao.save(subtitle) }
    }

    @Test
    fun `findByIdManga should return result from DAO`() {
        val subtitle = SubTitle(1L, 10L, Languages.JAPANESE, "Chapter", "Page", 1, "Path")
        every { subTitleDao.findByIdManga(10L) } returns subtitle
        
        val result = repository.findByIdManga(10L)
        
        assertEquals(subtitle, result)
        verify { subTitleDao.findByIdManga(10L) }
    }
}

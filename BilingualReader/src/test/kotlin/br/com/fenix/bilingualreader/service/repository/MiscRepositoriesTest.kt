package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.model.entity.*
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.service.repository.DataBaseDAO.*
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MiscRepositoriesTest {

    private lateinit var context: Context
    private val bookSearchDao: BookSearchDAO = mockk(relaxed = true)
    private val kanjaxDao: KanjaxDAO = mockk(relaxed = true)
    private val tagsDao: TagsDAO = mockk(relaxed = true)
    private val subTitleDao: SubTitleDAO = mockk(relaxed = true)
    private val dataBaseMock: DataBase = mockk(relaxed = true)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockkObject(DataBase)
        every { DataBase.getDataBase(any()) } returns dataBaseMock
        every { dataBaseMock.getBookSearch() } returns bookSearchDao
        every { dataBaseMock.getKanjaxDao() } returns kanjaxDao
        every { dataBaseMock.getTagsDao() } returns tagsDao
        every { dataBaseMock.getSubTitleDao() } returns subTitleDao
    }

    @Test
    fun testBookSearchRepository() {
        val repository = BookSearchRepository(context)
        val search = BookSearch(1L, "Query")
        
        every { bookSearchDao.save(any<BookSearch>()) } returns 123L
        
        val id = repository.save(search)
        
        assertEquals(123L, id)
        verify { bookSearchDao.save(any<BookSearch>()) }
    }

    @Test
    fun testKanjaxRepository() {
        val repository = KanjaxRepository(context)
        // 21 arguments: id, kanji, keyword, meaning, koohii, koohii2, onYomi, kunYomi, onWords, kunWords, jlpt, grade, frequence, strokes, variants, radical, parts, utf8, sjis, keywordPt, meaningPt
        val kanjax = Kanjax(null, "漢", "key", "mean", "k1", "k2", "on", "kun", "ow", "kw", 1, 1, 1, 1, "v", "r", "p", "u", "s", "kpt", "mpt")
        
        every { kanjaxDao.get("漢") } returns kanjax
        
        val result = repository.get("漢")
        assertEquals(kanjax, result)
    }

    @Test
    fun testTagsRepository() {
        val repository = TagsRepository(context)
        val tag = Tags(1L, "Action")
        
        every { tagsDao.get("Action") } returns tag
        
        val result = repository.get("Action")
        assertEquals(tag, result)
    }

    @Test
    fun testSubTitleRepository() {
        val repository = SubTitleRepository(context)
        val subTitle = SubTitle(1L, 1L, Languages.JAPANESE, path = "Path")
        
        every { subTitleDao.save(any<SubTitle>()) } returns 123L
        
        repository.save(subTitle)
        verify { subTitleDao.save(any<SubTitle>()) }
        verify { subTitleDao.deleteAll(1L) }
    }
}

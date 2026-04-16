package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.model.entity.History
import br.com.fenix.bilingualreader.model.enums.Type
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
class HistoryRepositoryTest {

    private lateinit var historyRepository: HistoryRepository
    private lateinit var db: DataBase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DataBase::class.java)
            .allowMainThreadQueries()
            .build()
        DataBase.setTestingInstance(db)

        historyRepository = HistoryRepository(context)
    }

    @After
    fun tearDown() {
        if (::db.isInitialized) {
            db.close()
        }
        DataBase.setTestingInstance(null)
        unmockkAll()
    }

    private fun createHistory(type: Type, library: Long, reference: Long, volume: String = "Vol 1") = History(
        fkLibrary = library,
        fkReference = reference,
        type = type,
        pageStart = 1,
        pages = 10,
        volume = volume,
        averageTimeByPage = 60,
        useTTS = false,
        isNotify = false
    )

    @Test
    fun `save and find should work`() {
        val history = createHistory(Type.MANGA, 1L, 1L)
        val id = historyRepository.save(history)
        assertNotNull(id)

        val found = historyRepository.find(Type.MANGA, 1L, 1L)
        assertNotNull(found)
        
        if (found.isEmpty()) {
            val all = historyRepository.findAll()
            println("DB Debug - Total records: ${all.size}")
            for (h in all) {
                println("DB Debug - Record: type=${h.type}, library=${h.fkLibrary}, reference=${h.fkReference}, volume=${h.volume}")
            }
        }
        
        assertEquals(1, found.size)
    }

    private fun HistoryRepository.findAll(): List<History> {
        return historyRepository.javaClass.getDeclaredField("mDataBase").let { field ->
            field.isAccessible = true
            val dao = field.get(historyRepository) as HistoryDAO
            dao.findAll()
        }
    }

    @Test
    fun `last should return the most recent record`() {
        historyRepository.save(createHistory(Type.MANGA, 1L, 1L, "Vol 1"))
        val history2 = createHistory(Type.MANGA, 1L, 1L, "Vol 2")
        historyRepository.save(history2)

        val last = historyRepository.last(Type.MANGA, 1L, 1L)
        assertNotNull(last)
        assertEquals("Vol 2", last?.volume)
    }

    @Test
    fun `clearAll should remove all records`() {
        historyRepository.save(createHistory(Type.MANGA, 1L, 1L))
        historyRepository.save(createHistory(Type.MANGA, 1L, 2L))
        
        historyRepository.clearAll()
        val found = historyRepository.find(Type.MANGA, 1L, 1L)
        assertEquals(0, found.size)
    }
}

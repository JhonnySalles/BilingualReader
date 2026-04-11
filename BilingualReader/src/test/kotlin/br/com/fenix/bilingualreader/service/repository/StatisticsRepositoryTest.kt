package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.model.entity.History
import br.com.fenix.bilingualreader.model.enums.Type
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class StatisticsRepositoryTest {

    private lateinit var statisticsRepository: StatisticsRepository
    private lateinit var historyRepository: HistoryRepository
    private lateinit var db: DataBase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DataBase::class.java)
            .allowMainThreadQueries()
            .build()
        DataBase.setTestingInstance(db)

        // Removed Crashlytics mocking due to ClassNotFoundException during instrumentation

        statisticsRepository = StatisticsRepository(context)
        historyRepository = HistoryRepository(context)
    }

    @After
    fun tearDown() {
        db.close()
        unmockkAll()
    }

    @Test
    fun `findAllStatistics should return data`() {
        val history = History(
            fkLibrary = 1L,
            fkReference = 1L,
            type = Type.MANGA,
            pageStart = 1,
            pages = 10,
            volume = "Vol 1",
            averageTimeByPage = 60,
            useTTS = false,
            isNotify = false
        )
        historyRepository.save(history)

        val list = statisticsRepository.statistics()
        assertNotNull(list)
    }
}

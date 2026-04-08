package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.model.entity.History
import br.com.fenix.bilingualreader.model.enums.Type
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
import java.time.LocalDateTime

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

        mockkStatic(Firebase::class)
        mockkStatic("com.google.firebase.crashlytics.ktx.CrashlyticsKt")
        val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
        every { Firebase.crashlytics } returns crashlytics

        historyRepository = HistoryRepository(context)
    }

    @After
    fun tearDown() {
        db.close()
        unmockkAll()
    }

    private fun createHistory(type: Type, library: Long, reference: Long) = History(
        fkLibrary = library,
        fkReference = reference,
        type = type,
        pageStart = 1,
        pages = 10,
        volume = "Vol 1",
        averageTimeByPage = 60,
        useTTS = false,
        isNotify = false
    )

    @Test
    fun `save and find should work`() {
        val history = History(
            fkLibrary = 1L,
            fkReference = 1L,
            type = Type.MANGA,
            pageStart = 1,
            pages = 10,
            volume = "Vol 1"
        )
        val id = historyRepository.save(history)
        assertNotNull(id)

        val found = historyRepository.find(Type.MANGA, 1L, 1L)
        assertNotNull(found)
        assertEquals(1, found?.size)
    }

    @Test
        historyRepository.save(History(1L, 1L, Type.MANGA, 1, 10, "Vol 1"))
        historyRepository.save(History(1L, 2L, Type.MANGA, 1, 10, "Vol 1"))
        
        historyRepository.clearAll()
        val found = historyRepository.find(Type.MANGA, 1L, 1L)
        assertEquals(0, found?.size ?: 0)
    }
}

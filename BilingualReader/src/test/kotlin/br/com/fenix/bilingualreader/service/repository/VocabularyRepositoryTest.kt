package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.model.entity.Vocabulary
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VocabularyRepositoryTest {

    private lateinit var vocabularyRepository: VocabularyRepository
    private lateinit var db: DataBase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DataBase::class.java)
            .allowMainThreadQueries()
            .build()
        DataBase.setTestingInstance(db)

        // Removed Crashlytics mocking due to ClassNotFoundException during instrumentation

        vocabularyRepository = VocabularyRepository(context)
    }

    @After
    fun tearDown() {
        db.close()
        unmockkAll()
    }

    private fun createVocabulary(word: String) = Vocabulary(
        id = null,
        word = word,
        portuguese = null,
        english = null,
        reading = null,
        basicForm = word,
        jlpt = 0,
        revised = false,
        favorite = false,
        appears = 0
    )

    @Test
    fun `save and get should work`() {
        val vocab = createVocabulary("test")
        val id = vocabularyRepository.save(vocab)
        assertNotNull(id)
        
        val found = vocabularyRepository.get(id)
        assertNotNull(found)
        assertEquals("test", found?.word)
    }

    @Test
    fun `findAll should return all records`() {
        vocabularyRepository.save(createVocabulary("v1"))
        vocabularyRepository.save(createVocabulary("v2"))
        
        val list = vocabularyRepository.findAll("")
        assertEquals(2, list.size)
    }

    @Test
    fun `delete should remove record`() {
        val vocab = createVocabulary("to-delete")
        val id = vocabularyRepository.save(vocab)
        val toDelete = vocab.copy(id = id)
        
        vocabularyRepository.delete(toDelete)
        val found = vocabularyRepository.get(id)
        assertEquals(null, found)
    }
}

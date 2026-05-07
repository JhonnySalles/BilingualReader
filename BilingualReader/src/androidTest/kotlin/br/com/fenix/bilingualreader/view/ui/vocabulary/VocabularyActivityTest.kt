package br.com.fenix.bilingualreader.view.ui.vocabulary

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VocabularyActivityTest {

    @Test
    fun testLaunchWithoutExtrasLoadsDefaultFragment() {
        ActivityScenario.launch(VocabularyActivity::class.java).use {
            onView(withId(R.id.vocabulary_root)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun testLaunchWithTypeMangaLoadsMangaFragment() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempFile = java.io.File(context.cacheDir, "test_manga.cbz")
        if (!tempFile.exists()) tempFile.createNewFile()
        val manga = Manga(null, 1L, tempFile)
        
        val intent = Intent(context, VocabularyActivity::class.java).apply {
            putExtra(GeneralConsts.KEYS.VOCABULARY.TYPE, Type.MANGA)
            putExtra(GeneralConsts.KEYS.OBJECT.MANGA, manga)
        }

        ActivityScenario.launch<VocabularyActivity>(intent).use {
            onView(withId(R.id.vocabulary_manga_root)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun testLaunchWithTypeBookLoadsBookFragment() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempFile = java.io.File(context.cacheDir, "test_book.epub")
        if (!tempFile.exists()) tempFile.createNewFile()
        val book = Book(null, 1L, tempFile)
        
        val intent = Intent(context, VocabularyActivity::class.java).apply {
            putExtra(GeneralConsts.KEYS.VOCABULARY.TYPE, Type.BOOK)
            putExtra(GeneralConsts.KEYS.OBJECT.BOOK, book)
        }

        ActivityScenario.launch<VocabularyActivity>(intent).use {
            onView(withId(R.id.vocabulary_book_root)).check(matches(isDisplayed()))
        }
    }
}

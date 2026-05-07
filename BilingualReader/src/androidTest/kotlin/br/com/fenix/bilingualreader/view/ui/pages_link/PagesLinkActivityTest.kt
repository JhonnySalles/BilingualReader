package br.com.fenix.bilingualreader.view.ui.pages_link

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PagesLinkActivityTest {

    @Test
    fun testLaunchLoadsFragment() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempFile = java.io.File(context.cacheDir, "test_manga.cbz")
        if (!tempFile.exists()) tempFile.createNewFile()
        val manga = Manga(null, 1L, tempFile)
        
        val intent = Intent(context, PagesLinkActivity::class.java).apply {
            putExtra(GeneralConsts.KEYS.OBJECT.MANGA, manga)
            putExtra(GeneralConsts.KEYS.MANGA.PAGE_NUMBER, 1)
        }

        ActivityScenario.launch<PagesLinkActivity>(intent).use {
            onView(withId(R.id.root_frame_pages_link)).check(matches(isDisplayed()))
            // Verifica se o fragmento foi carregado (pode verificar um ID de view do fragmento)
            onView(withId(R.id.pages_link_root)).check(matches(isDisplayed()))
        }
    }
}

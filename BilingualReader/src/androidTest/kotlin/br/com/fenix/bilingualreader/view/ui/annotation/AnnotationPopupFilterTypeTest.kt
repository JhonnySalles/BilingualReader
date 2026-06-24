package br.com.fenix.bilingualreader.view.ui.annotation

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.Filter
import br.com.fenix.bilingualreader.service.listener.AnnotationListener
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnnotationPopupFilterTypeTest {

    @Test
    fun testTypeSelectionCallsListener() {
        val listener = mockk<AnnotationListener>(relaxed = true)
        val scenario = launchFragmentInContainer<AnnotationPopupFilterType>(themeResId = R.style.Theme_MangaReader)
        
        scenario.onFragment { fragment ->
            fragment.setListener(listener)
        }

        // Clica na opção de "Favorito" (R.string.annotation_popup_filter_favorite)
        onView(withText(R.string.annotation_popup_filter_favorite)).perform(click())

        // Verifica se o listener foi chamado com o filtro correto
        verify { listener.filterType(Filter.Favorite, false) }
    }
}

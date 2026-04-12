package br.com.fenix.bilingualreader.view.ui.library.manga

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryMangaPopupFilterTest {

    @Test
    fun testSelectFavoriteFilter() {
        launchFragmentInContainer<LibraryMangaPopupFilter>(themeResId = R.style.AppBackground)
        
        onView(withId(R.id.popup_library_filter_favorite)).perform(click())
        onView(withId(R.id.popup_library_filter_favorite)).check(matches(isChecked()))
    }

    @Test
    fun testSelectReadingFilter() {
        launchFragmentInContainer<LibraryMangaPopupFilter>(themeResId = R.style.AppBackground)
        
        onView(withId(R.id.popup_library_filter_reading)).perform(click())
        onView(withId(R.id.popup_library_filter_reading)).check(matches(isChecked()))
    }
}

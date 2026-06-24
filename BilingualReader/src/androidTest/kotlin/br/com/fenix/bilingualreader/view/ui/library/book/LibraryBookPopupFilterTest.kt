package br.com.fenix.bilingualreader.view.ui.library.book

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryBookPopupFilterTest {

    @Test
    fun testSelectFavoriteFilter() {
        launchFragmentInContainer<LibraryBookPopupFilter>(themeResId = R.style.AppBackground)
        
        onView(withId(R.id.popup_library_filter_favorite)).perform(click())
        onView(withId(R.id.popup_library_filter_favorite)).check(matches(isChecked()))
        onView(withId(R.id.popup_library_filter_reading)).check(matches(isNotChecked()))
    }

    @Test
    fun testSelectReadingFilter() {
        launchFragmentInContainer<LibraryBookPopupFilter>(themeResId = R.style.AppBackground)
        
        onView(withId(R.id.popup_library_filter_reading)).perform(click())
        onView(withId(R.id.popup_library_filter_reading)).check(matches(isChecked()))
    }

    @Test
    fun testUnselectClearsFilter() {
        launchFragmentInContainer<LibraryBookPopupFilter>(themeResId = R.style.AppBackground)
        
        onView(withId(R.id.popup_library_filter_favorite)).perform(click()) // Check
        onView(withId(R.id.popup_library_filter_favorite)).perform(click()) // Uncheck
        
        onView(withId(R.id.popup_library_filter_favorite)).check(matches(isNotChecked()))
    }
}

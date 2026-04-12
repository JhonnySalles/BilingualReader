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
class LibraryMangaPopupTypeTest {

    @Test
    fun testSelectGridBig() {
        launchFragmentInContainer<LibraryMangaPopupType>(themeResId = R.style.AppBackground)
        
        onView(withId(R.id.popup_library_manga_type_grid_big)).perform(click())
        onView(withId(R.id.popup_library_manga_type_grid_big)).check(matches(isChecked()))
        onView(withId(R.id.popup_library_manga_type_line)).check(matches(isNotChecked()))
    }

    @Test
    fun testSelectGridMedium() {
        launchFragmentInContainer<LibraryMangaPopupType>(themeResId = R.style.AppBackground)
        
        onView(withId(R.id.popup_library_manga_type_grid_medium)).perform(click())
        onView(withId(R.id.popup_library_manga_type_grid_medium)).check(matches(isChecked()))
    }

    @Test
    fun testSelectLine() {
        launchFragmentInContainer<LibraryMangaPopupType>(themeResId = R.style.AppBackground)
        
        onView(withId(R.id.popup_library_manga_type_line)).perform(click())
        onView(withId(R.id.popup_library_manga_type_line)).check(matches(isChecked()))
    }
}

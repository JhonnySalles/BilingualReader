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
class LibraryBookPopupTypeTest {

    @Test
    fun testSelectGridBig() {
        launchFragmentInContainer<LibraryBookPopupType>(themeResId = R.style.AppTheme)
        
        onView(withId(R.id.popup_library_book_type_grid_big)).perform(click())
        onView(withId(R.id.popup_library_book_type_grid_big)).check(matches(isChecked()))
        onView(withId(R.id.popup_library_book_type_line)).check(matches(isNotChecked()))
    }

    @Test
    fun testSelectGridMedium() {
        launchFragmentInContainer<LibraryBookPopupType>(themeResId = R.style.AppTheme)
        
        onView(withId(R.id.popup_library_book_type_grid_medium)).perform(click())
        onView(withId(R.id.popup_library_book_type_grid_medium)).check(matches(isChecked()))
    }

    @Test
    fun testSelectSeparatorBig() {
        launchFragmentInContainer<LibraryBookPopupType>(themeResId = R.style.AppTheme)
        
        onView(withId(R.id.popup_library_book_type_separator_big)).perform(click())
        onView(withId(R.id.popup_library_book_type_separator_big)).check(matches(isChecked()))
    }

    @Test
    fun testSelectLine() {
        launchFragmentInContainer<LibraryBookPopupType>(themeResId = R.style.AppTheme)
        
        onView(withId(R.id.popup_library_book_type_line)).perform(click())
        onView(withId(R.id.popup_library_book_type_line)).check(matches(isChecked()))
    }
}

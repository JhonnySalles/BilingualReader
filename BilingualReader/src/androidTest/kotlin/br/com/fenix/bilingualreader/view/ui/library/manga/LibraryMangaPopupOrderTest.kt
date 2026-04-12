package br.com.fenix.bilingualreader.view.ui.library.manga

import android.view.View
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.MutableLiveData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.service.listener.PopupOrderListener
import br.com.fenix.bilingualreader.view.components.TriStateCheckBox
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryMangaPopupOrderTest {

    private fun withTriStateState(state: Int): Matcher<View> {
        return object : BoundedMatcher<View, TriStateCheckBox>(TriStateCheckBox::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("with TriStateCheckBox state: $state")
            }
            override fun matchesSafely(item: TriStateCheckBox): Boolean = item.state == state
        }
    }

    @Test
    fun testClickChangesState() {
        val scenario = launchFragmentInContainer<LibraryMangaPopupOrder>(themeResId = R.style.AppTheme)
        
        val liveData = MutableLiveData<Pair<Order, Boolean>>()
        val listener = object : PopupOrderListener {
            override fun popupOrderOnChange() {}
            override fun popupSorted(order: Order) {}
            override fun popupSorted(order: Order, isDesc: Boolean) {}
            override fun popupGetOrder(): Pair<Order, Boolean> = Pair(Order.Name, false)
            override fun popupGetObserver() = liveData
        }
        
        scenario.onFragment { it.setListener(listener) }

        onView(withId(R.id.popup_library_order_manga_author)).perform(click())
        onView(withId(R.id.popup_library_order_manga_author)).check(matches(withTriStateState(TriStateCheckBox.STATE_CHECKED)))
    }

    @Test
    fun testDoubleClickChangesStateToIndeterminate() {
        val scenario = launchFragmentInContainer<LibraryMangaPopupOrder>(themeResId = R.style.AppTheme)
        
        val liveData = MutableLiveData<Pair<Order, Boolean>>()
        val listener = object : PopupOrderListener {
            override fun popupOrderOnChange() {}
            override fun popupSorted(order: Order) {}
            override fun popupSorted(order: Order, isDesc: Boolean) {}
            override fun popupGetOrder(): Pair<Order, Boolean> = Pair(Order.Name, false)
            override fun popupGetObserver() = liveData
        }
        
        scenario.onFragment { it.setListener(listener) }

        onView(withId(R.id.popup_library_order_manga_date)).perform(click()) 
        onView(withId(R.id.popup_library_order_manga_date)).perform(click()) 
        
        onView(withId(R.id.popup_library_order_manga_date)).check(matches(withTriStateState(TriStateCheckBox.STATE_INDETERMINATE)))
    }
}

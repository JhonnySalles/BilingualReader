package br.com.fenix.bilingualreader

import android.content.res.Resources
import android.view.Gravity
import android.view.View
import androidx.core.content.edit
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.DrawerMatchers.isClosed
import androidx.test.espresso.contrib.DrawerMatchers.isOpen
import androidx.test.espresso.contrib.NavigationViewActions
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.view.ui.configuration.ConfigFragment
import br.com.fenix.bilingualreader.view.ui.history.HistoryFragment
import br.com.fenix.bilingualreader.view.ui.library.book.BookLibraryFragment
import br.com.fenix.bilingualreader.view.ui.library.manga.MangaLibraryFragment
import br.com.fenix.bilingualreader.view.ui.statistics.StatisticsFragment
import com.google.android.material.navigation.NavigationView
import junit.framework.TestCase.assertTrue
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainNavigationTest {

    private val waitTime = 1000L

    // Custom matcher para verificar o item marcado no NavigationView
    // substitui o NavigationViewMatchers.hasCheckedItem se este não estiver disponível
    private fun hasCheckedItem(id: Int): Matcher<View> {
        return object : BoundedMatcher<View, NavigationView>(NavigationView::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("has checked item with id: $id")
            }

            override fun matchesSafely(view: NavigationView): Boolean {
                val menu = view.menu
                for (i in 0 until menu.size()) {
                    val item = menu.getItem(i)
                    if (item.itemId == id && item.isChecked) return true
                    if (item.hasSubMenu()) {
                        val subMenu = item.subMenu ?: continue
                        for (j in 0 until subMenu.size()) {
                            val subItem = subMenu.getItem(j)
                            if (subItem.itemId == id && subItem.isChecked) return true
                        }
                    }
                }
                return false
            }
        }
    }

    @Test
    fun testDrawerBehaviorAndNavigation() {

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            
            // 1. Abre o Drawer
            onView(withId(R.id.drawer_layout))
                .check(matches(isClosed(Gravity.START)))
                .perform(DrawerActions.open())
            
            onView(withId(R.id.drawer_layout)).check(matches(isOpen(Gravity.START)))

            // 2. Navega para Estatísticas
            onView(withId(R.id.nav_view))
                .perform(NavigationViewActions.navigateTo(R.id.menu_statistics))
            
            Thread.sleep(waitTime)

            // Verifica se o Drawer fechou automaticamente
            onView(withId(R.id.drawer_layout)).check(matches(isClosed(Gravity.START)))

            // Verifica se o fragmento correto foi carregado
            scenario.onActivity {
                val fragment = it.supportFragmentManager.findFragmentById(R.id.main_content_root)
                assertTrue(fragment is StatisticsFragment)
            }

            // 3. Abre novamente e navega para Biblioteca de Livros
            onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
            onView(withId(R.id.nav_view))
                .perform(NavigationViewActions.navigateTo(R.id.menu_book_library_default))
            
            Thread.sleep(waitTime)

            scenario.onActivity {
                val fragment = it.supportFragmentManager.findFragmentById(R.id.main_content_root)
                assertTrue(fragment is BookLibraryFragment)
            }

            // 4. Verifica se o item no NavigationView está marcado corretamente
            onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
            onView(withId(R.id.nav_view)).check(matches(hasCheckedItem(R.id.menu_book_library_default)))
        }
    }

    @Test
    fun testNavigationBackStack() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            
            // Navega para Histórico
            onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
            onView(withId(R.id.nav_view)).perform(NavigationViewActions.navigateTo(R.id.menu_history))
            Thread.sleep(waitTime)

            // Navega para Estatísticas
            onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
            onView(withId(R.id.nav_view)).perform(NavigationViewActions.navigateTo(R.id.menu_statistics))
            Thread.sleep(waitTime)

            // Pressiona Back e volta para Histórico
            pressBack()
            Thread.sleep(waitTime)
            scenario.onActivity {
                val fragment = it.supportFragmentManager.findFragmentById(R.id.main_content_root)
                assertTrue(fragment is HistoryFragment)
            }

            // Pressiona Back novamente e volta para a Biblioteca Inicial (ou o que estava antes)
            pressBack()
            Thread.sleep(waitTime)
            scenario.onActivity {
                val fragment = it.supportFragmentManager.findFragmentById(R.id.main_content_root)
                // Dependendo do estado inicial, pode ser MangaLibraryFragment
                assertTrue(fragment is MangaLibraryFragment || fragment is BookLibraryFragment)
            }
        }
    }

    @Test
    fun testSearchNoResultsBehavior() {
        ActivityScenario.launch(MainActivity::class.java).use {
            Thread.sleep(waitTime)

            // Abre busca na biblioteca (assumindo que estamos na MangaLibrary inicial)
            onView(withId(R.id.menu_manga_library_search)).perform(click())
            
            // Digita algo que não existe
            val searchId = Resources.getSystem().getIdentifier("search_src_text", "id", "android")
            onView(withId(searchId)).perform(typeText("NonExistentMangaXYZ"), pressImeActionButton())
            
            Thread.sleep(waitTime)
            
            // Verifica se a lista do RecyclerView está vazia
            onView(withId(R.id.manga_library_recycler_view)).check { view, _ ->
                val rv = view as RecyclerView
                assertTrue(rv.adapter?.itemCount == 0)
            }
        }
    }

    @Test
    fun testThemeChangeStartFragment() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        GeneralConsts.getSharedPreferences(context).edit(commit = true) {
            putBoolean(GeneralConsts.KEYS.THEME.THEME_CHANGE, true)
        }

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            Thread.sleep(waitTime)
            
            // Deve iniciar no ConfigFragment se THEME_CHANGE for true
            scenario.onActivity {
                val fragment = it.supportFragmentManager.findFragmentById(R.id.main_content_root)
                assertTrue(fragment is ConfigFragment)
            }
        }
    }
}



package br.com.fenix.bilingualreader

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.NavigationViewActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anyOf
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookLibraryTest {

    private val waitTime = 1500L

    private fun waitForView(matcher: Matcher<View>, timeout: Long = 5000): ViewInteraction {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + timeout

        while (System.currentTimeMillis() < endTime) {
            try {
                val interaction = onView(matcher)
                interaction.check(matches(isDisplayed()))
                return interaction
            } catch (e: Throwable) {
                Thread.sleep(100)
            }
        }
        return onView(matcher).check(matches(isDisplayed()))
    }

    private fun waitForSkeleton() {
        val startTime = System.currentTimeMillis()
        val timeout = 10000L
        while (System.currentTimeMillis() < startTime + timeout) {
            try {
                onView(withId(R.id.skeleton_layout)).check(
                    matches(
                        anyOf(
                            withEffectiveVisibility(Visibility.GONE),
                            withEffectiveVisibility(Visibility.INVISIBLE)
                        )
                    )
                )
                return
            } catch (e: Throwable) {
                Thread.sleep(200)
            }
        }
    }

    private fun waitForRecyclerViewData(recyclerViewId: Int, timeout: Long = 10000) {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() < startTime + timeout) {
            try {
                onView(withId(recyclerViewId)).check { view, noViewFoundException ->
                    if (noViewFoundException != null) throw noViewFoundException
                    val recyclerView = view as RecyclerView
                    val adapter = recyclerView.adapter
                    if (adapter == null || adapter.itemCount == 0) {
                        throw RuntimeException("RecyclerView is empty")
                    }
                    
                    if (recyclerView.findViewHolderForAdapterPosition(0) == null) {
                        throw RuntimeException("ViewHolder for position 0 not found yet")
                    }
                }
                return
            } catch (e: Throwable) {
                Thread.sleep(200)
            }
        }
    }

    private fun clickChildViewWithId(id: Int): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> = allOf(isAssignableFrom(View::class.java), isDisplayed())
            override fun getDescription(): String = "Click on a child view with specified id."
            override fun perform(uiController: UiController, view: View) {
                val v = view.findViewById<View>(id)
                v.performClick()
            }
        }
    }

    @Test
    fun testBookLibraryFullRoutine() {
        ActivityScenario.launch(MainActivity::class.java).use {
            Thread.sleep(waitTime)

            // 2-Seguir abrindo a biblioteca de book
            onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
            onView(withId(R.id.nav_view)).perform(NavigationViewActions.navigateTo(R.id.menu_book_library_default))
            
            // Espera a biblioteca carregar (skeleton sumir e lista aparecer)
            waitForSkeleton()
            waitForView(withId(R.id.book_library_recycler_view))
            waitForRecyclerViewData(R.id.book_library_recycler_view)
            Thread.sleep(waitTime)

            // Força o scroll/layout do primeiro item antes de interagir
            onView(withId(R.id.book_library_recycler_view)).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(0))
            Thread.sleep(500)

            // 2.1-Selecionar como favorito um item
            onView(withId(R.id.book_library_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, clickChildViewWithId(R.id.book_line_favorite)))
            Thread.sleep(waitTime)

            // 2.2-Apagar outro item
            onView(withId(R.id.book_library_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, clickChildViewWithId(R.id.book_line_config)))
            Thread.sleep(waitTime)
            onView(withText(R.string.menu_book_config_delete)).perform(click())
            Thread.sleep(waitTime)
            onView(withText(R.string.action_delete)).perform(click()) 
            Thread.sleep(waitTime)

            // 2.3-Marcar o item como lido (Usando Book Mark)
            onView(withId(R.id.book_library_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, clickChildViewWithId(R.id.book_line_config)))
            Thread.sleep(waitTime)
            onView(withText(R.string.menu_book_config_book_mark)).perform(click())
            Thread.sleep(waitTime)
            // No popup de bookmark, apenas voltamos
            pressBack()
            Thread.sleep(waitTime)

            // 2.4-Limpar um item que estava com o lido
            onView(withId(R.id.book_library_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, clickChildViewWithId(R.id.book_line_config)))
            Thread.sleep(waitTime)
            onView(withText(R.string.menu_book_config_clear_progress)).perform(click())
            Thread.sleep(waitTime)

            // 2.5-Filtrar a pesquisa do item por nome, adicionar autor
            onView(withId(R.id.menu_book_library_search)).perform(click())
            onView(isAssignableFrom(android.widget.EditText::class.java)).perform(typeText("Book @author:Fenix"), pressImeActionButton())
            Thread.sleep(waitTime)

            // 2.6-Limpar a filtragem
            onView(withId(androidx.appcompat.R.id.search_close_btn)).perform(click())
            Thread.sleep(waitTime)
            pressBack() 
            Thread.sleep(waitTime)

            // 2.7-Mudar a ordenação da lista
            onView(withId(R.id.menu_book_library_list_order)).perform(click())
            Thread.sleep(waitTime)

            // 2.7-Abrir o popup de ordenação e mudar a ordem após isso selecionar apenas os favoritos
            onView(withId(R.id.menu_book_library_list_order)).perform(longClick())
            Thread.sleep(waitTime)
            onView(withText(R.string.popup_library_book_tab_item_filter)).perform(click())
            onView(withId(R.id.popup_library_filter_favorite)).perform(click())
            pressBack()
            Thread.sleep(waitTime)

            // 2.8-Abrir com o clique longo os detalhes, rolar um pouco a pagina e voltar para a biblioteca
            onView(withId(R.id.book_library_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, longClick()))
            Thread.sleep(waitTime)
            onView(withId(R.id.book_detail_scroll)).perform(swipeUp())
            Thread.sleep(waitTime)
            pressBack()
            Thread.sleep(waitTime)

            // 2.9-Abrir o item e esperar carregar a tela de leitura
            onView(withId(R.id.book_library_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
            Thread.sleep(waitTime)
            waitForView(withId(R.id.root_activity_book_reader))
        }
    }
}

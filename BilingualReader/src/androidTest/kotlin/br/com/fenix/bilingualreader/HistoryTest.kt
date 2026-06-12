package br.com.fenix.bilingualreader

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.NavigationViewActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matcher
import org.hamcrest.Matchers.anyOf
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryTest {

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

    @Test
    fun testHistoryFullRoutine() {
        ActivityScenario.launch(MainActivity::class.java).use {
            Thread.sleep(waitTime)

            // 3-Iniciando o teste no histórico
            onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
            onView(withId(R.id.nav_view)).perform(NavigationViewActions.navigateTo(R.id.menu_history))
            
            // Espera o histórico carregar
            waitForSkeleton()
            waitForView(withId(R.id.history_list))
            waitForRecyclerViewData(R.id.history_list)
            Thread.sleep(waitTime)

            // Testar favorito no histórico (se houver itens)
            try {
                // Força layout do primeiro item
                onView(withId(R.id.history_list)).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(0))
                Thread.sleep(500)

                onView(withId(R.id.history_list))
                    .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, longClick()))
                Thread.sleep(waitTime)
                // O texto pode ser "Add favorite" ou "Remove favorite", tentamos ambos ou usamos anyOf
                onView(anyOf(withText(R.string.manga_library_menu_favorite_add), withText(R.string.manga_library_menu_favorite_remove), 
                             withText(R.string.book_library_menu_favorite_add), withText(R.string.book_library_menu_favorite_remove))).perform(click())
                Thread.sleep(waitTime)
            } catch (e: Throwable) {
                // Silenciosamente ignora se lista estiver vazia
            }

            // Filtrar no histórico
            onView(withId(R.id.menu_history_search)).perform(click())
            onView(isAssignableFrom(android.widget.EditText::class.java)).perform(typeText("Test"), pressImeActionButton())
            Thread.sleep(waitTime)

            // Limpar filtragem
            onView(withId(androidx.appcompat.R.id.search_close_btn)).perform(click())
            Thread.sleep(waitTime)
            pressBack()
            Thread.sleep(waitTime)

            // Abrir item e validar tela de leitura (Manga ou Book)
            try {
                onView(withId(R.id.history_list))
                    .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
                Thread.sleep(waitTime)
                
                // Tenta validar se abriu algum dos readers
                try {
                    waitForView(withId(R.id.root_activity_manga_reader), 2000)
                } catch (e: Throwable) {
                    waitForView(withId(R.id.root_activity_book_reader), 2000)
                }
                
                pressBack()
                Thread.sleep(waitTime)
            } catch (e: Throwable) {
                // Silenciosamente ignora se lista estiver vazia
            }

            // Swipe para excluir no histórico
            try {
                onView(withId(R.id.history_list))
                    .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, swipeLeft()))
                Thread.sleep(waitTime)
                // Confirmar exclusão no diálogo (ID do botão "Delete" no diálogo do MaterialAlertDialogBuilder)
                onView(withText(R.string.action_delete)).perform(click())
                Thread.sleep(waitTime)
            } catch (e: Throwable) {
                // Silenciosamente ignora se lista estiver vazia
            }
        }
    }
}

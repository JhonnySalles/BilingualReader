package br.com.fenix.bilingualreader.view.ui.help

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import android.view.View
import android.widget.ScrollView
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HelpFragmentTest {

    /**
     * ViewAction personalizada para realizar um scroll instantâneo no ScrollView,
     * garantindo que o delta de movimento seja capturado pela lógica do fragmento.
     */
    private fun scrollBy(x: Int, y: Int): ViewAction = object : ViewAction {
        override fun getConstraints(): Matcher<View> = isAssignableFrom(ScrollView::class.java)
        override fun getDescription(): String = "scroll by $x, $y"
        override fun perform(uiController: UiController, view: View) {
            val scrollView = view as ScrollView
            scrollView.scrollBy(x, y)
            uiController.loopMainThreadUntilIdle()
        }
    }

    @Test
    fun testInitialDisplay() {
        launchFragmentInContainer<HelpFragment>(themeResId = R.style.Theme_MangaReader)

        // Verifica o título principal do índice
        onView(withId(R.id.help_content_title)).check(matches(isDisplayed()))

        // Verifica se alguns tópicos do índice estão visíveis
        onView(withId(R.id.help_library_content)).check(matches(isDisplayed()))
        onView(withId(R.id.help_reader_content)).check(matches(isDisplayed()))
        onView(withId(R.id.help_vocabulary_content)).check(matches(isDisplayed()))

        // O botão de subir deve estar escondido inicialmente
        onView(withId(R.id.help_scroll_up)).check(matches(not(isDisplayed())))
    }

    @Test
    fun testAnchorNavigation() {
        launchFragmentInContainer<HelpFragment>(themeResId = R.style.Theme_MangaReader)

        // Clica no tópico de "Vocabulário" no índice
        onView(withId(R.id.help_vocabulary_content)).perform(click())

        // Aguarda a rolagem suave (smoothScrollTo)
        Thread.sleep(1500)

        // Verifica se o título da seção de Vocabulário está visível na tela
        onView(withId(R.id.help_vocabulary_title)).check(matches(isDisplayed()))
    }

    @Test
    fun testScrollToTopFabBehavior() {
        launchFragmentInContainer<HelpFragment>(themeResId = R.style.Theme_MangaReader)

        // Primeiro, rolar para baixo uns 500px de forma determinística
        onView(withId(R.id.help_scroll_view)).perform(scrollBy(0, 500))
        Thread.sleep(500)
        
        // Garante que o botão ainda está escondido após rolar para baixo
        onView(withId(R.id.help_scroll_up)).check(matches(not(isDisplayed())))

        // Rolar para cima (delta negativo) para disparar o FAB
        // Com o threshold reduzido para -20 no Fragment, -100 é mais que suficiente
        onView(withId(R.id.help_scroll_view)).perform(scrollBy(0, -100))
        
        // Aguarda animação de "show" do FAB
        Thread.sleep(800)

        // Agora o botão de "Scroll Up" deve estar visível
        onView(withId(R.id.help_scroll_up)).check(matches(isDisplayed()))

        // Clica no botão para voltar ao topo
        onView(withId(R.id.help_scroll_up)).perform(click())
        Thread.sleep(2000)

        // Verifica se o título do topo está visível novamente
        onView(withId(R.id.help_content_title)).check(matches(isDisplayed()))
    }

    @Test
    fun testFabAutoHide() {
        launchFragmentInContainer<HelpFragment>(themeResId = R.style.Theme_MangaReader)

        // Rola para baixo e depois para cima para mostrar o FAB
        onView(withId(R.id.help_scroll_view)).perform(scrollBy(0, 500))
        Thread.sleep(500)
        onView(withId(R.id.help_scroll_view)).perform(scrollBy(0, -100))
        
        Thread.sleep(800)
        onView(withId(R.id.help_scroll_up)).check(matches(isDisplayed()))

        // Aguarda o auto-hide (o fragmento usa 3000ms)
        Thread.sleep(4000)

        // O botão deve ter sumido
        onView(withId(R.id.help_scroll_up)).check(matches(not(isDisplayed())))
    }
}

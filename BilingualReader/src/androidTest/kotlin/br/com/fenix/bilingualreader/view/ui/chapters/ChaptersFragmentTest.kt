package br.com.fenix.bilingualreader.view.ui.chapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Chapters
import br.com.fenix.bilingualreader.service.repository.SharedData
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.view.ui.menu.MenuActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChaptersFragmentTest {

    private val mockHeader = "Vol 1"
    private val mockPageTitle = "Page 1 - Chapter 1"
    private val mockPageNumber = 1
    
    @Before
    fun setup() {
        SharedData.clearChapters()
        
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val list = mutableListOf<Chapters>()
        
        // Adiciona um Header
        list.add(Chapters(mockHeader, 0, 0, 1.0f, true))
        
        // Adiciona uma Página
        list.add(Chapters(mockPageTitle, mockPageNumber, 0, 1.0f, false).apply {
            image = bitmap
        })
        
        SharedData.setChapters(null, list)
    }

    private fun getStartIntent(): Intent {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Intent(context, MenuActivity::class.java).apply {
            putExtra(GeneralConsts.KEYS.FRAGMENT.ID, R.id.frame_chapters)
            putExtra(GeneralConsts.KEYS.CHAPTERS.TITLE, "Mocked Chapters View")
            putExtra(GeneralConsts.KEYS.CHAPTERS.PAGE, 0)
        }
    }

    @Test
    fun testChaptersListIsDisplayed() {
        val scenario = ActivityScenario.launch<MenuActivity>(getStartIntent())
        
        // Aguarda renderização (SharedData é síncrono no mock mas o adapter demora um pouco)
        Thread.sleep(1000)
        
        // Verifica se a lista está visível
        onView(withId(R.id.chapters_recycler_view)).check(matches(isDisplayed()))
        
        // Verifica se o Header está visível
        onView(withText(mockHeader)).check(matches(isDisplayed()))
        
        // Verifica se o número da página está visível no card
        onView(withText(mockPageNumber.toString())).check(matches(isDisplayed()))
    }

    @Test
    fun testCardClickReturnsResult() {
        val scenario = ActivityScenario.launch<MenuActivity>(getStartIntent())
        
        Thread.sleep(1000)

        // Clica na página
        onView(withText(mockPageNumber.toString())).perform(click())
        
        // Verifica se a Activity encerrou com RESULT_OK
        assertEquals(Activity.RESULT_OK, scenario.result.resultCode)
        
        // Verifica se o Bundle de retorno contém os dados esperados
        val resultData = scenario.result.resultData
        assertNotNull(resultData)
        val extras = resultData.extras
        assertNotNull(extras)
        
        assertEquals(mockPageTitle, extras?.getString(GeneralConsts.KEYS.CHAPTERS.TITLE))
        assertEquals(mockPageNumber, extras?.getInt(GeneralConsts.KEYS.CHAPTERS.NUMBER))
    }

    @Test
    fun testCardLongClickOpensDetailPopup() {
        val scenario = ActivityScenario.launch<MenuActivity>(getStartIntent())
        
        Thread.sleep(1000)

        // Realiza o Clique Longo no card
        // Note: Clicamos no texto que está dentro do card ou no próprio root de cada item
        onView(withText(mockPageNumber.toString())).perform(longClick())
        
        // Verifica se o Popup apareceu checando o título da página no detalhe
        // R.id.popup_chapter_name está no layout popup_chapter_detail
        onView(withId(R.id.popup_chapter_name)).check(matches(isDisplayed()))
        onView(withText(mockPageTitle)).check(matches(isDisplayed()))
        
        // Clica no background para fechar
        onView(withId(R.id.popup_chapter_background)).perform(click())
        
        // Verifica se o popup sumiu (não deve mais encontrar a view)
        Thread.sleep(500)
        onView(withId(R.id.popup_chapter_name)).check(doesNotExist())
    }
}

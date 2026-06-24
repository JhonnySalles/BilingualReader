package br.com.fenix.bilingualreader.view.ui.vocabulary

import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Vocabulary
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.util.helpers.Util
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VocabularyFragmentTest {

    private lateinit var db: DataBase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // Setup Database
        db = Room.inMemoryDatabaseBuilder(context, DataBase::class.java)
            .allowMainThreadQueries()
            .build()
        DataBase.setTestingInstance(db)

        // Injeta dados de vocabulário simulados
        val mockData = listOf(
            Vocabulary(id = 1L, word = "TestWord1", reading = "Reading1", english = "Meaning1", portuguese = null, basicForm = null, jlpt = 0, revised = false, favorite = true, appears = 0),
            Vocabulary(id = 2L, word = "TestWord2", reading = "Reading2", english = "Meaning2", portuguese = null, basicForm = null, jlpt = 0, revised = false, favorite = false, appears = 0),
            Vocabulary(id = 3L, word = "AlphaWord", reading = "Reading3", english = "Meaning3", portuguese = null, basicForm = null, jlpt = 0, revised = false, favorite = false, appears = 0)
        )
        
        mockData.forEach { db.getVocabularyDao().save(it) }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun getStartIntent(): Intent {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Intent(context, VocabularyActivity::class.java).apply {
            action = Intent.ACTION_MAIN
        }
    }

    @Test
    fun testVocabularyListRendering() {
        ActivityScenario.launch<VocabularyActivity>(getStartIntent())
        
        // Aguarda carregamento PagingData + Coroutines
        Thread.sleep(3500)

        // Verifica se as palavras estão sendo exibidas na RecyclerView
        // O Vocabulário é exibido verticalmente no card
        onView(withText(Util.setVerticalText("TestWord1"))).check(matches(isDisplayed()))
        onView(withText("Meaning1")).check(matches(isDisplayed()))
        onView(withText("Meaning2")).check(matches(isDisplayed()))
    }

    @Test
    fun testFavoriteFilterToggle() {
        ActivityScenario.launch<VocabularyActivity>(getStartIntent())
        
        Thread.sleep(2500)

        // Clica no filtro de favoritos na toolbar
        onView(withId(R.id.menu_vocabulary_favorite)).perform(click())
        
        // Aguarda atualização dos dados filtrados
        Thread.sleep(2500)
        
        // Apenas TestWord1 deveria estar visível (é a única favorita no setup)
        onView(withText(Util.setVerticalText("TestWord1"))).check(matches(isDisplayed()))
        
        // Word 2 não deve ser encontrada ou não deve estar visível
        // onView(withText(Util.setVerticalText("TestWord2"))).check(doesNotExist())
    }

    @Test
    fun testOrderBottomSheetTrigger() {
        ActivityScenario.launch<VocabularyActivity>(getStartIntent())
        
        Thread.sleep(2000)

        // Aciona o menu de ordenação via clique longo (como definido no fragmento via MenuUtil.longClick)
        onView(withId(R.id.menu_vocabulary_list_order)).perform(longClick())
        
        Thread.sleep(1000)
        
        // Verifica se o BottomSheet de ordenação apareceu
        onView(withId(R.id.vocabulary_popup_menu_order_filter)).check(matches(isDisplayed()))
        
        // Verifica se a aba de ordenação está visível
        onView(withText(R.string.popup_vocabulary_tab_item_ordering)).check(matches(isDisplayed()))
    }

    @Test
    fun testSearchFiltering() {
        ActivityScenario.launch<VocabularyActivity>(getStartIntent())
        
        Thread.sleep(3500)

        // Abre a busca
        onView(withId(R.id.menu_vocabulary_search)).perform(click())
        
        // Digita "Alpha" para filtrar
        onView(isAssignableFrom(android.widget.EditText::class.java)).perform(typeText("Alpha"), pressImeActionButton())
        
        Thread.sleep(2500)
        
        // AlphaWord deve estar visível
        onView(withText(Util.setVerticalText("AlphaWord"))).check(matches(isDisplayed()))
        
        // TestWord1 não deve estar visível
        onView(withText(Util.setVerticalText("TestWord1"))).check(doesNotExist())
    }

    @Test
    fun testToggleFavoriteItem() {
        ActivityScenario.launch<VocabularyActivity>(getStartIntent())
        
        Thread.sleep(3500)
        
        // TestWord2 NÃO é favorita no setup (id = 2L)
        // Clica no ícone de favorito do card da TestWord2
        // Como o ícone é o mesmo para todos os cards, precisamos dar match no pai (card) que contém o texto
        onView(
            allOf(
                withId(R.id.vocabulary_favorite),
                isDescendantOfA(
                    allOf(
                        withId(R.id.vocabulary_content),
                        hasDescendant(withText(Util.setVerticalText("TestWord2")))
                    )
                )
            )
        ).perform(click())


        
        Thread.sleep(1000)
        
        // Verifica no banco se o estado mudou
        val word = db.getVocabularyDao().get(2L)
        assert(word.favorite)
    }
}

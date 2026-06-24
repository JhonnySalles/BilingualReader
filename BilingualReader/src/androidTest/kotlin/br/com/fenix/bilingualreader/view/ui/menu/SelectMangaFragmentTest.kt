package br.com.fenix.bilingualreader.view.ui.menu

import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.Themes
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import org.hamcrest.Matchers.anything
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SelectMangaFragmentTest {

    private lateinit var db: DataBase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DataBase::class.java)
            .allowMainThreadQueries()
            .build()
        DataBase.setTestingInstance(db)

        // Cria duas bibliotecas
        val lib1 = Library(1L, "Manga Lib A", "/path/a", Libraries.JAPANESE, Type.MANGA)
        val lib2 = Library(2L, "Manga Lib B", "/path/b", Libraries.PORTUGUESE, Type.MANGA)
        db.getLibrariesDao().save(lib1)
        db.getLibrariesDao().save(lib2)

        // Adiciona mangás na Lib A
        db.getMangaDao().save(Manga(1L, 10L, File("/path/a/manga1")).apply { name = "Alpha Manga" })
        db.getMangaDao().save(Manga(1L, 11L, File("/path/a/manga2")).apply { name = "Beta Manga" })

        // Adiciona mangás na Lib B
        db.getMangaDao().save(Manga(2L, 20L, File("/path/b/manga3")).apply { name = "Gamma Manga" })

        // Configura o tema padrão
        GeneralConsts.getSharedPreferences(context).edit()
            .putString(GeneralConsts.KEYS.THEME.THEME_USED, Themes.ORIGINAL.toString())
            .apply()
    }

    @After
    fun closeDb() {
        db.close()
    }

    private fun getStartIntent(): Intent {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MenuActivity::class.java)
        intent.putExtra(GeneralConsts.KEYS.FRAGMENT.ID, R.id.frame_select_manga)
        return intent
    }

    @Test
    fun testMangaListRendering() {
        ActivityScenario.launch<MenuActivity>(getStartIntent())
        
        Thread.sleep(2000)

        // Verifica o título da toolbar (biblioteca ativa)
        // Por padrão no ViewModel ele pega a default ou a primeira
        // Vamos checar se pelo menos um mangá da Lib A aparece
        onView(withText("Alpha Manga")).check(matches(isDisplayed()))
        onView(withText("Beta Manga")).check(matches(isDisplayed()))
    }

    @Test
    fun testSearchFiltering() {
        ActivityScenario.launch<MenuActivity>(getStartIntent())
        
        Thread.sleep(2000)

        // Abre a busca
        onView(withId(R.id.menu_select_manga_search)).perform(click())
        
        // Digita "Alpha"
        onView(isAssignableFrom(android.widget.EditText::class.java))
            .perform(typeText("Alpha"), pressImeActionButton())

        Thread.sleep(1000)

        // Valida que apenas Alpha aparece
        onView(withText("Alpha Manga")).check(matches(isDisplayed()))
        onView(withText("Beta Manga")).check(doesNotExist())
    }

    @Test
    fun testLibrarySwitchingViaContextMenu() {
        ActivityScenario.launch<MenuActivity>(getStartIntent())
        
        Thread.sleep(2000)

        // Realiza clique longo no título para abrir o ContextMenu das bibliotecas
        onView(withId(R.id.toolbar_select_manga_title)).perform(longClick())

        // Usa onData para selecionar a biblioteca "Manga Lib B" no popup
        // Em Android ContextMenu, as opções são renderizadas em um ListView interno do popup
        onData(anything())
            .atPosition(1) // Manga Lib B (Lib A é pos 0)
            .perform(click())

        Thread.sleep(2000)

        // Agora a lista deve mostrar o mangá da Lib B e não mais da Lib A
        onView(withText("Gamma Manga")).check(matches(isDisplayed()))
        onView(withText("Alpha Manga")).check(doesNotExist())
    }
}

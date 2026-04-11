package br.com.fenix.bilingualreader.view.ui.menu

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.room.Room
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.Themes
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import org.hamcrest.Matchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConfigLibrariesFragmentTest {

    private lateinit var db: DataBase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DataBase::class.java)
            .allowMainThreadQueries()
            .build()
        DataBase.setTestingInstance(db)

        // Pre-popula com algumas bibliotecas
        db.getLibrariesDao().save(Library(1L, "Manga Japanese", "/path/manga/jp", Libraries.JAPANESE, Type.MANGA))
        db.getLibrariesDao().save(Library(2L, "Book English", "/path/book/en", Libraries.ENGLISH, Type.BOOK))

        // Configura o tema padrão para a Activity não quebrar no init
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
        intent.putExtra(GeneralConsts.KEYS.FRAGMENT.ID, R.id.frame_config_libraries)
        intent.putExtra(GeneralConsts.KEYS.LIBRARY.LIBRARY_TYPE, Type.MANGA.toString())
        return intent
    }

    @Test
    fun testLibraryListRendering() {
        ActivityScenario.launch<MenuActivity>(getStartIntent())
        
        Thread.sleep(2000)

        // Verifica se as bibliotecas salvas aparecem na lista
        onView(withText("Manga Japanese")).check(matches(isDisplayed()))
    }

    @Test
    fun testAddLibraryDialogAndValidation() {
        ActivityScenario.launch<MenuActivity>(getStartIntent())
        
        Thread.sleep(2000)

        // Clica no botão de adicionar
        onView(withId(R.id.config_library_add_button)).perform(click())

        // Verifica se o dialog abriu verificando o título
        onView(withText(R.string.config_libraries_add_library)).check(matches(isDisplayed()))

        // Tenta salvar sem preencher nada para disparar a validação
        onView(withText(R.string.action_positive)).perform(click())

        // Verifica se as mensagens de erro de validação apareceram
        onView(withText(R.string.config_libraries_title_library_required)).check(matches(isDisplayed()))
        
        // Clica em cancelar para fechar
        onView(withText(R.string.action_negative)).perform(click())
        
        onView(withText(R.string.config_libraries_add_library)).check(doesNotExist())
    }

    @Test
    fun testDeleteLibrarySwipe() {
        ActivityScenario.launch<MenuActivity>(getStartIntent())
        
        Thread.sleep(2000)

        // Realiza swipe left no primeiro item
        onView(withId(R.id.rv_config_library_list))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, swipeLeft()))

        Thread.sleep(1000)

        // Verifica se o diálogo de confirmação apareceu
        onView(withText(R.string.config_libraries_delete_library)).check(matches(isDisplayed()))
        onView(withText(containsString("Manga Japanese"))).check(matches(isDisplayed()))

        // Clica em cancelar (dismiss)
        onView(withText(R.string.action_negative)).perform(click())

        // O item deve voltar a aparecer na lista
        onView(withText("Manga Japanese")).check(matches(isDisplayed()))
    }
}

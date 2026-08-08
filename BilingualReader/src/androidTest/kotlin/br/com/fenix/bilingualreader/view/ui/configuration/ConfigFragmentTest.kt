package br.com.fenix.bilingualreader.view.ui.configuration

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.NavigationViewActions
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.fenix.bilingualreader.MainActivity
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConfigFragmentTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            // Zera o estado de mudança de tema para não forçar abertura automática da config
            val context = ApplicationProvider.getApplicationContext<Context>()
            val prefs = GeneralConsts.getSharedPreferences(context)
            prefs.edit().putBoolean(GeneralConsts.KEYS.THEME.THEME_CHANGE, false).commit()
        }
    }

    private fun navigateToConfig() {
        // Abre o Drawer
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
        
        // Clica no item de configuração no NavigationView
        onView(withId(R.id.nav_view)).perform(NavigationViewActions.navigateTo(R.id.menu_configuration))
        
        // Aguarda a transição do fragmento
        Thread.sleep(1000)
    }

    @Test
    fun testSectionsVisibility() {
        ActivityScenario.launch(MainActivity::class.java)
        navigateToConfig()

        // Verifica se os títulos das seções principais estão visíveis (usando IDs de containers ou labels internos)
        // Como o layout é grande, fazemos scroll para garantir
        onView(withId(R.id.config_manga_libraries)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withId(R.id.config_book_libraries)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withId(R.id.config_system_backup)).perform(scrollTo()).check(matches(isDisplayed()))
    }

    @Test
    fun testSwitchInteractionPersists() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = GeneralConsts.getSharedPreferences(context)
        
        // Garante estado inicial conhecido
        prefs.edit().putBoolean(GeneralConsts.KEYS.READER.MANGA_SHOW_CLOCK_AND_BATTERY, false).commit()

        val scenario = ActivityScenario.launch(MainActivity::class.java)
        navigateToConfig()

        // Localiza o switch de relógio/bateria dos mangás
        val switchId = R.id.config_manga_reader_switch_show_clock_and_battery
        onView(withId(switchId)).perform(scrollTo(), click())
        
        // Verifica se visualmente mudou (opcional, foco é persistência)
        onView(withId(switchId)).check(matches(isChecked()))

        // Fecha a activity para disparar o saveConfig() no onDestroyView/onStop do Fragment
        scenario.close()
        
        // Verifica se salvou no SharedPreferences
        val isChecked = prefs.getBoolean(GeneralConsts.KEYS.READER.MANGA_SHOW_CLOCK_AND_BATTERY, false)
        assertTrue("O valor não foi persistido no SharedPreferences após o clique", isChecked)
    }

    @Test
    fun testClearCoversDialog() {
        ActivityScenario.launch(MainActivity::class.java)
        navigateToConfig()

        // Scroll até o botão de deletar capas
        onView(withId(R.id.config_covers_delete)).perform(scrollTo(), click())

        // Verifica o diálogo de confirmação (usando matches displayed para garantir que pegamos o título do diálogo e não o botão que pode ter o mesmo texto)
        onView(allOf(withText(R.string.config_covers_delete_title), isDisplayed())).check(matches(isDisplayed()))
        
        // Clica em cancelar
        onView(withText(R.string.action_cancel)).perform(click())
        
        // Verifica se o diálogo sumiu (o botão continua lá, então checamos se não há NENHUMA view com esse texto SENDO EXIBIDA como título de diálogo)
        // Como o ID do botão é único, podemos checar se o que sobrou é apenas o botão
        onView(allOf(withText(R.string.config_covers_delete_title), isDisplayed())).check(matches(withId(R.id.config_covers_delete)))
    }

    @Test
    fun testClearStatisticsDialog() {
        ActivityScenario.launch(MainActivity::class.java)
        navigateToConfig()

        // Scroll até o botão de deletar estatísticas
        onView(withId(R.id.config_statistics_delete)).perform(scrollTo(), click())

        // Verifica o diálogo
        onView(allOf(withText(R.string.config_statistics_clear_title), isDisplayed())).check(matches(isDisplayed()))
        
        // Cancela
        onView(withText(R.string.action_cancel)).perform(click())
        
        Thread.sleep(500)
        // Verifica que apenas o botão (com o mesmo texto) sobrou exibido
        onView(allOf(withText(R.string.config_statistics_clear_title), isDisplayed())).check(matches(withId(R.id.config_statistics_delete)))
    }
}

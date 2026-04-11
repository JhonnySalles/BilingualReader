package br.com.fenix.bilingualreader.view.ui.touch_screen

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.Position
import br.com.fenix.bilingualreader.model.enums.TouchScreen
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.TouchUtil.TouchUtils
import br.com.fenix.bilingualreader.view.ui.menu.MenuActivity
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TouchScreenFragmentTest {

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = GeneralConsts.getSharedPreferences(context)
        
        // Reseta as preferências de toque para um estado conhecido antes de cada teste
        with(prefs.edit()) {
            this.putString(GeneralConsts.KEYS.TOUCH.MANGA_TOP, TouchScreen.TOUCH_CHAPTER_LIST.toString())
            this.putString(GeneralConsts.KEYS.TOUCH.MANGA_BOTTOM, TouchScreen.TOUCH_PAGE_MARK.toString())
            this.commit()
        }
    }

    private fun getStartIntent(): Intent {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, MenuActivity::class.java)
        val bundle = Bundle()
        bundle.putInt(GeneralConsts.KEYS.FRAGMENT.ID, R.id.frame_config_touch)
        bundle.putSerializable(GeneralConsts.KEYS.OBJECT.TYPE, Type.MANGA)
        intent.putExtras(bundle)
        return intent
    }

    @Test
    fun testTouchZonesDisplay() {
        ActivityScenario.launch<MenuActivity>(getStartIntent())
        
        Thread.sleep(2000)

        // Verifica se a zona TOP exibe o texto configurado no setup (TOUCH_CHAPTER_LIST)
        // O método getDescription(TouchScreen) busca o R.string associado ao enum
        val context = ApplicationProvider.getApplicationContext<Context>()
        val expectedTopText = context.getString(TouchScreen.TOUCH_CHAPTER_LIST.getValue())
        val expectedBottomText = context.getString(TouchScreen.TOUCH_PAGE_MARK.getValue())

        onView(withId(R.id.touch_screen_config_top)).check(matches(withText(expectedTopText)))
        onView(withId(R.id.touch_screen_config_bottom)).check(matches(withText(expectedBottomText)))
    }

    @Test
    fun testChangeTouchZoneDialog() {
        ActivityScenario.launch<MenuActivity>(getStartIntent())
        
        Thread.sleep(2000)

        // Clica na zona TOP para abrir o diálogo de alteração
        onView(withId(R.id.touch_screen_config_top)).perform(click())
        
        // Verifica se o diálogo de troca apareceu
        onView(withText(R.string.reading_touch_screen_change_title)).check(matches(isDisplayed()))
        
        // Tenta selecionar uma nova opção na lista (ex: "Não atribuído")
        val context = ApplicationProvider.getApplicationContext<Context>()
        val unassignedText = context.getString(TouchScreen.TOUCH_NOT_ASSIGNED.getValue())
        
        onView(withText(unassignedText)).perform(click())
        
        // Verifica se a view atualizou para a nova opção
        Thread.sleep(500)
        onView(withId(R.id.touch_screen_config_top)).check(matches(withText(unassignedText)))
    }

    @Test
    fun testResetToDefault() {
        ActivityScenario.launch<MenuActivity>(getStartIntent())
        
        Thread.sleep(2000)

        // Clica no botão de restaurar padrão
        onView(withId(R.id.touch_screen_config_default)).perform(click())
        
        Thread.sleep(500)
        
        // Verifica se a zona TOP agora reflete o padrão do TouchUtils (no Manga costuma ser TOUCH_PREVIOUS_FILE ou similar)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val defaultTop = TouchUtils.getDefault(Type.MANGA)[Position.TOP]!!
        val defaultTopText = context.getString(defaultTop.getValue())
        
        onView(withId(R.id.touch_screen_config_top)).check(matches(withText(defaultTopText)))
    }
}

package br.com.fenix.bilingualreader.view.ui.menu

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MenuActivityTest {

    @Test
    fun testLaunchWithConfigLibrariesLoadsFragment() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, MenuActivity::class.java).apply {
            putExtra(GeneralConsts.KEYS.FRAGMENT.ID, R.id.frame_config_libraries)
        }

        ActivityScenario.launch<MenuActivity>(intent).use {
            // Verifica se o fragmento de bibliotecas foi carregado (pode verificar um ID de view do fragmento)
            onView(withId(R.id.root_frame_menu)).check(matches(isDisplayed()))
            onView(withId(R.id.rv_config_library_list)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun testLaunchWithTouchScreenConfigLoadsFragment() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, MenuActivity::class.java).apply {
            putExtra(GeneralConsts.KEYS.FRAGMENT.ID, R.id.frame_touch_screen_config)
            putExtra(GeneralConsts.KEYS.OBJECT.TYPE, br.com.fenix.bilingualreader.model.enums.Type.MANGA)
        }

        ActivityScenario.launch<MenuActivity>(intent).use {
            onView(withId(R.id.frame_touch_screen_config)).check(matches(isDisplayed()))
        }
    }
}

package br.com.fenix.bilingualreader.view.ui.statistics

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Statistics
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.view.ui.menu.MenuActivity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatisticsFragmentTest {

    private lateinit var db: DataBase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // Setup Database
        db = Room.inMemoryDatabaseBuilder(context, DataBase::class.java)
            .allowMainThreadQueries()
            .build()
        DataBase.setTestingInstance(db)

        // Injeta dados de estatística simulados no Banco
        // Assumindo que o StatisticsRepository lê da tabela de estatísticas
        val mockMangaStats = Statistics(
            type = Type.MANGA,
            reading = 5,
            toRead = 10,
            library = 50,
            read = 3,
            completeReadingPages = 150,
            completeReadingSeconds = 3600 // 1 hour
        )
        
        val mockBookStats = Statistics(
            type = Type.BOOK,
            reading = 2,
            toRead = 5,
            library = 20,
            read = 1,
            completeReadingPages = 500,
            completeReadingSeconds = 7200 // 2 hours
        )

        db.getStatisticsDao().save(mockMangaStats)
        db.getStatisticsDao().save(mockBookStats)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun getStartIntent(): Intent {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, MenuActivity::class.java)
        val bundle = Bundle()
        // Abre o StatisticsFragment via MenuActivity (devido ao BlurView)
        bundle.putInt(GeneralConsts.KEYS.FRAGMENT.ID, R.id.frame_statistics)
        intent.putExtras(bundle)
        return intent
    }

    @Test
    fun testStatisticsRendering() {
        ActivityScenario.launch<MenuActivity>(getStartIntent())
        
        // Aguarda carregamento do BlurView e Repository
        Thread.sleep(3000)

        // Verifica renderização dos dados de Mangá
        onView(withId(R.id.statistics_manga_reading)).check(matches(withText("5")))
        onView(withId(R.id.statistics_manga_to_read)).check(matches(withText("10")))
        onView(withId(R.id.statistics_manga_read)).check(matches(withText("3")))
        
        // Verifica cálculo de tempo (1h -> "1h")
        // No código: generateSeconds(3600) -> "1h " (com espaço ou sem dependendo do R.string)
        onView(withId(R.id.statistics_manga_completed_time)).check(matches(withText(containsString("1h"))))

        // Verifica renderização dos dados de Livro
        onView(withId(R.id.statistics_book_reading)).check(matches(withText("2")))
        onView(withId(R.id.statistics_book_to_read)).check(matches(withText("5")))
        onView(withId(R.id.statistics_book_completed_time)).check(matches(withText(containsString("2h"))))
    }
}

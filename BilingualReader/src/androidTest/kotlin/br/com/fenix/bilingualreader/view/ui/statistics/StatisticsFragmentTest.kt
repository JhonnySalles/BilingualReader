package br.com.fenix.bilingualreader.view.ui.statistics

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.utils.BookTestUtil
import br.com.fenix.bilingualreader.view.ui.menu.MenuActivity
import junit.framework.TestCase
import org.junit.AfterClass
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
class StatisticsFragmentTest {

    private var intent: Intent? = null

    private val awaitProcess = 2L

    @Test
    fun `1_test_statistics`() {
        val waiter = CountDownLatch(1)
        val scenario = launchFragmentInContainer<StatisticsFragment>()

        scenario.moveToState(Lifecycle.State.RESUMED)


        waiter.await(10, TimeUnit.MINUTES)
    }

}
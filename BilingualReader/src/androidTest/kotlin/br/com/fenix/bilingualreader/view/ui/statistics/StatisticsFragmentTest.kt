package br.com.fenix.bilingualreader.view.ui.statistics

import android.content.Intent
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.FixMethodOrder
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
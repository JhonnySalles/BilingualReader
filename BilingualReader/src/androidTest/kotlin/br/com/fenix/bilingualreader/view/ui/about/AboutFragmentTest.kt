package br.com.fenix.bilingualreader.view.ui.about

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.TestActivity
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anyOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AboutFragmentTest {

    @Before
    fun setUp() {
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    private fun launchFragment() {
        val scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activity ->
            activity.setFragment(AboutFragment())
        }
    }

    @Test
    fun testViewsAreDisplayed() {
        launchFragment()
        onView(withId(R.id.about_app_version_number)).check(matches(isDisplayed()))
        onView(withId(R.id.about_btn_rate_us)).check(matches(isDisplayed()))
        onView(withId(R.id.about_btn_shared)).check(matches(isDisplayed()))
        onView(withId(R.id.about_btn_suggestion)).check(matches(isDisplayed()))
        onView(withId(R.id.about_btn_email)).check(matches(isDisplayed()))
        onView(withId(R.id.about_btn_github)).check(matches(isDisplayed()))
        onView(withId(R.id.about_app_library)).check(matches(isDisplayed()))
    }

    @Test
    fun testAppVersionDisplay() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val expectedVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        
        launchFragment()
        
        onView(withId(R.id.about_app_version_number)).check(matches(withText(expectedVersion)))
    }

    @Test
    fun testGithubButtonClick() {
        launchFragment()

        val context = ApplicationProvider.getApplicationContext<Context>()
        val githubUrl = context.getString(R.string.about_app_github_link)

        intending(hasAction(android.content.Intent.ACTION_VIEW))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))

        onView(withId(R.id.about_btn_github)).perform(click())

        intended(allOf(
            hasAction(android.content.Intent.ACTION_VIEW),
            hasData(githubUrl)
        ))
    }

    @Test
    fun testShareButtonClick() {
        launchFragment()

        intending(hasAction(android.content.Intent.ACTION_SEND))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))

        onView(withId(R.id.about_btn_shared)).perform(click())

        intended(allOf(
            hasAction(android.content.Intent.ACTION_SEND),
            hasType("text/plain")
        ))
    }
}

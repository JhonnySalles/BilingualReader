package br.com.fenix.bilingualreader.util.secrets

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.util.helpers.Telemetry
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SecretsTest {

    @Before
    fun setUp() {
        Telemetry.isEnabled = false
    }

    @After
    fun tearDown() {
        Telemetry.isEnabled = true
    }

    @Test
    fun `test getSecrets singleton initialization`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val secrets = Secrets.getSecrets(context)
        assertNotNull(secrets)

        val malClientId = secrets.getMyAnimeListClientId()
        assertNotNull(malClientId)

        val googleToken = secrets.getGoogleIdToken()
        assertNotNull(googleToken)
    }
}

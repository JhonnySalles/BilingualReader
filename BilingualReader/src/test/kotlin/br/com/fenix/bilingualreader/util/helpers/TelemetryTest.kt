package br.com.fenix.bilingualreader.util.helpers

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TelemetryTest {

    private val crashlytics: FirebaseCrashlytics = mockk(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(Firebase::class)
        mockkStatic("com.google.firebase.crashlytics.ktx.CrashlyticsKt")
        every { Firebase.crashlytics } returns crashlytics
        Telemetry.isEnabled = true
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `recordException should call crashlytics when enabled`() {
        val exception = Exception("Test")
        Telemetry.recordException(exception, "Test message")
        
        verify { crashlytics.setCustomKey("message", "Test message") }
        verify { crashlytics.recordException(exception) }
    }

    @Test
    fun `recordException should not call crashlytics when disabled`() {
        Telemetry.isEnabled = false
        val exception = Exception("Test")
        Telemetry.recordException(exception)
        
        verify(exactly = 0) { crashlytics.recordException(any()) }
    }

    @Test
    fun `setCustomKey should call crashlytics`() {
        Telemetry.setCustomKey("key", "value")
        verify { crashlytics.setCustomKey("key", "value") }
    }
}

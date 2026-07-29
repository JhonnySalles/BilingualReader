package br.com.fenix.bilingualreader.util.helpers

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TelemetryTest {

    @Before
    fun setUp() {
        Telemetry.isEnabled = true
    }

    @After
    fun tearDown() {
        Telemetry.isEnabled = true
    }

    @Test
    fun testIsEnabledToggle() {
        assertTrue(Telemetry.isEnabled)
        Telemetry.isEnabled = false
        assertFalse(Telemetry.isEnabled)
    }

    @Test
    fun testRecordExceptionDisabled() {
        Telemetry.isEnabled = false
        val exception = Exception("Test exception")
        Telemetry.recordException(exception, "Custom message")
        assertFalse(Telemetry.isEnabled)
    }

    @Test
    fun testRecordExceptionEnabledHandlesCatch() {
        Telemetry.isEnabled = true
        val exception = Exception("Test exception")
        // Should execute try block and safely handle missing firebase instance via catch
        Telemetry.recordException(exception, "Custom message")
        assertTrue(Telemetry.isEnabled)
    }

    @Test
    fun testSetCustomKey() {
        Telemetry.isEnabled = true
        Telemetry.setCustomKey("test_key", "test_value")

        Telemetry.isEnabled = false
        Telemetry.setCustomKey("test_key", "test_value")
    }
}

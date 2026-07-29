package br.com.fenix.bilingualreader.service.services

import android.app.Service
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class OnClearFromRecentServiceTest {

    @Test
    fun `service lifecycle should return expected values`() {
        val service = OnClearFromRecentService()

        assertNull(service.onBind(null))
        assertEquals(Service.START_NOT_STICKY, service.onStartCommand(null, 0, 1))

        // Ensure onTaskRemoved and onDestroy complete without exception
        service.onTaskRemoved(Intent())
        service.onDestroy()
    }
}

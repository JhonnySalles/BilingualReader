package br.com.fenix.bilingualreader.service.services

import android.content.Context
import android.content.Intent
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NotificationBroadcastReceiverTest {

    @Test
    fun `onReceive should send broadcast with correct action and extra`() {
        val receiver = NotificationBroadcastReceiver()
        val context = mockk<Context>(relaxed = true)
        val incomingIntent = Intent("ACTION_TEST")

        receiver.onReceive(context, incomingIntent)

        val intentSlot = slot<Intent>()
        verify { context.sendBroadcast(capture(intentSlot)) }

        val broadcast = intentSlot.captured
        assertEquals(NotificationBroadcastReceiver.mIntentAction, broadcast.action)
        assertEquals("ACTION_TEST", broadcast.getStringExtra(NotificationBroadcastReceiver.mIntentExtra))
    }
}

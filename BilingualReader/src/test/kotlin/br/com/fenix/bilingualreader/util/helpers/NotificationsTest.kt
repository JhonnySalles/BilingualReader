package br.com.fenix.bilingualreader.util.helpers

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NotificationsTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    @Test
    fun testGetID() {
        val id1 = Notifications.getID()
        val id2 = Notifications.getID()
        assertEquals(id1 + 1, id2)
    }

    @Test
    fun testGetNotification() {
        val title = "Test Title"
        val content = "Test Content"
        val builder = Notifications.getNotification(context, title, content)

        assertNotNull(builder)
        val notification = builder.build()
        
        // Em Robolectric, podemos verificar os extras da notificação
        assertEquals(title, notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertEquals(content, notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
    }

    @Test
    fun testGetTTSNotification() {
        val title = "TTS Title"
        val content = "TTS Content"
        val icon = mockk<Bitmap>(relaxed = true)
        val receiver = mockk<BroadcastReceiver>(relaxed = true)
        
        val builder = Notifications.getTTSNotification(
            context,
            title,
            content,
            icon,
            hasPrevious = true,
            hasNext = true,
            isPaused = false,
            receiver = receiver
        )

        assertNotNull(builder)
        val notification = builder.build()

        assertEquals(title, notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertEquals(content, notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        
        // Verificando se as ações foram adicionadas (Previous, Play/Pause, Next, Stop)
        assertEquals(4, notification.actions.size)
    }
}

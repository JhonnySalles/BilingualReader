package br.com.fenix.bilingualreader.util.helpers

import android.content.Context
import androidx.core.app.NotificationCompat
import br.com.fenix.bilingualreader.R
import java.util.concurrent.atomic.AtomicInteger




class Notifications {
    companion object NotificationUtils {
        private val id = AtomicInteger(0)
        fun getID(): Int {
            return id.incrementAndGet()
        }

        const val NOTIFICATIONS_CHANNEL_ID = "BilingualReaderChannel"

        fun getNotification(context: Context, title: String, content: String) = NotificationCompat.Builder(context, NOTIFICATIONS_CHANNEL_ID)
            .setSmallIcon(R.mipmap.notification_icon)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(1, 0, true)
    }
}
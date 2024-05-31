package br.com.fenix.bilingualreader.util.helpers

import android.annotation.TargetApi
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import br.com.fenix.bilingualreader.R
import java.util.concurrent.atomic.AtomicInteger


class Notifications {
    companion object NotificationUtils {
        private val id = AtomicInteger(0)
        fun getID(): Int = id.incrementAndGet()

        const val NOTIFICATIONS_CHANNEL_ID = "BilingualReaderChannel"

        const val TTS_ACTION_PREVIUOS = "actionPrevious"
        const val TTS_ACTION_PLAY = "actionPlay"
        const val TTS_ACTION_NEXT = "actionNext"
        const val TTS_ACTION_STOP = "actionStop"

        fun getNotification(context: Context, title: String, content: String) = NotificationCompat.Builder(context, NOTIFICATIONS_CHANNEL_ID)
            .setSmallIcon(R.mipmap.notification_icon)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(1, 0, true)

        @TargetApi(Build.VERSION_CODES.O)
        fun getTTSNotification(
            context: Context, title: String, content: String, icon: Bitmap?, play: PendingIntent? = null, previous: PendingIntent? = null,
            next: PendingIntent? = null, stop: PendingIntent? = null
        ): Notification {
            val btnPlay = if (play != null) R.drawable.ic_tts_play else 0
            val btnPrevious = if (previous != null) R.drawable.ic_tts_previous else 0
            val btnNext = if (next != null) R.drawable.ic_tts_next else 0
            val btnStop = if (stop != null) R.drawable.ic_tts_close else 0

            val mediaSession = MediaSessionCompat(context, "TtsReading")

            val notification = NotificationCompat.Builder(context, NOTIFICATIONS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_tts_audio)
                .setContentTitle(title)
                .setContentText(content)
                .setOnlyAlertOnce(true)
                .setShowWhen(true)
                .addAction(btnPrevious, context.getString(R.string.button_tts_previous), previous)
                .addAction(btnPlay, context.getString(R.string.button_tts_play), play)
                .addAction(btnNext, context.getString(R.string.button_tts_next), next)
                .addAction(btnStop, context.getString(R.string.button_tts_close), stop)
                .setStyle(
                    androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2, 3)
                        .setMediaSession(mediaSession.sessionToken)
                )
                .setPriority(NotificationCompat.PRIORITY_LOW)

            if (icon != null)
                notification.setLargeIcon(icon)

            return notification.build()
        }
    }
}
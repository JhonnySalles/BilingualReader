package br.com.fenix.bilingualreader.util.helpers

import android.annotation.TargetApi
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.service.services.NotificationBroadcastReceiver
import br.com.fenix.bilingualreader.service.services.OnClearFromRecentService
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
        fun getTTSNotification(context: Context, title: String, content: String, icon: Bitmap?, hasPrevious: Boolean, hasNext: Boolean, receiver: BroadcastReceiver): Notification {
            context.registerReceiver(receiver, IntentFilter(NotificationBroadcastReceiver.mIntentAction), Context.RECEIVER_NOT_EXPORTED)
            context.startService(Intent(context, OnClearFromRecentService::class.java))

            val pendingIntentPrevious: PendingIntent? = if (hasPrevious) {
                val intentPrevious = Intent(context, NotificationBroadcastReceiver::class.java)
                    .setAction(Notifications.TTS_ACTION_PREVIUOS)
                PendingIntent.getBroadcast(
                    context, 3,
                    intentPrevious, PendingIntent.FLAG_UPDATE_CURRENT
                )
            } else
                null

            val intentPlay = Intent(context, NotificationBroadcastReceiver::class.java)
                .setAction(Notifications.TTS_ACTION_PLAY)
            val pendingIntentPlay = PendingIntent.getBroadcast(
                context, 1,
                intentPlay, PendingIntent.FLAG_UPDATE_CURRENT
            )

            val pendingIntentNext: PendingIntent? = if (hasNext) {
                val intentNext = Intent(context, NotificationBroadcastReceiver::class.java)
                    .setAction(Notifications.TTS_ACTION_NEXT)
                PendingIntent.getBroadcast(
                    context, 4,
                    intentNext, PendingIntent.FLAG_UPDATE_CURRENT
                )
            } else
                null

            val intentStop = Intent(context, NotificationBroadcastReceiver::class.java)
                .setAction(Notifications.TTS_ACTION_STOP)
            val pendingIntentStop = PendingIntent.getBroadcast(
                context, 2,
                intentStop, PendingIntent.FLAG_UPDATE_CURRENT
            )

            val btnPlay = R.drawable.ic_tts_play
            val btnPrevious = if (pendingIntentPrevious != null) R.drawable.ic_tts_previous else 0
            val btnNext = if (pendingIntentNext != null) R.drawable.ic_tts_next else 0
            val btnStop = R.drawable.ic_tts_close

            val mediaSession = MediaSessionCompat(context, "TtsReading")

            val notification = NotificationCompat.Builder(context, NOTIFICATIONS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_tts_audio)
                .setContentTitle(title)
                .setContentText(content)
                .setOnlyAlertOnce(true)
                .setShowWhen(true)
                .addAction(btnPrevious, context.getString(R.string.button_tts_previous), pendingIntentPrevious)
                .addAction(btnPlay, context.getString(R.string.button_tts_play), pendingIntentPlay)
                .addAction(btnNext, context.getString(R.string.button_tts_next), pendingIntentNext)
                .addAction(btnStop, context.getString(R.string.button_tts_close), pendingIntentStop)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            if (icon != null)
                notification.setLargeIcon(icon)

            return notification.build()
        }
    }
}
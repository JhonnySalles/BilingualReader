package br.com.fenix.bilingualreader.service.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class NotificationActionService : BroadcastReceiver() {

    companion object {
        const val mIntentAction = "READING_BOOK"
        const val mIntentExtra = "AUDIOTTS"
    }

    override fun onReceive(context: Context, intent: Intent) {
        context.sendBroadcast(Intent(mIntentAction).putExtra(mIntentExtra, intent.action))
    }
}
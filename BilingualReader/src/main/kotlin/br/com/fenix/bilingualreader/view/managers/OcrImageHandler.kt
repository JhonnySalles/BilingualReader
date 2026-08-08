package br.com.fenix.bilingualreader.view.managers

import android.os.Handler
import android.os.Looper
import android.os.Message

class OcrImageHandler(private val setText: (String?) -> Unit) : Handler(Looper.getMainLooper()) {
    override fun handleMessage(msg: Message) {
        when (msg.what) {
            1 -> setText(msg.obj as? String)
        }
    }
}

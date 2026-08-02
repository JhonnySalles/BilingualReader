package br.com.fenix.bilingualreader.view.managers

import android.os.Handler
import android.os.Looper
import android.os.Message
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import java.lang.ref.WeakReference

class BookLibraryHandler(listener: Listener) : Handler(Looper.getMainLooper()) {

    interface Listener {
        fun onBookAdd(book: Book)
        fun onBookRemove(book: Book)
        fun onBookUpdateFinished(isProcessed: Boolean)
    }

    private val mListenerRef: WeakReference<Listener> = WeakReference(listener)

    override fun handleMessage(msg: Message) {
        val listener = mListenerRef.get() ?: return
        val obj = msg.obj ?: return

        when (msg.what) {
            GeneralConsts.SCANNER.MESSAGE_BOOK_UPDATED_ADD -> (obj as? Book)?.let { listener.onBookAdd(it) }
            GeneralConsts.SCANNER.MESSAGE_BOOK_UPDATED_REMOVE -> (obj as? Book)?.let { listener.onBookRemove(it) }
            GeneralConsts.SCANNER.MESSAGE_BOOK_UPDATE_FINISHED -> (obj as? Boolean)?.let { listener.onBookUpdateFinished(it) }
        }
    }
}

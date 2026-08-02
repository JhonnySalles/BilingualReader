package br.com.fenix.bilingualreader.view.managers

import android.os.Handler
import android.os.Looper
import android.os.Message
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import java.lang.ref.WeakReference

class MangaLibraryHandler(listener: Listener) : Handler(Looper.getMainLooper()) {

    interface Listener {
        fun onMangaAdd(manga: Manga)
        fun onMangaRemove(manga: Manga)
        fun onMangaUpdateFinished(isProcessed: Boolean)
    }

    private val mListenerRef: WeakReference<Listener> = WeakReference(listener)

    override fun handleMessage(msg: Message) {
        val listener = mListenerRef.get() ?: return
        val obj = msg.obj ?: return

        when (msg.what) {
            GeneralConsts.SCANNER.MESSAGE_MANGA_UPDATED_ADD -> (obj as? Manga)?.let { listener.onMangaAdd(it) }
            GeneralConsts.SCANNER.MESSAGE_MANGA_UPDATED_REMOVE -> (obj as? Manga)?.let { listener.onMangaRemove(it) }
            GeneralConsts.SCANNER.MESSAGE_MANGA_UPDATE_FINISHED -> (obj as? Boolean)?.let { listener.onMangaUpdateFinished(it) }
        }
    }
}

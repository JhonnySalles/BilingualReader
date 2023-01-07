package br.com.fenix.bilingualreader.service.scanner

import android.content.Context
import android.os.Handler
import android.os.Message
import android.os.Process
import br.com.ebook.foobnix.dao2.FileMeta
import br.com.ebook.foobnix.ext.CacheZipUtils
import br.com.ebook.foobnix.ui2.FileMetaCore
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.enums.FileType
import br.com.fenix.bilingualreader.service.controller.BookImageCoverController
import br.com.fenix.bilingualreader.service.repository.Storage
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.LibraryUtil
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference

class ScannerBook(private val context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(ScannerBook::class.java)

    private var mUpdateHandler: MutableList<Handler> = ArrayList()
    private var mUpdateThread: Thread? = null

    private var mLibrary: String = LibraryUtil.getBook(context)
    private var mIsStopped = false
    private var mIsRestarted = false

    private val mRestartHandler: Handler = RestartHandler(this)

    private inner class RestartHandler(scanner: ScannerBook) :
        Handler() {
        private val mScannerRef: WeakReference<ScannerBook> = WeakReference<ScannerBook>(scanner)
        override fun handleMessage(msg: Message) {
            mScannerRef.get()?.scanLibrary()
        }
    }

    // Singleton - One thread initialize only
    companion object {
        private lateinit var INSTANCE: ScannerBook
        fun getInstance(context: Context): ScannerBook {
            if (!::INSTANCE.isInitialized)
                synchronized(ScannerBook::class.java) { // Used for a two or many cores
                    INSTANCE = ScannerBook(context)
                }
            return INSTANCE
        }
    }

    fun isRunning(): Boolean {
        return mUpdateThread != null &&
                mUpdateThread!!.isAlive && mUpdateThread!!.state != Thread.State.TERMINATED && mUpdateThread!!.state != Thread.State.NEW
    }

    fun forceScanLibrary() {
        if (isRunning()) {
            mIsStopped = true
            mIsRestarted = true
        } else
            scanLibrary()
    }

    fun scanLibrary() {
        if (mUpdateThread == null || mUpdateThread!!.state == Thread.State.TERMINATED) {
            val runnable = LibraryUpdateRunnable()
            mUpdateThread = Thread(runnable)
            mUpdateThread!!.priority =
                Process.THREAD_PRIORITY_DEFAULT + Process.THREAD_PRIORITY_LESS_FAVORABLE
            mUpdateThread!!.start()
        }
    }

    fun scanLibrariesSilent() {
        val runnable = LibraryUpdateRunnable()
        val thread = Thread(runnable)
        thread.priority = Process.THREAD_PRIORITY_DEFAULT + Process.THREAD_PRIORITY_LESS_FAVORABLE
        thread.start()
    }


    fun addUpdateHandler(handler: Handler) {
        if (mUpdateHandler.contains(handler))
            removeUpdateHandler(handler)

        mUpdateHandler.add(handler)
    }

    fun removeUpdateHandler(handler: Handler) {
        mUpdateHandler.remove(handler)
    }

    private fun notifyMediaUpdatedAdd(book: Book) {
        val msg = Message()
        msg.obj = book
        msg.what = GeneralConsts.SCANNER.MESSAGE_BOOK_UPDATED_ADD
        notifyHandlers(msg)
    }

    private fun notifyMediaUpdatedRemove(book: Book) {
        val msg = Message()
        msg.obj = book
        msg.what = GeneralConsts.SCANNER.MESSAGE_BOOK_UPDATED_REMOVE
        notifyHandlers(msg)
    }

    private fun notifyLibraryUpdateFinished(isProcessed: Boolean) {
        val msg = Message()
        msg.obj = isProcessed
        msg.what = GeneralConsts.SCANNER.MESSAGE_MANGA_UPDATE_FINISHED
        notifyHandlers(msg, 200)
    }

    private fun notifyHandlers(msg: Message, delay: Int = -1) {
        for (h in mUpdateHandler) {
            try {
                if (h.hasMessages(msg.what, msg.obj))
                    h.removeMessages(msg.what, msg.obj)

                if (delay > -1)
                    h.sendMessageDelayed(msg, 200)
                else
                    h.sendMessage(msg)

            } catch (e: Exception) {
                mLOGGER.error("Error when notify handlers", e)
            }
        }
    }

    private fun generateCover(book: Book) =
        BookImageCoverController.instance.getCoverFromFile(context, book.file)

    private inner class LibraryUpdateRunnable() : Runnable {
        override fun run() {
            var isProcess = false
            try {
                if (mLibrary == "" || !File(mLibrary).exists()) return

                val storage = Storage(context)
                val storageFiles: MutableMap<String, Book> = HashMap()
                val storageDeletes: MutableMap<String, Book> = HashMap()

                // create list of files available in storage
                for (c in storage.listBook()!!)
                    storageFiles[c.title] = c

                for (c in storage.listBookDeleted()!!)
                    storageDeletes[c.title] = c

                var walked = false
                // search and add comics if necessary
                val file = File(mLibrary)
                file.walk().onFail { _, ioException -> mLOGGER.warn("File walk error", ioException) }
                    .filterNot { it.isDirectory }.forEach {
                        walked = true
                        if (mIsStopped) return

                        if (FileType.isBook(it.name)) {
                            if (storageFiles.containsKey(it.name))
                                storageFiles.remove(it.name)
                            else {
                                isProcess = true
                                try {
                                    val ebookMeta = FileMetaCore.get()
                                        .getEbookMeta(it.path, CacheZipUtils.CacheDir.ZipApp, false)
                                    FileMetaCore.get().udpateFullMeta(FileMeta(it.path), ebookMeta)

                                    val book = if (storageDeletes.containsKey(it.name)) {
                                        storageFiles.remove(it.name)
                                        storageDeletes.getValue(it.name)
                                    } else Book(
                                        null,
                                        it,
                                        ebookMeta
                                    )

                                    book.path = it.path
                                    book.folder = it.parent
                                    book.excluded = false
                                    generateCover(book)
                                    book.id = storage.save(book)
                                    notifyMediaUpdatedAdd(book)

                                } catch (e: Exception) {
                                    mLOGGER.error("Error load book " + it.name, e)
                                } catch (e: IOException) {
                                    mLOGGER.error("Error load book " + it.name, e)
                                }
                            }
                        }
                    }

                // delete missing comics
                if (!mIsStopped && !mIsRestarted && walked)
                    for (missing in storageFiles.values) {
                        isProcess = true
                        storage.delete(missing)
                        notifyMediaUpdatedRemove(missing)
                    }
            } finally {
                mIsStopped = false
                if (mIsRestarted) {
                    mIsRestarted = false
                    mRestartHandler.sendEmptyMessageDelayed(1, 200)
                } else
                    notifyLibraryUpdateFinished(isProcess)
            }
        }
    }

}
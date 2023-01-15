package br.com.fenix.bilingualreader.service.scanner

import android.content.Context
import android.os.Handler
import android.os.Message
import android.os.Process
import br.com.ebook.foobnix.dao2.FileMeta
import br.com.ebook.foobnix.ext.CacheZipUtils
import br.com.ebook.foobnix.ui2.FileMetaCore
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.FileType
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.controller.BookImageCoverController
import br.com.fenix.bilingualreader.service.parses.manga.Parse
import br.com.fenix.bilingualreader.service.parses.manga.ParseFactory
import br.com.fenix.bilingualreader.service.parses.manga.RarParse
import br.com.fenix.bilingualreader.service.repository.Storage
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.LibraryUtil
import br.com.fenix.bilingualreader.util.helpers.Util
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.time.LocalDate

class ScannerBook(private val context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(ScannerBook::class.java)

    private var mUpdateHandler: MutableList<Handler> = ArrayList()
    private var mUpdateThread: Thread? = null

    private var mLibrary: Library = LibraryUtil.getDefault(context, Type.BOOK)
    private var mIsStopped = false
    private var mIsRestarted = false

    private val mRestartHandler: Handler = RestartHandler(this)

    private inner class RestartHandler(scanner: ScannerBook) :
        Handler() {
        private val mScannerRef: WeakReference<ScannerBook> = WeakReference<ScannerBook>(scanner)
        override fun handleMessage(msg: Message) {
            mScannerRef.get()?.scanLibrary(mLibrary)
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

    fun forceScanLibrary(library: Library) {
        if (isRunning()) {
            mIsStopped = true
            mIsRestarted = true
        } else
            scanLibrary(library)
    }

    fun stopScan() {
        mIsStopped = true
    }

    fun scanLibrary(library: Library) {
        if (mUpdateThread == null || mUpdateThread!!.state == Thread.State.TERMINATED) {
            val runnable = LibraryUpdateRunnable(library)
            mUpdateThread = Thread(runnable)
            mUpdateThread!!.priority =
                Process.THREAD_PRIORITY_DEFAULT + Process.THREAD_PRIORITY_LESS_FAVORABLE
            mUpdateThread!!.start()
        }
    }

    fun scanLibrariesSilent(libraries: List<Library>?) {
        if (libraries == null || libraries.isEmpty())
            return

        val runnable = LibrariesUpdateRunnable(libraries, isSilent = true)
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
        mIsStopped = true
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
        msg.what = GeneralConsts.SCANNER.MESSAGE_BOOK_UPDATE_FINISHED
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


    private inner class LibraryUpdateRunnable(var library: Library) : Runnable {
        override fun run() {
            var isProcess = false
            try {
                val libraryPath = library.path
                if (libraryPath == "" || !File(libraryPath).exists()) return

                mIsStopped = false
                val storage = Storage(context)
                val storageFiles: MutableMap<String, Book> = HashMap()
                val storageDeletes: MutableMap<String, Book> = HashMap()

                // create list of files available in storage
                for (c in storage.listBook(library)!!)
                    storageFiles[c.path] = c

                for (c in storage.listBookDeleted(library)!!)
                    storageDeletes[c.name] = c

                var walked = false
                // search and add comics if necessary
                val file = File(libraryPath)
                file.walk().onFail { _, ioException -> mLOGGER.warn("File walk error", ioException) }
                    .filterNot { it.isDirectory }.forEach {
                        walked = true
                        if (mIsStopped) return

                        if (FileType.isBook(it.name)) {
                            if (storageFiles.containsKey(it.path))
                                storageFiles.remove(it.path)
                            else {
                                isProcess = true
                                try {

                                    val book = if (storageDeletes.containsKey(it.nameWithoutExtension)) {
                                        storageFiles.remove(it.nameWithoutExtension)
                                        storageDeletes.getValue(it.nameWithoutExtension)
                                    } else {
                                        val ebookMeta = FileMetaCore.get()
                                            .getEbookMeta(it.path, CacheZipUtils.CacheDir.ZipApp, false)
                                        FileMetaCore.get().udpateFullMeta(FileMeta(it.path), ebookMeta)
                                        Book(library.id,null,  it,  ebookMeta)
                                    }

                                    book.path = it.path
                                    book.folder = it.parent
                                    book.excluded = false
                                    generateCover(book)
                                    book.id = storage.save(book)
                                    book.lastVerify = LocalDate.now()
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

    private inner class LibrariesUpdateRunnable(val libraries: List<Library>, val isSilent: Boolean = false) : Runnable {
        override fun run() {
            try {
                val storage = Storage(context)

                for (library in libraries) {
                    val libraryPath = library.path
                    if (libraryPath == "" || !File(libraryPath).exists())
                        continue

                    val storageFiles: MutableMap<String, Book> = HashMap()
                    val storageDeletes: MutableMap<String, Book> = HashMap()

                    // create list of files available in storage
                    for (c in storage.listBook(library)!!)
                        storageFiles[c.path] = c

                    for (c in storage.listBookDeleted(library)!!)
                        storageDeletes[c.name] = c

                    val file = File(libraryPath)
                    file.walk().onFail { _, ioException -> mLOGGER.warn("File walk libraries error", ioException) }
                        .filterNot { it.isDirectory }.forEach {
                            if (FileType.isBook(it.name)) {
                                if (storageFiles.containsKey(it.path))
                                    storageFiles.remove(it.path)
                                else {
                                    try {
                                        val book = if (storageDeletes.containsKey(it.nameWithoutExtension)) {
                                            storageFiles.remove(it.nameWithoutExtension)
                                            storageDeletes.getValue(it.nameWithoutExtension)
                                        } else {
                                            val ebookMeta = FileMetaCore.get()
                                                .getEbookMeta(it.path, CacheZipUtils.CacheDir.ZipApp, false)
                                            FileMetaCore.get().udpateFullMeta(FileMeta(it.path), ebookMeta)
                                            Book(library.id,null,  it,  ebookMeta)
                                        }

                                        val item = storage.findBookByPath(it.path)
                                        if (item == null || item.lastVerify != LocalDate.now()) {
                                            book.path = it.path
                                            book.folder = it.parent
                                            book.excluded = false
                                            book.lastVerify = LocalDate.now()

                                            if (!isSilent)
                                                generateCover(book)

                                            book.id = storage.save(book)
                                        }
                                    } catch (e: Exception) {
                                        mLOGGER.error("Error load book on library " + it.name, e)
                                    } catch (e: IOException) {
                                        mLOGGER.error("Error load book on library " + it.name, e)
                                    }
                                }
                            }
                        }
                }
            } catch (e: Exception) {
                mLOGGER.error("Error thread libraries", e)
            }
        }
    }

}
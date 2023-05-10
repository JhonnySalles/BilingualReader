package br.com.fenix.bilingualreader.service.scanner

import android.content.Context
import android.os.Handler
import android.os.Message
import android.os.Process
import br.com.ebook.foobnix.entity.FileMeta
import br.com.ebook.foobnix.ext.CacheZipUtils
import br.com.ebook.foobnix.sys.ImageExtractor
import br.com.ebook.foobnix.entity.FileMetaCore
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.enums.FileType
import br.com.fenix.bilingualreader.service.controller.BookImageCoverController
import br.com.fenix.bilingualreader.service.repository.Storage
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.time.LocalDate

class ScannerBook(private val context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(ScannerBook::class.java)

    private val mImgExtractor = ImageExtractor.getInstance(context)
    private var mUpdateHandler: MutableList<Handler> = ArrayList()
    private var mRunning = mutableMapOf<Library, ThreadRunning>()

    private inner class RestartHandler(scanner: ScannerBook, var library: Library) :
        Handler() {
        private val mScannerRef: WeakReference<ScannerBook> = WeakReference<ScannerBook>(scanner)
        override fun handleMessage(msg: Message) {
            mScannerRef.get()?.scanLibrary(library)
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

    fun isRunning(library: Library): Boolean {
        return mRunning.isNotEmpty() && mRunning.containsKey(library) && mRunning[library]!!.mThread.isAlive
            mRunning[library]!!.mThread.state != Thread.State.TERMINATED && mRunning[library]!!.mThread.state != Thread.State.NEW
    }

    fun forceScanLibrary(library: Library) {
        if (isRunning(library)) {
            mRunning[library]!!.mIsStopped = true
            mRunning[library]!!.mIsRestarted = true
        } else
            scanLibrary(library)
    }

    fun stopScan() {
        if (mRunning.isNotEmpty())
            for (running in mRunning.values)
                running.mIsStopped = true
    }

    fun scanLibrary(library: Library) {
        if (mRunning.isNotEmpty() && mRunning.containsKey(library)) {
            val run = mRunning.remove(library)!!
            if (run.mThread.state != Thread.State.TERMINATED)
                run.mIsStopped = true
        }

        val runnable = LibraryUpdateRunnable(library)

        val thread = Thread(runnable)
        thread.priority =
            Process.THREAD_PRIORITY_DEFAULT + Process.THREAD_PRIORITY_LESS_FAVORABLE
        mRunning[library] = ThreadRunning(thread)
        thread.start()
    }

    fun scanLibrariesSilent(libraries: List<Library>?) {
        if (libraries == null || libraries.isEmpty())
            return

        for (library in libraries) {
            val runnable = LibraryUpdateRunnable(library, isSilent = true)
            val thread = Thread(runnable)
            thread.priority =
                Process.THREAD_PRIORITY_DEFAULT + Process.THREAD_PRIORITY_LESS_FAVORABLE
            mRunning[library] = ThreadRunning(thread)
            thread.start()
        }
    }

    fun addUpdateHandler(handler: Handler) {
        if (mUpdateHandler.contains(handler))
            removeUpdateHandler(handler)

        mUpdateHandler.add(handler)
    }

    fun removeUpdateHandler(handler: Handler) {
        stopScan()
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

    private inner class ThreadRunning(thread: Thread) {
        val mThread = thread
        var mIsStopped = false
        var mIsRestarted = false
    }

    private inner class LibraryUpdateRunnable(var library: Library, val isSilent: Boolean = false) : Runnable {
        override fun run() {
            var isProcess = false
            try {
                val libraryPath = library.path
                if (libraryPath == "" || !File(libraryPath).exists()) return

                mRunning[library]!!.mIsStopped = false
                val storage = Storage(context)
                val storageFiles: MutableMap<String, Book> = HashMap()
                val storageDeletes: MutableMap<String, Book> = HashMap()

                // create list of files available in storage
                for (c in storage.listBook(library))
                    storageFiles[c.path] = c

                for (c in storage.listBookDeleted(library))
                    storageDeletes[c.name] = c

                var walked = false
                // search and add comics if necessary
                val file = File(libraryPath)
                file.walk().onFail { _, ioException -> mLOGGER.warn("File walk error", ioException) }
                    .filterNot { it.isDirectory }.forEach {
                        walked = true
                        if (mRunning[library]!!.mIsStopped) return

                        if (FileType.isBook(it.name)) {
                            if (storageFiles.containsKey(it.path))
                                storageFiles.remove(it.path)
                            else {
                                isProcess = true
                                try {

                                    val book = if (storageDeletes.containsKey(it.nameWithoutExtension)) {
                                        storageFiles.remove(it.nameWithoutExtension)
                                        storageDeletes.getValue(it.nameWithoutExtension)
                                    } else if (storage.findBookByPath(it.path) != null)
                                        return
                                    else {
                                        val ebookMeta = FileMetaCore.get()
                                            .getEbookMeta(it.path, CacheZipUtils.CacheDir.ZipApp, false)
                                        FileMetaCore.get().udpateFullMeta(FileMeta(it.path), ebookMeta)
                                        Book(library.id,null,  it,  ebookMeta)
                                    }

                                    book.path = it.path
                                    book.folder = it.parent
                                    book.excluded = false

                                    if (!isSilent)
                                        generateCover(book)

                                    book.id = storage.save(book)
                                    book.lastVerify = LocalDate.now()
                                    if (!isSilent)
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
                if (!mRunning[library]!!.mIsStopped && !mRunning[library]!!.mIsRestarted && walked)
                    for (missing in storageFiles.values) {
                        isProcess = true
                        storage.delete(missing)
                        if (!isSilent)
                            notifyMediaUpdatedRemove(missing)
                    }
            } finally {
                val run = mRunning.remove(library)!!
                run.mIsStopped = false
                if (run.mIsRestarted) {
                    run.mIsRestarted = false
                    val mRestartHandler: Handler = RestartHandler(this@ScannerBook, library)
                    mRestartHandler.sendEmptyMessageDelayed(1, 200)
                } else if (!isSilent)
                    notifyLibraryUpdateFinished(isProcess)
            }
        }
    }

}
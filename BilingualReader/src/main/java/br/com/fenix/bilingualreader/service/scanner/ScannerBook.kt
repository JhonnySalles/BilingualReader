package br.com.fenix.bilingualreader.service.scanner

import android.content.Context
import android.os.Handler
import android.os.Message
import android.os.Process
import br.com.ebook.foobnix.entity.FileMeta
import br.com.ebook.foobnix.entity.FileMetaCore
import br.com.ebook.foobnix.ext.CacheZipUtils
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
import java.util.UUID

class ScannerBook(private val context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(ScannerBook::class.java)

    private var mUpdateHandler: MutableList<Handler> = ArrayList()
    private var mThreads = mutableMapOf<UUID, LibraryUpdateRunnable>()
    private var mRunning = mutableMapOf<Library, LibraryUpdateRunnable>()

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

    private fun stopScan(library: Library) {
        if (mThreads.isNotEmpty())
            for (thread in mThreads)
                if (thread.value.mLibrary.id == library.id)
                    thread.value.mIsStopped = true
    }

    fun scanLibrary(library: Library) {
        if (isRunning(library))
            return

        stopScan(library)
        val id = UUID.randomUUID()
        val runnable = LibraryUpdateRunnable(id, library)
        val thread = Thread(runnable)
        thread.priority = Process.THREAD_PRIORITY_DEFAULT + Process.THREAD_PRIORITY_LESS_FAVORABLE
        runnable.mThread = thread
        mRunning[library] = runnable
        mThreads[id] = runnable
        thread.start()
    }

    fun scanLibrariesSilent(libraries: List<Library>?) {
        if (libraries == null || libraries.isEmpty())
            return

        for (library in libraries) {
            val id = UUID.randomUUID()
            val runnable = LibraryUpdateRunnable(id, library, isSilent = true)
            val thread = Thread(runnable)
            thread.priority = Process.THREAD_PRIORITY_DEFAULT + Process.THREAD_PRIORITY_LESS_FAVORABLE
            runnable.mThread = thread
            mThreads[id] = runnable
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

    private fun notifyMediaUpdatedChange(book: Book) {
        val msg = Message()
        msg.obj = book
        msg.what = GeneralConsts.SCANNER.MESSAGE_MANGA_UPDATED_CHANGE
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

    private fun generateCover(book: Book) = BookImageCoverController.instance.getCoverFromFile(context, book.file)

    private inner class LibraryUpdateRunnable(id: UUID, library: Library, val isSilent: Boolean = false) : Runnable {
        val mId = id
        val mLibrary = library
        var mIsStopped = false
        var mIsRestarted = false
        lateinit var mThread: Thread

        override fun run() {
            var isProcess = false
            try {
                val libraryPath = mLibrary.path
                if (libraryPath == "" || !File(libraryPath).exists()) return

                mIsStopped = false
                val storage = Storage(context)
                val storageFiles: MutableMap<String, Book> = HashMap()
                val storageDeletes: MutableMap<String, Book> = HashMap()

                // create list of files available in storage
                for (c in storage.listBook(mLibrary))
                    storageFiles[c.path] = c

                for (c in storage.listBookDeleted(mLibrary))
                    storageDeletes[c.name] = c

                var walked = false
                // search and add comics if necessary
                val file = File(libraryPath)
                file.walk().onFail { _, ioException -> mLOGGER.warn("File walk error", ioException) }
                    .filterNot { it.isDirectory }.forEach {
                        walked = true
                        if (mIsStopped)
                            return
                        if (FileType.isBook(it.name)) {
                            if (storageFiles.containsKey(it.path))
                                storageFiles.remove(it.path)
                            else {
                                mLOGGER.info("Processing book: " + it.name + ".")
                                isProcess = true
                                try {
                                    val ebookMeta = FileMetaCore.get().getEbookMeta(it.path, CacheZipUtils.CacheDir.ZipApp, false)
                                    FileMetaCore.get().udpateFullMeta(FileMeta(it.path), ebookMeta)
                                    var isCover = false

                                    val book = if (storageDeletes.containsKey(it.nameWithoutExtension)) {
                                        storageFiles.remove(it.nameWithoutExtension)
                                        val deleted = storageDeletes.getValue(it.nameWithoutExtension)
                                        deleted.update(ebookMeta, mLibrary.language)
                                        generateCover(deleted)
                                        isCover = true
                                        if (!deleted.path.equals(it.path, true))
                                            deleted.path = it.path
                                        notifyMediaUpdatedChange(deleted)
                                        deleted
                                    } else if (storage.findBookByPath(it.path) != null)
                                        return
                                    else
                                        Book(mLibrary.id,null,  it,  ebookMeta, mLibrary.language)

                                    book.path = it.path
                                    book.folder = it.parent
                                    book.excluded = false

                                    if (!isSilent && isCover)
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
                if (!mIsStopped && !mIsRestarted && walked)
                    for (missing in storageFiles.values) {
                        isProcess = true
                        storage.delete(missing)
                        if (!isSilent)
                            notifyMediaUpdatedRemove(missing)
                    }
            } catch (e: Exception) {
                mLOGGER.error("Error to scanner manga.", e)
            } finally {
                mThreads.remove(mId)
                if (mRunning.containsKey(mLibrary))
                    mRunning.remove(mLibrary)

                mIsStopped = false
                if (mIsRestarted) {
                    mIsRestarted = false
                    val mRestartHandler: Handler = RestartHandler(this@ScannerBook, mLibrary)
                    mRestartHandler.sendEmptyMessageDelayed(1, 200)
                } else if (!isSilent)
                    notifyLibraryUpdateFinished(isProcess)
            }
        }
    }

}
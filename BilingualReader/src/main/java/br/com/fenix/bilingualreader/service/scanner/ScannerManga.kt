package br.com.fenix.bilingualreader.service.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Message
import android.os.Process
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.FileType
import br.com.fenix.bilingualreader.service.controller.MangaImageCoverController
import br.com.fenix.bilingualreader.service.parses.manga.Parse
import br.com.fenix.bilingualreader.service.parses.manga.ParseFactory
import br.com.fenix.bilingualreader.service.parses.manga.RarParse
import br.com.fenix.bilingualreader.service.repository.Storage
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.Notifications
import br.com.fenix.bilingualreader.util.helpers.Util
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.Date
import java.util.UUID

class ScannerManga(private val context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(ScannerManga::class.java)

    private var mUpdateHandler: MutableList<Handler> = ArrayList()
    private var mThreads = mutableMapOf<UUID, LibraryUpdateRunnable>()
    private var mRunning = mutableMapOf<Library, LibraryUpdateRunnable>()

    private inner class RestartHandler(scanner: ScannerManga, var library: Library) :
        Handler() {
        private val mScannerRef: WeakReference<ScannerManga> = WeakReference<ScannerManga>(scanner)
        override fun handleMessage(msg: Message) {
            mScannerRef.get()?.scanLibrary(library)
        }
    }

    // Singleton - One thread initialize only
    companion object {
        private lateinit var INSTANCE: ScannerManga
        fun getInstance(context: Context): ScannerManga {
            if (!::INSTANCE.isInitialized)
                synchronized(ScannerManga::class.java) { // Used for a two or many cores
                    INSTANCE = ScannerManga(context)
                }
            return INSTANCE
        }
    }

    fun isRunning(library: Library): Boolean {
        return mRunning.isNotEmpty() && mRunning.containsKey(library)
    }

    fun forceScanLibrary(library: Library) {
        if (isRunning(library)) {
            mRunning[library]!!.mIsStopped = true
            mRunning[library]!!.mIsRestarted = true
        } else
            scanLibrary(library)
    }

    fun stopScan() {
        if (mThreads.isNotEmpty())
            for (running in mThreads.values)
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
        thread.priority = Process.THREAD_PRIORITY_DEFAULT + Process.THREAD_PRIORITY_BACKGROUND
        runnable.mThread = thread
        mRunning[library] = runnable
        mThreads[id] = runnable
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

    private fun notifyMediaUpdatedAdd(manga: Manga) {
        val msg = Message()
        msg.obj = manga
        msg.what = GeneralConsts.SCANNER.MESSAGE_MANGA_UPDATED_ADD
        notifyHandlers(msg)
    }

    private fun notifyMediaUpdatedRemove(manga: Manga) {
        val msg = Message()
        msg.obj = manga
        msg.what = GeneralConsts.SCANNER.MESSAGE_MANGA_UPDATED_REMOVE
        notifyHandlers(msg)
    }

    private fun notifyMediaUpdatedChange(manga: Manga) {
        val msg = Message()
        msg.obj = manga
        msg.what = GeneralConsts.SCANNER.MESSAGE_MANGA_UPDATED_CHANGE
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

    private fun generateCover(parse: Parse, manga: Manga) = MangaImageCoverController.instance.getCoverFromFile(context, manga.file, parse)

    private inner class LibraryUpdateRunnable(id: UUID, library: Library, val isSilent: Boolean = false) : Runnable {
        val mId = id
        val mLibrary = library
        var mIsStopped = false
        var mIsRestarted = false
        lateinit var mThread: Thread

        override fun run() {
            val libraryPath = mLibrary.path
            if (libraryPath == "" || !File(libraryPath).exists())
                return

            val notificationManager = NotificationManagerCompat.from(context)
            val notification = Notifications.getNotification(context, context.getString(R.string.notifications_scanner_library), context.getString(
                R.string.notifications_scanner_library_content, mLibrary.title, context.getString(R.string.menu_manga)))
            val notifyId = Notifications.getID()

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
                notificationManager.notify(notifyId, notification.build())

            var isProcess = false
            try {


                mIsStopped = false
                val storage = Storage(context)
                val storageFiles: MutableMap<String, Manga> = HashMap()
                val storageDeletes: MutableMap<String, Manga> = HashMap()

                // create list of files available in storage
                for (c in storage.listMangas(mLibrary)!!)
                    storageFiles[c.path] = c

                for (c in storage.listDeleted(mLibrary)!!)
                    storageDeletes[c.title] = c

                var walked = false
                // search and add comics if necessary
                val file = File(libraryPath)
                file.walk().onFail { _, ioException -> mLOGGER.warn("File walk error", ioException) }
                    .filterNot { it.isDirectory }.forEach {
                        walked = true
                        if (mIsStopped)
                            return
                        if (FileType.isManga(it.name)) {
                            if (storageFiles.containsKey(it.path))
                                storageFiles.remove(it.path)
                            else {
                                isProcess = true
                                try {
                                    mLOGGER.info("Precessing manga " + it.name + ".")
                                    val parse: Parse? = ParseFactory.create(it)
                                    try {
                                        if (parse != null)
                                            if (parse.numPages() > 0) {
                                                if (parse is RarParse) {
                                                    val folder = GeneralConsts.CACHE_FOLDER.RAR + '/' + Util.normalizeNameCache(it.nameWithoutExtension)
                                                    val cacheDir = File(GeneralConsts.getCacheDir(context), folder)
                                                    (parse as RarParse?)!!.setCacheDirectory(cacheDir)
                                                }

                                                val manga = if (storageDeletes.containsKey(it.nameWithoutExtension)) {
                                                    storageFiles.remove(it.nameWithoutExtension)
                                                    val deleted = storageDeletes.getValue(it.nameWithoutExtension)
                                                    if (!deleted.path.equals(it.path, true))
                                                        deleted.path = it.path
                                                    notifyMediaUpdatedChange(deleted)
                                                    deleted
                                                } else
                                                    Manga(mLibrary.id, null, it)

                                                manga.path = it.path
                                                manga.folder = it.parent
                                                manga.excluded = false
                                                manga.lastVerify = Date()

                                                val exists = storage.findMangaByPath(it.path)
                                                if (exists != null)
                                                    manga.id = exists.id
                                                
                                                manga.id = storage.save(manga)

                                                manga.update(parse)
                                                storage.save(manga)

                                                if (!isSilent)
                                                    generateCover(parse, manga)

                                                if (!isSilent)
                                                    notifyMediaUpdatedAdd(manga)
                                            }
                                    } finally {
                                        Util.destroyParse(parse)
                                    }
                                } catch (e: Exception) {
                                    mLOGGER.error("Error load manga " + it.name, e)
                                } catch (e: IOException) {
                                    mLOGGER.error("Error load manga " + it.name, e)
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
                    val mRestartHandler: Handler = RestartHandler(this@ScannerManga, mLibrary)
                    mRestartHandler.sendEmptyMessageDelayed(1, 200)
                } else if (!isSilent)
                    notifyLibraryUpdateFinished(isProcess)

                notification.setContentText(context.getString(R.string.notifications_scanner_library_processed, mLibrary.title))
                    .setProgress(0, 0, false)
                    .setOngoing(false)

                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify(notifyId, notification.build())
                    notificationManager.cancel(notifyId)
                }
            }
        }
    }

}
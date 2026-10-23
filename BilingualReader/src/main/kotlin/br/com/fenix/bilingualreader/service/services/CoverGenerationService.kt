package br.com.fenix.bilingualreader.service.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.interfaces.History
import br.com.fenix.bilingualreader.service.controller.BookImageCoverController
import br.com.fenix.bilingualreader.service.controller.MangaImageCoverController
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.Notifications
import br.com.fenix.bilingualreader.util.helpers.Telemetry
import br.com.fenix.bilingualreader.util.helpers.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.File
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class CoverGenerationService private constructor() {

    companion object {
        val instance: CoverGenerationService by lazy { HOLDER.INSTANCE }
    }

    private object HOLDER {
        val INSTANCE = CoverGenerationService()
    }

    private val mLOGGER = LoggerFactory.getLogger(CoverGenerationService::class.java)

    private val mLock = Any()
    private val mQueue = ConcurrentLinkedQueue<History>()
    private val mEnqueuedKeys = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
    private val mIsRunning = AtomicBoolean(false)
    private val mTotalItems = AtomicInteger(0)
    private val mProcessedItems = AtomicInteger(0)
    private var mNotifyId: Int = 0

    private val mScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun isCoverCached(context: Context, item: History): Boolean {
        return try {
            when (item) {
                is Manga -> {
                    val hash = Util.MD5(item.file.path + item.file.name)
                    val cacheDir = File(GeneralConsts.getCoverDir(context), GeneralConsts.CACHE_FOLDER.MANGA_COVERS)
                    val cacheFile = File(cacheDir, hash)
                    cacheFile.exists() && cacheFile.length() > 0
                }
                is Book -> {
                    val hash = Util.MD5(item.file.path + item.file.name)
                    val cacheDir = File(GeneralConsts.getCoverDir(context), GeneralConsts.CACHE_FOLDER.BOOK_COVERS)
                    val cacheFile = File(cacheDir, hash)
                    cacheFile.exists() && cacheFile.length() > 0
                }
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun generateItemKey(item: History): String {
        val fileKey = when (item) {
            is Manga -> item.file.absolutePath
            is Book -> item.file.absolutePath
            else -> item.name
        }
        return "${item.type}_${item.id ?: fileKey}"
    }

    fun enqueue(context: Context, items: List<History>) {
        if (items.isEmpty()) return

        val appContext = context.applicationContext

        synchronized(mLock) {
            var addedCount = 0
            for (item in items) {
                val key = generateItemKey(item)
                if (mEnqueuedKeys.add(key)) {
                    mQueue.offer(item)
                    addedCount++
                }
            }

            if (addedCount == 0 && mIsRunning.get()) {
                return
            }

            mTotalItems.addAndGet(addedCount)

            if (mIsRunning.compareAndSet(false, true)) {
                startWorker(appContext)
            }
        }
    }

    private fun startWorker(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)
        mNotifyId = Notifications.getID()
        val title = context.getString(R.string.notifications_generate_covers_title)
        val notification = Notifications.getNotification(context, title, "")

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(mNotifyId, notification.build())
        }

        mScope.launch {
            try {
                while (true) {
                    val item: History
                    synchronized(mLock) {
                        val next = mQueue.poll()
                        if (next == null) {
                            mIsRunning.set(false)
                            mQueue.clear()
                            mEnqueuedKeys.clear()
                            mTotalItems.set(0)
                            mProcessedItems.set(0)
                            return@launch
                        }
                        item = next
                    }

                    val currentProcessed = mProcessedItems.incrementAndGet()
                    val total = mTotalItems.get()

                    val cached = isCoverCached(context, item)

                    if (!cached) {
                        try {
                            val contentText = context.getString(
                                R.string.notifications_generate_covers_progress,
                                currentProcessed,
                                total,
                                item.title.ifEmpty { item.name }
                            )
                            notification.setContentText(contentText)
                                .setProgress(total, currentProcessed, false)
                                .setOngoing(true)

                            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                notificationManager.notify(mNotifyId, notification.build())
                            }

                            when (item) {
                                is Manga -> {
                                    MangaImageCoverController.instance.getMangaCoverFile(context, item, isCoverSize = true)
                                }
                                is Book -> {
                                    BookImageCoverController.instance.getBookCoverFile(context, item, isCoverSize = true)
                                }
                            }
                        } catch (e: Exception) {
                            mLOGGER.error("Error generating cover for ${item.name}: ${e.message}", e)
                            Telemetry.recordException(e, "Error generating cover: ${e.message}")
                        }
                    } else {
                        if (currentProcessed % 10 == 0 || currentProcessed == total) {
                            val contentText = context.getString(
                                R.string.notifications_generate_covers_progress,
                                currentProcessed,
                                total,
                                item.title.ifEmpty { item.name }
                            )
                            notification.setContentText(contentText)
                                .setProgress(total, currentProcessed, false)
                                .setOngoing(true)

                            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                notificationManager.notify(mNotifyId, notification.build())
                            }
                        }
                    }

                    mEnqueuedKeys.remove(generateItemKey(item))
                }
            } catch (e: Exception) {
                mLOGGER.error("Error in cover generation worker: ${e.message}", e)
                Telemetry.recordException(e, "Error in cover generation worker: ${e.message}")
            } finally {
                try {
                    notificationManager.cancel(mNotifyId)
                } catch (e: Exception) {
                    mLOGGER.error("Error canceling cover notification: ${e.message}", e)
                }
                synchronized(mLock) {
                    mIsRunning.set(false)
                    mQueue.clear()
                    mEnqueuedKeys.clear()
                    mTotalItems.set(0)
                    mProcessedItems.set(0)
                }
            }
        }
    }
}

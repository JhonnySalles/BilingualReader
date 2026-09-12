package br.com.fenix.bilingualreader.service.functions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.text.Html
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.History
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.listener.BookParseListener
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.service.repository.HistoryRepository
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.Notifications
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class ReadingTimeCalculator(private val context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(ReadingTimeCalculator::class.java)
    private val mPreferences = GeneralConsts.getSharedPreferences(context)
    private val mHistoryRepository = HistoryRepository(context)
    private val mMangaRepository = MangaRepository(context)
    private val mBookRepository = BookRepository(context)

    fun getMangaAvgTimePerPage(): Float {
        return mPreferences.getFloat(
            GeneralConsts.KEYS.READER.MANGA_AVG_TIME_PER_PAGE,
            GeneralConsts.KEYS.READER.MANGA_AVG_TIME_PER_PAGE_DEFAULT
        )
    }

    fun getBookAvgWordsPerMinute(): Float {
        return mPreferences.getFloat(
            GeneralConsts.KEYS.READER.BOOK_AVG_WORDS_PER_MINUTE,
            GeneralConsts.KEYS.READER.BOOK_AVG_WORDS_PER_MINUTE_DEFAULT
        )
    }

    fun calculateMangaReadingTime(pagesRead: Int): Long {
        val avgSecondsPerPage = getMangaAvgTimePerPage()
        val pages = if (pagesRead <= 0) 1 else pagesRead
        return (pages * avgSecondsPerPage).toLong()
    }

    fun calculateBookReadingTime(wordsCount: Long): Long {
        val wpm = getBookAvgWordsPerMinute()
        if (wpm <= 0f || wordsCount <= 0L) return 0L
        return ((wordsCount.toDouble() / wpm.toDouble()) * 60.0).toLong()
    }

    fun countWordsFromHtml(html: String?): Long {
        if (html.isNullOrBlank()) return 0L

        val text = try {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString()
        } catch (e: Exception) {
            html.replace(Regex("<[^>]*>"), " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
        }

        return countWordsFromText(text)
    }

    fun countWordsFromText(text: String): Long {
        if (text.isBlank()) return 0L

        var count = 0L
        val nonCjkBuffer = StringBuilder()

        for (ch in text) {
            val codePoint = ch.code
            val isCjk = (codePoint in 0x4E00..0x9FFF) || // CJK Unified Ideographs
                    (codePoint in 0x3400..0x4DBF) || // CJK Extension A
                    (codePoint in 0x3040..0x309F) || // Hiragana
                    (codePoint in 0x30A0..0x30FF) || // Katakana
                    (codePoint in 0xAC00..0xD7AF)    // Hangul Syllables

            if (isCjk) {
                count++
                if (nonCjkBuffer.isNotEmpty()) {
                    val words = nonCjkBuffer.toString().trim().split(Regex("\\s+")).filter { it.isNotBlank() }
                    count += words.size
                    nonCjkBuffer.clear()
                }
            } else {
                nonCjkBuffer.append(ch)
            }
        }

        if (nonCjkBuffer.isNotEmpty()) {
            val words = nonCjkBuffer.toString().trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            count += words.size
        }

        return count
    }

    fun countWordsFromDocument(document: DocumentParse, pageStart: Int, pageEnd: Int): Long {
        var totalWords = 0L
        val totalPages = document.pageCount
        val start = pageStart.coerceIn(0, totalPages)
        val end = pageEnd.coerceIn(start, totalPages)

        if (start == 0 && end >= totalPages) {
            val fullHtml = document.documentToHtml()
            if (fullHtml.isNotBlank()) {
                return countWordsFromHtml(fullHtml)
            }
        }

        for (i in start until end) {
            try {
                val page = document.getPage(i)
                val html = page.pageHTML
                if (!html.isNullOrBlank()) {
                    totalWords += countWordsFromHtml(html)
                }
            } catch (e: Exception) {
                mLOGGER.warn("Error counting words on page $i: " + e.message)
            }
        }

        return totalWords
    }

    fun recalculateBatch(
        type: Type,
        onlyNew: Boolean,
        onProgress: ((Int, Int) -> Unit)? = null,
        onFinished: (() -> Unit)? = null
    ) {
        val notifyId = Notifications.getID()
        val notificationManager = NotificationManagerCompat.from(context)
        val title = context.getString(
            if (type == Type.MANGA) R.string.config_recalculate_reading_time_manga_title
            else R.string.config_recalculate_reading_time_book_title
        )
        val notification = Notifications.getNotification(
            context,
            title,
            context.getString(R.string.config_recalculate_reading_time_start)
        )

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(notifyId, notification.build())
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val histories = if (onlyNew) {
                    mHistoryRepository.listNotAutomatic(type)
                } else {
                    mHistoryRepository.listByType(type)
                }

                val total = histories.size
                var processedCount = 0

                for ((index, history) in histories.withIndex()) {
                    try {
                        val currentProgress = index + 1
                        withContext(Dispatchers.Main) {
                            onProgress?.invoke(currentProgress, total)
                            notification.setProgress(total, currentProgress, false)
                            notification.setContentText(
                                context.getString(R.string.config_recalculate_progress, currentProgress, total)
                            )
                            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                notificationManager.notify(notifyId, notification.build())
                            }
                        }

                        val pagesRead = maxOf(1, history.getPageEnd() - history.pageStart)

                        if (type == Type.MANGA) {
                            val calculatedSeconds = calculateMangaReadingTime(pagesRead)
                            if (calculatedSeconds > history.getSecondsRead() || history.getSecondsRead() <= 0L) {
                                history.setSecondsRead(calculatedSeconds)
                            }
                            history.averageTimeByPage = if (pagesRead > 0) history.getSecondsRead() / pagesRead else calculatedSeconds
                            history.secondsReadAutomatic = true
                            mHistoryRepository.update(history)
                            mMangaRepository.updateLastAlteration(history.fkReference, LocalDateTime.now())
                            processedCount++
                        } else {
                            if (history.wordCount <= 0L) {
                                val book = DataBase.getDataBase(context).getBookDao().get(history.fkReference)
                                if (book != null && book.file.exists()) {
                                    val openJob: CompletableJob = Job()
                                    var loaded = false
                                    val fontSize = mPreferences.getFloat(
                                        GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_SIZE,
                                        GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_SIZE_DEFAULT
                                    ).toInt()

                                    val doc = DocumentParse(
                                        book.path,
                                        book.password,
                                        fontSize,
                                        isLandscape = false,
                                        isVertical = false,
                                        listener = object : BookParseListener {
                                            override fun onLoading(isFinished: Boolean, isLoaded: Boolean) {
                                                if (isFinished) {
                                                    loaded = isLoaded
                                                    openJob.complete()
                                                }
                                            }

                                            override fun onSearching(isSearching: Boolean) {}
                                            override fun onConverting(isConverting: Boolean) {}
                                        }
                                    )

                                    joinAll(openJob)

                                    if (loaded) {
                                        val words = countWordsFromDocument(doc, history.pageStart, history.getPageEnd())
                                        history.wordCount = words
                                        doc.recycle()
                                    }
                                }
                            }

                            val calculatedSeconds = if (history.wordCount > 0L) {
                                calculateBookReadingTime(history.wordCount)
                            } else {
                                val avgWordsPerPage = 250L
                                calculateBookReadingTime(pagesRead * avgWordsPerPage)
                            }

                            if (calculatedSeconds > history.getSecondsRead() || history.getSecondsRead() <= 0L) {
                                history.setSecondsRead(calculatedSeconds)
                            }

                            history.averageTimeByPage = if (pagesRead > 0) history.getSecondsRead() / pagesRead else calculatedSeconds
                            history.secondsReadAutomatic = true
                            mHistoryRepository.update(history)
                            mBookRepository.updateLastAlteration(history.fkReference, LocalDateTime.now())
                            processedCount++
                        }
                    } catch (e: Exception) {
                        mLOGGER.error("Error processing history id ${history.id}: ${e.message}", e)
                    }
                }

                withContext(Dispatchers.Main) {
                    notification.setProgress(0, 0, false)
                        .setContentText(context.getString(R.string.config_recalculate_complete, processedCount))
                        .setOngoing(false)

                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                        notificationManager.notify(notifyId, notification.build())
                    }
                    onFinished?.invoke()
                }
            } catch (e: Exception) {
                mLOGGER.error("Error in recalculateBatch: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    notification.setProgress(0, 0, false)
                        .setContentText(context.getString(R.string.config_recalculate_error))
                        .setOngoing(false)

                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                        notificationManager.notify(notifyId, notification.build())
                    }
                    onFinished?.invoke()
                }
            }
        }
    }

}

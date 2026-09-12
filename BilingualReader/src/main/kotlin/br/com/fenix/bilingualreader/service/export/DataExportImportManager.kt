package br.com.fenix.bilingualreader.service.export

import android.content.Context
import android.net.Uri
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.model.entity.ExportItem
import br.com.fenix.bilingualreader.model.entity.History
import br.com.fenix.bilingualreader.model.entity.MangaAnnotation
import br.com.fenix.bilingualreader.model.enums.Color
import br.com.fenix.bilingualreader.model.enums.MarkType
import br.com.fenix.bilingualreader.service.repository.BookAnnotationRepository
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.HistoryRepository
import br.com.fenix.bilingualreader.service.repository.MangaAnnotationRepository
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.Telemetry
import br.com.fenix.bilingualreader.util.helpers.Util
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.LocalDateTime
import java.util.Date
import kotlin.math.abs

class DataExportImportManager(private val context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(DataExportImportManager::class.java)

    private val repositoryManga = MangaRepository(context)
    private val repositoryBook = BookRepository(context)
    private val repositoryHistory = HistoryRepository(context)
    private val repositoryMangaAnnotation = MangaAnnotationRepository(context)
    private val repositoryBookAnnotation = BookAnnotationRepository(context)

    private val gson: Gson = GsonBuilder()
        .setDateFormat(GeneralConsts.SHARE_MARKS.PARSE_DATE_TIME)
        .excludeFieldsWithoutExposeAnnotation()
        .setPrettyPrinting()
        .create()

    suspend fun exportData(uri: Uri): Int = withContext(Dispatchers.IO) {
        val exportList = mutableListOf<ExportItem>()

        try {
            val allMangas = repositoryManga.findAll().filter { !it.excluded }
            for (manga in allMangas) {
                val libraryId = manga.fkLibrary ?: GeneralConsts.KEYS.LIBRARY.DEFAULT_MANGA
                val mangaId = manga.id ?: continue
                val histories = repositoryHistory.find(manga.type, libraryId, mangaId)
                val annotations = repositoryMangaAnnotation.findByManga(mangaId)

                val hasProgress = manga.bookMark > 0 || manga.completed
                val hasRecords = histories.isNotEmpty() || annotations.isNotEmpty()

                if (hasProgress || hasRecords) {
                    exportList.add(ExportItem(manga, histories, annotations))
                }
            }

            val allBooks = repositoryBook.findAll().filter { !it.excluded }
            for (book in allBooks) {
                val libraryId = book.fkLibrary ?: GeneralConsts.KEYS.LIBRARY.DEFAULT_BOOK
                val bookId = book.id ?: continue
                val histories = repositoryHistory.find(book.type, libraryId, bookId)
                val annotations = repositoryBookAnnotation.findByBook(bookId)

                val hasProgress = book.bookMark > 0 || book.completed
                val hasRecords = histories.isNotEmpty() || annotations.isNotEmpty()

                if (hasProgress || hasRecords) {
                    exportList.add(ExportItem(book, histories, annotations))
                }
            }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    gson.toJson(exportList, writer)
                    writer.flush()
                }
            }

            mLOGGER.info("Exported ${exportList.size} reading items successfully to $uri")
            exportList.size
        } catch (e: Exception) {
            mLOGGER.error("Error exporting data: ${e.message}", e)
            Telemetry.recordException(e, "Error exporting data: ${e.message}")
            throw e
        }
    }

    suspend fun importData(uri: Uri): Int = withContext(Dispatchers.IO) {
        var updatedCount = 0

        try {
            val items: List<ExportItem> = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                InputStreamReader(inputStream, Charsets.UTF_8).use { reader ->
                    val type = object : TypeToken<List<ExportItem>>() {}.type
                    gson.fromJson<List<ExportItem>>(reader, type)
                }
            } ?: emptyList()

            if (items.isEmpty()) {
                mLOGGER.warn("Import file was empty or could not be parsed.")
                return@withContext 0
            }

            for (item in items) {
                val isMangaType = item.type.equals(ExportItem.TYPE_MANGA, ignoreCase = true) ||
                        item.type.equals("manga", ignoreCase = true)
                val isBookType = item.type.equals(ExportItem.TYPE_BOOK, ignoreCase = true) ||
                        item.type.equals("livro", ignoreCase = true) ||
                        item.type.equals("book", ignoreCase = true)

                if (isMangaType) {
                    val updated = importMangaItem(item)
                    if (updated) updatedCount++
                } else if (isBookType) {
                    val updated = importBookItem(item)
                    if (updated) updatedCount++
                } else {
                    // Try to match manga or book if type is unknown
                    val manga = repositoryManga.findByFileName(item.file)
                    if (manga != null) {
                        val updated = importMangaItem(item)
                        if (updated) updatedCount++
                    } else {
                        val book = repositoryBook.findByFileName(item.file)
                        if (book != null) {
                            val updated = importBookItem(item)
                            if (updated) updatedCount++
                        }
                    }
                }
            }

            mLOGGER.info("Imported reading data: $updatedCount items updated.")
            updatedCount
        } catch (e: Exception) {
            mLOGGER.error("Error importing data: ${e.message}", e)
            Telemetry.recordException(e, "Error importing data: ${e.message}")
            throw e
        }
    }

    private fun importMangaItem(item: ExportItem): Boolean {
        val manga = repositoryManga.findByFileName(item.file) ?: return false
        val mangaId = manga.id ?: return false
        val libraryId = manga.fkLibrary ?: GeneralConsts.KEYS.LIBRARY.DEFAULT_MANGA
        var wasUpdated = false

        // Update progress if imported item has newer or valid bookmark
        if (item.bookMark > manga.bookMark || item.completed || (manga.lastAccess == null && item.lastAccess.time > 0)) {
            manga.bookMark = item.bookMark
            manga.completed = item.completed || (manga.pages > 0 && manga.bookMark >= manga.pages)
            if (item.lastAccess.time > 0) {
                manga.lastAccess = GeneralConsts.dateToDateTime(item.lastAccess)
            }
            if (item.favorite) {
                manga.favorite = true
            }
            repositoryManga.update(manga)
            wasUpdated = true
        }

        // Process History avoiding duplicates
        item.history?.let { hMap ->
            if (hMap.isNotEmpty()) {
                val localHistories = repositoryHistory.find(manga.type, libraryId, mangaId)
                val localStartTimes = localHistories.map { GeneralConsts.dateTimeToDate(it.start).time }

                for (shared in hMap.values) {
                    val sharedStartTime = shared.start.time
                    val exists = localStartTimes.any { abs(it - sharedStartTime) < 1000 }
                    if (!exists) {
                        val newHistory = History(
                            null,
                            libraryId,
                            mangaId,
                            manga.type,
                            shared.pageStart,
                            shared.pageEnd,
                            shared.pages,
                            shared.completed,
                            shared.volume,
                            shared.chaptersRead,
                            GeneralConsts.dateToDateTime(shared.start),
                            GeneralConsts.dateToDateTime(shared.end),
                            shared.secondsRead,
                            shared.averageTimeByPage,
                            shared.useTTS,
                            isNotify = false
                        )
                        repositoryHistory.save(newHistory)
                        wasUpdated = true
                    }
                }
            }
        }

        // Process Annotations avoiding duplicates
        item.annotation?.let { aMap ->
            if (aMap.isNotEmpty()) {
                val localAnnotations = repositoryMangaAnnotation.findByManga(mangaId)

                for (shared in aMap.values) {
                    val sharedCreatedTime = shared.created.time
                    val exists = localAnnotations.any { local ->
                        abs(GeneralConsts.dateTimeToDate(local.created).time - sharedCreatedTime) < 1000 ||
                                (local.page == shared.page && local.annotation == shared.annotation && local.chapter == shared.chapter)
                    }

                    if (!exists) {
                        val markType = try {
                            MarkType.valueOf(shared.type)
                        } catch (e: Exception) {
                            MarkType.BookMark
                        }
                        val newAnnotation = MangaAnnotation(
                            null,
                            mangaId,
                            shared.page,
                            shared.pages,
                            markType,
                            shared.chapter,
                            shared.text,
                            shared.annotation,
                            LocalDateTime.now(),
                            GeneralConsts.dateToDateTime(shared.created)
                        )
                        repositoryMangaAnnotation.save(newAnnotation)
                        wasUpdated = true
                    }
                }
            }
        }

        return wasUpdated
    }

    private fun importBookItem(item: ExportItem): Boolean {
        val book = repositoryBook.findByFileName(item.file) ?: return false
        val bookId = book.id ?: return false
        val libraryId = book.fkLibrary ?: GeneralConsts.KEYS.LIBRARY.DEFAULT_BOOK
        var wasUpdated = false

        // Update progress if imported item has valid bookmark
        if (item.bookMark > book.bookMark || item.completed || (book.lastAccess == null && item.lastAccess.time > 0)) {
            book.bookMark = item.bookMark
            book.completed = item.completed || (book.pages > 0 && book.bookMark >= book.pages)
            if (item.lastAccess.time > 0) {
                book.lastAccess = GeneralConsts.dateToDateTime(item.lastAccess)
            }
            if (item.favorite) {
                book.favorite = true
            }
            repositoryBook.update(book)
            wasUpdated = true
        }

        // Process History avoiding duplicates
        item.history?.let { hMap ->
            if (hMap.isNotEmpty()) {
                val localHistories = repositoryHistory.find(book.type, libraryId, bookId)
                val localStartTimes = localHistories.map { GeneralConsts.dateTimeToDate(it.start).time }

                for (shared in hMap.values) {
                    val sharedStartTime = shared.start.time
                    val exists = localStartTimes.any { abs(it - sharedStartTime) < 1000 }
                    if (!exists) {
                        val newHistory = History(
                            null,
                            libraryId,
                            bookId,
                            book.type,
                            shared.pageStart,
                            shared.pageEnd,
                            shared.pages,
                            shared.completed,
                            shared.volume,
                            shared.chaptersRead,
                            GeneralConsts.dateToDateTime(shared.start),
                            GeneralConsts.dateToDateTime(shared.end),
                            shared.secondsRead,
                            shared.averageTimeByPage,
                            shared.useTTS,
                            isNotify = false
                        )
                        repositoryHistory.save(newHistory)
                        wasUpdated = true
                    }
                }
            }
        }

        // Process Annotations avoiding duplicates
        item.annotation?.let { aMap ->
            if (aMap.isNotEmpty()) {
                val localAnnotations = repositoryBookAnnotation.findByBook(bookId)

                for (shared in aMap.values) {
                    val sharedCreatedTime = shared.created.time
                    val exists = localAnnotations.any { local ->
                        abs(GeneralConsts.dateTimeToDate(local.created).time - sharedCreatedTime) < 1000 ||
                                (local.page == shared.page && local.text == shared.text && local.annotation == shared.annotation)
                    }

                    if (!exists) {
                        val markType = try {
                            MarkType.valueOf(shared.type)
                        } catch (e: Exception) {
                            MarkType.BookMark
                        }
                        val color = try {
                            Color.valueOf(shared.color)
                        } catch (e: Exception) {
                            Color.Yellow
                        }
                        val newAnnotation = BookAnnotation(
                            null,
                            bookId,
                            shared.page,
                            shared.pages,
                            shared.fontSize,
                            markType,
                            shared.chapterNumber,
                            shared.chapter,
                            shared.text,
                            Util.stringToIntArray(shared.range),
                            shared.annotation,
                            shared.favorite,
                            color,
                            LocalDateTime.now(),
                            GeneralConsts.dateToDateTime(shared.created),
                            shared.cfiRange
                        )
                        repositoryBookAnnotation.save(newAnnotation)
                        wasUpdated = true
                    }
                }
            }
        }

        return wasUpdated
    }
}

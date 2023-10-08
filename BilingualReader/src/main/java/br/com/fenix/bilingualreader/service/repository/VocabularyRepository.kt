package br.com.fenix.bilingualreader.service.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.entity.SubTitleChapter
import br.com.fenix.bilingualreader.model.entity.Vocabulary
import br.com.fenix.bilingualreader.model.entity.VocabularyBook
import br.com.fenix.bilingualreader.model.entity.VocabularyManga
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.service.japanese.Formatter
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.util.constants.DataBaseConsts
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.FontUtil
import br.com.fenix.bilingualreader.util.helpers.Notifications
import br.com.fenix.bilingualreader.view.ui.vocabulary.VocabularyViewModel
import br.com.fenix.bilingualreader.view.ui.vocabulary.book.VocabularyBookViewModel
import br.com.fenix.bilingualreader.view.ui.vocabulary.manga.VocabularyMangaViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.Collections
import java.util.Date
import java.util.Objects
import java.util.stream.Collectors

class VocabularyRepository(var context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(VocabularyRepository::class.java)
    private val mBase = DataBase.getDataBase(context)
    private var mDataBaseDAO = mBase.getVocabularyDao()

    private fun getOrderDesc(order: Order) : String {
        return when(order) {
            Order.Description -> DataBaseConsts.VOCABULARY.COLUMNS.WORD
            Order.Favorite -> DataBaseConsts.VOCABULARY.COLUMNS.FAVORITE
            Order.Frequency -> DataBaseConsts.VOCABULARY.COLUMNS.APPEARS
            else -> ""
        }
    }

    fun save(obj: Vocabulary): Long {
        val exist = mDataBaseDAO.exists(obj.word, obj.basicForm ?: "")
        return if (exist != null)
            exist.id!!
        else
            mDataBaseDAO.save(obj)
    }

    fun update(obj: Vocabulary) {
        mDataBaseDAO.update(obj)
    }

    fun delete(obj: Vocabulary) {
        mDataBaseDAO.delete(obj)
    }

    fun get(id: Long): Vocabulary? {
        return try {
            mDataBaseDAO.get(id)
        } catch (e: Exception) {
            mLOGGER.error("Error when get Vocabulary: " + e.message, e)
            null
        }
    }

    fun find(vocabulary: String): Vocabulary? {
        return try {
            mDataBaseDAO.find(vocabulary)
        } catch (e: Exception) {
            mLOGGER.error("Error when get Library: " + e.message, e)
            null
        }
    }

    fun findAll(vocabulary: String): List<Vocabulary> {
        return try {
            mDataBaseDAO.findAll(vocabulary)
        } catch (e: Exception) {
            mLOGGER.error("Error when find Library: " + e.message, e)
            listOf()
        }
    }

    fun list(
        query: VocabularyViewModel.Query,
        padding: Int,
        size: Int
    ): List<Vocabulary> {
        val orderInverse = if (query.order.first == Order.Favorite) !query.order.second else query.order.second
        return if (query.vocabulary.isNotEmpty())
            mDataBaseDAO.list(
                query.vocabulary,
                query.vocabulary,
                query.favorite,
                getOrderDesc(query.order.first),
                orderInverse,
                padding,
                size
            )
        else
            mDataBaseDAO.list(query.favorite, getOrderDesc(query.order.first), orderInverse, padding, size)
    }

    // --------------------------------------------------------- Comic / Manga ---------------------------------------------------------
    fun listManga(
        query: VocabularyMangaViewModel.Query,
        padding: Int,
        size: Int
    ): List<Vocabulary> {
        val orderInverse = if (query.order.first == Order.Favorite) !query.order.second else query.order.second
        return if (query.manga.isNotEmpty() && query.vocabulary.isNotEmpty())
            mDataBaseDAO.listByManga(
                query.manga,
                query.vocabulary,
                query.vocabulary,
                query.favorite,
                getOrderDesc(query.order.first),
                orderInverse,
                padding,
                size
            )
        else if (query.manga.isNotEmpty())
            mDataBaseDAO.listByManga(query.manga, query.favorite, getOrderDesc(query.order.first), orderInverse, padding, size)
        else if (query.vocabulary.isNotEmpty())
            mDataBaseDAO.list(
                query.vocabulary,
                query.vocabulary,
                query.favorite,
                getOrderDesc(query.order.first),
                orderInverse,
                padding,
                size
            )
        else
            mDataBaseDAO.list(query.favorite, getOrderDesc(query.order.first), orderInverse, padding, size)
    }

    fun findVocabMangaByVocabulary(mangaName: String, vocabulary: Vocabulary): Vocabulary {
        vocabulary.vocabularyMangas = findVocabMangaByVocabulary(mangaName, vocabulary.id!!)
        return vocabulary
    }

    private val mMangaList = mutableMapOf<Long, Manga?>()
    private fun findVocabMangaByVocabulary(
        mangaName: String,
        idVocabulary: Long
    ): List<VocabularyManga> {
        val list = mDataBaseDAO.findMangaByVocabulary(mangaName, idVocabulary)
        list.forEach {
            if (mMangaList.containsKey(it.idManga))
                it.manga = mMangaList[it.idManga]

            if (it.manga == null) {
                it.manga = mDataBaseDAO.getManga(it.idManga)
                mMangaList[it.idManga] = it.manga
            }
        }
        return list
    }

    fun getManga(idManga: Long) =
        mDataBaseDAO.getManga(idManga)

    // --------------------------------------------------------- Book ---------------------------------------------------------
    fun listBook(query: VocabularyBookViewModel.Query, padding: Int, size: Int): List<Vocabulary> {
        val orderInverse = if (query.order.first == Order.Favorite) !query.order.second else query.order.second
        return if (query.book.isNotEmpty() && query.vocabulary.isNotEmpty())
            mDataBaseDAO.listByBook(
                query.book,
                query.vocabulary,
                query.vocabulary,
                query.favorite,
                getOrderDesc(query.order.first),
                orderInverse,
                padding,
                size
            )
        else if (query.book.isNotEmpty())
            mDataBaseDAO.listByBook(query.book, query.favorite, getOrderDesc(query.order.first), orderInverse, padding, size)
        else if (query.vocabulary.isNotEmpty())
            mDataBaseDAO.list(
                query.vocabulary,
                query.vocabulary,
                query.favorite,
                getOrderDesc(query.order.first),
                orderInverse,
                padding,
                size
            )
        else
            mDataBaseDAO.list(query.favorite, getOrderDesc(query.order.first), orderInverse, padding, size)
    }

    fun findVocabBookByVocabulary(bookName: String, vocabulary: Vocabulary): Vocabulary {
        vocabulary.vocabularyBooks = findVocabBookByVocabulary(bookName, vocabulary.id!!)
        return vocabulary
    }

    private val mBookList = mutableMapOf<Long, Book?>()
    private fun findVocabBookByVocabulary(
        bookName: String,
        idVocabulary: Long
    ): List<VocabularyBook> {
        val list = mDataBaseDAO.findBookByVocabulary(bookName, idVocabulary)
        list.forEach {
            if (mBookList.containsKey(it.idBook))
                it.book = mBookList[it.idBook]

            if (it.book == null) {
                it.book = mDataBaseDAO.getBook(it.idBook)
                mBookList[it.idBook] = it.book
            }
        }
        return list
    }

    fun getBook(idBook: Long) = mDataBaseDAO.getBook(idBook)

    // --------------------------------------------------------- Substring ---------------------------------------------------------
    fun insert(idMangaOrBook: Long, idVocabulary: Long, appears: Int, isManga: Boolean = true) {
        mDataBaseDAO.insert(mBase.openHelper, idMangaOrBook, idVocabulary, appears, isManga)
    }

    fun processVocabulary(chaptersList: Collection<SubTitleChapter>) : List<Pair<Vocabulary, Int>> {
        val list = mutableSetOf<Vocabulary?>()
        val pages = mutableListOf<Vocabulary>()

        //Not use parallel because error when add pages list
        chaptersList.stream()
            .forEach {
                for (vocabulary in it.vocabulary)
                    if (!list.contains(vocabulary))
                        list.add(vocabulary)

                it.subTitlePages.stream().forEach { p -> pages.addAll(p.vocabulary) }
            }

        val newList = mutableListOf<Pair<Vocabulary, Int>>()

        for ((index, vocabulary) in list.withIndex()) {
            if (vocabulary != null) {
                var appears = 0
                pages.parallelStream().forEach { v -> if (v == vocabulary) appears++ }
                newList.add(Pair(vocabulary, appears))
            }
        }

        return newList
    }

    fun processVocabulary(idManga: Long?, subTitleChapters: List<SubTitleChapter>, forced : Boolean = false) {
        if (subTitleChapters.isEmpty() || idManga == null)
            return

        val list = subTitleChapters.toList()
        val manga = mBase.getMangaDao().get(idManga)
        if (!forced && manga.lastVocabImport != null && !Date(manga.file.lastModified()).after(manga.fileAlteration))
            return

        val chaptersList = Collections.synchronizedCollection(list.parallelStream()
                .filter(Objects::nonNull)
                .filter { it.language == Languages.JAPANESE && it.vocabulary.isNotEmpty() }
                .collect(Collectors.toList()))

        val notifyId = Notifications.getID()
        val notificationManager = NotificationManagerCompat.from(context)
        val notification = Notifications.getNotification(context, context.getString(R.string.vocabulary_import_title), context.getString(R.string.vocabulary_import, manga.title))

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
            notificationManager.notify(notifyId, notification.build())

        CoroutineScope(newSingleThreadContext("VocabularyThread")).launch {
            async {
                try {
                    val processed = processVocabulary(chaptersList)
                    for ((index, vocab) in processed.withIndex())
                        withContext(Dispatchers.Main) {
                            vocab.first.id = save(vocab.first)
                            vocab.first.id?.let { insert(manga.id!!, it, vocab.second) }

                            notification.setProgress(processed.size, index, false)
                            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
                                notificationManager.notify(notifyId, notification.build())
                        }

                    manga.lastVocabImport = LocalDateTime.now()
                    manga.fileAlteration = Date(manga.file.lastModified())

                    val mMsgImport = "${context.getString(R.string.vocabulary_imported)}\n${manga.title}"
                    withContext(Dispatchers.Main) {
                        updateMangaImport(manga.id!!, manga.lastVocabImport!!, manga.fileAlteration)

                        notification.setContentText(mMsgImport)
                            .setProgress(list.size, list.size, false)

                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
                            notificationManager.notify(notifyId, notification.build())

                        Toast.makeText(context, mMsgImport, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    mLOGGER.error("Error process manga vocabulary. ", e)
                    withContext(Dispatchers.Main) {
                        val msg = context.getString(R.string.vocabulary_import_error)
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        notification.setContentText(msg)

                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
                            notificationManager.notify(notifyId, notification.build())
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        notification.setOngoing(false)

                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
                            notificationManager.notify(notifyId, notification.build())
                    }
                }
            }
        }
    }

    fun updateMangaImport(id: Long, lastVocabImport: LocalDateTime, fileAlteration : Date) {
        mDataBaseDAO.updateMangaImport(id, lastVocabImport, fileAlteration)
    }

    fun processVocabulary(context: Context, idBook: Long?) {
        val prefs = GeneralConsts.getSharedPreferences(context)
        val isProcess = prefs.getBoolean(GeneralConsts.KEYS.READER.BOOK_PROCESS_VOCABULARY, true)

        if (!isProcess || idBook == null)
            return

        val book = mBase.getBookDao().get(idBook)
        if (book.language != Languages.JAPANESE || (book.lastVocabImport != null && !Date(book.file.lastModified()).after(book.fileAlteration)))
            return

        val notifyId = Notifications.getID()
        val notificationManager = NotificationManagerCompat.from(context)
        val notification = Notifications.getNotification(context, context.getString(R.string.vocabulary_import_title), context.getString(R.string.vocabulary_import, book.title))

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
            notificationManager.notify(notifyId, notification.build())

        CoroutineScope(newSingleThreadContext("VocabularyThread")).launch {
            async {
                try {
                    Formatter.initializeAsync(context)

                    val document = DocumentParse(book.path, book.password, FontUtil.pixelToDips(context, GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_SIZE_DEFAULT), false)
                    val list = mutableListOf<Vocabulary>()

                    for (i in 0 until document.pageCount) {
                        val page = document.getPage(i).pageHTML
                        if (page != null && page.isNotEmpty())
                            list.addAll(Formatter.generateVocabulary(page))
                    }

                    val vocabulary = list.stream().distinct()
                    for (vocab in vocabulary) {
                        var appears = 0
                        list.parallelStream().forEach { v -> if (v == vocab) appears++ }

                        withContext(Dispatchers.Main) {
                            vocab.id?.let { insert(book.id!!, it, appears, isManga = false) }
                        }
                    }

                    book.lastVocabImport = LocalDateTime.now()
                    book.fileAlteration = Date(book.file.lastModified())

                    val mMsgImport = "${context.getString(R.string.vocabulary_imported)}\n${book.title}"

                    withContext(Dispatchers.Main) {
                        mBase.getBookDao().update(book)

                        notification.setContentText(mMsgImport)
                            .setProgress(0, 0, false)
                            .setOngoing(false)

                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
                            notificationManager.notify(notifyId, notification.build())
                    }

                    Toast.makeText(context, mMsgImport, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    mLOGGER.error("Error process book vocabulary. ", e)

                    withContext(Dispatchers.Main) {
                        val msg = context.getString(R.string.vocabulary_import_error)
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        notification.setContentText(msg).setOngoing(false)

                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
                            notificationManager.notify(notifyId, notification.build())
                    }
                }
            }
        }
    }

}
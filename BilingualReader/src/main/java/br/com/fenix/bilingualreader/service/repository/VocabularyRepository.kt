package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import android.widget.Toast
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.*
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.util.constants.DataBaseConsts
import br.com.fenix.bilingualreader.view.ui.vocabulary.VocabularyViewModel
import br.com.fenix.bilingualreader.view.ui.vocabulary.book.VocabularyBookViewModel
import br.com.fenix.bilingualreader.view.ui.vocabulary.manga.VocabularyMangaViewModel
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*
import kotlin.streams.toList

class VocabularyRepository(context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(VocabularyRepository::class.java)
    private val mBase = DataBase.getDataBase(context)
    private var mDataBaseDAO = mBase.getVocabularyDao()
    private val mMsgImport = context.getString(R.string.vocabulary_imported)
    private val mVocabImported = Toast.makeText(context, mMsgImport, Toast.LENGTH_SHORT)
    private var mLastImport: Long? = null

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

    fun findJlpt(vocabulary: String): Int {
        return try {
            mDataBaseDAO.findJlpt(vocabulary) ?: 0
        } catch (e: Exception) {
            mLOGGER.error("Error when get Library: " + e.message, e)
            0
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

    fun processVocabulary(idManga: Long?, subTitleChapters: List<SubTitleChapter>) {
        if (subTitleChapters.isEmpty() || idManga == null || idManga == mLastImport)
            return

        val manga = mBase.getMangaDao().get(idManga)
        if (manga.lastVocabImport != null && manga.file.lastModified() == manga.fileAlteration)
            return

        val chaptersList = Collections
            .synchronizedCollection(subTitleChapters.parallelStream()
                .filter(Objects::nonNull)
                .filter { it.language == Languages.JAPANESE && it.vocabulary.isNotEmpty() }
                .toList())

        CoroutineScope(newSingleThreadContext("VocabularyThread")).launch {
            async {
                try {
                    val list = mutableSetOf<Vocabulary?>()
                    val pages = mutableListOf<Vocabulary>()

                    chaptersList.parallelStream()
                        .forEach {
                            for (vocabulary in it.vocabulary)
                                if (!list.contains(vocabulary))
                                    list.add(vocabulary)

                            it.subTitlePages.parallelStream().forEach { p -> pages.addAll(p.vocabulary) }
                        }

                    for (vocabulary in list) {
                        if (vocabulary != null) {
                            var appears = 0

                            pages.parallelStream().forEach { v -> if (v == vocabulary) appears++ }

                            withContext(Dispatchers.Main) {
                                vocabulary.id = save(vocabulary)
                                vocabulary.id?.let { insert(manga.id!!, it, appears) }
                            }
                        }
                    }

                    mLastImport = manga.id
                    manga.lastVocabImport = LocalDateTime.now()
                    mBase.getMangaDao().save(manga)

                    mVocabImported.setText("$mMsgImport\n${manga.title}")
                    mVocabImported.show()
                } catch (e: Exception) {
                    mLOGGER.error("Error process vocabulary. ", e)
                }
            }
        }
    }

}
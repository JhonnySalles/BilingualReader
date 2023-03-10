package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import android.widget.Toast
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Chapter
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.entity.Vocabulary
import br.com.fenix.bilingualreader.model.entity.VocabularyManga
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.view.ui.vocabulary.VocabularyViewModel
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.streams.toList

class VocabularyRepository(context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(VocabularyRepository::class.java)
    private val mBase = DataBase.getDataBase(context)
    private var mDataBaseDAO = mBase.getVocabularyDao()
    private val mMsgImport = context.getString(R.string.vocabulary_imported)
    private val mVocabImported = Toast.makeText(context, mMsgImport, Toast.LENGTH_SHORT)
    private var mLastImport: Long? = null

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

    fun listManga(query: VocabularyViewModel.Query, padding: Int, size: Int): List<Vocabulary> {
        return if (query.manga.isNotEmpty() && query.vocabulary.isNotEmpty())
            mDataBaseDAO.list(query.manga, query.vocabulary, query.vocabulary, query.favorite, query.orderInverse, padding, size)
        else if (query.manga.isNotEmpty())
            mDataBaseDAO.list(query.manga, query.favorite, query.orderInverse, padding, size)
        else if (query.vocabulary.isNotEmpty())
            mDataBaseDAO.list(query.vocabulary, query.vocabulary, query.favorite, query.orderInverse, padding, size)
        else
            mDataBaseDAO.list(query.favorite, query.orderInverse, padding, size)
    }

    fun findVocabMangaByVocabulary(mangaName: String, vocabulary: Vocabulary): Vocabulary {
        vocabulary.vocabularyMangas = findVocabMangaByVocabulary(mangaName, vocabulary.id!!)
        return vocabulary
    }

    private val mMangaList = mutableMapOf<Long, Manga?>()
    private fun findVocabMangaByVocabulary(mangaName: String, idVocabulary: Long): List<VocabularyManga> {
        val list = mDataBaseDAO.findByVocabulary(mangaName, idVocabulary)
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

    fun findByManga(idManga: Long): List<Vocabulary> {
        return try {
            mDataBaseDAO.find(idManga)
        } catch (e: Exception) {
            mLOGGER.error("Error when find Library: " + e.message, e)
            listOf()
        }
    }

    fun getManga(idManga: Long) =
        mDataBaseDAO.getManga(idManga)


    fun insert(idMangaOrBook: Long, idVocabulary: Long, appears: Int, isManga: Boolean = true) {
        mDataBaseDAO.insert(mBase.openHelper, idMangaOrBook, idVocabulary, appears, isManga)
    }

    fun processVocabulary(idManga: Long?, chapters: List<Chapter>) {
        if (idManga == null || chapters.isEmpty() || idManga == mLastImport)
            return

        val chaptersList = Collections
            .synchronizedCollection(chapters.parallelStream()
                .filter(Objects::nonNull)
                .filter { it.language == Languages.JAPANESE && it.vocabulary.isNotEmpty() }
                .toList())

        CoroutineScope(Dispatchers.IO).launch {
            async {
                try {
                    val list = mutableSetOf<Vocabulary>()
                    val pages = mutableListOf<Vocabulary>()

                    chaptersList.parallelStream()
                        .forEach {
                            for (vocabulary in it.vocabulary)
                                if (!list.contains(vocabulary))
                                    list.add(vocabulary)

                            it.pages.parallelStream().forEach { p -> pages.addAll(p.vocabulary) }
                        }

                    for (vocabulary in list) {
                        if (vocabulary != null) {
                            var appears = 0

                            pages.parallelStream().forEach { v -> if (v == vocabulary) appears++ }

                            withContext(Dispatchers.Main) {
                                vocabulary.id = save(vocabulary)
                                vocabulary.id?.let { insert(idManga, it, appears) }
                            }
                        }
                    }

                    mLastImport = idManga
                    mVocabImported.setText("$mMsgImport\n${mDataBaseDAO.getManga(idManga).title}")
                    mVocabImported.show()
                } catch (e: Exception) {
                    mLOGGER.error("Error process vocabulary. ", e)
                }
            }
        }
    }

}
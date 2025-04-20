package br.com.fenix.bilingualreader.view.ui.history

import android.app.Application
import android.widget.Filter
import android.widget.Filterable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.FileType
import br.com.fenix.bilingualreader.model.enums.Filter as FilterType
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.model.interfaces.History
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.LibraryRepository
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import br.com.fenix.bilingualreader.service.repository.TagsRepository
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.Objects
import java.util.stream.Collectors

class HistoryViewModel(var app: Application) : AndroidViewModel(app), Filterable {

    private val mLOGGER = LoggerFactory.getLogger(HistoryViewModel::class.java)

    private val mLibraryRepository: LibraryRepository = LibraryRepository(app.applicationContext)

    private val mMangaRepository: MangaRepository = MangaRepository(app.applicationContext)
    private val mBookRepository: BookRepository = BookRepository(app.applicationContext)
    private val mTagsRepository: TagsRepository = TagsRepository(app.applicationContext)

    private val mDefaultKey = -3L
    val mDefaultLibrary = Library(mDefaultKey, app.applicationContext.getString(R.string.history_library_default), "", excluded = true)

    private var mLibrary: Library? = null
    private var mType: Type? = null
    private var mWordFilter: String = ""

    private var mListFull = MutableLiveData<ArrayList<History>>(arrayListOf())
    private var mList = MutableLiveData<ArrayList<History>>(arrayListOf())
    val history: LiveData<ArrayList<History>> = mList

    private var mSuggestionAuthor = setOf<String>()
    private var mSuggestionPublisher = setOf<String>()
    private var mSuggestionSeries = setOf<String>()
    private var mSuggestionVolume = setOf<String>()
    private var mSuggestionTags = mTagsRepository.list()

    fun list() {
        var list = mutableListOf<History>()

        val mangas = mMangaRepository.listHistory()
        if (mangas != null)
            list.addAll(mangas)

        val books = mBookRepository.listHistory()
        if (books != null)
            list.addAll(books)

        val format = DateTimeFormatter.ofPattern(GeneralConsts.PATTERNS.DATE_TIME_PATTERN)
        list = list.sortedByDescending { it.lastAccess }.distinctBy { it.lastAccess!!.format(format) }.toMutableList()

        mListFull.value = ArrayList(list)
        mList.value = ArrayList(list)
        setSuggestions(mListFull.value)
    }

    fun list(refreshComplete: (Int) -> (Unit)) {
        var list = mutableListOf<History>()

        val mangas = mMangaRepository.listHistory()
        if (mangas != null)
            list.addAll(mangas)

        val books = mBookRepository.listHistory()
        if (books != null)
            list.addAll(books)

        val format = DateTimeFormatter.ofPattern(GeneralConsts.PATTERNS.DATE_TIME_PATTERN)
        list = list.sortedByDescending { it.lastAccess }.distinctBy { it.lastAccess!!.format(format) }.toMutableList()

        if (mList.value == null || mList.value!!.isEmpty()) {
            mList.value = ArrayList(list)
            mListFull.value = ArrayList(list)
        } else
            update(list)

        setSuggestions(mListFull.value)
        refreshComplete(mList.value!!.size - 1)
    }

    fun update(list: List<History>) {
        if (list.isNotEmpty()) {
            for (history in list) {
                if (!mList.value!!.contains(history))
                    mList.value!!.add(history)

                if (!mListFull.value!!.contains(history))
                    mListFull.value!!.add(history)
            }
        }
    }

    fun updateDelete(history: History) {
        when (history) {
            is Manga ->  mMangaRepository.delete(history)
            is Book ->  mBookRepository.delete(history)
        }
    }

    fun updateLastAccess(history: History) {
        when (history) {
            is Manga ->  mMangaRepository.update(history)
            is Book ->  mBookRepository.update(history)
        }
    }

    fun clear(history: History?) {
        if (history != null) {
            save(history)
            if (mList.value!!.contains(history))
                mList.value!!.remove(history)

            if (mListFull.value!!.contains(history))
                mListFull.value!!.remove(history)
        }
    }

    fun deletePermanent(history: History?) {
        history ?: return

        when (history) {
            is Manga -> mMangaRepository.deletePermanent(history)
            is Book -> mBookRepository.deletePermanent(history)
        }
    }

    fun save(history: History?) {
        history ?: return

        when (history) {
            is Manga -> {
                if (history.id == 0L)
                    history.id = mMangaRepository.save(history)
                else
                    mMangaRepository.update(history)
            }
            is Book -> {
                if (history.id == 0L)
                    history.id = mBookRepository.save(history)
                else
                    mBookRepository.update(history)
            }
        }

    }

    fun remove(history: History) {
        if (mList.value != null && mList.value!!.contains(history))
            mList.value!!.remove(history)

        if (mListFull.value != null && mListFull.value!!.contains(history))
            mListFull.value!!.remove(history)
    }

    fun add(history: History, index: Int) {
        if (mList.value != null)
            mList.value!!.add(index, history)

        if (mListFull.value != null)
            mListFull.value!!.add(index, history)
    }

    fun getAndRemove(position: Int): History? {
        val manga = if (mList.value != null) mList.value!!.removeAt(position) else null

        if (mList.value != null && mList.value!!.contains(manga))
            mList.value!!.remove(manga)

        return manga
    }

    private fun filterList(): ArrayList<History> {
        val list = arrayListOf<History>()

        val isTitleId = null
        var title: History? = null
        if (mListFull.value != null && mListFull.value!!.isNotEmpty())
            for (history in mListFull.value!!) {
                if (history == null)
                    continue

                if (history.id == isTitleId) {
                    title = history
                    continue
                }

                if (mType != null && history.type != mType)
                    continue

                if (mLibrary != null) {
                    val key = if (mLibrary!!.id == mDefaultKey) {
                        when (history) {
                            is Manga -> GeneralConsts.KEYS.LIBRARY.DEFAULT_MANGA
                            is Book -> GeneralConsts.KEYS.LIBRARY.DEFAULT_BOOK
                            else -> mLibrary!!.id
                        }
                    } else
                        mLibrary!!.id

                    if (history.fkLibrary != key)
                        continue
                }

                if (mWordFilter.isNotEmpty()) {
                    if (history.name.lowercase(Locale.getDefault()).contains(mWordFilter) || history.fileType.compareExtension(mWordFilter)) {
                        if (title != null) {
                            list.add(title)
                            title = null
                        }
                        list.add(history)
                    }
                } else {
                    if (title != null) {
                        list.add(title)
                        title = null
                    }
                    list.add(history)
                }
            }

        return list
    }

    fun filterLibrary(library: Library?) {
        if (library == mLibrary)
            return

        mLibrary = library
        mList.value = filterList()
    }
    fun filterType(type: Type?) {
        if (type == mType)
            return

        mType = type
        mList.value = filterList()
    }

    override fun getFilter(): Filter {
        return mHistoryFilter
    }

    private val mHistoryFilter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            mWordFilter = constraint.toString().lowercase(Locale.getDefault()).trim()
            val results = FilterResults()
            results.values = filterList()
            return results
        }

        override fun publishResults(constraint: CharSequence?, filterResults: FilterResults?) {
            val list = arrayListOf<History>()
            filterResults?.let {
                list.addAll(it.values as Collection<History>)
            }
            mList.value = list
        }
    }

    fun getLibraryList(): List<Library> {
        val list = mutableListOf<Library>()
        list.addAll(mLibraryRepository.list(Type.MANGA))
        list.addAll(mLibraryRepository.list(Type.BOOK))
        return list
    }

    fun clearFilter() {
        mWordFilter = ""
        val newList: MutableList<History> = mutableListOf()
        newList.addAll(mListFull.value!!.filter(Objects::nonNull))
        mList.value = ArrayList(newList)
    }

    private fun setSuggestions(list : List<History>?) {
        mSuggestionAuthor = setOf()
        mSuggestionPublisher = setOf()
        mSuggestionSeries = setOf()
        mSuggestionVolume = setOf()

        if (list.isNullOrEmpty())
            return

        val process = list.parallelStream().collect(Collectors.toList())

        CoroutineScope(newSingleThreadContext("SuggestionThread")).launch {
            async {
                try {
                    val authors = mutableSetOf<String>()
                    val publishers = mutableSetOf<String>()
                    val series = mutableSetOf<String>()
                    val volumes = mutableSetOf<String>()
                    val tags = mutableSetOf<String>()

                    process.forEach {
                        if (it.type == Type.MANGA) {
                            authors.add((it as Manga).author)
                            publishers.add(it.publisher)
                            series.add(it.series)
                        }
                        volumes.add(it.volume)
                    }

                    authors.removeIf { it.isEmpty() }
                    publishers.removeIf { it.isEmpty() }
                    series.removeIf { it.isEmpty() }
                    volumes.removeIf { it.isEmpty() }

                    withContext(Dispatchers.Main) {
                        mSuggestionAuthor = authors
                        mSuggestionPublisher = publishers
                        mSuggestionSeries = series
                        mSuggestionVolume = volumes
                    }
                } catch (e: Exception) {
                    mLOGGER.error("Error generate suggestion: " + e.message, e)
                }
            }
        }
    }

    fun getSuggestions(filter : String): List<String> {
        val type = filter.substringBeforeLast(':')
        val condition = filter.substringAfterLast(':')
        return when(Util.historyStringToFilter(app, type, true)) {
            FilterType.Author -> mSuggestionAuthor.parallelStream().filter { condition.isEmpty() || it.contains(condition, true) }.collect(Collectors.toList())
            FilterType.Publisher -> mSuggestionPublisher.parallelStream().filter { condition.isEmpty() || it.contains(condition, true) }.collect(Collectors.toList())
            FilterType.Series -> mSuggestionSeries.parallelStream().filter { condition.isEmpty() || it.contains(condition, true) }.collect(Collectors.toList())
            FilterType.Volume ->  mSuggestionVolume.parallelStream().filter { condition.isEmpty() || it.contains(condition, true) }.collect(Collectors.toList())
            FilterType.Type -> FileType.getManga().parallelStream().map { "$it" }.collect(Collectors.toList())
            FilterType.Tag -> mSuggestionTags.parallelStream().map { "$it" }.collect(Collectors.toList())
            else -> listOf()
        }
    }

}
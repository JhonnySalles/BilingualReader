package br.com.fenix.bilingualreader.view.ui.history

import android.app.Application
import android.widget.Filter
import android.widget.Filterable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.FileType
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.model.interfaces.History
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.LibraryRepository
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import br.com.fenix.bilingualreader.service.repository.TagsRepository
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.Telemetry
import br.com.fenix.bilingualreader.util.helpers.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.Objects
import java.util.stream.Collectors
import br.com.fenix.bilingualreader.model.enums.Filter as FilterType

class HistoryViewModel(var app: Application) : AndroidViewModel(app), Filterable {

    private val mLOGGER = LoggerFactory.getLogger(HistoryViewModel::class.java)

    private val mLibraryRepository: LibraryRepository = LibraryRepository(app.applicationContext)

    private val mMangaRepository: MangaRepository = MangaRepository(app.applicationContext)
    private val mBookRepository: BookRepository = BookRepository(app.applicationContext)
    private val mTagsRepository: TagsRepository = TagsRepository(app.applicationContext)

    private val mDefaultKey = -3L
    val mDefaultLibrary = Library(mDefaultKey, app.applicationContext.getString(R.string.history_library_default), "", excluded = true)

    private var mLibrary: Library? = null
    private var mWordFilter: String = ""

    private var mLoading = MutableLiveData<Boolean>(false)
    val loading: LiveData<Boolean> = mLoading

    private var mType = MutableLiveData<Type?>(null)
    val type: LiveData<Type?> = mType

    private var mListFull = MutableLiveData<ArrayList<History>>(arrayListOf())
    private var mList = MutableLiveData<ArrayList<History>>(arrayListOf())
    val history: LiveData<ArrayList<History>> = mList

    private var mSuggestionAuthor = setOf<String>()
    private var mSuggestionPublisher = setOf<String>()
    private var mSuggestionSeries = setOf<String>()
    private var mSuggestionVolume = setOf<String>()
    private var mSuggestionTags = listOf<br.com.fenix.bilingualreader.model.entity.Tags>()

    private val mLibraries = MutableLiveData<List<Library>>(emptyList())
    val libraries: LiveData<List<Library>> = mLibraries

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val tags = mTagsRepository.list()
            val libs = mutableListOf<Library>()
            libs.addAll(mLibraryRepository.list(Type.MANGA))
            libs.addAll(mLibraryRepository.list(Type.BOOK))
            withContext(Dispatchers.Main) {
                mSuggestionTags = tags
                mLibraries.value = libs
            }
        }
    }

    fun list() {
        mLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            var list = mutableListOf<History>()

            val mangas = mMangaRepository.listHistory()
            if (mangas != null)
                list.addAll(mangas)

            val books = mBookRepository.listHistory()
            if (books != null)
                list.addAll(books)

            val format = DateTimeFormatter.ofPattern(GeneralConsts.PATTERNS.DATE_TIME_PATTERN)
            list = list.sortedByDescending { it.lastAccess }.distinctBy { it.lastAccess!!.format(format) }.toMutableList()

            withContext(Dispatchers.Main) {
                mLoading.value = false

                mListFull.value = ArrayList(list)
                mList.value = ArrayList(list)
                setSuggestions(mListFull.value)
            }
        }
    }

    fun list(refreshComplete: (Int) -> (Unit)) {
        mLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            var list = mutableListOf<History>()

            val mangas = mMangaRepository.listHistory()
            if (mangas != null)
                list.addAll(mangas)

            val books = mBookRepository.listHistory()
            if (books != null)
                list.addAll(books)

            val format = DateTimeFormatter.ofPattern(GeneralConsts.PATTERNS.DATE_TIME_PATTERN)
            list = list.sortedByDescending { it.lastAccess }.distinctBy { it.lastAccess!!.format(format) }.toMutableList()

            withContext(Dispatchers.Main) {
                mLoading.value = false

                if (mList.value == null || mList.value!!.isEmpty()) {
                    mList.value = ArrayList(list)
                    mListFull.value = ArrayList(list)
                } else
                    update(list)

                setSuggestions(mListFull.value)

                refreshComplete(mList.value!!.size - 1)
            }
        }
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
        viewModelScope.launch(Dispatchers.IO) {
            when (history) {
                is Manga ->  mMangaRepository.delete(history)
                is Book ->  mBookRepository.delete(history)
            }
        }
    }

    fun updateLastAccess(history: History) {
        viewModelScope.launch(Dispatchers.IO) {
            when (history) {
                is Manga ->  mMangaRepository.update(history)
                is Book ->  mBookRepository.update(history)
            }
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
        viewModelScope.launch(Dispatchers.IO) {
            when (history) {
                is Manga -> mMangaRepository.deletePermanent(history)
                is Book -> mBookRepository.deletePermanent(history)
            }
        }
    }

    fun save(history: History?) {
        history ?: return
        viewModelScope.launch(Dispatchers.IO) {
            when (history) {
                is Manga -> {
                    if (history.id == null || history.id == 0L)
                        history.id = mMangaRepository.save(history)
                    else
                        mMangaRepository.update(history)
                }
                is Book -> {
                    if (history.id == null || history.id == 0L)
                        history.id = mBookRepository.save(history)
                    else
                        mBookRepository.update(history)
                }
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

        if (mListFull.value != null && mListFull.value!!.isNotEmpty()) {
            val filter = mWordFilter
            val tags = mutableMapOf<FilterType, String>()
            var searchText = filter

            if (filter.contains("@")) {
                val matches = Regex("@(\\w+):(?:\"([^\"]*)\"|(\\S+))").findAll(filter)
                matches.forEach { match ->
                    val keyStr = match.groups[1]?.value ?: ""
                    val value = match.groups[2]?.value ?: match.groups[3]?.value ?: ""
                    val key = Util.historyStringToFilter(app, keyStr, true)
                    if (key != FilterType.None) {
                        tags[key] = value.lowercase(Locale.getDefault())
                        searchText = searchText.replace(match.value, "")
                    }
                }
                searchText = searchText.trim()
            }

            for (history in mListFull.value!!) {
                if (history == null)
                    continue

                if (history.id == isTitleId) {
                    title = history
                    continue
                }

                if (mType.value != null && history.type != mType.value)
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

                var matches = true
                if (searchText.isNotEmpty()) {
                    matches = history.name.lowercase(Locale.getDefault()).contains(searchText) ||
                            history.fileType.compareExtension(searchText)
                }

                if (matches && tags.isNotEmpty()) {
                    for ((key, value) in tags) {
                        val historyValue = when (key) {
                            FilterType.Author -> if (history is Manga) history.author else if (history is Book) history.author else ""
                            FilterType.Publisher -> if (history is Manga) history.publisher else if (history is Book) history.publisher else ""
                            FilterType.Series -> if (history is Manga) history.series else if (history is Book) history.series else ""
                            FilterType.Volume -> history.volume
                            FilterType.Type -> history.fileType.toString()
                            else -> ""
                        }
                        if (!historyValue.lowercase(Locale.getDefault()).contains(value)) {
                            matches = false
                            break
                        }
                    }
                }

                if (matches) {
                    if (title != null) {
                        list.add(title)
                        title = null
                    }
                    list.add(history)
                }
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
        if (type == mType.value)
            return

        mType.value = type
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

        val process = ArrayList(list)

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val authors = mutableSetOf<String>()
                val publishers = mutableSetOf<String>()
                val series = mutableSetOf<String>()
                val volumes = mutableSetOf<String>()

                process.forEach {
                    when (it.type) {
                        Type.BOOK -> {
                            if ((it as Book).author.contains(","))
                                authors.addAll(it.author.split(",").map { it.trim() })
                            else
                                authors.add(it.author)

                            publishers.add(it.publisher)
                            series.add(it.series)
                        }
                        Type.MANGA -> {
                            if ((it as Manga).author.endsWith("."))
                                authors.add(it.author.substringBeforeLast("."))
                            else
                                authors.add(it.author)

                            publishers.add(it.publisher)
                            series.add(it.series)
                        }
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
                Telemetry.recordException(e, "Error generate suggestion: " + e.message)
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
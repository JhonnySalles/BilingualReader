package br.com.fenix.bilingualreader.view.ui.statistics

import android.app.Application
import android.widget.Filter
import android.widget.Filterable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.HistoryStatistics
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.FileType
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.model.interfaces.History
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.HistoryRepository
import br.com.fenix.bilingualreader.service.repository.LibraryRepository
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import br.com.fenix.bilingualreader.service.repository.StatisticsRepository
import br.com.fenix.bilingualreader.service.repository.TagsRepository
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.Telemetry
import br.com.fenix.bilingualreader.util.helpers.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.Objects
import java.util.stream.Collectors
import br.com.fenix.bilingualreader.model.enums.Filter as FilterType

class HistoryStatisticsViewModel(var app: Application) : AndroidViewModel(app), Filterable {

    private val mLOGGER = LoggerFactory.getLogger(HistoryStatisticsViewModel::class.java)

    private val mLibraryRepository: LibraryRepository = LibraryRepository(app.applicationContext)
    private val mMangaRepository: MangaRepository = MangaRepository(app.applicationContext)
    private val mBookRepository: BookRepository = BookRepository(app.applicationContext)
    private val mTagsRepository: TagsRepository = TagsRepository(app.applicationContext)
    private val mHistoryRepository: HistoryRepository = HistoryRepository(app.applicationContext)
    private val mStatisticsRepository: StatisticsRepository = StatisticsRepository(app.applicationContext)

    private val mDefaultKey = -3L
    val mDefaultLibrary = Library(mDefaultKey, app.applicationContext.getString(R.string.history_library_default), "", excluded = true)

    private val mLibrary = MutableLiveData<Library?>(null)
    val selectedLibrary: LiveData<Library?> = mLibrary
    private var mWordFilter: String = ""

    var mTypeFilter: Type = Type.MANGA
    private val mYearsFilter = MutableLiveData<Set<Int>>(emptySet())
    val selectedYears: LiveData<Set<Int>> = mYearsFilter

    private var mLoading = MutableLiveData<Boolean>(false)
    val loading: LiveData<Boolean> = mLoading

    private var mListFull = MutableLiveData<ArrayList<History>>(arrayListOf())
    private var mList = MutableLiveData<ArrayList<History>>(arrayListOf())
    val history: LiveData<ArrayList<History>> = mList

    private val mOrder = MutableLiveData<Pair<Order, Boolean>>(Pair(Order.LastAccess, true))
    val order: LiveData<Pair<Order, Boolean>> = mOrder

    fun sorted(order: Order, isDesc: Boolean = false) {
        mOrder.value = Pair(order, isDesc)
        list()
    }

    private var mSuggestionAuthor = setOf<String>()
    private var mSuggestionPublisher = setOf<String>()
    private var mSuggestionSeries = setOf<String>()
    private var mSuggestionVolume = setOf<String>()
    private var mSuggestionTags = listOf<br.com.fenix.bilingualreader.model.entity.Tags>()

    private val mLibraries = MutableLiveData<List<Library>>(emptyList())
    val libraries: LiveData<List<Library>> = mLibraries

    private val mYears = MutableLiveData<List<Int>>(emptyList())
    val years: LiveData<List<Int>> = mYears

    fun initData() {
        viewModelScope.launch(Dispatchers.IO) {
            val tags = mTagsRepository.list()
            val libs = mLibraryRepository.list(mTypeFilter)
            val activeYears = mStatisticsRepository.listYears(mTypeFilter)
            withContext(Dispatchers.Main) {
                mSuggestionTags = tags
                mLibraries.value = libs
                mYears.value = activeYears
            }
        }
    }

    fun list() {
        mLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            val list = loadAggregatedHistory()

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
            val list = loadAggregatedHistory()

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

    private fun loadAggregatedHistory(): List<History> {
        val historyLogs = mHistoryRepository.listHistory().filter { it.type == mTypeFilter }
        val selected = mYearsFilter.value ?: emptySet()
        val filteredLogs = if (selected.isNotEmpty()) {
            historyLogs.filter { selected.contains(it.start.year) }
        } else {
            historyLogs
        }

        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val groupedLogs = filteredLogs.groupBy {
            Pair(it.start.format(dateFormatter), it.fkReference)
        }

        val cachedItems = mutableMapOf<Long, History?>()
        val statsItems = mutableListOf<HistoryStatistics>()

        for ((key, logsForGroup) in groupedLogs) {
            val refId = key.second
            val baseItem = cachedItems.getOrPut(refId) {
                if (mTypeFilter == Type.MANGA) {
                    mMangaRepository.get(refId)
                } else {
                    mBookRepository.get(refId)
                }
            }

            if (baseItem != null) {
                val totalSeconds = logsForGroup.sumOf { it.getSecondsRead() }
                val totalPagesRead = logsForGroup.sumOf {
                    if (it.getPageEnd() > it.pageStart) it.getPageEnd() - it.pageStart else 0
                }

                val latestSession = logsForGroup.maxByOrNull { it.start }!!

                statsItems.add(
                    HistoryStatistics(
                        base = baseItem,
                        timeRead = totalSeconds,
                        pagesRead = totalPagesRead,
                        sort = latestSession.start,
                        lastAccess = latestSession.start
                    )
                )
            }
        }

        val currentOrder = mOrder.value ?: Pair(Order.LastAccess, true)
        val isDesc = currentOrder.second
        val order = currentOrder.first

        val sortedList = if (isDesc) {
            when (order) {
                Order.Name -> statsItems.sortedByDescending { it.name }
                Order.Favorite -> statsItems.sortedWith(compareBy<HistoryStatistics> { it.favorite }.thenByDescending { it.name })
                Order.LastAccess -> statsItems.sortedWith(compareBy<HistoryStatistics> { it.lastAccess }.thenByDescending { it.name })
                else -> statsItems.sortedByDescending { it.lastAccess }
            }
        } else {
            when (order) {
                Order.Name -> statsItems.sortedBy { it.name }
                Order.Favorite -> statsItems.sortedWith(compareByDescending<HistoryStatistics> { it.favorite }.thenBy { it.name })
                Order.LastAccess -> statsItems.sortedWith(compareByDescending<HistoryStatistics> { it.lastAccess }.thenBy { it.name })
                else -> statsItems.sortedBy { it.lastAccess }
            }
        }

        val listWithHeaders = mutableListOf<History>()
        var lastDateStr = ""
        val headerDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        for (item in sortedList) {
            val dateStr = item.lastAccess?.format(headerDateFormatter) ?: ""
            if (dateStr != lastDateStr) {
                lastDateStr = dateStr
                val header = if (mTypeFilter == Type.MANGA) {
                    Manga(null, null, File("")).apply { lastAccess = item.lastAccess }
                } else {
                    Book(null, null, File("")).apply { lastAccess = item.lastAccess }
                }
                listWithHeaders.add(header)
            }
            listWithHeaders.add(item)
        }

        return listWithHeaders
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
        val base = if (history is HistoryStatistics) history.base else history
        viewModelScope.launch(Dispatchers.IO) {
            when (base) {
                is Manga ->  mMangaRepository.delete(base)
                is Book ->  mBookRepository.delete(base)
            }
        }
    }

    fun updateLastAccess(history: History) {
        val base = if (history is HistoryStatistics) history.base else history
        viewModelScope.launch(Dispatchers.IO) {
            when (base) {
                is Manga ->  mMangaRepository.update(base)
                is Book ->  mBookRepository.update(base)
            }
        }
    }

    fun clear(history: History?) {
        if (history != null) {
            val base = if (history is HistoryStatistics) history.base else history
            save(base)
            if (mList.value!!.contains(history))
                mList.value!!.remove(history)

            if (mListFull.value!!.contains(history))
                mListFull.value!!.remove(history)
        }
    }

    fun deletePermanent(history: History?) {
        history ?: return
        val base = if (history is HistoryStatistics) history.base else history
        viewModelScope.launch(Dispatchers.IO) {
            when (base) {
                is Manga -> mMangaRepository.deletePermanent(base)
                is Book -> mBookRepository.deletePermanent(base)
            }
        }
    }

    fun save(history: History?) {
        history ?: return
        val base = if (history is HistoryStatistics) history.base else history
        viewModelScope.launch(Dispatchers.IO) {
            when (base) {
                is Manga -> {
                    if (base.id == null || base.id == 0L)
                        base.id = mMangaRepository.save(base)
                    else
                        mMangaRepository.update(base)
                }
                is Book -> {
                    if (base.id == null || base.id == 0L)
                        base.id = mBookRepository.save(base)
                    else
                        mBookRepository.update(base)
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

                val currentLib = mLibrary.value
                if (currentLib != null) {
                    val key = if (currentLib.id == mDefaultKey) {
                        when (history) {
                            is Manga -> GeneralConsts.KEYS.LIBRARY.DEFAULT_MANGA
                            is Book -> GeneralConsts.KEYS.LIBRARY.DEFAULT_BOOK
                            else -> currentLib.id
                        }
                    } else
                        currentLib.id

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
        if (library == mLibrary.value)
            return

        mLibrary.value = library
        mList.value = filterList()
    }

    fun setYear(year: Int?) {
        mYearsFilter.value = if (year != null) setOf(year) else emptySet()
    }

    fun filterYear(year: Int?) {
        val current = mYearsFilter.value ?: emptySet()
        if (year == null) {
            if (current.isEmpty()) return
            mYearsFilter.value = emptySet()
        } else {
            val next = current.toMutableSet()
            if (next.contains(year)) {
                next.remove(year)
            } else {
                next.add(year)
            }
            mYearsFilter.value = next
        }
        list()
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
            val values = filterResults?.values
            val items = when (values) {
                is Collection<*> -> values.filterIsInstance<History>()
                else -> emptyList()
            }
            list.addAll(items)
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

                process.forEach { historyItem ->
                    val it = if (historyItem is HistoryStatistics) historyItem.base else historyItem
                    when (it.type) {
                        Type.BOOK -> {
                            val book = it as Book
                            if (book.author.contains(","))
                                authors.addAll(book.author.split(",").map { it.trim() })
                            else
                                authors.add(book.author)

                            publishers.add(book.publisher)
                            series.add(book.series)
                        }
                        Type.MANGA -> {
                            val manga = it as Manga
                            if (manga.author.endsWith("."))
                                authors.add(manga.author.substringBeforeLast("."))
                            else
                                authors.add(manga.author)

                            publishers.add(manga.publisher)
                            series.add(manga.series)
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

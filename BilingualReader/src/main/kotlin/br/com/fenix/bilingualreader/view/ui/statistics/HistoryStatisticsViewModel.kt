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
import br.com.fenix.bilingualreader.model.entity.HistoryGroup
import br.com.fenix.bilingualreader.model.entity.HistoryStatistics
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.entity.Separator
import br.com.fenix.bilingualreader.model.enums.FileType
import br.com.fenix.bilingualreader.model.enums.HistoryType
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
import java.time.format.DateTimeFormatter
import java.util.Locale
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

    private var mLoading = MutableLiveData(false)
    val loading: LiveData<Boolean> = mLoading

    private var mListFull = MutableLiveData<ArrayList<History>>(arrayListOf())
    private var mList = MutableLiveData<ArrayList<Any>>(arrayListOf())
    val history: LiveData<ArrayList<Any>> = mList

    private val mOrder = MutableLiveData(Pair(Order.LastAccess, false))
    val order: LiveData<Pair<Order, Boolean>> = mOrder

    private val mHistoryType = MutableLiveData(loadHistoryType())
    val historyType: LiveData<HistoryType> = mHistoryType

    private val mSeriesEmptyLabel: String = app.applicationContext.getString(R.string.history_series_empty)
    private val mFavoriteLabel: String = app.applicationContext.getString(R.string.manga_library_separator_favorite)
    private val mNotFavoriteLabel: String = app.applicationContext.getString(R.string.manga_library_separator_non_favorite)

    fun sorted(order: Order, isDesc: Boolean = false) {
        mOrder.value = Pair(order, isDesc)
        mList.value = filterList()
    }

    fun changeHistoryType() {
        val next = when (mHistoryType.value) {
            HistoryType.LINE -> HistoryType.SEPARATOR_BIG
            HistoryType.SEPARATOR_BIG -> HistoryType.SEPARATOR_MEDIUM
            HistoryType.SEPARATOR_MEDIUM -> HistoryType.SEPARATOR_CAROUSEL
            HistoryType.SEPARATOR_CAROUSEL -> HistoryType.SEPARATOR_LINE
            HistoryType.SEPARATOR_LINE -> HistoryType.LINE
            else -> HistoryType.SEPARATOR_LINE
        }
        setHistoryType(next)
    }

    fun setHistoryType(type: HistoryType) {
        if (mHistoryType.value == type)
            return

        mHistoryType.value = type
        GeneralConsts.getSharedPreferences(app.applicationContext).edit()
            .putString(GeneralConsts.KEYS.LIBRARY.HISTORY_STATISTICS_TYPE, type.toString())
            .apply()
        mList.value = filterList()
    }

    private fun loadHistoryType(): HistoryType {
        val raw = GeneralConsts.getSharedPreferences(app.applicationContext).getString(
            GeneralConsts.KEYS.LIBRARY.HISTORY_STATISTICS_TYPE,
            HistoryType.SEPARATOR_LINE.toString()
        ).toString()
        return try {
            HistoryType.valueOf(raw)
        } catch (_: Exception) {
            HistoryType.SEPARATOR_LINE
        }
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
                mList.value = filterList()
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

                if (mListFull.value == null || mListFull.value!!.isEmpty()) {
                    mListFull.value = ArrayList(list)
                } else
                    update(list)

                mList.value = filterList()
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

        return statsItems
    }

    fun update(list: List<History>) {
        if (list.isNotEmpty()) {
            for (history in list) {
                if (!mListFull.value!!.contains(history))
                    mListFull.value!!.add(history)
            }
            mList.value = filterList()
        }
    }

    fun updateDelete(history: History) {
        val base = unwrap(history)
        viewModelScope.launch(Dispatchers.IO) {
            when (base) {
                is Manga -> mMangaRepository.delete(base)
                is Book -> mBookRepository.delete(base)
            }
        }
    }

    fun updateLastAccess(history: History) {
        val base = unwrap(history)
        viewModelScope.launch(Dispatchers.IO) {
            when (base) {
                is Manga -> mMangaRepository.update(base)
                is Book -> mBookRepository.update(base)
            }
        }
    }

    fun clear(history: History?) {
        if (history != null) {
            val base = unwrap(history)
            save(base)
            removeMatching(history)
            mList.value = filterList()
        }
    }

    fun deletePermanent(history: History?) {
        history ?: return
        val base = unwrap(history)
        viewModelScope.launch(Dispatchers.IO) {
            when (base) {
                is Manga -> mMangaRepository.deletePermanent(base)
                is Book -> mBookRepository.deletePermanent(base)
            }
        }
    }

    fun save(history: History?) {
        history ?: return
        val base = unwrap(history)
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
        removeMatching(history)
        mList.value = filterList()
    }

    private fun removeMatching(history: History) {
        val target = unwrap(history)
        val access = history.lastAccess
        mListFull.value?.removeAll {
            val item = unwrap(it)
            item.id == target.id && it.lastAccess == access
        }
    }

    fun add(history: History, index: Int = -1) {
        if (mListFull.value != null) {
            if (index in 0..mListFull.value!!.size)
                mListFull.value!!.add(index, history)
            else
                mListFull.value!!.add(history)
        }
        mList.value = filterList()
    }

    fun getAndRemove(position: Int): History? {
        val item = mList.value?.getOrNull(position) as? History ?: return null
        remove(item)
        return item
    }

    private fun unwrap(history: History): History =
        if (history is HistoryStatistics) history.base else history

    private fun getSeparatorTitle(order: Order, history: History): String {
        return when (order) {
            Order.Name -> history.title.take(1).uppercase(Locale.getDefault()).ifEmpty { "#" }
            Order.LastAccess -> GeneralConsts.formatCountDays(app.applicationContext, history.lastAccess)
            Order.Favorite -> if (history.favorite) mFavoriteLabel else mNotFavoriteLabel
            Order.Author -> history.author.trim().lowercase(Locale.getDefault())
            Order.Genre -> history.genre.trim().lowercase(Locale.getDefault())
            Order.Series -> history.series.trim().ifEmpty { mSeriesEmptyLabel }
            else -> GeneralConsts.formatCountDays(app.applicationContext, history.lastAccess)
        }
    }

    private fun filterList(): ArrayList<Any> {
        val sortedList = arrayListOf<Any>()
        val contentItems = mutableListOf<History>()

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
                val base = unwrap(history)

                val currentLib = mLibrary.value
                if (currentLib != null) {
                    val key = if (currentLib.id == mDefaultKey) {
                        when (base) {
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
                            FilterType.Author -> history.author
                            FilterType.Publisher -> when (base) {
                                is Manga -> base.publisher
                                is Book -> base.publisher
                                else -> ""
                            }
                            FilterType.Series -> history.series
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

                if (matches)
                    contentItems.add(history)
            }
        }

        val currentOrder = mOrder.value ?: Pair(Order.LastAccess, false)
        val isDesc = currentOrder.second
        val order = currentOrder.first

        if (isDesc) {
            when (order) {
                Order.Name -> contentItems.sortByDescending { it.name }
                Order.Favorite -> contentItems.sortWith(compareBy<History> { it.favorite }.thenByDescending { it.name })
                Order.LastAccess -> contentItems.sortWith(compareBy<History> { it.lastAccess }.thenByDescending { it.name })
                Order.Series -> contentItems.sortWith(compareByDescending<History> { it.series }.thenByDescending { it.name })
                Order.Author -> contentItems.sortWith(compareByDescending<History> { it.author }.thenByDescending { it.name })
                Order.Genre -> contentItems.sortWith(compareByDescending<History> { it.genre }.thenByDescending { it.name })
                else -> contentItems.sortByDescending { it.lastAccess }
            }
        } else {
            when (order) {
                Order.Name -> contentItems.sortBy { it.name }
                Order.Favorite -> contentItems.sortWith(compareByDescending<History> { it.favorite }.thenBy { it.name })
                Order.LastAccess -> contentItems.sortWith(compareByDescending<History> { it.lastAccess }.thenBy { it.name })
                Order.Series -> contentItems.sortWith(compareBy<History> { it.series }.thenBy { it.name })
                Order.Author -> contentItems.sortWith(compareBy<History> { it.author }.thenBy { it.name })
                Order.Genre -> contentItems.sortWith(compareBy<History> { it.genre }.thenBy { it.name })
                else -> contentItems.sortByDescending { it.lastAccess }
            }
        }

        when (mHistoryType.value ?: HistoryType.SEPARATOR_LINE) {
            HistoryType.LINE -> sortedList.addAll(contentItems)
            HistoryType.SEPARATOR_LINE,
            HistoryType.SEPARATOR_BIG,
            HistoryType.SEPARATOR_MEDIUM -> buildGroupedList(sortedList, contentItems, order, carousel = false)
            HistoryType.SEPARATOR_CAROUSEL -> buildGroupedList(sortedList, contentItems, order, carousel = true)
        }

        return sortedList
    }

    private fun buildGroupedList(
        sortedList: ArrayList<Any>,
        contentItems: List<History>,
        order: Order,
        carousel: Boolean
    ) {
        if (contentItems.isEmpty())
            return

        val grouped = linkedMapOf<String, MutableList<History>>()
        for (item in contentItems) {
            val key = getSeparatorTitle(order, item)
            grouped.getOrPut(key) { mutableListOf() }.add(item)
        }

        for ((title, items) in grouped) {
            sortedList.add(Separator(title, items.size))
            if (carousel)
                sortedList.add(HistoryGroup(title, items))
            else
                sortedList.addAll(items)
        }
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
                next.clear()
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
            val list = arrayListOf<Any>()
            val values = filterResults?.values
            val items = when (values) {
                is Collection<*> -> values.filterIsInstance<Any>()
                else -> emptyList()
            }
            list.addAll(items)
            mList.value = list
        }
    }

    fun clearFilter() {
        mWordFilter = ""
        mList.value = filterList()
    }

    private fun setSuggestions(list: List<History>?) {
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
                    val it = unwrap(historyItem)
                    when (it.type) {
                        Type.BOOK -> {
                            val book = it as Book
                            if (book.author.contains(","))
                                authors.addAll(book.author.split(",").map { a -> a.trim() })
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

    fun getSuggestions(filter: String): List<String> {
        val type = filter.substringBeforeLast(':')
        val condition = filter.substringAfterLast(':')
        return when (Util.historyStringToFilter(app, type, true)) {
            FilterType.Author -> mSuggestionAuthor.parallelStream().filter { condition.isEmpty() || it.contains(condition, true) }.collect(Collectors.toList())
            FilterType.Publisher -> mSuggestionPublisher.parallelStream().filter { condition.isEmpty() || it.contains(condition, true) }.collect(Collectors.toList())
            FilterType.Series -> mSuggestionSeries.parallelStream().filter { condition.isEmpty() || it.contains(condition, true) }.collect(Collectors.toList())
            FilterType.Volume -> mSuggestionVolume.parallelStream().filter { condition.isEmpty() || it.contains(condition, true) }.collect(Collectors.toList())
            FilterType.Type -> FileType.getManga().parallelStream().map { "$it" }.collect(Collectors.toList())
            FilterType.Tag -> mSuggestionTags.parallelStream().map { "$it" }.collect(Collectors.toList())
            else -> listOf()
        }
    }

}

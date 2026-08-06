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
import br.com.fenix.bilingualreader.model.entity.HistoryGroup
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.entity.Separator
import br.com.fenix.bilingualreader.model.enums.FileType
import br.com.fenix.bilingualreader.model.enums.HistoryType
import br.com.fenix.bilingualreader.model.enums.Order
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

    private val mSelectedLibraries = MutableLiveData<Set<Library>>(emptySet())
    val selectedLibraries: LiveData<Set<Library>> = mSelectedLibraries
    private var mWordFilter: String = ""

    private var mLoading = MutableLiveData(false)
    val loading: LiveData<Boolean> = mLoading

    private val mContentTypes = MutableLiveData<Set<Type>>(emptySet())
    val contentTypes: LiveData<Set<Type>> = mContentTypes

    private var mType = MutableLiveData<Type?>(null)
    val type: LiveData<Type?> = mType

    private val mYears = MutableLiveData<Set<Int>>(emptySet())
    val selectedYears: LiveData<Set<Int>> = mYears

    private val mAvailableYears = MutableLiveData<List<Int>>(emptyList())
    val availableYears: LiveData<List<Int>> = mAvailableYears

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
            .putString(GeneralConsts.KEYS.LIBRARY.HISTORY_TYPE, type.toString())
            .apply()
        mList.value = filterList()
    }

    private fun loadHistoryType(): HistoryType {
        val raw = GeneralConsts.getSharedPreferences(app.applicationContext).getString(
            GeneralConsts.KEYS.LIBRARY.HISTORY_TYPE,
            HistoryType.SEPARATOR_LINE.toString()
        ).toString()
        return when (raw) {
            "LINE_DATE", "SERIES_LINE" -> HistoryType.SEPARATOR_LINE
            "SERIES_CAROUSEL" -> HistoryType.SEPARATOR_CAROUSEL
            else -> try {
                HistoryType.valueOf(raw)
            } catch (_: Exception) {
                HistoryType.SEPARATOR_LINE
            }
        }
    }

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
            var list = loadHistoryList()

            withContext(Dispatchers.Main) {
                mLoading.value = false

                mListFull.value = ArrayList(list)
                refreshAvailableYears(list)
                mList.value = filterList()
                setSuggestions(mListFull.value)
            }
        }
    }

    fun list(refreshComplete: (Int) -> (Unit)) {
        mLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            var list = loadHistoryList()

            withContext(Dispatchers.Main) {
                mLoading.value = false

                if (mListFull.value == null || mListFull.value!!.isEmpty()) {
                    mListFull.value = ArrayList(list)
                } else
                    update(list)

                refreshAvailableYears(mListFull.value)
                mList.value = filterList()
                setSuggestions(mListFull.value)

                refreshComplete(mList.value!!.size - 1)
            }
        }
    }

    private fun loadHistoryList(): MutableList<History> {
        var list = mutableListOf<History>()

        val mangas = mMangaRepository.listHistory()
        if (mangas != null)
            list.addAll(mangas)

        val books = mBookRepository.listHistory()
        if (books != null)
            list.addAll(books)

        list = list.filter { it.id != null }.toMutableList()

        val format = DateTimeFormatter.ofPattern(GeneralConsts.PATTERNS.DATE_TIME_PATTERN)
        return list.sortedByDescending { it.lastAccess }
            .distinctBy { it.lastAccess!!.format(format) }
            .toMutableList()
    }

    private fun refreshAvailableYears(list: List<History>?) {
        mAvailableYears.value = list.orEmpty()
            .mapNotNull { it.lastAccess?.year }
            .distinct()
            .sortedDescending()
    }

    fun update(list: List<History>) {
        if (list.isNotEmpty()) {
            for (history in list) {
                if (!mListFull.value!!.contains(history))
                    mListFull.value!!.add(history)
            }
            refreshAvailableYears(mListFull.value)
            mList.value = filterList()
        }
    }

    fun updateDelete(history: History) {
        viewModelScope.launch(Dispatchers.IO) {
            when (history) {
                is Manga -> mMangaRepository.delete(history)
                is Book -> mBookRepository.delete(history)
            }
        }
    }

    fun updateLastAccess(history: History) {
        viewModelScope.launch(Dispatchers.IO) {
            when (history) {
                is Manga -> mMangaRepository.update(history)
                is Book -> mBookRepository.update(history)
            }
        }
    }

    fun clear(history: History?) {
        if (history != null) {
            save(history)
            mListFull.value?.remove(history)
            mList.value = filterList()
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
        if (history.id == null && history.title.isBlank())
            return
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
        mListFull.value?.remove(history)
        mList.value = filterList()
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

            val contentFilter = mContentTypes.value.orEmpty()
            val libraryFilter = mSelectedLibraries.value.orEmpty()
            val yearFilter = mYears.value.orEmpty()

            for (history in mListFull.value!!) {
                if (contentFilter.isNotEmpty() && history.type !in contentFilter)
                    continue

                if (libraryFilter.isNotEmpty() && !matchesLibrary(history, libraryFilter))
                    continue

                if (yearFilter.isNotEmpty()) {
                    val year = history.lastAccess?.year ?: continue
                    if (year !in yearFilter)
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
                            FilterType.Publisher -> if (history is Manga) history.publisher else if (history is Book) history.publisher else ""
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

                if (matches) {
                    contentItems.add(history)
                }
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

    private fun matchesLibrary(history: History, libraries: Set<Library>): Boolean {
        return libraries.any { lib ->
            val key = if (lib.id == mDefaultKey) {
                when (history) {
                    is Manga -> GeneralConsts.KEYS.LIBRARY.DEFAULT_MANGA
                    is Book -> GeneralConsts.KEYS.LIBRARY.DEFAULT_BOOK
                    else -> lib.id
                }
            } else
                lib.id
            history.fkLibrary == key
        }
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

    fun filterLibraries(libraries: Set<Library>) {
        if (libraries == mSelectedLibraries.value)
            return

        mSelectedLibraries.value = libraries
        mList.value = filterList()
    }

    fun filterLibrary(library: Library?) {
        filterLibraries(if (library == null) emptySet() else setOf(library))
    }

    fun filterContentTypes(types: Set<Type>) {
        val normalized = if (types.size >= Type.values().size) emptySet() else types
        if (normalized == mContentTypes.value)
            return

        mContentTypes.value = normalized
        mType.value = when {
            normalized.isEmpty() -> null
            normalized.size == 1 -> normalized.first()
            else -> null
        }
        mList.value = filterList()
    }

    fun filterType(type: Type?) {
        filterContentTypes(if (type == null) emptySet() else setOf(type))
    }

    fun filterYears(years: Set<Int>) {
        if (years == mYears.value)
            return

        mYears.value = years
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

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, filterResults: FilterResults?) {
            val list = arrayListOf<Any>()
            filterResults?.let {
                list.addAll(it.values as Collection<Any>)
            }
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

                process.forEach {
                    when (it.type) {
                        Type.BOOK -> {
                            if ((it as Book).author.contains(","))
                                authors.addAll(it.author.split(",").map { a -> a.trim() })
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

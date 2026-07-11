package br.com.fenix.bilingualreader.view.ui.library.manga

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.widget.Filter
import android.widget.Filterable
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.entity.SubTitleChapter
import br.com.fenix.bilingualreader.model.enums.FileType
import br.com.fenix.bilingualreader.model.enums.Import
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.LibraryMangaType
import br.com.fenix.bilingualreader.model.enums.ListMode
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.model.enums.ShareMarkType
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.controller.SubTitleController
import br.com.fenix.bilingualreader.service.parses.manga.ParseFactory
import br.com.fenix.bilingualreader.service.parses.manga.RarParse
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import br.com.fenix.bilingualreader.service.repository.VocabularyRepository
import br.com.fenix.bilingualreader.service.sharemark.ShareMarkBase
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.Notifications
import br.com.fenix.bilingualreader.util.helpers.Telemetry
import br.com.fenix.bilingualreader.util.helpers.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDateTime
import java.util.Collections
import java.util.Date
import java.util.LinkedHashMap
import java.util.Locale
import java.util.Objects
import java.util.regex.Pattern
import java.util.stream.Collectors
import br.com.fenix.bilingualreader.model.enums.Filter as FilterType


class MangaLibraryViewModel(var app: Application) : AndroidViewModel(app), Filterable {

    private val mLOGGER = LoggerFactory.getLogger(MangaLibraryViewModel::class.java)

    var isLoading : Boolean = true

    private var mStackLibrary = mutableMapOf<String, Triple<Int, Library, LinkedHashMap<Long, Manga>>>()
    private var mLibrary: Library = Library(GeneralConsts.KEYS.LIBRARY.DEFAULT_MANGA)
    private val mMangaRepository: MangaRepository = MangaRepository(app.applicationContext)
    private val mPreferences = GeneralConsts.getSharedPreferences(app.applicationContext)

    private var mLoading = MutableLiveData<Boolean>(false)
    val loading: LiveData<Boolean> = mLoading

    private var mWordFilter = ""

    private var mOrder = MutableLiveData(Pair(Order.Name, false))
    val order: LiveData<Pair<Order, Boolean>> = mOrder
    private var mTypeFilter = MutableLiveData(FilterType.None)
    val typeFilter: LiveData<FilterType> = mTypeFilter

    private var mLibraryType = MutableLiveData(LibraryMangaType.GRID_BIG)
    val libraryType: LiveData<LibraryMangaType> = mLibraryType

    private val mFullMap = LinkedHashMap<Long, Manga>()
    private var mListMangas = MutableLiveData<MutableList<Manga>>(mutableListOf())
    val listMangas: LiveData<MutableList<Manga>> = mListMangas

    private var mSuggestionAuthor = setOf<String>()
    private var mSuggestionPublisher = setOf<String>()
    private var mSuggestionSeries = setOf<String>()
    private var mSuggestionVolume = setOf<String>()

    private var mProcessShareMark = false

    private data class IncrementalDiff(
        val change: Boolean,
        val indexes: MutableList<Pair<ListMode, Int>>,
        val toAdd: List<Manga>,
        val toRemoveVisibleIndices: List<Int>,
        val existingUpdates: List<Pair<Long, Manga>>
    )

    private fun isFilterActive(): Boolean =
        mWordFilter.isNotEmpty() || mTypeFilter.value != FilterType.None

    private fun fullValues(): Collection<Manga> = mFullMap.values

    private fun setFullFromList(list: List<Manga>) {
        mFullMap.clear()
        for (manga in list) {
            manga.id?.let { mFullMap[it] = manga }
        }
    }

    private fun rebuildFullMap(sorted: List<Manga>) {
        mFullMap.clear()
        for (manga in sorted) {
            manga.id?.let { mFullMap[it] = manga }
        }
    }

    private fun insertInFullMap(manga: Manga, position: Int) {
        val id = manga.id ?: return
        if (position > -1 && position < mFullMap.size) {
            val newMap = LinkedHashMap<Long, Manga>()
            var index = 0
            for ((key, value) in mFullMap) {
                if (index == position) newMap[id] = manga
                newMap[key] = value
                index++
            }
            mFullMap.clear()
            mFullMap.putAll(newMap)
        } else {
            mFullMap[id] = manga
        }
    }

    private fun removeFromFull(manga: Manga) {
        manga.id?.let { mFullMap.remove(it) }
    }

    private fun containsInFull(manga: Manga): Boolean =
        manga.id != null && mFullMap.containsKey(manga.id)

    private fun setSuggestionsFromFull() = setSuggestions(mFullMap.values.toList())

    private fun sortList(list: MutableList<Manga>, order: Order, isDesc: Boolean) {
        if (isDesc) {
            when (order) {
                Order.Date -> list.sortByDescending { it.dateCreate }
                Order.LastAccess -> list.sortWith(compareBy<Manga> { it.lastAccess }.thenByDescending { it.name })
                Order.Favorite -> list.sortWith(compareBy<Manga> { it.favorite }.thenByDescending { it.name })
                else -> list.sortByDescending { it.name }
            }
        } else {
            when (order) {
                Order.Date -> list.sortBy { it.dateCreate }
                Order.LastAccess -> list.sortWith(compareByDescending<Manga> { it.lastAccess }.thenBy { it.name })
                Order.Favorite -> list.sortWith(compareByDescending<Manga> { it.favorite }.thenBy { it.name })
                else -> list.sortBy { it.name }
            }
        }
    }

    private fun computeIncrementalDiff(
        recentChanges: List<Manga>?,
        recentDeleted: List<Manga>?,
        fullSnapshot: Map<Long, Manga>,
        visibleSnapshot: List<Manga>
    ): IncrementalDiff {
        val visibleIndexById = HashMap<Long, Int>()
        visibleSnapshot.forEachIndexed { index, manga ->
            manga.id?.let { visibleIndexById[it] = index }
        }

        val indexes = mutableListOf<Pair<ListMode, Int>>()
        var change = false
        val toAdd = mutableListOf<Manga>()
        val toRemoveVisibleIndices = mutableListOf<Int>()
        val existingUpdates = mutableListOf<Pair<Long, Manga>>()

        if (!recentChanges.isNullOrEmpty()) {
            change = true
            for (manga in recentChanges) {
                val id = manga.id ?: continue
                val existing = fullSnapshot[id]
                if (existing != null) {
                    if (existing.modify(manga)) {
                        existingUpdates.add(id to manga)
                        visibleIndexById[id]?.let { index ->
                            indexes.add(Pair(ListMode.MOD, index))
                        }
                    }
                } else {
                    toAdd.add(manga)
                }
            }
        }

        if (!recentDeleted.isNullOrEmpty()) {
            change = true
            for (manga in recentDeleted) {
                val id = manga.id ?: continue
                if (fullSnapshot.containsKey(id)) {
                    visibleIndexById[id]?.let { index ->
                        toRemoveVisibleIndices.add(index)
                        indexes.add(Pair(ListMode.REM, index))
                    }
                }
            }
        }

        return IncrementalDiff(change, indexes, toAdd, toRemoveVisibleIndices, existingUpdates)
    }

    private fun applyIncrementalDiff(diff: IncrementalDiff) {
        for ((id, manga) in diff.existingUpdates) {
            mFullMap[id]?.update(manga, true)
        }

        for (manga in diff.toAdd) {
            manga.id?.let { mFullMap[it] = manga }
            mListMangas.value!!.add(manga)
            diff.indexes.add(Pair(ListMode.ADD, mListMangas.value!!.size - 1))
        }

        for (index in diff.toRemoveVisibleIndices.sortedDescending()) {
            val manga = mListMangas.value!!.removeAt(index)
            removeFromFull(manga)
        }
    }

    fun setDefaultLibrary(library: Library) {
        if (mLibrary.id == library.id)
            mLibrary = library
    }

    fun setLibrary(library: Library) {
        if (mLibrary.id != library.id) {
            mFullMap.clear()
            mListMangas.value = mutableListOf()
            setSuggestionsFromFull()
        }
        mLibrary = library
    }

    fun saveLastLibrary() {
        val key = if (mLibrary.id == GeneralConsts.KEYS.LIBRARY.DEFAULT_MANGA)
            R.id.menu_manga_library_default
        else
            mLibrary.id!!
        mPreferences.edit().putLong(
            GeneralConsts.KEYS.LIBRARY.LAST_LIBRARY,
            key.toLong()
        ).apply()
    }

    fun getLibrary() = mLibrary

    fun existStack(id: String): Boolean = mStackLibrary.contains(id)

    fun restoreLastStackLibrary(id: String) {
        if (mStackLibrary.contains(id)) {
            val item = mStackLibrary.remove(id)!!
            mLibrary = item.second
            mFullMap.clear()
            mFullMap.putAll(item.third)
            mListMangas.value = mFullMap.values.toMutableList()
            setSuggestionsFromFull()
        }
    }

    fun addStackLibrary(id: String, library: Library) =
        mStackLibrary.put(id, Triple(mStackLibrary.size + 1, library, LinkedHashMap(mFullMap)))

    fun removeStackLibrary(id: String) = mStackLibrary.remove(id)

    fun emptyList(idLibrary: Long) {
        if (mLibrary.id == idLibrary) {
            mFullMap.clear()
            mListMangas.value = mutableListOf()
            setSuggestionsFromFull()
        } else {
            for (stack in mStackLibrary)
                if (stack.value.second.id == idLibrary)
                    stack.value.third.clear()
        }
    }

    fun clearHistory(manga: Manga) {
        mMangaRepository.clearHistory(manga)
    }

    fun save(obj: Manga): Manga {
        if (obj.id == 0L)
            obj.id = mMangaRepository.save(obj)
        else
            mMangaRepository.update(obj)

        return obj
    }

    fun add(manga: Manga, position: Int = -1) {
        if (position > -1) {
            mListMangas.value!!.add(position, manga)
            insertInFullMap(manga, position)
        } else {
            mListMangas.value!!.add(manga)
            insertInFullMap(manga, -1)
        }
    }

    fun delete(obj: Manga) {
        mMangaRepository.delete(obj)
        remove(obj)
    }

    fun getAndRemove(position: Int): Manga? {
        val manga = if (mListMangas.value != null) mListMangas.value!!.removeAt(position) else null
        if (manga != null) removeFromFull(manga)
        return manga
    }

    fun remove(manga: Manga) {
        mListMangas.value!!.remove(manga)
        removeFromFull(manga)
    }

    fun remove(position: Int) {
        val manga = mListMangas.value!!.removeAt(position)
        removeFromFull(manga)
    }

    fun update(list: List<Manga>) {
        if (list.isNotEmpty()) {
            for (manga in list) {
                if (!containsInFull(manga)) {
                    mListMangas.value!!.add(manga)
                    insertInFullMap(manga, -1)
                }
            }
        }
    }

    fun setList(list: ArrayList<Manga>) {
        mListMangas.value = list
        setFullFromList(list)
    }

    fun addList(manga: Manga): Int {
        var index = -1
        if (!containsInFull(manga)) {
            index = mListMangas.value!!.size
            mListMangas.value!!.add(manga)
            insertInFullMap(manga, -1)
        }

        return index
    }

    fun remList(manga: Manga): Int {
        var index = -1

        if (containsInFull(manga)) {
            index = mListMangas.value!!.indexOf(manga)
            mListMangas.value!!.remove(manga)
            removeFromFull(manga)
        }

        return index
    }

    fun updateList(index: Int): Int {
        val list = mListMangas.value
        if (list.isNullOrEmpty() || index < 0 || index >= list.size) {
            return -1
        }
        val manga = list[index]
        val updatedManga = mMangaRepository.get(manga.id!!)
        if (updatedManga != null && manga.modify(updatedManga)) {
            manga.update(updatedManga, true)
            return index
        }
        return -1
    }

    fun updateList(refreshComplete: (Boolean, indexes: MutableList<Pair<ListMode, Int>>) -> (Unit)) {
        viewModelScope.launch {
            if (mFullMap.isNotEmpty()) {
                val list = withContext(Dispatchers.IO) { mMangaRepository.listRecentChange(mLibrary) }
                val listDel = withContext(Dispatchers.IO) { mMangaRepository.listRecentDeleted(mLibrary) }

                val fullSnapshot = LinkedHashMap(mFullMap)
                val visibleSnapshot = mListMangas.value?.toList() ?: emptyList()

                val diff = withContext(Dispatchers.Default) {
                    computeIncrementalDiff(list, listDel, fullSnapshot, visibleSnapshot)
                }

                withContext(Dispatchers.Main) {
                    applyIncrementalDiff(diff)
                    setSuggestionsFromFull()
                    refreshComplete(diff.change, diff.indexes)
                }
            } else {
                val list = withContext(Dispatchers.IO) { mMangaRepository.list(mLibrary) }
                val indexes = mutableListOf<Pair<ListMode, Int>>()
                if (list != null) {
                    indexes.add(Pair(ListMode.FULL, list.size))
                    mListMangas.value = list.toMutableList()
                    setFullFromList(list)
                    sorted()
                } else {
                    mListMangas.value = mutableListOf()
                    mFullMap.clear()
                    indexes.add(Pair(ListMode.FULL, 0))
                }
                setSuggestionsFromFull()
                refreshComplete(false, indexes)
            }
        }
    }

    fun list(refreshComplete: (Boolean) -> (Unit)) {
        mLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val list = mMangaRepository.list(mLibrary)
            withContext(Dispatchers.Main) {
                mLoading.value = false

                if (list != null) {
                    if (mFullMap.isEmpty()) {
                        mListMangas.value = list.toMutableList()
                        setFullFromList(list)
                        setSuggestionsFromFull()
                    } else
                        update(list)
                } else {
                    mFullMap.clear()
                    mListMangas.value = mutableListOf()
                    setSuggestionsFromFull()
                }

                refreshComplete(mListMangas.value!!.isNotEmpty())
            }
        }
    }

    fun changeLibraryType() {
        val isLandscape = app.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val type = when (mLibraryType.value) {
            LibraryMangaType.LINE -> LibraryMangaType.GRID_BIG
            LibraryMangaType.GRID_BIG -> LibraryMangaType.GRID_MEDIUM
            LibraryMangaType.GRID_MEDIUM -> if (isLandscape) LibraryMangaType.GRID_SMALL else LibraryMangaType.SEPARATOR_BIG
            LibraryMangaType.GRID_SMALL -> LibraryMangaType.SEPARATOR_BIG
            LibraryMangaType.SEPARATOR_BIG -> LibraryMangaType.SEPARATOR_MEDIUM
            LibraryMangaType.SEPARATOR_MEDIUM -> LibraryMangaType.LINE
            else -> LibraryMangaType.LINE
        }
        setLibraryType(type)
    }

    fun setLibraryType(type: LibraryMangaType) {
        mLibraryType.value = type
    }

    fun isEmpty(): Boolean = mListMangas.value == null || mListMangas.value!!.isEmpty()

    fun sorted() = sorted(mOrder.value?.first ?: Order.Name)

    fun sorted(order: Order, isDesc: Boolean = false) {
        mOrder.value = Pair(order, isDesc)

        val sortedFull = mFullMap.values.toMutableList()
        sortList(sortedFull, order, isDesc)
        rebuildFullMap(sortedFull)

        if (!isFilterActive()) {
            mListMangas.value = sortedFull
        } else {
            val sortedVisible = mListMangas.value!!.toMutableList()
            sortList(sortedVisible, order, isDesc)
            mListMangas.value = sortedVisible
        }
    }

    private fun setSuggestions(list : List<Manga>?) {
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
                    if (it.author.endsWith("."))
                        authors.add(it.author.substringBeforeLast("."))
                    else
                        authors.add(it.author)

                    publishers.add(it.publisher)
                    series.add(it.series)
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
        return when(Util.stringToFilter(app, Type.MANGA, type, true)) {
            FilterType.Author -> mSuggestionAuthor.parallelStream().filter { condition.isEmpty() || it.contains(condition, true) }.collect(Collectors.toList())
            FilterType.Publisher -> mSuggestionPublisher.parallelStream().filter { condition.isEmpty() || it.contains(condition, true) }.collect(Collectors.toList())
            FilterType.Series -> mSuggestionSeries.parallelStream().filter { condition.isEmpty() || it.contains(condition, true) }.collect(Collectors.toList())
            FilterType.Volume ->  mSuggestionVolume.parallelStream().filter { condition.isEmpty() || it.contains(condition, true) }.collect(Collectors.toList())
            FilterType.Type -> FileType.getManga().parallelStream().map { "$it" }.collect(Collectors.toList())
            else -> listOf()
        }
    }

    fun filterType(filter: FilterType) {
        mTypeFilter.value = filter
        getFilter().filter(mWordFilter)
    }

    fun clearFilterType() = filterType(FilterType.None)

    fun clearFilter() {
        mTypeFilter.value = FilterType.None
        mWordFilter = ""
        val newList: MutableList<Manga> = mutableListOf()
        newList.addAll(fullValues().filter(Objects::nonNull))
        mListMangas.value = newList
    }

    override fun getFilter(): Filter {
        return mMangaFilter
    }

    private fun filtered(manga: Manga?, filterPattern: String, filterConditions :ArrayList<Pair<FilterType, String>>): Boolean {
        if (manga == null)
            return false

        if (mTypeFilter.value != FilterType.None) {
            if (mTypeFilter.value == FilterType.Reading && manga.lastAccess == null)
                return false

            if (mTypeFilter.value == FilterType.Favorite && !manga.favorite)
                return false
        }

        if (filterConditions.isNotEmpty()) {
            var condition = false
            filterConditions.forEach {
                when (it.first) {
                    FilterType.Type -> {
                        if (manga.fileType.name.contains(it.second, true))
                            condition = true
                    }
                    FilterType.Volume -> {
                        if (manga.volume.equals(it.second, true))
                            condition = true
                    }
                    FilterType.Publisher -> {
                        if (manga.publisher.contains(it.second, true))
                            condition = true
                    }
                    FilterType.Series -> {
                        if (manga.series.contains(it.second, true))
                            condition = true
                    }
                    FilterType.Author -> {
                        if (manga.author.contains(it.second, true))
                            condition = true
                    }
                    else -> {}
                }
            }

            if (!condition)
                return false
        }

        return filterPattern.isEmpty() || manga.name.lowercase(Locale.getDefault()).contains(filterPattern) || manga.fileType.compareExtension(filterPattern)
    }

    private val mMangaFilter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            mWordFilter = constraint?.toString() ?: ""
            val filteredList: MutableList<Manga> = mutableListOf()

            if (constraint.isNullOrEmpty() && mTypeFilter.value == FilterType.None) {
                filteredList.addAll(fullValues().filter(Objects::nonNull))
            } else {
                var filterPattern = constraint?.toString() ?: ""
                val filterCondition = arrayListOf<Pair<FilterType, String>>()
                if (constraint != null && constraint.contains('@')) {
                    val m = Pattern.compile("(@\\S*:(\"[^\"]*\"|[^\\s]+))\\s*").matcher(constraint)
                    while (m.find()) {
                        val fullMatch = m.group(0) ?: continue
                        val item = m.group(1)?.replace("\"", "") ?: continue
                        filterPattern = filterPattern.replace(fullMatch, "", true)
                        val type = Util.stringToFilter(app.applicationContext, Type.MANGA, item.substringBefore(":").replace("@", ""))
                        if (type != FilterType.None) {
                            val condition = item.substringAfter(":")
                            if (condition.isNotEmpty())
                                filterCondition.add(Pair(type, condition))
                        }
                    }
                }

                filterPattern = filterPattern.lowercase(Locale.getDefault()).trim()
                filteredList.addAll(fullValues().filter {
                    filtered(it, filterPattern, filterCondition)
                })
            }

            val results = FilterResults()
            results.values = filteredList

            return results
        }

        override fun publishResults(constraint: CharSequence?, filterResults: FilterResults?) {
            val list = mutableListOf<Manga>()
            filterResults?.let {
                list.addAll(it.values as Collection<Manga>)
            }
            mListMangas.value = list
        }
    }

    /**
     * @param context    Context system
     * @param processed  Function call when processed, parameter is true if can notify list change
     */
    fun processShareMarks(context: Context, idNotification : Int, processed: (result: ShareMarkType, idNotification: Int) -> (Unit)) {
        if (!mProcessShareMark) {
            mProcessShareMark = true
            val share = ShareMarkBase.getInstance(context)
            var notify = false
            val process: (manga: Manga) -> (Unit) = { item ->
                if (mLibrary.id == item.fkLibrary) {
                    notify = true
                    item.id?.let { id ->
                        mFullMap[id]?.let { manga ->
                            manga.favorite = item.favorite
                            manga.bookMark = item.bookMark
                            manga.pages = item.pages
                            manga.completed = item.completed
                            manga.lastAccess = item.lastAccess
                        }
                    }
                }
            }
            share.mangaShareMark(process) {
                mProcessShareMark = false
                if ((it == ShareMarkType.SUCCESS || it == ShareMarkType.NOT_ALTERATION) && notify)
                    processed(ShareMarkType.NOTIFY_DATA_SET, idNotification)
                else
                    processed(it, idNotification)
            }
        } else
            processed(ShareMarkType.SYNC_IN_PROGRESS, idNotification)
    }

    var mImportingVocab = false
    fun importVocabulary(import: Import = Import.FULL_ITEMS) {
        if (mImportingVocab)
            return

        val list = mFullMap.values.toList()
        val cache = GeneralConsts.getCacheDir(app.applicationContext)

        if (list.isEmpty())
            return

        val repository = VocabularyRepository(app.applicationContext)
        mImportingVocab = true

        val notifyId = Notifications.getID()
        val notificationManager = NotificationManagerCompat.from(app.applicationContext)
        val notification = Notifications.getNotification(app.applicationContext, app.getString(R.string.vocabulary_import_title), "")

        if (ActivityCompat.checkSelfPermission(app.applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
            notificationManager.notify(notifyId, notification.build())

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val size = list.size
                for ((index, manga) in list.withIndex()) {
                    when (import) {
                        Import.DEFAULT ->  {
                            if (manga.lastVocabImport != null && manga.lastVocabImport!!.isAfter(LocalDateTime.now().minusDays(1)))
                                continue
                        }
                        Import.RE_IMPORT -> {
                            if (manga.lastVocabImport == null)
                                continue
                        }
                        Import.NEW_ITEMS -> {
                            if (manga.lastVocabImport != null)
                                continue
                        }
                        Import.FULL_ITEMS -> { }
                    }

                    withContext(Dispatchers.Main) {
                        notification.setContentText(manga.name).setProgress(size, index, false).setOngoing(true)

                        if (ActivityCompat.checkSelfPermission(app.applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
                            notificationManager.notify(notifyId, notification.build())
                    }

                    val parse = ParseFactory.create(manga.file) ?: continue

                    try {
                        if (parse is RarParse) {
                            val folder = GeneralConsts.CACHE_FOLDER.RAR + '/' + Util.normalizeNameCache(manga.name)
                            val cacheDir = File(cache, folder)
                            (parse as RarParse?)!!.setCacheDirectory(cacheDir)
                        }

                        val listJson: List<String> = parse.getSubtitles()
                        if (listJson.isNotEmpty()) {
                            val listSubTitleChapter: MutableList<SubTitleChapter> = SubTitleController.getChapterFromJson(listJson)
                            val chaptersList = Collections.synchronizedCollection(listSubTitleChapter.parallelStream()
                                .filter(Objects::nonNull)
                                .filter { it.language == Languages.JAPANESE && it.vocabulary.isNotEmpty() }
                                .collect(Collectors.toList()))
                            val processed = repository.processVocabulary(chaptersList)
                            for (vocab in processed) {
                                vocab.first.id = repository.save(vocab.first)
                                vocab.first.id?.let { repository.insert(manga.id!!, it, vocab.second) }
                            }

                            manga.lastVocabImport = LocalDateTime.now()
                            manga.fileAlteration = Date(manga.file.lastModified())

                            repository.updateImport(manga)
                        }
                    } finally {
                        Util.destroyParse(parse)
                    }
                }
                withContext(Dispatchers.Main) {
                    val mMsgImport = app.getString(R.string.vocabulary_imported)
                    notification.setContentText(mMsgImport)
                        .setProgress(list.size, list.size, false)

                    if (ActivityCompat.checkSelfPermission(app.applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
                        notificationManager.notify(notifyId, notification.build())
                }
            } catch (e: Exception) {
                mLOGGER.error("Error to import vocabulary: " + e.message, e)
                Telemetry.recordException(e, "Error to import vocabulary: " + e.message)
            } finally {
                withContext(Dispatchers.Main) {
                    mImportingVocab = false
                    notification.setOngoing(false)

                    if (ActivityCompat.checkSelfPermission(app.applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
                        notificationManager.notify(notifyId, notification.build())
                }
            }
        }
    }
}

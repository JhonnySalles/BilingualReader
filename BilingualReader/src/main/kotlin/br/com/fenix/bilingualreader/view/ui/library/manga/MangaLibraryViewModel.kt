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
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.entity.SubTitleChapter
import br.com.fenix.bilingualreader.model.enums.FileType
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
import br.com.fenix.bilingualreader.util.helpers.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDateTime
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.Objects
import java.util.regex.Pattern
import java.util.stream.Collectors
import br.com.fenix.bilingualreader.model.enums.Filter as FilterType


class MangaLibraryViewModel(var app: Application) : AndroidViewModel(app), Filterable {

    private val mLOGGER = LoggerFactory.getLogger(MangaLibraryViewModel::class.java)

    var isLaunch : Boolean = true

    private var mStackLibrary = mutableMapOf<String, Triple<Int, Library, MutableList<Manga>>>()
    private var mLibrary: Library = Library(GeneralConsts.KEYS.LIBRARY.DEFAULT_MANGA)
    private val mMangaRepository: MangaRepository = MangaRepository(app.applicationContext)
    private val mPreferences = GeneralConsts.getSharedPreferences(app.applicationContext)

    private var mWordFilter = ""

    private var mOrder = MutableLiveData(Pair(Order.Name, false))
    val order: LiveData<Pair<Order, Boolean>> = mOrder
    private var mTypeFilter = MutableLiveData(FilterType.None)
    val typeFilter: LiveData<FilterType> = mTypeFilter

    private var mLibraryType = MutableLiveData(LibraryMangaType.GRID_BIG)
    val libraryType: LiveData<LibraryMangaType> = mLibraryType

    private var mListMangasFull = MutableLiveData<MutableList<Manga>>(mutableListOf())
    private var mListMangas = MutableLiveData<MutableList<Manga>>(mutableListOf())
    val listMangas: LiveData<MutableList<Manga>> = mListMangas

    private var mSuggestionAuthor = setOf<String>()
    private var mSuggestionPublisher = setOf<String>()
    private var mSuggestionSeries = setOf<String>()
    private var mSuggestionVolume = setOf<String>()

    private var mProcessShareMark = false

    fun setDefaultLibrary(library: Library) {
        if (mLibrary.id == library.id)
            mLibrary = library
    }

    fun setLibrary(library: Library) {
        if (mLibrary.id != library.id) {
            mListMangasFull.value = mutableListOf()
            mListMangas.value = mutableListOf()
            setSuggestions(mListMangasFull.value)
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
            mStackLibrary.remove(id)
            for (item in mStackLibrary) {
                if (item.value.first == mStackLibrary.size) {
                    mLibrary = item.value.second
                    mListMangasFull.value = item.value.third.toMutableList()
                    mListMangas.value = item.value.third.toMutableList()
                    setSuggestions(mListMangasFull.value)
                    break
                }
            }
        }
    }

    fun addStackLibrary(id: String, library: Library) = mStackLibrary.put(id, Triple(mStackLibrary.size + 1, library, mListMangasFull.value!!))

    fun removeStackLibrary(id: String) = mStackLibrary.remove(id)

    fun emptyList(idLibrary: Long) {
        if (mLibrary.id == idLibrary) {
            mListMangasFull.value = mutableListOf()
            mListMangas.value = mutableListOf()
            setSuggestions(mListMangasFull.value)
        } else {
            for (stack in mStackLibrary)
                if (stack.value.second.id == idLibrary)
                    stack.value.third.clear()
        }
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
            mListMangasFull.value!!.add(position, manga)
        } else {
            mListMangas.value!!.add(manga)
            mListMangasFull.value!!.add(manga)
        }
    }

    fun delete(obj: Manga) {
        mMangaRepository.delete(obj)
        remove(obj)
    }

    fun getAndRemove(position: Int): Manga? {
        val manga = if (mListMangas.value != null) mListMangas.value!!.removeAt(position) else null
        if (manga != null) mListMangasFull.value!!.remove(manga)
        return manga
    }

    fun remove(manga: Manga) {
        if (mListMangasFull.value != null) {
            mListMangas.value!!.remove(manga)
            mListMangasFull.value!!.remove(manga)
        }
    }

    fun remove(position: Int) {
        if (mListMangasFull.value != null) {
            val manga = mListMangas.value!!.removeAt(position)
            mListMangasFull.value!!.remove(manga)
        }
    }

    fun update(list: List<Manga>) {
        if (list.isNotEmpty()) {
            for (manga in list) {
                if (!mListMangasFull.value!!.contains(manga)) {
                    mListMangas.value!!.add(manga)
                    mListMangasFull.value!!.add(manga)
                }
            }
        }
    }

    fun setList(list: ArrayList<Manga>) {
        mListMangas.value = list
        mListMangasFull.value = list.toMutableList()
    }

    fun addList(manga: Manga): Int {
        var index = -1
        if (!mListMangasFull.value!!.contains(manga)) {
            index = mListMangas.value!!.size
            mListMangas.value!!.add(manga)
            mListMangasFull.value!!.add(manga)
        }

        return index
    }

    fun remList(manga: Manga): Int {
        var index = -1

        if (mListMangasFull.value!!.contains(manga)) {
            index = mListMangas.value!!.indexOf(manga)
            mListMangas.value!!.remove(manga)
            mListMangasFull.value!!.remove(manga)
        }

        return index
    }

    fun updateList(index : Int) : Int {
        val manga = mListMangas.value!![index]
        mMangaRepository.get(manga.id!!)?.let {
            manga.update(it, true)
        }
        return index
    }

    fun updateList(refreshComplete: (Boolean, indexes: MutableList<Pair<ListMode, Int>>) -> (Unit)) {
        var change = false
        val indexes = mutableListOf<Pair<ListMode, Int>>()
        if (mListMangasFull.value != null && mListMangasFull.value!!.isNotEmpty()) {
            val list = mMangaRepository.listRecentChange(mLibrary)
            if (!list.isNullOrEmpty()) {
                change = true
                for (manga in list) {
                    if (mListMangasFull.value!!.contains(manga)) {
                        if (mListMangasFull.value!![mListMangasFull.value!!.indexOf(manga)].update(manga, true)) {
                            val index = mListMangas.value!!.indexOf(manga)
                            if (index > -1)
                                indexes.add(Pair(ListMode.MOD, index))
                        }
                    } else {
                        mListMangas.value!!.add(manga)
                        mListMangasFull.value!!.add(manga)
                        indexes.add(Pair(ListMode.ADD, mListMangas.value!!.size))
                    }
                }
            }
            val listDel = mMangaRepository.listRecentDeleted(mLibrary)
            if (!listDel.isNullOrEmpty()) {
                change = true
                for (manga in listDel) {
                    if (mListMangasFull.value!!.contains(manga)) {
                        val index = mListMangas.value!!.indexOf(manga)
                        mListMangas.value!!.remove(manga)
                        mListMangasFull.value!!.remove(manga)
                        indexes.add(Pair(ListMode.REM, index))
                    }
                }
            }
        } else {
            val list = mMangaRepository.list(mLibrary)
            if (list != null) {
                indexes.add(Pair(ListMode.FULL, list.size))
                mListMangas.value = list.toMutableList()
                mListMangasFull.value = list.toMutableList()
            } else {
                mListMangas.value = mutableListOf()
                mListMangasFull.value = mutableListOf()
                indexes.add(Pair(ListMode.FULL, 0))
            }
            //Receive value force refresh, not necessary notify
            change = false
        }

        setSuggestions(mListMangasFull.value)
        refreshComplete(change, indexes)
    }

    fun list(refreshComplete: (Boolean) -> (Unit)) {
        val list = mMangaRepository.list(mLibrary)
        if (list != null) {
            if (mListMangasFull.value == null || mListMangasFull.value!!.isEmpty()) {
                mListMangas.value = list.toMutableList()
                mListMangasFull.value = list.toMutableList()
                setSuggestions(mListMangasFull.value)
            } else
                update(list)
        } else {
            mListMangasFull.value = mutableListOf()
            mListMangas.value = mutableListOf()
            setSuggestions(mListMangasFull.value)
        }

        refreshComplete(mListMangas.value!!.isNotEmpty())
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

        if (isDesc)
            when (order) {
                Order.Date -> {
                    mListMangasFull.value!!.sortByDescending { it.dateCreate }
                    mListMangas.value!!.sortByDescending { it.dateCreate }
                }
                Order.LastAccess -> {
                    mListMangasFull.value!!.sortWith(compareBy<Manga> { it.lastAccess }.thenByDescending { it.name })
                    mListMangas.value!!.sortWith(compareBy<Manga> { it.lastAccess }.thenByDescending { it.name })
                }
                Order.Favorite -> {
                    mListMangasFull.value!!.sortWith(compareBy<Manga> { it.favorite }.thenByDescending { it.name })
                    mListMangas.value!!.sortWith(compareBy<Manga> { it.favorite }.thenByDescending { it.name })
                }
                else -> {
                    mListMangasFull.value!!.sortByDescending { it.name }
                    mListMangas.value!!.sortByDescending { it.name }
                }
            }
        else
            when (order) {
                Order.Date -> {
                    mListMangasFull.value!!.sortBy { it.dateCreate }
                    mListMangas.value!!.sortBy { it.dateCreate }
                }
                Order.LastAccess -> {
                    mListMangasFull.value!!.sortWith(compareByDescending<Manga> { it.lastAccess }.thenBy { it.name })
                    mListMangas.value!!.sortWith(compareByDescending<Manga> { it.lastAccess }.thenBy { it.name })
                }
                Order.Favorite -> {
                    mListMangasFull.value!!.sortWith(compareByDescending<Manga> { it.favorite }.thenBy { it.name })
                    mListMangas.value!!.sortWith(compareByDescending<Manga> { it.favorite }.thenBy { it.name })
                }
                else -> {
                    mListMangasFull.value!!.sortBy { it.name }
                    mListMangas.value!!.sortBy { it.name }
                }
            }
    }

    private fun setSuggestions(list : List<Manga>?) {
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

                    process.forEach {
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
                }
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
        newList.addAll(mListMangasFull.value!!.filter(Objects::nonNull))
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
            mWordFilter = constraint.toString()
            val filteredList: MutableList<Manga> = mutableListOf()

            if (constraint.isNullOrEmpty() && mTypeFilter.value == FilterType.None) {
                filteredList.addAll(mListMangasFull.value!!.filter(Objects::nonNull))
            } else {
                var filterPattern = constraint.toString()
                val filterCondition = arrayListOf<Pair<FilterType, String>>()
                constraint!!.contains('@').run {
                    val m = Pattern.compile("(@\\S*:([^\"]\\S*|\".+?\"\\s*))").matcher(constraint)
                    while (m.find()) {
                        val item = m.group(1)?.replace("\"", "") ?: continue
                        filterPattern = filterPattern.replace(m.group(1)!!, "", true)
                        val type = Util.stringToFilter(app.applicationContext, Type.MANGA, item.substringBefore(":").replace("@", ""))
                        if (type != FilterType.None) {
                            val condition = item.substringAfter(":")
                            if (condition.isNotEmpty())
                                filterCondition.add(Pair(type, condition))
                        }
                    }
                }

                filterPattern = filterPattern.lowercase(Locale.getDefault()).trim()
                filteredList.addAll(mListMangasFull.value!!.filter {
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
    fun processShareMarks(context: Context, processed: (result: ShareMarkType) -> (Unit)) {
        if (!mProcessShareMark) {
            mProcessShareMark = true
            val share = ShareMarkBase.getInstance(context)
            var notify = false
            val process: (manga: Manga) -> (Unit) = { item ->
                if (mLibrary.id == item.fkLibrary) {
                    notify = true
                    mListMangasFull.value?.find { manga -> manga.id == item.id }?.let { manga ->
                        manga.favorite = item.favorite
                        manga.bookMark = item.bookMark
                        manga.lastAccess = item.lastAccess
                    }
                }
            }
            share.mangaShareMark(process) {
                mProcessShareMark = false
                if ((it == ShareMarkType.SUCCESS || it == ShareMarkType.NOT_ALTERATION) && notify)
                    processed(ShareMarkType.NOTIFY_DATA_SET)
                else
                    processed(it)
            }
        }
    }

    var mImportingVocab = false
    fun importVocabulary() {
        if (mImportingVocab)
            return

        val list = mListMangasFull.value?.toList() ?: return
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

        CoroutineScope(newSingleThreadContext("VocabularyThread")).launch {
            async {
                try {
                    val size = list.size
                    for ((index, manga) in list.withIndex()) {
                        if (manga.lastVocabImport != null && manga.lastVocabImport!!.isAfter(LocalDateTime.now().minusDays(1)))
                            continue;

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
                                for (vocab in processed)
                                    withContext(Dispatchers.Main) {
                                        vocab.first.id = repository.save(vocab.first)
                                        vocab.first.id?.let { repository.insert(manga.id!!, it, vocab.second) }
                                    }

                                manga.lastVocabImport = LocalDateTime.now()
                                manga.fileAlteration = Date(manga.file.lastModified())

                                withContext(Dispatchers.Main) {
                                    repository.updateImport(manga)
                                }
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
                    mLOGGER.error("Error to import vocabulary", e)
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
}
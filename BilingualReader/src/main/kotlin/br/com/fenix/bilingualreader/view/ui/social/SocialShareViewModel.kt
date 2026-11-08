package br.com.fenix.bilingualreader.view.ui.social

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.HistoryStatistics
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.entity.SocialShareConfig
import br.com.fenix.bilingualreader.model.enums.SocialAspectRatio
import br.com.fenix.bilingualreader.model.enums.SocialColorTheme
import br.com.fenix.bilingualreader.model.enums.SocialLayoutTemplate
import br.com.fenix.bilingualreader.model.enums.SocialMediaTypeFilter
import br.com.fenix.bilingualreader.model.enums.SocialPeriodFilter
import br.com.fenix.bilingualreader.model.enums.SocialSortOrder
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.controller.SocialImageGenerator
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.HistoryRepository
import br.com.fenix.bilingualreader.service.repository.LibraryRepository
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

class SocialShareViewModel(application: Application) : AndroidViewModel(application) {

    private val mHistoryRepository = HistoryRepository(application)
    private val mMangaRepository = MangaRepository(application)
    private val mBookRepository = BookRepository(application)
    private val mLibrariesRepository = LibraryRepository(application)

    private val mConfig = MutableLiveData(SocialShareConfig())
    val config: LiveData<SocialShareConfig> = mConfig

    private val mAllReadings = MutableLiveData<List<HistoryStatistics>>(emptyList())
    val allReadings: LiveData<List<HistoryStatistics>> = mAllReadings

    private val mFilteredReadings = MutableLiveData<List<HistoryStatistics>>(emptyList())
    val filteredReadings: LiveData<List<HistoryStatistics>> = mFilteredReadings

    private val mPreviewBitmaps = MutableLiveData<List<Bitmap>>(emptyList())
    val previewBitmaps: LiveData<List<Bitmap>> = mPreviewBitmaps

    private val mIsRendering = MutableLiveData(false)
    val isRendering: LiveData<Boolean> = mIsRendering

    private val mLibraries = MutableLiveData<List<Library>>(emptyList())
    val libraries: LiveData<List<Library>> = mLibraries

    private var mRenderJob: Job? = null

    init {
        loadSavedPreferences()
        loadLibraries()
        reloadHistoryData()
    }

    private fun loadSavedPreferences() {
        val app = getApplication<Application>()
        val pref = GeneralConsts.getSharedPreferences(app)
        val handle = pref.getString(GeneralConsts.KEYS.SOCIAL_SHARE.USER_HANDLE, "") ?: ""
        val current = mConfig.value ?: SocialShareConfig()
        mConfig.value = current.copy(
            title = getDefaultTitle(app),
            subtitle = getDefaultSubtitle(),
            userHandle = handle
        )
    }

    fun getDefaultTitle(context: Context, date: LocalDate = LocalDate.now()): String {
        val months = context.resources.getStringArray(R.array.mouth_descriptions)
        val monthIndex = date.monthValue - 1
        val monthName = if (monthIndex in months.indices) months[monthIndex] else date.month.name
        return context.getString(R.string.social_share_default_title_month, monthName)
    }

    fun getDefaultSubtitle(date: LocalDate = LocalDate.now()): String {
        return date.year.toString()
    }

    fun saveUserHandle(handle: String) {
        val pref = GeneralConsts.getSharedPreferences(getApplication())
        pref.edit().putString(GeneralConsts.KEYS.SOCIAL_SHARE.USER_HANDLE, handle).apply()
        updateConfig { it.copy(userHandle = handle) }
    }

    private fun loadLibraries() {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val defaultManga = Library(
                GeneralConsts.KEYS.LIBRARY.DEFAULT_MANGA,
                app.getString(R.string.manga_library_default),
                "",
                type = Type.MANGA
            )
            val defaultBook = Library(
                GeneralConsts.KEYS.LIBRARY.DEFAULT_BOOK,
                app.getString(R.string.book_library_default),
                "",
                type = Type.BOOK
            )
            val dbLibraries = mLibrariesRepository.listEnabled()
            val list = listOf(defaultManga, defaultBook) + dbLibraries
            withContext(Dispatchers.Main) {
                mLibraries.value = list
            }
        }
    }

    fun updateConfig(modifier: (SocialShareConfig) -> SocialShareConfig) {
        val old = mConfig.value
        val updated = modifier(old ?: SocialShareConfig())
        if (old == updated) return
        mConfig.value = updated

        if (old?.periodFilter != updated.periodFilter ||
            old.startDate != updated.startDate ||
            old.endDate != updated.endDate ||
            old.mediaType != updated.mediaType ||
            old.libraryId != updated.libraryId ||
            old.onlyCompleted != updated.onlyCompleted
        ) {
            reloadHistoryData()
        } else {
            applyFilteringAndSort(updated)
            triggerPreviewRender()
        }
    }

    fun setPeriodPreset(preset: SocialPeriodFilter, specificMonth: Int? = null, specificYear: Int? = null) {
        val today = LocalDate.now()
        val (start, end) = when (preset) {
            SocialPeriodFilter.THIS_WEEK -> {
                val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                Pair(startOfWeek, today)
            }
            SocialPeriodFilter.THIS_MONTH -> {
                Pair(today.withDayOfMonth(1), today)
            }
            SocialPeriodFilter.SPECIFIC_MONTH -> {
                val m = specificMonth ?: today.monthValue
                val y = specificYear ?: today.year
                val startDate = LocalDate.of(y, m, 1)
                val endDate = startDate.with(TemporalAdjusters.lastDayOfMonth())
                Pair(startDate, endDate)
            }
            SocialPeriodFilter.THIS_YEAR -> {
                Pair(LocalDate.of(today.year, 1, 1), today)
            }
            SocialPeriodFilter.CUSTOM_RANGE -> {
                Pair(mConfig.value?.startDate ?: today.withDayOfMonth(1), mConfig.value?.endDate ?: today)
            }
        }

        val autoTitle = when (preset) {
            SocialPeriodFilter.THIS_WEEK -> getApplication<Application>().getString(R.string.date_format_this_week)
            SocialPeriodFilter.THIS_MONTH, SocialPeriodFilter.SPECIFIC_MONTH -> {
                GeneralConsts.formatDateSeparator(getApplication(), start.atStartOfDay())
            }
            SocialPeriodFilter.THIS_YEAR -> "${start.year}"
            else -> ""
        }

        updateConfig {
            it.copy(
                periodFilter = preset,
                startDate = start,
                endDate = end,
                title = if (it.title.isBlank() || it.title == autoTitle) autoTitle else it.title
            )
        }
    }

    fun toggleItemExclusion(itemKey: String, isIncluded: Boolean) {
        val currentExcluded = (mConfig.value?.excludedItemKeys ?: emptySet()).toMutableSet()
        if (isIncluded) {
            currentExcluded.remove(itemKey)
        } else {
            currentExcluded.add(itemKey)
        }
        updateConfig { it.copy(excludedItemKeys = currentExcluded) }
    }

    fun selectAllItems(selectAll: Boolean) {
        val items = mAllReadings.value ?: emptyList()
        val newExcluded = if (selectAll) {
            emptySet()
        } else {
            items.map { "${it.base.type}_${it.base.id}" }.toSet()
        }
        updateConfig { it.copy(excludedItemKeys = newExcluded) }
    }

    fun reloadHistoryData() {
        val config = mConfig.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val startDate = config.startDate
            val endDate = config.endDate

            val allHistoryLogs = mHistoryRepository.listHistory()
            val filteredLogs = allHistoryLogs.filter { log ->
                val startDay = log.start.toLocalDate()
                val endDay = log.getEnd().toLocalDate()
                (startDay in startDate..endDate) || (endDay in startDate..endDate)
            }

            val cachedManga = mutableMapOf<Long, Manga?>()
            val cachedBook = mutableMapOf<Long, Book?>()

            fun getManga(id: Long): Manga? = cachedManga.getOrPut(id) {
                mMangaRepository.get(id) ?: mMangaRepository.findById(id)
            }

            fun getBook(id: Long): Book? = cachedBook.getOrPut(id) {
                mBookRepository.get(id) ?: mBookRepository.findById(id)
            }

            // Group by type and reference
            val aggregatedMap = mutableMapOf<Pair<Type, Long>, HistoryStatistics>()

            for (log in filteredLogs) {
                val logLibId = if (log.fkLibrary == 0L) {
                    if (log.type == Type.MANGA) GeneralConsts.KEYS.LIBRARY.DEFAULT_MANGA else GeneralConsts.KEYS.LIBRARY.DEFAULT_BOOK
                } else {
                    log.fkLibrary
                }
                if (config.libraryId != null && logLibId != config.libraryId) continue

                if (config.mediaType == SocialMediaTypeFilter.MANGA && log.type != Type.MANGA) continue
                if (config.mediaType == SocialMediaTypeFilter.BOOK && log.type != Type.BOOK) continue

                val pages = if (log.getPageEnd() > log.pageStart) log.getPageEnd() - log.pageStart else 0
                val seconds = log.getSecondsRead()

                val key = Pair(log.type, log.fkReference)
                val existing = aggregatedMap[key]
                if (existing != null) {
                    val latestEnd = if (existing.lastAccess != null && log.getEnd().isAfter(existing.lastAccess)) log.getEnd() else existing.lastAccess
                    val updated = existing.copy(
                        timeRead = existing.timeRead + seconds,
                        pagesRead = existing.pagesRead + pages,
                        lastAccess = latestEnd
                    )
                    aggregatedMap[key] = updated
                } else {
                    val base = when (log.type) {
                        Type.MANGA -> getManga(log.fkReference)
                        Type.BOOK -> getBook(log.fkReference)
                    }
                    if (base != null) {
                        aggregatedMap[key] = HistoryStatistics(base, seconds, pages, base.sort, log.getEnd())
                    }
                }
            }

            // Also complement with items that have lastAccess in the period (even if logs were cleared/aggregated)
            if (config.mediaType == SocialMediaTypeFilter.ALL || config.mediaType == SocialMediaTypeFilter.MANGA) {
                val mangas = mMangaRepository.listHistory().orEmpty()
                for (manga in mangas) {
                    val mId = manga.id ?: continue
                    val libId = manga.fkLibrary ?: GeneralConsts.KEYS.LIBRARY.DEFAULT_MANGA
                    if (config.libraryId != null && libId != config.libraryId) continue

                    val accessDate = manga.lastAccess?.toLocalDate()
                    if (accessDate != null && accessDate in startDate..endDate) {
                        val key = Pair(Type.MANGA, mId)
                        if (!aggregatedMap.containsKey(key)) {
                            val pagesRead = if (manga.bookMark > 0) manga.bookMark else manga.pages
                            val timeSeconds = (manga.pages * 30L).coerceAtLeast(60L) // estimated time if no explicit logs
                            aggregatedMap[key] = HistoryStatistics(manga, timeSeconds, pagesRead, manga.sort, manga.lastAccess)
                        }
                    }
                }
            }

            if (config.mediaType == SocialMediaTypeFilter.ALL || config.mediaType == SocialMediaTypeFilter.BOOK) {
                val books = mBookRepository.listHistory().orEmpty()
                for (book in books) {
                    val bId = book.id ?: continue
                    val libId = book.fkLibrary ?: GeneralConsts.KEYS.LIBRARY.DEFAULT_BOOK
                    if (config.libraryId != null && libId != config.libraryId) continue

                    val accessDate = book.lastAccess?.toLocalDate()
                    if (accessDate != null && accessDate in startDate..endDate) {
                        val key = Pair(Type.BOOK, bId)
                        if (!aggregatedMap.containsKey(key)) {
                            val pagesRead = if (book.bookMark > 0) book.bookMark else book.pages
                            val timeSeconds = (book.pages * 60L).coerceAtLeast(60L) // estimated time if no explicit logs
                            aggregatedMap[key] = HistoryStatistics(book, timeSeconds, pagesRead, book.sort, book.lastAccess)
                        }
                    }
                }
            }

            var result = aggregatedMap.values.toList()

            if (config.onlyCompleted) {
                result = result.filter { it.base.completed || (it.base.bookMark >= it.base.pages && it.base.pages > 0) }
            }

            withContext(Dispatchers.Main) {
                mAllReadings.value = result
                applyFilteringAndSort(config)
                triggerPreviewRender()
            }
        }
    }

    private fun applyFilteringAndSort(config: SocialShareConfig) {
        val all = mAllReadings.value ?: emptyList()
        val excluded = config.excludedItemKeys

        val active = all.filter { !excluded.contains("${it.base.type}_${it.base.id}") }

        val sorted = when (config.sortOrder) {
            SocialSortOrder.TIME_READ -> active.sortedByDescending { it.timeRead }
            SocialSortOrder.PAGES_READ -> active.sortedByDescending { it.pagesRead }
            SocialSortOrder.RECENT -> active.sortedByDescending { it.lastAccess ?: LocalDateTime.MIN }
            SocialSortOrder.ALPHABETICAL -> active.sortedBy { it.base.title.lowercase() }
        }

        mFilteredReadings.value = sorted
    }

    private fun triggerPreviewRender() {
        mRenderJob?.cancel()
        mRenderJob = viewModelScope.launch {
            delay(250) // Debounce
            mIsRendering.value = true

            val currentConfig = mConfig.value ?: return@launch
            val items = mFilteredReadings.value ?: emptyList()

            val bitmaps = SocialImageGenerator.instance.generateImages(
                context = getApplication(),
                config = currentConfig,
                items = items,
                scale = 0.5f // Scaled preview for performance
            )

            mPreviewBitmaps.value = bitmaps
            mIsRendering.value = false
        }
    }

    suspend fun generateExportBitmaps(): List<Bitmap> {
        val currentConfig = mConfig.value ?: return emptyList()
        val items = mFilteredReadings.value ?: return emptyList()

        return SocialImageGenerator.instance.generateImages(
            context = getApplication(),
            config = currentConfig,
            items = items,
            scale = 1.0f // Full native resolution
        )
    }
}


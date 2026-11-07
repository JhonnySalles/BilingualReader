package br.com.fenix.bilingualreader.view.ui.social

import android.app.Application
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

    private val mPreviewBitmap = MutableLiveData<Bitmap?>()
    val previewBitmap: LiveData<Bitmap?> = mPreviewBitmap

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
        val pref = GeneralConsts.getSharedPreferences(getApplication())
        val handle = pref.getString(GeneralConsts.KEYS.SOCIAL_SHARE.USER_HANDLE, "") ?: ""
        val current = mConfig.value ?: SocialShareConfig()
        mConfig.value = current.copy(userHandle = handle)
    }

    fun saveUserHandle(handle: String) {
        val pref = GeneralConsts.getSharedPreferences(getApplication())
        pref.edit().putString(GeneralConsts.KEYS.SOCIAL_SHARE.USER_HANDLE, handle).apply()
        updateConfig { it.copy(userHandle = handle) }
    }

    private fun loadLibraries() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = mLibrariesRepository.listEnabled()
            withContext(Dispatchers.Main) {
                mLibraries.value = list
            }
        }
    }

    fun updateConfig(modifier: (SocialShareConfig) -> SocialShareConfig) {
        val updated = modifier(mConfig.value ?: SocialShareConfig())
        val old = mConfig.value
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
            val startDateTime = config.startDate.atStartOfDay()
            val endDateTime = config.endDate.atTime(23, 59, 59)

            val rawHistory = mHistoryRepository.findByPeriod(startDateTime, endDateTime)

            // Group by type and reference
            val aggregatedMap = mutableMapOf<Pair<Type, Long>, HistoryStatistics>()

            for (log in rawHistory) {
                if (config.libraryId != null && log.fkLibrary != config.libraryId) continue

                if (config.mediaType == SocialMediaTypeFilter.MANGA && log.type != Type.MANGA) continue
                if (config.mediaType == SocialMediaTypeFilter.BOOK && log.type != Type.BOOK) continue

                val pages = if (log.getPageEnd() > log.pageStart) log.getPageEnd() - log.pageStart else 0
                val seconds = log.getSecondsRead()

                val key = Pair(log.type, log.fkReference)
                val existing = aggregatedMap[key]
                if (existing != null) {
                    val updated = existing.copy(
                        timeRead = existing.timeRead + seconds,
                        pagesRead = existing.pagesRead + pages
                    )
                    aggregatedMap[key] = updated
                } else {
                    val base = when (log.type) {
                        Type.MANGA -> mMangaRepository.get(log.fkReference)
                        Type.BOOK -> mBookRepository.get(log.fkReference)
                    }
                    if (base != null) {
                        aggregatedMap[key] = HistoryStatistics(base, seconds, pages, base.sort, log.getEnd())
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

            val bitmap = SocialImageGenerator.instance.generateImage(
                context = getApplication(),
                config = currentConfig,
                items = items,
                scale = 0.5f // Scaled preview for performance
            )

            mPreviewBitmap.value = bitmap
            mIsRendering.value = false
        }
    }

    suspend fun generateExportBitmap(): Bitmap? {
        val currentConfig = mConfig.value ?: return null
        val items = mFilteredReadings.value ?: return null

        return SocialImageGenerator.instance.generateImage(
            context = getApplication(),
            config = currentConfig,
            items = items,
            scale = 1.0f // Full native resolution
        )
    }
}


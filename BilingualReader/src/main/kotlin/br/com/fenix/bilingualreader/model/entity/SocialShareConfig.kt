package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.model.enums.SocialAspectRatio
import br.com.fenix.bilingualreader.model.enums.SocialColorTheme
import br.com.fenix.bilingualreader.model.enums.SocialLayoutTemplate
import br.com.fenix.bilingualreader.model.enums.SocialMediaTypeFilter
import br.com.fenix.bilingualreader.model.enums.SocialPeriodFilter
import br.com.fenix.bilingualreader.model.enums.SocialSortOrder
import java.time.LocalDate

data class SocialShareConfig(
    val periodFilter: SocialPeriodFilter = SocialPeriodFilter.THIS_MONTH,
    val startDate: LocalDate = LocalDate.now().withDayOfMonth(1),
    val endDate: LocalDate = LocalDate.now(),
    val mediaType: SocialMediaTypeFilter = SocialMediaTypeFilter.ALL,
    val libraryId: Long? = null,
    val onlyCompleted: Boolean = false,

    val aspectRatio: SocialAspectRatio = SocialAspectRatio.POST_4_5,
    val template: SocialLayoutTemplate = SocialLayoutTemplate.BENTO_GRID,
    val colorTheme: SocialColorTheme = SocialColorTheme.DYNAMIC_AMBIENT,

    val title: String = "",
    val subtitle: String = "",
    val userHandle: String = "",

    val maxCovers: Int = 9,
    val sortOrder: SocialSortOrder = SocialSortOrder.TIME_READ,

    val showReadingTime: Boolean = true,
    val showPagesRead: Boolean = true,
    val showTitles: Boolean = true,
    val showProgressStatus: Boolean = true,

    val excludedItemKeys: Set<String> = emptySet()
)


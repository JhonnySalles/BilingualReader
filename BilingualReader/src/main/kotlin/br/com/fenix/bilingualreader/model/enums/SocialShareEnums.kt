package br.com.fenix.bilingualreader.model.enums

import br.com.fenix.bilingualreader.R

enum class SocialAspectRatio(
    val width: Int,
    val height: Int,
    val labelRes: Int,
    val description: String
) {
    POST_4_5(1080, 1350, R.string.social_share_aspect_4_5, "4:5"),
    STORY_9_16(1080, 1920, R.string.social_share_aspect_9_16, "9:16"),
    SQUARE_1_1(1080, 1080, R.string.social_share_aspect_1_1, "1:1"),
    TWITTER_16_9(1600, 900, R.string.social_share_aspect_16_9, "16:9"),
    TUMBLR_2_3(1200, 1800, R.string.social_share_aspect_2_3, "2:3");

    val aspectRatio: Float
        get() = width.toFloat() / height.toFloat()
}

enum class SocialLayoutTemplate(val labelRes: Int) {
    BENTO_GRID(R.string.social_share_template_bento),
    PERSPECTIVE_SHELF(R.string.social_share_template_shelf),
    WRAPPED_STATS(R.string.social_share_template_wrapped),
    MINIMALIST_LIST(R.string.social_share_template_list)
}

enum class SocialColorTheme(val labelRes: Int) {
    DYNAMIC_AMBIENT(R.string.social_share_theme_dynamic),
    APP_THEME(R.string.social_share_theme_app),
    DARK_SLATE(R.string.social_share_theme_dark_slate),
    OLED_BLACK(R.string.social_share_theme_oled),
    WARM_PAPER(R.string.social_share_theme_warm_paper),
    MANGA_POP(R.string.social_share_theme_manga_pop)
}

enum class SocialPeriodFilter(val labelRes: Int) {
    THIS_WEEK(R.string.social_share_period_week),
    THIS_MONTH(R.string.social_share_period_month),
    SPECIFIC_MONTH(R.string.social_share_period_specific_month),
    THIS_YEAR(R.string.social_share_period_year),
    CUSTOM_RANGE(R.string.social_share_period_custom)
}

enum class SocialSortOrder(val labelRes: Int) {
    TIME_READ(R.string.social_share_sort_time),
    PAGES_READ(R.string.social_share_sort_pages),
    RECENT(R.string.social_share_sort_recent),
    ALPHABETICAL(R.string.social_share_sort_alpha)
}

enum class SocialMediaTypeFilter(val labelRes: Int) {
    ALL(R.string.social_share_media_all),
    MANGA(R.string.social_share_media_manga),
    BOOK(R.string.social_share_media_book)
}


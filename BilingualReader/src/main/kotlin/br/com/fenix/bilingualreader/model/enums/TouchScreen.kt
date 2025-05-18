package br.com.fenix.bilingualreader.model.enums

import br.com.fenix.bilingualreader.R

enum class TouchScreen(private val value: Int) {
    TOUCH_NOT_IMPLEMENTED(0),
    TOUCH_NOT_ASSIGNED(R.string.reading_touch_screen_not_assigned),
    TOUCH_ASPECT_FIT(R.string.reading_touch_screen_aspect_fit),
    TOUCH_FIT_WIDTH(R.string.reading_touch_screen_fit_width),
    TOUCH_CHAPTER_LIST(R.string.reading_touch_screen_chapters),
    TOUCH_NEXT_FILE(R.string.reading_touch_screen_next_file),
    TOUCH_PREVIOUS_FILE(R.string.reading_touch_screen_previous_file),
    TOUCH_NEXT_PAGE(R.string.reading_touch_screen_next_page),
    TOUCH_PREVIOUS_PAGE(R.string.reading_touch_screen_previous_page),
    TOUCH_SHARE_IMAGE(R.string.reading_touch_screen_share_image),
    TOUCH_PAGE_MARK(R.string.reading_touch_screen_page_mark);

    open fun getValue() : Int = this.value
}
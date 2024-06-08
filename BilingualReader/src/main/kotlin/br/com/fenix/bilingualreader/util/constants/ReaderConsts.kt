package br.com.fenix.bilingualreader.util.constants

class ReaderConsts {

    object READER {
        const val MAX_PAGE_HEIGHT = 2400
        const val MAX_PAGE_WIDTH = 3000
        const val MANGA_OFF_SCREEN_PAGE_LIMIT = 6
        const val BOOK_OFF_SCREEN_PAGE_LIMIT = 6

        // DEFAULT FUNCTIONS IMPLEMENTED
        const val BOOK_WEB_VIEW_MODE = false
        const val BOOK_NATIVE_POPUP_MENU_SELECT = false
    }

    object COVER {
        const val MANGA_COVER_THUMBNAIL_HEIGHT = 300
        const val MANGA_COVER_THUMBNAIL_WIDTH = 200
        const val BOOK_COVER_THUMBNAIL_WIDTH = 300
        const val BOOK_COVER_READER_WIDTH = 1080
    }

    object PAGE {
        const val PAGE_CHAPTER_LIST_HEIGHT = 150
        const val PAGE_CHAPTER_LIST_WIDTH = 100
    }

    object PAGESLINK {
        const val IMAGES_HEIGHT = 300
        const val IMAGES_WIDTH = 200
    }

    object STATES {
        const val STATE_FULLSCREEN = "STATE_FULLSCREEN"
        const val STATE_NEW_COMIC = "STATE_NEW_COMIC"
        const val STATE_NEW_COMIC_TITLE = "STATE_NEW_COMIC_TITLE"
        const val STATE_NEW_BOOK = "STATE_NEW_BOOK"
        const val STATE_NEW_BOOK_TITLE = "STATE_NEW_BOOK_TITLE"
    }

    object TOKENIZER {
        object SUDACHI {
            val DICTIONARY_NAME = "sudachi_smalldict.json"
        }
    }

}
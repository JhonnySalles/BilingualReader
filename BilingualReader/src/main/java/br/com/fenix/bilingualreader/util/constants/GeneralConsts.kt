package br.com.fenix.bilingualreader.util.constants

import android.annotation.TargetApi
import android.content.Context
import android.content.SharedPreferences
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.util.constants.GeneralConsts.PATTERNS.DATE_PATTERN
import br.com.fenix.bilingualreader.util.constants.GeneralConsts.PATTERNS.DATE_PATTERN_SMALL
import br.com.fenix.bilingualreader.util.constants.GeneralConsts.PATTERNS.TIME_PATTERN
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class GeneralConsts private constructor() {
    companion object {
        fun getCacheDir(context: Context): File? {
            return context.externalCacheDir
        }

        fun getSharedPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences(KEYS.PREFERENCE_NAME, Context.MODE_PRIVATE)
        }

        fun formatterDate(context: Context, dateTime: Date?, isSmall: Boolean = false): String {
            if (dateTime == null)
                return context.getString(R.string.date_format_unknown)

            val preferences = getSharedPreferences(context)
            val format = if (isSmall) DATE_PATTERN_SMALL else DATE_PATTERN
            val key = if (isSmall) KEYS.SYSTEM.FORMAT_DATA_SMALL else KEYS.SYSTEM.FORMAT_DATA
            val pattern = preferences.getString(key, format)
            return SimpleDateFormat(pattern, Locale.getDefault()).format(dateTime)
        }

        fun formatterDateTime(context: Context, dateTime: Date): String {
            val preferences = getSharedPreferences(context)
            val pattern = preferences.getString(KEYS.SYSTEM.FORMAT_DATA, DATE_PATTERN) + TIME_PATTERN
            return SimpleDateFormat(pattern, Locale.getDefault()).format(dateTime)
        }

        @TargetApi(26)
        fun formatterDate(context: Context, dateTime: LocalDateTime, isSmall: Boolean = false): String {
            val preferences = getSharedPreferences(context)
            val format = if (isSmall) DATE_PATTERN_SMALL else DATE_PATTERN
            val key = if (isSmall) KEYS.SYSTEM.FORMAT_DATA_SMALL else KEYS.SYSTEM.FORMAT_DATA
            val pattern = preferences.getString(key, format)
            return dateTime.format(DateTimeFormatter.ofPattern(pattern))
        }

        @TargetApi(26)
        fun formatterDateTime(context: Context, dateTime: LocalDateTime): String {
            val preferences = getSharedPreferences(context)
            val pattern = preferences.getString(KEYS.SYSTEM.FORMAT_DATA, DATE_PATTERN) + " " + TIME_PATTERN
            return dateTime.format(DateTimeFormatter.ofPattern(pattern))
        }
    }

    object PATTERNS {
        const val DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss"
        const val BACKUP_DATE_PATTERN = "yyyy-MM-dd_HH-mm-ss"
        const val DATE_PATTERN = "yyyy-MM-dd"
        const val DATE_PATTERN_SMALL = "yy-MM-dd"
        const val TIME_PATTERN = "hh:mm:ss a"
    }

    object KEYS {
        const val PREFERENCE_NAME = "SHARED_PREFS"

        object LIBRARY {
            const val DEFAULT = -1L
            const val ORIENTATION = "LAST_ORIENTATION"

            const val MANGA_FOLDER = "MANGA_LIBRARY_FOLDER"
            const val MANGA_ORDER = "MANGA_LIBRARY_ORDER"
            const val MANGA_LIBRARY_TYPE = "MANGA_LAST_LIBRARY_TYPE"

            const val BOOK_FOLDER = "BOOK_FOLDER"
            const val BOOK_ORDER = "BOOK_LIBRARY_ORDER"
            const val BOOK_LIBRARY_TYPE = "BOOK_LAST_LIBRARY_TYPE"

            const val LAST_LIBRARY = "LAST_LIBRARY"
            const val LIBRARY_TYPE = "LIBRARY_TYPE"
        }

        object LIBRARIES {
            const val MANGA_INDEX_LIBRARIES = 1000
            const val BOOK_INDEX_LIBRARIES = 2000
        }

        object SUBTITLE {
            const val FOLDER = "SUBTITLE_FOLDER"
            const val LANGUAGE = "SUBTITLE_LANGUAGE"
            const val TRANSLATE = "SUBTITLE_TRANSLATE"
        }

        object READER {
            const val MANGA_READER_MODE = "MANGA_READER_MODE"
            const val MANGA_PAGE_MODE = "MANGA_READER_PAGE_MODE"
            const val MANGA_SHOW_CLOCK_AND_BATTERY = "MANGA_SHOW_CLOCK_AND_BATTERY"
            const val MANGA_USE_MAGNIFIER_TYPE = "MANGA_USE_MAGNIFIER_TYPE"
        }

        object SYSTEM {
            const val LANGUAGE = "SYSTEM_LANGUAGE"
            const val FORMAT_DATA = "SYSTEM_FORMAT_DATA"
            const val FORMAT_DATA_SMALL = "FORMAT_DATA_SMALL"
        }

        object MANGA {
            const val ID = "MANGA_ID"
            const val NAME = "MANGA_NAME"
            const val MARK = "MANGA_MARK"
            const val PAGE_NUMBER = "PAGE_NUMBER"
        }

        object BOOK {
            const val ID = "BOOK_ID"
            const val NAME = "BOOK_NAME"
            const val MARK = "BOOK_MARK"
            const val PAGE_NUMBER = "PAGE_NUMBER"
        }

        object VOCABULARY {
            const val TEXT = "VOCABULARY_TEXT"
            const val TYPE = "VOCABULARY_TYPE"
        }

        object OBJECT {
            const val MANGA = "MANGA_OBJECT"
            const val FILE = "FILE_OBJECT"
            const val PAGE_LINK = "PAGE_LINK"
            const val LIBRARY = "LIBRARY"
            const val BOOK = "BOOK_OBJECT"
        }

        object COLOR_FILTER {
            const val CUSTOM_FILTER = "CUSTOM_FILTER"
            const val GRAY_SCALE = "GRAY_SCALE"
            const val INVERT_COLOR = "INVERT_COLOR"
            const val COLOR_RED = "COLOR_RED"
            const val COLOR_BLUE = "COLOR_BLUE"
            const val COLOR_GREEN = "COLOR_GREEN"
            const val COLOR_ALPHA = "COLOR_ALPHA"
            const val SEPIA = "SEPIA"
            const val BLUE_LIGHT = "BLUE_LIGHT"
            const val BLUE_LIGHT_ALPHA = "BLUE_LIGHT_ALPHA"
        }

        object PAGE_LINK {
            const val USE_IN_SEARCH_TRANSLATE = "USE_PAGE_LINK_IN_SEARCH_TRANSLATE"
            const val USE_DUAL_PAGE_CALCULATE = "USE_DUAL_PAGE_CALCULATE"
            const val USE_PAGE_PATH_FOR_LINKED = "USE_PAGE_PATH_FOR_LINKED"
        }

        object MONITORING {
            const val MY_ANIME_LIST = "MONITORING_MY_ANIME_LIST"
        }

        object DATABASE {
            const val LAST_BACKUP = "LAST_BACKUP"
            const val BACKUP_RESTORE_ROLLBACK_FILE_NAME = "BilingualReaderBackup.db"
            const val RESTORE_DATABASE = "RESTORE_DATABASE"
        }

        object FRAGMENT {
            const val ID = "FRAGMENT_ID"
        }

        object THEME {
            const val THEME_USED = "THEME_USED"
            const val THEME_MODE = "THEME_MODE"
            const val THEME_CHANGE = "THEME_CHANGE"
        }
    }

    object TAG {
        const val LOG = "MangaReader"
        const val STACKTRACE = "[STACKTRACE] "

        object DATABASE {
            const val INSERT = "$LOG - [DATABASE] INSERT"
            const val SELECT = "$LOG - [DATABASE] SELECT"
            const val DELETE = "$LOG - [DATABASE] DELETE"
            const val LIST = "$LOG - [DATABASE] LIST"
        }
    }

    object CONFIG {
        val DATA_FORMAT = listOf("dd/MM/yyyy", "MM/dd/yy", "dd/MM/yy", "yyyy-MM-dd")
        val DATA_FORMAT_SMALL = listOf("dd/MM/yy", "MM/dd/yy", "dd/MM/yy", "yy-MM-dd")
    }

    object CACHE_FOLDER {
        const val TESSERACT = "tesseract"
        const val RAR = "RarTemp"
        const val MANGA_COVERS = "MangaCovers"
        const val BOOK_COVERS = "BookCovers"
        const val LINKED = "Linked"
        const val IMAGE = "Image"
        const val THREAD = "thread"
        const val A = "a"
        const val B = "b"
        const val C = "c"
        const val D = "d"
    }

    object SCANNER {
        const val POSITION = "POSITION"
        const val MESSAGE_MANGA_UPDATE_FINISHED = 0
        const val MESSAGE_MANGA_UPDATED_ADD = 1
        const val MESSAGE_MANGA_UPDATED_REMOVE = 2

        const val MESSAGE_BOOK_UPDATE_FINISHED = 6
        const val MESSAGE_BOOK_UPDATED_ADD = 7
        const val MESSAGE_BOOK_UPDATED_REMOVE = 8
    }

    object FILE_LINK {
        const val FOLDER_MANGA = "manga"
        const val FOLDER_LINK = "link"
    }

    object LINKS {
        const val TATOEBA = "https://tatoeba.org/pt-br/sentences/search?query="
    }

    object REQUEST {
        const val PERMISSION_DRAW_OVERLAYS = 505
        const val PERMISSION_DRAW_OVERLAYS_FLOATING_OCR = 506
        const val PERMISSION_DRAW_OVERLAYS_FLOATING_SUBTITLE = 507
        const val PERMISSION_DRAW_OVERLAYS_FLOATING_BUTTONS = 508
        const val PERMISSION_WRITE_BACKUP = 509
        const val OPEN_JSON = 205
        const val OPEN_PAGE_LINK = 206
        const val OPEN_MANGA_FOLDER = 105
        const val OPEN_BOOK_FOLDER = 106
        const val PERMISSION_FILES_ACCESS = 101
        const val GENERATE_BACKUP = 500
        const val RESTORE_BACKUP = 501
        const val CONFIG_LIBRARIES = 600
        const val SELECT_MANGA = 601
        const val MANGA_DETAIL = 602
        const val BOOK_DETAIL = 603
    }

}
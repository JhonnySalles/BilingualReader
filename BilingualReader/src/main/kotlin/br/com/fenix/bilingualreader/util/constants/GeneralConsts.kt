package br.com.fenix.bilingualreader.util.constants

import android.annotation.TargetApi
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.util.constants.GeneralConsts.PATTERNS.DATE_PATTERN
import br.com.fenix.bilingualreader.util.constants.GeneralConsts.PATTERNS.DATE_PATTERN_SMALL
import br.com.fenix.bilingualreader.util.constants.GeneralConsts.PATTERNS.TIME_PATTERN
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class GeneralConsts private constructor() {
    companion object {
        fun getCoverDir(context: Context): File? {
            val caches = context.externalCacheDirs
            return caches.last()
        }

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

        @TargetApi(Build.VERSION_CODES.O)
        fun formatterDate(context: Context, date: Long): String {
            val preferences = getSharedPreferences(context)
            val pattern = preferences.getString(KEYS.SYSTEM.FORMAT_DATA, DATE_PATTERN)
            val dateTime = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).toLocalDate()
            return dateTime.format(DateTimeFormatter.ofPattern(pattern))
        }

        @TargetApi(26)
        fun dateTimeToDate(dateTime: LocalDateTime): Date {
            return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant())
        }

        @TargetApi(26)
        fun dateToDateTime(dateTime: Date): LocalDateTime {
            return LocalDateTime.ofInstant(dateTime.toInstant(), ZoneId.systemDefault())
        }

        @TargetApi(26)
        fun formatterDate(context: Context, dateTime: LocalDate): String {
            val preferences = getSharedPreferences(context)
            val pattern = preferences.getString(KEYS.SYSTEM.FORMAT_DATA, DATE_PATTERN)
            return dateTime.format(DateTimeFormatter.ofPattern(pattern))
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

        @TargetApi(26)
        fun formatCountDays(context: Context, dateTime: LocalDateTime?): String {
            val today = LocalDateTime.of(LocalDate.now(), LocalTime.of(0, 0, 0))
            return if (dateTime == null)
                ""
            else if (dateTime.isAfter(today))
                context.getString(R.string.date_format_today)
            else if (dateTime.isAfter(today.minusDays(1)))
                context.getString(R.string.date_format_yesterday)
            else if (dateTime.isAfter(today.minusDays(7)))
                context.getString(
                    R.string.date_format_day_ago,
                    ChronoUnit.DAYS.between(dateTime, today).toString()
                )
            else
                formatterDate(context, dateTime)
        }

        fun formatCountDays(context: Context, dateTime: Date?): String {
            return if (dateTime == null)
                ""
            else {
                val today = Calendar.getInstance()
                today.add(Calendar.DAY_OF_YEAR, -1)
                val seven = Calendar.getInstance()
                seven.add(Calendar.DAY_OF_YEAR, -7)

                if (today.after(dateTime))
                    context.getString(R.string.date_format_today)
                else if (seven.after(dateTime)) {
                    val date = Calendar.getInstance()
                    date.time = dateTime
                    val days = TimeUnit.MILLISECONDS.toDays(Calendar.getInstance().timeInMillis - date.timeInMillis)
                    context.getString(
                        R.string.date_format_day_ago,
                        days.toString()
                    )
                } else
                    formatterDate(
                        context,
                        dateTime
                    )
            }
        }
    }

    object DEFAULTS {
        const val DEFAULT_HANDLE_SEARCH_FILTER = 500L
    }

    object PATTERNS {
        const val DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss"
        const val BACKUP_DATE_PATTERN = "yyyy-MM-dd_HH-mm-ss"
        const val DATE_PATTERN = "yyyy-MM-dd"
        const val DATE_PATTERN_SMALL = "yy-MM-dd"
        const val TIME_PATTERN = "hh:mm:ss a"
        const val FULL_DATE_TIME = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
    }

    object KEYS {
        const val PREFERENCE_NAME = "SHARED_PREFS"

        object LIBRARY {
            const val DEFAULT_MANGA = -1L
            const val DEFAULT_BOOK = -2L
            const val ORIENTATION = "LAST_ORIENTATION"

            const val MANGA_ORDER = "MANGA_LIBRARY_ORDER"
            const val MANGA_LIBRARY_TYPE = "MANGA_LAST_LIBRARY_TYPE"

            const val BOOK_ORDER = "BOOK_LIBRARY_ORDER"
            const val BOOK_LIBRARY_TYPE = "BOOK_LAST_LIBRARY_TYPE"

            const val LAST_LIBRARY = "LAST_LIBRARY"
            const val LIBRARY_TYPE = "LIBRARY_TYPE"
            const val LIBRARY_ID = "LIBRARY_ID"
            const val LIBRARY_ARRAY_ID = "LIBRARY_ARRAY_ID"

            const val CLEAR_LIBRARY_LIST = "CLEAR_LIBRARY_LIST"
        }

        object LIBRARIES {
            const val MANGA_INDEX_LIBRARIES = 1000
            const val BOOK_INDEX_LIBRARIES = 2000
            const val NOTIFICATION_SOLICITED = "NOTIFICATION_SOLICITED"
        }

        object SUBTITLE {
            const val FOLDER = "SUBTITLE_FOLDER"
            const val LANGUAGE = "SUBTITLE_LANGUAGE"
            const val TRANSLATE = "SUBTITLE_TRANSLATE"
        }

        object READER {
            const val MANGA_READER_MODE = "MANGA_READER_MODE"
            const val MANGA_PAGE_SCROLLING_MODE = "MANGA_PAGE_SCROLLING_MODE"
            const val MANGA_SHOW_CLOCK_AND_BATTERY = "MANGA_SHOW_CLOCK_AND_BATTERY"
            const val MANGA_USE_MAGNIFIER_TYPE = "MANGA_USE_MAGNIFIER_TYPE"
            const val MANGA_KEEP_ZOOM_BETWEEN_PAGES = "MANGA_KEEP_ZOOM_BETWEEN_PAGES"

            const val BOOK_PAGE_ALIGNMENT = "BOOK_PAGE_ALIGNMENT"
            const val BOOK_PAGE_MARGIN = "BOOK_PAGE_MARGIN"
            const val BOOK_PAGE_SPACING = "BOOK_PAGE_SPACING"
            const val BOOK_PAGE_SCROLLING_MODE = "BOOK_PAGE_SCROLLING_MODE"
            const val BOOK_PAGE_FONT_TYPE_NORMAL = "BOOK_PAGE_FONT_TYPE_NORMAL"
            const val BOOK_PAGE_FONT_TYPE_JAPANESE = "BOOK_PAGE_FONT_TYPE_JAPANESE"
            const val BOOK_PAGE_FONT_SIZE = "BOOK_PAGE_FONT_SIZE"
            const val BOOK_PAGE_FONT_SIZE_DEFAULT = 12f
            const val BOOK_PROCESS_JAPANESE_TEXT = "BOOK_PROCESS_JAPANESE_TEXT"
            const val BOOK_GENERATE_FURIGANA_ON_TEXT = "BOOK_GENERATE_FURIGANA_ON_TEXT"
            const val BOOK_FONT_JAPANESE_STYLE = "BOOK_FONT_JAPANESE_STYLE"
            const val BOOK_PROCESS_VOCABULARY = "BOOK_PROCESS_VOCABULARY"
            const val BOOK_READER_TTS_VOICE_NORMAL = "BOOK_READER_TTS_VOICE_NORMAL"
            const val BOOK_READER_TTS_VOICE_JAPANESE = "BOOK_READER_TTS_VOICE_JAPANESE"
            const val BOOK_READER_TTS_SPEED = "BOOK_READER_TTS_SPEED"
            const val BOOK_READER_TTS_SPEED_DEFAULT = 0f
        }

        object TOUCH {
            const val MANGA_TOUCH_DEMONSTRATION = "MANGA_TOUCH_DEMONSTRATION"
            const val BOOK_TOUCH_DEMONSTRATION = "BOOK_TOUCH_DEMONSTRATION"

            const val MANGA_TOP = "MANGA_TOUCH_TOP"
            const val MANGA_TOP_RIGHT = "MANGA_TOUCH_TOP_RIGHT"
            const val MANGA_TOP_LEFT = "MANGA_TOUCH_TOP_LEFT"
            const val MANGA_RIGHT = "MANGA_TOUCH_RIGHT"
            const val MANGA_LEFT = "MANGA_TOUCH_LEFT"
            const val MANGA_BOTTOM = "MANGA_TOUCH_BOTTOM"
            const val MANGA_BOTTOM_RIGHT = "MANGA_TOUCH_BOTTOM_RIGHT"
            const val MANGA_BOTTOM_LEFT = "MANGA_TOUCH_BOTTOM_LEFT"

            const val BOOK_TOP = "BOOK_TOUCH_TOP"
            const val BOOK_TOP_RIGHT = "BOOK_TOUCH_TOP_RIGHT"
            const val BOOK_TOP_LEFT = "BOOK_TOUCH_TOP_LEFT"
            const val BOOK_RIGHT = "BOOK_TOUCH_RIGHT"
            const val BOOK_LEFT = "BOOK_TOUCH_LEFT"
            const val BOOK_BOTTOM = "BOOK_TOUCH_BOTTOM"
            const val BOOK_BOTTOM_RIGHT = "BOOK_TOUCH_BOTTOM_RIGHT"
            const val BOOK_BOTTOM_LEFT = "BOOK_TOUCH_BOTTOM_LEFT"
        }

        object SYSTEM {
            const val LANGUAGE = "SYSTEM_LANGUAGE"
            const val FORMAT_DATA = "SYSTEM_FORMAT_DATA"
            const val FORMAT_DATA_SMALL = "FORMAT_DATA_SMALL"
            const val SHARE_MARK_ENABLED = "SHARE_MARK_ENABLED"
            const val SHARE_MARK_CLOUD = "SHARE_MARK_CLOUD"
        }

        object MANGA {
            const val ID = "MANGA_ID"
            const val NAME = "MANGA_NAME"
            const val TITLE = "MANGA_TITLE"
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

        object CHAPTERS {
            const val TITLE = "CHAPTERS_TITLE"
            const val NUMBER = "CHAPTERS_NUMBER"
            const val PAGE = "CHAPTERS_PAGE"
        }

        object OBJECT {
            const val MANGA = "MANGA_OBJECT"
            const val FILE = "FILE_OBJECT"
            const val PAGE_LINK = "PAGE_LINK"
            const val LIBRARY = "LIBRARY"
            const val BOOK = "BOOK_OBJECT"
            const val TYPE = "TYPE_OBJECT"
            const val DOCUMENT_PATH = "DOCUMENT_PATH"
            const val DOCUMENT_FONT_SIZE = "DOCUMENT_FONT_SIZE"
            const val DOCUMENT_PASSWORD = "DOCUMENT_PASSWORD"
            const val DOCUMENT_JAPANESE_STYLE = "DOCUMENT_JAPANESE_STYLE"
            const val BOOK_FONT_SIZE = "BOOK_FONT_SIZE"
            const val BOOK_ANNOTATION = "BOOK_ANNOTATION_OBJECT"
            const val BOOK_SEARCH = "BOOK_SEARCH_OBJECT"
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

        object DATABASE {
            const val LAST_BACKUP = "LAST_BACKUP"
            const val BACKUP_RESTORE_ROLLBACK_FILE_NAME = "BilingualReaderBackup.db"
            const val RESTORE_DATABASE = "RESTORE_DATABASE"
            const val LAST_AUTO_BACKUP = "LAST_AUTO_BACKUP"
        }

        object FRAGMENT {
            const val ID = "FRAGMENT_ID"
        }

        object THEME {
            const val THEME_USED = "THEME_USED"
            const val THEME_MODE = "THEME_MODE"
            const val THEME_CHANGE = "THEME_CHANGE"
        }

        object SHARE_MARKS {
            const val LAST_SYNC_MANGA = "SHARE_MARKS_LAST_SYNC_MANGA"
            const val LAST_SYNC_BOOK = "SHARE_MARKS_LAST_SYNC_BOOK"
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
        val DATA_FORMAT = listOf("dd/MM/yyyy", "MM/dd/yyyy", "dd/MM/yy", "yyyy-MM-dd", "yy-MM-dd")
        val DATA_FORMAT_SMALL = listOf("dd/MM/yy", "MM/dd/yy", "yy-MM-dd")
    }

    object CACHE_FOLDER {
        const val TESSERACT = "Tesseract"
        const val RAR = "RarTemp"
        const val MANGA_COVERS = "MangaCovers"
        const val BOOK_COVERS = "BookCovers"
        const val BOOKS = "Books"
        const val LINKED = "Linked"
        const val IMAGE = "Image"
        const val AUDIO = "Audio"
        const val THREAD = "Thread"
        const val A = "a"
        const val B = "b"
        const val C = "c"
        const val D = "d"
        const val E = "e"
        const val F = "f"
        const val G = "g"
    }

    object SCANNER {
        const val POSITION = "POSITION"
        const val MESSAGE_MANGA_UPDATE_FINISHED = 0
        const val MESSAGE_MANGA_UPDATED_ADD = 1
        const val MESSAGE_MANGA_UPDATED_REMOVE = 2
        const val MESSAGE_MANGA_UPDATED_CHANGE = 3

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
        const val PERMISSION_WRITE_BACKUP = 509
        const val PERMISSION_NOTIFICATIONS = 510
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
        const val BOOK_DETAIL = 610
        const val BOOK_SEARCH = 611
        const val BOOK_ANNOTATION = 612
        const val CHAPTERS = 620
        const val TOUCH_CONFIGURATION = 630
        const val GOOGLE_SIGN_IN = 700
        const val DRIVE_AUTHORIZATION = 701
    }

    object SHARE_MARKS {
        val MIN_DATE_TIME: LocalDateTime = LocalDateTime.of(2000, 1, 1, 0, 0)

        const val PARSE_DATE_TIME = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
        const val DATE_TIME = "yyyyMMdd HHmmss"
        const val FILE_EXTENSION = ".json"

        const val FOLDER = "BilingualReader"
        const val MANGA_FILE = "MangaMarks"
        const val BOOK_FILE = "BookMarks"

        const val MANGA_FILE_WITH_EXTENSION = MANGA_FILE + FILE_EXTENSION
        const val BOOK_FILE_WITH_EXTENSION = BOOK_FILE + FILE_EXTENSION
    }

}
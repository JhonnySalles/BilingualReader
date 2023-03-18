package br.com.fenix.bilingualreader.util.constants

class DataBaseConsts private constructor() {
    object MANGA {
        const val TABLE_NAME = "Manga"

        object COLUMNS {
            const val ID = "id"
            const val TITLE = "title"
            const val PAGES = "pages"
            const val CHAPTERS = "chapters"
            const val BOOK_MARK = "book_mark"
            const val FILE_PATH = "path"
            const val FILE_NAME = "name"
            const val FILE_TYPE = "type"
            const val FILE_FOLDER = "folder"
            const val FILE_SIZE = "size"
            const val FAVORITE = "favorite"
            const val HAS_SUBTITLE = "has_subtitle"
            const val AUTHOR = "author"
            const val SERIES = "series"
            const val PUBLISHER = "publisher"
            const val RELEASE = "release"
            const val VOLUME = "volume"
            const val DATE_CREATE = "date_create"
            const val LAST_ACCESS = "last_access"
            const val EXCLUDED = "excluded"
            const val FK_ID_LIBRARY = "id_library"
            const val LAST_ALTERATION = "last_alteration"
            const val FILE_ALTERATION = "file_alteration"
            const val LAST_VOCABULARY_IMPORT = "last_vocabulary_import"
            const val LAST_VERIFY = "last_verify"
            const val SORT = "sort"
        }
    }

    object COVER {
        const val TABLE_NAME = "Covers"

        object COLUMNS {
            const val ID = "id"
            const val FK_ID_MANGA = "id_manga"
            const val NAME = "name"
            const val SIZE = "size"
            const val TYPE = "type"
            const val IMAGE = "image"
        }
    }

    object SUBTITLES {
        const val TABLE_NAME = "SubTitles"

        object COLUMNS {
            const val ID = "id"
            const val FK_ID_MANGA = "id_manga"
            const val LANGUAGE = "language"
            const val CHAPTER_KEY = "chapter_key"
            const val PAGE_KEY = "page_key"
            const val PAGE = "pageCount"
            const val FILE_PATH = "path"
            const val DATE_CREATE = "date_create"
            const val LAST_ALTERATION = "last_alteration"
        }
    }

    object JLPT {
        const val TABLE_NAME = "Jlpt"

        object COLUMNS {
            const val ID = "id"
            const val KANJI = "kanji"
            const val LEVEL = "level"
        }
    }

    object KANJAX {
        const val TABLE_NAME = "Kanjax"

        object COLUMNS {
            const val ID = "id"
            const val KANJI = "kanji"
            const val KEYWORD = "keyword"
            const val MEANING = "meaning"
            const val KOOHII = "koohii"
            const val KOOHII2 = "kohii2"
            const val ONYOMI = "onyomi"
            const val KUNYOMI = "kunyomi"
            const val ONWORDS = "onwords"
            const val KUNWORDS = "kunwords"
            const val JLPT = "jlpt"
            const val GRADE = "grade"
            const val FREQUENCE = "frequence"
            const val STROKES = "strokes"
            const val VARIANTS = "variants"
            const val RADICAL = "radical"
            const val PARTS = "parts"
            const val UTF8 = "utf8"
            const val SJIS = "sjis"
            const val KEYWORDS_PT = "keywords_pt"
            const val MEANING_PT = "meaning_pt"
        }
    }

    object FILELINK {
        const val TABLE_NAME = "FileLink"

        object COLUMNS {
            const val ID = "id"
            const val FK_ID_MANGA = "id_manga"
            const val PAGES = "pages"
            const val FILE_PATH = "path"
            const val FILE_NAME = "name"
            const val FILE_TYPE = "type"
            const val FILE_FOLDER = "folder"
            const val LANGUAGE = "language"
            const val DATE_CREATE = "date_create"
            const val LAST_ACCESS = "last_access"
            const val LAST_ALTERATION = "last_alteration"
        }
    }

    object PAGESLINK {
        const val TABLE_NAME = "PagesLink"

        object COLUMNS {
            const val ID = "id"
            const val FK_ID_FILE = "id_file"
            const val MANGA_PAGE = "manga_page"
            const val MANGA_PAGES = "manga_pages"
            const val MANGA_PAGE_NAME = "manga_page_name"
            const val MANGA_PAGE_PATH = "manga_page_path"
            const val FILE_LINK_PAGE = "file_link_page"
            const val FILE_LINK_PAGES = "file_link_pages"
            const val FILE_LINK_PAGE_NAME = "file_link_page_name"
            const val FILE_LINK_PAGE_PATH = "file_link_page_path"
            const val FILE_RIGHT_LINK_PAGE = "file_right_link_page"
            const val FILE_RIGHT_LINK_PAGE_NAME = "file_right_link_page_name"
            const val FILE_RIGHT_LINK_PAGE_PATH = "file_right_link_page_path"
            const val NOT_LINKED = "not_linked"
            const val DUAL_IMAGE = "dual_image"
            const val MANGA_DUAL_PAGE = "manga_dual_page"
            const val FILE_LEFT_DUAL_PAGE = "file_left_dual_page"
            const val FILE_RIGHT_DUAL_PAGE = "file_right_dual_page"
        }
    }

    object BOOK {
        const val TABLE_NAME = "Book"

        object COLUMNS {
            const val ID = "id"
            const val TITLE = "title"
            const val AUTHOR = "author"

            const val ANNOTATION = "annotation"
            const val YEAR = "year"
            const val GENRE = "genre"
            const val PUBLISHER = "publisher"
            const val ISBN = "isbn"
            const val PAGES = "pages"
            const val CHAPTER = "chapter"
            const val CHAPTER_DESCRIPTION = "chapter_description"
            const val BOOK_MARK = "book_mark"
            const val LANGUAGE = "language"
            const val FILE_PATH = "path"
            const val FILE_NAME = "name"
            const val FILE_TYPE = "type"
            const val FILE_FOLDER = "folder"
            const val FILE_SIZE = "size"

            const val FAVORITE = "favorite"
            const val DATE_CREATE = "date_create"
            const val LAST_ACCESS = "last_access"
            const val FK_ID_LIBRARY = "id_library"
            const val EXCLUDED = "excluded"
            const val LAST_ALTERATION = "last_alteration"
            const val FILE_ALTERATION = "file_alteration"
            const val LAST_VOCABULARY_IMPORT = "last_vocabulary_import"
            const val LAST_VERIFY = "last_verify"
        }
    }

    object BOOK_ANNOTATION {
        const val TABLE_NAME = "BookMark"

        object COLUMNS {
            const val ID = "id"
            const val FK_ID_BOOK = "id_book"
            const val CHAPTER_NUMBER = "chapter_number"
            const val CHAPTER = "chapter"
            const val PAGE = "page"
            const val PAGES = "pages"
            const val TYPE = "type"
            const val TEXT = "text"
            const val ANNOTATION = "annotation"
            const val FAVORITE = "favorite"
            const val COLOR = "color"
            const val ALTERATION = "alteration"
            const val CREATED = "created"
            const val COUNT = "count"
        }
    }

    object BOOK_SEARCH_HISTORY {
        const val TABLE_NAME = "BookSearchHistory"

        object COLUMNS {
            const val ID = "id"
            const val FK_ID_BOOK = "id_book"
            const val SEARCH = "search"
            const val DATE = "date"
        }
    }

    object BOOK_CONFIGURATION {
        const val TABLE_NAME = "BookConfiguration"

        object COLUMNS {
            const val ID = "id"
            const val FK_ID_BOOK = "id_book"
            const val ALIGNMENT = "alignment"
            const val MARGIN = "margin"
            const val SPACING = "spacing"
            const val SCROLLING = "scrolling"
            const val FONT_TYPE = "font_type"
            const val FONT_SIZE = "font_size"
        }
    }

    object VOCABULARY {
        const val TABLE_NAME = "Vocabulary"

        object COLUMNS {
            const val ID = "id"
            const val WORD = "word"
            const val BASIC_FORM = "basic_form"
            const val PORTUGUESE = "portuguese"
            const val ENGLISH = "english"
            const val READING = "reading"
            const val REVISED = "revised"
            const val JLPT = "jlpt"
            const val FAVORITE = "favorite"
            const val APPEARS = "appears"
        }
    }

    object MANGA_VOCABULARY {
        const val TABLE_NAME = "MangaVocabulary"

        object COLUMNS {
            const val ID = "id"
            const val ID_MANGA = "id_manga"
            const val ID_VOCABULARY = "id_vocabulary"
            const val APPEARS = "appears"
        }
    }

    object BOOK_VOCABULARY {
        const val TABLE_NAME = "BookVocabulary"

        object COLUMNS {
            const val ID = "id"
            const val ID_BOOK = "id_book"
            const val ID_VOCABULARY = "id_vocabulary"
            const val APPEARS = "appears"
        }
    }

    object LIBRARIES {
        const val TABLE_NAME = "Libraries"

        object COLUMNS {
            const val ID = "id"
            const val TITLE = "title"
            const val PATH = "path"
            const val LANGUAGE = "language"
            const val TYPE = "type"
            const val ENABLED = "enabled"
            const val EXCLUDED = "excluded"
        }

        object DEFAULT {
            const val ID = -1
        }
    }

}
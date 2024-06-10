package br.com.fenix.bilingualreader.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Color
import br.com.fenix.bilingualreader.model.enums.FileType
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.MarkType
import br.com.fenix.bilingualreader.service.controller.BookImageCoverController
import br.com.fenix.bilingualreader.service.controller.MangaImageCoverController
import br.com.fenix.bilingualreader.service.parses.manga.ParseFactory
import br.com.fenix.bilingualreader.service.parses.manga.RarParse
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.Util
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date


class TestUtil {
    companion object TestUtils {
        fun getCoverMipmap(index: Int = 0): Int {
            return if (index > 5 || index < 0)
                getCoverMipmap((0..5).random())
            else when (index) {
                1 -> R.mipmap.book_cover_1
                2 -> R.mipmap.book_cover_2
                3 -> R.mipmap.book_cover_3
                4 -> R.mipmap.book_cover_4
                5 -> R.mipmap.book_cover_5
                else -> R.mipmap.book_cover_1
            }
        }

        fun getRandomDate(): LocalDate {
            return LocalDate.of((1990..2022).random(), (1..12).random(), (1..28).random())
        }

        fun getRandomDateTime(): LocalDateTime {
            return LocalDateTime.of(
                (1990..2022).random(),
                (1..12).random(),
                (1..28).random(),
                (0..23).random(),
                (0..59).random(),
                (0..59).random()
            )
        }
    }
}

class MangaTestUtil {
    companion object MangaTestUtils {

        private val MANGA_TEST_FILE_PATH = "storage/emulated/0/Manga/Manga of test.cbr"

        fun generateCovers(context: Context, mangas: ArrayList<Manga>) {
            for ((index, manga) in mangas.withIndex())
                generateCovers(context, manga, index)
        }

        fun generateCovers(context: Context, manga: Manga, index: Int = 0) {
            val minMap = TestUtil.getCoverMipmap(index)
            val cover = BitmapFactory.decodeResource(context.resources, minMap)
            MangaImageCoverController.instance.saveCoverToCache(context, manga, cover)
        }

        fun getManga(context: Context, filePath: String = "", id : Long? = null): Manga {
            val mangaPath = filePath.ifEmpty { MANGA_TEST_FILE_PATH }
            val manga = Manga(
                id,
                Util.getNameWithoutExtensionFromPath(mangaPath),
                mangaPath,
                Util.getFolderFromPath(mangaPath),
                Util.getNameFromPath(mangaPath),
                10 * 1024,
                FileType.CBR,
                10,
                intArrayOf(2, 4),
                5,
                (1..2).random() > 1,
                (1..2).random() > 1,
                "Teste (writer), Teste (penciller).",
                "Séries",
                "Publisher",
                (1..15).random().toString(),
                TestUtil.getRandomDate(),
                GeneralConsts.KEYS.LIBRARY.DEFAULT_MANGA,
                (1..5).random() > 2,
                TestUtil.getRandomDateTime(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                Date(),
                if ((1..2).random() > 1) LocalDateTime.now() else null,
                if ((1..2).random() > 1) LocalDate.now() else null
            )

            if (filePath.isNotEmpty()) {
                val parse = ParseFactory.create(filePath) ?: return manga
                try {
                    if (parse is RarParse) {
                        val folder = GeneralConsts.CACHE_FOLDER.LINKED + '/' + Util.normalizeNameCache(manga.name)
                        val cacheDir = File(GeneralConsts.getCacheDir(context), folder)
                        (parse as RarParse?)!!.setCacheDirectory(cacheDir)
                    }

                    manga.pages = parse.numPages()
                    MangaImageCoverController.instance.getCoverFromFile(context, manga.file, parse)
                } finally {
                    Util.destroyParse(parse)
                }
            } else
                generateCovers(context, manga)

            return manga
        }

        fun getManga(): Manga {
            return Manga(
                1,
                Util.getNameWithoutExtensionFromPath(MANGA_TEST_FILE_PATH),
                MANGA_TEST_FILE_PATH,
                Util.getFolderFromPath(MANGA_TEST_FILE_PATH),
                Util.getNameFromPath(MANGA_TEST_FILE_PATH),
                3 * 2048,
                FileType.CB7,
                10,
                intArrayOf(2, 4),
                5,
                (1..2).random() > 1,
                (1..2).random() > 1,
                "Teste (writer), Teste (penciller).",
                "Séries",
                "Publisher",
                (1..15).random().toString(),
                TestUtil.getRandomDate(),
                GeneralConsts.KEYS.LIBRARY.DEFAULT_MANGA,
                (1..5).random() > 2,
                TestUtil.getRandomDateTime(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                Date(),
                if ((1..5).random() > 2) LocalDateTime.now() else null,
                if ((1..2).random() > 1) LocalDate.now() else null
            )
        }

        fun getArrayMangas(context: Context): ArrayList<Manga> {
            val mangas = getArrayMangas()
            generateCovers(context, mangas)
            return mangas
        }

        fun getArrayMangas(): ArrayList<Manga> {
            val array = arrayListOf<Manga>()

            for (i in 0 until 10)
                array.add(
                    Manga(
                        i.toLong(),
                        Util.getNameWithoutExtensionFromPath(MANGA_TEST_FILE_PATH),
                        MANGA_TEST_FILE_PATH,
                        Util.getFolderFromPath(MANGA_TEST_FILE_PATH),
                        Util.getNameFromPath(MANGA_TEST_FILE_PATH),
                        i * 2048L,
                        FileType.CBZ,
                        125,
                        intArrayOf(5, 15, 50, 75, 91, 115),
                        (0..100).random(),
                        i in 2..5,
                        (1..2).random() > 1,
                        "Teste (writer), Teste (penciller).",
                        "Séries",
                        "Publisher",
                        (1..15).random().toString(),
                        TestUtil.getRandomDate(),
                        GeneralConsts.KEYS.LIBRARY.DEFAULT_MANGA,
                        i in 4..6,
                        TestUtil.getRandomDateTime(),
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        Date(),
                        if (i in 4..6) LocalDateTime.now() else null,
                        if ((1..2).random() > 1) LocalDate.now() else null
                    )
                )

            return array
        }

    }
}

class BookTestUtil {
    companion object BookTestUtils {
        const val mBookPage = 24
        const val mBookPath = "/storage/1CFF-100F/Livros/The Irregular at Magic High School - Volume 01 [Yen Press] [Darkmeep].epub"
        private val BOOK_TEST_FILE_PATH = "storage/emulated/0/Book/Book of test.cbr"
        fun clearCache(context: Context) {
            val cache = File(
                GeneralConsts.getCacheDir(context),
                GeneralConsts.CACHE_FOLDER.BOOKS + '/'
            )

            if (cache.exists() && cache.listFiles() != null)
                for (file in cache.listFiles())
                    file.deleteRecursively()
        }

        fun getCover(context: Context, index: Int = 0) : Bitmap {
            val minMap = TestUtil.getCoverMipmap(index)
            return BitmapFactory.decodeResource(context.resources, minMap)
        }

        fun generateCovers(context: Context, books: ArrayList<Book>) {
            for ((index, book) in books.withIndex())
                generateCovers(context, book, index)
        }

        fun generateCovers(context: Context, book: Book, index: Int = 0) {
            val cover = getCover(context, index)
            BookImageCoverController.instance.saveCoverToCache(context, book, cover)
        }

        fun getBook(context: Context, filePath: String = ""): Book {
            val bookPath = filePath.ifEmpty { BOOK_TEST_FILE_PATH }
            val book = Book(
                1,
                Util.getNameWithoutExtensionFromPath(bookPath),
                "Author",
                "",
                "Annotation",
                TestUtil.getRandomDateTime().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd")
                ),
                "Genre",
                "Publisher",
                "Isbn",
                150,
                0,
                "",
                10,
                Languages.ENGLISH,
                bookPath,
                Util.getFolderFromPath(bookPath),
                Util.getNameFromPath(bookPath),
                FileType.EPUB,
                10 * 1024,
                (1..2).random() > 1,
                GeneralConsts.KEYS.LIBRARY.DEFAULT_BOOK,
                mutableListOf(),
                (1..5).random() > 4,
                TestUtil.getRandomDateTime(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                Date(),
                if ((1..2).random() > 1) LocalDateTime.now() else null,
                if ((1..2).random() > 1) LocalDate.now() else null
            )

            /*if (filePath.isNotEmpty()) {
                val parse = ParseFactory.create(filePath) ?: return book
                try {
                    if (parse is RarParse) {
                        val folder =
                            GeneralConsts.CACHE_FOLDER.LINKED + '/' + Util.normalizeNameCache(manga.name)
                        val cacheDir = File(GeneralConsts.getCacheDir(context), folder)
                        (parse as RarParse?)!!.setCacheDirectory(cacheDir)
                    }

                    book.pages = parse.numPages()
                    MangaImageCoverController.instance.getCoverFromFile(context, manga.file, parse)
                } finally {
                    Util.destroyParse(parse)
                }
            } else
                generateCovers(context, book)*/

            return book
        }

        fun getBook(): Book {
            return Book(
                1,
                Util.getNameWithoutExtensionFromPath(BOOK_TEST_FILE_PATH),
                "Author",
                "",
                "Annotation",
                TestUtil.getRandomDateTime().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd")
                ),
                "Genre",
                "Publisher",
                "Isbn",
                150,
                0,
                "",
                0,
                Languages.ENGLISH,
                BOOK_TEST_FILE_PATH,
                Util.getFolderFromPath(BOOK_TEST_FILE_PATH),
                Util.getNameFromPath(BOOK_TEST_FILE_PATH),
                FileType.EPUB,
                10 * 1024,
                (1..2).random() > 1,
                GeneralConsts.KEYS.LIBRARY.DEFAULT_BOOK,
                mutableListOf(),
                (1..5).random() > 4,
                TestUtil.getRandomDateTime(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                Date(),
                if ((1..2).random() > 1) LocalDateTime.now() else null,
                if ((1..2).random() > 1) LocalDate.now() else null
            )
        }

        fun getArrayBooks(context: Context): ArrayList<Book> {
            val books = getArrayBooks()
            generateCovers(context, books)
            return books
        }

        fun getArrayBooks(): ArrayList<Book> {
            val array = arrayListOf<Book>()

            for (i in 0 until 10)
                array.add(
                    Book(
                        i.toLong(),
                        Util.getNameWithoutExtensionFromPath(BOOK_TEST_FILE_PATH),
                        "Author",
                        "",
                        "Annotation",
                        TestUtil.getRandomDateTime().format(
                            DateTimeFormatter.ofPattern("yyyy-MM-dd")
                        ),
                        "Genre",
                        "Publisher",
                        "Isbn",
                        150,
                        0,
                        "",
                        0,
                        Languages.ENGLISH,
                        BOOK_TEST_FILE_PATH,
                        Util.getFolderFromPath(BOOK_TEST_FILE_PATH),
                        Util.getNameFromPath(BOOK_TEST_FILE_PATH),
                        FileType.EPUB,
                        10 * 1024,
                        (1..2).random() > 1,
                        GeneralConsts.KEYS.LIBRARY.DEFAULT_BOOK,
                        mutableListOf(),
                        (1..5).random() > 4,
                        TestUtil.getRandomDateTime(),
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        Date(),
                        if ((1..2).random() > 1) LocalDateTime.now() else null,
                        if ((1..2).random() > 1) LocalDate.now() else null
                    )
                )

            return array
        }

        fun getAnnotations(book: Book) : List<BookAnnotation> {
            val annotations = mutableListOf<BookAnnotation>()
            val chapters = mapOf(1 to 1f, 2 to 1f, 3 to 1f, 4 to 1f, 5 to 3f, 6 to 4f, 7 to 50f, 8 to 90f, 9 to 90f, 10 to 90f)
            val pages = mapOf(1 to 1, 2 to 3, 3 to 3, 4 to 3, 5 to 4, 6 to 5, 7 to 5, 8 to 6, 9 to 6, 10 to 6)
            val types = mapOf(1 to MarkType.Annotation, 2 to MarkType.Annotation, 3 to MarkType.Annotation, 4 to MarkType.Annotation, 5 to MarkType.BookMark, 6 to MarkType.PageMark, 7 to MarkType.Annotation, 8 to MarkType.BookMark, 9 to MarkType.PageMark, 10 to MarkType.Annotation)
            val colors = mapOf(1 to Color.Yellow, 2 to Color.Green, 3 to Color.Blue, 4 to Color.Red, 5 to Color.None, 6 to Color.None, 7 to Color.Green, 8 to Color.None, 9 to Color.None, 10 to Color.Blue)

            var annotationsCount = 0
            var marksCount = 0

            for (i in 1 until 11) {
                val type = types[i]
                val text : String
                val annotation : String

                if (type != MarkType.Annotation) {
                    annotationsCount++
                    text = "Text Annotation $i"
                    annotation = "Text Annotation $annotationsCount"
                } else {
                    marksCount++
                    text = "Text mark $i"
                    annotation = "Text Annotation $marksCount"
                }

                annotations.add(
                    BookAnnotation(
                        i.toLong(),
                        book.id!!,
                        pages[i]!!,
                        book.pages,
                        type!!,
                        chapters[i]!!,
                        "Chapter ${chapters[i]}",
                        text,
                        intArrayOf((1..5).random(), (6..10).random()),
                        annotation,
                        (1..3).random() < 2,
                        colors[i]!!,
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        i
                    )
                )
            }
            return annotations
        }
    }
}


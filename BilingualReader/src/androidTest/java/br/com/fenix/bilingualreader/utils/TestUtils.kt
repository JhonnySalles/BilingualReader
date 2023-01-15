package br.com.fenix.bilingualreader.utils

import android.content.Context
import android.graphics.BitmapFactory
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.FileType
import br.com.fenix.bilingualreader.service.controller.MangaImageCoverController
import br.com.fenix.bilingualreader.service.parses.manga.ParseFactory
import br.com.fenix.bilingualreader.service.parses.manga.RarParse
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.Util
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

class TestUtils {

    companion object TestUtils {

        private val TEST_FILE_PATH = "storage/emulated/0/Manga/Manga of test.cbr"

        private fun getCoverMipmap(index: Int = 0): Int {
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

        fun generateCovers(context: Context, mangas: ArrayList<Manga>) {
            for ((index, manga) in mangas.withIndex())
                generateCovers(context, manga, index)
        }

        fun generateCovers(context: Context, manga: Manga, index: Int = 0) {
            val minMap = getCoverMipmap(index)
            val cover = BitmapFactory.decodeResource(context.resources, minMap)
            MangaImageCoverController.instance.saveCoverToCache(context, manga, cover)
        }

        fun getManga(context: Context, filePath: String = ""): Manga {
            val mangaPath = filePath.ifEmpty { TEST_FILE_PATH }
            val manga = Manga(
                1,
                Util.getNameFromPath(mangaPath),
                mangaPath,
                Util.getFolderFromPath(mangaPath),
                Util.getNameWithoutExtensionFromPath(mangaPath),
                10 *1024,
                FileType.CBR,
                10,
                intArrayOf(2, 4),
                5,
                (1..2).random() > 1,
                (1..2).random() > 1,
                GeneralConsts.KEYS.LIBRARY.DEFAULT,
                (1..5).random() > 2,
                LocalDateTime.parse("2022-06-28T14:15:50.63"),
                LocalDateTime.now(),
                LocalDateTime.now(),
                0,
                if ((1..2).random() > 1) LocalDateTime.now() else null,
                if ((1..2).random() > 1) LocalDate.now() else null
            )

            if (filePath.isNotEmpty()) {
                val parse = ParseFactory.create(filePath) ?: return manga
                try {
                    if (parse is RarParse) {
                        val folder =
                            GeneralConsts.CACHE_FOLDER.LINKED + '/' + Util.normalizeNameCache(manga.name)
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
                Util.getNameFromPath(TEST_FILE_PATH),
                TEST_FILE_PATH,
                Util.getFolderFromPath(TEST_FILE_PATH),
                Util.getNameWithoutExtensionFromPath(TEST_FILE_PATH),
                3*2048,
                FileType.CB7,
                10,
                intArrayOf(2, 4),
                5,
                (1..2).random() > 1,
                (1..2).random() > 1,
                GeneralConsts.KEYS.LIBRARY.DEFAULT,
                (1..5).random() > 2,
                LocalDateTime.parse("2022-06-28T14:15:50.63"),
                LocalDateTime.now(),
                LocalDateTime.now(),
                0,
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
                        Util.getNameFromPath(TEST_FILE_PATH),
                        TEST_FILE_PATH,
                        Util.getFolderFromPath(TEST_FILE_PATH),
                        Util.getNameWithoutExtensionFromPath(TEST_FILE_PATH),
                        i*2048L,
                        FileType.CBZ,
                        125,
                        intArrayOf(5, 15, 50, 75, 91, 115),
                        (0..100).random(),
                        i in 2..5,
                        (1..2).random() > 1,
                        GeneralConsts.KEYS.LIBRARY.DEFAULT,
                        i in 4..6,
                        LocalDateTime.parse("2022-06-28T14:15:50.63"),
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        0,
                        if (i in 4..6) LocalDateTime.now() else null,
                        if ((1..2).random() > 1) LocalDate.now() else null
                    )
                )

            return array
        }

    }
}
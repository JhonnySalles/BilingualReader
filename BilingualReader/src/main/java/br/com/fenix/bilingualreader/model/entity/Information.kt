package br.com.fenix.bilingualreader.model.entity

import android.content.Context
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.service.tracker.mal.MalMangaDetail
import br.com.fenix.bilingualreader.service.tracker.mal.MalTransform
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.FileUtil
import br.com.fenix.bilingualreader.util.helpers.Util
import java.time.LocalDate

class Information() {
    companion object {
        const val MY_ANIME_LIST = "MyAnimeList"
        const val COMIC_INFO = "ComicInfo"
    }

    constructor(context: Context, manga: MalMangaDetail) : this() {
        setManga(context, manga)
    }

    constructor(context: Context, manga: ComicInfo) : this() {
        setManga(context, manga)
    }

    constructor(context: Context, book: Book) : this() {
        setBook(context, book)
    }

    var link: String = ""
    var imageLink: String? = null
    var title: String = ""
    var alternativeTitles: String = ""
    var synopsis: String = ""
    var synonyms: String = ""
    var volumes: String = ""
    var chapters: String = ""
    var status: String = ""
    var release: String = ""
    var genres: String = ""
    var authors: String = ""
    var origin: String = ""

    var language: Languages = Languages.ENGLISH
    var languageDescription: String = ""
    var series: String = ""
    var characters: String = ""
    var teams: String = ""
    var locations: String = ""
    var storyArch: String = ""

    var bookMarks = mapOf<String, Int>()

    var annotation: String = ""
    var publisher: String = ""
    var year: String = ""
    var isbn: String = ""
    var file: String = ""

    private fun setManga(context: Context, manga: MalMangaDetail) {
        this.link = "https://myanimelist.net/manga/${manga.id}"
        this.imageLink = manga.mainPicture?.medium.toString()
        this.title = manga.title
        this.alternativeTitles = ""

        manga.alternativeTitles?.let {
            if (it.english.isNotEmpty())
                this.alternativeTitles += it.english + ", "
            if (it.japanese.isNotEmpty())
                this.alternativeTitles += it.japanese + ", "
            if (it.synonyms.isNotEmpty())
                this.alternativeTitles += it.synonyms + ", "

            this.synonyms = it.synonyms.toString()
        }

        this.alternativeTitles = context.getString(
            R.string.manga_detail_web_information_alternative_titles,
            this.alternativeTitles.substringBeforeLast(",").plus(".")
        )

        manga.synopsis?.let { this.synopsis = it }
        this.volumes = context.getString(
            R.string.manga_detail_web_information_volumes,
            manga.volumes.toString()
        )
        this.chapters = context.getString(
            R.string.manga_detail_web_information_chapters,
            manga.chapters.toString()
        )
        manga.status?.let {
            this.status = context.getString(R.string.manga_detail_web_information_status, it.name)
        }

        val start = if (manga.startDate != null) MalTransform.getDate(manga.startDate) else null
        val end = if (manga.endDate != null) MalTransform.getDate(manga.endDate) else null
        this.release = if (start != null && end != null)
            context.getString(
                R.string.manga_detail_web_information_publish_from_to, GeneralConsts.formatterDate(
                    context,
                    start
                ), GeneralConsts.formatterDate(
                    context,
                    end
                )
            )
        else if (manga.startDate != null)
            context.getString(
                R.string.manga_detail_web_information_publish, GeneralConsts.formatterDate(
                    context,
                    start
                )
            )
        else
            ""

        this.genres = if (manga.authors != null)
            context.getString(
                R.string.manga_detail_web_information_genre,
                manga.genres?.joinToString { it.name })
        else
            ""

        this.authors = if (manga.authors != null)
            context.getString(
                R.string.manga_detail_web_information_authors,
                manga.authors.joinToString { it.author.firstName + " " + it.author.lastName + "(" + it.role + ")" })
        else
            ""

        this.origin = MY_ANIME_LIST
    }

    private fun setManga(context: Context, manga: ComicInfo) {
        manga.title?.let {
            this.title = context.getString(
                R.string.manga_detail_local_information_comic_info_title,
                it
            )
        }
        manga.series?.let {
            this.series = context.getString(R.string.manga_detail_local_information_series, it)
        }
        manga.volume?.let {
            this.volumes = context.getString(
                R.string.manga_detail_local_information_volume,
                it.toString()
            )
        }
        manga.publisher?.let {
            this.publisher =
                context.getString(R.string.manga_detail_local_information_publisher, it)
        }

        manga.storyArc?.let {
            this.storyArch =
                context.getString(R.string.manga_detail_local_information_comic_info_story_arch, it)
        }
        manga.genre?.let {
            this.genres =
                context.getString(R.string.manga_detail_local_information_comic_info_genre, it)
        }
        manga.characters?.let {
            this.characters =
                context.getString(R.string.manga_detail_local_information_comic_info_characters, it)
        }
        manga.teams?.let {
            this.teams =
                context.getString(R.string.manga_detail_local_information_comic_info_teams, it)
        }
        manga.locations?.let {
            this.locations =
                context.getString(R.string.manga_detail_local_information_comic_info_locations, it)
        }

        manga.languageISO?.let {
            this.language = when (it) {
                "pt" -> Languages.PORTUGUESE
                "ja" -> Languages.JAPANESE
                else -> Languages.ENGLISH
            }
            languageDescription = context.getString(
                R.string.manga_detail_local_information_comic_info_language,
                Util.languageToString(context, this.language)
            )
        }

        val authors = ""
        manga.writer?.let { this@Information.authors += it + " (" + context.getString(R.string.text_writer) + "), " }
        manga.penciller?.let { this@Information.authors += it + " (" + context.getString(R.string.text_penciller) + "), " }
        manga.inker?.let { this@Information.authors += it + " (" + context.getString(R.string.text_inker) + "), " }
        manga.coverArtist?.let { this@Information.authors += it + " (" + context.getString(R.string.text_cover_artist) + "), " }
        manga.colorist?.let { this@Information.authors += it + " (" + context.getString(R.string.text_colorist) + "), " }

        this.release = if (manga.year != null) context.getString(
            R.string.manga_detail_local_information_release,
            GeneralConsts.formatterDate(
                context,
                LocalDate.of(manga.year!!, manga.month ?: 1, manga.day ?: 1)
            )
        ) else ""

        if (authors.isNotEmpty())
            this.authors = context.getString(
                R.string.manga_detail_local_information_authors,
                authors.substringBeforeLast(", ") + "."
            )

        manga.pages?.let {
            bookMarks = it.filter { c -> c.bookmark != null && c.image != null }
                .associate { c -> c.bookmark!! to (c.image!! + 1) }
        }

        this.origin = COMIC_INFO
    }

    private fun setBook(context: Context, book: Book): Information {
        this.genres = book.genre
        this.annotation = book.annotation
        this.publisher = book.publisher
        this.year = book.year
        this.isbn = book.isbn
        this.file =
            book.fileName + "  " + FileUtil.formatSize(book.fileSize) + "  " + GeneralConsts.formatterDate(
                context,
                book.fileAlteration
            )

        return this
    }
}
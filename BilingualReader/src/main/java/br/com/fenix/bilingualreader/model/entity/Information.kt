package br.com.fenix.bilingualreader.model.entity

import android.content.Context
import br.com.fenix.bilingualreader.service.tracker.mal.MalMangaDetail
import br.com.fenix.bilingualreader.service.tracker.mal.MalTransform
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.FileUtil
import java.util.*

class Information(
    link: String, imageLink: String?, title: String, alternativeTitles: String, synopsis: String, synonyms: String, volumes: String,
    chapters: String, status: String, startDate: Date?, endDate: Date?, genres: String, authors: String
) {
    companion object {
        val MY_ANIME_LIST = "MyAnimeList"
    }

    constructor() : this("", null, "", "", "", "", "", "", "", null, null, "", "")

    constructor(manga: MalMangaDetail) : this() {
        setManga(manga)
    }

    var link: String = link
    var imageLink: String? = imageLink
    var title: String = title
    var alternativeTitles: String = alternativeTitles
    var synopsis: String = synopsis
    var synonyms: String = synonyms
    var volumes: String = volumes
    var chapters: String = chapters
    var status: String = status
    var startDate: Date? = startDate
    var endDate: Date? = endDate
    var genres: String = genres
    var authors: String = authors
    var origin: String = ""

    var annotation: String = ""
    var publisher: String = ""
    var year: String = ""
    var isbn: String = ""
    var file: String = ""

    fun setManga(manga: MalMangaDetail) {
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

        this.alternativeTitles = this.alternativeTitles.substringBeforeLast(",").plus(".")

        manga.synopsis?.let { this.synopsis = it }
        this.volumes = manga.volumes.toString()
        this.chapters = manga.chapters.toString()
        manga.status?.let { this.status = it.name }

        manga.startDate?.let { if (it.isNotEmpty()) this.startDate = MalTransform.getDate(it) }
        manga.endDate?.let { if (it.isNotEmpty()) this.endDate = MalTransform.getDate(it) }

        this.genres = manga.genres?.joinToString { it.name } ?: ""
        this.authors = manga.authors?.joinToString { it.author.firstName + " " + it.author.lastName + "(" + it.role + ")" } ?: ""
        this.origin = MY_ANIME_LIST
    }

    fun setBook(context: Context, book: Book) : Information {
        this.genres = book.genre
        this.annotation = book.annotation
        this.publisher = book.publisher
        this.year = book.year
        this.isbn = book.isbn
        this.file = book.fileName + "  " + FileUtil.formatSize(book.fileSize) + "  " + GeneralConsts.formatterDate(context, book.fileAlteration)

        return this
    }
}
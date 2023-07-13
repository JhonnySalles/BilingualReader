package br.com.fenix.bilingualreader.model.entity

import androidx.room.*
import br.com.fenix.bilingualreader.model.enums.FileType
import br.com.fenix.bilingualreader.service.parses.manga.Parse
import br.com.fenix.bilingualreader.util.constants.DataBaseConsts
import br.com.fenix.bilingualreader.util.helpers.FileUtil
import br.com.fenix.bilingualreader.util.helpers.Util
import java.io.File
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

@Entity(
    tableName = DataBaseConsts.MANGA.TABLE_NAME,
    indices = [Index(value = [DataBaseConsts.MANGA.COLUMNS.FILE_NAME, DataBaseConsts.MANGA.COLUMNS.TITLE])]
)
class Manga(
    id: Long?,
    title: String,
    path: String,
    folder: String,
    name: String,
    fileSize: Long,
    type: FileType,
    pages: Int,
    chapters: IntArray,
    bookMark: Int,
    favorite: Boolean,
    hasSubtitle: Boolean,
    author: String,
    series: String,
    publisher: String,
    volume: String,
    release: Date?,
    fkLibrary: Long?,
    excluded: Boolean,
    dateCreate: Date?,
    lastAccess: Date?,
    lastAlteration: Date?,
    fileAlteration: Date,
    lastVocabImport: Date?,
    lastVerify: Date?
) : Serializable {

    constructor( id: Long?, title: String,
        path: String, folder: String, name: String, size: Long, type: FileType,
        pages: Int, chapters: IntArray, bookMark: Int, favorite: Boolean, hasSubtitle: Boolean,
        author: String , series: String, publisher: String, volume: String, idLibrary: Long?,
        excluded: Boolean, dateCreate: Date?, fileAlteration: Date, lastVocabularyImport: Date?,
        lastVerify: Date?, release: Date?, lastAlteration: Date?,
        lastAccess: Date?, sort: Date? = null
    ) : this( id, title, path, folder, name, size, type, pages, chapters, bookMark, favorite,
        hasSubtitle, author, series, publisher, volume, release, idLibrary, excluded,
        dateCreate, lastAccess, lastAlteration, fileAlteration, lastVocabularyImport, lastVerify
    ) {
        this.sort = sort
    }

    @Ignore
    constructor(fkLibrary: Long?, id: Long?, file: File, parse: Parse) : this(
        id, file.nameWithoutExtension, file.path, file.parent, file.name, file.length(), FileType.UNKNOWN,
        parse.numPages(), parse.getChapters(), 0, false, parse.hasSubtitles(), "", "", "",
        "", null, fkLibrary, false, Date(), null, null, Date(file.lastModified()),
        null, null
    ) {
        this.type = FileUtil.getFileType(file.name)
        this.volume = title.substringAfterLast("Volume", "").trim().replace(Regex("[^\\d.][\\s\\S]+"), "")

        parse.getComicInfo()?.let {
            this.author = ""

            if (it.writer != null && it.writer!!.isNotEmpty())
                author += it.writer + ", "
            if (it.penciller != null && it.penciller!!.isNotEmpty() && !author.contains(it.penciller!!, true))
                author += it.penciller + ", "
            if (it.inker != null && it.inker!!.isNotEmpty() && !author.contains(it.inker!!, true))
                author += it.inker + ", "

            if (author.contains(","))
                author = author.substringBeforeLast(", ") + "."

            this.series = it.series.toString()
            this.publisher = it.publisher.toString()
            this.volume = it.volume.toString()
            this.release = if (it.year != null) SimpleDateFormat("yyyy/MM/dd").parse("${it.year!!}/${it.month?:1}/${it.day?:1}") else null
        }
    }

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.ID)
    var id: Long? = id

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.TITLE)
    var title: String = title

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.PAGES)
    var pages: Int = pages

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.CHAPTERS)
    var chapters: IntArray = chapters

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.BOOK_MARK)
    var bookMark: Int = bookMark

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.FILE_PATH)
    var path: String = path

    @Ignore
    var file: File = File(path)

    @Ignore
    var fileName: String = Util.getNameWithoutExtensionFromPath(path)

    @Ignore
    var extension: String = Util.getExtensionFromPath(path)

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.FILE_SIZE)
    var fileSize: Long = fileSize

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.FILE_NAME)
    var name: String = name

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.FILE_TYPE)
    var type: FileType = type

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.FILE_FOLDER)
    var folder: String = folder

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.FAVORITE)
    var favorite: Boolean = favorite

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.HAS_SUBTITLE)
    var hasSubtitle: Boolean = hasSubtitle

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.AUTHOR)
    var author: String = author

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.SERIES)
    var series: String = series

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.PUBLISHER)
    var publisher: String = publisher

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.VOLUME)
    var volume: String = volume

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.RELEASE)
    var release: Date? = release

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.EXCLUDED)
    var excluded: Boolean = excluded

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.FK_ID_LIBRARY)
    var fkLibrary: Long? = fkLibrary

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.DATE_CREATE)
    var dateCreate: Date? = dateCreate

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.LAST_ACCESS)
    var lastAccess: Date? = lastAccess

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.LAST_ALTERATION)
    var lastAlteration: Date? = lastAlteration

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.FILE_ALTERATION)
    var fileAlteration: Date = fileAlteration

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.LAST_VOCABULARY_IMPORT)
    var lastVocabImport: Date? = lastVocabImport

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.LAST_VERIFY)
    var lastVerify: Date? = lastVerify

    @Ignore
    var library: Library = Library(null)

    @Ignore
    var update: Boolean = false

    @Ignore
    var subTitles: List<SubTitle> = arrayListOf()

    @Ignore
    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.SORT)
    var sort: Date? = null

    override fun toString(): String {
        return "Manga(id=$id, title='$title', pages=$pages, bookMark=$bookMark, type='$type', update=$update)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Manga

        if (id != other.id) return false
        if (path != other.path) return false
        if (name != other.name) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + path.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    fun update(manga: Manga) : Boolean {
        val updated = this.bookMark != manga.bookMark || this.favorite != manga.favorite ||
                this.hasSubtitle != manga.hasSubtitle || this.lastAccess != manga.lastAccess

        this.bookMark = manga.bookMark
        this.favorite = manga.favorite
        this.lastAccess = manga.lastAccess
        this.hasSubtitle = manga.hasSubtitle
        this.lastAlteration = manga.lastAlteration
        this.lastVocabImport = manga.lastVocabImport

        return updated
    }

    fun update(parse: Parse) {
        this.hasSubtitle = parse.hasSubtitles()
        this.chapters = parse.getChapters()
        this.fileSize = file.length()
        this.fileAlteration = Date(file.lastModified())
        this.lastVocabImport = null
        this.type = FileUtil.getFileType(file.name)
        this.volume = title.substringAfterLast("Volume", "").trim().replace(Regex("[^\\d.][\\s\\S]+"), "")

        parse.getComicInfo()?.let {
            this.author = ""

            if (it.writer != null && it.writer!!.isNotEmpty())
                author += it.writer + ", "
            if (it.penciller != null && it.penciller!!.isNotEmpty() && !author.contains(it.penciller!!, true))
                author += it.penciller + ", "
            if (it.inker != null && it.inker!!.isNotEmpty() && !author.contains(it.inker!!, true))
                author += it.inker + ", "

            if (author.contains(","))
                author = author.substringBeforeLast(", ") + "."

            this.series = it.series.toString()
            this.publisher = it.publisher.toString()
            this.volume = it.volume.toString()
            this.release = if (it.year != null) SimpleDateFormat("yyyy/MM/dd").parse("${it.year!!}/${it.month?:1}/${it.day?:1}") else null
        }
    }

}
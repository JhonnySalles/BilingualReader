package br.com.fenix.bilingualreader.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.fenix.bilingualreader.model.enums.FileType
import br.com.fenix.bilingualreader.service.parses.manga.Parse
import br.com.fenix.bilingualreader.util.constants.DataBaseConsts
import br.com.fenix.bilingualreader.util.helpers.FileUtil
import br.com.fenix.bilingualreader.util.helpers.Util
import java.io.File
import java.io.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Date


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
    release: LocalDate?,
    fkLibrary: Long?,
    excluded: Boolean,
    dateCreate: LocalDateTime?,
    lastAccess: LocalDateTime?,
    lastAlteration: LocalDateTime?,
    fileAlteration: Date,
    lastVocabImport: LocalDateTime?,
    lastVerify: LocalDate?
) : Serializable {

    constructor( id: Long?, title: String,
        path: String, folder: String, name: String, size: Long, type: FileType,
        pages: Int, chapters: IntArray, bookMark: Int, favorite: Boolean, hasSubtitle: Boolean,
        author: String , series: String, publisher: String, volume: String, idLibrary: Long?,
        excluded: Boolean, dateCreate: LocalDateTime?, fileAlteration: Date, lastVocabularyImport: LocalDateTime?,
        lastVerify: LocalDate?, release: LocalDate?, lastAlteration: LocalDateTime?,
        lastAccess: LocalDateTime?, sort: LocalDateTime? = null
    ) : this( id, title, path, folder, name, size, type, pages, chapters, bookMark, favorite,
        hasSubtitle, author, series, publisher, volume, release, idLibrary, excluded,
        dateCreate, lastAccess, lastAlteration, fileAlteration, lastVocabularyImport, lastVerify
    ) {
        this.sort = sort
    }

    @Ignore
    constructor(fkLibrary: Long?, id: Long?, file: File) : this(
        id, file.nameWithoutExtension, file.path, file.parent, file.name, file.length(), FileType.UNKNOWN,
        1, intArrayOf(), 0, false, false, "", "", "",
        "", null, fkLibrary, false, LocalDateTime.now(), null, null, Date(file.lastModified()),
        null, null
    ) {
        this.type = FileUtil.getFileType(file.name)
        this.volume = title.substringAfterLast("Volume", "").trim().replace(Regex("[^\\d.][\\s\\S]+"), "")
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
        set(value) {
            field = value
            this.file = File(value)
            this.fileName = Util.getNameWithoutExtensionFromPath(value)
            this.extension = Util.getExtensionFromPath(value)
            this.folder = file.parent ?: ""
        }

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
    var release: LocalDate? = release

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.EXCLUDED)
    var excluded: Boolean = excluded

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.FK_ID_LIBRARY)
    var fkLibrary: Long? = fkLibrary

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.DATE_CREATE)
    var dateCreate: LocalDateTime? = dateCreate

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.LAST_ACCESS)
    var lastAccess: LocalDateTime? = lastAccess

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.LAST_ALTERATION)
    var lastAlteration: LocalDateTime? = lastAlteration

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.FILE_ALTERATION)
    var fileAlteration: Date = fileAlteration

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.LAST_VOCABULARY_IMPORT)
    var lastVocabImport: LocalDateTime? = lastVocabImport

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.LAST_VERIFY)
    var lastVerify: LocalDate? = lastVerify

    @Ignore
    var library: Library = Library(null)

    @Ignore
    var update: Boolean = false

    @Ignore
    var subTitles: List<SubTitle> = arrayListOf()

    @Ignore
    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.SORT)
    var sort: LocalDateTime? = null

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
                this.hasSubtitle != manga.hasSubtitle || this.lastAccess != manga.lastAccess ||
                this.author != manga.author || this.series != manga.series || this.publisher != manga.publisher ||
                this.volume != manga.volume || this.release != manga.release

        this.bookMark = manga.bookMark
        this.favorite = manga.favorite
        this.lastAccess = manga.lastAccess
        this.hasSubtitle = manga.hasSubtitle
        this.lastAlteration = manga.lastAlteration
        this.lastVocabImport = manga.lastVocabImport
        this.author = manga.author
        this.series = manga.series
        this.publisher = manga.publisher
        this.volume = manga.volume
        this.release = manga.release

        return updated
    }

    fun update(parse: Parse) {
        this.hasSubtitle = parse.hasSubtitles()
        this.pages = parse.numPages()
        this.chapters = parse.getChapters()
        this.fileSize = file.length()
        this.fileAlteration = Date(file.lastModified())
        this.lastVocabImport = null
        this.type = FileUtil.getFileType(file.name)
        this.volume = title.substringAfterLast("Volume", "").trim().replace(Regex("[^\\d.][\\s\\S]+"), "")

        parse.getComicInfo()?.let {
            update(it)
        }
    }

    fun update(comic: ComicInfo) : Boolean  {
        var author = ""

        if (comic.writer != null && comic.writer!!.isNotEmpty())
            author += comic.writer + ", "
        if (comic.penciller != null && comic.penciller!!.isNotEmpty() && !author.contains(comic.penciller!!, true))
            author += comic.penciller + ", "
        if (comic.inker != null && comic.inker!!.isNotEmpty() && !author.contains(comic.inker!!, true))
            author += comic.inker + ", "

        if (author.contains(","))
            author = author.substringBeforeLast(", ") + "."

        val release = if (comic.year != null) LocalDate.of(comic.year!!, comic.month ?: 1, comic.day ?: 1) else null

        val updated = this.author != author || this.series != comic.series .toString()|| this.publisher != comic.publisher.toString() ||
                this.volume != comic.volume.toString() || this.release != release

        this.series = comic.series.toString()
        this.publisher = comic.publisher.toString()
        this.volume = comic.volume.toString()
        this.author = author
        this.release = release

        return updated
    }

}
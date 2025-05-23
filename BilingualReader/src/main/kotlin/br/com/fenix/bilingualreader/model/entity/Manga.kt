package br.com.fenix.bilingualreader.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.fenix.bilingualreader.model.enums.FileType
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.model.interfaces.History
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
    fileType: FileType,
    pages: Int,
    chapters: IntArray,
    chaptersPages: Map<Int, String>,
    bookMark: Int,
    completed: Boolean,
    favorite: Boolean,
    hasSubtitle: Boolean,
    author: String,
    series: String,
    genre: String,
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
) : Serializable, History {

    constructor( id: Long?, title: String,
        path: String, folder: String, name: String, size: Long, fileType: FileType,
        pages: Int, chapters: IntArray, chaptersPages: Map<Int, String>, bookMark: Int, completed: Boolean, favorite: Boolean, hasSubtitle: Boolean,
        author: String , series: String, genre: String, publisher: String, volume: String, idLibrary: Long?,
        excluded: Boolean, dateCreate: LocalDateTime?, fileAlteration: Date, lastVocabularyImport: LocalDateTime?,
        lastVerify: LocalDate?, release: LocalDate?, lastAlteration: LocalDateTime?,
        lastAccess: LocalDateTime?, sort: LocalDateTime? = null
    ) : this( id, title, path, folder, name, size, fileType, pages, chapters, chaptersPages, bookMark, completed, favorite,
        hasSubtitle, author, series, genre, publisher, volume, release, idLibrary, excluded,
        dateCreate, lastAccess, lastAlteration, fileAlteration, lastVocabularyImport, lastVerify
    ) {
        this.sort = sort
    }

    @Ignore
    constructor(fkLibrary: Long?, id: Long?, file: File) : this(
        id, file.nameWithoutExtension, file.path, file.parent, file.name, file.length(), FileType.UNKNOWN,
        1, intArrayOf(), mapOf(), 0, false, false, false, "", "", "", "",
        "", null, fkLibrary, false, LocalDateTime.now(), null, null, Date(file.lastModified()),
        null, null
    ) {
        this.fileType = FileUtil.getFileType(file.name)
        this.volume = title.lowercase().substringAfterLast("volume", "").trim().replace(Regex("[^\\d.][\\s\\S]+"), "")
    }

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.ID)
    override var id: Long? = id

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.TITLE)
    override var title: String = title

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.PAGES)
    override var pages: Int = pages

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.CHAPTERS)
    var chapters: IntArray = chapters

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.CHAPTERS_PAGES)
    var chaptersPages: Map<Int, String> = chaptersPages

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.BOOK_MARK)
    override var bookMark: Int = bookMark
        set(value) {
            field = value
            this.completed = value >= pages
        }

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.COMPLETED)
    override var completed: Boolean = completed

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
    override var fileSize: Long = fileSize

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.FILE_NAME)
    override var name: String = name

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.FILE_TYPE)
    override var fileType: FileType = fileType

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.FILE_FOLDER)
    var folder: String = folder

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.FAVORITE)
    override var favorite: Boolean = favorite

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.HAS_SUBTITLE)
    var hasSubtitle: Boolean = hasSubtitle

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.AUTHOR)
    var author: String = author

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.SERIES)
    var series: String = series

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.GENRE)
    var genre: String = genre

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.PUBLISHER)
    var publisher: String = publisher

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.VOLUME)
    override var volume: String = volume

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.RELEASE)
    var release: LocalDate? = release

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.EXCLUDED)
    override var excluded: Boolean = excluded

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.FK_ID_LIBRARY)
    override var fkLibrary: Long? = fkLibrary

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.DATE_CREATE)
    var dateCreate: LocalDateTime? = dateCreate

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.LAST_ACCESS)
    override var lastAccess: LocalDateTime? = lastAccess

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.LAST_ALTERATION)
    var lastAlteration: LocalDateTime? = lastAlteration

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.FILE_ALTERATION)
    var fileAlteration: Date = fileAlteration

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.LAST_VOCABULARY_IMPORT)
    var lastVocabImport: LocalDateTime? = lastVocabImport

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.LAST_VERIFY)
    var lastVerify: LocalDate? = lastVerify

    @Ignore
    override var library: Library = Library(null)

    @Ignore
    var update: Boolean = false

    @Ignore
    var subTitles: List<SubTitle> = arrayListOf()

    @Ignore
    override val type: Type = Type.MANGA

    @Ignore
    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.SORT)
    override var sort: LocalDateTime? = null

    override fun toString(): String {
        return "Manga(id=$id, title='$title', pages=$pages, bookMark=$bookMark, fileType='$fileType', update=$update)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Manga

        if (id != other.id) return false
        if (path != other.path) return false
        if (name != other.name) return false
        if (fileType != other.fileType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + path.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + fileType.hashCode()
        return result
    }

    fun update(manga: Manga, isFull: Boolean = false) : Boolean {
        val updated = this.bookMark != manga.bookMark || this.favorite != manga.favorite ||
                this.hasSubtitle != manga.hasSubtitle || this.lastAccess != manga.lastAccess ||
                this.author != manga.author || this.series != manga.series || this.publisher != manga.publisher ||
                this.volume != manga.volume || this.release != manga.release

        this.bookMark = manga.bookMark
        this.completed = manga.completed
        this.favorite = manga.favorite
        this.lastAccess = manga.lastAccess
        this.hasSubtitle = manga.hasSubtitle
        this.lastAlteration = manga.lastAlteration
        this.lastVocabImport = manga.lastVocabImport
        this.author = manga.author
        this.series = manga.series
        this.genre = manga.genre
        this.publisher = manga.publisher
        this.volume = manga.volume
        this.release = manga.release

        if (isFull) {
            this.title = manga.title
            this.chapters = manga.chapters
            this.chaptersPages = manga.chaptersPages
            this.pages = manga.pages
            this.pages = manga.pages
        }

        return updated
    }

    fun update(parse: Parse) {
        this.hasSubtitle = parse.hasSubtitles()
        this.pages = parse.numPages()
        this.chapters = parse.getChapters()
        this.fileSize = file.length()
        this.fileAlteration = Date(file.lastModified())
        this.lastVocabImport = null
        this.fileType = FileUtil.getFileType(file.name)
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
                this.genre != comic.genre.toString() || this.volume != comic.volume.toString() || this.release != release

        this.chaptersPages = if (comic.pages != null && comic.pages!!.any { it.bookmark != null }) {
            val map = mutableMapOf<Int, String>()
            for ((index, page) in comic.pages!!.withIndex()) {
                if (page.bookmark != null)
                    map[index] = page.bookmark!!
            }
            map.toMap()
        } else
            mapOf()

        this.publisher = comic.publisher.toString()
        this.volume = comic.volume.toString()
        this.author = author
        this.release = release
        this.genre = comic.genre.toString()
        this.series = comic.series ?: let {
            var index = -1
            if (title.contains(" vol.",true)) {
                index = title.lastIndexOf(" vol.", 0, true)
                if (index < 0)
                    index = title.indexOf(" vol.", 0, true)
            } else if (title.contains("volume", true)){
                index = title.lastIndexOf("volume", 0, true)
                if (index < 0)
                    index = title.indexOf("volume", 0, true)
            }
            if (index > -1) title.substring(0, index).trim() else ""
        }

        return updated
    }

}
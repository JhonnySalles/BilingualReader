package br.com.fenix.bilingualreader.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.ebook.foobnix.ext.EbookMeta
import br.com.fenix.bilingualreader.model.enums.FileType
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.model.interfaces.History
import br.com.fenix.bilingualreader.util.constants.DataBaseConsts
import br.com.fenix.bilingualreader.util.helpers.FileUtil
import br.com.fenix.bilingualreader.util.helpers.Util
import java.io.File
import java.io.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Date


@Entity(
    tableName = DataBaseConsts.BOOK.TABLE_NAME,
    indices = [Index(value = [DataBaseConsts.BOOK.COLUMNS.FILE_NAME, DataBaseConsts.BOOK.COLUMNS.TITLE])]
)
class Book(
    id: Long?,
    title: String,
    author: String,
    password: String,
    annotation: String,
    year: String,
    genre: String,
    publisher: String,
    isbn: String,
    pages: Int,
    chapter: Int,
    chapterDescription: String,
    bookMark: Int,
    language: Languages,
    path: String,
    folder: String,
    name: String,
    fileType: FileType,
    fileSize: Long,
    favorite: Boolean,
    fkLibrary: Long?,
    tags: MutableList<Long>,
    excluded: Boolean,
    dateCreate: LocalDateTime?,
    lastAccess: LocalDateTime?,
    lastAlteration: LocalDateTime?,
    fileAlteration: Date,
    lastVocabImport: LocalDateTime?,
    lastVerify: LocalDate?
) : Serializable, History {

    constructor(
        id: Long?, title: String, author: String, password: String, annotation: String, year: String, genre: String, publisher: String, isbn: String,
        pages: Int, chapter: Int, chapterDescription: String, bookMark: Int, language: Languages, path: String, name: String, fileType: FileType, folder: String,
        fileSize: Long, favorite: Boolean, dateCreate: LocalDateTime?, fkLibrary: Long?, tags: MutableList<Long>, excluded: Boolean, lastAlteration: LocalDateTime?,
        fileAlteration: Date, lastVocabImport: LocalDateTime?, lastVerify: LocalDate?, lastAccess: LocalDateTime?, sort: LocalDateTime?
    ) : this(
        id, title, author, password, annotation, year, genre, publisher, isbn, pages, chapter, chapterDescription, bookMark, language, path, folder,
        name, fileType, fileSize, favorite, fkLibrary, tags, excluded, dateCreate, lastAccess, lastAlteration, fileAlteration, lastVocabImport, lastVerify
    ) {
        this.sort = sort
    }


    @Ignore
    constructor(
        fkLibrary: Long?, title: String, author: String, annotation: String, year: String, genre: String, publisher: String, isbn: String, path: String,
        folder: String, name: String, fileSize: Long, pages: Int
    ) : this(
        null, title, author, "", annotation, year, genre, publisher, isbn, pages, 0, "", 0,
        Languages.ENGLISH, path, folder, name, FileType.UNKNOWN, fileSize, false, fkLibrary, mutableListOf(), false,
        LocalDateTime.now(), null, LocalDateTime.now(), Date(), null, null
    ) {
        this.fileType = FileUtil.getFileType(this.fileName)
        this.fileAlteration = Date(this.file.lastModified())
    }

    @Ignore
    constructor(fkLibrary: Long?, id: Long?, file: File) : this(
        id, "",  "", "",  "", "",  "", "", "", 1, 0,
        "", 0, Languages.ENGLISH, file.path, file.parent, file.name, FileType.UNKNOWN, file.length(), false,
        fkLibrary, mutableListOf(), false, LocalDateTime.now(), null, LocalDateTime.now(), Date(), null, null
    ) {
        this.fileType = FileUtil.getFileType(file.name)
        this.fileAlteration = Date(this.file.lastModified())
    }

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.ID)
    override var id: Long? = id

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.TITLE)
    override var title: String = title

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.AUTHOR)
    var author: String = author

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.PASSWORD)
    var password: String = password

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.ANNOTATION)
    var annotation: String = annotation

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.YEAR)
    var year: String = year

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.GENRE)
    var genre: String = genre

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.PUBLISHER)
    var publisher: String = publisher

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.ISBN)
    var isbn: String = isbn

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.PAGES)
    override var pages: Int = pages

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.CHAPTER)
    var chapter: Int = chapter

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.CHAPTER_DESCRIPTION)
    var chapterDescription: String = chapterDescription

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.BOOK_MARK)
    override var bookMark: Int = bookMark

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.LANGUAGE)
    var language: Languages = language

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.FILE_PATH)
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

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.FILE_SIZE)
    override var fileSize: Long = fileSize

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.FILE_NAME)
    override var name: String = name

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.FILE_TYPE)
    override var fileType: FileType = fileType

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.FILE_FOLDER)
    var folder: String = folder

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.FAVORITE)
    override var favorite: Boolean = favorite

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.EXCLUDED)
    override var excluded: Boolean = excluded

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.FK_ID_LIBRARY)
    override var fkLibrary: Long? = fkLibrary

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.TAGS)
    var tags: MutableList<Long> = tags

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.DATE_CREATE)
    var dateCreate: LocalDateTime? = dateCreate

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.LAST_ACCESS)
    override var lastAccess: LocalDateTime? = lastAccess

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.LAST_ALTERATION)
    var lastAlteration: LocalDateTime? = lastAlteration

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.FILE_ALTERATION)
    var fileAlteration: Date = fileAlteration

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.LAST_VOCABULARY_IMPORT)
    var lastVocabImport: LocalDateTime? = lastVocabImport

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.LAST_VERIFY)
    var lastVerify: LocalDate? = lastVerify

    @Ignore
    override var library: Library = Library(null)

    @Ignore
    override val type: Type = Type.BOOK

    @Ignore
    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.SORT)
    override var sort: LocalDateTime? = null

    override fun toString(): String {
        return "Book(id=$id, title='$title', author='$author', language=$language, path='$path', fileName='$fileName', extension='$extension', fileSize=$fileSize, name='$name', favorite=$favorite, excluded=$excluded)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Book

        if (id != other.id) return false
        if (title != other.title) return false
        if (author != other.author) return false
        if (path != other.path) return false
        if (name != other.name) return false
        if (fileType != other.fileType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + title.hashCode()
        result = 31 * result + author.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + fileType.hashCode()
        return result
    }

    fun update(book: Book) : Boolean {
        val updated = this.bookMark != book.bookMark || this.favorite != book.favorite ||
                this.pages != book.pages || this.language != book.language ||
                this.tags != book.tags || this.lastAccess != book.lastAccess

        this.bookMark = book.bookMark
        this.pages = book.pages
        this.tags = book.tags
        this.language = book.language
        this.favorite = book.favorite
        this.lastAccess = book.lastAccess
        this.lastAlteration = book.lastAlteration
        this.lastVocabImport = book.lastVocabImport

        return updated
    }

    fun update(meta: EbookMeta, language: Libraries) {
        this.title = meta.title
        this.author = meta.author ?: ""
        this.annotation = meta.annotation ?: ""
        this.genre = meta.genre ?: ""
        this.fileSize = file.length()

        this.language = when (meta.lang) {
            "ja", "jp" -> Languages.JAPANESE
            "en" -> Languages.ENGLISH
            "pt" -> Languages.PORTUGUESE
            else -> when(language) {
                Libraries.ENGLISH -> Languages.ENGLISH
                Libraries.JAPANESE -> Languages.JAPANESE
                Libraries.PORTUGUESE -> Languages.PORTUGUESE
                else -> this.language
            }
        }
        this.fileType = FileUtil.getFileType(file.name)
        this.fileAlteration = Date(this.file.lastModified())
        this.lastVocabImport = null
    }

}
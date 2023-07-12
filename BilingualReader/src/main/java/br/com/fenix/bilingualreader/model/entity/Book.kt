package br.com.fenix.bilingualreader.model.entity

import androidx.room.*
import br.com.ebook.foobnix.ext.EbookMeta
import br.com.fenix.bilingualreader.model.enums.FileType
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.util.constants.DataBaseConsts
import br.com.fenix.bilingualreader.util.helpers.FileUtil
import br.com.fenix.bilingualreader.util.helpers.Util
import java.io.File
import java.io.Serializable
import java.util.*

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
    type: FileType,
    fileSize: Long,
    favorite: Boolean,
    fkLibrary: Long?,
    tags: MutableList<Long>,
    excluded: Boolean,
    dateCreate: Date?,
    lastAccess: Date?,
    lastAlteration: Date?,
    fileAlteration: Date,
    lastVocabImport: Date?,
    lastVerify: Date?
) : Serializable {

    @Ignore
    constructor(
        fkLibrary: Long?, title: String, author: String, annotation: String,
        year: String, genre: String, publisher: String, isbn: String,
        path: String, folder: String, name: String, fileSize: Long,
        pages: Int
    ) : this(
        null, title, author, "", annotation, year, genre, publisher, isbn, pages, 0, "", 0,
        Languages.ENGLISH, path, folder, name, FileType.UNKNOWN, fileSize, false, fkLibrary, mutableListOf(), false,
        Date(), null, Date(), Date(), null, null
    ) {
        this.type = FileUtil.getFileType(this.fileName)
        this.fileAlteration = Date(this.file.lastModified())
    }

    @Ignore
    constructor(fkLibrary: Long?, id: Long?, file: File, meta: EbookMeta, language: Libraries) : this(
        id, meta.title, meta.author ?: "", "", meta.annotation ?: "", "", meta.genre ?: "", "", "", 1, 0,
        "", 0, Languages.ENGLISH, file.path, file.parent, file.name, FileType.UNKNOWN, file.length(), false,
        fkLibrary, mutableListOf(), false, Date(), null, Date(), Date(), null, null
    ) {
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
        this.type = FileUtil.getFileType(file.name)
        this.fileAlteration = Date(this.file.lastModified())
    }

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.ID)
    var id: Long? = id

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.TITLE)
    var title: String = title

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
    var pages: Int = pages

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.CHAPTER)
    var chapter: Int = chapter

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.CHAPTER_DESCRIPTION)
    var chapterDescription: String = chapterDescription

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.BOOK_MARK)
    var bookMark: Int = bookMark

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.LANGUAGE)
    var language: Languages = language

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.FILE_PATH)
    var path: String = path

    @Ignore
    var file: File = File(path)

    @Ignore
    var fileName: String = Util.getNameWithoutExtensionFromPath(path)

    @Ignore
    var extension: String = Util.getExtensionFromPath(path)

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.FILE_SIZE)
    var fileSize: Long = fileSize

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.FILE_NAME)
    var name: String = name

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.FILE_TYPE)
    var type: FileType = type

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.FILE_FOLDER)
    var folder: String = folder

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.FAVORITE)
    var favorite: Boolean = favorite

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.EXCLUDED)
    var excluded: Boolean = excluded

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.FK_ID_LIBRARY)
    var fkLibrary: Long? = fkLibrary

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.TAGS)
    var tags: MutableList<Long> = tags

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.DATE_CREATE)
    var dateCreate: Date? = dateCreate

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.LAST_ACCESS)
    var lastAccess: Date? = lastAccess

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.LAST_ALTERATION)
    var lastAlteration: Date? = lastAlteration

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.FILE_ALTERATION)
    var fileAlteration: Date = fileAlteration

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.LAST_VOCABULARY_IMPORT)
    var lastVocabImport: Date? = lastVocabImport

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.LAST_VERIFY)
    var lastVerify: Date? = lastVerify

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
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + title.hashCode()
        result = 31 * result + author.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    fun update(book: Book) {
        this.bookMark = book.bookMark
        this.pages = book.pages
        this.tags = book.tags
        this.language = book.language
        this.favorite = book.favorite
        this.lastAccess = book.lastAccess
        this.lastAlteration = book.lastAlteration
        this.lastVocabImport = book.lastVocabImport
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
        this.type = FileUtil.getFileType(file.name)
        this.fileAlteration = Date(this.file.lastModified())
        this.lastVocabImport = null
    }

}
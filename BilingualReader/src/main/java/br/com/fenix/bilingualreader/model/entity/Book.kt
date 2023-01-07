package br.com.fenix.bilingualreader.model.entity

import androidx.room.*
import br.com.ebook.foobnix.ext.EbookMeta
import br.com.fenix.bilingualreader.model.enums.FileType
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.util.constants.DataBaseConsts
import br.com.fenix.bilingualreader.util.helpers.FileUtil
import br.com.fenix.bilingualreader.util.helpers.Util
import java.io.File
import java.io.Serializable
import java.time.LocalDateTime

@Entity(
    tableName = DataBaseConsts.BOOK.TABLE_NAME,
    indices = [Index(value = [DataBaseConsts.BOOK.COLUMNS.FILE_NAME, DataBaseConsts.BOOK.COLUMNS.TITLE])]
)
class Book(
    id: Long?,
    title: String,
    author: String,
    annotation: String,
    year: String,
    genre: String,
    publisher: String,
    isbn: String,
    pages: Int,
    bookMark: Int,
    language: Languages,
    path: String,
    fileSize: Long,
    name: String,
    type: FileType,
    folder: String,
    favorite: Boolean,
    dateCreate: LocalDateTime?,
    lastAccess: LocalDateTime?,
    lastAlteration: LocalDateTime?,
    excluded: Boolean
) : Serializable {

    constructor(
        title: String, author: String, annotation: String,
        year: String, genre: String, publisher: String, isbn: String,
        path: String, folder: String, name: String, fileSize: Long,
        pages: Int
    ) : this(
        null, title, author, annotation, year, genre, publisher, isbn, pages, 0,
        Languages.ENGLISH, path, fileSize, name, FileType.UNKNOWN, folder, false,
        LocalDateTime.now(), null, LocalDateTime.now(), false
    ) {
        this.file = File(path)
        this.fileName = Util.getNameWithoutExtensionFromPath(path)
        this.extension = Util.getExtensionFromPath(path)
        this.type = FileUtil.getFileType(this.fileName)
    }

    constructor( id: Long?, file: File, meta: EbookMeta) : this(
        id, meta.title, meta.author ?: "", meta.annotation ?: "", "", meta.genre ?: "", "", "", 0, 0,
        Languages.ENGLISH, file.path, file.length(), file.nameWithoutExtension, FileType.UNKNOWN, file.parent, false,
        LocalDateTime.now(), null, LocalDateTime.now(), false
    ) {
        this.language = when (meta.lang) {
            "ja", "jp" -> Languages.JAPANESE
            "en" -> Languages.ENGLISH
            else -> Languages.PORTUGUESE
        }

        this.file = File(path)
        this.fileName = Util.getNameWithoutExtensionFromPath(path)
        this.extension = Util.getExtensionFromPath(path)
        this.type = FileUtil.getFileType(this.fileName)
    }

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.ID)
    var id: Long? = id

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.TITLE)
    var title: String = title

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.AUTHOR)
    var author: String = author

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

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.BOOK_MARK)
    var bookMark: Int = bookMark

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.LANGUAGE)
    var language: Languages = language

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.FILE_PATH)
    var path: String = path

    @Ignore
    var file: File = File(path)

    @Ignore
    var fileName: String = ""

    @Ignore
    var extension: String = ""

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

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.DATE_CREATE)
    var dateCreate: LocalDateTime? = dateCreate

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.LAST_ACCESS)
    var lastAccess: LocalDateTime? = lastAccess

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.LAST_ALTERATION)
    var lastAlteration: LocalDateTime? = lastAlteration

    @ColumnInfo(name = DataBaseConsts.BOOK.COLUMNS.EXCLUDED)
    var excluded: Boolean = excluded

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
        this.favorite = book.favorite
        this.lastAccess = book.lastAccess
    }

}
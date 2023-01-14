package br.com.fenix.bilingualreader.model.entity

import androidx.room.*
import br.com.fenix.bilingualreader.util.constants.DataBaseConsts
import java.io.File
import java.io.Serializable
import java.time.LocalDateTime

@Entity(
    tableName = DataBaseConsts.MANGA.TABLE_NAME,
    indices = [Index(value = [DataBaseConsts.MANGA.COLUMNS.FILE_NAME, DataBaseConsts.MANGA.COLUMNS.TITLE])]
)
class Manga(
    id: Long?,
    title: String,
    subTitle: String,
    path: String,
    folder: String,
    name: String,
    type: String,
    pages: Int,
    chapters: IntArray,
    bookMark: Int,
    favorite: Boolean,
    hasSubtitle: Boolean,
    dateCreate: LocalDateTime?,
    lastAccess: LocalDateTime?,
    lastAlteration: LocalDateTime?,
    fileAlteration: Long,
    fkLibrary: Long?,
    excluded: Boolean
) : Serializable {

    constructor(
        id: Long?, title: String, subTitle: String,
        path: String, folder: String, name: String, type: String,
        pages: Int, chapters: IntArray, bookMark: Int, favorite: Boolean, hasSubtitle: Boolean,
        dateCreate: LocalDateTime?, lastAccess: LocalDateTime?,
        lastAlteration: LocalDateTime?, fileAlteration: Long,
        fkLibrary: Long?, sort: LocalDateTime? = null
    ) : this( id, title, subTitle, path, folder, name, type,
        pages, chapters, bookMark, favorite, hasSubtitle, dateCreate,
        lastAccess, lastAlteration, fileAlteration, fkLibrary, false
    ) {
        this.sort = sort
    }

    @Ignore
    constructor(
        id: Long?,
        title: String,
        subTitle: String,
        path: String,
        folder: String,
        name: String,
        type: String,
        pages: Int,
        chapters: IntArray,
        fkLibrary: Long?,
        fileAlteration: Long,
    ) : this(
        id, title, subTitle, path, folder, name, type,
        pages, chapters, 0, false, false, LocalDateTime.now(),
        null, null, fileAlteration, fkLibrary, false
    )


    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.ID)
    var id: Long? = id

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.TITLE)
    var title: String = title

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.SUB_TITLE)
    var subTitle: String = subTitle

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.PAGES)
    var pages: Int = pages

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.CHAPTERS)
    var chapters: IntArray = chapters

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.BOOK_MARK)
    var bookMark: Int = bookMark

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.FILE_PATH)
    var path: String = path

    @Ignore
    var fileName: String = title

    @Ignore
    var file: File = File(path)

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.FILE_NAME)
    var name: String = name

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.FILE_TYPE)
    var type: String = type

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.FILE_FOLDER)
    var folder: String = folder

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.FAVORITE)
    var favorite: Boolean = favorite

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.HAS_SUBTITLE)
    var hasSubtitle: Boolean = hasSubtitle

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.DATE_CREATE)
    var dateCreate: LocalDateTime? = dateCreate

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.LAST_ACCESS)
    var lastAccess: LocalDateTime? = lastAccess

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.LAST_ALTERATION)
    var lastAlteration: LocalDateTime? = lastAlteration

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.FILE_ALTERATION)
    var fileAlteration: Long = fileAlteration

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.EXCLUDED)
    var excluded: Boolean = excluded

    @ColumnInfo(name = DataBaseConsts.MANGA.COLUMNS.FK_ID_LIBRARY)
    var fkLibrary: Long? = fkLibrary

    @Ignore
    var library: Library = Library(null)

    @Ignore
    var update: Boolean = false

    @Ignore
    var subTitles: List<SubTitle> = arrayListOf()

    @Ignore
    var sort: LocalDateTime? = null

    override fun toString(): String {
        return "Book(id=$id, title='$title', subTitle='$subTitle', pages=$pages, bookMark=$bookMark, type='$type', update=$update)"
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

    fun update(manga: Manga) {
        this.bookMark = manga.bookMark
        this.favorite = manga.favorite
        this.lastAccess = manga.lastAccess
        this.hasSubtitle = manga.hasSubtitle
    }
}
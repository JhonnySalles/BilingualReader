package br.com.fenix.bilingualreader.model.entity

import android.graphics.Bitmap
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.fenix.bilingualreader.model.enums.MarkType
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.model.interfaces.Annotation
import br.com.fenix.bilingualreader.util.constants.DataBaseConsts
import java.io.Serializable
import java.time.LocalDateTime


@Entity(
    tableName = DataBaseConsts.MANGA_ANNOTATION.TABLE_NAME,
    indices = [Index(value = [DataBaseConsts.MANGA_ANNOTATION.COLUMNS.FK_ID_MANGA, DataBaseConsts.MANGA_ANNOTATION.COLUMNS.CHAPTER])]
)
data class MangaAnnotation(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = DataBaseConsts.MANGA_ANNOTATION.COLUMNS.ID)
    var id: Long?,
    @ColumnInfo(name = DataBaseConsts.MANGA_ANNOTATION.COLUMNS.FK_ID_MANGA)
    override val id_parent: Long,
    @ColumnInfo(name = DataBaseConsts.MANGA_ANNOTATION.COLUMNS.PAGE)
    var page: Int,
    @ColumnInfo(name = DataBaseConsts.MANGA_ANNOTATION.COLUMNS.PAGES)
    var pages: Int,
    @ColumnInfo(name = DataBaseConsts.MANGA_ANNOTATION.COLUMNS.TYPE)
    override val markType: MarkType,
    @ColumnInfo(name = DataBaseConsts.MANGA_ANNOTATION.COLUMNS.CHAPTER)
    override var chapter: String,
    @ColumnInfo(name = DataBaseConsts.MANGA_ANNOTATION.COLUMNS.FOLDER)
    var folder: String,
    @ColumnInfo(name = DataBaseConsts.MANGA_ANNOTATION.COLUMNS.ANNOTATION)
    override var annotation: String,
    @ColumnInfo(name = DataBaseConsts.MANGA_ANNOTATION.COLUMNS.ALTERATION)
    var alteration: LocalDateTime,
    @ColumnInfo(name = DataBaseConsts.MANGA_ANNOTATION.COLUMNS.CREATED)
    var created: LocalDateTime
) : Serializable, Annotation {

    //For a annotation title
    @Ignore
    override val type: Type = Type.MANGA
    @Ignore
    override var isRoot: Boolean = false
    @Ignore
    override var isTitle: Boolean = false
    @Ignore
    override var parent: Annotation? = null
    @Ignore
    override var count: Int = 0
    @Ignore
    var image: Bitmap? = null
    @Ignore
    var isSelected: Boolean = false

    //Not used in the type
    @Ignore
    override var chapterNumber: Float = 0f

    @Ignore
    constructor(
        id_manga: Long, page: Int, pages: Int, type: MarkType, chapter: String, folder: String, annotation: String
    ) : this(
        null, id_manga, page, pages, type, chapter, folder, annotation, LocalDateTime.now(), LocalDateTime.now()
    )
    
    @Ignore //For a annotation title
    constructor(id_manga: Long, chapter: String, folder: String, annotation: String, isRoot: Boolean = false, isTitle: Boolean = false) : this(
        null, id_manga, -1, -1, MarkType.PageMark, chapter, folder, annotation, LocalDateTime.now(), LocalDateTime.now()
    ) {
        this.isRoot = isRoot
        this.isTitle = isTitle
        this.count = 0
    }

    @Ignore
    constructor(other: MangaAnnotation) : this(
        other.id, other.id_parent, other.page, other.pages, other.markType, other.chapter, other.folder, other.annotation, other.alteration, other.created
    ) {
        this.count = other.count
        this.isTitle = other.isTitle
        this.isRoot = other.isRoot
        this.image = other.image
    }

    fun update(other: MangaAnnotation) {
        this.id = other.id
        this.page = other.page
        this.pages = other.pages
        this.annotation = other.annotation
        this.alteration = other.alteration
        this.created = other.created
        this.count = other.count
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MangaAnnotation

        if (id != other.id) return false
        if (id_parent != other.id_parent) return false
        if (page != other.page) return false
        if (pages != other.pages) return false
        if (isRoot != other.isRoot) return false
        if (isTitle != other.isTitle) return false
        if (chapter != other.chapter) return false
        if (alteration != other.alteration) return false
        if (created != other.created) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + id_parent.hashCode()
        result = 31 * result + page
        result = 31 * result + pages
        result = 31 * result + isRoot.hashCode()
        result = 31 * result + isTitle.hashCode()
        result = 31 * result + chapter.hashCode()
        result = 31 * result + alteration.hashCode()
        result = 31 * result + created.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

}
package br.com.fenix.bilingualreader.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.util.constants.DataBaseConsts
import java.io.File
import java.time.LocalDateTime


@Entity(
    tableName = DataBaseConsts.SUBTITLES.TABLE_NAME,
    indices = [Index(value = [DataBaseConsts.SUBTITLES.COLUMNS.LANGUAGE, DataBaseConsts.SUBTITLES.COLUMNS.FK_ID_MANGA])]
)
data class SubTitle(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = DataBaseConsts.SUBTITLES.COLUMNS.ID)
    var id: Long? = null,

    @ColumnInfo(name = DataBaseConsts.SUBTITLES.COLUMNS.FK_ID_MANGA)
    var id_manga: Long = 0,

    @ColumnInfo(name = DataBaseConsts.SUBTITLES.COLUMNS.LANGUAGE)
    var language: Languages,

    @ColumnInfo(name = DataBaseConsts.SUBTITLES.COLUMNS.CHAPTER_KEY)
    var chapterKey: String = "",

    @ColumnInfo(name = DataBaseConsts.SUBTITLES.COLUMNS.PAGE_KEY)
    var pageKey: String = "",

    @ColumnInfo(name = DataBaseConsts.SUBTITLES.COLUMNS.PAGE)
    var pageCount: Int = 0,

    @ColumnInfo(name = DataBaseConsts.SUBTITLES.COLUMNS.FILE_PATH)
    var path: String = "",

    @ColumnInfo(name = DataBaseConsts.SUBTITLES.COLUMNS.DATE_CREATE)
    var dateCreate: LocalDateTime? = LocalDateTime.now(),

    @ColumnInfo(name = DataBaseConsts.SUBTITLES.COLUMNS.LAST_ALTERATION)
    var lastAlteration: LocalDateTime? = LocalDateTime.now(),

    @Ignore
    var file: File = File(path),

    @Ignore
    var subTitleChapter: SubTitleChapter? = null,

    @Ignore
    var update: Boolean = false
) {

    constructor(
        id: Long? = 0,
        id_manga: Long = 0,
        language: Languages,
        chapterKey: String = "",
        pageKey: String = "",
        pageCount: Int = 0,
        path: String = "",
        dateCreate: LocalDateTime? = LocalDateTime.now(),
        lastAlteration: LocalDateTime? = LocalDateTime.now(),
    ) : this( id, id_manga, language, chapterKey, pageKey, pageCount, path, dateCreate, lastAlteration, File(path) )

    constructor(
        id_manga: Long = 0,
        language: Languages,
        chapterKey: String = "",
        pageKey: String = "",
        pageCount: Int = 0,
        path: String = "",
        subTitleChapter: SubTitleChapter?
    ) : this(
        null, id_manga, language, chapterKey, pageKey, pageCount, path, LocalDateTime.now(), LocalDateTime.now(), File(path)
    ) {
        this.subTitleChapter = subTitleChapter
    }

    override fun toString(): String {
        return "SubTitle(id=$id, id_manga=$id_manga, language=$language, chapterKey='$chapterKey', pageKey=$pageKey, pageCount=$pageCount, path='$path', dateCreate=$dateCreate, update=$update)"
    }

}
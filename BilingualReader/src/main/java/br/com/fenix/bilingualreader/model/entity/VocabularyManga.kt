package br.com.fenix.bilingualreader.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.fenix.bilingualreader.util.constants.DataBaseConsts

@Entity(
    tableName = DataBaseConsts.MANGA_VOCABULARY.TABLE_NAME,
    foreignKeys = [ForeignKey(
        entity = Manga::class,
        parentColumns = arrayOf(DataBaseConsts.MANGA.COLUMNS.ID),
        childColumns = arrayOf(DataBaseConsts.MANGA_VOCABULARY.COLUMNS.ID_MANGA),
        onDelete = ForeignKey.NO_ACTION,
        onUpdate = ForeignKey.NO_ACTION
    ), ForeignKey(
        entity = Vocabulary::class,
        parentColumns = arrayOf(DataBaseConsts.VOCABULARY.COLUMNS.ID),
        childColumns = arrayOf(DataBaseConsts.MANGA_VOCABULARY.COLUMNS.ID_VOCABULARY),
        onDelete = ForeignKey.NO_ACTION,
        onUpdate = ForeignKey.NO_ACTION
    )],
    indices = [Index(value = [DataBaseConsts.MANGA_VOCABULARY.COLUMNS.ID_MANGA, DataBaseConsts.MANGA_VOCABULARY.COLUMNS.ID_VOCABULARY])]
)
data class VocabularyManga(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = DataBaseConsts.MANGA_VOCABULARY.COLUMNS.ID)
    var id: Long?,
    @ColumnInfo(name = DataBaseConsts.MANGA_VOCABULARY.COLUMNS.ID_VOCABULARY, index = true)
    val idVocabulary: Long,
    @ColumnInfo(name = DataBaseConsts.MANGA_VOCABULARY.COLUMNS.ID_MANGA, index = true)
    val idManga: Long,
    @ColumnInfo(name = DataBaseConsts.MANGA_VOCABULARY.COLUMNS.APPEARS)
    var appears: Int
) {
    @Ignore
    var manga: Manga? = null
}
package br.com.fenix.bilingualreader.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.fenix.bilingualreader.util.constants.DataBaseConsts


@Entity(
    tableName = DataBaseConsts.BOOK_VOCABULARY.TABLE_NAME,
    foreignKeys = [ForeignKey(
        entity = Book::class,
        parentColumns = arrayOf(DataBaseConsts.BOOK.COLUMNS.ID),
        childColumns = arrayOf(DataBaseConsts.BOOK_VOCABULARY.COLUMNS.ID_BOOK),
        onDelete = ForeignKey.NO_ACTION,
        onUpdate = ForeignKey.NO_ACTION
    ), ForeignKey(
        entity = Vocabulary::class,
        parentColumns = arrayOf(DataBaseConsts.VOCABULARY.COLUMNS.ID),
        childColumns = arrayOf(DataBaseConsts.BOOK_VOCABULARY.COLUMNS.ID_VOCABULARY),
        onDelete = ForeignKey.NO_ACTION,
        onUpdate = ForeignKey.NO_ACTION
    )],
    indices = [Index(value = [DataBaseConsts.BOOK_VOCABULARY.COLUMNS.ID_BOOK, DataBaseConsts.BOOK_VOCABULARY.COLUMNS.ID_VOCABULARY])]
)
data class VocabularyBook(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = DataBaseConsts.BOOK_VOCABULARY.COLUMNS.ID)
    var id: Long?,
    @ColumnInfo(name = DataBaseConsts.BOOK_VOCABULARY.COLUMNS.ID_VOCABULARY, index = true)
    val idVocabulary: Long,
    @ColumnInfo(name = DataBaseConsts.BOOK_VOCABULARY.COLUMNS.ID_BOOK, index = true)
    val idBook: Long,
    @ColumnInfo(name = DataBaseConsts.BOOK_VOCABULARY.COLUMNS.APPEARS)
    var appears: Int
) {
    @Ignore
    var book: Book? = null
}
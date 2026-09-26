package br.com.fenix.bilingualreader.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Entity(tableName = "context_chunks_fts")
@Fts4
data class ContextChunkFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowid: Int = 0,

    @ColumnInfo(name = "chapterTitle")
    val chapterTitle: String,

    @ColumnInfo(name = "text")
    val text: String,

    @ColumnInfo(name = "pageNumber")
    val pageNumber: Int
)

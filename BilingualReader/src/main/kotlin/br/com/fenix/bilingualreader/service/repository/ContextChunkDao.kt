package br.com.fenix.bilingualreader.service.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.fenix.bilingualreader.model.entity.ContextChunkFtsEntity

@Dao
interface ContextChunkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(chunks: List<ContextChunkFtsEntity>)

    @Query("SELECT rowid, * FROM context_chunks_fts WHERE context_chunks_fts MATCH :query LIMIT :limit")
    fun searchChunks(query: String, limit: Int = 3): List<ContextChunkFtsEntity>

    @Query("SELECT rowid, * FROM context_chunks_fts LIMIT :limit")
    fun getAll(limit: Int = 10): List<ContextChunkFtsEntity>

    @Query("DELETE FROM context_chunks_fts")
    fun clearAll()
}

package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import br.com.fenix.bilingualreader.model.entity.ContextChunkFtsEntity

@Database(entities = [ContextChunkFtsEntity::class], version = 1, exportSchema = false)
abstract class RAGContextDatabase : RoomDatabase() {
    abstract fun contextChunkDao(): ContextChunkDao

    companion object {
        @Volatile
        private var INSTANCE: RAGContextDatabase? = null

        fun getInstance(context: Context): RAGContextDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.inMemoryDatabaseBuilder(
                    context.applicationContext,
                    RAGContextDatabase::class.java
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

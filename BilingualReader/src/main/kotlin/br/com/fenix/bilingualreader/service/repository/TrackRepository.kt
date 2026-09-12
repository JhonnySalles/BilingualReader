package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.Track
import br.com.fenix.bilingualreader.util.helpers.Telemetry
import org.slf4j.LoggerFactory

class TrackRepository(var context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(TrackRepository::class.java)
    private var mDataBase = DataBase.getDataBase(context).getTrackDao()

    fun save(obj: Track): Long {
        return try {
            mDataBase.save(obj)
        } catch (e: Exception) {
            mLOGGER.error("Error when save Track: ${e.message}", e)
            Telemetry.recordException(e, "Error when save Track: ${e.message}")
            0L
        }
    }

    fun update(obj: Track): Int {
        return try {
            mDataBase.update(obj)
        } catch (e: Exception) {
            mLOGGER.error("Error when update Track: ${e.message}", e)
            Telemetry.recordException(e, "Error when update Track: ${e.message}")
            0
        }
    }

    fun delete(obj: Track) {
        try {
            if (obj.id != null) {
                mDataBase.deleteById(obj.id!!)
            } else {
                mDataBase.delete(obj)
            }
        } catch (e: Exception) {
            mLOGGER.error("Error when delete Track: ${e.message}", e)
            Telemetry.recordException(e, "Error when delete Track: ${e.message}")
        }
    }

    fun deleteById(id: Long) {
        try {
            mDataBase.deleteById(id)
        } catch (e: Exception) {
            mLOGGER.error("Error when delete Track by id: ${e.message}", e)
            Telemetry.recordException(e, "Error when delete Track by id: ${e.message}")
        }
    }

    fun get(id: Long): Track? {
        return try {
            mDataBase.get(id)
        } catch (e: Exception) {
            mLOGGER.error("Error when get Track: ${e.message}", e)
            Telemetry.recordException(e, "Error when get Track: ${e.message}")
            null
        }
    }

    fun listAll(): List<Track> {
        return try {
            mDataBase.listAll()
        } catch (e: Exception) {
            mLOGGER.error("Error when list all Tracks: ${e.message}", e)
            Telemetry.recordException(e, "Error when list all Tracks: ${e.message}")
            emptyList()
        }
    }

    fun listByLibrary(idLibrary: Long): List<Track> {
        return try {
            mDataBase.listByLibrary(idLibrary)
        } catch (e: Exception) {
            mLOGGER.error("Error when list Track by library: ${e.message}", e)
            Telemetry.recordException(e, "Error when list Track by library: ${e.message}")
            emptyList()
        }
    }

    fun findByMalId(malId: Long): Track? {
        return try {
            mDataBase.findByMalId(malId)
        } catch (e: Exception) {
            mLOGGER.error("Error when find Track by malId: ${e.message}", e)
            Telemetry.recordException(e, "Error when find Track by malId: ${e.message}")
            null
        }
    }

    fun findByAniId(aniId: Long): Track? {
        return try {
            mDataBase.findByAniId(aniId)
        } catch (e: Exception) {
            mLOGGER.error("Error when find Track by aniId: ${e.message}", e)
            Telemetry.recordException(e, "Error when find Track by aniId: ${e.message}")
            null
        }
    }

    fun findByLibraryAndMalId(idLibrary: Long, malId: Long): Track? {
        return try {
            mDataBase.findByLibraryAndMalId(idLibrary, malId)
        } catch (e: Exception) {
            mLOGGER.error("Error when find Track by library and malId: ${e.message}", e)
            Telemetry.recordException(e, "Error when find Track by library and malId: ${e.message}")
            null
        }
    }

    fun findByLibraryAndAniId(idLibrary: Long, aniId: Long): Track? {
        return try {
            mDataBase.findByLibraryAndAniId(idLibrary, aniId)
        } catch (e: Exception) {
            mLOGGER.error("Error when find Track by library and aniId: ${e.message}", e)
            Telemetry.recordException(e, "Error when find Track by library and aniId: ${e.message}")
            null
        }
    }

    fun deleteByLibrary(idLibrary: Long) {
        try {
            mDataBase.deleteByLibrary(idLibrary)
        } catch (e: Exception) {
            mLOGGER.error("Error when delete Track by library: ${e.message}", e)
            Telemetry.recordException(e, "Error when delete Track by library: ${e.message}")
        }
    }
}

package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Date
import java.util.Locale

class MangaRepository(context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(MangaRepository::class.java)
    private var mDataBase = DataBase.getDataBase(context).getMangaDao()
    private var mLibrary = DataBase.getDataBase(context).getLibrariesDao()

    fun save(obj: Manga): Long {
        obj.lastAlteration = LocalDateTime.now()
        return mDataBase.save(obj)
    }

    fun update(obj: Manga) {
        obj.lastAlteration = LocalDateTime.now()
        mDataBase.update(obj)
    }

    fun updateBookMark(obj: Manga) {
        obj.lastAlteration = LocalDateTime.now()
        if (obj.id != null)
            mDataBase.updateBookMark(obj.id!!, obj.bookMark)
    }

    fun updateLastAccess(obj: Manga) {
        obj.lastAlteration = LocalDateTime.now()
        obj.lastAccess = LocalDateTime.now()
        if (obj.id != null)
            mDataBase.update(obj)
    }

    fun delete(obj: Manga) {
        obj.lastAlteration = LocalDateTime.now()
        if (obj.id != null)
            mDataBase.delete(obj.id!!)
    }

    fun deletePermanent(obj: Manga) {
        obj.lastAlteration = LocalDateTime.now()
        if (obj.id != null)
            mDataBase.delete(obj)
    }

    fun list(library: Library): List<Manga>? {
        return try {
            loadLibrary(mDataBase.list(library.id))
        } catch (e: Exception) {
            mLOGGER.error("Error when list Manga: " + e.message, e)
            null
        }
    }

    fun listRecentChange(library: Library): List<Manga>? {
        return try {
            loadLibrary(mDataBase.listRecentChange(library.id))
        } catch (e: Exception) {
            mLOGGER.error("Error when list Manga: " + e.message, e)
            null
        }
    }

    fun listRecentDeleted(library: Library): List<Manga>? {
        return try {
            loadLibrary(mDataBase.listRecentDeleted(library.id))
        } catch (e: Exception) {
            mLOGGER.error("Error when list Manga: " + e.message, e)
            null
        }
    }

    fun listDeleted(library: Library): List<Manga>? {
        return try {
            loadLibrary(mDataBase.listDeleted(library.id))
        } catch (e: Exception) {
            mLOGGER.error("Error when list Manga: " + e.message, e)
            null
        }
    }

    fun listHistory(): List<Manga>? {
        return try {
            loadLibrary(mDataBase.listHistory())
        } catch (e: Exception) {
            mLOGGER.error("Error when list Manga History: " + e.message, e)
            null
        }
    }

    fun markRead(obj: Manga?) {
        try {
            if (obj != null) {
                obj.lastAlteration = LocalDateTime.now()
                obj.lastAccess = LocalDateTime.now()
                obj.bookMark = obj.pages
                if (obj.id != null)
                    mDataBase.update(obj)
            }
        } catch (e: Exception) {
            mLOGGER.error("Error when mark read Manga: " + e.message, e)
        }
    }

    fun clearHistory(obj: Manga?) {
        try {
            if (obj != null) {
                obj.lastAlteration = LocalDateTime.now()
                obj.lastAccess = null
                obj.bookMark = 0
                obj.favorite = false
                if (obj.id != null)
                    mDataBase.update(obj)
            }
        } catch (e: Exception) {
            mLOGGER.error("Error when clear Manga History: " + e.message, e)
        }
    }

    fun get(id: Long): Manga? {
        return try {
            loadLibrary(mDataBase.get(id))
        } catch (e: Exception) {
            mLOGGER.error("Error when get Manga: " + e.message, e)
            null
        }
    }

    fun findByFileName(name: String): Manga? {
        return try {
            loadLibrary(mDataBase.getByFileName(name))
        } catch (e: Exception) {
            mLOGGER.error("Error when find Manga by file name: " + e.message, e)
            null
        }
    }

    fun findByFilePath(name: String): Manga? {
        return try {
            loadLibrary(mDataBase.getByPath(name))
        } catch (e: Exception) {
            mLOGGER.error("Error when find Manga by file name: " + e.message, e)
            null
        }
    }

    fun findByFileFolder(folder: String): List<Manga>? {
        return try {
            loadLibrary(mDataBase.listByFolder(folder))
        } catch (e: Exception) {
            mLOGGER.error("Error when find Manga by file folder: " + e.message, e)
            null
        }
    }

    fun listOrderByTitle(library: Library): List<Manga>? {
        return try {
            loadLibrary(mDataBase.listOrderByTitle(library.id))
        } catch (e: Exception) {
            mLOGGER.error("Error when find Manga by file folder: " + e.message, e)
            null
        }
    }

    fun getLastedRead() : Pair<Manga?, Manga?> {
        return try {
            val last = mDataBase.getLastOpen()

            return if (last != null && last.isNotEmpty()) {
                val first = last[0]
                val second = if(last.size > 1) last[1] else null
                Pair (first, second)
            } else
                Pair(null, null)
        } catch (e: Exception) {
            mLOGGER.error("Error when find last Manga open: " + e.message, e)
            Pair(null, null)
        }
    }

    fun listSync(date: Date): List<Manga> {
        return try {
            val simple = SimpleDateFormat(GeneralConsts.PATTERNS.FULL_DATE_TIME, Locale.getDefault())
            loadLibrary(mDataBase.listSync(simple.format(date)))
        } catch (e: Exception) {
            mLOGGER.error("Error when list Manga: " + e.message, e)
            listOf()
        }
    }

    private fun loadLibrary(manga: Manga?) : Manga? {
        manga ?: return null
        if (manga.fkLibrary != GeneralConsts.KEYS.LIBRARY.DEFAULT_MANGA)
            manga.library = mLibrary.get(manga.fkLibrary!!)
        return manga
    }

    private fun loadLibrary(list: List<Manga>) : List<Manga> {
        list.forEach { loadLibrary(it) }
        return list
    }

}
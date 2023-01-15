package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class BookRepository(context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(BookRepository::class.java)
    private var mDataBase = DataBase.getDataBase(context).getBookDao()

    fun save(obj: Book): Long {
        obj.lastAlteration = LocalDateTime.now()
        return mDataBase.save(obj)
    }

    fun update(obj: Book) {
        obj.lastAlteration = LocalDateTime.now()
        mDataBase.update(obj)
    }

    fun updateBookMark(obj: Book) {
        obj.lastAlteration = LocalDateTime.now()
        if (obj.id != null)
            mDataBase.updateBookMark(obj.id!!, obj.bookMark)
    }

    fun updateLastAccess(obj: Book) {
        obj.lastAlteration = LocalDateTime.now()
        obj.lastAccess = LocalDateTime.now()
        if (obj.id != null)
            mDataBase.update(obj)
    }

    fun delete(obj: Book) {
        obj.lastAlteration = LocalDateTime.now()
        if (obj.id != null)
            mDataBase.delete(obj.id!!)
    }

    fun deletePermanent(obj: Book) {
        obj.lastAlteration = LocalDateTime.now()
        if (obj.id != null)
            mDataBase.delete(obj)
    }

    fun list(library: Library): List<Book>? {
        return try {
            mDataBase.list(library.id)
        } catch (e: Exception) {
            mLOGGER.error("Error when list Book: " + e.message, e)
            null
        }
    }

    fun listRecentChange(library: Library): List<Book>? {
        return try {
            mDataBase.listRecentChange(library.id)
        } catch (e: Exception) {
            mLOGGER.error("Error when list Book: " + e.message, e)
            null
        }
    }

    fun listRecentDeleted(library: Library): List<Book>? {
        return try {
            mDataBase.listRecentDeleted(library.id)
        } catch (e: Exception) {
            mLOGGER.error("Error when list Book: " + e.message, e)
            null
        }
    }

    fun listDeleted(library: Library): List<Book>? {
        return try {
            mDataBase.listDeleted(library.id)
        } catch (e: Exception) {
            mLOGGER.error("Error when list Book: " + e.message, e)
            null
        }
    }

    fun markRead(obj: Book?) {
        try {
            if (obj != null) {
                obj.lastAlteration = LocalDateTime.now()
                obj.lastAccess = LocalDateTime.now()
                obj.bookMark = obj.pages
                if (obj.id != null)
                    mDataBase.update(obj)
            }
        } catch (e: Exception) {
            mLOGGER.error("Error when mark read Book: " + e.message, e)
        }
    }

    fun clearHistory(obj: Book?) {
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
            mLOGGER.error("Error when clear Book History: " + e.message, e)
        }
    }

    fun get(id: Long): Book? {
        return try {
            mDataBase.get(id)
        } catch (e: Exception) {
            mLOGGER.error("Error when get Book: " + e.message, e)
            null
        }
    }

    fun findByFileName(name: String): Book? {
        return try {
            mDataBase.get(name)
        } catch (e: Exception) {
            mLOGGER.error("Error when find Book by file name: " + e.message, e)
            null
        }
    }

    fun findByFilePath(name: String): Book? {
        return try {
            mDataBase.getByPath(name)
        } catch (e: Exception) {
            mLOGGER.error("Error when find Book by file name: " + e.message, e)
            null
        }
    }

    fun findByFileFolder(folder: String): List<Book>? {
        return try {
            mDataBase.listByFolder(folder)
        } catch (e: Exception) {
            mLOGGER.error("Error when find Book by file folder: " + e.message, e)
            null
        }
    }

    fun listOrderByTitle(library: Library): List<Book>? {
        return try {
            mDataBase.listOrderByTitle(library.id)
        } catch (e: Exception) {
            mLOGGER.error("Error when find Book by file folder: " + e.message, e)
            null
        }
    }

    fun getLastedRead(): Pair<Book?, Book?> {
        return try {
            val last = mDataBase.getLastOpen()

            return if (last != null && last.isNotEmpty()) {
                val first = last[0]
                val second = if (last.size > 1) last[1] else null
                Pair(first, second)
            } else
                Pair(null, null)
        } catch (e: Exception) {
            mLOGGER.error("Error when find last Book open: " + e.message, e)
            Pair(null, null)
        }
    }

}
package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.BookConfiguration
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.Date

class BookRepository(context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(BookRepository::class.java)
    private var mDataBase = DataBase.getDataBase(context).getBookDao()
    private var mConfiguration = DataBase.getDataBase(context).getBookConfigurationDao()
    private var mLibrary = DataBase.getDataBase(context).getLibrariesDao()

    // --------------------------------------------------------- BOOK ---------------------------------------------------------
    fun save(obj: Book, lastAlteration: LocalDateTime? = LocalDateTime.now()): Long {
        if (lastAlteration != null)
            obj.lastAlteration = lastAlteration
        return mDataBase.save(obj)
    }

    fun update(obj: Book, lastAlteration: LocalDateTime? = LocalDateTime.now()) {
        if (lastAlteration != null)
            obj.lastAlteration = lastAlteration
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

    fun list(library: Library): List<Book> {
        return try {
            loadLibrary(mDataBase.list(library.id))
        } catch (e: Exception) {
            mLOGGER.error("Error when list Book: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when list Book: " + e.message)
                recordException(e)
            }
            listOf()
        }
    }

    fun listRecentChange(library: Library): List<Book> {
        return try {
            loadLibrary(mDataBase.listRecentChange(library.id))
        } catch (e: Exception) {
            mLOGGER.error("Error when list recent change Book: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when list recent change Book: " + e.message)
                recordException(e)
            }
            listOf()
        }
    }

    fun listRecentDeleted(library: Library): List<Book> {
        return try {
            loadLibrary(mDataBase.listRecentDeleted(library.id))
        } catch (e: Exception) {
            mLOGGER.error("Error when list recent deleted Book: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when list recent deleted Book: " + e.message)
                recordException(e)
            }
            listOf()
        }
    }

    fun listDeleted(library: Library): List<Book> {
        return try {
            loadLibrary(mDataBase.listDeleted(library.id))
        } catch (e: Exception) {
            mLOGGER.error("Error when list deleted Book: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when list deleted Book: " + e.message)
                recordException(e)
            }
            listOf()
        }
    }

    fun listHistory(): List<Book>? {
        return try {
            loadLibrary(mDataBase.listHistory())
        } catch (e: Exception) {
            mLOGGER.error("Error when list Book History: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when list Book History: " + e.message)
                recordException(e)
            }
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
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when mark read Book: " + e.message)
                recordException(e)
            }
        }
    }

    fun clearHistory(obj: Book?) {
        try {
            if (obj != null) {
                obj.lastAlteration = LocalDateTime.now()
                obj.lastAccess = null
                obj.bookMark = 0
                obj.completed = false
                obj.favorite = false
                if (obj.id != null)
                    mDataBase.update(obj)
            }
        } catch (e: Exception) {
            mLOGGER.error("Error when clear Book History: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when clear Book History: " + e.message)
                recordException(e)
            }
        }
    }

    fun get(id: Long): Book? {
        return try {
            loadLibrary(mDataBase.get(id))
        } catch (e: Exception) {
            mLOGGER.error("Error when get Book: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when get Book: " + e.message)
                recordException(e)
            }
            null
        }
    }

    fun findByFileName(name: String): Book? {
        return try {
            loadLibrary(mDataBase.getByFileName(name))
        } catch (e: Exception) {
            mLOGGER.error("Error when find Book by file name: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when find Book by file name: " + e.message)
                recordException(e)
            }
            null
        }
    }

    fun findByFilePath(name: String): Book? {
        return try {
            loadLibrary(mDataBase.getByPath(name))
        } catch (e: Exception) {
            mLOGGER.error("Error when find Book by file name: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when find Book by file name: " + e.message)
                recordException(e)
            }
            null
        }
    }

    fun findByFileFolder(folder: String): List<Book>? {
        return try {
            loadLibrary(mDataBase.listByFolder(folder))
        } catch (e: Exception) {
            mLOGGER.error("Error when find Book by file folder: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when find Book by file folder: " + e.message)
                recordException(e)
            }
            null
        }
    }

    fun listOrderByPath(library: Library): List<Book>? {
        return try {
            loadLibrary(mDataBase.listOrderByPath(library.id))
        } catch (e: Exception) {
            mLOGGER.error("Error when find Book by file folder: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when find Book by file folder: " + e.message)
                recordException(e)
            }
            null
        }
    }

    fun listSync(date: Date): List<Book> {
        return try {
            loadLibrary(mDataBase.listSync(GeneralConsts.dateToDateTime(date)))
        } catch (e: Exception) {
            mLOGGER.error("Error when list Book to sync: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when list Book to sync: " + e.message)
                recordException(e)
            }
            listOf()
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
            mLOGGER.error("Error when find last read Book: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when find last read Book: " + e.message)
                recordException(e)
            }
            Pair(null, null)
        }
    }

    private fun loadLibrary(book: Book?) : Book? {
        book ?: return null

        if (book.fkLibrary != GeneralConsts.KEYS.LIBRARY.DEFAULT_BOOK)
            book.library = mLibrary.get(book.fkLibrary!!)
        return book
    }

    private fun loadLibrary(list: List<Book>) : List<Book> {
        list.forEach { loadLibrary(it) }
        return list
    }

    // --------------------------------------------------------- Configuration ---------------------------------------------------------

    fun saveConfiguration(config: BookConfiguration) {
        mConfiguration.save(config)
    }

    fun updateConfiguration(config: BookConfiguration) {
        mConfiguration.update(config)
    }

    fun findConfiguration(idBook: Long): BookConfiguration? {
        return try {
            mConfiguration.findByBook(idBook)
        } catch (e: Exception) {
            mLOGGER.error("Error when find Book Configuration: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when find Book Configuration: " + e.message)
                recordException(e)
            }
            null
        }
    }

}
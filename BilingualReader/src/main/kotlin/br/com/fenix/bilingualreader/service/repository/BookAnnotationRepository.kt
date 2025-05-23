package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class BookAnnotationRepository(context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(BookAnnotationRepository::class.java)
    private var mDataBase = DataBase.getDataBase(context).getBookAnnotation()

    // --------------------------------------------------------- BOOK ANNOTATION ---------------------------------------------------------

    fun find(id: Long): BookAnnotation? = mDataBase.find(id)

    fun save(obj: BookAnnotation): Long {
        obj.alteration = LocalDateTime.now()
        obj.id = mDataBase.save(obj)
        return obj.id!!
    }

    fun update(obj: BookAnnotation) {
        obj.alteration = LocalDateTime.now()
        mDataBase.update(obj)
    }

    fun delete(obj: BookAnnotation) {
        if (obj.id != null)
            mDataBase.delete(obj)
    }

    fun findAllOrderByBook(): List<BookAnnotation> {
        return try {
            mDataBase.findAllOrderByBook()
        } catch (e: Exception) {
            mLOGGER.error("Error when list annotation of Book: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when list annotation of Book: " + e.message)
                recordException(e)
            }
            arrayListOf()
        }
    }

    fun findAll(idBook: Long): List<BookAnnotation> {
        return try {
            mDataBase.findAllByBook(idBook)
        } catch (e: Exception) {
            mLOGGER.error("Error when list annotation of Book: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when list annotation of Book: " + e.message)
                recordException(e)
            }
            arrayListOf()
        }
    }

    fun findByBook(idBook: Long): List<BookAnnotation> {
        return try {
            mDataBase.findByBook(idBook)
        } catch (e: Exception) {
            mLOGGER.error("Error when find annotation by book: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when find annotation by book: " + e.message)
                recordException(e)
            }
            arrayListOf()
        }
    }

    fun findByPage(idBook: Long, page: Int): List<BookAnnotation> {
        return try {
            mDataBase.findByPage(idBook, page)
        } catch (e: Exception) {
            mLOGGER.error("Error when find annotation by page: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when find annotation by page: " + e.message)
                recordException(e)
            }
            arrayListOf()
        }
    }

}
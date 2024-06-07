package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class BookAnnotationRepository(context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(BookAnnotationRepository::class.java)
    private var mDataBase = DataBase.getDataBase(context).getBookAnnotation()

    // --------------------------------------------------------- BOOK ANNOTATION ---------------------------------------------------------
    fun save(obj: BookAnnotation) {
        obj.alteration = LocalDateTime.now()
        mDataBase.save(obj)
    }

    fun update(obj: BookAnnotation) {
        obj.alteration = LocalDateTime.now()
        mDataBase.update(obj)
    }

    fun delete(obj: BookAnnotation) {
        if (obj.id != null)
            mDataBase.delete(obj)
    }

    fun findAll(idBook: Long): List<BookAnnotation> {
        return try {
            mDataBase.findAll(idBook)
        } catch (e: Exception) {
            mLOGGER.error("Error when list annotation of Book: " + e.message, e)
            arrayListOf()
        }
    }

    fun list(idBook: Long): List<BookAnnotation> {
        return try {
            mDataBase.list(idBook)
        } catch (e: Exception) {
            mLOGGER.error("Error when list annotation of Book: " + e.message, e)
            arrayListOf()
        }
    }

    fun findByBook(idBook: Long): List<BookAnnotation> {
        return try {
            mDataBase.findByBook(idBook)
        } catch (e: Exception) {
            mLOGGER.error("Error when find annotation by book: " + e.message, e)
            arrayListOf()
        }
    }

    fun findByPage(idBook: Long, page: Int): List<BookAnnotation> {
        return try {
            mDataBase.findByPage(idBook, page)
        } catch (e: Exception) {
            mLOGGER.error("Error when find annotation by page: " + e.message, e)
            arrayListOf()
        }
    }

}
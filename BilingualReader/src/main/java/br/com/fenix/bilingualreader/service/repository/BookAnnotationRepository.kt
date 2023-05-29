package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

class BookAnnotationRepository(context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(BookAnnotationRepository::class.java)
    private var mDataBase = DataBase.getDataBase(context).getBookAnnotation()

    // --------------------------------------------------------- BOOK ANNOTATION ---------------------------------------------------------
    fun save(obj: BookAnnotation) {
        mDataBase.save(obj)
    }

    fun update(obj: BookAnnotation) {
        obj.alteration = Date()
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
            mLOGGER.error("Error when list Book Annotations: " + e.message, e)
            arrayListOf()
        }
    }

    fun list(idBook: Long): List<BookAnnotation> {
        return try {
            mDataBase.list(idBook)
        } catch (e: Exception) {
            mLOGGER.error("Error when list Book: " + e.message, e)
            arrayListOf()
        }
    }

}
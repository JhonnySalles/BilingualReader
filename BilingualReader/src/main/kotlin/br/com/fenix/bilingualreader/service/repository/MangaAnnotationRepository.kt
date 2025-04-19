package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.MangaAnnotation
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class MangaAnnotationRepository(context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(MangaAnnotationRepository::class.java)
    private var mDataBase = DataBase.getDataBase(context).getMangaAnnotation()

    // --------------------------------------------------------- BOOK ANNOTATION ---------------------------------------------------------

    fun find(id: Long): MangaAnnotation? = mDataBase.find(id)

    fun save(obj: MangaAnnotation): Long {
        obj.alteration = LocalDateTime.now()
        obj.id = mDataBase.save(obj)
        return obj.id!!
    }

    fun update(obj: MangaAnnotation) {
        obj.alteration = LocalDateTime.now()
        mDataBase.update(obj)
    }

    fun delete(obj: MangaAnnotation) {
        if (obj.id != null)
            mDataBase.delete(obj)
    }

    fun findAllOrderByManga(): List<MangaAnnotation> {
        return try {
            mDataBase.findAllOrderByManga()
        } catch (e: Exception) {
            mLOGGER.error("Error when list annotation of Manga: " + e.message, e)
            arrayListOf()
        }
    }

    fun findAll(idManga: Long): List<MangaAnnotation> {
        return try {
            mDataBase.findAllByManga(idManga)
        } catch (e: Exception) {
            mLOGGER.error("Error when list annotation of Manga: " + e.message, e)
            arrayListOf()
        }
    }

    fun findByManga(idManga: Long): List<MangaAnnotation> {
        return try {
            mDataBase.findByManga(idManga)
        } catch (e: Exception) {
            mLOGGER.error("Error when find annotation by manga: " + e.message, e)
            arrayListOf()
        }
    }

    fun findByPage(idManga: Long, page: Int): List<MangaAnnotation> {
        return try {
            mDataBase.findByPage(idManga, page)
        } catch (e: Exception) {
            mLOGGER.error("Error when find annotation by page: " + e.message, e)
            arrayListOf()
        }
    }

}
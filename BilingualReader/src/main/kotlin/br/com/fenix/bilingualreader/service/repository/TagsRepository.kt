package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Tags
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import org.slf4j.LoggerFactory

class TagsRepository(context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(TagsRepository::class.java)
    private var mDataBase = DataBase.getDataBase(context).getTagsDao()
    private var mBookDataBase = DataBase.getDataBase(context).getBookDao()

    fun save(book: Book) {
        mBookDataBase.update(book)
    }

    fun save(tag: Tags): Long {
        return if (tag.id == null)
            mDataBase.save(tag)
        else {
            mDataBase.update(tag)
            tag.id!!
        }
    }

    fun delete(tag: Tags) = mDataBase.delete(tag)

    fun get(id: Long): Tags? {
        return try {
            mDataBase.get(id)
        } catch (e: Exception) {
            mLOGGER.error("Error when get Tag: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when get Tag: " + e.message)
                recordException(e)
            }
            null
        }
    }

    fun get(nome: String): Tags? {
        return try {
            mDataBase.get(nome)
        } catch (e: Exception) {
            mLOGGER.error("Error when get Tag: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when get Tag: " + e.message)
                recordException(e)
            }
            null
        }
    }

    fun valid(nome: String): Boolean {
        return try {
            mDataBase.valid(nome) == null
        } catch (e: Exception) {
            mLOGGER.error("Error when valid Tag: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when valid Tag: " + e.message)
                recordException(e)
            }
            true
        }
    }

    fun list(): MutableList<Tags> {
        return try {
            mDataBase.list() ?: mutableListOf()
        } catch (e: Exception) {
            mLOGGER.error("Error when list Tag: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when list Tag: " + e.message)
                recordException(e)
            }
            mutableListOf()
        }
    }

}
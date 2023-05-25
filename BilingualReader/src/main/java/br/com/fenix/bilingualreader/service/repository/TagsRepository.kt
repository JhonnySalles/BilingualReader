package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Kanjax
import br.com.fenix.bilingualreader.model.entity.Tags
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
            null
        }
    }

    fun get(nome: String): Tags? {
        return try {
            mDataBase.get(nome)
        } catch (e: Exception) {
            mLOGGER.error("Error when get Tag: " + e.message, e)
            null
        }
    }

    fun valid(nome: String): Boolean {
        return try {
            mDataBase.valid(nome) == null
        } catch (e: Exception) {
            mLOGGER.error("Error when valid Tag: " + e.message, e)
            true
        }
    }

    fun list(): MutableList<Tags> {
        return try {
            mDataBase.list() ?: mutableListOf()
        } catch (e: Exception) {
            mLOGGER.error("Error when list Tag: " + e.message, e)
            mutableListOf()
        }
    }

}
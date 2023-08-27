package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.util.helpers.LibraryUtil
import org.slf4j.LoggerFactory

class LibraryRepository(var context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(LibraryRepository::class.java)
    private var mDataBase = DataBase.getDataBase(context).getLibrariesDao()

    fun save(obj: Library): Long {
        return mDataBase.save(obj)
    }

    fun update(obj: Library) {
        mDataBase.update(obj)
    }

    fun delete(obj: Library) {
        if (obj.id != null)
            mDataBase.delete(obj.id!!)
    }

    fun list(type: Type?): List<Library> {
        return try {
            if (type != null)
                mDataBase.list().filter { it.type == type }
            else
                mDataBase.list()
        } catch (e: Exception) {
            mLOGGER.error("Error when list Library: " + e.message, e)
            listOf()
        }
    }

    fun listEnabled(): List<Library> {
        return try {
            mDataBase.listEnabled()
        } catch (e: Exception) {
            mLOGGER.error("Error when list Library: " + e.message, e)
            listOf()
        }
    }

    fun get(id: Long): Library? {
        return try {
            mDataBase.get(id)
        } catch (e: Exception) {
            mLOGGER.error("Error when get Library: " + e.message, e)
            null
        }
    }

    fun get(type: Type, language: Libraries): Library? {
        return try {
            mDataBase.get(type, language)
        } catch (e: Exception) {
            mLOGGER.error("Error when get Library: " + e.message, e)
            null
        }
    }

    fun findDeleted(path: String): Library? {
        return try {
            mDataBase.findDeleted(path)
        } catch (e: Exception) {
            mLOGGER.error("Error when find Library: " + e.message, e)
            null
        }
    }

    fun removeDefault(path: String) {
        try {
            mDataBase.removeDefault(path)
        } catch (e: Exception) {
            mLOGGER.error("Error when remove Default Library: " + e.message, e)
        }
    }

    fun getDefault(type: Type): String {
        return try {
            LibraryUtil.getDefault(context, type).path
        } catch (e: Exception) {
            mLOGGER.error("Error when get Default Library: " + e.message, e)
            ""
        }
    }

    fun saveDefault(type: Type, path: String) {
        try {
            val library = LibraryUtil.getDefault(context, type)
            library.path = path
            if (mDataBase.getDefault(library.id!!) != null)
                mDataBase.update(library)
            else
                mDataBase.save(library)
        } catch (e: Exception) {
            mLOGGER.error("Error when save Default Library: " + e.message, e)
        }
    }

}
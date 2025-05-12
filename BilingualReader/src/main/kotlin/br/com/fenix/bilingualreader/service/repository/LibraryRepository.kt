package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.util.helpers.LibraryUtil
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
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
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when list Library: " + e.message)
                recordException(e)
            }
            listOf()
        }
    }

    fun listEnabled(): List<Library> {
        return try {
            mDataBase.listEnabled()
        } catch (e: Exception) {
            mLOGGER.error("Error when list Library enabled: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when list Library enabled: " + e.message)
                recordException(e)
            }
            listOf()
        }
    }

    fun get(id: Long): Library? {
        return try {
            mDataBase.get(id)
        } catch (e: Exception) {
            mLOGGER.error("Error when get Library: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when get Library: " + e.message)
                recordException(e)
            }
            null
        }
    }

    fun get(type: Type, language: Libraries): Library? {
        return try {
            mDataBase.get(type, language)
        } catch (e: Exception) {
            mLOGGER.error("Error when get Library: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when get Library: " + e.message)
                recordException(e)
            }
            null
        }
    }

    fun findDeleted(path: String): Library? {
        return try {
            mDataBase.findDeleted(path)
        } catch (e: Exception) {
            mLOGGER.error("Error when find Library deleted: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when find Library deleted: " + e.message)
                recordException(e)
            }
            null
        }
    }

    fun removeDefault(path: String) {
        try {
            mDataBase.removeDefault(path)
        } catch (e: Exception) {
            mLOGGER.error("Error when remove Default Library: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when remove Default Library: " + e.message)
                recordException(e)
            }
        }
    }

    fun getDefault(type: Type): String {
        return try {
            LibraryUtil.getDefault(context, type).path
        } catch (e: Exception) {
            mLOGGER.error("Error when get Default Library: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when get Default Library: " + e.message)
                recordException(e)
            }
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
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when save Default Library: " + e.message)
                recordException(e)
            }
        }
    }

    fun deleteAllByPathDefault(type: Type, path: String) {
        val library = LibraryUtil.getDefault(context, type)
        deleteAllByPath(library.id!!, type, path)
    }

    fun deleteAllByPath(idLibrary: Long, type: Type, path: String) {
        when(type) {
            Type.MANGA -> mDataBase.deleteMangaByPath(idLibrary, path)
            Type.BOOK -> mDataBase.deleteBookByPath(idLibrary, path)
        }
    }

}
package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.SubTitle
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class SubTitleRepository(context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(SubTitleRepository::class.java)
    private var mDataBase = DataBase.getDataBase(context).getSubTitleDao()

    fun save(obj: SubTitle): Long {
        deleteAll(obj.id_manga)
        obj.lastAlteration = LocalDateTime.now()
        return mDataBase.save(obj)
    }

    fun delete(obj: SubTitle) = mDataBase.delete(obj)

    private fun deleteAll(idManga: Long) = mDataBase.deleteAll(idManga)

    fun get(idManga: Long, id: Long): SubTitle? {
        return try {
            mDataBase.get(idManga, id)
        } catch (e: Exception) {
            mLOGGER.error("Error when get SubTitle: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when get SubTitle: " + e.message)
                recordException(e)
            }
            null
        }
    }

    fun findByIdManga(idManga: Long): SubTitle? {
        return try {
            mDataBase.findByIdManga(idManga)
        } catch (e: Exception) {
            mLOGGER.error("Error when find SubTitle by id manga: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when find SubTitle by id manga: " + e.message)
                recordException(e)
            }
            null
        }
    }

    fun updateHasSubtitle(idManga: Long, hasSubtitle: Boolean) {
        try {
            mDataBase.updateHasSubtitle(idManga, hasSubtitle)
        } catch (e: Exception) {
            mLOGGER.error("Error when update HasSubTitle by id manga: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when update HasSubTitle by id manga: " + e.message)
                recordException(e)
            }
        }
    }

}
package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.AssistantHistory
import br.com.fenix.bilingualreader.model.enums.Type
import org.slf4j.LoggerFactory

class AssistantHistoryRepository(private val context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(AssistantHistoryRepository::class.java)
    private val mDataBase get() = DataBase.getDataBase(context).getAssistantHistoryDao()

    fun save(obj: AssistantHistory): Long {
        return try {
            if (obj.id != null) {
                mDataBase.update(obj)
                obj.id!!
            } else {
                mDataBase.save(obj)
            }
        } catch (e: Exception) {
            mLOGGER.error("Error saving assistant history: ${e.message}", e)
            -1L
        }
    }

    fun find(type: Type, idReference: Long): List<AssistantHistory> =
        mDataBase.find(type, idReference)

    fun clear(type: Type, idReference: Long) {
        try {
            mDataBase.deleteAll(type, idReference)
        } catch (e: Exception) {
            mLOGGER.error("Error clearing assistant history: ${e.message}", e)
        }
    }
}

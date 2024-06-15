package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.History
import br.com.fenix.bilingualreader.model.enums.Type
import org.slf4j.LoggerFactory


class HistoryRepository(var context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(HistoryRepository::class.java)
    private var mDataBase = DataBase.getDataBase(context).getHistoryDao()


    fun save(obj: History): Long {
        return if (obj.id != null) {
            mDataBase.update(obj)
            obj.id!!
        } else
            mDataBase.save(obj)
    }

    fun last(type: Type, idLibrary: Long, idReference: Long): History? = mDataBase.last(type, idLibrary, idReference)

    fun notify(id: Long) = mDataBase.notify(id)

    fun notify(obj: History) {
        mDataBase.notify(obj.type, obj.fkLibrary, obj.fkReference)
    }

}
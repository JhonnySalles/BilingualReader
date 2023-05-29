package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.BookSearch
import org.slf4j.LoggerFactory

class BookSearchRepository(context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(BookSearchRepository::class.java)
    private var mDataBase = DataBase.getDataBase(context).getBookSearch()

    // --------------------------------------------------------- BOOK SEARCH ---------------------------------------------------------
    fun save(obj: BookSearch): Long {
        return mDataBase.save(obj)
    }

    fun update(obj: BookSearch) {
        mDataBase.update(obj)
    }

    fun delete(idBook: Long) {
        val list = mDataBase.findAll(idBook)
        for (obj in list)
            mDataBase.delete(obj)
    }

    fun delete(obj: BookSearch) {
        if (obj.id != null)
            mDataBase.delete(obj)
    }

    fun findAll(idBook: Long): List<BookSearch> {
        return mDataBase.findAll(idBook)
    }

}
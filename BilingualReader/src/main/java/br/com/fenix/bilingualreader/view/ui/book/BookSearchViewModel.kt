package br.com.fenix.bilingualreader.view.ui.book

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.model.entity.BookSearch
import br.com.fenix.bilingualreader.service.repository.BookSearchRepository
import org.slf4j.LoggerFactory

class BookSearchViewModel(var app: Application) : AndroidViewModel(app) {

    private val mRepository: BookSearchRepository = BookSearchRepository(app.applicationContext)

    private val mLOGGER = LoggerFactory.getLogger(BookSearchViewModel::class.java)

    val book: Book? = null

    private var mListHistory: MutableLiveData<MutableList<BookSearch>> = MutableLiveData(arrayListOf())
    val history: LiveData<MutableList<BookSearch>> = mListHistory

    private var mListSearch: MutableLiveData<MutableList<BookSearch>> = MutableLiveData(arrayListOf())
    val search: LiveData<MutableList<BookSearch>> = mListSearch


    fun save(obj: BookSearch) {
        if (obj.id == null)
            obj.id = mRepository.save(obj)
        else
            mRepository.update(obj)
        mListHistory.value!!.add(obj)
    }

    fun delete(obj: BookSearch) {
        if (mListHistory.value!!.equals(obj))
            mListHistory.value!!.remove(obj)

        mRepository.delete(obj)
    }

    fun search(idBook: Long) {
        mListHistory.value = mRepository.findAll(idBook).toMutableList()
    }

}
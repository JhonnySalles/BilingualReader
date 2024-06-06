package br.com.fenix.bilingualreader.view.ui.book

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.service.repository.BookAnnotationRepository
import org.slf4j.LoggerFactory

class BookAnnotationViewModel(var app: Application) : AndroidViewModel(app) {

    private val mRepository: BookAnnotationRepository = BookAnnotationRepository(app.applicationContext)

    private val mLOGGER = LoggerFactory.getLogger(BookAnnotationViewModel::class.java)

    val book: Book? = null

    private var mAnnotationFull = MutableLiveData<MutableList<BookAnnotation>>(mutableListOf())
    private var mAnnotation: MutableLiveData<MutableList<BookAnnotation>> = MutableLiveData(arrayListOf())
    val annotation: LiveData<MutableList<BookAnnotation>> = mAnnotation

    fun save(obj: BookAnnotation) {
        mRepository.save(obj)
    }

    fun delete(obj: BookAnnotation) {
        mRepository.delete(obj)
        remove(obj)
    }

    fun search(idBook: Long) {
        val list = mRepository.findAll(idBook)
        mAnnotationFull.value = list.toMutableList()
        mAnnotation.value = list.toMutableList()
    }

    fun getAndRemove(position: Int): BookAnnotation? {
        val annotation = if (mAnnotation.value != null) mAnnotation.value!!.removeAt(position) else null
        if (annotation != null)
            mAnnotationFull.value!!.remove(annotation)
        return annotation
    }

    fun add(annotation: BookAnnotation, position: Int = -1) {
        if (position > -1) {
            mAnnotation.value!!.add(position, annotation)
            mAnnotationFull.value!!.add(position, annotation)
        } else {
            mAnnotation.value!!.add(annotation)
            mAnnotationFull.value!!.add(annotation)
        }
    }

    fun remove(annotation: BookAnnotation) {
        if (mAnnotationFull.value != null) {
            mAnnotation.value!!.remove(annotation)
            mAnnotationFull.value!!.remove(annotation)
        }
    }

}
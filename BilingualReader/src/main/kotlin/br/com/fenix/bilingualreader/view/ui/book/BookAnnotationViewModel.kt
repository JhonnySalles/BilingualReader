package br.com.fenix.bilingualreader.view.ui.book

import android.app.Application
import android.widget.Filter
import android.widget.Filterable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Color
import br.com.fenix.bilingualreader.model.enums.MarkType
import br.com.fenix.bilingualreader.model.enums.Filter as FilterType
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.BookAnnotationRepository
import br.com.fenix.bilingualreader.util.helpers.Util
import org.slf4j.LoggerFactory
import java.util.Locale
import java.util.Objects
import java.util.regex.Pattern

class BookAnnotationViewModel(var app: Application) : AndroidViewModel(app), Filterable {

    private val mRepository: BookAnnotationRepository = BookAnnotationRepository(app.applicationContext)

    private val mLOGGER = LoggerFactory.getLogger(BookAnnotationViewModel::class.java)

    val book: Book? = null

    private var mAnnotationFull = MutableLiveData<MutableList<BookAnnotation>>(mutableListOf())
    private var mAnnotation: MutableLiveData<MutableList<BookAnnotation>> = MutableLiveData(arrayListOf())
    val annotation: LiveData<MutableList<BookAnnotation>> = mAnnotation

    private var mWordFilter = ""

    private var mTypeFilter = MutableLiveData(setOf<FilterType>())
    val typeFilter: LiveData<Set<FilterType>> = mTypeFilter

    private var mColorFilter = MutableLiveData(setOf<Color>())
    val colorFilter: LiveData<Set<Color>> = mColorFilter

    fun save(obj: BookAnnotation) {
        if (obj.id != null)
            mRepository.update(obj)
        else
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

    fun filterType(filter: FilterType, isRemove : Boolean = false) {
        val types = mTypeFilter.value!!.toMutableSet()
        if (isRemove)
            types.remove(filter)
        else
            types.add(filter)
        mTypeFilter.value = types.toSet()
        getFilter().filter(mWordFilter)
    }

    fun clearFilterType() {
        mTypeFilter.value = setOf()
        getFilter().filter(mWordFilter)
    }

    fun filterColor(filter: Color, isRemove : Boolean = false) {
        val types = mColorFilter.value!!.toMutableSet()
        if (isRemove)
            types.remove(filter)
        else
            types.add(filter)
        mColorFilter.value = types.toSet()
        getFilter().filter(mWordFilter)
    }

    fun clearFilterColor() {
        mColorFilter.value = setOf()
        getFilter().filter(mWordFilter)
    }

    override fun getFilter(): Filter {
        return mAnnotationFilter
    }

    private fun filtered(annotation: BookAnnotation?, filterPattern: String): Boolean {
        if (annotation == null)
            return false

        if (mTypeFilter.value!!.isNotEmpty()) {
            var condition = false
            mTypeFilter.value!!.forEach {
                when (it) {
                    FilterType.Favorite -> {
                        if (annotation.favorite)
                            condition = true
                    }
                    FilterType.Detach -> {
                        if (annotation.type == MarkType.Annotation)
                            condition = true
                    }
                    FilterType.PageMark -> {
                        if (annotation.type == MarkType.PageMark)
                            condition = true
                    }
                    FilterType.BookMark -> {
                        if (annotation.type == MarkType.BookMark)
                            condition = true
                    }
                    else -> {}
                }
            }

            if (!condition)
                return false
        }

        if (mColorFilter.value!!.isNotEmpty()) {
            var condition = false
            mColorFilter.value!!.forEach {
                if (annotation.color == it)
                    condition = true
            }

            if (!condition)
                return false
        }

        val text = annotation.text.lowercase(Locale.getDefault()).contains(filterPattern) || annotation.annotation.lowercase(Locale.getDefault()).contains(filterPattern)
        return filterPattern.isEmpty() || text
    }

    private val mAnnotationFilter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            mWordFilter = constraint.toString()
            val filteredList: MutableList<BookAnnotation> = mutableListOf()

            if (constraint.isNullOrEmpty() && mTypeFilter.value!!.isEmpty() && mColorFilter.value!!.isEmpty()) {
                filteredList.addAll(mAnnotationFull.value!!.filter(Objects::nonNull))
            } else {
                filteredList.addAll(mAnnotationFull.value!!.filter {
                    filtered(it, constraint.toString().lowercase(Locale.getDefault()).trim())
                })
            }

            val results = FilterResults()
            results.values = filteredList

            return results
        }

        override fun publishResults(constraint: CharSequence?, filterResults: FilterResults?) {
            val list = mutableListOf<BookAnnotation>()
            filterResults?.let {
                list.addAll(it.values as Collection<BookAnnotation>)
            }
            mAnnotation.value = list
        }
    }

}
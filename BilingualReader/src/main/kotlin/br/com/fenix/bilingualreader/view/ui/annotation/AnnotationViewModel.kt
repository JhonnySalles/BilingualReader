package br.com.fenix.bilingualreader.view.ui.annotation

import android.app.Application
import android.widget.Filter
import android.widget.Filterable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.model.enums.Color
import br.com.fenix.bilingualreader.model.enums.MarkType
import br.com.fenix.bilingualreader.model.interfaces.Annotation
import br.com.fenix.bilingualreader.service.repository.BookAnnotationRepository
import br.com.fenix.bilingualreader.service.repository.BookRepository
import org.slf4j.LoggerFactory
import java.util.Locale
import java.util.Objects
import br.com.fenix.bilingualreader.model.enums.Filter as FilterType


class AnnotationViewModel(var app: Application) : AndroidViewModel(app), Filterable {

    private val mRepository: BookAnnotationRepository = BookAnnotationRepository(app.applicationContext)
    private val mRepositoryBook: BookRepository = BookRepository(app.applicationContext)

    private val mLOGGER = LoggerFactory.getLogger(AnnotationViewModel::class.java)

    private var mAnnotationFull = MutableLiveData<MutableList<Annotation>>(mutableListOf())
    private var mAnnotation: MutableLiveData<MutableList<Annotation>> = MutableLiveData(arrayListOf())
    val annotation: LiveData<MutableList<Annotation>> = mAnnotation
    private var mWordFilter = ""

    private var mChapters: MutableLiveData<Map<String, Float>> = MutableLiveData(mapOf())
    val chapters: LiveData<Map<String, Float>> = mChapters

    private var mTypeFilter = MutableLiveData(setOf<FilterType>())
    val typeFilter: LiveData<Set<FilterType>> = mTypeFilter

    private var mColorFilter = MutableLiveData(setOf<Color>())
    val colorFilter: LiveData<Set<Color>> = mColorFilter

    private var mChapterFilter: MutableLiveData<Map<String, Float>> = MutableLiveData(mapOf())
    val chapterFilter: LiveData<Map<String, Float>> = mChapterFilter

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

    fun findAll() {
        val list = mutableListOf<Annotation>()
        val annotations = mRepository.findAllOrderByBook()
        var root: Annotation? = null
        var parent: Annotation? = null
        for (annotation in annotations) {
            if (list.none { it.isRoot && it.id_book == annotation.id_book }) {
                val book = mRepositoryBook.get(annotation.id_book) ?: continue
                root = BookAnnotation(book.id!!, 0f, "", book.title, book.fileName, isRoot = true, isTitle = true)
                list.add(root)
                parent = null
            }

            if (list.none { it.isTitle && it.chapterNumber == annotation.chapterNumber }) {
                parent = BookAnnotation(parent?.id_book ?: 0, annotation.chapterNumber, annotation.chapter, "", "", isTitle = true)
                parent.parent = root
                list.add(parent)
            }

            annotation.parent = parent ?: root
            parent?.let { it.count++ }

            list.add(annotation)
        }

        mAnnotationFull.value = list.toMutableList()
        mAnnotation.value = list.toMutableList()

        getChapters(mAnnotationFull.value!!)
    }

    fun getBook(id: Long): Book? = mRepositoryBook.get(id)

    private fun getChapters(list: List<Annotation>) {
        val chapters = if (list.isNotEmpty())
            list.filterIsInstance<BookAnnotation>().sortedBy { it.chapter }.filter { it.chapter.isNotEmpty() }.associate { it.chapter to it.chapterNumber }
        else
            mapOf()

        mChapters.value = chapters

        if (mChapters.value!!.isNotEmpty()) {
            mChapterFilter.value = if (chapters.isEmpty())
                mapOf()
            else
                mChapterFilter.value!!.filter { f -> chapters.any { c -> c.value == f.value } }
        }
    }

    fun getAndRemove(position: Int): BookAnnotation? {
        if (mAnnotation.value!![position].isTitle)
            return null

        val annotation = if (mAnnotation.value != null) mAnnotation.value!!.removeAt(position) else return null
        mAnnotationFull.value!!.remove(annotation)
        getChapters(mAnnotationFull.value!!)
        return annotation as BookAnnotation
    }

    fun add(annotation: BookAnnotation, position: Int = -1) {
        if (position > -1) {
            mAnnotation.value!!.add(position, annotation)
            mAnnotationFull.value!!.add(position, annotation)
        } else {
            mAnnotation.value!!.add(annotation)
            mAnnotationFull.value!!.add(annotation)
        }
        getChapters(mAnnotationFull.value!!)
    }

    fun remove(annotation: BookAnnotation) {
        if (mAnnotationFull.value != null) {
            mAnnotation.value!!.remove(annotation)
            mAnnotationFull.value!!.remove(annotation)
            getChapters(mAnnotationFull.value!!)
        }
    }

    fun search(search: String) {
        mWordFilter = search
        getFilter().filter(mWordFilter)
    }

    fun clearSearch() {
        mWordFilter = ""
        getFilter().filter(mWordFilter)
    }

    fun filterType(filter: FilterType, isRemove: Boolean = false) {
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

    fun filterColor(filter: Color, isRemove: Boolean = false) {
        val colors = mColorFilter.value!!.toMutableSet()
        if (isRemove)
            colors.remove(filter)
        else
            colors.add(filter)
        mColorFilter.value = colors.toSet()
        getFilter().filter(mWordFilter)
    }

    fun clearFilterColor() {
        mColorFilter.value = setOf()
        getFilter().filter(mWordFilter)
    }

    fun filterChapter(list: MutableSet<String>) {
        mChapterFilter.value = mChapters.value!!.filter { list.any { s -> it.key == s  } }
        getFilter().filter(mWordFilter)
    }

    fun filterChapter(filter: String, isRemove: Boolean = false) = filterChapter(mChapters.value!![filter]!!, isRemove)

    fun filterChapter(filter: Float, isRemove: Boolean = false) {
        val chapter = mChapters.value!!.entries.firstOrNull { it.value == filter }?.key ?: return

        val list = mChapterFilter.value!!.toMutableMap()

        if (isRemove)
            list.remove(chapter)
        else
            list[chapter] = mChapters.value!![chapter]!!

        mChapterFilter.value = list.toMap()
        getFilter().filter(mWordFilter)
    }

    fun clearFilterChapter() {
        mChapterFilter.value = mapOf()
        getFilter().filter(mWordFilter)
    }

    override fun getFilter(): Filter {
        return mAnnotationFilter
    }

    private fun filtered(annotation: Annotation?, filterPattern: String): Boolean {
        if (annotation == null || annotation.isTitle)
            return false

        val annotation = annotation as BookAnnotation
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

        if (mChapterFilter.value!!.isNotEmpty()) {
            var condition = false
            mChapterFilter.value!!.forEach {
                if (annotation.chapterNumber == it.value)
                    condition = true
            }

            if (!condition)
                return false
        }

        val text = annotation.text.lowercase(Locale.getDefault()).contains(filterPattern) || annotation.annotation.lowercase(Locale.getDefault()).contains(filterPattern)
        return filterPattern.isEmpty() || text
    }

    private fun addParent(list: MutableList<Annotation>, annotation: Annotation) {
        if (annotation.parent == null || list.contains(annotation.parent))
            return
        addParent(list, annotation.parent!!)
        annotation.parent!!.count = 0
        list.add(annotation.parent!!)
    }

    private val mAnnotationFilter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            mWordFilter = constraint.toString()
            val filteredList: MutableList<Annotation> = mutableListOf()

            if (constraint.isNullOrEmpty() && mTypeFilter.value!!.isEmpty() && mColorFilter.value!!.isEmpty() && mChapterFilter.value!!.isEmpty()) {
                filteredList.addAll(mAnnotationFull.value!!.filter(Objects::nonNull))

                var parent: Annotation? = null
                for (annotation in filteredList) {
                    if (parent != annotation.parent) {
                        parent = annotation.parent
                        parent?.let { it.count = 0 }
                    }
                    parent?.let { it.count++ }
                }
            } else {
                val newList = mAnnotationFull.value!!.filter { filtered(it, constraint.toString().lowercase(Locale.getDefault()).trim()) }
                for (annotation in newList) {
                    addParent(filteredList, annotation)
                    filteredList.add(annotation)
                    annotation.parent?.let { it.count++ }
                }
            }

            val results = FilterResults()
            results.values = filteredList

            return results
        }

        override fun publishResults(constraint: CharSequence?, filterResults: FilterResults?) {
            val list = mutableListOf<Annotation>()
            filterResults?.let {
                list.addAll(it.values as Collection<Annotation>)
            }
            mAnnotation.value = list
        }
    }

}
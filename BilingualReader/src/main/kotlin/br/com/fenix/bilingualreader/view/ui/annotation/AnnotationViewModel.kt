package br.com.fenix.bilingualreader.view.ui.annotation

import android.app.Application
import android.widget.Filter
import android.widget.Filterable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.entity.MangaAnnotation
import br.com.fenix.bilingualreader.model.enums.Color
import br.com.fenix.bilingualreader.model.enums.MarkType
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.model.interfaces.Annotation
import br.com.fenix.bilingualreader.model.interfaces.History
import br.com.fenix.bilingualreader.service.repository.BookAnnotationRepository
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.MangaAnnotationRepository
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import org.slf4j.LoggerFactory
import java.util.Locale
import java.util.Objects
import br.com.fenix.bilingualreader.model.enums.Filter as FilterType


class AnnotationViewModel(var app: Application) : AndroidViewModel(app), Filterable {

    private val mLOGGER = LoggerFactory.getLogger(AnnotationViewModel::class.java)

    private val mBookAnnotationRepository: BookAnnotationRepository = BookAnnotationRepository(app.applicationContext)
    private val mBookRepository: BookRepository = BookRepository(app.applicationContext)
    private val mMangaAnnotationRepository: MangaAnnotationRepository = MangaAnnotationRepository(app.applicationContext)
    private val mMangaRepository: MangaRepository = MangaRepository(app.applicationContext)

    private var mAnnotationFull = MutableLiveData<MutableList<Annotation>>(mutableListOf())
    private var mAnnotation: MutableLiveData<MutableList<Annotation>> = MutableLiveData(arrayListOf())
    val annotation: LiveData<MutableList<Annotation>> = mAnnotation
    private var mWordFilter = ""

    private var mType: Type? = null

    private var mChapters: MutableLiveData<Map<String, Float>> = MutableLiveData(mapOf())
    val chapters: LiveData<Map<String, Float>> = mChapters

    private var mTypeFilter = MutableLiveData(setOf<FilterType>())
    val typeFilter: LiveData<Set<FilterType>> = mTypeFilter

    private var mColorFilter = MutableLiveData(setOf<Color>())
    val colorFilter: LiveData<Set<Color>> = mColorFilter

    private val mChapterManga = mutableMapOf<String, Float>()
    private val mChapterBook = mutableMapOf<String, Float>()
    private var mChapterFilter: MutableLiveData<Map<String, Float>> = MutableLiveData(mapOf())
    val chapterFilter: LiveData<Map<String, Float>> = mChapterFilter

    // --------------------------------------------------------- BOOK ANNOTATION ---------------------------------------------------------

    fun save(obj: BookAnnotation) {
        if (obj.id != null)
            mBookAnnotationRepository.update(obj)
        else
            mBookAnnotationRepository.save(obj)
    }

    fun delete(obj: BookAnnotation) {
        mBookAnnotationRepository.delete(obj)
        remove(obj)
    }

    fun getBook(id: Long): Book? = mBookRepository.get(id)

    // --------------------------------------------------------- MANGA ANNOTATION ---------------------------------------------------------

    fun save(obj: MangaAnnotation) {
        if (obj.id != null)
            mMangaAnnotationRepository.update(obj)
        else
            mMangaAnnotationRepository.save(obj)
    }

    fun delete(obj: MangaAnnotation) {
        mMangaAnnotationRepository.delete(obj)
        remove(obj)
    }

    fun getManga(id: Long): Manga? = mMangaRepository.get(id)

    // --------------------------------------------------------- ANNOTATION ---------------------------------------------------------

    fun findAll() {
        val list = mutableListOf<Annotation>()
        val annotationsBook = mBookAnnotationRepository.findAllOrderByBook()
        var root: Annotation? = null
        var parent: Annotation? = null
        for (annotation in annotationsBook) {
            if (list.none { it.isRoot && it.type == Type.BOOK && it.id_parent == annotation.id_parent }) {
                val book = mBookRepository.get(annotation.id_parent) ?: continue
                root = BookAnnotation(book.id!!, 0f, book.title, "", book.fileName, isRoot = true, isTitle = true)
                list.add(root)
                parent = null
            }

            if (list.none { it.isTitle && it.type == Type.BOOK && it.chapterNumber == annotation.chapterNumber }) {
                parent = BookAnnotation(parent?.id_parent ?: root?.id_parent ?: 0, annotation.chapterNumber, annotation.chapter, "", "", isTitle = true)
                parent.parent = root
                list.add(parent)
            }

            annotation.parent = parent ?: root
            parent?.let { it.count++ }

            list.add(annotation)
        }

        val annotationsManga = mMangaAnnotationRepository.findAllOrderByManga()
        root = null
        parent = null
        for (annotation in annotationsManga) {
            if (list.none { it.isRoot && it.type == Type.MANGA && it.id_parent == annotation.id_parent }) {
                val manga = mMangaRepository.get(annotation.id_parent) ?: continue
                root = MangaAnnotation(manga.id!!, manga.title, manga.fileName, "", isRoot = true, isTitle = true)
                list.add(root)
                parent = null
            }

            if (list.none { it.isTitle && it.type == Type.MANGA && it.chapter == annotation.chapter }) {
                parent = MangaAnnotation(parent?.id_parent ?: root?.id_parent ?: 0, annotation.chapter, "", "", isTitle = true)
                parent.parent = root
                list.add(parent)
            }

            annotation.parent = parent ?: root
            parent?.let { it.count++ }

            list.add(annotation)
        }

        mAnnotationFull.value = list.toMutableList()
        mAnnotation.value = list.toMutableList()

        getChapters(mAnnotationFull.value!!, isInitial = true)
    }

    private fun getChapters(list: List<Annotation>, isInitial : Boolean = false) {
        val chapters = mutableMapOf<String, Float>()

        if (isInitial) {
            mChapterBook.clear()
            mChapterManga.clear()
        }

        if (list.isNotEmpty()) {
            val books = if (isInitial || mType == null || mType == Type.BOOK)
                list.filterIsInstance<BookAnnotation>().filter { !it.isTitle && !it.isRoot && it.chapter.isNotEmpty() }.sortedBy { it.chapter }.associate { it.chapter to it.chapterNumber }
            else
                mapOf()
            chapters.putAll(books)

            val mangas = if (isInitial || mType == null || mType == Type.MANGA)
                list.filterIsInstance<MangaAnnotation>().filter { !it.isTitle && !it.isRoot && it.chapter.isNotEmpty() }.sortedBy { it.chapter }.associate { it.chapter to it.page.toFloat() }
            else
                mapOf()

            chapters.putAll(mangas)

            if (isInitial) {
                mChapterBook.putAll(books)
                mChapterManga.putAll(mangas)
            }
        }

        mChapters.value = chapters

        if (mChapters.value!!.isNotEmpty()) {
            mChapterFilter.value = if (chapters.isEmpty())
                mapOf()
            else
                mChapterFilter.value!!.filter { f -> chapters.any { c -> c.value == f.value } }
        }
    }

    fun getAndRemove(position: Int): Annotation? {
        if (mAnnotation.value!![position].isTitle)
            return null

        val annotation = if (mAnnotation.value != null) mAnnotation.value!!.removeAt(position) else return null
        mAnnotationFull.value!!.remove(annotation)
        getChapters(mAnnotationFull.value!!)
        return annotation
    }

    fun add(annotation: Annotation, position: Int = -1) {
        if (position > -1) {
            mAnnotation.value!!.add(position, annotation)
            mAnnotationFull.value!!.add(position, annotation)
        } else {
            mAnnotation.value!!.add(annotation)
            mAnnotationFull.value!!.add(annotation)
        }
        getChapters(mAnnotationFull.value!!)
    }

    fun save(obj: Annotation) {
        when (obj.type) {
            Type.BOOK -> save(obj as BookAnnotation)
            Type.MANGA -> save(obj as MangaAnnotation)
        }
    }

    fun delete(obj: Annotation) {
        when (obj.type) {
            Type.BOOK -> delete(obj as BookAnnotation)
            Type.MANGA -> delete(obj as MangaAnnotation)
        }
    }

    fun remove(annotation: Annotation) {
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

    fun filterType(type: Type?) {
        if (type == mType)
            return

        mType = type
        mAnnotation.value = filterList()
        getChapters(mAnnotationFull.value!!)
    }

    private fun filterList(): MutableList<Annotation> {
        if (mType == null && mWordFilter.isEmpty())
            return mAnnotationFull.value!!
        else {
            val list = mutableListOf<Annotation>()
            val newList = mAnnotationFull.value!!.filter { filtered(it, mWordFilter) }
            for (annotation in newList) {
                addParent(list, annotation)
                list.add(annotation)
                annotation.parent?.let { it.count++ }
            }
            return list
        }
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

        if (mType != null && annotation.type != mType)
            return false

        val annotation = annotation
        if (mTypeFilter.value!!.isNotEmpty()) {
            var condition = false
            mTypeFilter.value!!.forEach {
                when (it) {
                    FilterType.Favorite -> {
                        if (annotation is BookAnnotation && annotation.favorite)
                            condition = true
                    }

                    FilterType.Detach -> {
                        if (annotation.markType == MarkType.Annotation)
                            condition = true
                    }

                    FilterType.PageMark -> {
                        if (annotation.markType == MarkType.PageMark)
                            condition = true
                    }

                    FilterType.BookMark -> {
                        if (annotation.markType == MarkType.BookMark)
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
                if (annotation is BookAnnotation && annotation.color == it)
                    condition = true
            }

            if (!condition)
                return false
        }

        if (mChapterFilter.value!!.isNotEmpty()) {
            var condition = false
            mChapterFilter.value!!.forEach {
                when (annotation.type) {
                    Type.BOOK -> {
                        if (annotation.chapterNumber == it.value)
                            condition = true
                    }
                    Type.MANGA -> {
                        if (annotation.chapter == it.key)
                            condition = true
                    }
                }
            }

            if (!condition)
                return false
        }


        val text = when (annotation) {
            is BookAnnotation -> annotation.text.lowercase(Locale.getDefault()).contains(filterPattern)
            is MangaAnnotation -> annotation.chapter.lowercase(Locale.getDefault()).contains(filterPattern)
            else -> false
        }

        return filterPattern.isEmpty() || text || annotation.annotation.lowercase(Locale.getDefault()).contains(filterPattern)
    }

    private fun addParent(list: MutableList<Annotation>, annotation: Annotation) {
        if (annotation.parent == null)
            return

        val parent = annotation.parent!!
        if (parent.isRoot && list.any { it.isRoot && it.type == parent.type && it.id_parent == parent.id_parent })
            return

        if (!parent.isRoot && parent.isTitle) {
            when (annotation.type) {
                Type.BOOK -> {
                    if (list.any { it.isTitle && it.type == parent.type && it.chapterNumber == parent.chapterNumber })
                        return
                }
                Type.MANGA -> {
                    if (list.any { it.isTitle && it.type == parent.type && it.chapter == parent.chapter })
                        return
                }

            }
        }

        addParent(list, parent)
        parent.count = 0
        list.add(parent)
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
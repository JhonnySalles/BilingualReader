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
import br.com.fenix.bilingualreader.service.repository.BookAnnotationRepository
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.MangaAnnotationRepository
import br.com.fenix.bilingualreader.service.repository.MangaRepository
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

    private var mType = MutableLiveData<Type?>(null)
    val type: LiveData<Type?> = mType

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

    /**
     * Delete item on database and remove from list
     *
     * @param annotation Item remove
     * @param refresh Callback function of refresh item, primary has index and second true if is remove
     */
    fun delete(obj: BookAnnotation, refresh: (index: Int, isRemove : Boolean) -> (Unit)) {
        mBookAnnotationRepository.delete(obj)
        remove(obj, refresh)
    }

    fun getBook(id: Long): Book? = mBookRepository.get(id)

    // --------------------------------------------------------- MANGA ANNOTATION ---------------------------------------------------------

    fun save(obj: MangaAnnotation) {
        if (obj.id != null)
            mMangaAnnotationRepository.update(obj)
        else
            mMangaAnnotationRepository.save(obj)
    }

    /**
     * Delete item on database and remove from list
     *
     * @param annotation Item remove
     * @param refresh Callback function of refresh item, primary has index and second true if is remove
     */
    fun delete(obj: MangaAnnotation, refresh: (index: Int, isRemove : Boolean) -> (Unit)) {
        mMangaAnnotationRepository.delete(obj)
        remove(obj, refresh)
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
            val books = if (isInitial || mType.value == null || mType.value == Type.BOOK)
                list.filterIsInstance<BookAnnotation>().filter { !it.isTitle && !it.isRoot && it.chapter.isNotEmpty() }.sortedBy { it.chapter }.associate { it.chapter to it.chapterNumber }
            else
                mapOf()
            chapters.putAll(books)

            val mangas = if (isInitial || mType.value == null || mType.value == Type.MANGA)
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

    /**
     * Delete item on database and remove from list
     *
     * @param annotation Item remove
     * @param refresh Callback function of refresh item, primary has index and second true if is remove
     */
    fun delete(obj: Annotation, refresh: (index: Int, isRemove : Boolean) -> (Unit)) {
        when (obj.type) {
            Type.BOOK -> delete(obj as BookAnnotation, refresh)
            Type.MANGA -> delete(obj as MangaAnnotation, refresh)
        }
    }

    /**
     * Remove a item on list, and if necessary refresh or remove parent
     *
     * @param annotation Item remove
     * @param refresh Callback function of refresh item, primary has index and second true if is remove
     */
    fun remove(annotation: Annotation, refresh: (index: Int, isRemove : Boolean) -> (Unit)) {
        if (mAnnotationFull.value != null) {
            if (annotation.parent != null) {
                val parent = annotation.parent!!
                val index = mAnnotation.value!!.indexOf(parent)
                parent.count--

                if (parent.count <= 0) {
                    mAnnotation.value!!.remove(parent)
                    mAnnotationFull.value!!.remove(parent)
                    refresh(index, true)

                    if (parent.parent != null && parent.parent!!.isRoot) {
                        val root = parent.parent!!
                        if (mAnnotation.value!!.none { it.isTitle && it.type == root.type && it.parent != null && it.parent!!.id_parent == root.id_parent }) {
                            val index = mAnnotation.value!!.indexOf(parent)
                            mAnnotation.value!!.remove(root)
                            mAnnotationFull.value!!.remove(root)
                            refresh(index, true)
                        }
                    }
                } else
                    refresh(index, false)
            }

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
        if (type == mType.value)
            return

        mType.value = type
        mAnnotation.value = filterList()
        getChapters(mAnnotationFull.value!!)
    }

    private fun filterList(): MutableList<Annotation> {
        if (mType.value == null && mWordFilter.isEmpty())
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

        if (mType.value != null && annotation.type != mType.value)
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


        val condition = when (annotation) {
            is BookAnnotation -> annotation.text.lowercase(Locale.getDefault()).contains(filterPattern) || annotation.chapter.lowercase(Locale.getDefault()).contains(filterPattern) || annotation.annotation.lowercase(Locale.getDefault()).contains(filterPattern)
            is MangaAnnotation -> annotation.chapter.lowercase(Locale.getDefault()).contains(filterPattern) || annotation.page.toString().contains(filterPattern)
            else -> false
        }

        return filterPattern.isEmpty() || condition
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
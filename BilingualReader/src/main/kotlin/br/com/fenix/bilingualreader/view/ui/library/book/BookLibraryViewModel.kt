package br.com.fenix.bilingualreader.view.ui.library.book

import android.app.Application
import android.content.Context
import android.widget.Filter
import android.widget.Filterable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Tags
import br.com.fenix.bilingualreader.model.enums.FileType
import br.com.fenix.bilingualreader.model.enums.LibraryBookType
import br.com.fenix.bilingualreader.model.enums.ListMode
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.model.enums.ShareMarkType
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.TagsRepository
import br.com.fenix.bilingualreader.service.sharemark.ShareMarkBase
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.Telemetry
import br.com.fenix.bilingualreader.util.helpers.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.util.Locale
import java.util.Objects
import java.util.regex.Pattern
import java.util.stream.Collectors
import br.com.fenix.bilingualreader.model.enums.Filter as FilterType

class BookLibraryViewModel(var app: Application) : AndroidViewModel(app), Filterable {

    private val mLOGGER = LoggerFactory.getLogger(BookLibraryViewModel::class.java)

    var isLoading : Boolean = true

    private var mStackLibrary = mutableMapOf<String, Triple<Int, Library, LinkedHashMap<Long, Book>>>()
    private var mLibrary: Library = Library(GeneralConsts.KEYS.LIBRARY.DEFAULT_BOOK)
    private val mBookRepository: BookRepository = BookRepository(app.applicationContext)
    private val mTagsRepository: TagsRepository = TagsRepository(app.applicationContext)
    private val mPreferences = GeneralConsts.getSharedPreferences(app.applicationContext)

    private var mLoading = MutableLiveData<Boolean>(false)
    val loading: LiveData<Boolean> = mLoading

    private var mWordFilter = ""
    val wordFilter: String get() = mWordFilter

    private var mOrder = MutableLiveData(Pair(Order.Name, false))
    val order: LiveData<Pair<Order, Boolean>> = mOrder
    private var mTypeFilter = MutableLiveData(FilterType.None)
    val typeFilter: LiveData<FilterType> = mTypeFilter

    private var mLibraryType = MutableLiveData(LibraryBookType.GRID_BIG)
    val libraryType: LiveData<LibraryBookType> = mLibraryType

    private val mFullMap = LinkedHashMap<Long, Book>()
    private var mListBook = MutableLiveData<MutableList<Book>>(mutableListOf())
    val listBook: LiveData<MutableList<Book>> = mListBook

    private var mSuggestionAuthor = setOf<String>()
    private var mSuggestionPublisher = setOf<String>()
    private var mTags = mutableListOf<Tags>()

    private var mProcessShareMark = false

    private data class IncrementalDiff(
        val change: Boolean,
        val indexes: MutableList<Pair<ListMode, Int>>,
        val toAdd: List<Book>,
        val toRemoveVisibleIndices: List<Int>,
        val existingUpdates: List<Pair<Long, Book>>
    )

    private fun isFilterActive(): Boolean =
        mWordFilter.isNotEmpty() || mTypeFilter.value != FilterType.None

    private fun fullValues(): Collection<Book> = mFullMap.values

    private fun setFullFromList(list: List<Book>) {
        mFullMap.clear()
        for (book in list) {
            book.id?.let { mFullMap[it] = book }
        }
    }

    private fun rebuildFullMap(sorted: List<Book>) {
        mFullMap.clear()
        for (book in sorted) {
            book.id?.let { mFullMap[it] = book }
        }
    }

    private fun insertInFullMap(book: Book, position: Int) {
        val id = book.id ?: return
        if (position > -1 && position < mFullMap.size) {
            val newMap = LinkedHashMap<Long, Book>()
            var index = 0
            for ((key, value) in mFullMap) {
                if (index == position) newMap[id] = book
                newMap[key] = value
                index++
            }
            mFullMap.clear()
            mFullMap.putAll(newMap)
        } else {
            mFullMap[id] = book
        }
    }

    private fun removeFromFull(book: Book) {
        book.id?.let { mFullMap.remove(it) }
    }

    private fun containsInFull(book: Book): Boolean =
        book.id != null && mFullMap.containsKey(book.id)

    private fun setSuggestionsFromFull() = setSuggestions(mFullMap.values.toList())

    private fun sortList(list: MutableList<Book>, order: Order, isDesc: Boolean) {
        if (isDesc) {
            when (order) {
                Order.Date -> list.sortByDescending { it.dateCreate }
                Order.LastAccess -> list.sortWith(compareByDescending<Book> { it.lastAccess }.thenByDescending { it.name })
                Order.Favorite -> list.sortWith(compareByDescending<Book> { it.favorite }.thenByDescending { it.name })
                Order.Author -> list.sortWith(compareByDescending<Book> { it.author }.thenByDescending { it.name })
                Order.Series -> list.sortWith(compareByDescending<Book> { it.series }.thenByDescending { it.name })
                else -> list.sortByDescending { it.name }
            }
        } else {
            when (order) {
                Order.Date -> list.sortBy { it.dateCreate }
                Order.LastAccess -> list.sortWith(compareByDescending<Book> { it.lastAccess }.thenBy { it.name })
                Order.Favorite -> list.sortWith(compareByDescending<Book> { it.favorite }.thenBy { it.name })
                Order.Author -> list.sortWith(compareByDescending<Book> { it.author }.thenBy { it.name })
                Order.Series -> list.sortWith(compareByDescending<Book> { it.series }.thenBy { it.name })
                else -> list.sortBy { it.name }
            }
        }
    }

    private fun computeIncrementalDiff(
        recentChanges: List<Book>,
        recentDeleted: List<Book>,
        fullSnapshot: Map<Long, Book>,
        visibleSnapshot: List<Book>
    ): IncrementalDiff {
        val visibleIndexById = HashMap<Long, Int>()
        visibleSnapshot.forEachIndexed { index, book ->
            book.id?.let { visibleIndexById[it] = index }
        }

        val indexes = mutableListOf<Pair<ListMode, Int>>()
        var change = false
        val toAdd = mutableListOf<Book>()
        val toRemoveVisibleIndices = mutableListOf<Int>()
        val existingUpdates = mutableListOf<Pair<Long, Book>>()

        if (recentChanges.isNotEmpty()) {
            change = true
            for (book in recentChanges) {
                val id = book.id ?: continue
                val existing = fullSnapshot[id]
                if (existing != null) {
                    if (existing.modify(book)) {
                        existingUpdates.add(id to book)
                        visibleIndexById[id]?.let { index ->
                            indexes.add(Pair(ListMode.MOD, index))
                        }
                    }
                } else {
                    toAdd.add(book)
                }
            }
        }

        if (recentDeleted.isNotEmpty()) {
            change = true
            for (book in recentDeleted) {
                val id = book.id ?: continue
                if (fullSnapshot.containsKey(id)) {
                    visibleIndexById[id]?.let { index ->
                        toRemoveVisibleIndices.add(index)
                        indexes.add(Pair(ListMode.REM, index))
                    }
                }
            }
        }

        return IncrementalDiff(change, indexes, toAdd, toRemoveVisibleIndices, existingUpdates)
    }

    private fun applyIncrementalDiff(diff: IncrementalDiff) {
        for ((id, book) in diff.existingUpdates) {
            mFullMap[id]?.update(book, true)
        }

        for (book in diff.toAdd) {
            book.id?.let { mFullMap[it] = book }
            mListBook.value!!.add(book)
            diff.indexes.add(Pair(ListMode.ADD, mListBook.value!!.size - 1))
        }

        for (index in diff.toRemoveVisibleIndices.sortedDescending()) {
            val book = mListBook.value!!.removeAt(index)
            removeFromFull(book)
        }
    }

    fun setDefaultLibrary(library: Library) {
        if (mLibrary.id == library.id)
            mLibrary = library
    }

    fun setLibrary(library: Library) {
        if (mLibrary.id != library.id) {
            mTypeFilter.value = FilterType.None
            mWordFilter = ""
            mFullMap.clear()
            mListBook.value = mutableListOf()
        }
        mLibrary = library
    }

    fun saveLastLibrary() {
        val key = if (mLibrary.id == GeneralConsts.KEYS.LIBRARY.DEFAULT_BOOK)
            R.id.menu_book_library_default
        else
            mLibrary.id!!
        mPreferences.edit().putLong(
            GeneralConsts.KEYS.LIBRARY.LAST_LIBRARY,
            key.toLong()
        ).apply()
    }

    fun getLibrary() = mLibrary

    fun existStack(id: String): Boolean = mStackLibrary.contains(id)

    fun restoreLastStackLibrary(id: String) {
        if (mStackLibrary.contains(id)) {
            val item = mStackLibrary.remove(id)!!
            mLibrary = item.second
            mFullMap.clear()
            mFullMap.putAll(item.third)
            mListBook.value = mFullMap.values.toMutableList()
            setSuggestionsFromFull()
        }
    }

    fun addStackLibrary(id: String, library: Library) =
        mStackLibrary.put(id, Triple(mStackLibrary.size + 1, library, LinkedHashMap(mFullMap)))

    fun removeStackLibrary(id: String) = mStackLibrary.remove(id)

    fun emptyList(idLibrary: Long) {
        if (mLibrary.id == idLibrary) {
            mFullMap.clear()
            mListBook.value = mutableListOf()
            setSuggestionsFromFull()
        } else {
            for (stack in mStackLibrary)
                if (stack.value.second.id == idLibrary)
                    stack.value.third.clear()
        }
    }

    fun save(obj: Book): Book {
        if (obj.id == 0L)
            obj.id = mBookRepository.save(obj)
        else
            mBookRepository.update(obj)

        return obj
    }

    fun add(Book: Book, position: Int = -1) {
        if (position > -1) {
            mListBook.value!!.add(position, Book)
            insertInFullMap(Book, position)
        } else {
            mListBook.value!!.add(Book)
            insertInFullMap(Book, -1)
        }
    }

    fun delete(obj: Book) {
        mBookRepository.delete(obj)
        remove(obj)
    }

    fun getAndRemove(position: Int): Book? {
        val Book = if (mListBook.value != null) mListBook.value!!.removeAt(position) else null
        if (Book != null) removeFromFull(Book)
        return Book
    }

    fun remove(Book: Book) {
        mListBook.value!!.remove(Book)
        removeFromFull(Book)
    }

    fun remove(position: Int) {
        val Book = mListBook.value!!.removeAt(position)
        removeFromFull(Book)
    }

    fun update(list: List<Book>) {
        if (list.isNotEmpty()) {
            for (Book in list) {
                if (!containsInFull(Book)) {
                    mListBook.value!!.add(Book)
                    insertInFullMap(Book, -1)
                }
            }
        }
    }

    fun setList(list: ArrayList<Book>) {
        mListBook.value = list
        setFullFromList(list)
        setSuggestions(list)
    }

    fun addList(Book: Book): Int {
        var index = -1
        if (!containsInFull(Book)) {
            index = mListBook.value!!.size
            mListBook.value!!.add(Book)
            insertInFullMap(Book, -1)
        }

        return index
    }

    fun remList(Book: Book): Int {
        var index = -1

        if (containsInFull(Book)) {
            index = mListBook.value!!.indexOf(Book)
            mListBook.value!!.remove(Book)
            removeFromFull(Book)
        }

        return index
    }

    fun updateList(index: Int): Int {
        val list = mListBook.value
        if (list.isNullOrEmpty() || index < 0 || index >= list.size) {
            return -1
        }
        val book = list[index]
        val updatedBook = mBookRepository.get(book.id!!)
        if (updatedBook != null && book.modify(updatedBook)) {
            book.update(updatedBook, true)
            return index
        }
        return -1
    }

    fun updateList(refreshComplete: (Boolean, indexes: MutableList<Pair<ListMode, Int>>) -> (Unit)) {
        viewModelScope.launch {
            if (mFullMap.isNotEmpty()) {
                val list = withContext(Dispatchers.IO) { mBookRepository.listRecentChange(mLibrary) }
                val listDel = withContext(Dispatchers.IO) { mBookRepository.listRecentDeleted(mLibrary) }

                val fullSnapshot = LinkedHashMap(mFullMap)
                val visibleSnapshot = mListBook.value?.toList() ?: emptyList()

                val diff = withContext(Dispatchers.Default) {
                    computeIncrementalDiff(list, listDel, fullSnapshot, visibleSnapshot)
                }

                withContext(Dispatchers.Main) {
                    applyIncrementalDiff(diff)
                    setSuggestionsFromFull()
                    refreshComplete(diff.change, diff.indexes)
                }
            } else {
                val list = withContext(Dispatchers.IO) { mBookRepository.list(mLibrary) }
                val indexes = mutableListOf<Pair<ListMode, Int>>()
                indexes.add(Pair(ListMode.FULL, list.size))
                mListBook.value = list.toMutableList()
                setFullFromList(list)
                sorted()
                setSuggestionsFromFull()
                refreshComplete(false, indexes)
            }
        }
    }

    fun list(refreshComplete: (Boolean) -> (Unit)) {
        mLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val list = mBookRepository.list(mLibrary)
            withContext(Dispatchers.Main) {
                mLoading.value = false

                if (mFullMap.isEmpty()) {
                    mListBook.value = list.toMutableList()
                    setFullFromList(list)
                    setSuggestionsFromFull()
                } else
                    update(list)

                refreshComplete(mListBook.value!!.isNotEmpty())
            }
        }
    }

    fun clearHistory(book: Book) {
        mBookRepository.clearHistory(book)
    }

    fun loadTags(): MutableList<Tags> {
        mTags = mTagsRepository.list()
        return mTags
    }

    fun changeLibraryType() {
        val type = when (mLibraryType.value) {
            LibraryBookType.LINE -> LibraryBookType.GRID_BIG
            LibraryBookType.GRID_BIG -> LibraryBookType.GRID_MEDIUM
            LibraryBookType.GRID_MEDIUM -> LibraryBookType.SEPARATOR_BIG
            LibraryBookType.SEPARATOR_BIG -> LibraryBookType.SEPARATOR_MEDIUM
            LibraryBookType.SEPARATOR_MEDIUM -> LibraryBookType.SEPARATOR_CAROUSEL
            LibraryBookType.SEPARATOR_CAROUSEL -> LibraryBookType.SEPARATOR_LINE
            LibraryBookType.SEPARATOR_LINE -> LibraryBookType.LINE
            else -> LibraryBookType.LINE
        }
        setLibraryType(type)
    }

    fun setLibraryType(type: LibraryBookType) {
        mLibraryType.value = type
    }

    fun isEmpty(): Boolean = mListBook.value == null || mListBook.value!!.isEmpty()

    fun sorted() {
        sorted(mOrder.value?.first ?: Order.Name)
    }

    fun sorted(order: Order, isDesc: Boolean = false) {
        mOrder.value = Pair(order, isDesc)

        val sortedFull = mFullMap.values.toMutableList()
        sortList(sortedFull, order, isDesc)
        rebuildFullMap(sortedFull)

        if (!isFilterActive()) {
            mListBook.value = sortedFull
        } else {
            val sortedVisible = mListBook.value!!.toMutableList()
            sortList(sortedVisible, order, isDesc)
            mListBook.value = sortedVisible
        }
    }

    private fun setSuggestions(list : List<Book>?) {
        mSuggestionAuthor = setOf()
        mSuggestionPublisher = setOf()

        if (list.isNullOrEmpty())
            return

        val process = ArrayList(list)

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val authors = mutableSetOf<String>()
                val publishers = mutableSetOf<String>()

                process.forEach {
                    if (it.author.contains(","))
                        authors.addAll(it.author.split(",").map { it.trim() })
                    else
                        authors.add(it.author)
                    publishers.add(it.publisher)
                }

                authors.removeIf {it.isEmpty()}
                publishers.removeIf {it.isEmpty()}

                withContext(Dispatchers.Main) {
                    mSuggestionAuthor = authors
                    mSuggestionPublisher = publishers
                }
            } catch (e: Exception) {
                mLOGGER.error("Error generate suggestion: " + e.message, e)
                Telemetry.recordException(e, "Error generate suggestion: " + e.message)
            }
        }
    }

    fun getSuggestions(filter : String): List<String> {
        val type = filter.substringBeforeLast(':')
        val condition = filter.substringAfterLast(':')
        return when(Util.stringToFilter(app, Type.BOOK, type, true)) {
            FilterType.Author -> mSuggestionAuthor.parallelStream().filter { condition.isEmpty() || it.contains(condition, true) }.collect(Collectors.toList())
            FilterType.Publisher -> mSuggestionPublisher.parallelStream().filter { condition.isEmpty() || it.contains(condition, true) }.collect(Collectors.toList())
            FilterType.Tag -> mTags.parallelStream().map { "$it" }.collect(Collectors.toList())
            FilterType.Type -> FileType.getBook().parallelStream().map { "$it" }.collect(Collectors.toList())
            else -> listOf()
        }
    }

    fun filterType(filter: FilterType) {
        mTypeFilter.value = filter
        getFilter().filter(mWordFilter)
    }

    fun clearFilter() {
        val newList: MutableList<Book> = mutableListOf()
        newList.addAll(fullValues().filter(Objects::nonNull))
        mListBook.value = newList
    }

    override fun getFilter(): Filter {
        return mBookFilter
    }

    private fun filtered(book: Book?, filterPattern: String, filterConditions :ArrayList<Pair<FilterType, String>>): Boolean {
        if (book == null)
            return false

        if (mTypeFilter.value != FilterType.None) {
            if (mTypeFilter.value == FilterType.Reading && book.lastAccess == null)
                return false

            if (mTypeFilter.value == FilterType.Favorite && !book.favorite)
                return false
        }

        if (filterConditions.isNotEmpty()) {
            var condition = false
            filterConditions.forEach {
                when (it.first) {
                    FilterType.Type -> {
                        if (book.fileType.name.contains(it.second, true))
                            condition = true
                    }
                    FilterType.Publisher -> {
                        if (book.publisher.contains(it.second, true))
                            condition = true
                    }
                    FilterType.Author -> {
                        if (book.author.contains(it.second, true))
                            condition = true
                    }
                    FilterType.Tag -> {
                        if (it.second.isEmpty() && book.tags.isEmpty())
                            return false
                        else if (it.second.isNotEmpty()) {
                            mTags.find { t -> t.name.equals(it.second.replace("'", ""), true) }?.let { t ->
                                if (book.tags.contains(t.id))
                                    condition = true
                            }
                        }
                    }
                    else -> {}
                }
            }

            if (!condition)
                return false
        }

        return filterPattern.isEmpty() || book.name.lowercase(Locale.getDefault()).contains(filterPattern) ||
                book.title.lowercase(Locale.getDefault()).contains(filterPattern) || book.fileType.compareExtension(filterPattern)
    }

    private val mBookFilter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            mWordFilter = constraint?.toString() ?: ""
            val filteredList: MutableList<Book> = mutableListOf()

            if (constraint.isNullOrEmpty() && mTypeFilter.value == FilterType.None) {
                filteredList.addAll(fullValues().filter(Objects::nonNull))
            } else {
                var filterPattern = constraint?.toString() ?: ""
                val filterCondition = arrayListOf<Pair<FilterType, String>>()
                if (constraint != null && constraint.contains('@')) {
                    val m = Pattern.compile("(@\\S*:(\"[^\"]*\"|[^\\s]+))\\s*").matcher(constraint)
                    while (m.find()) {
                        val fullMatch = m.group(0) ?: continue
                        val item = m.group(1)?.replace("\"", "") ?: continue
                        filterPattern = filterPattern.replace(fullMatch, "", true)
                        val type = Util.stringToFilter(app.applicationContext, Type.BOOK, item.substringBefore(":").replace("@", ""))
                        if (type != FilterType.None) {
                            val condition = item.substringAfter(":")
                            if (condition.isNotEmpty())
                                filterCondition.add(Pair(type, condition))
                        }
                    }
                }

                filterPattern = filterPattern.lowercase(Locale.getDefault()).trim()
                filteredList.addAll(fullValues().filter {
                    filtered(it, filterPattern, filterCondition)
                })
            }

            val results = FilterResults()
            results.values = filteredList

            return results
        }

        override fun publishResults(constraint: CharSequence?, filterResults: FilterResults?) {
            val list = mutableListOf<Book>()
            val values = filterResults?.values
            val items = when (values) {
                is Collection<*> -> values.filterIsInstance<Book>()
                else -> emptyList()
            }
            list.addAll(items)
            mListBook.value = list
        }
    }

    fun processShareMarks(context: Context, idNotification: Int, processed: (shareMark: ShareMarkType, idNotification: Int) -> Unit) {
        if (!mProcessShareMark) {
            mProcessShareMark = true
            val share = ShareMarkBase.getInstance(context)
            var notify = false
            val process: (book: Book) -> (Unit) = { item ->
                if (mLibrary.id == item.fkLibrary) {
                    notify = true
                    item.id?.let { id ->
                        mFullMap[id]?.let { book ->
                            book.favorite = item.favorite
                            book.bookMark = item.bookMark
                            book.pages = item.pages
                            book.completed = item.completed
                            book.lastAccess = item.lastAccess
                        }
                    }
                }
            }
            share.bookShareMark(process) {
                mProcessShareMark = false
                if ((it == ShareMarkType.SUCCESS || it == ShareMarkType.NOT_ALTERATION) && notify)
                    processed(ShareMarkType.NOTIFY_DATA_SET, idNotification)
                else
                    processed(it, idNotification)
            }
        } else
            processed(ShareMarkType.SYNC_IN_PROGRESS, idNotification)
    }
}

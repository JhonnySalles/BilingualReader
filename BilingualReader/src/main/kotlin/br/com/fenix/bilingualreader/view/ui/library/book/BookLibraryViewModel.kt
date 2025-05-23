package br.com.fenix.bilingualreader.view.ui.library.book

import android.app.Application
import android.content.Context
import android.widget.Filter
import android.widget.Filterable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
import br.com.fenix.bilingualreader.util.helpers.Util
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.util.Locale
import java.util.Objects
import java.util.regex.Pattern
import java.util.stream.Collectors
import br.com.fenix.bilingualreader.model.enums.Filter as FilterType

class BookLibraryViewModel(var app: Application) : AndroidViewModel(app), Filterable {

    private val mLOGGER = LoggerFactory.getLogger(BookLibraryViewModel::class.java)

    var isLaunch : Boolean = true

    private var mStackLibrary = mutableMapOf<String, Triple<Int, Library, MutableList<Book>>>()
    private var mLibrary: Library = Library(GeneralConsts.KEYS.LIBRARY.DEFAULT_BOOK)
    private val mBookRepository: BookRepository = BookRepository(app.applicationContext)
    private val mTagsRepository: TagsRepository = TagsRepository(app.applicationContext)
    private val mPreferences = GeneralConsts.getSharedPreferences(app.applicationContext)

    private var mLoading = MutableLiveData<Boolean>(false)
    val loading: LiveData<Boolean> = mLoading

    private var mWordFilter = ""

    private var mOrder = MutableLiveData(Pair(Order.Name, false))
    val order: LiveData<Pair<Order, Boolean>> = mOrder
    private var mTypeFilter = MutableLiveData(FilterType.None)
    val typeFilter: LiveData<FilterType> = mTypeFilter

    private var mLibraryType = MutableLiveData(LibraryBookType.GRID_BIG)
    val libraryType: LiveData<LibraryBookType> = mLibraryType

    private var mListBookFull = MutableLiveData<MutableList<Book>>(mutableListOf())
    private var mListBook = MutableLiveData<MutableList<Book>>(mutableListOf())
    val listBook: LiveData<MutableList<Book>> = mListBook

    private var mSuggestionAuthor = setOf<String>()
    private var mSuggestionPublisher = setOf<String>()
    private var mTags = mutableListOf<Tags>()

    private var mProcessShareMark = false

    fun setDefaultLibrary(library: Library) {
        if (mLibrary.id == library.id)
            mLibrary = library
    }

    fun setLibrary(library: Library) {
        if (mLibrary.id != library.id) {
            mListBookFull.value = mutableListOf()
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
            mStackLibrary.remove(id)
            for (item in mStackLibrary) {
                if (item.value.first == mStackLibrary.size) {
                    mLibrary = item.value.second
                    mListBookFull.value = item.value.third.toMutableList()
                    mListBook.value = item.value.third.toMutableList()
                    setSuggestions(mListBookFull.value)
                    break
                }
            }
        }
    }

    fun addStackLibrary(id: String, library: Library) = mStackLibrary.put(id, Triple(mStackLibrary.size + 1, library, mListBookFull.value!!))

    fun removeStackLibrary(id: String) = mStackLibrary.remove(id)

    fun emptyList(idLibrary: Long) {
        if (mLibrary.id == idLibrary) {
            mListBookFull.value = mutableListOf()
            mListBook.value = mutableListOf()
            setSuggestions(mListBookFull.value)
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
            mListBookFull.value!!.add(position, Book)
        } else {
            mListBook.value!!.add(Book)
            mListBookFull.value!!.add(Book)
        }
    }

    fun delete(obj: Book) {
        mBookRepository.delete(obj)
        remove(obj)
    }

    fun getAndRemove(position: Int): Book? {
        val Book = if (mListBook.value != null) mListBook.value!!.removeAt(position) else null
        if (Book != null) mListBookFull.value!!.remove(Book)
        return Book
    }

    fun remove(Book: Book) {
        if (mListBookFull.value != null) {
            mListBook.value!!.remove(Book)
            mListBookFull.value!!.remove(Book)
        }
    }

    fun remove(position: Int) {
        if (mListBookFull.value != null) {
            val Book = mListBook.value!!.removeAt(position)
            mListBookFull.value!!.remove(Book)
        }
    }

    fun update(list: List<Book>) {
        if (list.isNotEmpty()) {
            for (Book in list) {
                if (!mListBookFull.value!!.contains(Book)) {
                    mListBook.value!!.add(Book)
                    mListBookFull.value!!.add(Book)
                }
            }
        }
    }

    fun setList(list: ArrayList<Book>) {
        mListBook.value = list
        mListBookFull.value = list.toMutableList()
        setSuggestions(list)
    }

    fun addList(Book: Book): Int {
        var index = -1
        if (!mListBookFull.value!!.contains(Book)) {
            index = mListBook.value!!.size
            mListBook.value!!.add(Book)
            mListBookFull.value!!.add(Book)
        }

        return index
    }

    fun remList(Book: Book): Int {
        var index = -1

        if (mListBookFull.value!!.contains(Book)) {
            index = mListBook.value!!.indexOf(Book)
            mListBook.value!!.remove(Book)
            mListBookFull.value!!.remove(Book)
        }

        return index
    }

    fun updateList(index : Int) : Int {
        val book = mListBook.value!![index]
        mBookRepository.get(book.id!!)?.let {
            book.update(it, true)
        }
        return index
    }

    fun updateList(refreshComplete: (Boolean, indexes: MutableList<Pair<ListMode, Int>>) -> (Unit)) {
        var change = false
        val indexes = mutableListOf<Pair<ListMode, Int>>()
        if (mListBookFull.value != null && mListBookFull.value!!.isNotEmpty()) {
            val list = mBookRepository.listRecentChange(mLibrary)
            if (list.isNotEmpty()) {
                change = true
                for (Book in list) {
                    if (mListBookFull.value!!.contains(Book)) {
                        if (mListBookFull.value!![mListBookFull.value!!.indexOf(Book)].update(Book, true)) {
                            val index = mListBook.value!!.indexOf(Book)
                            if (index > -1)
                                indexes.add(Pair(ListMode.MOD, index))
                        }
                    } else {
                        mListBook.value!!.add(Book)
                        mListBookFull.value!!.add(Book)
                        indexes.add(Pair(ListMode.ADD, mListBook.value!!.size))
                    }
                }
            }
            val listDel = mBookRepository.listRecentDeleted(mLibrary)
            if (listDel.isNotEmpty()) {
                change = true
                for (Book in listDel) {
                    if (mListBookFull.value!!.contains(Book)) {
                        val index = mListBook.value!!.indexOf(Book)
                        mListBook.value!!.remove(Book)
                        mListBookFull.value!!.remove(Book)
                        indexes.add(Pair(ListMode.REM, index))
                    }
                }
            }
        } else {
            val list = mBookRepository.list(mLibrary)
            indexes.add(Pair(ListMode.FULL, list.size))
            mListBook.value = list.toMutableList()
            mListBookFull.value = list.toMutableList()
            //Receive value force refresh, not necessary notify
            change = false
        }

        setSuggestions(mListBookFull.value)
        refreshComplete(change, indexes)
    }

    fun list(refreshComplete: (Boolean) -> (Unit)) {
        mLoading.value = true
        CoroutineScope(Dispatchers.IO).launch {
            async {
                val list = mBookRepository.list(mLibrary)
                withContext(Dispatchers.Main) {
                    mLoading.value = false

                    if (mListBookFull.value == null || mListBookFull.value!!.isEmpty()) {
                        mListBook.value = list.toMutableList()
                        mListBookFull.value = list.toMutableList()
                        setSuggestions(mListBookFull.value)
                    } else
                        update(list)

                    refreshComplete(mListBook.value!!.isNotEmpty())
                }
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
            LibraryBookType.SEPARATOR_MEDIUM -> LibraryBookType.LINE
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

        if (isDesc)
            when (order) {
                Order.Date -> {
                    mListBookFull.value!!.sortByDescending { it.dateCreate }
                    mListBook.value!!.sortByDescending { it.dateCreate }
                }
                Order.LastAccess -> {
                    mListBookFull.value!!.sortWith(compareByDescending<Book> { it.lastAccess }.thenByDescending { it.name })
                    mListBook.value!!.sortWith(compareByDescending<Book> { it.lastAccess }.thenByDescending { it.name })
                }
                Order.Favorite -> {
                    mListBookFull.value!!.sortWith(compareByDescending<Book> { it.favorite }.thenByDescending { it.name })
                    mListBook.value!!.sortWith(compareByDescending<Book> { it.favorite }.thenByDescending { it.name })
                }
                Order.Author -> {
                    mListBookFull.value!!.sortWith(compareByDescending<Book> { it.author }.thenByDescending { it.name })
                    mListBook.value!!.sortWith(compareByDescending<Book> { it.author }.thenByDescending { it.name })
                }
                else -> {
                    mListBookFull.value!!.sortByDescending { it.name }
                    mListBook.value!!.sortByDescending { it.name }
                }
            }
        else
            when (order) {
                Order.Date -> {
                    mListBookFull.value!!.sortBy { it.dateCreate }
                    mListBook.value!!.sortBy { it.dateCreate }
                }
                Order.LastAccess -> {
                    mListBookFull.value!!.sortWith(compareByDescending<Book> { it.lastAccess }.thenBy { it.name })
                    mListBook.value!!.sortWith(compareByDescending<Book> { it.lastAccess }.thenBy { it.name })
                }
                Order.Favorite -> {
                    mListBookFull.value!!.sortWith(compareByDescending<Book> { it.favorite }.thenBy { it.name })
                    mListBook.value!!.sortWith(compareByDescending<Book> { it.favorite }.thenBy { it.name })
                }
                Order.Author -> {
                    mListBookFull.value!!.sortWith(compareByDescending<Book> { it.author }.thenBy { it.name })
                    mListBook.value!!.sortWith(compareByDescending<Book> { it.author }.thenBy { it.name })
                }
                else -> {
                    mListBookFull.value!!.sortBy { it.name }
                    mListBook.value!!.sortBy { it.name }
                }
            }
    }

    private fun setSuggestions(list : List<Book>?) {
        mSuggestionAuthor = setOf()
        mSuggestionPublisher = setOf()

        if (list.isNullOrEmpty())
            return

        val process = list.parallelStream().collect(Collectors.toList())

        CoroutineScope(newSingleThreadContext("SuggestionThread")).launch {
            async {
                try {
                    val authors = mutableSetOf<String>()
                    val publishers = mutableSetOf<String>()
                    val tags = mutableSetOf<String>()

                    process.forEach {
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
                    Firebase.crashlytics.apply {
                        setCustomKey("message", "Error generate suggestion: " + e.message)
                        recordException(e)
                    }
                }
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

    fun clearFilter() {
        val newList: MutableList<Book> = mutableListOf()
        newList.addAll(mListBookFull.value!!.filter(Objects::nonNull))
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
            mWordFilter = constraint.toString()
            val filteredList: MutableList<Book> = mutableListOf()

            if (constraint.isNullOrEmpty() && mTypeFilter.value == FilterType.None) {
                filteredList.addAll(mListBookFull.value!!.filter(Objects::nonNull))
            } else {
                var filterPattern = constraint.toString()
                val filterCondition = arrayListOf<Pair<FilterType, String>>()
                constraint?.contains('@').run {
                    val m = Pattern.compile("(@\\S*:([^\"]\\S*|\".+?\"\\s*))").matcher(constraint)
                    while (m.find()) {
                        val item = m.group(1)?.replace("\"", "") ?: continue
                        filterPattern = filterPattern.replace(m.group(1)!!, "", true)
                        val type = Util.stringToFilter(app.applicationContext, Type.BOOK, item.substringBefore(":").replace("@", ""))
                        if (type != FilterType.None) {
                            val condition = item.substringAfter(":")
                            if (condition.isNotEmpty())
                                filterCondition.add(Pair(type, condition))
                        }
                    }
                }

                filterPattern = filterPattern.lowercase(Locale.getDefault()).trim()
                filteredList.addAll(mListBookFull.value!!.filter {
                    filtered(it, filterPattern, filterCondition)
                })
            }

            val results = FilterResults()
            results.values = filteredList

            return results
        }

        override fun publishResults(constraint: CharSequence?, filterResults: FilterResults?) {
            val list = mutableListOf<Book>()
            filterResults?.let {
                list.addAll(it.values as Collection<Book>)
            }
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
                    mListBookFull.value?.find { book -> book.id == item.id }?.let { book ->
                        book.favorite = item.favorite
                        book.bookMark = item.bookMark
                        book.pages = item.pages
                        book.completed = item.completed
                        book.lastAccess = item.lastAccess
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
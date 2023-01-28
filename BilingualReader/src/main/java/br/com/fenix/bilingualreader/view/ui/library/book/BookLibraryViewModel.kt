package br.com.fenix.bilingualreader.view.ui.library.book

import android.app.Application
import android.widget.Filter
import android.widget.Filterable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.enums.ListMod
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import java.util.*
import br.com.fenix.bilingualreader.model.enums.Filter as FilterType

class BookLibraryViewModel(application: Application) : AndroidViewModel(application), Filterable {

    private var mStackLibrary = mutableMapOf<String, Triple<Int, Library, MutableList<Book>>>()
    private var mLibrary: Library = Library(GeneralConsts.KEYS.LIBRARY.DEFAULT_BOOK)
    private val mBookRepository: BookRepository = BookRepository(application.applicationContext)
    private val mPreferences = GeneralConsts.getSharedPreferences(application.applicationContext)

    private var mWordFilter = ""

    private var mOrder = MutableLiveData(Pair(Order.Name, false))
    val order: LiveData<Pair<Order, Boolean>> = mOrder
    private var mTypeFilter = MutableLiveData(FilterType.None)
    val typeFilter: LiveData<br.com.fenix.bilingualreader.model.enums.Filter> = mTypeFilter

    private var mListBookFull = MutableLiveData<MutableList<Book>>(mutableListOf())
    private var mListBook = MutableLiveData<MutableList<Book>>(mutableListOf())
    val listBook: LiveData<MutableList<Book>> = mListBook

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
            mLibrary.menuKey
        mPreferences.edit().putLong(
            GeneralConsts.KEYS.LIBRARY.LAST_LIBRARY,
            key.toLong()
        ).apply()
    }

    fun getLibrary() =
        mLibrary

    fun existStack(id: String): Boolean =
        mStackLibrary.contains(id)

    fun restoreLastStackLibrary(id: String) {
        if (mStackLibrary.contains(id)) {
            mStackLibrary.remove(id)
            for (item in mStackLibrary) {
                if (item.value.first == mStackLibrary.size) {
                    mLibrary = item.value.second
                    mListBookFull.value = item.value.third.toMutableList()
                    mListBook.value = item.value.third.toMutableList()
                    break
                }
            }
        }
    }

    fun addStackLibrary(id: String, library: Library) =
        mStackLibrary.put(id, Triple(mStackLibrary.size + 1, library, mListBookFull.value!!))

    fun removeStackLibrary(id: String) =
        mStackLibrary.remove(id)

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

    fun updateList(refreshComplete: (Boolean, indexes: MutableList<Pair<ListMod, Int>>) -> (Unit)) {
        var change = false
        val indexes = mutableListOf<Pair<ListMod, Int>>()
        if (mListBookFull.value != null && mListBookFull.value!!.isNotEmpty()) {
            val list = mBookRepository.listRecentChange(mLibrary)
            if (list != null && list.isNotEmpty()) {
                change = true
                for (Book in list) {
                    if (mListBookFull.value!!.contains(Book)) {
                        mListBookFull.value!![mListBookFull.value!!.indexOf(Book)].update(Book)
                        val index = mListBook.value!!.indexOf(Book)
                        if (index > -1)
                            indexes.add(Pair(ListMod.MOD, index))
                    } else {
                        mListBook.value!!.add(Book)
                        mListBookFull.value!!.add(Book)
                        indexes.add(Pair(ListMod.ADD, mListBook.value!!.size))
                    }
                }
            }
            val listDel = mBookRepository.listRecentDeleted(mLibrary)
            if (listDel != null && listDel.isNotEmpty()) {
                change = true
                for (Book in listDel) {
                    if (mListBookFull.value!!.contains(Book)) {
                        val index = mListBook.value!!.indexOf(Book)
                        mListBook.value!!.remove(Book)
                        mListBookFull.value!!.remove(Book)
                        indexes.add(Pair(ListMod.REM, index))
                    }
                }
            }
        } else {
            val list = mBookRepository.list(mLibrary)
            if (list != null) {
                indexes.add(Pair(ListMod.FULL, list.size))
                mListBook.value = list.toMutableList()
                mListBookFull.value = list.toMutableList()
            } else {
                mListBook.value = mutableListOf()
                mListBookFull.value = mutableListOf()
                indexes.add(Pair(ListMod.FULL, 0))
            }
            //Receive value force refresh, not necessary notify
            change = false
        }

        refreshComplete(change, indexes)
    }

    fun list(refreshComplete: (Boolean) -> (Unit)) {
        val list = mBookRepository.list(mLibrary)
        if (list != null) {
            if (mListBookFull.value == null || mListBookFull.value!!.isEmpty()) {
                mListBook.value = list.toMutableList()
                mListBookFull.value = list.toMutableList()
            } else
                update(list)
        } else {
            mListBookFull.value = mutableListOf()
            mListBook.value = mutableListOf()
        }

        refreshComplete(mListBook.value!!.isNotEmpty())
    }

    fun clearHistory(book: Book) {
        mBookRepository.clearHistory(book)
    }

    fun isEmpty(): Boolean =
        mListBook.value == null || mListBook.value!!.isEmpty()

    fun sorted(order: Order) {
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
            else -> {
                mListBookFull.value!!.sortBy { it.name }
                mListBook.value!!.sortBy { it.name }
            }
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

    private val mBookFilter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filteredList: MutableList<Book> = mutableListOf()

            if (constraint == null || constraint.isEmpty()) {
                filteredList.addAll(mListBookFull.value!!.filter(Objects::nonNull))
            } else {
                val filterPattern = constraint.toString().lowercase(Locale.getDefault()).trim()

                filteredList.addAll(mListBookFull.value!!.filter(Objects::nonNull).filter {
                    it.name.lowercase(Locale.getDefault()).contains(filterPattern) ||
                            it.extension.lowercase(Locale.getDefault()).contains(filterPattern)
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
}
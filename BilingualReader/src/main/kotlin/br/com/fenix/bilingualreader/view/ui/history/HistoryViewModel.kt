package br.com.fenix.bilingualreader.view.ui.history

import android.app.Application
import android.widget.Filter
import android.widget.Filterable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.model.interfaces.History
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.LibraryRepository
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import java.time.format.DateTimeFormatter
import java.util.Locale

class HistoryViewModel(application: Application) : AndroidViewModel(application), Filterable {

    private val mLibraryRepository: LibraryRepository = LibraryRepository(application.applicationContext)

    private val mMangaRepository: MangaRepository = MangaRepository(application.applicationContext)
    private val mBookRepository: BookRepository = BookRepository(application.applicationContext)

    private val mDefaultKey = -3L
    val mDefaultLibrary = Library(mDefaultKey, application.applicationContext.getString(R.string.history_library_default), "", excluded = true)

    private var mLibrary: Library? = null
    private var mType: Type? = null
    private var mFilter: String = ""

    private var mListFull = MutableLiveData<ArrayList<History>>(arrayListOf())
    private var mList = MutableLiveData<ArrayList<History>>(arrayListOf())
    val history: LiveData<ArrayList<History>> = mList

    fun list() {
        var list = mutableListOf<History>()

        val mangas = mMangaRepository.listHistory()
        if (mangas != null)
            list.addAll(mangas)

        val books = mBookRepository.listHistory()
        if (books != null)
            list.addAll(books)

        val format = DateTimeFormatter.ofPattern(GeneralConsts.PATTERNS.DATE_TIME_PATTERN)
        list = list.sortedByDescending { it.lastAccess }.distinctBy { it.lastAccess!!.format(format) }.toMutableList()

        mListFull.value = ArrayList(list)
        mList.value = ArrayList(list)
    }

    fun list(refreshComplete: (Int) -> (Unit)) {
        var list = mutableListOf<History>()

        val mangas = mMangaRepository.listHistory()
        if (mangas != null)
            list.addAll(mangas)

        val books = mBookRepository.listHistory()
        if (books != null)
            list.addAll(books)

        val format = DateTimeFormatter.ofPattern(GeneralConsts.PATTERNS.DATE_TIME_PATTERN)
        list = list.sortedByDescending { it.lastAccess }.distinctBy { it.lastAccess!!.format(format) }.toMutableList()

        if (mList.value == null || mList.value!!.isEmpty()) {
            mList.value = ArrayList(list)
            mListFull.value = ArrayList(list)
        } else
            update(list)

        refreshComplete(mList.value!!.size - 1)
    }

    fun update(list: List<History>) {
        if (list.isNotEmpty()) {
            for (history in list) {
                if (!mList.value!!.contains(history))
                    mList.value!!.add(history)

                if (!mListFull.value!!.contains(history))
                    mListFull.value!!.add(history)
            }
        }
    }

    fun updateDelete(history: History) {
        when (history) {
            is Manga ->  mMangaRepository.delete(history)
            is Book ->  mBookRepository.delete(history)
        }
    }

    fun updateLastAccess(history: History) {
        when (history) {
            is Manga ->  mMangaRepository.update(history)
            is Book ->  mBookRepository.update(history)
        }
    }

    fun clear(history: History?) {
        if (history != null) {
            save(history)
            if (mList.value!!.contains(history))
                mList.value!!.remove(history)

            if (mListFull.value!!.contains(history))
                mListFull.value!!.remove(history)
        }
    }

    fun deletePermanent(history: History?) {
        history ?: return

        when (history) {
            is Manga -> mMangaRepository.deletePermanent(history)
            is Book -> mBookRepository.deletePermanent(history)
        }
    }

    fun save(history: History?) {
        history ?: return

        when (history) {
            is Manga -> {
                if (history.id == 0L)
                    history.id = mMangaRepository.save(history)
                else
                    mMangaRepository.update(history)
            }
            is Book -> {
                if (history.id == 0L)
                    history.id = mBookRepository.save(history)
                else
                    mBookRepository.update(history)
            }
        }

    }

    fun remove(history: History) {
        if (mList.value != null && mList.value!!.contains(history))
            mList.value!!.remove(history)

        if (mListFull.value != null && mListFull.value!!.contains(history))
            mListFull.value!!.remove(history)
    }

    fun add(history: History, index: Int) {
        if (mList.value != null)
            mList.value!!.add(index, history)

        if (mListFull.value != null)
            mListFull.value!!.add(index, history)
    }

    fun getAndRemove(position: Int): History? {
        val manga = if (mList.value != null) mList.value!!.removeAt(position) else null

        if (mList.value != null && mList.value!!.contains(manga))
            mList.value!!.remove(manga)

        return manga
    }

    private fun filterList(): ArrayList<History> {
        val list = arrayListOf<History>()

        val isTitleId = null
        var title: History? = null
        if (mListFull.value != null && mListFull.value!!.isNotEmpty())
            for (history in mListFull.value!!) {
                if (history == null)
                    continue

                if (history.id == isTitleId) {
                    title = history
                    continue
                }

                if (mType != null && history.type != mType)
                    continue

                if (mLibrary != null) {
                    val key = if (mLibrary!!.id == mDefaultKey) {
                        when (history) {
                            is Manga -> GeneralConsts.KEYS.LIBRARY.DEFAULT_MANGA
                            is Book -> GeneralConsts.KEYS.LIBRARY.DEFAULT_BOOK
                            else -> mLibrary!!.id
                        }
                    } else
                        mLibrary!!.id

                    if (history.fkLibrary != key)
                        continue
                }

                if (mFilter.isNotEmpty()) {
                    if (history.name.lowercase(Locale.getDefault()).contains(mFilter) || history.fileType.compareExtension(mFilter)) {
                        if (title != null) {
                            list.add(title)
                            title = null
                        }
                        list.add(history)
                    }
                } else {
                    if (title != null) {
                        list.add(title)
                        title = null
                    }
                    list.add(history)
                }
            }

        return list
    }

    fun filterLibrary(library: Library?) {
        if (library == mLibrary)
            return

        mLibrary = library
        mList.value = filterList()
    }
    fun filterType(type: Type?) {
        if (type == mType)
            return

        mType = type
        mList.value = filterList()
    }

    override fun getFilter(): Filter {
        return mHistoryFilter
    }

    private val mHistoryFilter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            mFilter = constraint.toString().lowercase(Locale.getDefault()).trim()
            val results = FilterResults()
            results.values = filterList()
            return results
        }

        override fun publishResults(constraint: CharSequence?, filterResults: FilterResults?) {
            val list = arrayListOf<History>()
            filterResults?.let {
                list.addAll(it.values as Collection<History>)
            }
            mList.value = list
        }
    }

    fun getLibraryList(): List<Library> {
        val list = mutableListOf<Library>()
        list.addAll(mLibraryRepository.list(Type.MANGA))
        list.addAll(mLibraryRepository.list(Type.BOOK))
        return list
    }

}
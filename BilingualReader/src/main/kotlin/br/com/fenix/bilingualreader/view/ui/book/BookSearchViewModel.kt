package br.com.fenix.bilingualreader.view.ui.book

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.BookSearch
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.service.repository.BookSearchRepository
import br.com.fenix.bilingualreader.util.helpers.ColorUtil
import br.com.fenix.bilingualreader.util.helpers.TextUtil
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil.ThemeUtils.getColorFromAttr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

class BookSearchViewModel(var app: Application) : AndroidViewModel(app) {

    private val mRepository: BookSearchRepository = BookSearchRepository(app.applicationContext)

    private val mLOGGER = LoggerFactory.getLogger(BookSearchViewModel::class.java)

    private var mColorHighlight = app.applicationContext.getColorFromAttr(R.attr.colorTertiary)

    private var last = ""
    var book: Book? = null
    var parse: DocumentParse? = null
    var stopSearch: Boolean = false

    private var mInSearching: MutableLiveData<Boolean> = MutableLiveData(false)
    val inSearching: LiveData<Boolean> = mInSearching

    private var mListHistory: MutableLiveData<MutableList<BookSearch>> = MutableLiveData(mutableListOf())
    val history: LiveData<MutableList<BookSearch>> = mListHistory

    private var mListSearch: MutableLiveData<List<BookSearch>> = MutableLiveData(listOf())
    val search: LiveData<List<BookSearch>> = mListSearch

    fun deleteAll() {
        mListHistory.value = mutableListOf()
        if (book != null)
            mRepository.delete(book!!.id!!)
    }

    fun save(obj: BookSearch) {
        if (obj.id == null)
            obj.id = mRepository.save(obj)
        else
            mRepository.update(obj)
        mListHistory.value!!.add(obj)
    }

    fun delete(obj: BookSearch) {
        if (mListHistory.value!!.contains(obj))
            mListHistory.value = mListHistory.value!!.filterNot { it == obj }.toMutableList()

        mRepository.delete(obj)
    }

    fun initialize(context: Context, book: Book, parse: DocumentParse) {
        mColorHighlight = context.getColorFromAttr(R.attr.colorTertiary)
        last = ""
        this.book = book
        this.parse = parse

        if (book.id != null)
            mListHistory.value = mRepository.findAll(book.id!!).toMutableList()
    }

    fun getPageCount(): Int = parse?.pageCount ?: 0

    fun clearSearch() {
        mListSearch.value = mutableListOf()
    }

    fun search(text: String) {
        if (text.isEmpty() || last.equals(text.trim(), true))
            return

        if (parse != null && book != null) {
            last = text.trim()
            save(BookSearch(book!!.id!!, text))
            CoroutineScope(Dispatchers.IO).launch {
                if (inSearching.value!!) {
                    stopSearch = true
                    delay(300L)
                }

                withContext(Dispatchers.Main) {
                    clearSearch()
                    mInSearching.value = true
                }

                val color = ColorUtil.getColor(mColorHighlight)
                stopSearch = false

                val list = mutableListOf<BookSearch>()
                val texts: MutableList<BookSearch> = arrayListOf()
                val chapters = parse!!.getChapters()
                var title = BookSearch(book!!.id!!, "", 0f)

                val deferred = async {
                    for (i in 0 until parse!!.pageCount) {
                        val lines =
                            TextUtil.formatHtml(parse!!.getPage(i).pageHTML, "||").split("||").filter { it.isNotEmpty() }

                        if (stopSearch)
                            break

                        for (line in lines)
                            if (line.contains(text, true)) {
                                val chapter = chapters.lastOrNull { it.first <= i }
                                if (chapter != null && title.chapter != chapter.first.toFloat()) {
                                    title = BookSearch(book!!.id!!, chapter.second, chapter.first.toFloat())
                                    texts.add(title)
                                }

                                val index = line.indexOf(text, ignoreCase = true)
                                val contain = line.substring(index, index + text.length)
                                val search = TextUtil.highlightWordInText(line, contain, color)
                                texts.add(BookSearch(book!!.id!!, search, i, title))
                            }

                        if (stopSearch)
                            break

                        if (texts.isNotEmpty())
                            list.addAll(texts)
                        texts.clear()
                        withContext(Dispatchers.Main) {
                            mListSearch.value = list.toMutableList()
                        }
                    }
                }
                deferred.await()
                withContext(Dispatchers.Main) {
                    mInSearching.value = false
                }
            }
        }
    }

}
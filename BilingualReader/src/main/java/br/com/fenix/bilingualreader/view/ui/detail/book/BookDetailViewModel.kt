package br.com.fenix.bilingualreader.view.ui.detail.book

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.FileLink
import br.com.fenix.bilingualreader.model.entity.Information
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.FileLinkRepository
import org.slf4j.LoggerFactory

class BookDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val mLOGGER = LoggerFactory.getLogger(BookDetailViewModel::class.java)

    private val mBookRepository: BookRepository = BookRepository(application.applicationContext)
    private val mFileLinkRepository: FileLinkRepository =
        FileLinkRepository(application.applicationContext)

    var library: Library? = null
    private var mBook = MutableLiveData<Book?>(null)
    val book: LiveData<Book?> = mBook

    private var mPaths: Map<String, Int> = mapOf()

    private var mListChapters = MutableLiveData<MutableList<String>>(mutableListOf())
    val listChapters: LiveData<MutableList<String>> = mListChapters

    private var mListFileLinks = MutableLiveData<MutableList<FileLink>>(mutableListOf())
    val listFileLinks: LiveData<MutableList<FileLink>> = mListFileLinks

    private var mListSubtitles = MutableLiveData<MutableList<String>>(mutableListOf())
    val listSubtitles: LiveData<MutableList<String>> = mListSubtitles

    private var mInformation = MutableLiveData<Information?>(null)
    val information: LiveData<Information?> = mInformation

    fun setBook(context: Context, book: Book) {
        mBook.value = book

        mListFileLinks.value = if (book.id != null) mFileLinkRepository.findAllByManga(book.id!!)
            ?.toMutableList() else mutableListOf()

        mInformation.value = Information(context, book)
    }

    fun getPage(folder: String): Int {
        return mPaths[folder]?.plus(1) ?: mBook.value?.bookMark ?: 1
    }

    fun clear() {
        mBook.value = null
        mListFileLinks.value = mutableListOf()
        mListChapters.value = mutableListOf()
        mListSubtitles.value = mutableListOf()
    }

    fun delete() {
        if (mBook.value != null)
            mBookRepository.delete(mBook.value!!)
    }

    fun save(book: Book?) {
        book ?: return
        mBookRepository.update(book)
        mBook.value = book
    }

    fun markRead() {
        mBook.value ?: return
        mBookRepository.markRead(mBook.value)
        mBook.value = mBook.value
    }

    fun clearHistory() {
        mBook.value ?: return
        mBookRepository.clearHistory(mBook.value)
        mBook.value = mBook.value
    }

}
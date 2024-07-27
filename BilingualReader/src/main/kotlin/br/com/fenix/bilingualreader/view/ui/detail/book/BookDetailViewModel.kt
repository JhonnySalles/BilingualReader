package br.com.fenix.bilingualreader.view.ui.detail.book

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Information
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.LinkedFile
import br.com.fenix.bilingualreader.model.entity.Tags
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.service.controller.BookImageCoverController
import br.com.fenix.bilingualreader.service.listener.ApiListener
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.FileLinkRepository
import br.com.fenix.bilingualreader.service.repository.TagsRepository
import br.com.fenix.bilingualreader.service.tracker.ParseInformation
import br.com.fenix.bilingualreader.service.tracker.mal.MalMangaDetail
import br.com.fenix.bilingualreader.service.tracker.mal.MyAnimeListTracker
import br.com.fenix.bilingualreader.util.helpers.Util
import org.slf4j.LoggerFactory

class BookDetailViewModel(var app: Application) : AndroidViewModel(app) {

    private val mLOGGER = LoggerFactory.getLogger(BookDetailViewModel::class.java)

    private val mBookRepository: BookRepository = BookRepository(app.applicationContext)
    private val mFileLinkRepository: FileLinkRepository = FileLinkRepository(app.applicationContext)
    private val mTagsRepository: TagsRepository = TagsRepository(app.applicationContext)

    var library: Library? = null
    private var mBook = MutableLiveData<Book?>(null)
    val book: LiveData<Book?> = mBook

    private var mCover = MutableLiveData<Bitmap?>(null)
    val cover: LiveData<Bitmap?> = mCover

    private var mPaths: Map<String, Int> = mapOf()

    private var mListChapters = MutableLiveData<MutableList<String>>(mutableListOf())
    val listChapters: LiveData<MutableList<String>> = mListChapters

    private var mListLinkedFileLinks = MutableLiveData<MutableList<LinkedFile>>(mutableListOf())
    val listLinkedFileLinks: LiveData<MutableList<LinkedFile>> = mListLinkedFileLinks

    private var mListSubtitles = MutableLiveData<MutableList<String>>(mutableListOf())
    val listSubtitles: LiveData<MutableList<String>> = mListSubtitles

    private var mInformation = MutableLiveData<Information?>(null)
    val information: LiveData<Information?> = mInformation

    private var mWebInformation = MutableLiveData<Information?>(null)
    val webInformation: LiveData<Information?> = mWebInformation

    private var mWebInformationRelations = MutableLiveData<MutableList<Information>>(mutableListOf())
    val webInformationRelations: LiveData<MutableList<Information>> = mWebInformationRelations

    private var mTags = MutableLiveData<List<Tags>>(listOf())
    val tags: LiveData<List<Tags>> = mTags

    private val mTracker = MyAnimeListTracker(app.applicationContext)

    fun setBook(context: Context, book: Book) {
        mBook.value = book

        mListLinkedFileLinks.value = if (book.id != null) mFileLinkRepository.findAllByManga(book.id!!)?.toMutableList() else mutableListOf()
        mWebInformation.value = null
        mInformation.value = Information(context, book)
        mTags.value = mTagsRepository.list().filter { t -> book.tags.any { b -> b.compareTo(t.id!!) == 0 } }
        mWebInformationRelations.value = mutableListOf()

        BookImageCoverController.instance.setImageCoverAsync(context, book,  false) { mCover.value = it }
    }

    fun getInformation() {
        var name = mBook.value?.title ?: ""

        if (name.isEmpty())
            return

        name = Util.getNameFromMangaTitle(name).replace(" ", "%")
        mTracker.getListNovel(name, object : ApiListener<List<MalMangaDetail>> {
            override fun onSuccess(result: List<MalMangaDetail>) {
                setInformation(result)
            }

            override fun onFailure(message: String) {
                mLOGGER.warn("Error to search manga info", message)
            }
        })

    }

    private val PATTERN = Regex("[^\\w\\s]")
    fun <T> setInformation(mangas: List<T>) {
        val list = ParseInformation.getInformation(app.applicationContext, mangas)

        val name = Util.getNameFromMangaTitle(mBook.value?.title ?: "").replace(PATTERN, "")

        mWebInformation.value = list.find {
            it.title.replace(PATTERN, "").trim().equals(name, true) || it.alternativeTitles.contains(name, true)
        }
        if (mWebInformation.value != null)
            list.remove(mWebInformation.value)

        mWebInformationRelations.value = list
    }

    fun getPage(folder: String): Int {
        return mPaths[folder]?.plus(1) ?: mBook.value?.bookMark ?: 1
    }

    fun clear() {
        mBook.value = null
        mListLinkedFileLinks.value = mutableListOf()
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

    fun changeLanguage(languages: Languages) {
        mBook.value?.language = languages
        save(mBook.value)
    }

}
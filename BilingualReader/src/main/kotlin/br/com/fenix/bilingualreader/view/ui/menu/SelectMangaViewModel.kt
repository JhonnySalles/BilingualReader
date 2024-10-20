package br.com.fenix.bilingualreader.view.ui.menu

import android.app.Application
import android.widget.Filter
import android.widget.Filterable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.LibraryRepository
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import br.com.fenix.bilingualreader.util.helpers.LibraryUtil
import br.com.fenix.bilingualreader.util.helpers.Util
import org.slf4j.LoggerFactory
import java.util.Locale


class SelectMangaViewModel(application: Application) : AndroidViewModel(application), Filterable {

    private val mLOGGER = LoggerFactory.getLogger(SelectMangaViewModel::class.java)

    private val mLibraryRepository: LibraryRepository = LibraryRepository(application.applicationContext)
    private val mMangaRepository: MangaRepository = MangaRepository(application.applicationContext)

    private var mDefaultLibrary = LibraryUtil.getDefault(application.applicationContext, Type.MANGA)
    private var mLibrary: Library = mDefaultLibrary

    private var mListMangasFull = MutableLiveData<MutableList<Manga>>(mutableListOf())
    private var mListMangas = MutableLiveData<MutableList<Manga>>(mutableListOf())
    val listMangas: LiveData<MutableList<Manga>> = mListMangas

    var id: Long = -1
    var manga: String = ""

    fun clearMangaSelected() {
        id = -1
        manga = ""
    }

    fun setDefaultLibrary(library: Library) {
        if (mLibrary.id == library.id)
            mLibrary = library
    }

    fun setDefaultLibrary(language: Libraries) {
        mLibrary = mLibraryRepository.get(Type.MANGA, language) ?: mDefaultLibrary
    }

    fun setLibrary(library: Library) {
        if (mLibrary.id != library.id) {
            mListMangasFull.value = mutableListOf()
            mListMangas.value = mutableListOf()
        }
        mLibrary = library
    }

    fun changeLibrary(library: Library) {
        mListMangasFull.value = mutableListOf()
        mListMangas.value = mutableListOf()
        mLibrary = library

        val list = mMangaRepository.list(mLibrary)
        if (list.isNullOrEmpty()) {
            mListMangasFull.value = mutableListOf()
            mListMangas.value = mutableListOf()
        } else {
            mListMangas.value = list.toMutableList()
            mListMangasFull.value = list.toMutableList()
        }

        prepareList()
    }

    fun getLibrary() = mLibrary

    fun list(id: Long, manga: String, refreshComplete: (Boolean) -> (Unit)) {
        this.id = id
        this.manga = manga

        val list = mMangaRepository.list(mLibrary)
        if (list != null) {
            if (mListMangasFull.value == null || mListMangasFull.value!!.isEmpty()) {
                mListMangas.value = list.toMutableList()
                mListMangasFull.value = list.toMutableList()
            } else {
                for (manga in list) {
                    if (!mListMangasFull.value!!.contains(manga)) {
                        mListMangas.value!!.add(manga)
                        mListMangasFull.value!!.add(manga)
                    }
                }
            }
        } else {
            mListMangasFull.value = mutableListOf()
            mListMangas.value = mutableListOf()
        }

        prepareList()
        refreshComplete(mListMangas.value!!.isNotEmpty())
    }

    fun getLibraryList(): List<Library> {
        val list = mutableListOf<Library>()
        list.add(mDefaultLibrary)
        list.addAll(mLibraryRepository.list(Type.MANGA))
        return list
    }

    private fun prepareList() {
        mListMangasFull.value?.removeIf { it.id == id }
        mListMangas.value?.removeIf { it.id == id }
        sorted(manga)
    }

    fun sorted(manga: String) {
        val name = Util.getNameWithoutVolumeAndChapter(manga)
        mListMangasFull.value!!.sortWith(compareByDescending<Manga> { it.name.contains(name) }.thenBy { it.name })
        mListMangas.value!!.sortWith(compareByDescending<Manga> { it.name.contains(name) }.thenBy { it.name })
    }

    override fun getFilter(): Filter {
        return mMangaFilter
    }

    private val mMangaFilter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filteredList: MutableList<Manga> = mutableListOf()

            if (constraint == null || constraint.isEmpty()) {
                filteredList.addAll(mListMangasFull.value!!)
            } else {
                val filterPattern = constraint.toString().lowercase(Locale.getDefault()).trim()

                filteredList.addAll(mListMangasFull.value!!.filter {
                    it.name.lowercase(Locale.getDefault()).contains(filterPattern) ||
                            it.fileType.compareExtension(filterPattern)
                })
            }

            val results = FilterResults()
            results.values = filteredList

            return results
        }

        override fun publishResults(constraint: CharSequence?, filterResults: FilterResults?) {
            val list = mutableListOf<Manga>()
            filterResults?.let {
                list.addAll(it.values as Collection<Manga>)
            }
            mListMangas.value = list
        }
    }

}
package br.com.fenix.bilingualreader.view.ui.menu

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.enums.FontType
import br.com.fenix.bilingualreader.model.enums.Themes
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.LibraryRepository

class ConfigLibrariesViewModel(application: Application) : AndroidViewModel(application) {

    private val mRepository: LibraryRepository = LibraryRepository(application.applicationContext)

    private var mLibraryFull = mutableListOf<Library>()
    private var mListLibraries = MutableLiveData<MutableList<Library>>(mutableListOf())
    val libraries: LiveData<MutableList<Library>> = mListLibraries

    private var mListThemes: MutableLiveData<MutableList<Pair<Themes, Boolean>>> = MutableLiveData(arrayListOf())
    val themes: LiveData<MutableList<Pair<Themes, Boolean>>> = mListThemes

    private var mListFontsNormal: MutableLiveData<MutableList<Pair<FontType, Boolean>>> = MutableLiveData(arrayListOf())
    val fontsNormal: LiveData<MutableList<Pair<FontType, Boolean>>> = mListFontsNormal

    private var mListFontsJapanese: MutableLiveData<MutableList<Pair<FontType, Boolean>>> = MutableLiveData(arrayListOf())
    val fontsJapanese: LiveData<MutableList<Pair<FontType, Boolean>>> = mListFontsJapanese


    fun newLibrary(library: Library) {
        if (library.title.isEmpty() || library.path.isEmpty())
            return

        if (mListLibraries.value!!.contains(library)) {
            val item = mListLibraries.value!!.find { it == library }
            library.id = item?.merge(library)
            mListLibraries.value = mListLibraries.value
        } else {
            val deleted = mRepository.findDeleted(library.path)
            if (deleted != null)
                library.id = deleted.id
            addLibrary(library)
        }

        saveLibrary(library)
    }

    fun addLibrary(library: Library, position: Int = -1) {
        if (mListLibraries.value!!.contains(library))
            mListLibraries.value!![mListLibraries.value!!.indexOf(library)].merge(library)
        else if (position > -1)
            mListLibraries.value!!.add(position, library)
        else
            mListLibraries.value!!.add(library)

        mListLibraries.value = mListLibraries.value
    }

    fun deleteLibrary(library: Library) {
        mListLibraries.value?.removeIf { it == library }
        if (library.id != null) {
            mRepository.delete(library)
            deleteAllByPath(library.id!!, library.type, library.path)
        }
    }

    fun saveLibrary(library: Library) {
        if (library.id == null)
            library.id = mRepository.save(library)
        else {
            val saved = mRepository.get(library.id!!)
            if (!saved!!.path.equals(library.path, true))
                deleteAllByPath(saved.id!!, saved.type, saved.path)
            mRepository.update(library)
        }
    }

    fun findLibraryDeleted(path: String): Library? {
        return mRepository.findDeleted(path)
    }

    fun loadLibrary(type: Type?) {
        mListLibraries.value = mRepository.list(type).toMutableList()
    }

    fun loadThemes(initial: Themes = Themes.ORIGINAL) {
        mListThemes.value = Themes.values().map { Pair(it, it == initial) }.toMutableList()
    }

    fun getSelectedThemeIndex(): Int  {
        val index = mListThemes.value?.indexOfFirst { it.second } ?: 0
        return if (index == -1) 0 else index
    }

    fun loadFontsNormal(initial: FontType = FontType.TimesNewRoman) {
        mListFontsNormal.value = FontType.values().filter { !it.isJapanese() }.map { Pair(it, it == initial) }.toMutableList()
    }

    fun loadFontsJapanese(initial: FontType = FontType.BabelStoneHan) {
        mListFontsJapanese.value = FontType.values().filter { it.isJapanese() }.map { Pair(it, it == initial) }.toMutableList()
    }

    fun getSelectedFontTypeIndex(isJapanese: Boolean): Int  {
        val index = if (isJapanese) mListFontsJapanese.value?.indexOfFirst { it.second } ?: 0 else mListFontsNormal.value?.indexOfFirst { it.second } ?: 0
        return if (index == -1) 0 else index
    }

    fun getLibraryAndRemove(position: Int): Library? {
        return if (mListLibraries.value != null) mListLibraries.value!!.removeAt(position) else null
    }

    fun removeLibraryDefault(vararg args: String) {
        for (path in args) {
            mRepository.removeDefault(path)
            mListLibraries.value!!.removeIf { it.path.equals(path, true) }
        }
    }

    fun getListLibrary(): List<Library> {
        return mListLibraries.value?.filter { it.enabled } ?: mutableListOf()
    }

    fun setEnableTheme(theme: Themes) {
        if (mListThemes.value != null)
            mListThemes.value = mListThemes.value!!.map { Pair(it.first, it.first == theme) }.toMutableList()
    }

    fun setEnableFont(font: FontType, isJapanese : Boolean) {
        if (isJapanese) {
            if (mListFontsJapanese.value != null)
                mListFontsJapanese.value = mListFontsJapanese.value!!.map { Pair(it.first, it.first == font) }.toMutableList()
        } else {
            if (mListFontsNormal.value != null)
                mListFontsNormal.value = mListFontsNormal.value!!.map { Pair(it.first, it.first == font) }.toMutableList()
        }
    }

    fun getDefault(type: Type) : String {
        return mRepository.getDefault(type)
    }

    fun saveDefault(type: Type, path: String) {
        mRepository.saveDefault(type, path)
    }

    fun deleteAllByPathDefault(type: Type, oldPath: String) = mRepository.deleteAllByPathDefault(type, oldPath)
    fun deleteAllByPath(idLibrary: Long, type: Type, oldPath: String) = mRepository.deleteAllByPath(idLibrary, type, oldPath)

}
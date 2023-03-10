package br.com.fenix.bilingualreader.view.ui.manga_detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import br.com.fenix.bilingualreader.model.entity.FileLink
import br.com.fenix.bilingualreader.model.entity.Information
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.service.listener.ApiListener
import br.com.fenix.bilingualreader.service.parses.manga.ParseFactory
import br.com.fenix.bilingualreader.service.parses.manga.RarParse
import br.com.fenix.bilingualreader.service.repository.FileLinkRepository
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import br.com.fenix.bilingualreader.service.tracker.ParseInformation
import br.com.fenix.bilingualreader.service.tracker.mal.MalMangaDetail
import br.com.fenix.bilingualreader.service.tracker.mal.MyAnimeListTracker
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.Util
import org.slf4j.LoggerFactory
import java.io.File

class MangaDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val mLOGGER = LoggerFactory.getLogger(MangaDetailViewModel::class.java)

    private val mMangaRepository: MangaRepository = MangaRepository(application.applicationContext)
    private val mFileLinkRepository: FileLinkRepository = FileLinkRepository(application.applicationContext)
    private val cache = GeneralConsts.getCacheDir(application.applicationContext)

    private var mManga = MutableLiveData<Manga>(null)
    val manga: LiveData<Manga> = mManga

    private var mPaths: Map<String, Int> = mapOf()

    private var mListChapters = MutableLiveData<MutableList<String>>(mutableListOf())
    val listChapters: LiveData<MutableList<String>> = mListChapters

    private var mListFileLinks = MutableLiveData<MutableList<FileLink>>(mutableListOf())
    val listFileLinks: LiveData<MutableList<FileLink>> = mListFileLinks

    private var mListSubtitles = MutableLiveData<MutableList<String>>(mutableListOf())
    val listSubtitles: LiveData<MutableList<String>> = mListSubtitles

    private var mInformation = MutableLiveData<Information>(null)
    val information: LiveData<Information> = mInformation

    private var mInformationRelations = MutableLiveData<MutableList<Information>>(mutableListOf())
    val informationRelations: LiveData<MutableList<Information>> = mInformationRelations

    private val mTracker = MyAnimeListTracker(application.applicationContext)

    fun setManga(manga: Manga) {
        mManga.value = manga

        mListFileLinks.value = if (manga.id != null) mFileLinkRepository.findAllByManga(manga.id!!)?.toMutableList() else mutableListOf()
        mInformation.value = null
        mInformationRelations.value = mutableListOf()

        val parse = ParseFactory.create(manga.file) ?: return
        try {
            if (parse is RarParse) {
                val folder = GeneralConsts.CACHE_FOLDER.RAR + '/' + Util.normalizeNameCache(manga.file.nameWithoutExtension)
                val cacheDir = File(cache, folder)
                (parse as RarParse?)!!.setCacheDirectory(cacheDir)
            }

            mPaths = parse.getPagePaths()
            mListChapters.value = mPaths.keys.toMutableList()
            mListSubtitles.value = parse.getSubtitlesNames().keys.toMutableList()
        } finally {
            Util.destroyParse(parse)
        }
    }

    fun getInformation() {
        var name = mManga.value?.title ?: ""

        if (name.isEmpty())
            return

        name = Util.getNameFromMangaTitle(name).replace(" ", "%")
        mTracker.getListManga(name, object : ApiListener<List<MalMangaDetail>> {
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
        val list = ParseInformation.getInformation(mangas)

        val name = Util.getNameFromMangaTitle(mManga.value?.title ?: "").replace(PATTERN, "")

        mInformation.value = list.find {
            it.title.replace(PATTERN, "").trim().equals(name, true) ||
                    it.alternativeTitles.contains(name, true)
        }
        if (mInformation.value != null)
            list.remove(mInformation.value)

        mInformationRelations.value = list
    }

    fun getPage(folder: String): Int {
        return mPaths[folder]?.plus(1) ?: mManga.value?.bookMark ?: 1
    }

    fun clear() {
        mManga.value = null
        mListFileLinks.value = mutableListOf()
        mListChapters.value = mutableListOf()
        mListSubtitles.value = mutableListOf()
    }

    fun delete() {
        if (mManga.value != null)
            mMangaRepository.delete(mManga.value!!)
    }

    fun save(manga: Manga?) {
        manga ?: return
        mMangaRepository.update(manga)
        mManga.value = manga
    }

    fun markRead() {
        mManga.value ?: return
        mMangaRepository.markRead(mManga.value)
        mManga.value = mManga.value
    }

    fun clearHistory() {
        mManga.value ?: return
        mMangaRepository.clearHistory(mManga.value)
        mManga.value = mManga.value
    }

    fun getChapterFolder(chapter: Int): String {
        var folder = ""
        if (mPaths.isNotEmpty())
            for (path in mPaths) {
                if (chapter >= path.value) {
                    folder = path.key
                }
            }
        return folder
    }

}
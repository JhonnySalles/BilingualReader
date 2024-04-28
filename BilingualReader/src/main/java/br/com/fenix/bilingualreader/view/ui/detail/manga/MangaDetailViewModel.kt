package br.com.fenix.bilingualreader.view.ui.detail.manga

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import br.com.fenix.bilingualreader.model.entity.Information
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.LinkedFile
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.entity.SubTitleChapter
import br.com.fenix.bilingualreader.model.entity.SubTitleVolume
import br.com.fenix.bilingualreader.service.controller.BookImageCoverController
import br.com.fenix.bilingualreader.service.controller.MangaImageCoverController
import br.com.fenix.bilingualreader.service.controller.SubTitleController
import br.com.fenix.bilingualreader.service.listener.ApiListener
import br.com.fenix.bilingualreader.service.parses.manga.ParseFactory
import br.com.fenix.bilingualreader.service.parses.manga.RarParse
import br.com.fenix.bilingualreader.service.repository.FileLinkRepository
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import br.com.fenix.bilingualreader.service.repository.VocabularyRepository
import br.com.fenix.bilingualreader.service.tracker.ParseInformation
import br.com.fenix.bilingualreader.service.tracker.mal.MalMangaDetail
import br.com.fenix.bilingualreader.service.tracker.mal.MyAnimeListTracker
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.Util
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class MangaDetailViewModel(var app: Application) : AndroidViewModel(app) {

    private val mLOGGER = LoggerFactory.getLogger(MangaDetailViewModel::class.java)

    private val mMangaRepository: MangaRepository = MangaRepository(app.applicationContext)
    private val mFileLinkRepository: FileLinkRepository = FileLinkRepository(app.applicationContext)
    private val mVocabularyRepository: VocabularyRepository = VocabularyRepository(app.applicationContext)
    private val cache = GeneralConsts.getCacheDir(app.applicationContext)

    var library: Library? = null
    private var mManga = MutableLiveData<Manga?>(null)
    val manga: LiveData<Manga?> = mManga

    private var mCover = MutableLiveData<Bitmap?>(null)
    val cover: LiveData<Bitmap?> = mCover

    private var mPaths: Map<String, Int> = mapOf()

    private var mListChapters = MutableLiveData<MutableList<String>>(mutableListOf())
    val listChapters: LiveData<MutableList<String>> = mListChapters

    private var mListLinkedFileLinks = MutableLiveData<MutableList<LinkedFile>>(mutableListOf())
    val listLinkedFileLinks: LiveData<MutableList<LinkedFile>> = mListLinkedFileLinks

    private var mListSubtitles = MutableLiveData<MutableList<String>>(mutableListOf())
    val listSubtitles: LiveData<MutableList<String>> = mListSubtitles

    private var mLocalInformation = MutableLiveData<Information?>(null)
    val localInformation: LiveData<Information?> = mLocalInformation

    private var mWebInformation = MutableLiveData<Information?>(null)
    val webInformation: LiveData<Information?> = mWebInformation

    private var mWebInformationRelations = MutableLiveData<MutableList<Information>>(mutableListOf())
    val webInformationRelations: LiveData<MutableList<Information>> = mWebInformationRelations

    private val mTracker = MyAnimeListTracker(app.applicationContext)

    fun setManga(manga: Manga) {
        mManga.value = manga

        mListLinkedFileLinks.value = if (manga.id != null) mFileLinkRepository.findAllByManga(manga.id!!)?.toMutableList() else mutableListOf()
        mWebInformation.value = null
        mLocalInformation.value = null
        mWebInformationRelations.value = mutableListOf()

        CoroutineScope(Dispatchers.IO).launch {
            val parse = ParseFactory.create(manga.file) ?: return@launch
            try {
                if (parse is RarParse) {
                    val folder = GeneralConsts.CACHE_FOLDER.RAR + '/' + Util.normalizeNameCache(manga.file.nameWithoutExtension)
                    val cacheDir = File(cache, folder)
                    (parse as RarParse?)!!.setCacheDirectory(cacheDir)
                }

                MangaImageCoverController.instance.getMangaCover(app.applicationContext, manga, true).let {
                    withContext(Dispatchers.Main) {
                        mCover.value = it
                    }
                }

                val info = parse.getComicInfo()
                if (info != null) {
                    if (manga.update(info))
                        mMangaRepository.update(manga)
                    withContext(Dispatchers.Main) {
                        mLocalInformation.value = Information(app.applicationContext, info)
                    }
                }

                if (manga.hasSubtitle != parse.hasSubtitles()) {
                    mMangaRepository.updateHasSubtitle(manga.id!!, parse.hasSubtitles())
                    manga.hasSubtitle = parse.hasSubtitles()
                    manga.lastVocabImport = null
                }

                val paths = parse.getPagePaths()
                val listChapters = paths.keys.toMutableList()
                val listSubtitles = parse.getSubtitlesNames().keys.toMutableList()

                withContext(Dispatchers.Main) {
                    mPaths = paths
                    mListChapters.value = listChapters
                    mListSubtitles.value = listSubtitles
                }

                var image: Bitmap? = null
                val deferred = async {
                    image = MangaImageCoverController.instance.getMangaCover(app.applicationContext, manga, false)
                }
                deferred.await()
                withContext(Dispatchers.Main) {
                    mCover.value = image
                }
            } finally {
                Util.destroyParse(parse)
            }
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
        val list = ParseInformation.getInformation(app.applicationContext, mangas)

        val name = Util.getNameFromMangaTitle(mManga.value?.title ?: "").replace(PATTERN, "")

        mWebInformation.value = list.find {
            it.title.replace(PATTERN, "").trim().equals(name, true) ||
                    it.alternativeTitles.contains(name, true)
        }
        if (mWebInformation.value != null)
            list.remove(mWebInformation.value)

        mWebInformationRelations.value = list
    }

    fun getPage(folder: String): Int {
        return mPaths[folder]?.plus(1) ?: mManga.value?.bookMark ?: 1
    }

    fun clear() {
        mManga.value = null
        mListLinkedFileLinks.value = mutableListOf()
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

    fun importVocabulary() {
        CoroutineScope(Dispatchers.IO).launch {
            async {
                val manga = manga.value ?: return@async
                val parse = ParseFactory.create(manga.file) ?: return@async

                try {
                    if (parse is RarParse) {
                        val folder = GeneralConsts.CACHE_FOLDER.RAR + '/' + Util.normalizeNameCache(manga.name)
                        val cacheDir = File(cache, folder)
                        (parse as RarParse?)!!.setCacheDirectory(cacheDir)
                    }

                    val listJson: List<String> = parse.getSubtitles()
                    if (listJson.isNotEmpty()) {
                        val listSubTitleChapter: MutableList<SubTitleChapter> = SubTitleController.getChapterFromJson(listJson)
                        mVocabularyRepository.processVocabulary(manga.id, listSubTitleChapter, true)
                    }
                } finally {
                    Util.destroyParse(parse)
                }
            }
        }
    }
}
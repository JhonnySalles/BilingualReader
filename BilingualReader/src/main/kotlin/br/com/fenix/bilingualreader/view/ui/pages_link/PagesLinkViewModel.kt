package br.com.fenix.bilingualreader.view.ui.pages_link

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Message
import android.os.Process
import android.widget.ImageView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import br.com.fenix.bilingualreader.model.entity.LinkedFile
import br.com.fenix.bilingualreader.model.entity.LinkedPage
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.LoadFile
import br.com.fenix.bilingualreader.model.enums.PageLinkType
import br.com.fenix.bilingualreader.service.controller.SubTitleController
import br.com.fenix.bilingualreader.service.parses.manga.Parse
import br.com.fenix.bilingualreader.service.parses.manga.ParseFactory
import br.com.fenix.bilingualreader.service.parses.manga.RarParse
import br.com.fenix.bilingualreader.service.repository.FileLinkRepository
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.constants.PageLinkConsts
import br.com.fenix.bilingualreader.util.constants.ReaderConsts
import br.com.fenix.bilingualreader.util.helpers.FileUtil
import br.com.fenix.bilingualreader.util.helpers.Util
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.io.InterruptedIOException
import java.time.LocalDateTime
import kotlin.math.max


class PagesLinkViewModel(application: Application) : AndroidViewModel(application) {

    private val mLOGGER = LoggerFactory.getLogger(PagesLinkViewModel::class.java)

    private val mApplication = application
    private val mFileLinkRepository: FileLinkRepository = FileLinkRepository(application.applicationContext)

    private var mFileLinkImageList = ArrayList<Triple<Int, Boolean, Bitmap?>>(ArrayList())
    private var mManga: Manga? = null
    private var mLinkedFile = MutableLiveData<LinkedFile?>()
    val linkedFile: LiveData<LinkedFile?> = mLinkedFile
    private var mPagesLink = MutableLiveData<ArrayList<LinkedPage>>(ArrayList())
    val pagesLink: LiveData<ArrayList<LinkedPage>> = mPagesLink
    private var mPagesNotLinked = MutableLiveData<ArrayList<LinkedPage>>(ArrayList())
    val pagesLinkNotLinked: LiveData<ArrayList<LinkedPage>> = mPagesNotLinked
    private var mLanguage = MutableLiveData(Languages.PORTUGUESE)
    val language: LiveData<Languages> = mLanguage

    private var mBackupLinked = ArrayList<ArrayList<LinkedPage>>(ArrayList())
    private var mHasBackup = MutableLiveData(false)
    val hasBackup: LiveData<Boolean> = mHasBackup

    private var mGenerateImageHandler: MutableList<Handler>? = java.util.ArrayList()
    private var mGenerateImageThread: ArrayList<ImageLoadThread> = ArrayList()

    private var mUsePagePath = GeneralConsts.getSharedPreferences(application.applicationContext)
        .getBoolean(GeneralConsts.KEYS.PAGE_LINK.USE_PAGE_PATH_FOR_LINKED, false)

    fun getMangaId(): Long {
        return mManga?.id ?: -1
    }

    fun getMangaName(): String {
        return mManga?.fileName ?: ""
    }

    private fun getParse(path: String, type: PageLinkType): Parse? =
        getParse(File(path), type)

    private fun getParse(file: File, type: PageLinkType): Parse? {
        val parse = ParseFactory.create(file)
        if (parse is RarParse) {
            val prefix = (if (type == PageLinkType.MANGA) GeneralConsts.FILE_LINK.FOLDER_MANGA else GeneralConsts.FILE_LINK.FOLDER_LINK) + "_"
            val folder = GeneralConsts.CACHE_FOLDER.LINKED + '/' + Util.normalizeNameCache(file.nameWithoutExtension, prefix, false)
            val cacheDir = File(GeneralConsts.getCacheDir(mApplication.applicationContext), folder)
            (parse as RarParse?)!!.setCacheDirectory(cacheDir)
        }
        return parse
    }

    fun getFileLink(manga: Manga? = null, isBackup: Boolean = false): LinkedFile? {
        return if (!isBackup && (mLinkedFile.value == null || mLinkedFile.value!!.path == ""))
            if (manga != null)
                mFileLinkRepository.get(manga)
            else
                null
        else this.get()
    }

    // Keeps the data to quickly load the file. Cleaning takes place by the main activity
    fun onDestroy() {
        if (mLinkedFile.value?.parseManga == null) {
            Util.destroyParse(mLinkedFile.value?.parseManga, false)
            mLinkedFile.value?.parseManga = null
        }

        if (mLinkedFile.value?.parseFileLink == null) {
            Util.destroyParse(mLinkedFile.value?.parseFileLink, false)
            mLinkedFile.value?.parseFileLink = null
        }
    }

    private fun verify(linkedFile: LinkedFile?) {
        if (linkedFile == null) return

        if (linkedFile.parseManga == null)
            linkedFile.parseManga = getParse(linkedFile.manga!!.path, PageLinkType.MANGA)

        if (linkedFile.parseFileLink == null && linkedFile.path.isNotEmpty())
            linkedFile.parseFileLink = getParse(linkedFile.path, PageLinkType.LINKED)
    }

    private fun reload(refresh: (index: Int?, type: PageLinkType) -> (Unit)): Boolean {
        val fileLink = SubTitleController.getInstance(mApplication.applicationContext).getFileLink() ?: return false
        return if (mManga == fileLink.manga) {
            endThread(true)
            verify(fileLink)
            mLinkedFile.value = fileLink
            mPagesLink.value = fileLink.pagesLink?.let { ArrayList(it) }
            mPagesNotLinked.value = fileLink.pagesNotLink?.let { ArrayList(it) }
            setLanguage(fileLink.language)
            refresh(null, PageLinkType.ALL)

            getImage(mLinkedFile.value!!.parseManga, mLinkedFile.value!!.parseFileLink, mPagesLink.value!!, PageLinkType.ALL, true)

            if (mPagesNotLinked.value!!.isNotEmpty())
                getImage(null, mLinkedFile.value!!.parseFileLink, mPagesNotLinked.value!!, PageLinkType.NOT_LINKED, true)

            true
        } else false
    }

    fun reload(linkedFile: LinkedFile?, refresh: (index: Int?, type: PageLinkType) -> (Unit)): Boolean {
        return if (linkedFile != null) {
            endThread(true)
            verify(linkedFile)
            mLinkedFile.value = linkedFile
            mPagesLink.value = linkedFile.pagesLink?.let { ArrayList(it) }
            mPagesNotLinked.value = linkedFile.pagesNotLink?.let { ArrayList(it) }
            refresh(null, PageLinkType.ALL)

            getImage(mLinkedFile.value!!.parseManga, mLinkedFile.value!!.parseFileLink, mPagesLink.value!!, PageLinkType.ALL, true)

            if (mPagesNotLinked.value!!.isNotEmpty())
                getImage(null, mLinkedFile.value!!.parseFileLink, mPagesNotLinked.value!!, PageLinkType.NOT_LINKED, true)

            true
        } else false
    }

    private fun find(isLoadManga: Boolean, refresh: (index: Int?, type: PageLinkType) -> (Unit)): Boolean {
        if (mManga == null) return false
        val obj = mFileLinkRepository.get(mManga!!) ?: return false
        set(obj, refresh, isLoadManga)
        return (obj.pagesLink != null) && (obj.pagesLink!!.isNotEmpty())
    }

    fun find(name: String, pages: Int, refresh: (index: Int?, type: PageLinkType) -> (Unit)): Boolean {
        if (mManga == null || mManga!!.id == null) return false
        val obj = mFileLinkRepository.findByFileName(mManga!!.id!!, name, pages) ?: return false
        set(obj, refresh)
        return true
    }

    fun set(obj: LinkedFile, refresh: (index: Int?, type: PageLinkType) -> (Unit), isLoadManga: Boolean = false) {
        endThread(true)

        clearBackup()
        Util.destroyParse(mLinkedFile.value?.parseManga, false)
        Util.destroyParse(mLinkedFile.value?.parseFileLink, false)

        if (mPagesLink.value != null && mPagesLink.value!!.isNotEmpty())
            obj.pagesLink?.forEachIndexed { index, pageLink -> pageLink.imageMangaPage = mPagesLink.value!![index].imageMangaPage }

        mLinkedFile.value = obj
        mFileLinkImageList.clear()
        mPagesLink.value?.forEach { it.clearPageLink() }
        mPagesNotLinked.value?.clear()
        setLanguage(obj.language)

        val mParseManga = getParse(mManga!!.file, PageLinkType.MANGA) ?: return
        mLinkedFile.value?.parseManga = mParseManga
        val mParseLink = getParse(obj.file, PageLinkType.LINKED) ?: return
        mLinkedFile.value?.parseFileLink = mParseLink

        verify(obj)
        if (isLoadManga)
            mPagesLink.value = obj.pagesLink?.let { ArrayList(it) }
        else
            obj.pagesLink?.forEachIndexed { index, pageLink -> mPagesLink.value!![index].merge(pageLink) }
        mPagesNotLinked.value = obj.pagesNotLink?.let { ArrayList(it) }
        refresh(null, PageLinkType.ALL)

        val type = if (isLoadManga) PageLinkType.ALL else PageLinkType.LINKED

        for (i in 0 until mParseLink.numPages())
            mFileLinkImageList.add(Triple(i, false, null))

        getImage(mParseManga, mParseLink, mPagesLink.value!!, type)
        if (mPagesNotLinked.value!!.isNotEmpty()) {
            refresh(null, PageLinkType.NOT_LINKED)
            getImage(mParseManga, mParseLink, mPagesNotLinked.value!!, PageLinkType.NOT_LINKED)
        }
    }

    fun save(obj: LinkedFile): LinkedFile {
        obj.lastAccess = LocalDateTime.now()
        if (obj.id == 0L)
            obj.id = mFileLinkRepository.save(obj)
        else
            mFileLinkRepository.update(obj)

        return obj
    }

    fun save() {
        val obj = this.get()
        obj.lastAccess = LocalDateTime.now()
        if (obj.id == null || obj.id == 0L)
            mLinkedFile.value!!.id = mFileLinkRepository.save(obj)
        else
            mFileLinkRepository.update(obj)
    }

    fun get(): LinkedFile {
        val obj = mLinkedFile.value!!
        obj.manga = mManga
        obj.idManga = mManga!!.id!!
        obj.pagesLink = mPagesLink.value
        obj.pagesNotLink = mPagesNotLinked.value
        return obj
    }

    fun delete(refresh: (index: Int?, type: PageLinkType) -> (Unit)) {
        if (mLinkedFile.value != null)
            mFileLinkRepository.delete(mLinkedFile.value!!)

        clearFileLink(refresh)
    }

    fun delete(obj: LinkedFile) {
        mFileLinkRepository.delete(obj)
    }

    fun getPagesIndex(isMangaIndexes: Boolean): Map<String, Int> {
        val paths = mutableMapOf<String, Int>()

        for ((index, page) in mPagesLink.value!!.withIndex()) {
            val path = if (isMangaIndexes)
                page.mangaPagePath
            else
                page.fileLinkLeftPagePath

            if (path.isNotEmpty() && !paths.containsKey(path))
                paths[path] = index
        }

        return paths
    }

    fun loadImageManga(page: Int, image: ImageView) {
        if (mLinkedFile.value == null || mLinkedFile.value?.parseManga == null || page < 0)
            return

        try {
            val parse = mLinkedFile.value?.parseManga!!
            val stream: InputStream = parse.getPage(page)
            image.setImageBitmap(BitmapFactory.decodeStream(stream))
            Util.closeInputStream(stream)
        } catch (i: InterruptedIOException) {
            mLOGGER.info("Interrupted error when generate bitmap: " + i.message)
        } catch (e: Exception) {
            mLOGGER.info("Error when generate bitmap: " + e.message)
        }
    }

    fun loadImagePageLink(page: Int, image: ImageView) {
        if (mLinkedFile.value == null || mLinkedFile.value?.parseFileLink == null || page < 0)
            return

        try {
            val parse = mLinkedFile.value?.parseFileLink!!
            val stream: InputStream = parse.getPage(page)
            image.setImageBitmap(BitmapFactory.decodeStream(stream))
            Util.closeInputStream(stream)
        } catch (i: InterruptedIOException) {
            mLOGGER.info("Interrupted error when generate bitmap: " + i.message)
        } catch (e: Exception) {
            mLOGGER.info("Error when generate bitmap: " + e.message)
        }
    }

    fun getFilesNames(): Pair<String, String> {
        val manga = mManga?.fileName ?: ""
        val fileLink = mLinkedFile.value?.name ?: ""
        return Pair(manga, fileLink)
    }

    fun reloadPageLink(refresh: (index: Int?, type: PageLinkType) -> (Unit)) {
        clearBackup()
        if (mLinkedFile.value != null) {
            val fileLink = mFileLinkRepository.get(mManga!!)
            if (fileLink != null) {
                endThread()
                fileLink.parseFileLink = mLinkedFile.value!!.parseFileLink
                fileLink.parseManga = mLinkedFile.value!!.parseManga

                for ((index, page) in fileLink.pagesLink!!.withIndex()) {
                    if (index >= mPagesLink.value!!.size)
                        break

                    page.imageMangaPage = mPagesLink.value!![index].imageMangaPage

                    if (page.fileLinkLeftPage != PageLinkConsts.VALUES.PAGE_EMPTY) {
                        val item = mPagesLink.value?.find {
                            it.fileLinkLeftPage.compareTo(page.fileLinkLeftPage) == 0 || it.fileLinkRightPage.compareTo(
                                page.fileLinkLeftPage
                            ) == 0
                        } ?: mPagesNotLinked.value?.find { it.fileLinkLeftPage.compareTo(page.fileLinkLeftPage) == 0 }

                        if (item != null) {
                            if (item.fileLinkLeftPage.compareTo(page.fileLinkLeftPage) == 0)
                                page.imageLeftFileLinkPage = item.imageLeftFileLinkPage
                            else if (item.fileLinkRightPage.compareTo(page.fileLinkLeftPage) == 0)
                                page.imageLeftFileLinkPage = item.imageRightFileLinkPage
                        }
                    }

                    if (page.fileLinkRightPage != PageLinkConsts.VALUES.PAGE_EMPTY) {
                        val item = mPagesLink.value?.find {
                            it.fileLinkLeftPage.compareTo(page.fileLinkRightPage) == 0 || it.fileLinkRightPage.compareTo(
                                page.fileLinkRightPage
                            ) == 0
                        } ?: mPagesNotLinked.value?.find { it.fileLinkLeftPage.compareTo(page.fileLinkRightPage) == 0 }

                        if (item != null) {
                            if (item.fileLinkLeftPage.compareTo(page.fileLinkRightPage) == 0)
                                page.imageRightFileLinkPage = item.imageLeftFileLinkPage
                            else if (item.fileLinkRightPage.compareTo(page.fileLinkRightPage) == 0)
                                page.imageRightFileLinkPage = item.imageRightFileLinkPage
                        }
                    }
                }

                for (page in fileLink.pagesNotLink!!) {
                    val item = mPagesNotLinked.value?.find { it.fileLinkLeftPage.compareTo(page.fileLinkLeftPage) == 0 }
                        ?: mPagesLink.value?.find {
                            it.fileLinkLeftPage.compareTo(page.fileLinkLeftPage) == 0 || it.fileLinkRightPage.compareTo(
                                page.fileLinkLeftPage
                            ) == 0
                        }

                    if (item != null) {
                        if (item.fileLinkLeftPage.compareTo(page.fileLinkLeftPage) == 0)
                            page.imageLeftFileLinkPage = item.imageLeftFileLinkPage
                        else if (item.fileLinkRightPage.compareTo(page.fileLinkLeftPage) == 0)
                            page.imageLeftFileLinkPage = item.imageRightFileLinkPage
                    }
                }

                verify(fileLink)
                mLinkedFile.value = fileLink
                mPagesLink.value = fileLink.pagesLink?.let { ArrayList(it) }
                mPagesNotLinked.value = fileLink.pagesNotLink?.let { ArrayList(it) }

                getImage(fileLink.parseManga, fileLink.parseFileLink, mPagesLink.value!!, PageLinkType.ALL, true)

                if (mPagesNotLinked.value!!.isNotEmpty())
                    getImage(null, fileLink.parseFileLink, mPagesNotLinked.value!!, PageLinkType.NOT_LINKED, true)

            } else if (mLinkedFile.value!!.path.isNotEmpty())
                readFileLink(mLinkedFile.value!!.path, true, refresh)
        }
    }

    fun setLanguage(language: Languages? = null, isClear: Boolean = false) {
        mLanguage.value = if (isClear || language == null) Languages.PORTUGUESE else language
    }

    fun loadManga(manga: Manga, refresh: (index: Int?, type: PageLinkType) -> (Unit)) {
        mManga = manga
        mPagesLink.value?.clear()
        setLanguage(isClear = true)

        if (reload(refresh)) return
        if (find(true, refresh)) return

        clearBackup()
        Util.destroyParse(mLinkedFile.value?.parseManga, false)

        val parse = getParse(manga.file, PageLinkType.MANGA) ?: return
        mLinkedFile.value = LinkedFile(manga)
        mLinkedFile.value!!.parseManga = parse

        val list = ArrayList<LinkedPage>()
        for (i in 0 until parse.numPages()) {
            val name = parse.getPagePath(i) ?: ""
            if (FileUtil.isImage(name))
                list.add(LinkedPage(mLinkedFile.value!!.id, i, manga.pages, Util.getNameFromPath(name), Util.getFolderFromPath(name)))
        }
        mPagesLink.value = list
        refresh(null, PageLinkType.MANGA)
        getImage(parse, null, mPagesLink.value!!, PageLinkType.MANGA)
    }

    fun readFileLink(path: String, isReload: Boolean = false, refresh: (index: Int?, type: PageLinkType) -> (Unit)): LoadFile {
        var loaded = LoadFile.ERROR_NOT_LOAD

        val file = File(path)
        if (file.name.endsWith(".rar") ||
            file.name.endsWith(".zip") ||
            file.name.endsWith(".cbr") ||
            file.name.endsWith(".cbz")
        ) {

            Util.destroyParse(mLinkedFile.value?.parseFileLink, false)
            val parse = getParse(path, PageLinkType.LINKED)
            if (parse != null) {
                loaded = LoadFile.LOADED

                if (!isReload && find(file.name, parse.numPages(), refresh)) return loaded

                endThread(true)
                mLinkedFile.value = LinkedFile(
                    mManga!!, mLinkedFile.value!!.parseManga, parse.numPages(), path,
                    file.name, file.extension, file.parent
                )
                mLinkedFile.value!!.parseFileLink = parse
                mPagesLink.value?.forEach { it.clearPageLink() }

                var folder = ""
                var lastFolder = ""
                var padding = 0
                val mangaParse = mLinkedFile.value!!.parseManga!!
                val hasFolders = mangaParse.getPagePaths().isNotEmpty() && parse.getPagePaths().isNotEmpty()

                val pagesLink = mPagesLink.value!!
                val listNotLink = ArrayList<LinkedPage>()
                for (i in 0 until parse.numPages()) {
                    val pagePath = parse.getPagePath(i) ?: ""
                    if (FileUtil.isImage(pagePath)) {
                        if (mUsePagePath && hasFolders) {
                            folder = Util.getFolderFromPath(pagePath)
                            if (!folder.equals(lastFolder, true)) {
                                lastFolder = folder
                                if (i > 0 && (i + padding) < pagesLink.size) {
                                    if (pagesLink[i + padding].mangaPagePath.equals(pagesLink[i + padding - 1].mangaPagePath, true)) {
                                        do {
                                            padding++
                                            if ((i + padding) >= pagesLink.size)
                                                break
                                        } while (pagesLink[i + padding].mangaPagePath.equals(
                                                pagesLink[i + padding - 1].mangaPagePath,
                                                true
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        val index = i + padding
                        if (index > -1 && index < pagesLink.size) {
                            val page = pagesLink[index]
                            page.fileLinkLeftPage = i
                            page.fileLinkLeftPageName = Util.getNameFromPath(pagePath)
                            page.fileLinkLeftPagePath = Util.getFolderFromPath(pagePath)
                            page.fileLinkLeftPages = parse.numPages()
                            refresh(i, PageLinkType.LINKED)
                        } else
                            listNotLink.add(
                                LinkedPage(
                                    mLinkedFile.value!!.id, true, i, parse.numPages(), Util.getNameFromPath(pagePath),
                                    Util.getFolderFromPath(pagePath)
                                )
                            )
                    }
                }

                mPagesNotLinked.value = listNotLink
                refresh(null, PageLinkType.LINKED)
                getImage(null, parse, mPagesLink.value!!, PageLinkType.LINKED)

                if (mPagesNotLinked.value!!.isNotEmpty()) {
                    refresh(null, PageLinkType.NOT_LINKED)
                    getImage(null, parse, mPagesNotLinked.value!!, PageLinkType.NOT_LINKED)
                }

            }
        } else
            loaded = LoadFile.ERROR_FILE_WRONG

        if (loaded != LoadFile.LOADED)
            clearFileLink(refresh)

        return loaded
    }

    fun clearFileLink(refresh: (index: Int?, type: PageLinkType) -> (Unit)) {
        endThread(true)
        mLinkedFile.value = LinkedFile(mManga!!, mLinkedFile.value!!.parseManga)
        setLanguage(isClear = true)
        mPagesNotLinked.value?.clear()
        mPagesLink.value?.forEach { page -> page.clearPageLink() }
        refresh(null, PageLinkType.ALL)
    }

    fun getPageLink(page: LinkedPage): String =
        mPagesLink.value!!.indexOf(page).toString()

    fun getPageLink(index: Int): LinkedPage? {
        return if (index >= mPagesLink.value!!.size || index == -1)
            null
        else
            mPagesLink.value!![index]
    }

    fun getPageNotLink(page: LinkedPage): String =
        mPagesNotLinked.value!!.indexOf(page).toString()

    fun getPageNotLink(index: Int): LinkedPage? {
        return if (index >= mPagesNotLinked.value!!.size || index == -1)
            null
        else
            mPagesNotLinked.value!![index]
    }

    private fun getPageNotLinkLastIndex(): Int {
        return if (mPagesNotLinked.value!!.isEmpty())
            0
        else
            mPagesNotLinked.value!!.size - 1
    }

    fun getPageNotLinkIndex(page: LinkedPage?): Int? {
        return if (page == null || mPagesNotLinked.value!!.isEmpty()) null
        else {
            val index = mPagesNotLinked.value!!.indexOf(page)
            if (index < 0)
                null
            else
                index
        }
    }

    private fun addNotLinked(page: LinkedPage) {
        if (page.fileLinkLeftPage != PageLinkConsts.VALUES.PAGE_EMPTY) {
            mPagesNotLinked.value!!.add(
                LinkedPage(
                    page.idFile, true, page.fileLinkLeftPage, page.fileLinkLeftPages, page.fileLinkLeftPageName,
                    page.fileLinkLeftPagePath, page.isFileLeftDualPage, page.imageLeftFileLinkPage
                )
            )
            notifyMessages(PageLinkType.NOT_LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_IMAGE_ADDED, getPageNotLinkLastIndex())
        }

        if (page.isDualImage) {
            mPagesNotLinked.value!!.add(
                LinkedPage(
                    page.idFile, true, page.fileLinkRightPage, page.fileLinkLeftPages, page.fileLinkRightPageName,
                    page.fileLinkRightPagePath, page.isFileRightDualPage, page.imageRightFileLinkPage
                )
            )
            page.clearRightPageLink()
            notifyMessages(PageLinkType.NOT_LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_IMAGE_ADDED, getPageNotLinkLastIndex())
        }
    }

    fun onMoveDualPage(originType: PageLinkType, origin: LinkedPage?, destinyType: PageLinkType, destiny: LinkedPage?) {
        if (origin == null || destiny == null) {
            notifyMessages(PageLinkType.ALL, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_CHANGE)
            return
        }

        if (origin == destiny && destinyType == PageLinkType.DUAL_PAGE) {
            notifyMessages(PageLinkType.LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_CHANGE, mPagesLink.value!!.indexOf(destiny))
            return
        }

        var notLink = if (destinyType != PageLinkType.LINKED && destiny.isDualImage)
            LinkedPage(destiny.idFile, true, destiny.fileLinkRightPage, destiny.fileLinkLeftPages, destiny.fileLinkRightPageName,
                destiny.fileLinkRightPagePath, destiny.isFileRightDualPage, destiny.imageRightFileLinkPage)
        else if (destinyType == PageLinkType.LINKED && destiny.fileLinkLeftPage != PageLinkConsts.VALUES.PAGE_EMPTY)
            LinkedPage(destiny.idFile, true, destiny.fileLinkLeftPage, destiny.fileLinkLeftPages, destiny.fileLinkLeftPageName,
                destiny.fileLinkLeftPagePath, destiny.isFileLeftDualPage, destiny.imageLeftFileLinkPage)
        else
            null

        when {
            (originType == PageLinkType.DUAL_PAGE && destinyType == PageLinkType.DUAL_PAGE) -> {
                val originIndex = mPagesLink.value!!.indexOf(origin)
                val destinyIndex = mPagesLink.value!!.indexOf(destiny)

                destiny.addRightPageLink(origin)
                origin.clearRightPageLink()

                notifyMessages(originType, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_CHANGE, originIndex)
                notifyMessages(destinyType, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_CHANGE, destinyIndex)
            }
            (originType == PageLinkType.NOT_LINKED || destinyType == PageLinkType.NOT_LINKED) -> {
                if (originType == PageLinkType.NOT_LINKED && destinyType == PageLinkType.NOT_LINKED)
                    return
                else if (originType == PageLinkType.NOT_LINKED) {
                    val originIndex = mPagesNotLinked.value!!.indexOf(origin)
                    val destinyIndex = mPagesLink.value!!.indexOf(destiny)

                    destiny.addRightFromLeftPageLink(origin)
                    mPagesNotLinked.value!!.remove(origin)

                    notifyMessages(originType, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_IMAGE_REMOVED, originIndex)
                    notifyMessages(destinyType, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_CHANGE, destinyIndex)
                } else if (destinyType == PageLinkType.NOT_LINKED) {
                    val originIndex = mPagesLink.value!!.indexOf(destiny)
                    origin.clearRightPageLink()
                    notifyMessages(originType, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_CHANGE, originIndex)
                }
            }
            else -> {
                var originIndex = mPagesLink.value!!.indexOf(origin)
                var destinyIndex = mPagesLink.value!!.indexOf(destiny)

                when {
                    originType != PageLinkType.DUAL_PAGE && destinyType == PageLinkType.LINKED -> destiny.addLeftPageLink(origin)
                    originType != PageLinkType.DUAL_PAGE && destinyType != PageLinkType.LINKED -> destiny.addRightFromLeftPageLink(origin)
                    originType == PageLinkType.DUAL_PAGE && destinyType == PageLinkType.LINKED -> {
                        if ((destinyIndex + 1) < mPagesLink.value!!.size) {
                            notLink = null
                            onMove(mPagesLink.value!![destinyIndex], mPagesLink.value!![destinyIndex + 1])
                        }
                        destiny.addLeftFromRightPageLink(origin)
                    }
                    originType == PageLinkType.DUAL_PAGE && destinyType != PageLinkType.LINKED -> destiny.addRightFromLeftPageLink(origin)
                }

                notifyMessages(destinyType, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_CHANGE, destinyIndex)

                val moved = when (originType) {
                    PageLinkType.LINKED -> origin.clearLeftPageLink(true)
                    PageLinkType.DUAL_PAGE -> {
                        origin.clearRightPageLink()
                        false
                    }
                    else -> false
                }

                notifyMessages(originType, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_CHANGE, originIndex)

                if (originIndex > destinyIndex && originType != PageLinkType.DUAL_PAGE && !moved) {
                    originIndex += 1
                    destinyIndex += 1

                    if (originIndex >= mPagesLink.value!!.size || destinyIndex >= mPagesLink.value!!.size)
                        return

                    val nextOrigin = mPagesLink.value!![originIndex]
                    val nextDestiny = mPagesLink.value!![destinyIndex]

                    if (nextOrigin.fileLinkLeftPage != 1)
                        onMove(nextOrigin, nextDestiny)
                }
            }
        }

        if (notLink != null) {
            mPagesNotLinked.value!!.add(notLink)
            notifyMessages(PageLinkType.NOT_LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_IMAGE_ADDED, getPageNotLinkLastIndex())
        }
    }

    fun onMove(origin: LinkedPage?, destiny: LinkedPage?) {
        if (origin == null || destiny == null) {
            notifyMessages(PageLinkType.ALL, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_CHANGE)
            return
        }
        if (origin == destiny) return
        val originIndex = mPagesLink.value!!.indexOf(origin)
        val destinyIndex = mPagesLink.value!!.indexOf(destiny)
        var differ = destinyIndex - originIndex

        if (originIndex > destinyIndex) {
            var limit = mPagesLink.value!!.size - 1
            var index = mPagesLink.value!!.indexOf(mPagesLink.value!!.findLast { it.imageLeftFileLinkPage != null })
            if (index < 0)
                index = mPagesLink.value!!.size - 1

            for (i in index downTo originIndex)
                if (mPagesLink.value!![i].fileLinkLeftPage == PageLinkConsts.VALUES.PAGE_EMPTY)
                    limit = i

            for (i in destinyIndex until originIndex)
                addNotLinked(mPagesLink.value!![i])

            differ *= -1
            for (i in destinyIndex until limit) {
                when {
                    i == destinyIndex -> {
                        mPagesLink.value!![i].addLeftPageLink(origin)
                        origin.clearLeftPageLink()
                        notifyMessages(PageLinkType.LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_CHANGE, originIndex)
                    }
                    (i + differ) > (limit) -> continue
                    else -> {
                        mPagesLink.value!![i].addLeftPageLink(mPagesLink.value!![i + differ])
                        mPagesLink.value!![i + differ].clearLeftPageLink()
                    }
                }
                notifyMessages(PageLinkType.LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_CHANGE, i)
            }

            for (i in destinyIndex until limit)
                if (mPagesLink.value!![i].isDualImage && mPagesLink.value!![i].fileLinkLeftPage == PageLinkConsts.VALUES.PAGE_EMPTY &&
                    mPagesLink.value!![i].fileLinkRightPage != PageLinkConsts.VALUES.PAGE_EMPTY
                ) {
                    mPagesLink.value!![i].movePageLinkRightToLeft()
                    notifyMessages(PageLinkType.LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_CHANGE, i)
                }
        } else {
            var limit = mPagesLink.value!!.size - 1
            var spacesFree = 0

            for (i in originIndex until limit)
                if (mPagesLink.value!![i].fileLinkLeftPage == PageLinkConsts.VALUES.PAGE_EMPTY)
                    spacesFree++

            if (differ > spacesFree) {
                for (i in limit downTo limit - differ)
                    addNotLinked(mPagesLink.value!![i])

                for (i in limit downTo originIndex) {
                    when {
                        i < destinyIndex -> mPagesLink.value!![i].clearLeftPageLink()
                        else -> mPagesLink.value!![i].addLeftPageLink(mPagesLink.value!![i - differ])
                    }
                    notifyMessages(PageLinkType.LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_CHANGE, i)
                }
            } else {
                var spaceUsed = 0
                for (i in originIndex until limit) {
                    if (mPagesLink.value!![i].fileLinkLeftPage == PageLinkConsts.VALUES.PAGE_EMPTY) {
                        spaceUsed++
                        if (spaceUsed >= differ) {
                            limit = i
                            break
                        }
                    }
                }

                spaceUsed = 0
                var index: Int
                for (i in limit downTo originIndex) {
                    if (i < destinyIndex)
                        mPagesLink.value!![i].clearLeftPageLink(true)
                    else {
                        index = i - (1 + spaceUsed)
                        if (mPagesLink.value!![index].fileLinkLeftPage == PageLinkConsts.VALUES.PAGE_EMPTY) {
                            do {
                                spaceUsed++
                                index = i - (1 + spaceUsed)
                            } while (mPagesLink.value!![index].fileLinkLeftPage == PageLinkConsts.VALUES.PAGE_EMPTY)
                        }
                        mPagesLink.value!![i].addLeftPageLink(mPagesLink.value!![index])
                    }
                    notifyMessages(PageLinkType.LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_CHANGE, i)
                }
            }
        }
    }

    fun onNotLinked(origin: LinkedPage?) {
        if (origin == null) {
            notifyMessages(PageLinkType.LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_CHANGE)
            return
        }

        mPagesNotLinked.value!!.add(
            LinkedPage(
                origin.idFile, true, origin.fileLinkLeftPage, origin.fileLinkLeftPages, origin.fileLinkLeftPageName,
                origin.fileLinkLeftPagePath, origin.isFileLeftDualPage, origin.imageLeftFileLinkPage
            )
        )

        notifyMessages(PageLinkType.NOT_LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_IMAGE_ADDED, mPagesNotLinked.value!!.size - 1)

        val originIndex = mPagesLink.value!!.indexOf(origin)
        if (origin.isDualImage)
            origin.movePageLinkRightToLeft()
        else
            origin.clearPageLink()

        notifyMessages(PageLinkType.LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_CHANGE, originIndex)
    }

    fun fromNotLinked(origin: LinkedPage?, destiny: LinkedPage?) {
        if (origin == null || destiny == null) {
            notifyMessages(PageLinkType.NOT_LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_CHANGE)
            return
        }

        val destinyIndex = mPagesLink.value!!.indexOf(destiny)
        val size = mPagesLink.value!!.size - 1
        mPagesNotLinked.value!!.remove(origin)
        notifyMessages(PageLinkType.NOT_LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_IMAGE_REMOVED, size)

        if (destiny.imageLeftFileLinkPage == null) {
            mPagesLink.value!![destinyIndex].addLeftPageLink(origin)
            notifyMessages(PageLinkType.LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_CHANGE, destinyIndex)
        } else {
            addNotLinked(mPagesLink.value!![size])

            for (i in size downTo destinyIndex) {
                when (i) {
                    destinyIndex -> mPagesLink.value!![i].addLeftPageLink(origin)
                    else -> mPagesLink.value!![i].addLeftPageLink(mPagesLink.value!![i - 1])
                }
                notifyMessages(PageLinkType.LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_CHANGE, i)
            }
        }
    }

    fun hasBackup(): Boolean =
        mBackupLinked.isNotEmpty()

    private fun clearBackup() {
        mBackupLinked.clear()
        mHasBackup.value = mBackupLinked.isNotEmpty()
    }

    private fun generateBackup() {
        val backup = arrayListOf<LinkedPage>()

        for (pageLink in mPagesLink.value!!) {
            val page = LinkedPage(pageLink)
            page.merge(pageLink)
            backup.add(page)
        }

        for (pageLink in mPagesNotLinked.value!!) {
            backup.add(
                LinkedPage(
                    pageLink.idFile, true, pageLink.fileLinkLeftPage, pageLink.fileLinkLeftPages, pageLink.fileLinkLeftPageName,
                    pageLink.fileLinkLeftPagePath, pageLink.isFileLeftDualPage, pageLink.imageLeftFileLinkPage
                )
            )
        }

        if (mBackupLinked.size > 10)
            mBackupLinked.removeAt(0)

        mBackupLinked.add(backup)
        mHasBackup.value = mBackupLinked.isNotEmpty()
    }

    fun returnBackup() {
        if (mBackupLinked.isEmpty())
            return

        notifyMessages(PageLinkType.ALL, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_UNDO_LAST_CHANGE_START)

        val backup = mBackupLinked.removeLast()

        mPagesLink.value!!.clear()
        mPagesNotLinked.value!!.clear()

        for (pageLink in backup) {
            if (!pageLink.isNotLinked)
                mPagesLink.value!!.add(pageLink)
        }

        for (pageLink in backup) {
            if (pageLink.isNotLinked)
                mPagesNotLinked.value!!.add(pageLink)
        }

        mHasBackup.value = mBackupLinked.isNotEmpty()
        notifyMessages(PageLinkType.ALL, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_UNDO_LAST_CHANGE_FINISHED)
    }

    fun reorderDoublePages(isUseDualPageCalculate: Boolean = false, initial: LinkedPage? = null) {
        if (mLinkedFile.value == null || mLinkedFile.value!!.path.isEmpty()) return

        if (mLinkedFile.value != null) {
            notifyMessages(PageLinkType.LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_DOUBLE_PAGES_START)
            generateBackup()

            val indexChanges = mutableSetOf<Int>()
            val pagesLink = mPagesLink.value!!
            val startIndex = if (initial == null) -1 else pagesLink.indexOf(initial)
            var padding = 1

            for ((index, page) in pagesLink.withIndex()) {
                if (index < startIndex || page.isDualImage || (isUseDualPageCalculate && page.isFileLeftDualPage))
                    continue

                if ((index + padding) >= pagesLink.size)
                    break

                var next = pagesLink[index + padding]
                if (next.fileLinkLeftPage == PageLinkConsts.VALUES.PAGE_EMPTY) {
                    do {
                        padding++
                        if ((index + padding) >= pagesLink.size)
                            break
                        next = pagesLink[index + padding]
                    } while (next.fileLinkLeftPage == PageLinkConsts.VALUES.PAGE_EMPTY)
                }

                if ((index + padding) >= pagesLink.size)
                    break

                if (next.isDualImage || (isUseDualPageCalculate && page.isFileLeftDualPage)) {
                    if (page.fileLinkLeftPage != PageLinkConsts.VALUES.PAGE_EMPTY)
                        continue
                    else
                        page.merge(next)
                    next.clearPageLink()
                    indexChanges.addAll(arrayOf(index, index + padding))
                } else {
                    if (isUseDualPageCalculate) {
                        if (page.fileLinkLeftPage == PageLinkConsts.VALUES.PAGE_EMPTY) {
                            page.addLeftPageLink(next)
                            next.clearLeftPageLink()
                            indexChanges.addAll(arrayOf(index, index + padding))

                            if (!page.isFileLeftDualPage) {
                                if (next.fileLinkRightPage != PageLinkConsts.VALUES.PAGE_EMPTY) {
                                    page.addRightPageLink(next)
                                    next.clearRightPageLink()
                                } else {
                                    padding++
                                    if ((index + padding) >= pagesLink.size) {
                                        padding--
                                        continue
                                    }

                                    next = pagesLink[index + padding]
                                    if (next.fileLinkLeftPage != PageLinkConsts.VALUES.PAGE_EMPTY && !next.isDualImage && !next.isFileLeftDualPage) {
                                        page.addRightFromLeftPageLink(next)
                                        next.clearLeftPageLink()
                                        indexChanges.add(index + padding)
                                    } else
                                        padding--
                                }
                            }
                        } else {
                            if (next.isFileLeftDualPage)
                                continue
                            else {
                                page.addRightFromLeftPageLink(next)
                                next.clearLeftPageLink()
                                indexChanges.addAll(arrayOf(index, index + padding))
                            }
                        }

                    } else {
                        if (page.fileLinkLeftPage == PageLinkConsts.VALUES.PAGE_EMPTY) {
                            page.addLeftPageLink(next)
                            next.clearLeftPageLink()
                            indexChanges.addAll(arrayOf(index, index + padding))

                            if (next.fileLinkRightPage != PageLinkConsts.VALUES.PAGE_EMPTY) {
                                page.addRightPageLink(next)
                                next.clearRightPageLink()
                            } else {
                                padding++
                                if ((index + padding) >= pagesLink.size) {
                                    padding--
                                    continue
                                }

                                next = pagesLink[index + padding]
                                if (!next.isDualImage && next.fileLinkLeftPage != PageLinkConsts.VALUES.PAGE_EMPTY) {
                                    page.addRightFromLeftPageLink(next)
                                    next.clearLeftPageLink()
                                    indexChanges.add(index + padding)
                                } else
                                    padding--
                            }
                        } else {
                            page.addRightFromLeftPageLink(next)
                            next.clearLeftPageLink()
                            indexChanges.addAll(arrayOf(index, index + padding))
                        }
                    }
                }
            }

            if (mPagesNotLinked.value!!.isNotEmpty()) {
                val pagesNotLinked = mPagesNotLinked.value!!.sortedBy { it.fileLinkLeftPage }.toMutableList()

                for (page in pagesLink) {
                    if (pagesNotLinked.isEmpty())
                        break

                    if (page.isDualImage || (isUseDualPageCalculate && page.isFileRightDualPage))
                        continue

                    if (page.fileLinkLeftPage != PageLinkConsts.VALUES.PAGE_EMPTY)
                        page.addRightFromLeftPageLink(pagesNotLinked.removeAt(0))
                    else if (isUseDualPageCalculate) {
                        val notLinked = pagesNotLinked.removeAt(0)
                        page.addLeftPageLink(notLinked)

                        if (notLinked.isFileLeftDualPage)
                            continue

                        if (pagesNotLinked.isEmpty())
                            break
                        page.addRightFromLeftPageLink(pagesNotLinked.removeAt(0))
                    } else {
                        page.addLeftPageLink(pagesNotLinked.removeAt(0))
                        if (pagesNotLinked.isEmpty())
                            break

                        page.addRightFromLeftPageLink(pagesNotLinked.removeAt(0))
                    }
                }

                mPagesNotLinked.value!!.clear()
                if (pagesNotLinked.isNotEmpty())
                    mPagesNotLinked.value!!.addAll(pagesNotLinked)

                notifyMessages(PageLinkType.NOT_LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_CHANGE)
            }

            for (index in indexChanges)
                notifyMessages(PageLinkType.LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_CHANGE, index)

            notifyMessages(PageLinkType.LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_DOUBLE_PAGES_FINISHED)
        }
    }

    fun reorderSimplePages(isNotify: Boolean = true, initial: LinkedPage? = null) {
        if (mLinkedFile.value == null || mLinkedFile.value!!.path.isEmpty()) return

        val hasDualImage = mPagesLink.value?.any { it.isDualImage } ?: false

        if (hasDualImage) {
            if (isNotify) {
                notifyMessages(PageLinkType.LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_SIMPLE_PAGES_START)
                generateBackup()
            }

            val indexChanges = mutableSetOf<Int>()
            val pagesLink = mPagesLink.value!!
            val startIndex = if (initial == null) 0 else pagesLink.indexOf(initial)
            var amount = 0
            pagesLink.forEach { if (it.isDualImage) amount += 1 }
            var amountNotLink = (amount * 2) - pagesLink.size

            if (amountNotLink > 0) {
                for (i in (pagesLink.size - 1) downTo startIndex) {
                    val item = pagesLink[i]
                    if (item.fileLinkLeftPage == PageLinkConsts.VALUES.PAGE_EMPTY)
                        continue

                    val page = pagesLink[i]
                    if (item.isDualImage) {
                        addNotLinked(page)
                        amountNotLink -= 2
                        page.clearPageLink()
                    } else if (item.fileLinkLeftPage != PageLinkConsts.VALUES.PAGE_EMPTY) {
                        addNotLinked(page)
                        amountNotLink--
                        page.clearLeftPageLink()
                    }

                    if (amountNotLink < 1)
                        break
                }

                mPagesNotLinked.value!!.sortBy { it.fileLinkLeftPage }

                if (isNotify)
                    notifyMessages(PageLinkType.NOT_LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_CHANGE)
            }

            var padding = 0
            val pagesLinkTemp = mutableListOf<LinkedPage>()

            for ((index, pageLink) in pagesLink.withIndex()) {
                if (index < startIndex) {
                    pagesLinkTemp.add(pageLink)
                } else {
                    val newPage = LinkedPage(pageLink)
                    pagesLinkTemp.add(newPage)
                    val page = pagesLink[index - padding]

                    newPage.addLeftPageLink(page)

                    if (page.isDualImage) {
                        page.clearLeftPageLink(true)
                        padding++
                    }
                }

                indexChanges.add(index)
            }

            mPagesLink.value = ArrayList(pagesLinkTemp)

            if (isNotify) {
                for (index in indexChanges)
                    notifyMessages(PageLinkType.LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_CHANGE, index)

                notifyMessages(PageLinkType.LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_SIMPLE_PAGES_FINISHED)
            }
        }
    }

    fun autoReorderDoublePages(type: PageLinkType, isClear: Boolean = false, isNotify: Boolean = true) {
        if (mLinkedFile.value == null || mLinkedFile.value!!.path.isEmpty()) return

        if (isNotify) {
            notifyMessages(type, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_AUTO_PAGES_START)
            generateBackup()
        }

        val hasDualImage = if (isClear) {
            reorderSimplePages(false)
            false
        } else
            mPagesLink.value?.any { it.isDualImage } ?: false

        if (!hasDualImage && (mLinkedFile.value?.id == null || isClear)) {
            val indexChanges = mutableSetOf<Int>()
            val pagesLink = mPagesLink.value!!
            val lastIndex = pagesLink.size - 1
            for ((index, page) in pagesLink.withIndex()) {
                if (page.fileLinkLeftPage == PageLinkConsts.VALUES.PAGE_EMPTY || index >= lastIndex)
                    continue

                if (page.isMangaDualPage && page.isFileLeftDualPage)
                    continue

                if (page.isMangaDualPage) {
                    val nextPage = pagesLink[index + 1]

                    if (nextPage.fileLinkLeftPage == PageLinkConsts.VALUES.PAGE_EMPTY || nextPage.isFileLeftDualPage)
                        continue

                    indexChanges.addAll(arrayOf(index, index + 1))
                    page.addRightFromLeftPageLink(nextPage)
                    nextPage.clearPageLink()

                    for ((idxNext, next) in pagesLink.withIndex()) {
                        if (idxNext < (index + 1) || idxNext >= lastIndex)
                            continue

                        val aux = pagesLink[idxNext + 1]
                        next.addLeftPageLink(aux)
                        aux.clearLeftPageLink()
                        indexChanges.addAll(arrayOf(idxNext, idxNext + 1))
                    }
                } else if (page.isFileLeftDualPage) {
                    if (pagesLink[index + 1].fileLinkLeftPage == PageLinkConsts.VALUES.PAGE_EMPTY)
                        continue

                    indexChanges.addAll(arrayOf(index, index + 1))
                    var indexEmpty = lastIndex
                    for (i in (index + 1) until lastIndex) {
                        if (pagesLink[i].fileLinkLeftPage == PageLinkConsts.VALUES.PAGE_EMPTY) {
                            indexEmpty = i
                            break
                        }
                    }

                    addNotLinked(pagesLink[indexEmpty])

                    for (i in indexEmpty downTo (index + 2)) {
                        val aux = pagesLink[i - 1]
                        pagesLink[i].addLeftPageLink(aux)
                        aux.clearLeftPageLink()
                        indexChanges.addAll(arrayOf(i, i - 1))
                    }
                }
            }

            if (isNotify)
                for (index in indexChanges)
                    notifyMessages(type, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_CHANGE, index)
        }

        if (isNotify)
            notifyMessages(type, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_AUTO_PAGES_FINISHED)
    }

    fun reorderBySortPages() {
        if (mLinkedFile.value == null || mLinkedFile.value!!.path.isEmpty()) return

        notifyMessages(PageLinkType.LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_SORTED_PAGES_START)

        generateBackup()

        val pagesNotLink = mPagesNotLinked.value!!
        val pagesLink = mPagesLink.value!!
        val pagesLinkTemp = mutableListOf<LinkedPage>()
        val pagesNotLinkTemp = arrayListOf<LinkedPage>()
        var maxNumPage = 0

        for (page in pagesLink) {
            if (page.fileLinkLeftPage > maxNumPage)
                maxNumPage = page.fileLinkLeftPage

            if (page.fileLinkRightPage > maxNumPage)
                maxNumPage = page.fileLinkRightPage
        }

        for (page in pagesNotLink) {
            if (page.fileLinkLeftPage > maxNumPage)
                maxNumPage = page.fileLinkLeftPage
        }

        for (page in pagesLink) {
            if (page.mangaPage == PageLinkConsts.VALUES.PAGE_EMPTY)
                continue

            if (page.mangaPage > maxNumPage)
                pagesLinkTemp.add(LinkedPage(page))
            else {
                val linkedPage = LinkedPage(page)
                pagesLinkTemp.add(linkedPage)

                val findPageLink = pagesLink.find { it.fileLinkLeftPage == page.mangaPage || it.fileLinkRightPage == page.mangaPage }
                    ?: pagesNotLink.find { it.fileLinkLeftPage == page.mangaPage }

                if (findPageLink != null) {
                    if (linkedPage.fileLinkRightPage == page.mangaPage)
                        linkedPage.addLeftFromRightPageLink(findPageLink)
                    else
                        linkedPage.addLeftPageLink(findPageLink)
                }
            }
        }

        if (maxNumPage >= pagesLinkTemp.size) {
            for (numPage in pagesLinkTemp.size until maxNumPage) {
                val pageLink = pagesLink.find { it.fileLinkLeftPage == numPage || it.fileLinkRightPage == numPage }
                    ?: pagesNotLink.find { it.fileLinkLeftPage == numPage }

                if (pageLink != null) {
                    if (pageLink.fileLinkLeftPage == numPage)
                        pagesNotLinkTemp.add(
                            LinkedPage(
                                pageLink.idFile, true, pageLink.fileLinkLeftPage, pageLink.fileLinkLeftPages, pageLink.fileLinkLeftPageName,
                                pageLink.fileLinkLeftPagePath, pageLink.isFileLeftDualPage, pageLink.imageLeftFileLinkPage
                            )
                        )
                    else
                        pagesNotLinkTemp.add(
                            LinkedPage(
                                pageLink.idFile,
                                true,
                                pageLink.fileLinkRightPage,
                                pageLink.fileLinkLeftPages,
                                pageLink.fileLinkRightPageName,
                                pageLink.fileLinkRightPagePath,
                                pageLink.isFileRightDualPage,
                                pageLink.imageRightFileLinkPage
                            )
                        )
                }
            }
        }

        pagesLink.clear()
        pagesNotLink.clear()
        pagesLinkTemp.sortBy { it.mangaPage }
        pagesNotLinkTemp.sortBy { it.fileLinkLeftPage }

        mPagesLink.value = ArrayList(pagesLinkTemp)
        mPagesNotLinked.value = pagesNotLinkTemp

        notifyMessages(PageLinkType.LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_SORTED_PAGES_FINISHED)
    }

    fun autoReorderDoublePages(initial: LinkedPage) {
        if (mLinkedFile.value == null || mLinkedFile.value!!.path.isEmpty()) return

        notifyMessages(PageLinkType.LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_AUTO_PAGES_START)
        generateBackup()

        val startIndex = pagesLink.value?.indexOf(initial) ?: 0
        val hasDualImage = mPagesLink.value?.filterIndexed { index, _ -> index >= startIndex }?.any { it.isDualImage } ?: false

        if (!hasDualImage) {
            val indexChanges = mutableSetOf<Int>()
            val pagesLink = mPagesLink.value!!
            val lastIndex = pagesLink.size - 1
            for ((index, page) in pagesLink.withIndex()) {
                if (index < startIndex)
                    continue

                if (page.fileLinkLeftPage == PageLinkConsts.VALUES.PAGE_EMPTY || index >= lastIndex)
                    continue

                if (page.isMangaDualPage && page.isFileLeftDualPage)
                    continue

                if (page.isMangaDualPage) {
                    val nextPage = pagesLink[index + 1]

                    if (nextPage.fileLinkLeftPage == PageLinkConsts.VALUES.PAGE_EMPTY || nextPage.isFileLeftDualPage)
                        continue

                    indexChanges.addAll(arrayOf(index, index + 1))
                    page.addRightFromLeftPageLink(nextPage)
                    nextPage.clearPageLink()

                    for ((idxNext, next) in pagesLink.withIndex()) {
                        if (idxNext < (index + 1) || idxNext >= lastIndex)
                            continue

                        val aux = pagesLink[idxNext + 1]
                        next.addLeftPageLink(aux)
                        aux.clearLeftPageLink()
                        indexChanges.addAll(arrayOf(idxNext, idxNext + 1))
                    }
                } else if (page.isFileLeftDualPage) {
                    if (pagesLink[index + 1].fileLinkLeftPage == PageLinkConsts.VALUES.PAGE_EMPTY)
                        continue

                    indexChanges.addAll(arrayOf(index, index + 1))
                    var indexEmpty = lastIndex
                    for (i in (index + 1) until lastIndex) {
                        if (pagesLink[i].fileLinkLeftPage == PageLinkConsts.VALUES.PAGE_EMPTY) {
                            indexEmpty = i
                            break
                        }
                    }

                    addNotLinked(pagesLink[indexEmpty])

                    for (i in indexEmpty downTo (index + 2)) {
                        val aux = pagesLink[i - 1]
                        pagesLink[i].addLeftPageLink(aux)
                        aux.clearLeftPageLink()
                        indexChanges.addAll(arrayOf(i, i - 1))
                    }
                }
            }

            for (index in indexChanges)
                notifyMessages(PageLinkType.LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_CHANGE, index)
        }

        notifyMessages(PageLinkType.LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_AUTO_PAGES_FINISHED)
    }

    fun reorderReturnPages(initial: LinkedPage) {
        if (mLinkedFile.value == null || mLinkedFile.value!!.path.isEmpty()) return

        if (initial.fileLinkLeftPage == PageLinkConsts.VALUES.PAGE_EMPTY) {
            notifyMessages(PageLinkType.LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_RETURN_PAGES_START)
            generateBackup()

            val indexChanges = mutableSetOf<Int>()
            val pagesLink = mPagesLink.value!!
            val startIndex = pagesLink.indexOf(initial)
            var padding = 1
            var notifyNotLinked = false
            var lastNumber = -1

            for ((index, pageLink) in pagesLink.withIndex()) {
                if (index < startIndex) {
                    lastNumber = max(lastNumber, max(pageLink.fileLinkLeftPage, pageLink.fileLinkRightPage))
                    continue
                }

                if ((index + padding) < pagesLink.size) {
                    val nextPage = pagesLink[index + padding]

                    if (nextPage.isDualImage) {
                        pageLink.addLeftPageLink(nextPage)
                        pageLink.addRightPageLink(nextPage)
                        nextPage.clearPageLink()
                    } else if (nextPage.fileLinkLeftPage != PageLinkConsts.VALUES.PAGE_EMPTY) {
                        pageLink.addLeftPageLink(nextPage)
                        nextPage.clearPageLink()
                    } else {
                        pageLink.clearPageLink()
                    }

                    lastNumber = max(lastNumber, max(pageLink.fileLinkLeftPage, pageLink.fileLinkRightPage))

                    padding++
                } else if (mPagesNotLinked.value != null && mPagesNotLinked.value!!.size > 0) {
                    mPagesNotLinked.value!!.find { it.fileLinkLeftPage > lastNumber }?.let {
                        notifyNotLinked = true
                        pageLink.addLeftPageLink(it)
                        lastNumber = max(lastNumber, it.fileLinkLeftPage)
                        mPagesNotLinked.value!!.remove(it)
                    }
                } else
                    pageLink.clearPageLink()

                indexChanges.add(index)
            }

            for (index in indexChanges)
                notifyMessages(PageLinkType.LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_CHANGE, index)

            if (notifyNotLinked)
                notifyMessages(PageLinkType.NOT_LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_CHANGE)

            notifyMessages(PageLinkType.LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_RETURN_PAGES_FINISHED)
        }
    }

    fun reorderNotLinked(initial: LinkedPage) {
        if (mLinkedFile.value == null || mLinkedFile.value!!.path.isEmpty()) return

        if (mPagesNotLinked.value == null || mPagesNotLinked.value!!.isEmpty()) return

        if (initial.fileLinkLeftPage == PageLinkConsts.VALUES.PAGE_EMPTY) {
            notifyMessages(PageLinkType.LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_GET_NOT_LINKED_START)
            generateBackup()

            val indexChanges = mutableSetOf<Int>()
            val pagesLink = mPagesLink.value!!
            val startIndex = pagesLink.indexOf(initial)
            var notifyNotLinked = false
            var lastNumber = -1

            for ((index, pageLink) in pagesLink.withIndex()) {
                lastNumber = max(lastNumber, max(pageLink.fileLinkLeftPage, pageLink.fileLinkRightPage))

                if (index < startIndex)
                    continue

                if (pageLink.fileLinkLeftPage != PageLinkConsts.VALUES.PAGE_EMPTY)
                    break

                mPagesNotLinked.value!!.find { it.fileLinkLeftPage > lastNumber }?.let {
                    notifyNotLinked = true
                    pageLink.addLeftPageLink(it)
                    lastNumber = max(lastNumber, it.fileLinkLeftPage)
                    mPagesNotLinked.value!!.remove(it)
                }

                indexChanges.add(index)
            }

            for (index in indexChanges)
                notifyMessages(PageLinkType.LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_CHANGE, index)

            if (notifyNotLinked)
                notifyMessages(PageLinkType.NOT_LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_CHANGE)

            notifyMessages(PageLinkType.LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_GET_NOT_LINKED_FINISHED)
        }
    }

    fun addNotLinked(page: LinkedPage, isRight: Boolean) {
        val index = mPagesLink.value!!.indexOf(page)
        if (isRight) {
            if (page.isDualImage) {
                mPagesNotLinked.value!!.add(
                    LinkedPage(
                        page.idFile, true, page.fileLinkRightPage, page.fileLinkLeftPages, page.fileLinkRightPageName,
                        page.fileLinkRightPagePath, page.isFileRightDualPage, page.imageRightFileLinkPage
                    )
                )
                page.clearRightPageLink()
                notifyMessages(PageLinkType.NOT_LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_IMAGE_ADDED, getPageNotLinkLastIndex())
            }
        } else {
            if (page.isDualImage) {
                if (page.fileLinkLeftPage != PageLinkConsts.VALUES.PAGE_EMPTY) {
                    mPagesNotLinked.value!!.add(
                        LinkedPage(
                            page.idFile, true, page.fileLinkLeftPage, page.fileLinkLeftPages, page.fileLinkLeftPageName,
                            page.fileLinkLeftPagePath, page.isFileLeftDualPage, page.imageLeftFileLinkPage
                        )
                    )
                    notifyMessages(PageLinkType.NOT_LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_IMAGE_ADDED, getPageNotLinkLastIndex())
                }
                page.clearLeftPageLink(true)
            } else if (page.fileLinkLeftPage != PageLinkConsts.VALUES.PAGE_EMPTY) {
                mPagesNotLinked.value!!.add(
                    LinkedPage(
                        page.idFile, true, page.fileLinkLeftPage, page.fileLinkLeftPages, page.fileLinkLeftPageName,
                        page.fileLinkLeftPagePath, page.isFileLeftDualPage, page.imageLeftFileLinkPage
                    )
                )
                page.clearLeftPageLink()
                notifyMessages(PageLinkType.NOT_LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_IMAGE_ADDED, getPageNotLinkLastIndex())
            }
        }

        notifyMessages(PageLinkType.LINKED, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_CHANGE, index)
    }

    private fun removeThread(type: PageLinkType, isEnd: Boolean = false) {
        if (!isEnd)
            mGenerateImageThread.filter { it.type.compareTo(type) == 0 }.forEach {
                (it.runnable as ImageLoadRunnable).forceEnd = true
                it.thread.interrupt()
            }

        mGenerateImageThread.removeAll { it.type.compareTo(type) == 0 }
    }

    fun endThread(isIgnoreManga: Boolean = false) {
        if (isIgnoreManga) {
            val reloadManga = mGenerateImageThread.isNotEmpty() && mGenerateImageThread.any { it.type.compareTo(PageLinkType.ALL) == 0 } &&
                    !mGenerateImageThread.any { it.type.compareTo(PageLinkType.MANGA) == 0 }

            mGenerateImageThread.filter { it.type.compareTo(PageLinkType.MANGA) != 0 }.forEach {
                (it.runnable as ImageLoadRunnable).forceEnd = true
                it.thread.interrupt()
            }
            mGenerateImageThread.removeAll { it.type.compareTo(PageLinkType.MANGA) != 0 }

            if (reloadManga)
                reLoadImages(PageLinkType.MANGA, isVerifyImages = true, isForced = true)
        } else {
            mGenerateImageThread.forEach {
                (it.runnable as ImageLoadRunnable).forceEnd = true
                it.thread.interrupt()
            }
            mGenerateImageThread.clear()
        }
    }

    fun imageThreadLoadingProgress(): Int =
        mGenerateImageThread.size

    fun addImageLoadHandler(handler: Handler) {
        mGenerateImageHandler!!.add(handler)
    }

    fun removeImageLoadHandler(handler: Handler) {
        mGenerateImageHandler!!.remove(handler)
    }

    private fun notifyMessages(type: PageLinkType, message: Int, index: Int? = null) {
        val msg = Message()
        msg.obj = ImageLoad(index, type)
        msg.what = message
        for (h in mGenerateImageHandler!!)
            h.sendMessage(msg)
    }

    fun getProgress(): Pair<Int, Int> {
        if (mGenerateImageThread.isEmpty())
            return Pair(-1, 1)

        var size = 0
        var progress = 0
        mGenerateImageThread.forEach {
            size += (it.runnable as ImageLoadRunnable).size
            progress += (it.runnable as ImageLoadRunnable).progress
        }

        return Pair(progress, size)
    }

    private fun isAllImagesLoaded(type: PageLinkType = PageLinkType.ALL): Boolean {
        val isFileLink = mLinkedFile.value!!.path != ""
        if (type != PageLinkType.NOT_LINKED)
            for (page in mPagesLink.value!!) {
                if ((type == PageLinkType.ALL || type == PageLinkType.MANGA) && (page.imageMangaPage == null))
                    return false
                if (isFileLink && (type == PageLinkType.ALL || type == PageLinkType.LINKED)) {
                    if (page.fileLinkLeftPage != PageLinkConsts.VALUES.PAGE_EMPTY && page.imageLeftFileLinkPage == null) {
                        val image = mFileLinkImageList.find { it.first == page.fileLinkLeftPage }
                        if (image?.third != null) {
                            page.isFileLeftDualPage = image.second
                            page.imageLeftFileLinkPage = image.third
                        } else
                            return false
                    } else if (page.isDualImage && page.imageRightFileLinkPage == null) {
                        val image = mFileLinkImageList.find { it.first == page.fileLinkRightPage }
                        if (image?.third != null) {
                            page.isFileRightDualPage = image.second
                            page.imageRightFileLinkPage = image.third
                        } else
                            return false
                    }
                }
            }

        if (isFileLink && (type == PageLinkType.NOT_LINKED || type == PageLinkType.ALL))
            for (page in mPagesNotLinked.value!!) {
                if (page.fileLinkLeftPage != PageLinkConsts.VALUES.PAGE_EMPTY && page.imageLeftFileLinkPage == null) {
                    val image = mFileLinkImageList.find { it.first == page.fileLinkLeftPage }
                    if (image?.third != null) {
                        page.isFileLeftDualPage = image.second
                        page.imageLeftFileLinkPage = image.third
                    } else
                        return false
                }
            }

        notifyMessages(type, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ALL_IMAGES_LOADED)
        return true
    }

    private var mLoadVerify: Int = 0
    private var mLoadError: Int = 0
    fun reLoadImages(type: PageLinkType = PageLinkType.ALL, isVerifyImages: Boolean = false, isForced: Boolean = false, isCloseThreads: Boolean = false) {
        mLoadVerify += 1
        if (!isForced && (mLoadError > 3 || (isVerifyImages && mLoadVerify > 3))) {
            if (mLoadError > 3)
                notifyMessages(type, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_IMAGE_LOAD_ERROR_ENABLE_MANUAL)
            else if (mLoadVerify > 3 && !isAllImagesLoaded(type))
                notifyMessages(type, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_IMAGE_LOAD_ERROR_ENABLE_MANUAL)

            return
        }

        if (!isForced && isVerifyImages && isAllImagesLoaded(type)) return

        if (isCloseThreads) endThread()

        clearBackup()
        Util.destroyParse(mLinkedFile.value?.parseManga, false)
        Util.destroyParse(mLinkedFile.value?.parseFileLink, false)

        val parseManga = getParse(mManga!!.file, PageLinkType.MANGA)
        val parseFileLink = getParse(mLinkedFile.value!!.file, PageLinkType.LINKED)

        mLinkedFile.value!!.parseManga = parseManga
        mLinkedFile.value!!.parseFileLink = parseFileLink

        if (type != PageLinkType.NOT_LINKED)
            getImage(parseManga, parseFileLink, mPagesLink.value!!, type, true)

        if ((type == PageLinkType.NOT_LINKED || type == PageLinkType.ALL) && mPagesNotLinked.value!!.isNotEmpty())
            getImage(null, parseFileLink, mPagesNotLinked.value!!, PageLinkType.NOT_LINKED, true)
    }

    fun allImagesLoaded(): Boolean =
        isAllImagesLoaded(PageLinkType.ALL)

    private fun getImage(parseManga: Parse?, parsePageLink: Parse?, list: ArrayList<LinkedPage>, type: PageLinkType, reload: Boolean = false) {
        if (!reload) mLoadVerify = 0

        removeThread(type)
        var imageLoadThread: ImageLoadThread? = null
        val runnable = ImageLoadRunnable(parseManga, parsePageLink, list, type, reload) {
            if (imageLoadThread != null && mGenerateImageThread.contains(imageLoadThread))
                mGenerateImageThread.remove(imageLoadThread)
        }

        val thread = Thread(runnable)
        thread.priority = Process.THREAD_PRIORITY_DEFAULT + Process.THREAD_PRIORITY_LESS_FAVORABLE
        thread.start()
        imageLoadThread = ImageLoadThread(type, thread, runnable)
        mGenerateImageThread.add(imageLoadThread)
    }

    private fun generateBitmap(parse: Parse, index: Int, setImage: (isDualPage: Boolean, image: Bitmap?) -> (Unit)) {
        val image = generateBitmap(parse, index)
        setImage(image.first, image.second)
    }

    private fun generateBitmap(parse: Parse, index: Int): Pair<Boolean, Bitmap?> {
        return if (index == -1) Pair(false, null) else
            try {
                val stream: InputStream = parse.getPage(index)
                val image = BitmapFactory.decodeStream(stream)
                val isDualPage = (image.width / image.height) > 0.9
                val bitmap = Bitmap.createScaledBitmap(
                    image,
                    ReaderConsts.PAGESLINK.IMAGES_WIDTH,
                    ReaderConsts.PAGESLINK.IMAGES_HEIGHT,
                    false
                )
                Util.closeInputStream(stream)
                Pair(isDualPage, bitmap)
            } catch (i: InterruptedIOException) {
                mLOGGER.info("Interrupted error when generate bitmap: " + i.message)
                Pair(false, null)
            } catch (e: Exception) {
                mLOGGER.info("Error when generate bitmap: " + e.message)
                Pair(false, null)
            }
    }

    inner class ImageLoad(var index: Int?, var type: PageLinkType)
    private inner class ImageLoadThread(var type: PageLinkType, var thread: Thread, var runnable: Runnable)
    private inner class ImageLoadRunnable(
        private var parseManga: Parse?, private var parsePageLink: Parse?, private var list: ArrayList<LinkedPage>,
        private var type: PageLinkType, private var reload: Boolean = false, private var callEnded: () -> (Unit)
    ) : Runnable {
        var forceEnd: Boolean = false
        var progress: Int = 0
        var size: Int = 0

        override fun run() {
            var error = false
            try {
                progress = 0
                forceEnd = false
                size = list.size
                notifyMessages(type, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_IMAGE_START)

                for ((index, page) in list.withIndex()) {
                    progress = index
                    if (reload) {
                        when (type) {
                            PageLinkType.ALL -> {
                                if ((page.isDualImage) && (page.imageMangaPage != null && page.imageLeftFileLinkPage != null && page.imageRightFileLinkPage != null))
                                    continue
                                else if (page.imageMangaPage != null && page.imageLeftFileLinkPage != null)
                                    continue
                            }
                            PageLinkType.MANGA -> if (page.imageMangaPage != null) continue
                            PageLinkType.NOT_LINKED -> if (page.imageLeftFileLinkPage != null) continue
                            PageLinkType.LINKED -> {
                                if (page.isDualImage && (page.imageLeftFileLinkPage != null && page.imageRightFileLinkPage != null))
                                    continue
                                else if (page.imageLeftFileLinkPage != null)
                                    continue
                            }
                            else -> {}
                        }
                    }

                    when (type) {
                        PageLinkType.ALL -> {
                            if (parseManga != null) {
                                val (isDualPage, image) = generateBitmap(parseManga!!, page.mangaPage)
                                page.isMangaDualPage = isDualPage
                                page.imageMangaPage = image
                            }

                            if (parsePageLink != null) {
                                val number = page.fileLinkLeftPage
                                val (isDualPage, image) = generateBitmap(parsePageLink!!, number)
                                mFileLinkImageList.find { it.first == number }?.let {
                                    mFileLinkImageList.remove(it)
                                    mFileLinkImageList.add(Triple(number, isDualPage, image))
                                }

                                if (page.fileLinkLeftPage == number) {
                                    page.imageLeftFileLinkPage = image
                                    page.isFileLeftDualPage = isDualPage

                                    if (page.isDualImage) {
                                        val number = page.fileLinkRightPage
                                        generateBitmap(
                                            parsePageLink!!,
                                            page.fileLinkRightPage
                                        ) { IsDualPage, Image ->
                                            run {
                                                mFileLinkImageList.find { it.first == number }?.let {
                                                    mFileLinkImageList.remove(it)
                                                    mFileLinkImageList.add(Triple(number, isDualPage, image))
                                                }

                                                if (page.fileLinkRightPage == number) {
                                                    page.imageRightFileLinkPage = Image
                                                    page.isFileRightDualPage = IsDualPage
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        PageLinkType.MANGA -> generateBitmap(
                            parseManga!!,
                            page.mangaPage
                        ) { isDualPage, image -> page.imageMangaPage = image; page.isMangaDualPage = isDualPage }
                        PageLinkType.NOT_LINKED -> {
                            val number = page.fileLinkLeftPage
                            val (isDualPage, image) = generateBitmap(parsePageLink!!, number)
                            mFileLinkImageList.find { it.first == number }?.let {
                                mFileLinkImageList.remove(it)
                                mFileLinkImageList.add(Triple(number, isDualPage, image))
                            }

                            if (page.fileLinkLeftPage == number) {
                                page.imageLeftFileLinkPage = image
                                page.isFileLeftDualPage = isDualPage
                            }
                        }
                        PageLinkType.LINKED -> {
                            val number = page.fileLinkLeftPage
                            val (isDualPage, image) = generateBitmap(parsePageLink!!, number)
                            mFileLinkImageList.find { it.first == number }?.let {
                                mFileLinkImageList.remove(it)
                                mFileLinkImageList.add(Triple(number, isDualPage, image))
                            }

                            if (page.fileLinkLeftPage == number) {
                                page.imageLeftFileLinkPage = image
                                page.isFileLeftDualPage = isDualPage

                                if (page.isDualImage) {
                                    val number = page.fileLinkRightPage
                                    generateBitmap(
                                        parsePageLink!!,
                                        page.fileLinkRightPage
                                    ) { IsDualPage, Image ->
                                        run {
                                            mFileLinkImageList.find { it.first == number }?.let {
                                                mFileLinkImageList.remove(it)
                                                mFileLinkImageList.add(Triple(number, isDualPage, image))
                                            }

                                            if (page.fileLinkRightPage == number) {
                                                page.imageRightFileLinkPage = Image
                                                page.isFileRightDualPage = IsDualPage
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else -> {}
                    }

                    if (forceEnd)
                        break
                    else
                        notifyMessages(type, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_IMAGE_UPDATED, index)
                }
            } catch (e: Exception) {
                error = true
                if (!forceEnd) {
                    mLoadError += 1
                    notifyMessages(type, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_IMAGE_LOAD_ERROR)
                }
            } finally {
                callEnded()

                progress = size
                if (!error)
                    mLoadError = 0

                if (!forceEnd)
                    notifyMessages(type, PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_IMAGE_FINISHED)
            }
        }
    }
}
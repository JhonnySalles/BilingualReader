package br.com.fenix.bilingualreader.service.controller

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.collection.arraySetOf
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.*
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.service.parses.manga.Parse
import br.com.fenix.bilingualreader.service.parses.manga.ParseFactory
import br.com.fenix.bilingualreader.service.parses.manga.RarParse
import br.com.fenix.bilingualreader.service.repository.SubTitleRepository
import br.com.fenix.bilingualreader.service.repository.VocabularyRepository
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.constants.ReaderConsts
import br.com.fenix.bilingualreader.util.helpers.Util
import br.com.fenix.bilingualreader.view.components.manga.PageImageView
import br.com.fenix.bilingualreader.view.ui.reader.manga.MangaReaderFragment
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.LoggerFactory
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.lang.ref.WeakReference


class SubTitleController private constructor(private val context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(SubTitleController::class.java)

    var mReaderFragment: MangaReaderFragment? = null

    private val mSubtitleRepository: SubTitleRepository = SubTitleRepository(context)
    private val mVocabularyRepository = VocabularyRepository(context)

    private lateinit var mParse: Parse
    var mManga: Manga? = null

    private var mLanguages: MutableSet<Languages> = arraySetOf()
    private var mComboListInternal: HashMap<String, Chapter> = hashMapOf()
    private var mComboListSelected: HashMap<String, Chapter> = hashMapOf()
    private var mListPages: HashMap<String, Page> = hashMapOf()
    var pathSubtitle: String = ""

    private var mChaptersKeys: MutableLiveData<List<String>> = MutableLiveData()
    var chaptersKeys: LiveData<List<String>> = mChaptersKeys
    private var mPagesKeys: MutableLiveData<List<String>> = MutableLiveData()
    var pagesKeys: LiveData<List<String>> = mPagesKeys

    private var mChapterSelected: MutableLiveData<Chapter> = MutableLiveData()
    var chapterSelected: LiveData<Chapter> = mChapterSelected
    private var mPageSelected: MutableLiveData<Page> = MutableLiveData()
    var pageSelected: LiveData<Page> = mPageSelected
    private var mTextSelected: MutableLiveData<Text> = MutableLiveData()
    var textSelected: LiveData<Text> = mTextSelected

    private var mForceExpandFloatingPopup: MutableLiveData<Boolean> = MutableLiveData(true)
    var forceExpandFloatingPopup: LiveData<Boolean> = mForceExpandFloatingPopup

    private var mSelectedSubTitle: MutableLiveData<SubTitle> = MutableLiveData()
    var selectedSubtitle: LiveData<SubTitle> = mSelectedSubTitle

    private lateinit var mSubtitleLang: Languages
    private lateinit var mTranslateLang: Languages
    private var labelChapter: String =
        context.resources.getString(R.string.popup_reading_manga_subtitle_chapter)
    private var labelExtra: String =
        context.resources.getString(R.string.popup_reading_manga_subtitle_extra)

    private var mUseFileLink: Boolean = false
    private var mFileLink: FileLink? = null

    var isSelected = false
    var isNotEmpty = false
    private fun getSubtitle(): HashMap<String, Chapter> =
        if (isSelected) mComboListSelected else mComboListInternal

    init {
        val sharedPreferences = GeneralConsts.getSharedPreferences(context)
        try {
            mSubtitleLang = Languages.valueOf(
                sharedPreferences.getString(
                    GeneralConsts.KEYS.SUBTITLE.LANGUAGE,
                    Languages.JAPANESE.toString()
                )!!
            )
            mTranslateLang = Languages.valueOf(
                sharedPreferences.getString(
                    GeneralConsts.KEYS.SUBTITLE.TRANSLATE,
                    Languages.PORTUGUESE.toString()
                )!!
            )
            mSelectedSubTitle.value = SubTitle(language = mSubtitleLang)
            mUseFileLink = sharedPreferences.getBoolean(GeneralConsts.KEYS.PAGE_LINK.USE_IN_SEARCH_TRANSLATE, false)
        } catch (e: Exception) {
            mLOGGER.error("Preferences languages not loaded: " + e.message, e)
        }
    }

    companion object {
        private lateinit var INSTANCE: SubTitleController

        fun getInstance(context: Context): SubTitleController {
            if (!::INSTANCE.isInitialized)
                INSTANCE = SubTitleController(context)
            return INSTANCE
        }
    }

    fun initialize(chapterKey: String, pageKey: String) {
        if (chapterKey.isEmpty())
            return

        selectedSubtitle(chapterKey)
        selectedPage(pageKey)
    }

    fun getPageKey(page: Page): String =
        page.number.toString().padStart(3, '0') + " " + page.name

    fun getChapterKey(chapter: Chapter): String {
        val number = if ((chapter.chapter % 1).compareTo(0) == 0)
            "%.0f".format(chapter.chapter)
        else
            "%.1f".format(chapter.chapter)
        val label = if (chapter.extra) labelExtra else labelChapter
        return chapter.language.name + " - " + label + " " + number.padStart(2, '0')
    }


    fun getListChapter(manga: Manga?, parse: Parse) =
        runBlocking { // this: CoroutineScope
            launch { // launch a new coroutine and continue
                mManga = manga
                mParse = parse
                val listJson: List<String> = mParse.getSubtitles()
                isSelected = false
                getChapterFromJson(listJson)

                manga?.let {
                    if (it.hasSubtitle != parse.hasSubtitles()) {
                        mSubtitleRepository.updateHasSubtitle(it.id, parse.hasSubtitles())
                    }
                }
            }
        }

    fun getChapterFromJson(listJson: List<String>, isSelected: Boolean = false) {
        this.isSelected = isSelected
        mLanguages.clear()
        getSubtitle().clear()
        isNotEmpty = listJson.isNotEmpty()
        if (listJson.isNotEmpty()) {
            val gson = Gson()
            val listChapter: MutableList<Chapter> = arrayListOf()

            listJson.forEach {
                try {
                    val volume: Volume = gson.fromJson(it, Volume::class.java)
                    for (chapter in volume.chapters) {
                        chapter.manga = volume.manga
                        chapter.volume = volume.volume
                        chapter.language = volume.language
                    }
                    listChapter.addAll(volume.chapters)
                } catch (volExcept: Exception) {
                    try {
                        val chapter: Chapter = gson.fromJson(it, Chapter::class.java)
                        listChapter.add(chapter)
                    } catch (chapExcept: Exception) {
                    }
                }
            }

            mVocabularyRepository.processVocabulary(mManga?.id, listChapter)
            setListChapter(listChapter)
        }
    }

    private fun setListChapter(chapters: MutableList<Chapter>) {
        if (chapters.isEmpty())
            return

        var lastLanguage: Languages = chapters[0].language
        mLanguages.add(chapters[0].language)
        for (chapter in chapters) {
            if (lastLanguage != chapter.language) {
                mLanguages.add(chapters[0].language)
                lastLanguage = chapter.language
            }
            getSubtitle()[getChapterKey(chapter)] = chapter
        }

        mChaptersKeys.value = getSubtitle().keys.toTypedArray().sorted()
    }

    private fun findKeys(imagePath: String, imageHash: String): Triple<String, String, Int> {
        var find = false
        var chapterKey = ""
        var pageKey = ""
        var pageNumber = 0
        val subtitles = getSubtitle()
        val keys = run {
            val selectedLanguage = chapterSelected.value!!.language
            subtitles.keys.filter { it.contains(selectedLanguage.name) }
        }

        for (k in keys) {
            for (p in subtitles[k]?.pages!!) {
                if (p.hash.equals(imageHash, true)) {
                    chapterKey = k
                    pageKey = getPageKey(p)
                    pageNumber = p.number
                    find = true
                    break
                }
            }
            if (find)
                break
        }

        if (chapterKey.isEmpty()) {
            find = false
            val pageName = Util.getNameFromPath(imagePath)
            val chapter = Util.getChapterFromPath(imagePath)
            for (k in keys) {
                if (chapter > -1f && (subtitles[k]?.chapter?.compareTo(chapter) ?: 0) != 0)
                    continue

                for (p in subtitles[k]?.pages!!) {
                    if (p.name.equals(pageName, true)) {
                        chapterKey = k
                        pageKey = getPageKey(p)
                        pageNumber = p.number
                        find = true
                        break
                    }
                }
                if (find)
                    break
            }

            if (chapter > -1f && chapterKey.isEmpty()) {
                find = false
                for (k in keys) {
                    for (p in subtitles[k]?.pages!!) {
                        if (p.name.equals(pageName, true)) {
                            chapterKey = k
                            pageKey = getPageKey(p)
                            pageNumber = p.number
                            find = true
                            break
                        }
                    }
                    if (find)
                        break
                }
            }
        }

        return Triple(chapterKey, pageKey, pageNumber)
    }

    fun findSubtitle() {
        val currentPage = MangaReaderFragment.mCurrentPage

        if (currentPage < 0 || mParse.numPages() < currentPage) {
            Toast.makeText(
                context,
                context.resources.getString(R.string.popup_reading_manga_subtitle_not_find_subtitle),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val image: InputStream = mParse.getPage(currentPage)
        val hash = String(Hex.encodeHex(DigestUtils.md5(image)))
        Util.closeInputStream(image)
        val path: String = mParse.getPagePath(currentPage) ?: ""

        if (chapterSelected.value == null || path.isEmpty()) {
            Toast.makeText(
                context,
                context.resources.getString(R.string.popup_reading_manga_subtitle_not_find_subtitle),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val (chapterKey, pageKey, pageNumber) = findKeys(path, hash)

        if (chapterKey.isNotEmpty()) {
            mSelectedSubTitle.value?.chapterKey = chapterKey
            mSelectedSubTitle.value?.pageKey =
                if (mListPages.keys.contains(pageKey)) pageKey else ""
            updatePageSelect()
            initialize(chapterKey, pageKey)

            val subtitles = getSubtitle()
            val text: String =
                context.resources.getString(R.string.popup_reading_manga_subtitle_find_subtitle)
            Toast.makeText(
                context,
                text.format(subtitles[chapterKey]?.chapter.toString(), pageNumber.toString()),
                Toast.LENGTH_SHORT
            ).show()
        } else
            Toast.makeText(
                context,
                context.resources.getString(R.string.popup_reading_manga_subtitle_not_find_subtitle),
                Toast.LENGTH_SHORT
            ).show()
    }

    private fun findSubtitle(
        manga: Manga,
        pageNumber: Int
    ): SubTitle {
        val image: InputStream = mParse.getPage(pageNumber)
        val hash = String(DigestUtils.md5(image))
        Util.closeInputStream(image)
        val path: String = mParse.getPagePath(pageNumber) ?: ""
        val (chapterKey, _, pageNumber) = findKeys(path, hash)

        return if (chapterKey.isNotEmpty()) {
            SubTitle(
                manga.id!!,
                mSubtitleLang,
                chapterKey,
                "",
                pageNumber,
                pathSubtitle,
                chapterSelected.value
            )
        } else
            SubTitle(
                manga.id!!,
                mSubtitleLang,
                "",
                "",
                pageNumber,
                pathSubtitle,
                null
            )
    }

    private fun searchSubtitleFromFileLink(language: Languages, isMangaLanguage : Boolean = false): Boolean {
        var findSubtitle = false
        if (mUseFileLink && mFileLink != null) {
            val currentPageNumber = MangaReaderFragment.mCurrentPage
            var currentPage: PageLink? = null
            for (page in mFileLink!!.pagesLink!!) {
                if (page.mangaPage.compareTo(currentPageNumber) == 0) {
                    currentPage = page
                    break
                }
            }

            if (currentPage != null) {
                if (isMangaLanguage || currentPage.fileLinkLeftPage > -1) {
                    var keyChapter = ""
                    var keyPage = ""
                    var pageName = ""

                    val parse: Parse?
                    try {
                        parse = if (isMangaLanguage)
                            mFileLink!!.parseManga ?: ParseFactory.create(mFileLink!!.manga!!.file)
                        else
                            mFileLink!!.parseFileLink ?: ParseFactory.create(mFileLink!!.file)

                        val fileName = if (isMangaLanguage)
                            mFileLink!!.manga!!.file.nameWithoutExtension
                        else
                            mFileLink!!.file.nameWithoutExtension

                        if ((isMangaLanguage && mFileLink!!.parseManga == null) || (!isMangaLanguage && mFileLink!!.parseFileLink == null)) {
                            if (parse is RarParse) {
                                val folder = GeneralConsts.CACHE_FOLDER.LINKED + '/' + Util.normalizeNameCache(fileName)
                                val cacheDir = File(GeneralConsts.getCacheDir(context), folder)
                                (parse as RarParse?)!!.setCacheDirectory(cacheDir)
                            }
                            
                            if (isMangaLanguage)
                                mFileLink!!.parseManga = parse
                            else
                                mFileLink!!.parseFileLink = parse
                        }

                        val pageNumber = if (isMangaLanguage)
                            currentPage.mangaPage
                        else
                            currentPage.fileLinkLeftPage
                        val image: InputStream = parse!!.getPage(pageNumber)
                        val hash = String(Hex.encodeHex(DigestUtils.md5(image)))
                        Util.closeInputStream(image)
                        val subtitles = getSubtitle().filterKeys { it.contains(language.name) }

                        var find = false
                        for (chapters in subtitles) {
                            for (page in chapters.value.pages) {
                                if (page.hash.equals(hash, true)) {
                                    keyChapter = getChapterKey(chapters.value)
                                    keyPage = getPageKey(page)
                                    find = true
                                    break
                                }
                            }
                            if (find) break
                        }

                        if (keyPage.isEmpty()) {
                            val path = parse.getPagePath(pageNumber) ?: ""
                            val chapter = Util.getChapterFromPath(path)

                            find = false
                            pageName = if (isMangaLanguage)
                                currentPage.mangaPageName
                            else
                                currentPage.fileLinkLeftPageName
                            for (chapters in subtitles) {
                                if (chapter > -1f && chapters.value.chapter.compareTo(chapter) != 0)
                                    continue

                                for (page in chapters.value.pages) {
                                    if (page.name.equals(pageName, true)) {
                                        keyChapter = getChapterKey(chapters.value)
                                        keyPage = getPageKey(page)
                                        find = true
                                        break
                                    }
                                }
                                if (find) break
                            }

                            if (chapter > -1f && keyPage.isEmpty()) {
                                find = false
                                for (chapters in subtitles) {
                                    for (page in chapters.value.pages) {
                                        if (page.name.equals(pageName, true)) {
                                            keyChapter = getChapterKey(chapters.value)
                                            keyPage = getPageKey(page)
                                            find = true
                                            break
                                        }
                                    }
                                    if (find) break
                                }
                            }
                        }

                        if (keyChapter.isNotEmpty()) {
                            selectedSubtitle(keyChapter)

                            if (keyPage.isNotEmpty())
                                selectedPage(keyPage)

                            if (mSelectedSubTitle.value != null)
                                mSelectedSubTitle.value?.language = language
                        }
                    } catch (e: Exception) {
                        mLOGGER.warn("Error find page link: " + e.message, e)
                    }

                    if (keyChapter.isNotEmpty() || keyPage.isNotEmpty())
                        findSubtitle = true
                    else {
                        Toast.makeText(
                            context,
                            context.resources.getString(R.string.popup_reading_manga_subtitle_chapter_from_page_link_not_found) + " (" +
                                    pageName + ")",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        return findSubtitle
    }

    fun changeLanguage() =
        runBlocking { // this: CoroutineScope
            launch {
                if (chaptersKeys.value == null || chaptersKeys.value!!.isEmpty()) {
                    Toast.makeText(
                        context,
                        context.resources.getString(R.string.popup_reading_manga_subtitle_list_empty),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                var toMangaLanguage = true
                var language = mSubtitleLang
                var chapterSelect = ""
                var pageSelect = ""
                if (selectedSubtitle.value != null && selectedSubtitle.value!!.chapterKey.isNotEmpty()) {
                    toMangaLanguage = selectedSubtitle.value!!.language.compareTo(mTranslateLang) == 0
                    language = if (selectedSubtitle.value!!.language.compareTo(mTranslateLang) == 0)
                        mSubtitleLang
                    else
                        mTranslateLang

                    chapterSelect = selectedSubtitle.value!!.chapterKey
                    if (chapterSelect.isNotEmpty())
                        chapterSelect = chapterSelect.substringAfterLast("-").trim()

                    pageSelect = selectedSubtitle.value!!.pageKey
                    if (pageSelect.isNotEmpty())
                        pageSelect = pageSelect.substringBefore(" ").trim()
                }

                if (searchSubtitleFromFileLink(language, toMangaLanguage))
                    return@launch

                var key = ""
                var first = ""
                for (chapter in mChaptersKeys.value!!) {
                    if (chapter.contains(language.name)) {
                        if (chapterSelect.isNotEmpty()) {
                            if (chapter.contains(chapterSelect)) {
                                key = chapter
                                break
                            } else if (first.isEmpty())
                                first = chapter
                        } else {
                            key = chapter
                            break
                        }
                    }
                }

                if (key.isEmpty() && first.isEmpty()) {
                    Toast.makeText(
                        context,
                        context.resources.getString(R.string.popup_reading_manga_subtitle_chapter_not_found) + " (" +
                                language.name + ")",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                if (key.isEmpty())
                    selectedSubtitle(first)
                else
                    selectedSubtitle(key)

                if (mSelectedSubTitle.value != null)
                    mSelectedSubTitle.value?.language = language

                if (mPagesKeys.value == null || pageSelect.isEmpty())
                    return@launch

                key = ""
                first = ""
                for (page in mPagesKeys.value!!) {
                    if (first.isEmpty())
                        first = page

                    if (page.substringBefore(" ").contains(pageSelect)) {
                        key = page
                        break
                    }
                }

                if (key.isEmpty())
                    selectedPage(first)
                else
                    selectedPage(key)
            }
        }

    fun changeSubtitleInReader(manga: Manga, pageNumber: Int) =
        runBlocking { // this: CoroutineScope
            launch {
                mManga = manga
                if (mSelectedSubTitle.value == null || mSelectedSubTitle.value?.id == null) {
                    mSelectedSubTitle.value = mSubtitleRepository.findByIdManga(manga.id!!)

                    if (mSelectedSubTitle.value == null)
                        try {
                            mSelectedSubTitle.value = findSubtitle(manga, pageNumber)
                        } catch (e: java.lang.Exception) {
                            mLOGGER.info("Subtitle not founded in file: " + e.message)
                            return@launch
                        }
                }

                if (mSelectedSubTitle.value?.pageCount != pageNumber) {
                    var differ = pageNumber - mSelectedSubTitle.value?.pageCount!!
                    if (differ == 0) differ = 1
                    val run = if (mSelectedSubTitle.value?.pageCount!! < pageNumber)
                        getNextSelectPage(differ)
                    else
                        getBeforeSelectPage(false, differ * -1)

                    if (!run) {
                        if (mSelectedSubTitle.value?.pageCount!! < pageNumber)
                            getNextSelectSubtitle()
                        else
                            getBeforeSelectSubtitle()
                    }
                }

                mSelectedSubTitle.value?.pageCount = pageNumber
                updatePageSelect()
            }
        }

    private fun updatePageSelect() {
        if (mSelectedSubTitle.value != null)
            mSubtitleRepository.save(mSelectedSubTitle.value!!)
    }

    ///////////////////// DRAWING //////////////
    private var mImageBackup = mutableMapOf<Int, Bitmap>()
    private var target: MyTarget? = null
    fun drawSelectedText() {
        if (mReaderFragment == null || pageSelected.value == null || pageSelected.value?.texts!!.isEmpty())
            return

        val view: PageImageView = mReaderFragment!!.getCurrencyImageView() ?: return
        if (mImageBackup.containsKey(MangaReaderFragment.mCurrentPage)) {
            val percentScroll = view.getScrollPercent()
            view.setImageBitmap(mImageBackup.remove(MangaReaderFragment.mCurrentPage))
            view.setScrollPercent(percentScroll)
        } else {
            target = MyTarget(view, true)
            mReaderFragment!!.loadImage(target!!, MangaReaderFragment.mCurrentPage, false)
        }
    }

    private fun drawPageLinked(path: Uri) {
        val view: PageImageView = mReaderFragment!!.getCurrencyImageView() ?: return
        if (mImageBackup.containsKey(MangaReaderFragment.mCurrentPage)) {
            val percentScroll = view.getScrollPercent()
            view.setImageBitmap(mImageBackup.remove(MangaReaderFragment.mCurrentPage))
            view.setScrollPercent(percentScroll)
            return
        }

        target = MyTarget(view)
        mReaderFragment!!.loadImage(target!!, path, false)
    }

    fun clearImageBackup() = mImageBackup.clear()
    fun removeImageBackup(pageNumber: Int) = mImageBackup.remove(pageNumber)

    inner class MyTarget(layout: View, isText: Boolean = false, isKeepScroll: Boolean = true) : Target {
        private val mLayout: WeakReference<View> = WeakReference(layout)
        private val isText = isText
        private val isKeepScroll = isKeepScroll

        override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
            val layout = mLayout.get() ?: return
            val iv = layout.findViewById<View>(R.id.page_image_view) as PageImageView
            if (isText) {
                if (bitmap == null)
                    return

                mImageBackup[MangaReaderFragment.mCurrentPage] = bitmap

                val image: Bitmap = bitmap.copy(bitmap.config, true)
                val canvas = Canvas(image)
                val paint = Paint()
                paint.color = Color.RED
                paint.strokeWidth = 3f
                paint.textSize = 50f
                for (text in pageSelected.value!!.texts) {
                    paint.style = Paint.Style.FILL
                    canvas.drawText(
                        text.sequence.toString(),
                        text.x1.toFloat(),
                        text.y1.toFloat(),
                        paint
                    )
                    paint.style = Paint.Style.STROKE
                    canvas.drawRect(
                        text.x1.toFloat(),
                        text.y1.toFloat(),
                        text.x2.toFloat(),
                        text.y2.toFloat(),
                        paint
                    )
                }
                if (isKeepScroll) {
                    val percentScroll = iv.getScrollPercent()
                    iv.setImageBitmap(image)
                    iv.setScrollPercent(percentScroll)
                } else
                    iv.setImageBitmap(image)
            } else {
                mImageBackup[MangaReaderFragment.mCurrentPage] = iv.drawable.toBitmap()

                if (isKeepScroll) {
                    val percentScroll = iv.getScrollPercent()
                    iv.setImageBitmap(bitmap)
                    iv.setScrollPercent(percentScroll)
                } else
                    iv.setImageBitmap(bitmap)
            }
        }

        override fun onBitmapFailed(e: Exception, errorDrawable: Drawable?) {
            mLOGGER.error("Bitmap load fail: " + e.message, e)
        }

        override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
        }

    }

    ///////////////////////// LANGUAGE ///////////////
    fun clearLanguage() {
        val sharedPreferences = GeneralConsts.getSharedPreferences(context)
        mTranslateLang = Languages.valueOf(
            sharedPreferences.getString(
                GeneralConsts.KEYS.SUBTITLE.TRANSLATE,
                Languages.PORTUGUESE.toString()
            )!!
        )
    }

    fun selectedLanguage(language: Languages) {
        mTranslateLang = language
    }

    ///////////////////////// VOLUME ///////////////
    fun clearExternalSubtitlesSelected() {
        isSelected = false
        mChaptersKeys.value = mComboListInternal.keys.toTypedArray().sorted()
        clearSubtitlesSelected()
    }

    fun clearSubtitlesSelected() {
        mChapterSelected.value = null
        mPageSelected.value = null
        mTextSelected.value = null
        mSelectedSubTitle.value?.language = Languages.JAPANESE
    }

    fun selectedSubtitle(key: String) {
        if (key.isNotEmpty() && getSubtitle().containsKey(key))
            setChapter(getSubtitle()[key])
    }

    private fun getNextSelectSubtitle(): Boolean {
        val index: Int = if (mChaptersKeys.value!!.isNotEmpty()
        ) mChaptersKeys.value!!.indexOf(mSelectedSubTitle.value?.chapterKey!!)
            .plus(1) else 0

        return if (getSubtitle().keys.size >= index && getSubtitle().containsKey(mChaptersKeys.value!![index])) {
            setChapter(getSubtitle()[mChaptersKeys.value!![index]])
            true
        } else
            false
    }

    private fun getBeforeSelectSubtitle(): Boolean {
        val index: Int = if (mChaptersKeys.value!!.isNotEmpty()
        ) mChaptersKeys.value!!.indexOf(mSelectedSubTitle.value?.chapterKey!!)
            .minus(1) else 0

        return if (index >= 0 && getSubtitle().containsKey(mChaptersKeys.value!![index])) {
            setChapter(getSubtitle()[mChaptersKeys.value!![index]])
            true
        } else
            false
    }

    ///////////////////// CHAPTER //////////////
    private fun setChapter(chapter: Chapter?) {
        mChapterSelected.value = chapter
        mListPages.clear()
        mSelectedSubTitle.value?.chapter = chapter
        mSelectedSubTitle.value?.language = Languages.JAPANESE
        if (chapterSelected.value != null) {
            chapterSelected.value!!.pages.forEach { mListPages[getPageKey(it)] = it }
            mPagesKeys.value = mListPages.keys.toTypedArray().sorted()
            mSelectedSubTitle.value?.chapterKey = getChapterKey(mChapterSelected.value!!)
            mSelectedSubTitle.value?.language = chapterSelected.value!!.language
            setPage(false, chapterSelected.value!!.pages[0])
        } else
            mSelectedSubTitle.value?.chapterKey = ""
    }

    private fun setPage(lastText: Boolean, page: Page?) {
        mOriginalSize = null
        mPageSelected.value = page
        mSelectedSubTitle.value?.pageKey =
            if (mPageSelected.value == null) "" else getPageKey(mPageSelected.value!!)
        if (pageSelected.value!!.texts.isNotEmpty()) {
            val text = if (lastText) pageSelected.value!!.texts.last()
            else pageSelected.value!!.texts.first()
            setText(text)
        } else
            setText(null)

        updatePageSelect()
    }

    private fun setText(text: Text?) {
        mTextSelected.value = text
    }

    fun selectedPage(index: String) {
        if (chapterSelected.value != null) {
            if (mListPages.containsKey(index))
                setPage(false, mListPages[index])
        }
    }

    fun getNextSelectPage(differ: Int = 1): Boolean {
        if (chapterSelected.value == null)
            return true

        val index: Int =
            if (mSelectedSubTitle.value?.pageKey!!.isNotEmpty())
                mPagesKeys.value!!.indexOf(mSelectedSubTitle.value?.pageKey!!)
                    .plus(differ) else 0

        return if (mListPages.size > index && mListPages.containsKey(mPagesKeys.value!![index])) {
            setPage(false, mListPages[mPagesKeys.value!![index]])
            true
        } else
            false
    }

    private fun getBeforeSelectPage(lastText: Boolean, differ: Int = 1): Boolean {
        if (chapterSelected.value == null)
            return true

        val index: Int =
            if (mSelectedSubTitle.value?.pageKey!!.isNotEmpty()) mPagesKeys.value!!.indexOf(
                mSelectedSubTitle.value?.pageKey!!
            ).minus(differ) else 0

        return if (index >= 0 && mListPages.containsKey(mPagesKeys.value!![index])) {
            setPage(lastText, mListPages[mPagesKeys.value!![index]])
            true
        } else
            false
    }

    fun getNextText(): Boolean {
        if (pageSelected.value == null || selectedSubtitle.value == null)
            return true

        val index: Int =
            if (textSelected.value != null) pageSelected.value!!.texts.indexOf(
                textSelected.value
            )
                .plus(1) else 0

        return if (pageSelected.value!!.texts.size > index) {
            setText(pageSelected.value!!.texts[index])
            true
        } else {
            getNextSelectPage()
            false
        }
    }

    fun getBeforeText(): Boolean {
        if (pageSelected.value == null || selectedSubtitle.value == null)
            return true

        val index: Int =
            if (textSelected.value != null) pageSelected.value!!.texts.indexOf(
                textSelected.value
            ).minus(1) else 0

        return if (index >= 0 && pageSelected.value!!.texts.isNotEmpty()) {
            setText(pageSelected.value!!.texts[index])
            true
        } else {
            getBeforeSelectPage(true)
            false
        }
    }

    fun getLanguage() : Languages = selectedSubtitle.value?.language ?: Languages.PORTUGUESE

    private var mOriginalSize: FloatArray? = null
    fun selectTextByCoordinate(coord: FloatArray) {
        if (pageSelected.value == null)
            return

        val point = if (!mImageBackup.containsKey(MangaReaderFragment.mCurrentPage)) {
            if (mOriginalSize == null) {
                val input = BufferedInputStream(mParse.getPage(MangaReaderFragment.mCurrentPage))
                val image = BitmapFactory.decodeStream(input)
                //Position 2 has a new image size
                mOriginalSize = if (image != null && image.width > ReaderConsts.READER.MAX_PAGE_WIDTH)
                    floatArrayOf(image.width.toFloat(), image.height.toFloat())
                else
                    floatArrayOf(coord[2], coord[3])
                Util.closeInputStream(input)
            }

            Point((coord[0] / coord[2] * mOriginalSize!![0]).toInt(), (coord[1] / coord[3] * mOriginalSize!![1]).toInt())
        } else
            Point(coord[0].toInt(), coord[1].toInt())

        pageSelected.value!!.texts.forEach {
            if (it.x1 <= point.x && it.x2 >= point.x && it.y1 <= point.y && it.y2 >= point.y) {
                setText(it)
                mForceExpandFloatingPopup.value = !mForceExpandFloatingPopup.value!!
                return@forEach
            }
        }
    }

    fun drawPageLinked() {
        if (mImageBackup.containsKey(MangaReaderFragment.mCurrentPage)) {
            val view: PageImageView = mReaderFragment!!.getCurrencyImageView() ?: return
            val percentScroll = view.getScrollPercent()
            view.setImageBitmap(mImageBackup.remove(MangaReaderFragment.mCurrentPage))
            view.setScrollPercent(percentScroll)
        } else
            locateFileLink(MangaReaderFragment.mCurrentPage)
    }

    fun setUseFileLink(useInSearchTranslate: Boolean) {
        mUseFileLink = useInSearchTranslate
    }

    fun setFileLink(file: FileLink?) {
        mFileLink = file
    }

    fun getFileLink(): FileLink? =
        mFileLink

    fun locateFileLink(pageName: String) {
        if (mFileLink == null || mFileLink!!.parseFileLink == null)
            return

        mFileLink!!.pagesLink!!.first { it.mangaPageName.compareTo(pageName, true) == 0 }.let {
            if (it.fileLinkLeftPage > -1) {
                val uri = if (it.isDualImage)
                    saveImageFolder(mFileLink!!, it.fileLinkLeftPage, it.fileLinkRightPage)
                else
                    saveImageFolder(mFileLink!!, it.fileLinkLeftPage)
                drawPageLinked(uri)
            }
        }
    }

    fun locateFileLink(page: Int) {
        if (mFileLink == null) return

        mFileLink!!.pagesLink!!.firstOrNull { it.mangaPage.compareTo(page) == 0 }.let {
            if (it != null && it.fileLinkLeftPage > -1) {
                val uri = if (it.isDualImage)
                    saveImageFolder(mFileLink!!, it.fileLinkLeftPage, it.fileLinkRightPage)
                else
                    saveImageFolder(mFileLink!!, it.fileLinkLeftPage)
                drawPageLinked(uri)
            }
        }
    }

    private var mError: Int = 0
    private fun getCombinedBitmap(fileLink: FileLink, parse: Parse, index: Int, extra: Int): Bitmap? {
        var drawnBitmap: Bitmap? = null
        try {
            val img1 = BitmapFactory.decodeStream(parse.getPage(index))
            val img2 = BitmapFactory.decodeStream(parse.getPage(extra))
            val width = img1.width + img2.width
            val height = if (img1.height > img2.height) img1.height else img2.height
            drawnBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(drawnBitmap)
            canvas.drawBitmap(img1, 0f, 0f, null)
            canvas.drawBitmap(img2, (img1.width + 1).toFloat(), 0f, null)
        } catch (e: java.lang.Exception) {
            mLOGGER.warn("Error when combine images. Attempt number: " + mError + " - " + e.message, e)
            mError += 1
            if (mError < 3) {
                getFileLinkParser(fileLink.path, fileLink)
                drawnBitmap = getCombinedBitmap(fileLink, fileLink.parseFileLink!!, index, extra)
            }
        }
        mError = 0
        return drawnBitmap
    }

    private fun saveImageFolder(fileLink: FileLink, index: Int, extra: Int = -1): Uri {
        val parse = fileLink.parseFileLink ?: getFileLinkParser(fileLink.path, fileLink)!!
        var stream = parse.getPage(index)
        if (extra > -1) {
            val newBitmap = getCombinedBitmap(fileLink, parse, index, extra)
            if (newBitmap != null) {
                Util.closeInputStream(stream)
                stream = Util.imageToInputStream(newBitmap)
            }
        }

        val cacheDir = File(GeneralConsts.getCacheDir(context), GeneralConsts.CACHE_FOLDER.IMAGE)
        if (!cacheDir.exists())
            cacheDir.mkdir()

        var name = parse.getPagePath(index) ?: index.toString()
        name = Util.getNameFromPath(name)

        if (extra > -1) {
            var nameExtra = parse.getPagePath(extra) ?: index.toString()
            nameExtra = Util.getNameFromPath(nameExtra)
            name = "dual_" + name.substringBeforeLast('.') + "-" + nameExtra.substringBeforeLast('.') + ".jpeg"
        }

        val image = File(cacheDir.path + '/' + name)
        image.writeBytes(stream.readBytes())
        Util.closeInputStream(stream)
        return image.toUri()
    }

    private fun getFileLinkParser(path: String, fileLink: FileLink? = null): Parse? {
        if (fileLink?.parseFileLink != null) {
            Util.destroyParse(fileLink.parseFileLink)
            fileLink.parseFileLink = null
        }

        val parse = ParseFactory.create(path)
        if (parse is RarParse) {
            val folder = GeneralConsts.CACHE_FOLDER.IMAGE
            val cacheDir = File(GeneralConsts.getCacheDir(context), folder)
            (parse as RarParse?)!!.setCacheDirectory(cacheDir)
        }

        if (fileLink != null)
            fileLink.parseFileLink = parse

        return parse
    }
}
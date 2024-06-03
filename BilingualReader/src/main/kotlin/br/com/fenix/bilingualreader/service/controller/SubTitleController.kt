package br.com.fenix.bilingualreader.service.controller

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
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
import br.com.fenix.bilingualreader.model.entity.LinkedFile
import br.com.fenix.bilingualreader.model.entity.LinkedPage
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.entity.SubTitle
import br.com.fenix.bilingualreader.model.entity.SubTitleChapter
import br.com.fenix.bilingualreader.model.entity.SubTitlePage
import br.com.fenix.bilingualreader.model.entity.SubTitleText
import br.com.fenix.bilingualreader.model.entity.SubTitleVolume
import br.com.fenix.bilingualreader.model.enums.ImageLoadType
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.service.ocr.OcrProcess
import br.com.fenix.bilingualreader.service.parses.manga.Parse
import br.com.fenix.bilingualreader.service.parses.manga.ParseFactory
import br.com.fenix.bilingualreader.service.parses.manga.RarParse
import br.com.fenix.bilingualreader.service.repository.SubTitleRepository
import br.com.fenix.bilingualreader.service.repository.VocabularyRepository
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.constants.ReaderConsts
import br.com.fenix.bilingualreader.util.helpers.ColorUtil
import br.com.fenix.bilingualreader.util.helpers.ColorUtil.ColorsUtils.isDark
import br.com.fenix.bilingualreader.util.helpers.ImageUtil
import br.com.fenix.bilingualreader.util.helpers.Util
import br.com.fenix.bilingualreader.view.components.manga.ImageViewPage
import br.com.fenix.bilingualreader.view.ui.reader.manga.MangaReaderFragment
import com.google.gson.Gson
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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
    private var mComboListInternal: HashMap<String, SubTitleChapter> = hashMapOf()
    private var mComboListSelected: HashMap<String, SubTitleChapter> = hashMapOf()
    private var mListPages: HashMap<String, SubTitlePage> = hashMapOf()
    var pathSubtitle: String = ""

    private var mChaptersKeys: MutableLiveData<List<String>> = MutableLiveData()
    var chaptersKeys: LiveData<List<String>> = mChaptersKeys
    private var mPagesKeys: MutableLiveData<List<String>> = MutableLiveData()
    var pagesKeys: LiveData<List<String>> = mPagesKeys

    private var mSubTitleChapterSelected: MutableLiveData<SubTitleChapter?> = MutableLiveData()
    var subTitleChapterSelected: LiveData<SubTitleChapter?> = mSubTitleChapterSelected
    private var mSubTitlePageSelected: MutableLiveData<SubTitlePage?> = MutableLiveData()
    var subTitlePageSelected: LiveData<SubTitlePage?> = mSubTitlePageSelected
    private var mSubTitleTextSelected: MutableLiveData<SubTitleText?> = MutableLiveData()
    var subTitleTextSelected: LiveData<SubTitleText?> = mSubTitleTextSelected

    private var mForceExpandFloatingPopup: MutableLiveData<Boolean> = MutableLiveData(true)
    var forceExpandFloatingPopup: LiveData<Boolean> = mForceExpandFloatingPopup

    private var mSelectedSubTitle: MutableLiveData<SubTitle> = MutableLiveData()
    var selectedSubtitle: LiveData<SubTitle> = mSelectedSubTitle

    private lateinit var mSubtitleLang: Languages
    private lateinit var mTranslateLang: Languages
    private var labelChapter: String = context.resources.getString(R.string.popup_reading_manga_subtitle_chapter)
    private var labelExtra: String = context.resources.getString(R.string.popup_reading_manga_subtitle_extra)

    private var mUseFileLink: Boolean = false
    private var mLinkedFile: LinkedFile? = null

    private var mOcrLang: Languages? = null

    var isSelected = false
    var isNotEmpty = false
    private fun getSubtitle(): HashMap<String, SubTitleChapter> = if (isSelected) mComboListSelected else mComboListInternal

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
        @SuppressLint("StaticFieldLeak")
        private lateinit var INSTANCE: SubTitleController

        fun getInstance(context: Context): SubTitleController {
            if (!::INSTANCE.isInitialized)
                INSTANCE = SubTitleController(context)
            return INSTANCE
        }

        fun getChapterFromJson(listJson: List<String>) : MutableList<SubTitleChapter>  {
            val listSubTitleChapter: MutableList<SubTitleChapter> = arrayListOf()
            if (listJson.isNotEmpty()) {
                val gson = Gson()
                listJson.forEach {
                    try {
                        val subTitleVolume: SubTitleVolume = gson.fromJson(it, SubTitleVolume::class.java)
                        for (chapter in subTitleVolume.subTitleChapters) {
                            chapter.manga = subTitleVolume.manga
                            chapter.volume = subTitleVolume.volume
                            chapter.language = subTitleVolume.language
                        }
                        listSubTitleChapter.addAll(subTitleVolume.subTitleChapters)
                    } catch (volExcept: Exception) {
                        try {
                            val subTitleChapter: SubTitleChapter = gson.fromJson(it, SubTitleChapter::class.java)
                            listSubTitleChapter.add(subTitleChapter)
                        } catch (_: Exception) {
                        }
                    }
                }
            }
            return listSubTitleChapter;
        }
    }

    fun initialize(chapterKey: String, pageKey: String) {
        if (chapterKey.isEmpty())
            return

        selectedSubtitle(chapterKey)
        selectedPage(pageKey)
    }

    fun getPageKey(subTitlePage: SubTitlePage): String =
        subTitlePage.number.toString().padStart(3, '0') + " " + subTitlePage.name

    fun getChapterKey(subTitleChapter: SubTitleChapter): String {
        val number = if ((subTitleChapter.chapter % 1).compareTo(0) == 0)
            "%.0f".format(subTitleChapter.chapter)
        else
            "%.1f".format(subTitleChapter.chapter)
        val label = if (subTitleChapter.extra) labelExtra else labelChapter
        return subTitleChapter.language.name + " - " + label + " " + number.padStart(2, '0')
    }


    fun getListChapter(manga: Manga?, parse: Parse) =
        CoroutineScope(Dispatchers.IO).launch {
            async {
                mManga = manga
                mParse = parse
                val listJson: List<String> = mParse.getSubtitles()

                withContext(Dispatchers.Main) {
                    clean()
                }

                val listSubTitleChapter = getChapterFromJson(listJson)
                mVocabularyRepository.processVocabulary(mManga?.id, listSubTitleChapter)

                withContext(Dispatchers.Main) {
                    setListChapter(listSubTitleChapter)
                }

                manga?.let {
                    if (it.hasSubtitle != parse.hasSubtitles()) {
                        mSubtitleRepository.updateHasSubtitle(it.id!!, parse.hasSubtitles())
                        manga.lastVocabImport = null
                    }
                }
            }
        }

    private fun clean() {
        mLanguages.clear()
        getSubtitle().clear()
        mChaptersKeys.value = listOf()
        mPagesKeys.value = listOf()
        clearSubtitlesSelected()
    }

    fun getChapterFromJson(listJson: List<String>, isSelected: Boolean = false) : MutableList<SubTitleChapter>  {
        this.isSelected = isSelected
        isNotEmpty = listJson.isNotEmpty()
        return SubTitleController.getChapterFromJson(listJson);
    }

    private fun setListChapter(subTitleChapters: MutableList<SubTitleChapter>) {
        if (subTitleChapters.isEmpty())
            return

        var lastLanguage: Languages = subTitleChapters[0].language
        mLanguages.add(subTitleChapters[0].language)
        for (chapter in subTitleChapters) {
            if (lastLanguage != chapter.language) {
                mLanguages.add(subTitleChapters[0].language)
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
            val selectedLanguage = subTitleChapterSelected.value!!.language
            subtitles.keys.filter { it.contains(selectedLanguage.name) }
        }

        for (k in keys) {
            for (p in subtitles[k]?.subTitlePages!!) {
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

                for (p in subtitles[k]?.subTitlePages!!) {
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
                    for (p in subtitles[k]?.subTitlePages!!) {
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

        if (subTitleChapterSelected.value == null || path.isEmpty()) {
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
            mSelectedSubTitle.value?.pageKey = if (mListPages.keys.contains(pageKey)) pageKey else ""
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
                subTitleChapterSelected.value
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
        if (mUseFileLink && mLinkedFile != null) {
            val currentPageNumber = MangaReaderFragment.mCurrentPage
            var currentPage: LinkedPage? = null
            for (page in mLinkedFile!!.pagesLink!!) {
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
                            mLinkedFile!!.parseManga ?: ParseFactory.create(mLinkedFile!!.manga!!.file)
                        else
                            mLinkedFile!!.parseFileLink ?: ParseFactory.create(mLinkedFile!!.file)

                        val fileName = if (isMangaLanguage)
                            mLinkedFile!!.manga!!.file.nameWithoutExtension
                        else
                            mLinkedFile!!.file.nameWithoutExtension

                        if ((isMangaLanguage && mLinkedFile!!.parseManga == null) || (!isMangaLanguage && mLinkedFile!!.parseFileLink == null)) {
                            if (parse is RarParse) {
                                val folder = GeneralConsts.CACHE_FOLDER.LINKED + '/' + Util.normalizeNameCache(fileName)
                                val cacheDir = File(GeneralConsts.getCacheDir(context), folder)
                                (parse as RarParse?)!!.setCacheDirectory(cacheDir)
                            }
                            
                            if (isMangaLanguage)
                                mLinkedFile!!.parseManga = parse
                            else
                                mLinkedFile!!.parseFileLink = parse
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
                            for (page in chapters.value.subTitlePages) {
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

                                for (page in chapters.value.subTitlePages) {
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
                                    for (page in chapters.value.subTitlePages) {
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
        if (mReaderFragment == null || subTitlePageSelected.value == null || subTitlePageSelected.value?.subTitleTexts!!.isEmpty())
            return

        val view: ImageViewPage = mReaderFragment!!.getCurrencyImageView() ?: return
        if (!clearDrawing()) {
            target = MyTarget(view, ImageLoadType.TEXT)
            mReaderFragment!!.loadImage(target!!, MangaReaderFragment.mCurrentPage, false)
        }
    }

    private fun drawPageLinked(path: Uri) {
        val view: ImageViewPage = mReaderFragment?.getCurrencyImageView() ?: return

        if (!clearDrawing()) {
            target = MyTarget(view)
            mReaderFragment!!.loadImage(target!!, path, false)
        }
    }

    fun clearControllers() {
        mImageBackup.clear()
        mOcrLang = null
    }

    fun removeImageBackup(pageNumber: Int) = mImageBackup.remove(pageNumber)

    inner class MyTarget(layout: View, private val type: ImageLoadType = ImageLoadType.RELOAD, private val isKeepScroll: Boolean = true) : Target {
        private val mLayout: WeakReference<View> = WeakReference(layout)

        override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
            val layout = mLayout.get() ?: return
            val iv = layout.findViewById<View>(R.id.page_image_view) as ImageViewPage
            when (type) {
                ImageLoadType.TEXT -> {
                    mImageBackup[MangaReaderFragment.mCurrentPage] = bitmap

                    val image: Bitmap = bitmap.copy(bitmap.config, true)
                    val canvas = Canvas(image)
                    val paint = Paint()
                    paint.color = Color.RED
                    paint.strokeWidth = 3f
                    paint.textSize = 50f
                    for (text in subTitlePageSelected.value!!.subTitleTexts) {
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
                }
                ImageLoadType.OCR,
                ImageLoadType.TRANSLATE -> {
                    val newBitmap = bitmap.copy(bitmap.config, true)
                    val options = if (mOcrLang == Languages.JAPANESE) JapaneseTextRecognizerOptions.Builder().build() else TextRecognizerOptions.DEFAULT_OPTIONS
                    val recognizer = TextRecognition.getClient(options)
                    val input = InputImage.fromBitmap(newBitmap, 0)

                    Toast.makeText(context, context.resources.getString(R.string.ocr_google_vision_get_request), Toast.LENGTH_SHORT).show()

                    recognizer.process(input)
                        .addOnSuccessListener {txts ->
                            if (txts.textBlocks.size == 0 )
                                Toast.makeText(context, context.resources.getString(R.string.ocr_google_vision_not_detected), Toast.LENGTH_SHORT).show()
                            else {
                                val canvas = Canvas(newBitmap)
                                val paint = Paint()
                                paint.strokeWidth = 3f
                                paint.textSize = 12f

                                txts.textBlocks.forEach { block ->
                                    block.lines.forEach { line ->
                                        line.elements.forEach { element ->
                                            val pos = element.boundingBox!!
                                            paint.color = ColorUtil.getColorPalette(bitmap, pos)
                                            paint.style = Paint.Style.FILL_AND_STROKE
                                            canvas.drawRect(pos, paint)

                                            if (paint.color.isDark())
                                                paint.color = Color.WHITE
                                            else
                                                paint.color = Color.BLACK

                                            paint.style = Paint.Style.FILL
                                            canvas.drawText(element.text, pos.left.toFloat(), pos.centerY().toFloat(), paint)
                                        }
                                    }
                                }

                                mImageBackup[MangaReaderFragment.mCurrentPage] = bitmap
                                if (isKeepScroll) {
                                    val percentScroll = iv.getScrollPercent()
                                    iv.setImageBitmap(newBitmap)
                                    iv.setScrollPercent(percentScroll)
                                } else
                                    iv.setImageBitmap(newBitmap)
                            }
                        }
                        .addOnFailureListener { e ->
                            mLOGGER.error("Error process ocr image", e)
                            Toast.makeText(context, (context.resources.getString(R.string.ocr_google_vision_error) + " " + e.message).trim(), Toast.LENGTH_SHORT).show()
                        }
                }
                else -> {
                    mImageBackup[MangaReaderFragment.mCurrentPage] = iv.drawable.toBitmap()

                    if (isKeepScroll) {
                        val percentScroll = iv.getScrollPercent()
                        iv.setImageBitmap(bitmap)
                        iv.setScrollPercent(percentScroll)
                    } else
                        iv.setImageBitmap(bitmap)
                }
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
        mSubTitleChapterSelected.value = null
        mSubTitlePageSelected.value = null
        mSubTitleTextSelected.value = null
        mSelectedSubTitle.value?.language = Languages.JAPANESE
    }

    fun selectedSubtitle(key: String) {
        if (key.isNotEmpty() && getSubtitle().containsKey(key))
            setChapter(getSubtitle()[key])
    }

    private fun getNextSelectSubtitle(): Boolean {
        val index: Int = if (mChaptersKeys.value!!.isNotEmpty()) mChaptersKeys.value!!.indexOf(mSelectedSubTitle.value?.chapterKey!!).plus(1) else 0

        return if (getSubtitle().keys.size >= index && getSubtitle().containsKey(mChaptersKeys.value!![index])) {
            setChapter(getSubtitle()[mChaptersKeys.value!![index]])
            true
        } else
            false
    }

    private fun getBeforeSelectSubtitle(): Boolean {
        val index: Int = if (mChaptersKeys.value!!.isNotEmpty()) mChaptersKeys.value!!.indexOf(mSelectedSubTitle.value?.chapterKey!!).minus(1) else 0

        return if (index >= 0 && getSubtitle().containsKey(mChaptersKeys.value!![index])) {
            setChapter(getSubtitle()[mChaptersKeys.value!![index]])
            true
        } else
            false
    }

    ///////////////////// CHAPTER //////////////
    private fun setChapter(subTitleChapter: SubTitleChapter?) {
        mSubTitleChapterSelected.value = subTitleChapter
        mListPages.clear()
        mSelectedSubTitle.value?.subTitleChapter = subTitleChapter
        mSelectedSubTitle.value?.language = Languages.JAPANESE
        if (subTitleChapterSelected.value != null) {
            subTitleChapterSelected.value!!.subTitlePages.forEach { mListPages[getPageKey(it)] = it }
            mPagesKeys.value = mListPages.keys.toTypedArray().sorted()
            mSelectedSubTitle.value?.chapterKey = getChapterKey(mSubTitleChapterSelected.value!!)
            mSelectedSubTitle.value?.language = subTitleChapterSelected.value!!.language
            setPage(false, subTitleChapterSelected.value!!.subTitlePages[0])
        } else
            mSelectedSubTitle.value?.chapterKey = ""
    }

    private fun setPage(lastText: Boolean, subTitlePage: SubTitlePage?) {
        mOriginalSize = null
        mSubTitlePageSelected.value = subTitlePage
        mSelectedSubTitle.value?.pageKey =
            if (mSubTitlePageSelected.value == null) "" else getPageKey(mSubTitlePageSelected.value!!)
        if (subTitlePageSelected.value!!.subTitleTexts.isNotEmpty()) {
            val text = if (lastText) subTitlePageSelected.value!!.subTitleTexts.last()
            else subTitlePageSelected.value!!.subTitleTexts.first()
            setText(text)
        } else
            setText(null)

        updatePageSelect()
    }

    private fun setText(subTitleText: SubTitleText?) {
        mSubTitleTextSelected.value = subTitleText
    }

    fun selectedPage(index: String) {
        if (subTitleChapterSelected.value != null) {
            if (mListPages.containsKey(index))
                setPage(false, mListPages[index])
        }
    }

    fun getNextSelectPage(differ: Int = 1): Boolean {
        if (subTitleChapterSelected.value == null)
            return true

        val index: Int = if (mSelectedSubTitle.value?.pageKey!!.isNotEmpty())
            mPagesKeys.value!!.indexOf(mSelectedSubTitle.value?.pageKey!!).plus(differ)
        else
            0

        return if (mListPages.size > index && mListPages.containsKey(mPagesKeys.value!![index])) {
            setPage(false, mListPages[mPagesKeys.value!![index]])
            true
        } else
            false
    }

    private fun getBeforeSelectPage(lastText: Boolean, differ: Int = 1): Boolean {
        if (subTitleChapterSelected.value == null)
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
        if (subTitlePageSelected.value == null || selectedSubtitle.value == null)
            return true

        val index: Int =
            if (subTitleTextSelected.value != null) subTitlePageSelected.value!!.subTitleTexts.indexOf(
                subTitleTextSelected.value
            )
                .plus(1) else 0

        return if (subTitlePageSelected.value!!.subTitleTexts.size > index) {
            setText(subTitlePageSelected.value!!.subTitleTexts[index])
            true
        } else {
            getNextSelectPage()
            false
        }
    }

    fun getBeforeText(): Boolean {
        if (subTitlePageSelected.value == null || selectedSubtitle.value == null)
            return true

        val index: Int =
            if (subTitleTextSelected.value != null) subTitlePageSelected.value!!.subTitleTexts.indexOf(
                subTitleTextSelected.value
            ).minus(1) else 0

        return if (index >= 0 && subTitlePageSelected.value!!.subTitleTexts.isNotEmpty()) {
            setText(subTitlePageSelected.value!!.subTitleTexts[index])
            true
        } else {
            getBeforeSelectPage(true)
            false
        }
    }

    fun getLanguage() : Languages = selectedSubtitle.value?.language ?: Languages.PORTUGUESE

    private var mOriginalSize: FloatArray? = null
    fun selectTextByCoordinate(coord: FloatArray) {
        if (subTitlePageSelected.value == null)
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

        subTitlePageSelected.value!!.subTitleTexts.forEach {
            if (it.x1 <= point.x && it.x2 >= point.x && it.y1 <= point.y && it.y2 >= point.y) {
                setText(it)
                mForceExpandFloatingPopup.value = !mForceExpandFloatingPopup.value!!
                return@forEach
            }
        }
    }

    fun isDrawing() = mImageBackup.containsKey(MangaReaderFragment.mCurrentPage)

    private fun clearDrawing() : Boolean {
        return if (isDrawing()) {
            val view: ImageViewPage = mReaderFragment!!.getCurrencyImageView() ?: return false
            val percentScroll = view.getScrollPercent()
            view.setImageBitmap(mImageBackup.remove(MangaReaderFragment.mCurrentPage))
            view.setScrollPercent(percentScroll)
            true
        } else
            false
    }

    fun drawPageLinked() {
        if (!clearDrawing())
            locateFileLink(MangaReaderFragment.mCurrentPage)
    }

    fun drawOcrPage(type: ImageLoadType, ocr: OcrProcess) {
        val view: ImageViewPage = mReaderFragment?.getCurrencyImageView() ?: return
        mOcrLang = ocr.getLanguage() ?: return
        if (!clearDrawing())
            mReaderFragment!!.loadImage(MyTarget(view, type), MangaReaderFragment.mCurrentPage, false)
    }

    fun setUseFileLink(useInSearchTranslate: Boolean) {
        mUseFileLink = useInSearchTranslate
    }

    fun setFileLink(file: LinkedFile?) {
        mLinkedFile = file
    }

    fun getFileLink(): LinkedFile? =
        mLinkedFile

    fun locateFileLink(pageName: String) {
        if (mLinkedFile == null || mLinkedFile!!.parseFileLink == null)
            return

        mLinkedFile!!.pagesLink!!.first { it.mangaPageName.compareTo(pageName, true) == 0 }.let {
            if (it.fileLinkLeftPage > -1) {
                val uri = if (it.isDualImage)
                    saveImageFolder(mLinkedFile!!, it.fileLinkLeftPage, it.fileLinkRightPage)
                else
                    saveImageFolder(mLinkedFile!!, it.fileLinkLeftPage)
                drawPageLinked(uri)
            }
        }
    }

    fun locateFileLink(page: Int) {
        if (mLinkedFile == null)
            return

        mLinkedFile!!.pagesLink!!.firstOrNull { it.mangaPage.compareTo(page) == 0 }.let {
            if (it != null && it.fileLinkLeftPage > -1) {
                val uri = if (it.isDualImage)
                    saveImageFolder(mLinkedFile!!, it.fileLinkLeftPage, it.fileLinkRightPage)
                else
                    saveImageFolder(mLinkedFile!!, it.fileLinkLeftPage)
                drawPageLinked(uri)
            }
        }
    }

    private var mError: Int = 0
    private fun getCombinedBitmap(linkedFile: LinkedFile, parse: Parse, index: Int, extra: Int): Bitmap? {
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
                getFileLinkParser(linkedFile.path, linkedFile)
                drawnBitmap = getCombinedBitmap(linkedFile, linkedFile.parseFileLink!!, index, extra)
            }
        }
        mError = 0
        return drawnBitmap
    }

    private fun saveImageFolder(linkedFile: LinkedFile, index: Int, extra: Int = -1): Uri {
        val parse = linkedFile.parseFileLink ?: getFileLinkParser(linkedFile.path, linkedFile)!!
        var stream = parse.getPage(index)
        if (extra > -1) {
            val newBitmap = getCombinedBitmap(linkedFile, parse, index, extra)
            if (newBitmap != null) {
                Util.closeInputStream(stream)
                stream = ImageUtil.imageToInputStream(newBitmap)
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

    private fun getFileLinkParser(path: String, linkedFile: LinkedFile? = null): Parse? {
        if (linkedFile?.parseFileLink != null) {
            Util.destroyParse(linkedFile.parseFileLink)
            linkedFile.parseFileLink = null
        }

        val parse = ParseFactory.create(path)
        if (parse is RarParse) {
            val folder = GeneralConsts.CACHE_FOLDER.IMAGE + '/' + Util.normalizeNameCache(Util.getNameFromPath(path))
            val cacheDir = File(GeneralConsts.getCacheDir(context), folder)
            (parse as RarParse?)!!.setCacheDirectory(cacheDir)
        }

        if (linkedFile != null)
            linkedFile.parseFileLink = parse

        return parse
    }
}
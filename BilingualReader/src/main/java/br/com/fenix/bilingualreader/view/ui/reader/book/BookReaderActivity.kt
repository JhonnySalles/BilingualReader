package br.com.fenix.bilingualreader.view.ui.reader.book

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.*
import android.graphics.Bitmap
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.*
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import br.com.ebook.foobnix.android.utils.Dips
import br.com.ebook.foobnix.ext.CacheZipUtils
import br.com.ebook.foobnix.pdf.info.AppSharedPreferences
import br.com.ebook.foobnix.pdf.info.ExtUtils
import br.com.ebook.foobnix.pdf.info.IMG
import br.com.ebook.foobnix.pdf.info.TintUtil
import br.com.ebook.foobnix.pdf.info.wrapper.AppState
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.Position
import br.com.fenix.bilingualreader.model.enums.Themes
import br.com.fenix.bilingualreader.service.ocr.OcrProcess
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.LibraryRepository
import br.com.fenix.bilingualreader.service.repository.Storage
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.FileUtil
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil.ThemeUtils.getColorFromAttr
import br.com.fenix.bilingualreader.util.helpers.Util
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.google.android.material.tabs.TabLayout
import org.ebookdroid.common.cache.CacheManager
import org.ebookdroid.common.settings.SettingsManager
import org.slf4j.LoggerFactory
import java.io.File


class BookReaderActivity : AppCompatActivity(), OcrProcess {

    init {
        System.loadLibrary("mypdf")
        System.loadLibrary("mobi")
    }

    private val mLOGGER = LoggerFactory.getLogger(BookReaderActivity::class.java)

    private val mViewModel: BookReaderViewModel by viewModels()

    private lateinit var mToolBarTop: AppBarLayout
    private lateinit var mToolBarChapter: TextView
    private lateinit var mToolBarTitle : TextView
    private lateinit var mToolBarTitleContent : LinearLayout

    private lateinit var mToolBarBottomTitle: TextView
    private lateinit var mToolBarBottom: LinearLayout
    private lateinit var mToolBarBottomProgress: Slider
    private lateinit var mToolBarBottomAuthor: TextView

    private lateinit var mBackgroundContainer: LinearLayout
    private lateinit var mBackgroundTitle: TextView
    private lateinit var mBackgroundProgress: ProgressBar
    private lateinit var mBackgroundClock: TextClock
    private lateinit var mBackgroundBattery: TextView

    private lateinit var mTouchView: ConstraintLayout

    private var mHandler = Handler(Looper.getMainLooper())
    private val mMonitoringBattery = Runnable { getBatteryPercent() }
    private val mDismissTouchView = Runnable { closeViewTouch() }

    private lateinit var mPreferences: SharedPreferences
    private lateinit var mStorage: Storage
    private lateinit var mRepository: BookRepository
    private lateinit var mLibrary: Library
    private var mFragment: BookReaderFragment? = null
    private var mBook: Book? = null

    companion object {
        private lateinit var mPopupTranslateTab: TabLayout
        fun selectTabReader() =
            mPopupTranslateTab.selectTab(mPopupTranslateTab.getTabAt(0), true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val theme = Themes.valueOf(
            GeneralConsts.getSharedPreferences(this)
                .getString(GeneralConsts.KEYS.THEME.THEME_USED, Themes.ORIGINAL.toString())!!
        )
        setTheme(theme.getValue())

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_reader)

        // Dependencias dos projetos do livro
        Dips.init(this)
        AppState.get().load(this)
        AppSharedPreferences.get().init(this)
        CacheZipUtils.init(this)
        ExtUtils.init(this)
        IMG.init(this)
        TintUtil.init()
        SettingsManager.init(this)
        CacheManager.init(this)
        // -------

        mRepository = BookRepository(applicationContext)

        mToolBarTop = findViewById(R.id.reader_book_toolbar_top)
        mToolBarChapter = findViewById(R.id.reader_book_toolbar_chapter)
        mToolBarTitle = findViewById(R.id.reader_book_toolbar_title)
        mToolBarTitleContent = findViewById(R.id.reader_book_toolbar_title_content)

        mToolBarBottom = findViewById(R.id.reader_book_toolbar_bottom)
        mToolBarBottomTitle = findViewById(R.id.reader_book_toolbar_bottom_title)
        mToolBarBottomProgress = findViewById(R.id.reader_book_toolbar_bottom_progress)
        mToolBarBottomAuthor = findViewById(R.id.reader_book_toolbar_bottom_author)

        mBackgroundContainer = findViewById(R.id.container_book_progress)
        mBackgroundTitle = findViewById(R.id.book_progress_text)
        mBackgroundProgress = findViewById(R.id.book_progress_bar)
        mBackgroundClock = findViewById(R.id.book_progress_clock)
        mBackgroundBattery = findViewById(R.id.book_progress_battery)

        mToolBarTitleContent.setOnClickListener { dialogPageIndex() }
        mToolBarTitleContent.setOnLongClickListener {
            mBook?.let { FileUtil(this).copyName(it) }
            true
        }

        if (savedInstanceState == null) {
            if (Intent.ACTION_VIEW == intent.action) {
                if (intent.extras != null && intent.extras!!.containsKey(GeneralConsts.KEYS.BOOK.ID)) {
                    val book =
                        mRepository.get(intent.extras!!.getLong(GeneralConsts.KEYS.BOOK.ID))
                    book?.fkLibrary?.let {
                        val library = LibraryRepository(this)
                        mLibrary = library.get(it) ?: mLibrary
                    }
                    initialize(book)
                } else
                    intent.data?.path?.let {
                        val file = File(it)
                        initialize(file, 0, 100)
                    }
            } else {
                val extras = intent.extras

                if (extras != null)
                    mLibrary = extras.getSerializable(GeneralConsts.KEYS.OBJECT.LIBRARY) as Library

                val book =
                    if (extras != null) (extras.getSerializable(GeneralConsts.KEYS.OBJECT.BOOK) as Book?) else null
                book?.let {
                    it.bookMark = extras?.getInt(GeneralConsts.KEYS.BOOK.MARK) ?: 0
                }

                initialize(book)
            }
        } else
            mFragment =
                supportFragmentManager.findFragmentById(R.id.root_frame_book_reader) as BookReaderFragment?

        getBatteryPercent()
    }

    private fun initialize(book: Book?) {
        val fragment: BookReaderFragment = if (book != null && book.file.exists()) {
            setBook(book)
            BookReaderFragment.create(mLibrary, book)
        } else
            BookReaderFragment.create()

        setFragment(fragment)
    }

    private fun initialize(file: File?, page: Int, pages: Int) {
        val fragment: BookReaderFragment = if (file != null) {
            changePage(0, "", page, pages)
            BookReaderFragment.create(mLibrary, file)
        } else
            BookReaderFragment.create()

        setFragment(fragment)
    }

    private fun switchBook(isNext: Boolean = true) {
        if (mBook == null) return

        val changeBook = if (isNext)
            mStorage.getNextBook(mLibrary, mBook!!)
        else
            mStorage.getPrevBook(mLibrary, mBook!!)

        if (changeBook == null) {
            val content =
                if (isNext) R.string.switch_next_comic_last_comic else R.string.switch_prev_comic_first_comic
            MaterialAlertDialogBuilder(this, R.style.AppCompatAlertDialogStyle)
                .setTitle(getString(R.string.switch_next_comic_not_found))
                .setMessage(content)
                .setPositiveButton(
                    R.string.action_neutral
                ) { _, _ ->
                }
                .create().show()
            return
        }

        val title = if (isNext) R.string.switch_next_comic else R.string.switch_prev_comic

        val dialog: AlertDialog =
            MaterialAlertDialogBuilder(this, R.style.AppCompatAlertDialogStyle)
                .setTitle(title)
                .setMessage(changeBook.file.name)
                .setPositiveButton(
                    R.string.switch_action_positive
                ) { _, _ ->
                    changeBook(changeBook)
                }
                .setNegativeButton(
                    R.string.switch_action_negative
                ) { _, _ -> }
                .create()
        dialog.show()
    }

    fun changeBook(book: Book) {
        setBook(book)

        //****setFragment(Fragment.create(mLibrary, book))
    }

    fun changePage(chapter: Int, description: String, page: Int, pages: Int) {
        mToolBarBottomTitle.text =
            if (page > 0) getString(R.string.reading_book_title_position, page, pages, Util.formatDecimal(page.toFloat() / pages * 100)) else ""
        mToolBarChapter.text =
            if (chapter > 0) getString(R.string.reading_book_title_chapter, chapter, description) else description
        mBackgroundProgress.progress = page
        mBackgroundProgress.max = pages
        mBackgroundTitle.text = if (description.isNotEmpty())
            getString(R.string.book_chapter, description, page, pages)
        else
            getString(R.string.progress, page, pages)
    }

    private fun setBook(book: Book) {
        changePage(book.chapter, book.chapterDescription, book.bookMark, book.pages)
        mBook = book
        mRepository.updateLastAccess(book)

        mToolBarTitle.text = book.name
        mToolBarBottomAuthor.text = book.author
    }

    private fun dialogPageIndex() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.root_frame_book_reader) ?: return
        val codec = (currentFragment as BookReaderFragment).mCodecDocument ?: return

        if (codec.outline.isEmpty()) {
            MaterialAlertDialogBuilder(this, R.style.AppCompatAlertDialogStyle)
                .setTitle(resources.getString(R.string.reading_book_page_index))
                .setMessage(resources.getString(R.string.reading_book_page_empty))
                .setPositiveButton(
                    R.string.action_neutral
                ) { _, _ -> }
                .create()
                .show()
            return
        }

        var chapter = 0
        val chapters = codec.outline.filter { it.title.isNotEmpty() }.associate {"${++chapter} - ${it.title}" to it.link.replace(Regex("[^\\d+]"), "") }
        val items = chapters.keys.toTypedArray()

        val title = LinearLayout(this)
        title.orientation = LinearLayout.VERTICAL
        title.setPadding(resources.getDimensionPixelOffset(R.dimen.page_link_page_index_title_padding))
        val name = TextView(this)
        name.text = mToolBarTitle.text
        name.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.title_index_dialog_size))
        name.setTextColor(getColorFromAttr(R.attr.colorPrimary))
        title.addView(name)
        val index = TextView(this)
        index.text = resources.getString(R.string.reading_book_page_index)
        index.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.title_small_index_dialog_size))
        index.setTextColor(getColorFromAttr(R.attr.colorSecondary))
        title.addView(index)
        title.setOnLongClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied Text", mToolBarTitle.text)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(
                this,
                getString(R.string.action_copy, mToolBarTitle.text),
                Toast.LENGTH_LONG
            ).show()

            true
        }

        MaterialAlertDialogBuilder(this, R.style.AppCompatMaterialAlertList)
            .setCustomTitle(title)
            .setItems(items) { _, selected ->
                val pageNumber = chapters[items[selected]]
                if (pageNumber != null && pageNumber.isNotEmpty())
                    currentFragment.setCurrentPage(pageNumber.toInt())
            }
            .show()
    }

    @SuppressLint("MissingSuperCall")
    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        if (mBook != null)
            savedInstanceState.putSerializable(GeneralConsts.KEYS.OBJECT.MANGA, mBook)

        super.onSaveInstanceState(savedInstanceState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        val book = (savedInstanceState.getSerializable(GeneralConsts.KEYS.OBJECT.BOOK) as Book?)
        if (book != null) {
            mBook = book
            changePage(book.chapter, book.chapterDescription, book.bookMark, book.pages)
        }
    }

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (mHandler.hasCallbacks(mMonitoringBattery))
                mHandler.removeCallbacks(mMonitoringBattery)

            if (mHandler.hasCallbacks(mDismissTouchView))
                mHandler.removeCallbacks(mDismissTouchView)
        } else {
            mHandler.removeCallbacks(mMonitoringBattery)
            mHandler.removeCallbacks(mDismissTouchView)
        }

        super.onDestroy()
    }

    fun setFragment(fragment: Fragment) {
        mFragment = if (fragment is BookReaderFragment) fragment else null
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.root_frame_book_reader, fragment)
            .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        /*if (mMenuPopupTranslate.visibility != View.GONE || mMenuPopupColor.visibility != View.GONE) {
            mMenuPopupTranslate.visibility = View.GONE
            mMenuPopupColor.visibility = View.GONE
            return
        }*/

        super.onBackPressed()
        finish()
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val favoriteItem = menu.findItem(R.id.menu_item_reader_favorite)
        val icon = if (mBook != null && mBook!!.favorite)
            ContextCompat.getDrawable(this, R.drawable.ico_favorite_mark)
        else
            ContextCompat.getDrawable(this, R.drawable.ico_favorite_unmark)
        icon?.setTint(getColorFromAttr(R.attr.colorSecondary))
        favoriteItem.icon = icon
        return super.onPrepareOptionsMenu(menu)
    }

    private fun changeFavorite(item: MenuItem) {
        if (mBook == null)
            return

        mBook?.favorite = !mBook!!.favorite

        val icon = if (mBook!!.favorite)
            ContextCompat.getDrawable(this, R.drawable.ico_animated_favorited_marked)
        else
            ContextCompat.getDrawable(this, R.drawable.ico_animated_favorited_unmarked)
        icon?.setTint(getColorFromAttr(R.attr.colorSecondary))
        item.icon = icon
        (item.icon as AnimatedVectorDrawable).start()
        mRepository.update(mBook!!)
    }

    private fun openViewTouch() {
        mFragment?.setFullscreen(true)

        mTouchView.alpha = 0.0f
        mTouchView.animate().alpha(1.0f).setDuration(300L)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    mTouchView.visibility = View.VISIBLE
                }
            })

        mHandler.postDelayed(mDismissTouchView, 5000)
    }

    fun touchPosition(position: Position): Boolean {
        /*if (position != Position.BOTTOM && mChapterContent.visibility == View.VISIBLE) {
            chapterVisibility(false)
            return true
        }*/

        return when (position) {
            Position.CORNER_TOP_RIGHT -> {
                //mFragment?.changeAspect(mToolBar, ReaderMode.FIT_WIDTH)
                true
            }
            Position.CORNER_TOP_LEFT -> {
                //mFragment?.changeAspect(mToolBar, ReaderMode.ASPECT_FIT)
                true
            }
            Position.CORNER_BOTTOM_RIGHT -> {
                mFragment?.hitEnding()
                true
            }
            Position.CORNER_BOTTOM_LEFT -> {
                mFragment?.hitBeginning()
                true
            }
            Position.BOTTOM -> {
                /*val initial = mFragment?.getCurrentPage() ?: 0
                val loaded = mViewModel.loadChapter(
                    mBook,
                    initial
                ) { page ->
                    if (!mChapterList.isComputingLayout) mChapterList.adapter?.notifyItemChanged(
                        page
                    )
                }
                chapterVisibility(true)
                mFragment?.let {
                    if (loaded)
                        mChapterList.scrollToPosition(it.getCurrentPage() - 1)
                    else
                        mChapterList.smoothScrollToPosition(it.getCurrentPage() - 1)

                    mViewModel.selectPage(it.getCurrentPage())
                }*/
                true
            }
            else -> false
        }
    }

    private fun closeViewTouch() {
        mTouchView.alpha = 1.0f
        mTouchView.animate().alpha(0.0f).setDuration(300L)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    mTouchView.visibility = View.GONE
                }
            })
    }

    override fun getImage(): Bitmap? {
        TODO("Not yet implemented")
    }

    override fun getImage(x: Int, y: Int, width: Int, height: Int): Bitmap? {
        TODO("Not yet implemented")
    }

    override fun getLanguage(): Languages {
        TODO("Not yet implemented")
    }

    override fun setText(text: String?) {
        TODO("Not yet implemented")
    }

    override fun setText(text: ArrayList<String>) {
        TODO("Not yet implemented")
    }

    override fun clearList() {
        TODO("Not yet implemented")
    }

    private fun getBatteryPercent() {
        try {
            val percent =
                (getSystemService(BATTERY_SERVICE) as BatteryManager).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            mBackgroundBattery.text = getString(R.string.percent, percent)
        } finally {
            mHandler.postDelayed(mMonitoringBattery, 60000)
        }
    }

}
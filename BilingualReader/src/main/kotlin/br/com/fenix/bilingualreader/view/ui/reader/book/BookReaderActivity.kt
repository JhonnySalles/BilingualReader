package br.com.fenix.bilingualreader.view.ui.reader.book

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.enums.PageMode
import br.com.fenix.bilingualreader.model.enums.Position
import br.com.fenix.bilingualreader.model.enums.ReaderMode
import br.com.fenix.bilingualreader.model.enums.Themes
import br.com.fenix.bilingualreader.service.japanese.Formatter
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.LibraryRepository
import br.com.fenix.bilingualreader.service.repository.SharedData
import br.com.fenix.bilingualreader.service.repository.Storage
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.AnimationUtil
import br.com.fenix.bilingualreader.util.helpers.FileUtil
import br.com.fenix.bilingualreader.util.helpers.MenuUtil
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil.ThemeUtils.getColorFromAttr
import br.com.fenix.bilingualreader.util.helpers.Util
import br.com.fenix.bilingualreader.view.components.DottedSeekBar
import br.com.fenix.bilingualreader.view.ui.menu.MenuActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import org.slf4j.LoggerFactory
import java.io.File


class BookReaderActivity : AppCompatActivity() {

    private val mLOGGER = LoggerFactory.getLogger(BookReaderActivity::class.java)

    private val mViewModel: BookReaderViewModel by lazy { ViewModelProvider(this)[BookReaderViewModel::class.java] }

    private lateinit var mToolBarTop: Toolbar
    private lateinit var mToolBarChapter: TextView
    private lateinit var mToolBarTitle : TextView

    private lateinit var mToolBarBottomProgressTitle: TextView
    private lateinit var mToolBarBottom: LinearLayout
    private lateinit var mToolBarBottomProgress: DottedSeekBar
    private lateinit var mToolBarBottomAuthor: TextView

    private lateinit var mBackgroundContainer: LinearLayout
    private lateinit var mBackgroundTitle: TextView
    private lateinit var mBackgroundProgress: ProgressBar
    private lateinit var mBackgroundClock: TextClock

    private lateinit var mMenuPopupConfiguration: FrameLayout
    private lateinit var mPopupConfigurationTab: TabLayout
    private lateinit var mPopupConfigurationView: ViewPager

    private lateinit var mPopupReaderFont: PopupBookFont
    private lateinit var mPopupReaderLayout: PopupBookLayout
    private lateinit var mPopupReaderLanguage: PopupBookLanguage

    private var mMenuPopupBottomSheet: Boolean = false
    private lateinit var mBottomSheetConfiguration: BottomSheetBehavior<FrameLayout>

    private lateinit var mTouchView: ConstraintLayout

    private val mHandler = Handler(Looper.getMainLooper())
    private val mDismissTouchView = Runnable { closeViewTouch() }

    private lateinit var mPreferences: SharedPreferences
    private lateinit var mStorage: Storage
    private lateinit var mRepository: BookRepository
    private lateinit var mLibrary: Library
    private var mFragment: BookReaderFragment? = null
    private var mBook: Book? = null
    private var mDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val theme = Themes.valueOf(GeneralConsts.getSharedPreferences(this).getString(GeneralConsts.KEYS.THEME.THEME_USED, Themes.ORIGINAL.toString())!!)
        setTheme(theme.getValue())

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_reader)

        Formatter.initializeAsync(applicationContext)

        mRepository = BookRepository(applicationContext)

        mToolBarTop = findViewById(R.id.toolbar_book_reader)
        MenuUtil.tintToolbar(mToolBarTop, theme)
        mToolBarChapter = findViewById(R.id.reader_book_toolbar_chapter)
        mToolBarTitle = findViewById(R.id.reader_book_toolbar_title)

        mToolBarBottom = findViewById(R.id.reader_book_toolbar_bottom)
        mToolBarBottomProgressTitle = findViewById(R.id.reader_book_bottom_progress_title)
        mToolBarBottomProgress = findViewById(R.id.reader_book_bottom_progress)
        mToolBarBottomAuthor = findViewById(R.id.reader_book_toolbar_bottom_author)

        mBackgroundContainer = findViewById(R.id.container_book_progress)
        mBackgroundTitle = findViewById(R.id.book_progress_text)
        mBackgroundProgress = findViewById(R.id.book_progress_bar)
        mBackgroundClock = findViewById(R.id.book_progress_clock)

        setSupportActionBar(mToolBarTop)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        mMenuPopupConfiguration = findViewById(R.id.popup_book_configuration)
        mPopupConfigurationTab = findViewById(R.id.popup_book_configuration_tab)
        mPopupConfigurationView = findViewById(R.id.popup_book_configuration_view_pager)

        mToolBarTitle.setOnClickListener {  }
        mToolBarTitle.setOnLongClickListener {
            mBook?.let { FileUtil(this).copyName(it) }
            true
        }

        if (findViewById<ImageView>(R.id.popup_book_configuration_center_button) == null)
            mMenuPopupBottomSheet = true
        else {
            mBottomSheetConfiguration = BottomSheetBehavior.from(mMenuPopupConfiguration).apply {
                peekHeight = 195
                this.state = BottomSheetBehavior.STATE_COLLAPSED
                mBottomSheetConfiguration = this
            }
            mBottomSheetConfiguration.isDraggable = false

            findViewById<ImageView>(R.id.popup_book_configuration_center_button).setOnClickListener {
                if (mBottomSheetConfiguration.state == BottomSheetBehavior.STATE_COLLAPSED)
                    mBottomSheetConfiguration.state = BottomSheetBehavior.STATE_EXPANDED
                else
                    mBottomSheetConfiguration.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

        findViewById<ImageView>(R.id.popup_book_configuration_close_button).setOnClickListener {
            AnimationUtil.animatePopupClose(this, mMenuPopupConfiguration, !mMenuPopupBottomSheet, navigationColor = false)
        }

        mPopupReaderFont = PopupBookFont()
        mPopupReaderLayout = PopupBookLayout()
        mPopupReaderLanguage = PopupBookLanguage()

        mPopupConfigurationTab.setupWithViewPager(mPopupConfigurationView)

        val viewPagerAdapter = ViewPagerAdapter(supportFragmentManager, 0)
        viewPagerAdapter.addFragment(
            mPopupReaderFont,
            resources.getString(R.string.popup_reading_book_tab_item_font)
        )
        viewPagerAdapter.addFragment(
            mPopupReaderLayout,
            resources.getString(R.string.popup_reading_book_tab_item_layout)
        )
        viewPagerAdapter.addFragment(
            mPopupReaderLanguage,
            resources.getString(R.string.popup_reading_book_tab_item_language)
        )

        mPopupConfigurationView.adapter = viewPagerAdapter

        if (savedInstanceState == null) {
            SharedData.clearChapters()
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

                val book = if (extras != null) (extras.getSerializable(GeneralConsts.KEYS.OBJECT.BOOK) as Book?) else null
                book?.let {
                    it.bookMark = if (extras!!.containsKey(GeneralConsts.KEYS.BOOK.PAGE_NUMBER))
                        extras.getInt(GeneralConsts.KEYS.BOOK.PAGE_NUMBER)
                    else
                        extras.getInt(GeneralConsts.KEYS.BOOK.MARK)
                }

                initialize(book)
            }
        } else
            mFragment = supportFragmentManager.findFragmentById(R.id.root_frame_book_reader) as BookReaderFragment?
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
            changePageDescription(0, "", page, pages)
            BookReaderFragment.create(mLibrary, file)
        } else
            BookReaderFragment.create()

        setFragment(fragment)
    }

    fun changeBook(book: Book) {
        setBook(book)
        setFragment(BookReaderFragment.create(mLibrary, book))
    }

    fun changePageDescription(chapter: Int, description: String, page: Int, pages: Int) {
        mToolBarBottomProgressTitle.text = if (page > 0) getString(R.string.reading_book_title_position, page, pages, Util.formatDecimal(page.toFloat() / pages * 100)) else ""
        mToolBarChapter.text = if (chapter > 0) getString(R.string.reading_book_title_chapter, chapter, description) else description
        mBackgroundProgress.progress = page
        mBackgroundProgress.max = pages
        mBackgroundTitle.text = if (description.isNotEmpty())
            getString(R.string.book_chapter, description, page, pages)
        else
            getString(R.string.progress, page, pages)

        SharedData.selectPage(page)
    }

    private fun setBook(book: Book) {
        SharedData.clearChapters()
        changePageDescription(book.chapter, book.chapterDescription, book.bookMark, book.pages)
        mBook = book
        mRepository.updateLastAccess(book)

        mToolBarTitle.text = book.name
        mToolBarBottomAuthor.text = book.author

        setDots(mutableListOf(), mutableListOf())
    }

    fun setDots(dots: MutableList<Int>, inverse: MutableList<Int>) = mToolBarBottomProgress.setDots(dots.toIntArray(), inverse.toIntArray())

    private fun dialogPageIndex() {
        if  (mDialog != null) return

        val currentFragment = supportFragmentManager.findFragmentById(R.id.root_frame_book_reader) ?: return
        val codec = (currentFragment as BookReaderFragment).mParse ?: return

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

        mDialog = MaterialAlertDialogBuilder(this, R.style.AppCompatMaterialAlertList)
            .setCustomTitle(title)
            .setItems(items) { _, selected ->
                val pageNumber = chapters[items[selected]]
                if (pageNumber != null && pageNumber.isNotEmpty())
                    currentFragment.setCurrentPage(pageNumber.toInt())
            }
            .setOnDismissListener { mDialog = null }
            .create()
        mDialog?.show()
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
            changePageDescription(book.chapter, book.chapterDescription, book.bookMark, book.pages)
        }
    }

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (mHandler.hasCallbacks(mDismissTouchView))
                mHandler.removeCallbacks(mDismissTouchView)
        } else
            mHandler.removeCallbacks(mDismissTouchView)

        super.onDestroy()
    }

    fun setFragment(fragment: Fragment) {
        mFragment = if (fragment is BookReaderFragment) fragment else null
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.root_frame_book_reader, fragment)
            .commit()
    }

    private fun optionsSave(any: Any?) {
        if (any == null)
            return

        when (any) {
            is PageMode -> mPreferences.edit()
                .putString(GeneralConsts.KEYS.READER.MANGA_PAGE_MODE, any.toString())
                .apply()
            is ReaderMode -> mPreferences.edit()
                .putString(GeneralConsts.KEYS.READER.MANGA_READER_MODE, any.toString())
                .apply()
            is Boolean -> mPreferences.edit()
                .putBoolean(GeneralConsts.KEYS.READER.MANGA_SHOW_CLOCK_AND_BATTERY, any)
                .apply()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val created = super.onCreateOptionsMenu(menu)

        MenuUtil.longClick(this, R.id.menu_item_reader_book_chapter) {
            openChapters()
        }

        return created
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.menu_item_reader_book_tts -> {}
            R.id.menu_item_reader_book_chapter -> { dialogPageIndex() }
            R.id.menu_item_reader_book_search -> {}
            R.id.menu_item_reader_book_annotation -> {}
            R.id.menu_item_reader_book_font_style -> {
                if (mMenuPopupConfiguration.visibility == View.GONE) {
                    if (!mMenuPopupBottomSheet)
                        mBottomSheetConfiguration.state = BottomSheetBehavior.STATE_EXPANDED

                    AnimationUtil.animatePopupOpen(this, mMenuPopupConfiguration, !mMenuPopupBottomSheet, navigationColor = false)
                } else
                    AnimationUtil.animatePopupClose(this, mMenuPopupConfiguration, !mMenuPopupBottomSheet, navigationColor = false)
            }
            R.id.menu_item_reader_book_mark_page -> {}
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (mMenuPopupConfiguration.visibility != View.GONE) {
            AnimationUtil.animatePopupClose(this, mMenuPopupConfiguration, !mMenuPopupBottomSheet, navigationColor = false)
            return
        }

        if (mFragment != null && !mFragment!!.onBackPressed())
            return

        super.onBackPressed()
        finish()
    }

    fun touchPosition(position: Position): Boolean {
        return when (position) {
            Position.CORNER_BOTTOM_RIGHT -> {
                mFragment?.hitEnding()
                true
            }
            Position.CORNER_BOTTOM_LEFT -> {
                mFragment?.hitBeginning()
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

    inner class ViewPagerAdapter(fm: FragmentManager, behavior: Int) :
        FragmentPagerAdapter(fm, behavior) {
        private val fragments: MutableList<Fragment> = ArrayList()
        private val fragmentTitle: MutableList<String> = ArrayList()
        fun addFragment(fragment: Fragment, title: String) {
            fragments.add(fragment)
            fragmentTitle.add(title)
        }

        override fun getItem(position: Int): Fragment {
            return fragments[position]
        }

        override fun getCount(): Int {
            return fragments.size
        }

        override fun getPageTitle(position: Int): CharSequence {
            return fragmentTitle[position]
        }
    }

    private fun openChapters() {
        if (mFragment == null)
            return

        val page = mFragment!!.getCurrentPage() - 1
        mViewModel.loadChapter(this, mFragment!!.mParse, page)

        val intent = Intent(this, MenuActivity::class.java)
        val bundle = Bundle()
        bundle.putInt(GeneralConsts.KEYS.FRAGMENT.ID, R.id.frame_chapters)
        bundle.putString(GeneralConsts.KEYS.MANGA.TITLE, mBook?.title ?: "")
        bundle.putInt(GeneralConsts.KEYS.MANGA.PAGE_NUMBER, page)
        intent.putExtras(bundle)
        overridePendingTransition(R.anim.fade_in_fragment_add_enter, R.anim.fade_out_fragment_remove_exit)
        startActivityForResult(intent, GeneralConsts.REQUEST.MANGA_CHAPTERS, null)
    }

}
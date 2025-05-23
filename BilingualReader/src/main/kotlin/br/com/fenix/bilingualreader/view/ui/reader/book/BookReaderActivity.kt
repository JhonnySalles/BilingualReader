package br.com.fenix.bilingualreader.view.ui.reader.book

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isGone
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.enums.Position
import br.com.fenix.bilingualreader.model.enums.ScrollingType
import br.com.fenix.bilingualreader.model.enums.Themes
import br.com.fenix.bilingualreader.model.enums.TouchScreen
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.japanese.Formatter
import br.com.fenix.bilingualreader.service.listener.PopupLayoutListener
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.LibraryRepository
import br.com.fenix.bilingualreader.service.repository.SharedData
import br.com.fenix.bilingualreader.service.repository.Storage
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.AnimationUtil
import br.com.fenix.bilingualreader.util.helpers.FileUtil
import br.com.fenix.bilingualreader.util.helpers.MenuUtil
import br.com.fenix.bilingualreader.util.helpers.PopupUtil.PopupUtils
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil.ThemeUtils.getColorFromAttr
import br.com.fenix.bilingualreader.util.helpers.TouchUtil.TouchUtils
import br.com.fenix.bilingualreader.util.helpers.Util
import br.com.fenix.bilingualreader.view.components.DottedSeekBar
import br.com.fenix.bilingualreader.view.ui.menu.MenuActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.sidesheet.SideSheetBehavior
import com.google.android.material.tabs.TabLayout
import org.slf4j.LoggerFactory
import java.io.File


class BookReaderActivity : AppCompatActivity(), PopupLayoutListener {

    private val mLOGGER = LoggerFactory.getLogger(BookReaderActivity::class.java)

    private val mViewModel: BookReaderViewModel by lazy { ViewModelProvider(this)[BookReaderViewModel::class.java] }

    private lateinit var mToolBarTop: MaterialToolbar
    private var mToolBarTitle : TextView? = null
    private var mToolBarChapter: TextView? = null

    private lateinit var mToolBarBottomProgressTitle: TextView
    private lateinit var mToolBarBottom: LinearLayout
    private lateinit var mToolBarBottomProgress: DottedSeekBar
    private lateinit var mToolBarBottomAuthor: TextView

    private lateinit var mBackgroundContainer: LinearLayout
    private lateinit var mBackgroundTitle: TextView
    private lateinit var mBackgroundProgress: ProgressBar
    private lateinit var mBackgroundClock: TextClock

    private var mMenuPopupConfigurationBottom: FrameLayout? = null
    private var mMenuPopupConfigurationLeft: FrameLayout? = null
    private lateinit var mPopupConfigurationTab: TabLayout
    private lateinit var mPopupConfigurationView: ViewPager

    private lateinit var mPopupReaderFont: PopupBookFont
    private lateinit var mPopupReaderLayout: PopupBookLayout
    private lateinit var mPopupReaderLanguage: PopupBookLanguage

    private var mMenuPopupBottomSheet: Boolean = true
    private lateinit var mBottomSheetConfiguration: BottomSheetBehavior<FrameLayout>
    private lateinit var mLeftSheetConfiguration: SideSheetBehavior<FrameLayout>

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
        mPreferences = GeneralConsts.getSharedPreferences(this)
        val theme = Themes.valueOf(mPreferences.getString(GeneralConsts.KEYS.THEME.THEME_USED, Themes.ORIGINAL.toString())!!)
        setTheme(theme.getValue())

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_reader)

        Formatter.initializeAsync(applicationContext)

        mRepository = BookRepository(applicationContext)

        mToolBarTop = findViewById(R.id.toolbar_book_reader)
        MenuUtil.tintToolbar(mToolBarTop, theme)
        mToolBarTitle = findViewById(R.id.reader_book_toolbar_title)
        mToolBarChapter = findViewById(R.id.reader_book_toolbar_chapter)

        mToolBarBottom = findViewById(R.id.reader_book_toolbar_bottom)
        mToolBarBottomProgressTitle = findViewById(R.id.reader_book_bottom_progress_title)
        mToolBarBottomProgress = findViewById(R.id.reader_book_bottom_progress)
        mToolBarBottomAuthor = findViewById(R.id.reader_book_toolbar_bottom_author)

        mBackgroundContainer = findViewById(R.id.container_book_progress)
        mBackgroundTitle = findViewById(R.id.book_progress_text)
        mBackgroundProgress = findViewById(R.id.book_progress_bar)
        mBackgroundClock = findViewById(R.id.book_progress_clock)
        mTouchView = findViewById(R.id.reader_book_container_touch_demonstration)

        setSupportActionBar(mToolBarTop)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        mMenuPopupBottomSheet = findViewById<ImageView>(R.id.popup_book_configuration_center_button) != null
        mMenuPopupConfigurationBottom = findViewById(R.id.popup_book_configuration_bottom_sheet)
        mMenuPopupConfigurationLeft = findViewById(R.id.popup_book_configuration_side_sheet)
        mPopupConfigurationTab = findViewById(R.id.popup_book_configuration_tab)
        mPopupConfigurationView = findViewById(R.id.popup_book_configuration_view_pager)

        if (mToolBarTitle != null) {
            mToolBarTitle!!.setOnClickListener { }
            mToolBarTitle!!.setOnLongClickListener {
                mBook?.let { FileUtil(this).copyName(it) }
                true
            }
        } else {
            mToolBarTop.setOnClickListener { }
            mToolBarTop.setOnLongClickListener {
                mBook?.let { FileUtil(this).copyName(it) }
                true
            }
        }

        if (!mMenuPopupBottomSheet) {
            mLeftSheetConfiguration = SideSheetBehavior.from(mMenuPopupConfigurationLeft!!)
            findViewById<ImageView>(R.id.popup_book_configuration_close_button)?.setOnClickListener {
                AnimationUtil.animatePopupClose(this, mMenuPopupConfigurationLeft!!, isVertical = false, navigationColor = false)
            }
            mPopupConfigurationTab.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    mLeftSheetConfiguration.isDraggable = mPopupConfigurationTab.selectedTabPosition <= 0
                }
                override fun onTabUnselected(tab: TabLayout.Tab?) { }
                override fun onTabReselected(tab: TabLayout.Tab?) { }
            })
        } else {
            mBottomSheetConfiguration = BottomSheetBehavior.from(mMenuPopupConfigurationBottom!!).apply {
                peekHeight = 195
                this.state = BottomSheetBehavior.STATE_COLLAPSED
                mBottomSheetConfiguration = this
            }
            mBottomSheetConfiguration.isDraggable = false
            PopupUtils.onPopupTouch(this, mMenuPopupConfigurationBottom!!, mBottomSheetConfiguration, findViewById<ImageView>(R.id.popup_book_configuration_center_button), navigationColor = false)
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

        mPopupReaderLayout.setListener(this)
        mPopupConfigurationView.adapter = viewPagerAdapter

        mTouchView.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (mHandler.hasCallbacks(mDismissTouchView))
                    mHandler.removeCallbacks(mDismissTouchView)
            } else {
                mHandler.removeCallbacks(mDismissTouchView)
            }

            closeViewTouch()
        }

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
        val title = if (chapter > 0) getString(R.string.reading_book_title_chapter, chapter, description) else description

        if (mToolBarChapter != null) {
            mToolBarChapter!!.text = title
            mToolBarTop.title = ""
            mToolBarTop.subtitle = ""
        } else {
            mToolBarTop.title = mBook?.name ?: ""
            mToolBarTop.subtitle = title
        }

        mBackgroundProgress.progress = page
        mBackgroundProgress.max = pages

        if (description.isNotEmpty()) {
            mBackgroundTitle.gravity = Gravity.START
            mBackgroundTitle.text = getString(R.string.book_chapter, page, pages, description)
        } else {
            mBackgroundTitle.gravity = Gravity.CENTER
            mBackgroundTitle.text = getString(R.string.progress, page, pages)
        }

        SharedData.selectPage(page)
    }

    fun updateSeekBar(type: ScrollingType) {
        val background: Int = if (type == ScrollingType.PaginationRightToLeft) R.attr.colorPrimaryContainer else R.attr.colorOnPrimaryContainer
        val tint: Int = if (type == ScrollingType.PaginationRightToLeft) R.attr.colorOnPrimaryContainer else R.attr.colorPrimaryContainer
        mBackgroundProgress.progressTintList = ColorStateList.valueOf(getColorFromAttr(tint))
        mBackgroundProgress.progressBackgroundTintList = ColorStateList.valueOf(getColorFromAttr(background))
    }

    private fun setBook(book: Book) {
        mViewModel.stopLoadChapters = true
        SharedData.clearChapters()
        changePageDescription(book.chapter, book.chapterDescription, book.bookMark, book.pages)
        mBook = book
        mRepository.updateLastAccess(book)

        if (mToolBarTitle != null) {
            mToolBarTitle!!.text = book.name
            mToolBarTop.title = ""
        } else
            mToolBarTop.title = book.name

        mToolBarBottomAuthor.text = book.author

        setBookDots(mutableListOf(), mutableListOf())
    }

    fun setBookDots(dots: MutableList<Int>, inverse: MutableList<Int>) = mToolBarBottomProgress.setPrimaryDots(dots.toIntArray(), inverse.toIntArray())

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
        name.text = if (mToolBarTitle != null) mToolBarTitle!!.text else mToolBarTop.title
        name.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.title_index_dialog_size))
        name.setTextColor(getColorFromAttr(R.attr.colorOnBackground))
        title.addView(name)
        val index = TextView(this)
        index.text = resources.getString(R.string.reading_book_page_index)
        index.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.title_small_index_dialog_size))
        index.setTextColor(getColorFromAttr(R.attr.colorPrimary))
        title.addView(index)
        title.setOnLongClickListener {
            val title = if (mToolBarTitle != null) mToolBarTitle!!.text else mToolBarTop.title
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied Text", title)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, getString(R.string.action_copy, title), Toast.LENGTH_LONG).show()

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
            savedInstanceState.putSerializable(GeneralConsts.KEYS.OBJECT.BOOK, mBook)

        super.onSaveInstanceState(savedInstanceState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        val book = (savedInstanceState.getSerializable(GeneralConsts.KEYS.OBJECT.BOOK) as Book?)
        if (book != null) {
            setBook(book)
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
                val layout = if (mMenuPopupBottomSheet) mMenuPopupConfigurationBottom else mMenuPopupConfigurationLeft
                if (layout!!.isGone) {
                    if (mMenuPopupBottomSheet)
                        mBottomSheetConfiguration.state = BottomSheetBehavior.STATE_EXPANDED
                    else
                        mLeftSheetConfiguration.state = SideSheetBehavior.STATE_EXPANDED
                    AnimationUtil.animatePopupOpen(this, layout, mMenuPopupBottomSheet, navigationColor = false)
                } else
                    AnimationUtil.animatePopupClose(this, layout, mMenuPopupBottomSheet, navigationColor = false)
            }
            R.id.menu_item_reader_book_mark_page -> {}
            R.id.menu_item_reader_book_view_touch_screen -> openTouchFunctions()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        val layout = if (mMenuPopupBottomSheet) mMenuPopupConfigurationBottom else mMenuPopupConfigurationLeft
        if (layout!!.visibility != View.GONE) {
            AnimationUtil.animatePopupClose(this, layout, mMenuPopupBottomSheet, navigationColor = false)
            return
        }

        if (mFragment != null && !mFragment!!.onBackPressed())
            return

        super.onBackPressed()
        finish()
    }

    fun touchPosition(touchScreen: TouchScreen): Boolean {
        return when (touchScreen) {
            TouchScreen.TOUCH_NEXT_FILE -> {
                mFragment?.hitEnding()
                true
            }
            TouchScreen.TOUCH_PREVIOUS_FILE -> {
                mFragment?.hitBeginning()
                true
            }

            TouchScreen.TOUCH_PAGE_MARK -> {
                mFragment?.markCurrentPage()
                true
            }

            TouchScreen.TOUCH_CHAPTER_LIST -> {
                dialogPageIndex()
                true
            }

            else -> false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            GeneralConsts.REQUEST.CHAPTERS -> {
                if (data?.extras != null && data.extras!!.containsKey(GeneralConsts.KEYS.CHAPTERS.PAGE)) {
                    val page = data.extras!!.getInt(GeneralConsts.KEYS.CHAPTERS.PAGE)
                    mFragment?.setCurrentPage(page)
                }
                mFragment?.setFullscreen(true)
            }
        }
    }


    override fun configTouchFunctions() {
        val layout = if (mMenuPopupBottomSheet) mMenuPopupConfigurationBottom else mMenuPopupConfigurationLeft
        if (layout!!.visibility != View.GONE)
            AnimationUtil.animatePopupClose(this, layout, mMenuPopupBottomSheet, navigationColor = false)

        mFragment?.configTouchFunctions()
    }

    override fun openTouchFunctions() {
        val layout = if (mMenuPopupBottomSheet) mMenuPopupConfigurationBottom else mMenuPopupConfigurationLeft
        if (layout!!.visibility != View.GONE)
            AnimationUtil.animatePopupClose(this, layout, mMenuPopupBottomSheet, navigationColor = false)

        mFragment?.setFullscreen(true)

        val touch = TouchUtils.getTouch(this, Type.BOOK)

        val touchTop = findViewById<TextView>(R.id.reader_book_touch_top)
        val touchTopRight = findViewById<TextView>(R.id.reader_book_touch_top_right)
        val touchTopLeft = findViewById<TextView>(R.id.reader_book_touch_top_left)
        val touchLeft = findViewById<TextView>(R.id.reader_book_touch_left)
        val touchRight = findViewById<TextView>(R.id.reader_book_touch_right)
        val touchBottom = findViewById<TextView>(R.id.reader_book_touch_bottom)
        val touchBottomLeft = findViewById<TextView>(R.id.reader_book_touch_bottom_left)
        val touchBottomRight = findViewById<TextView>(R.id.reader_book_touch_bottom_right)

        touchTop.text = getString(touch[Position.TOP]!!.getValue())
        touchTopRight.text = getString(touch[Position.CORNER_TOP_RIGHT]!!.getValue())
        touchTopLeft.text = getString(touch[Position.CORNER_TOP_LEFT]!!.getValue())
        touchLeft.text = getString(touch[Position.LEFT]!!.getValue())
        touchRight.text = getString(touch[Position.RIGHT]!!.getValue())
        touchBottom.text = getString(touch[Position.BOTTOM]!!.getValue())
        touchBottomLeft.text = getString(touch[Position.CORNER_BOTTOM_LEFT]!!.getValue())
        touchBottomRight.text = getString(touch[Position.CORNER_BOTTOM_RIGHT]!!.getValue())

        if ((touch[Position.CORNER_TOP_RIGHT] == TouchScreen.TOUCH_NOT_ASSIGNED && touch[Position.CORNER_TOP_LEFT] == TouchScreen.TOUCH_NOT_ASSIGNED) ||
            (touch[Position.CORNER_TOP_RIGHT] == touch[Position.RIGHT] && touch[Position.CORNER_TOP_LEFT] == touch[Position.LEFT])) {
            touchTopRight.visibility = View.GONE
            touchTopLeft.visibility = View.GONE
            touchTop.setBackgroundColor(getColorFromAttr(R.attr.colorPrimaryContainer))
        } else {
            touchTopRight.visibility = View.VISIBLE
            touchTopLeft.visibility = View.VISIBLE
            touchTop.setBackgroundColor(getColor(R.color.touch_demonstration_alter))
        }

        if ((touch[Position.CORNER_BOTTOM_RIGHT] == TouchScreen.TOUCH_NOT_ASSIGNED && touch[Position.CORNER_BOTTOM_LEFT] == TouchScreen.TOUCH_NOT_ASSIGNED) ||
            (touch[Position.CORNER_BOTTOM_RIGHT] == touch[Position.RIGHT] && touch[Position.CORNER_BOTTOM_LEFT] == touch[Position.LEFT])) {
        touchBottomRight.visibility = View.GONE
            touchBottomLeft.visibility = View.GONE
            touchBottom.setBackgroundColor(getColorFromAttr(R.attr.colorPrimaryContainer))
        } else {
            touchBottomRight.visibility = View.VISIBLE
            touchBottomLeft.visibility = View.VISIBLE
            touchBottom.setBackgroundColor(getColor(R.color.touch_demonstration_alter))
        }

        mTouchView.alpha = 0.0f
        mTouchView.visibility = View.VISIBLE
        mTouchView.animate().alpha(1.0f).setDuration(300L)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    mTouchView.alpha = 1f
                    mTouchView.visibility = View.VISIBLE
                }
            })

        mHandler.postDelayed(mDismissTouchView, 5000)
    }

    private fun closeViewTouch() {
        if (mTouchView.isGone)
            return

        mTouchView.alpha = 1.0f
        mTouchView.animate().alpha(0.0f).setDuration(300L)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    mTouchView.visibility = View.GONE
                    mTouchView.alpha = 1f
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
        bundle.putString(GeneralConsts.KEYS.CHAPTERS.TITLE, mBook?.title ?: "")
        bundle.putInt(GeneralConsts.KEYS.CHAPTERS.PAGE, page)
        intent.putExtras(bundle)
        overridePendingTransition(R.anim.fade_in_fragment_add_enter, R.anim.fade_out_fragment_remove_exit)
        startActivityForResult(intent, GeneralConsts.REQUEST.CHAPTERS, null)
    }

}
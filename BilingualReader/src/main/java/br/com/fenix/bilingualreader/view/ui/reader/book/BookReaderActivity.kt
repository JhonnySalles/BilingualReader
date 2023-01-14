package br.com.fenix.bilingualreader.view.ui.reader.book

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.*
import android.graphics.Bitmap
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.*
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.*
import br.com.fenix.bilingualreader.service.ocr.OcrProcess
import br.com.fenix.bilingualreader.service.repository.*
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.Util
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil.ThemeUtils.getColorFromAttr
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import org.slf4j.LoggerFactory
import java.io.File


class BookReaderActivity : AppCompatActivity(), OcrProcess {

    private val mLOGGER = LoggerFactory.getLogger(BookReaderActivity::class.java)

    private val mViewModel: BookReaderViewModel by viewModels()

    private lateinit var mReaderTitle: TextView
    private lateinit var mReaderProgress: SeekBar
    private lateinit var mNavReader: LinearLayout
    private lateinit var mToolBar: Toolbar
    private lateinit var mToolbarTitle: TextView
    private lateinit var mToolbarSubTitle: TextView
    private lateinit var mToolbarTitleContent: LinearLayout
    private lateinit var mSubToolbar: LinearLayout
    private lateinit var mLanguageOcrDescription: TextView
    private lateinit var mMenuPopupTranslate: FrameLayout
    private lateinit var mPopupTranslateView: ViewPager
    private lateinit var mMenuPopupColor: FrameLayout
    private lateinit var mPopupColorView: ViewPager
    private lateinit var mPopupColorTab: TabLayout
    private lateinit var mBottomSheetTranslate: BottomSheetBehavior<FrameLayout>
    private lateinit var mBottomSheetColor: BottomSheetBehavior<FrameLayout>

    private lateinit var mClockAndBattery: LinearLayout
    private lateinit var mBattery: TextView
    private lateinit var mTouchView: ConstraintLayout

    private var mHandler = Handler(Looper.getMainLooper())
    private val mMonitoringBattery = Runnable {  }
    private val mDismissTouchView = Runnable { closeViewTouch() }

    private lateinit var mPreferences: SharedPreferences
    private lateinit var mStorage: Storage
    private lateinit var mRepository: BookRepository
    private var mFragment: Fragment? = null
    private var mBook: Book? = null

    companion object {
        private lateinit var mPopupTranslateTab: TabLayout
        fun selectTabReader() =
            mPopupTranslateTab.selectTab(mPopupTranslateTab.getTabAt(0), true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val theme = Themes.valueOf(
            GeneralConsts.getSharedPreferences(this).getString(GeneralConsts.KEYS.THEME.THEME_USED, Themes.ORIGINAL.toString())!!
        )
        setTheme(theme.getValue())

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_reader)

    }

    private fun initialize(manga: Manga?) {
        /*val fragment: MangaReaderFragment = if (manga != null && manga.file.exists()) {
            setManga(manga)
            MangaReaderFragment.create(mLibrary, manga)
        } else
            MangaReaderFragment.create()

        val fileLink: PagesLinkViewModel by viewModels()
        mSubtitleController.setFileLink(fileLink.getFileLink(manga))
        setFragment(fragment)*/
    }

    private fun initialize(file: File?, page: Int) {
        /*val fragment: MangaReaderFragment = if (file != null) {
            changePage(file.name, "", page)
            MangaReaderFragment.create(mLibrary, file)
        } else
            MangaReaderFragment.create()

        mSubtitleController.setFileLink(null)
        setFragment(fragment)*/
    }

    private fun switchManga(isNext: Boolean = true) {
        if (mBook == null) return

        val changeBook = if (isNext)
            mStorage.getNextBook(mBook!!)
        else
            mStorage.getPrevBook(mBook!!)

        if (changeBook == null) {
            val content = if (isNext) R.string.switch_next_comic_last_comic else R.string.switch_prev_comic_first_comic
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

        //****setFragment(Fragment.create(mLibrary, manga))
    }

    fun setLanguage(language: Languages) {
        mLanguageOcrDescription.text = getString(R.string.languages_description, Util.languageToString(this, language))
        mSubToolbar.visibility = View.VISIBLE
    }

    private fun changeShowBatteryClock(enabled: Boolean) {
        mClockAndBattery.visibility = if (enabled)
            View.VISIBLE
        else
            View.GONE
    }

    fun changePage(title: String, text: String, page: Int) {
        mReaderTitle.text = if (page > -1) "$page/${mBook?.pages ?: ""}" else ""
        mToolbarTitle.text = title
        mToolbarSubTitle.text = text
    }

    private fun setBook(book: Book) {
        changePage(book.title, "", book.bookMark)
        mBook = book
        mRepository.updateLastAccess(book)
    }

    private fun dialogPageIndex() {
        /*val currentFragment = supportFragmentManager.findFragmentById(R.id.root_frame_reader) ?: return
        val parse = (currentFragment as MangaReaderFragment).mParse ?: return

        val paths = parse.getPagePaths()

        if (paths.isEmpty()) {
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

        val items = paths.keys.toTypedArray()

        val title = LinearLayout(this)
        title.orientation = LinearLayout.VERTICAL
        title.setPadding(resources.getDimensionPixelOffset(R.dimen.page_link_page_index_title_padding))
        val name = TextView(this)
        name.text = mToolbarTitle.text
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
            val clip = ClipData.newPlainText("Copied Text", mToolbarTitle.text)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(
                this,
                getString(R.string.action_copy, mToolbarTitle.text),
                Toast.LENGTH_LONG
            ).show()

            true
        }

        MaterialAlertDialogBuilder(this, R.style.AppCompatMaterialAlertList)
            .setCustomTitle(title)
            .setItems(items) { _, selected ->
                val pageNumber = paths[items[selected]]
                if (pageNumber != null)
                    currentFragment.setCurrentPage(pageNumber + 1)
            }
            .show()*/
    }

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
            changePage(book.title, "", book.bookMark)
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
        //mFragment = if (fragment is MangaReaderFragment) fragment else null
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.root_frame_reader, fragment)
            .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (mMenuPopupTranslate.visibility != View.GONE || mMenuPopupColor.visibility != View.GONE) {
            mMenuPopupTranslate.visibility = View.GONE
            mMenuPopupColor.visibility = View.GONE
            return
        }

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
        //mFragment?.setFullscreen(true)

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

}
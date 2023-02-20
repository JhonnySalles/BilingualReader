package br.com.fenix.bilingualreader.view.ui.reader.book

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.*
import android.util.Base64
import android.view.*
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import br.com.ebook.foobnix.sys.ImageExtractor
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.enums.Position
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.Storage
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.constants.ReaderConsts
import br.com.fenix.bilingualreader.util.helpers.FontUtil
import br.com.fenix.bilingualreader.util.helpers.LibraryUtil
import br.com.fenix.bilingualreader.view.components.book.WebViewPage
import br.com.fenix.bilingualreader.view.components.book.WebViewPager
import br.com.fenix.bilingualreader.view.components.manga.ImageViewPage
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import org.ebookdroid.core.codec.CodecDocument
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.math.roundToInt


class BookReaderFragment : Fragment(), View.OnTouchListener {

    private val mLOGGER = LoggerFactory.getLogger(BookReaderFragment::class.java)

    private val mViewModel: BookReaderViewModel by activityViewModels()

    private lateinit var mRoot: CoordinatorLayout
    private lateinit var mToolbarTop: AppBarLayout
    private lateinit var mToolbarBottom: LinearLayout
    private lateinit var mViewPager: WebViewPager
    private lateinit var mPagerAdapter: BookPagerAdapter

    private lateinit var mPageSlider: Slider
    private lateinit var mGestureDetector: GestureDetector

    private var mIsFullscreen = false
    private var mFileName: String? = null

    private lateinit var mLibrary: Library
    private var mBook: Book? = null
    private lateinit var mStorage: Storage

    private var mIsLeftToRight = false
    private val mWidth : Int = Resources.getSystem().displayMetrics.widthPixels
    private val mHeight: Int = Resources.getSystem().displayMetrics.heightPixels

    var mCodecDocument: CodecDocument? = null

    companion object {
        var mCurrentPage = 0
        fun create(): BookReaderFragment {
            val fragment = BookReaderFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }

        fun create(library: Library, path: File): BookReaderFragment {
            val fragment = BookReaderFragment()
            val args = Bundle()
            args.putSerializable(GeneralConsts.KEYS.OBJECT.LIBRARY, library)
            args.putSerializable(GeneralConsts.KEYS.OBJECT.FILE, path)
            fragment.arguments = args
            return fragment
        }

        fun create(library: Library, book: Book): BookReaderFragment {
            val fragment = BookReaderFragment()
            val args = Bundle()
            args.putSerializable(GeneralConsts.KEYS.OBJECT.LIBRARY, library)
            args.putSerializable(GeneralConsts.KEYS.OBJECT.BOOK, book)
            fragment.arguments = args
            return fragment
        }
    }

    private var mCurrentFragment: FrameLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mCurrentPage = 0
        mStorage = Storage(requireContext())
        mLibrary = LibraryUtil.getDefault(requireContext(), Type.MANGA)

        val bundle: Bundle? = arguments
        if (bundle != null && !bundle.isEmpty) {
            mLibrary = bundle.getSerializable(GeneralConsts.KEYS.OBJECT.LIBRARY) as Library

            mBook = bundle.getSerializable(GeneralConsts.KEYS.OBJECT.BOOK) as Book?
            val file: File? = if (mBook != null) {
                mBook?.file
                if (mBook?.file != null)
                    mBook?.file
                else
                    File(mBook?.path!!)
            } else
                bundle.getSerializable(GeneralConsts.KEYS.OBJECT.FILE) as File?

            if (file != null && file.exists()) {
                if (mBook == null)
                    mBook = mStorage.findBookByName(file.name)

                val metrics = Resources.getSystem().displayMetrics
                mCodecDocument = ImageExtractor.getNewCodecContext(
                    file.path,
                    "",
                    metrics.widthPixels,
                    metrics.heightPixels,
                    FontUtil.pixelToDips(requireContext(), mViewModel.fontSize.value!!)
                )

                if (mBook != null && mCodecDocument != null) {
                    mFileName = file.name
                    mCurrentPage = mBook!!.bookMark
                    mBook!!.pages = mCodecDocument!!.getPageCount(mWidth, mHeight, FontUtil.pixelToDips(requireContext(), mViewModel.fontSize.value!!))
                    mStorage.updateLastAccess(mBook!!)
                }
            } else {
                mLOGGER.info("File not founded.")
                MaterialAlertDialogBuilder(requireActivity(), R.style.AppCompatAlertDialogStyle)
                    .setTitle(getString(R.string.manga_excluded))
                    .setMessage(getString(R.string.file_not_found))
                    .setPositiveButton(
                        R.string.action_neutral
                    ) { _, _ -> }
                    .create()
                    .show()
            }
        }

        setHasOptionsMenu(true)
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_book_reader, container, false)

        mRoot = requireActivity().findViewById(R.id.root_activity_book_reader)
        mViewPager = view.findViewById<View>(R.id.fragment_book_reader) as WebViewPager
        mPageSlider = requireActivity().findViewById(R.id.reader_book_bottom_progress)

        mToolbarTop = requireActivity().findViewById(R.id.reader_book_toolbar_top)
        mToolbarBottom = requireActivity().findViewById(R.id.reader_book_toolbar_bottom)

        mPageSlider.valueTo = mCodecDocument?.getPageCount(mWidth, mHeight, FontUtil.pixelToDips(requireContext(), mViewModel.fontSize.value!!)) ?.toFloat() ?: 2f
        mPageSlider.valueFrom = 1F
        mPageSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser)
                if (mIsLeftToRight) setCurrentPage(value.roundToInt())
                else
                    setCurrentPage((mPageSlider.valueTo - value).roundToInt())

        }

        mPagerAdapter = BookPagerAdapter()
        mGestureDetector = GestureDetector(requireActivity(), MyTouchListener())

        mViewPager.isSaveEnabled = false
        mViewPager.isSaveFromParentEnabled = false

        mViewPager.adapter = mPagerAdapter
        mViewPager.offscreenPageLimit = ReaderConsts.READER.OFF_SCREEN_PAGE_LIMIT
        mViewPager.setOnTouchListener(this)
        mViewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                if (mIsLeftToRight)
                    setCurrentPage(position + 1)
                else
                    setCurrentPage(mViewPager.adapter!!.count - position)
            }
        })
        mViewPager.setOnSwipeOutListener(object : WebViewPager.OnSwipeOutListener {
            override fun onSwipeOutAtStart() {
                if (mIsLeftToRight) hitBeginning() else hitEnding()
            }

            override fun onSwipeOutAtEnd() {
                if (mIsLeftToRight) hitEnding() else hitBeginning()
            }
        })

        requireActivity().title = mFileName

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_reader_book, menu)
    }

    override fun onPause() {
        if (mBook != null) {
            mBook?.bookMark = getCurrentPage()
            mStorage.updateBookMark(mBook!!)
        }
        super.onPause()
    }


    override fun onTouch(v: View, event: MotionEvent): Boolean {
        v.performClick()
        return mGestureDetector.onTouchEvent(event)
    }

    fun getCurrentPage(): Int {
        return when {
            mIsLeftToRight -> if (::mViewPager.isInitialized) mViewPager.currentItem.plus(1) else 1
            ::mViewPager.isInitialized && mViewPager.adapter != null -> (mViewPager.adapter!!.count - mViewPager.currentItem)
            else -> 1
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return super.onOptionsItemSelected(item)
    }

    private fun getPosition(e: MotionEvent): Position {
        val isLandscape =
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        val horizontalSize =
            resources.getDimensionPixelSize(R.dimen.reader_touch_demonstration_initial_horizontal)
        val horizontal =
            (if (isLandscape) horizontalSize * 1.2 else horizontalSize * 1.5).toFloat()

        val x = e.x
        val y = e.y
        val divider = if (isLandscape) 5 else 3

        val height = Resources.getSystem().displayMetrics.heightPixels
        val width = Resources.getSystem().displayMetrics.widthPixels

        if (x < width / divider) {
            return if (y <= horizontal)
                Position.CORNER_TOP_LEFT
            else if (y >= (height - horizontal))
                Position.CORNER_BOTTOM_LEFT
            else
                Position.LEFT
        } else if (x > width / divider * (divider - 1)) {
            return if (y <= horizontal)
                Position.CORNER_TOP_RIGHT
            else if (y >= (height - horizontal))
                Position.CORNER_BOTTOM_RIGHT
            else
                Position.RIGHT
        } else {
            return if (y <= horizontal)
                Position.TOP
            else if (y >= (height - horizontal))
                Position.BOTTOM
            else
                Position.CENTER
        }
    }

    private fun updatePageViews(parentView: ViewGroup, change: (ImageViewPage) -> (Unit)) {
        for (i in 0 until parentView.childCount) {
            val child = parentView.getChildAt(i)
            if (child is ViewGroup) {
                updatePageViews(child, change)
            } else if (child is ImageViewPage) {
                val view: ImageViewPage = child
                change(view)
            }
        }
    }

    private fun getActionBar(): ActionBar? {
        return (requireActivity() as AppCompatActivity).supportActionBar
    }

    private val windowInsetsController by lazy {
        WindowInsetsControllerCompat(
            requireActivity().window,
            mViewPager
        )
    }

    fun setFullscreen(fullscreen: Boolean) {
        mIsFullscreen = fullscreen
        val w: Window = requireActivity().window
        if (fullscreen) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                windowInsetsController.let {
                    it.hide(WindowInsetsCompat.Type.systemBars())
                    it.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
                WindowCompat.setDecorFitsSystemWindows(w, true)
            } else {
                getActionBar()?.hide()
                @Suppress("DEPRECATION")
                mViewPager.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN // Hide top iu
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // Hide navigator
                        or View.SYSTEM_UI_FLAG_IMMERSIVE // Force navigator hide
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY // Force top iu hide
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN // Force full screen
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE // Stable transition on fullscreen and immersive
                        )

                Handler(Looper.getMainLooper()).postDelayed({
                    w.clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                    w.addFlags(ContextCompat.getColor(requireContext(), R.color.transparent))
                }, 300)
            }

            mRoot.fitsSystemWindows = false
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                windowInsetsController.let {
                    it.show(WindowInsetsCompat.Type.systemBars())
                }
                WindowCompat.setDecorFitsSystemWindows(w, false)
            } else {
                getActionBar()?.show()
                @Suppress("DEPRECATION")
                mViewPager.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

                Handler(Looper.getMainLooper()).postDelayed({
                    w.clearFlags(ContextCompat.getColor(requireContext(), R.color.transparent))
                    w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                }, 300)
            }

            w.statusBarColor = resources.getColor(R.color.status_bar_color)
            w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            w.navigationBarColor = resources.getColor(R.color.status_bar_color)

            //mRoot.fitsSystemWindows = true
        }

        changeContentsVisibility(fullscreen)
    }

    private val duration = 200L
    private fun changeContentsVisibility(isFullScreen: Boolean) {
        val visibility = if (isFullScreen) View.GONE else View.VISIBLE
        val finalAlpha = if (isFullScreen) 0.0f else 1.0f
        val initialAlpha = if (isFullScreen) 1.0f else 0.0f
        val initialTranslation = if (isFullScreen) 0f else -50f
        val finalTranslation = if (isFullScreen) -50f else 0f

        if (!isFullScreen) {
            mToolbarBottom.visibility = visibility
            mToolbarTop.visibility = visibility
            mToolbarBottom.alpha = initialAlpha
            mToolbarTop.alpha = initialAlpha
            mToolbarTop.translationY = initialTranslation
        }

        mToolbarBottom.animate().alpha(finalAlpha).translationY(finalTranslation * -1)
            .setDuration(duration).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    mToolbarBottom.visibility = visibility
                }
            })

        mToolbarTop.animate().alpha(finalAlpha).translationY(finalTranslation)
            .setDuration(duration).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    mToolbarTop.visibility = visibility
                }
            })
    }

    fun setCurrentPage(page: Int) {
        if (mIsLeftToRight) {
            mViewPager.currentItem = page - 1
            mPageSlider.value = if (page <= 0) 1F else (page - 1).toFloat()
        } else {
            mViewPager.currentItem = mViewPager.adapter!!.count - page
            mPageSlider.value = if (page > mPageSlider.valueTo) mPageSlider.valueTo else (mViewPager.adapter!!.count - page).toFloat()
        }

        mCurrentPage = mViewPager.currentItem

        if (mCurrentPage < 0)
            mCurrentPage = 0

        if (mCodecDocument != null && mCodecDocument?.outline != null && mCodecDocument?.outline!!.isNotEmpty())
            for (link in mCodecDocument?.outline!!) {
                val number = link.link.replace(Regex("[^\\d+]"), "")
                if (number.isNotEmpty() && mCurrentPage <= number.toInt()) {
                    mBook!!.chapter = number.toInt()
                    mBook!!.chapterDescription = link.title
                    break
                }
            }

        (requireActivity() as BookReaderActivity).changePage(
            mBook!!.chapter,
            mBook!!.chapterDescription,
            mCurrentPage,
            mViewPager.adapter!!.count
        )
    }

    fun isFullscreen(): Boolean = mIsFullscreen

    fun hitBeginning() {

    }

    fun hitEnding() {

    }

    inner class MyTouchListener : GestureDetector.SimpleOnGestureListener() {

        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)

        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (!isFullscreen()) {
                setFullscreen(fullscreen = true)
                return true
            }

            val position = getPosition(e)
            if ((requireActivity() as BookReaderActivity).touchPosition(position))
                return true

            when (position) {
                Position.LEFT -> {
                    if (mIsLeftToRight) {
                        if (getCurrentPage() == 1) hitBeginning() else setCurrentPage(getCurrentPage() - 1)
                    } else {
                        if (getCurrentPage() == mViewPager.adapter!!.count
                        ) hitEnding() else setCurrentPage(getCurrentPage() + 1)
                    }
                }
                Position.RIGHT -> {
                    if (mIsLeftToRight) {
                        if (getCurrentPage() == mViewPager.adapter!!.count
                        ) hitEnding() else setCurrentPage(getCurrentPage() + 1)
                    } else {
                        if (getCurrentPage() == 1) hitBeginning() else setCurrentPage(getCurrentPage() - 1)
                    }
                }
                Position.CENTER -> setFullscreen(fullscreen = false)
                else -> setFullscreen(fullscreen = false)
            }

            return true
        }
    }

    inner class BookPagerAdapter : PagerAdapter() {
        override fun isViewFromObject(view: View, o: Any): Boolean {
            return view === o
        }

        override fun getCount(): Int {
            return mCodecDocument?.getPageCount(mWidth, mHeight, FontUtil.pixelToDips(requireContext(), mViewModel.fontSize.value!!)) ?: 1
        }

        override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
            if (mCurrentFragment !== `object`)
                mCurrentFragment = `object` as FrameLayout
            super.setPrimaryItem(container, position, `object`)
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val inflater =
                requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val layout: View = inflater.inflate(R.layout.fragment_book_page, container, false)
            val webViewPage: WebViewPage =
                layout.findViewById<View>(R.id.page_web_view) as WebViewPage

            var html = "<!DOCTYPE html><html>" + mViewModel.getDefaultCSS() + "<body>" + mCodecDocument?.getPage(position)?.pageHTMLWithImages + "</body></html>"

            if (html.contains("<image-begin>image"))
                html = html.replace("<image-begin>", "<img src=\"data:").replace("<image-end>", "\" />")

            html = html.replace("<p>" , "").replace("</p>", "").replace("<end-line>", "<br>")

            webViewPage.loadData(html, "text/html", "UTF-8")
            webViewPage.setOnTouchListener(this@BookReaderFragment)
            webViewPage.settings.javaScriptEnabled = true
            webViewPage.settings.defaultFontSize = mViewModel.mWebFontSize
            container.addView(layout)

            return layout
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            val layout = `object` as View
            container.removeView(layout)
            val iv = layout.findViewById<View>(R.id.page_web_view) as WebViewPage
            iv.destroy()
        }
    }

}
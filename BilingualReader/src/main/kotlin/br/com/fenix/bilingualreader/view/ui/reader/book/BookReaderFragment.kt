package br.com.fenix.bilingualreader.view.ui.reader.book

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnticipateOvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.transition.Slide
import androidx.transition.Transition
import androidx.transition.TransitionManager
import androidx.viewpager2.widget.ViewPager2
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.model.entity.BookSearch
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Speech
import br.com.fenix.bilingualreader.model.enums.AudioStatus
import br.com.fenix.bilingualreader.model.enums.Color
import br.com.fenix.bilingualreader.model.enums.MarkType
import br.com.fenix.bilingualreader.model.enums.Position
import br.com.fenix.bilingualreader.model.enums.ScrollingType
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.controller.BookImageCoverController
import br.com.fenix.bilingualreader.service.controller.TextToSpeechController
import br.com.fenix.bilingualreader.service.listener.BookParseListener
import br.com.fenix.bilingualreader.service.listener.TTSListener
import br.com.fenix.bilingualreader.service.listener.TextSelectCallbackListener
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.service.repository.SharedData
import br.com.fenix.bilingualreader.service.repository.Storage
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.constants.ReaderConsts
import br.com.fenix.bilingualreader.util.helpers.LibraryUtil
import br.com.fenix.bilingualreader.util.helpers.MenuUtil
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil.ThemeUtils.getColorFromAttr
import br.com.fenix.bilingualreader.view.components.AutoScroll
import br.com.fenix.bilingualreader.view.components.DottedSeekBar
import br.com.fenix.bilingualreader.view.components.book.TextViewPage
import br.com.fenix.bilingualreader.view.components.book.TextViewPager
import br.com.fenix.bilingualreader.view.components.book.WebViewPage
import br.com.fenix.bilingualreader.view.components.book.WebViewPager
import br.com.fenix.bilingualreader.view.ui.menu.MenuActivity
import br.com.fenix.bilingualreader.view.ui.popup.PopupTTS
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.LinkedList


class BookReaderFragment : Fragment(), View.OnTouchListener, BookParseListener, TTSListener, TextSelectCallbackListener {

    private val mLOGGER = LoggerFactory.getLogger(BookReaderFragment::class.java)

    private val mViewModel: BookReaderViewModel by activityViewModels()

    private lateinit var mRoot: CoordinatorLayout
    private lateinit var mToolbarTop: AppBarLayout
    private lateinit var mToolbarBottom: LinearLayout
    private lateinit var miChapter: MenuItem
    private lateinit var miAnnotation: MenuItem
    private lateinit var miFontStyle: MenuItem
    private lateinit var miMarkPage: MenuItem
    private lateinit var miSearch: MenuItem
    private lateinit var miReaderTTS: MenuItem
    private lateinit var mViewPager: ViewPager2
    private lateinit var mPagerAdapter: Adapter<RecyclerView.ViewHolder>
    private lateinit var mReaderTTSContainer: LinearLayout
    private lateinit var mReaderTTSPlay: MaterialButton
    private lateinit var mReaderTTSProgress: CircularProgressIndicator

    private lateinit var mCoverContent: ConstraintLayout
    private lateinit var mCoverImage: ImageView
    private lateinit var mCoverMessage: TextView

    private lateinit var mConfiguration: FrameLayout

    private lateinit var mPageSeekBar: DottedSeekBar
    private lateinit var mGestureDetector: GestureDetector

    private lateinit var mLastPageContainer: MaterialCardView
    private lateinit var mLastPageImage: ImageView
    private lateinit var mLastPageText: TextView

    private var mIsFullscreen = false
    private var mIsLeftToRight = true

    private var mFileName: String? = null
    private lateinit var mLibrary: Library
    private var mBook: Book? = null
    private var mNewBook: Book? = null
    private var mNewBookTitle = 0
    private lateinit var mStorage: Storage
    private var mTextToSpeech: TextToSpeechController? = null

    private val mLastPage = LinkedList<Pair<Int, Bitmap>>()
    private val mHandler = Handler(Looper.getMainLooper())

    var mParse: DocumentParse? = null

    companion object {
        private const val ANIMATION_DURATION = 200L

        var mCurrentPage = 1
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mCurrentPage = 1
        mStorage = Storage(requireContext())
        mLibrary = LibraryUtil.getDefault(requireContext(), Type.BOOK)

        val bundle: Bundle? = arguments
        if (bundle != null && !bundle.isEmpty) {
            mLibrary = bundle.getSerializable(GeneralConsts.KEYS.OBJECT.LIBRARY) as Library

            mBook = bundle.getSerializable(GeneralConsts.KEYS.OBJECT.BOOK) as Book?
            val file: File? = if (mBook != null)
                mBook?.file
            else
                bundle.getSerializable(GeneralConsts.KEYS.OBJECT.FILE) as File?

            if (file != null && file.exists()) {
                if (mBook == null)
                    mBook = mStorage.findBookByName(file.name)

                mViewModel.loadConfiguration(mBook)

                mParse = DocumentParse(
                    file.path,
                    mBook?.password ?: "",
                    mViewModel.fontSize.value!!.toInt(),
                    resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE,
                    this
                )

                if (mBook != null) {
                    mFileName = file.name
                    mCurrentPage = mBook!!.bookMark
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view: View = inflater.inflate(R.layout.fragment_book_reader, container, false)

        mRoot = requireActivity().findViewById(R.id.root_activity_book_reader)
        mViewPager = view.findViewById(R.id.fragment_book_reader)
        mPageSeekBar = requireActivity().findViewById(R.id.reader_book_bottom_progress)
        mConfiguration = requireActivity().findViewById(R.id.popup_book_configuration)

        mCoverContent = view.findViewById(R.id.reader_book_cover_content)
        mCoverImage = view.findViewById(R.id.reader_book_cover)
        mCoverMessage = view.findViewById(R.id.reader_book_cover_message)

        mToolbarTop = requireActivity().findViewById(R.id.reader_book_toolbar_top)
        mToolbarBottom = requireActivity().findViewById(R.id.reader_book_toolbar_bottom)

        mReaderTTSContainer = requireActivity().findViewById(R.id.container_book_tts)
        mReaderTTSPlay = requireActivity().findViewById(R.id.reader_book_tts_play)
        mReaderTTSProgress = requireActivity().findViewById(R.id.reader_book_tts_progress)

        mLastPageContainer = requireActivity().findViewById(R.id.reader_last_page)
        mLastPageImage = requireActivity().findViewById(R.id.last_page_image)
        mLastPageText = requireActivity().findViewById(R.id.last_page_text)

        mLastPageContainer.visibility = View.GONE

        if (mBook != null) {
            BookImageCoverController.instance.setImageCoverAsync(requireContext(), mBook!!, mCoverImage, null, true)
            BookImageCoverController.instance.setImageCoverAsync(requireContext(), mBook!!, mCoverImage, null, false)
        }

        onLoading(isFinished = false, isLoaded = false)

        mPageSeekBar.isEnabled = false
        mPageSeekBar.max = 2
        mPageSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    if (mIsLeftToRight)
                        setCurrentPage(progress + 1)
                    else
                        setCurrentPage(mPageSeekBar.max - progress + 1)
                    changeLastPagePosition(progress + 1)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                try {
                    val text = (mViewPager.adapter as TextViewPager).getHolder(mViewPager.currentItem)?.textView ?: return

                    val page = seekBar.progress + 1
                    if (mLastPage.any { it.first == page })
                        return

                    if (mLastPage.size > 3)
                        mLastPage.removeLast()

                    val bitmap = Bitmap.createBitmap(text.width, text.height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    text.draw(canvas)

                    mLastPage.addFirst(Pair(page, bitmap))
                    openLastPage()
                } catch (e: Exception) {
                    mLOGGER.error("Error to insert last page", e)
                }
            }

            override fun onStopTrackingTouch(p0: SeekBar?) { }
        })

        mLastPageContainer.setOnClickListener {
            val old = mLastPage.removeFirst()
            if (mLastPage.isEmpty())
                closeLastPage()
            else {
                changeLastPage(mLastPage.first)
                changeLastPagePosition(old.first)
            }
            setCurrentPage(old.first)
        }

        mGestureDetector = GestureDetector(requireActivity(), MyTouchListener())
        requireActivity().title = "" // Title beside to the icons
        mReaderTTSContainer.visibility = View.GONE

        mReaderTTSPlay.setOnClickListener { mTextToSpeech?.pause() }
        mReaderTTSPlay.setOnLongClickListener {
            mTextToSpeech?.stop()
            true
        }

        requireActivity().findViewById<MaterialButton>(R.id.reader_book_tts_previous).setOnClickListener { mTextToSpeech?.previous() }
        requireActivity().findViewById<MaterialButton>(R.id.reader_book_tts_next).setOnClickListener { mTextToSpeech?.next() }

        requireActivity().findViewById<MaterialButton>(R.id.reader_book_tts_config).setOnClickListener { openMenuTTS() }
        requireActivity().findViewById<MaterialButton>(R.id.reader_book_tts_close).setOnClickListener { mTextToSpeech?.stop() }

        updateSeekBar()

        if (savedInstanceState != null) {
            val fullscreen = savedInstanceState.getBoolean(ReaderConsts.STATES.STATE_FULLSCREEN)
            setFullscreen(fullscreen)
            val newBookId = savedInstanceState.getLong(ReaderConsts.STATES.STATE_NEW_BOOK)
            val titleRes = savedInstanceState.getInt(ReaderConsts.STATES.STATE_NEW_BOOK_TITLE)
            confirmSwitch(mStorage.getBook(newBookId), titleRes)
        } else
            setFullscreen(true)
        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_reader_book, menu)
        super.onCreateOptionsMenu(menu, inflater)

        miChapter = menu.findItem(R.id.menu_item_reader_book_chapter)
        miAnnotation = menu.findItem(R.id.menu_item_reader_book_annotation)
        miFontStyle = menu.findItem(R.id.menu_item_reader_book_font_style)
        miMarkPage = menu.findItem(R.id.menu_item_reader_book_mark_page)
        miSearch = menu.findItem(R.id.menu_item_reader_book_search)
        miReaderTTS = menu.findItem(R.id.menu_item_reader_book_tts)

        MenuUtil.longClick(requireActivity(), R.id.menu_item_reader_book_tts) {
            openMenuTTS()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(ReaderConsts.STATES.STATE_FULLSCREEN, isFullscreen())
        outState.putLong(ReaderConsts.STATES.STATE_NEW_BOOK, (if (mNewBook != null) mNewBook!!.id else -1)!!)
        outState.putInt(ReaderConsts.STATES.STATE_NEW_BOOK_TITLE, if (mNewBook != null) mNewBookTitle else -1)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        if (mBook != null) {
            mBook?.bookMark = getCurrentPage()
            mStorage.updateBookMark(mBook!!)
        }
        super.onPause()
    }

    override fun onDestroy() {
        mTextToSpeech?.stop()
        SharedData.setDocumentParse(null)
        removeRefreshSizeDelay()
        super.onDestroy()
    }

    override fun onLoading(isFinished: Boolean, isLoaded: Boolean) {
        if (isFinished) {
            if (isLoaded) {
                val pages = (mParse?.getPageCount(mViewModel.getFontSize(isBook = true).toInt()) ?: 2)
                mPageSeekBar.max = pages -1
                mCoverContent.animate().alpha(0.0f)
                    .setDuration(400L).setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            mCoverContent.visibility = View.GONE
                        }
                    })
                setBookDots(pages)
                if (mBook != null) {
                    mBook!!.pages = mPageSeekBar.progress + 1
                    mStorage.updateLastAccess(mBook!!)
                }
                preparePager()
            } else {
                mParse = null
                mCoverImage.setImageResource(R.mipmap.book_cover_not_found)
                mCoverMessage.text = getString(R.string.reading_book_open_exception)
            }
        } else {
            if (::mCoverContent.isInitialized) {
                mCoverContent.visibility = View.VISIBLE
                mCoverContent.alpha = 1f
                mCoverMessage.text = ""
            }
        }
    }

    override fun onSearching(isSearching: Boolean) {
        TODO("Not yet implemented")
    }

    override fun onConverting(isConverting: Boolean) {
        TODO("Not yet implemented")
    }

    @SuppressLint("ClickableViewAccessibility", "UncheckCast")
    private fun preparePager() {
        mPageSeekBar.isEnabled = true

        mPagerAdapter = if (ReaderConsts.READER.BOOK_WEB_VIEW_MODE)
            WebViewPager(requireActivity(), requireContext(), mViewModel, mParse, this@BookReaderFragment) as Adapter<RecyclerView.ViewHolder>
        else
            TextViewPager(requireContext(), mViewModel, mParse, this@BookReaderFragment, this@BookReaderFragment) as Adapter<RecyclerView.ViewHolder>

        mViewPager.adapter = mPagerAdapter

        var scrolling = mViewModel.scrollingType.value

        if (scrolling == null) {
            val preferences = GeneralConsts.getSharedPreferences(requireContext())
            scrolling = ScrollingType.valueOf(preferences.getString(GeneralConsts.KEYS.READER.BOOK_PAGE_SCROLLING_MODE, ScrollingType.Pagination.toString())!!)
            mViewModel.changeScrolling(scrolling)
        }

        mViewPager.orientation = if (scrolling == ScrollingType.Scrolling) ViewPager2.ORIENTATION_VERTICAL else ViewPager2.ORIENTATION_HORIZONTAL

        mViewPager.isSaveEnabled = false
        mViewPager.isSaveFromParentEnabled = false
        mViewPager.setOnTouchListener(this)
        mViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (mIsLeftToRight)
                    setCurrentPage(position + 1, false)
                else
                    setCurrentPage(mViewPager.adapter!!.itemCount - position, false)
            }
        })

        var startX = 0f
        mViewPager.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN)
                startX = motionEvent.x
            else if (motionEvent.action == MotionEvent.ACTION_UP) {
                val diff = motionEvent.x - startX
                if (diff > 0 && mViewPager.currentItem == 0) {
                    if (mIsLeftToRight) hitBeginning() else hitEnding()
                } else if (diff < 0 && mViewPager.currentItem == mViewPager.adapter!!.itemCount - 1) {
                    if (mIsLeftToRight) hitEnding() else hitBeginning()
                }
            }
            false
        }
        if (mBook != null)
            setCurrentPage(mCurrentPage, isAnimated = false)

        observer()
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        v.performClick()
        return mGestureDetector.onTouchEvent(event)
    }

    fun getCurrentPage(): Int {
        return when {
            mIsLeftToRight -> if (::mViewPager.isInitialized) mViewPager.currentItem.plus(1) else 1
            ::mViewPager.isInitialized && mViewPager.adapter != null -> (mViewPager.adapter!!.itemCount - mViewPager.currentItem)
            else -> 1
        }
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.menu_item_reader_book_tts -> {
                if (mTextToSpeech == null)
                    executeTTS(mViewPager.currentItem)
                else
                    mTextToSpeech?.stop()
            }

            R.id.menu_item_reader_book_chapter -> (miChapter.icon as AnimatedVectorDrawable).start()
            R.id.menu_item_reader_book_font_style -> {
                (miFontStyle.icon as AnimatedVectorDrawable).start()
                if (mTextToSpeech != null)
                    mTextToSpeech?.stop()
            }

            R.id.menu_item_reader_book_mark_page -> {
                markCurrentPage()
                (miMarkPage.icon as AnimatedVectorDrawable).start()
            }

            R.id.menu_item_reader_book_annotation -> {
                (miAnnotation.icon as AnimatedVectorDrawable).start()
                openBookAnnotation()
            }

            R.id.menu_item_reader_book_search -> {
                (miSearch.icon as AnimatedVectorDrawable).start()
                openBookSearch()
            }
        }

        return super.onOptionsItemSelected(menuItem)
    }


    private fun getPosition(e: MotionEvent): Position {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        val horizontalSize = resources.getDimensionPixelSize(R.dimen.reader_touch_demonstration_initial_horizontal)
        val horizontal = (if (isLandscape) horizontalSize * 1.2 else horizontalSize * 1.5).toFloat()

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

    private fun updatePagesViews(parentView: ViewGroup, css: String) {
        for (i in 0 until parentView.childCount) {
            when (val child = parentView.getChildAt(i)) {
                is WebViewPage -> {
                    child.loadUrl(
                        "javascript:(function() {" +
                                "var parent = document.getElementsByTagName('head').item(0);" +
                                "var style = document.createElement('style');" +
                                "style.type = 'text/css';" +
                                "style.innerHTML = window.atob('" + css + "');" +
                                "parent.removeChild(parent.firstChild);" +
                                "parent.appendChild(style);" +
                                "})()"
                    )
                }

                is TextViewPage -> mViewModel.changeTextStyle(child)
                is ViewGroup -> updatePagesViews(child, css)
            }
        }
    }

    private val mRefreshSizeDelay = Runnable {
        if (!ReaderConsts.READER.BOOK_WEB_VIEW_MODE)
            (mPagerAdapter as TextViewPager).refreshSize()
        mPagerAdapter.notifyDataSetChanged()
    }

    private fun removeRefreshSizeDelay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (mHandler.hasCallbacks(mRefreshSizeDelay))
                mHandler.removeCallbacks(mRefreshSizeDelay)
            else
                mHandler.removeCallbacks(mRefreshSizeDelay)
        }
    }

    private fun observer() {
        mViewModel.scrollingType.observe(viewLifecycleOwner) {
            mViewPager.orientation = if (it == ScrollingType.Scrolling)
                ViewPager2.ORIENTATION_VERTICAL
            else
                ViewPager2.ORIENTATION_HORIZONTAL
        }

        mViewModel.ttsVoice.observe(viewLifecycleOwner) {
            mTextToSpeech?.setVoice(mViewModel.ttsVoice.value!!)
        }

        if (ReaderConsts.READER.BOOK_WEB_VIEW_MODE)
            mViewModel.fontCss.observe(viewLifecycleOwner) {
                try {
                    val inputStream = ByteArrayInputStream(mViewModel.fontCss.value!!.toByteArray(StandardCharsets.UTF_8))
                    val buffer = ByteArray(inputStream.available())
                    inputStream.read(buffer)
                    inputStream.close()
                    val encoded = Base64.encodeToString(buffer, Base64.NO_WRAP)
                    updatePagesViews(mViewPager, encoded)
                    mPagerAdapter.notifyDataSetChanged()
                } catch (e: Exception) {
                    mLOGGER.error("Error generator css for book page: " + e.message, e)
                }
            }
        else
            mViewModel.fontUpdate.observeForever {
                removeRefreshSizeDelay()
                mHandler.postDelayed(mRefreshSizeDelay, 1000)
            }

        mViewModel.fontSize.observeForever { font ->
            if (!isAdded)
                return@observeForever

            SharedData.clearChapters()

            mParse?.let { parse ->
                val pages = parse.getPageCount((font + DocumentParse.BOOK_FONT_SIZE_DIFFER).toInt())
                mPageSeekBar.max = (if (pages > 1) pages else 2) - 1
                if (!ReaderConsts.READER.BOOK_WEB_VIEW_MODE)
                    (mPagerAdapter as TextViewPager).refreshSize()
                mPagerAdapter.notifyItemChanged(mViewPager.currentItem)

                removeRefreshSizeDelay()
                mHandler.postDelayed(mRefreshSizeDelay, 1000)

                mBook?.let { book ->
                    (requireActivity() as BookReaderActivity).changePageDescription(book.chapter, book.chapterDescription, mCurrentPage, pages)
                    book.pages = pages
                    mViewModel.update(book)
                    setBookDots(pages)
                }
            }
        }
    }

    private fun getActionBar(): ActionBar? = (requireActivity() as AppCompatActivity).supportActionBar

    private val windowInsetsController by lazy {
        WindowInsetsControllerCompat(requireActivity().window, mViewPager)
    }

    fun setFullscreen(fullscreen: Boolean) {
        if (!isAdded || context == null)
            return

        mIsFullscreen = fullscreen

        val window: Window = requireActivity().window
        if (fullscreen) {
            mRoot.fitsSystemWindows = false
            changeContentsVisibility(fullscreen)
            Handler(Looper.getMainLooper()).postDelayed({ if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    windowInsetsController.let {
                        it.hide(WindowInsetsCompat.Type.systemBars())
                        it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                    WindowCompat.setDecorFitsSystemWindows(window, true)
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
                        window.clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                        window.addFlags(ContextCompat.getColor(requireContext(), R.color.transparent))
                    }, ANIMATION_DURATION + 100)
                }

                mConfiguration.visibility = View.GONE
            }, ANIMATION_DURATION)
        } else {
            Handler(Looper.getMainLooper()).postDelayed({ changeContentsVisibility(fullscreen)  }, ANIMATION_DURATION)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                WindowCompat.setDecorFitsSystemWindows(window, false)
            } else {
                getActionBar()?.show()
                @Suppress("DEPRECATION")
                mViewPager.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

                Handler(Looper.getMainLooper()).postDelayed({
                    window.clearFlags(ContextCompat.getColor(requireContext(), R.color.transparent))
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                }, ANIMATION_DURATION + 100)
            }

            window.statusBarColor = requireContext().getColor(R.color.status_bar_color)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.navigationBarColor = requireContext().getColor(R.color.status_bar_color)
        }


        if (fullscreen)
            closeLastPage()
        else
            openLastPage()
    }

    private fun changeContentsVisibility(isFullScreen: Boolean) {
        val visibility = if (isFullScreen) View.GONE else View.VISIBLE
        val finalAlpha = if (isFullScreen) 0.0f else 1.0f
        val initialAlpha = if (isFullScreen) 1.0f else 0.0f
        val initialTranslation = if (isFullScreen) 0f else -50f
        val finalTranslation = if (isFullScreen) -50f else 0f

        if (!isFullScreen) {
            mToolbarBottom.visibility = View.VISIBLE
            mToolbarBottom.translationY = initialTranslation * -1
            mToolbarBottom.alpha = initialAlpha
            mToolbarTop.visibility = View.VISIBLE
            mToolbarTop.translationY = initialTranslation
            mToolbarTop.alpha = initialAlpha
        }

        mToolbarBottom.animate().alpha(finalAlpha).translationY(finalTranslation * -1)
            .setDuration(ANIMATION_DURATION).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    mToolbarBottom.visibility = visibility
                }
            })

        mToolbarTop.animate().alpha(finalAlpha).translationY(finalTranslation)
            .setDuration(ANIMATION_DURATION).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    mToolbarTop.visibility = visibility
                }
            })
    }

    fun setCurrentPage(page: Int, isChangePage: Boolean = true, isAnimated: Boolean = true) {
        if (isChangePage) {
            // Use animated to load because wrong page is set started
            if (mIsLeftToRight)
                mViewPager.setCurrentItem(page - 1, isAnimated)
            else
                mViewPager.setCurrentItem(mViewPager.adapter!!.itemCount - page, isAnimated)
        }

        mCurrentPage = mViewPager.currentItem

        if (mCurrentPage < 0)
            mCurrentPage = 0

        if (mParse != null)
            mParse!!.getChapter(mCurrentPage)?.let {
                mBook!!.chapter = it.first
                mBook!!.chapterDescription = it.second
            }

        mCurrentPage = mViewPager.currentItem + 1
        mPageSeekBar.progress = mCurrentPage-1

        (requireActivity() as BookReaderActivity).changePageDescription(mBook!!.chapter, mBook!!.chapterDescription, mCurrentPage, mViewPager.adapter!!.itemCount)
    }

    fun isFullscreen(): Boolean = mIsFullscreen

    fun hitBeginning() {
        if (mBook != null) {
            val c: Book? = mStorage.getPrevBook(mLibrary, mBook!!)
            confirmSwitch(c, R.string.switch_prev_book)
        }
    }

    fun hitEnding() {
        if (mBook != null) {
            val c: Book? = mStorage.getNextBook(mLibrary, mBook!!)
            confirmSwitch(c, R.string.switch_next_book)
        }
    }

    private fun confirmSwitch(newBook: Book?, titleRes: Int) {
        if (newBook == null) return
        var confirm = false
        mNewBook = newBook
        mNewBookTitle = titleRes
        val dialog: AlertDialog =
            MaterialAlertDialogBuilder(requireActivity(), R.style.AppCompatAlertDialogStyle)
                .setTitle(titleRes)
                .setMessage(newBook.fileName)
                .setPositiveButton(
                    R.string.switch_action_positive
                ) { _, _ ->
                    confirm = true
                    val activity = requireActivity() as BookReaderActivity
                    activity.changeBook(mNewBook!!)
                }
                .setNegativeButton(
                    R.string.switch_action_negative
                ) { _, _ -> }
                .setOnDismissListener {
                    if (!confirm) {
                        mNewBook = null
                        setFullscreen(fullscreen = true)
                    }
                }
                .create()
        dialog.show()
    }

    private fun openBookAnnotation() {
        if (mTextToSpeech != null)
            mTextToSpeech?.stop()

        val intent = Intent(requireContext(), MenuActivity::class.java)
        val bundle = Bundle()
        bundle.putInt(GeneralConsts.KEYS.FRAGMENT.ID, R.id.frame_book_annotation)
        bundle.putSerializable(GeneralConsts.KEYS.OBJECT.BOOK, mBook!!)
        intent.putExtras(bundle)
        requireActivity().overridePendingTransition(R.anim.fade_in_fragment_add_enter, R.anim.fade_out_fragment_remove_exit)
        startActivityForResult(intent, GeneralConsts.REQUEST.BOOK_ANNOTATION, null)
    }

    private fun openBookSearch(search: BookSearch? = null) {
        if (mParse == null)
            return

        if (mTextToSpeech != null)
            mTextToSpeech?.stop()

        SharedData.setDocumentParse(mParse)

        val intent = Intent(requireContext(), MenuActivity::class.java)

        val bundle = Bundle()
        bundle.putInt(GeneralConsts.KEYS.FRAGMENT.ID, R.id.frame_book_search)
        bundle.putSerializable(GeneralConsts.KEYS.OBJECT.BOOK, mBook)
        bundle.putString(GeneralConsts.KEYS.OBJECT.DOCUMENT_PATH, mParse!!.path)
        bundle.putString(GeneralConsts.KEYS.OBJECT.DOCUMENT_PASSWORD, mParse!!.password)
        bundle.putInt(GeneralConsts.KEYS.OBJECT.DOCUMENT_FONT_SIZE, mParse!!.fontSize)

        if (search != null)
            bundle.putSerializable(GeneralConsts.KEYS.OBJECT.BOOK_SEARCH, search)

        intent.putExtras(bundle)
        requireActivity().overridePendingTransition(R.anim.fade_in_fragment_add_enter, R.anim.fade_out_fragment_remove_exit)
        startActivityForResult(intent, GeneralConsts.REQUEST.BOOK_SEARCH, null)
    }

    private fun executeTTS(page: Int, initial: String = "") {
        if (mTextToSpeech == null) {
            setFullscreen(fullscreen = true)
            mTextToSpeech = TextToSpeechController(requireContext(), mBook!!, mParse, (mCoverImage.drawable as BitmapDrawable).bitmap, mViewModel.getFontSize(true).toInt())
            mTextToSpeech!!.addListener(this)
            mTextToSpeech!!.addListener(mPagerAdapter as TTSListener)
            mTextToSpeech!!.setVoice(mViewModel.ttsVoice.value!!)
            mTextToSpeech!!.start(page, initial)
        } else
            mTextToSpeech?.find(page, initial)
    }

    override fun statusTTS(status: AudioStatus) {
        when (status) {
            AudioStatus.PREPARE, AudioStatus.ENDING -> {
                val visibility = if (status == AudioStatus.ENDING) View.GONE else View.VISIBLE

                val transition = Slide(Gravity.TOP)
                transition.setDuration(800L)
                transition.addTarget(mReaderTTSContainer)
                transition.interpolator = AccelerateDecelerateInterpolator()

                TransitionManager.beginDelayedTransition(mRoot, transition)
                mReaderTTSContainer.visibility = visibility
            }

            AudioStatus.PLAY -> {
                if (mReaderTTSProgress.isIndeterminate) {
                    mReaderTTSProgress.isIndeterminate = false
                    mReaderTTSProgress.progress = mCurrentPage
                    mReaderTTSProgress.max = mViewPager.adapter!!.itemCount

                    mViewModel.history?.let {
                        it.useTTS = true
                    }
                }

                mReaderTTSPlay.setIconResource(R.drawable.ic_tts_pause)
            }

            AudioStatus.PAUSE -> mReaderTTSPlay.setIconResource(R.drawable.ic_tts_play)
            AudioStatus.STOP -> mReaderTTSPlay.setIconResource(R.drawable.ic_tts_close)
        }
    }

    override fun readingLine(speech: Speech) {}

    override fun changePageTTS(old: Int, new: Int) {
        mReaderTTSProgress.progress = new
        setCurrentPage(new + 1, isAnimated = true)
    }

    override fun stopTTS() {
        mTextToSpeech = null
    }

    private fun openMenuTTS() {
        val old = mViewModel.ttsVoice.value
        PopupTTS(requireContext()).getPopupTTS(old!!) {
            if (old != it) {
                mViewModel.changeTTSVoice(it)
                mTextToSpeech?.setVoice(it)
            }
        }
    }

    override fun textSelectReadingFrom(page: Int, text: String) = executeTTS(page, text)

    override fun textSelectAddMark(page: Int, text: String, color: Color, start: Int, end: Int): BookAnnotation {
        val chapter = mParse!!.getChapter(page) ?: Pair(0, "")
        val annotation = BookAnnotation(
            mBook!!.id!!, page, mParse!!.pageCount, MarkType.Annotation, chapter.first.toFloat(), chapter.second, text,
            intArrayOf(start, end), "", color = color
        )
        mViewModel.save(annotation)
        return annotation
    }

    override fun textSelectRemoveMark(page: Int, start: Int, end: Int) {
        var refresh = false
        val annotations = mViewModel.findAnnotationByPage(mBook!!, page)
        if (annotations.isNotEmpty()) {
            for (annotation in annotations) {
                if (annotation.range[0] == start && annotation.range[1] == end) {
                    mViewModel.delete(annotation)
                    refresh = true
                } else if ((start >= annotation.range[0] && start <= annotation.range[1]) ||
                    (end >= annotation.range[0] && end <= annotation.range[1]) ||
                    (start <= annotation.range[0] && end >= annotation.range[1])
                ) {
                    mViewModel.delete(annotation)
                    refresh = true
                }
            }
        }
        if (refresh) {
            mViewModel.refreshAnnotations(mBook)
            mViewPager.adapter!!.notifyItemChanged(page)
        }
    }

    override fun textSelectRemoveMark(annotation: BookAnnotation) = mViewPager.adapter!!.notifyItemChanged(annotation.page)

    override fun textSelectChangeMark(annotation: BookAnnotation) = mViewPager.adapter!!.notifyItemChanged(annotation.page)

    override fun textSearch(page: Int, text: String) = openBookSearch(BookSearch(mBook!!.id!!, text, page))


    private fun markCurrentPage() {
        val msg: String

        val mark = mViewModel.findAnnotationByPage(mBook!!, mCurrentPage).find { it.type == MarkType.PageMark }
        if (mark != null) {
            mViewModel.delete(mark)
            msg = getString(R.string.book_annotation_page_unmarked, mCurrentPage)
        } else {

            val chapter = mParse!!.getChapter(mCurrentPage) ?: Pair(0, "")

            val html = mParse!!.getPage(mCurrentPage).pageHTMLWithImages.replace("<image-begin>", "<img src=\"data:").replace("<image-end>", "\" />")

            val separator = if (html.contains("<end-line>")) "<end-line>" else "<br>"
            val texts = html.split(separator).map { it.replace("<[^>]*>".toRegex(), "") }

            var text = ""
            for ((index, itm) in texts.withIndex()) {
                text += itm
                if (index >= 3)
                    break
            }

            val annotation = BookAnnotation(mBook!!.id!!, mCurrentPage, mParse!!.pageCount, MarkType.PageMark, chapter.first.toFloat(), chapter.second, text, intArrayOf(), "")
            mViewModel.save(annotation)
            msg = getString(R.string.book_annotation_page_marked, mCurrentPage)
        }

        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    inner class MyTouchListener : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (!isFullscreen()) {
                setFullscreen(fullscreen = true)
                return true
            }

            val position = getPosition(e)
            if ((requireActivity() as BookReaderActivity).touchPosition(position))
                return true

            if (position == Position.LEFT || position == Position.RIGHT) {
                val id = if (ReaderConsts.READER.BOOK_WEB_VIEW_MODE) R.id.page_web_view else R.id.page_scroll_view
                val view: AutoScroll? = (mViewPager[0] as RecyclerView).layoutManager?.findViewByPosition(mViewPager.currentItem)?.findViewById(id)
                view?.let {
                    val isBack = if (mIsLeftToRight) position == Position.LEFT else position == Position.RIGHT
                    if (it.autoScroll(isBack))
                        return true
                }
            }

            when (position) {
                Position.LEFT -> {
                    if (mIsLeftToRight) {
                        if (getCurrentPage() == 1) hitBeginning() else setCurrentPage(getCurrentPage() - 1)
                    } else {
                        if (getCurrentPage() == mViewPager.adapter!!.itemCount) hitEnding() else setCurrentPage(getCurrentPage() + 1)
                    }
                }

                Position.RIGHT -> {
                    if (mIsLeftToRight) {
                        if (getCurrentPage() == mViewPager.adapter!!.itemCount) hitEnding() else setCurrentPage(getCurrentPage() + 1)
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

    private fun setBookDots(pages: Int) {
        val dots = mutableListOf<Int>()
        val inverse = mutableListOf<Int>()

        mParse?.getChapters()?.let {
            for (chapter in it) {
                inverse.add(pages - chapter.first)
                dots.add(chapter.first)
            }
        }

        (requireActivity() as BookReaderActivity).setDots(dots, inverse)
    }

    private fun updateSeekBar() {
        val seekRes: Int = if (mIsLeftToRight) R.drawable.reader_nav_progress else R.drawable.reader_nav_progress_inverse
        val d: Drawable? = ContextCompat.getDrawable(requireActivity(), seekRes)
        val bounds = mPageSeekBar.progressDrawable.bounds
        mPageSeekBar.progressDrawable = d
        mPageSeekBar.progressDrawable.bounds = bounds
        mPageSeekBar.thumb.setColorFilter(requireContext().getColorFromAttr(R.attr.colorTertiary), PorterDuff.Mode.SRC_IN)
        mPageSeekBar.setDotsMode(!mIsLeftToRight)
    }

    private var mLastPageIsLeft = true
    private fun openLastPage() {
        if (mLastPage.isEmpty())
            return

        val page = mLastPage.first
        changeLastPage(page)
        if (mLastPageContainer.visibility != View.VISIBLE)
            transitionLastPage(true, mLastPageIsLeft)
    }

    private fun changeLastPagePosition(page: Int) {
        if (mLastPage.isEmpty() || mLastPage.first.first == page)
            return

        val position = mLastPage.first.first < page

        if (position != mLastPageIsLeft) {
            mLastPageIsLeft = position
            transitionLastPage(false, !position, object : Transition.TransitionListener {
                override fun onTransitionStart(transition: Transition) { }

                override fun onTransitionEnd(transition: Transition) {
                    mHandler.postDelayed({ transitionLastPage(true, position) }, 100)
                }

                override fun onTransitionCancel(transition: Transition) { }

                override fun onTransitionPause(transition: Transition) { }

                override fun onTransitionResume(transition: Transition) { }
            })
        }
    }

    private fun changeLastPage(lastPage: Pair<Int, Bitmap>) {
        mLastPageImage.setImageBitmap(lastPage.second)
        mLastPageText.text = lastPage.first.toString()
    }

    private fun closeLastPage() {
        if (mLastPageContainer.visibility == View.GONE)
            return

        transitionLastPage(false, mLastPageIsLeft)
    }

    private fun transitionLastPage(isVisible: Boolean, isLeft: Boolean, listenner: Transition.TransitionListener? = null) {
        if (isVisible && mLastPageContainer.visibility == View.VISIBLE || !isVisible && mLastPageContainer.visibility == View.GONE)
            return

        if (isVisible) {
            if (isLeft) {
                (mLastPageContainer.layoutParams as RelativeLayout.LayoutParams).removeRule(RelativeLayout.ALIGN_PARENT_END)
                (mLastPageContainer.layoutParams as RelativeLayout.LayoutParams).addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE)
            } else {
                (mLastPageContainer.layoutParams as RelativeLayout.LayoutParams).removeRule(RelativeLayout.ALIGN_PARENT_START)
                (mLastPageContainer.layoutParams as RelativeLayout.LayoutParams).addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE)
            }
        }

        val transition = if (isLeft) Slide(Gravity.START) else Slide(Gravity.END)
        transition.setDuration(800L)
        transition.addTarget(mLastPageContainer)
        transition.interpolator = AnticipateOvershootInterpolator()

        if (listenner != null)
            transition.addListener(listenner)

        TransitionManager.beginDelayedTransition(mRoot, transition)
        mLastPageContainer.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

}
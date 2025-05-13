package br.com.fenix.bilingualreader.view.ui.reader.book

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
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
import android.os.PowerManager
import android.provider.Settings
import android.text.Selection
import android.text.Spannable
import android.util.Base64
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
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
import br.com.fenix.bilingualreader.model.entity.History
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Speech
import br.com.fenix.bilingualreader.model.enums.AudioStatus
import br.com.fenix.bilingualreader.model.enums.Color
import br.com.fenix.bilingualreader.model.enums.MarkType
import br.com.fenix.bilingualreader.model.enums.Position
import br.com.fenix.bilingualreader.model.enums.ScrollingType
import br.com.fenix.bilingualreader.model.enums.TouchScreen
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.controller.BookImageCoverController
import br.com.fenix.bilingualreader.service.controller.TextToSpeechController
import br.com.fenix.bilingualreader.service.functions.AutoScroll
import br.com.fenix.bilingualreader.service.listener.BookParseListener
import br.com.fenix.bilingualreader.service.listener.TTSListener
import br.com.fenix.bilingualreader.service.listener.TextSelectCallbackListener
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.service.repository.HistoryRepository
import br.com.fenix.bilingualreader.service.repository.SharedData
import br.com.fenix.bilingualreader.service.repository.Storage
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.constants.ReaderConsts
import br.com.fenix.bilingualreader.util.helpers.AnimationUtil
import br.com.fenix.bilingualreader.util.helpers.ImageUtil
import br.com.fenix.bilingualreader.util.helpers.LibraryUtil
import br.com.fenix.bilingualreader.util.helpers.MenuUtil
import br.com.fenix.bilingualreader.util.helpers.TextUtil
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil.ThemeUtils.getColorFromAttr
import br.com.fenix.bilingualreader.util.helpers.TouchUtil.TouchUtils
import br.com.fenix.bilingualreader.view.components.DottedSeekBar
import br.com.fenix.bilingualreader.view.components.book.TextViewPage
import br.com.fenix.bilingualreader.view.components.book.TextViewPager
import br.com.fenix.bilingualreader.view.components.book.WebViewPage
import br.com.fenix.bilingualreader.view.components.book.WebViewPager
import br.com.fenix.bilingualreader.view.components.manga.ZoomRecyclerView
import br.com.fenix.bilingualreader.view.ui.menu.MenuActivity
import br.com.fenix.bilingualreader.view.ui.popup.PopupTTS
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.LinkedList
import kotlin.math.abs


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
    private lateinit var miScrollingMode: MenuItem
    private lateinit var mViewPager: ViewPager2
    private lateinit var mViewRecycler: ZoomRecyclerView
    private lateinit var mPagerAdapter: Adapter<RecyclerView.ViewHolder>
    private lateinit var mReaderTTSContainer: LinearLayout
    private lateinit var mReaderTTSPlay: MaterialButton
    private lateinit var mReaderTTSProgress: CircularProgressIndicator

    private lateinit var mCoverContent: ConstraintLayout
    private lateinit var mCoverImage: ImageView
    private lateinit var mCoverMessage: TextView
    private lateinit var mCoverWarning: ImageView

    private lateinit var mPopupConfiguration: FrameLayout

    private lateinit var mPageSeekBar: DottedSeekBar
    private lateinit var mGestureDetector: GestureDetector

    private lateinit var mLastPageContainer: MaterialCardView
    private lateinit var mLastPageImage: ImageView
    private lateinit var mLastPageText: TextView

    private lateinit var mTimeToEnding: TextView

    private var mIsFullscreen = false
    private var mScrollingMode: ScrollingType = ScrollingType.Pagination

    private var mTouchScreen = mapOf<Position, TouchScreen>()
    private var mFileName: String? = null
    private lateinit var mLibrary: Library
    private var mBook: Book? = null
    private var mNewBook: Book? = null
    private var mNewBookTitle = 0
    private lateinit var mStorage: Storage
    private lateinit var mHistoryRepository : HistoryRepository
    private var mTextToSpeech: TextToSpeechController? = null
    private var mDialog: AlertDialog? = null
    private var mMenuPopupBottomSheet: Boolean = false

    private var mIsSeekBarChange = false
    private var mPageStartReading = LocalDateTime.now()
    private var mPagesAverage = mutableListOf<Long>()

    private val mLastPage = LinkedList<Pair<Int, Bitmap>>()
    private val mHandler = Handler(Looper.getMainLooper())

    var mParse: DocumentParse? = null

    companion object {
        private const val ANIMATION_DURATION = 200L

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mCurrentPage = 0
        mStorage = Storage(requireContext())
        mHistoryRepository = HistoryRepository(requireContext())
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
                    file.path, mBook?.password ?: "", mViewModel.fontSize.value!!.toInt(),
                    resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE,
                    mViewModel.isJapaneseStyle(), this
                )

                if (mBook != null) {
                    mFileName = file.name
                    mCurrentPage = mBook!!.bookMark - 1
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
        mViewPager = view.findViewById(R.id.fragment_book_reader_pager)
        mViewRecycler = view.findViewById(R.id.fragment_book_reader_recycler)
        mPageSeekBar = requireActivity().findViewById(R.id.reader_book_bottom_progress)
        mPopupConfiguration = requireActivity().findViewById(R.id.popup_book_configuration)

        mCoverContent = view.findViewById(R.id.reader_book_cover_content)
        mCoverImage = view.findViewById(R.id.reader_book_cover)
        mCoverMessage = view.findViewById(R.id.reader_book_cover_message)
        mCoverWarning = view.findViewById(R.id.reader_book_cover_warning)

        mToolbarTop = requireActivity().findViewById(R.id.reader_book_toolbar_top)
        mToolbarBottom = requireActivity().findViewById(R.id.reader_book_toolbar_bottom)

        mReaderTTSContainer = requireActivity().findViewById(R.id.container_book_tts)
        mReaderTTSPlay = requireActivity().findViewById(R.id.reader_book_tts_play)
        mReaderTTSProgress = requireActivity().findViewById(R.id.reader_book_tts_progress)

        mLastPageContainer = requireActivity().findViewById(R.id.reader_last_page)
        mLastPageImage = requireActivity().findViewById(R.id.last_page_image)
        mLastPageText = requireActivity().findViewById(R.id.last_page_text)

        mTimeToEnding = requireActivity().findViewById(R.id.book_time_to_ending)

        mTouchScreen = TouchUtils.getTouch(requireContext(), Type.BOOK)
        mLastPageContainer.visibility = View.GONE

        mTimeToEnding.text = ""

        if (mBook != null) {
            mCoverMessage.visibility = View.GONE
            mCoverWarning.visibility = View.GONE

            BookImageCoverController.instance.setImageCoverAsync(requireContext(), mBook!!, mCoverImage, null, true)
            mHandler.postDelayed({ BookImageCoverController.instance.setImageCoverAsync(requireContext(), mBook!!, mCoverImage, null, false) }, 300)
            generateHistory(mBook!!)
        } else {
            mCoverMessage.visibility = View.VISIBLE
            mCoverWarning.visibility = View.VISIBLE

            mCoverImage.setImageBitmap(ImageUtil.applyCoverEffect(requireContext(), null, Type.BOOK))
            mCoverMessage.text = getString(R.string.reading_book_open_exception)
        }

        mMenuPopupBottomSheet = requireActivity().findViewById<ImageView>(R.id.popup_book_configuration_center_button) == null
        mPageStartReading = LocalDateTime.now()
        mPagesAverage = mutableListOf()

        onLoading(isFinished = false, isLoaded = false)

        mPageSeekBar.isEnabled = false
        mPageSeekBar.max = 2
        mPageSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    if (mScrollingMode == ScrollingType.PaginationRightToLeft)
                        setCurrentPage(mPageSeekBar.max - progress + 1)
                    else
                        setCurrentPage(progress + 1)
                    changeLastPagePosition(progress + 1)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                try {
                    mIsSeekBarChange = true

                    val current = (mPagerAdapter as TextViewPager).getHolder(getCurrentPage(isInternal = true)) ?: return
                    val page = seekBar.progress + 1
                    if (mLastPage.any { it.first == page })
                        return

                    if (mLastPage.size > 3)
                        mLastPage.removeLast()

                    val bitmap = if (!current.isOnlyImage) {
                        val text = current.textView
                        val bitmap = Bitmap.createBitmap(text.width, text.height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        text.draw(canvas)
                        bitmap
                    } else
                        (current.imageView.drawable as BitmapDrawable).bitmap

                    mLastPage.addFirst(Pair(page, bitmap))
                    openLastPage()
                } catch (e: Exception) {
                    mLOGGER.error("Error to insert last page: " + e.message, e)
                    Firebase.crashlytics.apply {
                        setCustomKey("message", "Error to insert last page: " + e.message)
                        recordException(e)
                    }
                }
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                mIsSeekBarChange = false
            }
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
        menu.clear()
        inflater.inflate(R.menu.menu_reader_book, menu)
        super.onCreateOptionsMenu(menu, inflater)

        miChapter = menu.findItem(R.id.menu_item_reader_book_chapter)
        miAnnotation = menu.findItem(R.id.menu_item_reader_book_annotation)
        miFontStyle = menu.findItem(R.id.menu_item_reader_book_font_style)
        miMarkPage = menu.findItem(R.id.menu_item_reader_book_mark_page)
        miSearch = menu.findItem(R.id.menu_item_reader_book_search)
        miReaderTTS = menu.findItem(R.id.menu_item_reader_book_tts)
        miScrollingMode = menu.findItem(R.id.menu_item_reader_book_scrolling_mode)

        when (mViewModel.scrollingType.value) {
            ScrollingType.Pagination -> menu.findItem(R.id.menu_item_reader_book_scrolling_pagination).isChecked = true
            ScrollingType.PaginationRightToLeft -> menu.findItem(R.id.menu_item_reader_book_scrolling_pagination_right_to_left).isChecked = true
            ScrollingType.PaginationVertical -> menu.findItem(R.id.menu_item_reader_book_scrolling_pagination_vertical).isChecked = true
            ScrollingType.Scrolling -> menu.findItem(R.id.menu_item_reader_book_scrolling_infinity_scrolling).isChecked = true
            else -> menu.findItem(R.id.menu_item_reader_book_scrolling_infinity_scrolling).isChecked = true
        }

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

        mViewModel.history?.let {
            it.setPageEnd(getCurrentPage())
            it.setEnd(LocalDateTime.now())
            it.id = mHistoryRepository.save(it)
        }

        super.onPause()
    }

    override fun onDestroy() {
        mViewModel.stopLoadChapters = true
        if (mViewModel.isLoadChapters) {
            mHandler.postDelayed({ destroyParse()
                SharedData.setDocumentParse(null)
            }, 200)
        } else {
            destroyParse()
            SharedData.setDocumentParse(null)
        }
        mTextToSpeech?.stop()
        mWakeLock?.release()
        mWakeLock = null
        removeRefreshSizeDelay()
        removeRefreshScrolling()
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }

    override fun onLoading(isFinished: Boolean, isLoaded: Boolean) {
        if (isFinished) {
            if (isLoaded && mParse != null) {
                val pages = mParse!!.getPageCount(mViewModel.getFontSize(isBook = true).toInt())
                mPageSeekBar.max = pages -1
                mCoverContent.animate().alpha(0.0f)
                    .setDuration(400L).setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            mCoverContent.visibility = View.GONE

                            val preferences = GeneralConsts.getSharedPreferences(requireContext())
                            if (preferences.getBoolean(GeneralConsts.KEYS.TOUCH.BOOK_TOUCH_DEMONSTRATION, true)) {
                                with(preferences.edit()) {
                                    this.putBoolean(GeneralConsts.KEYS.TOUCH.BOOK_TOUCH_DEMONSTRATION, false)
                                    this.commit()
                                }
                                (requireActivity() as BookReaderActivity).openTouchFunctions()
                            }
                        }
                    })
                setBookDots(pages)
                if (mBook != null && mBook!!.pages != pages) {
                    if (mBook!!.completed) {
                        mBook!!.bookMark = pages
                        mCurrentPage = mBook!!.bookMark - 1
                    }

                    mBook!!.pages = pages
                    mStorage.updateLastAccess(mBook!!)
                }
                preparePager()
            } else {
                mParse = null
                val cover = if (mBook != null) BookImageCoverController.instance.getBookCover(requireContext(), mBook!!, isCoverSize = true) else null
                mCoverMessage.visibility = View.VISIBLE
                mCoverWarning.visibility = View.VISIBLE

                mCoverImage.setImageBitmap(ImageUtil.applyCoverEffect(requireContext(), cover, Type.BOOK))
                mCoverMessage.text = getString(R.string.reading_book_open_exception)
            }
        } else {
            if (::mCoverContent.isInitialized) {
                mCoverContent.visibility = View.VISIBLE
                mCoverContent.alpha = 1f

                if (mBook == null) {
                    mCoverMessage.text = getString(R.string.reading_book_open_exception)
                    mCoverMessage.visibility = View.VISIBLE
                    mCoverWarning.visibility = View.VISIBLE
                } else {
                    mCoverMessage.text = ""
                    mCoverMessage.visibility = View.GONE
                    mCoverWarning.visibility = View.GONE
                }
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

        var scrolling = mViewModel.scrollingType.value

        if (scrolling == null) {
            val preferences = GeneralConsts.getSharedPreferences(requireContext())
            scrolling = ScrollingType.valueOf(preferences.getString(GeneralConsts.KEYS.READER.BOOK_PAGE_SCROLLING_MODE, ScrollingType.Pagination.toString())!!)
        }

        mPagerAdapter = if (ReaderConsts.READER.BOOK_WEB_VIEW_MODE)
            WebViewPager(requireActivity(), requireContext(), mViewModel, mParse, this@BookReaderFragment) as Adapter<RecyclerView.ViewHolder>
        else
            TextViewPager(requireContext(), mViewModel, mParse, this@BookReaderFragment, this@BookReaderFragment) as Adapter<RecyclerView.ViewHolder>

        configureScrolling(scrolling, true)
        observer()
    }

    private fun configureScrolling(type: ScrollingType, isInitial: Boolean = false) : Boolean {
        val page = if (isInitial) mCurrentPage + 1 else getCurrentPage()
        val isChange = isInitial || (type == ScrollingType.Scrolling && mViewPager.isVisible) ||
                ((type == ScrollingType.Pagination || type == ScrollingType.PaginationVertical || type == ScrollingType.PaginationRightToLeft) && mViewRecycler.isVisible)

        val isMode = ((mScrollingMode == ScrollingType.PaginationRightToLeft || type == ScrollingType.PaginationRightToLeft) && mScrollingMode != type)
        mScrollingMode = type

        if (isChange) {
            when (mScrollingMode) {
                ScrollingType.Pagination,
                ScrollingType.PaginationRightToLeft,
                ScrollingType.PaginationVertical,
                    -> {
                    mViewRecycler.setOnTouchListener(null)
                    mViewRecycler.clearOnScrollListeners()
                    mViewRecycler.setOnSwipeOutListener(null)

                    mViewRecycler.adapter = null
                    mViewRecycler.layoutManager = null

                    (mPagerAdapter as TextViewPager).refreshLayout(mScrollingMode)
                    mPagerAdapter.notifyDataSetChanged()
                    mViewPager.adapter = mPagerAdapter
                    mViewPager.offscreenPageLimit = ReaderConsts.READER.BOOK_OFF_SCREEN_PAGE_LIMIT
                    mViewPager.orientation = if (mScrollingMode == ScrollingType.PaginationVertical) ViewPager2.ORIENTATION_VERTICAL else ViewPager2.ORIENTATION_HORIZONTAL

                    mViewModel.changeScrolling(mScrollingMode)
                    mViewPager.isSaveEnabled = false
                    mViewPager.isSaveFromParentEnabled = false
                    mViewPager.setOnTouchListener(this@BookReaderFragment)
                    mViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                        override fun onPageSelected(position: Int) {
                            super.onPageSelected(position)
                            if (mScrollingMode == ScrollingType.PaginationRightToLeft)
                                setCurrentPage(mPagerAdapter.itemCount - position, false)
                            else
                                setCurrentPage(position + 1, false)

                            generatePageAverage()
                        }
                    })

                    mViewPager.visibility = View.VISIBLE
                    mViewRecycler.visibility = View.GONE

                    if (mBook != null && page != -1 && !isMode)
                        setCurrentPage(page, isAnimated = false)
                }

                ScrollingType.Scrolling -> {
                    mViewPager.adapter = null
                    mViewPager.setOnTouchListener(null)

                    mViewRecycler.setOnTouchListener(this@BookReaderFragment)
                    mViewRecycler.addOnScrollListener(InfinityScrollingListener())
                    mViewRecycler.setOnSwipeOutListener(object : ZoomRecyclerView.OnSwipeOutListener {
                        override fun onSwipeOutAtStart() = hitBeginning()
                        override fun onSwipeOutAtEnd() = hitEnding()
                    })

                    (mPagerAdapter as TextViewPager).refreshLayout(mScrollingMode)
                    mViewRecycler.adapter = mPagerAdapter
                    mViewRecycler.layoutManager = LinearLayoutManager(requireContext())
                    mViewRecycler.setItemViewCacheSize(ReaderConsts.READER.BOOK_OFF_SCREEN_PAGE_LIMIT)
                    mViewRecycler.isEnableZoom = false

                    mPagerAdapter.notifyDataSetChanged()
                    if (page != -1) {
                        mViewRecycler.layoutManager?.scrollToPosition(page - 1)
                        setChangeProgress(page, page)
                    }

                    mViewPager.visibility = View.GONE
                    mViewRecycler.visibility = View.VISIBLE
                }

                else -> {}
            }
        } else
            when (mScrollingMode) {
                ScrollingType.Horizontal,
                ScrollingType.HorizontalRightToLeft,
                ScrollingType.Vertical,
                    -> {
                    mViewPager.orientation = if (mScrollingMode == ScrollingType.PaginationVertical) ViewPager2.ORIENTATION_VERTICAL else ViewPager2.ORIENTATION_HORIZONTAL
                }

                ScrollingType.Scrolling -> mViewRecycler.adapter?.notifyDataSetChanged()

                else -> {}
            }

        if (isMode) {
            if (mBook != null)
                setCurrentPage(page, false)
            mPagerAdapter.notifyDataSetChanged()
            updateSeekBar()
        }

        return isChange
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        v.performClick()
        return mGestureDetector.onTouchEvent(event)
    }

    fun getCurrentPage(isInternal : Boolean = false): Int {
        if (!::mViewPager.isInitialized || !::mViewRecycler.isInitialized)
            return 1

        return when (mScrollingMode) {
            ScrollingType.Pagination,
            ScrollingType.PaginationVertical,
                -> if (isInternal) mViewPager.currentItem else mViewPager.currentItem.plus(1)

            ScrollingType.PaginationRightToLeft -> if (isInternal) (mPagerAdapter.itemCount - mViewPager.currentItem).minus(1) else mPagerAdapter.itemCount - mViewPager.currentItem
            ScrollingType.Scrolling,
                -> if (mViewRecycler.layoutManager != null) {
                val first = (mViewRecycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition().plus(1)
                val last = (mViewRecycler.layoutManager as LinearLayoutManager).findLastVisibleItemPosition().plus(1)
                (first + ((last - first) / 2)).toInt()
            } else 1

            else -> 1
        }
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.menu_item_reader_book_tts -> {
                if (mTextToSpeech == null)
                    executeTTS(getCurrentPage(isInternal = true))
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
                (miMarkPage.icon as AnimatedVectorDrawable).reset()
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

            R.id.menu_item_reader_book_config_touch_screen -> {
                configTouchFunctions()
            }

            R.id.menu_item_reader_book_scrolling_pagination,
            R.id.menu_item_reader_book_scrolling_pagination_right_to_left,
            R.id.menu_item_reader_book_scrolling_pagination_vertical,
            R.id.menu_item_reader_book_scrolling_infinity_scrolling,
                -> {
                menuItem.isChecked = true

                val scrolling = when (menuItem.itemId) {
                    R.id.menu_item_reader_book_scrolling_pagination -> ScrollingType.Pagination
                    R.id.menu_item_reader_book_scrolling_pagination_right_to_left -> ScrollingType.PaginationRightToLeft
                    R.id.menu_item_reader_book_scrolling_pagination_vertical -> ScrollingType.PaginationVertical
                    R.id.menu_item_reader_book_scrolling_infinity_scrolling -> ScrollingType.Scrolling
                    else -> ScrollingType.Scrolling
                }

                mViewModel.changeScrolling(scrolling)
            }
        }

        return super.onOptionsItemSelected(menuItem)
    }


    private fun getPosition(e: MotionEvent): Position {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        val horizontalSize = resources.getDimensionPixelSize(R.dimen.reader_touch_demonstration_initial_horizontal)
        val horizontal = (if (isLandscape) horizontalSize * 1.2 else horizontalSize * 1.5).toFloat()

        val x = e.rawX
        val y = e.rawY
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
        } else
            mHandler.removeCallbacks(mRefreshSizeDelay)
    }

    private var mRefreshScrollingMode = Runnable { }
    private fun refreshScrolling(scrolling: ScrollingType) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (mHandler.hasCallbacks(mRefreshScrollingMode))
                mHandler.removeCallbacks(mRefreshScrollingMode)
        } else
            mHandler.removeCallbacks(mRefreshScrollingMode)

        mRefreshScrollingMode = Runnable {
            if (!::miScrollingMode.isInitialized)
                mHandler.postDelayed(mRefreshScrollingMode, 300)
            else {
                if (miScrollingMode.subMenu != null) {
                    when (scrolling) {
                        ScrollingType.Pagination -> miScrollingMode.subMenu!!.findItem(R.id.menu_item_reader_book_scrolling_pagination).isChecked = true
                        ScrollingType.PaginationRightToLeft -> miScrollingMode.subMenu!!.findItem(R.id.menu_item_reader_book_scrolling_pagination_right_to_left).isChecked = true
                        ScrollingType.PaginationVertical -> miScrollingMode.subMenu!!.findItem(R.id.menu_item_reader_book_scrolling_pagination_vertical).isChecked = true
                        ScrollingType.Scrolling -> miScrollingMode.subMenu!!.findItem(R.id.menu_item_reader_book_scrolling_infinity_scrolling).isChecked = true
                        else -> miScrollingMode.subMenu!!.findItem(R.id.menu_item_reader_book_scrolling_pagination).isChecked = true
                    }
                }
            }
        }
    }

    private fun removeRefreshScrolling() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (mHandler.hasCallbacks(mRefreshScrollingMode))
                mHandler.removeCallbacks(mRefreshScrollingMode)
        } else
            mHandler.removeCallbacks(mRefreshScrollingMode)
    }

    private fun observer() {
        mViewModel.scrollingType.observe(viewLifecycleOwner) {
            if (!configureScrolling(it))
                (mPagerAdapter as TextViewPager).refreshLayout(it)
            mScrollingMode = it
            refreshScrolling(it)
        }

        mViewModel.ttsVoice.observe(viewLifecycleOwner) {
            mTextToSpeech?.setVoice(mViewModel.ttsVoice.value!!, mViewModel.ttsSpeed.value!!)
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
                    Firebase.crashlytics.apply {
                        setCustomKey("message", "Error generator css for book page: " + e.message)
                        recordException(e)
                    }
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
                mPagerAdapter.notifyItemChanged(getCurrentPage(isInternal = true))

                removeRefreshSizeDelay()
                mHandler.postDelayed(mRefreshSizeDelay, 1000)

                mBook?.let { book ->
                    (requireActivity() as BookReaderActivity).changePageDescription(book.chapter, book.chapterDescription, getCurrentPage(), pages)
                    book.pages = pages
                    mViewModel.update(book)
                    setBookDots(pages)
                    mViewModel.history?.setPages(pages)
                }
            }

            mPagesAverage.clear()
            generatePageAverage(isOnlyCalculate = true)
        }
    }

    private fun getActionBar(): ActionBar? = if (activity != null) (requireActivity() as AppCompatActivity).supportActionBar else null

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

                if (mPopupConfiguration.visibility != View.GONE)
                    AnimationUtil.animatePopupClose(requireActivity(), mPopupConfiguration, !mMenuPopupBottomSheet, navigationColor = false)
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
        val animated = if (isAnimated) abs(mCurrentPage - page) < 10 else false
        var seek = page

        if (isChangePage) {
            // Use animated to load because wrong page is set started
            when (mScrollingMode) {
                ScrollingType.Pagination,
                ScrollingType.PaginationVertical,
                    -> {
                    mViewPager.currentItem = page - 1
                }

                ScrollingType.PaginationRightToLeft -> {
                    mViewPager.setCurrentItem(mPagerAdapter.itemCount - page, animated)
                    seek = mPagerAdapter.itemCount - page
                }

                ScrollingType.Scrolling -> {
                    val isShort = abs(mCurrentPage - page) < 10
                    if (animated && isShort)
                        mViewRecycler.smoothScrollToPosition(page - 1)
                    else {
                        mViewRecycler.adapter?.notifyDataSetChanged()
                        mViewRecycler.layoutManager?.scrollToPosition(page - 1)
                    }
                }

                else -> return
            }
        }

        setChangeProgress(page, seek)
    }

    private fun setChangeProgress(page: Int, seekbar: Int) {
        mCurrentPage = page -1

        if (mCurrentPage < 0)
            mCurrentPage = 0

        if (mParse != null)
            mParse!!.getChapter(mCurrentPage)?.let {
                mBook!!.chapter = it.first
                mBook!!.chapterDescription = it.second
            }

        mPageSeekBar.progress = seekbar
        (requireActivity() as BookReaderActivity).changePageDescription(mBook!!.chapter, mBook!!.chapterDescription, page, mPagerAdapter.itemCount)
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
        if (newBook == null || mDialog != null) return
        var confirm = false
        mNewBook = newBook
        mNewBookTitle = titleRes
        mDialog = MaterialAlertDialogBuilder(requireActivity(), R.style.AppCompatAlertDialogStyle)
                .setTitle(titleRes)
                .setMessage(newBook.fileName)
                .setPositiveButton(R.string.switch_action_positive) { _, _ ->
                    if (activity == null)
                        return@setPositiveButton

                    mViewModel.history?.let {
                        it.setPageEnd(getCurrentPage())
                        it.setEnd(LocalDateTime.now())
                        it.id = mHistoryRepository.save(it)
                    }

                    confirm = true
                    if (mViewModel.isLoadChapters) {
                        mViewModel.stopLoadChapters = true
                        mHandler.postDelayed({
                            destroyParse()
                            (requireActivity() as BookReaderActivity).changeBook(mNewBook!!)
                        }, 200)
                    } else {
                        destroyParse()
                        (requireActivity() as BookReaderActivity).changeBook(mNewBook!!)
                    }
                }
                .setNegativeButton(R.string.switch_action_negative) { _, _ -> }
                .setOnDismissListener {
                    mDialog = null
                    if (!confirm) {
                        mNewBook = null
                        setFullscreen(fullscreen = true)
                    }
                }
                .create()
        mDialog?.show()
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

    fun configTouchFunctions() {
        if (mTextToSpeech != null)
            mTextToSpeech?.stop()

        val intent = Intent(requireContext(), MenuActivity::class.java)
        val bundle = Bundle()
        bundle.putInt(GeneralConsts.KEYS.FRAGMENT.ID, R.id.frame_touch_screen_config)
        bundle.putSerializable(GeneralConsts.KEYS.OBJECT.TYPE, Type.BOOK)
        bundle.putSerializable(GeneralConsts.KEYS.OBJECT.BOOK, mBook!!)
        intent.putExtras(bundle)
        requireActivity().overridePendingTransition(R.anim.fade_in_fragment_add_enter, R.anim.fade_out_fragment_remove_exit)
        startActivityForResult(intent, GeneralConsts.REQUEST.TOUCH_CONFIGURATION, null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            GeneralConsts.REQUEST.BOOK_ANNOTATION -> {
                mViewModel.refreshAnnotations(mBook)
                mPagerAdapter.notifyDataSetChanged()

                if (data?.extras != null && data.extras!!.containsKey(GeneralConsts.KEYS.OBJECT.BOOK_ANNOTATION)) {
                    val annotation = data.extras!!.getSerializable(GeneralConsts.KEYS.OBJECT.BOOK_ANNOTATION) as BookAnnotation

                    if (annotation.fontSize != mViewModel.fontSize.value!!) {
                        mViewModel.changeFontSize(annotation.fontSize)
                        mHandler.postDelayed({
                            setCurrentPage(annotation.page + 1, isAnimated = false)
                            mPagerAdapter.notifyItemChanged(annotation.page)
                        }, 1000)
                    } else if (annotation.page > 0)
                        setCurrentPage(annotation.page + 1, isAnimated = false)
                }

                setFullscreen(true)
            }
            GeneralConsts.REQUEST.BOOK_SEARCH -> {
                if (data?.extras != null && data.extras!!.containsKey(GeneralConsts.KEYS.OBJECT.BOOK_SEARCH)) {
                    val search = data.extras!!.getSerializable(GeneralConsts.KEYS.OBJECT.BOOK_SEARCH) as BookSearch
                    setCurrentPage(search.page, isAnimated = false)
                    mHandler.postDelayed({
                        setFullscreen(true)
                        if (!ReaderConsts.READER.BOOK_WEB_VIEW_MODE) {
                            val textView = (mPagerAdapter as TextViewPager).getHolder(search.page-1)?.textView ?: return@postDelayed
                            val text = TextUtil.clearHighlightWordInText(search.search)
                            val position = textView.text.indexOf(text)
                            if (position > 0) {
                                textView.requestFocus()
                                Selection.setSelection(textView.text as Spannable, position, position + text.length)
                            }
                        }
                    }, 1200)
                }
            }
            GeneralConsts.REQUEST.TOUCH_CONFIGURATION -> {
                mTouchScreen = TouchUtils.getTouch(requireContext(), Type.BOOK)
            }
        }
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
        bundle.putBoolean(GeneralConsts.KEYS.OBJECT.DOCUMENT_JAPANESE_STYLE, mViewModel.isJapaneseStyle())
        mViewModel.fontSize.value?.let { bundle.putFloat(GeneralConsts.KEYS.OBJECT.BOOK_FONT_SIZE, it) }

        if (search != null)
            bundle.putSerializable(GeneralConsts.KEYS.OBJECT.BOOK_SEARCH, search)

        intent.putExtras(bundle)
        requireActivity().overridePendingTransition(R.anim.fade_in_fragment_add_enter, R.anim.fade_out_fragment_remove_exit)
        startActivityForResult(intent, GeneralConsts.REQUEST.BOOK_SEARCH, null)
    }

    private var mWakeLock : PowerManager.WakeLock? = null
    fun executeTTS(page: Int, initial: String = "") {
        if (mTextToSpeech == null) {
            setFullscreen(fullscreen = true)
            mTextToSpeech = TextToSpeechController(requireContext(), mBook!!, mParse, (mCoverImage.drawable as BitmapDrawable).bitmap, mViewModel.getFontSize(true).toInt())
            mTextToSpeech!!.addListener(this)
            mTextToSpeech!!.addListener(mPagerAdapter as TTSListener)
            mTextToSpeech!!.setVoice(mViewModel.ttsVoice.value!!, mViewModel.ttsSpeed.value!!)
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

                generateHistory(mBook!!)
                mViewModel.history?.let { it.useTTS = true }

                try {
                    (requireActivity().getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                        mWakeLock = newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EndlessService::lock")
                        mWakeLock?.acquire()

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (!isIgnoringBatteryOptimizations(requireContext().packageName)) {
                                //  Prompt the user to disable battery optimization
                                val intent = Intent()
                                intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                startActivity(intent)
                            }
                        }
                    }

                    requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } catch (_: Exception) {
                }
            }

            AudioStatus.PLAY -> {
                if (mReaderTTSProgress.isIndeterminate) {
                    mReaderTTSProgress.isIndeterminate = false
                    mReaderTTSProgress.progress = getCurrentPage(isInternal = true)
                    mReaderTTSProgress.max = mPagerAdapter.itemCount

                    mViewModel.history?.let {
                        it.useTTS = true
                        it.id = mHistoryRepository.save(it)
                    }
                }

                mReaderTTSPlay.setIconResource(R.drawable.ico_tts_pause)
            }

            AudioStatus.PAUSE -> mReaderTTSPlay.setIconResource(R.drawable.ico_tts_play)
            AudioStatus.STOP -> {
                mReaderTTSPlay.setIconResource(R.drawable.ico_tts_close)
                generateHistory(mBook!!)

                try {
                    requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } catch (_: Exception) {
                }
            }
        }
    }

    override fun readingLine(speech: Speech) { }

    override fun changePageTTS(old: Int, new: Int) {
        mReaderTTSProgress.progress = new
        setCurrentPage(new + 1, isAnimated = true)
    }

    override fun stopTTS() {
        mTextToSpeech = null
        mWakeLock?.release()
        mWakeLock = null
    }

    private fun openMenuTTS() {
        val voice = mViewModel.ttsVoice.value
        val speed = mViewModel.ttsSpeed.value
        PopupTTS(requireContext()).getPopupTTS(voice!!, speed!!) { v, s ->
            if (voice != v || speed != s) {
                mViewModel.changeTTSVoice(v, s)
                mTextToSpeech?.setVoice(v, s)
            }
        }
    }

    override fun textSelectReadingFrom(page: Int, text: String) = executeTTS(page, text)

    override fun textSelectAddMark(page: Int, text: String, color: Color, start: Int, end: Int): BookAnnotation {
        val chapter = mParse!!.getChapter(page) ?: Pair(0, "")
        val annotation = BookAnnotation(
            mBook!!.id!!, page, mParse!!.pageCount, mViewModel.fontSize.value!!, MarkType.Annotation, chapter.first.toFloat(), chapter.second, text,
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

    override fun textSearch(page: Int, text: String) = openBookSearch(BookSearch(mBook!!.id!!, text, page + 1))


    fun markCurrentPage() {
        val msg: String

        val page = getCurrentPage()
        val current = getCurrentPage(isInternal = true)
        val mark = mViewModel.findAnnotationByPage(mBook!!, page).find { it.markType == MarkType.PageMark }
        if (mark != null) {
            mViewModel.delete(mark)
            msg = getString(R.string.book_annotation_page_unmarked, page)
        } else {
            val chapter = mParse!!.getChapter(current) ?: Pair(0, "")

            val html = mParse!!.getPage(current).pageHTMLWithImages.replace("<image-begin>", "<img src=\"data:").replace("<image-end>", "\" />")

            val separator = if (html.contains("<end-line>")) "<end-line>" else "<br>"
            val texts = html.split(separator).map { it.replace("<[^>]*>".toRegex(), "") }

            var text = ""
            for ((index, itm) in texts.withIndex()) {
                text += itm
                if (index >= 3)
                    break
            }

            val annotation = BookAnnotation(mBook!!.id!!, page, mParse!!.pageCount, mViewModel.fontSize.value!!, MarkType.PageMark, chapter.first.toFloat(), chapter.second, text, intArrayOf(), "")
            mViewModel.save(annotation)
            msg = getString(R.string.book_annotation_page_marked, page)
        }

        (mPagerAdapter as TextViewPager).getHolder(getCurrentPage(isInternal = true))?.pageMark?.let {
            it.visibility = if (mark == null) View.VISIBLE else View.GONE
        }

        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    private fun generateHistory(book: Book) {
        if (mViewModel.history != null) {
            if (mViewModel.history!!.fkLibrary != book.fkLibrary || mViewModel.history!!.fkReference != book.id) {
                mViewModel.history!!.setEnd(LocalDateTime.now())
                mHistoryRepository.save(mViewModel.history!!)
                mViewModel.history = History(book.fkLibrary!!, book.id!!, Type.BOOK, book.bookMark, book.pages, book.volume)
            }
        } else
            mViewModel.history = History(book.fkLibrary!!, book.id!!, Type.BOOK, book.bookMark, book.pages, book.volume)
    }

    private fun generatePageAverage(isOnlyCalculate: Boolean = false) {
        if (mIsSeekBarChange) {
            mPageStartReading = LocalDateTime.now()
            return
        }

        if (!isOnlyCalculate) {
            val pageSeconds = ChronoUnit.SECONDS.between(mPageStartReading, LocalDateTime.now())
            mPageStartReading = LocalDateTime.now()

            if (mPagesAverage.size > 5) {
                val first = mPagesAverage.first()
                val last = mPagesAverage.last()

                if (pageSeconds <= first)
                    mPagesAverage.remove(first)
                else if (pageSeconds >= last)
                    mPagesAverage.remove(last)
                else {
                    val center = (last - first)
                    if (pageSeconds == center)
                        mPagesAverage.remove(mPagesAverage[3])
                    else if (pageSeconds < center)
                        mPagesAverage.remove(mPagesAverage[2])
                    else
                        mPagesAverage.remove(mPagesAverage[4])
                }

                mPagesAverage.add(pageSeconds)
            } else
                mPagesAverage.add(pageSeconds)

            mPagesAverage.sort()
        }

        if (mPagesAverage.isEmpty())
            return

        var average = 0L
        for (second in mPagesAverage)
            average += second

        val page = getCurrentPage()
        average /= mPagesAverage.size
        mViewModel.history?.let {
            if (!isOnlyCalculate) {
                it.setPageEnd(page)
                it.setEnd(LocalDateTime.now())
            }
            it.averageTimeByPage = average
            it.id = mHistoryRepository.save(it)
        }

        if (mPagesAverage.size < 2) {
            mTimeToEnding.text = ""
            return
        }

        val remaining = average * (mPagerAdapter.itemCount - page)
        val now = LocalDateTime.now()
        val ending = now.plusSeconds(remaining)
        val hours = ChronoUnit.HOURS.between(now, ending)
        val minutes = (ChronoUnit.SECONDS.between(now, ending) % (60 * 60)) / 60

        mTimeToEnding.text = if (hours <= 0L && minutes <= 0L)
            ""
        else if (hours > 0)
            getString(R.string.reading_book_time_to_ending_hour, hours, minutes)
        else
            getString(R.string.reading_book_time_to_ending_minutes, minutes)
    }

    inner class MyTouchListener : SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (!isFullscreen()) {
                setFullscreen(fullscreen = true)
                return true
            }

            val position = getPosition(e)
            val touch = mTouchScreen[position]!!
            if ((requireActivity() as BookReaderActivity).touchPosition(touch))
                return true

            if (touch == TouchScreen.TOUCH_PREVIOUS_PAGE || touch == TouchScreen.TOUCH_NEXT_PAGE) {
                if (mScrollingMode == ScrollingType.Pagination || mScrollingMode == ScrollingType.PaginationVertical || mScrollingMode == ScrollingType.PaginationRightToLeft) {
                    val isBack = if (mScrollingMode == ScrollingType.PaginationRightToLeft) touch == TouchScreen.TOUCH_NEXT_PAGE else touch == TouchScreen.TOUCH_PREVIOUS_PAGE
                    if (ReaderConsts.READER.BOOK_WEB_VIEW_MODE) {
                        val view: AutoScroll? = (mViewPager[0] as RecyclerView).layoutManager?.findViewByPosition(mViewPager.currentItem)?.findViewById(R.id.page_web_view)
                        view?.let {
                            if (it.autoScroll(isBack))
                                return true
                        }
                    } else {
                        val textView: AutoScroll? = (mViewPager[0] as RecyclerView).layoutManager?.findViewByPosition(mViewPager.currentItem)?.findViewById(R.id.page_scroll_view)
                        val imageView: AutoScroll? = (mViewPager[0] as RecyclerView).layoutManager?.findViewByPosition(mViewPager.currentItem)?.findViewById(R.id.page_image_view)
                        val view = if (imageView != null && imageView.isVisible()) imageView else textView
                        view?.let {
                            if (it.autoScroll(isBack))
                                return true
                        }
                    }
                }
            }

            if (position == Position.CENTER)
                setFullscreen(fullscreen = false)
            else
                when (touch) {
                    TouchScreen.TOUCH_PREVIOUS_PAGE -> {
                        when (mScrollingMode) {
                            ScrollingType.Pagination,
                            ScrollingType.PaginationVertical,
                                -> {
                                if (getCurrentPage() == 1) hitBeginning() else setCurrentPage(getCurrentPage() - 1)
                            }

                            ScrollingType.PaginationRightToLeft -> {
                                if (getCurrentPage() == mPagerAdapter.itemCount) hitEnding() else setCurrentPage(getCurrentPage() + 1)
                            }

                            ScrollingType.Scrolling -> {
                                var first = (mViewRecycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                                if (first <= 0)
                                    hitBeginning()
                                else
                                    mViewRecycler.smoothScrollToPosition(first.minus(1))
                            }

                            else -> {}
                        }
                    }

                    TouchScreen.TOUCH_NEXT_PAGE -> {
                        when (mScrollingMode) {
                            ScrollingType.Pagination,
                            ScrollingType.PaginationVertical,
                                -> {
                                if (getCurrentPage() == mPagerAdapter.itemCount) hitEnding() else setCurrentPage(getCurrentPage() + 1)
                            }

                            ScrollingType.PaginationRightToLeft -> {
                                if (getCurrentPage() == mPagerAdapter.itemCount) hitEnding() else setCurrentPage(getCurrentPage() + 1)
                            }

                            ScrollingType.Scrolling -> {
                                val last = (mViewRecycler.layoutManager as LinearLayoutManager).findLastVisibleItemPosition().plus(1)
                                if (last >= mPagerAdapter.itemCount)
                                    hitEnding()
                                else
                                    mViewRecycler.smoothScrollToPosition(if (last > 1) last else (mViewRecycler.layoutManager as LinearLayoutManager).findLastVisibleItemPosition())
                            }

                            else -> {}
                        }
                    }

                    else -> setFullscreen(fullscreen = false)
                }

            return true
        }
    }

    inner class InfinityScrollingListener() : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                val adapter = (mViewRecycler.layoutManager as LinearLayoutManager)
                var first: Int = adapter.findFirstCompletelyVisibleItemPosition()
                if (first < 0)
                    first = adapter.findFirstVisibleItemPosition()
                var last: Int = adapter.findLastCompletelyVisibleItemPosition()
                if (last < 0)
                    last = adapter.findLastVisibleItemPosition()
                val center = if (last == first) first else (first + ((last - first) / 2)).toInt()
                setChangeProgress(center + 1, center)
                generatePageAverage()
            }
        }
    }

    private fun setBookDots(pages: Int) {
        val dots = mutableListOf<Int>()
        val inverse = mutableListOf<Int>()

        mParse?.getChapters()?.let {
            for (chapter in it) {
                inverse.add(pages - chapter.value)
                dots.add(chapter.value)
            }
        }

        (requireActivity() as BookReaderActivity).setDots(dots, inverse)
    }

    private fun updateSeekBar() {
        val seekRes: Int = if (mScrollingMode == ScrollingType.PaginationRightToLeft) R.drawable.reader_progress_pointer_inverse else R.drawable.reader_progress_pointer
        val drawable: Drawable? = ContextCompat.getDrawable(requireActivity(), seekRes)
        val bounds = mPageSeekBar.progressDrawable.bounds
        mPageSeekBar.progressDrawable = drawable
        mPageSeekBar.progressDrawable.bounds = bounds
        mPageSeekBar.thumb.setColorFilter(requireContext().getColorFromAttr(R.attr.colorTertiary), PorterDuff.Mode.SRC_IN)
        mPageSeekBar.setDotsMode(mScrollingMode == ScrollingType.PaginationRightToLeft)
        (requireActivity() as BookReaderActivity).updateSeekBar(mScrollingMode)
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

    private fun transitionLastPage(isVisible: Boolean, isLeft: Boolean, listener: Transition.TransitionListener? = null) {
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

        if (listener != null)
            transition.addListener(listener)

        TransitionManager.beginDelayedTransition(mRoot, transition)
        mLastPageContainer.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    fun onBackPressed(): Boolean {
        return if (mTextToSpeech != null && mTextToSpeech!!.getStatus() == AudioStatus.PLAY) {
            mTextToSpeech!!.stop()
            false
        } else
            true
    }

    fun destroyParse() {
        if (::mPagerAdapter.isInitialized) {
            if (ReaderConsts.READER.BOOK_WEB_VIEW_MODE)
                (mPagerAdapter as WebViewPager).clearParse()
            else
                (mPagerAdapter as TextViewPager).clearParse()
        }

        mParse?.destroy()
    }

}
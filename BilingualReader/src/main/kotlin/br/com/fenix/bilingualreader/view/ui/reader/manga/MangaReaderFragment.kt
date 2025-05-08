package br.com.fenix.bilingualreader.view.ui.reader.manga

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore.Images
import android.util.SparseArray
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
import android.view.animation.AnticipateOvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.util.isNotEmpty
import androidx.core.util.size
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Slide
import androidx.transition.Transition
import androidx.transition.TransitionManager
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.History
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.entity.MangaAnnotation
import br.com.fenix.bilingualreader.model.enums.MarkType
import br.com.fenix.bilingualreader.model.enums.Position
import br.com.fenix.bilingualreader.model.enums.ReaderMode
import br.com.fenix.bilingualreader.model.enums.ScrollingType
import br.com.fenix.bilingualreader.model.enums.TouchScreen
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.model.interfaces.BaseImageView
import br.com.fenix.bilingualreader.service.controller.MangaImageCoverController
import br.com.fenix.bilingualreader.service.controller.SubTitleController
import br.com.fenix.bilingualreader.service.parses.manga.Parse
import br.com.fenix.bilingualreader.service.parses.manga.ParseFactory
import br.com.fenix.bilingualreader.service.parses.manga.RarParse
import br.com.fenix.bilingualreader.service.repository.Storage
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.constants.ReaderConsts
import br.com.fenix.bilingualreader.util.helpers.AnimationUtil
import br.com.fenix.bilingualreader.util.helpers.ImageUtil
import br.com.fenix.bilingualreader.util.helpers.LibraryUtil
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil.ThemeUtils.getColorFromAttr
import br.com.fenix.bilingualreader.util.helpers.TouchUtil.TouchUtils
import br.com.fenix.bilingualreader.util.helpers.Util
import br.com.fenix.bilingualreader.view.components.DottedSeekBar
import br.com.fenix.bilingualreader.view.components.manga.ImageViewPage
import br.com.fenix.bilingualreader.view.components.manga.ImageViewPager
import br.com.fenix.bilingualreader.view.components.manga.ImageViewScrolling
import br.com.fenix.bilingualreader.view.components.manga.ZoomRecyclerView
import br.com.fenix.bilingualreader.view.managers.MangaHandler
import br.com.fenix.bilingualreader.view.ui.menu.MenuActivity
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.Picasso
import com.squareup.picasso.Picasso.LoadedFrom
import com.squareup.picasso.RequestHandler
import com.squareup.picasso.Target
import org.slf4j.LoggerFactory
import java.io.File
import java.io.OutputStream
import java.lang.ref.WeakReference
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.LinkedList
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class MangaReaderFragment : Fragment(), View.OnTouchListener {

    private val mLOGGER = LoggerFactory.getLogger(MangaReaderFragment::class.java)

    private val mViewModel: MangaReaderViewModel by activityViewModels()

    private lateinit var mRoot: CoordinatorLayout
    private lateinit var mToolbarTop: AppBarLayout
    private lateinit var mPageNavLayout: LinearLayout
    private lateinit var mPopupSubtitle: FrameLayout
    private lateinit var mPopupColor: FrameLayout
    private lateinit var mToolbarBottom: LinearLayout
    private lateinit var mPageSeekBar: DottedSeekBar
    private lateinit var mPageNavTextView: TextView
    private lateinit var mPreferences: SharedPreferences
    private lateinit var mGestureDetector: GestureDetector
    private lateinit var mViewPager: ImageViewPager
    private lateinit var mViewRecycler: ZoomRecyclerView
    private lateinit var mPreviousButton: MaterialButton
    private lateinit var mNextButton: MaterialButton
    private lateinit var miMarkPage: MenuItem

    private lateinit var mCoverContent: ConstraintLayout
    private lateinit var mCoverImage: ImageView
    private lateinit var mCoverMessage: TextView

    private lateinit var mLastPageContainer: MaterialCardView
    private lateinit var mLastPageImage: ImageView
    private lateinit var mLastPageText: TextView

    private var mTouchScreen = mapOf<Position, TouchScreen>()
    private var mResourceViewMode: HashMap<Int, ReaderMode> = HashMap()
    private var mIsFullscreen = false
    private var mFileName: String? = null
    var mReaderMode: ReaderMode = ReaderMode.FIT_WIDTH
    private var mScrollingMode: ScrollingType = ScrollingType.Horizontal
    var mUseMagnifierType = false
    var mKeepZoomBetweenPage = false

    var mParse: Parse? = null
    lateinit var mPicasso: Picasso
    private lateinit var mComicHandler: MangaHandler
    var mTargets = SparseArray<Target>()
    private var mLastZoomScale = 0f

    private var mIsSeekBarChange = false
    private var mPageStartReading = LocalDateTime.now()
    private var mPagesAverage = mutableListOf<Long>()
    private var mChapterSelected = ""

    private lateinit var mLibrary: Library
    private var mManga: Manga? = null
    private var mNewManga: Manga? = null
    private var mNewMangaTitle = 0
    private lateinit var mStorage: Storage
    private lateinit var mSubtitleController: SubTitleController
    private var mDialog: AlertDialog? = null
    private var mMenuPopupBottomSheet: Boolean = false

    private val mLastPage = LinkedList<Pair<Int, Bitmap>>()
    private val mHandler = Handler(Looper.getMainLooper())

    init {
        mResourceViewMode[R.id.manga_view_mode_aspect_fill] = ReaderMode.ASPECT_FILL
        mResourceViewMode[R.id.manga_view_mode_aspect_fit] = ReaderMode.ASPECT_FIT
        mResourceViewMode[R.id.manga_view_mode_fit_width] = ReaderMode.FIT_WIDTH
    }

    companion object {
        private const val LAST_PAGE_OUT_SCREEN = 100
        private const val ANIMATION_DURATION = 200L

        var mCurrentPage = 0
        private var mCacheFolderIndex = 0
        private val mCacheFolder = arrayOf(
            GeneralConsts.CACHE_FOLDER.A,
            GeneralConsts.CACHE_FOLDER.B,
            GeneralConsts.CACHE_FOLDER.C,
            GeneralConsts.CACHE_FOLDER.D,
            GeneralConsts.CACHE_FOLDER.E,
            GeneralConsts.CACHE_FOLDER.F,
            GeneralConsts.CACHE_FOLDER.G
        )

        fun create(): MangaReaderFragment {
            if (mCacheFolderIndex >= (mCacheFolder.size - 1))
                mCacheFolderIndex = 0
            else
                mCacheFolderIndex += 1

            val fragment = MangaReaderFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }

        fun create(library: Library, path: File): MangaReaderFragment {
            if (mCacheFolderIndex >= (mCacheFolder.size - 1))
                mCacheFolderIndex = 0
            else
                mCacheFolderIndex += 1

            val fragment = MangaReaderFragment()
            val args = Bundle()
            args.putSerializable(GeneralConsts.KEYS.OBJECT.LIBRARY, library)
            args.putSerializable(GeneralConsts.KEYS.OBJECT.FILE, path)
            fragment.arguments = args
            return fragment
        }

        fun create(library: Library, manga: Manga): MangaReaderFragment {
            if (mCacheFolderIndex >= (mCacheFolder.size - 1))
                mCacheFolderIndex = 0
            else
                mCacheFolderIndex += 1

            val fragment = MangaReaderFragment()
            val args = Bundle()
            args.putSerializable(GeneralConsts.KEYS.OBJECT.LIBRARY, library)
            args.putSerializable(GeneralConsts.KEYS.OBJECT.MANGA, manga)
            fragment.arguments = args
            return fragment
        }
    }

    private var mCurrentFragment: FrameLayout? = null
    fun getCurrencyImageView(): ImageView? {
        if (!isAdded || context == null)
            return null

        return when (mScrollingMode) {
            ScrollingType.Vertical,
            ScrollingType.Horizontal,
            ScrollingType.HorizontalRightToLeft,
                -> {
                mCurrentFragment?.findViewById<ImageView?>(R.id.page_image_view)
            }

            ScrollingType.Scrolling,
            ScrollingType.ScrollingDivider,
                -> {
                val adapter = (mViewRecycler.layoutManager as LinearLayoutManager)
                var first: Int = adapter.findFirstCompletelyVisibleItemPosition()
                if (first < 0)
                    first = adapter.findFirstVisibleItemPosition()
                var last: Int = adapter.findLastCompletelyVisibleItemPosition()
                if (last < 0)
                    last = adapter.findLastVisibleItemPosition()
                val center = if (last == first) first else (first + ((last - first) / 2)).toInt()
                val fragment = mViewRecycler.findViewHolderForAdapterPosition(center)?.itemView as FrameLayout?
                fragment?.findViewById(R.id.page_image_view) as ImageView
            }

            else -> null
        }
    }

    private fun getItemsCount(): Int {
        return when (mScrollingMode) {
            ScrollingType.Vertical,
            ScrollingType.Horizontal,
            ScrollingType.HorizontalRightToLeft,
                -> mViewPager.adapter!!.count

            ScrollingType.Scrolling,
            ScrollingType.ScrollingDivider,
                -> mViewRecycler.adapter!!.itemCount

            else -> mParse?.numPages() ?: 0
        }
    }

    private fun onRefresh() {
        if (!::mViewPager.isInitialized)
            return

        if (mTargets.isNotEmpty()) {
            when (mScrollingMode) {
                ScrollingType.Vertical,
                ScrollingType.Horizontal,
                ScrollingType.HorizontalRightToLeft,
                    -> {
                    loadImage(mTargets[mViewPager.currentItem] as MyTarget, mViewPager.adapter!!.count)
                    for (i in 0 until mTargets.size) {
                        if (mViewPager.currentItem != i)
                            loadImage(mTargets[mTargets.keyAt(i)] as MyTarget, mViewPager.adapter!!.count)
                    }
                }

                ScrollingType.Scrolling,
                ScrollingType.ScrollingDivider,
                    -> {
                    for (i in 0 until mTargets.size)
                        loadImage(mTargets[mTargets.keyAt(i)] as MyTarget, mViewRecycler.adapter!!.itemCount)
                }

                else -> {}
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mCurrentPage = 0
        mStorage = Storage(requireContext())
        mLibrary = LibraryUtil.getDefault(requireContext(), Type.MANGA)
        mPreferences = GeneralConsts.getSharedPreferences(requireContext())
        mSubtitleController = SubTitleController.getInstance(requireContext())

        val bundle: Bundle? = arguments
        if (bundle != null && !bundle.isEmpty) {
            mLastPage.clear()

            mLibrary = bundle.getSerializable(GeneralConsts.KEYS.OBJECT.LIBRARY) as Library

            mManga = bundle.getSerializable(GeneralConsts.KEYS.OBJECT.MANGA) as Manga?
            val file: File? = if (mManga != null) {
                mManga?.file
                if (mManga?.file != null)
                    mManga?.file
                else
                    File(mManga?.path!!)
            } else
                bundle.getSerializable(GeneralConsts.KEYS.OBJECT.FILE) as File?

            if (file != null && file.exists()) {
                if (mManga == null)
                    mManga = mStorage.findMangaByName(file.name)

                if (mManga != null) {
                    mCurrentPage = mManga!!.bookMark
                    mStorage.updateLastAccess(mManga!!)
                }

                mParse = ParseFactory.create(file)
                if (mParse != null) {
                    if (mParse is RarParse) {
                        val child = mCacheFolder[mCacheFolderIndex]
                        val cacheDir = File(GeneralConsts.getCacheDir(requireContext()), child)
                        if (!cacheDir.exists()) {
                            cacheDir.mkdir()
                        } else {
                            if (cacheDir.listFiles() != null)
                                for (f in cacheDir.listFiles()!!)
                                    f.delete()
                        }
                        (mParse as RarParse?)!!.setCacheDirectory(cacheDir)
                    }

                    if (savedInstanceState == null)
                        mSubtitleController.getListChapter(mManga, mParse!!)

                    val dots = mutableListOf<Int>()
                    val inverse = mutableListOf<Int>()

                    val pages = (mParse?.numPages() ?: 2) - 1
                    for (chapter in mParse?.getChapters() ?: intArrayOf()) {
                        inverse.add(pages - chapter)
                        dots.add(chapter)
                    }

                    var times = 0
                    val handler = Handler()
                    var setDot: () -> Unit = {}
                    setDot = {
                        try {
                            (requireActivity() as MangaReaderActivity).setDots(dots, inverse)
                        } catch (e: Exception) {
                            mLOGGER.error("Error to set dots", e)
                            times++
                            if (times < 3)
                                handler.postDelayed(setDot, 1000)
                        }
                    }
                    handler.postDelayed(setDot, 1000)

                    mSubtitleController.mReaderFragment = this
                    mFileName = file.name
                    mCurrentPage = max(1, min(mCurrentPage, mParse!!.numPages()))
                    mComicHandler = MangaHandler(mParse!!)
                    mPicasso = Picasso.Builder(requireContext())
                        .addRequestHandler((mComicHandler as RequestHandler))
                        .build()
                } else
                    mLOGGER.info("Error in open file.")
            } else {
                (requireActivity() as MangaReaderActivity).setDots(mutableListOf(), mutableListOf())
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

            mGestureDetector = GestureDetector(requireActivity(), MyTouchListener())

            mReaderMode = ReaderMode.valueOf(
                mPreferences.getString(
                    GeneralConsts.KEYS.READER.MANGA_READER_MODE,
                    ReaderMode.FIT_WIDTH.toString()
                ).toString()
            )

            mScrollingMode = ScrollingType.valueOf(
                mPreferences.getString(
                    GeneralConsts.KEYS.READER.MANGA_PAGE_SCROLLING_MODE,
                    ScrollingType.Horizontal.toString()
                ).toString()
            )

            mUseMagnifierType = mPreferences.getBoolean(GeneralConsts.KEYS.READER.MANGA_USE_MAGNIFIER_TYPE, false)
            mKeepZoomBetweenPage = mPreferences.getBoolean(GeneralConsts.KEYS.READER.MANGA_KEEP_ZOOM_BETWEEN_PAGES, false)
        }

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view: View = inflater.inflate(R.layout.fragment_manga_reader, container, false)

        mRoot = requireActivity().findViewById(R.id.root_activity_manga_reader)
        mToolbarTop = requireActivity().findViewById(R.id.reader_manga_toolbar_reader_top)
        mPopupSubtitle = requireActivity().findViewById(R.id.popup_manga_translate)
        mPopupColor = requireActivity().findViewById(R.id.popup_manga_configurations)
        mPageNavLayout = requireActivity().findViewById(R.id.reader_manga_bottom_progress_content)
        mToolbarBottom = requireActivity().findViewById(R.id.reader_manga_toolbar_reader_bottom)
        mPreviousButton = requireActivity().findViewById(R.id.reader_manga_nav_previous_file)
        mNextButton = requireActivity().findViewById(R.id.reader_manga_nav_next_file)
        mViewPager = view.findViewById<View>(R.id.fragment_manga_reader) as ImageViewPager
        mViewRecycler = view.findViewById<View>(R.id.fragment_manga_reader_recycler) as ZoomRecyclerView

        mCoverContent = view.findViewById(R.id.reader_manga_cover_content)
        mCoverImage = view.findViewById(R.id.reader_manga_cover)
        mCoverMessage = view.findViewById(R.id.reader_manga_cover_message)

        mLastPageContainer = requireActivity().findViewById(R.id.reader_last_page)
        mLastPageImage = requireActivity().findViewById(R.id.last_page_image)
        mLastPageText = requireActivity().findViewById(R.id.last_page_text)

        mTouchScreen = TouchUtils.getTouch(requireContext(), Type.MANGA)
        mLastPageContainer.visibility = View.GONE

        mPageStartReading = LocalDateTime.now()
        mPagesAverage = mutableListOf()

        (mPageNavLayout.findViewById<View>(R.id.reader_manga_bottom_progress) as DottedSeekBar).also {
            mPageSeekBar = it
        }
        mPageNavTextView = mPageNavLayout.findViewById<View>(R.id.reader_manga_bottom_progress_title) as TextView

        if (mParse == null) {
            val cover = if (mManga != null) MangaImageCoverController.instance.getMangaCover(requireContext(), mManga!!, isCoverSize = true) else null
            mCoverImage.setImageBitmap(ImageUtil.applyCoverEffect(requireContext(), cover, Type.BOOK))
            mCoverMessage.text = getString(R.string.reading_manga_open_exception)
            mPageNavTextView.text = ""
            return view
        } else {
            MangaImageCoverController.instance.setImageCoverAsync(requireContext(), mManga!!, mCoverImage, null, true)
            mHandler.postDelayed({ MangaImageCoverController.instance.setImageCoverAsync(requireContext(), mManga!!, mCoverImage, null, false) }, 300)
        }

        mMenuPopupBottomSheet = requireActivity().findViewById<ImageView>(R.id.popup_manga_translate_center_button) == null

        var run: Runnable? = null
        run = Runnable {
            val image = getCurrencyImageView()
            if (image == null || image.isGone)
                mHandler.postDelayed(run!!, 800)
            else {
                mCoverContent.animate().alpha(0.0f).setDuration(600L).setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        mCoverContent.visibility = View.GONE
                    }
                })
            }
        }

        mHandler.postDelayed(run, 2000)

        mPageSeekBar.max = (mParse?.numPages() ?: 2) - 1
        mPageSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    if (mScrollingMode == ScrollingType.HorizontalRightToLeft)
                        setCurrentPage(mPageSeekBar.max - progress + 1)
                    else
                        setCurrentPage(progress + 1)
                    changeLastPagePosition(progress + 1)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                mIsSeekBarChange = true
                mPicasso.pauseTag(this@MangaReaderFragment.requireActivity())

                try {
                    val view = getCurrencyImageView() ?: return

                    val page = seekBar.progress + 1
                    if (mLastPage.any { it.first == page })
                        return

                    if (mLastPage.size > 3)
                        mLastPage.removeLast()

                    val bitmap = (view.drawable as BitmapDrawable).bitmap
                    mLastPage.addFirst(Pair(page, bitmap.copy(bitmap.config, true)))
                    openLastPage()
                } catch (e: Exception) {
                    mLOGGER.error("Error to insert last page", e)
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                mIsSeekBarChange = false
                mPicasso.resumeTag(this@MangaReaderFragment.requireActivity())
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

        configureScrolling(mScrollingMode, true)

        if (savedInstanceState != null) {
            val fullscreen = savedInstanceState.getBoolean(ReaderConsts.STATES.STATE_FULLSCREEN)
            setFullscreen(fullscreen)
            val newComicId = savedInstanceState.getLong(ReaderConsts.STATES.STATE_NEW_COMIC)
            val titleRes = savedInstanceState.getInt(ReaderConsts.STATES.STATE_NEW_COMIC_TITLE)
            confirmSwitch(mStorage.getManga(newComicId), titleRes)
        } else
            setFullscreen(true)

        mManga?.let { generateHistory(it) }
        requireActivity().title = mFileName
        updateSeekBar()

        mViewModel.filters.observe(viewLifecycleOwner) { onRefresh() }

        return view
    }

    private fun configureScrolling(type: ScrollingType, isInitial: Boolean = false) {
        val page = getCurrentPage()
        val isChange = isInitial || ((type == ScrollingType.Scrolling || type == ScrollingType.ScrollingDivider) && mViewPager.isVisible) ||
                ((type == ScrollingType.Horizontal || type == ScrollingType.HorizontalRightToLeft || type == ScrollingType.Vertical) && mViewRecycler.isVisible)

        val isMode = ((mScrollingMode == ScrollingType.HorizontalRightToLeft || type == ScrollingType.HorizontalRightToLeft) && mScrollingMode != type)
        mScrollingMode = type

        when (mScrollingMode) {
            ScrollingType.Horizontal,
            ScrollingType.HorizontalRightToLeft,
            ScrollingType.Vertical,
                -> {
                mViewPager.setSwipeOrientation(type == ScrollingType.Vertical)
            }

            ScrollingType.Scrolling,
            ScrollingType.ScrollingDivider,
                -> {
            }

            else -> {}
        }

        if (isChange) {
            mCurrentFragment = null
            when (mScrollingMode) {
                ScrollingType.Horizontal,
                ScrollingType.HorizontalRightToLeft,
                ScrollingType.Vertical,
                    -> {
                    mViewPager.setSwipeOrientation(mScrollingMode == ScrollingType.Vertical)
                    mViewPager.adapter = ComicPagerAdapter()
                    mViewPager.offscreenPageLimit = ReaderConsts.READER.MANGA_OFF_SCREEN_PAGE_LIMIT
                    mViewPager.setOnTouchListener(this@MangaReaderFragment)
                    mViewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
                        override fun onPageSelected(position: Int) {
                            if (mScrollingMode == ScrollingType.HorizontalRightToLeft)
                                setCurrentPage(mViewPager.adapter!!.count - position)
                            else
                                setCurrentPage(position + 1)

                            generatePageAverage()
                        }
                    })
                    mViewPager.setOnSwipeOutListener(object : ImageViewPager.OnSwipeOutListener {
                        override fun onSwipeOutAtStart() {
                            if (mScrollingMode == ScrollingType.HorizontalRightToLeft) hitEnding() else hitBeginning()
                        }

                        override fun onSwipeOutAtEnd() {
                            if (mScrollingMode == ScrollingType.HorizontalRightToLeft) hitBeginning() else hitEnding()
                        }
                    })

                    mViewRecycler.setOnTouchListener(null)
                    mViewRecycler.clearOnScrollListeners()
                    mViewRecycler.setOnSwipeOutListener(null)

                    mViewRecycler.adapter = null
                    mViewRecycler.layoutManager = null

                    mViewPager.visibility = View.VISIBLE
                    mViewRecycler.visibility = View.GONE

                    if (mCurrentPage != -1 && !isMode)
                        setCurrentPage(mCurrentPage)
                }

                ScrollingType.Scrolling,
                ScrollingType.ScrollingDivider,
                    -> {
                    mViewPager.setSwipeOrientation(false)
                    mViewPager.adapter = null
                    mViewPager.setOnTouchListener(null)
                    mViewPager.clearOnPageChangeListeners()
                    mViewPager.setOnSwipeOutListener(null)

                    mViewRecycler.setOnTouchListener(this@MangaReaderFragment)
                    mViewRecycler.addOnScrollListener(ComicRecyclerListener())
                    mViewRecycler.setOnSwipeOutListener(object : ZoomRecyclerView.OnSwipeOutListener {
                        override fun onSwipeOutAtStart() = hitBeginning()
                        override fun onSwipeOutAtEnd() = hitEnding()
                    })

                    mViewRecycler.adapter = ComicRecyclerAdapter()
                    mViewRecycler.layoutManager = LinearLayoutManager(requireContext())
                    mViewRecycler.setItemViewCacheSize(ReaderConsts.READER.MANGA_OFF_SCREEN_PAGE_LIMIT)
                    mViewRecycler.useMagnifierType = mUseMagnifierType

                    mViewRecycler.adapter?.notifyDataSetChanged()
                    if (mCurrentPage != -1) {
                        mViewRecycler.layoutManager?.scrollToPosition(mCurrentPage - 1)
                        setChangeProgress(mCurrentPage, mCurrentPage)
                    }

                    mViewPager.visibility = View.GONE
                    mViewRecycler.visibility = View.VISIBLE
                }

                else -> {}
            }
        } else if (mScrollingMode == ScrollingType.Scrolling || mScrollingMode == ScrollingType.ScrollingDivider)
            mViewRecycler.adapter?.notifyDataSetChanged()

        if (isMode) {
            setCurrentPage(page, false)
            mViewPager.adapter?.notifyDataSetChanged()
            updateSeekBar()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_reader_manga, menu)
        when (mReaderMode) {
            ReaderMode.ASPECT_FILL -> menu.findItem(R.id.manga_view_mode_aspect_fill).isChecked = true
            ReaderMode.ASPECT_FIT -> menu.findItem(R.id.manga_view_mode_aspect_fit).isChecked = true
            ReaderMode.FIT_WIDTH -> menu.findItem(R.id.manga_view_mode_fit_width).isChecked = true
        }

        when (mScrollingMode) {
            ScrollingType.Horizontal -> menu.findItem(R.id.reading_manga_scrolling_horizontal).isChecked = true
            ScrollingType.HorizontalRightToLeft -> menu.findItem(R.id.reading_manga_scrolling_horizontal_right_to_left).isChecked = true
            ScrollingType.Vertical -> menu.findItem(R.id.reading_manga_scrolling_vertical).isChecked = true
            ScrollingType.Scrolling -> menu.findItem(R.id.reading_manga_scrolling_scrolling).isChecked = true
            ScrollingType.ScrollingDivider -> menu.findItem(R.id.reading_manga_scrolling_scrolling_divider).isChecked = true
            else -> menu.findItem(R.id.reading_manga_scrolling_horizontal).isChecked = true
        }

        menu.findItem(R.id.menu_item_reader_manga_use_magnifier_type).isChecked = mUseMagnifierType
        menu.findItem(R.id.menu_item_reader_manga_keep_zoom_between_pages).isChecked = mKeepZoomBetweenPage
        menu.findItem(R.id.menu_item_reader_manga_show_clock_and_battery).isChecked =
            mPreferences.getBoolean(GeneralConsts.KEYS.READER.MANGA_SHOW_CLOCK_AND_BATTERY, false)

        miMarkPage = menu.findItem(R.id.menu_item_reader_manga_mark_page)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(ReaderConsts.STATES.STATE_FULLSCREEN, isFullscreen())
        outState.putLong(ReaderConsts.STATES.STATE_NEW_COMIC, (if (mNewManga != null) mNewManga!!.id else -1)!!)
        outState.putInt(ReaderConsts.STATES.STATE_NEW_COMIC_TITLE, if (mNewManga != null) mNewMangaTitle else -1)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        if (mManga != null) {
            mManga?.bookMark = getCurrentPage()
            mStorage.updateBookMark(mManga!!)
        }
        mViewModel.history?.let {
            it.setPageEnd(mCurrentPage + 1)
            it.setEnd(LocalDateTime.now())
            it.id = mViewModel.save(it)
        }
        super.onPause()
    }

    override fun onDestroy() {
        mViewModel.stopExecutions()
        mSubtitleController.clearControllers()
        if (mSubtitleController.mReaderFragment == this)
            mSubtitleController.mReaderFragment = null
        Util.destroyParse(mParse)
        if (::mPicasso.isInitialized)
            mPicasso.shutdown()

        super.onDestroy()
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        v.performClick()
        return mGestureDetector.onTouchEvent(event)
    }

    private fun openTouchFunctions() {
        val intent = Intent(requireContext(), MenuActivity::class.java)
        val bundle = Bundle()
        bundle.putInt(GeneralConsts.KEYS.FRAGMENT.ID, R.id.frame_touch_screen_config)
        bundle.putSerializable(GeneralConsts.KEYS.OBJECT.TYPE, Type.MANGA)
        bundle.putSerializable(GeneralConsts.KEYS.OBJECT.MANGA, mManga!!)
        intent.putExtras(bundle)
        requireActivity().overridePendingTransition(R.anim.fade_in_fragment_add_enter, R.anim.fade_out_fragment_remove_exit)
        startActivityForResult(intent, GeneralConsts.REQUEST.TOUCH_CONFIGURATION, null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            GeneralConsts.REQUEST.TOUCH_CONFIGURATION -> {
                mTouchScreen = TouchUtils.getTouch(requireContext(), Type.MANGA)
            }
        }
    }

    fun getCurrentPage(): Int {
        if (!::mViewPager.isInitialized || !::mViewRecycler.isInitialized)
            return 1

        return when (mScrollingMode) {
            ScrollingType.Vertical,
            ScrollingType.Horizontal,
                -> mViewPager.currentItem.plus(1)

            ScrollingType.HorizontalRightToLeft -> if (mViewPager.adapter != null) (mViewPager.adapter!!.count - mViewPager.currentItem) else 1
            ScrollingType.Scrolling,
            ScrollingType.ScrollingDivider,
                -> if (mViewRecycler.layoutManager != null) {
                val first = (mViewRecycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition().plus(1)
                val last = (mViewRecycler.layoutManager as LinearLayoutManager).findLastVisibleItemPosition().plus(1)
                (first + ((last - first) / 2)).toInt()
            } else 1

            else -> 1
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.manga_view_mode_aspect_fill, R.id.manga_view_mode_aspect_fit, R.id.manga_view_mode_fit_width -> {
                item.isChecked = true
                mReaderMode = mResourceViewMode[item.itemId] ?: ReaderMode.FIT_WIDTH
                changeAspect()
            }

            R.id.reading_manga_scrolling_horizontal,
            R.id.reading_manga_scrolling_horizontal_right_to_left,
            R.id.reading_manga_scrolling_vertical,
            R.id.reading_manga_scrolling_scrolling,
            R.id.reading_manga_scrolling_scrolling_divider,
                -> {
                item.isChecked = true

                val scrolling = when (item.itemId) {
                    R.id.reading_manga_scrolling_horizontal -> ScrollingType.Horizontal
                    R.id.reading_manga_scrolling_horizontal_right_to_left -> ScrollingType.HorizontalRightToLeft
                    R.id.reading_manga_scrolling_vertical -> ScrollingType.Vertical
                    R.id.reading_manga_scrolling_scrolling -> ScrollingType.Scrolling
                    R.id.reading_manga_scrolling_scrolling_divider -> ScrollingType.ScrollingDivider
                    else -> ScrollingType.Horizontal
                }
                with(mPreferences.edit()) {
                    this.putString(GeneralConsts.KEYS.READER.MANGA_PAGE_SCROLLING_MODE, scrolling.toString())
                    this.commit()
                }
                configureScrolling(scrolling)
            }

            R.id.menu_item_reader_manga_use_magnifier_type -> {
                item.isChecked = !item.isChecked
                mUseMagnifierType = item.isChecked

                with(mPreferences.edit()) {
                    this.putBoolean(GeneralConsts.KEYS.READER.MANGA_USE_MAGNIFIER_TYPE, mUseMagnifierType)
                    this.commit()
                }

                if (mScrollingMode == ScrollingType.Scrolling || mScrollingMode == ScrollingType.ScrollingDivider)
                    mViewRecycler.useMagnifierType = mUseMagnifierType
                else {
                    updatePageViews(mViewPager) {
                        (it as ImageViewPage).useMagnifierType = mUseMagnifierType
                    }
                }
            }

            R.id.menu_item_reader_manga_keep_zoom_between_pages -> {
                item.isChecked = !item.isChecked
                mKeepZoomBetweenPage = item.isChecked

                with(mPreferences.edit()) {
                    this.putBoolean(GeneralConsts.KEYS.READER.MANGA_KEEP_ZOOM_BETWEEN_PAGES, mKeepZoomBetweenPage)
                    this.commit()
                }
            }

            R.id.menu_item_reader_manga_save_share_image -> openPopupSaveShareImage()

            R.id.menu_item_reader_manga_mark_page -> {
                markCurrentPage()
                (miMarkPage.icon as AnimatedVectorDrawable).reset()
                (miMarkPage.icon as AnimatedVectorDrawable).start()
            }

            R.id.menu_item_reader_manga_config_touch_screen -> {
                openTouchFunctions()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun changeAspect(toolbar: Toolbar, mode: ReaderMode) {
        mReaderMode = mode
        changeAspect()
        val id: Int = mResourceViewMode.entries.first { it.value == mode }.key
        val menuItem: MenuItem = toolbar.menu.findItem(id)
        menuItem.isChecked = true
    }

    private fun changeAspect() {
        if (mScrollingMode == ScrollingType.Scrolling || mScrollingMode == ScrollingType.ScrollingDivider) {
            val first = (mViewRecycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            val last = (mViewRecycler.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
            mViewRecycler.adapter?.notifyItemChanged(first, last)
        } else {
            updatePageViews(mViewPager) {
                if (mReaderMode === ReaderMode.ASPECT_FILL)
                    (it as ImageViewPage).setTranslateToRightEdge(mScrollingMode == ScrollingType.HorizontalRightToLeft)
                (it as ImageViewPage).setViewMode(mReaderMode)
            }
        }
    }

    fun setCurrentPage(page: Int) {
        setCurrentPage(page, true)
    }

    private fun setCurrentPage(page: Int, animated: Boolean) {
        var seek = page - 1
        when (mScrollingMode) {
            ScrollingType.Vertical,
            ScrollingType.Horizontal,
                -> {
                mViewPager.currentItem = page - 1
            }

            ScrollingType.HorizontalRightToLeft -> {
                mViewPager.setCurrentItem(mViewPager.adapter!!.count - page, animated)
                seek = mViewPager.adapter!!.count - page
            }

            ScrollingType.Scrolling,
            ScrollingType.ScrollingDivider,
                -> {
                val isShort = abs(mCurrentPage - page) < 6
                if (animated && isShort)
                    mViewRecycler.smoothScrollToPosition(page - 1)
                else {
                    mViewRecycler.adapter?.notifyDataSetChanged()
                    mViewRecycler.layoutManager?.scrollToPosition(page - 1)
                }
            }

            else -> {}
        }

        setChangeProgress(page, seek)
    }

    private fun setChangeProgress(page: Int, seekbar: Int) {
        mPageSeekBar.progress = seekbar

        val navPage: String = if (mParse == null) "" else StringBuilder()
            .append(page).append("/").append(mParse?.numPages() ?: 1)
            .toString()
        mPageNavTextView.text = navPage
        mCurrentPage = page - 1

        if (mCurrentPage < 0)
            mCurrentPage = 0

        if (mManga != null)
            mSubtitleController.changeSubtitleInReader(mManga!!, mCurrentPage)

        (requireActivity() as MangaReaderActivity).changePage(mManga?.title ?: "", getChapterSelected(mCurrentPage), mCurrentPage + 1)
    }

    private fun getChapterSelected(page: Int): String {
        var chapter = ""

        if (mManga != null && mManga!!.chaptersPages.isEmpty() && mParse != null) {
            if (mParse!!.isComicInfo()) {
                mParse!!.getComicInfo()?.let {
                    val chapters = mutableMapOf<Int, String>()
                    if (it.pages != null && it.pages!!.size > mCurrentPage) {
                        for ((index, comic) in it.pages!!.withIndex()) {
                            if (comic.bookmark != null)
                                chapters[index] = comic.bookmark!!

                        }
                    }
                    mManga!!.chaptersPages = chapters
                    mViewModel.save(mManga!!)
                }
            }
        }

        if (mManga != null && mManga!!.chaptersPages.isNotEmpty()) {
            var last = mManga!!.chaptersPages.keys.first()
            for (chapter in mManga!!.chaptersPages.keys) {
                if (chapter > page)
                    break
                last = chapter
            }

            chapter = mManga!!.chaptersPages[last] ?: ""
        }

        if (chapter.isEmpty())
            chapter = mParse?.getPagePath(page) ?: ""

        return chapter
    }

    inner class ComicPagerAdapter : PagerAdapter() {
        override fun isViewFromObject(view: View, o: Any): Boolean {
            return view === o
        }

        override fun getCount(): Int {
            return mParse?.numPages() ?: 1
        }

        override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
            if (mCurrentFragment !== `object`) {
                if ((`object` as FrameLayout).findViewById<View>(R.id.page_image_view) != null) {
                    mLastZoomScale = (getCurrencyImageView() as ImageViewPage?)?.getCurrentScale() ?: 0f

                    mCurrentFragment = `object`

                    if (mKeepZoomBetweenPage && mLastZoomScale != 0f)
                        (getCurrencyImageView() as ImageViewPage?)?.zoomAnimated(
                            mLastZoomScale,
                            mScrollingMode != ScrollingType.HorizontalRightToLeft
                        )
                }
            }
            super.setPrimaryItem(container, position, `object`)
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val inflater = requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val layout: View = inflater.inflate(R.layout.fragment_manga_page_pager, container, false)
            val imageViewPage: ImageViewPage = layout.findViewById<View>(R.id.page_image_view) as ImageViewPage
            if (mReaderMode === ReaderMode.ASPECT_FILL)
                imageViewPage.setTranslateToRightEdge(mScrollingMode == ScrollingType.HorizontalRightToLeft)
            imageViewPage.setViewMode(mReaderMode)
            imageViewPage.useMagnifierType = mUseMagnifierType
            imageViewPage.setOnTouchListener(this@MangaReaderFragment)
            container.addView(layout)
            val t = MyTarget(layout, position) { }
            loadImage(t, getItemsCount())
            mTargets.put(position, t)
            return layout
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            val layout = `object` as View
            mSubtitleController.removeImageBackup(position)
            mPicasso.cancelRequest(mTargets[position])
            mTargets.delete(position)
            container.removeView(layout)
            val iv = layout.findViewById<View>(R.id.page_image_view) as ImageView
            val drawable = iv.drawable
            if (drawable is BitmapDrawable) {
                val bm = drawable.bitmap
                bm?.recycle()
            }
        }
    }

    inner class ComicRecyclerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val item = LayoutInflater.from(parent.context).inflate(R.layout.fragment_manga_page_scrolling, parent, false)
            return ComicRecyclerViewHolder(item)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            (holder as ComicRecyclerViewHolder).bind(position, itemCount)
        }

        override fun getItemCount(): Int = mParse?.numPages() ?: 1
    }

    inner class ComicRecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var lastPosition: Int = -1
        fun bind(position: Int, itemCount: Int) {
            if (lastPosition > -1) {
                mSubtitleController.removeImageBackup(lastPosition)
                mTargets[lastPosition]?.run { mPicasso.cancelRequest(this) }
                mTargets.delete(lastPosition)
            }

            itemView.findViewById<View>(R.id.load_progress_bar).visibility = View.VISIBLE
            val rootViewPage = itemView.findViewById<FrameLayout>(R.id.frame_reader_page_root)
            val baseViewPage = itemView.findViewById<LinearLayout>(R.id.page_image_base)
            val imageViewPage = itemView.findViewById<ImageViewScrolling>(R.id.page_image_view)
            val dividerPage = itemView.findViewById<TextView>(R.id.page_divider_name)
            dividerPage.visibility = View.GONE
            imageViewPage.visibility = View.GONE
            imageViewPage.setImageBitmap(null)

            rootViewPage.layoutParams.width = FrameLayout.LayoutParams.MATCH_PARENT
            rootViewPage.layoutParams.height = FrameLayout.LayoutParams.MATCH_PARENT
            val t = MyTarget(itemView, position) {
                when (mReaderMode) {
                    ReaderMode.ASPECT_FILL -> {
                        imageViewPage.layoutParams.width = FrameLayout.LayoutParams.WRAP_CONTENT
                        imageViewPage.layoutParams.height = FrameLayout.LayoutParams.WRAP_CONTENT
                    }

                    ReaderMode.ASPECT_FIT -> {
                        imageViewPage.layoutParams.width = FrameLayout.LayoutParams.WRAP_CONTENT
                        imageViewPage.layoutParams.height = FrameLayout.LayoutParams.MATCH_PARENT
                    }

                    ReaderMode.FIT_WIDTH -> {
                        imageViewPage.layoutParams.width = FrameLayout.LayoutParams.MATCH_PARENT
                        imageViewPage.layoutParams.height = FrameLayout.LayoutParams.WRAP_CONTENT
                    }
                }

                rootViewPage.layoutParams.width = imageViewPage.layoutParams.width
                rootViewPage.layoutParams.height = imageViewPage.layoutParams.height
                baseViewPage.layoutParams.width = imageViewPage.layoutParams.width
                baseViewPage.layoutParams.height = imageViewPage.layoutParams.height

                dividerPage.visibility = if (mScrollingMode == ScrollingType.ScrollingDivider) {
                    dividerPage.text = "$position / $itemCount"
                    View.VISIBLE
                } else
                    View.GONE
            }

            loadImage(t, itemCount)
            mTargets.put(position, t)
            lastPosition = position
        }
    }

    inner class ComicRecyclerListener() : RecyclerView.OnScrollListener() {
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

    fun loadImage(t: Target, position: Int, resize: Boolean = true) {
        try {
            val request = mPicasso.load(mComicHandler.getPageUri(position))
                .memoryPolicy(MemoryPolicy.NO_STORE)
                .tag(requireActivity())

            if (resize)
                request.resize(ReaderConsts.READER.MAX_PAGE_WIDTH, ReaderConsts.READER.MAX_PAGE_HEIGHT)
                    .centerInside()
                    .onlyScaleDown()

            request.transform(mViewModel.filters.value!!).into(t)
        } catch (e: Exception) {
            mLOGGER.error("Error in open image: " + e.message, e)
        }
    }

    fun loadImage(t: Target, path: Uri, resize: Boolean = true) {
        try {
            val request = mPicasso.load(path)
                .memoryPolicy(MemoryPolicy.NO_STORE)
                .tag(requireActivity())

            if (resize)
                request.resize(ReaderConsts.READER.MAX_PAGE_WIDTH, ReaderConsts.READER.MAX_PAGE_HEIGHT)
                    .centerInside()
                    .onlyScaleDown()

            request.transform(mViewModel.filters.value!!).into(t)
        } catch (e: Exception) {
            mLOGGER.error("Error in open image: " + e.message, e)
        }
    }

    fun loadImage(t: MyTarget, pages: Int) {
        val pos: Int = if (mScrollingMode == ScrollingType.HorizontalRightToLeft)
            pages - t.position - 1
        else
            t.position

        try {
            mPicasso.load(mComicHandler.getPageUri(pos))
                .memoryPolicy(MemoryPolicy.NO_STORE)
                .tag(requireActivity())
                .resize(ReaderConsts.READER.MAX_PAGE_WIDTH, ReaderConsts.READER.MAX_PAGE_HEIGHT)
                .centerInside()
                .onlyScaleDown()
                .transform(mViewModel.filters.value!!)
                .into(t)
        } catch (e: Exception) {
            mLOGGER.error("Error in open image: " + e.message, e)
        }
    }

    inner class MyTarget(layout: View, val position: Int, val onLoaded: (View) -> (Unit)) : Target, View.OnClickListener {
        private val mLayout: WeakReference<View> = WeakReference(layout)

        private fun setVisibility(imageView: Int, progressBar: Int, reloadButton: Int) {
            val layout = mLayout.get() ?: return
            layout.findViewById<View>(R.id.page_image_view).visibility = imageView
            layout.findViewById<View>(R.id.load_progress_bar).visibility = progressBar
            layout.findViewById<View>(R.id.reload_Button).visibility = reloadButton
        }

        override fun onBitmapLoaded(bitmap: Bitmap, from: LoadedFrom) {
            val layout = mLayout.get() ?: return
            setVisibility(View.VISIBLE, View.GONE, View.GONE)
            val iv = layout.findViewById<View>(R.id.page_image_view) as ImageView
            iv.setImageBitmap(bitmap)
            onLoaded(layout)
        }

        override fun onBitmapFailed(p0: Exception, errorDrawable: Drawable?) {
            mLOGGER.error("Bitmap load fail: " + p0.message, p0)
            val layout = mLayout.get() ?: return
            setVisibility(View.GONE, View.GONE, View.VISIBLE)
            val ib = layout.findViewById<View>(R.id.reload_Button) as ImageButton
            ib.setOnClickListener(this@MyTarget)
        }

        override fun onPrepareLoad(placeHolderDrawable: Drawable?) {

        }

        override fun onClick(v: View) {
            mLayout.get() ?: return
            setVisibility(View.GONE, View.VISIBLE, View.GONE)
            loadImage(this, getItemsCount())
        }

    }

    inner class MyTouchListener : SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)
            val view: BaseImageView = (getCurrencyImageView() as BaseImageView?) ?: return
            val coordinator = view.getPointerCoordinate(e)
            mSubtitleController.selectTextByCoordinate(coordinator)
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (!isFullscreen()) {
                setFullscreen(fullscreen = true)
                return true
            }

            val position = getPosition(e)
            val touch = mTouchScreen[position]!!
            if ((requireActivity() as MangaReaderActivity).touchPosition(touch))
                return true

            if (touch == TouchScreen.TOUCH_PREVIOUS_PAGE || touch == TouchScreen.TOUCH_NEXT_PAGE) {
                if (mScrollingMode == ScrollingType.Vertical || mScrollingMode == ScrollingType.Horizontal || mScrollingMode == ScrollingType.HorizontalRightToLeft)
                    (getCurrencyImageView() as ImageViewPage?)?.let {
                        val isBack =
                            if (mScrollingMode == ScrollingType.HorizontalRightToLeft) touch == TouchScreen.TOUCH_NEXT_PAGE else touch == TouchScreen.TOUCH_PREVIOUS_PAGE
                        if (it.autoScroll(isBack))
                            return true
                    }
            }

            if (position == Position.CENTER)
                setFullscreen(fullscreen = false)
            else
                when (touch) {
                    TouchScreen.TOUCH_SHARE_IMAGE -> shareImage(true)
                    TouchScreen.TOUCH_PREVIOUS_PAGE -> {
                        when (mScrollingMode) {
                            ScrollingType.Vertical,
                            ScrollingType.Horizontal,
                                -> {
                                if (getCurrentPage() == 1) hitBeginning() else setCurrentPage(getCurrentPage() - 1)
                            }

                            ScrollingType.HorizontalRightToLeft -> {
                                if (getCurrentPage() == mViewPager.adapter!!.count) hitEnding() else setCurrentPage(getCurrentPage() + 1)
                            }

                            ScrollingType.Scrolling,
                            ScrollingType.ScrollingDivider,
                                -> {
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
                            ScrollingType.Vertical,
                            ScrollingType.Horizontal,
                                -> {
                                if (getCurrentPage() == mViewPager.adapter!!.count) hitEnding() else setCurrentPage(getCurrentPage() + 1)
                            }

                            ScrollingType.HorizontalRightToLeft -> {
                                if (getCurrentPage() == 1) hitBeginning() else setCurrentPage(getCurrentPage() - 1)
                            }

                            ScrollingType.Scrolling,
                            ScrollingType.ScrollingDivider,
                                -> {
                                val last = (mViewRecycler.layoutManager as LinearLayoutManager).findLastVisibleItemPosition().plus(1)
                                if (last >= mViewRecycler.adapter!!.itemCount)
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

    private fun updatePageViews(parentView: ViewGroup, change: (BaseImageView) -> (Unit)) {
        for (i in 0 until parentView.childCount) {
            val child = parentView.getChildAt(i)
            if (child is ViewGroup)
                updatePageViews(child, change)
            else if (child is BaseImageView)
                change(child)
        }
    }

    private fun getActionBar(): ActionBar? = if (activity != null) (requireActivity() as AppCompatActivity).supportActionBar else null

    private val windowInsetsController by lazy {
        WindowInsetsControllerCompat(requireActivity().window, mViewPager)
    }

    fun setFullscreen(fullscreen: Boolean) {
        // Use to press full screen when rotate causes crash
        if (!isAdded || context == null)
            return

        mIsFullscreen = fullscreen

        val window: Window = requireActivity().window
        if (fullscreen) {
            mRoot.fitsSystemWindows = false
            changeContentsVisibility(fullscreen)
            Handler(Looper.getMainLooper()).postDelayed({
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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

                if (mPopupSubtitle.visibility != View.GONE)
                    AnimationUtil.animatePopupClose(requireActivity(), mPopupSubtitle, !mMenuPopupBottomSheet, navigationColor = false)

                if (mPopupColor.visibility != View.GONE)
                    AnimationUtil.animatePopupClose(requireActivity(), mPopupColor, !mMenuPopupBottomSheet, navigationColor = false)
            }, ANIMATION_DURATION)
        } else {
            Handler(Looper.getMainLooper()).postDelayed({ changeContentsVisibility(fullscreen) }, ANIMATION_DURATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                windowInsetsController.let {
                    it.show(WindowInsetsCompat.Type.systemBars())
                }
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

            window.statusBarColor = resources.getColor(R.color.status_bar_color)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.navigationBarColor = resources.getColor(R.color.status_bar_color)
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
            mPageNavLayout.visibility = visibility
            mToolbarBottom.visibility = visibility
            mToolbarTop.visibility = visibility
            mNextButton.visibility = visibility
            mPreviousButton.visibility = visibility

            mPageNavLayout.alpha = initialAlpha
            mToolbarTop.alpha = initialAlpha
            mToolbarBottom.alpha = initialAlpha
            mNextButton.alpha = initialAlpha
            mPreviousButton.alpha = initialAlpha

            mToolbarTop.translationY = initialTranslation
            mToolbarBottom.translationY = (initialTranslation * -1)
        }

        mPageNavLayout.animate().alpha(finalAlpha).setDuration(ANIMATION_DURATION)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    mPageNavLayout.visibility = visibility
                }
            })

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

        mNextButton.animate().alpha(finalAlpha).setDuration(ANIMATION_DURATION)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    mNextButton.visibility = visibility
                }
            })

        mPreviousButton.animate().alpha(finalAlpha).setDuration(ANIMATION_DURATION)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    mPreviousButton.visibility = visibility
                }
            })
    }

    fun isFullscreen(): Boolean = mIsFullscreen

    fun hitBeginning() {
        if (mManga != null) {
            val c: Manga? = mStorage.getPrevManga(mLibrary, mManga!!)
            confirmSwitch(c, R.string.switch_prev_comic)
        }
    }

    fun hitEnding() {
        if (mManga != null) {
            val c: Manga? = mStorage.getNextManga(mLibrary, mManga!!)
            confirmSwitch(c, R.string.switch_next_comic)
        }
    }

    private fun confirmSwitch(newManga: Manga?, titleRes: Int) {
        if (newManga == null || mDialog != null) return
        var confirm = false
        mNewManga = newManga
        mNewMangaTitle = titleRes
        mDialog = MaterialAlertDialogBuilder(requireActivity(), R.style.AppCompatAlertDialogStyle)
            .setTitle(titleRes)
            .setMessage(newManga.fileName)
            .setPositiveButton(R.string.switch_action_positive) { _, _ ->
                if (activity == null)
                    return@setPositiveButton

                mViewModel.history?.let {
                    it.setPageEnd(mCurrentPage + 1)
                    it.setEnd(LocalDateTime.now())
                    it.id = mViewModel.save(it)
                }

                confirm = true
                mViewModel.stopExecutions()
                val activity = requireActivity() as MangaReaderActivity
                activity.changeManga(mNewManga!!)
            }
            .setNegativeButton(R.string.switch_action_negative) { _, _ -> }
            .setOnDismissListener {
                mDialog = null
                if (!confirm) {
                    mNewManga = null
                    setFullscreen(fullscreen = true)
                }
            }
            .create()
        mDialog?.show()
    }

    private fun updateSeekBar() {
        val seekRes: Int =
            if (mScrollingMode == ScrollingType.HorizontalRightToLeft) R.drawable.reader_progress_pointer_inverse else R.drawable.reader_progress_pointer
        val d: Drawable? = ContextCompat.getDrawable(requireActivity(), seekRes)
        val bounds = mPageSeekBar.progressDrawable.bounds
        mPageSeekBar.progressDrawable = d
        mPageSeekBar.progressDrawable.bounds = bounds
        mPageSeekBar.thumb.setColorFilter(requireContext().getColorFromAttr(R.attr.colorTertiary), PorterDuff.Mode.SRC_IN)
        mPageSeekBar.setDotsMode(mScrollingMode == ScrollingType.HorizontalRightToLeft)
    }

    private fun openPopupSaveShareImage() {
        val items = arrayListOf(
            requireContext().getString(R.string.reading_manga_choice_save_image),
            requireContext().getString(R.string.reading_manga_choice_share_image)
        ).toTypedArray()

        mDialog = MaterialAlertDialogBuilder(requireContext(), R.style.AppCompatMaterialAlertList)
            .setTitle(getString(R.string.reading_manga_title_save_share_image))
            .setIcon(R.drawable.ico_save_share_image)
            .setItems(items) { _, selectItem ->
                val language = items[selectItem]
                shareImage(
                    language.equals(
                        requireContext().getString(R.string.reading_manga_choice_share_image),
                        true
                    )
                )
            }
            .setOnDismissListener { mDialog = null }
            .create()
        mDialog?.show()
    }

    private fun shareImage(isShare: Boolean) {
        mParse?.getPage(mCurrentPage)?.let {
            val os: OutputStream
            try {
                val fileName = (mManga?.name ?: mCurrentPage.toString()) + ".jpeg"
                val values = ContentValues()
                values.put(Images.Media.DISPLAY_NAME, fileName)
                values.put(Images.Media.TITLE, fileName)
                values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis())
                values.put(Images.Media.MIME_TYPE, "image/jpeg")

                val uri: Uri? = requireContext().contentResolver.insert(
                    Images.Media.EXTERNAL_CONTENT_URI,
                    values
                )
                os = requireContext().contentResolver.openOutputStream(uri!!)!!
                val bitmap = BitmapFactory.decodeStream(it)
                bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, os)

                if (isShare) {
                    val shareIntent = Intent()
                    shareIntent.action = Intent.ACTION_SEND
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    shareIntent.type = "image/*"
                    shareIntent.putExtra(Intent.EXTRA_TEXT, fileName)
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                    startActivity(
                        Intent.createChooser(
                            shareIntent,
                            requireContext().getString(R.string.reading_manga_choice_share_chose_app)
                        )
                    )
                }

                Util.closeOutputStream(os)
            } catch (e: Exception) {
                mLOGGER.error("Error generate image to share.", e)
            } finally {
                Util.closeInputStream(it)
            }
        }
    }

    private fun markCurrentPage() {
        val msg: String

        val page = mCurrentPage + 1
        val mark = mViewModel.findAnnotationByPage(mManga!!, page).find { it.markType == MarkType.PageMark }
        if (mark != null) {
            mViewModel.delete(mark)
            msg = getString(R.string.manga_annotation_page_unmarked, page)
        } else {
            val path = mParse!!.getPagePath(mCurrentPage) ?: ""
            val chapter = getChapterSelected(mCurrentPage)
            val annotation = MangaAnnotation(mManga!!.id!!, page, mParse!!.numPages(), MarkType.PageMark, chapter, path, "")
            getCurrencyImageView()?.let {
                if (it.drawable != null) {
                    val bitmap = (it.drawable as BitmapDrawable).bitmap
                    annotation.image = bitmap.copy(bitmap.config, true)
                }
            }
            mViewModel.save(annotation)
            msg = getString(R.string.manga_annotation_page_marked, page)
        }

        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    private fun generateHistory(manga: Manga) {
        if (mViewModel.history != null) {
            if (mViewModel.history!!.fkLibrary != manga.fkLibrary || mViewModel.history!!.fkReference != manga.id) {
                mViewModel.history!!.setEnd(LocalDateTime.now())
                mViewModel.save(mViewModel.history!!)
                mViewModel.history = History(manga.fkLibrary!!, manga.id!!, Type.MANGA, manga.bookMark, manga.pages, manga.volume)
            }
        } else
            mViewModel.history = History(manga.fkLibrary!!, manga.id!!, Type.MANGA, manga.bookMark, manga.pages, manga.volume)
    }

    private fun generatePageAverage() {
        if (mIsSeekBarChange) {
            mPageStartReading = LocalDateTime.now()
            return
        }

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

        var average = 0L
        for (second in mPagesAverage)
            average += second

        average /= mPagesAverage.size
        mViewModel.history?.let {
            it.setPageEnd(mCurrentPage + 1)
            it.setEnd(LocalDateTime.now())
            it.averageTimeByPage = average
            it.id = mViewModel.save(it)
        }
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
                override fun onTransitionStart(transition: Transition) {}

                override fun onTransitionEnd(transition: Transition) {
                    mHandler.postDelayed({ transitionLastPage(true, position) }, 100)
                }

                override fun onTransitionCancel(transition: Transition) {}

                override fun onTransitionPause(transition: Transition) {}

                override fun onTransitionResume(transition: Transition) {}
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

}
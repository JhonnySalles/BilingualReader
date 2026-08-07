package br.com.fenix.bilingualreader.view.ui.reader.manga

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore.Images
import android.util.LruCache
import android.util.SparseArray
import android.util.TypedValue
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
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.util.isNotEmpty
import androidx.core.util.size
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
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
import br.com.fenix.bilingualreader.model.enums.PaginationType
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
import br.com.fenix.bilingualreader.util.helpers.NavigationUtil.NavigationUtils.overrideActivityTransitionCompat
import br.com.fenix.bilingualreader.util.helpers.Telemetry
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil.ThemeUtils.getColorFromAttr
import br.com.fenix.bilingualreader.util.helpers.TouchUtil.TouchUtils
import br.com.fenix.bilingualreader.util.helpers.Util
import br.com.fenix.bilingualreader.util.helpers.blurOnceDeferred
import br.com.fenix.bilingualreader.view.components.DottedSeekBar
import br.com.fenix.bilingualreader.view.components.GlassRenderScheduler
import br.com.fenix.bilingualreader.view.components.PageCurlFrame
import br.com.fenix.bilingualreader.view.components.manga.ImageViewPage
import br.com.fenix.bilingualreader.view.components.manga.ImageViewPager
import br.com.fenix.bilingualreader.view.components.manga.ImageViewScrolling
import br.com.fenix.bilingualreader.view.components.manga.ZoomRecyclerView
import br.com.fenix.bilingualreader.view.managers.MangaReaderHandler
import br.com.fenix.bilingualreader.view.ui.menu.MenuActivity
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.GlassSetup
import eightbitlab.com.blurview.RenderEffectBlur
import coil.transform.Transformation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private var mSavedInstanceState: Bundle? = null
    private var mFileToParse: File? = null

    private lateinit var mRoot: CoordinatorLayout
    private lateinit var mToolbarTop: AppBarLayout
    private lateinit var mPageNavLayout: LinearLayout
    private var mPopupSubtitleBottom: FrameLayout? = null
    private var mPopupSubtitleLeft: FrameLayout? = null
    private var mPopupColorBottom: FrameLayout? = null
    private var mPopupColorLeft: FrameLayout? = null
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

    private lateinit var mBlurTop: BlurView
    private lateinit var mBlurBottom: BlurView
    private lateinit var mBlurProgress: BlurView
    private lateinit var mBlurNavPrevious: BlurView
    private lateinit var mBlurNavNext: BlurView

    private var mOriginalToolbarTopBg: Drawable? = null
    private var mOriginalToolbarBottomBg: Drawable? = null
    private var mOriginalPageNavBg: Drawable? = null
    private var mOriginalNavPreviousTint: ColorStateList? = null
    private var mOriginalNavNextTint: ColorStateList? = null

    private lateinit var mCoverContent: ConstraintLayout
    private lateinit var mCoverImage: ImageView
    private lateinit var mCoverMessage: TextView
    private lateinit var mCoverWarning: ImageView

    private lateinit var mLastPageContainer: MaterialCardView
    private lateinit var mLastPageImage: ImageView
    private lateinit var mLastPageText: TextView

    private var mTouchScreen = mapOf<Position, TouchScreen>()
    private var mResourceViewMode: HashMap<Int, ReaderMode> = HashMap()
    private var mIsFullscreen = false
    private var mFileName: String? = null
    var mReaderMode: ReaderMode = ReaderMode.FIT_WIDTH
    private var mScrollingMode: ScrollingType = ScrollingType.Horizontal
    private var mPaginationType: PaginationType = PaginationType.Default
    var mUseMagnifierType = false
    var mKeepZoomBetweenPage = false

    var mParse: Parse? = null
    private lateinit var mComicHandler: MangaReaderHandler
    var mTargets = SparseArray<MyTarget>()
    private var mLastZoomScale = 0f

    private val mPageCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(
        maxOf(1, (Runtime.getRuntime().maxMemory() / 1024L / 8L).toInt())
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    fun getActiveFilters(): List<Transformation> =
        mViewModel.filters.value ?: emptyList()

    private fun filtersSignature(): String =
        (mViewModel.filters.value ?: emptyList<Transformation>()).joinToString("|") { it.cacheKey }

    private fun pageCacheKey(page: Int): String = "$page@${filtersSignature()}"

    private var mIsSeekBarChange = false
    private var mCoverStartTime = System.currentTimeMillis()
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
    private var mPopupBottomSheet: Boolean = false

    private val mLastPage = LinkedList<Pair<Int, Bitmap>>()
    private val mHandler = Handler(Looper.getMainLooper())

    private var mLocalCurrentPage = 0
        set(value) {
            field = value
            if (isAdded && !isRemoving && !isDetached) {
                Companion.mCurrentPage = value
            }
        }

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

        // Filtros mudaram: as chaves antigas (com outra assinatura) nao serao mais
        // reutilizadas; limpar evita reter bitmaps obsoletos e dobrar o uso de memoria.
        mPageCache.evictAll()

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
        mLocalCurrentPage = 0
        mStorage = Storage(requireContext())
        mLibrary = LibraryUtil.getDefault(requireContext(), Type.MANGA)
        mPreferences = GeneralConsts.getSharedPreferences(requireContext())
        mSubtitleController = SubTitleController.getInstance(requireContext())

        val bundle: Bundle? = arguments
        if (bundle != null && !bundle.isEmpty) {
            mLastPage.clear()

            mLibrary = BundleCompat.getSerializable(bundle, GeneralConsts.KEYS.OBJECT.LIBRARY, Library::class.java) as Library

            mManga = BundleCompat.getSerializable(bundle, GeneralConsts.KEYS.OBJECT.MANGA, Manga::class.java)
            val file: File? = if (mManga != null) {
                mManga?.file
                if (mManga?.file != null)
                    mManga?.file
                else
                    File(mManga?.path!!)
            } else
                BundleCompat.getSerializable(bundle, GeneralConsts.KEYS.OBJECT.FILE, File::class.java)

            mFileToParse = file

            if (file != null && file.exists()) {
                if (mManga == null)
                    mManga = mStorage.findMangaByName(file.name)

                if (mManga != null) {
                    mLocalCurrentPage = mManga!!.bookMark - 1
                    mStorage.updateLastAccess(mManga!!)
                }
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

            mPaginationType = PaginationType.valueOf(
                mPreferences.getString(
                    GeneralConsts.KEYS.READER.MANGA_PAGE_PAGINATION_TYPE,
                    PaginationType.Default.toString()
                ).toString()
            )

            mUseMagnifierType = mPreferences.getBoolean(GeneralConsts.KEYS.READER.MANGA_USE_MAGNIFIER_TYPE, false)
            mKeepZoomBetweenPage = mPreferences.getBoolean(GeneralConsts.KEYS.READER.MANGA_KEEP_ZOOM_BETWEEN_PAGES, false)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mSavedInstanceState = savedInstanceState
        val view: View = inflater.inflate(R.layout.fragment_manga_reader, container, false)

        mRoot = requireActivity().findViewById(R.id.root_activity_manga_reader)
        mToolbarTop = requireActivity().findViewById(R.id.reader_manga_toolbar_reader_top)
        mPopupSubtitleBottom = requireActivity().findViewById(R.id.popup_manga_translate_bottom_sheet)
        mPopupSubtitleLeft = requireActivity().findViewById(R.id.popup_manga_translate_side_sheet)
        mPopupColorBottom = requireActivity().findViewById(R.id.popup_manga_configurations_bottom_sheet)
        mPopupColorLeft = requireActivity().findViewById(R.id.popup_manga_configurations_side_sheet)
        mPageNavLayout = requireActivity().findViewById(R.id.reader_manga_bottom_progress_content)
        mToolbarBottom = requireActivity().findViewById(R.id.reader_manga_toolbar_reader_bottom)

        mBlurTop = requireActivity().findViewById(R.id.reader_manga_blur_top)
        mBlurBottom = requireActivity().findViewById(R.id.reader_manga_blur_bottom)
        mBlurProgress = requireActivity().findViewById(R.id.reader_manga_blur_progress)
        mBlurNavPrevious = requireActivity().findViewById(R.id.reader_manga_blur_nav_previous)
        mBlurNavNext = requireActivity().findViewById(R.id.reader_manga_blur_nav_next)

        mOriginalToolbarTopBg = mToolbarTop.background
        mOriginalToolbarBottomBg = mToolbarBottom.background
        mOriginalPageNavBg = mPageNavLayout.background

        mPreviousButton = requireActivity().findViewById(R.id.reader_manga_nav_previous_file)
        mNextButton = requireActivity().findViewById(R.id.reader_manga_nav_next_file)

        mOriginalNavPreviousTint = mPreviousButton.backgroundTintList
        mOriginalNavNextTint = mNextButton.backgroundTintList
        mViewPager = view.findViewById<View>(R.id.fragment_manga_reader_pager) as ImageViewPager
        mViewRecycler = view.findViewById<View>(R.id.fragment_manga_reader_recycler) as ZoomRecyclerView

        mCoverContent = view.findViewById(R.id.reader_manga_cover_content)
        mCoverImage = view.findViewById(R.id.reader_manga_cover)
        mCoverMessage = view.findViewById(R.id.reader_manga_cover_message)
        mCoverWarning = view.findViewById(R.id.reader_manga_cover_warning)

        mLastPageContainer = requireActivity().findViewById(R.id.reader_last_page)
        mLastPageImage = requireActivity().findViewById(R.id.last_page_image)
        mLastPageText = requireActivity().findViewById(R.id.last_page_text)

        mPopupBottomSheet = requireActivity().findViewById<ImageView>(R.id.popup_manga_translate_center_button) != null

        mTouchScreen = TouchUtils.getTouch(requireContext(), Type.MANGA)
        mLastPageContainer.visibility = View.GONE

        mCoverStartTime = System.currentTimeMillis()
        mPageStartReading = LocalDateTime.now()
        mPagesAverage = mutableListOf()

        (mPageNavLayout.findViewById<View>(R.id.reader_manga_bottom_progress) as DottedSeekBar).also {
            mPageSeekBar = it
        }
        mPageNavTextView = mPageNavLayout.findViewById<View>(R.id.reader_manga_bottom_progress_title) as TextView

        mCoverMessage.visibility = View.GONE
        mCoverWarning.visibility = View.GONE

        if (mManga != null) {
            MangaImageCoverController.instance.setImageCoverAsync(requireContext(), mManga!!, mCoverImage, null, true) {
                activity?.supportStartPostponedEnterTransition()
            }
            mHandler.postDelayed({ MangaImageCoverController.instance.setImageCoverAsync(requireContext(), mManga!!, mCoverImage, null, false) }, 300)
        } else {
            mCoverImage.setImageBitmap(ImageUtil.applyCoverEffect(requireContext(), null, Type.BOOK))
            mCoverMessage.text = getString(R.string.reading_manga_open_exception)
            mCoverMessage.visibility = View.VISIBLE
            mCoverWarning.visibility = View.VISIBLE
            activity?.supportStartPostponedEnterTransition()
        }

        mPageSeekBar.isEnabled = false

        val file = mFileToParse
        if (file != null && file.exists()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val parse = ParseFactory.create(file)
                    if (parse != null) {
                        if (parse is RarParse) {
                            // Pasta de cache estavel por arquivo: reaproveita a extracao entre
                            // sessoes (evita reextrair RAR solido) e invalida sozinha se o arquivo mudar.
                            val baseDir = File(GeneralConsts.getCacheDir(requireContext()), GeneralConsts.CACHE_FOLDER.RAR)
                            if (!baseDir.exists())
                                baseDir.mkdirs()
                            val folderName = RarParse.cacheFolderName(file)
                            val cacheDir = File(baseDir, folderName)
                            parse.setCacheDirectory(cacheDir, preserveExisting = true)
                            RarParse.trimCache(baseDir, folderName)
                        }

                        withContext(Dispatchers.Main) {
                            if (isAdded && context != null) {
                                mParse = parse
                                mSubtitleController.mReaderFragment = this@MangaReaderFragment
                                mFileName = file.name
                                mLocalCurrentPage = max(0, min(mLocalCurrentPage, parse.numPages()))
                                mComicHandler = MangaReaderHandler(parse)

                                if (mSavedInstanceState == null)
                                    mSubtitleController.getListChapter(mManga, parse)

                                setupMangaChaptersDots(parse)
                                prepareMangaReader()
                                requireActivity().invalidateOptionsMenu()
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            if (isAdded && context != null) showParseError()
                        }
                    }
                } catch (e: Exception) {
                    mLOGGER.error("Error parsing manga asynchronously", e)
                    withContext(Dispatchers.Main) {
                        if (isAdded && context != null) showParseError()
                    }
                }
            }
        } else {
            showParseError()
        }

        mViewModel.filters.observe(viewLifecycleOwner) { onRefresh() }

        setupBlurViews()
        setupWindowInsets()
        setupTitleBackgrounds()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear()
                menuInflater.inflate(R.menu.menu_reader_manga, menu)

                val isLoaded = mParse != null
                for (i in 0 until menu.size()) {
                    menu.getItem(i).isVisible = isLoaded
                }

                if (isLoaded) {
                    when (mReaderMode) {
                        ReaderMode.ASPECT_FILL -> menu.findItem(R.id.manga_view_mode_aspect_fill)?.isChecked = true
                        ReaderMode.ASPECT_FIT -> menu.findItem(R.id.manga_view_mode_aspect_fit)?.isChecked = true
                        ReaderMode.FIT_WIDTH -> menu.findItem(R.id.manga_view_mode_fit_width)?.isChecked = true
                    }

                    when (mScrollingMode) {
                        ScrollingType.Horizontal -> menu.findItem(R.id.reading_manga_scrolling_horizontal)?.isChecked = true
                        ScrollingType.HorizontalRightToLeft -> menu.findItem(R.id.reading_manga_scrolling_horizontal_right_to_left)?.isChecked = true
                        ScrollingType.Vertical -> menu.findItem(R.id.reading_manga_scrolling_vertical)?.isChecked = true
                        ScrollingType.Scrolling -> menu.findItem(R.id.reading_manga_scrolling_scrolling)?.isChecked = true
                        ScrollingType.ScrollingDivider -> menu.findItem(R.id.reading_manga_scrolling_scrolling_divider)?.isChecked = true
                        else -> menu.findItem(R.id.reading_manga_scrolling_horizontal)?.isChecked = true
                    }

                    when (mPaginationType) {
                        PaginationType.Default -> menu.findItem(R.id.reading_manga_pagination_default)?.isChecked = true
                        PaginationType.CurlPage -> menu.findItem(R.id.reading_manga_pagination_page_curl)?.isChecked = true
                        PaginationType.Curl3DPage -> menu.findItem(R.id.reading_manga_pagination_page_curl_3d)?.isChecked = true
                        PaginationType.Stack -> menu.findItem(R.id.reading_manga_pagination_stack)?.isChecked = true
                        PaginationType.Zooming -> menu.findItem(R.id.reading_manga_pagination_zoom)?.isChecked = true
                        PaginationType.Depth -> menu.findItem(R.id.reading_manga_pagination_depth)?.isChecked = true
                        PaginationType.Fade -> menu.findItem(R.id.reading_manga_pagination_fade)?.isChecked = true
                        else -> menu.findItem(R.id.reading_manga_pagination_default)?.isChecked = true
                    }

                    menu.findItem(R.id.menu_item_reader_manga_use_magnifier_type)?.isChecked = mUseMagnifierType
                    menu.findItem(R.id.menu_item_reader_manga_keep_zoom_between_pages)?.isChecked = mKeepZoomBetweenPage
                    menu.findItem(R.id.menu_item_reader_manga_show_clock_and_battery)?.isChecked =
                        mPreferences.getBoolean(GeneralConsts.KEYS.READER.MANGA_SHOW_CLOCK_AND_BATTERY, false)

                    menu.findItem(R.id.menu_item_reader_manga_mark_page)?.let { miMarkPage = it }
                }
            }

            override fun onPrepareMenu(menu: Menu) {
                menu.findItem(R.id.menu_item_reader_manga_favorite)?.let { item ->
                    (activity as? MangaReaderActivity)?.applyFavoriteMenuIcon(item)
                }
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean {
                return when (item.itemId) {
                    R.id.menu_item_reader_manga_favorite -> {
                        (activity as? MangaReaderActivity)?.changeFavorite(item)
                        true
                    }

                    R.id.manga_view_mode_aspect_fill, R.id.manga_view_mode_aspect_fit, R.id.manga_view_mode_fit_width -> {
                        item.isChecked = true
                        mReaderMode = mResourceViewMode[item.itemId] ?: ReaderMode.FIT_WIDTH
                        changeAspect()
                        true
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
                        configureScrolling(scrolling, mPaginationType)
                        true
                    }

                    R.id.reading_manga_pagination_default,
                    R.id.reading_manga_pagination_page_curl,
                    R.id.reading_manga_pagination_page_curl_3d,
                    R.id.reading_manga_pagination_stack,
                    R.id.reading_manga_pagination_zoom,
                    R.id.reading_manga_pagination_depth,
                    R.id.reading_manga_pagination_fade,
                        -> {
                        item.isChecked = true

                        val pagination = when (item.itemId) {
                            R.id.reading_manga_pagination_default -> PaginationType.Default
                            R.id.reading_manga_pagination_page_curl -> PaginationType.CurlPage
                            R.id.reading_manga_pagination_page_curl_3d -> PaginationType.Curl3DPage
                            R.id.reading_manga_pagination_stack -> PaginationType.Stack
                            R.id.reading_manga_pagination_zoom -> PaginationType.Zooming
                            R.id.reading_manga_pagination_depth -> PaginationType.Depth
                            R.id.reading_manga_pagination_fade -> PaginationType.Fade
                            else -> PaginationType.Default
                        }
                        with(mPreferences.edit()) {
                            this.putString(GeneralConsts.KEYS.READER.MANGA_PAGE_PAGINATION_TYPE, pagination.toString())
                            this.commit()
                        }
                        configurePagination(pagination)
                        true
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
                            updatePageViews<BaseImageView>(mViewPager, BaseImageView::class.java) {
                                (it as ImageViewPage).useMagnifierType = mUseMagnifierType
                            }
                        }
                        true
                    }

                    R.id.menu_item_reader_manga_keep_zoom_between_pages -> {
                        item.isChecked = !item.isChecked
                        mKeepZoomBetweenPage = item.isChecked

                        with(mPreferences.edit()) {
                            this.putBoolean(GeneralConsts.KEYS.READER.MANGA_KEEP_ZOOM_BETWEEN_PAGES, mKeepZoomBetweenPage)
                            this.commit()
                        }
                        true
                    }

                    R.id.menu_item_reader_manga_save_share_image -> {
                        openPopupSaveShareImage()
                        true
                    }

                    R.id.menu_item_reader_manga_mark_page -> {
                        markCurrentPage()
                        if (::miMarkPage.isInitialized) {
                            (miMarkPage.icon as? AnimatedVectorDrawable)?.let { icon ->
                                icon.reset()
                                icon.start()
                            }
                        }
                        true
                    }

                    R.id.menu_item_reader_manga_config_touch_screen -> {
                        openTouchFunctions()
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    fun syncMangaFavorite(favorite: Boolean) {
        mManga?.favorite = favorite
    }

    private fun setupMangaChaptersDots(parse: Parse) {
        val dots = mutableListOf<Int>()
        val inverse = mutableListOf<Int>()

        val pages = parse.numPages() - 1
        for (chapter in parse.getChapters() ?: intArrayOf()) {
            inverse.add(pages - chapter)
            dots.add(chapter)
        }

        try {
            (requireActivity() as MangaReaderActivity).setMangaDots(dots, inverse)
        } catch (e: Exception) {
            mLOGGER.error("Error to set dots: " + e.message, e)
        }
    }

    private fun showParseError() {
        (requireActivity() as MangaReaderActivity).setMangaDots(mutableListOf(), mutableListOf())
        mCoverMessage.visibility = View.VISIBLE
        mCoverWarning.visibility = View.VISIBLE
        val cover = if (mManga != null) MangaImageCoverController.instance.getMangaCover(requireContext(), mManga!!, isCoverSize = true) else null
        mCoverImage.setImageBitmap(ImageUtil.applyCoverEffect(requireContext(), cover, Type.BOOK))
        mCoverMessage.text = getString(R.string.reading_manga_open_exception)
        mPageNavTextView.text = ""
        setFullscreen(false)
        requireActivity().invalidateOptionsMenu()
        activity?.supportStartPostponedEnterTransition()
    }

    private fun prepareMangaReader() {
        val parse = mParse ?: return
        mPageSeekBar.isEnabled = true
        mPageSeekBar.max = parse.numPages() - 1
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
                setReaderBlurContinuous(true)

                try {
                    val view = getCurrencyImageView() ?: return

                    val page = seekBar.progress + 1
                    if (mLastPage.any { it.first == page })
                        return

                    if (mLastPage.size > 3)
                        mLastPage.removeLast()

                    val bitmap = (view.drawable as BitmapDrawable).bitmap
                    mLastPage.addFirst(Pair(page, bitmap.copy(bitmap.config, true)))
                    updateDotsLastPage()
                    openLastPage()
                } catch (e: Exception) {
                    mLOGGER.error("Error to insert last page: " + e.message, e)
                    Telemetry.recordException(e, "Error to insert last page: " + e.message)
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                mIsSeekBarChange = false
                setReaderBlurContinuous(false)
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
            updateDotsLastPage()
        }

        configureScrolling(mScrollingMode, mPaginationType, true)

        val savedState = mSavedInstanceState
        if (savedState != null) {
            val fullscreen = savedState.getBoolean(ReaderConsts.STATES.STATE_FULLSCREEN)
            setFullscreen(fullscreen)
            val newComicId = savedState.getLong(ReaderConsts.STATES.STATE_NEW_COMIC)
            val titleRes = savedState.getInt(ReaderConsts.STATES.STATE_NEW_COMIC_TITLE)
            confirmSwitch(mStorage.getManga(newComicId), titleRes)
        } else
            setFullscreen(true)

        mManga?.let { generateHistory(it) }
        requireActivity().title = mFileName
        updateSeekBar()

        var run: Runnable? = null
        run = Runnable {
            val image = getCurrencyImageView()
            val elapsedTime = System.currentTimeMillis() - mCoverStartTime

            if (image == null || image.isGone || elapsedTime < GeneralConsts.DEFAULTS.DEFAULT_COVER_DELAY) {
                val nextCheckDelay = if (elapsedTime < GeneralConsts.DEFAULTS.DEFAULT_COVER_DELAY) GeneralConsts.DEFAULTS.DEFAULT_COVER_DELAY - elapsedTime else 300L
                mHandler.postDelayed(run!!, nextCheckDelay)
            } else {
                mCoverContent.animate().alpha(0.0f).setDuration(600L).setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        mCoverContent.visibility = View.GONE

                        if (mPreferences.getBoolean(GeneralConsts.KEYS.TOUCH.MANGA_TOUCH_DEMONSTRATION, true)) {
                            with(mPreferences.edit()) {
                                this.putBoolean(GeneralConsts.KEYS.TOUCH.MANGA_TOUCH_DEMONSTRATION, false)
                                this.commit()
                            }
                            (requireActivity() as MangaReaderActivity).openViewTouch()
                        }
                    }
                })
            }
        }

        mHandler.postDelayed(run, 500)
    }

    private fun configureScrolling(scrolling: ScrollingType, pagination: PaginationType, isInitial: Boolean = false) {
        val page = if (isInitial) mLocalCurrentPage + 1 else getCurrentPage()
        val isChange = isInitial || ((scrolling == ScrollingType.Scrolling || scrolling == ScrollingType.ScrollingDivider) && mViewPager.isVisible) ||
                ((scrolling == ScrollingType.Horizontal || scrolling == ScrollingType.HorizontalRightToLeft || scrolling == ScrollingType.Vertical) && mViewRecycler.isVisible)

        val isMode = ((mScrollingMode == ScrollingType.HorizontalRightToLeft || scrolling == ScrollingType.HorizontalRightToLeft) && mScrollingMode != scrolling)
        mScrollingMode = scrolling
        mPaginationType = pagination

        if (isChange) {
            mCurrentFragment = null
            when (mScrollingMode) {
                ScrollingType.Horizontal,
                ScrollingType.HorizontalRightToLeft,
                ScrollingType.Vertical,
                ScrollingType.Pagination,
                ScrollingType.PaginationVertical,
                ScrollingType.PaginationRightToLeft -> {
                    mViewPager.setSwipeOrientation(mScrollingMode, mPaginationType)
                    mViewPager.adapter = ComicPagerAdapter()
                    mViewPager.offscreenPageLimit = ReaderConsts.READER.MANGA_OFF_SCREEN_PAGE_LIMIT
                    mViewPager.setOnTouchListener(this@MangaReaderFragment)
                    mViewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
                        override fun onPageSelected(position: Int) {
                            if (!isAdded || isRemoving || isDetached) return
                            if (mScrollingMode == ScrollingType.HorizontalRightToLeft)
                                setCurrentPage(mViewPager.adapter!!.count - position)
                            else
                                setCurrentPage(position + 1)

                            generatePageAverage()
                        }

                        override fun onPageScrollStateChanged(state: Int) {
                            when (state) {
                                ViewPager.SCROLL_STATE_DRAGGING, ViewPager.SCROLL_STATE_SETTLING ->
                                    setReaderBlurContinuous(true)
                                ViewPager.SCROLL_STATE_IDLE ->
                                    setReaderBlurContinuous(false)
                            }
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
                    mViewRecycler.onZoomInteractionChanged = null
                    mViewRecycler.setOnSwipeOutListener(null)

                    mViewRecycler.adapter = null
                    mViewRecycler.layoutManager = null

                    mViewPager.visibility = View.VISIBLE
                    mViewRecycler.visibility = View.GONE

                    if (page != -1 && !isMode)
                        setCurrentPage(page)
                }

                ScrollingType.Scrolling,
                ScrollingType.ScrollingDivider,
                    -> {
                    mViewPager.setSwipeOrientation(mScrollingMode, mPaginationType)
                    mViewPager.adapter = null
                    mViewPager.setOnTouchListener(null)
                    mViewPager.clearOnPageChangeListeners()
                    mViewPager.setOnSwipeOutListener(null)

                    mViewRecycler.setOnTouchListener(this@MangaReaderFragment)
                    mViewRecycler.addOnScrollListener(ComicRecyclerListener())
                    mViewRecycler.onZoomInteractionChanged = { active -> setReaderBlurContinuous(active) }
                    mViewRecycler.setOnSwipeOutListener(object : ZoomRecyclerView.OnSwipeOutListener {
                        override fun onSwipeOutAtStart() = hitBeginning()
                        override fun onSwipeOutAtEnd() = hitEnding()
                    })

                    mViewRecycler.adapter = ComicRecyclerAdapter()
                    mViewRecycler.layoutManager = LinearLayoutManager(requireContext())
                    mViewRecycler.setItemViewCacheSize(ReaderConsts.READER.MANGA_OFF_SCREEN_PAGE_LIMIT)
                    mViewRecycler.useMagnifierType = mUseMagnifierType
                    mViewRecycler.isEnableZoom = true

                    mViewRecycler.adapter?.notifyDataSetChanged()
                    if (page != -1) {
                        mViewRecycler.layoutManager?.scrollToPosition(page - 1)
                        setChangeProgress(page, page)
                    }

                    mViewPager.visibility = View.GONE
                    mViewRecycler.visibility = View.VISIBLE
                }
            }
        } else
            when (mScrollingMode) {
                ScrollingType.Horizontal,
                ScrollingType.HorizontalRightToLeft,
                ScrollingType.Vertical,
                    -> {
                    mViewPager.setSwipeOrientation(mScrollingMode, mPaginationType)
                }

                ScrollingType.Scrolling,
                ScrollingType.ScrollingDivider,
                    -> mViewRecycler.adapter?.notifyDataSetChanged()

                else -> {}
            }

        if (isMode) {
            setCurrentPage(page, false)
            mViewPager.adapter?.notifyDataSetChanged()
            updateSeekBar()
        }
    }

    private fun configurePagination(pagination: PaginationType) {
        if (mPaginationType == pagination)
            return

        mPaginationType = pagination

        mViewPager.setSwipeOrientation(mScrollingMode, mPaginationType)
        if (mScrollingMode in setOf(ScrollingType.Vertical, ScrollingType.Horizontal, ScrollingType.HorizontalRightToLeft)) {
            val isCurl = mPaginationType == PaginationType.CurlPage || mPaginationType == PaginationType.Curl3DPage
            updatePageViews<PageCurlFrame>(mViewPager, PageCurlFrame::class.java) {
                (it as PageCurlFrame).isCurlPage = isCurl
                (it as PageCurlFrame).is3DMode = mPaginationType == PaginationType.Curl3DPage
            }
        }
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
            it.setPageEnd(getCurrentPage())
            it.setEnd(LocalDateTime.now())
            it.id = mViewModel.save(it)
        }
        setBlurAutoUpdate(false)
        super.onPause()
    }

    override fun onDestroy() {
        mViewModel.stopExecutions()
        mSubtitleController.clearControllers()
        if (mSubtitleController.mReaderFragment == this)
            mSubtitleController.mReaderFragment = null
        // Preserva o cache de disco do RAR entre sessoes (o trimCache limita o total).
        Util.destroyParse(mParse, isClearCache = false)

        mPageCache.evictAll()
        mHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        v.performClick()
        return mGestureDetector.onTouchEvent(event)
    }


    private val touchConfigurationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        mTouchScreen = TouchUtils.getTouch(requireContext(), Type.MANGA)
    }

    private fun openTouchFunctions() {
        val intent = Intent(requireContext(), MenuActivity::class.java)
        val bundle = Bundle()
        bundle.putInt(GeneralConsts.KEYS.FRAGMENT.ID, R.id.frame_touch_screen_config)
        bundle.putSerializable(GeneralConsts.KEYS.OBJECT.TYPE, Type.MANGA)
        bundle.putSerializable(GeneralConsts.KEYS.OBJECT.MANGA, mManga!!)
        intent.putExtras(bundle)
        requireActivity().overrideActivityTransitionCompat(R.anim.fade_in_fragment_add_enter, R.anim.fade_out_fragment_remove_exit)
        touchConfigurationLauncher.launch(intent)
    }

    fun getCurrentPage(isInternal: Boolean = false): Int {
        if (!::mViewPager.isInitialized || !::mViewRecycler.isInitialized)
            return 1

        return when (mScrollingMode) {
            ScrollingType.Vertical,
            ScrollingType.Horizontal,
                -> if (isInternal) mViewPager.currentItem else mViewPager.currentItem.plus(1)

            ScrollingType.HorizontalRightToLeft -> {
                if (mViewPager.adapter != null) {
                    if (isInternal)
                        (mViewPager.adapter!!.count - mViewPager.currentItem).minus(1)
                    else
                        (mViewPager.adapter!!.count - mViewPager.currentItem)
                } else
                    1
            }
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
            updatePageViews<BaseImageView>(mViewPager, BaseImageView::class.java) {
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
                mViewPager.setCurrentItem(page - 1, animated)
            }

            ScrollingType.HorizontalRightToLeft -> {
                mViewPager.setCurrentItem(mViewPager.adapter!!.count - page, animated)
                seek = mViewPager.adapter!!.count - page
            }

            ScrollingType.Scrolling,
            ScrollingType.ScrollingDivider,
                -> {
                val isShort = abs(mLocalCurrentPage - page) < 6
                if (animated && isShort)
                    mViewRecycler.smoothScrollToPosition(page - 1)
                else {
                    mViewRecycler.adapter?.notifyDataSetChanged()
                    mViewRecycler.layoutManager?.scrollToPosition(page - 1)
                }
            }

            else -> return
        }

        setChangeProgress(page, seek)
    }

    private fun setChangeProgress(page: Int, seekbar: Int) {
        if (!isAdded || isRemoving || isDetached) return
        mPageSeekBar.progress = seekbar

        val navPage: String = if (mParse == null) "" else StringBuilder()
            .append(page).append("/").append(mParse?.numPages() ?: 1)
            .toString()

        mPageNavTextView.text = navPage
        mLocalCurrentPage = page - 1

        if (mLocalCurrentPage < 0)
            mLocalCurrentPage = 0

        if (mManga != null)
            mSubtitleController.changeSubtitleInReader(mManga!!, mLocalCurrentPage)

        (requireActivity() as MangaReaderActivity).changePage(mManga?.title ?: "", getChapterSelected(mLocalCurrentPage), page)
    }

    private var mComicInfoProcessed = false

    private fun getChapterSelected(page: Int): String {
        var chapter = ""

        val manga = mManga
        val parse = mParse
        if (manga != null && manga.chaptersPages.isEmpty() && !mComicInfoProcessed && parse != null && parse.isComicInfo()) {
            // Executa a leitura do ComicInfo apenas uma vez por abertura e fora da main thread,
            // pois getComicInfo() pode reparsear o arquivo. Ao concluir, atualiza o titulo.
            mComicInfoProcessed = true
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val info = parse.getComicInfo() ?: return@launch
                    val chapters = mutableMapOf<Int, String>()
                    info.pages?.let { pages ->
                        for ((index, comic) in pages.withIndex()) {
                            if (comic.bookmark != null)
                                chapters[index] = comic.bookmark!!
                        }
                    }
                    if (chapters.isNotEmpty()) {
                        manga.chaptersPages = chapters
                        mViewModel.save(manga)
                        withContext(Dispatchers.Main) {
                            if (isAdded && !isRemoving && !isDetached)
                                (requireActivity() as MangaReaderActivity)
                                    .changePage(manga.title, getChapterSelected(mLocalCurrentPage), mLocalCurrentPage + 1)
                        }
                    }
                } catch (e: Exception) {
                    mLOGGER.error("Error to load comic info chapters: " + e.message, e)
                }
            }
        }

        if (manga != null && manga.chaptersPages.isNotEmpty()) {
            var last = manga.chaptersPages.keys.first()
            for (chapterKey in manga.chaptersPages.keys) {
                if (chapterKey > page)
                    break
                last = chapterKey
            }

            chapter = manga.chaptersPages[last] ?: ""
        }

        if (chapter.isEmpty())
            chapter = parse?.getPagePath(page) ?: ""

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
            
            val isCurl = mPaginationType == PaginationType.CurlPage || mPaginationType == PaginationType.Curl3DPage
            val curlFrame = layout.findViewById<PageCurlFrame>(R.id.frame_reader_page_root)
            curlFrame.isCurlPage = isCurl
            curlFrame.is3DMode = mPaginationType == PaginationType.Curl3DPage

            val imageViewPage: ImageViewPage = layout.findViewById<View>(R.id.page_image_view) as ImageViewPage
            if (mReaderMode === ReaderMode.ASPECT_FILL)
                imageViewPage.setTranslateToRightEdge(mScrollingMode == ScrollingType.HorizontalRightToLeft)
            imageViewPage.setViewMode(mReaderMode)
            imageViewPage.useMagnifierType = mUseMagnifierType
            imageViewPage.onZoomInteractionChanged = { active -> setReaderBlurContinuous(active) }
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
            mTargets.delete(position)
            container.removeView(layout)
            // Nao reciclar o bitmap aqui: ele pode estar retido pelo mPageCache (LRU)
            // para reutilizacao ao voltar a pagina. O LRU limita o uso de memoria e o
            // GC libera os bitmaps evictados que nao estejam mais em uso.
            val iv = layout.findViewById<View>(R.id.page_image_view) as ImageView
            iv.setImageDrawable(null)
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
                    dividerPage.text = "${position +1} / $itemCount"
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
            when (newState) {
                RecyclerView.SCROLL_STATE_DRAGGING, RecyclerView.SCROLL_STATE_SETTLING ->
                    setReaderBlurContinuous(true)
                RecyclerView.SCROLL_STATE_IDLE -> {
                    setReaderBlurContinuous(false)
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
    }

    fun loadImage(t: MyTarget, pages: Int) {
        val pos: Int = if (mScrollingMode == ScrollingType.HorizontalRightToLeft)
            pages - t.position - 1
        else
            t.position

        val key = pageCacheKey(pos)
        t.cacheKey = key

        val cached = mPageCache.get(key)
        if (cached != null && !cached.isRecycled) {
            t.onBitmapLoaded(cached, true)
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                var bitmap = mComicHandler.loadPage(pos)
                if (bitmap != null) {
                    val filters = mViewModel.filters.value ?: emptyList()
                    for (filter in filters) {
                        bitmap = filter.transform(bitmap!!, coil.size.Size.ORIGINAL)
                    }
                    val finalBitmap = bitmap
                    withContext(Dispatchers.Main) {
                        t.onBitmapLoaded(finalBitmap!!, false)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        t.onBitmapFailed(Exception("Failed to decode image at page $pos"), null)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    t.onBitmapFailed(e, null)
                }
            }
        }
    }

    inner class MyTarget(layout: View, val position: Int, val onLoaded: (View) -> (Unit)) : View.OnClickListener {
        private val mLayout: WeakReference<View> = WeakReference(layout)
        var cacheKey: String? = null

        private fun setVisibility(imageView: Int, progressBar: Int, reloadButton: Int) {
            val layout = mLayout.get() ?: return
            layout.findViewById<View>(R.id.page_image_view).visibility = imageView
            layout.findViewById<View>(R.id.load_progress_bar).visibility = progressBar
            layout.findViewById<View>(R.id.reload_button).visibility = reloadButton
        }

        fun onBitmapLoaded(bitmap: Bitmap, isFromMemory: Boolean = false) {
            val layout = mLayout.get() ?: return
            setVisibility(View.VISIBLE, View.GONE, View.GONE)
            val iv = layout.findViewById<View>(R.id.page_image_view) as ImageView
            iv.setImageBitmap(bitmap)

            if (!isFromMemory && !bitmap.isRecycled)
                cacheKey?.let { mPageCache.put(it, bitmap) }

            onLoaded(layout)
        }

        fun onBitmapFailed(e: Exception, errorDrawable: Drawable?) {
            mLOGGER.error("Bitmap load fail: " + e.message, e)
            Telemetry.recordException(e, "Bitmap load fail: " + e.message)
            val layout = mLayout.get() ?: return
            setVisibility(View.GONE, View.GONE, View.VISIBLE)
            val ib = layout.findViewById<View>(R.id.reload_button) as ImageButton
            ib.setOnClickListener(this@MyTarget)
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
                        val isBack = if (mScrollingMode == ScrollingType.HorizontalRightToLeft) touch == TouchScreen.TOUCH_NEXT_PAGE else touch == TouchScreen.TOUCH_PREVIOUS_PAGE
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

    private fun <T : Any> updatePageViews(parentView: ViewGroup, item: Class<T>, change: (Any) -> (Unit)) {
        for (i in 0 until parentView.childCount) {
            val child = parentView.getChildAt(i)
            if (item.isInstance(child))
                change(child)
            else if (child is ViewGroup)
                updatePageViews(child, item, change)
        }
    }

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
            mHandler.postDelayed({
                WindowCompat.setDecorFitsSystemWindows(window, false)
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                windowInsetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

                if (mPopupBottomSheet) {
                    if (mPopupSubtitleBottom!!.visibility != View.GONE)
                        AnimationUtil.animatePopupClose(requireActivity(), mPopupSubtitleBottom!!, mPopupBottomSheet, navigationColor = false)

                    if (mPopupColorBottom!!.visibility != View.GONE)
                        AnimationUtil.animatePopupClose(requireActivity(), mPopupColorBottom!!, mPopupBottomSheet, navigationColor = false)
                } else {
                    if (mPopupSubtitleLeft!!.visibility != View.GONE)
                        AnimationUtil.animatePopupClose(requireActivity(), mPopupSubtitleLeft!!, mPopupBottomSheet, navigationColor = false)

                    if (mPopupColorLeft!!.visibility != View.GONE)
                        AnimationUtil.animatePopupClose(requireActivity(), mPopupColorLeft!!, mPopupBottomSheet, navigationColor = false)
                }
            }, ANIMATION_DURATION)
        } else {
            mHandler.postDelayed({ changeContentsVisibility(fullscreen) }, ANIMATION_DURATION)

            WindowCompat.setDecorFitsSystemWindows(window, false)
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())

            val isNight = resources.getBoolean(R.bool.isNight)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
                window.isStatusBarContrastEnforced = false
            }
            WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = !isNight
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

        val targetTop = mBlurTop ?: mToolbarTop
        val targetBottom = mBlurBottom ?: mToolbarBottom
        val targetProgress = mBlurProgress ?: mPageNavLayout
        val prevTarget = mBlurNavPrevious ?: mPreviousButton
        val nextTarget = mBlurNavNext ?: mNextButton

        setupTitleBackgrounds()

        val isErrorState = mParse == null
        val targetBottomVisibility = if (isFullScreen || isErrorState) View.GONE else View.VISIBLE

        if (!isFullScreen) {
            setReaderBlurContinuous(true)
            targetTop.visibility = View.VISIBLE
            targetTop.translationY = initialTranslation
            targetTop.alpha = initialAlpha

            val bottomVis = if (isErrorState) View.GONE else View.VISIBLE
            targetBottom.visibility = bottomVis
            targetBottom.translationY = (initialTranslation * -1)
            targetBottom.alpha = initialAlpha

            targetProgress.visibility = bottomVis
            targetProgress.translationY = (initialTranslation * -1)
            targetProgress.alpha = initialAlpha

            nextTarget.visibility = bottomVis
            nextTarget.translationY = (initialTranslation * -1)
            nextTarget.alpha = initialAlpha

            prevTarget.visibility = bottomVis
            prevTarget.translationY = (initialTranslation * -1)
            prevTarget.alpha = initialAlpha
        }

        val interpolator = if (isFullScreen) AccelerateInterpolator(2.0f) else DecelerateInterpolator(2.0f)

        targetTop.animate().alpha(finalAlpha).translationY(finalTranslation)
            .setDuration(ANIMATION_DURATION).setInterpolator(interpolator)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    targetTop.visibility = visibility
                    if (!isFullScreen)
                        setReaderBlurContinuous(false)
                }
            })

        if (isErrorState) {
            targetBottom.visibility = View.GONE
            targetProgress.visibility = View.GONE
            nextTarget.visibility = View.GONE
            prevTarget.visibility = View.GONE
        } else {
            targetBottom.animate().alpha(finalAlpha).translationY(finalTranslation * -1)
                .setDuration(ANIMATION_DURATION).setInterpolator(interpolator)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        targetBottom.visibility = targetBottomVisibility
                    }
                })

            targetProgress.animate().alpha(finalAlpha).translationY(finalTranslation * -1)
                .setDuration(ANIMATION_DURATION).setInterpolator(interpolator)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        targetProgress.visibility = targetBottomVisibility
                    }
                })

            nextTarget.animate().alpha(finalAlpha).translationY(finalTranslation * -1)
                .setDuration(ANIMATION_DURATION).setInterpolator(interpolator)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        nextTarget.visibility = targetBottomVisibility
                    }
                })

            prevTarget.animate().alpha(finalAlpha).translationY(finalTranslation * -1)
                .setDuration(ANIMATION_DURATION).setInterpolator(interpolator)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        prevTarget.visibility = targetBottomVisibility
                    }
                })
        }
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
                    it.setPageEnd(getCurrentPage())
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
        val seekRes: Int = if (mScrollingMode == ScrollingType.HorizontalRightToLeft) R.drawable.reader_progress_pointer_inverse else R.drawable.reader_progress_pointer
        val drawable: Drawable? = ContextCompat.getDrawable(requireActivity(), seekRes)
        val bounds = mPageSeekBar.progressDrawable.bounds
        mPageSeekBar.progressDrawable = drawable
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
        val page = getCurrentPage()
        mParse?.getPage(getCurrentPage(isInternal = true))?.let {
            val os: OutputStream
            try {
                val fileName = (mManga?.name ?: page.toString()) + ".jpeg"
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
                val bitmap = br.com.fenix.bilingualreader.util.helpers.ImageUtil.decodeInputStream(it)
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
                mLOGGER.error("Error generate image to share: " + e.message, e)
                Telemetry.recordException(e, "Error generate image to share: " + e.message)
            } finally {
                Util.closeInputStream(it)
            }
        }
    }

    private fun markCurrentPage() {
        val msg: String

        val page = getCurrentPage()
        val current = getCurrentPage(isInternal = true)
        val mark = mViewModel.findAnnotationByPage(mManga!!, page).find { it.markType == MarkType.PageMark }
        if (mark != null) {
            mViewModel.delete(mark)
            msg = getString(R.string.manga_annotation_page_unmarked, page)
        } else {
            val path = mParse!!.getPagePath(current) ?: ""
            val chapter = getChapterSelected(current)
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
            it.setPageEnd(getCurrentPage())
            it.setEnd(LocalDateTime.now())
            it.averageTimeByPage = average
            saveHistoryAsync(it)
        }
    }

    private val mHistorySaveLock = Any()
    private fun saveHistoryAsync(history: History) {
        CoroutineScope(Dispatchers.IO).launch {
            // Serializa as gravacoes para garantir que o id seja definido antes da proxima,
            // evitando insercao duplicada de historico entre viradas de pagina rapidas.
            synchronized(mHistorySaveLock) {
                history.id = mViewModel.save(history)
            }
        }
    }

    private fun updateDotsLastPage() {
        val pages = mPageSeekBar.max + 1
        val dots = mutableListOf<Int>()
        val inverse = mutableListOf<Int>()

        for (page in mLastPage) {
            inverse.add(pages - page.first)
            dots.add(page.first)
        }

        mPageSeekBar.setSecondaryDots(dots.toIntArray(), inverse.toIntArray())
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

    override fun onResume() {
        super.onResume()
        Companion.mCurrentPage = mLocalCurrentPage
        refreshReaderBlur()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden)
            setBlurAutoUpdate(false)
        else
            refreshReaderBlur()
    }

    private fun setReaderBlurContinuous(active: Boolean) {
        if (!isAdded || !::mBlurTop.isInitialized)
            return
        if (mIsFullscreen)
            return
        setBlurAutoUpdate(active)
    }

    private fun refreshReaderBlur() {
        if (!isAdded || !::mBlurTop.isInitialized)
            return
        setupTitleBackgrounds()
        // Idle: on-demand mode + one-shot refresh (do not keep continuous forever)
        setBlurAutoUpdate(false)
        if (mPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false) && !mIsFullscreen) {
            mBlurTop.blurOnceDeferred(mHandler, 50)
            mBlurBottom.blurOnceDeferred(mHandler, 50)
            mBlurProgress.blurOnceDeferred(mHandler, 50)
            mBlurNavPrevious.blurOnceDeferred(mHandler, 50)
            mBlurNavNext.blurOnceDeferred(mHandler, 50)
        }
    }

    private fun setBlurAutoUpdate(enabled: Boolean) {
        if (!::mBlurTop.isInitialized || !::mBlurBottom.isInitialized || !::mBlurProgress.isInitialized || !::mBlurNavPrevious.isInitialized || !::mBlurNavNext.isInitialized)
            return

        val isGlass = mPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)
        val autoUpdate = isGlass && enabled

        mBlurTop.setBlurEnabled(isGlass)
        mBlurBottom.setBlurEnabled(isGlass)
        mBlurProgress.setBlurEnabled(isGlass)
        mBlurNavPrevious.setBlurEnabled(isGlass)
        mBlurNavNext.setBlurEnabled(isGlass)

        mBlurTop.setBlurAutoUpdate(autoUpdate)
        mBlurBottom.setBlurAutoUpdate(autoUpdate)
        mBlurProgress.setBlurAutoUpdate(autoUpdate)
        mBlurNavPrevious.setBlurAutoUpdate(autoUpdate)
        mBlurNavNext.setBlurAutoUpdate(autoUpdate)

        if (isGlass && !enabled) {
            GlassRenderScheduler.requestUpdate(mBlurTop)
            GlassRenderScheduler.requestUpdate(mBlurBottom)
            GlassRenderScheduler.requestUpdate(mBlurProgress)
            GlassRenderScheduler.requestUpdate(mBlurNavPrevious)
            GlassRenderScheduler.requestUpdate(mBlurNavNext)
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(mToolbarTop) { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(view.paddingLeft, statusBarHeight, view.paddingRight, view.paddingBottom)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(mToolbarBottom) { view, insets ->
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, navBarHeight)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(mBlurProgress) { view, insets ->
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val lp = view.layoutParams as ViewGroup.MarginLayoutParams
            lp.bottomMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80f, resources.displayMetrics).toInt() + navBarHeight
            view.layoutParams = lp
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(mBlurNavPrevious) { view, insets ->
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val lp = view.layoutParams as ViewGroup.MarginLayoutParams
            lp.bottomMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 85f, resources.displayMetrics).toInt() + navBarHeight
            view.layoutParams = lp
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(mBlurNavNext) { view, insets ->
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val lp = view.layoutParams as ViewGroup.MarginLayoutParams
            lp.bottomMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 85f, resources.displayMetrics).toInt() + navBarHeight
            view.layoutParams = lp
            insets
        }
    }

    private fun setupTitleBackgrounds() {
        if (!::mBlurTop.isInitialized || !::mBlurBottom.isInitialized || !::mBlurProgress.isInitialized || !::mBlurNavPrevious.isInitialized || !::mBlurNavNext.isInitialized)
            return

        val isGlass = mPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)
        val context = requireContext()
        val themeColor = context.getColorFromAttr(R.attr.colorSurface)

        mBlurTop.setBlurEnabled(isGlass)
        mBlurBottom.setBlurEnabled(isGlass)
        mBlurProgress.setBlurEnabled(isGlass)
        mBlurNavPrevious.setBlurEnabled(isGlass)
        mBlurNavNext.setBlurEnabled(isGlass)

        val isNight = resources.getBoolean(R.bool.isNight)
        val alpha = if (isNight) 0xD9 else 0x73
        val translucentColor = (themeColor and 0x00FFFFFF) or (alpha shl 24)

        val topBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(translucentColor)
        }
        mBlurTop.background = topBg
        mToolbarTop.background = android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)

        val bottomBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(translucentColor)
        }
        mBlurBottom.background = bottomBg
        mToolbarBottom.background = android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
        val progressCornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, resources.displayMetrics)
        val buttonCornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100f, resources.displayMetrics)

        val progressBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(translucentColor)
            this.cornerRadius = progressCornerRadius
        }
        val prevButtonBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(translucentColor)
            this.cornerRadius = buttonCornerRadius
        }
        val nextButtonBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(translucentColor)
            this.cornerRadius = buttonCornerRadius
        }

        if (isGlass) {
            mBlurProgress.background = progressBg
            mPageNavLayout.background = android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            mBlurNavPrevious.background = prevButtonBg
            mBlurNavNext.background = nextButtonBg
            mPreviousButton.backgroundTintList = ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
            mNextButton.backgroundTintList = ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
        } else {
            mBlurProgress.background = android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            mPageNavLayout.background = progressBg
            mBlurNavPrevious.background = android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            mBlurNavNext.background = android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            mPreviousButton.background = prevButtonBg
            mPreviousButton.backgroundTintList = null
            mNextButton.background = nextButtonBg
            mNextButton.backgroundTintList = null
        }

        if (!mIsFullscreen) {
            val window = requireActivity().window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
                window.isStatusBarContrastEnforced = false
            }
            WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = !isNight
        }
    }

    private fun setupBlurViews() {
        if (!::mBlurTop.isInitialized || !::mBlurBottom.isInitialized || !::mBlurProgress.isInitialized || !::mBlurNavPrevious.isInitialized || !::mBlurNavNext.isInitialized)
            return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
            return

        val decorView = requireActivity().window.decorView
        val background = decorView.background ?: android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK)
        val rootView = decorView.findViewById<ViewGroup>(android.R.id.content)

        GlassSetup.setupGlass(mBlurTop, rootView, RenderEffectBlur())
            .setFrameClearDrawable(background)
            .setBlurRadius(15f)

        GlassSetup.setupGlass(mBlurBottom, rootView, RenderEffectBlur())
            .setFrameClearDrawable(background)
            .setBlurRadius(15f)

        GlassSetup.setupGlass(mBlurProgress, rootView, RenderEffectBlur())
            .setFrameClearDrawable(background)
            .setBlurRadius(15f)

        GlassSetup.setupGlass(mBlurNavPrevious, rootView, RenderEffectBlur())
            .setFrameClearDrawable(background)
            .setBlurRadius(15f)

        GlassSetup.setupGlass(mBlurNavNext, rootView, RenderEffectBlur())
            .setFrameClearDrawable(background)
            .setBlurRadius(15f)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBlurProgress.clipToOutline = true
            mBlurNavPrevious.clipToOutline = true
            mBlurNavNext.clipToOutline = true
        }
    }

}
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
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.transition.Slide
import androidx.transition.Transition
import androidx.transition.TransitionManager
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.History
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.PageMode
import br.com.fenix.bilingualreader.model.enums.Position
import br.com.fenix.bilingualreader.model.enums.ReaderMode
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.controller.SubTitleController
import br.com.fenix.bilingualreader.service.parses.manga.Parse
import br.com.fenix.bilingualreader.service.parses.manga.ParseFactory
import br.com.fenix.bilingualreader.service.parses.manga.RarParse
import br.com.fenix.bilingualreader.service.repository.HistoryRepository
import br.com.fenix.bilingualreader.service.repository.Storage
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.constants.ReaderConsts
import br.com.fenix.bilingualreader.util.helpers.LibraryUtil
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil.ThemeUtils.getColorFromAttr
import br.com.fenix.bilingualreader.util.helpers.Util
import br.com.fenix.bilingualreader.view.components.DottedSeekBar
import br.com.fenix.bilingualreader.view.components.manga.ImageViewPage
import br.com.fenix.bilingualreader.view.components.manga.ImageViewPager
import br.com.fenix.bilingualreader.view.managers.MangaHandler
import br.com.fenix.bilingualreader.view.ui.reader.book.BookReaderFragment
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
    private lateinit var mPagerAdapter: ComicPagerAdapter
    private lateinit var mPreferences: SharedPreferences
    private lateinit var mGestureDetector: GestureDetector
    private lateinit var mViewPager: ImageViewPager
    private lateinit var mPreviousButton: MaterialButton
    private lateinit var mNextButton: MaterialButton

    private lateinit var mLastPageContainer: MaterialCardView
    private lateinit var mLastPageImage: ImageView
    private lateinit var mLastPageText: TextView

    private var mResourceViewMode: HashMap<Int, ReaderMode> = HashMap()
    private var mIsFullscreen = false
    private var mFileName: String? = null
    var mReaderMode: ReaderMode = ReaderMode.FIT_WIDTH
    var mUseMagnifierType = false
    var mIsLeftToRight = false
    var mKeepZoomBetweenPage = false

    var mParse: Parse? = null
    lateinit var mPicasso: Picasso
    private lateinit var mComicHandler: MangaHandler
    var mTargets = SparseArray<Target>()
    private var mLastZoomScale = 0f

    private var mIsSeekBarChange = false
    private var mPageStartReading = LocalDateTime.now()
    private var mPagesAverage = mutableListOf<Long>()

    private lateinit var mLibrary: Library
    private var mManga: Manga? = null
    private var mNewManga: Manga? = null
    private var mNewMangaTitle = 0
    private lateinit var mStorage: Storage
    private lateinit var mHistoryRepository: HistoryRepository
    private lateinit var mSubtitleController: SubTitleController

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
    fun getCurrencyImageView(): ImageViewPage? {
        if (!isAdded || context == null || mCurrentFragment == null)
            return null
        return mCurrentFragment?.findViewById(R.id.page_image_view) as ImageViewPage
    }

    private fun onRefresh() {
        if (!::mViewPager.isInitialized)
            return

        if (mTargets.size() > 0) {
            loadImage(mTargets[mViewPager.currentItem] as MyTarget)
            for (i in 0 until mTargets.size()) {
                if (mViewPager.currentItem != i)
                    loadImage(mTargets[mTargets.keyAt(i)] as MyTarget)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (mParse != null)
            setFullscreen(fullscreen = true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mCurrentPage = 0
        mStorage = Storage(requireContext())
        mLibrary = LibraryUtil.getDefault(requireContext(), Type.MANGA)
        mPreferences = GeneralConsts.getSharedPreferences(requireContext())
        mSubtitleController = SubTitleController.getInstance(requireContext())
        mHistoryRepository = HistoryRepository(requireContext())

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

            mPagerAdapter = ComicPagerAdapter()
            mGestureDetector = GestureDetector(requireActivity(), MyTouchListener())

            mReaderMode = ReaderMode.valueOf(
                mPreferences.getString(
                    GeneralConsts.KEYS.READER.MANGA_READER_MODE,
                    ReaderMode.FIT_WIDTH.toString()
                ).toString()
            )

            mUseMagnifierType = mPreferences.getBoolean(GeneralConsts.KEYS.READER.MANGA_USE_MAGNIFIER_TYPE, false)
            mKeepZoomBetweenPage = mPreferences.getBoolean(GeneralConsts.KEYS.READER.MANGA_KEEP_ZOOM_BETWEEN_PAGES, false)

            mIsLeftToRight = PageMode.valueOf(
                mPreferences.getString(
                    GeneralConsts.KEYS.READER.MANGA_PAGE_MODE,
                    PageMode.Comics.toString()
                )!!
            ) == PageMode.Comics
        }

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view: View = inflater.inflate(R.layout.fragment_manga_reader, container, false)

        mRoot = requireActivity().findViewById(R.id.root_activity_manga_reader)
        mToolbarTop = requireActivity().findViewById(R.id.reader_manga_toolbar_reader_top)
        mPopupSubtitle = requireActivity().findViewById(R.id.popup_manga_translate)
        mPopupColor = requireActivity().findViewById(R.id.popup_manga_color)
        mPageNavLayout = requireActivity().findViewById(R.id.reader_manga_bottom_progress_content)
        mToolbarBottom = requireActivity().findViewById(R.id.reader_manga_toolbar_reader_bottom)
        mPreviousButton = requireActivity().findViewById(R.id.reader_manga_nav_previous_file)
        mNextButton = requireActivity().findViewById(R.id.reader_manga_nav_next_file)
        mViewPager = view.findViewById<View>(R.id.fragment_reader) as ImageViewPager

        mLastPageContainer = requireActivity().findViewById(R.id.reader_last_page)
        mLastPageImage = requireActivity().findViewById(R.id.last_page_image)
        mLastPageText = requireActivity().findViewById(R.id.last_page_text)

        mLastPageContainer.visibility = View.GONE

        mPageStartReading = LocalDateTime.now()
        mPagesAverage = mutableListOf()

        (mPageNavLayout.findViewById<View>(R.id.reader_manga_bottom_progress) as DottedSeekBar).also {
            mPageSeekBar = it
        }
        mPageNavTextView = mPageNavLayout.findViewById<View>(R.id.reader_manga_bottom_progress_title) as TextView

        if (mParse == null) {
            view.findViewById<ImageView>(R.id.image_error).visibility = View.VISIBLE
            mPageNavTextView.text = ""
            return view
        } else
            view.findViewById<ImageView>(R.id.image_error).visibility = View.GONE

        mPageSeekBar.max = (mParse?.numPages() ?: 2) - 1
        mPageSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
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

        mViewPager.adapter = mPagerAdapter
        mViewPager.offscreenPageLimit = ReaderConsts.READER.MANGA_OFF_SCREEN_PAGE_LIMIT
        mViewPager.setOnTouchListener(this)
        mViewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                if (mIsLeftToRight)
                    setCurrentPage(position + 1)
                else
                    setCurrentPage(mViewPager.adapter!!.count - position)

                generatePageAverage()
            }
        })
        mViewPager.setOnSwipeOutListener(object : ImageViewPager.OnSwipeOutListener {
            override fun onSwipeOutAtStart() {
                if (mIsLeftToRight) hitBeginning() else hitEnding()
            }

            override fun onSwipeOutAtEnd() {
                if (mIsLeftToRight) hitEnding() else hitBeginning()
            }
        })

        if (mCurrentPage != -1)
            setCurrentPage(mCurrentPage)

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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_reader_manga, menu)
        when (mReaderMode) {
            ReaderMode.ASPECT_FILL -> menu.findItem(R.id.manga_view_mode_aspect_fill).isChecked = true
            ReaderMode.ASPECT_FIT -> menu.findItem(R.id.manga_view_mode_aspect_fit).isChecked = true
            ReaderMode.FIT_WIDTH -> menu.findItem(R.id.manga_view_mode_fit_width).isChecked = true
        }
        if (mIsLeftToRight)
            menu.findItem(R.id.reading_manga_left_to_right).isChecked = true
        else
            menu.findItem(R.id.reading_manga_right_to_left).isChecked = true

        menu.findItem(R.id.menu_item_reader_manga_use_magnifier_type).isChecked = mUseMagnifierType
        menu.findItem(R.id.menu_item_reader_manga_keep_zoom_between_pages).isChecked = mKeepZoomBetweenPage
        menu.findItem(R.id.menu_item_reader_manga_show_clock_and_battery).isChecked = mPreferences.getBoolean(
            GeneralConsts.KEYS.READER.MANGA_SHOW_CLOCK_AND_BATTERY,
            false
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(ReaderConsts.STATES.STATE_FULLSCREEN, isFullscreen())
        outState.putLong(
            ReaderConsts.STATES.STATE_NEW_COMIC,
            (if (mNewManga != null) mNewManga!!.id else -1)!!
        )
        outState.putInt(
            ReaderConsts.STATES.STATE_NEW_COMIC_TITLE,
            if (mNewManga != null) mNewMangaTitle else -1
        )
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        if (mManga != null) {
            mManga?.bookMark = getCurrentPage()
            mStorage.updateBookMark(mManga!!)
        }
        mViewModel.history?.let {
            it.pageEnd = mCurrentPage
            it.setEnd(LocalDateTime.now())
            mHistoryRepository.save(it)
        }
        super.onPause()
    }

    override fun onDestroy() {
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

    fun getCurrentPage(): Int {
        return when {
            mIsLeftToRight -> if (::mViewPager.isInitialized) mViewPager.currentItem.plus(1) else 1
            ::mViewPager.isInitialized && mViewPager.adapter != null -> (mViewPager.adapter!!.count - mViewPager.currentItem)
            else -> 1
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.manga_view_mode_aspect_fill, R.id.manga_view_mode_aspect_fit, R.id.manga_view_mode_fit_width -> {
                item.isChecked = true
                mReaderMode = mResourceViewMode[item.itemId] ?: ReaderMode.FIT_WIDTH
                updatePageViews(mViewPager) {
                    if (mReaderMode === ReaderMode.ASPECT_FILL)
                        it.setTranslateToRightEdge(!mIsLeftToRight)
                    it.setViewMode(mReaderMode)
                }
            }

            R.id.reading_manga_left_to_right, R.id.reading_manga_right_to_left -> {
                item.isChecked = true
                val page = getCurrentPage()
                mIsLeftToRight = item.itemId == R.id.reading_manga_left_to_right
                setCurrentPage(page, false)
                mViewPager.adapter?.notifyDataSetChanged()
                updateSeekBar()
            }

            R.id.menu_item_reader_manga_use_magnifier_type -> {
                item.isChecked = !item.isChecked
                mUseMagnifierType = item.isChecked
                updatePageViews(mViewPager) {
                    it.useMagnifierType = mUseMagnifierType
                }

                with(mPreferences.edit()) {
                    this.putBoolean(GeneralConsts.KEYS.READER.MANGA_USE_MAGNIFIER_TYPE, mUseMagnifierType)
                    this.commit()
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
        }
        return super.onOptionsItemSelected(item)
    }

    fun changeAspect(toolbar: Toolbar, mode: ReaderMode) {
        mReaderMode = mode
        updatePageViews(mViewPager) {
            if (mReaderMode === ReaderMode.ASPECT_FILL)
                it.setTranslateToRightEdge(!mIsLeftToRight)
            it.setViewMode(mReaderMode)
        }

        val id: Int = mResourceViewMode.filterValues { it == mode }.keys.first()
        val menuItem: MenuItem = toolbar.menu.findItem(id)
        menuItem.isChecked = true
    }

    fun setCurrentPage(page: Int) {
        setCurrentPage(page, true)
    }

    private fun setCurrentPage(page: Int, animated: Boolean) {
        if (mIsLeftToRight) {
            mViewPager.currentItem = page - 1
            mPageSeekBar.progress = page - 1
        } else {
            mViewPager.setCurrentItem(mViewPager.adapter!!.count - page, animated)
            mPageSeekBar.progress = mViewPager.adapter!!.count - page
        }
        val navPage: String = if (mParse == null) "" else StringBuilder()
            .append(page).append("/").append(mParse?.numPages() ?: 1)
            .toString()
        mPageNavTextView.text = navPage
        mCurrentPage = page - 1

        if (mCurrentPage < 0)
            mCurrentPage = 0

        if (mManga != null)
            mSubtitleController.changeSubtitleInReader(mManga!!, mCurrentPage)

        (requireActivity() as MangaReaderActivity).changePage(mManga?.title ?: "", mParse?.getPagePath(mCurrentPage) ?: "", mCurrentPage + 1)
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
                    mLastZoomScale = getCurrencyImageView()?.getCurrentScale() ?: 0f

                    mCurrentFragment = `object`

                    if (mKeepZoomBetweenPage && mLastZoomScale != 0f)
                        getCurrencyImageView()?.zoomAnimated(mLastZoomScale, mIsLeftToRight)
                }
            }
            super.setPrimaryItem(container, position, `object`)
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val inflater = requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val layout: View = inflater.inflate(R.layout.fragment_manga_page, container, false)
            val imageViewPage: ImageViewPage = layout.findViewById<View>(R.id.page_image_view) as ImageViewPage
            if (mReaderMode === ReaderMode.ASPECT_FILL)
                imageViewPage.setTranslateToRightEdge(!mIsLeftToRight)
            imageViewPage.setViewMode(mReaderMode)
            imageViewPage.useMagnifierType = mUseMagnifierType
            imageViewPage.setOnTouchListener(this@MangaReaderFragment)
            container.addView(layout)
            val t = MyTarget(layout, position)
            loadImage(t)
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

    fun loadImage(t: MyTarget) {
        val pos: Int = if (mIsLeftToRight)
            t.position
        else
            mViewPager.adapter!!.count - t.position - 1

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

    inner class MyTarget(layout: View, val position: Int) : Target,
        View.OnClickListener {
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
            loadImage(this)
        }

    }

    inner class MyTouchListener : SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)
            val view: ImageViewPage = getCurrencyImageView() ?: return
            val coordinator = view.getPointerCoordinate(e)
            mSubtitleController.selectTextByCoordinate(coordinator)
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (!isFullscreen()) {
                setFullscreen(fullscreen = true)
                return true
            }

            val position = getPosition(e)
            if ((requireActivity() as MangaReaderActivity).touchPosition(position))
                return true

            if (position == Position.LEFT || position == Position.RIGHT) {
                val view = getCurrencyImageView()
                view?.let {
                    val isBack = if (mIsLeftToRight) position == Position.LEFT else position == Position.RIGHT
                    if (it.autoScroll(isBack))
                        return true
                }
            }

            when (position) {
                Position.TOP -> shareImage(true)
                Position.LEFT -> {
                    if (mIsLeftToRight) {
                        if (getCurrentPage() == 1) hitBeginning() else setCurrentPage(getCurrentPage() - 1)
                    } else {
                        if (getCurrentPage() == mViewPager.adapter!!.count) hitEnding() else setCurrentPage(getCurrentPage() + 1)
                    }
                }

                Position.RIGHT -> {
                    if (mIsLeftToRight) {
                        if (getCurrentPage() == mViewPager.adapter!!.count) hitEnding() else setCurrentPage(getCurrentPage() + 1)
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

                mPopupSubtitle.visibility = View.GONE
                mPopupColor.visibility = View.GONE
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
        if (newManga == null) return
        var confirm = false
        mNewManga = newManga
        mNewMangaTitle = titleRes
        val dialog = MaterialAlertDialogBuilder(requireActivity(), R.style.AppCompatAlertDialogStyle)
            .setTitle(titleRes)
            .setMessage(newManga.fileName)
            .setPositiveButton(
                R.string.switch_action_positive
            ) { _, _ ->
                if (activity == null)
                    return@setPositiveButton

                confirm = true
                val activity = requireActivity() as MangaReaderActivity
                activity.changeManga(mNewManga!!)
            }
            .setNegativeButton(
                R.string.switch_action_negative
            ) { _, _ -> }
            .setOnDismissListener {
                if (!confirm) {
                    mNewManga = null
                    setFullscreen(fullscreen = true)
                }
            }
            .create()
        dialog.show()
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

    private fun openPopupSaveShareImage() {
        val items = arrayListOf(
            requireContext().getString(R.string.reading_manga_choice_save_image),
            requireContext().getString(R.string.reading_manga_choice_share_image)
        ).toTypedArray()

        MaterialAlertDialogBuilder(requireContext(), R.style.AppCompatMaterialAlertList)
            .setTitle(getString(R.string.reading_manga_title_save_share_image))
            .setIcon(R.drawable.ic_save_share_image)
            .setItems(items) { _, selectItem ->
                val language = items[selectItem]
                shareImage(
                    language.equals(
                        requireContext().getString(R.string.reading_manga_choice_share_image),
                        true
                    )
                )
            }
            .show()
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

    private fun generateHistory(manga: Manga) {
        if (mViewModel.history != null) {
            if (mViewModel.history!!.fkLibrary != manga.fkLibrary || mViewModel.history!!.fkReference != manga.id) {
                mViewModel.history!!.setEnd(LocalDateTime.now())
                mHistoryRepository.save(mViewModel.history!!)
                mViewModel.history = History(manga.fkLibrary!!, manga.id!!, Type.MANGA, manga.bookMark, manga.pages, 0)
            }
        } else
            mViewModel.history = History(manga.fkLibrary!!, manga.id!!, Type.MANGA, manga.bookMark, manga.pages, 0)
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
            it.pageEnd = mCurrentPage
            it.setEnd(LocalDateTime.now())
            it.averageTimeByPage = average
            mHistoryRepository.save(it)
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
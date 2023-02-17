package br.com.fenix.bilingualreader.view.ui.reader.book

import android.content.res.Configuration
import android.content.res.Resources
import android.os.*
import android.util.DisplayMetrics
import android.view.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager.widget.ViewPager
import br.com.ebook.foobnix.android.utils.LOG
import br.com.ebook.foobnix.ext.CacheZipUtils
import br.com.ebook.foobnix.pdf.info.AppSharedPreferences
import br.com.ebook.foobnix.pdf.info.ExtUtils
import br.com.ebook.foobnix.pdf.info.IMG
import br.com.ebook.foobnix.pdf.info.TintUtil
import br.com.ebook.foobnix.pdf.info.model.BookCSS
import br.com.ebook.foobnix.pdf.info.wrapper.AppState
import br.com.ebook.foobnix.pdf.search.activity.HorizontalModeController
import br.com.ebook.foobnix.pdf.search.activity.ImagePageFragment
import br.com.ebook.foobnix.pdf.search.activity.UpdatableFragmentPagerAdapter
import br.com.ebook.foobnix.ui2.AppDB
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.enums.Position
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.Storage
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.constants.ReaderConsts
import br.com.fenix.bilingualreader.util.helpers.LibraryUtil
import br.com.fenix.bilingualreader.view.components.book.VerticalViewPager
import br.com.fenix.bilingualreader.view.components.manga.PageImageView
import br.com.fenix.bilingualreader.view.ui.reader.manga.MangaReaderViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.ebookdroid.core.codec.CodecDocument
import org.slf4j.LoggerFactory
import java.io.File


class BookReaderFragment : Fragment(), View.OnTouchListener {

    private val mLOGGER = LoggerFactory.getLogger(BookReaderFragment::class.java)

    private val mViewModel: MangaReaderViewModel by activityViewModels()

    private lateinit var mRoot: CoordinatorLayout
    private lateinit var mViewPager: VerticalViewPager

    private var mIsFullscreen = false
    private var mFileName: String? = null

    private lateinit var mLibrary: Library
    private var mBook: Book? = null
    private var mNewBook: Book? = null
    private var mNewBookTitle = 0
    private lateinit var mStorage: Storage

    private var codeDocument:CodecDocument? = null


    var dc: HorizontalModeController? = null

    companion object {
        var mCurrentPage = 0
        private var mCacheFolderIndex = 0
        private val mCacheFolder = arrayOf(
            GeneralConsts.CACHE_FOLDER.A,
            GeneralConsts.CACHE_FOLDER.B,
            GeneralConsts.CACHE_FOLDER.C,
            GeneralConsts.CACHE_FOLDER.D
        )

        fun create(): BookReaderFragment {
            if (mCacheFolderIndex >= 2)
                mCacheFolderIndex = 0
            else
                mCacheFolderIndex += 1

            val fragment = BookReaderFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }

        fun create(library: Library, path: File): BookReaderFragment {
            if (mCacheFolderIndex >= 2)
                mCacheFolderIndex = 0
            else
                mCacheFolderIndex += 1

            val fragment = BookReaderFragment()
            val args = Bundle()
            args.putSerializable(GeneralConsts.KEYS.OBJECT.LIBRARY, library)
            args.putSerializable(GeneralConsts.KEYS.OBJECT.FILE, path)
            fragment.arguments = args
            return fragment
        }

        fun create(library: Library, book: Book): BookReaderFragment {
            if (mCacheFolderIndex >= 2)
                mCacheFolderIndex = 0
            else
                mCacheFolderIndex += 1

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

                if (mBook != null) {
                    mCurrentPage = mBook!!.bookMark
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

            //codeDocument = ImageExtractor.getNewCodecContext(file!!.path, "", imageWidth, imageHeight);
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
        mViewPager = view.findViewById<View>(R.id.fragment_book_reader) as VerticalViewPager

        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)

        dc = object : HorizontalModeController(requireActivity(), displayMetrics.widthPixels, displayMetrics.heightPixels) {
            override fun onGoToPageImpl(page: Int) {

            }

            override fun showInterstialAndClose() {}
        }
        requireActivity().intent.data = mBook?.path?.toUri()

        AppDB.get().open(requireContext())
        CacheZipUtils.init(requireContext())
        BookCSS.get().load(requireContext())


        AppState.get().load(requireContext())
        AppSharedPreferences.get().init(requireContext())

        ExtUtils.init(requireContext())
        IMG.init(requireContext())

        TintUtil.init()
        dc?.init(requireActivity())

        val count: Int = dc?.pageCount ?: 0
        val mPagerAdapter = object : UpdatableFragmentPagerAdapter(requireActivity().supportFragmentManager) {
            override fun getCount(): Int {
                return count
            }

            override fun getItem(position: Int): Fragment {
                val imageFragment = ImagePageFragment()
                val b = Bundle()
                b.putInt(ImagePageFragment.POS, position)
                b.putString(ImagePageFragment.PAGE_PATH, dc?.getPageUrl(position).toString())
                imageFragment.arguments = b
                return imageFragment
            }

            override fun saveState(): Parcelable? {
                return try {
                    super.saveState()
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        R.string.msg_unexpected_error,
                        Toast.LENGTH_LONG
                    ).show()
                    LOG.e(e)
                    null
                }
            }

            override fun restoreState(arg0: Parcelable?, arg1: ClassLoader?) {
                try {
                    super.restoreState(arg0, arg1)
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        R.string.msg_unexpected_error,
                        Toast.LENGTH_LONG
                    ).show()
                    LOG.e(e)
                }
            }
        }

        mViewPager.isSaveEnabled = false
        mViewPager.isSaveFromParentEnabled = false

        mViewPager.adapter = mPagerAdapter
        mViewPager.offscreenPageLimit = ReaderConsts.READER.OFF_SCREEN_PAGE_LIMIT
        mViewPager.setOnTouchListener(this)
        mViewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {

            }
        })

        requireActivity().title = mFileName
        //mViewModel.filters.observe(viewLifecycleOwner) { onRefresh() }

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
        return true //mGestureDetector.onTouchEvent(event)
    }

    fun getCurrentPage(): Int {
        return when {
            ::mViewPager.isInitialized -> mViewPager.currentItem.plus(1)
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

    private fun updatePageViews(parentView: ViewGroup, change: (PageImageView) -> (Unit)) {
        for (i in 0 until parentView.childCount) {
            val child = parentView.getChildAt(i)
            if (child is ViewGroup) {
                updatePageViews(child, change)
            } else if (child is PageImageView) {
                val view: PageImageView = child
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

        }

    }

}
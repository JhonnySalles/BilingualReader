package br.com.fenix.bilingualreader.view.ui.reader.manga

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.MenuItem
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.BundleCompat
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Chapters
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.Position
import br.com.fenix.bilingualreader.model.enums.ReaderMode
import br.com.fenix.bilingualreader.model.enums.Themes
import br.com.fenix.bilingualreader.model.enums.TouchScreen
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.controller.MangaImageCoverController
import br.com.fenix.bilingualreader.service.controller.SubTitleController
import br.com.fenix.bilingualreader.service.japanese.Formatter
import br.com.fenix.bilingualreader.service.listener.ChapterCardListener
import br.com.fenix.bilingualreader.service.listener.ChapterLoadListener
import br.com.fenix.bilingualreader.service.listener.ReaderListener
import br.com.fenix.bilingualreader.service.ocr.GoogleVision
import br.com.fenix.bilingualreader.service.ocr.OcrProcess
import br.com.fenix.bilingualreader.service.repository.FileLinkRepository
import br.com.fenix.bilingualreader.service.repository.LibraryRepository
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import br.com.fenix.bilingualreader.service.repository.SharedData
import br.com.fenix.bilingualreader.service.repository.Storage
import br.com.fenix.bilingualreader.service.repository.SubTitleRepository
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.AnimationUtil
import br.com.fenix.bilingualreader.util.helpers.FileUtil
import br.com.fenix.bilingualreader.util.helpers.LibraryUtil
import br.com.fenix.bilingualreader.util.helpers.MenuUtil
import br.com.fenix.bilingualreader.util.helpers.NavigationUtil.NavigationUtils.overrideActivityTransitionCompat
import br.com.fenix.bilingualreader.util.helpers.PopupUtil.PopupUtils
import br.com.fenix.bilingualreader.util.helpers.Telemetry
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil.ThemeUtils.getColorFromAttr
import br.com.fenix.bilingualreader.util.helpers.TouchUtil.TouchUtils
import br.com.fenix.bilingualreader.util.helpers.Util
import br.com.fenix.bilingualreader.util.helpers.executeWithAnimation
import br.com.fenix.bilingualreader.view.adapter.reader.MangaChaptersCardAdapter
import br.com.fenix.bilingualreader.view.components.ComponentsUtil
import br.com.fenix.bilingualreader.view.components.DottedSeekBar
import br.com.fenix.bilingualreader.view.ui.menu.MenuActivity
import br.com.fenix.bilingualreader.view.ui.pages_link.PagesLinkActivity
import br.com.fenix.bilingualreader.view.ui.pages_link.PagesLinkViewModel
import br.com.fenix.bilingualreader.view.ui.assistant.ReadingAssistantActivity
import br.com.fenix.bilingualreader.view.ui.window.FloatingButtons
import br.com.fenix.bilingualreader.view.ui.window.FloatingOcr
import br.com.fenix.bilingualreader.view.ui.window.FloatingSubtitleReader
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.sidesheet.SideSheetBehavior
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.GlassSetup
import eightbitlab.com.blurview.RenderEffectBlur
import org.slf4j.LoggerFactory
import java.io.File


class MangaReaderActivity : AppCompatActivity(), OcrProcess, ChapterLoadListener, ReaderListener {

    private val mLOGGER = LoggerFactory.getLogger(MangaReaderActivity::class.java)

    private val mViewModel: MangaReaderViewModel by lazy { ViewModelProvider(this)[MangaReaderViewModel::class.java] }

    private lateinit var mReaderTitle: TextView
    private lateinit var mReaderProgress: DottedSeekBar
    private lateinit var mProgressContent: LinearLayout
    private lateinit var mToolBar: MaterialToolbar
    private lateinit var mSubToolBar: LinearLayout
    private lateinit var mLanguageOcrDescription: TextView
    private var mMenuPopupTranslateBottom: FrameLayout? = null
    private var mMenuPopupTranslateLeft: FrameLayout? = null
    private var mMenuPopupTranslateBackground: BlurView? = null
    private lateinit var mPopupTranslateView: ViewPager2
    private var mMenuPopupConfigurationsBottom: FrameLayout? = null
    private var mMenuPopupConfigurationsLeft: FrameLayout? = null
    private var mMenuPopupConfigurationsBackground: BlurView? = null
    private lateinit var mPopupConfigurationsView: ViewPager2
    private lateinit var mPopupConfigurationsTab: TabLayout
    private lateinit var mLeftSheetTranslate: SideSheetBehavior<FrameLayout>
    private lateinit var mBottomSheetTranslate: BottomSheetBehavior<FrameLayout>
    private lateinit var mLeftSheetConfigurations: SideSheetBehavior<FrameLayout>
    private lateinit var mBottomSheetConfigurations: BottomSheetBehavior<FrameLayout>

    private val mBottomSheetTranslateCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            val isGlass = mIsGlassmorphism
            if (isGlass) {
                mMenuPopupTranslateBackground?.let { bg ->
                    if (newState == BottomSheetBehavior.STATE_DRAGGING || newState == BottomSheetBehavior.STATE_SETTLING) {
                        bg.setBlurAutoUpdate(true)
                    } else {
                        bg.setBlurAutoUpdate(false)
                    }
                }
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            val isGlass = mIsGlassmorphism
            if (isGlass) {
                mMenuPopupTranslateBackground?.setBlurAutoUpdate(true)
            }
        }
    }

    private val mBottomSheetConfigurationsCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            val isGlass = mIsGlassmorphism
            if (isGlass) {
                mMenuPopupConfigurationsBackground?.let { bg ->
                    if (newState == BottomSheetBehavior.STATE_DRAGGING || newState == BottomSheetBehavior.STATE_SETTLING) {
                        bg.setBlurAutoUpdate(true)
                    } else {
                        bg.setBlurAutoUpdate(false)
                    }
                }
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            val isGlass = mIsGlassmorphism
            if (isGlass) {
                mMenuPopupConfigurationsBackground?.setBlurAutoUpdate(true)
            }
        }
    }


    private lateinit var mPopupMangaColorFilterFragment: PopupMangaColorFilterFragment
    private lateinit var mPopupMangaAnnotationsFragment: PopupMangaAnnotationsFragment
    private lateinit var mPopupMangaSubtitleConfigurationFragment: PopupMangaSubtitleConfiguration
    private lateinit var mPopupMangaSubtitleReaderFragment: PopupMangaSubtitleReader
    private lateinit var mPopupMangaSubtitleVocabularyFragment: PopupMangaSubtitleVocabulary

    private lateinit var mFloatingSubtitleReader: FloatingSubtitleReader
    private var mFloatingWindowOcr: FloatingOcr? = null
    private lateinit var mFloatingButtons: FloatingButtons

    private lateinit var mClockAndBattery: LinearLayout
    private lateinit var mBattery: TextView
    private lateinit var mTouchView: ConstraintLayout

    private lateinit var mChapterContent: LinearLayout
    private lateinit var mChapterList: RecyclerView

    private val mHandler = Handler(Looper.getMainLooper())
    private val mMonitoringBattery = Runnable { getBatteryPercent() }
    private val mDismissTouchView = Runnable { closeViewTouch() }

    private lateinit var mPreferences: SharedPreferences
    // O tema glassmorphism so muda com recriacao da Activity, entao lemos a preferencia uma
    // unica vez em vez de a cada onSlide/onStateChanged dos BottomSheets.
    private val mIsGlassmorphism: Boolean by lazy { mPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false) }
    private lateinit var mStorage: Storage
    private lateinit var mRepository: MangaRepository
    private lateinit var mSubtitleController: SubTitleController
    private lateinit var mLibrary: Library
    private var mFragment: MangaReaderFragment? = null
    private var mManga: Manga? = null
    private var mMenuPopupBottomSheet: Boolean = true
    private var mDialog: AlertDialog? = null

    companion object {
        private lateinit var mPopupTranslateTab: TabLayout
        fun selectTabReader() = mPopupTranslateTab.selectTab(mPopupTranslateTab.getTabAt(0), true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtil.applyThemeMode(this)
        mPreferences = GeneralConsts.getSharedPreferences(this)
        val theme = Themes.valueOf(mPreferences.getString(GeneralConsts.KEYS.THEME.THEME_USED, Themes.ORIGINAL.toString())!!)
        setTheme(theme.getValue())

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manga_reader)

        supportPostponeEnterTransition()
        mHandler.postDelayed({
            supportStartPostponedEnterTransition()
        }, 1000)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        Formatter.initializeAsync(applicationContext)

        mSubtitleController = SubTitleController.getInstance(applicationContext)

        if (savedInstanceState == null)
            mSubtitleController.clearExternalSubtitlesSelected()

        mToolBar = findViewById(R.id.reader_manga_toolbar_reader)
        MenuUtil.tintToolbar(mToolBar, theme)
        mSubToolBar = findViewById(R.id.reader_manga_sub_toolbar)
        mLanguageOcrDescription = findViewById(R.id.reader_manga_ocr_language)
        mLanguageOcrDescription.setOnClickListener {
            choiceLanguage {
                mViewModel.mLanguageOcr = it
            }
        }

        setSupportActionBar(mToolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        ensureToolbarTitleEllipsis()

        mReaderTitle = findViewById(R.id.reader_manga_bottom_progress_title)
        mReaderProgress = findViewById(R.id.reader_manga_bottom_progress)
        mProgressContent = findViewById(R.id.reader_manga_bottom_progress_content)
        mMenuPopupTranslateBottom = findViewById(R.id.popup_manga_translate_bottom_sheet)
        mMenuPopupTranslateLeft = findViewById(R.id.popup_manga_translate_side_sheet)
        mMenuPopupTranslateBackground = findViewById(R.id.popup_manga_translate_header_background)
        mMenuPopupConfigurationsBottom = findViewById(R.id.popup_manga_configurations_bottom_sheet)
        mMenuPopupConfigurationsLeft = findViewById(R.id.popup_manga_configurations_side_sheet)
        mMenuPopupConfigurationsBackground = findViewById(R.id.popup_manga_configurations_header_background)

        val btnMenuFloating = findViewById<ImageView>(R.id.popup_manga_translate_floating_button)
        btnMenuFloating.setOnClickListener {
            (btnMenuFloating.drawable as AnimatedVectorDrawable).start()
            openFloatingSubtitle()
        }

        val btnMenuFileLink = findViewById<MaterialButton>(R.id.reader_manga_btn_menu_file_link)
        btnMenuFileLink.setOnClickListener {
            btnMenuFileLink.executeWithAnimation {
                openFileLink()
            }
        }

        val btnFloatingButtons = findViewById<MaterialButton>(R.id.reader_manga_btn_floating_buttons)
        btnFloatingButtons.setOnClickListener {
            btnFloatingButtons.executeWithAnimation {
                openFloatingButtons()
            }
        }

        val btnRotate = findViewById<MaterialButton>(R.id.reader_manga_btn_screen_rotate)
        btnRotate.setOnClickListener {
            btnRotate.executeWithAnimation {
                requestedOrientation = if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    else
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        }

        btnRotate.setOnLongClickListener {
            showMenuFromButton(btnRotate, it)
            true
        }

        val buttonChapters = findViewById<MaterialButton>(R.id.reader_manga_btn_menu_chapters)
        buttonChapters.setOnClickListener {
            buttonChapters.executeWithAnimation {
                openChapters()
            }
        }

        val buttonOcr = findViewById<MaterialButton>(R.id.reader_manga_btn_menu_ocr)
        buttonOcr?.setOnClickListener {
            buttonOcr.executeWithAnimation {
                showMenuFromButton(buttonOcr, buttonOcr)
            }
        }

        val btnMenuPage = findViewById<MaterialButton>(R.id.reader_manga_btn_menu_page_linked)
        btnMenuPage.setOnClickListener {
            when {
                mSubtitleController.isDrawing() -> {
                    btnMenuPage.setIconResource(R.drawable.ico_animated_page_linked_remove)
                    btnMenuPage.executeWithAnimation {
                        mSubtitleController.drawPageLinked()
                    }
                }
                mSubtitleController.hasLinkedPage(MangaReaderFragment.mCurrentPage) -> {
                    btnMenuPage.setIconResource(R.drawable.ico_animated_page_linked_insert)
                    btnMenuPage.executeWithAnimation {
                        mSubtitleController.drawPageLinked()
                    }
                }
                else -> {
                    btnMenuPage.setIconResource(R.drawable.ico_animated_page_linked_empty)
                    btnMenuPage.executeWithAnimation {
                        btnMenuPage.setIconResource(R.drawable.ico_animated_page_linked_insert)
                    }
                }
            }
        }

        mLibrary = LibraryUtil.getDefault(this, Type.MANGA)
        mStorage = Storage(applicationContext)
        val previous = findViewById<MaterialButton>(R.id.reader_manga_nav_previous_file)
        val next = findViewById<MaterialButton>(R.id.reader_manga_nav_next_file)

        previous.setOnClickListener {
            previous.executeWithAnimation {
                switchManga(false)
            }
        }
        next.setOnClickListener {
            next.executeWithAnimation {
                switchManga(true)
            }
        }

        mToolBar.setOnClickListener { dialogPageIndex() }
        mToolBar.setOnLongClickListener {
            mManga?.let { FileUtil(this).copyName(it) }
            true
        }

        mPopupTranslateTab = findViewById(R.id.popup_manga_translate_tab)
        mPopupTranslateView = findViewById(R.id.popup_manga_translate_view_pager)

        if (findViewById<ImageView>(R.id.popup_manga_translate_center_button) == null) {
            mMenuPopupBottomSheet = false
            mLeftSheetTranslate = SideSheetBehavior.from(mMenuPopupTranslateLeft!!)
            findViewById<ImageView>(R.id.popup_manga_translate_close_button)?.setOnClickListener {
                AnimationUtil.animatePopupClose(this, mMenuPopupTranslateLeft!!, isVertical = false, navigationColor = false)
            }
            mPopupTranslateTab.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    mLeftSheetTranslate.isDraggable = mPopupTranslateTab.selectedTabPosition <= 0
                }
                override fun onTabUnselected(tab: TabLayout.Tab?) { }
                override fun onTabReselected(tab: TabLayout.Tab?) { }
            })
        } else {
            mBottomSheetTranslate = BottomSheetBehavior.from(mMenuPopupTranslateBottom!!).apply {
                peekHeight = 195
                this.state = BottomSheetBehavior.STATE_COLLAPSED
                mBottomSheetTranslate = this
            }
            mBottomSheetTranslate.isDraggable = false
            mBottomSheetTranslate.addBottomSheetCallback(mBottomSheetTranslateCallback)
            PopupUtils.onPopupTouch(this, mMenuPopupTranslateBottom!!, mBottomSheetTranslate, findViewById<ImageView>(R.id.popup_manga_translate_center_button), navigationColor = false)
        }

        mClockAndBattery = findViewById(R.id.reader_manga_container_clock_battery)
        mBattery = findViewById(R.id.txt_battery)
        mTouchView = findViewById(R.id.reader_manga_container_touch_demonstration)

        mPopupMangaColorFilterFragment = PopupMangaColorFilterFragment()
        mPopupMangaAnnotationsFragment = PopupMangaAnnotationsFragment()
        mPopupMangaSubtitleConfigurationFragment = PopupMangaSubtitleConfiguration()
        mPopupMangaSubtitleReaderFragment = PopupMangaSubtitleReader()
        mPopupMangaSubtitleVocabularyFragment = PopupMangaSubtitleVocabulary()
        mPopupMangaSubtitleVocabularyFragment.setBackground(getColorFromAttr(R.attr.background))

        mFloatingSubtitleReader = FloatingSubtitleReader(applicationContext, this)
        prepareFloatingSubtitle()

        val viewTranslatePagerAdapter = ViewPagerAdapter(this)
        viewTranslatePagerAdapter.addFragment(
            mPopupMangaSubtitleReaderFragment,
            resources.getString(R.string.popup_reading_manga_tab_item_subtitle)
        )
        viewTranslatePagerAdapter.addFragment(
            mPopupMangaSubtitleVocabularyFragment,
            resources.getString(R.string.popup_reading_manga_tab_item_subtitle_vocabulary)
        )
        viewTranslatePagerAdapter.addFragment(
            mPopupMangaSubtitleConfigurationFragment,
            resources.getString(R.string.popup_reading_manga_tab_item_subtitle_import)
        )

        mPopupTranslateView.adapter = viewTranslatePagerAdapter
        TabLayoutMediator(mPopupTranslateTab, mPopupTranslateView) { tab, position ->
            tab.text = viewTranslatePagerAdapter.getPageTitle(position)
        }.attach()

        mPopupConfigurationsTab = findViewById(R.id.popup_manga_configurations_tab)
        mPopupConfigurationsView = findViewById(R.id.popup_manga_configurations_view_pager)

        if (findViewById<ImageView>(R.id.popup_manga_configurations_center_button) != null) {
            mBottomSheetConfigurations = BottomSheetBehavior.from(mMenuPopupConfigurationsBottom!!).apply {
                peekHeight = 195
                this.state = BottomSheetBehavior.STATE_COLLAPSED
                mBottomSheetConfigurations = this
            }
            mBottomSheetConfigurations.isDraggable = true
            mBottomSheetConfigurations.addBottomSheetCallback(mBottomSheetConfigurationsCallback)
            PopupUtils.onPopupTouch(this, mMenuPopupConfigurationsBottom!!, mBottomSheetConfigurations, findViewById<ImageView>(R.id.popup_manga_configurations_center_button), navigationColor = false)
        } else {
            mLeftSheetConfigurations = SideSheetBehavior.from(mMenuPopupConfigurationsLeft!!)
            findViewById<ImageView>(R.id.popup_manga_configurations_close_button)?.setOnClickListener {
                AnimationUtil.animatePopupClose(this, mMenuPopupConfigurationsLeft!!, isVertical = false, navigationColor = false)
            }

            mPopupConfigurationsTab.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    mLeftSheetConfigurations.isDraggable = mPopupConfigurationsTab.selectedTabPosition <= 0
                }
                override fun onTabUnselected(tab: TabLayout.Tab?) { }
                override fun onTabReselected(tab: TabLayout.Tab?) { }
            })
        }

        val viewColorPagerAdapter = ViewPagerAdapter(this)
        viewColorPagerAdapter.addFragment(
            mPopupMangaColorFilterFragment,
            resources.getString(R.string.popup_reading_manga_tab_item_configuration_brightness)
        )
        viewColorPagerAdapter.addFragment(
            mPopupMangaAnnotationsFragment,
            resources.getString(R.string.popup_reading_manga_tab_item_configuration_bookmarks)
        )
        mPopupConfigurationsView.adapter = viewColorPagerAdapter
        TabLayoutMediator(mPopupConfigurationsTab, mPopupConfigurationsView) { tab, position ->
            tab.text = viewColorPagerAdapter.getPageTitle(position)
        }.attach()
        mPopupMangaAnnotationsFragment.setListener(this)
        mPopupConfigurationsView.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            private fun refreshCover() = mViewModel.refreshCover(mManga) { mPopupMangaAnnotationsFragment.notifyItemChanged(it) }

            override fun onPageSelected(position: Int) {
                if (viewColorPagerAdapter.getItem(position) == mPopupMangaAnnotationsFragment) {
                    val layout = if (mMenuPopupBottomSheet) mMenuPopupConfigurationsBottom else mMenuPopupConfigurationsLeft
                    if (!layout!!.isVisible) {
                        var afterVisible: Runnable? = null
                        afterVisible = Runnable {
                            if (!layout.isVisible)
                                mHandler.postDelayed(afterVisible!!, 300)
                            else
                                refreshCover()
                        }
                        mHandler.postDelayed(afterVisible, 300)
                    } else
                        refreshCover()
                }
            }
        })

        findViewById<MaterialButton>(R.id.reader_manga_btn_popup_color).setOnClickListener {
            val layout = if (mMenuPopupBottomSheet) mMenuPopupConfigurationsBottom else mMenuPopupConfigurationsLeft
            val isOpened = layout!!.isVisible

            if (mMenuPopupBottomSheet)
                mMenuPopupTranslateBottom!!.visibility = View.GONE
            else
                mMenuPopupConfigurationsLeft!!.visibility = View.GONE

            if (mMenuPopupBottomSheet)
                mBottomSheetConfigurations.state = BottomSheetBehavior.STATE_EXPANDED
            else
                mLeftSheetConfigurations.state = SideSheetBehavior.STATE_EXPANDED

            var index = -1
            for (i in 0 until viewColorPagerAdapter.itemCount)
                if (viewColorPagerAdapter.getItem(i) == mPopupMangaColorFilterFragment) {
                    index = i
                    break
                }
            if (index >= 0)
                mPopupConfigurationsTab.selectTab(mPopupConfigurationsTab.getTabAt(index), true)

            if (!isOpened)
                AnimationUtil.animatePopupOpen(this, layout, mMenuPopupBottomSheet, navigationColor = false)
        }

        val buttonAnnotations = findViewById<MaterialButton>(R.id.reader_manga_btn_menu_annotations)
        buttonAnnotations.setOnClickListener {
            var index = -1
            for (i in 0 until viewColorPagerAdapter.itemCount)
                if (viewColorPagerAdapter.getItem(i) == mPopupMangaAnnotationsFragment) {
                    index = i
                    break
                }
            if (index >= 0)
                mPopupConfigurationsTab.selectTab(mPopupConfigurationsTab.getTabAt(index), true)

            buttonAnnotations.executeWithAnimation {
                val layout = if (mMenuPopupBottomSheet) mMenuPopupConfigurationsBottom else mMenuPopupConfigurationsLeft
                val isOpened = layout!!.isVisible

                if (mMenuPopupBottomSheet)
                    mMenuPopupTranslateBottom!!.visibility = View.GONE
                else
                    mMenuPopupConfigurationsLeft!!.visibility = View.GONE

                if (mMenuPopupBottomSheet)
                    mBottomSheetConfigurations.state = BottomSheetBehavior.STATE_EXPANDED
                else
                    mLeftSheetConfigurations.state = SideSheetBehavior.STATE_EXPANDED

                if (!isOpened)
                    AnimationUtil.animatePopupOpen(this@MangaReaderActivity, layout, mMenuPopupBottomSheet, navigationColor = false)
            }
        }

        mRepository = MangaRepository(applicationContext)

        mClockAndBattery.visibility = if (mPreferences.getBoolean(GeneralConsts.KEYS.READER.MANGA_SHOW_CLOCK_AND_BATTERY, false))
            View.VISIBLE
        else
            View.GONE

        getBatteryPercent()

        mTouchView.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (mHandler.hasCallbacks(mDismissTouchView))
                    mHandler.removeCallbacks(mDismissTouchView)
            } else {
                mHandler.removeCallbacks(mDismissTouchView)
            }

            closeViewTouch()
        }

        mChapterContent = findViewById(R.id.reader_manga_container_chapters_list)
        mChapterList = findViewById(R.id.manga_chapters_list_covers)
        mChapterContent.visibility = View.GONE
        prepareChapters()

        val bundle = intent.extras
        if (bundle != null) {
            val name = bundle.getString(GeneralConsts.KEYS.MANGA.NAME) ?: ""
            val bookMark = if (bundle.containsKey(GeneralConsts.KEYS.MANGA.PAGE_NUMBER))
                bundle.getInt(GeneralConsts.KEYS.MANGA.PAGE_NUMBER)
            else
                bundle.getInt(GeneralConsts.KEYS.MANGA.MARK)
            changePage(name, "", bookMark)
        } else
            changePage("", "", 0)

        if (savedInstanceState == null) {
            SharedData.clearChapters()
            if (Intent.ACTION_VIEW == intent.action) {
                if (intent.extras != null && intent.extras!!.containsKey(GeneralConsts.KEYS.MANGA.ID)) {
                    val manga =
                        mRepository.get(intent.extras!!.getLong(GeneralConsts.KEYS.MANGA.ID))
                    manga?.fkLibrary?.let {
                        val library = LibraryRepository(this)
                        mLibrary = library.get(it) ?: mLibrary
                    }
                    initialize(manga)
                } else
                    intent.data?.path?.let {
                        val file = File(it)
                        initialize(file, 0)
                    }
            } else {
                val extras = intent.extras

                if (extras != null)
                    mLibrary = BundleCompat.getSerializable(extras, GeneralConsts.KEYS.OBJECT.LIBRARY, Library::class.java) as Library

                val manga = if (extras != null) BundleCompat.getSerializable(extras, GeneralConsts.KEYS.OBJECT.MANGA, Manga::class.java) else null
                manga?.let {
                    it.bookMark = extras?.getInt(GeneralConsts.KEYS.MANGA.MARK) ?: 0
                }

                initialize(manga)
            }
        } else
            mFragment = supportFragmentManager.findFragmentById(R.id.root_frame_manga_reader) as MangaReaderFragment?

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setupPopupBackgrounds()
        setupBottomSheetInsets()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val layoutTranslate = if (mMenuPopupBottomSheet) mMenuPopupTranslateBottom else mMenuPopupTranslateLeft
                val layoutConfiguration = if (mMenuPopupBottomSheet) mMenuPopupConfigurationsBottom else mMenuPopupConfigurationsLeft

                if (layoutTranslate!!.visibility != View.GONE || layoutConfiguration!!.visibility != View.GONE) {
                    if (layoutTranslate.visibility != View.GONE)
                        AnimationUtil.animatePopupClose(this@MangaReaderActivity, layoutTranslate, mMenuPopupBottomSheet, navigationColor = false)
                    if (layoutConfiguration!!.visibility != View.GONE)
                        AnimationUtil.animatePopupClose(this@MangaReaderActivity, layoutConfiguration, mMenuPopupBottomSheet, navigationColor = false)
                    return
                }

                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                finish()
            }
        })
    }

    private fun initialize(manga: Manga?) {
        val fragment: MangaReaderFragment = if (manga != null && manga.file.exists()) {
            setManga(manga)
            MangaReaderFragment.create(mLibrary, manga)
        } else
            MangaReaderFragment.create()

        val fileLink = ViewModelProvider(this)[PagesLinkViewModel::class.java]
        mSubtitleController.setFileLink(fileLink.getFileLink(manga))
        setFragment(fragment)
    }

    private fun initialize(file: File?, page: Int) {
        val fragment: MangaReaderFragment = if (file != null) {
            changePage(file.name, "", page)
            MangaReaderFragment.create(mLibrary, file)
        } else
            MangaReaderFragment.create()

        mSubtitleController.setFileLink(null)
        setFragment(fragment)
    }

    private fun switchManga(isNext: Boolean = true) {
        if (mManga == null || mDialog != null) return

        val changeManga = if (isNext)
            mStorage.getNextManga(mLibrary, mManga!!)
        else
            mStorage.getPrevManga(mLibrary, mManga!!)

        if (changeManga == null) {
            val content = if (isNext) R.string.switch_next_comic_last_comic else R.string.switch_prev_comic_first_comic
            MaterialAlertDialogBuilder(this, R.style.AppCompatAlertDialogStyle)
                    .setTitle(getString(R.string.switch_next_comic_not_found))
                    .setMessage(content)
                    .setPositiveButton(R.string.action_neutral) { _, _ -> }
                    .show()
            return
        }

        val title = if (isNext) R.string.switch_next_comic else R.string.switch_prev_comic

        mDialog = MaterialAlertDialogBuilder(this, R.style.AppCompatAlertDialogStyle)
                .setTitle(title)
                .setMessage(changeManga.fileName)
                .setPositiveButton(R.string.switch_action_positive) { _, _ -> changeManga(changeManga) }
                .setNegativeButton(R.string.switch_action_negative) { _, _ -> }
                .setOnDismissListener { mDialog = null }
                .create()
        mDialog?.show()
    }

    fun changeManga(manga: Manga) {
        setManga(manga)

        mSubtitleController.clearExternalSubtitlesSelected()
        val fileLink = ViewModelProvider(this)[PagesLinkViewModel::class.java]
        mSubtitleController.setFileLink(fileLink.getFileLink(manga))

        setFragment(MangaReaderFragment.create(mLibrary, manga))
    }

    fun setLanguage(language: Languages) {
        mViewModel.mLanguageOcr = language
        mLanguageOcrDescription.text = getString(R.string.languages_description, Util.languageToString(this, language))
        mSubToolBar.visibility = View.VISIBLE
    }

    private fun changeShowBatteryClock(enabled: Boolean) {
        mClockAndBattery.visibility = if (enabled)
            View.VISIBLE
        else
            View.GONE

        optionsSave(enabled)
    }

    fun changePage(title: String, text: String, page: Int) {
        mReaderTitle.text = if (page > -1 && mManga != null) getString(
            R.string.progress,
            page,
            mManga!!.pages
        ) else ""
        mToolBar.title = title
        val boldSubtitle = android.text.SpannableString(text)
        boldSubtitle.setSpan(
            android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
            0,
            text.length,
            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        mToolBar.subtitle = boldSubtitle
        ensureToolbarTitleEllipsis()
        SharedData.selectPage(page)
    }

    private fun ensureToolbarTitleEllipsis() {
        mToolBar.post {
            for (i in 0 until mToolBar.childCount) {
                val child = mToolBar.getChildAt(i)
                if (child is TextView) {
                    child.maxLines = 1
                    child.ellipsize = android.text.TextUtils.TruncateAt.END
                }
            }
        }
    }

    private fun setManga(manga: Manga, isRestore: Boolean = false) {
        if (!isRestore) {
            mViewModel.stopExecutions()
            mViewModel.clear()
            SharedData.clearChapters()
            mManga = manga
            mRepository.updateLastAccess(manga)
            setShortCutManga()
        }

        mManga = manga
        changePage(manga.title, "", manga.bookMark)
        setMangaDots(mutableListOf(), mutableListOf())
        mViewModel.refreshAnnotations(mManga)
    }

    fun setMangaDots(dots: MutableList<Int>, inverse: MutableList<Int>) = mReaderProgress.setPrimaryDots(dots.toIntArray(), inverse.toIntArray())

    private fun setShortCutManga() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1)
            return

        try {
            val shortcut = getSystemService(ShortcutManager::class.java)
            val lasts = mRepository.getLastedRead()
            val list = mutableListOf<ShortcutInfo>()

            lasts.first?.let {
                list.add(generateInfo("comic1", it))
            }

            lasts.second?.let {
                list.add(generateInfo("comic2", it))
            }

            shortcut.dynamicShortcuts.clear()
            shortcut.dynamicShortcuts = list
        } catch (e: Exception) {
            mLOGGER.error("Error generate shortcut: " + e.message, e)
            Telemetry.recordException(e, "Error generate shortcut: " + e.message)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    fun getMangaIconAdaptive(manga: Manga): Icon {
        val image = MangaImageCoverController.instance.getMangaCover(this, manga, true)
            ?: return Icon.createWithResource(
                this,
                R.drawable.ico_shortcut_book
            )
        val bitmapDrawable: Drawable = BitmapDrawable(resources, image)
        val drawableIcon = AdaptiveIconDrawable(bitmapDrawable, bitmapDrawable)
        val result = Bitmap.createBitmap(
            drawableIcon.intrinsicWidth,
            drawableIcon.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(result)
        drawableIcon.setBounds(0, 0, canvas.width, canvas.height)
        drawableIcon.draw(canvas)
        return Icon.createWithAdaptiveBitmap(result)
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun generateInfo(id: String, manga: Manga): ShortcutInfo {
        val icon = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            getMangaIconAdaptive(manga)
        else
            Icon.createWithResource(this, R.drawable.ico_shortcut_book)

        val intent = Intent(this, MangaReaderActivity::class.java)
        intent.action = Intent.ACTION_VIEW
        intent.data = Uri.parse(manga.path)
        intent.extras

        val bundle = Bundle()
        bundle.putLong(GeneralConsts.KEYS.MANGA.ID, manga.id ?: -1)
        intent.putExtras(bundle)

        return ShortcutInfo.Builder(this, id)
            .setShortLabel(manga.title)
            .setIcon(icon)
            .setIntent(intent)
            .build()
    }

    private fun dialogPageIndex() {
        if  (mDialog != null) return

        val currentFragment = supportFragmentManager.findFragmentById(R.id.root_frame_manga_reader) ?: return
        val parse = (currentFragment as MangaReaderFragment).mParse ?: return

        var paths = mapOf<String, Int>()

        parse.getComicInfo()?.let {
            it.pages?.let { p ->
                paths = p.filter { c -> c.bookmark != null && c.image != null }
                    .associate { c -> c.bookmark!! to c.image!! }
            }
        }

        if (paths.isEmpty())
            paths = parse.getPagePaths()

        if (paths.isEmpty()) {
            MaterialAlertDialogBuilder(this, R.style.AppCompatAlertDialogStyle)
                .setTitle(resources.getString(R.string.reading_manga_page_index))
                .setMessage(resources.getString(R.string.reading_manga_page_empty))
                .setPositiveButton(R.string.action_neutral) { _, _ -> }
                .show()
            return
        }

        val items = paths.keys.toTypedArray()

        val title = LinearLayout(this)
        title.orientation = LinearLayout.VERTICAL
        title.setPadding(resources.getDimensionPixelOffset(R.dimen.page_link_page_index_title_padding))
        val name = TextView(this)
        name.text = mToolBar.title
        name.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.title_index_dialog_size))
        name.setTextColor(getColorFromAttr(R.attr.colorOnBackground))
        title.addView(name)
        val index = TextView(this)
        index.text = resources.getString(R.string.reading_manga_page_index)
        index.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.title_small_index_dialog_size))
        index.setTextColor(getColorFromAttr(R.attr.colorPrimary))
        title.addView(index)
        title.setOnLongClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied Text", mToolBar.title)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(
                this,
                getString(R.string.action_copy, mToolBar.title),
                Toast.LENGTH_LONG
            ).show()

            true
        }

        mDialog = MaterialAlertDialogBuilder(this, R.style.AppCompatMaterialAlertList)
            .setCustomTitle(title)
            .setItems(items) { _, selected ->
                val pageNumber = paths[items[selected]]
                if (pageNumber != null)
                    currentFragment.setCurrentPage(pageNumber + 1)
            }
            .setOnDismissListener { mDialog = null }
            .create()
        mDialog?.show()
    }

    @SuppressLint("MissingSuperCall")
    override fun onSaveInstanceState(outState: Bundle) {
       outState.putSerializable(GeneralConsts.KEYS.OBJECT.LIBRARY, mLibrary)

        if (mManga != null)
            outState.putSerializable(GeneralConsts.KEYS.OBJECT.MANGA, mManga)

        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        mLibrary = BundleCompat.getSerializable(savedInstanceState, GeneralConsts.KEYS.OBJECT.LIBRARY, Library::class.java) as Library

        val manga = BundleCompat.getSerializable(savedInstanceState, GeneralConsts.KEYS.OBJECT.MANGA, Manga::class.java)
        if (manga != null)
            setManga(manga, true)
    }

    private var mLastFloatingWindowOcr = false
    private var mLastFloatingShowing = false
    private var mLastFloatingButtons = false
    override fun onResume() {
        super.onResume()
        setupPopupBackgrounds()
        if (mLastFloatingWindowOcr)
            openFloatingOcr()

        if (mLastFloatingShowing)
            mFloatingSubtitleReader.show()

        if (mLastFloatingButtons)
            openFloatingButtons()

        if (mManga != null) {
            val fileLinkRepository = FileLinkRepository(applicationContext)
            val linkedFile = fileLinkRepository.get(mManga!!)
            mSubtitleController.setFileLink(linkedFile)
        }
    }

    override fun onStop() {
        if (::mFloatingSubtitleReader.isInitialized) {
            mLastFloatingShowing = mFloatingSubtitleReader.isShowing
            if (mFloatingSubtitleReader.isShowing)
                mFloatingSubtitleReader.dismiss()
        }

        closeFloatingOcr()

        if (::mFloatingButtons.isInitialized) {
            mLastFloatingButtons = mFloatingButtons.isShowing
            if (mFloatingButtons.isShowing)
                mFloatingButtons.dismiss()
        }

        super.onStop()
    }

    override fun onDestroy() {
        mViewModel.mLanguageOcr = null

        if (::mFloatingSubtitleReader.isInitialized)
            mFloatingSubtitleReader.destroy()

        closeFloatingOcr()

        if (::mFloatingButtons.isInitialized)
            mFloatingButtons.destroy()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (mHandler.hasCallbacks(mMonitoringBattery))
                mHandler.removeCallbacks(mMonitoringBattery)

            if (mHandler.hasCallbacks(mDismissTouchView))
                mHandler.removeCallbacks(mDismissTouchView)
        } else {
            mHandler.removeCallbacks(mMonitoringBattery)
            mHandler.removeCallbacks(mDismissTouchView)
        }

        if (::mBottomSheetTranslate.isInitialized) {
            mBottomSheetTranslate.removeBottomSheetCallback(mBottomSheetTranslateCallback)
        }
        
        if (::mBottomSheetConfigurations.isInitialized) {
            mBottomSheetConfigurations.removeBottomSheetCallback(mBottomSheetConfigurationsCallback)
        }

        super.onDestroy()
    }

    fun setFragment(fragment: Fragment) {
        closeFloatingOcr()
        mFragment = if (fragment is MangaReaderFragment) fragment else null
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.root_frame_manga_reader, fragment)
            .commit()
    }

    private fun optionsSave(any: Any?) {
        if (any == null)
            return

        when (any) {
            is ReaderMode -> mPreferences.edit()
                .putString(GeneralConsts.KEYS.READER.MANGA_READER_MODE, any.toString())
                .apply()

            is Boolean -> mPreferences.edit()
                .putBoolean(GeneralConsts.KEYS.READER.MANGA_SHOW_CLOCK_AND_BATTERY, any)
                .apply()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }

            R.id.manga_view_mode_aspect_fill -> optionsSave(ReaderMode.ASPECT_FILL)
            R.id.manga_view_mode_aspect_fit -> optionsSave(ReaderMode.ASPECT_FIT)
            R.id.manga_view_mode_fit_width -> optionsSave(ReaderMode.FIT_WIDTH)
            R.id.menu_item_reader_manga_popup_open_floating -> openFloatingSubtitle()
            R.id.menu_item_reader_manga_mark_page -> { }
            R.id.menu_item_reader_manga_popup_subtitle -> {
                val layout = if (mMenuPopupBottomSheet) mMenuPopupTranslateBottom else mMenuPopupTranslateLeft
                if (layout!!.isGone) {
                    if (mMenuPopupBottomSheet)
                        mMenuPopupConfigurationsBottom!!.visibility = View.GONE
                    else
                        mMenuPopupConfigurationsLeft!!.visibility = View.GONE

                    if (mMenuPopupBottomSheet)
                        mBottomSheetTranslate.state = BottomSheetBehavior.STATE_EXPANDED
                    else
                        mLeftSheetTranslate.state = SideSheetBehavior.STATE_EXPANDED

                    AnimationUtil.animatePopupOpen(this, layout, mMenuPopupBottomSheet, navigationColor = false)
                } else
                    AnimationUtil.animatePopupClose(this, layout, mMenuPopupBottomSheet, navigationColor = false)
            }

            R.id.menu_item_reader_manga_popup_color -> {
                val layout = if (mMenuPopupBottomSheet) mMenuPopupConfigurationsBottom else mMenuPopupConfigurationsLeft
                if (layout!!.isGone) {
                    if (mMenuPopupBottomSheet)
                        mMenuPopupTranslateBottom!!.visibility = View.GONE
                    else
                        mMenuPopupTranslateLeft!!.visibility = View.GONE

                    if (mMenuPopupBottomSheet)
                        mBottomSheetConfigurations.state = BottomSheetBehavior.STATE_EXPANDED
                    else
                        mLeftSheetConfigurations.state = SideSheetBehavior.STATE_EXPANDED

                    AnimationUtil.animatePopupOpen(this, layout, mMenuPopupBottomSheet, navigationColor = false)
                } else
                    AnimationUtil.animatePopupClose(this, layout, mMenuPopupBottomSheet, navigationColor = false)
            }

            R.id.menu_item_reader_manga_file_link -> openFileLink()
            R.id.menu_item_reader_manga_open_kaku -> {
                val launchIntent = packageManager.getLaunchIntentForPackage("ca.fuwafuwa.kaku")
                launchIntent?.let { startActivity(it) } ?: Toast.makeText(
                    application,
                    getString(R.string.open_app_kaku_not_founded),
                    Toast.LENGTH_SHORT
                ).show()
            }

            R.id.menu_item_reader_manga_floating_buttons -> openFloatingButtons()
            R.id.menu_item_reader_manga_view_touch_screen -> openViewTouch()
            R.id.menu_item_reader_manga_show_clock_and_battery -> {
                item.isChecked = !item.isChecked
                changeShowBatteryClock(item.isChecked)
            }

            R.id.menu_item_reader_manga_chapters_menu -> openChapters()
            R.id.menu_item_reader_manga_assistant -> openReadingAssistant()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showMenuFromButton(button: Button, view: View) {
        when (button.id) {
            R.id.reader_manga_btn_menu_ocr -> {
                val popup = PopupMenu(this, view, 0, R.attr.popupMenuStyle, R.style.PopupMenu)
                popup.menuInflater.inflate(R.menu.menu_ocr, popup.menu)
                popup.setOnMenuItemClickListener { menuItem: MenuItem ->
                    when (menuItem.itemId) {
                        R.id.menu_popup_ocr_tesseract -> openTesseract()
                        R.id.menu_popup_ocr_google_vision -> openGoogleVisionOcr(translate = false)
                        R.id.menu_popup_ocr_translate -> openGoogleVisionOcr(translate = true)
                    }
                    true
                }
                popup.setOnDismissListener {
                }
                popup.show()
            }
            R.id.reader_manga_btn_screen_rotate -> {
                val popup = PopupMenu(this, view, 0, R.attr.popupMenuStyle, R.style.PopupMenu)
                popup.menuInflater.inflate(R.menu.menu_orientation, popup.menu)
                popup.setOnMenuItemClickListener { menuItem: MenuItem ->
                    when (menuItem.itemId) {
                        R.id.menu_popup_orientation_auto -> { requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR }
                        R.id.menu_popup_orientation_portrait ->  { requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
                        R.id.menu_popup_orientation_portrait_locked -> {
                            val display = (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay
                            val rotation = display.rotation

                            requestedOrientation = if(rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_270)
                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            else
                                ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                        }
                        R.id.menu_popup_orientation_landscape -> { requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
                        R.id.menu_popup_orientation_landscape_locked -> {
                            val display = (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay
                            val rotation = display.rotation

                            requestedOrientation = if(rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90)
                                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            else
                                ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                        }
                    }
                    true
                }
                popup.setOnDismissListener {
                }
                popup.show()
            }
        }
    }

    fun applyFavoriteMenuIcon(item: MenuItem) {
        val icon = if (mManga != null && mManga!!.favorite)
            ContextCompat.getDrawable(this, R.drawable.ico_favorite_mark)
        else
            ContextCompat.getDrawable(this, R.drawable.ico_favorite_unmark)
        icon?.setTint(getColorFromAttr(R.attr.colorOnSecondary))
        item.icon = icon
    }

    fun changeFavorite(item: MenuItem) {
        if (mManga == null)
            return

        mManga?.favorite = !mManga!!.favorite
        mFragment?.syncMangaFavorite(mManga!!.favorite)

        item.setIcon(
            if (mManga!!.favorite) R.drawable.ico_animated_favorited_marked
            else R.drawable.ico_animated_favorited_unmarked
        )
        (item.icon as? AnimatedVectorDrawable)?.let { icon ->
            icon.reset()
            icon.start()
        }
        mRepository.update(mManga!!)
    }

    private fun prepareFloatingSubtitle() {
        mSubtitleController.subTitlePageSelected.observe(this) {
            mFloatingSubtitleReader.updatePage(it)
        }

        mSubtitleController.subTitleTextSelected.observe(this) {
            mFloatingSubtitleReader.updateText(it)
        }

        mSubtitleController.forceExpandFloatingPopup.observe(this) {
            if (mFloatingSubtitleReader.isShowing)
                mFloatingSubtitleReader.expanded(true)
        }

        mViewModel.ocrItem.observe(this) {
            mFloatingSubtitleReader.updateOcrList(it)
        }

        if (mSubtitleController.mManga != null && mSubtitleController.mManga!!.id != null && mSubtitleController.subTitleTextSelected.value == null) {
            val mSubtitleRepository = SubTitleRepository(applicationContext)
            val lastSubtitle = mSubtitleRepository.findByIdManga(mSubtitleController.mManga!!.id!!)
            if (lastSubtitle != null)
                mSubtitleController.initialize(lastSubtitle.chapterKey, lastSubtitle.pageKey)
        }
    }

    private fun prepareChapters() {
        val lineAdapter = MangaChaptersCardAdapter()
        mChapterList.adapter = lineAdapter
        val layout = GridLayoutManager(this, 1)
        layout.orientation = RecyclerView.HORIZONTAL
        mChapterList.layoutManager = layout

        val listener = object : ChapterCardListener {
            override fun onClick(page: Chapters) {
                mFragment?.setCurrentPage(page.page)
            }

            override fun onLongClick(page: Chapters) {

            }
        }

        lineAdapter.attachListener(listener)
        SharedData.chapters.observe(this) {
            lineAdapter.updateList(it.filter { p -> !p.isTitle })
        }
    }

    fun openFileLink() {
        if (mManga != null) {
            val intent = Intent(applicationContext, PagesLinkActivity::class.java)
            val bundle = Bundle()
            bundle.putSerializable(GeneralConsts.KEYS.OBJECT.LIBRARY, mLibrary)
            bundle.putSerializable(GeneralConsts.KEYS.OBJECT.MANGA, mManga)
            bundle.putInt(GeneralConsts.KEYS.MANGA.PAGE_NUMBER, mReaderProgress.progress)
            intent.putExtras(bundle)
            startActivity(intent)
        } else
            MaterialAlertDialogBuilder(this, R.style.AppCompatAlertDialogStyle)
                .setTitle(getString(R.string.page_link_manga_empty))
                .setMessage(getString(R.string.page_link_manga_empty_description))
                .setPositiveButton(R.string.action_neutral) { _, _ -> }
                .show()
    }

    private fun verifySubtitle() {
        if (!mViewModel.mIsAlertSubtitle && !mSubtitleController.isNotEmpty) {
            mViewModel.mIsAlertSubtitle = true
            val message = getString(
                if (mSubtitleController.isSelected) R.string.popup_reading_manga_subtitle_selected_empty
                else R.string.popup_reading_manga_subtitle_embedded_empty
            )

            MaterialAlertDialogBuilder(this, R.style.AppCompatAlertDialogStyle)
                .setTitle(getString(R.string.popup_reading_manga_subtitle_empty))
                .setMessage(message)
                .setPositiveButton(R.string.action_neutral) { _, _ -> }
                .show()
        }
    }

    fun openFloatingSubtitle() {
        if (mMenuPopupBottomSheet) {
            if (mMenuPopupConfigurationsBottom!!.isVisible)
                AnimationUtil.animatePopupClose(this, mMenuPopupConfigurationsBottom!!, mMenuPopupBottomSheet, navigationColor = false)

            if (mMenuPopupTranslateBottom!!.isVisible)
                AnimationUtil.animatePopupClose(this, mMenuPopupTranslateBottom!!, mMenuPopupBottomSheet, navigationColor = false)
        } else {
            if (mMenuPopupConfigurationsLeft!!.isVisible)
                AnimationUtil.animatePopupClose(this, mMenuPopupConfigurationsLeft!!, mMenuPopupBottomSheet, navigationColor = false)

            if (mMenuPopupTranslateLeft!!.isVisible)
                AnimationUtil.animatePopupClose(this, mMenuPopupTranslateLeft!!, mMenuPopupBottomSheet, navigationColor = false)
        }

        if (mFloatingSubtitleReader.isShowing)
            mFloatingSubtitleReader.dismiss()
        else {
            if (ComponentsUtil.canDrawOverlays(applicationContext)) {
                verifySubtitle()
                mFloatingSubtitleReader.show()
            } else
                startManageDrawOverlaysPermission()
        }
    }

    private fun openFloatingButtons() {
        if (!::mFloatingButtons.isInitialized)
            mFloatingButtons = FloatingButtons(this, this, findViewById(R.id.root_activity_manga_reader))

        if (mFloatingButtons.isShowing)
            mFloatingButtons.dismiss()
        else
            mFloatingButtons.show()
    }

    private fun getIconForTouchScreen(touchScreen: TouchScreen): Int {
        return when (touchScreen) {
            TouchScreen.TOUCH_NOT_ASSIGNED -> R.drawable.ico_close
            TouchScreen.TOUCH_ASPECT_FIT -> R.drawable.ico_reader_mode
            TouchScreen.TOUCH_FIT_WIDTH -> R.drawable.ico_reading_mode
            TouchScreen.TOUCH_CHAPTER_LIST -> R.drawable.ico_item_chapters_menu
            TouchScreen.TOUCH_NEXT_FILE -> R.drawable.ico_tts_next
            TouchScreen.TOUCH_PREVIOUS_FILE -> R.drawable.ico_tts_previous
            TouchScreen.TOUCH_NEXT_PAGE -> R.drawable.ico_animated_text_next
            TouchScreen.TOUCH_PREVIOUS_PAGE -> R.drawable.ico_animated_text_before
            TouchScreen.TOUCH_SHARE_IMAGE -> R.drawable.ico_save_share_image
            TouchScreen.TOUCH_PAGE_MARK -> R.drawable.ico_book_reader_page_mark
            else -> 0
        }
    }

    private fun setupTouchZone(card: View, touchScreen: TouchScreen, isPrimary: Boolean, isGlass: Boolean) {
        var textView: TextView? = null
        var imageView: ImageView? = null
        var blurView: BlurView? = null

        fun findViews(view: View) {
            if (view is TextView) {
                textView = view
            } else if (view is ImageView) {
                imageView = view
            } else if (view is BlurView) {
                blurView = view
            }
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    findViews(view.getChildAt(i))
                }
            }
        }

        findViews(card)

        if (card is com.google.android.material.card.MaterialCardView) {
            card.setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        val tv = textView
        if (tv != null) {
            tv.text = getString(touchScreen.getValue())
        }

        val iv = imageView
        if (iv != null) {
            val iconRes = getIconForTouchScreen(touchScreen)
            if (iconRes != 0) {
                iv.setImageResource(iconRes)
                iv.visibility = View.VISIBLE
            } else {
                iv.visibility = View.GONE
            }
        }

        val bv = blurView
        if (bv != null) {
            val baseColor = if (isPrimary) {
                getColorFromAttr(R.attr.colorPrimaryContainer)
            } else {
                ContextCompat.getColor(this, R.color.touch_demonstration_alter)
            }

            val alpha = if (isGlass) 0x66 else 0xA6 // 40% for Glass, 65% (35% transparency) for Flat
            val dynamicColor = (baseColor and 0x00FFFFFF) or (alpha shl 24)

            val cornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics)
            val shapeBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(dynamicColor)
                this.cornerRadius = cornerRadius
            }

            bv.background = shapeBg
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                bv.clipToOutline = true
            }
            bv.outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
                }
            }

            bv.setBlurEnabled(isGlass && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            if (isGlass && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val decorView = window.decorView
                val rootView = decorView.findViewById<ViewGroup>(android.R.id.content)
                val background = decorView.background ?: android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK)
                GlassSetup.setupGlass(bv, rootView, RenderEffectBlur())
                    .setFrameClearDrawable(background)
                    .setBlurRadius(15f)
                bv.setBlurAutoUpdate(true)
                mHandler.postDelayed({
                    bv.setBlurAutoUpdate(false)
                }, 100)
            } else {
                bv.setBlurAutoUpdate(false)
            }
        }
    }

    fun openViewTouch() {
        mFragment?.setFullscreen(true)

        val touch = TouchUtils.getTouch(this, Type.MANGA)

        val touchTop = findViewById<View>(R.id.reader_manga_touch_top)
        val touchTopRight = findViewById<View>(R.id.reader_manga_touch_top_right)
        val touchTopLeft = findViewById<View>(R.id.reader_manga_touch_top_left)
        val touchLeft = findViewById<View>(R.id.reader_manga_touch_left)
        val touchRight = findViewById<View>(R.id.reader_manga_touch_right)
        val touchBottom = findViewById<View>(R.id.reader_manga_touch_bottom)
        val touchBottomLeft = findViewById<View>(R.id.reader_manga_touch_bottom_left)
        val touchBottomRight = findViewById<View>(R.id.reader_manga_touch_bottom_right)

        val sharedPreferences = GeneralConsts.getSharedPreferences(this)
        val isGlass = sharedPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)

        val showTopCorners = !((touch[Position.CORNER_TOP_RIGHT] == TouchScreen.TOUCH_NOT_ASSIGNED && touch[Position.CORNER_TOP_LEFT] == TouchScreen.TOUCH_NOT_ASSIGNED) ||
                (touch[Position.CORNER_TOP_RIGHT] == touch[Position.RIGHT] && touch[Position.CORNER_TOP_LEFT] == touch[Position.LEFT]))

        val showBottomCorners = !((touch[Position.CORNER_BOTTOM_RIGHT] == TouchScreen.TOUCH_NOT_ASSIGNED && touch[Position.CORNER_BOTTOM_LEFT] == TouchScreen.TOUCH_NOT_ASSIGNED) ||
                (touch[Position.CORNER_BOTTOM_RIGHT] == touch[Position.RIGHT] && touch[Position.CORNER_BOTTOM_LEFT] == touch[Position.LEFT]))

        touchTopRight.visibility = if (showTopCorners) View.VISIBLE else View.GONE
        touchTopLeft.visibility = if (showTopCorners) View.VISIBLE else View.GONE
        touchBottomRight.visibility = if (showBottomCorners) View.VISIBLE else View.GONE
        touchBottomLeft.visibility = if (showBottomCorners) View.VISIBLE else View.GONE

        setupTouchZone(touchTop, touch[Position.TOP]!!, isPrimary = !showTopCorners, isGlass = isGlass)
        setupTouchZone(touchTopRight, touch[Position.CORNER_TOP_RIGHT]!!, isPrimary = true, isGlass = isGlass)
        setupTouchZone(touchTopLeft, touch[Position.CORNER_TOP_LEFT]!!, isPrimary = true, isGlass = isGlass)
        setupTouchZone(touchLeft, touch[Position.LEFT]!!, isPrimary = false, isGlass = isGlass)
        setupTouchZone(touchRight, touch[Position.RIGHT]!!, isPrimary = false, isGlass = isGlass)
        setupTouchZone(touchBottom, touch[Position.BOTTOM]!!, isPrimary = !showBottomCorners, isGlass = isGlass)
        setupTouchZone(touchBottomLeft, touch[Position.CORNER_BOTTOM_LEFT]!!, isPrimary = true, isGlass = isGlass)
        setupTouchZone(touchBottomRight, touch[Position.CORNER_BOTTOM_RIGHT]!!, isPrimary = true, isGlass = isGlass)

        mTouchView.animate().cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (mHandler.hasCallbacks(mDismissTouchView))
                mHandler.removeCallbacks(mDismissTouchView)
        } else {
            mHandler.removeCallbacks(mDismissTouchView)
        }

        mTouchView.alpha = 0.0f
        mTouchView.visibility = View.VISIBLE
        mTouchView.animate().alpha(1.0f).setDuration(300L)
            .setListener(null)
            .withEndAction {
                mTouchView.alpha = 1f
                mTouchView.visibility = View.VISIBLE
            }

        mHandler.postDelayed(mDismissTouchView, 5000)
    }

    fun touchPosition(touchScreen: TouchScreen): Boolean {
        if (touchScreen != TouchScreen.TOUCH_CHAPTER_LIST && mChapterContent.isVisible) {
            chapterVisibility(false)
            return true
        }

        return when (touchScreen) {
            TouchScreen.TOUCH_FIT_WIDTH -> {
                mFragment?.changeAspect(mToolBar, ReaderMode.FIT_WIDTH)
                true
            }

            TouchScreen.TOUCH_ASPECT_FIT -> {
                mFragment?.changeAspect(mToolBar, ReaderMode.ASPECT_FIT)
                true
            }

            TouchScreen.TOUCH_NEXT_FILE -> {
                mFragment?.hitEnding()
                true
            }

            TouchScreen.TOUCH_PREVIOUS_FILE -> {
                mFragment?.hitBeginning()
                true
            }

            TouchScreen.TOUCH_CHAPTER_LIST -> {
                val initial = mFragment?.getCurrentPage() ?: 0
                val loaded = mViewModel.loadChapter(mManga, initial)
                chapterVisibility(true)
                mFragment?.let {
                    if (loaded)
                        mChapterList.scrollToPosition(it.getCurrentPage() - 1)
                    else
                        mChapterList.smoothScrollToPosition(it.getCurrentPage() - 1)

                    SharedData.selectPage(it.getCurrentPage())
                }
                true
            }

            else -> false
        }
    }

    private fun closeViewTouch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (mHandler.hasCallbacks(mDismissTouchView))
                mHandler.removeCallbacks(mDismissTouchView)
        } else {
            mHandler.removeCallbacks(mDismissTouchView)
        }

        if (mTouchView.isGone)
            return

        mTouchView.animate().cancel()
        mTouchView.animate().alpha(0.0f).setDuration(300L)
            .setListener(null)
            .withEndAction {
                mTouchView.visibility = View.GONE
            }
    }

    private fun chapterVisibility(isVisible: Boolean) {
        val visibility = if (isVisible) View.VISIBLE else View.GONE
        val finalAlpha = if (isVisible) 1.0f else 0.0f
        val initialAlpha = if (isVisible) 0.0f else 1.0f

        if (isVisible) {
            mChapterContent.visibility = View.VISIBLE
            mChapterContent.alpha = initialAlpha
        }

        mChapterContent.animate().alpha(finalAlpha).setDuration(300L)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    mChapterContent.visibility = visibility
                }
            })
    }

    private val chaptersLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (data?.extras != null && data.extras!!.containsKey(GeneralConsts.KEYS.CHAPTERS.PAGE)) {
            val page = data.extras!!.getInt(GeneralConsts.KEYS.CHAPTERS.PAGE)
            mFragment?.setCurrentPage(page)
        }
        mFragment?.setFullscreen(true)
    }

    private val drawOverlaysLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (ComponentsUtil.canDrawOverlays(applicationContext)) {
            verifySubtitle()
            mFloatingSubtitleReader.show()
        } else
            Toast.makeText(
                application,
                getString(R.string.floating_reading_not_permission),
                Toast.LENGTH_SHORT
            ).show()
    }

    private fun startManageDrawOverlaysPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${applicationContext.packageName}")
        )
        drawOverlaysLauncher.launch(intent)
    }


    private fun openChapters() {
        val initial = (mFragment?.getCurrentPage() ?: 1) - 1
        mViewModel.loadChapter(mManga, initial)

        val intent = Intent(this, MenuActivity::class.java)
        val bundle = Bundle()
        bundle.putInt(GeneralConsts.KEYS.FRAGMENT.ID, R.id.frame_chapters)
        bundle.putString(GeneralConsts.KEYS.CHAPTERS.TITLE, mManga?.title ?: "")
        bundle.putInt(GeneralConsts.KEYS.CHAPTERS.PAGE, initial)
        intent.putExtras(bundle)
        overrideActivityTransitionCompat(R.anim.fade_in_fragment_add_enter, R.anim.fade_out_fragment_remove_exit)
        chaptersLauncher.launch(intent)
    }

    inner class ViewPagerAdapter(fa: androidx.fragment.app.FragmentActivity) :
        FragmentStateAdapter(fa) {
        private val fragments: MutableList<Fragment> = ArrayList()
        private val fragmentTitle: MutableList<String> = ArrayList()
        fun addFragment(fragment: Fragment, title: String) {
            fragments.add(fragment)
            fragmentTitle.add(title)
        }

        fun getItem(position: Int): Fragment {
            return fragments[position]
        }

        override fun createFragment(position: Int): Fragment {
            return fragments[position]
        }

        override fun getItemCount(): Int {
            return fragments.size
        }

        fun getPageTitle(position: Int): CharSequence {
            return fragmentTitle[position]
        }
    }

    private fun choiceLanguage(selected: (language: Languages) -> (Unit)) {
        if  (mDialog != null) return

        val mapLanguage = Util.getLanguages(this)
        val items = mapLanguage.keys.filterNot { it == Util.googleLang }.toTypedArray()

        mDialog = MaterialAlertDialogBuilder(this, R.style.AppCompatMaterialAlertList)
            .setTitle(getString(R.string.languages_choice))
            .setItems(items) { _, selectItem ->
                val language = mapLanguage[items[selectItem]]
                if (language != null) {
                    setLanguage(language)
                    selected(language)
                }
            }
            .setOnDismissListener { mDialog = null }
            .create()
        mDialog?.show()
    }

    private fun openTesseract() {
        if (!closeFloatingOcr(isOnlyDismiss = true)) {
            if (mViewModel.mLanguageOcr == null)
                choiceLanguage {
                    mViewModel.mLanguageOcr = it
                    openFloatingOcr()
                }
            else
                openFloatingOcr()
        }
    }

    private fun openFloatingOcr() {
        if (mFloatingWindowOcr == null)
            mFloatingWindowOcr = FloatingOcr(this, this, findViewById(R.id.frame_reader_root))

        mFloatingWindowOcr!!.show()
    }

    private fun closeFloatingOcr(isOnlyDismiss: Boolean = false) : Boolean {
        return if (mFloatingWindowOcr != null) {
            val isVisible = mFloatingWindowOcr!!.isShowing
            if (isVisible)
                mFloatingWindowOcr!!.dismiss()

            if (!isOnlyDismiss) {
                mLastFloatingWindowOcr = isVisible
                mFloatingWindowOcr!!.destroy()
                mFloatingWindowOcr = null
            }
            isVisible
        } else
            false
    }

    private fun openGoogleVisionOcr(translate: Boolean = false) {
        val image = getImage() ?: return
        GoogleVision.getInstance(this).process(image, mViewModel.mLanguageOcr, translate) { setText(it) }
    }

    private fun openReadingAssistant() {
        val manga = mManga ?: return
        val parse = mFragment?.mParse
        ReadingAssistantActivity.prepareManga(
            title = manga.title.ifBlank { manga.name },
            page = MangaReaderFragment.mCurrentPage,
            mangaId = manga.id,
            parse = parse,
            ocrLanguage = mViewModel.mLanguageOcr
        )
        startActivity(Intent(this, ReadingAssistantActivity::class.java))
    }

    override fun getImage(): Bitmap? {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.root_frame_manga_reader) ?: return null
        val view = (currentFragment as MangaReaderFragment).getCurrencyImageView() ?: return null
        return view.drawable.toBitmap()
    }

    override fun getImage(x: Int, y: Int, width: Int, height: Int): Bitmap? {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.root_frame_manga_reader) ?: return null
        val view = (currentFragment as MangaReaderFragment).getCurrencyImageView() ?: return null
        view.isDrawingCacheEnabled = true
        view.buildDrawingCache()
        val screenshot = view.drawingCache
        val image = Bitmap.createBitmap(screenshot, x, y, width, height, null, false)
            .copy(screenshot.config, true)
        view.isDrawingCacheEnabled = false
        return image
    }

    override fun getLanguage(): Languages? {
        if (mViewModel.mLanguageOcr == null)
            choiceLanguage {
                mViewModel.mLanguageOcr = it
            }
        return mViewModel.mLanguageOcr
    }

    override fun setText(text: String?) {
        if (::mFloatingSubtitleReader.isInitialized) {
            mViewModel.mIsAlertSubtitle = true
            mViewModel.addOcrItem(text)
            mFloatingSubtitleReader.updateTextOcr(text)
            mFloatingSubtitleReader.showWithoutDismiss()
        }
    }

    override fun setText(text: ArrayList<String>) {
        if (::mFloatingSubtitleReader.isInitialized) {
            mViewModel.mIsAlertSubtitle = true
            mViewModel.addOcrItem(text)
            mFloatingSubtitleReader.showWithoutDismiss()
            mFloatingSubtitleReader.changeLayout(false)
        }
    }

    override fun clearList() {
        mViewModel.clearOcrItem()
    }

    private fun getBatteryPercent() {
        try {
            val percent = (getSystemService(BATTERY_SERVICE) as BatteryManager).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            mBattery.text = getString(R.string.percent, percent)
        } finally {
            mHandler.postDelayed(mMonitoringBattery, 60000)
        }
    }

    override fun onLoading(page: Int) {
        if (!mChapterList.isComputingLayout)
            mChapterList.adapter?.notifyItemChanged(page)
    }

    override fun setCurrentPage(page: Int) {
        mFragment?.setCurrentPage(page)
    }

    private fun setupPopupBackgrounds() {
        PopupUtils.setupPopupBackgrounds(this, mMenuPopupTranslateBottom, mMenuPopupTranslateBackground)
        PopupUtils.setupPopupBackgrounds(this, mMenuPopupConfigurationsBottom, mMenuPopupConfigurationsBackground)
    }

    private fun setupBottomSheetInsets() {
        mMenuPopupTranslateBottom?.let { bottomSheet ->
            ViewCompat.setOnApplyWindowInsetsListener(bottomSheet) { view, insets ->
                val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, navBarHeight)
                insets
            }
        }
        mMenuPopupConfigurationsBottom?.let { bottomSheet ->
            ViewCompat.setOnApplyWindowInsetsListener(bottomSheet) { view, insets ->
                val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, navBarHeight)
                insets
            }
        }
    }

}
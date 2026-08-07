package br.com.fenix.bilingualreader

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import br.com.fenix.bilingualreader.databinding.ActivityMainBinding
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.enums.Themes
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.listener.MainListener
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.service.repository.LibraryRepository
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.LibraryUtil
import br.com.fenix.bilingualreader.util.helpers.MenuUtil
import br.com.fenix.bilingualreader.util.helpers.MsgUtil
import br.com.fenix.bilingualreader.util.helpers.Notifications
import br.com.fenix.bilingualreader.util.helpers.Telemetry
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil
import br.com.fenix.bilingualreader.util.helpers.blurOnceDeferred
import br.com.fenix.bilingualreader.view.components.GlassRenderScheduler
import br.com.fenix.bilingualreader.view.ui.about.AboutFragment
import br.com.fenix.bilingualreader.view.ui.annotation.AnnotationFragment
import br.com.fenix.bilingualreader.view.ui.configuration.ConfigFragment
import br.com.fenix.bilingualreader.view.ui.help.HelpFragment
import br.com.fenix.bilingualreader.view.ui.history.HistoryFragment
import br.com.fenix.bilingualreader.view.ui.library.book.BookLibraryFragment
import br.com.fenix.bilingualreader.view.ui.library.book.BookLibraryViewModel
import br.com.fenix.bilingualreader.view.ui.library.manga.MangaLibraryFragment
import br.com.fenix.bilingualreader.view.ui.library.manga.MangaLibraryViewModel
import br.com.fenix.bilingualreader.view.ui.statistics.StatisticsFragment
import br.com.fenix.bilingualreader.view.ui.vocabulary.VocabularyFragment
import com.google.android.material.navigation.NavigationView
import eightbitlab.com.blurview.BlurView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDate

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, MainListener {

    private val mLOGGER = LoggerFactory.getLogger(MainActivity::class.java)

    private lateinit var mPreferences: SharedPreferences

    private lateinit var mMangaLibraryModel: MangaLibraryViewModel
    private lateinit var mBookLibraryModel: BookLibraryViewModel
    private lateinit var mBlurTop: BlurView
    private lateinit var mToolBar: Toolbar
    private lateinit var mFragmentManager: FragmentManager
    private lateinit var mNavigationView: NavigationView
    private lateinit var mMenu: Menu
    private lateinit var mToggle: ActionBarDrawerToggle
    private lateinit var mDrawer: DrawerLayout

    private lateinit var mBinding: ActivityMainBinding

    private val mHandler = Handler(Looper.getMainLooper())
    private val mDefaultUncaughtHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun onCreate(savedInstanceState: Bundle?) {
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            mLOGGER.error("*** CRASH APP *** ", e)
            Telemetry.recordException(e, "*** CRASH APP ***: " + e.message)
            mDefaultUncaughtHandler?.uncaughtException(t, e)
        }

        mMangaLibraryModel = ViewModelProvider(this)[MangaLibraryViewModel::class.java]
        mBookLibraryModel = ViewModelProvider(this)[BookLibraryViewModel::class.java]

        installSplashScreen().setKeepOnScreenCondition {
            mMangaLibraryModel.isLoading || mBookLibraryModel.isLoading
        }

        val isDark = ThemeUtil.applyThemeMode(this)

        mPreferences = GeneralConsts.getSharedPreferences(this)
        val theme = Themes.valueOf(mPreferences.getString(GeneralConsts.KEYS.THEME.THEME_USED, Themes.ORIGINAL.toString())!!)
        setTheme(theme.getValue())

        super.onCreate(savedInstanceState)

        ThemeUtil.statusBarTransparentTheme(window, isDark, isLightStatus = !isDark)

        initializeBook()
        createNotificationChannel()
        DataBase.initializeBackup(this)

        mBinding = ActivityMainBinding.inflate(layoutInflater)
        val view = mBinding.root
        setContentView(view)

        mToolBar = findViewById(R.id.main_toolbar)
        mBlurTop = findViewById(R.id.main_blur_top)

        MenuUtil.tintToolbar(mToolBar, theme)
        setSupportActionBar(mToolBar)

        // drawer_Layout is a default layout from app
        mDrawer = mBinding.drawerLayout
        mToggle = ActionBarDrawerToggle(
            this,
            mDrawer,
            mToolBar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        mDrawer.addDrawerListener(mToggle)
        mDrawer.addDrawerListener(object : androidx.drawerlayout.widget.DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerOpened(drawerView: View) {
                setNavigatorBlurAutoUpdate(true)
            }
            override fun onDrawerClosed(drawerView: View) {
                setNavigatorBlurAutoUpdate(false)
            }
            override fun onDrawerStateChanged(newState: Int) {}
        })
        mToggle.syncState()

        // nav_view have a menu layout
        mNavigationView = mBinding.navView
        mNavigationView.setNavigationItemSelectedListener(this)

        mFragmentManager = supportFragmentManager
        mFragmentManager.registerFragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                super.onFragmentResumed(fm, f)
                when (f) {
                    is HistoryFragment -> mToolBar.title = getString(R.string.menu_history)
                    is VocabularyFragment -> mToolBar.title = getString(R.string.menu_vocabulary)
                    is StatisticsFragment -> mToolBar.title = getString(R.string.menu_statistics)
                    is ConfigFragment -> mToolBar.title = getString(R.string.menu_config)
                    is HelpFragment -> mToolBar.title = getString(R.string.menu_help)
                    is AboutFragment -> mToolBar.title = getString(R.string.menu_about)
                    is AnnotationFragment -> mToolBar.title = getString(R.string.menu_annotations)
                }

                val isGlass = mPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)
                if (isGlass) {
                    mBlurTop.blurOnceDeferred(mHandler, 100)
                }
            }
        }, false)

        lifecycleScope.launch(Dispatchers.IO) {
            val defaultManga = LibraryUtil.getDefault(this@MainActivity, Type.MANGA)
            val defaultBook = LibraryUtil.getDefault(this@MainActivity, Type.BOOK)

            var loadedLibraries = listOf<Library>()
            try {
                val repository = LibraryRepository(this@MainActivity)
                loadedLibraries = repository.listEnabled()
            } catch (e: Exception) {
                mLOGGER.error("Error loading libraries: " + e.message, e)
                Telemetry.recordException(e, "Error loading libraries: " + e.message)
            }

            withContext(Dispatchers.Main) {
                mMangaLibraryModel.setDefaultLibrary(defaultManga)
                mBookLibraryModel.setDefaultLibrary(defaultBook)

                if (loadedLibraries.isNotEmpty()) {
                    setLibraries(loadedLibraries)
                }

                var fragment: Fragment
                if (mPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_CHANGE, false)) {
                    mPreferences.edit(commit = true) {
                        this.putBoolean(GeneralConsts.KEYS.THEME.THEME_CHANGE, false)
                    }

                    mMangaLibraryModel.isLoading = false
                    mBookLibraryModel.isLoading = false
                    fragment = ConfigFragment()
                } else {
                    val idLibrary = mPreferences.getLong(GeneralConsts.KEYS.LIBRARY.LAST_LIBRARY, GeneralConsts.KEYS.LIBRARY.DEFAULT_MANGA)
                    val library = loadedLibraries.find { it.id == idLibrary } ?: if (idLibrary.compareTo(R.id.menu_book_library_default) == 0)
                        defaultBook
                    else
                        defaultManga

                    fragment = when (library.type) {
                        Type.MANGA -> {
                            mMangaLibraryModel.setLibrary(library)
                            mBookLibraryModel.isLoading = false
                            MangaLibraryFragment()
                        }

                        Type.BOOK -> {
                            mBookLibraryModel.setLibrary(library)
                            mMangaLibraryModel.isLoading = false
                            BookLibraryFragment()
                        }
                    }

                    intent.dataString?.let {
                        mMangaLibraryModel.isLoading = false
                        mBookLibraryModel.isLoading = false
                        fragment = when (it) {
                            "history" -> HistoryFragment()
                            else -> fragment
                        }
                    }
                }

                // content_fragment use for receive fragments layout
                mFragmentManager.beginTransaction().replace(R.id.main_content_root, fragment).commit()

                setupBlurViews()
                setupWindowInsets()
                setupTitleBackgrounds()
            }
        }
    }

    private fun clearCache() {
        val cacheDir = GeneralConsts.getCacheDir(this)
        CoroutineScope(Dispatchers.IO).launch {
            async {
                try {
                    val rar = File(cacheDir, GeneralConsts.CACHE_FOLDER.RAR)
                    if (rar.exists())
                        rar.listFiles()?.let {
                            for (f in it)
                                f.delete()
                        }

                    val images = File(cacheDir, GeneralConsts.CACHE_FOLDER.IMAGE)
                    if (images.exists())
                        images.listFiles()?.let {
                            for (f in it)
                                f.delete()
                        }
                } catch (e: Exception) {
                    mLOGGER.error("Error clearing cache folders: " + e.message, e)
                    Telemetry.recordException(e, "Error clearing cache folders: " + e.message)
                }
            }
        }
    }

    private fun libraries() {
        try {
            val repository = LibraryRepository(this@MainActivity)
            val libraries = repository.listEnabled()
            if (libraries.isNotEmpty())
                setLibraries(libraries)
        } catch (e: Exception) {
            mLOGGER.error("Error loading libraries: " + e.message, e)
            Telemetry.recordException(e, "Error loading libraries: " + e.message)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        mMenu = mNavigationView.menu

        val fragment = supportFragmentManager.findFragmentById(item.itemId)
        val newFragment = fragment ?: when (item.itemId) {
            R.id.menu_manga_library_default -> {
                mMangaLibraryModel.setLibrary(LibraryUtil.getDefault(this, Type.MANGA))
                MangaLibraryFragment()
            }

            R.id.menu_book_library_default -> {
                mBookLibraryModel.setLibrary(LibraryUtil.getDefault(this, Type.BOOK))
                BookLibraryFragment()
            }

            R.id.menu_configuration -> ConfigFragment()
            R.id.menu_statistics -> StatisticsFragment()
            R.id.menu_annotations -> AnnotationFragment()
            R.id.menu_help -> HelpFragment()
            R.id.menu_about -> AboutFragment()
            R.id.menu_history -> HistoryFragment()
            R.id.menu_vocabulary -> VocabularyFragment()
            in GeneralConsts.KEYS.LIBRARIES.MANGA_INDEX_LIBRARIES..(GeneralConsts.KEYS.LIBRARIES.MANGA_INDEX_LIBRARIES + mLibraries.filter { it.type == Type.MANGA }.size) -> {
                val library = mLibraries.find { it.menuKey == item.itemId } ?: LibraryUtil.getDefault(this, Type.MANGA)
                mMangaLibraryModel.setLibrary(library)
                MangaLibraryFragment()
            }

            in GeneralConsts.KEYS.LIBRARIES.BOOK_INDEX_LIBRARIES..(GeneralConsts.KEYS.LIBRARIES.BOOK_INDEX_LIBRARIES + mLibraries.filter { it.type == Type.BOOK }.size) -> {
                val library = mLibraries.find { it.menuKey == item.itemId } ?: LibraryUtil.getDefault(this, Type.BOOK)
                mBookLibraryModel.setLibrary(library)
                BookLibraryFragment()
            }

            else -> null
        }

        mDrawer.closeDrawer(GravityCompat.START)

        if (newFragment != null) {
            lifecycleScope.launch {
                delay(250)
                if (!isFinishing && !isDestroyed) {
                    openFragment(newFragment)
                }
            }
        }

        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == GeneralConsts.REQUEST.PERMISSION_FILES_ACCESS) {
            MsgUtil.validPermission(this, grantResults)
        }
    }

    override fun showUpButton() {
        //mDrawer.removeDrawerListener(mToggle)
        //supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }


    override fun hideUpButton() {
        //Achar uma forma de habilitar e desabilitar o menu principal
        //mDrawer.addDrawerListener(mToggle)
        //supportActionBar?.setDisplayHomeAsUpEnabled(false)
        //supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    override fun changeLibraryTitle(library: String) {
        mToolBar.title = library
    }

    override fun clearLibraryTitle() {
        mToolBar.title = getString(R.string.app_name)
    }

    private fun openFragment(fragment: Fragment) {
        if (fragment is MangaLibraryFragment)
            mMangaLibraryModel.saveLastLibrary()
        else if (fragment is BookLibraryFragment)
            mBookLibraryModel.saveLastLibrary()

        mFragmentManager.beginTransaction().setCustomAnimations(R.anim.slide_fragment_add_enter, R.anim.slide_fragment_add_exit,
            R.anim.slide_fragment_remove_enter, R.anim.slide_fragment_remove_exit)
            .replace(R.id.main_content_root, fragment)
            .addToBackStack(null)
            .commit()
    }

    private var mLibraries: List<Library> = listOf()
    private fun cleanLibraries(menu: Menu) {
        var submenu: SubMenu? = menu.findItem(R.id.menu_manga_library_content).subMenu
        var list = mLibraries.filter { it.type == Type.MANGA }

        for ((index, _) in list.withIndex())
            submenu?.removeItem(GeneralConsts.KEYS.LIBRARIES.MANGA_INDEX_LIBRARIES + index)

        submenu = menu.findItem(R.id.menu_book_library_content).subMenu
        list = mLibraries.filter { it.type == Type.BOOK }

        for ((index, _) in list.withIndex())
            submenu?.removeItem(GeneralConsts.KEYS.LIBRARIES.BOOK_INDEX_LIBRARIES + index)
    }

    private fun setLibraries(submenu: SubMenu?, type: Type, libraries: List<Library>) {
        val list = libraries.filter { it.type == type }
        val icon = if (type == Type.BOOK) R.drawable.ico_library_book else R.drawable.ico_library_manga
        for ((index, library) in list.withIndex())
            submenu?.let {
                val key =
                    (if (type == Type.BOOK) GeneralConsts.KEYS.LIBRARIES.BOOK_INDEX_LIBRARIES else GeneralConsts.KEYS.LIBRARIES.MANGA_INDEX_LIBRARIES) + index
                library.menuKey = key
                it.add(0, key, 0, library.title).apply { setIcon(icon) }
            }
    }

    fun setLibraries(libraries: List<Library>) {
        val menu = mNavigationView.menu

        cleanLibraries(menu)
        setLibraries(menu.findItem(R.id.menu_manga_library_content).subMenu, Type.MANGA, libraries)
        setLibraries(menu.findItem(R.id.menu_book_library_content).subMenu, Type.BOOK, libraries)

        mLibraries = libraries
        mNavigationView.invalidate()
    }

    private fun initializeBook() {
        DocumentParse.init(this)
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Notifications.NOTIFICATIONS_CHANNEL_ID,
                getString(R.string.notifications_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notifications_channel_description)
            }
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        mHandler.removeCallbacksAndMessages(null)
        NotificationManagerCompat.from(this).cancelAll()
        if (isFinishing) {
            clearCache()

            if (LocalDate.now().isAfter(LocalDate.parse(mPreferences.getString(GeneralConsts.KEYS.DATABASE.LAST_AUTO_BACKUP, "2025-01-01")))) {
                mPreferences.edit(commit = true) { putString(GeneralConsts.KEYS.DATABASE.LAST_AUTO_BACKUP, LocalDate.now().toString()) }
                DataBase.autoBackupDatabase(this)
            }
        }

        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        val isDark = ThemeUtil.applyThemeMode(this)
        val theme = Themes.valueOf(mPreferences.getString(GeneralConsts.KEYS.THEME.THEME_USED, Themes.ORIGINAL.toString())!!)
        setTheme(theme.getValue())
        ThemeUtil.statusBarTransparentTheme(window, isDark, isLightStatus = !isDark)
        setupTitleBackgrounds()

        val isGlass = mPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)
        mBlurTop.setBlurEnabled(isGlass)
        if (isGlass) {
            mBlurTop.blurOnceDeferred(mHandler, 100)
        } else {
            mBlurTop.setBlurAutoUpdate(false)
        }
        setNavigatorBlurAutoUpdate(mDrawer.isDrawerOpen(GravityCompat.START))
    }

    override fun onPause() {
        super.onPause()
        mBlurTop.setBlurAutoUpdate(false)
        mBlurTop.setBlurEnabled(false)
        setNavigatorBlurAutoUpdate(false)
    }

    val blurViews: List<BlurView>
        get() {
            val list = mutableListOf<BlurView>()
            if (::mBlurTop.isInitialized) list.add(mBlurTop)
            if (::mNavigationView.isInitialized) {
                val headerView = mNavigationView.getHeaderView(0)
                val navigatorBlur = headerView?.findViewById<BlurView>(R.id.navigator_blur)
                if (navigatorBlur != null) list.add(navigatorBlur)
            }
            return list
        }

    fun setBlurAutoUpdate(enabled: Boolean) {
        val isGlass = mPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)
        if (isGlass) {
            mBlurTop.setBlurAutoUpdate(enabled)
            if (!enabled) {
                GlassRenderScheduler.requestUpdate(mBlurTop)
            }
        } else {
            mBlurTop.setBlurAutoUpdate(false)
        }
    }

    fun blurOnceDeferred(delayMs: Long) {
        val isGlass = mPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)
        if (isGlass && ::mBlurTop.isInitialized) {
            mBlurTop.blurOnceDeferred(mHandler, delayMs)
        }
    }

    private fun setNavigatorBlurAutoUpdate(enabled: Boolean) {
        if (!::mNavigationView.isInitialized)
            return

        val headerView = mNavigationView.getHeaderView(0)
        val navigatorBlur = headerView?.findViewById<BlurView>(R.id.navigator_blur)
        val isGlass = mPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)
        navigatorBlur?.setBlurEnabled(isGlass)
        if (isGlass) {
            navigatorBlur?.setBlurAutoUpdate(enabled)
            if (!enabled && navigatorBlur != null) {
                GlassRenderScheduler.requestUpdate(navigatorBlur)
            }
        } else {
            navigatorBlur?.setBlurAutoUpdate(false)
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        val isDark = ThemeUtil.applyThemeMode(this)
        val theme = Themes.valueOf(mPreferences.getString(GeneralConsts.KEYS.THEME.THEME_USED, Themes.ORIGINAL.toString())!!)
        setTheme(theme.getValue())
        super.onConfigurationChanged(newConfig)
        ThemeUtil.statusBarTransparentTheme(window, isDark, isLightStatus = !isDark)
        setupTitleBackgrounds()
    }

    private fun setupWindowInsets() {
        if (!::mBlurTop.isInitialized)
            return

        val mainContentRoot = findViewById<View>(R.id.main_content_root) ?: return

        ViewCompat.setOnApplyWindowInsetsListener(mBlurTop) { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(view.paddingLeft, statusBarHeight, view.paddingRight, view.paddingBottom)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(mainContentRoot) { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(view.paddingLeft, statusBarHeight, view.paddingRight, 0)
            insets
        }
    }

    private fun setupBlurViews() {
        if (!::mBlurTop.isInitialized || Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
            return

        val decorView = window.decorView
        val background = decorView.background ?: android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK)
        // Prefer the fragment container so the blur pass skips the app bar / drawer chrome
        val rootView = (findViewById<ViewGroup>(R.id.main_content_root)
            ?: decorView.findViewById(android.R.id.content)) as ViewGroup

        eightbitlab.com.blurview.GlassSetup.setupGlass(mBlurTop, rootView, eightbitlab.com.blurview.RenderEffectBlur())
                .setFrameClearDrawable(background)
                .setBlurRadius(15f)

        val headerView = mNavigationView.getHeaderView(0)
        val navigatorBlur = headerView?.findViewById<BlurView>(R.id.navigator_blur)
        if (navigatorBlur != null) {
            // Drawer header still needs the broader content root (includes main chrome behind the drawer)
            val navRoot = decorView.findViewById<ViewGroup>(android.R.id.content)
            eightbitlab.com.blurview.GlassSetup.setupGlass(navigatorBlur, navRoot, eightbitlab.com.blurview.RenderEffectBlur())
                .setFrameClearDrawable(background)
                .setBlurRadius(15f)
        }
    }

    fun setupTitleBackgrounds() {
        val mainBarLayout = findViewById<View>(R.id.main_bar_layout)
        MenuUtil.setupToolbar(this, mToolBar, mBlurTop, mainBarLayout)
    }
}
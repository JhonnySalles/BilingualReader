package br.com.fenix.bilingualreader

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import br.com.fenix.bilingualreader.databinding.ActivityMainBinding
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.enums.ThemeMode
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
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil.ThemeUtils.getColorFromAttr
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
import br.com.fenix.bilingualreader.util.helpers.Telemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import androidx.lifecycle.lifecycleScope
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDate


import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, MainListener {

    private val mLOGGER = LoggerFactory.getLogger(MainActivity::class.java)

    private lateinit var mMangaLibraryModel: MangaLibraryViewModel
    private lateinit var mBookLibraryModel: BookLibraryViewModel
    private lateinit var mToolBar: Toolbar
    private lateinit var mFragmentManager: FragmentManager
    private lateinit var mNavigationView: NavigationView
    private lateinit var mMenu: Menu
    private lateinit var mToggle: ActionBarDrawerToggle
    private lateinit var mDrawer: DrawerLayout
    private var mBlurTop: eightbitlab.com.blurview.BlurView? = null

    private lateinit var binding: ActivityMainBinding

    private val mDefaultUncaughtHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun onCreate(savedInstanceState: Bundle?) {
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            mLOGGER.error("*** CRASH APP *** ", e)
            mDefaultUncaughtHandler?.uncaughtException(t, e)
        }

        mMangaLibraryModel = ViewModelProvider(this)[MangaLibraryViewModel::class.java]
        mBookLibraryModel = ViewModelProvider(this)[BookLibraryViewModel::class.java]

        installSplashScreen().setKeepOnScreenCondition {
            mMangaLibraryModel.isLaunch && mBookLibraryModel.isLaunch
        }

        val isDark = ThemeUtil.applyThemeMode(this)

        val theme = Themes.valueOf(GeneralConsts.getSharedPreferences(this).getString(GeneralConsts.KEYS.THEME.THEME_USED, Themes.ORIGINAL.toString())!!)
        setTheme(theme.getValue())

        super.onCreate(savedInstanceState)

        ThemeUtil.statusBarTransparentTheme(window, isDark, isLightStatus = !isDark)

        initializeBook()
        createNotificationChannel()
        DataBase.initializeBackup(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        mToolBar = findViewById(R.id.main_toolbar)
        MenuUtil.tintToolbar(mToolBar, theme)
        setSupportActionBar(mToolBar)

        // drawer_Layout is a default layout from app
        mDrawer = binding.drawerLayout
        mToggle = ActionBarDrawerToggle(
            this,
            mDrawer,
            mToolBar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        mDrawer.addDrawerListener(mToggle)
        mToggle.syncState()

        // nav_view have a menu layout
        mNavigationView = binding.navView
        mNavigationView.setNavigationItemSelectedListener(this)

        mMangaLibraryModel.setDefaultLibrary(LibraryUtil.getDefault(this, Type.MANGA))
        mBookLibraryModel.setDefaultLibrary(LibraryUtil.getDefault(this, Type.BOOK))

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
            }
        }, false)

        libraries()

        var fragment: Fragment
        if (GeneralConsts.getSharedPreferences(this).getBoolean(GeneralConsts.KEYS.THEME.THEME_CHANGE, false)) {
            with(GeneralConsts.getSharedPreferences(this).edit()) {
                this.putBoolean(GeneralConsts.KEYS.THEME.THEME_CHANGE, false)
                this.commit()
            }

            mMangaLibraryModel.isLaunch = false
            mBookLibraryModel.isLaunch = false
            fragment = ConfigFragment()
        } else {
            val idLibrary = GeneralConsts.getSharedPreferences(this).getLong(
                GeneralConsts.KEYS.LIBRARY.LAST_LIBRARY,
                GeneralConsts.KEYS.LIBRARY.DEFAULT_MANGA
            )

            val library = mLibraries.find { it.id == idLibrary } ?: if (idLibrary.compareTo(R.id.menu_book_library_default) == 0)
                LibraryUtil.getDefault(this, Type.BOOK)
            else
                LibraryUtil.getDefault(this, Type.MANGA)

            fragment = when (library.type) {
                Type.MANGA -> {
                    mMangaLibraryModel.setLibrary(library)
                    MangaLibraryFragment()
                }

                Type.BOOK -> {
                    mBookLibraryModel.setLibrary(library)
                    BookLibraryFragment()
                }

                else -> {
                    mMangaLibraryModel.setLibrary(LibraryUtil.getDefault(this, Type.MANGA))
                    MangaLibraryFragment()
                }
            }

            intent.dataString?.let {
                mMangaLibraryModel.isLaunch = false
                mBookLibraryModel.isLaunch = false
                fragment = when (it) {
                    "history" -> HistoryFragment()
                    else -> fragment
                }
            }
        }

        // content_fragment use for receive fragments layout
        mFragmentManager.beginTransaction().replace(R.id.main_content_root, fragment).commit()

        mBlurTop = findViewById(R.id.main_blur_top)
        setupBlurViews()
        setupWindowInsets()
        applyGlassmorphism()
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
        NotificationManagerCompat.from(this).cancelAll()
        clearCache()

        val preferences = GeneralConsts.getSharedPreferences(this)
        if (LocalDate.now().isAfter(LocalDate.parse(preferences.getString(GeneralConsts.KEYS.DATABASE.LAST_AUTO_BACKUP, "2025-01-01")))) {
            GeneralConsts.getSharedPreferences(this).edit(commit = true) { putString(GeneralConsts.KEYS.DATABASE.LAST_AUTO_BACKUP, LocalDate.now().toString()) }
            DataBase.autoBackupDatabase(this)
        }

        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        ThemeUtil.applyThemeMode(this)
        val theme = Themes.valueOf(GeneralConsts.getSharedPreferences(this).getString(GeneralConsts.KEYS.THEME.THEME_USED, Themes.ORIGINAL.toString())!!)
        setTheme(theme.getValue())
        applyGlassmorphism()

        val preferences = GeneralConsts.getSharedPreferences(this)
        val isGlass = preferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)
        val useBlur = isGlass && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        mBlurTop?.setBlurAutoUpdate(useBlur)
        mBlurTop?.setBlurEnabled(useBlur)
    }

    override fun onPause() {
        super.onPause()
        mBlurTop?.setBlurAutoUpdate(false)
        mBlurTop?.setBlurEnabled(false)
    }

    fun setBlurAutoUpdate(enabled: Boolean) {
        val preferences = GeneralConsts.getSharedPreferences(this)
        val isGlass = preferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)
        val useBlur = isGlass && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        if (useBlur) {
            mBlurTop?.setBlurAutoUpdate(enabled)
        } else {
            mBlurTop?.setBlurAutoUpdate(false)
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        ThemeUtil.applyThemeMode(this)
        val theme = Themes.valueOf(GeneralConsts.getSharedPreferences(this).getString(GeneralConsts.KEYS.THEME.THEME_USED, Themes.ORIGINAL.toString())!!)
        setTheme(theme.getValue())
        super.onConfigurationChanged(newConfig)
        applyGlassmorphism()
    }

    private fun setupWindowInsets() {
        val mainBlurTop = mBlurTop ?: return
        val mainContentRoot = findViewById<View>(R.id.main_content_root) ?: return

        ViewCompat.setOnApplyWindowInsetsListener(mainBlurTop) { view, insets ->
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
        val mainBlurTop = mBlurTop ?: return
        val decorView = window.decorView
        val background = decorView.background ?: android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK)
        val blurAlgorithm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) eightbitlab.com.blurview.RenderEffectBlur() else eightbitlab.com.blurview.RenderScriptBlur(this)

        val rootView = decorView.findViewById<ViewGroup>(android.R.id.content)
        mainBlurTop.setupWith(rootView, blurAlgorithm)
            .setFrameClearDrawable(background)
            .setBlurRadius(15f)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val radius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 28f, resources.displayMetrics).toInt()
            val outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, -radius, view.width, view.height, radius.toFloat())
                }
            }
            mainBlurTop.outlineProvider = outlineProvider
            mainBlurTop.clipToOutline = true

            val mainBarLayout = findViewById<View>(R.id.main_bar_layout)
            mainBarLayout?.outlineProvider = outlineProvider
            mainBarLayout?.clipToOutline = true
        }
    }

    private fun applyGlassmorphism() {
        val mainBarLayout = findViewById<View>(R.id.main_bar_layout)
        mainBarLayout?.background = android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
        mToolBar.background = android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)

        val sharedPreferences = GeneralConsts.getSharedPreferences(this)
        val isGlass = sharedPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)
        val useBlur = isGlass && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        mBlurTop?.setBlurEnabled(useBlur)

        val themeColor = getColorFromAttr(R.attr.colorSurface)
        val isNight = ThemeUtil.applyThemeMode(this)
        val alpha = if (isNight) 0xD9 else 0x73 // 85% opacity for dark theme, 45% for light theme
        val translucentColor = ((themeColor and 0x00FFFFFF) or (alpha shl 24)).toInt()
        val solidColor = ((themeColor and 0x00FFFFFF) or (0xFF shl 24)).toInt()
        val cornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 28f, resources.displayMetrics)

        val topBg = if (useBlur) {
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(translucentColor)
                cornerRadii = floatArrayOf(
                    0f, 0f,
                    0f, 0f,
                    cornerRadius, cornerRadius,
                    cornerRadius, cornerRadius
                )
            }
        } else {
            val middleColor = ((themeColor and 0x00FFFFFF) or (0xB3 shl 24)).toInt() // 70% opacity
            val transparentColor = (themeColor and 0x00FFFFFF).toInt() // 0% opacity
            GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(solidColor, solidColor, middleColor, transparentColor)).apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadii = floatArrayOf(
                    0f, 0f,
                    0f, 0f,
                    cornerRadius, cornerRadius,
                    cornerRadius, cornerRadius
                )
            }
        }
        mBlurTop?.background = topBg
    }
}
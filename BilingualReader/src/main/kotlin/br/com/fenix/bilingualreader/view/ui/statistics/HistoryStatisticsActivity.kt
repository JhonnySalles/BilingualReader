package br.com.fenix.bilingualreader.view.ui.statistics

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.Themes
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.MenuUtil
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil.ThemeUtils.getColorFromAttr
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderEffectBlur
import eightbitlab.com.blurview.RenderScriptBlur

class HistoryStatisticsActivity : AppCompatActivity() {

    private var mBlurTop: BlurView? = null
    private lateinit var mToolBar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtil.applyThemeMode(this)
        val theme = Themes.valueOf(GeneralConsts.getSharedPreferences(this).getString(GeneralConsts.KEYS.THEME.THEME_USED, Themes.ORIGINAL.toString())!!)
        setTheme(theme.getValue())

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history_statistics)

        ThemeUtil.statusBarTransparentTheme(window, !resources.getBoolean(R.bool.isNight))

        val toolbar = findViewById<Toolbar>(R.id.toolbar_history_statistics)
        MenuUtil.tintToolbar(toolbar, theme)
        setSupportActionBar(toolbar)

        toolbar.setTitleTextAppearance(this, R.style.DetailTitleShadow)
        toolbar.setSubtitleTextAppearance(this, R.style.DetailSubTitleShadow)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.title = getString(R.string.menu_history)

        mBlurTop = findViewById(R.id.history_statistics_blur_top)
        mToolBar = toolbar

        setupBlurViews()
        setupWindowInsets()
        applyGlassmorphism()

        val bundle: Bundle? = intent.extras

        val fragment: Fragment = HistoryStatisticsFragment()
        fragment.arguments = bundle

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.root_frame_history_statistics, fragment)
                .commit()
        }
    }

    override fun onResume() {
        super.onResume()
        ThemeUtil.applyThemeMode(this)
        val theme = Themes.valueOf(GeneralConsts.getSharedPreferences(this).getString(GeneralConsts.KEYS.THEME.THEME_USED, Themes.ORIGINAL.toString())!!)
        setTheme(theme.getValue())
        applyGlassmorphism()
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
        val rootFrame = findViewById<View>(R.id.root_frame_history_statistics) ?: return

        ViewCompat.setOnApplyWindowInsetsListener(mainBlurTop) { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(view.paddingLeft, statusBarHeight, view.paddingRight, view.paddingBottom)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(rootFrame) { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(view.paddingLeft, statusBarHeight, view.paddingRight, 0)
            insets
        }
    }

    private fun setupBlurViews() {
        val mainBlurTop = mBlurTop ?: return
        val decorView = window.decorView
        val background = decorView.background ?: android.graphics.drawable.ColorDrawable(Color.BLACK)
        val blurAlgorithm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) RenderEffectBlur() else RenderScriptBlur(this)

        val rootLayout = findViewById<ViewGroup>(R.id.history_statistics_root_layout)
        mainBlurTop.setupWith(rootLayout, blurAlgorithm)
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

            val mainBarLayout = findViewById<View>(R.id.history_statistics_bar_layout)
            mainBarLayout?.outlineProvider = outlineProvider
            mainBarLayout?.clipToOutline = true
        }
    }

    private fun applyGlassmorphism() {
        val mainBarLayout = findViewById<View>(R.id.history_statistics_bar_layout)
        mainBarLayout?.background = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
        mToolBar.background = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                supportFinishAfterTransition()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        val intent = Intent()
        setResult(RESULT_OK, intent)
        super.onBackPressed()
        supportFinishAfterTransition()
    }

}

package br.com.fenix.bilingualreader.view.ui.statistics

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
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
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderEffectBlur
import eightbitlab.com.blurview.RenderScriptBlur

class HistoryStatisticsActivity : AppCompatActivity() {

    private lateinit var mBlurTop: BlurView
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

        val bundle: Bundle? = intent.extras

        val fragment: Fragment = HistoryStatisticsFragment()
        fragment.arguments = bundle

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.root_frame_history_statistics, fragment)
                .commit()
        }

        setupBlurViews()
        setupWindowInsets()
        setupTitleBackgrounds()
    }

    override fun onResume() {
        super.onResume()
        ThemeUtil.applyThemeMode(this)
        val theme = Themes.valueOf(GeneralConsts.getSharedPreferences(this).getString(GeneralConsts.KEYS.THEME.THEME_USED, Themes.ORIGINAL.toString())!!)
        setTheme(theme.getValue())
        ThemeUtil.statusBarTransparentTheme(window, !resources.getBoolean(R.bool.isNight))
        setupTitleBackgrounds()

        val sharedPreferences = GeneralConsts.getSharedPreferences(this)
        val isGlass = sharedPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)
        mBlurTop.setBlurAutoUpdate(isGlass)
        mBlurTop.setBlurEnabled(isGlass)
    }

    override fun onPause() {
        super.onPause()
        mBlurTop.setBlurAutoUpdate(false)
        mBlurTop.setBlurEnabled(false)
    }

    fun setBlurAutoUpdate(enabled: Boolean) {
        val sharedPreferences = GeneralConsts.getSharedPreferences(this)
        val isGlass = sharedPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)
        if (isGlass) {
            mBlurTop.setBlurAutoUpdate(enabled)
        } else {
            mBlurTop.setBlurAutoUpdate(false)
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        ThemeUtil.applyThemeMode(this)
        val theme = Themes.valueOf(GeneralConsts.getSharedPreferences(this).getString(GeneralConsts.KEYS.THEME.THEME_USED, Themes.ORIGINAL.toString())!!)
        setTheme(theme.getValue())
        super.onConfigurationChanged(newConfig)
        ThemeUtil.statusBarTransparentTheme(window, !resources.getBoolean(R.bool.isNight))
        setupTitleBackgrounds()
    }

    private fun setupWindowInsets() {
        if (!::mBlurTop.isInitialized)
            return

        val rootFrame = findViewById<View>(R.id.root_frame_history_statistics) ?: return

        ViewCompat.setOnApplyWindowInsetsListener(mBlurTop) { view, insets ->
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
        if (!::mBlurTop.isInitialized)
            return

        val decorView = window.decorView
        val background = decorView.background ?: android.graphics.drawable.ColorDrawable(Color.BLACK)
        val blurAlgorithm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) RenderEffectBlur() else RenderScriptBlur(this)

        val rootLayout = decorView.findViewById<ViewGroup>(android.R.id.content)
        mBlurTop.setupWith(rootLayout, blurAlgorithm)
            .setFrameClearDrawable(background)
            .setBlurRadius(15f)
    }

    private fun setupTitleBackgrounds() {
        val mainBarLayout = findViewById<View>(R.id.history_statistics_bar_layout)
        MenuUtil.setupToolbar(this, mToolBar, mBlurTop, mainBarLayout)
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

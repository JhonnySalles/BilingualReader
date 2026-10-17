package br.com.fenix.bilingualreader.view.ui.detail

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.Themes
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.MenuUtil
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil
import br.com.fenix.bilingualreader.view.ui.detail.book.BookDetailFragment
import br.com.fenix.bilingualreader.view.ui.detail.manga.MangaDetailFragment


class DetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtil.applyThemeMode(this)
        val theme = Themes.valueOf(GeneralConsts.getSharedPreferences(this).getString(GeneralConsts.KEYS.THEME.THEME_USED, Themes.ORIGINAL.toString())!!)
        setTheme(theme.getValue())

        super.onCreate(savedInstanceState)
        setupSharedElementTransitions()
        supportPostponeEnterTransition()
        setContentView(R.layout.activity_detail)

        ThemeUtil.statusBarTransparentTheme(window, !resources.getBoolean(R.bool.isNight))

        val toolbar = findViewById<Toolbar>(R.id.toolbar_detail)
        MenuUtil.tintToolbar(toolbar, theme)
        setSupportActionBar(toolbar)

        toolbar.setTitleTextAppearance(this, R.style.DetailTitleShadow)
        toolbar.setSubtitleTextAppearance(this, R.style.DetailSubTitleShadow)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        val bundle: Bundle? = intent.extras

        var fragment : Fragment = MangaDetailFragment()

        bundle?.let {
            if (it.containsKey(GeneralConsts.KEYS.OBJECT.MANGA))
                fragment = MangaDetailFragment()
            else if (it.containsKey(GeneralConsts.KEYS.OBJECT.BOOK))
                fragment = BookDetailFragment()
        }

        fragment.arguments = bundle

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.root_frame_detail, fragment)
                .commit()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent()
                setResult(RESULT_OK, intent)
                revertFragmentTransition()
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                supportFinishAfterTransition()
            }
        })
    }

    private fun revertFragmentTransition() {
        val fragment = supportFragmentManager.findFragmentById(R.id.root_frame_detail)
        if (fragment is MangaDetailFragment) {
            fragment.revertCoverTransition()
        } else if (fragment is BookDetailFragment) {
            fragment.revertCoverTransition()
        }
    }

    private fun setupSharedElementTransitions() {
        val bundle = intent.extras
        val explicitTextColor = if (bundle?.containsKey(GeneralConsts.KEYS.SHARED_ELEMENT_TEXT_COLOR) == true) bundle.getInt(GeneralConsts.KEYS.SHARED_ELEMENT_TEXT_COLOR) else null
        val explicitProgressColor = if (bundle?.containsKey(GeneralConsts.KEYS.SHARED_ELEMENT_PROGRESS_COLOR) == true) bundle.getInt(GeneralConsts.KEYS.SHARED_ELEMENT_PROGRESS_COLOR) else null

        val enterTransition = android.transition.TransitionSet().apply {
            ordering = android.transition.TransitionSet.ORDERING_TOGETHER
            addTransition(android.transition.ChangeBounds())
            addTransition(android.transition.ChangeTransform())
            addTransition(android.transition.ChangeClipBounds())
            addTransition(android.transition.ChangeImageTransform())
            addTransition(br.com.fenix.bilingualreader.view.animation.TextColorTransition(explicitTextColor))
            addTransition(br.com.fenix.bilingualreader.view.animation.ProgressColorTransition(explicitProgressColor))
            duration = 350L
            interpolator = android.view.animation.AnimationUtils.loadInterpolator(this@DetailActivity, android.R.interpolator.fast_out_slow_in)
        }

        val returnTransition = android.transition.TransitionSet().apply {
            ordering = android.transition.TransitionSet.ORDERING_TOGETHER
            addTransition(android.transition.ChangeBounds())
            addTransition(android.transition.ChangeTransform())
            addTransition(android.transition.ChangeClipBounds())
            addTransition(android.transition.ChangeImageTransform())
            addTransition(br.com.fenix.bilingualreader.view.animation.TextColorTransition(explicitEndColor = explicitTextColor))
            addTransition(br.com.fenix.bilingualreader.view.animation.ProgressColorTransition(explicitEndColor = explicitProgressColor))
            duration = 350L
            interpolator = android.view.animation.AnimationUtils.loadInterpolator(this@DetailActivity, android.R.interpolator.fast_out_slow_in)
        }

        window.sharedElementEnterTransition = enterTransition
        window.sharedElementReturnTransition = returnTransition
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                revertFragmentTransition()
                supportFinishAfterTransition()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}


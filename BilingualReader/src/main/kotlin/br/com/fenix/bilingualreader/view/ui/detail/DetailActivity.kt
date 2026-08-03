package br.com.fenix.bilingualreader.view.ui.detail

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
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


@Suppress("DEPRECATION")
class DetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtil.applyThemeMode(this)
        val theme = Themes.valueOf(GeneralConsts.getSharedPreferences(this).getString(GeneralConsts.KEYS.THEME.THEME_USED, Themes.ORIGINAL.toString())!!)
        setTheme(theme.getValue())

        super.onCreate(savedInstanceState)
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
    }

    private fun revertFragmentTransition() {
        val fragment = supportFragmentManager.findFragmentById(R.id.root_frame_detail)
        if (fragment is MangaDetailFragment) {
            fragment.revertCoverTransition()
        } else if (fragment is BookDetailFragment) {
            fragment.revertCoverTransition()
        }
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

    override fun onBackPressed() {
        val intent = Intent()
        setResult(RESULT_OK, intent)
        revertFragmentTransition()
        super.onBackPressed()
        supportFinishAfterTransition()
    }

}
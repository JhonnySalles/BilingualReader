package br.com.fenix.bilingualreader.view.ui.tracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.Themes
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.MenuUtil
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil

class TrackerActivity : AppCompatActivity() {

    companion object {
        fun start(
            context: Context,
            trackId: Long? = null,
            libraryId: Long? = null,
            prefillTitle: String? = null,
            prefillRegex: String? = null,
            prefillMalId: Long? = null,
            prefillAniId: Long? = null,
            prefillVol: Int? = null,
            prefillChap: Float? = null
        ) {
            val intent = Intent(context, TrackerActivity::class.java).apply {
                if (trackId != null) putExtra(TrackerFragment.ARG_TRACK_ID, trackId)
                if (libraryId != null) putExtra(TrackerFragment.ARG_LIBRARY_ID, libraryId)
                if (prefillTitle != null) putExtra(TrackerFragment.ARG_PREFILL_TITLE, prefillTitle)
                if (prefillRegex != null) putExtra(TrackerFragment.ARG_PREFILL_REGEX, prefillRegex)
                if (prefillMalId != null) putExtra(TrackerFragment.ARG_PREFILL_MAL_ID, prefillMalId)
                if (prefillAniId != null) putExtra(TrackerFragment.ARG_PREFILL_ANI_ID, prefillAniId)
                if (prefillVol != null) putExtra(TrackerFragment.ARG_PREFILL_VOL, prefillVol)
                if (prefillChap != null) putExtra(TrackerFragment.ARG_PREFILL_CHAP, prefillChap)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtil.applyThemeMode(this)
        val theme = Themes.valueOf(
            GeneralConsts.getSharedPreferences(this)
                .getString(GeneralConsts.KEYS.THEME.THEME_USED, Themes.ORIGINAL.toString())!!
        )
        setTheme(theme.getValue())

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracker)

        ThemeUtil.statusBarTransparentTheme(window, !resources.getBoolean(R.bool.isNight))

        val toolbar = findViewById<Toolbar>(R.id.toolbar_tracker)
        MenuUtil.tintToolbar(toolbar, theme)
        setSupportActionBar(toolbar)

        toolbar.setTitleTextAppearance(this, R.style.DetailTitleShadow)
        toolbar.setSubtitleTextAppearance(this, R.style.DetailSubTitleShadow)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.setTitle(R.string.tracker_title)

        if (savedInstanceState == null) {
            val fragment = TrackerFragment()
            fragment.arguments = intent.extras
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.root_frame_tracker, fragment)
                .commit()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                supportFinishAfterTransition()
            }
        })
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
}

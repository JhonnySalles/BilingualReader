package br.com.fenix.bilingualreader.view.ui.pages_link

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Themes
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.MenuUtil
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.File
import java.util.Calendar


class PagesLinkActivity : AppCompatActivity() {

    private val mLOGGER = LoggerFactory.getLogger(PagesLinkActivity::class.java)

    private lateinit var mTheme : Themes

    override fun onCreate(savedInstanceState: Bundle?) {
        mTheme = Themes.valueOf(GeneralConsts.getSharedPreferences(this).getString(GeneralConsts.KEYS.THEME.THEME_USED, Themes.ORIGINAL.toString())!!)
        setTheme(mTheme.getValue())

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pages_link)

        ThemeUtil.statusBarTransparentTheme(window, resources.getBoolean(R.bool.isNight), isLightStatus = !resources.getBoolean(R.bool.isNight))

        val extras = intent.extras
        val fragment = supportFragmentManager.findFragmentById(R.id.root_frame_pages_link)

        val newFragment = if (fragment != null) fragment as PagesLinkFragment else PagesLinkFragment()

        if (extras != null) {
            val bundle = Bundle()
            bundle.putSerializable(GeneralConsts.KEYS.OBJECT.MANGA, extras.getSerializable(GeneralConsts.KEYS.OBJECT.MANGA) as Manga)
            bundle.putInt(GeneralConsts.KEYS.MANGA.PAGE_NUMBER, extras.getInt(GeneralConsts.KEYS.MANGA.PAGE_NUMBER, 0))
            newFragment.arguments = bundle
        }

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.root_frame_pages_link, newFragment)
            .commit()

        clearCache()
    }

    fun setActionBar(toolbar: Toolbar) {
        MenuUtil.tintToolbar(toolbar, mTheme)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
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

    private fun clearCache() {
        val cacheDir = GeneralConsts.getCacheDir(this)
        CoroutineScope(Dispatchers.IO).launch {
            async {
                try {
                    val linked = File(cacheDir, GeneralConsts.CACHE_FOLDER.LINKED)
                    if (linked.exists()) {
                        linked.listFiles()?.let {
                            val lastDay = Calendar.getInstance()
                            lastDay.add(Calendar.DAY_OF_MONTH, -1)
                            val items = it.toMutableList().filter { file -> file.lastModified() < lastDay.timeInMillis }

                            val link = items.filter { file -> file.nameWithoutExtension.contains(GeneralConsts.FILE_LINK.FOLDER_LINK + "_") }
                            for (f in link) {
                                f.listFiles()?.let { item ->
                                    for (i in item)
                                        i.delete()
                                }
                                f.delete()
                            }

                            val manga = items.filter { file -> file.nameWithoutExtension.contains(GeneralConsts.FILE_LINK.FOLDER_MANGA + "_") }
                                .sortedByDescending { file -> file.lastModified() }
                            for ((index, f) in manga.withIndex()) {
                                if (index > 10) {
                                    f.listFiles()?.let { item ->
                                        for (i in item)
                                            i.delete()
                                    }
                                    f.delete()
                                }
                            }

                            val other = items.filter { file ->
                                !file.nameWithoutExtension.contains(GeneralConsts.FILE_LINK.FOLDER_MANGA) &&
                                        !file.nameWithoutExtension.contains(
                                            GeneralConsts.FILE_LINK.FOLDER_LINK
                                        )
                            }
                            for (f in other) {
                                f.listFiles()?.let { item ->
                                    for (i in item)
                                        i.delete()
                                }
                                f.delete()
                            }
                        }
                    }
                } catch (e: Exception) {
                    mLOGGER.error("Error clearing cache folders: " + e.message, e)
                    Firebase.crashlytics.run {
                        setCustomKey("message", "Error clearing cache folders: " + e.message)
                        recordException(e)
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        supportFinishAfterTransition()
    }

}
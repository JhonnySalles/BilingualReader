package br.com.fenix.bilingualreader.view.ui.menu

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


class MenuActivity : AppCompatActivity() {

    private lateinit var mTheme : Themes

    override fun onCreate(savedInstanceState: Bundle?) {
        mTheme = Themes.valueOf(GeneralConsts.getSharedPreferences(this).getString(GeneralConsts.KEYS.THEME.THEME_USED, Themes.ORIGINAL.toString())!!)
        setTheme(mTheme.getValue())

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        MenuUtil.tintBackground(this, findViewById(R.id.menu_background))

        val id = intent.extras!!.getInt(GeneralConsts.KEYS.FRAGMENT.ID)

        val fragment = supportFragmentManager.findFragmentById(id)
        val newFragment = fragment ?: when (id) {
            R.id.frame_config_libraries -> ConfigLibrariesFragment()
            R.id.frame_select_manga -> SelectMangaFragment()
            else -> null
        }

        if (newFragment != null) {
            newFragment.arguments = intent.extras
            setFragment(newFragment)
        } else
            onBackPressed()
    }

    private fun setFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.root_frame_menu, fragment)
            .commit()
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

    fun onBack(bundle: Bundle) {
        val intent = Intent()
        intent.putExtras(bundle)
        setResult(RESULT_OK, intent)
        supportFinishAfterTransition()
        this.finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent()
        setResult(RESULT_OK, intent)
        supportFinishAfterTransition()
    }

}
package br.com.fenix.bilingualreader.view.ui.vocabulary

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.os.BundleCompat
import androidx.core.view.WindowInsetsControllerCompat
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.model.enums.Themes
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.controller.BookImageCoverController
import br.com.fenix.bilingualreader.service.controller.MangaImageCoverController
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.MenuUtil
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil
import br.com.fenix.bilingualreader.view.ui.vocabulary.book.VocabularyBookFragment
import br.com.fenix.bilingualreader.view.ui.vocabulary.manga.VocabularyMangaFragment


class VocabularyActivity : AppCompatActivity() {

    companion object VocabularyData {
        var mVocabularySelect : String = ""
        var mIsFavorite : Boolean = false
        var mSortType: Order = Order.Description
        var mSortDesc: Boolean = false
    }

    private lateinit var mBackgroundImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtil.applyThemeMode(this)
        val theme = Themes.valueOf(
            GeneralConsts.getSharedPreferences(this)
                .getString(GeneralConsts.KEYS.THEME.THEME_USED, Themes.ORIGINAL.toString())!!
        )
        setTheme(theme.getValue())

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vocabulary)

        ThemeUtil.statusBarTransparentTheme(window, resources.getBoolean(R.bool.isNight))

        val toolbar = findViewById<Toolbar>(R.id.toolbar_vocabulary)
        MenuUtil.tintToolbar(toolbar, theme)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        val bundle: Bundle? = intent.extras

        val type = if (bundle != null && bundle.containsKey(GeneralConsts.KEYS.VOCABULARY.TYPE))
            BundleCompat.getSerializable(bundle, GeneralConsts.KEYS.VOCABULARY.TYPE, Type::class.java)
        else
            null

        mVocabularySelect = if (bundle != null && bundle.containsKey(GeneralConsts.KEYS.VOCABULARY.TEXT))
            bundle.getString(GeneralConsts.KEYS.VOCABULARY.TEXT) ?: ""
        else
            ""

        val obj = if (bundle != null && bundle.containsKey(GeneralConsts.KEYS.OBJECT.MANGA))
            BundleCompat.getSerializable(bundle, GeneralConsts.KEYS.OBJECT.MANGA, Manga::class.java)
        else if (bundle != null && bundle.containsKey(GeneralConsts.KEYS.OBJECT.BOOK))
            BundleCompat.getSerializable(bundle, GeneralConsts.KEYS.OBJECT.BOOK, Book::class.java)
        else
            null

        mBackgroundImage = findViewById(R.id.vocabulary_background_image)
        mBackgroundImage.visibility = View.GONE

        val shadow = findViewById<View>(R.id.vocabulary_background_image_shadow)
        shadow.visibility = View.GONE

        val backgroundSupper = findViewById<View>(R.id.vocabulary_background_supper)
        backgroundSupper.visibility = View.GONE

        val fragment = if (type == null)
            VocabularyFragment()
        else
            when (type) {
                Type.BOOK -> {
                    val frag = VocabularyBookFragment()
                    if (obj != null) {
                        frag.setObject(obj as Book)
                        BookImageCoverController.instance.setImageCoverAsync(this, obj, mBackgroundImage, null, false) {
                            if (it != null)
                                mBackgroundImage.visibility = View.VISIBLE

                            shadow.visibility = mBackgroundImage.visibility
                            backgroundSupper.visibility = shadow.visibility
                            WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
                        }
                    }
                    frag
                }
                Type.MANGA -> {
                    val frag = VocabularyMangaFragment()
                    if (obj != null) {
                        frag.setObject(obj as Manga)
                        MangaImageCoverController.instance.setImageCoverAsync(this, obj, mBackgroundImage, null, false) {
                            if (it != null)
                                mBackgroundImage.visibility = View.VISIBLE

                            shadow.visibility = mBackgroundImage.visibility
                            backgroundSupper.visibility = shadow.visibility
                            WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
                        }
                    }
                    frag
                }
            }

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.root_frame_vocabulary, fragment)
            .commit()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                val intent = Intent()
                setResult(RESULT_OK, intent)
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
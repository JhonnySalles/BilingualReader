package br.com.fenix.bilingualreader.view.ui.vocabulary

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
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
        val theme = Themes.valueOf(
            GeneralConsts.getSharedPreferences(this)
                .getString(GeneralConsts.KEYS.THEME.THEME_USED, Themes.ORIGINAL.toString())!!
        )
        setTheme(theme.getValue())

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vocabulary)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_vocabulary)
        MenuUtil.tintToolbar(toolbar, theme)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        val bundle: Bundle? = intent.extras

        val type = if (bundle != null && bundle.containsKey(GeneralConsts.KEYS.VOCABULARY.TYPE))
            bundle[GeneralConsts.KEYS.VOCABULARY.TYPE] as Type
        else
            null

        mVocabularySelect = if (bundle != null && bundle.containsKey(GeneralConsts.KEYS.VOCABULARY.TEXT))
            bundle[GeneralConsts.KEYS.VOCABULARY.TEXT] as String
        else
            ""

        val obj = if (bundle != null && bundle.containsKey(GeneralConsts.KEYS.OBJECT.MANGA))
            bundle[GeneralConsts.KEYS.OBJECT.MANGA] as Manga
        else if (bundle != null && bundle.containsKey(GeneralConsts.KEYS.OBJECT.BOOK))
            bundle[GeneralConsts.KEYS.OBJECT.BOOK] as Book
        else
            null

        mBackgroundImage = findViewById(R.id.vocabulary_background_image)
        mBackgroundImage.setImageResource(R.mipmap.vocabulary_semi)

        val fragment = if (type == null)
            VocabularyFragment()
        else
            when (type) {
                Type.BOOK -> {
                    val frag = VocabularyBookFragment()
                    if (obj != null) {
                        frag.setObject(obj as Book)
                        BookImageCoverController.instance.setImageCoverAsync(
                            this,
                            obj,
                            arrayListOf(mBackgroundImage),
                            false
                        )
                    }
                    frag
                }
                Type.MANGA -> {
                    val frag = VocabularyMangaFragment()
                    if (obj != null) {
                        frag.setObject(obj as Manga)
                        MangaImageCoverController.instance.setImageCoverAsync(
                            this,
                            obj,
                            arrayListOf(mBackgroundImage),
                            false
                        )
                    }
                    frag
                }
            }

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.root_frame_vocabulary, fragment)
            .commit()
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
        super.onBackPressed()
        val intent = Intent()
        setResult(RESULT_OK, intent)
        supportFinishAfterTransition()
    }

}
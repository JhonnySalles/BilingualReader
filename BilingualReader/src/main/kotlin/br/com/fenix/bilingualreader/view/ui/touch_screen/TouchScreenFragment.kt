package br.com.fenix.bilingualreader.view.ui.touch_screen

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Position
import br.com.fenix.bilingualreader.model.enums.TouchScreen
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.controller.BookImageCoverController
import br.com.fenix.bilingualreader.service.controller.MangaImageCoverController
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.TouchUtil.TouchUtils
import br.com.fenix.bilingualreader.view.ui.menu.MenuActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.slf4j.LoggerFactory
import kotlin.properties.Delegates


class TouchScreenFragment : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(TouchScreenFragment::class.java)

    private lateinit var mImage: ImageView
    private lateinit var mTouchTop: TextView
    private lateinit var mTouchTopRight: TextView
    private lateinit var mTouchTopLeft: TextView
    private lateinit var mTouchLeft: TextView
    private lateinit var mTouchRight: TextView
    private lateinit var mTouchBottom: TextView
    private lateinit var mTouchBottomLeft: TextView
    private lateinit var mTouchBottomRight: TextView
    private lateinit var mSave: MaterialButton
    private lateinit var mDefault: MaterialButton

    private lateinit var mToolbar: Toolbar

    private var mType: Type = Type.MANGA
    private var mCover : Bitmap? by Delegates.observable(null) { _, _, newValue ->
        if (newValue != null)
            mImage.setImageBitmap(newValue)
        else
            mImage.setImageResource(R.mipmap.navigator_header_image)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_config_touch, container, false)

        mToolbar = root.findViewById(R.id.toolbar_touch_screen_config)

        (requireActivity() as MenuActivity).setActionBar(mToolbar)

        mImage = root.findViewById(R.id.touch_screen_config_image)
        mSave = root.findViewById(R.id.touch_screen_config_save)
        mDefault = root.findViewById(R.id.touch_screen_config_default)

        mTouchTop = root.findViewById(R.id.touch_screen_config_top)
        mTouchTopRight = root.findViewById(R.id.touch_screen_config_top_right)
        mTouchTopLeft = root.findViewById(R.id.touch_screen_config_top_left)
        mTouchLeft = root.findViewById(R.id.touch_screen_config_left)
        mTouchRight = root.findViewById(R.id.touch_screen_config_right)
        mTouchBottom = root.findViewById(R.id.touch_screen_config_bottom)
        mTouchBottomLeft = root.findViewById(R.id.touch_screen_config_bottom_left)
        mTouchBottomRight = root.findViewById(R.id.touch_screen_config_bottom_right)

        mTouchTop.setOnClickListener { changeTouch(mTouchTop) }
        mTouchTopRight.setOnClickListener { changeTouch(mTouchTopRight) }
        mTouchTopLeft.setOnClickListener { changeTouch(mTouchTopLeft) }
        mTouchLeft.setOnClickListener { changeTouch(mTouchLeft) }
        mTouchRight.setOnClickListener { changeTouch(mTouchRight) }
        mTouchBottom.setOnClickListener { changeTouch(mTouchBottom) }
        mTouchBottomLeft.setOnClickListener { changeTouch(mTouchBottomLeft) }
        mTouchBottomRight.setOnClickListener { changeTouch(mTouchBottomRight) }

        mSave.setOnClickListener {
            saveConfig()
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        mDefault.setOnClickListener {
            TouchUtils.setDefault(requireContext(), mType)
            loadConfig()
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            mType = it.getSerializable(GeneralConsts.KEYS.OBJECT.TYPE) as Type

            if (it.containsKey(GeneralConsts.KEYS.OBJECT.BOOK)) {
                val book = it.getSerializable(GeneralConsts.KEYS.OBJECT.BOOK) as Book
                BookImageCoverController.instance.setImageCoverAsync(requireContext(), book, isCoverSize = true) { mCover = it }
                BookImageCoverController.instance.setImageCoverAsync(requireContext(), book, isCoverSize = false) { mCover = it }
            } else if (it.containsKey(GeneralConsts.KEYS.OBJECT.MANGA)) {
                val book = it.getSerializable(GeneralConsts.KEYS.OBJECT.MANGA) as Manga
                MangaImageCoverController.instance.setImageCoverAsync(requireContext(), book, isCoverSize = true) { mCover = it }
                MangaImageCoverController.instance.setImageCoverAsync(requireContext(), book, isCoverSize = false) { mCover = it }
            }
        }

        mToolbar.title = when(mType) {
            Type.MANGA -> getString(R.string.reading_touch_screen_manga)
            Type.BOOK -> getString(R.string.reading_touch_screen_book)
        }

        loadConfig()
    }

    private fun getDescription(touchScreen: TouchScreen) : String = getString(touchScreen.getValue())

    private fun loadConfig() {
        val touch = TouchUtils.getTouch(requireContext(), mType)

        mTouchTop.tag = touch[Position.TOP]
        mTouchTopRight.tag = touch[Position.CORNER_TOP_RIGHT]
        mTouchTopLeft.tag = touch[Position.CORNER_TOP_LEFT]
        mTouchLeft.tag = touch[Position.LEFT]
        mTouchRight.tag = touch[Position.RIGHT]
        mTouchBottom.tag = touch[Position.BOTTOM]
        mTouchBottomLeft.tag = touch[Position.CORNER_BOTTOM_LEFT]
        mTouchBottomRight.tag = touch[Position.CORNER_BOTTOM_RIGHT]
        
        mTouchTop.text = getDescription(mTouchTop.tag as TouchScreen)
        mTouchTopRight.text = getDescription(mTouchTopRight.tag as TouchScreen)
        mTouchTopLeft.text = getDescription(mTouchTopLeft.tag as TouchScreen)
        mTouchLeft.text = getDescription(mTouchLeft.tag as TouchScreen)
        mTouchRight.text = getDescription(mTouchRight.tag as TouchScreen)
        mTouchBottom.text = getDescription(mTouchBottom.tag as TouchScreen)
        mTouchBottomLeft.text = getDescription(mTouchBottomLeft.tag as TouchScreen)
        mTouchBottomRight.text = getDescription(mTouchBottomRight.tag as TouchScreen)
    }

    private fun saveConfig() {
        val sharedPreferences = GeneralConsts.getSharedPreferences(requireContext())
        with(sharedPreferences.edit()) {
            when (mType) {
                Type.MANGA -> {
                    this.putString(
                        GeneralConsts.KEYS.TOUCH.MANGA_TOP,
                        (mTouchTop.tag as TouchScreen).toString()
                    )

                    this.putString(
                        GeneralConsts.KEYS.TOUCH.MANGA_TOP_RIGHT,
                        (mTouchTopRight.tag as TouchScreen).toString()
                    )

                    this.putString(
                        GeneralConsts.KEYS.TOUCH.MANGA_TOP_LEFT,
                        (mTouchTopLeft.tag as TouchScreen).toString()
                    )

                    this.putString(
                        GeneralConsts.KEYS.TOUCH.MANGA_LEFT,
                        (mTouchLeft.tag as TouchScreen).toString()
                    )

                    this.putString(
                        GeneralConsts.KEYS.TOUCH.MANGA_RIGHT,
                        (mTouchRight.tag as TouchScreen).toString()
                    )

                    this.putString(
                        GeneralConsts.KEYS.TOUCH.MANGA_BOTTOM,
                        (mTouchBottom.tag as TouchScreen).toString()
                    )

                    this.putString(
                        GeneralConsts.KEYS.TOUCH.MANGA_BOTTOM_LEFT,
                        (mTouchBottomLeft.tag as TouchScreen).toString()
                    )

                    this.putString(
                        GeneralConsts.KEYS.TOUCH.MANGA_BOTTOM_RIGHT,
                        (mTouchBottomRight.tag as TouchScreen).toString()
                    )
                }
                Type.BOOK -> {
                    this.putString(
                        GeneralConsts.KEYS.TOUCH.BOOK_TOP,
                        (mTouchTop.tag as TouchScreen).toString()
                    )

                    this.putString(
                        GeneralConsts.KEYS.TOUCH.BOOK_TOP_RIGHT,
                        (mTouchTopRight.tag as TouchScreen).toString()
                    )

                    this.putString(
                        GeneralConsts.KEYS.TOUCH.BOOK_TOP_LEFT,
                        (mTouchTopLeft.tag as TouchScreen).toString()
                    )

                    this.putString(
                        GeneralConsts.KEYS.TOUCH.BOOK_LEFT,
                        (mTouchLeft.tag as TouchScreen).toString()
                    )

                    this.putString(
                        GeneralConsts.KEYS.TOUCH.BOOK_RIGHT,
                        (mTouchRight.tag as TouchScreen).toString()
                    )

                    this.putString(
                        GeneralConsts.KEYS.TOUCH.BOOK_BOTTOM,
                        (mTouchBottom.tag as TouchScreen).toString()
                    )

                    this.putString(
                        GeneralConsts.KEYS.TOUCH.BOOK_BOTTOM_LEFT,
                        (mTouchBottomLeft.tag as TouchScreen).toString()
                    )

                    this.putString(
                        GeneralConsts.KEYS.TOUCH.BOOK_BOTTOM_RIGHT,
                        (mTouchBottomRight.tag as TouchScreen).toString()
                    )
                }
            }

            this.commit()
        }
    }

    private fun changeTouch(button: TextView) {
        val touch = TouchScreen.values().filter { it != TouchScreen.TOUCH_NOT_IMPLEMENTED }.associate { getString(it.getValue()) to it }
        val items = touch.keys.sorted().toTypedArray()
        MaterialAlertDialogBuilder(requireContext(), R.style.AppCompatAlertDialogStyle)
            .setTitle(getString(R.string.reading_touch_screen_change_title))
            .setItems(items) { _, selected ->
                button.tag = if (selected >= 0 && items.size > selected)
                    touch[items[selected]]
                else
                    TouchScreen.TOUCH_NOT_ASSIGNED
                button.text = getDescription(button.tag as TouchScreen)
            }
            .show()
    }

}
package br.com.fenix.bilingualreader.view.ui.touch_screen

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import br.com.fenix.bilingualreader.util.helpers.MenuUtil
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil.ThemeUtils.getColorFromAttr
import br.com.fenix.bilingualreader.util.helpers.TouchUtil.TouchUtils
import br.com.fenix.bilingualreader.view.ui.menu.MenuActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.GlassSetup
import eightbitlab.com.blurview.RenderEffectBlur
import eightbitlab.com.blurview.RenderScriptBlur
import org.slf4j.LoggerFactory
import kotlin.properties.Delegates


@Suppress("DEPRECATION")
class TouchScreenFragment : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(TouchScreenFragment::class.java)

    private lateinit var mPreferences: SharedPreferences
    private lateinit var mBlurTop: BlurView
    private lateinit var mToolbar: Toolbar

    private lateinit var mImage: ImageView
    private lateinit var mTouchTop: View
    private lateinit var mTouchTopRight: View
    private lateinit var mTouchTopLeft: View
    private lateinit var mTouchLeft: View
    private lateinit var mTouchRight: View
    private lateinit var mTouchBottom: View
    private lateinit var mTouchBottomLeft: View
    private lateinit var mTouchBottomRight: View
    private lateinit var mSave: MaterialButton
    private lateinit var mDefault: MaterialButton

    private val mHandler = Handler(Looper.getMainLooper())
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
        mPreferences = GeneralConsts.getSharedPreferences(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_config_touch, container, false)

        mToolbar = root.findViewById(R.id.toolbar_touch_screen_config)

        (requireActivity() as MenuActivity).setActionBar(mToolbar)

        mBlurTop = root.findViewById(R.id.touch_screen_blur_top)
        setupBlurViews(root)
        setupWindowInsets()
        setupTitleBackgrounds()

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

        updateTouchZone(mTouchTop, touch[Position.TOP] ?: TouchScreen.TOUCH_NOT_ASSIGNED)
        updateTouchZone(mTouchTopRight, touch[Position.CORNER_TOP_RIGHT] ?: TouchScreen.TOUCH_NOT_ASSIGNED)
        updateTouchZone(mTouchTopLeft, touch[Position.CORNER_TOP_LEFT] ?: TouchScreen.TOUCH_NOT_ASSIGNED)
        updateTouchZone(mTouchLeft, touch[Position.LEFT] ?: TouchScreen.TOUCH_NOT_ASSIGNED)
        updateTouchZone(mTouchRight, touch[Position.RIGHT] ?: TouchScreen.TOUCH_NOT_ASSIGNED)
        updateTouchZone(mTouchBottom, touch[Position.BOTTOM] ?: TouchScreen.TOUCH_NOT_ASSIGNED)
        updateTouchZone(mTouchBottomLeft, touch[Position.CORNER_BOTTOM_LEFT] ?: TouchScreen.TOUCH_NOT_ASSIGNED)
        updateTouchZone(mTouchBottomRight, touch[Position.CORNER_BOTTOM_RIGHT] ?: TouchScreen.TOUCH_NOT_ASSIGNED)

        val isGlass = mPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)
        updateTouchZoneBackgrounds(isGlass)
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

    private fun changeTouch(card: View) {
        val touch = TouchScreen.values().filter { it != TouchScreen.TOUCH_NOT_IMPLEMENTED }.associate { getString(it.getValue()) to it }
        val items = touch.keys.sorted().toTypedArray()
        MaterialAlertDialogBuilder(requireContext(), R.style.AppCompatAlertDialogStyle)
            .setTitle(getString(R.string.reading_touch_screen_change_title))
            .setItems(items) { _, selected ->
                val selectedTouch = if (selected >= 0 && items.size > selected)
                    touch[items[selected]] ?: TouchScreen.TOUCH_NOT_ASSIGNED
                else
                    TouchScreen.TOUCH_NOT_ASSIGNED
                updateTouchZone(card, selectedTouch)
            }
            .show()
    }

    private fun getIconForTouchScreen(touchScreen: TouchScreen): Int {
        return when (touchScreen) {
            TouchScreen.TOUCH_NOT_ASSIGNED -> R.drawable.ico_close
            TouchScreen.TOUCH_ASPECT_FIT -> R.drawable.ico_reader_mode
            TouchScreen.TOUCH_FIT_WIDTH -> R.drawable.ico_reading_mode
            TouchScreen.TOUCH_CHAPTER_LIST -> R.drawable.ico_item_chapters_menu
            TouchScreen.TOUCH_NEXT_FILE -> R.drawable.ico_tts_next
            TouchScreen.TOUCH_PREVIOUS_FILE -> R.drawable.ico_tts_previous
            TouchScreen.TOUCH_NEXT_PAGE -> R.drawable.ico_animated_text_next
            TouchScreen.TOUCH_PREVIOUS_PAGE -> R.drawable.ico_animated_text_before
            TouchScreen.TOUCH_SHARE_IMAGE -> R.drawable.ico_save_share_image
            TouchScreen.TOUCH_PAGE_MARK -> R.drawable.ico_book_reader_page_mark
            else -> 0
        }
    }

    private fun updateTouchZone(card: View, touchScreen: TouchScreen) {
        card.tag = touchScreen
        var textView: TextView? = null
        var imageView: ImageView? = null

        fun findViews(view: View) {
            if (view is TextView) {
                textView = view
            } else if (view is ImageView) {
                imageView = view
            } else if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    findViews(view.getChildAt(i))
                }
            }
        }

        findViews(card)

        if (textView != null) {
            textView!!.text = getDescription(touchScreen)
        }
        if (imageView != null) {
            val iconRes = getIconForTouchScreen(touchScreen)
            if (iconRes != 0) {
                imageView!!.setImageResource(iconRes)
                imageView!!.visibility = View.VISIBLE
            } else {
                imageView!!.visibility = View.GONE
            }
        }
    }

    private fun updateTouchZoneBackgrounds(isGlass: Boolean) {
        if (!isAdded) return

        fun applyBg(card: View, isPrimary: Boolean) {
            var blurView: BlurView? = null
            if (card is ViewGroup) {
                for (i in 0 until card.childCount) {
                    val child = card.getChildAt(i)
                    if (child is BlurView) {
                        blurView = child
                        break
                    }
                }
            }
            if (blurView == null) return

            val baseColor = if (isPrimary) {
                requireContext().getColorFromAttr(R.attr.colorPrimaryContainer)
            } else {
                ContextCompat.getColor(requireContext(), R.color.touch_demonstration_alter)
            }

            val alpha = if (isGlass) 0x66 else 0xA6 // 40% for Glass, 65% (35% transparency) for Flat
            val dynamicColor = (baseColor and 0x00FFFFFF) or (alpha shl 24)

            val cornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics)
            val shapeBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(dynamicColor)
                this.cornerRadius = cornerRadius
            }

            blurView.background = shapeBg
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                blurView.clipToOutline = true
            }
            blurView.outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
                }
            }

            blurView.setBlurEnabled(isGlass)
            if (isGlass) {
                val decorView = requireActivity().window.decorView
                val rootView = decorView.findViewById<ViewGroup>(android.R.id.content)
                val background = decorView.background ?: android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK)
                val blurAlgorithm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) RenderEffectBlur() else RenderScriptBlur(requireContext())
                GlassSetup.setupGlass(blurView, rootView, blurAlgorithm)
                    .setFrameClearDrawable(background)
                    .setBlurRadius(15f)
                blurView.setBlurAutoUpdate(true)
                mHandler.postDelayed({
                    blurView.setBlurAutoUpdate(false)
                }, 100)
            } else {
                blurView.setBlurAutoUpdate(false)
            }
        }

        applyBg(mTouchTop, false)
        applyBg(mTouchBottom, false)
        applyBg(mTouchLeft, false)
        applyBg(mTouchRight, false)
        applyBg(mTouchTopRight, true)
        applyBg(mTouchTopLeft, true)
        applyBg(mTouchBottomRight, true)
        applyBg(mTouchBottomLeft, true)
    }

    override fun onResume() {
        super.onResume()
        setupTitleBackgrounds()
        val isGlass = mPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)
        mBlurTop.setBlurEnabled(isGlass)
        if (isGlass) {
            mBlurTop.setBlurAutoUpdate(true)
            mHandler.postDelayed({
                mBlurTop.setBlurAutoUpdate(false)
            }, 100)
        } else {
            mBlurTop.setBlurAutoUpdate(false)
        }
        updateTouchZoneBackgrounds(isGlass)
    }

    override fun onPause() {
        super.onPause()
        mBlurTop.setBlurAutoUpdate(false)
        mBlurTop.setBlurEnabled(false)
    }

    override fun onDestroy() {
        mHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        val isGlass = mPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)
        if (hidden) {
            mBlurTop.setBlurAutoUpdate(false)
            mBlurTop.setBlurEnabled(false)
        } else {
            mBlurTop.setBlurEnabled(isGlass)
            if (isGlass) {
                mBlurTop.setBlurAutoUpdate(true)
                mHandler.postDelayed({
                    mBlurTop.setBlurAutoUpdate(false)
                }, 100)
            } else {
                mBlurTop.setBlurAutoUpdate(false)
            }
            updateTouchZoneBackgrounds(isGlass)
        }
    }

    private fun setupWindowInsets() {
        if (!::mBlurTop.isInitialized)
            return

        ViewCompat.setOnApplyWindowInsetsListener(mBlurTop) { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(view.paddingLeft, statusBarHeight, view.paddingRight, view.paddingBottom)
            insets
        }
    }

    private fun setupBlurViews(root: View) {
        if (!::mBlurTop.isInitialized)
            return

        val context = requireContext()
        val decorView = requireActivity().window.decorView
        val background = decorView.background ?: android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK)
        val blurAlgorithm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) RenderEffectBlur() else RenderScriptBlur(context)

        val rootView = decorView.findViewById<ViewGroup>(android.R.id.content)
        GlassSetup.setupGlass(mBlurTop, rootView, blurAlgorithm)
            .setFrameClearDrawable(background)
            .setBlurRadius(15f)
    }

    private fun setupTitleBackgrounds() {
        val barLayout = view?.findViewById<View>(R.id.content_toolbar_touch_screen_config)
        val activity = activity ?: return
        MenuUtil.setupToolbar(activity, mToolbar, mBlurTop, barLayout)
    }

}
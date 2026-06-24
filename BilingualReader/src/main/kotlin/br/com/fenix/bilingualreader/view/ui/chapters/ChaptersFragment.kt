package br.com.fenix.bilingualreader.view.ui.chapters

import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Chapters
import br.com.fenix.bilingualreader.service.listener.ChapterCardListener
import br.com.fenix.bilingualreader.service.listener.ChapterLoadListener
import br.com.fenix.bilingualreader.service.repository.SharedData
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.ImageUtil
import br.com.fenix.bilingualreader.util.helpers.MenuUtil
import br.com.fenix.bilingualreader.view.adapter.chapters.ChaptersGridAdapter
import br.com.fenix.bilingualreader.view.ui.menu.MenuActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderEffectBlur
import eightbitlab.com.blurview.RenderScriptBlur
import org.slf4j.LoggerFactory
import kotlin.math.max


class ChaptersFragment : Fragment(), ChapterLoadListener {

    private val mLOGGER = LoggerFactory.getLogger(ChaptersFragment::class.java)

    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mScrollUp: FloatingActionButton
    private lateinit var mScrollDown: FloatingActionButton
    private lateinit var mPreferences: SharedPreferences

    private lateinit var mBlurTop: BlurView
    private lateinit var mToolbar: Toolbar

    private var mPosInitial = 0
    private var mToolbarTitle = ""

    private val mHandler = Handler(Looper.getMainLooper())
    private val mDismissUpButton = Runnable { mScrollUp.hide() }
    private val mDismissDownButton = Runnable { mScrollDown.hide() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        mPreferences = GeneralConsts.getSharedPreferences(requireContext())

        requireArguments().let {
            mPosInitial = if (it.containsKey(GeneralConsts.KEYS.CHAPTERS.PAGE)) it.getInt(GeneralConsts.KEYS.CHAPTERS.PAGE) else 0
            mToolbarTitle = it.getString(GeneralConsts.KEYS.CHAPTERS.TITLE, "")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_chapters, container, false)

        mRecyclerView = root.findViewById(R.id.chapters_recycler_view)
        mScrollUp = root.findViewById(R.id.chapter_scroll_up)
        mScrollDown = root.findViewById(R.id.chapter_scroll_down)
        mToolbar = root.findViewById(R.id.toolbar_chapter)
        mBlurTop = root.findViewById(R.id.chapter_blur_top)

        (requireActivity() as MenuActivity).setActionBar(mToolbar)
        setupBlurViews(root)
        setupWindowInsets()
        setupTitleBackgrounds()

        mToolbar.title = mToolbarTitle

        mScrollUp.visibility = View.GONE
        mScrollDown.visibility = View.GONE

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mScrollUp.setOnClickListener {
            (mScrollUp.drawable as AnimatedVectorDrawable).start()
            mRecyclerView.smoothScrollToPosition(0)
        }
        mScrollUp.setOnLongClickListener {
            (mScrollUp.drawable as AnimatedVectorDrawable).start()
            mRecyclerView.scrollToPosition(0)
            true
        }
        mScrollDown.setOnClickListener {
            (mScrollDown.drawable as AnimatedVectorDrawable).start()
            mRecyclerView.smoothScrollToPosition((mRecyclerView.adapter as RecyclerView.Adapter).itemCount)
        }
        mScrollDown.setOnLongClickListener {
            (mScrollDown.drawable as AnimatedVectorDrawable).start()
            mRecyclerView.scrollToPosition((mRecyclerView.adapter as RecyclerView.Adapter).itemCount -1)
            true
        }

        mRecyclerView.setOnScrollChangeListener { _, _, _, _, yOld ->
            if (yOld > 20) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (mHandler.hasCallbacks(mDismissDownButton))
                        mHandler.removeCallbacks(mDismissDownButton)
                } else
                    mHandler.removeCallbacks(mDismissDownButton)

                mScrollDown.hide()
            } else if (yOld < -20) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (mHandler.hasCallbacks(mDismissUpButton))
                        mHandler.removeCallbacks(mDismissUpButton)
                } else
                    mHandler.removeCallbacks(mDismissUpButton)

                mScrollUp.hide()
            }

            if (yOld > 150) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (mHandler.hasCallbacks(mDismissUpButton))
                        mHandler.removeCallbacks(mDismissUpButton)
                } else
                    mHandler.removeCallbacks(mDismissUpButton)

                mHandler.postDelayed(mDismissUpButton, 3000)
                mScrollUp.show()
            } else if (yOld < -150) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (mHandler.hasCallbacks(mDismissUpButton))
                        mHandler.removeCallbacks(mDismissUpButton)
                } else
                    mHandler.removeCallbacks(mDismissUpButton)

                mHandler.postDelayed(mDismissDownButton, 3000)
                mScrollDown.show()
            }
        }

        mRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val isGlass = mPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)
                if (isGlass) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        mBlurTop.setBlurAutoUpdate(false)
                    } else {
                        mBlurTop.setBlurAutoUpdate(true)
                    }
                }
            }
        })

        val listener = object : ChapterCardListener {
            override fun onClick(page: Chapters) {
                val bundle = Bundle()
                bundle.putString(GeneralConsts.KEYS.CHAPTERS.TITLE, page.title)
                bundle.putInt(GeneralConsts.KEYS.CHAPTERS.NUMBER, page.number)
                bundle.putInt(GeneralConsts.KEYS.CHAPTERS.PAGE, page.page)
                (requireActivity() as MenuActivity).onBack(bundle)
            }

            override fun onLongClick(page: Chapters) {
                openImageDetail(page)
            }
        }

        val adapter = ChaptersGridAdapter()
        adapter.attachListener(listener)
        mRecyclerView.adapter = adapter

        val columnWidth: Int = resources.getDimension(R.dimen.chapters_grid_card_layout_width).toInt() + 1
        val spaceCount: Int = max(1, (Resources.getSystem().displayMetrics.widthPixels -3) / columnWidth)
        mRecyclerView.layoutManager = StaggeredGridLayoutManager(spaceCount, StaggeredGridLayoutManager.VERTICAL)

        observer()
        SharedData.addListener(this)
    }

    private fun observer() {
        SharedData.chapters.observe(viewLifecycleOwner) {
            (mRecyclerView.adapter as ChaptersGridAdapter).updateList(it)
        }
        if (mPosInitial > 0)
            mRecyclerView.scrollToPosition(mPosInitial)
    }

    override fun onDestroy() {
        SharedData.remListener(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (mHandler.hasCallbacks(mDismissUpButton))
                mHandler.removeCallbacks(mDismissUpButton)
            if (mHandler.hasCallbacks(mDismissDownButton))
                mHandler.removeCallbacks(mDismissDownButton)
        } else {
            mHandler.removeCallbacks(mDismissUpButton)
            mHandler.removeCallbacks(mDismissDownButton)
        }

        super.onDestroy()
    }

    override fun onLoading(page: Int) {
        (mRecyclerView.adapter as ChaptersGridAdapter).notifyItemChanged(page)
    }

    private fun openImageDetail(page: Chapters) {
        val layout = LayoutInflater.from(requireContext()).inflate(R.layout.popup_chapter_detail, null, false)
        val image = layout.findViewById<ImageView>(R.id.popup_chapter_detail)
        val name = layout.findViewById<TextView>(R.id.popup_chapter_name)

        val popup = MaterialAlertDialogBuilder(requireContext(), R.style.AppCompatMaterialAlertDialog)
            .setView(layout)
            .create()

        ImageUtil.setZoomPinch(requireContext(), image) { popup.dismiss() }

        image.setImageBitmap(page.image)
        name.text = page.title

        layout.findViewById<LinearLayout>(R.id.popup_chapter_background).setOnClickListener { popup.dismiss() }
        popup.show()
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
    }

    override fun onPause() {
        super.onPause()
        mBlurTop.setBlurAutoUpdate(false)
        mBlurTop.setBlurEnabled(false)
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
        mBlurTop.setupWith(rootView, blurAlgorithm)
            .setFrameClearDrawable(background)
            .setBlurRadius(15f)
    }

    private fun setupTitleBackgrounds() {
        val barLayout = view?.findViewById<View>(R.id.content_toolbar_chapter)
        val activity = activity ?: return
        MenuUtil.setupToolbar(activity, mToolbar, mBlurTop, barLayout)
    }

}
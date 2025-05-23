package br.com.fenix.bilingualreader.view.ui.pages_link

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.LinkedFile
import br.com.fenix.bilingualreader.model.entity.LinkedPage
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.LoadFile
import br.com.fenix.bilingualreader.model.enums.PageLinkType
import br.com.fenix.bilingualreader.service.controller.SubTitleController
import br.com.fenix.bilingualreader.service.listener.PageLinkCardListener
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.constants.PageLinkConsts
import br.com.fenix.bilingualreader.util.helpers.ImageUtil
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil.ThemeUtils.getColorFromAttr
import br.com.fenix.bilingualreader.util.helpers.Util
import br.com.fenix.bilingualreader.view.adapter.page_link.PageLinkCardAdapter
import br.com.fenix.bilingualreader.view.adapter.page_link.PageNotLinkCardAdapter
import br.com.fenix.bilingualreader.view.components.ComponentsUtil
import br.com.fenix.bilingualreader.view.components.ImageShadowBuilder
import br.com.fenix.bilingualreader.view.components.MaterialButtonExpanded
import br.com.fenix.bilingualreader.view.components.manga.TextViewEllipsizing
import br.com.fenix.bilingualreader.view.ui.menu.MenuActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import org.slf4j.LoggerFactory
import java.lang.ref.WeakReference


class PagesLinkFragment : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(PagesLinkFragment::class.java)

    private val mViewModel: PagesLinkViewModel by viewModels()
    private lateinit var mRoot: ConstraintLayout
    private lateinit var mImageLoading: CircularProgressIndicator
    private lateinit var mScrollUp: FloatingActionButton
    private lateinit var mScrollDown: FloatingActionButton
    private lateinit var mRecyclerPageLink: RecyclerView
    private lateinit var mRecyclerPageNotLink: RecyclerView
    private lateinit var mPageNotLinkContent: ConstraintLayout
    private lateinit var mPageNotLinkIcon: ImageView
    private lateinit var mContent: LinearLayout
    private lateinit var mFileLink: TextInputLayout
    private lateinit var mFileLinkAutoComplete: MaterialAutoCompleteTextView
    private lateinit var mFileLinkLanguage: TextInputLayout
    private lateinit var mFileLinkLanguageAutoComplete: MaterialAutoCompleteTextView
    private lateinit var mContentButton: LinearLayout
    private lateinit var mSave: Button
    private lateinit var mReload: MaterialButtonExpanded
    private lateinit var mFullScreen: MaterialButton
    private lateinit var mListener: PageLinkCardListener
    private lateinit var mButtonsGroup: MaterialButtonToggleGroup
    private lateinit var mAutoProcess: MaterialButtonExpanded
    private lateinit var mReorderPages: MaterialButtonExpanded
    private lateinit var mSinglePages: MaterialButtonExpanded
    private lateinit var mDualPages: MaterialButtonExpanded
    private lateinit var mUndoChanges: MaterialButtonExpanded
    private lateinit var mHelp: MaterialButtonExpanded
    private lateinit var mDelete: MaterialButtonExpanded
    private lateinit var mPagesIndex: MaterialButton
    private lateinit var mForceImageReload: MaterialButton
    private lateinit var mToolbar: androidx.appcompat.widget.Toolbar
    private lateinit var mMangaName: TextView

    private lateinit var mMapLanguage: HashMap<String, Languages>
    private val mImageLoadHandler: Handler = ImageLoadHandler(this)
    private var mShowScrollButton: Boolean = true
    private val mHandler = Handler(Looper.getMainLooper())

    private val mDismissUpButton = Runnable { mScrollUp.hide() }
    private val mDismissDownButton = Runnable { mScrollDown.hide() }
    private val mReduceSizeGroupButton = Runnable {
        ComponentsUtil.changeWidthAnimateSize(mButtonsGroup, mCollapseButtonsGroupSize, true)
        changColorButton(false)
    }

    private var mUseDualPageCalculate = false
    private var mAutoReorderPages: Boolean = true
    private var mInDrag: Boolean = false
    private var mIsTabletOrLandscape: Boolean = false
    private var mPageSelected: Int = 0
    private lateinit var mCollapseButtonsGroupSize: ConstraintLayout.LayoutParams
    private lateinit var mExpandedButtonsGroupSize: ConstraintLayout.LayoutParams

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_manga_pages_link, container, false)

        mRoot = root.findViewById(R.id.pages_link_root)
        mRecyclerPageLink = root.findViewById(R.id.pages_link_pages_linked_recycler)
        mRecyclerPageNotLink = root.findViewById(R.id.pages_link_pages_not_linked_recycler)
        mPageNotLinkContent = root.findViewById(R.id.pages_link_content_pages_not_linked)
        mPageNotLinkIcon = root.findViewById(R.id.pages_link_icon_pages_not_linked)

        mContent = root.findViewById(R.id.pages_link_content)
        mFileLink = root.findViewById(R.id.pages_link_file_link_text)
        mFileLinkAutoComplete = root.findViewById(R.id.pages_link_file_link_autocomplete)
        mFileLinkLanguage = root.findViewById(R.id.pages_link_language_combo)
        mFileLinkLanguageAutoComplete = root.findViewById(R.id.pages_link_language_autocomplete)
        mPagesIndex = root.findViewById(R.id.pages_link_pages_index)
        mMangaName = root.findViewById(R.id.pages_link_name_manga)

        mContentButton = root.findViewById(R.id.pages_link_buttons_content)
        mSave = root.findViewById(R.id.pages_link_save_button)
        mReload = root.findViewById(R.id.pages_link_reload_button)
        mFullScreen = root.findViewById(R.id.pages_link_full_screen_button)

        mImageLoading = root.findViewById(R.id.pages_link_loading_progress)
        mScrollUp = root.findViewById(R.id.pages_link_scroll_up)
        mScrollDown = root.findViewById(R.id.pages_link_scroll_down)

        mButtonsGroup = root.findViewById(R.id.page_link_buttons_group)
        mAutoProcess = root.findViewById(R.id.pages_link_auto_process_button)
        mReorderPages = root.findViewById(R.id.pages_link_reorder_button)
        mSinglePages = root.findViewById(R.id.pages_link_single_page_button)
        mDualPages = root.findViewById(R.id.pages_link_dual_page_button)
        mUndoChanges = root.findViewById(R.id.pages_link_undo_changes_button)
        mHelp = root.findViewById(R.id.pages_link_help_button)
        mDelete = root.findViewById(R.id.file_link_delete_button)
        mForceImageReload = root.findViewById(R.id.pages_link_force_image_reload)
        mToolbar = root.findViewById(R.id.toolbar_manga_pages_link)

        (requireActivity() as PagesLinkActivity).setActionBar(mToolbar)

        if (mHelp.tag.toString().compareTo("not_used", true) != 0) {
            mExpandedButtonsGroupSize = mButtonsGroup.layoutParams as ConstraintLayout.LayoutParams
            mButtonsGroup.layoutParams = ConstraintLayout.LayoutParams(mButtonsGroup.layoutParams as ConstraintLayout.LayoutParams)
            mButtonsGroup.layoutParams.width = resources.getDimension(R.dimen.page_link_buttons_size).toInt()
            mCollapseButtonsGroupSize = mButtonsGroup.layoutParams as ConstraintLayout.LayoutParams
        }

        mScrollUp.visibility = View.GONE
        mScrollDown.visibility = View.GONE

        mScrollUp.setOnClickListener {
            (mScrollUp.drawable as AnimatedVectorDrawable).start()
            mRecyclerPageLink.smoothScrollToPosition(0)
        }
        mScrollUp.setOnLongClickListener {
            (mScrollUp.drawable as AnimatedVectorDrawable).start()
            mRecyclerPageLink.scrollToPosition(0)
            true
        }
        mScrollDown.setOnClickListener {
            (mScrollDown.drawable as AnimatedVectorDrawable).start()
            mRecyclerPageLink.smoothScrollToPosition((mRecyclerPageLink.adapter as RecyclerView.Adapter).itemCount)
        }
        mScrollDown.setOnLongClickListener {
            (mScrollDown.drawable as AnimatedVectorDrawable).start()
            mRecyclerPageLink.scrollToPosition((mRecyclerPageLink.adapter as RecyclerView.Adapter).itemCount -1)
            true
        }

        mRecyclerPageLink.setOnScrollChangeListener { _, _, _, _, yOld ->
            if (mShowScrollButton) {
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
                        if (mHandler.hasCallbacks(mDismissDownButton))
                            mHandler.removeCallbacks(mDismissDownButton)
                    } else
                        mHandler.removeCallbacks(mDismissDownButton)

                    mHandler.postDelayed(mDismissDownButton, 3000)
                    mScrollDown.show()
                }
            } else {
                mScrollUp.hide()
                mScrollDown.hide()
            }
        }

        mFileLinkAutoComplete.setOnClickListener {
            choiceSelectManga()
        }

        val languages = resources.getStringArray(R.array.languages)
        mMapLanguage = hashMapOf(
            languages[0] to Languages.PORTUGUESE,
            languages[1] to Languages.ENGLISH,
            languages[2] to Languages.JAPANESE
        )

        mFileLinkLanguageAutoComplete.setAdapter(ArrayAdapter(requireContext(), R.layout.list_item, mMapLanguage.keys.toTypedArray()))
        mFileLinkLanguageAutoComplete.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                val language = if (parent.getItemAtPosition(position).toString().isNotEmpty() &&
                    mMapLanguage.containsKey(parent.getItemAtPosition(position).toString())
                )
                    mMapLanguage[parent.getItemAtPosition(position).toString()]
                else
                    null

                mViewModel.setLanguage(language)
            }

        mUseDualPageCalculate = GeneralConsts.getSharedPreferences(requireContext())
            .getBoolean(GeneralConsts.KEYS.PAGE_LINK.USE_DUAL_PAGE_CALCULATE, false)

        mSave.setOnClickListener { save() }
        mReload.setOnClickListener {
            (mReload.icon as AnimatedVectorDrawable).start()
            refresh()
        }

        mAutoProcess.setOnClickListener {
            (mAutoProcess.icon as AnimatedVectorDrawable).start()
            mViewModel.autoReorderDoublePages(PageLinkType.LINKED, true)
        }
        mReorderPages.setOnClickListener {
            (mReorderPages.icon as AnimatedVectorDrawable).start()
            mViewModel.reorderBySortPages()
        }
        mSinglePages.setOnClickListener {
            (mSinglePages.icon as AnimatedVectorDrawable).start()
            mViewModel.reorderSimplePages()
        }
        mDualPages.setOnClickListener {
            (mDualPages.icon as AnimatedVectorDrawable).start()
            mViewModel.reorderDoublePages(mUseDualPageCalculate)
        }
        mUndoChanges.setOnClickListener {
            (mUndoChanges.icon as AnimatedVectorDrawable).start()
            mViewModel.returnBackup()
        }

        mDelete.setOnClickListener {
            (mDelete.icon as AnimatedVectorDrawable).start()
            MaterialAlertDialogBuilder(requireActivity(), R.style.AppCompatAlertDialogStyle)
                .setTitle(getString(R.string.manga_library_menu_delete))
                .setMessage(getString(R.string.page_link_delete_description))
                .setPositiveButton(
                    R.string.action_positive
                ) { _, _ ->
                    mViewModel.delete { index, type -> notifyItemChanged(type, index) }
                }
                .setNegativeButton(
                    R.string.action_negative
                ) { _, _ -> }
                .create().show()
        }

        mHelp.setOnClickListener {
            (mHelp.icon as AnimatedVectorDrawable).start()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (mHandler.hasCallbacks(mReduceSizeGroupButton))
                    mHandler.removeCallbacks(mReduceSizeGroupButton)
            } else
                mHandler.removeCallbacks(mReduceSizeGroupButton)

            mHandler.postDelayed(mReduceSizeGroupButton, 5000)

            ComponentsUtil.changeWidthAnimateSize(mButtonsGroup, mExpandedButtonsGroupSize, false)
            changColorButton(true)
        }

        mFullScreen.setOnClickListener {
            val visible = mContent.visibility == View.GONE

            if (mHelp.tag.toString().compareTo("not_used", true) != 0)
                ComponentsUtil.changeAnimateVisibility(mHelp, visible)

            ComponentsUtil.changeAnimateVisibility(
                arrayListOf(mContent, mSave, mReload, mButtonsGroup, mToolbar),
                visible
            )

            val image = if (visible)
                R.drawable.ico_animated_full_screen_exit
            else
                R.drawable.ico_animated_full_screen_enter

            mFullScreen.icon = ContextCompat.getDrawable(requireContext(), image)
            (mFullScreen.icon as AnimatedVectorDrawable).start()
        }

        mPagesIndex.setOnClickListener {
            (mPagesIndex.icon as AnimatedVectorDrawable).start()
            openMenuIndexes()
        }

        mForceImageReload.setOnClickListener { mViewModel.reLoadImages() }

        mListener = object : PageLinkCardListener {
            override fun onClick(view: View, page: LinkedPage, isManga: Boolean, isRight: Boolean) {
                if (!isManga)
                    openPopupFunctions(view, page, isRight)
            }

            override fun onClickLong(view: View, page: LinkedPage, origin: PageLinkType, position: Int): Boolean {
                mAutoReorderPages = false
                val pageLink = if (origin == PageLinkType.NOT_LINKED) mViewModel.getPageNotLink(page) else mViewModel.getPageLink(page)
                val item = ClipData.Item(pageLink)
                val name = if (origin == PageLinkType.DUAL_PAGE) page.fileLinkRightPageName else page.fileLinkLeftPageName
                val dragData = ClipData(position.toString(), arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN), item)
                dragData.addItem(ClipData.Item(origin.name))
                dragData.addItem(ClipData.Item(name))
                val myShadow = if (origin == PageLinkType.NOT_LINKED && page.imageLeftFileLinkPage != null)
                    ImageShadowBuilder(createNotLinkView(page))
                else
                    View.DragShadowBuilder(view)

                view.startDragAndDrop(
                    dragData,
                    myShadow,
                    view,
                    0
                )
                view.visibility = View.INVISIBLE
                return true
            }

            override fun onDoubleClick(view: View, page: LinkedPage, isManga: Boolean, isRight: Boolean) {
                openImageDetail(page, isManga)
            }

            override fun onDropItem(origin: PageLinkType, destiny: PageLinkType, dragIndex: String, drop: LinkedPage) {
                when {
                    origin == PageLinkType.DUAL_PAGE || destiny == PageLinkType.DUAL_PAGE -> {
                        val pageLink = if (origin == PageLinkType.NOT_LINKED)
                            mViewModel.getPageNotLink(Integer.valueOf(dragIndex))
                        else
                            mViewModel.getPageLink(Integer.valueOf(dragIndex))
                        mViewModel.onMoveDualPage(origin, pageLink, destiny, drop)
                    }
                    origin == PageLinkType.LINKED && destiny == PageLinkType.LINKED -> mViewModel.onMove(
                        mViewModel.getPageLink(Integer.valueOf(dragIndex)),
                        drop
                    )
                    origin == PageLinkType.LINKED && destiny == PageLinkType.NOT_LINKED -> {
                        mViewModel.onNotLinked(mViewModel.getPageLink(Integer.valueOf(dragIndex)))
                    }
                    origin == PageLinkType.NOT_LINKED && destiny == PageLinkType.LINKED -> mViewModel.fromNotLinked(
                        mViewModel.getPageNotLink(
                            Integer.valueOf(
                                dragIndex
                            )
                        ), drop
                    )
                }
            }

            override fun onDragScrolling(pointScreen: Point) {
                onPageLinkScrolling(pointScreen)
            }

            override fun onAddNotLink(page: LinkedPage, isRight: Boolean) {
                mViewModel.addNotLinked(page, isRight)
            }
        }

        mRecyclerPageLink.setOnDragListener { _, dragEvent ->
            when (dragEvent.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    mInDrag = true
                    mShowScrollButton = false
                    true
                }

                DragEvent.ACTION_DRAG_ENDED -> {
                    mInDrag = false
                    mShowScrollButton = true
                    true
                }
                else -> true
            }
        }

        mRecyclerPageNotLink.setOnDragListener { _, dragEvent ->
            when (dragEvent.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    mPageNotLinkContent.background = AppCompatResources.getDrawable(requireContext(), R.drawable.file_linked_rounded_border)
                    mPageNotLinkIcon.visibility = View.VISIBLE
                    mShowScrollButton = false
                    dragEvent.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                }

                DragEvent.ACTION_DRAG_ENTERED -> {
                    mPageNotLinkContent.background = AppCompatResources.getDrawable(
                        requireContext(),
                        R.drawable.file_linked_background_selected
                    )
                    mPageNotLinkIcon.visibility = View.VISIBLE
                    true
                }

                DragEvent.ACTION_DRAG_EXITED -> {
                    mPageNotLinkContent.background = AppCompatResources.getDrawable(requireContext(), R.drawable.file_linked_rounded_border)
                    mPageNotLinkIcon.visibility = View.VISIBLE
                    true
                }

                DragEvent.ACTION_DROP -> {
                    when (val origin = PageLinkType.valueOf(dragEvent.clipData.getItemAt(PageLinkConsts.CLIPDATA.PAGE_TYPE).text.toString())) {
                        PageLinkType.LINKED -> mViewModel.onNotLinked(
                            mViewModel.getPageLink(
                                Integer.valueOf(
                                    dragEvent.clipData.getItemAt(
                                        PageLinkConsts.CLIPDATA.PAGE_LINK
                                    ).text.toString()
                                )
                            )
                        )
                        PageLinkType.DUAL_PAGE -> {
                            val pageLink = mViewModel.getPageLink(Integer.valueOf(dragEvent.clipData.getItemAt(PageLinkConsts.CLIPDATA.PAGE_LINK).text.toString()))
                            mViewModel.onMoveDualPage(origin, pageLink, PageLinkType.NOT_LINKED, pageLink)
                        }
                        else -> {}
                    }
                    true
                }

                DragEvent.ACTION_DRAG_ENDED -> {
                    mPageNotLinkContent.background = AppCompatResources.getDrawable(requireContext(), R.drawable.file_linked_background)
                    mPageNotLinkIcon.visibility = View.INVISIBLE
                    mShowScrollButton = true

                    val v = dragEvent.localState as View
                    if (!dragEvent.result || v.tag.toString().compareTo(PageLinkConsts.TAG.PAGE_LINK_RIGHT, true) != 0)
                        v.visibility = View.VISIBLE
                    true
                }

                else -> true
            }
        }

        observer()
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapterPageLink = PageLinkCardAdapter()
        mRecyclerPageLink.adapter = adapterPageLink
        mRecyclerPageLink.layoutManager = LinearLayoutManager(requireContext())
        adapterPageLink.attachListener(mListener)

        mIsTabletOrLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ||
                mRecyclerPageNotLink.tag.toString().compareTo("vertical", true) == 0

        val adapterPageNotLink = PageNotLinkCardAdapter()
        mRecyclerPageNotLink.adapter = adapterPageNotLink
        val orientation = if (mIsTabletOrLandscape) RecyclerView.VERTICAL else RecyclerView.HORIZONTAL
        mRecyclerPageNotLink.layoutManager = LinearLayoutManager(requireContext(), orientation, false)
        adapterPageNotLink.attachListener(mListener)

        if (savedInstanceState != null) {
            val fileLink = savedInstanceState.getSerializable(GeneralConsts.KEYS.OBJECT.PAGE_LINK)
            if (fileLink != null)
                mViewModel.reload(fileLink as LinkedFile) { index, type -> notifyItemChanged(type, index) }
            else
                mViewModel.reLoadImages(PageLinkType.ALL, true, isCloseThreads = true)
            mMangaName.text = mViewModel.getMangaName()
        } else {
            val bundle = this.arguments
            if (bundle != null && bundle.containsKey(GeneralConsts.KEYS.OBJECT.MANGA)) {
                mViewModel.loadManga(bundle[GeneralConsts.KEYS.OBJECT.MANGA] as Manga) { index, type -> notifyItemChanged(type, index) }
                mMangaName.text = mViewModel.getMangaName()
                mPageSelected = bundle.getInt(GeneralConsts.KEYS.MANGA.PAGE_NUMBER, 0)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GeneralConsts.REQUEST.OPEN_PAGE_LINK) {
                resultData?.data?.also { uri ->
                    try {
                        mAutoReorderPages = true
                        val path = Util.normalizeFilePath(uri.path.toString())
                        val loaded = mViewModel.readFileLink(path) { index, type -> notifyItemChanged(type, index) }
                        if (loaded != LoadFile.LOADED) {
                            val msg = if (loaded == LoadFile.ERROR_FILE_WRONG) getString(R.string.page_link_load_file_wrong) else getString(
                                R.string.page_link_load_error
                            )
                            MaterialAlertDialogBuilder(requireContext(), R.style.AppCompatAlertDialogStyle)
                                .setTitle(msg)
                                .setMessage(path)
                                .setPositiveButton(
                                    R.string.action_neutral
                                ) { _, _ -> }
                                .create()
                                .show()
                        }
                    } catch (e: Exception) {
                        mLOGGER.warn("Error when open file: " + e.message, e)
                    }
                }

            } else if (requestCode == GeneralConsts.REQUEST.SELECT_MANGA) {
                resultData?.extras?.also {
                    if (it.containsKey(GeneralConsts.KEYS.OBJECT.MANGA)) {
                        val link = it.getSerializable(GeneralConsts.KEYS.OBJECT.MANGA) as Manga
                        val loaded = mViewModel.readFileLink(link.path) { index, type -> notifyItemChanged(type, index) }
                        if (loaded != LoadFile.LOADED) {
                            val msg = if (loaded == LoadFile.ERROR_FILE_WRONG) getString(R.string.page_link_load_file_wrong) else getString(
                                R.string.page_link_load_error
                            )
                            MaterialAlertDialogBuilder(requireContext(), R.style.AppCompatAlertDialogStyle)
                                .setTitle(msg)
                                .setMessage(link.path)
                                .setPositiveButton(
                                    R.string.action_neutral
                                ) { _, _ -> }
                                .create()
                                .show()
                        }
                    }
                }
            }
        } else {
            mFileLinkAutoComplete.setText("")
            mViewModel.clearFileLink { index, type -> notifyItemChanged(type, index) }
        }
    }

    private fun onPageLinkScrolling(point: Point) {
        val recycler = Rect()
        mRecyclerPageLink.getGlobalVisibleRect(recycler)

        val space = recycler.height() / 4
        val spaceTop = recycler.top + space
        val spaceBottom = recycler.bottom - space

        val padding = if (point.y < spaceTop) {
            val fast = (space / 4)
            if (point.y < (recycler.top + fast)) -600
            else if (point.y < (recycler.top + (fast * 2))) -350
            else -150
        } else if (point.y > spaceBottom) {
            val fast = (space / 4)
            if (point.y > (recycler.bottom - fast)) +600
            else if (point.y > (recycler.bottom - (fast * 2))) +350
            else +150
        } else 0

        if (padding != 0)
            mRecyclerPageLink.smoothScrollBy(0, padding)
    }

    private fun observer() {
        mViewModel.pagesLink.observe(viewLifecycleOwner) {
            (mRecyclerPageLink.adapter as PageLinkCardAdapter).updateList(it)
        }

        mViewModel.pagesLinkNotLinked.observe(viewLifecycleOwner) {
            (mRecyclerPageNotLink.adapter as PageNotLinkCardAdapter).updateList(it)
        }

        mViewModel.linkedFile.observe(viewLifecycleOwner) {
            val description = it?.name ?: ""
            mFileLinkAutoComplete.setText(description)
        }

        mViewModel.language.observe(viewLifecycleOwner) {
            var description = ""

            if (it != null) {
                for (language in mMapLanguage.entries)
                    if (language.value.compareTo(it) == 0)
                        description = language.key
            }

            mFileLinkLanguageAutoComplete.setText(description, false)
        }

        mViewModel.hasBackup.observe(viewLifecycleOwner) {
            mUndoChanges.isEnabled = it
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun notifyItemChanged(type: PageLinkType, index: Int?, add: Boolean = false, remove: Boolean = false) {
        when {
            type == PageLinkType.NOT_LINKED && add && index != null && index > -1 -> (mRecyclerPageNotLink.adapter as PageNotLinkCardAdapter).notifyItemInserted(
                index
            )
            type == PageLinkType.NOT_LINKED && remove && index != null && index > -1 -> (mRecyclerPageNotLink.adapter as PageNotLinkCardAdapter).notifyItemRemoved(
                index
            )
            type == PageLinkType.NOT_LINKED && index != null && index > -1 -> (mRecyclerPageNotLink.adapter as PageNotLinkCardAdapter).notifyItemChanged(
                index
            )
            type == PageLinkType.NOT_LINKED && (index == null || index == -1) -> (mRecyclerPageNotLink.adapter as PageNotLinkCardAdapter).notifyDataSetChanged()

            type != PageLinkType.NOT_LINKED && add && index != null && index > -1 -> (mRecyclerPageLink.adapter as PageLinkCardAdapter).notifyItemInserted(
                index
            )
            type != PageLinkType.NOT_LINKED && remove && index != null && index > -1 -> (mRecyclerPageLink.adapter as PageLinkCardAdapter).notifyItemRemoved(
                index
            )
            type != PageLinkType.NOT_LINKED && index != null && index > -1 -> (mRecyclerPageLink.adapter as PageLinkCardAdapter).notifyItemChanged(
                index
            )
            type != PageLinkType.NOT_LINKED && (index == null || index == -1) -> (mRecyclerPageLink.adapter as PageLinkCardAdapter).notifyDataSetChanged()
        }
    }

    private fun enableContent(enabled: Boolean) {
        mAutoReorderPages = false
        mRecyclerPageLink.isEnabled = enabled
        mRecyclerPageNotLink.isEnabled = enabled
        mFileLink.isEnabled = enabled
        mFileLinkLanguage.isEnabled = enabled
        mSave.isEnabled = enabled
        mReload.isEnabled = enabled
        mButtonsGroup.isEnabled = enabled
        mPagesIndex.isEnabled = enabled

        mUndoChanges.isEnabled = if (enabled) mViewModel.hasBackup() else false
    }

    private fun changColorButton(expanded: Boolean) {
        mAutoProcess.setIsExpanded(expanded)
        mReorderPages.setIsExpanded(expanded)
        mSinglePages.setIsExpanded(expanded)
        mDualPages.setIsExpanded(expanded)
        mHelp.setIsExpanded(expanded)
        mReload.setIsExpanded(expanded)
        mDelete.setIsExpanded(expanded)
        mUndoChanges.setIsExpanded(expanded)

        mAutoProcess.refreshDrawableState()
        mReorderPages.refreshDrawableState()
        mSinglePages.refreshDrawableState()
        mDualPages.refreshDrawableState()
        mHelp.refreshDrawableState()
        mReload.refreshDrawableState()
        mDelete.refreshDrawableState()
        mUndoChanges.refreshDrawableState()

        if (mPagesIndex.tag.toString().compareTo("color", true) == 0) {
            (mPagesIndex as MaterialButtonExpanded).setIsExpanded(expanded)
            mPagesIndex.refreshDrawableState()
        }
    }

    private fun save() {
        enableContent(false)

        mViewModel.save()
        Toast.makeText(
            requireContext(),
            getString(R.string.page_link_saved),
            Toast.LENGTH_SHORT
        ).show()

        enableContent(true)
    }

    private fun refresh() {
        enableContent(false)

        mViewModel.reloadPageLink { index, type -> notifyItemChanged(type, index) }
        Toast.makeText(
            requireContext(),
            getString(R.string.page_link_refreshed),
            Toast.LENGTH_SHORT
        ).show()

        enableContent(true)
    }

    override fun onResume() {
        super.onResume()
        mViewModel.addImageLoadHandler(mImageLoadHandler)
        processImageLoading(isVerify = true)
        mRecyclerPageLink.scrollToPosition(mPageSelected)
    }

    override fun onPause() {
        mPageSelected = (mRecyclerPageLink.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
        mViewModel.removeImageLoadHandler(mImageLoadHandler)
        super.onPause()
    }

    override fun onStop() {
        SubTitleController.getInstance(requireContext()).setFileLink(mViewModel.getFileLink(null))
        super.onStop()
    }

    override fun onDestroy() {
        mViewModel.endThread()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (mHandler.hasCallbacks(mDismissUpButton))
                mHandler.removeCallbacks(mDismissUpButton)
            if (mHandler.hasCallbacks(mDismissDownButton))
                mHandler.removeCallbacks(mDismissDownButton)
            if (mHandler.hasCallbacks(mReduceSizeGroupButton))
                mHandler.removeCallbacks(mReduceSizeGroupButton)

            if (mHandler.hasCallbacks(mVerifyAllImagesFinished))
                mHandler.removeCallbacks(mVerifyAllImagesFinished)
            if (mHandler.hasCallbacks(mVerifyAllImagesFinishedDelay))
                mHandler.removeCallbacks(mVerifyAllImagesFinishedDelay)
        } else {
            mHandler.removeCallbacks(mDismissUpButton)
            mHandler.removeCallbacks(mDismissDownButton)
            mHandler.removeCallbacks(mReduceSizeGroupButton)

            mHandler.removeCallbacks(mVerifyAllImagesFinished)
            mHandler.removeCallbacks(mVerifyAllImagesFinishedDelay)
        }

        mViewModel.onDestroy()
        super.onDestroy()
    }

    private fun openMenuIndexes() {
        val fileLinkLoaded = mViewModel.linkedFile.value?.path?.isNotEmpty() ?: false
        if (!fileLinkLoaded)
            openMenuIndexes(true)
        else {
            val (manga, linked) = mViewModel.getFilesNames()
            val items = arrayOf(manga, linked)
            MaterialAlertDialogBuilder(requireActivity(), R.style.AppCompatAlertDialogStyleFileChoice)
                .setTitle(resources.getString(R.string.page_link_select_file_page_index))
                .setItems(items) { _, selected ->
                    val itemSelected = items[selected]
                    openMenuIndexes(itemSelected == manga)
                }
                .show()
        }
    }

    private fun openMenuIndexes(isMangaIndexes: Boolean) {
        val paths = mViewModel.getPagesIndex(isMangaIndexes)

        if (paths.isEmpty()) {
            MaterialAlertDialogBuilder(requireContext(), R.style.AppCompatAlertDialogStyle)
                .setTitle(resources.getString(R.string.reading_manga_page_index))
                .setMessage(resources.getString(R.string.reading_manga_page_empty))
                .setPositiveButton(
                    R.string.action_neutral
                ) { _, _ -> }
                .create()
                .show()
            return
        }

        val items = paths.keys.toTypedArray()

        val title = LinearLayout(requireContext())
        title.orientation = LinearLayout.VERTICAL
        title.setPadding(resources.getDimensionPixelOffset(R.dimen.page_link_page_index_title_padding))
        val (manga, file) = mViewModel.getFilesNames()
        val name = TextView(requireContext())
        name.text = if (isMangaIndexes) manga else file
        name.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.title_index_dialog_size))
        name.setTextColor(requireContext().getColorFromAttr(R.attr.colorOnBackground))
        title.addView(name)
        val index = TextView(requireContext())
        index.text = resources.getString(R.string.reading_manga_page_index)
        index.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.title_small_index_dialog_size))
        index.setTextColor(requireContext().getColorFromAttr(R.attr.colorPrimary))
        title.addView(index)

        MaterialAlertDialogBuilder(requireActivity(), R.style.AppCompatAlertDialogStyle)
            .setCustomTitle(title)
            .setItems(items) { _, selected ->
                val pageIndex = paths[items[selected]]
                if (pageIndex != null)
                    mRecyclerPageLink.smoothScrollToPosition(pageIndex)
            }
            .show()
    }

    private fun choiceSelectManga() {
        val origins = requireContext().resources.getStringArray(R.array.origin_manga)
        MaterialAlertDialogBuilder(requireActivity(), R.style.AppCompatMaterialAlertList)
            .setTitle(R.string.page_link_select_origin_manga)
            .setItems(origins) { _, selected ->
                val origin = origins[selected]
                if (origin == origins[0])
                    openLibrarySelectManga()
                else
                    openIntentSelectManga()
            }
            .show()
    }

    private fun openLibrarySelectManga() {
        val intent = Intent(requireContext(), MenuActivity::class.java)
        val bundle = Bundle()
        bundle.putInt(GeneralConsts.KEYS.FRAGMENT.ID, R.id.frame_select_manga)
        bundle.putLong(GeneralConsts.KEYS.MANGA.ID, mViewModel.getMangaId())
        bundle.putString(GeneralConsts.KEYS.MANGA.NAME, mViewModel.getMangaName())
        intent.putExtras(bundle)
        requireActivity().overridePendingTransition(R.anim.fade_in_fragment_add_enter, R.anim.fade_out_fragment_remove_exit)
        startActivityForResult(intent, GeneralConsts.REQUEST.SELECT_MANGA, null)
    }

    private fun openIntentSelectManga() {
        val intent = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/zip|application/x-cbz|application/rar|application/x-cbr|application/x-rar-compressed|" +
                        "application/x-zip-compressed|application/cbr|application/cbz|*/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/*"
                putExtra(
                    Intent.EXTRA_MIME_TYPES, arrayOf(
                        "application/zip", "application/x-cbz", "application/rar", "application/x-cbr",
                        "application/x-rar-compressed", "application/x-zip-compressed", "application/cbr", "application/cbz"
                    )
                )
            }
        startActivityForResult(intent, GeneralConsts.REQUEST.OPEN_PAGE_LINK)
    }

    private fun createNotLinkView(page: LinkedPage): Bitmap {
        val card = LayoutInflater.from(requireContext()).inflate(R.layout.grid_card_page_link, null, false)
        val root = card.findViewById<MaterialCardView>(R.id.page_link_root)

        val image = card.findViewById<ImageView>(R.id.page_link_image)
        image.setImageBitmap(page.imageLeftFileLinkPage)
        image.visibility = View.VISIBLE

        card.findViewById<ProgressBar>(R.id.page_link_progress_bar).visibility = View.GONE
        card.findViewById<TextView>(R.id.page_link_page_number).text = page.fileLinkLeftPage.toString()
        card.findViewById<TextViewEllipsizing>(R.id.page_link_page_name).text = page.fileLinkLeftPagePath + "\\" + page.fileLinkLeftPageName

        card.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        card.layout(0, 0, card.measuredWidth, card.measuredHeight)

        val shadow = Bitmap.createBitmap(root.layoutParams.width, root.layoutParams.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(shadow)
        root.layout(root.left, root.top, root.right, root.bottom)
        root.draw(c)

        return shadow
    }

    private fun openPopupFunctions(view: View, page: LinkedPage, isRight: Boolean) {
        if (page.isNotLinked)
            return

        val popup = PopupMenu(ContextThemeWrapper(requireContext(), R.style.PopupMenu), view, 0,  R.attr.popupMenuStyle, R.style.PopupMenu)
        popup.menuInflater.inflate(R.menu.menu_page_link, popup.menu)

        if (isRight || page.fileLinkLeftPage != PageLinkConsts.VALUES.PAGE_EMPTY) {
            popup.menu.findItem(R.id.menu_page_link_popup_reorder_return).isVisible = false
            popup.menu.findItem(R.id.menu_page_link_popup_reorder_not_linked).isVisible = false
        }

        if ((!isRight && page.fileLinkLeftPage == PageLinkConsts.VALUES.PAGE_EMPTY) || (isRight && page.fileLinkRightPage == PageLinkConsts.VALUES.PAGE_EMPTY))
            popup.menu.findItem(R.id.menu_page_link_popup_add_not_linked).isVisible = false

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_page_link_popup_reorder_return -> mViewModel.reorderReturnPages(page)
                R.id.menu_page_link_popup_reorder_single_page -> mViewModel.reorderSimplePages(initial = page)
                R.id.menu_page_link_popup_reorder_dual_page -> mViewModel.reorderDoublePages(initial = page)
                R.id.menu_page_link_popup_reorder_auto_page -> mViewModel.autoReorderDoublePages(page)
                R.id.menu_page_link_popup_reorder_not_linked -> mViewModel.reorderNotLinked(page)
                R.id.menu_page_link_popup_add_not_linked -> mViewModel.addNotLinked(page, isRight)
            }
            true
        }

        popup.show()
    }

    private fun openImageDetail(page: LinkedPage, isManga: Boolean) {
        if (!isManga && page.fileLinkLeftPage == PageLinkConsts.VALUES.PAGE_EMPTY)
            return

        val layout = LayoutInflater.from(requireContext()).inflate(R.layout.popup_page_link_image_detail, null, false)
        val imageLeft = layout.findViewById<ImageView>(R.id.popup_image_detail_left)
        val name = layout.findViewById<TextView>(R.id.popup_image_name)

        val popup = MaterialAlertDialogBuilder(requireContext(), R.style.AppCompatMaterialAlertDialog)
            .setView(layout)
            .create()

        ImageUtil.setZoomPinch(requireContext(), imageLeft) { popup.dismiss() }

        if (isManga) {
            mViewModel.loadImageManga(page.mangaPage, imageLeft)
            name.text = page.mangaPageName
        } else if (page.isDualImage) {
            mViewModel.loadImagePageLink(page.fileLinkLeftPage, imageLeft)
            val imageRight = layout.findViewById<ImageView>(R.id.popup_image_detail_right)
            ImageUtil.setZoomPinch(requireContext(), imageRight) { popup.dismiss() }
            mViewModel.loadImagePageLink(page.fileLinkRightPage, imageRight)
            imageRight.visibility = View.VISIBLE
            name.text = page.fileLinkLeftPageName + "\n" + page.fileLinkRightPageName
        } else if (page.isNotLinked) {
            mViewModel.loadImagePageLink(page.fileLinkLeftPage, imageLeft)
            name.text = page.fileLinkLeftPagePath + "\\" + page.fileLinkLeftPageName
        } else {
            mViewModel.loadImagePageLink(page.fileLinkLeftPage, imageLeft)
            name.text = page.fileLinkLeftPageName
        }

        layout.findViewById<LinearLayout>(R.id.popup_image_background).setOnClickListener { popup.dismiss() }
        popup.show()
    }

    private fun processImageLoading(isInitial: Boolean = false, isEnding: Boolean = false, isVerify: Boolean = false) {
        if (isInitial) {
            mImageLoading.isIndeterminate = true
            mImageLoading.visibility = View.VISIBLE
        } else if (isEnding || isVerify) {
            mImageLoading.isIndeterminate = isVerify
            val progress = mViewModel.imageThreadLoadingProgress()
            mImageLoading.visibility = if (isVerify && progress > 0 || isEnding && progress > 1)
                View.VISIBLE
            else
                View.INVISIBLE
        } else {
            val (progress, size) = mViewModel.getProgress()
            if (progress == -1)
                mImageLoading.isIndeterminate = true
            else {
                mImageLoading.isIndeterminate = false
                mImageLoading.max = size
                mImageLoading.progress = progress
            }
        }
    }

    private fun processImages(isInitial: Boolean = false, isEnding: Boolean = false, message: String = "") {
        processImageLoading(isInitial, isEnding)

        val enabled = if (isInitial)
            false
        else
            isEnding

        enableContent(enabled)

        if (message.isNotEmpty())
            Toast.makeText(
                requireContext(),
                message,
                Toast.LENGTH_SHORT
            ).show()
    }

    private val mVerifyAllImagesFinished = Runnable { mViewModel.reLoadImages(PageLinkType.ALL, true) }
    private val mVerifyAllImagesFinishedDelay = Runnable { verifyAllImagesFinished() }

    private fun verifyAllImagesFinished() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (mHandler.hasCallbacks(mVerifyAllImagesFinishedDelay))
                mHandler.removeCallbacks(mVerifyAllImagesFinishedDelay)
        } else
            mHandler.removeCallbacks(mVerifyAllImagesFinishedDelay)

        if (mViewModel.imageThreadLoadingProgress() <= 0)
            mHandler.postDelayed(mVerifyAllImagesFinished, 500L)
        else
            mHandler.postDelayed(mVerifyAllImagesFinishedDelay, 1000L)
    }

    private inner class ImageLoadHandler(fragment: PagesLinkFragment) : Handler() {
        private val mOwner: WeakReference<PagesLinkFragment> = WeakReference(fragment)
        override fun handleMessage(msg: Message) {
            val imageLoad = msg.obj as PagesLinkViewModel.ImageLoad
            when (msg.what) {
                PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_IMAGE_START -> processImageLoading(true)
                PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_IMAGE_UPDATED -> {
                    processImageLoading()
                    notifyItemChanged(imageLoad.type, imageLoad.index)
                }
                PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_IMAGE_LOAD_ERROR -> mHandler.postDelayed(
                    { mViewModel.reLoadImages(imageLoad.type) },
                    500L
                )
                PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_IMAGE_LOAD_ERROR_ENABLE_MANUAL -> mForceImageReload.visibility = View.VISIBLE
                PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_IMAGE_ADDED -> notifyItemChanged(imageLoad.type, imageLoad.index, add = true)
                PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_IMAGE_REMOVED -> notifyItemChanged(
                    imageLoad.type,
                    imageLoad.index,
                    remove = true
                )
                PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_IMAGE_FINISHED -> {
                    processImageLoading(isEnding = true)
                    verifyAllImagesFinished()
                }
                PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ALL_IMAGES_LOADED -> {
                    processImageLoading(isEnding = true)
                    if (mAutoReorderPages) {
                        mAutoReorderPages = false
                        mViewModel.autoReorderDoublePages(imageLoad.type, isNotify = false)
                    }

                    mForceImageReload.visibility = if (!mViewModel.allImagesLoaded()) View.VISIBLE else View.GONE
                }
                PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_CHANGE -> notifyItemChanged(imageLoad.type, imageLoad.index)
                PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_ADD -> notifyItemChanged(imageLoad.type, imageLoad.index, add = true)
                PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_REMOVE -> notifyItemChanged(imageLoad.type, imageLoad.index, remove = true)
                PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_AUTO_PAGES_START -> processImages(isInitial = true)
                PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_AUTO_PAGES_FINISHED -> processImages(
                    isEnding = true,
                    message = getString(R.string.page_link_process_reorder_auto_done)
                )
                PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_DOUBLE_PAGES_START -> processImages(isInitial = true)
                PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_DOUBLE_PAGES_FINISHED -> processImages(
                    isEnding = true,
                    message = getString(R.string.page_link_process_reorder_dual_pages_done)
                )
                PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_SIMPLE_PAGES_START -> processImages(isInitial = true)
                PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_SIMPLE_PAGES_FINISHED -> processImages(
                    isEnding = true,
                    message = getString(R.string.page_link_process_reorder_single_page_done)
                )
                PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_SORTED_PAGES_START -> processImages(isInitial = true)
                PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_SORTED_PAGES_FINISHED -> processImages(
                    isEnding = true,
                    message = getString(R.string.page_link_process_reorder_sorted_page_done)
                )
                PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_RETURN_PAGES_START -> processImages(isInitial = true)
                PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_RETURN_PAGES_FINISHED -> processImages(
                    isEnding = true,
                    message = getString(R.string.page_link_process_reorder_sorted_page_done)
                )
                PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_GET_NOT_LINKED_START -> processImages(isInitial = true)
                PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_GET_NOT_LINKED_FINISHED -> processImages(
                    isEnding = true,
                    message = getString(R.string.page_link_process_reorder_sorted_page_done)
                )

                PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_UNDO_LAST_CHANGE_START -> processImages(isInitial = true)
                PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_UNDO_LAST_CHANGE_FINISHED -> {
                    processImages(
                        isEnding = true,
                        message = getString(R.string.page_link_process_undo_last_change_done)
                    )
                    notifyItemChanged(PageLinkType.LINKED, null)
                    notifyItemChanged(PageLinkType.NOT_LINKED, null)
                }
            }
        }
    }
}
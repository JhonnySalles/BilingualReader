package br.com.fenix.bilingualreader.view.ui.window

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Resources
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.SubTitlePage
import br.com.fenix.bilingualreader.model.entity.SubTitleText
import br.com.fenix.bilingualreader.service.controller.SubTitleController
import br.com.fenix.bilingualreader.service.japanese.Formatter
import br.com.fenix.bilingualreader.service.ocr.OcrProcess
import br.com.fenix.bilingualreader.view.components.ComponentsUtil
import com.pedromassango.doubleclick.DoubleClick
import com.pedromassango.doubleclick.DoubleClickListener
import kotlin.math.abs


class FloatingSubtitleReader constructor(
    private val context: Context,
    private val activity: AppCompatActivity
) {

    private var windowManager: WindowManager? = null
        get() {
            if (field == null) field =
                (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
            return field
        }

    private var mFloatingView: View =
        LayoutInflater.from(context).inflate(R.layout.floating_manga_subtitle_reader, null)

    private var layoutParams: WindowManager.LayoutParams

    private var lastX: Int = 0
    private var lastY: Int = 0
    private var firstX: Int = 0
    private var firstY: Int = 0

    var isShowing = false
    private var touchConsumedByMove = false

    private val mOnFlingListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (e1 != null)
                if (abs(e1.x - e2.x) > 150) {
                    if (abs(e2.x) > abs(e1.x))
                        moveWindow(false)
                    else if (abs(e2.x) < abs(e1.x))
                        moveWindow(true)

                    return false
                }

            return super.onFling(e1, e2, velocityX, velocityY)
        }
    }

    private fun moveWindow(toLeft: Boolean) {
        if (toLeft)
            layoutParams.x = 0
        else
            layoutParams.x = mRealDisplaySize.x - layoutParams.width

        windowManager?.apply {
            updateViewLayout(mFloatingView, layoutParams)
        }
    }

    private val mOnFlingDetector = GestureDetector(context, mOnFlingListener)

    private val onTouchListener = View.OnTouchListener { view, event ->
        mOnFlingDetector.onTouchEvent(event)
        onMove(view, event)
    }

    private fun onMove(view: View, event: MotionEvent): Boolean {
        val totalDeltaX = lastX - firstX
        val totalDeltaY = lastY - firstY

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.rawX.toInt()
                lastY = event.rawY.toInt()
                firstX = lastX
                firstY = lastY
            }
            MotionEvent.ACTION_UP -> {
                // If use this, call two times on function onDoubleClick
                //view.performClick()
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX.toInt() - lastX
                val deltaY = event.rawY.toInt() - lastY
                lastX = event.rawX.toInt()
                lastY = event.rawY.toInt()
                if (abs(totalDeltaX) >= 5 || abs(totalDeltaY) >= 5) {
                    if (event.pointerCount == 1) {
                        layoutParams.x += deltaX
                        layoutParams.y += deltaY
                        touchConsumedByMove = true
                        windowManager?.apply {
                            updateViewLayout(mFloatingView, layoutParams)
                        }
                    } else {
                        touchConsumedByMove = false
                    }
                } else {
                    touchConsumedByMove = false
                }
            }
            else -> {
                touchConsumedByMove = false
            }
        }
        return touchConsumedByMove
    }

    private var mResizer: View
    private var mSubtitleContent: ConstraintLayout
    private var mOcrContent: LinearLayout

    private var mSubTitleController: SubTitleController
    private var mSubtitleScrollContent: NestedScrollView
    private var mLabelChapter: String
    private var mLabelPage: String
    private var mLabelText: String
    private var mSubtitleTitle: TextView
    private var mSubtitleText: TextView
    private var mListPageVocabulary: ListView
    private var mVocabulary: Map<String, String> = mapOf()
    private var mVocabularyItem = ArrayList<String>()
    private var mOriginalHeight: Int = 0
    private var mGestureDetector: GestureDetector

    private var mOcrText: TextView
    private var mOcrKanjiDetail: TextView
    private var mOcrItem = ArrayList<String>()
    private var mOcrListText: ListView
    private var mOcrScrollContent: NestedScrollView

    private var mBtnChangeSubtitle: AppCompatImageButton
    private var mBtnChangeOcr: AppCompatImageButton
    private var mBtnExpanded: AppCompatImageButton
    private var mIconExpanded: Drawable?
    private var mIconRetracted: Drawable?

    init {
        with(mFloatingView) {
            mSubTitleController = SubTitleController.getInstance(context)
            mSubtitleScrollContent = this.findViewById(R.id.floating_manga_subtitle_scroll)
            mLabelChapter = context.getString(R.string.popup_reading_manga_subtitle_chapter)
            mLabelPage = context.getString(R.string.popup_reading_manga_subtitle_page)
            mLabelText = context.getString(R.string.popup_reading_manga_subtitle_text)

            this.findViewById<AppCompatImageButton>(R.id.floating_manga_subtitle_close)
                .setOnClickListener { dismiss() }
            this.findViewById<AppCompatImageButton>(R.id.floating_manga_subtitle_before_text)
                .setOnClickListener { mSubTitleController.getBeforeText() }
            this.findViewById<AppCompatImageButton>(R.id.floating_manga_subtitle_next_text)
                .setOnClickListener { mSubTitleController.getNextText() }
            this.findViewById<AppCompatImageButton>(R.id.floating_manga_subtitle_refresh)
                .setOnClickListener { mSubTitleController.findSubtitle() }
            this.findViewById<AppCompatImageButton>(R.id.floating_manga_subtitle_draw)
                .setOnClickListener { mSubTitleController.drawSelectedText() }
            this.findViewById<AppCompatImageButton>(R.id.floating_manga_subtitle_change_language)
                .setOnClickListener { mSubTitleController.changeLanguage() }
            this.findViewById<AppCompatImageButton>(R.id.floating_manga_subtitle_page_linked)
                .setOnClickListener { mSubTitleController.drawPageLinked() }

            mBtnChangeSubtitle = this.findViewById(R.id.floating_manga_subtitle_change_subtitle)
            mBtnChangeOcr = this.findViewById(R.id.floating_manga_subtitle_change_ocr)

            mBtnChangeSubtitle.setOnClickListener { changeLayout() }
            mBtnChangeOcr.setOnClickListener { changeLayout(false) }

            this.findViewById<AppCompatImageButton>(R.id.floating_manga_subtitle_go_to_top)
                .setOnClickListener {
                    mSubtitleScrollContent.smoothScrollTo(0, 0)
                }

            mResizer = this.findViewById(R.id.floating_manga_subtitle_resizer)
            setResizer()

            mIconExpanded = AppCompatResources.getDrawable(context, R.drawable.ico_expanded)
            mIconRetracted =
                AppCompatResources.getDrawable(context, R.drawable.ico_floating_subtitle_retracted)

            mBtnExpanded = this.findViewById(R.id.floating_manga_subtitle_expanded)
            mBtnExpanded.setOnClickListener { expanded() }

            mSubtitleContent = this.findViewById(R.id.floating_manga_subtitle_subtitle_content)
            mSubtitleTitle = this.findViewById(R.id.floating_manga_subtitle_title)
            mSubtitleText = this.findViewById(R.id.floating_manga_subtitle_subtitle)
            mListPageVocabulary =
                this.findViewById(R.id.floating_manga_subtitle_list_page_vocabulary)
            mListPageVocabulary.adapter =
                ArrayAdapter(context, R.layout.floating_manga_subtitle_list_item, mVocabularyItem)
            mListPageVocabulary.choiceMode = ListView.CHOICE_MODE_SINGLE
            mListPageVocabulary.isLongClickable = true
            mListPageVocabulary.onItemLongClickListener =
                OnItemLongClickListener { _, _, index, _ ->
                    if (index >= 0 && mVocabularyItem.size > index)
                        makeCopyText(mVocabularyItem[index])
                    true
                }

            mSubtitleText.setOnClickListener(
                DoubleClick(object : DoubleClickListener {
                    override fun onSingleClick(view: View?) {}
                    override fun onDoubleClick(view: View?) {
                        mSubTitleController.getNextText()
                    }
                }, 500)
            )

            mSubtitleText.setOnLongClickListener {
                val text = mSubTitleController.subTitleTextSelected.value?.text ?: ""
                makeCopyText(text)
                true
            }

            mSubtitleText.movementMethod = LinkMovementMethod.getInstance()

            mOcrContent = this.findViewById(R.id.floating_manga_subtitle_ocr_content)
            mOcrText = this.findViewById(R.id.floating_manga_subtitle_ocr_text)
            mOcrText.movementMethod = LinkMovementMethod.getInstance()
            mOcrScrollContent = this.findViewById(R.id.floating_manga_subtitle_ocr_scroll)
            mOcrKanjiDetail = this.findViewById(R.id.floating_manga_subtitle_ocr_kanji_detail)

            mOcrText.setOnLongClickListener {
                makeCopyText(mOcrText.text)
                true
            }

            mOcrListText = this.findViewById(R.id.floating_manga_subtitle_ocr_list)
            mOcrListText.adapter =
                ArrayAdapter(context, R.layout.floating_manga_subtitle_list_item, mOcrItem)
            mOcrListText.choiceMode = ListView.CHOICE_MODE_SINGLE
            mOcrListText.isLongClickable = true
            mOcrListText.onItemClickListener = AdapterView.OnItemClickListener { _, _, index, _ ->
                if (index >= 0 && mOcrItem.size > index)
                    updateTextOcr(mOcrItem[index])
            }

            mOcrListText.onItemLongClickListener = OnItemLongClickListener { _, _, index, _ ->
                if (index >= 0 && mOcrItem.size > index)
                    makeCopyText(mOcrItem[index])
                true
            }

            this.findViewById<AppCompatImageButton>(R.id.floating_manga_subtitle_ocr_clear_list)
                .setOnClickListener {
                    (activity as OcrProcess).clearList()
                    mOcrText.text = ""
                    mOcrKanjiDetail.text = ""
                    mOcrKanjiDetail.visibility = View.GONE
                }

            this.findViewById<AppCompatImageButton>(R.id.floating_manga_google_translate)
                .setOnClickListener {
                    if (mSubtitleText.text.toString().isNotEmpty()) {
                        val list = mSubtitleText.text.split(" ").filter { it.isNotEmpty() }.toSet()
                        val text = list.joinToString("\n")

                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Copied Text", text)
                        clipboard.setPrimaryClip(clip)

                        Toast.makeText(
                            context,
                            context.getString(R.string.action_copy, text),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        mGestureDetector = GestureDetector(context, ChangeTextTouchListener())
        mSubtitleTitle.setOnTouchListener { view, motionEvent ->
            mGestureDetector.onTouchEvent(motionEvent)
            onMove(view, motionEvent)
        }
        mSubtitleText.setOnTouchListener { view, motionEvent ->
            mGestureDetector.onTouchEvent(motionEvent)
            onMove(view, motionEvent)
        }
        mListPageVocabulary.setOnTouchListener { _, motionEvent ->
            mGestureDetector.onTouchEvent(
                motionEvent
            )
        }
        mFloatingView.setOnTouchListener(onTouchListener)

        val layoutType = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_PHONE
        else
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        layoutParams = WindowManager.LayoutParams().apply {
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            type = layoutType
            gravity = Gravity.TOP or Gravity.LEFT
            width =
                context.resources.getDimension(R.dimen.floating_manga_subtitle_reader_width).toInt()
            height = context.resources.getDimension(R.dimen.floating_manga_subtitle_reader_height)
                .toInt()
            x = (mRealDisplaySize.x / 2) - (width / 2)
            y = context.resources.getDimension(R.dimen.floating_reader_initial_top_padding).toInt()
        }

        mOriginalHeight = mFloatingView.height
    }

    inner class ChangeTextTouchListener : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (e1 != null)
                if (abs(e1.x - e2.x) > 200)
                    if (abs(e2.x) > abs(e1.x))
                        mSubTitleController.getBeforeText()
                    else if (abs(e2.x) < abs(e1.x))
                        mSubTitleController.getNextText()


            return super.onFling(e1, e2, velocityX, velocityY)
        }
    }

    private lateinit var mRealDisplaySize: Point
    private var minSize = 0
    private fun setResizer() {
        minSize = context.resources.getDimension(R.dimen.floating_reader_min_size).toInt()
        val metrics = Resources.getSystem().displayMetrics
        mRealDisplaySize = Point(metrics.widthPixels, metrics.heightPixels)

        var dx = 0
        var dy = 0
        var updateTimer = System.currentTimeMillis()

        mResizer.setOnTouchListener { _, me ->
            when (me.action) {
                MotionEvent.ACTION_DOWN -> {
                    dx = layoutParams.width - me.rawX.toInt()
                    dy = layoutParams.height - me.rawY.toInt()
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_UP -> {
                    fixBoxBounds()
                    windowManager?.updateViewLayout(mFloatingView, mFloatingView.layoutParams)
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams.width = dx + me.rawX.toInt()
                    layoutParams.height = dy + me.rawY.toInt()

                    fixBoxBounds()
                    val currTime = System.currentTimeMillis()
                    if (currTime - updateTimer > 50) {
                        updateTimer = currTime
                        windowManager?.updateViewLayout(mFloatingView, mFloatingView.layoutParams)
                    }
                    return@setOnTouchListener true
                }
                else -> return@setOnTouchListener true
            }
        }
    }

    private fun fixBoxBounds() {
        with(layoutParams) {
            if (x < 0)
                x = 0
            else if (x + width > mRealDisplaySize.x)
                x = mRealDisplaySize.x - width

            if (y < 0)
                y = 0
            else if (y + height > mRealDisplaySize.y)
                y = mRealDisplaySize.y - height

            if (width > mRealDisplaySize.x)
                width = mRealDisplaySize.x

            if (height > mRealDisplaySize.y)
                height = mRealDisplaySize.y

            if (isExpanded) {
                if (width < minSize)
                    width = minSize
            } else {
                if (width < mMinimisedSize)
                    width = mMinimisedSize
            }

            if (height < minSize)
                height = minSize
        }

    }

    private fun makeCopyText(text: CharSequence) {
        if (text.isNotEmpty()) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied Text", text)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(
                context,
                context.getString(R.string.action_copy, text),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun updatePage(subTitlePage: SubTitlePage?) {
        if (subTitlePage != null) {
            if (subTitlePage.vocabulary != null && subTitlePage.vocabulary.isNotEmpty()) {
                mVocabulary =
                    subTitlePage.vocabulary.map { vocab -> vocab.word to vocab.word + " - " + vocab.portuguese + if (!vocab.revised) "¹" else "" }
                        .toMap()
                mVocabularyItem.clear()
                mVocabularyItem.addAll(subTitlePage.vocabulary.map { vocab -> vocab.word + " - " + vocab.portuguese + if (!vocab.revised) "¹" else "" })
                mListPageVocabulary.visibility = View.VISIBLE
            } else {
                mVocabularyItem.clear()
                mListPageVocabulary.visibility = View.GONE
            }
            (mListPageVocabulary.adapter as ArrayAdapter<*>).notifyDataSetChanged()
        }
    }

    private fun findVocabulary(vocabulary: String) {
        mListPageVocabulary.clearChoices()
        if (mVocabularyItem.isNotEmpty()) {
            var index = -1

            if (mVocabulary.containsKey(vocabulary))
                index = mVocabularyItem.indexOf(mVocabulary[vocabulary])
            else {
                for (word in mVocabulary.keys)
                    if (vocabulary in word) {
                        index = mVocabularyItem.indexOf(mVocabulary[word])
                        break
                    }

                if (index < 0)
                    return
            }

            mSubtitleScrollContent.smoothScrollTo(0, mListPageVocabulary.top)
            mListPageVocabulary.smoothScrollToPosition(index)
            mListPageVocabulary.requestFocusFromTouch()
            mListPageVocabulary.setSelection(index)
        }
    }

    private var mMinimisedSize =
        context.resources.getDimension(R.dimen.floating_reader_button_close).toInt()
    private var isExpanded = true
    fun expanded(expand: Boolean = false) {
        if (expand && isExpanded)
            return

        if (expand || !isExpanded) {
            if (mOriginalHeight < minSize)
                mOriginalHeight = minSize

            val params = mFloatingView.layoutParams as WindowManager.LayoutParams
            params.height = mOriginalHeight
            mFloatingView.layoutParams = params
            mBtnExpanded.setImageDrawable(mIconRetracted)
            isExpanded = true
        } else {
            mOriginalHeight = mFloatingView.height
            val params = mFloatingView.layoutParams as WindowManager.LayoutParams
            params.height = mMinimisedSize
            mFloatingView.layoutParams = params
            mBtnExpanded.setImageDrawable(mIconExpanded)
            isExpanded = false
        }

        windowManager?.apply {
            updateViewLayout(mFloatingView, mFloatingView.layoutParams)
        }
    }

    fun updateText(subTitleText: SubTitleText?) {
        var title = ""
        mSubtitleText.text = ""
        mSubtitleScrollContent.smoothScrollTo(0, 0)
        if (subTitleText != null) {
            val index =
                mSubTitleController.subTitlePageSelected.value?.subTitleTexts?.indexOf(mSubTitleController.subTitleTextSelected.value)
                    ?.plus(1)

            title =
                "$mLabelChapter ${mSubTitleController.subTitleChapterSelected.value?.chapter.toString()} - " +
                        "$mLabelPage ${mSubTitleController.subTitlePageSelected.value!!.number} - " +
                        "$mLabelText $index/${mSubTitleController.subTitlePageSelected.value?.subTitleTexts?.size}"

            Formatter.generateFurigana(
                subTitleText.text,
                furigana = { mSubtitleText.text = it },
                vocabularyClick = { findVocabulary(it) })
        } else if (mSubTitleController.subTitleChapterSelected.value != null && mSubTitleController.subTitlePageSelected.value != null) {
            title =
                "$mLabelChapter ${mSubTitleController.subTitleChapterSelected.value?.chapter.toString()} - " +
                        "$mLabelPage ${mSubTitleController.subTitlePageSelected.value!!.number} - "
            "$mLabelText 0/${if (mSubTitleController.subTitlePageSelected.value?.subTitleTexts == null) 0 else mSubTitleController.subTitlePageSelected.value?.subTitleTexts?.size}"
        }

        mSubtitleTitle.text = title

        changeLayout()
    }

    private fun setKanjiDetail(kanji: SpannableString, detail: SpannableString) {
        if (detail.isEmpty()) {
            mOcrKanjiDetail.text = ""
            mOcrKanjiDetail.visibility = View.GONE
        } else {
            mOcrKanjiDetail.text = String().plus(kanji).plus(": ").plus(detail)
            if (mOcrKanjiDetail.visibility == View.GONE) {
                mOcrKanjiDetail.visibility = View.VISIBLE
                mOcrScrollContent.smoothScrollTo(0, mOcrListText.top)
            } else
                mOcrScrollContent.smoothScrollTo(0, mOcrKanjiDetail.top)
        }
    }

    fun updateOcrList(texts: ArrayList<String>) {
        mOcrItem.clear()
        mOcrItem.addAll(texts)
        (mOcrListText.adapter as ArrayAdapter<*>).notifyDataSetChanged()
    }

    fun updateTextOcr(text: String?) {
        if (text != null) {
            changeLayout(false)
            Formatter.generateKanjiColor(text,
                { kanji -> mOcrText.text = kanji },
                { kanji, detail -> setKanjiDetail(kanji, detail) })
            mOcrScrollContent.smoothScrollTo(0, 0)
        } else
            mOcrText.text = ""
    }

    fun changeLayout(isSubtitle: Boolean = true) {
        if (isSubtitle) {
            mSubtitleContent.visibility = View.VISIBLE
            mOcrContent.visibility = View.GONE

            mBtnChangeSubtitle.setBackgroundColor(context.getColor(R.color.floating_button_subtitle_background_selected))
            mBtnChangeOcr.setBackgroundColor(context.getColor(R.color.floating_button_subtitle_background))
        } else {
            mSubtitleContent.visibility = View.GONE
            mOcrContent.visibility = View.VISIBLE

            mBtnChangeSubtitle.setBackgroundColor(context.getColor(R.color.floating_button_subtitle_background))
            mBtnChangeOcr.setBackgroundColor(context.getColor(R.color.floating_button_subtitle_background_selected))
        }
    }

    fun show() {
        if (ComponentsUtil.canDrawOverlays(context)) {
            dismiss()
            isShowing = true
            windowManager?.addView(mFloatingView, layoutParams)
        }
    }

    fun showWithoutDismiss() {
        if (!isShowing)
            show()
    }

    fun forceZIndex() {
        if (isShowing)
            show()
    }

    fun dismiss() {
        if (isShowing) {
            windowManager?.removeView(mFloatingView)
            isShowing = false
        }
    }

    fun destroy() {
        dismiss()
    }

}
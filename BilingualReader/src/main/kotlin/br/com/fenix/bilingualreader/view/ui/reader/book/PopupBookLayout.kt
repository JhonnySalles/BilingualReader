package br.com.fenix.bilingualreader.view.ui.reader.book

import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.AlignmentLayoutType
import br.com.fenix.bilingualreader.model.enums.BookLayoutType
import br.com.fenix.bilingualreader.model.enums.MarginLayoutType
import br.com.fenix.bilingualreader.model.enums.PaginationType
import br.com.fenix.bilingualreader.model.enums.ScrollingType
import br.com.fenix.bilingualreader.model.enums.SpacingLayoutType
import br.com.fenix.bilingualreader.service.listener.PopupLayoutListener
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import org.slf4j.LoggerFactory


class PopupBookLayout : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(PopupBookLayout::class.java)

    private val mViewModel: BookReaderViewModel by activityViewModels()

    private lateinit var mScrollingMode: TextInputLayout
    private lateinit var mScrollingModeAutoComplete: MaterialAutoCompleteTextView
    private lateinit var mPaginationType: TextInputLayout
    private lateinit var mPaginationTypeAutoComplete: MaterialAutoCompleteTextView

    private lateinit var mMarginSmall: MaterialButton
    private lateinit var mMarginMedium: MaterialButton
    private lateinit var mMarginBig: MaterialButton
    private lateinit var mSpacingSmall: MaterialButton
    private lateinit var mSpacingMedium: MaterialButton
    private lateinit var mSpacingBig: MaterialButton
    private lateinit var mAlignmentJustify: MaterialButton
    private lateinit var mAlignmentLeft: MaterialButton
    private lateinit var mAlignmentCenter: MaterialButton
    private lateinit var mAlignmentRight: MaterialButton
    private lateinit var mReadingTouchFunction: MaterialButton

    private lateinit var mBookMapScrollingMode: HashMap<String, ScrollingType>
    private lateinit var mBookMapPaginationType: HashMap<String, PaginationType>

    private var mListener : PopupLayoutListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.popup_book_layout, container, false)

        mScrollingMode = root.findViewById(R.id.popup_book_layout_scrolling_mode)
        mScrollingModeAutoComplete = root.findViewById(R.id.popup_book_layout_scrolling_mode_menu_autocomplete)

        mPaginationType = root.findViewById(R.id.popup_book_layout_pagination_type)
        mPaginationTypeAutoComplete = root.findViewById(R.id.popup_book_layout_pagination_type_menu_autocomplete)

        mMarginSmall = root.findViewById(R.id.popup_book_layout_margin_small)
        mMarginMedium = root.findViewById(R.id.popup_book_layout_margin_medium)
        mMarginBig = root.findViewById(R.id.popup_book_layout_margin_big)
        mSpacingSmall = root.findViewById(R.id.popup_book_layout_spacing_small)
        mSpacingMedium = root.findViewById(R.id.popup_book_layout_spacing_medium)
        mSpacingBig = root.findViewById(R.id.popup_book_layout_spacing_big)
        mAlignmentJustify = root.findViewById(R.id.popup_book_layout_alignment_justify)
        mAlignmentCenter = root.findViewById(R.id.popup_book_layout_alignment_center)
        mAlignmentLeft = root.findViewById(R.id.popup_book_layout_alignment_left)
        mAlignmentRight = root.findViewById(R.id.popup_book_layout_alignment_right)
        mReadingTouchFunction = root.findViewById(R.id.popup_book_layout_reading_touch_screen)

        mBookMapScrollingMode = hashMapOf(
            getString(R.string.config_book_scrolling_infinity_scrolling) to ScrollingType.Scrolling,
            getString(R.string.config_book_scrolling_pagination) to ScrollingType.Pagination,
            getString(R.string.config_book_scrolling_pagination_vertical) to ScrollingType.PaginationVertical,
            getString(R.string.config_book_scrolling_pagination_right_to_left) to ScrollingType.PaginationRightToLeft
        )

        mBookMapPaginationType = hashMapOf(
            getString(R.string.config_book_pagination_default) to PaginationType.Default,
            getString(R.string.config_book_pagination_page_curl) to PaginationType.CurlPage,
            getString(R.string.config_book_pagination_page_stack) to PaginationType.Stack,
            getString(R.string.config_book_pagination_page_zoom) to PaginationType.Zooming,
            getString(R.string.config_book_pagination_page_fade) to PaginationType.Fade,
            getString(R.string.config_book_pagination_page_depth) to PaginationType.Depth
        )

        mScrollingModeAutoComplete.setAdapter(ArrayAdapter(requireContext(), R.layout.list_item, mBookMapScrollingMode.keys.sorted().toTypedArray()))
        mScrollingModeAutoComplete.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
                val scrolling = if (parent.getItemAtPosition(position).toString().isNotEmpty() && mBookMapScrollingMode.containsKey(parent.getItemAtPosition(position).toString()))
                    mBookMapScrollingMode[parent.getItemAtPosition(position).toString()]!!
                else
                    ScrollingType.Pagination

                mViewModel.changeScrolling(scrolling)
            }

        mPaginationTypeAutoComplete.setAdapter(ArrayAdapter(requireContext(), R.layout.list_item, mBookMapPaginationType.keys.sorted().toTypedArray()))
        mPaginationTypeAutoComplete.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
                val pagination = if (parent.getItemAtPosition(position).toString().isNotEmpty() && mBookMapPaginationType.containsKey(parent.getItemAtPosition(position).toString()))
                    mBookMapPaginationType[parent.getItemAtPosition(position).toString()]!!
                else
                    PaginationType.Default

                mViewModel.changePagination(pagination)
            }


        mMarginSmall.setOnClickListener {
            (mMarginSmall.icon as AnimatedVectorDrawable).start()
            mViewModel.setSelectMargin(MarginLayoutType.Small)
        }

        mMarginMedium.setOnClickListener {
            (mMarginMedium.icon as AnimatedVectorDrawable).start()
            mViewModel.setSelectMargin(MarginLayoutType.Medium)
        }

        mMarginBig.setOnClickListener {
            (mMarginBig.icon as AnimatedVectorDrawable).start()
            mViewModel.setSelectMargin(MarginLayoutType.Big)
        }


        mSpacingSmall.setOnClickListener {
            (mSpacingSmall.icon as AnimatedVectorDrawable).start()
            mViewModel.setSelectSpacing(SpacingLayoutType.Small)
        }

        mSpacingMedium.setOnClickListener {
            (mSpacingMedium.icon as AnimatedVectorDrawable).start()
            mViewModel.setSelectSpacing(SpacingLayoutType.Medium)
        }

        mSpacingBig.setOnClickListener {
            (mSpacingBig.icon as AnimatedVectorDrawable).start()
            mViewModel.setSelectSpacing(SpacingLayoutType.Big)
        }


        mAlignmentJustify.setOnClickListener {
            (mAlignmentJustify.icon as AnimatedVectorDrawable).start()
            mViewModel.setSelectAlignment(AlignmentLayoutType.Justify)
        }

        mAlignmentCenter.setOnClickListener {
            (mAlignmentCenter.icon as AnimatedVectorDrawable).start()
            mViewModel.setSelectAlignment(AlignmentLayoutType.Center)
        }

        mAlignmentLeft.setOnClickListener {
            (mAlignmentLeft.icon as AnimatedVectorDrawable).start()
            mViewModel.setSelectAlignment(AlignmentLayoutType.Left)
        }

        mAlignmentRight.setOnClickListener {
            (mAlignmentRight.icon as AnimatedVectorDrawable).start()
            mViewModel.setSelectAlignment(AlignmentLayoutType.Right)
        }

        setButtonMarked(getAlignmentsButton(), getSelected(mViewModel.alignmentType.value ?: AlignmentLayoutType.Justify))
        setButtonMarked(getMarginsButton(), getSelected(mViewModel.marginType.value ?: MarginLayoutType.Small))
        setButtonMarked(getSpacingsButton(), getSelected(mViewModel.spacingType.value ?: SpacingLayoutType.Small))

        mScrollingModeAutoComplete.setText(mBookMapScrollingMode.entries.first { it.value == mViewModel.scrollingMode.value }.key, false)

        mReadingTouchFunction.setOnClickListener {
            mListener?.openTouchFunctions()
        }

        mReadingTouchFunction.setOnLongClickListener {
            mListener?.configTouchFunctions()
            true
        }

        observer()
        return root
    }

    fun setListener(listener: PopupLayoutListener) {
        mListener = listener
    }

    fun clearListener() {
        mListener = null
    }

    private fun getAlignmentsButton() = arrayListOf(mAlignmentJustify, mAlignmentCenter, mAlignmentLeft, mAlignmentRight)

    private fun getSpacingsButton() = arrayListOf(mSpacingSmall, mSpacingMedium, mSpacingBig)
    private fun getMarginsButton() = arrayListOf(mMarginSmall, mMarginMedium, mMarginBig)

    private fun setButtonMarked(buttons: ArrayList<MaterialButton>, marked: MaterialButton?) {
        if (marked == null)
            return

        for (button in buttons)
            button.setStrokeWidthResource(if (button != marked) R.dimen.popup_reader_button_border_width else R.dimen.popup_reader_button_selected_border_width)
    }

    private fun getSelected(type: BookLayoutType): MaterialButton? {
        return when (type) {
            is AlignmentLayoutType -> {
                when (type) {
                    AlignmentLayoutType.Justify -> mAlignmentJustify
                    AlignmentLayoutType.Center -> mAlignmentCenter
                    AlignmentLayoutType.Right -> mAlignmentRight
                    else -> mAlignmentLeft
                }
            }
            is MarginLayoutType -> {
                when (type) {
                    MarginLayoutType.Medium -> mMarginMedium
                    MarginLayoutType.Big -> mMarginBig
                    else -> mMarginSmall
                }
            }
            is SpacingLayoutType -> {
                when (type) {
                    SpacingLayoutType.Medium -> mSpacingMedium
                    SpacingLayoutType.Big -> mSpacingBig
                    else -> mSpacingSmall
                }
            }
            else -> null
        }
    }

    private fun observer() {
        mViewModel.scrollingMode.observe(viewLifecycleOwner) {
            mScrollingModeAutoComplete.setText(mBookMapScrollingMode.entries.first { s -> s.value == it }.key, false)
        }

        mViewModel.paginationType.observe(viewLifecycleOwner) {
            mPaginationTypeAutoComplete.setText(mBookMapPaginationType.entries.first { s -> s.value == it }.key, false)
        }

        mViewModel.alignmentType.observe(viewLifecycleOwner) {
            setButtonMarked(getAlignmentsButton(), getSelected(it))
        }

        mViewModel.marginType.observe(viewLifecycleOwner) {
            setButtonMarked(getMarginsButton(), getSelected(it))
        }

        mViewModel.spacingType.observe(viewLifecycleOwner) {
            setButtonMarked(getSpacingsButton(), getSelected(it))
        }
    }

}
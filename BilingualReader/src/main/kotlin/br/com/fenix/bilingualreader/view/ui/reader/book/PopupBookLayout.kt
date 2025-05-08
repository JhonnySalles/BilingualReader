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
import br.com.fenix.bilingualreader.model.enums.ScrollingType
import br.com.fenix.bilingualreader.model.enums.SpacingLayoutType
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import org.slf4j.LoggerFactory


class PopupBookLayout : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(PopupBookLayout::class.java)

    private val mViewModel: BookReaderViewModel by activityViewModels()

    private lateinit var mScrollingType: TextInputLayout
    private lateinit var mScrollingTypeAutoComplete: MaterialAutoCompleteTextView

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

    private lateinit var mBookMapScrollingMode: HashMap<String, ScrollingType>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.popup_book_layout, container, false)

        mScrollingType = root.findViewById(R.id.popup_book_layout_scrolling_type)
        mScrollingTypeAutoComplete = root.findViewById(R.id.popup_book_layout_scrolling_type_menu_autocomplete)

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

        mBookMapScrollingMode = hashMapOf(
            getString(R.string.config_book_scrolling_infinity_scrolling) to ScrollingType.Scrolling,
            getString(R.string.config_book_scrolling_pagination) to ScrollingType.Pagination,
            getString(R.string.config_book_scrolling_pagination_vertical) to ScrollingType.PaginationVertical,
            getString(R.string.config_book_scrolling_pagination_right_to_left) to ScrollingType.PaginationRightToLeft
        )

        val adapterBookScrollingMode = ArrayAdapter(requireContext(), R.layout.list_item, mBookMapScrollingMode.keys.sorted().toTypedArray())
        mScrollingTypeAutoComplete.setAdapter(adapterBookScrollingMode)
        mScrollingTypeAutoComplete.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
                val scrolling = if (parent.getItemAtPosition(position).toString().isNotEmpty() &&
                    mBookMapScrollingMode.containsKey(parent.getItemAtPosition(position).toString())
                )
                    mBookMapScrollingMode[parent.getItemAtPosition(position).toString()]!!
                else
                    ScrollingType.Pagination

                mViewModel.changeScrolling(scrolling)
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

        mScrollingTypeAutoComplete.setText(mBookMapScrollingMode.entries.first { it.value == mViewModel.scrollingType.value }.key, false)

        observer()
        return root
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
        mViewModel.scrollingType.observe(viewLifecycleOwner) {
            mScrollingTypeAutoComplete.setText(mBookMapScrollingMode.entries.first { s -> s.value == it }.key, false)
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
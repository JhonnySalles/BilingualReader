package br.com.fenix.bilingualreader.view.ui.reader.book

import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.AlignmentType
import br.com.fenix.bilingualreader.model.enums.BookType
import br.com.fenix.bilingualreader.model.enums.MarginType
import br.com.fenix.bilingualreader.model.enums.SpacingType
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import org.slf4j.LoggerFactory


class PopupBookLayout : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(PopupBookLayout::class.java)

    private val mViewModel: BookReaderViewModel by activityViewModels()

    private lateinit var mScrollingType: TextInputLayout
    private lateinit var mScrollingTypeAutoComplete: AutoCompleteTextView

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.popup_book_layout, container, false)

        mScrollingType = root.findViewById(R.id.popup_book_layout_scrolling_type)
        mScrollingTypeAutoComplete =
            root.findViewById(R.id.popup_book_layout_scrolling_type_menu_autocomplete)

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


        mMarginSmall.setOnClickListener {
            (mMarginSmall.icon as AnimatedVectorDrawable).start()
            mViewModel.setSelectMargin(MarginType.Small)
        }
        mMarginMedium.setOnClickListener {
            (mMarginMedium.icon as AnimatedVectorDrawable).start()
            mViewModel.setSelectMargin(MarginType.Medium)
        }
        mMarginBig.setOnClickListener {
            (mMarginBig.icon as AnimatedVectorDrawable).start()
            mViewModel.setSelectMargin(MarginType.Big)
        }

        mSpacingSmall.setOnClickListener {
            (mSpacingSmall.icon as AnimatedVectorDrawable).start()
            mViewModel.setSelectSpacing(SpacingType.Small)
        }
        mSpacingMedium.setOnClickListener {
            (mSpacingMedium.icon as AnimatedVectorDrawable).start()
            mViewModel.setSelectSpacing(SpacingType.Medium)
        }
        mSpacingBig.setOnClickListener {
            (mSpacingBig.icon as AnimatedVectorDrawable).start()
            mViewModel.setSelectSpacing(SpacingType.Big)
        }

        mAlignmentJustify.setOnClickListener {
            (mAlignmentJustify.icon as AnimatedVectorDrawable).start()
            mViewModel.setSelectAlignment(AlignmentType.Justify)
        }
        mAlignmentCenter.setOnClickListener {
            (mAlignmentCenter.icon as AnimatedVectorDrawable).start()
            mViewModel.setSelectAlignment(AlignmentType.Center)
        }
        mAlignmentLeft.setOnClickListener {
            (mAlignmentLeft.icon as AnimatedVectorDrawable).start()
            mViewModel.setSelectAlignment(AlignmentType.Left)
        }
        mAlignmentRight.setOnClickListener {
            (mAlignmentRight.icon as AnimatedVectorDrawable).start()
            mViewModel.setSelectAlignment(AlignmentType.Right)
        }

        setButtonMarked(getAlignmentsButton(), getSelected(mViewModel.alignmentType.value ?: AlignmentType.Justify))
        setButtonMarked(getMarginsButton(), getSelected(mViewModel.marginType.value ?: MarginType.Small))
        setButtonMarked(getSpacingsButton(), getSelected(mViewModel.spacingType.value ?: SpacingType.Small))

        observer()
        return root
    }

    private fun getAlignmentsButton() =
        arrayListOf(mAlignmentJustify, mAlignmentCenter, mAlignmentLeft, mAlignmentRight)

    private fun getSpacingsButton() = arrayListOf(mSpacingSmall, mSpacingMedium, mSpacingBig)
    private fun getMarginsButton() = arrayListOf(mMarginSmall, mMarginMedium, mMarginBig)

    private fun setButtonMarked(buttons: ArrayList<MaterialButton>, marked: MaterialButton?) {
        if (marked == null)
            return

        for (button in buttons)
            button.setStrokeWidthResource(if (button != marked) R.dimen.popup_reader_button_border_width else R.dimen.popup_reader_button_selected_border_width)
    }

    private fun getSelected(type: BookType): MaterialButton? {
        return when (type) {
            is AlignmentType -> {
                when (type) {
                    AlignmentType.Justify -> mAlignmentJustify
                    AlignmentType.Center -> mAlignmentCenter
                    AlignmentType.Right -> mAlignmentRight
                    else -> mAlignmentLeft
                }
            }
            is MarginType -> {
                when (type) {
                    MarginType.Medium -> mMarginMedium
                    MarginType.Big -> mMarginBig
                    else -> mMarginSmall
                }
            }
            is SpacingType -> {
                when (type) {
                    SpacingType.Medium -> mSpacingMedium
                    SpacingType.Big -> mSpacingBig
                    else -> mSpacingSmall
                }
            }
            else -> null
        }
    }

    private fun observer() {
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
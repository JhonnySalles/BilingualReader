package br.com.fenix.bilingualreader.view.ui.reader.book

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.view.ui.reader.manga.MangaReaderViewModel
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
    private lateinit var mAlignmentComplete: MaterialButton
    private lateinit var mAlignmentLeft: MaterialButton
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
        mAlignmentComplete = root.findViewById(R.id.popup_book_layout_alignment_complete)
        mAlignmentLeft = root.findViewById(R.id.popup_book_layout_alignment_right)
        mAlignmentRight = root.findViewById(R.id.popup_book_layout_alignment_left)


        observer()
        return root
    }

    private fun observer() {

    }

}
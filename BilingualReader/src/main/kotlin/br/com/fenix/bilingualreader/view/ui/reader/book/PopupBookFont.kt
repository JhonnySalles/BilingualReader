package br.com.fenix.bilingualreader.view.ui.reader.book

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.FontType
import br.com.fenix.bilingualreader.service.listener.FontsListener
import br.com.fenix.bilingualreader.view.adapter.fonts.FontsCardAdapter
import com.google.android.material.slider.Slider
import org.lucasr.twowayview.TwoWayView
import org.slf4j.LoggerFactory


class PopupBookFont : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(PopupBookFont::class.java)

    private val mViewModel: BookReaderViewModel by activityViewModels()

    private lateinit var mFontType: TwoWayView
    private lateinit var mFontSize: Slider

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.popup_book_font, container, false)

        mFontType = root.findViewById(R.id.popup_book_font_type)
        mFontSize = root.findViewById(R.id.popup_book_font_size)

        mFontSize.value = mViewModel.fontSize.value!!
        mFontSize.addOnChangeListener { _, value, fromUser ->
            if (fromUser)
                mViewModel.changeFontSize(value)
        }

        prepareFonts()
        observer()
        return root
    }

    private fun observer() {
        mViewModel.fontSize.observe(viewLifecycleOwner) {
            mFontSize.value = if (it < mFontSize.valueFrom)
                mFontSize.valueFrom
            else if (it > mFontSize.valueTo)
                mFontSize.valueTo
            else
                it.toFloat()
        }

        mViewModel.fontType.observe(viewLifecycleOwner) {
            (mFontType.adapter as FontsCardAdapter).notifyDataSetChanged()
            mFontType.scrollBy(mViewModel.getSelectedFontTypeIndex())
        }
    }

    private fun prepareFonts() {
        val listener = object : FontsListener {
            override fun onClick(font: Pair<FontType, Boolean>) {
                mViewModel.setSelectFont(font.first)
            }
        }

        mViewModel.loadFonts()
        val lineAdapter = FontsCardAdapter(requireContext(), mViewModel.fonts.value!!, listener)
        mFontType.adapter = lineAdapter
        mFontType.scrollBy(mViewModel.getSelectedFontTypeIndex())

        mViewModel.fonts.observe(viewLifecycleOwner) {
            lineAdapter.updateList(it)
        }
    }
}
package br.com.fenix.bilingualreader.view.ui.annotation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.view.ui.book.BookAnnotationViewModel
import org.slf4j.LoggerFactory


class AnnotationPopupFilterChapter : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(AnnotationPopupFilterChapter::class.java)

    private lateinit var mViewModel: BookAnnotationViewModel

    private lateinit var mChapters: ListView
    private lateinit var mListener: AdapterView.OnItemClickListener
    private var mIsManual = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = ViewModelProvider(requireActivity())[BookAnnotationViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.popup_annotation_filter_chapter, container, false)

        mChapters = root.findViewById(R.id.popup_annotation_filter_chapter_list)
        mChapters.adapter = ArrayAdapter(requireContext(), R.layout.list_item_multiple_choice, mViewModel.chapters.value!!.keys.toList())
        mListener = AdapterView.OnItemClickListener { _, _, index, _ ->
            try {
                mIsManual = true
                mViewModel.filterChapter(mChapters.getItemAtPosition(index) as String, !mChapters.isItemChecked(index))
            } finally {
                mIsManual = false
            }
        }
        mChapters.onItemClickListener = mListener

        return root
    }

    private fun observer() {
        mViewModel.chapters.observe(viewLifecycleOwner) {
            val test = mViewModel.chapters.value!!.keys
            (mChapters.adapter as ArrayAdapter<*>).clear()
            (mChapters.adapter as ArrayAdapter<String>).addAll(it.keys)
            (mChapters.adapter as ArrayAdapter<*>).notifyDataSetChanged()
        }


        mViewModel.chapterFilter.observe(viewLifecycleOwner) {
            if (!mIsManual) {
                try {
                    mChapters.onItemClickListener = null
                    for (i in 0 until mChapters.count)
                        mChapters.setItemChecked(i, it.any { c -> c.key == mChapters.getItemAtPosition(i) })
                } finally {
                    mChapters.onItemClickListener = mListener
                }
            }
        }
    }

}
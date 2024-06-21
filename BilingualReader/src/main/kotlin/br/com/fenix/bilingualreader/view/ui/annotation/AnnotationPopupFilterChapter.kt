package br.com.fenix.bilingualreader.view.ui.annotation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.Fragment
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.service.listener.AnnotationListener
import org.slf4j.LoggerFactory


class AnnotationPopupFilterChapter : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(AnnotationPopupFilterChapter::class.java)

    private lateinit var mChapters: ListView
    private lateinit var mOnItemClickListener: AdapterView.OnItemClickListener
    private var mIsManual = false
    private var mListener: AnnotationListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.popup_annotation_filter_chapter, container, false)

        mChapters = root.findViewById(R.id.popup_annotation_filter_chapter_list)
        mChapters.adapter = ArrayAdapter(requireContext(), R.layout.list_item_multiple_choice, mListener?.getChapters()?.keys?.toList() ?: listOf())
        mOnItemClickListener = AdapterView.OnItemClickListener { _, _, index, _ ->
            try {
                mIsManual = true
                mListener?.filterChapter(mChapters.getItemAtPosition(index) as String, !mChapters.isItemChecked(index))
            } finally {
                mIsManual = false
            }
        }
        mChapters.onItemClickListener = mOnItemClickListener

        return root
    }

    fun setListener(listener: AnnotationListener?) {
        mListener = listener
    }

    fun setChapters(chapters: Map<String, Float>) {
        if (!::mChapters.isInitialized)
            return
        
        (mChapters.adapter as ArrayAdapter<*>).clear()
        (mChapters.adapter as ArrayAdapter<String>).addAll(chapters.keys)
        (mChapters.adapter as ArrayAdapter<*>).notifyDataSetChanged()
    }

    fun setChaptersFilter(chapters: Map<String, Float>) {
        if (!::mChapters.isInitialized)
            return

        if (!mIsManual) {
            try {
                mChapters.onItemClickListener = null
                for (i in 0 until mChapters.count)
                    mChapters.setItemChecked(i, chapters.any { c -> c.key == mChapters.getItemAtPosition(i) })
            } finally {
                mChapters.onItemClickListener = mOnItemClickListener
            }
        }
    }

}
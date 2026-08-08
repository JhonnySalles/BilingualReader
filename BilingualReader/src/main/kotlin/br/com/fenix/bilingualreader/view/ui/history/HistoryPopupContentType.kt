package br.com.fenix.bilingualreader.view.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.Type

class HistoryPopupContentType : Fragment() {

    private lateinit var mViewModel: HistoryViewModel
    private lateinit var mManga: CheckBox
    private lateinit var mBook: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = ViewModelProvider(requireParentFragment())[HistoryViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.popup_history_content_type, container, false)
        mManga = root.findViewById(R.id.popup_history_content_manga)
        mBook = root.findViewById(R.id.popup_history_content_book)

        setChecked(mViewModel.contentTypes.value.orEmpty())
        observer()
        addListener()
        return root
    }

    private fun setChecked(types: Set<Type>) {
        // Empty set means all — show both unchecked to represent "no filter"
        mManga.isChecked = Type.MANGA in types
        mBook.isChecked = Type.BOOK in types
    }

    private fun addListener() {
        mManga.setOnCheckedChangeListener { _, _ -> applyFilter() }
        mBook.setOnCheckedChangeListener { _, _ -> applyFilter() }
    }

    private fun removeListener() {
        mManga.setOnCheckedChangeListener(null)
        mBook.setOnCheckedChangeListener(null)
    }

    private fun applyFilter() {
        removeListener()
        val selected = mutableSetOf<Type>()
        if (mManga.isChecked) selected.add(Type.MANGA)
        if (mBook.isChecked) selected.add(Type.BOOK)
        mViewModel.filterContentTypes(selected)
        addListener()
    }

    private fun observer() {
        mViewModel.contentTypes.observe(viewLifecycleOwner) {
            removeListener()
            setChecked(it)
            addListener()
        }
    }
}

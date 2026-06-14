package br.com.fenix.bilingualreader.view.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.enums.Type

class HistoryPopupLibraries : Fragment() {

    private lateinit var mViewModel: HistoryViewModel
    private lateinit var mContainer: LinearLayout
    private val mItems = mutableListOf<Pair<Library?, TextView>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = ViewModelProvider(requireParentFragment())[HistoryViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.popup_checkbox_list, container, false)
        mContainer = root.findViewById(R.id.popup_checkbox_list_container)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel.libraries.observe(viewLifecycleOwner) {
            buildList()
        }
        mViewModel.selectedLibrary.observe(viewLifecycleOwner) { selected ->
            updateSelection(selected)
        }
    }

    private fun buildList() {
        mContainer.removeAllViews()
        mItems.clear()

        val inflater = LayoutInflater.from(context)

        // 1. "Todos"
        val viewAll = inflater.inflate(R.layout.popup_list_item, mContainer, false) as TextView
        viewAll.text = getString(R.string.history_menu_choice_all)
        mItems.add(Pair(null, viewAll))
        mContainer.addView(viewAll)

        // 2. Geral (Default Library)
        val defaultLib = mViewModel.mDefaultLibrary
        val viewDefault = inflater.inflate(R.layout.popup_list_item, mContainer, false) as TextView
        viewDefault.text = defaultLib.title
        mItems.add(Pair(defaultLib, viewDefault))
        mContainer.addView(viewDefault)

        val libs = mViewModel.libraries.value ?: emptyList()
        val bookLibs = libs.filter { it.type == Type.BOOK }
        val mangaLibs = libs.filter { it.type == Type.MANGA }

        // 3. Books Section
        if (bookLibs.isNotEmpty()) {
            val divider = inflater.inflate(R.layout.popup_divider, mContainer, false)
            mContainer.addView(divider)
            for (lib in bookLibs) {
                val tv = inflater.inflate(R.layout.popup_list_item, mContainer, false) as TextView
                tv.text = lib.title
                mItems.add(Pair(lib, tv))
                mContainer.addView(tv)
            }
        }

        // 4. Mangas Section
        if (mangaLibs.isNotEmpty()) {
            val divider = inflater.inflate(R.layout.popup_divider, mContainer, false)
            mContainer.addView(divider)
            for (lib in mangaLibs) {
                val tv = inflater.inflate(R.layout.popup_list_item, mContainer, false) as TextView
                tv.text = lib.title
                mItems.add(Pair(lib, tv))
                mContainer.addView(tv)
            }
        }

        setupListeners()
        updateSelection(mViewModel.selectedLibrary.value)
    }

    private fun setupListeners() {
        for (item in mItems) {
            val lib = item.first
            val tv = item.second
            tv.setOnClickListener {
                mViewModel.filterLibrary(lib)
            }
        }
    }

    private fun updateSelection(selected: Library?) {
        for (item in mItems) {
            val lib = item.first
            val tv = item.second
            if (lib == selected) {
                tv.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ico_check_mark, 0)
            } else {
                tv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            }
        }
    }
}

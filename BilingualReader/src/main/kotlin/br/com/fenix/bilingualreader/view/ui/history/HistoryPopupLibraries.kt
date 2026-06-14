package br.com.fenix.bilingualreader.view.ui.history

import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.enums.Type

class HistoryPopupLibraries : Fragment() {

    private lateinit var mViewModel: HistoryViewModel
    private lateinit var mContainer: LinearLayout
    private val mCheckBoxes = mutableListOf<Pair<Library?, CheckBox>>()

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
        mCheckBoxes.clear()

        val inflater = LayoutInflater.from(context)

        // 1. "Todos"
        val cbAll = CheckBox(ContextThemeWrapper(requireContext(), R.style.CheckBox)).apply {
            text = getString(R.string.history_menu_choice_all)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        mCheckBoxes.add(Pair(null, cbAll))
        mContainer.addView(cbAll)

        // 2. Geral (Default Library)
        val defaultLib = mViewModel.mDefaultLibrary
        val cbDefault = CheckBox(ContextThemeWrapper(requireContext(), R.style.CheckBox)).apply {
            text = defaultLib.title
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        mCheckBoxes.add(Pair(defaultLib, cbDefault))
        mContainer.addView(cbDefault)

        val libs = mViewModel.libraries.value ?: emptyList()
        val bookLibs = libs.filter { it.type == Type.BOOK }
        val mangaLibs = libs.filter { it.type == Type.MANGA }

        // 3. Books Section
        if (bookLibs.isNotEmpty()) {
            val divider = inflater.inflate(R.layout.popup_divider, mContainer, false)
            mContainer.addView(divider)
            for (lib in bookLibs) {
                val cb = CheckBox(ContextThemeWrapper(requireContext(), R.style.CheckBox)).apply {
                    text = lib.title
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                }
                mCheckBoxes.add(Pair(lib, cb))
                mContainer.addView(cb)
            }
        }

        // 4. Mangas Section
        if (mangaLibs.isNotEmpty()) {
            val divider = inflater.inflate(R.layout.popup_divider, mContainer, false)
            mContainer.addView(divider)
            for (lib in mangaLibs) {
                val cb = CheckBox(ContextThemeWrapper(requireContext(), R.style.CheckBox)).apply {
                    text = lib.title
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                }
                mCheckBoxes.add(Pair(lib, cb))
                mContainer.addView(cb)
            }
        }

        setupListeners()
        updateSelection(mViewModel.selectedLibrary.value)
    }

    private fun setupListeners() {
        for (item in mCheckBoxes) {
            val lib = item.first
            val cb = item.second
            cb.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    removeListeners()
                    for (other in mCheckBoxes) {
                        if (other.second != cb) {
                            other.second.isChecked = false
                        }
                    }
                    mViewModel.filterLibrary(lib)
                    setupListeners()
                } else {
                    cb.isChecked = true
                }
            }
        }
    }

    private fun removeListeners() {
        for (item in mCheckBoxes) {
            item.second.setOnCheckedChangeListener(null)
        }
    }

    private fun updateSelection(selected: Library?) {
        removeListeners()
        for (item in mCheckBoxes) {
            val lib = item.first
            val cb = item.second
            cb.isChecked = lib == selected
        }
        setupListeners()
    }
}

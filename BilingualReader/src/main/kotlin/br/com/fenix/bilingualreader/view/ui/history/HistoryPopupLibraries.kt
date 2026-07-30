package br.com.fenix.bilingualreader.view.ui.history

import android.os.Bundle
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
    private val mItems = mutableListOf<Pair<Library?, CheckBox>>()

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
        mViewModel.libraries.observe(viewLifecycleOwner) { buildList() }
        mViewModel.selectedLibraries.observe(viewLifecycleOwner) { updateSelection(it) }
    }

    private fun createCheckBox(text: String): CheckBox {
        val check = LayoutInflater.from(requireContext())
            .inflate(R.layout.popup_checkbox_item, mContainer, false) as CheckBox
        check.text = text
        return check
    }

    private fun buildList() {
        mContainer.removeAllViews()
        mItems.clear()

        val inflater = LayoutInflater.from(context)
        val selected = mViewModel.selectedLibraries.value.orEmpty()

        val viewAll = createCheckBox(getString(R.string.history_menu_choice_all))
        viewAll.isChecked = selected.isEmpty()
        mItems.add(Pair(null, viewAll))
        mContainer.addView(viewAll)

        val defaultLib = mViewModel.mDefaultLibrary
        val viewDefault = createCheckBox(defaultLib.title)
        viewDefault.isChecked = selected.any { it.id == defaultLib.id }
        mItems.add(Pair(defaultLib, viewDefault))
        mContainer.addView(viewDefault)

        val libs = mViewModel.libraries.value ?: emptyList()
        val bookLibs = libs.filter { it.type == Type.BOOK }
        val mangaLibs = libs.filter { it.type == Type.MANGA }

        if (bookLibs.isNotEmpty()) {
            mContainer.addView(inflater.inflate(R.layout.popup_divider, mContainer, false))
            for (lib in bookLibs) {
                val check = createCheckBox(lib.title)
                check.isChecked = selected.any { it.id == lib.id }
                mItems.add(Pair(lib, check))
                mContainer.addView(check)
            }
        }

        if (mangaLibs.isNotEmpty()) {
            mContainer.addView(inflater.inflate(R.layout.popup_divider, mContainer, false))
            for (lib in mangaLibs) {
                val check = createCheckBox(lib.title)
                check.isChecked = selected.any { it.id == lib.id }
                mItems.add(Pair(lib, check))
                mContainer.addView(check)
            }
        }

        setupListeners()
    }

    private fun setupListeners() {
        for ((lib, check) in mItems) {
            check.setOnCheckedChangeListener { _, isChecked ->
                if (lib == null) {
                    if (isChecked)
                        mViewModel.filterLibraries(emptySet())
                    else if (mViewModel.selectedLibraries.value.isNullOrEmpty())
                        check.isChecked = true
                } else {
                    val current = mViewModel.selectedLibraries.value.orEmpty().toMutableSet()
                    if (isChecked) {
                        current.removeAll { it.id == lib.id }
                        current.add(lib)
                    } else {
                        current.removeAll { it.id == lib.id }
                    }
                    mViewModel.filterLibraries(current)
                }
            }
        }
    }

    private fun updateSelection(selected: Set<Library>) {
        for ((lib, check) in mItems) {
            check.setOnCheckedChangeListener(null)
            check.isChecked = if (lib == null) selected.isEmpty() else selected.any { it.id == lib.id }
        }
        setupListeners()
    }
}

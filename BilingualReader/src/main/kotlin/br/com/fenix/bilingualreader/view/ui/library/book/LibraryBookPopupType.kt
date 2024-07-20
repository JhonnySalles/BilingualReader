package br.com.fenix.bilingualreader.view.ui.library.book

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.LibraryBookType
import br.com.fenix.bilingualreader.model.enums.LibraryMangaType
import org.slf4j.LoggerFactory


class LibraryBookPopupType : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(LibraryBookPopupType::class.java)

    private lateinit var mViewModel: BookLibraryViewModel

    private lateinit var mTypeGridBig: CheckBox
    private lateinit var mTypeGridMedium: CheckBox
    private lateinit var mTypeSeparatorBig: CheckBox
    private lateinit var mTypeSeparatorMedium: CheckBox
    private lateinit var mTypeLine: CheckBox

    private lateinit var mCheckMap : Map<LibraryBookType, CheckBox>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = ViewModelProvider(requireActivity()).get(BookLibraryViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.popup_library_type_book, container, false)

        mTypeGridBig = root.findViewById(R.id.popup_library_book_type_grid_big)
        mTypeGridMedium = root.findViewById(R.id.popup_library_book_type_grid_medium)
        mTypeSeparatorBig = root.findViewById(R.id.popup_library_book_type_separator_big)
        mTypeSeparatorMedium = root.findViewById(R.id.popup_library_book_type_separator_medium)

        mTypeLine = root.findViewById(R.id.popup_library_book_type_line)

        mCheckMap = mapOf(
            Pair(LibraryBookType.GRID_BIG, mTypeGridBig),
            Pair(LibraryBookType.GRID_MEDIUM, mTypeGridMedium),
            Pair(LibraryBookType.SEPARATOR_BIG, mTypeSeparatorBig),
            Pair(LibraryBookType.SEPARATOR_MEDIUM, mTypeSeparatorMedium),
            Pair(LibraryBookType.LINE, mTypeLine)
        )

        setChecked(mCheckMap, mViewModel.libraryType.value ?: LibraryBookType.GRID_BIG)
        observer()
        addListener(mCheckMap)

        return root
    }

    private fun setChecked(checkboxes: Map<LibraryBookType, CheckBox>, type: LibraryBookType) {
        for (check in checkboxes)
            check.value.isChecked = check.key == type
    }

    private fun addListener(checkboxes: Map<LibraryBookType, CheckBox>) {
        for (check in checkboxes)
            check.value.setOnCheckedChangeListener { _, isChecked ->
                removeListener(mCheckMap)
                if (isChecked)
                    mViewModel.setLibraryType(check.key)
                else if (check.key == LibraryBookType.GRID_BIG)
                    mViewModel.setLibraryType(LibraryBookType.LINE)
                else
                    mViewModel.setLibraryType(LibraryBookType.GRID_BIG)
                addListener(mCheckMap)
            }
    }

    private fun removeListener(checkboxes: Map<LibraryBookType, CheckBox>) {
        for (check in checkboxes)
            check.value.setOnCheckedChangeListener(null)
    }

    private fun observer() {
        mViewModel.libraryType.observe(viewLifecycleOwner) {
            removeListener(mCheckMap)
            setChecked(mCheckMap, it)
            addListener(mCheckMap)
        }
    }

}
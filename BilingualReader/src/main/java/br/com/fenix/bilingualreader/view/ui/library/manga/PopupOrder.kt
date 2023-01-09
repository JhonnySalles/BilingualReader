package br.com.fenix.bilingualreader.view.ui.library.manga

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.view.components.TriStateCheckBox
import org.slf4j.LoggerFactory

class PopupOrder : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(PopupOrder::class.java)

    private lateinit var mViewModel: MangaLibraryViewModel

    private lateinit var mOrderName: TriStateCheckBox
    private lateinit var mOrderDate: TriStateCheckBox
    private lateinit var mOrderAccess: TriStateCheckBox
    private lateinit var mOrderFavorite: TriStateCheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = ViewModelProvider(requireActivity()).get(MangaLibraryViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.popup_library_order, container, false)

        mOrderName = root.findViewById(R.id.popup_library_order_name)
        mOrderDate = root.findViewById(R.id.popup_library_order_date)
        mOrderAccess = root.findViewById(R.id.popup_library_order_access)
        mOrderFavorite = root.findViewById(R.id.popup_library_order_favorite)
        val listCheck = arrayListOf(mOrderName, mOrderDate, mOrderAccess, mOrderFavorite)

        for (check in listCheck)
            check.state = TriStateCheckBox.STATE_UNCHECKED

        when (mViewModel.getSorted()) {
            Order.Name -> mOrderName.state =
                if (mViewModel.getIsDesc()) TriStateCheckBox.STATE_INDETERMINATE else TriStateCheckBox.STATE_CHECKED
            Order.Date -> mOrderDate.state =
                if (mViewModel.getIsDesc()) TriStateCheckBox.STATE_INDETERMINATE else TriStateCheckBox.STATE_CHECKED
            Order.LastAccess -> mOrderAccess.state =
                if (mViewModel.getIsDesc()) TriStateCheckBox.STATE_INDETERMINATE else TriStateCheckBox.STATE_CHECKED
            Order.Favorite -> mOrderFavorite.state =
                if (mViewModel.getIsDesc()) TriStateCheckBox.STATE_INDETERMINATE else TriStateCheckBox.STATE_CHECKED
            else -> {}
        }

        addListener()

        return root
    }

    private fun getNextState(checkbox : TriStateCheckBox) : Int {
        return when (checkbox.state) {
            TriStateCheckBox.STATE_UNCHECKED -> TriStateCheckBox.STATE_CHECKED
            TriStateCheckBox.STATE_CHECKED -> TriStateCheckBox.STATE_INDETERMINATE
            TriStateCheckBox.STATE_INDETERMINATE -> TriStateCheckBox.STATE_UNCHECKED
            else -> TriStateCheckBox.STATE_INDETERMINATE
        }
    }

    private fun addListener() {
        val listCheck = arrayListOf(mOrderName, mOrderDate, mOrderAccess, mOrderFavorite)

        mOrderName.setOnCheckedChangeListener { _, _ ->
            removeListener(listCheck)

            for (check in listCheck)
                if (check != mOrderName)
                    check.state = TriStateCheckBox.STATE_UNCHECKED

            mOrderName.state = getNextState(mOrderName)
            when (mOrderName.state) {
                TriStateCheckBox.STATE_INDETERMINATE -> mViewModel.sorted(Order.Name, true)
                TriStateCheckBox.STATE_CHECKED -> mViewModel.sorted(Order.Name)
                TriStateCheckBox.STATE_UNCHECKED -> {
                    mOrderName.state = TriStateCheckBox.STATE_CHECKED
                    mViewModel.sorted(Order.Name)
                }
                else -> {}
            }

            addListener()
        }

        mOrderDate.setOnCheckedChangeListener { _, _ ->
            removeListener(listCheck)

            for (check in listCheck)
                if (check != mOrderDate)
                    check.state = TriStateCheckBox.STATE_UNCHECKED

            mOrderDate.state = getNextState(mOrderDate)
            when (mOrderDate.state) {
                TriStateCheckBox.STATE_INDETERMINATE -> mViewModel.sorted(Order.Date, true)
                TriStateCheckBox.STATE_CHECKED -> mViewModel.sorted(Order.Date)
                TriStateCheckBox.STATE_UNCHECKED -> {
                    mOrderName.state = TriStateCheckBox.STATE_CHECKED
                    mViewModel.sorted(Order.Name)
                }
                else -> {}
            }

            addListener()
        }

        mOrderAccess.setOnCheckedChangeListener { _, _ ->
            removeListener(listCheck)

            for (check in listCheck)
                if (check != mOrderAccess)
                    check.state = TriStateCheckBox.STATE_UNCHECKED

            mOrderAccess.state = getNextState(mOrderAccess)
            when (mOrderAccess.state) {
                TriStateCheckBox.STATE_INDETERMINATE -> mViewModel.sorted(Order.LastAccess, true)
                TriStateCheckBox.STATE_CHECKED -> mViewModel.sorted(Order.LastAccess)
                TriStateCheckBox.STATE_UNCHECKED -> {
                    mOrderName.state = TriStateCheckBox.STATE_CHECKED
                    mViewModel.sorted(Order.Name)
                }
                else -> {}
            }

            addListener()
        }

        mOrderFavorite.setOnCheckedChangeListener { _, _ ->
            removeListener(listCheck)

            for (check in listCheck)
                if (check != mOrderFavorite)
                    check.state = TriStateCheckBox.STATE_UNCHECKED

            mOrderFavorite.state = getNextState(mOrderFavorite)
            when (mOrderFavorite.state) {
                TriStateCheckBox.STATE_INDETERMINATE -> mViewModel.sorted(Order.Favorite, true)
                TriStateCheckBox.STATE_CHECKED -> mViewModel.sorted(Order.Favorite)
                TriStateCheckBox.STATE_UNCHECKED -> {
                    mOrderName.state = TriStateCheckBox.STATE_CHECKED
                    mViewModel.sorted(Order.Name)
                }
                else -> {}
            }

            addListener()
        }
    }

    private fun removeListener(checkboxes: ArrayList<TriStateCheckBox>) {
        for (check in checkboxes)
            check.setOnCheckedChangeListener(null)
    }

}
package br.com.fenix.bilingualreader.view.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.view.components.TriStateCheckBox

class HistoryPopupOrder : Fragment() {

    private lateinit var mViewModel: HistoryViewModel

    private lateinit var mOrderName: TriStateCheckBox
    private lateinit var mOrderAccess: TriStateCheckBox
    private lateinit var mOrderFavorite: TriStateCheckBox
    private lateinit var mOrderSeries: TriStateCheckBox
    private lateinit var mOrderAuthor: TriStateCheckBox
    private lateinit var mOrderGenre: TriStateCheckBox

    private lateinit var mCheckList: Map<TriStateCheckBox, Order>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = ViewModelProvider(requireParentFragment())[HistoryViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.popup_history_order, container, false)

        mOrderName = root.findViewById(R.id.popup_history_order_name)
        mOrderAccess = root.findViewById(R.id.popup_history_order_access)
        mOrderFavorite = root.findViewById(R.id.popup_history_order_favorite)
        mOrderSeries = root.findViewById(R.id.popup_history_order_series)
        mOrderAuthor = root.findViewById(R.id.popup_history_order_author)
        mOrderGenre = root.findViewById(R.id.popup_history_order_genre)

        mCheckList = mapOf(
            mOrderName to Order.Name,
            mOrderAccess to Order.LastAccess,
            mOrderFavorite to Order.Favorite,
            mOrderSeries to Order.Series,
            mOrderAuthor to Order.Author,
            mOrderGenre to Order.Genre
        )

        val currentOrder = mViewModel.order.value ?: Pair(Order.LastAccess, false)
        setChecked(mCheckList, currentOrder.first, currentOrder.second)
        addListener(mCheckList)
        observer()
        return root
    }

    private fun setChecked(checkboxes: Map<TriStateCheckBox, Order>, order: Order, isDesc: Boolean) {
        for (check in checkboxes.keys) {
            check.state = TriStateCheckBox.STATE_UNCHECKED
        }

        val state = if (isDesc) TriStateCheckBox.STATE_INDETERMINATE else TriStateCheckBox.STATE_CHECKED
        when (order) {
            Order.Name -> mOrderName.state = state
            Order.LastAccess -> mOrderAccess.state = state
            Order.Favorite -> mOrderFavorite.state = state
            Order.Series -> mOrderSeries.state = state
            Order.Author -> mOrderAuthor.state = state
            Order.Genre -> mOrderGenre.state = state
            else -> {}
        }
    }

    private fun getNextState(checkbox: TriStateCheckBox): Int {
        return when (checkbox.state) {
            TriStateCheckBox.STATE_UNCHECKED -> TriStateCheckBox.STATE_CHECKED
            TriStateCheckBox.STATE_CHECKED -> TriStateCheckBox.STATE_INDETERMINATE
            TriStateCheckBox.STATE_INDETERMINATE -> TriStateCheckBox.STATE_CHECKED
            else -> TriStateCheckBox.STATE_INDETERMINATE
        }
    }

    private fun setOnCheckedChangeListener(checkbox: TriStateCheckBox, order: Order) {
        checkbox.setOnCheckedChangeListener { _, _ ->
            removeListener(mCheckList)

            checkbox.state = getNextState(checkbox)
            when (checkbox.state) {
                TriStateCheckBox.STATE_INDETERMINATE -> mViewModel.sorted(order, true)
                TriStateCheckBox.STATE_CHECKED -> mViewModel.sorted(order, false)
                TriStateCheckBox.STATE_UNCHECKED -> {
                    checkbox.state = TriStateCheckBox.STATE_CHECKED
                    mViewModel.sorted(order, false)
                }
                else -> {}
            }

            addListener(mCheckList)
        }
    }

    private fun addListener(checkboxes: Map<TriStateCheckBox, Order>) {
        for (check in checkboxes.keys) {
            setOnCheckedChangeListener(check, checkboxes[check]!!)
        }
    }

    private fun removeListener(checkboxes: Map<TriStateCheckBox, Order>) {
        for (check in checkboxes.keys) {
            check.setOnCheckedChangeListener(null)
        }
    }

    private fun observer() {
        mViewModel.order.observe(viewLifecycleOwner) {
            removeListener(mCheckList)
            setChecked(mCheckList, it.first, it.second)
            addListener(mCheckList)
        }
    }
}

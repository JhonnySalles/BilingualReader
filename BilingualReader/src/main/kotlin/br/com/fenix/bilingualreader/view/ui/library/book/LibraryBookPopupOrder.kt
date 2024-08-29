package br.com.fenix.bilingualreader.view.ui.library.book

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.service.listener.PopupOrderListener
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.view.components.TriStateCheckBox
import org.slf4j.LoggerFactory


class LibraryBookPopupOrder : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(LibraryBookPopupOrder::class.java)

    private lateinit var mOrderName: TriStateCheckBox
    private lateinit var mOrderDate: TriStateCheckBox
    private lateinit var mOrderAccess: TriStateCheckBox
    private lateinit var mOrderFavorite: TriStateCheckBox
    private lateinit var mOrderGenre: TriStateCheckBox
    private lateinit var mOrderAuthor: TriStateCheckBox
    private lateinit var mOrderSeries: TriStateCheckBox

    private lateinit var mCheckList : Map<TriStateCheckBox, Order>

    private var listener: PopupOrderListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.popup_library_order_book, container, false)

        mOrderName = root.findViewById(R.id.popup_library_order_book_name)
        mOrderDate = root.findViewById(R.id.popup_library_order_book_date)
        mOrderAccess = root.findViewById(R.id.popup_library_order_book_access)
        mOrderFavorite = root.findViewById(R.id.popup_library_order_book_favorite)
        mOrderGenre = root.findViewById(R.id.popup_library_order_book_genre)
        mOrderAuthor = root.findViewById(R.id.popup_library_order_book_author)
        mOrderSeries = root.findViewById(R.id.popup_library_order_book_series)

        mCheckList = mapOf(mOrderName to Order.Name, mOrderDate to Order.Date, mOrderAccess to Order.LastAccess, mOrderFavorite to Order.Favorite,
            mOrderGenre to Order.Genre, mOrderAuthor to Order.Author, mOrderSeries to Order.Series)

        setChecked(mCheckList, listener?.popupGetOrder()?.first ?: Order.Name, listener?.popupGetOrder()?.second ?: false)
        addListener(mCheckList)
        observer()
        return root
    }

    fun setListener(listener: PopupOrderListener) {
        this.listener = listener
    }

    private fun setChecked(checkboxes: Map<TriStateCheckBox, Order>, order: Order, isDesc: Boolean) {
        for (check in checkboxes.keys)
            check.state = TriStateCheckBox.STATE_UNCHECKED

        when (order) {
            Order.Name -> mOrderName.state =
                if (isDesc) TriStateCheckBox.STATE_INDETERMINATE else TriStateCheckBox.STATE_CHECKED
            Order.Date -> mOrderDate.state =
                if (isDesc) TriStateCheckBox.STATE_INDETERMINATE else TriStateCheckBox.STATE_CHECKED
            Order.LastAccess -> mOrderAccess.state =
                if (isDesc) TriStateCheckBox.STATE_INDETERMINATE else TriStateCheckBox.STATE_CHECKED
            Order.Favorite -> mOrderFavorite.state =
                if (isDesc) TriStateCheckBox.STATE_INDETERMINATE else TriStateCheckBox.STATE_CHECKED
            Order.Genre -> mOrderGenre.state =
                if (isDesc) TriStateCheckBox.STATE_INDETERMINATE else TriStateCheckBox.STATE_CHECKED
            Order.Author -> mOrderAuthor.state =
                if (isDesc) TriStateCheckBox.STATE_INDETERMINATE else TriStateCheckBox.STATE_CHECKED
            Order.Series -> mOrderSeries.state =
                if (isDesc) TriStateCheckBox.STATE_INDETERMINATE else TriStateCheckBox.STATE_CHECKED
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
                TriStateCheckBox.STATE_INDETERMINATE -> listener?.popupSorted(order, true)
                TriStateCheckBox.STATE_CHECKED -> listener?.popupSorted(order)
                TriStateCheckBox.STATE_UNCHECKED -> {
                    checkbox.state = TriStateCheckBox.STATE_CHECKED
                    listener?.popupSorted(order)
                }
                else -> {}
            }

            listener?.popupOrderOnChange()
            addListener(mCheckList)
        }
    }

    private fun addListener(checkboxes: Map<TriStateCheckBox, Order>) {
        for (check in checkboxes.keys)
            setOnCheckedChangeListener(check, checkboxes[check]!!)
    }

    private fun removeListener(checkboxes: Map<TriStateCheckBox, Order>) {
        for (check in checkboxes.keys)
            check.setOnCheckedChangeListener(null)
    }

    private fun observer() {
        listener?.popupGetObserver()?.observe(viewLifecycleOwner) {
            removeListener(mCheckList)
            setChecked(mCheckList, it.first, it.second)
            addListener(mCheckList)
            save(it.first)
        }
    }

    private fun save(order: Order) {
        val sharedPreferences = GeneralConsts.getSharedPreferences(requireContext())
        with(sharedPreferences.edit()) {
            this!!.putString(GeneralConsts.KEYS.LIBRARY.BOOK_ORDER, order.toString())
            this.commit()
        }
    }

}
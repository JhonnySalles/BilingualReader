package br.com.fenix.bilingualreader.view.ui.library.manga

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


class LibraryMangaPopupOrder : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(LibraryMangaPopupOrder::class.java)

    private lateinit var mOrderName: TriStateCheckBox
    private lateinit var mOrderDate: TriStateCheckBox
    private lateinit var mOrderAccess: TriStateCheckBox
    private lateinit var mOrderFavorite: TriStateCheckBox
    private lateinit var mOrderGenre: TriStateCheckBox
    private lateinit var mOrderAuthor: TriStateCheckBox
    private lateinit var mOrderSeries: TriStateCheckBox

    private lateinit var mCheckList : Map<TriStateCheckBox, Order>

    private lateinit var listener: PopupOrderListener

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.popup_library_order_manga, container, false)

        mOrderName = root.findViewById(R.id.popup_library_order_manga_name)
        mOrderDate = root.findViewById(R.id.popup_library_order_manga_date)
        mOrderAccess = root.findViewById(R.id.popup_library_order_manga_access)
        mOrderFavorite = root.findViewById(R.id.popup_library_order_manga_favorite)
        mOrderGenre = root.findViewById(R.id.popup_library_order_manga_genre)
        mOrderAuthor = root.findViewById(R.id.popup_library_order_manga_author)
        mOrderSeries = root.findViewById(R.id.popup_library_order_manga_series)

        mCheckList = mapOf(mOrderName to Order.Name, mOrderDate to Order.Date, mOrderAccess to Order.LastAccess, mOrderFavorite to Order.Favorite,
            mOrderGenre to Order.Genre, mOrderAuthor to Order.Author, mOrderSeries to Order.Series)

        var order = Order.Name
        var isDesc = false
        if (::listener.isInitialized) {
            order = listener.popupGetOrder()?.first ?: Order.Name
            isDesc = listener.popupGetOrder()?.second ?: false
        }

        setChecked(mCheckList, order, isDesc)
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
        listener.popupGetObserver().observe(viewLifecycleOwner) {
            removeListener(mCheckList)
            setChecked(mCheckList, it.first, it.second)
            addListener(mCheckList)
            save(it.first)
        }
    }

    private fun save(order: Order) {
        val sharedPreferences = GeneralConsts.getSharedPreferences(requireContext())
        with(sharedPreferences.edit()) {
            this!!.putString(GeneralConsts.KEYS.LIBRARY.MANGA_ORDER, order.toString())
            this.commit()
        }
    }

}
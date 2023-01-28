package br.com.fenix.bilingualreader.view.ui.vocabulary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.view.components.PopupOrderListener
import br.com.fenix.bilingualreader.view.components.TriStateCheckBox
import org.slf4j.LoggerFactory

class VocabularyPopupOrder(var listener: PopupOrderListener) : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(VocabularyPopupOrder::class.java)

    private lateinit var mOrderDescription: TriStateCheckBox
    private lateinit var mOrderFrequency: TriStateCheckBox
    private lateinit var mOrderFavorite: TriStateCheckBox

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.popup_vocabulary_order, container, false)

        mOrderDescription = root.findViewById(R.id.popup_vocabulary_order_description)
        mOrderFrequency = root.findViewById(R.id.popup_vocabulary_order_frequency)
        mOrderFavorite = root.findViewById(R.id.popup_vocabulary_order_favorite)

        setChecked(
            getCheckList(),
            listener.popupGetOrder()?.first ?: Order.Description,
            listener.popupGetOrder()?.second ?: false
        )
        addListener()
        observer()
        return root
    }

    private fun setChecked(checkboxes: ArrayList<TriStateCheckBox>, order: Order, isDesc: Boolean) {
        for (check in checkboxes)
            check.state = TriStateCheckBox.STATE_UNCHECKED

        when (order) {
            Order.Description -> mOrderDescription.state =
                if (isDesc) TriStateCheckBox.STATE_INDETERMINATE else TriStateCheckBox.STATE_CHECKED
            Order.Frequency -> mOrderFrequency.state =
                if (isDesc) TriStateCheckBox.STATE_INDETERMINATE else TriStateCheckBox.STATE_CHECKED
            Order.Favorite -> mOrderFavorite.state =
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

    private fun getCheckList() =
        arrayListOf(mOrderDescription, mOrderFrequency, mOrderFavorite)

    private fun addListener() {
        val listCheck = getCheckList()

        mOrderDescription.setOnCheckedChangeListener { _, _ ->
            removeListener(listCheck)

            for (check in listCheck)
                if (check != mOrderDescription)
                    check.state = TriStateCheckBox.STATE_UNCHECKED

            mOrderDescription.state = getNextState(mOrderDescription)
            when (mOrderDescription.state) {
                TriStateCheckBox.STATE_INDETERMINATE -> listener.popupSorted(Order.Description, true)
                TriStateCheckBox.STATE_CHECKED -> listener.popupSorted(Order.Description)
                TriStateCheckBox.STATE_UNCHECKED -> {
                    mOrderDescription.state = TriStateCheckBox.STATE_CHECKED
                    listener.popupSorted(Order.Description)
                }
                else -> {}
            }

            addListener()
        }

        mOrderFrequency.setOnCheckedChangeListener { _, _ ->
            removeListener(listCheck)

            for (check in listCheck)
                if (check != mOrderFrequency)
                    check.state = TriStateCheckBox.STATE_UNCHECKED

            mOrderFrequency.state = getNextState(mOrderFrequency)
            when (mOrderFrequency.state) {
                TriStateCheckBox.STATE_INDETERMINATE -> listener.popupSorted(Order.Frequency, true)
                TriStateCheckBox.STATE_CHECKED -> listener.popupSorted(Order.Frequency)
                TriStateCheckBox.STATE_UNCHECKED -> {
                    mOrderDescription.state = TriStateCheckBox.STATE_CHECKED
                    listener.popupSorted(Order.Description)
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
                TriStateCheckBox.STATE_INDETERMINATE -> listener.popupSorted(Order.Favorite, true)
                TriStateCheckBox.STATE_CHECKED -> listener.popupSorted(Order.Favorite)
                TriStateCheckBox.STATE_UNCHECKED -> {
                    mOrderDescription.state = TriStateCheckBox.STATE_CHECKED
                    listener.popupSorted(Order.Description)
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

    private fun observer() {
        val listCheck = getCheckList()
        listener.popupGetObserver().observe(viewLifecycleOwner) {
            removeListener(listCheck)
            setChecked(getCheckList(), it.first, it.second)
            addListener()
        }
    }

}
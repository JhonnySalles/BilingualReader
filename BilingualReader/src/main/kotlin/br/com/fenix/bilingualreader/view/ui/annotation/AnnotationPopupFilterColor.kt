package br.com.fenix.bilingualreader.view.ui.annotation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.fragment.app.Fragment
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.Color
import br.com.fenix.bilingualreader.service.listener.AnnotationListener
import org.slf4j.LoggerFactory


class AnnotationPopupFilterColor : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(AnnotationPopupFilterColor::class.java)

    private lateinit var mFilterNone: CheckBox
    private lateinit var mFilterYellow: CheckBox
    private lateinit var mFilterRed: CheckBox
    private lateinit var mFilterGreen: CheckBox
    private lateinit var mFilterBlue: CheckBox

    private lateinit var mCheckMap: Map<CheckBox, Color>
    private var mListener: AnnotationListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.popup_annotation_filter_color, container, false)

        mFilterNone = root.findViewById(R.id.popup_annotation_filter_color_none)
        mFilterYellow = root.findViewById(R.id.popup_annotation_filter_color_yellow)
        mFilterRed = root.findViewById(R.id.popup_annotation_filter_color_red)
        mFilterGreen = root.findViewById(R.id.popup_annotation_filter_color_green)
        mFilterBlue = root.findViewById(R.id.popup_annotation_filter_color_blue)

        mCheckMap = mapOf(mFilterNone to Color.None, mFilterYellow to Color.Yellow, mFilterRed to Color.Red, mFilterGreen to Color.Green, mFilterBlue to Color.Blue)

        setChecked(mCheckMap, mListener?.getColors() ?: setOf())
        addListener()

        return root
    }

    private fun setChecked(map: Map<CheckBox, Color>, filter: Set<Color>) {
        for (check in map.keys) {
            val selected = filter.any { it == map[check] }
            if (selected != check.isChecked)
                check.isChecked = selected
        }
    }

    private fun addListener() {
        for (check in mCheckMap.keys)
            check.setOnCheckedChangeListener { _, isChecked ->
                removeListener(mCheckMap.keys)
                mListener?.filterColor(mCheckMap[check]!!, !isChecked)
                addListener()
            }

    }

    private fun removeListener(checkboxes : Set<CheckBox>) {
        for (check in checkboxes)
            check.setOnCheckedChangeListener(null)
    }

    fun setListener(listener: AnnotationListener?) {
        mListener = listener
    }

    fun setColors(colors: Set<Color>) {
        if (!::mCheckMap.isInitialized)
            return

        removeListener(mCheckMap.keys)
        setChecked(mCheckMap, colors)
        addListener()
    }

}
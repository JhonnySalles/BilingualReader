package br.com.fenix.bilingualreader.view.components

import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.util.AttributeSet
import androidx.annotation.StyleableRes
import br.com.fenix.bilingualreader.R
import com.google.android.material.checkbox.MaterialCheckBox

class TriStateCheckBox : MaterialCheckBox {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, com.google.android.material.R.attr.checkboxStyle)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        val sets = intArrayOf(R.attr.state)

        val typedArray = context.obtainStyledAttributes(attrs, sets)

        try {
            state = typedArray.getInt(ATTR_STATE, STATE_UNCHECKED)
        } finally {
            typedArray.recycle()
        }

        initComponent()
    }

    companion object {
        // @formatter:off
        @StyleableRes private const val ATTR_STATE = 0
        // @formatter:on

        const val STATE_UNCHECKED: Int = 0
        const val STATE_INDETERMINATE: Int = 1
        const val STATE_CHECKED: Int = 2

        private val UNCHECKED = intArrayOf(R.attr.state_unchecked)
        private val INDETERMINATE = intArrayOf(R.attr.state_indeterminate)
        private val CHECKED = intArrayOf(R.attr.state_checked)
    }

    private var isChangingState = false

    var state: Int
        @Throws(IllegalStateException::class)
        set(value) {
            if (isChangingState) return
            if (field == value) return
            isChangingState = true

            field = value
            isChecked = when (value) {
                STATE_UNCHECKED -> false
                STATE_INDETERMINATE -> true
                STATE_CHECKED -> true
                else -> throw IllegalStateException("$value is not a valid state for ${this.javaClass.name}")
            }
            refreshDrawableState()

            isChangingState = false

            println("change state " + state)

            onStateChanged?.let { it(this@TriStateCheckBox, value) }
        }

    var onStateChanged: ((TriStateCheckBox, Int) -> Unit)? = null

    private fun initComponent() {
        setButtonDrawable(R.drawable.custom_checkbox_tristate)
        setOnCheckedChangeListener { _, _ ->
            state = when (state) {
                STATE_UNCHECKED -> STATE_INDETERMINATE
                STATE_INDETERMINATE -> STATE_CHECKED
                STATE_CHECKED -> STATE_UNCHECKED
                else -> STATE_INDETERMINATE
            }
        }
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)

        mergeDrawableStates(
            drawableState, when (state) {
                STATE_UNCHECKED -> UNCHECKED
                STATE_INDETERMINATE -> INDETERMINATE
                STATE_CHECKED -> CHECKED
                else -> INDETERMINATE
            }
        )

        return drawableState
    }
}
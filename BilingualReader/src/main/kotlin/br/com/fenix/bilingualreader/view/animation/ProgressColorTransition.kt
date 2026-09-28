package br.com.fenix.bilingualreader.view.animation

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.transition.Transition
import android.transition.TransitionValues
import android.view.ViewGroup
import android.widget.ProgressBar

/**
 * Custom shared element Transition that smoothly interpolates ProgressBar colors (ARGB)
 * during Activity and Fragment transitions, preventing abrupt color jumps.
 */
class ProgressColorTransition(
    private val explicitStartColor: Int? = null,
    private val explicitEndColor: Int? = null
) : Transition() {

    companion object {
        private const val PROPNAME_PROGRESS_COLOR = "bilingualreader:progressColor:color"
        private val TRANSITION_PROPERTIES = arrayOf(PROPNAME_PROGRESS_COLOR)

        fun extractProgressColor(progressBar: ProgressBar): Int? {
            val tint = progressBar.progressTintList
            if (tint != null) {
                val color = tint.defaultColor
                if (color != 0) return color
            }

            val drawable = progressBar.progressDrawable
            if (drawable is LayerDrawable) {
                val progressItem = drawable.findDrawableByLayerId(android.R.id.progress)
                if (progressItem is ClipDrawable) {
                    val shape = progressItem.drawable
                    if (shape is GradientDrawable) {
                        val colors = shape.colors
                        if (colors != null && colors.isNotEmpty()) {
                            return colors[0]
                        }
                        shape.color?.let {
                            return it.defaultColor
                        }
                    }
                }
            }
            return null
        }
    }

    override fun getTransitionProperties(): Array<String> = TRANSITION_PROPERTIES

    private fun captureValues(transitionValues: TransitionValues) {
        val view = transitionValues.view
        if (view is ProgressBar) {
            val color = extractProgressColor(view)
            if (color != null) {
                transitionValues.values[PROPNAME_PROGRESS_COLOR] = color
            }
        }
    }

    override fun captureStartValues(transitionValues: TransitionValues) {
        if (explicitStartColor != null) {
            transitionValues.values[PROPNAME_PROGRESS_COLOR] = explicitStartColor
        } else {
            captureValues(transitionValues)
        }
    }

    override fun captureEndValues(transitionValues: TransitionValues) {
        if (explicitEndColor != null) {
            transitionValues.values[PROPNAME_PROGRESS_COLOR] = explicitEndColor
        } else {
            captureValues(transitionValues)
        }
    }

    override fun createAnimator(
        sceneRoot: ViewGroup,
        startValues: TransitionValues?,
        endValues: TransitionValues?
    ): Animator? {
        if (startValues == null || endValues == null) return null
        val view = endValues.view as? ProgressBar ?: return null
        val startColor = startValues.values[PROPNAME_PROGRESS_COLOR] as? Int ?: return null
        val endColor = endValues.values[PROPNAME_PROGRESS_COLOR] as? Int ?: return null

        if (startColor == endColor) return null

        view.progressTintList = ColorStateList.valueOf(startColor)

        val animator = ValueAnimator.ofArgb(startColor, endColor)
        animator.addUpdateListener { va ->
            val color = va.animatedValue as Int
            view.progressTintList = ColorStateList.valueOf(color)
        }
        return animator
    }
}

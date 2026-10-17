package br.com.fenix.bilingualreader.view.animation

import android.animation.Animator
import android.animation.ObjectAnimator
import android.transition.Transition
import android.transition.TransitionValues
import android.view.ViewGroup
import android.widget.TextView

/**
 * Custom shared element Transition that smoothly interpolates TextView text color (ARGB)
 * during Activity and Fragment transitions, preventing abrupt color changes.
 */
class TextColorTransition(
    private val explicitStartColor: Int? = null,
    private val explicitEndColor: Int? = null
) : Transition() {

    companion object {
        private const val PROPNAME_TEXT_COLOR = "bilingualreader:textColor:color"
        private val TRANSITION_PROPERTIES = arrayOf(PROPNAME_TEXT_COLOR)
    }

    override fun getTransitionProperties(): Array<String> = TRANSITION_PROPERTIES

    private fun captureValues(transitionValues: TransitionValues) {
        val view = transitionValues.view
        if (view is TextView) {
            transitionValues.values[PROPNAME_TEXT_COLOR] = view.currentTextColor
        }
    }

    override fun captureStartValues(transitionValues: TransitionValues) {
        if (explicitStartColor != null) {
            transitionValues.values[PROPNAME_TEXT_COLOR] = explicitStartColor
        } else {
            captureValues(transitionValues)
        }
    }

    override fun captureEndValues(transitionValues: TransitionValues) {
        if (explicitEndColor != null) {
            transitionValues.values[PROPNAME_TEXT_COLOR] = explicitEndColor
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
        val view = endValues.view as? TextView ?: return null
        val startColor = startValues.values[PROPNAME_TEXT_COLOR] as? Int ?: return null
        val endColor = endValues.values[PROPNAME_TEXT_COLOR] as? Int ?: return null

        if (startColor == endColor) return null

        view.setTextColor(startColor)
        return ObjectAnimator.ofArgb(view, "textColor", startColor, endColor)
    }
}

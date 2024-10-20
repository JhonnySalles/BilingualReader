package br.com.fenix.bilingualreader.view.components

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.provider.Settings
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil.ThemeUtils.getColorFromAttr


class ComponentsUtil {

    companion object ComponentsUtils {
        fun canDrawOverlays(context: Context): Boolean =
            Settings.canDrawOverlays(context)

        private const val duration = 300L
        fun changeAnimateVisibility(component: View, visible: Boolean) {
            if ((visible && component.visibility == View.VISIBLE) || (!visible && component.visibility != View.VISIBLE))
                return

            val visibility = if (visible) View.VISIBLE else View.GONE
            val initialAlpha = if (visible) 0.0f else 1.0f
            val finalAlpha = if (visible) 1.0f else 0.0f


            if (visible) {
                component.visibility = visibility
                component.alpha = initialAlpha
            }

            component.animate().alpha(finalAlpha).setDuration(duration)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        component.visibility = visibility
                    }
                })
        }

        fun changeAnimateVisibility(components: List<View>, visible: Boolean) {
            val visibility = if (visible) View.VISIBLE else View.GONE
            val initialAlpha = if (visible) 0.0f else 1.0f
            val finalAlpha = if (visible) 1.0f else 0.0f

            for (component in components) {
                if ((visible && component.visibility == View.VISIBLE) || (!visible && component.visibility != View.VISIBLE))
                    continue

                if (visible) {
                    component.visibility = visibility
                    component.alpha = initialAlpha
                }

                component.animate().alpha(finalAlpha).setDuration(duration)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            component.visibility = visibility
                        }
                    })
            }
        }

        fun changeWidthAnimateSize(view: View, finalLayout: ConstraintLayout.LayoutParams, isExpanded: Boolean? = null) {
            val isInitialExpanded = isExpanded ?: (view.width >= finalLayout.width)
            view.layoutParams = ConstraintLayout.LayoutParams(view.layoutParams as ConstraintLayout.LayoutParams)

            val finalWidth = finalLayout.width
            val initialWidth = if (isInitialExpanded) view.width - finalLayout.width else view.width

            val animation: Animation = object : Animation() {
                override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                    if (interpolatedTime >= 1f)
                        view.layoutParams = finalLayout
                    else {
                        view.layoutParams.width = if (isInitialExpanded)
                            finalWidth + (initialWidth - (initialWidth * interpolatedTime).toInt())
                        else
                            initialWidth + (finalWidth * interpolatedTime).toInt()
                    }
                    view.requestLayout()
                }

                override fun willChangeBounds(): Boolean {
                    return true
                }
            }

            animation.duration = duration / 2
            view.startAnimation(animation)
        }

        fun setThemeColor(context: Context, swipe: SwipeRefreshLayout) {
            swipe.setColorSchemeColors(
                context.getColorFromAttr(R.attr.colorOnPrimaryContainer),
                context.getColorFromAttr(R.attr.colorOnPrimaryContainer),
                context.getColorFromAttr(R.attr.colorPrimaryContainer)
            )
            swipe.setProgressBackgroundColorSchemeColor(context.getColorFromAttr(R.attr.colorPrimaryContainer))
        }

    }
}
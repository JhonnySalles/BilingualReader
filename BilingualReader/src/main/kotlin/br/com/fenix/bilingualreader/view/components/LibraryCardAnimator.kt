package br.com.fenix.bilingualreader.view.components

import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.util.helpers.AnimationUtil

/**
 * Property-animator based entry animations for library cards.
 * Unlike legacy [android.view.animation.Animation], these run in the Choreographer
 * animation callback and are immune to BlurView's extra software draw pass.
 */
object LibraryCardAnimator {

    const val DURATION_MS = 200L
    private val interpolator = DecelerateInterpolator()

    enum class Style {
        LINE,
        SEPARATOR_TITLE_RIGHT,
        GRID,
        SEPARATOR_TITLE_CENTER,
        CAROUSEL_TITLE,
        CAROUSEL_ITEM
    }

    fun animate(view: View, style: Style) {
        view.animate().cancel()

        when (style) {
            Style.LINE, Style.SEPARATOR_TITLE_CENTER, Style.CAROUSEL_ITEM -> {
                // Crescimento Centralizado
                view.alpha = 0f
                view.scaleX = 0.5f
                view.scaleY = 0.5f
                view.translationX = 0f
                view.translationY = 0f
                val start = {
                    view.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(DURATION_MS)
                        .setInterpolator(interpolator)
                        .start()
                }
                start()
            }
            Style.GRID, Style.SEPARATOR_TITLE_RIGHT -> {
                // Vem da Direita
                view.alpha = 0f
                view.scaleX = 1f
                view.scaleY = 1f
                view.translationY = 0f
                val start = {
                    val dx = if (view.width > 0) view.width.toFloat() else view.resources.displayMetrics.widthPixels.toFloat()
                    view.translationX = dx
                    view.animate()
                        .alpha(1f)
                        .translationX(0f)
                        .setDuration(DURATION_MS)
                        .setInterpolator(interpolator)
                        .start()
                }
                if (view.width > 0) start() else view.post(start)
            }
            Style.CAROUSEL_TITLE -> {
                // Slide de Baixo para Cima
                view.alpha = 0f
                view.scaleX = 1f
                view.scaleY = 1f
                view.translationX = 0f
                val start = {
                    val dy = if (view.height > 0) view.height.toFloat() else view.resources.displayMetrics.density * 48f
                    view.translationY = dy
                    view.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(DURATION_MS)
                        .setInterpolator(interpolator)
                        .start()
                }
                if (view.height > 0) start() else view.post(start)
            }
        }
    }

    fun clear(view: View) {
        view.animate().cancel()
        view.alpha = 1f
        view.translationX = 0f
        view.translationY = 0f
        view.scaleX = 1f
        view.scaleY = 1f
    }

    fun clear(holder: RecyclerView.ViewHolder) = clear(holder.itemView)

    fun hasNoAnimationPayload(payloads: List<Any>): Boolean =
        payloads.any { it == AnimationUtil.PROPERTY_NO_ANIMATION }
}

package br.com.fenix.bilingualreader.view.components

import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView

/**
 * Pauses glass blur for the duration of RecyclerView item animations by acquiring
 * a scheduler token in [runPendingAnimations] and releasing it when animations finish.
 *
 * Add/remove appearance is left to [LibraryCardAnimator] (bind-time) to avoid racing
 * on the same [View.animate] and leaving cards stuck at entry scale/alpha.
 */
class BlurAwareItemAnimator : DefaultItemAnimator() {

    private var activeToken: GlassRenderScheduler.Token? = null

    override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
        dispatchAddFinished(holder)
        return false
    }

    override fun animateRemove(holder: RecyclerView.ViewHolder): Boolean {
        dispatchRemoveFinished(holder)
        return false
    }

    override fun runPendingAnimations() {
        val acquiredNow = activeToken == null
        if (acquiredNow) {
            activeToken = GlassRenderScheduler.acquire("recycler")
        }
        super.runPendingAnimations()
        if (acquiredNow) {
            isRunning {
                val token = activeToken
                activeToken = null
                GlassRenderScheduler.release(token)
                GlassRenderScheduler.requestUpdateAll()
            }
        }
    }

    fun destroy() {
        GlassRenderScheduler.release(activeToken)
        activeToken = null
    }
}

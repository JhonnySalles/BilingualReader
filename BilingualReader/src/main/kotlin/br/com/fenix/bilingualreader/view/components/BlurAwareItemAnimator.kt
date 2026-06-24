package br.com.fenix.bilingualreader.view.components

import android.os.Handler
import android.os.Looper
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.util.helpers.blurOnceDeferred
import eightbitlab.com.blurview.BlurView

class BlurAwareItemAnimator(
    private val blurViews: List<BlurView>,
    private val autoRestoreDelay: Long = 50L
) : DefaultItemAnimator() {
    
    private var runningAnimations = 0
    private val handler = Handler(Looper.getMainLooper())
    
    override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
        pauseBlur()
        return super.animateAdd(holder)
    }
    
    override fun animateRemove(holder: RecyclerView.ViewHolder): Boolean {
        pauseBlur()
        return super.animateRemove(holder)
    }
    
    override fun animateChange(
        oldHolder: RecyclerView.ViewHolder,
        newHolder: RecyclerView.ViewHolder,
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int
    ): Boolean {
        pauseBlur()
        return super.animateChange(oldHolder, newHolder, fromX, fromY, toX, toY)
    }
    
    override fun animateMove(
        holder: RecyclerView.ViewHolder,
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int
    ): Boolean {
        pauseBlur()
        return super.animateMove(holder, fromX, fromY, toX, toY)
    }
    
    override fun onAddFinished(item: RecyclerView.ViewHolder) {
        super.onAddFinished(item)
        checkResumeBlur()
    }
    
    override fun onRemoveFinished(item: RecyclerView.ViewHolder) {
        super.onRemoveFinished(item)
        checkResumeBlur()
    }
    
    override fun onChangeFinished(item: RecyclerView.ViewHolder, oldItem: Boolean) {
        super.onChangeFinished(item, oldItem)
        checkResumeBlur()
    }
    
    override fun onMoveFinished(item: RecyclerView.ViewHolder) {
        super.onMoveFinished(item)
        checkResumeBlur()
    }
    
    private fun pauseBlur() {
        runningAnimations++
        getViews().forEach { it.setBlurAutoUpdate(false) }
    }
    
    private fun getViews(): List<BlurView> {
        val activityBlurs = (blurViews.firstOrNull()?.context as? br.com.fenix.bilingualreader.MainActivity)?.blurViews.orEmpty()
        return blurViews + activityBlurs
    }
    
    private fun checkResumeBlur() {
        runningAnimations--
        if (runningAnimations <= 0) {
            runningAnimations = 0
            handler.postDelayed({
                if (runningAnimations == 0) {
                    getViews().forEach { bv ->
                        bv.blurOnceDeferred(handler, 50)
                    }
                }
            }, autoRestoreDelay)
        }
    }

    fun destroy() {
        handler.removeCallbacksAndMessages(null)
    }
}

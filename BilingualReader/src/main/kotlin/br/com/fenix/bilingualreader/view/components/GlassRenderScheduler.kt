package br.com.fenix.bilingualreader.view.components

import android.app.ActivityManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.Trace
import android.view.Choreographer
import br.com.fenix.bilingualreader.BuildConfig
import eightbitlab.com.blurview.BlurView
import java.util.IdentityHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Central coordinator for glass/blur updates.
 *
 * Higher-priority UI work (RecyclerView animations, card entry, popups, transitions)
 * acquires tokens so blur is skipped for those frames. When no token is held, blur
 * still respects a per-frame budget, adaptive backoff and a rate cap — if it cannot
 * finish in time it is skipped and the last blurred bitmap is kept.
 */
object GlassRenderScheduler {

    private const val TOKEN_SAFETY_MS = 2_000L
    private const val DEFAULT_BUDGET_NS = 5_000_000L
    private const val LOW_RAM_BUDGET_NS = 3_000_000L
    private const val COST_CEILING_NS = 4_000_000L
    private const val LOW_RAM_COST_CEILING_NS = 2_500_000L
    private const val MAX_BACKOFF = 4
    private const val FORCE_AFTER_SKIPPED = 8
    private const val MIN_INTERVAL_NS = 16_000_000L
    private const val SCROLL_MIN_INTERVAL_NS = 33_000_000L
    private const val COST_EMA_ALPHA = 0.2

    enum class Mode { ON_DEMAND, CONTINUOUS }

    class Token internal constructor(
        val id: Int,
        val reason: String,
        val deadlineElapsedMs: Long
    )

    private data class ViewState(
        val blurView: BlurView,
        var mode: Mode = Mode.ON_DEMAND,
        var pendingUpdate: Boolean = false,
        var lastUpdateNanos: Long = 0L,
        var avgCostNanos: Long = 0L,
        var backoffN: Int = 1,
        var frameCounter: Int = 0,
        var skippedByBudget: Int = 0,
        var minIntervalNanos: Long = MIN_INTERVAL_NS
    )

    private val mainHandler = Handler(Looper.getMainLooper())
    private val tokens = LinkedHashMap<Int, Token>()
    private val tokenIdGen = AtomicInteger(0)
    private var suspendUntilElapsedMs = 0L

    private val byController = IdentityHashMap<Any, ViewState>()
    private val byView = IdentityHashMap<BlurView, ViewState>()

    @Volatile
    private var lastFrameTimeNanos = 0L

    @Volatile
    private var frameIntervalNanos = MIN_INTERVAL_NS

    private var choreographerRegistered = false
    private var budgetNanos = DEFAULT_BUDGET_NS
    private var costCeilingNanos = COST_CEILING_NS
    private var lowRamInitialized = false

    private var debugSkipped = 0L
    private var debugUpdated = 0L

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (byController.isEmpty()) {
                choreographerRegistered = false
                return
            }
            if (lastFrameTimeNanos != 0L) {
                val delta = frameTimeNanos - lastFrameTimeNanos
                if (delta in 1..(frameIntervalNanos * 4)) {
                    frameIntervalNanos = delta
                }
            }
            lastFrameTimeNanos = frameTimeNanos
            purgeExpiredTokens()
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private fun ensureLowRam(context: Context?) {
        if (lowRamInitialized || context == null) return
        lowRamInitialized = true
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        if (am?.isLowRamDevice == true) {
            budgetNanos = LOW_RAM_BUDGET_NS
            costCeilingNanos = LOW_RAM_COST_CEILING_NS
        }
    }

    private fun ensureChoreographer() {
        if (choreographerRegistered) return
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { ensureChoreographer() }
            return
        }
        if (!choreographerRegistered) {
            choreographerRegistered = true
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }

    private fun maybeStopChoreographer() {
        // Frame callback self-stops when byController is empty.
    }

    private fun purgeExpiredTokens() {
        if (tokens.isEmpty()) return
        val now = SystemClock.elapsedRealtime()
        val it = tokens.entries.iterator()
        while (it.hasNext()) {
            if (it.next().value.deadlineElapsedMs <= now) {
                it.remove()
            }
        }
    }

    fun acquire(reason: String): Token {
        purgeExpiredTokens()
        val token = Token(
            id = tokenIdGen.incrementAndGet(),
            reason = reason,
            deadlineElapsedMs = SystemClock.elapsedRealtime() + TOKEN_SAFETY_MS
        )
        tokens[token.id] = token
        return token
    }

    fun release(token: Token?) {
        if (token == null) return
        tokens.remove(token.id)
    }

    inline fun <T> withPriority(reason: String, block: () -> T): T {
        val token = acquire(reason)
        return try {
            block()
        } finally {
            release(token)
        }
    }

    fun suspendFor(durationMs: Long, reason: String = "") {
        val until = SystemClock.elapsedRealtime() + durationMs.coerceAtLeast(0L)
        if (until > suspendUntilElapsedMs) {
            suspendUntilElapsedMs = until
        }
        if (BuildConfig.DEBUG && reason.isNotEmpty()) {
            // reason retained for debugging; no allocation in release
        }
    }

    fun register(controller: Any, blurView: BlurView) {
        ensureLowRam(blurView.context)
        val state = ViewState(blurView)
        byController[controller] = state
        byView[blurView] = state
        ensureChoreographer()
    }

    fun unregister(controller: Any) {
        val state = byController.remove(controller) ?: return
        byView.remove(state.blurView)
        maybeStopChoreographer()
    }

    fun setContinuous(controller: Any, continuous: Boolean) {
        val state = byController[controller] ?: return
        state.mode = if (continuous) Mode.CONTINUOUS else Mode.ON_DEMAND
        if (continuous) {
            state.minIntervalNanos = SCROLL_MIN_INTERVAL_NS
        } else {
            state.minIntervalNanos = MIN_INTERVAL_NS
        }
    }

    fun setContinuous(blurView: BlurView, continuous: Boolean) {
        val state = byView[blurView] ?: return
        state.mode = if (continuous) Mode.CONTINUOUS else Mode.ON_DEMAND
        state.minIntervalNanos = if (continuous) SCROLL_MIN_INTERVAL_NS else MIN_INTERVAL_NS
    }

    fun setScrollRateCap(blurView: BlurView, enabled: Boolean) {
        val state = byView[blurView] ?: return
        state.minIntervalNanos = if (enabled) SCROLL_MIN_INTERVAL_NS else MIN_INTERVAL_NS
    }

    fun requestUpdate(blurView: BlurView) {
        val state = byView[blurView] ?: return
        state.pendingUpdate = true
        if (blurView.isAttachedToWindow) {
            blurView.invalidate()
        }
    }

    fun requestUpdateAll() {
        for (state in byView.values) {
            state.pendingUpdate = true
            if (state.blurView.isAttachedToWindow) {
                state.blurView.invalidate()
            }
        }
    }

    private fun isPriorityBlocking(): Boolean {
        purgeExpiredTokens()
        if (tokens.isNotEmpty()) return true
        return SystemClock.elapsedRealtime() < suspendUntilElapsedMs
    }

    /**
     * Called from [eightbitlab.com.blurview.GlassBlurController] on every pre-draw.
     * @return true when this frame's blur should be skipped.
     */
    @JvmStatic
    fun shouldSkip(controller: Any): Boolean {
        val state = byController[controller] ?: return true

        if (isPriorityBlocking()) {
            if (BuildConfig.DEBUG) debugSkipped++
            return true
        }

        val wantsUpdate = state.mode == Mode.CONTINUOUS || state.pendingUpdate
        if (!wantsUpdate) {
            return true
        }

        state.frameCounter++
        if (state.backoffN > 1 && (state.frameCounter % state.backoffN) != 0) {
            if (BuildConfig.DEBUG) debugSkipped++
            return true
        }

        val now = System.nanoTime()
        val sinceUpdate = now - state.lastUpdateNanos
        if (state.lastUpdateNanos != 0L && sinceUpdate < state.minIntervalNanos) {
            if (BuildConfig.DEBUG) debugSkipped++
            return true
        }

        val frameStart = lastFrameTimeNanos
        if (frameStart != 0L) {
            val elapsedInFrame = now - frameStart
            // lastFrameTimeNanos is vsync time; elapsed can be compared roughly to budget
            // when already deep into the frame (layout/measure consumed budget)
            if (elapsedInFrame > budgetNanos && state.skippedByBudget < FORCE_AFTER_SKIPPED) {
                state.skippedByBudget++
                if (BuildConfig.DEBUG) debugSkipped++
                return true
            }
        }

        // Allowed — consume pending flag
        state.pendingUpdate = false
        state.skippedByBudget = 0
        return false
    }

    @JvmStatic
    fun recordCost(controller: Any, costNanos: Long) {
        val state = byController[controller] ?: return
        state.lastUpdateNanos = System.nanoTime()

        if (state.avgCostNanos == 0L) {
            state.avgCostNanos = costNanos
        } else {
            state.avgCostNanos =
                (COST_EMA_ALPHA * costNanos + (1.0 - COST_EMA_ALPHA) * state.avgCostNanos).toLong()
        }

        state.backoffN = when {
            state.avgCostNanos > costCeilingNanos * 2 -> MAX_BACKOFF
            state.avgCostNanos > costCeilingNanos -> (state.backoffN + 1).coerceAtMost(MAX_BACKOFF)
            state.avgCostNanos < costCeilingNanos / 2 -> 1
            else -> state.backoffN
        }

        if (BuildConfig.DEBUG) {
            debugUpdated++
            Trace.beginSection("glassBlur")
            Trace.endSection()
        }
    }

    @JvmStatic
    fun beginBlurTrace() {
        if (BuildConfig.DEBUG) {
            Trace.beginSection("glassBlur")
        }
    }

    @JvmStatic
    fun endBlurTrace() {
        if (BuildConfig.DEBUG) {
            Trace.endSection()
        }
    }
}

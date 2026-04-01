package br.com.fenix.bilingualreader.view.components.manga

import android.view.MotionEvent
import io.mockk.mockk
import io.mockk.verify
import io.mockk.every
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ZoomGestureDetectorTest {

    private lateinit var listener: ZoomGestureDetector.Listener
    private lateinit var detector: ZoomGestureDetector

    @Before
    fun setup() {
        listener = mockk(relaxed = true)
        detector = ZoomGestureDetector(listener)
    }

    @Test
    fun `ACTION_MOVE with two pointers calculates scaling correctly`() {
        // Initial setup: ACTION_DOWN (pointer 0 at 100,100)
        val eventDown = mockk<MotionEvent>(relaxed = true)
        every { eventDown.actionMasked } returns MotionEvent.ACTION_DOWN
        every { eventDown.pointerCount } returns 1
        every { eventDown.x } returns 100f
        every { eventDown.y } returns 100f
        every { eventDown.getX(0) } returns 100f
        every { eventDown.getY(0) } returns 100f
        detector.onTouchEvent(eventDown)

        // ACTION_POINTER_DOWN (pointer 0 at 100,100; pointer 1 at 200,200)
        // Span = abs(150-100) + abs(150-100) + abs(150-200) + abs(150-200) / 2 = 100
        val eventPointerDown = mockk<MotionEvent>(relaxed = true)
        every { eventPointerDown.actionMasked } returns MotionEvent.ACTION_POINTER_DOWN
        every { eventPointerDown.pointerCount } returns 2
        every { eventPointerDown.getX(0) } returns 100f
        every { eventPointerDown.getY(0) } returns 100f
        every { eventPointerDown.getX(1) } returns 200f
        every { eventPointerDown.getY(1) } returns 200f
        detector.onTouchEvent(eventPointerDown)

        // ACTION_MOVE (pointer 0 at 100,100; pointer 1 at 300,300)
        // New Focal: (200, 200)
        // New Span: (abs(200-100) + abs(200-100) + abs(200-300) + abs(200-300)) / 2 = 200
        // Expected scaling: 200 / 100 = 2
        val eventMove = mockk<MotionEvent>(relaxed = true)
        every { eventMove.actionMasked } returns MotionEvent.ACTION_MOVE
        every { eventMove.pointerCount } returns 2
        every { eventMove.getX(0) } returns 100f
        every { eventMove.getY(0) } returns 100f
        every { eventMove.getX(1) } returns 300f
        every { eventMove.getY(1) } returns 300f
        detector.onTouchEvent(eventMove)

        verify { listener.onZoom(match { abs(it - 2.0f) < 0.01f }, any(), any(), any()) }
    }
}

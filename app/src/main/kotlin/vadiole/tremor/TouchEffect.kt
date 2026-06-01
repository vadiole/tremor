package vadiole.tremor

import android.animation.TimeInterpolator
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import kotlin.math.abs

class TouchEffect(
    private val pressedScale: Float = DEFAULT_PRESSED_SCALE,
    private val releaseDurationMs: Long = DEFAULT_RELEASE_DURATION_MS,
    private val releaseInterpolator: TimeInterpolator = DEFAULT_RELEASE_INTERPOLATOR,
    private val rubberBandDrag: Boolean = false,
    private val maxDragPx: Float = 0f,
    private val dragDamping: Float = DEFAULT_DRAG_DAMPING,
    private val pressDurationMs: Long = DEFAULT_PRESS_DURATION_MS,
    private val shouldHandleDown: (View, MotionEvent) -> Boolean = { _, _ -> true },
) : View.OnTouchListener {

    private var initialRawX = 0f
    private var initialRawY = 0f
    private var dragging = false
    private var touchSlop = 0
    private var active = false

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                active = shouldHandleDown(view, event)
                if (active) onDown(view, event)
            }
            MotionEvent.ACTION_MOVE -> if (active) onMove(view, event)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (active) {
                release(view)
                active = false
            }
        }
        return false
    }

    fun press(view: View) {
        view.animate().cancel()
        view.translationX = 0f
        view.translationY = 0f
        view.animate()
            .scaleX(pressedScale)
            .scaleY(pressedScale)
            .setDuration(pressDurationMs)
            .setInterpolator(PRESS_INTERPOLATOR)
            .start()
    }

    fun release(view: View) {
        view.animate().cancel()
        view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .translationX(0f)
            .translationY(0f)
            .setDuration(releaseDurationMs)
            .setInterpolator(releaseInterpolator)
            .start()
    }

    private fun onDown(view: View, event: MotionEvent) {
        initialRawX = event.rawX
        initialRawY = event.rawY
        dragging = false
        if (rubberBandDrag && touchSlop == 0) {
            touchSlop = ViewConfiguration.get(view.context).scaledTouchSlop
        }
        press(view)
    }

    private fun onMove(view: View, event: MotionEvent) {
        if (!rubberBandDrag || maxDragPx <= 0f) return

        val dx = event.rawX - initialRawX
        val dy = event.rawY - initialRawY
        if (!dragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
            dragging = true
        }
        if (dragging) {
            view.translationX = rubberBand(dx, maxDragPx, dragDamping)
            view.translationY = rubberBand(dy, maxDragPx, dragDamping)
        }
    }

    private companion object {
        const val DEFAULT_PRESSED_SCALE = 1.035f
        const val DEFAULT_PRESS_DURATION_MS = 80L
        const val DEFAULT_RELEASE_DURATION_MS = 180L
        const val DEFAULT_DRAG_DAMPING = 0.08f

        val PRESS_INTERPOLATOR: TimeInterpolator = DecelerateInterpolator(1.8f)
        val DEFAULT_RELEASE_INTERPOLATOR: TimeInterpolator = OvershootInterpolator(1.4f)
    }
}

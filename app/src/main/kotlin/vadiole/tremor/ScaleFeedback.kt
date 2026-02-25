package vadiole.tremor

import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator

private val releaseInterpolator = OvershootInterpolator(2f)

fun View.animatePress(scale: Float = 0.97f) {
    animate().scaleX(scale).scaleY(scale).setDuration(80).start()
}

fun View.animateRelease() {
    animate().scaleX(1f).scaleY(1f).setDuration(150)
        .setInterpolator(releaseInterpolator).start()
}

class ScaleFeedback(private val scale: Float = 0.97f) : View.OnTouchListener {
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> v.animatePress(scale)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animateRelease()
        }
        return false
    }
}

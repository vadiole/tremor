package vadiole.tremor

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

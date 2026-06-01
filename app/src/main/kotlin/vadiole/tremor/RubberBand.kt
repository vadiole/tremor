package vadiole.tremor

import kotlin.math.abs
import kotlin.math.sign

/**
 * Rubber-band resistance: an [offset] is dampened so resistance rises with distance and asymptotes
 * at [maxDistance]. Shared by [TouchEffect]'s drag handling and BallBoxView's stretch math.
 */
fun rubberBand(offset: Float, maxDistance: Float, damping: Float): Float {
    val distance = maxDistance.coerceAtLeast(1f)
    val constant = damping.coerceAtLeast(0.001f)
    val absOffset = abs(offset)
    if (absOffset < 0.01f) return 0f
    val dampened = (1f - 1f / (absOffset * constant / distance + 1f)) * distance
    return sign(offset) * dampened
}

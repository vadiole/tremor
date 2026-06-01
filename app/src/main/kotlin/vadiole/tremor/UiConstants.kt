package vadiole.tremor

object UiConstants {
    const val CORNER_RADIUS_DP = 8f
    const val CONTENT_PADDING_DP = 16
    const val SECTION_SPACING_DP = 24
    const val ITEM_SPACING_DP = 8

    object WaveOverlay {
        const val MAX_WAVES = 10
        const val BASE_DURATION_MS = 600f
        const val BASE_EXPAND_SPEED_DP = 800f
        const val BASE_RING_WIDTH_DP = 40f
        const val BASE_DISTORTION_AMPLITUDE_DP = 3f

        // Below this a wave contributes nothing visible; skip bbox/draw work.
        const val MIN_VISIBLE_INTENSITY = 0.001f
        const val DISTORTION_FREQUENCY = 34f
        const val DISTORTION_DECAY = 5.2f
        const val DISTORTION_ATTACK_SECONDS = 0.045f
        const val DISTORTION_DISTANCE_DECAY = 1800f
        const val RING_BRIGHTNESS_SCALE = 0.1f
    }
}

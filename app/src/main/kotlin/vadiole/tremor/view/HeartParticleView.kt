package vadiole.tremor.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.VibrationEffect
import android.view.View
import vadiole.tremor.Density

class HeartParticleView(
    context: Context,
    private val playPrimitive: (primitiveId: Int, scale: Float) -> Unit,
) : View(context), Density {

    private val gravity = 900f
    private val heartCount = 10

    private val redHeart = "\u2764\uFE0F"

    private val themedSets = arrayOf(
        arrayOf("\uD83D\uDC99", "\uD83D\uDC9B"),                       // 💙💛 Ukraine
        arrayOf("\uD83D\uDC97", "\uD83D\uDC9C", "\uD83D\uDC99"),       // 💗💜💙 Bi flag
        arrayOf("\uD83E\uDD0D", "\uD83E\uDD0D"),                       // 🤍❤️ Polish
        arrayOf("\uD83D\uDC1E"),                                        // 🐞 Ladybug
        arrayOf("\uD83C\uDF38", "\uD83C\uDF38", "\uD83C\uDF38"),       // 🌸🌸🌸 Cherry blossom
        arrayOf("\uD83C\uDF08"),                                        // 🌈 Rainbow
        arrayOf("\u2B50", "\u2B50"),                                     // ⭐⭐ Star
        arrayOf("\uD83E\uDD8B", "\uD83E\uDD8B"),                       // 🦋🦋 Butterfly
        arrayOf("\u2728", "\u2728"),                                     // ✨✨ Sparkles
    )

    private val heartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 20f.dp()
        textAlign = Paint.Align.CENTER
    }

    private class Heart(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var size: Float,
        var rotation: Float,
        var rotationSpeed: Float,
        val emoji: String,
        var passedBottom: Boolean = false,
    )

    private val hearts = mutableListOf<Heart>()
    private var lastFrameTime = 0L
    private var isRunning = false

    private val animRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return

            val now = System.nanoTime()
            val dt = ((now - lastFrameTime) / 1_000_000_000f).coerceAtMost(0.05f)
            lastFrameTime = now

            val iter = hearts.iterator()
            while (iter.hasNext()) {
                val h = iter.next()
                h.vy += gravity * dt
                h.x += h.vx * dt
                h.y += h.vy * dt
                h.rotation += h.rotationSpeed * dt

                if (!h.passedBottom && h.y > height) {
                    h.passedBottom = true
                    playPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.75f)
                }

                if (h.y > height + 100f.dp()) {
                    iter.remove()
                }
            }

            invalidate()

            if (hearts.isNotEmpty()) {
                postOnAnimation(this)
            } else {
                isRunning = false
            }
        }
    }

    fun launchHearts(screenX: Float, screenY: Float) {
        val loc = IntArray(2)
        getLocationOnScreen(loc)
        val localX = screenX - loc[0]
        val localY = screenY - loc[1]

        playPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 0.3f)

        // build emoji list: 1 in 3 chance of themed set
        val emojis = ArrayList<String>(heartCount)
        if (Math.random() < 0.33) {
            val theme = themedSets[(Math.random() * themedSets.size).toInt()]
            repeat(heartCount - theme.size) { emojis.add(redHeart) }
            for (e in theme) emojis.add(e)
        } else {
            repeat(heartCount) { emojis.add(redHeart) }
        }
        emojis.shuffle()

        // aim toward screen center with ±25° spread
        val targetX = width / 2f
        val targetY = height / 2f
        val baseAngle = Math.atan2((targetY - localY).toDouble(), (targetX - localX).toDouble())

        for (i in 0 until heartCount) {
            val spread = Math.toRadians(Math.random() * 50.0 - 25.0)
            val angle = baseAngle + spread
            val speed = 450f + (Math.random() * 350f).toFloat()
            hearts.add(
                Heart(
                    x = localX + (Math.random() * 20 - 10).toFloat(),
                    y = localY,
                    vx = (speed * Math.cos(angle)).toFloat(),
                    vy = (speed * Math.sin(angle)).toFloat(),
                    size = 16f.dp() + (Math.random() * 10f).toFloat().dp(),
                    rotation = (Math.random() * 40 - 20).toFloat(),
                    rotationSpeed = (Math.random() * 200 - 100).toFloat(),
                    emoji = emojis[i],
                ),
            )
        }

        if (!isRunning) {
            lastFrameTime = System.nanoTime()
            isRunning = true
            postOnAnimation(animRunnable)
        }
    }

    override fun onDraw(canvas: Canvas) {
        for (h in hearts) {
            canvas.save()
            canvas.translate(h.x, h.y)
            canvas.rotate(h.rotation)
            heartPaint.textSize = h.size
            canvas.drawText(h.emoji, 0f, heartPaint.textSize / 3f, heartPaint)
            canvas.restore()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isRunning = false
        removeCallbacks(animRunnable)
    }
}

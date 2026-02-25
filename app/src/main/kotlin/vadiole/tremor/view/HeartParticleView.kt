package vadiole.tremor.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import vadiole.tremor.Density

class HeartParticleView(context: Context) : View(context), Density {

    private val gravity = 900f
    private val heartCount = 10

    private val redHeart = "\u2764\uFE0F"
    private val specialEmojis = arrayOf(
        "\uD83D\uDC99",  // 💙 blue heart (Ukraine, bi)
        "\uD83D\uDC9B",  // 💛 yellow heart (Ukraine)
        "\uD83E\uDD0D",  // 🤍 white heart (Poland)
        "\uD83D\uDC97",  // 💗 pink heart (bi)
        "\uD83D\uDC9C",  // 💜 purple heart (bi)
        "\uD83D\uDC1E",  // 🐞 ladybug
    )

    private val vibrator: Vibrator = run {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    }

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
                    playPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 1.0f)
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

        hearts.clear()
        isRunning = false
        removeCallbacks(animRunnable)

        playPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 0.3f)

        val emojis = ArrayList<String>(heartCount)
        repeat(6) { emojis.add(redHeart) }
        repeat(4) { emojis.add(specialEmojis.random()) }
        emojis.shuffle()

        for (i in 0 until heartCount) {
            val angle = Math.toRadians(-90.0 + (Math.random() * 80 - 40))
            val speed = 500f + (Math.random() * 400f).toFloat()
            hearts.add(
                Heart(
                    x = localX + (Math.random() * 30 - 15).toFloat(),
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

        lastFrameTime = System.nanoTime()
        isRunning = true
        postOnAnimation(animRunnable)
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

    private fun playPrimitive(primitiveId: Int, scale: Float) {
        try {
            val effect = VibrationEffect.startComposition()
                .addPrimitive(primitiveId, scale)
                .compose()
            vibrator.vibrate(effect)
        } catch (_: Exception) {
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isRunning = false
        removeCallbacks(animRunnable)
    }
}

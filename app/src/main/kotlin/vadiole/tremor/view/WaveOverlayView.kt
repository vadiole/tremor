package vadiole.tremor.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import android.os.SystemClock
import android.view.View

class WaveOverlayView(context: Context) : View(context) {

    private val density = resources.displayMetrics.density
    private val waves = mutableListOf<Wave>()
    private val maxWaves = 10
    private val durationMs = 600f
    private val expandSpeed = 800f * density
    private val ringWidth = 40f * density

    private val useShader = Build.VERSION.SDK_INT >= 33
    private var shader: RuntimeShader? = null
    private val shaderPaint = Paint()

    private val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = ringWidth
    }

    init {
        isClickable = false
        isFocusable = false
        if (useShader) {
            initShader()
        }
    }

    private fun initShader() {
        if (Build.VERSION.SDK_INT >= 33) {
            shader = RuntimeShader(WAVE_SHADER)
        }
    }

    fun spawnWave(screenX: Float, screenY: Float) {
        val loc = IntArray(2)
        getLocationOnScreen(loc)
        val localX = screenX - loc[0]
        val localY = screenY - loc[1]

        if (waves.size >= maxWaves) {
            waves.removeAt(0)
        }
        waves.add(Wave(localX, localY, SystemClock.elapsedRealtime()))
        postInvalidateOnAnimation()
    }

    fun clearWaves() {
        waves.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (waves.isEmpty()) return

        val now = SystemClock.elapsedRealtime()
        waves.removeAll { now - it.startTime > durationMs }

        if (waves.isEmpty()) return

        if (useShader && shader != null && Build.VERSION.SDK_INT >= 33) {
            drawWithShader(canvas, now)
        } else {
            drawFallback(canvas, now)
        }

        postInvalidateOnAnimation()
    }

    private fun drawWithShader(canvas: Canvas, now: Long) {
        if (Build.VERSION.SDK_INT < 33) return
        val s = shader ?: return

        s.setFloatUniform("resolution", width.toFloat(), height.toFloat())
        s.setFloatUniform("ringWidth", ringWidth)

        val count = waves.size.coerceAtMost(maxWaves)
        s.setIntUniform("waveCount", count)

        val origins = FloatArray(maxWaves * 2)
        val radii = FloatArray(maxWaves)
        val intensities = FloatArray(maxWaves)

        for (i in 0 until count) {
            val wave = waves[i]
            val elapsed = (now - wave.startTime).toFloat()
            val progress = (elapsed / durationMs).coerceIn(0f, 1f)

            origins[i * 2] = wave.x
            origins[i * 2 + 1] = wave.y
            radii[i] = progress * expandSpeed * (durationMs / 1000f)
            val easeOut = 1f - progress * progress
            intensities[i] = easeOut
        }

        s.setFloatUniform("origins", origins)
        s.setFloatUniform("radii", radii)
        s.setFloatUniform("intensities", intensities)

        shaderPaint.shader = s
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), shaderPaint)
    }

    private fun drawFallback(canvas: Canvas, now: Long) {
        for (wave in waves) {
            val elapsed = (now - wave.startTime).toFloat()
            val progress = (elapsed / durationMs).coerceIn(0f, 1f)
            val radius = progress * expandSpeed * (durationMs / 1000f)
            val easeOut = 1f - progress * progress
            val alpha = (easeOut * 0.3f * 255).toInt().coerceIn(0, 255)

            fallbackPaint.alpha = alpha
            fallbackPaint.strokeWidth = ringWidth
            if (radius > ringWidth / 2f) {
                canvas.drawCircle(wave.x, wave.y, radius, fallbackPaint)
            }
        }
    }

    private data class Wave(val x: Float, val y: Float, val startTime: Long)

    companion object {
        private const val WAVE_SHADER = """
            uniform float2 resolution;
            uniform float ringWidth;
            uniform int waveCount;
            uniform float origins[20];
            uniform float radii[10];
            uniform float intensities[10];

            half4 main(float2 fragCoord) {
                float brightness = 0.0;
                float halfRing = ringWidth * 0.5;

                for (int i = 0; i < 10; i++) {
                    if (i >= waveCount) break;

                    float2 origin = float2(origins[i * 2], origins[i * 2 + 1]);
                    float dist = distance(fragCoord, origin);
                    float r = radii[i];

                    float inner = r - halfRing;
                    float outer = r + halfRing;

                    float ringShape = smoothstep(inner - halfRing * 0.5, r, dist)
                                    * smoothstep(outer + halfRing * 0.5, r, dist);

                    brightness += ringShape * intensities[i] * 0.25;
                }

                brightness = clamp(brightness, 0.0, 1.0);
                return half4(brightness, brightness, brightness, brightness);
            }
        """
    }
}

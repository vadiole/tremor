package vadiole.tremor.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.os.Build
import android.os.SystemClock
import android.view.View
import vadiole.tremor.Density
import vadiole.tremor.R

class WaveOverlayView(context: Context) : View(context), Density {

    private val waves = mutableListOf<Wave>()
    private val maxWaves = 10
    private val baseDurationMs = 600f
    private val baseExpandSpeed = 800f.dp()
    private val baseRingWidth = 40f.dp()

    private val useShader = Build.VERSION.SDK_INT >= 33
    private var shader: RuntimeShader? = null
    private val shaderPaint = Paint()

    private val shaderOrigins = FloatArray(maxWaves * 2)
    private val shaderRadii = FloatArray(maxWaves)
    private val shaderIntensities = FloatArray(maxWaves)
    private val shaderRingWidths = FloatArray(maxWaves)

    private val waveColor = context.getColor(R.color.foreground)

    private val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = waveColor
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

    fun spawnWave(screenX: Float, screenY: Float, strength: Float = 0.5f, style: WaveStyle = WaveStyle.DEFAULT) {
        val loc = IntArray(2)
        getLocationOnScreen(loc)
        val localX = screenX - loc[0]
        val localY = screenY - loc[1]

        if (waves.size >= maxWaves) {
            waves.removeAt(0)
        }
        val now = SystemClock.elapsedRealtime()
        waves.add(Wave(localX, localY, now, strength.coerceIn(0f, 1f), style))
        postInvalidateOnAnimation()
    }

    fun clearWaves() {
        waves.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val now = SystemClock.elapsedRealtime()
        waves.removeAll { now - it.startTime > it.totalDurationMs }

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

        val count = waves.size.coerceAtMost(maxWaves)
        s.setIntUniform("waveCount", count)

        for (i in 0 until count) {
            val wave = waves[i]
            val elapsed = (now - wave.startTime).toFloat()
            val effectiveElapsed = (elapsed - wave.delayMs).coerceAtLeast(0f)
            val progress = (effectiveElapsed / wave.durationMs).coerceIn(0f, 1f)

            shaderOrigins[i * 2] = wave.x
            shaderOrigins[i * 2 + 1] = wave.y
            shaderRadii[i] = progress * wave.expandSpeed * (wave.durationMs / 1000f)
            shaderRingWidths[i] = wave.ringWidth

            val easeOut = 1f - progress * progress
            shaderIntensities[i] = if (effectiveElapsed <= 0f) 0f else easeOut * wave.intensityMultiplier
        }

        s.setFloatUniform("origins", shaderOrigins)
        s.setFloatUniform("radii", shaderRadii)
        s.setFloatUniform("intensities", shaderIntensities)
        s.setFloatUniform("ringWidths", shaderRingWidths)
        s.setFloatUniform(
            "waveColor",
            ((waveColor shr 16) and 0xFF) / 255f,
            ((waveColor shr 8) and 0xFF) / 255f,
            (waveColor and 0xFF) / 255f,
        )
        shaderPaint.shader = s
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), shaderPaint)
    }

    private fun drawFallback(canvas: Canvas, now: Long) {
        for (wave in waves) {
            val elapsed = (now - wave.startTime).toFloat()
            val effectiveElapsed = (elapsed - wave.delayMs).coerceAtLeast(0f)
            if (effectiveElapsed <= 0f) continue

            val progress = (effectiveElapsed / wave.durationMs).coerceIn(0f, 1f)
            val radius = progress * wave.expandSpeed * (wave.durationMs / 1000f)
            val easeOut = 1f - progress * progress
            val alpha = (easeOut * wave.intensityMultiplier * 0.1f * 255).toInt().coerceIn(0, 255)

            fallbackPaint.color = (waveColor and 0x00FFFFFF) or (alpha shl 24)
            fallbackPaint.strokeWidth = wave.ringWidth
            if (radius > wave.ringWidth / 2f) {
                canvas.drawCircle(wave.x, wave.y, radius, fallbackPaint)
            }
        }
    }

    private inner class Wave(
        val x: Float,
        val y: Float,
        val startTime: Long,
        strength: Float,
        style: WaveStyle,
    ) {
        val delayMs = style.delayMs
        val durationMs = baseDurationMs * (0.6f + strength * 0.6f) * style.durationMultiplier
        val totalDurationMs = durationMs + delayMs
        val expandSpeed = baseExpandSpeed * (0.5f + strength * 0.7f) * style.speedMultiplier
        val ringWidth = baseRingWidth * (0.5f + strength * 0.5f) * style.ringWidthMultiplier
        val intensityMultiplier = (0.4f + strength * 0.6f) * style.intensityMultiplier
    }

    class WaveStyle(
        val durationMultiplier: Float = 1f,
        val speedMultiplier: Float = 1f,
        val ringWidthMultiplier: Float = 1f,
        val intensityMultiplier: Float = 1f,
        val delayMs: Float = 0f,
    ) {
        companion object {
            val DEFAULT = WaveStyle()

            // short taps: quick, thin
            val TICK = WaveStyle(durationMultiplier = 0.7f, speedMultiplier = 1.2f, ringWidthMultiplier = 0.6f)

            // medium impacts
            val CLICK = WaveStyle(ringWidthMultiplier = 0.8f)

            // heavy impacts: thick, intense
            val THUD = WaveStyle(durationMultiplier = 1.4f, speedMultiplier = 0.7f, ringWidthMultiplier = 1.8f, intensityMultiplier = 1.3f)

            // spin: wide, medium speed
            val SPIN = WaveStyle(durationMultiplier = 1.2f, speedMultiplier = 0.9f, ringWidthMultiplier = 1.5f, intensityMultiplier = 1.1f)

            // quick rise: fast expand, thickening
            val RISE = WaveStyle(durationMultiplier = 1.1f, speedMultiplier = 1.3f, ringWidthMultiplier = 1.4f, intensityMultiplier = 1.2f)

            // slow rise: delayed start, slow expand, thick
            val SLOW_RISE =
                WaveStyle(durationMultiplier = 1.5f, speedMultiplier = 0.6f, ringWidthMultiplier = 1.6f, intensityMultiplier = 1.1f, delayMs = 150f)

            // quick fall: fast shrink feel
            val FALL = WaveStyle(durationMultiplier = 0.8f, speedMultiplier = 1.4f, ringWidthMultiplier = 1.2f, intensityMultiplier = 0.9f)
        }
    }

    companion object {
        private const val WAVE_SHADER = """
            uniform float2 resolution;
            uniform int waveCount;
            uniform float origins[20];
            uniform float radii[10];
            uniform float intensities[10];
            uniform float ringWidths[10];
            uniform float3 waveColor;

            half4 main(float2 fragCoord) {
                float brightness = 0.0;

                for (int i = 0; i < 10; i++) {
                    if (i >= waveCount) break;

                    float2 origin = float2(origins[i * 2], origins[i * 2 + 1]);
                    float dist = distance(fragCoord, origin);
                    float r = radii[i];
                    float halfRing = ringWidths[i] * 0.5;

                    float inner = r - halfRing;
                    float outer = r + halfRing;

                    float ringShape = smoothstep(inner - halfRing * 0.5, r, dist)
                                    * smoothstep(outer + halfRing * 0.5, r, dist);

                    brightness += ringShape * intensities[i] * 0.1;
                }

                brightness = clamp(brightness, 0.0, 1.0);
                return half4(half3(waveColor) * brightness, brightness);
            }
        """
    }
}

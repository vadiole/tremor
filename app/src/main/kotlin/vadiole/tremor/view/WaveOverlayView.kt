package vadiole.tremor.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.view.View
import android.view.ViewTreeObserver
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import vadiole.tremor.Density
import vadiole.tremor.R

class WaveOverlayView(context: Context) : View(context), Density {

    private val waves = mutableListOf<Wave>()
    private val maxWaves = 10
    private val baseDurationMs = 600f
    private val baseExpandSpeed = 800f.dp
    private val baseRingWidth = 40f.dp
    private val baseDistortionAmplitude = 4f.dp

    private var shader: RuntimeShader? = null
    private val shaderPaint = Paint()

    private var distortionTarget: View? = null
    private var distortionShader: RuntimeShader? = null
    private var distortionActive = false

    private val shaderOrigins = FloatArray(maxWaves * 2)
    private val shaderRadii = FloatArray(maxWaves)
    private val shaderIntensities = FloatArray(maxWaves)
    private val shaderRingWidths = FloatArray(maxWaves)
    private val shaderInnerSq = FloatArray(maxWaves)
    private val shaderOuterSq = FloatArray(maxWaves)

    private val distortionOrigins = FloatArray(maxWaves * 2)
    private val distortionAges = FloatArray(maxWaves)
    private val distortionAmplitudes = FloatArray(maxWaves)
    private val distortionSpeeds = FloatArray(maxWaves)
    private val distortionIntensities = FloatArray(maxWaves)
    private val distortionReachSq = FloatArray(maxWaves)

    private val waveColor = context.getColor(R.color.foreground)
    private val waveColorR = ((waveColor shr 16) and 0xFF) / 255f
    private val waveColorG = ((waveColor shr 8) and 0xFF) / 255f
    private val waveColorB = (waveColor and 0xFF) / 255f

    private val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = waveColor
    }

    private val locationScratch = IntArray(2)
    private val targetLocationScratch = IntArray(2)
    private var targetScreenX = 0
    private var targetScreenY = 0
    private var targetPositionDirty = true
    private var targetScrollObserver: ViewTreeObserver? = null
    private val targetLayoutChangeListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        targetPositionDirty = true
    }
    private val targetScrollChangeListener = ViewTreeObserver.OnScrollChangedListener {
        targetPositionDirty = true
    }
    private val targetAttachStateListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            targetPositionDirty = true
            registerTargetScrollListener(v)
        }

        override fun onViewDetachedFromWindow(v: View) {
            unregisterTargetScrollListener()
        }
    }
    private var lastResolutionWidth = -1
    private var lastResolutionHeight = -1

    init {
        isClickable = false
        isFocusable = false
        if (Build.VERSION.SDK_INT >= 33) {
            shader = RuntimeShader(WAVE_SHADER)
            distortionShader = RuntimeShader(DISTORTION_SHADER)
        }
    }

    fun setDistortionTarget(target: View?) {
        if (Build.VERSION.SDK_INT < 33 || distortionTarget === target) return
        clearDistortionEffect()
        distortionTarget?.removeOnLayoutChangeListener(targetLayoutChangeListener)
        distortionTarget?.removeOnAttachStateChangeListener(targetAttachStateListener)
        unregisterTargetScrollListener()

        distortionTarget = target
        targetPositionDirty = true
        lastResolutionWidth = -1
        lastResolutionHeight = -1

        target?.addOnLayoutChangeListener(targetLayoutChangeListener)
        target?.addOnAttachStateChangeListener(targetAttachStateListener)
        if (target?.isAttachedToWindow == true) {
            registerTargetScrollListener(target)
        }
    }

    fun spawnWave(screenX: Float, screenY: Float, strength: Float = 0.5f, style: WaveStyle = WaveStyle.DEFAULT) {
        getLocationOnScreen(locationScratch)
        val localX = screenX - locationScratch[0]
        val localY = screenY - locationScratch[1]

        if (waves.size >= maxWaves) {
            waves.removeAt(0)
        }
        val durationScale = systemAnimationDurationScale()
        if (durationScale <= 0f) {
            clearWaves()
            return
        }
        val now = SystemClock.elapsedRealtime()
        waves.add(Wave(localX, localY, screenX, screenY, now, durationScale, strength.coerceIn(0f, 1f), style))
        updateDistortionEffect(now)
        postInvalidateOnAnimation()
    }

    fun clearWaves() {
        waves.clear()
        clearDistortionEffect()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val now = SystemClock.elapsedRealtime()
        var i = waves.size - 1
        while (i >= 0) {
            if (waves[i].elapsedMs(now) > waves[i].totalDurationMs) {
                waves.removeAt(i)
            }
            i--
        }

        if (waves.isEmpty()) {
            clearDistortionEffect()
            return
        }

        updateDistortionEffect(now)

        if (Build.VERSION.SDK_INT >= 33 && shader != null) {
            drawWithShader(canvas, now)
        } else {
            drawFallback(canvas, now)
        }

        postInvalidateOnAnimation()
    }

    private fun drawWithShader(canvas: Canvas, now: Long) {
        if (Build.VERSION.SDK_INT < 33) return
        val s = shader ?: return

        val count = waves.size.coerceAtMost(maxWaves)
        s.setIntUniform("waveCount", count)

        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY

        for (i in 0 until count) {
            val wave = waves[i]
            val effectiveElapsed = wave.effectiveElapsedMs(now)
            val progress = wave.progress(effectiveElapsed)
            val radius = progress * wave.maxRadius
            val ringW = wave.ringWidth

            shaderOrigins[i * 2] = wave.x
            shaderOrigins[i * 2 + 1] = wave.y
            shaderRadii[i] = radius
            shaderRingWidths[i] = ringW

            val intensity = wave.intensity(progress, effectiveElapsed)
            shaderIntensities[i] = intensity

            // Ring contribution is non-zero only for |dist - radius| < ringW * 1.5 * 0.5 = ringW * 0.75
            val halfRing = ringW * 0.5f
            val innerEdge = radius - halfRing * 1.5f
            val outerEdge = radius + halfRing * 1.5f
            shaderInnerSq[i] = if (innerEdge <= 0f) 0f else innerEdge * innerEdge
            shaderOuterSq[i] = outerEdge * outerEdge

            if (intensity > MIN_VISIBLE_INTENSITY) {
                val outer = radius + ringW * 0.75f
                if (wave.x - outer < minX) minX = wave.x - outer
                if (wave.x + outer > maxX) maxX = wave.x + outer
                if (wave.y - outer < minY) minY = wave.y - outer
                if (wave.y + outer > maxY) maxY = wave.y + outer
            }
        }

        s.setFloatUniform("origins", shaderOrigins)
        s.setFloatUniform("radii", shaderRadii)
        s.setFloatUniform("intensities", shaderIntensities)
        s.setFloatUniform("ringWidths", shaderRingWidths)
        s.setFloatUniform("innerSq", shaderInnerSq)
        s.setFloatUniform("outerSq", shaderOuterSq)
        s.setFloatUniform("waveColor", waveColorR, waveColorG, waveColorB)
        shaderPaint.shader = s

        if (minX == Float.POSITIVE_INFINITY) return

        val clipLeft = max(0f, floor(minX))
        val clipTop = max(0f, floor(minY))
        val clipRight = min(width.toFloat(), ceil(maxX))
        val clipBottom = min(height.toFloat(), ceil(maxY))
        if (clipRight <= clipLeft || clipBottom <= clipTop) return

        canvas.drawRect(clipLeft, clipTop, clipRight, clipBottom, shaderPaint)
    }

    private fun updateDistortionEffect(now: Long) {
        if (Build.VERSION.SDK_INT < 33) return
        val target = distortionTarget ?: return
        val s = distortionShader ?: return
        if (target.width <= 0 || target.height <= 0) return

        val count = waves.size.coerceAtMost(maxWaves)
        if (count == 0) {
            clearDistortionEffect()
            return
        }

        updateTargetPosition(target)
        var hasVisibleWave = false
        var bboxMinX = Float.POSITIVE_INFINITY
        var bboxMinY = Float.POSITIVE_INFINITY
        var bboxMaxX = Float.NEGATIVE_INFINITY
        var bboxMaxY = Float.NEGATIVE_INFINITY

        for (i in 0 until count) {
            val wave = waves[i]
            val effectiveElapsed = wave.effectiveElapsedMs(now)
            val progress = wave.progress(effectiveElapsed)
            val intensity = wave.intensity(progress, effectiveElapsed)

            val originX = wave.screenX - targetScreenX
            val originY = wave.screenY - targetScreenY
            val ageSec = effectiveElapsed / 1000f
            val reach = ageSec * wave.expandSpeed

            distortionOrigins[i * 2] = originX
            distortionOrigins[i * 2 + 1] = originY
            distortionAges[i] = ageSec
            distortionAmplitudes[i] = wave.distortionAmplitude
            distortionSpeeds[i] = wave.expandSpeed
            distortionIntensities[i] = intensity
            distortionReachSq[i] = reach * reach

            if (intensity > MIN_VISIBLE_INTENSITY && wave.distortionAmplitude > 0.001f && reach > 0f) {
                hasVisibleWave = true
                if (originX - reach < bboxMinX) bboxMinX = originX - reach
                if (originX + reach > bboxMaxX) bboxMaxX = originX + reach
                if (originY - reach < bboxMinY) bboxMinY = originY - reach
                if (originY + reach > bboxMaxY) bboxMaxY = originY + reach
            }
        }

        if (!hasVisibleWave) {
            clearDistortionEffect()
            return
        }

        if (lastResolutionWidth != target.width || lastResolutionHeight != target.height) {
            s.setFloatUniform("resolution", target.width.toFloat(), target.height.toFloat())
            lastResolutionWidth = target.width
            lastResolutionHeight = target.height
        }
        s.setIntUniform("waveCount", count)
        s.setFloatUniform("origins", distortionOrigins)
        s.setFloatUniform("ages", distortionAges)
        s.setFloatUniform("amplitudes", distortionAmplitudes)
        s.setFloatUniform("speeds", distortionSpeeds)
        s.setFloatUniform("intensities", distortionIntensities)
        s.setFloatUniform("reachSq", distortionReachSq)
        s.setFloatUniform("bbox", bboxMinX, bboxMinY, bboxMaxX, bboxMaxY)

        // Recreate after uniform changes; AOSP's RuntimeShader+RenderEffect sample:
        // https://android.googlesource.com/platform/frameworks/base/+/master/tests/graphics/HwAccelerationTest/src/com/android/test/hwui/RenderEffectViewActivity.kt#70
        target.setRenderEffect(RenderEffect.createRuntimeShaderEffect(s, "content"))
        distortionActive = true
    }

    private fun clearDistortionEffect() {
        if (!distortionActive) return
        distortionTarget?.setRenderEffect(null)
        distortionActive = false
    }

    private fun registerTargetScrollListener(target: View) {
        if (targetScrollObserver != null) return
        val observer = target.viewTreeObserver
        if (!observer.isAlive) return
        observer.addOnScrollChangedListener(targetScrollChangeListener)
        targetScrollObserver = observer
    }

    private fun unregisterTargetScrollListener() {
        val observer = targetScrollObserver
        if (observer?.isAlive == true) {
            observer.removeOnScrollChangedListener(targetScrollChangeListener)
        }
        targetScrollObserver = null
    }

    private fun updateTargetPosition(target: View) {
        if (!targetPositionDirty) return
        target.getLocationOnScreen(targetLocationScratch)
        targetScreenX = targetLocationScratch[0]
        targetScreenY = targetLocationScratch[1]
        targetPositionDirty = false
    }

    private fun systemAnimationDurationScale(): Float {
        val scale = if (Build.VERSION.SDK_INT >= 33) {
            ValueAnimator.getDurationScale()
        } else {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }
        return scale.coerceAtLeast(0f)
    }

    private fun drawFallback(canvas: Canvas, now: Long) {
        val strokeWidth = 1f.dp
        for (wave in waves) {
            val effectiveElapsed = wave.effectiveElapsedMs(now)
            if (effectiveElapsed <= 0f) continue

            val progress = wave.progress(effectiveElapsed)
            val radius = progress * wave.maxRadius
            val alpha = (wave.intensity(progress, effectiveElapsed) * 0.8f * 255).toInt().coerceIn(0, 255)

            fallbackPaint.color = (waveColor and 0x00FFFFFF) or (alpha shl 24)
            fallbackPaint.strokeWidth = strokeWidth
            if (radius > strokeWidth / 2f) {
                canvas.drawCircle(wave.x, wave.y, radius, fallbackPaint)
            }
        }
    }

    private inner class Wave(
        val x: Float,
        val y: Float,
        val screenX: Float,
        val screenY: Float,
        val startTime: Long,
        durationScale: Float,
        strength: Float,
        style: WaveStyle,
    ) {
        val delayMs = style.delayMs
        val durationMs = baseDurationMs * (0.6f + strength * 0.6f) * style.durationMultiplier
        val totalDurationMs = durationMs + delayMs
        val animationTimeScale = 1f / durationScale.coerceAtLeast(0.001f)
        val expandSpeed = (baseExpandSpeed * (0.5f + strength * 0.7f) * style.speedMultiplier).coerceAtLeast(1f)
        val maxRadius = expandSpeed * (durationMs / 1000f)
        val ringWidth = baseRingWidth * (0.5f + strength * 0.5f) * style.ringWidthMultiplier
        val intensityMultiplier = (0.4f + strength * 0.6f) * style.intensityMultiplier
        val distortionAmplitude = if (style.distortsContent) {
            baseDistortionAmplitude * (0.45f + strength * 0.55f)
        } else {
            0f
        }

        fun elapsedMs(now: Long): Float {
            return (now - startTime).toFloat() * animationTimeScale
        }

        fun effectiveElapsedMs(now: Long): Float {
            return (elapsedMs(now) - delayMs).coerceAtLeast(0f)
        }

        fun progress(effectiveElapsedMs: Float): Float {
            return (effectiveElapsedMs / durationMs).coerceIn(0f, 1f)
        }

        fun intensity(progress: Float, effectiveElapsedMs: Float): Float {
            if (effectiveElapsedMs <= 0f) return 0f
            return (1f - progress * progress) * intensityMultiplier
        }
    }

    class WaveStyle(
        val durationMultiplier: Float = 1f,
        val speedMultiplier: Float = 1f,
        val ringWidthMultiplier: Float = 1f,
        val intensityMultiplier: Float = 1f,
        val delayMs: Float = 0f,
        val distortsContent: Boolean = false,
    ) {
        companion object {
            val DEFAULT = WaveStyle()
            val TICK = WaveStyle(durationMultiplier = 0.7f, speedMultiplier = 1.2f, ringWidthMultiplier = 0.6f)
            val CLICK = WaveStyle(ringWidthMultiplier = 0.8f)
            val THUD = WaveStyle(durationMultiplier = 1.4f, speedMultiplier = 0.7f, ringWidthMultiplier = 1.8f, intensityMultiplier = 1.3f, distortsContent = true)
            val SPIN = WaveStyle(durationMultiplier = 1.2f, speedMultiplier = 0.9f, ringWidthMultiplier = 1.5f, intensityMultiplier = 1.1f, distortsContent = true)
            val RISE = WaveStyle(durationMultiplier = 1.1f, speedMultiplier = 1.3f, ringWidthMultiplier = 1.4f, intensityMultiplier = 1.2f, distortsContent = true)
            val SLOW_RISE = WaveStyle(durationMultiplier = 1.5f, speedMultiplier = 0.6f, ringWidthMultiplier = 1.6f, intensityMultiplier = 1.1f, delayMs = 150f, distortsContent = true)
            val FALL = WaveStyle(durationMultiplier = 0.8f, speedMultiplier = 1.4f, ringWidthMultiplier = 1.2f, intensityMultiplier = 0.9f, distortsContent = true)
        }
    }

    companion object {
        // below this a wave contributes nothing visible — skip its bbox/draw work
        private const val MIN_VISIBLE_INTENSITY = 0.001f

        private const val DISTORTION_SHADER = """
            uniform shader content;
            uniform float2 resolution;
            uniform int waveCount;
            uniform float origins[20];
            uniform float ages[10];
            uniform float amplitudes[10];
            uniform float speeds[10];
            uniform float intensities[10];
            uniform float reachSq[10];
            uniform float4 bbox;

            const float frequency = 34.0;
            const float decay = 5.2;
            const float attackSeconds = 0.045;
            const float distanceDecay = 1800.0;

            half4 main(float2 fragCoord) {
                if (fragCoord.x < bbox.x || fragCoord.x > bbox.z ||
                    fragCoord.y < bbox.y || fragCoord.y > bbox.w) {
                    return content.eval(fragCoord);
                }

                float2 sampleCoord = fragCoord;

                for (int i = 0; i < 10; i++) {
                    if (i >= waveCount) break;

                    float intensity = intensities[i];
                    if (intensity <= 0.001) continue;

                    float2 origin = float2(origins[i * 2], origins[i * 2 + 1]);
                    float2 delta = fragCoord - origin;
                    float distSq = dot(delta, delta);
                    if (distSq >= reachSq[i]) continue;

                    float dist = sqrt(distSq);
                    float localTime = ages[i] - dist / max(speeds[i], 1.0);
                    if (localTime <= 0.0) continue;

                    float2 direction = delta / max(dist, 1.0);
                    float attack = smoothstep(0.0, attackSeconds, localTime);
                    float damping = exp(-decay * localTime);
                    float distanceFade = 1.0 / (1.0 + dist / distanceDecay);
                    float ripple = sin(localTime * frequency) * attack * damping * distanceFade;

                    sampleCoord += direction * ripple * amplitudes[i] * intensity;
                }

                sampleCoord = clamp(
                    sampleCoord,
                    float2(0.0, 0.0),
                    resolution - float2(1.0, 1.0)
                );
                return content.eval(sampleCoord);
            }
        """

        private const val WAVE_SHADER = """
            uniform int waveCount;
            uniform float origins[20];
            uniform float radii[10];
            uniform float intensities[10];
            uniform float ringWidths[10];
            uniform float innerSq[10];
            uniform float outerSq[10];
            uniform float3 waveColor;

            half4 main(float2 fragCoord) {
                float brightness = 0.0;

                for (int i = 0; i < 10; i++) {
                    if (i >= waveCount) break;

                    float intensity = intensities[i];
                    if (intensity <= 0.001) continue;

                    float2 origin = float2(origins[i * 2], origins[i * 2 + 1]);
                    float2 delta = fragCoord - origin;
                    float distSq = dot(delta, delta);
                    if (distSq < innerSq[i] || distSq > outerSq[i]) continue;

                    float dist = sqrt(distSq);
                    float r = radii[i];
                    float halfRing = ringWidths[i] * 0.5;

                    float inner = r - halfRing;
                    float outer = r + halfRing;

                    float ringShape = smoothstep(inner - halfRing * 0.5, r, dist)
                                    * smoothstep(outer + halfRing * 0.5, r, dist);

                    brightness += ringShape * intensity * 0.1;
                }

                brightness = clamp(brightness, 0.0, 1.0);
                return half4(half3(waveColor) * brightness, brightness);
            }
        """
    }
}

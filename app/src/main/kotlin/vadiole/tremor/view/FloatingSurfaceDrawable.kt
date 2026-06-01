package vadiole.tremor.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import kotlin.math.abs
import kotlin.math.ceil
import vadiole.tremor.R

class FloatingSurfaceDrawable(
    context: Context,
    private val pathProvider: PathProvider? = null,
    private val surfaceColor: Int = context.getColor(R.color.floating_surface),
    pressedOverlayColor: Int = context.getColor(R.color.floating_pressed_overlay),
    private val shadowColor: Int = context.getColor(R.color.floating_shadow),
    private val borderColor: Int = context.getColor(R.color.floating_border),
) : Drawable() {
    fun interface PathProvider {
        fun update(bounds: Rect, borderInset: Float, surfacePath: Path, borderPath: Path)
    }

    interface ShapeWithCacheKey : PathProvider {
        val cacheKey: String
    }

    private val borderWidth = Floating.borderWidthPx(context)
    private val shadowRadius = Floating.SHADOW_RADIUS_DP.dp(context)
    private val shadowDy = Floating.SHADOW_DY_DP.dp(context)
    private val outsetPx: Int = ceil(shadowRadius + abs(shadowDy)).toInt()

    private val surfacePath = Path()
    private val borderPath = Path()

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = surfaceColor
        setShadowLayer(shadowRadius, 0f, shadowDy, shadowColor)
    }
    private val surfacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = surfaceColor
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = borderWidth
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        color = borderColor
    }

    private val pressedOverlayMaxAlpha = pressedOverlayColor ushr 24
    private var pressedOverlayAlpha = 0
    private var pressedOverlayTargetAlpha = 0
    private val pressedOverlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = pressedOverlayColor
        alpha = 0
    }
    private val pressedOverlayAnimator = ValueAnimator().apply {
        duration = PRESSED_OVERLAY_DURATION_MS
        addUpdateListener {
            pressedOverlayAlpha = it.animatedValue as Int
            pressedOverlayPaint.alpha = pressedOverlayAlpha
            invalidateSelf()
        }
    }

    private var cachedBitmap: Bitmap? = null

    override fun isStateful(): Boolean = true

    override fun onStateChange(state: IntArray): Boolean {
        val active = state.any {
            it == android.R.attr.state_pressed || it == android.R.attr.state_focused
        }
        val targetAlpha = if (active) pressedOverlayMaxAlpha else 0
        pressedOverlayTargetAlpha = targetAlpha
        pressedOverlayAnimator.cancel()
        if (targetAlpha == pressedOverlayAlpha) return false
        pressedOverlayAnimator.setIntValues(pressedOverlayAlpha, targetAlpha)
        pressedOverlayAnimator.start()
        return true
    }

    override fun onBoundsChange(bounds: Rect) {
        updatePaths(bounds)
        updateCachedBitmap(bounds)
    }

    private fun updatePaths(bounds: Rect) {
        surfacePath.rewind()
        borderPath.rewind()
        pathProvider?.update(bounds, borderWidth / 2f, surfacePath, borderPath)
    }

    private fun updateCachedBitmap(bounds: Rect) {
        val provider = pathProvider
        if (provider == null || bounds.width() <= 0 || bounds.height() <= 0) {
            cachedBitmap = null
            return
        }
        val shapeKey = (provider as? ShapeWithCacheKey)?.cacheKey
            ?: "instance:${System.identityHashCode(provider)}"
        val key = FloatingSurfaceCacheKey(
            width = bounds.width(),
            height = bounds.height(),
            shapeKey = shapeKey,
            surfaceColor = surfaceColor,
            borderColor = borderColor,
            shadowColor = shadowColor,
            shadowRadiusQ = (shadowRadius * 100f).toInt(),
            shadowDyQ = (shadowDy * 100f).toInt(),
            borderWidthQ = (borderWidth * 100f).toInt(),
            outsetPx = outsetPx,
        )
        cachedBitmap = FloatingSurfaceBitmapCache.get(key) { canvas ->
            renderBaseLayer(canvas, bounds.width(), bounds.height())
        }
    }

    private fun renderBaseLayer(canvas: Canvas, width: Int, height: Int) {
        val provider = pathProvider ?: return
        val localSurface = Path()
        val localBorder = Path()
        val localBounds = Rect(outsetPx, outsetPx, outsetPx + width, outsetPx + height)
        provider.update(localBounds, borderWidth / 2f, localSurface, localBorder)
        if (localSurface.isEmpty) return
        canvas.drawPath(localSurface, shadowPaint)
        canvas.drawPath(localSurface, surfacePaint)
        canvas.drawPath(localBorder, borderPaint)
    }

    override fun getOutline(outline: Outline) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !surfacePath.isEmpty) {
            outline.setPath(surfacePath)
            outline.alpha = 1f
        } else {
            outline.setEmpty()
        }
    }

    fun copySurfacePath(out: Path): Boolean {
        if (surfacePath.isEmpty) {
            out.rewind()
            return false
        }
        out.set(surfacePath)
        return true
    }

    override fun draw(canvas: Canvas) {
        val bitmap = cachedBitmap ?: return
        if (surfacePath.isEmpty) return
        canvas.drawBitmap(
            bitmap,
            (bounds.left - outsetPx).toFloat(),
            (bounds.top - outsetPx).toFloat(),
            null,
        )
        if (pressedOverlayAlpha > 0) {
            canvas.drawPath(surfacePath, pressedOverlayPaint)
        }
    }

    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun jumpToCurrentState() {
        super.jumpToCurrentState()
        pressedOverlayAnimator.cancel()
        pressedOverlayAlpha = pressedOverlayTargetAlpha
        pressedOverlayPaint.alpha = pressedOverlayAlpha
        invalidateSelf()
    }

    fun cancelAnimations() {
        pressedOverlayAnimator.cancel()
    }

    companion object {
        private const val PRESSED_OVERLAY_DURATION_MS = 120L

        /** A squircle-shaped floating surface, used by most rows and buttons. */
        fun squircleSurface(context: Context, cornerRadiusPx: Int = Int.MAX_VALUE): FloatingSurfaceDrawable =
            FloatingSurfaceDrawable(context, squircle(cornerRadiusPx))

        fun squircle(cornerRadius: Int = Int.MAX_VALUE): ShapeWithCacheKey {
            val surface = Squircle(cornerRadius)
            val border = Squircle(cornerRadius)
            return object : ShapeWithCacheKey {
                override val cacheKey: String = "sq:$cornerRadius"
                override fun update(bounds: Rect, borderInset: Float, surfacePath: Path, borderPath: Path) {
                    surface.setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom)
                    border.setBounds(
                        bounds.left + borderInset,
                        bounds.top + borderInset,
                        bounds.right - borderInset,
                        bounds.bottom - borderInset
                    )
                    surfacePath.set(surface.path)
                    borderPath.set(border.path)
                }
            }
        }

    }
}

private fun Float.dp(context: Context): Float = this * context.resources.displayMetrics.density

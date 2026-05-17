package vadiole.tremor.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import vadiole.tremor.R

class FloatingSurfaceDrawable(
    context: Context,
    private val pathProvider: PathProvider? = null,
    surfaceColor: Int = context.getColor(R.color.floating_surface),
    pressedOverlayColor: Int = context.getColor(R.color.floating_pressed_overlay),
    shadowColor: Int = context.getColor(R.color.floating_shadow),
    borderColor: Int = context.getColor(R.color.floating_border),
) : Drawable() {
    fun interface PathProvider {
        fun update(bounds: Rect, borderInset: Float, surfacePath: Path, borderPath: Path)
    }

    private val borderWidth = Floating.borderWidthPx(context)
    private val shadowRadius = Floating.SHADOW_RADIUS_DP.dp(context)
    private val shadowDy = Floating.SHADOW_DY_DP.dp(context)

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
        duration = 120L
        addUpdateListener {
            pressedOverlayAlpha = it.animatedValue as Int
            pressedOverlayPaint.alpha = pressedOverlayAlpha
            invalidateSelf()
        }
    }

    override fun isStateful(): Boolean = true

    override fun onStateChange(state: IntArray): Boolean {
        var pressed = false
        var focused = false
        state.forEach {
            when (it) {
                android.R.attr.state_pressed -> pressed = true
                android.R.attr.state_focused -> focused = true
            }
        }
        val targetAlpha = when {
            pressed || focused -> pressedOverlayMaxAlpha
            else -> 0
        }
        pressedOverlayTargetAlpha = targetAlpha
        pressedOverlayAnimator.cancel()
        if (targetAlpha == pressedOverlayAlpha) return false
        pressedOverlayAnimator.setIntValues(pressedOverlayAlpha, targetAlpha)
        pressedOverlayAnimator.start()
        return true
    }

    override fun onBoundsChange(bounds: Rect) {
        updatePaths(bounds)
    }

    private fun updatePaths(bounds: Rect) {
        surfacePath.rewind()
        borderPath.rewind()
        pathProvider?.update(bounds, borderWidth / 2f, surfacePath, borderPath)
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
        if (surfacePath.isEmpty) return
        canvas.drawPath(surfacePath, shadowPaint)
        canvas.drawPath(surfacePath, surfacePaint)
        canvas.drawPath(borderPath, borderPaint)
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
        fun squircle(cornerRadius: Int = Int.MAX_VALUE): PathProvider {
            val surface = Squircle(cornerRadius)
            val border = Squircle(cornerRadius)
            return PathProvider { bounds, borderInset, surfacePath, borderPath ->
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

private fun Float.dp(context: Context): Float = this * context.resources.displayMetrics.density

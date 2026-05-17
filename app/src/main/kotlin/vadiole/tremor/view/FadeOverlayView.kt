package vadiole.tremor.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ScrollView

/**
 * Sibling overlay that paints top/bottom fade gradients above a [ScrollView] without
 * subclassing it. Sits in viewport space, so positioning is independent of scroll.
 * Avoids android.view.View's saveLayerAlpha-based fading edges, which force a
 * fullscreen offscreen render target every frame the SV draws.
 */
class FadeOverlayView(
    context: Context,
    private val scrollView: ScrollView,
    private val fadeColor: Int,
    private val fadeLengthPx: Int,
) : View(context) {

    private val fadePaint = Paint()
    private var topGradient: LinearGradient? = null
    private var bottomGradient: LinearGradient? = null
    private val scrollListener = ViewTreeObserver.OnScrollChangedListener { invalidate() }

    init {
        isClickable = false
        isFocusable = false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewTreeObserver.addOnScrollChangedListener(scrollListener)
    }

    override fun onDetachedFromWindow() {
        viewTreeObserver.removeOnScrollChangedListener(scrollListener)
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (h <= 0 || fadeLengthPx <= 0) {
            topGradient = null
            bottomGradient = null
            return
        }
        val opaque = fadeColor or 0xFF000000.toInt()
        val transparent = fadeColor and 0x00FFFFFF
        topGradient = LinearGradient(
            0f, 0f, 0f, fadeLengthPx.toFloat(),
            opaque, transparent, Shader.TileMode.CLAMP,
        )
        bottomGradient = LinearGradient(
            0f, (h - fadeLengthPx).toFloat(), 0f, h.toFloat(),
            transparent, opaque, Shader.TileMode.CLAMP,
        )
    }

    override fun onDraw(canvas: Canvas) {
        if (fadeLengthPx <= 0) return
        val sy = scrollView.scrollY
        val range = scrollView.getChildAt(0)?.bottom ?: 0
        val maxScroll = (range - scrollView.height).coerceAtLeast(0)
        val topStrength = (sy.toFloat() / fadeLengthPx).coerceIn(0f, 1f)
        val bottomStrength = ((maxScroll - sy).toFloat() / fadeLengthPx).coerceIn(0f, 1f)

        val topG = topGradient
        if (topStrength > 0.001f && topG != null) {
            fadePaint.shader = topG
            fadePaint.alpha = (topStrength * 255f).toInt()
            canvas.drawRect(0f, 0f, width.toFloat(), fadeLengthPx.toFloat(), fadePaint)
        }
        val bottomG = bottomGradient
        if (bottomStrength > 0.001f && bottomG != null) {
            fadePaint.shader = bottomG
            fadePaint.alpha = (bottomStrength * 255f).toInt()
            canvas.drawRect(0f, (height - fadeLengthPx).toFloat(), width.toFloat(), height.toFloat(), fadePaint)
        }
    }
}

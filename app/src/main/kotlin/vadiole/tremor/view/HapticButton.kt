package vadiole.tremor.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import vadiole.tremor.Density
import vadiole.tremor.R
import vadiole.tremor.UiConstants
import vadiole.tremor.animatePress
import vadiole.tremor.animateRelease

class HapticButton(
    context: Context,
    private val label: String,
    private val constantName: String,
    private val onTrigger: (screenX: Float, screenY: Float) -> Unit,
) : View(context), Density {

    private val cornerRadius = UiConstants.CORNER_RADIUS_DP.dp()
    private val minHeight = 56.dp()
    private val horizontalPadding = 8f.dp()
    private val verticalPadding = 8f.dp()

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.surface)
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.border)
        style = Paint.Style.STROKE
        strokeWidth = 1f.dp()
    }

    private val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.foreground)
        textSize = 13f.dp()
        typeface = Typeface.MONOSPACE
        isSubpixelText = true
    }

    private val constantPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.text_secondary)
        textSize = 8f.dp()
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
        isSubpixelText = true
    }

    private val pressedColor = context.getColor(R.color.surface_pressed)
    private val normalColor = context.getColor(R.color.surface)

    private val constantFm = constantPaint.fontMetrics
    private val constantTextHeight = constantFm.descent - constantFm.ascent
    private val gap = 3f.dp()

    private val rect = RectF()
    private val location = IntArray(2)
    private var labelLayout: StaticLayout? = null
    private var truncatedConstant: String = constantName

    init {
        isClickable = true
        isFocusable = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val maxTextWidth = (width - horizontalPadding * 2).toInt().coerceAtLeast(1)

        labelLayout = StaticLayout.Builder.obtain(label, 0, label.length, labelPaint, maxTextWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setMaxLines(2)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()

        truncatedConstant = TextUtils.ellipsize(
            constantName, constantPaint, width - horizontalPadding * 2, TextUtils.TruncateAt.END,
        ).toString()

        val labelHeight = labelLayout!!.height
        val totalContentHeight = labelHeight + gap + constantTextHeight
        val height = maxOf(minHeight, (totalContentHeight + verticalPadding * 2).toInt())
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        val halfStroke = borderPaint.strokeWidth / 2f
        rect.set(halfStroke, halfStroke, width - halfStroke, height - halfStroke)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        val layout = labelLayout ?: return
        val centerX = width / 2f
        val maxTextWidth = (width - horizontalPadding * 2).toInt()
        val totalContentHeight = layout.height + gap + constantTextHeight

        // label (StaticLayout, centered)
        val labelTop = (height - totalContentHeight) / 2f
        canvas.save()
        canvas.translate(centerX - maxTextWidth / 2f, labelTop)
        layout.draw(canvas)
        canvas.restore()

        // constant name (single line, baseline from font metrics)
        val constantY = labelTop + layout.height + gap - constantFm.ascent
        canvas.drawText(truncatedConstant, centerX, constantY, constantPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                bgPaint.color = pressedColor
                invalidate()
                animatePress()
                getLocationOnScreen(location)
                val screenX = location[0] + event.x
                val screenY = location[1] + event.y
                onTrigger(screenX, screenY)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                bgPaint.color = normalColor
                invalidate()
                animateRelease()
            }
        }
        return true
    }
}

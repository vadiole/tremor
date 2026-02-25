package vadiole.tremor.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import vadiole.tremor.Density
import vadiole.tremor.R

class HapticButton(
    context: Context,
    private val label: String,
    private val constantName: String,
    private val onTrigger: (screenX: Float, screenY: Float) -> Unit,
) : View(context), Density {

    private val cornerRadius = 6f.dp()
    private val minHeight = 56.dp()
    private val horizontalPadding = 8f.dp()

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
        textAlign = Paint.Align.CENTER
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

    private val rect = RectF()
    private val location = IntArray(2)

    init {
        isClickable = true
        isFocusable = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, minHeight)
    }

    override fun onDraw(canvas: Canvas) {
        val halfStroke = borderPaint.strokeWidth / 2f
        rect.set(halfStroke, halfStroke, width - halfStroke, height - halfStroke)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        val centerX = width / 2f
        val maxTextWidth = width - horizontalPadding * 2
        val labelY = height / 2f - 3f.dp()
        val constantY = labelY + 13f.dp()

        val truncatedLabel = TextUtils.ellipsize(label, labelPaint, maxTextWidth, TextUtils.TruncateAt.END).toString()
        val truncatedConstant = TextUtils.ellipsize(constantName, constantPaint, maxTextWidth, TextUtils.TruncateAt.END).toString()

        canvas.drawText(truncatedLabel, centerX, labelY, labelPaint)
        canvas.drawText(truncatedConstant, centerX, constantY, constantPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                bgPaint.color = pressedColor
                invalidate()
                animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).start()
                getLocationOnScreen(location)
                val screenX = location[0] + event.x
                val screenY = location[1] + event.y
                onTrigger(screenX, screenY)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                bgPaint.color = normalColor
                invalidate()
                animate().scaleX(1f).scaleY(1f).setDuration(150)
                    .setInterpolator(android.view.animation.OvershootInterpolator(2f)).start()
            }
        }
        return true
    }
}

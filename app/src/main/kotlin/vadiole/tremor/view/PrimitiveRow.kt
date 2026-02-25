package vadiole.tremor.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.ViewGroup
import vadiole.tremor.Density
import vadiole.tremor.R

class PrimitiveRow(
    context: Context,
    private val label: String,
    private val constantName: String,
    private val onTrigger: (scale: Float, screenX: Float, screenY: Float) -> Unit,
) : ViewGroup(context), Density {

    private val rowHeight = 64.dp()
    private val cornerRadius = 6f.dp()
    private val padding = 12.dp()
    private val drumMarginStart = 8.dp()

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.surface)
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.border)
        style = Paint.Style.STROKE
        strokeWidth = 1f.dp()
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.foreground)
        textSize = 13f.dp()
        typeface = Typeface.MONOSPACE
    }

    private val constantPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.text_secondary)
        textSize = 9f.dp()
        typeface = Typeface.MONOSPACE
    }

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.text_secondary)
        textSize = 11f.dp()
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.RIGHT
    }

    private val pressedColor = context.getColor(R.color.surface_pressed)
    private val normalColor = context.getColor(R.color.surface)

    private val rect = RectF()
    private val location = IntArray(2)

    val drum = DrumRollerView(context).also { addView(it) }

    init {
        setWillNotDraw(false)
        drum.onValueChanged = { invalidate() }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        drum.measure(
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
        )
        setMeasuredDimension(width, rowHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val drumW = drum.measuredWidth
        val drumH = drum.measuredHeight
        val drumLeft = (r - l) - drumW
        val drumTop = (rowHeight - drumH) / 2
        drum.layout(drumLeft, drumTop, drumLeft + drumW, drumTop + drumH)
    }

    override fun onDraw(canvas: Canvas) {
        val halfStroke = borderPaint.strokeWidth / 2f
        rect.set(halfStroke, halfStroke, width - halfStroke, height - halfStroke)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        val labelX = padding.toFloat()
        val labelY = height / 2f - 2f.dp()
        val constantY = labelY + 12f.dp()
        canvas.drawText(label, labelX, labelY, labelPaint)
        canvas.drawText(constantName, labelX, constantY, constantPaint)

        val valueText = String.format("%.2f", drum.value)
        val valueX = drum.left.toFloat() - drumMarginStart
        val valueY = height / 2f + valuePaint.textSize / 3f
        canvas.drawText(valueText, valueX, valueY, valuePaint)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (isDrumTouch(ev)) return false
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                bgPaint.color = pressedColor
                invalidate()
                animate().scaleX(0.98f).scaleY(0.98f).setDuration(80).start()
                getLocationOnScreen(location)
                val screenX = location[0] + event.x
                val screenY = location[1] + event.y
                onTrigger(drum.value, screenX, screenY)
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

    private fun isDrumTouch(ev: MotionEvent): Boolean {
        val x = ev.x.toInt()
        val y = ev.y.toInt()
        return x >= drum.left && x <= drum.right && y >= drum.top && y <= drum.bottom
    }
}

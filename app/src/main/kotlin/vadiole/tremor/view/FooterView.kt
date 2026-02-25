package vadiole.tremor.view

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.TextPaint
import android.view.MotionEvent
import android.view.View
import vadiole.tremor.Density
import vadiole.tremor.R

class FooterView(
    context: Context,
    private val onLongPress: (screenX: Float, screenY: Float) -> Unit,
) : View(context), Density {

    private val viewPadding = 8.dp()
    private val longPressDelay = 500L
    private val linkColor = context.getColor(R.color.text_disabled)
    private val textColor = context.getColor(R.color.text_disabled)

    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = 10f.dp()
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
        isSubpixelText = true
    }

    private val linkText = context.getString(R.string.footer_vadiole)
    private val fullText = context.getString(R.string.footer_template, linkText)
    private val linkStart = fullText.indexOf(linkText)
    private val linkEnd = linkStart + linkText.length

    private var isLinkPressed = false

    private val prefix = fullText.substring(0, linkStart)
    private val link = fullText.substring(linkStart, linkEnd)
    private val suffix = fullText.substring(linkEnd)
    private val prefixWidth = textPaint.measureText(prefix)
    private val linkWidth = textPaint.measureText(link)
    private val suffixWidth = textPaint.measureText(suffix)
    private val totalTextWidth = prefixWidth + linkWidth + suffixWidth

    private val handler = Handler(Looper.getMainLooper())
    private var longPressTriggered = false
    private var downX = 0f
    private var downY = 0f
    private val location = IntArray(2)

    private val storeUrl = "https://play.google.com/store/apps/dev?id=4763171503902347202"

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val textHeight = (textPaint.descent() - textPaint.ascent()).toInt()
        setMeasuredDimension(w, textHeight + viewPadding * 2)
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val fm = textPaint.fontMetrics
        val textY = cy - (fm.ascent + fm.descent) / 2f
        val startX = cx - totalTextWidth / 2f

        val saved = textPaint.color
        val savedUnderline = textPaint.isUnderlineText

        textPaint.color = textColor
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.isUnderlineText = false
        canvas.drawText(prefix, startX, textY, textPaint)

        textPaint.color = linkColor
        textPaint.isUnderlineText = true
        if (isLinkPressed) textPaint.alpha = 128
        canvas.drawText(link, startX + prefixWidth, textY, textPaint)

        textPaint.color = textColor
        textPaint.isUnderlineText = false
        textPaint.alpha = 255
        canvas.drawText(suffix, startX + prefixWidth + linkWidth, textY, textPaint)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = saved
        textPaint.isUnderlineText = savedUnderline
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isLinkPressed = true
                longPressTriggered = false
                downX = event.x
                downY = event.y
                invalidate()
                handler.postDelayed({
                    if (isLinkPressed) {
                        longPressTriggered = true
                        getLocationOnScreen(location)
                        onLongPress(location[0] + downX, location[1] + downY)
                    }
                }, longPressDelay)
            }
            MotionEvent.ACTION_UP -> {
                isLinkPressed = false
                invalidate()
                handler.removeCallbacksAndMessages(null)
                if (!longPressTriggered) {
                    openStore()
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                isLinkPressed = false
                invalidate()
                handler.removeCallbacksAndMessages(null)
            }
        }
        return true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacksAndMessages(null)
    }

    private fun openStore() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(storeUrl))
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }
}

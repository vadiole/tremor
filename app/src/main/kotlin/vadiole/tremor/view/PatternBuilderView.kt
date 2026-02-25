package vadiole.tremor.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import vadiole.tremor.HapticEngine
import vadiole.tremor.R

class PatternBuilderView(
    context: Context,
    private val supportedPrimitives: List<HapticEngine.PrimitiveInfo>,
    private val onPlay: (entries: List<HapticEngine.PatternEntry>, screenX: Float, screenY: Float) -> Unit,
) : LinearLayout(context) {

    private val density = resources.displayMetrics.density
    private val entries = mutableListOf<PatternEntryData>()
    private val entryContainer = LinearLayout(context).apply {
        orientation = VERTICAL
    }
    private val maxEntries = 5

    private val addButton = AddButton(context) {
        showPrimitivePopup(it)
    }

    private val playButton = PlayButton(context) { screenX, screenY ->
        val patternEntries = entries.map { entry ->
            HapticEngine.PatternEntry(
                primitiveId = entry.info.primitiveId,
                name = entry.info.name,
                scale = entry.scaleDrum.value,
                delayMs = (entry.delayDrum.value).toInt(),
            )
        }
        onPlay(patternEntries, screenX, screenY)
    }

    init {
        orientation = VERTICAL
        addView(entryContainer)
        addView(addButton, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = (8 * density).toInt()
        })
        addView(playButton, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = (12 * density).toInt()
            gravity = Gravity.CENTER_HORIZONTAL
        })
        updatePlayButton()
    }

    private fun addEntry(info: HapticEngine.PrimitiveInfo) {
        val entryData = PatternEntryData(info, context)
        entries.add(entryData)
        entryContainer.addView(entryData.row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = if (entryContainer.childCount > 1) (4 * density).toInt() else 0
        })
        updateAddButton()
        updatePlayButton()
    }

    private fun removeEntry(entryData: PatternEntryData) {
        entryContainer.removeView(entryData.row)
        entries.remove(entryData)
        updateAddButton()
        updatePlayButton()
    }

    private fun updateAddButton() {
        addButton.visibility = if (entries.size >= maxEntries) GONE else VISIBLE
    }

    private fun updatePlayButton() {
        playButton.alpha = if (entries.isEmpty()) 0.3f else 1f
        playButton.isEnabled = entries.isNotEmpty()
    }

    private fun showPrimitivePopup(anchor: View) {
        val popupContent = LinearLayout(context).apply {
            orientation = VERTICAL
            setBackgroundColor(context.getColor(R.color.surface))
            setPadding(
                (4 * density).toInt(),
                (4 * density).toInt(),
                (4 * density).toInt(),
                (4 * density).toInt(),
            )
        }

        val popup = PopupWindow(
            popupContent,
            (200 * density).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true,
        )
        popup.elevation = 8f * density

        for (primitive in supportedPrimitives) {
            val item = TextView(context).apply {
                text = primitive.name
                setTextColor(context.getColor(R.color.foreground))
                textSize = 13f
                typeface = Typeface.MONOSPACE
                setPadding(
                    (12 * density).toInt(),
                    (10 * density).toInt(),
                    (12 * density).toInt(),
                    (10 * density).toInt(),
                )
                setOnClickListener {
                    addEntry(primitive)
                    popup.dismiss()
                }
            }
            popupContent.addView(item)
        }

        popup.showAsDropDown(anchor, 0, (4 * density).toInt())
    }

    private inner class PatternEntryData(
        val info: HapticEngine.PrimitiveInfo,
        context: Context,
    ) {
        val scaleDrum = DrumRollerView(context, minValue = 0f, maxValue = 1f, step = 0.05f)
        val delayDrum = DrumRollerView(context, minValue = 0f, maxValue = 500f, step = 10f)
        val row = PatternEntryRow(context, info.name, scaleDrum, delayDrum) {
            removeEntry(this)
        }

        init {
            scaleDrum.onValueChanged = { row.invalidate() }
            delayDrum.onValueChanged = { row.invalidate() }
        }
    }

    private class PatternEntryRow(
        context: Context,
        private val label: String,
        private val scaleDrum: DrumRollerView,
        private val delayDrum: DrumRollerView,
        private val onRemove: () -> Unit,
    ) : ViewGroup(context) {

        private val density = resources.displayMetrics.density
        private val rowHeight = (48 * density).toInt()
        private val padding = (10 * density).toInt()
        private val cornerRadius = 4f * density
        private val removeSize = (24 * density).toInt()

        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.getColor(R.color.background)
            style = Paint.Style.FILL
        }

        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.getColor(R.color.border)
            style = Paint.Style.STROKE
            strokeWidth = 1f * density
        }

        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.getColor(R.color.foreground)
            textSize = 11f * density
            typeface = Typeface.MONOSPACE
        }

        private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.getColor(R.color.text_secondary)
            textSize = 10f * density
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.RIGHT
        }

        private val removePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.getColor(R.color.text_disabled)
            strokeWidth = 1.5f * density
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        private val rect = RectF()

        init {
            setWillNotDraw(false)
            addView(scaleDrum)
            addView(delayDrum)
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            scaleDrum.measure(
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            )
            delayDrum.measure(
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            )
            setMeasuredDimension(width, rowHeight)
        }

        override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
            val w = r - l
            val removeRight = w - padding
            val removeLeft = removeRight - removeSize

            val delayDrumRight = removeLeft - (8 * density).toInt()
            val delayDrumLeft = delayDrumRight - delayDrum.measuredWidth
            val delayTop = (rowHeight - delayDrum.measuredHeight) / 2
            delayDrum.layout(delayDrumLeft, delayTop, delayDrumRight, delayTop + delayDrum.measuredHeight)

            val scaleDrumRight = delayDrumLeft - (40 * density).toInt()
            val scaleDrumLeft = scaleDrumRight - scaleDrum.measuredWidth
            val scaleTop = (rowHeight - scaleDrum.measuredHeight) / 2
            scaleDrum.layout(scaleDrumLeft, scaleTop, scaleDrumRight, scaleTop + scaleDrum.measuredHeight)
        }

        override fun onDraw(canvas: Canvas) {
            rect.set(0f, 0f, width.toFloat(), height.toFloat())
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

            val textY = height / 2f + textPaint.textSize / 3f
            canvas.drawText(label, padding.toFloat(), textY, textPaint)

            val scaleText = String.format("%.2f", scaleDrum.value)
            canvas.drawText(scaleText, scaleDrum.left.toFloat() - 4 * density, textY, valuePaint)

            val delayText = "${delayDrum.value.toInt()}ms"
            canvas.drawText(delayText, delayDrum.left.toFloat() - 4 * density, textY, valuePaint)

            val removeRight = width - padding
            val removeCx = removeRight - removeSize / 2f
            val removeCy = height / 2f
            val arm = 5f * density
            canvas.drawLine(removeCx - arm, removeCy - arm, removeCx + arm, removeCy + arm, removePaint)
            canvas.drawLine(removeCx - arm, removeCy + arm, removeCx + arm, removeCy - arm, removePaint)
        }

        override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
            val x = ev.x.toInt()
            val y = ev.y.toInt()
            if (x >= scaleDrum.left && x <= scaleDrum.right && y >= scaleDrum.top && y <= scaleDrum.bottom) return false
            if (x >= delayDrum.left && x <= delayDrum.right && y >= delayDrum.top && y <= delayDrum.bottom) return false
            return true
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_UP) {
                val removeRight = width - padding
                val removeLeft = removeRight - removeSize
                val x = event.x.toInt()
                if (x in removeLeft..removeRight) {
                    onRemove()
                    return true
                }
            }
            return true
        }
    }

    private class AddButton(
        context: Context,
        private val onClick: (view: View) -> Unit,
    ) : View(context) {

        private val density = resources.displayMetrics.density
        private val btnHeight = (40 * density).toInt()
        private val cornerRadius = 4f * density

        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.getColor(R.color.border)
            style = Paint.Style.STROKE
            strokeWidth = 1f * density
            pathEffect = DashPathEffect(floatArrayOf(6f * density, 4f * density), 0f)
        }

        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.getColor(R.color.text_muted)
            textSize = 12f * density
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
        }

        private val rect = RectF()

        init {
            setOnClickListener { onClick(this) }
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            setMeasuredDimension(width, btnHeight)
        }

        override fun onDraw(canvas: Canvas) {
            rect.set(0f, 0f, width.toFloat(), height.toFloat())
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
            canvas.drawText("+ ADD", width / 2f, height / 2f + textPaint.textSize / 3f, textPaint)
        }
    }

    private class PlayButton(
        context: Context,
        private val onPlay: (screenX: Float, screenY: Float) -> Unit,
    ) : View(context) {

        private val density = resources.displayMetrics.density
        private val btnHeight = (48 * density).toInt()
        private val btnWidth = (120 * density).toInt()
        private val cornerRadius = 6f * density

        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.getColor(R.color.foreground)
            style = Paint.Style.STROKE
            strokeWidth = 1f * density
        }

        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.getColor(R.color.foreground)
            textSize = 13f * density
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
        }

        private val rect = RectF()
        private val location = IntArray(2)

        init {
            isClickable = true
            isFocusable = true
            setOnClickListener {
                getLocationOnScreen(location)
                onPlay(
                    location[0] + width / 2f,
                    location[1] + height / 2f,
                )
            }
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            setMeasuredDimension(btnWidth, btnHeight)
        }

        override fun onDraw(canvas: Canvas) {
            rect.set(0f, 0f, width.toFloat(), height.toFloat())
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
            canvas.drawText("\u25B6 PLAY", width / 2f, height / 2f + textPaint.textSize / 3f, textPaint)
        }
    }
}

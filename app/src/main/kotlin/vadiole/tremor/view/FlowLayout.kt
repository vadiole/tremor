package vadiole.tremor.view

import android.content.Context
import android.view.View
import android.view.ViewGroup

class FlowLayout(
    context: Context,
    private val columns: Int = 3,
    private val horizontalGap: Int,
    private val verticalGap: Int,
) : ViewGroup(context) {

    private var rowHeights = IntArray(0)

    init {
        clipChildren = false
        clipToPadding = false
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val availableWidth = MeasureSpec.getSize(widthMeasureSpec)
        val totalGaps = (columns - 1) * horizontalGap
        val childWidth = (availableWidth - totalGaps) / columns
        val childWidthSpec = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY)

        var totalHeight = 0
        var rowHeight = 0
        var col = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE) continue
            child.measure(childWidthSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
            if (col == columns) {
                totalHeight += rowHeight + verticalGap
                rowHeight = 0
                col = 0
            }
            rowHeight = maxOf(rowHeight, child.measuredHeight)
            col++
        }
        totalHeight += rowHeight

        setMeasuredDimension(availableWidth, totalHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val availableWidth = r - l
        val totalGaps = (columns - 1) * horizontalGap
        val childWidth = (availableWidth - totalGaps) / columns

        // first pass: compute row heights
        val maxRows = (childCount + columns - 1) / columns
        if (rowHeights.size < maxRows) {
            rowHeights = IntArray(maxRows)
        }
        var rowCount = 0
        var rowHeight = 0
        var col = 0
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE) continue
            if (col == columns) {
                rowHeights[rowCount++] = rowHeight
                rowHeight = 0
                col = 0
            }
            rowHeight = maxOf(rowHeight, child.measuredHeight)
            col++
        }
        rowHeights[rowCount] = rowHeight

        // second pass: layout with equal row heights
        var x = 0
        var y = 0
        var rowIndex = 0
        col = 0
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE) continue
            if (col == columns) {
                y += rowHeights[rowIndex] + verticalGap
                x = 0
                rowIndex++
                col = 0
            }
            child.layout(x, y, x + childWidth, y + rowHeights[rowIndex])
            x += childWidth + horizontalGap
            col++
        }
    }
}

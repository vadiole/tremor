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

        var x = 0
        var y = 0
        var rowHeight = 0
        var col = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE) continue
            if (col == columns) {
                y += rowHeight + verticalGap
                x = 0
                rowHeight = 0
                col = 0
            }
            child.layout(x, y, x + childWidth, y + child.measuredHeight)
            x += childWidth + horizontalGap
            rowHeight = maxOf(rowHeight, child.measuredHeight)
            col++
        }
    }
}

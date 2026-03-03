package vadiole.tremor.view

import android.content.Context
import android.view.ViewGroup
import vadiole.tremor.Density

class KeyboardRowView(context: Context) : ViewGroup(context), Density {

    private val keys = charArrayOf('Q', 'W', 'E', 'R', 'T', 'Y')
    private val keyGap = 6.dp
    private val viewHeight = 56.dp

    init {
        clipChildren = false
        clipToPadding = false
        for (key in keys) {
            addView(KeyButton(context, key))
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val totalGaps = (keys.size - 1) * keyGap
        val keyWidth = (width - totalGaps) / keys.size
        val keyWidthSpec = MeasureSpec.makeMeasureSpec(keyWidth, MeasureSpec.EXACTLY)
        val keyHeightSpec = MeasureSpec.makeMeasureSpec(viewHeight, MeasureSpec.EXACTLY)

        for (i in 0 until childCount) {
            getChildAt(i).measure(keyWidthSpec, keyHeightSpec)
        }
        setMeasuredDimension(width, viewHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val totalGaps = (keys.size - 1) * keyGap
        val keyWidth = (r - l - totalGaps) / keys.size
        var x = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.layout(x, 0, x + keyWidth, viewHeight)
            x += keyWidth + keyGap
        }
    }
}

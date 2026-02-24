package vadiole.tremor

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView

class TremorActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content = FrameLayout(this).apply {
            setBackgroundColor(getColor(R.color.background))
        }
        val textView = TextView(this).apply {
            text = "Tremor"
            setTextColor(getColor(R.color.foreground))
            textSize = 20f
            gravity = Gravity.CENTER
        }
        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER,
        )
        content.addView(textView, lp)
        setContentView(content)
    }
}

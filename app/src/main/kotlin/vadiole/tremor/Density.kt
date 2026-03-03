package vadiole.tremor

import android.content.res.Resources

interface Density {
    fun getResources(): Resources

    val Float.dp: Float get() = this * getResources().displayMetrics.density
    val Int.dp: Int get() = (this * getResources().displayMetrics.density).toInt()

    val Float.sp: Float get() = this * getResources().displayMetrics.scaledDensity
    val Int.sp: Int get() = (this * getResources().displayMetrics.scaledDensity).toInt()
}

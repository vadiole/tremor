package vadiole.tremor

import android.content.res.Resources

interface Density {
    fun getResources(): Resources

    fun Float.dp(): Float = this * getResources().displayMetrics.density
    fun Int.dp(): Int = (this * getResources().displayMetrics.density).toInt()
}

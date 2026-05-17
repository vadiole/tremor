package vadiole.tremor.view

import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.LruCache

internal data class FloatingSurfaceCacheKey(
    val width: Int,
    val height: Int,
    val shapeKey: String,
    val surfaceColor: Int,
    val borderColor: Int,
    val shadowColor: Int,
    val shadowRadiusQ: Int,
    val shadowDyQ: Int,
    val borderWidthQ: Int,
    val outsetPx: Int,
)

internal object FloatingSurfaceBitmapCache {
    private const val MAX_BYTES = 8 * 1024 * 1024

    private val cache = object : LruCache<FloatingSurfaceCacheKey, Bitmap>(MAX_BYTES) {
        override fun sizeOf(key: FloatingSurfaceCacheKey, value: Bitmap): Int =
            value.allocationByteCount
    }

    fun get(key: FloatingSurfaceCacheKey, render: (Canvas) -> Unit): Bitmap {
        cache.get(key)?.let { return it }
        val bmpWidth = key.width + key.outsetPx * 2
        val bmpHeight = key.height + key.outsetPx * 2
        val bitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
        render(Canvas(bitmap))
        cache.put(key, bitmap)
        return bitmap
    }
}

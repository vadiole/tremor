package vadiole.tremor.view

import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider

private object EmptyNativeShadowOutlineProvider : ViewOutlineProvider() {
    override fun getOutline(view: View, outline: Outline) {
        outline.setEmpty()
    }
}

fun View.keepFloatingSurfaceShadowOnly() {
    outlineProvider = EmptyNativeShadowOutlineProvider
}

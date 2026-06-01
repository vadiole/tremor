package vadiole.tremor.view

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import vadiole.tremor.Density
import vadiole.tremor.R

/**
 * Debug-only live tuning panel for [BallBoxView]. Each row is a [DebugSlider] bound to a field of
 * [BallBoxTuning]; the ball reads those fields every frame, so changes are felt immediately with no
 * rebuild. RESET in the header restores every slider to its launch-time default. Shown in every
 * build except the Play release (see TremorActivity), so the short human labels here stay out of
 * strings.xml.
 */
class BallBoxDebugPanel(context: Context) : LinearLayout(context), Density {

    private val sliders = ArrayList<DebugSlider>()

    init {
        orientation = VERTICAL

        header()

        subhead("haptics · feel")
        slider("grab", "strength 0..1", 0f, 1f, { BallBoxTuning.grabScale }) { BallBoxTuning.grabScale = it }
        slider("drop", "strength 0..1", 0f, 1f, { BallBoxTuning.dropScale }) { BallBoxTuning.dropScale = it }
        slider("settle", "snap-in strength", 0f, 1f, { BallBoxTuning.settleScale }) { BallBoxTuning.settleScale = it }
        slider("pop", "escape strength", 0f, 1f, { BallBoxTuning.popScale }) { BallBoxTuning.popScale = it }
        slider("crack effect", "0 low · 1 tick · 2 click", 0f, 2f, { BallBoxTuning.crackPrimitive }) { BallBoxTuning.crackPrimitive = it }
        slider("crack base", "strength, low tension", 0f, 1f, { BallBoxTuning.crackScaleBase }) { BallBoxTuning.crackScaleBase = it }
        slider("crack ramp", "extra, at limit", 0f, 1f, { BallBoxTuning.crackScaleRamp }) { BallBoxTuning.crackScaleRamp = it }
        slider("crack spacing", "dp per tick", 2f, 30f, { BallBoxTuning.crackSpacing }) { BallBoxTuning.crackSpacing = it }
        slider("crack interval", "min gap, ms", 0f, 150f, { BallBoxTuning.crackInterval }) { BallBoxTuning.crackInterval = it }
        slider("bounce soft", "min strength 0..1", 0f, 1f, { BallBoxTuning.bounceMinScale }) { BallBoxTuning.bounceMinScale = it }
        slider("bounce hard", "max strength 0..1", 0f, 1f, { BallBoxTuning.bounceMaxScale }) { BallBoxTuning.bounceMaxScale = it }
        slider("bounce floor", "silent below, dp/s", 0f, 400f, { BallBoxTuning.minBounceSpeed }) { BallBoxTuning.minBounceSpeed = it }
        slider("bounce full", "hardest at, dp/s", 500f, 5000f, { BallBoxTuning.bounceRefSpeed }) { BallBoxTuning.bounceRefSpeed = it }

        subhead("physics · motion")
        slider("friction", "slowdown rate", 0f, 4f, { BallBoxTuning.friction }) { BallBoxTuning.friction = it }
        slider("restitution", "wall bounciness 0..1", 0f, 1f, { BallBoxTuning.restitution }) { BallBoxTuning.restitution = it }
        slider("scroll kick", "react to scrolling", 0f, 1.5f, { BallBoxTuning.scrollCoupling }) { BallBoxTuning.scrollCoupling = it }
        slider("drag damping", "rubber-band stiffness", 0f, 4f, { BallBoxTuning.dragDamping }) { BallBoxTuning.dragDamping = it }
        slider("launch gain", "power per stretch", 0f, 20f, { BallBoxTuning.launchGain }) { BallBoxTuning.launchGain = it }
        slider("launch cap", "max shot, dp/s", 200f, 3000f, { BallBoxTuning.launchMaxSpeed }) { BallBoxTuning.launchMaxSpeed = it }
        slider("magnet force", "pull strength", 0f, 8f, { BallBoxTuning.attractK }) { BallBoxTuning.attractK = it }
        slider("magnet range", "reach, dp", 0f, 120f, { BallBoxTuning.attractionRadius }) { BallBoxTuning.attractionRadius = it }
        slider("magnet cutoff", "off above, dp/s", 0f, 1500f, { BallBoxTuning.attractionMaxSpeed }) { BallBoxTuning.attractionMaxSpeed = it }
        slider("capture", "latch below, dp/s", 0f, 400f, { BallBoxTuning.captureSpeed }) { BallBoxTuning.captureSpeed = it }
        slider("socket spring", "stiffness", 0f, 600f, { BallBoxTuning.holdStiffness }) { BallBoxTuning.holdStiffness = it }
        slider("socket damping", "wobble decay", 0f, 40f, { BallBoxTuning.holdDamping }) { BallBoxTuning.holdDamping = it }
        slider("escape speed", "break free, dp/s", 0f, 2000f, { BallBoxTuning.escapeSpeed }) { BallBoxTuning.escapeSpeed = it }
        slider("escape dist", "break free, × radius", 1f, 4f, { BallBoxTuning.escapeRadiusMul }) { BallBoxTuning.escapeRadiusMul = it }

        subhead("grab · handling")
        slider("grab radius", "touch margin, dp", 0f, 120f, { BallBoxTuning.grabPadding }) { BallBoxTuning.grabPadding = it }
        slider("grab follow", "glide to finger", 0f, 40f, { BallBoxTuning.grabAttract }) { BallBoxTuning.grabAttract = it }
        slider("grab lift", "above finger, dp", 0f, 120f, { BallBoxTuning.grabLift }) { BallBoxTuning.grabLift = it }
    }

    private fun slider(name: String, hint: String, min: Float, max: Float, get: () -> Float, set: (Float) -> Unit) {
        val s = DebugSlider(context, name, hint, min, max, get(), set)
        sliders.add(s)
        addView(s, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    private fun header() {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(context).apply {
            text = "ball box · debug".uppercase()
            setTextColor(context.getColor(R.color.text_muted))
            textSize = 11f
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.05f
        }
        val reset = TextView(context).apply {
            text = "reset".uppercase()
            setTextColor(context.getColor(R.color.foreground))
            textSize = 11f
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.05f
            isClickable = true
            setPadding(12.dp, 6.dp, 4.dp, 6.dp)
            setOnClickListener { sliders.forEach { it.resetToDefault() } }
        }
        row.addView(title, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        row.addView(reset, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        lp.topMargin = 8.dp
        lp.bottomMargin = 4.dp
        addView(row, lp)
    }

    private fun subhead(text: String) {
        addView(
            TextView(context).apply {
                this.text = text
                setTextColor(context.getColor(R.color.text_muted))
                textSize = 10f
                typeface = Typeface.MONOSPACE
                val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                lp.topMargin = 10.dp
                lp.bottomMargin = 2.dp
                layoutParams = lp
            },
        )
    }
}

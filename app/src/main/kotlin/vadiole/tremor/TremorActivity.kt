package vadiole.tremor

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView
import vadiole.tremor.view.FlowLayout
import vadiole.tremor.view.HapticButton
import vadiole.tremor.view.PrimitiveRow
import vadiole.tremor.view.WaveOverlayView

class TremorActivity : Activity() {

    private lateinit var hapticEngine: HapticEngine
    private lateinit var waveOverlay: WaveOverlayView
    private var bannerView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        hapticEngine = HapticEngine(this)

        val density = resources.displayMetrics.density
        val padding = (16 * density).toInt()
        val sectionSpacing = (24 * density).toInt()
        val itemSpacing = (8 * density).toInt()

        val root = FrameLayout(this).apply {
            setBackgroundColor(getColor(R.color.background))
        }

        val scrollView = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            clipToPadding = false
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
            setOnApplyWindowInsetsListener { v, insets ->
                val systemBars = insets.getInsets(WindowInsets.Type.systemBars())
                v.setPadding(padding, padding + systemBars.top, padding, padding + systemBars.bottom)
                insets
            }
        }

        buildHapticFeedbackSection(content, density, sectionSpacing, itemSpacing)
        buildPredefinedEffectsSection(content, density, sectionSpacing, itemSpacing)
        buildPrimitivesSection(content, density, sectionSpacing, itemSpacing)
        buildDeviceInfo(content, density, sectionSpacing)

        scrollView.addView(content, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
        ))

        root.addView(scrollView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ))

        waveOverlay = WaveOverlayView(this).apply {
            isClickable = false
            isFocusable = false
        }
        root.addView(waveOverlay, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ))

        buildBanner(root, density)

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        updateBanner()
    }

    override fun onPause() {
        super.onPause()
        hapticEngine.cancel()
        waveOverlay.clearWaves()
    }

    private fun buildHapticFeedbackSection(
        parent: LinearLayout,
        density: Float,
        sectionSpacing: Int,
        itemSpacing: Int,
    ) {
        val constants = hapticEngine.getAvailableHapticConstants()
        if (constants.isEmpty()) return

        parent.addView(createSectionLabel(getString(R.string.section_haptic_feedback_constants), density))

        val flow = FlowLayout(this, columns = 2, horizontalGap = itemSpacing, verticalGap = itemSpacing)
        for (info in constants) {
            val button = HapticButton(this, info.name, info.constantName) { screenX, screenY ->
                performHapticFeedback(info.value)
                waveOverlay.spawnWave(screenX, screenY)
            }
            flow.addView(button)
        }
        parent.addView(flow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ))

        parent.addView(Space(this), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, sectionSpacing,
        ))
    }

    private fun buildPredefinedEffectsSection(
        parent: LinearLayout,
        density: Float,
        sectionSpacing: Int,
        itemSpacing: Int,
    ) {
        val effects = hapticEngine.getSupportedEffects()
        if (effects.isEmpty()) return

        parent.addView(createSectionLabel(getString(R.string.section_predefined_effects), density))

        val flow = FlowLayout(this, columns = 2, horizontalGap = itemSpacing, verticalGap = itemSpacing)
        for (info in effects) {
            val button = HapticButton(this, info.name, info.constantName) { screenX, screenY ->
                hapticEngine.playEffect(info.effectId)
                waveOverlay.spawnWave(screenX, screenY)
            }
            flow.addView(button)
        }
        parent.addView(flow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ))

        parent.addView(Space(this), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, sectionSpacing,
        ))
    }

    private fun buildPrimitivesSection(
        parent: LinearLayout,
        density: Float,
        sectionSpacing: Int,
        itemSpacing: Int,
    ) {
        val primitives = hapticEngine.getSupportedPrimitives()
        if (primitives.isEmpty()) return

        parent.addView(createSectionLabel(getString(R.string.section_primitives), density))

        for ((index, info) in primitives.withIndex()) {
            val row = PrimitiveRow(this, info.name, info.constantName) { scale, screenX, screenY ->
                hapticEngine.playPrimitive(info.primitiveId, scale)
                waveOverlay.spawnWave(screenX, screenY)
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            if (index > 0) lp.topMargin = itemSpacing
            parent.addView(row, lp)
        }

        parent.addView(Space(this), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, sectionSpacing,
        ))
    }

    private fun buildDeviceInfo(parent: LinearLayout, density: Float, sectionSpacing: Int) {
        val unavailable = mutableListOf<String>()

        for (info in hapticEngine.getUnavailableHapticConstants()) {
            unavailable.add(info.constantName)
        }
        for (info in hapticEngine.getUnsupportedEffects()) {
            unavailable.add(info.constantName)
        }
        for (info in hapticEngine.getUnsupportedPrimitives()) {
            unavailable.add(info.constantName)
        }

        if (unavailable.isEmpty()) return

        val text = getString(R.string.not_available_on_device, unavailable.joinToString(", "))
        val textView = TextView(this).apply {
            this.text = text
            setTextColor(getColor(R.color.text_disabled))
            textSize = 10f
            typeface = Typeface.MONOSPACE
            setLineSpacing(0f, 1.2f)
        }
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        lp.topMargin = (8 * density).toInt()
        lp.bottomMargin = sectionSpacing
        parent.addView(textView, lp)
    }

    private fun buildBanner(root: FrameLayout, density: Float) {
        bannerView = TextView(this).apply {
            text = getString(R.string.haptic_disabled_message)
            setTextColor(getColor(R.color.foreground))
            textSize = 12f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setBackgroundColor(getColor(R.color.surface))
            setPadding(
                (16 * density).toInt(),
                (12 * density).toInt(),
                (16 * density).toInt(),
                (12 * density).toInt(),
            )
            setOnApplyWindowInsetsListener { v, insets ->
                val navBar = insets.getInsets(WindowInsets.Type.systemBars()).bottom
                v.setPadding(
                    (16 * density).toInt(),
                    (12 * density).toInt(),
                    (16 * density).toInt(),
                    (12 * density).toInt() + navBar,
                )
                insets
            }
            setOnClickListener {
                try {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_SOUND_SETTINGS)
                    startActivity(intent)
                } catch (_: Exception) {
                }
            }
            visibility = View.GONE
        }
        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM,
        )
        root.addView(bannerView, lp)
    }

    private fun updateBanner() {
        val enabled = hapticEngine.isHapticEnabled(this)
        bannerView?.visibility = if (enabled) View.GONE else View.VISIBLE
    }

    private fun performHapticFeedback(constant: Int) {
        waveOverlay.performHapticFeedback(constant)
    }

    private fun createSectionLabel(text: String, density: Float): TextView {
        return TextView(this).apply {
            this.text = text
            setTextColor(getColor(R.color.text_muted))
            textSize = 11f
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.15f
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            lp.bottomMargin = (12 * density).toInt()
            layoutParams = lp
        }
    }
}

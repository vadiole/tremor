package vadiole.tremor

import android.app.Activity
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.view.HapticFeedbackConstants
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView
import vadiole.tremor.view.DragThresholdView
import vadiole.tremor.view.TutorialView
import vadiole.tremor.view.FlowLayout
import vadiole.tremor.view.FooterView
import vadiole.tremor.view.HapticButton
import vadiole.tremor.view.HapticCounter
import vadiole.tremor.view.HapticToggle
import vadiole.tremor.view.HeartParticleView
import vadiole.tremor.view.KeyboardRowView
import vadiole.tremor.view.LongPressButton
import vadiole.tremor.view.PrimitiveRow
import vadiole.tremor.view.RiseFallButton
import vadiole.tremor.view.ScrollWheelView
import vadiole.tremor.view.WaveOverlayView

class TremorActivity : Activity(), Density {

    private lateinit var hapticEngine: HapticEngine
    private lateinit var waveOverlay: WaveOverlayView
    private lateinit var heartOverlay: HeartParticleView
    private lateinit var fallbackEffectIds: Set<Int>
    private lateinit var supportedPrimitives: List<HapticEngine.PrimitiveInfo>
    private lateinit var supportedPrimitiveIds: Set<Int>
    private lateinit var unsupportedPrimitives: List<HapticEngine.PrimitiveInfo>
    private var bannerView: TutorialView? = null
    private var bannerShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION")
        window.setDecorFitsSystemWindows(false)

        hapticEngine = HapticEngine(this)
        fallbackEffectIds = hapticEngine.getFallbackEffectIds()
        supportedPrimitives = hapticEngine.getSupportedPrimitives()
        supportedPrimitiveIds = supportedPrimitives.map { it.primitiveId }.toSet()
        unsupportedPrimitives = HapticEngine.allPrimitives.filter { it.primitiveId !in supportedPrimitiveIds }

        val padding = UiConstants.CONTENT_PADDING_DP.dp
        val sectionSpacing = UiConstants.SECTION_SPACING_DP.dp
        val itemSpacing = UiConstants.ITEM_SPACING_DP.dp

        val root = FrameLayout(this).apply {
            setBackgroundColor(getColor(R.color.background))
            clipChildren = false
            clipToPadding = false
        }

        val scrollView = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            clipChildren = false
            clipToPadding = false
            isVerticalFadingEdgeEnabled = true
            setFadingEdgeLength(48.dp)
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
            setPadding(padding, padding, padding, 0)
            setOnApplyWindowInsetsListener { v, insets ->
                val systemBars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                v.setPadding(padding, padding + systemBars.top, padding, systemBars.bottom)
                insets
            }
        }

        buildHapticFeedbackSection(content, sectionSpacing, itemSpacing)
        buildPredefinedEffectsSection(content, sectionSpacing, itemSpacing)
        buildPrimitivesSection(content, sectionSpacing, itemSpacing)
        buildExamplesSection(content, sectionSpacing, itemSpacing)
        buildDeviceInfo(content, sectionSpacing)
        buildFooter(content)

        scrollView.addView(content, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
        ))

        val contentLayer = FrameLayout(this).apply {
            setBackgroundColor(getColor(R.color.background))
            clipChildren = false
            clipToPadding = false
        }
        contentLayer.addView(scrollView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ))

        root.addView(contentLayer, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ))

        waveOverlay = WaveOverlayView(this).apply {
            setDistortionTarget(contentLayer)
            isClickable = false
            isFocusable = false
        }
        root.addView(waveOverlay, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ))

        heartOverlay = HeartParticleView(this, hapticEngine::playPrimitive, supportedPrimitiveIds).apply {
            isClickable = false
            isFocusable = false
        }
        root.addView(heartOverlay, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ))

        buildBanner(root)

        setContentView(root)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !bannerShown) {
            bannerShown = true
            updateBanner()
        }
    }

    override fun onPause() {
        super.onPause()
        hapticEngine.cancel()
        waveOverlay.clearWaves()
    }

    private fun buildHapticFeedbackSection(
        parent: LinearLayout,
        sectionSpacing: Int,
        itemSpacing: Int,
    ) {
        val constants = hapticEngine.getAvailableHapticConstants()
        if (constants.isEmpty()) return

        parent.addView(createSectionLabel(getString(R.string.section_haptic_feedback_constants)))

        val flow = FlowLayout(this, columns = 2, horizontalGap = itemSpacing, verticalGap = itemSpacing)
        for (info in constants) {
            val strength = hapticConstantStrength(info.value)
            val button = HapticButton(this, getString(info.nameResId), info.constantName) { screenX, screenY ->
                performHapticFeedback(info.value)
                waveOverlay.spawnWave(screenX, screenY, strength)
                if (info.value == HapticFeedbackConstants.REJECT) {
                    waveOverlay.postDelayed({ waveOverlay.spawnWave(screenX, screenY, strength) }, 100)
                }
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
        sectionSpacing: Int,
        itemSpacing: Int,
    ) {
        val effects = hapticEngine.getAllEffects()
        if (effects.isEmpty()) return

        parent.addView(createSectionLabel(getString(R.string.section_predefined_effects)))

        val flow = FlowLayout(this, columns = 2, horizontalGap = itemSpacing, verticalGap = itemSpacing)
        for (info in effects) {
            val isFallback = info.effectId in fallbackEffectIds
            val strength = effectStrength(info.effectId)
            val button = HapticButton(this, getString(info.nameResId), info.constantName, isFallback) { screenX, screenY ->
                hapticEngine.playEffect(info.effectId)
                waveOverlay.spawnWave(screenX, screenY, strength)
                if (info.effectId == VibrationEffect.EFFECT_DOUBLE_CLICK) {
                    waveOverlay.postDelayed({ waveOverlay.spawnWave(screenX, screenY, strength) }, 100)
                }
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
        sectionSpacing: Int,
        itemSpacing: Int,
    ) {
        val primitives = supportedPrimitives
        if (primitives.isEmpty()) return

        parent.addView(createSectionLabel(getString(R.string.section_primitives)))

        for ((index, info) in primitives.withIndex()) {
            val waveStyle = primitiveWaveStyle(info.primitiveId)
            val row = PrimitiveRow(this, getString(info.nameResId), info.constantName) { scale, screenX, screenY ->
                hapticEngine.playPrimitive(info.primitiveId, scale)
                waveOverlay.spawnWave(screenX, screenY, scale, waveStyle)
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

    private fun buildExamplesSection(
        parent: LinearLayout,
        sectionSpacing: Int,
        itemSpacing: Int,
    ) {
        parent.addView(createSectionLabel(getString(R.string.section_examples)))

        val availableConstantNames = hapticEngine.getAvailableHapticConstants().map { it.constantName }.toSet()

        if ("TOGGLE_ON" in availableConstantNames) {
            val toggle = HapticToggle(this)
            val toggleRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                clipChildren = false
                clipToPadding = false
                isClickable = true
                isFocusable = true
                setOnClickListener { toggle.toggle() }
            }
            val toggleLabel = TextView(this).apply {
                text = getString(R.string.example_toggle)
                setTextColor(getColor(R.color.foreground))
                textSize = 13f
                typeface = Typeface.MONOSPACE
            }
            toggleRow.addView(toggleLabel, LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f,
            ))
            toggleRow.addView(toggle)
            parent.addView(toggleRow, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ))

            parent.addView(Space(this), LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, itemSpacing,
            ))
        }

        parent.addView(LongPressButton(this), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ))

        parent.addView(Space(this), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, itemSpacing,
        ))

        parent.addView(HapticCounter(this), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ))

        parent.addView(Space(this), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, itemSpacing,
        ))

        parent.addView(KeyboardRowView(this), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ))

        parent.addView(Space(this), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, itemSpacing,
        ))

        if ("SEGMENT_FREQUENT_TICK" in availableConstantNames) {
            parent.addView(ScrollWheelView(this), LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ))

            parent.addView(Space(this), LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, itemSpacing,
            ))
        }

        val riseFallSupported = VibrationEffect.Composition.PRIMITIVE_QUICK_RISE in supportedPrimitiveIds &&
            VibrationEffect.Composition.PRIMITIVE_QUICK_FALL in supportedPrimitiveIds
        if (riseFallSupported) {
            parent.addView(RiseFallButton(this, hapticEngine::playPrimitive), LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ))

            parent.addView(Space(this), LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, itemSpacing,
            ))
        }

        if ("DRAG_START" in availableConstantNames) {
            parent.addView(DragThresholdView(this), LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ))

            parent.addView(Space(this), LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, sectionSpacing,
            ))
        }
    }

    private fun buildDeviceInfo(parent: LinearLayout, sectionSpacing: Int) {
        val unavailable = mutableListOf<String>()

        for (info in hapticEngine.getUnavailableHapticConstants()) {
            unavailable.add(info.constantName)
        }
        for (info in unsupportedPrimitives) {
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
        lp.topMargin = 8.dp
        lp.bottomMargin = sectionSpacing
        parent.addView(textView, lp)
    }

    private fun buildFooter(parent: LinearLayout) {
        val footer = FooterView(this) { screenX, screenY ->
            heartOverlay.launchHearts(screenX, screenY)
        }
        parent.addView(footer, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ))
    }

    private fun buildBanner(root: FrameLayout) {
        bannerView = TutorialView(this)
        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM,
        )
        root.addView(bannerView, lp)
    }

    private val hideBannerRunnable = Runnable {
        bannerView?.visibility = View.GONE
    }

    private fun updateBanner() {
        val banner = bannerView ?: return
        banner.removeCallbacks(hideBannerRunnable)
        if (Build.VERSION.SDK_INT >= 35) {
            // Can't reliably detect disabled vibration — show as temporary hint
            banner.visibility = View.VISIBLE
            if (!hapticEngine.isDndActive()) {
                banner.postDelayed(hideBannerRunnable, 10_000)
            }
        } else {
            val enabled = hapticEngine.isHapticEnabled(banner)
            banner.visibility = if (enabled) View.GONE else View.VISIBLE
        }
    }

    private fun performHapticFeedback(constant: Int) {
        val played = waveOverlay.performHapticFeedback(constant)
        if (!played && bannerView?.visibility != View.VISIBLE) {
            bannerView?.visibility = View.VISIBLE
        }
    }

    private fun createSectionLabel(text: String): TextView {
        return TextView(this).apply {
            this.text = text.uppercase()
            setTextColor(getColor(R.color.text_muted))
            textSize = 11f
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.05f
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            lp.bottomMargin = 12.dp
            layoutParams = lp
        }
    }
}

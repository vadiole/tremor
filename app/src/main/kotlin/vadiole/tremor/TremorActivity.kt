package vadiole.tremor

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.os.VibrationEffect
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView
import vadiole.tremor.view.DragThresholdView
import vadiole.tremor.view.FlowLayout
import vadiole.tremor.view.HapticButton
import vadiole.tremor.view.HapticCounter
import vadiole.tremor.view.HapticToggle
import vadiole.tremor.view.KeyboardRowView
import vadiole.tremor.view.LongPressButton
import vadiole.tremor.view.PrimitiveRow
import vadiole.tremor.view.RiseFallButton
import vadiole.tremor.view.ScrollWheelView
import vadiole.tremor.view.WaveOverlayView

class TremorActivity : Activity(), Density {

    private lateinit var hapticEngine: HapticEngine
    private lateinit var waveOverlay: WaveOverlayView
    private var bannerView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        hapticEngine = HapticEngine(this)

        val padding = 16.dp()
        val sectionSpacing = 24.dp()
        val itemSpacing = 8.dp()

        val root = FrameLayout(this).apply {
            setBackgroundColor(getColor(R.color.background))
        }

        val scrollView = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            clipToPadding = false
            isVerticalFadingEdgeEnabled = true
            setFadingEdgeLength(48.dp())
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

        buildBanner(root)

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
        val effects = hapticEngine.getSupportedEffects()
        if (effects.isEmpty()) return

        parent.addView(createSectionLabel(getString(R.string.section_predefined_effects)))

        val flow = FlowLayout(this, columns = 2, horizontalGap = itemSpacing, verticalGap = itemSpacing)
        for (info in effects) {
            val strength = effectStrength(info.effectId)
            val button = HapticButton(this, getString(info.nameResId), info.constantName) { screenX, screenY ->
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
        val primitives = hapticEngine.getSupportedPrimitives()
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

        val toggle = HapticToggle(this)
        val toggleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
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

        parent.addView(ScrollWheelView(this), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ))

        parent.addView(Space(this), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, itemSpacing,
        ))

        parent.addView(RiseFallButton(this), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ))

        parent.addView(Space(this), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, itemSpacing,
        ))

        parent.addView(DragThresholdView(this), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ))

        parent.addView(Space(this), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, sectionSpacing,
        ))
    }

    private fun buildDeviceInfo(parent: LinearLayout, sectionSpacing: Int) {
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
        lp.topMargin = 8.dp()
        lp.bottomMargin = sectionSpacing
        parent.addView(textView, lp)
    }

    private fun buildFooter(parent: LinearLayout) {
        val vadioleText = getString(R.string.footer_vadiole)
        val full = getString(R.string.footer_template, vadioleText)

        val spannable = android.text.SpannableString(full)
        val linkStart = full.indexOf(vadioleText)
        val linkEnd = linkStart + vadioleText.length

        spannable.setSpan(
            android.text.style.UnderlineSpan(),
            linkStart, linkEnd,
            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )

        val linkColor = getColor(R.color.text_disabled)
        spannable.setSpan(
            object : android.text.style.ClickableSpan() {
                override fun onClick(widget: View) {
                    try {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://play.google.com/store/apps/dev?id=4763171503902347202"),
                        )
                        startActivity(intent)
                    } catch (_: Exception) {
                    }
                }

                override fun updateDrawState(ds: android.text.TextPaint) {
                    ds.isUnderlineText = true
                    ds.color = linkColor
                }
            },
            linkStart, linkEnd,
            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )

        val footer = TextView(this).apply {
            text = spannable
            movementMethod = android.text.method.LinkMovementMethod.getInstance()
            setTextColor(getColor(R.color.text_disabled))
            highlightColor = (linkColor and 0x00FFFFFF) or 0x80000000.toInt()
            textSize = 10f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
        }
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        lp.topMargin = 16.dp()
        lp.bottomMargin = 8.dp()
        parent.addView(footer, lp)
    }

    private fun buildBanner(root: FrameLayout) {
        bannerView = TextView(this).apply {
            text = getString(R.string.haptic_disabled_message)
            setTextColor(getColor(R.color.foreground))
            textSize = 12f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setBackgroundColor(getColor(R.color.surface))
            setPadding(16.dp(), 12.dp(), 16.dp(), 12.dp())
            setOnApplyWindowInsetsListener { v, insets ->
                val navBar = insets.getInsets(WindowInsets.Type.systemBars()).bottom
                v.setPadding(16.dp(), 12.dp(), 16.dp(), 12.dp() + navBar)
                insets
            }
            setOnClickListener {
                try {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_SOUND_SETTINGS)
                    startActivity(intent)
                } catch (_: Exception) {
                    try {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                        startActivity(intent)
                    } catch (_: Exception) {
                    }
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
        val played = waveOverlay.performHapticFeedback(constant)
        if (!played && bannerView?.visibility != View.VISIBLE) {
            bannerView?.visibility = View.VISIBLE
        }
    }

    private fun hapticConstantStrength(constant: Int): Float = when (constant) {
        HapticFeedbackConstants.LONG_PRESS,
        HapticFeedbackConstants.CONFIRM,
        HapticFeedbackConstants.REJECT -> 0.8f
        HapticFeedbackConstants.KEYBOARD_PRESS,
        HapticFeedbackConstants.VIRTUAL_KEY -> 0.5f
        HapticFeedbackConstants.CLOCK_TICK,
        HapticFeedbackConstants.CONTEXT_CLICK,
        HapticFeedbackConstants.TEXT_HANDLE_MOVE,
        HapticFeedbackConstants.KEYBOARD_RELEASE,
        HapticFeedbackConstants.VIRTUAL_KEY_RELEASE -> 0.3f
        else -> 0.5f
    }

    private fun effectStrength(effectId: Int): Float = when (effectId) {
        VibrationEffect.EFFECT_HEAVY_CLICK -> 1.0f
        VibrationEffect.EFFECT_DOUBLE_CLICK -> 0.7f
        VibrationEffect.EFFECT_CLICK -> 0.5f
        VibrationEffect.EFFECT_TICK -> 0.3f
        else -> 0.5f
    }

    private fun primitiveWaveStyle(primitiveId: Int): WaveOverlayView.WaveStyle = when (primitiveId) {
        VibrationEffect.Composition.PRIMITIVE_CLICK -> WaveOverlayView.WaveStyle.CLICK
        VibrationEffect.Composition.PRIMITIVE_TICK,
        VibrationEffect.Composition.PRIMITIVE_LOW_TICK -> WaveOverlayView.WaveStyle.TICK
        VibrationEffect.Composition.PRIMITIVE_QUICK_RISE -> WaveOverlayView.WaveStyle.RISE
        VibrationEffect.Composition.PRIMITIVE_SLOW_RISE -> WaveOverlayView.WaveStyle.SLOW_RISE
        VibrationEffect.Composition.PRIMITIVE_QUICK_FALL -> WaveOverlayView.WaveStyle.FALL
        VibrationEffect.Composition.PRIMITIVE_SPIN -> WaveOverlayView.WaveStyle.SPIN
        VibrationEffect.Composition.PRIMITIVE_THUD -> WaveOverlayView.WaveStyle.THUD
        else -> WaveOverlayView.WaveStyle.DEFAULT
    }

    private fun createSectionLabel(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            setTextColor(getColor(R.color.text_muted))
            textSize = 11f
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.05f
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            lp.bottomMargin = 12.dp()
            layoutParams = lp
        }
    }
}

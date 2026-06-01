package vadiole.tremor.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.os.VibrationEffect
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import kotlin.math.abs
import kotlin.math.sqrt
import vadiole.tremor.Density
import vadiole.tremor.R
import vadiole.tremor.UiConstants
import vadiole.tremor.rubberBand

/**
 * A 2:1 "tray seen from above" with a ball that obeys momentum, friction and walls — but no
 * gravity. The ball has a magnetic resting socket (dotted ring at 70% / 50%); scrolling the page
 * shakes the tray, so heavy scrolling can resonate the ball out of the socket, and a free ball is
 * gently pulled back home. Grabbing the ball lets you fling it around or drag it out through a
 * wall against a rubber band that stores energy and launches it back on release.
 *
 * Haptics are emitted on bounce (scaled by impact speed), grab, release/launch, magnet
 * settle/escape, and as "cracks" while the rubber band stretches. Off-screen bounce/settle/escape
 * haptics are suppressed (see [isVisibleY]); cracks always play while dragging. All feel is tunable
 * via [BallBoxTuning].
 */
class BallBoxView(
    context: Context,
    private val playPrimitive: (primitiveId: Int, scale: Float) -> Unit,
    private val playEffect: (effectId: Int) -> Unit,
    private val supportedPrimitives: Set<Int> = emptySet(),
) : View(context), Density {

    // geometry / surface
    private val cornerRadius = UiConstants.CORNER_RADIUS_DP.dp
    private val surfaceDrawable = FloatingSurfaceDrawable.squircleSurface(context, cornerRadius.toInt())
    private val surfaceInset = Floating.surfaceInsetPx(context)
    private val ballRadius = 11f.dp
    private val ringRadius = ballRadius + 6f.dp
    private val density = resources.displayMetrics.density

    // paints (allocated once, never in onDraw)
    private val ballPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.foreground)
        style = Paint.Style.FILL
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.text_muted)
        style = Paint.Style.STROKE
        strokeWidth = 1.5f.dp
        pathEffect = DashPathEffect(floatArrayOf(3f.dp, 4f.dp), 0f)
    }
    // scratch (never allocate per-frame)
    private val visibleRect = Rect()
    private val locScratch = IntArray(2)

    // haptic ids
    private val pLowTick = VibrationEffect.Composition.PRIMITIVE_LOW_TICK
    private val pTick = VibrationEffect.Composition.PRIMITIVE_TICK
    private val pClick = VibrationEffect.Composition.PRIMITIVE_CLICK
    private val softBounce = intArrayOf(pTick, pLowTick)
    private val hardBounce = intArrayOf(pClick, pTick)
    private val crackPrims = intArrayOf(pLowTick, pTick)
    private val grabPrims = intArrayOf(pClick, pTick)
    private val settlePrims = intArrayOf(pLowTick, pTick)
    private val popPrims = intArrayOf(pClick, pTick)
    private val launchPrims = intArrayOf(pClick, pTick)

    // ball state (view-local px)
    private var bx = 0f
    private var by = 0f
    private var vx = 0f
    private var vy = 0f
    private var captured = true
    private var grabbed = false
    private var pendingCatch = false
    private var grabScale = 1f

    // wall bounds for the ball CENTER
    private var minX = 0f
    private var maxX = 0f
    private var minY = 0f
    private var maxY = 0f
    private var restX = 0f
    private var restY = 0f
    private var maxStretch = 0f

    // rubber-band drag
    private var grabOffsetX = 0f
    private var grabOffsetY = 0f
    private var stretch = 0f
    private var airborne = false
    private var lastCrackLevel = 0
    private var lastCrackTime = 0L
    private var fingerVx = 0f
    private var fingerVy = 0f
    private var prevBx = 0f
    private var prevBy = 0f
    private var touchX = 0f
    private var touchY = 0f

    // scroll coupling
    private var lastBoxY = 0f
    private var boxVel = 0f
    private var lastBoxVel = 0f

    // frame loop
    private var lastFrameTime = 0L
    private var isRunning = false

    // only react to scrolling while the box is actually on screen — scrolling elsewhere in the
    // page shouldn't silently agitate a ball the user can't even see
    private val scrollListener = ViewTreeObserver.OnScrollChangedListener {
        if (getLocalVisibleRect(visibleRect)) startLoop()
    }

    private val animRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            val now = System.nanoTime()
            val dt = ((now - lastFrameTime) / 1_000_000_000f).coerceAtMost(0.05f)
            lastFrameTime = now

            step(dt)
            invalidate()

            if (isActive()) postOnAnimation(this) else isRunning = false
        }
    }

    init {
        isClickable = true
        background = surfaceDrawable
        keepFloatingSurfaceShadowOnly()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, w / 2)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        minX = surfaceInset + ballRadius
        maxX = w - surfaceInset - ballRadius
        minY = surfaceInset + ballRadius
        maxY = h - surfaceInset - ballRadius
        restX = w * 0.70f
        restY = h * 0.50f
        maxStretch = h * BallBoxTuning.maxStretchFactor
        if (!grabbed) {
            bx = restX
            by = restY
            vx = 0f
            vy = 0f
            captured = true
            stretch = 0f
            airborne = false
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewTreeObserver.addOnScrollChangedListener(scrollListener)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewTreeObserver.removeOnScrollChangedListener(scrollListener)
        isRunning = false
        grabbed = false
        isPressed = false
        removeCallbacks(animRunnable)
        surfaceDrawable.cancelAnimations()
    }

    // --- frame loop ---

    private fun startLoop() {
        if (isRunning || width == 0) return
        getLocationInWindow(locScratch)
        lastBoxY = locScratch[1].toFloat()
        boxVel = 0f
        lastBoxVel = 0f
        lastFrameTime = System.nanoTime()
        isRunning = true
        postOnAnimation(animRunnable)
    }

    private fun isActive(): Boolean {
        if (grabbed || airborne || pendingCatch) return true
        val dist = len(bx - restX, by - restY)
        val moving = len(vx, vy) > BallBoxTuning.sleepSpeed * density
        val scrolling = abs(boxVel) > BallBoxTuning.sleepBoxVel * density
        val springing = captured && dist > 0.5f
        // a free ball within the socket's local pull (not yet captured) is still being drawn in,
        // even if momentarily slow at a turning point — keep simulating until it settles
        val attracting = !captured && dist in 0.5f..(BallBoxTuning.attractionRadius * density)
        val scaling = abs(grabScale - 1f) > 0.01f
        return moving || scrolling || springing || attracting || scaling
    }

    private fun step(dt: Float) {
        // track the tray's on-screen velocity so we can apply inertial sloshing
        getLocationInWindow(locScratch)
        val boxY = locScratch[1].toFloat()
        val rawVel = ((boxY - lastBoxY) / dt)
            .coerceIn(-BallBoxTuning.maxBoxVel * density, BallBoxTuning.maxBoxVel * density)
        lastBoxY = boxY
        boxVel += (rawVel - boxVel) * BallBoxTuning.scrollSmooth
        val dBoxVel = boxVel - lastBoxVel
        lastBoxVel = boxVel

        // smoothly grow the ball while held
        val targetScale = if (grabbed) BallBoxTuning.grabGrow else 1f
        grabScale += (targetScale - grabScale) * (12f * dt).coerceAtMost(1f)

        if (grabbed) {
            // ease the ball to sit centred on the finger horizontally and lifted above it
            val lift = BallBoxTuning.grabLift.dp
            val ease = (BallBoxTuning.grabAttract * dt).coerceAtMost(1f)
            grabOffsetX -= grabOffsetX * ease
            grabOffsetY += (-lift - grabOffsetY) * ease
            positionGrabbed()
            // track the ball's velocity for a flick-release
            fingerVx += ((bx - prevBx) / dt - fingerVx) * 0.4f
            fingerVy += ((by - prevBy) / dt - fingerVy) * 0.4f
            prevBx = bx
            prevBy = by
            return
        }

        // inertial pseudo-force: ball lags the tray's acceleration (only while on screen)
        if (getLocalVisibleRect(visibleRect)) {
            vy += -BallBoxTuning.scrollCoupling * dBoxVel
        }

        if (captured) {
            val dx = bx - restX
            val dy = by - restY
            // underdamped spring hold — lets rhythmic scrolling build resonance
            vx += (-BallBoxTuning.holdStiffness * dx - BallBoxTuning.holdDamping * vx) * dt
            vy += (-BallBoxTuning.holdStiffness * dy - BallBoxTuning.holdDamping * vy) * dt
            val escapeRadius = ringRadius * BallBoxTuning.escapeRadiusMul
            if (len(dx, dy) > escapeRadius || len(vx, vy) > BallBoxTuning.escapeSpeed * density) {
                captured = false
                if (isVisibleY(restY)) playPrim(popPrims, BallBoxTuning.popScale, EFFECT_CLICK)
            }
        } else {
            // weak magnet felt only NEAR the socket and only at LOW speed
            val dx = restX - bx
            val dy = restY - by
            val dist = len(dx, dy)
            if (dist < BallBoxTuning.attractionRadius * density &&
                len(vx, vy) < BallBoxTuning.attractionMaxSpeed * density
            ) {
                var ax = BallBoxTuning.attractK * dx
                var ay = BallBoxTuning.attractK * dy
                val am = len(ax, ay)
                val cap = BallBoxTuning.attractMax * density
                if (am > cap) {
                    val s = cap / am
                    ax *= s
                    ay *= s
                }
                vx += ax * dt
                vy += ay * dt
            }
        }

        // friction (frame-rate independent exponential decay)
        val decay = expDecay(BallBoxTuning.friction, dt)
        vx *= decay
        vy *= decay

        val speed = len(vx, vy)
        val maxSpeed = BallBoxTuning.maxSpeed * density
        if (speed > maxSpeed) {
            val s = maxSpeed / speed
            vx *= s
            vy *= s
        }

        bx += vx * dt
        by += vy * dt

        if (airborne) {
            // launched from outside the box — let it fly back in without snapping to a wall
            if (bx in minX..maxX && by in minY..maxY) airborne = false
        } else {
            collide()
        }

        // latch into the socket when arriving slow & close
        if (!captured) {
            if (len(bx - restX, by - restY) < ringRadius &&
                len(vx, vy) < BallBoxTuning.captureSpeed * density
            ) {
                captured = true
                if (isVisibleY(restY)) playPrim(settlePrims, BallBoxTuning.settleScale, EFFECT_TICK)
            }
        }

        // a held finger that missed the ball catches it if it drifts near
        if (pendingCatch && len(bx - touchX, by - touchY) <= grabRadius()) {
            beginGrab()
        }
    }

    private fun collide() {
        val e = BallBoxTuning.restitution
        if (bx < minX) {
            bx = minX
            if (vx < 0f) { bounce(-vx, minX, by); vx = -vx * e }
        } else if (bx > maxX) {
            bx = maxX
            if (vx > 0f) { bounce(vx, maxX, by); vx = -vx * e }
        }
        if (by < minY) {
            by = minY
            if (vy < 0f) { bounce(-vy, bx, minY); vy = -vy * e }
        } else if (by > maxY) {
            by = maxY
            if (vy > 0f) { bounce(vy, bx, maxY); vy = -vy * e }
        }
    }

    private fun bounce(impactSpeed: Float, px: Float, py: Float) {
        val minSpeed = BallBoxTuning.minBounceSpeed * density
        if (impactSpeed < minSpeed) return
        if (!isVisibleY(py)) return
        val refSpeed = BallBoxTuning.bounceRefSpeed * density
        val p = ((impactSpeed - minSpeed) / (refSpeed - minSpeed)).coerceIn(0f, 1f)
        val scale = BallBoxTuning.bounceMinScale + p * (BallBoxTuning.bounceMaxScale - BallBoxTuning.bounceMinScale)
        if (p < 0.5f) playPrim(softBounce, scale, EFFECT_TICK)
        else playPrim(hardBounce, scale, EFFECT_CLICK)
    }

    // --- touch ---

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchX = event.x
                touchY = event.y
                if (len(event.x - bx, event.y - by) <= grabRadius()) {
                    beginGrab()
                } else {
                    // missed the ball — keep watching; if it drifts under the finger, catch it.
                    // don't disallow parent intercept, so a vertical scroll still passes through.
                    pendingCatch = true
                    startLoop()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (grabbed || pendingCatch) {
                    touchX = event.x
                    touchY = event.y
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!grabbed) {
                    pendingCatch = false
                    return true
                }
                grabbed = false
                isPressed = false
                parent?.requestDisallowInterceptTouchEvent(false)
                if (stretch > BallBoxTuning.launchMinStretch.dp) {
                    // shoot toward the box centre and fly there via physics (no snap/teleport)
                    val dx = width * 0.5f - bx
                    val dy = height * 0.5f - by
                    val d = len(dx, dy)
                    if (d > 0.001f) {
                        val cap = BallBoxTuning.launchMaxSpeed * density
                        val sp = (stretch * BallBoxTuning.launchGain).coerceAtMost(cap)
                        vx = dx / d * sp
                        vy = dy / d * sp
                    }
                    airborne = true
                    val prog = (stretch / maxStretch).coerceIn(0f, 1f)
                    if (isVisibleY(by)) {
                        if (prog > 0.66f) playPrim(launchPrims, 0.5f + 0.5f * prog, EFFECT_HEAVY_CLICK)
                        else playPrim(launchPrims, 0.4f + 0.5f * prog, EFFECT_CLICK)
                    }
                } else {
                    val maxSpeed = BallBoxTuning.maxSpeed * density
                    vx = fingerVx.coerceIn(-maxSpeed, maxSpeed)
                    vy = fingerVy.coerceIn(-maxSpeed, maxSpeed)
                    if (isVisibleY(by)) playPrim(crackPrims, BallBoxTuning.dropScale, EFFECT_TICK)
                }
                stretch = 0f
                startLoop()
            }
        }
        return true
    }

    // grab radius is computed live — grabPadding can change at runtime via a slider
    private fun grabRadius(): Float = ballRadius + BallBoxTuning.grabPadding.dp

    private fun beginGrab() {
        grabbed = true
        captured = false
        isPressed = true
        grabOffsetX = bx - touchX
        grabOffsetY = by - touchY
        vx = 0f
        vy = 0f
        stretch = 0f
        airborne = false
        lastCrackLevel = 0
        lastCrackTime = 0L
        prevBx = bx
        prevBy = by
        fingerVx = 0f
        fingerVy = 0f
        pendingCatch = false
        parent?.requestDisallowInterceptTouchEvent(true)
        playPrim(grabPrims, BallBoxTuning.grabScale, EFFECT_CLICK)
        startLoop()
    }

    private fun positionGrabbed() {
        val tx = touchX + grabOffsetX
        val ty = touchY + grabOffsetY
        val ex = tx.coerceIn(minX, maxX)
        val ey = ty.coerceIn(minY, maxY)
        val ox = tx - ex
        val oy = ty - ey
        val om = len(ox, oy)
        if (om < 0.5f) {
            bx = ex
            by = ey
            stretch = 0f
            lastCrackLevel = 0
        } else {
            val mag = rubberBand(om, maxStretch, BallBoxTuning.dragDamping)
            bx = ex + ox / om * mag
            by = ey + oy / om * mag
            stretch = mag
            // a crack each time the visual stretch advances a notch — rate-limited so each lands
            // as a distinct, feelable pulse instead of a superseded blur. No visibility gate: the
            // ball is literally under the finger while dragging.
            val level = (stretch / BallBoxTuning.crackSpacing.dp).toInt()
            if (level > lastCrackLevel) {
                val now = System.nanoTime()
                if (now - lastCrackTime >= (BallBoxTuning.crackInterval * 1_000_000f).toLong()) {
                    lastCrackTime = now
                    val prog = (stretch / maxStretch).coerceIn(0f, 1f)
                    playCrack(BallBoxTuning.crackScaleBase + prog * BallBoxTuning.crackScaleRamp)
                }
            }
            lastCrackLevel = level
        }
    }

    // --- draw ---

    override fun onDraw(canvas: Canvas) {
        canvas.drawCircle(restX, restY, ringRadius, ringPaint)
        canvas.drawCircle(bx, by, ballRadius * grabScale, ballPaint)
    }

    // --- helpers ---

    private fun playCrack(scale: Float) {
        val prim = when ((BallBoxTuning.crackPrimitive + 0.5f).toInt()) {
            0 -> pLowTick
            2 -> pClick
            else -> pTick
        }
        val s = scale.coerceIn(0.01f, 1f)
        if (prim in supportedPrimitives) playPrimitive(prim, s) else playEffect(EFFECT_TICK)
    }

    private fun playPrim(prefs: IntArray, scale: Float, fallbackEffect: Int) {
        val s = scale.coerceIn(0.01f, 1f)
        for (p in prefs) {
            if (p in supportedPrimitives) {
                playPrimitive(p, s)
                return
            }
        }
        playEffect(fallbackEffect)
    }

    private fun isVisibleY(y: Float): Boolean {
        if (!getLocalVisibleRect(visibleRect)) return false
        return y >= visibleRect.top && y <= visibleRect.bottom
    }

    private fun len(x: Float, y: Float): Float = sqrt(x * x + y * y)

    private companion object {
        val EFFECT_TICK = VibrationEffect.EFFECT_TICK
        val EFFECT_CLICK = VibrationEffect.EFFECT_CLICK
        val EFFECT_HEAVY_CLICK = VibrationEffect.EFFECT_HEAVY_CLICK

        // approximate e^(-rate*dt) without kotlin.math.exp import noise
        fun expDecay(rate: Float, dt: Float): Float {
            val x = rate * dt
            return 1f / (1f + x + 0.5f * x * x)
        }
    }
}

/**
 * Debug customization for [BallBoxView]. Distances/speeds marked "(dp)" are expressed in
 * density-independent units (per second where they are velocities) and scaled to pixels at use, so
 * the feel is consistent across screens. Everything else is a unitless multiplier. Tweak and
 * rebuild to test different haptic and physics options.
 */
object BallBoxTuning {
    // motion
    var friction = 0.0f            // velocity e-folding rate per second (higher = stops sooner)
    var restitution = 0.85f        // wall bounce energy kept (0..1)
    var maxSpeed = 5000f           // (dp/s) hard velocity clamp

    // resting-spot magnet (felt only near the socket and only at low speed)
    var attractionRadius = 60f     // (dp) pull is felt only within this distance of the socket
    var attractionMaxSpeed = 700f  // (dp/s) pull is felt only when slower than this
    var attractK = 3.0f            // pull stiffness near the socket (1/s^2)
    var attractMax = 600f          // (dp/s^2) cap on the pull
    var captureSpeed = 130f        // (dp/s) latch into the socket only below this speed
    var holdStiffness = 220f       // socket spring stiffness (1/s^2)
    var holdDamping = 9f           // socket spring damping (underdamped -> can resonate)
    var escapeRadiusMul = 1.8f     // escape when displacement > ringRadius * this
    var escapeSpeed = 900f         // (dp/s) escape when shaken faster than this

    // scroll coupling (tray inertia)
    var scrollCoupling = 1.01f     // how hard the ball reacts to the tray's acceleration
    var scrollSmooth = 0.5f        // low-pass on measured tray velocity (0..1)
    var maxBoxVel = 9000f          // (dp/s) clamp on measured tray velocity

    // rubber-band drag
    var dragDamping = 2.1f         // rubber-band resistance constant (higher = stiffer band)
    var maxStretchFactor = 1.0f    // band asymptote as a fraction of box height
    var launchGain = 20.0f         // release velocity per pixel of stretch (1/s)
    var launchMaxSpeed = 2545f     // (dp/s) cap on launch speed so the shot stays visible
    var launchMinStretch = 6f      // (dp) below this a release is a drop, not a launch

    // grab
    var grabPadding = 48f          // (dp) extra touch radius beyond the ball (bigger = easier to grab)
    var grabAttract = 20f          // how fast a grabbed ball glides to the finger (per second)
    var grabLift = 36f             // (dp) the ball rides this far above the finger

    // sleep thresholds
    var sleepSpeed = 6f            // (dp/s) ball is considered still below this
    var sleepBoxVel = 40f          // (dp/s) scrolling considered stopped below this

    // haptics — bounce
    var minBounceSpeed = 55f       // (dp/s) below this a bounce is silent (settling)
    var bounceRefSpeed = 2600f     // (dp/s) speed mapped to the strongest bounce
    var bounceMinScale = 0.88f     // even gentle bounces are clearly felt
    var bounceMaxScale = 1.0f

    // haptics — rubber-band cracks (a distinct tick each notch of stretch)
    var crackPrimitive = 1f        // 0 = low tick, 1 = tick, 2 = click
    var crackSpacing = 10f         // (dp) visual-stretch between cracks
    var crackInterval = 35f        // (ms) minimum time between cracks so each is felt distinctly
    var crackScaleBase = 0.5f      // crack strength at low tension
    var crackScaleRamp = 0.4f      // extra strength at full tension

    // haptics — discrete events
    var grabScale = 0.41f
    var dropScale = 0.74f
    var settleScale = 1.0f
    var popScale = 0.49f
    var grabGrow = 1.10f           // visual ball scale while held
}

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

    private val cornerRadius = UiConstants.CORNER_RADIUS_DP.dp
    private val surfaceDrawable = FloatingSurfaceDrawable.squircleSurface(context, cornerRadius.toInt())
    private val surfaceInset = Floating.surfaceInsetPx(context)
    private val ballRadius = 12f.dp
    private val ringRadius = ballRadius + 6f.dp
    private val density = resources.displayMetrics.density

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
    private val visibleRect = Rect()
    private val locScratch = IntArray(2)

    private val primitiveLowTick = VibrationEffect.Composition.PRIMITIVE_LOW_TICK
    private val primitiveTick = VibrationEffect.Composition.PRIMITIVE_TICK
    private val primitiveClick = VibrationEffect.Composition.PRIMITIVE_CLICK
    private val bouncePrimitives = intArrayOf(primitiveClick, primitiveTick)
    private val crackPrimitives = intArrayOf(primitiveLowTick, primitiveTick)
    private val grabPrimitives = intArrayOf(primitiveClick, primitiveTick)
    private val settlePrimitives = intArrayOf(primitiveLowTick, primitiveTick)
    private val popPrimitives = intArrayOf(primitiveClick, primitiveTick)
    private val launchPrimitives = intArrayOf(primitiveClick, primitiveTick)

    // ball state, view-local px
    private var ballX = 0f
    private var ballY = 0f
    private var viewX = 0f
    private var viewY = 0f
    private var captured = true
    private var grabbed = false
    private var pendingCatch = false
    private var grabScale = 1f

    private var wallMinX = 0f
    private var wallMaxX = 0f
    private var wallMinY = 0f
    private var wallMaxY = 0f
    private var restX = 0f
    private var restY = 0f
    private var maxStretch = 0f

    private var grabOffsetX = 0f
    private var grabOffsetY = 0f
    private var stretch = 0f
    private var airborne = false
    private var lastCrackLevel = 0
    private var lastCrackTime = 0L
    private var fingerVx = 0f
    private var fingerVy = 0f
    private var prevBallX = 0f
    private var prevBallY = 0f
    private var touchX = 0f
    private var touchY = 0f

    private var lastBoxY = 0f
    private var boxVelocity = 0f
    private var lastBoxVelocity = 0f

    private var lastFrameTime = 0L
    private var isRunning = false

    // only react to scrolling while the box is actually on screen — scrolling elsewhere in the
    // page shouldn't silently agitate a ball the user can't even see
    private val scrollListener = ViewTreeObserver.OnScrollChangedListener {
        if (getLocalVisibleRect(visibleRect)) startLoop()
    }

    //TODO refactor to choreographer and 120 fps support.
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
        z = 1f
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, width / 2)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        wallMinX = surfaceInset + ballRadius
        wallMaxX = w - surfaceInset - ballRadius
        wallMinY = surfaceInset + ballRadius
        wallMaxY = h - surfaceInset - ballRadius
        restX = w * 0.70f
        restY = h * 0.50f
        maxStretch = h * BallBoxTuning.maxStretchFactor
        if (!grabbed) {
            ballX = restX
            ballY = restY
            viewX = 0f
            viewY = 0f
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

    private fun startLoop() {
        if (isRunning || width == 0) return
        getLocationInWindow(locScratch)
        lastBoxY = locScratch[1].toFloat()
        boxVelocity = 0f
        lastBoxVelocity = 0f
        lastFrameTime = System.nanoTime()
        isRunning = true
        postOnAnimation(animRunnable)
    }

    private fun isActive(): Boolean {
        if (grabbed || airborne || pendingCatch) return true
        val dist = length(ballX - restX, ballY - restY)
        val moving = length(viewX, viewY) > BallBoxTuning.sleepSpeed * density
        val scrolling = abs(boxVelocity) > BallBoxTuning.sleepBoxVel * density
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
        boxVelocity += (rawVel - boxVelocity) * BallBoxTuning.scrollSmooth
        val dBoxVel = boxVelocity - lastBoxVelocity
        lastBoxVelocity = boxVelocity

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
            fingerVx += ((ballX - prevBallX) / dt - fingerVx) * 0.4f
            fingerVy += ((ballY - prevBallY) / dt - fingerVy) * 0.4f
            prevBallX = ballX
            prevBallY = ballY
            return
        }

        // inertial pseudo-force: ball lags the tray's acceleration (only while on screen)
        if (getLocalVisibleRect(visibleRect)) {
            viewY += -BallBoxTuning.scrollCoupling * dBoxVel
        }

        if (captured) {
            val dx = ballX - restX
            val dy = ballY - restY
            // underdamped spring hold — lets rhythmic scrolling build resonance
            viewX += (-BallBoxTuning.holdStiffness * dx - BallBoxTuning.holdDamping * viewX) * dt
            viewY += (-BallBoxTuning.holdStiffness * dy - BallBoxTuning.holdDamping * viewY) * dt
            val escapeRadius = ringRadius * BallBoxTuning.escapeRadiusMul
            if (length(dx, dy) > escapeRadius || length(viewX, viewY) > BallBoxTuning.escapeSpeed * density) {
                captured = false
                if (isVisibleY(restY)) playPrim(popPrimitives, BallBoxTuning.popScale, EFFECT_CLICK)
            }
        } else {
            // weak magnet felt only NEAR the socket and only at LOW speed
            val dx = restX - ballX
            val dy = restY - ballY
            val dist = length(dx, dy)
            if (dist < BallBoxTuning.attractionRadius * density &&
                length(viewX, viewY) < BallBoxTuning.attractionMaxSpeed * density
            ) {
                var ax = BallBoxTuning.attractK * dx
                var ay = BallBoxTuning.attractK * dy
                val am = length(ax, ay)
                val cap = BallBoxTuning.attractMax * density
                if (am > cap) {
                    val s = cap / am
                    ax *= s
                    ay *= s
                }
                viewX += ax * dt
                viewY += ay * dt
            }
        }

        // frame-rate-independent exponential decay
        val decay = exponentialDecay(BallBoxTuning.friction, dt)
        viewX *= decay
        viewY *= decay

        val speed = length(viewX, viewY)
        val maxSpeed = BallBoxTuning.maxSpeed * density
        if (speed > maxSpeed) {
            val s = maxSpeed / speed
            viewX *= s
            viewY *= s
        }

        ballX += viewX * dt
        ballY += viewY * dt

        if (airborne) {
            // launched from outside the box — let it fly back in without snapping to a wall
            if (ballX in wallMinX..wallMaxX && ballY in wallMinY..wallMaxY) airborne = false
        } else {
            collide()
        }

        // latch into the socket when arriving slow & close
        if (!captured) {
            if (length(ballX - restX, ballY - restY) < ringRadius &&
                length(viewX, viewY) < BallBoxTuning.captureSpeed * density
            ) {
                captured = true
                if (isVisibleY(restY)) playPrim(settlePrimitives, BallBoxTuning.settleScale, EFFECT_TICK)
            }
        }

        // a held finger that missed the ball catches it if it drifts near
        if (pendingCatch && length(ballX - touchX, ballY - touchY) <= grabRadius()) {
            beginGrab()
        }
    }

    private fun collide() {
        val e = BallBoxTuning.restitution
        if (ballX < wallMinX) {
            ballX = wallMinX
            if (viewX < 0f) {
                bounce(-viewX, wallMinX, ballY); viewX = -viewX * e
            }
        } else if (ballX > wallMaxX) {
            ballX = wallMaxX
            if (viewX > 0f) {
                bounce(viewX, wallMaxX, ballY); viewX = -viewX * e
            }
        }
        if (ballY < wallMinY) {
            ballY = wallMinY
            if (viewY < 0f) {
                bounce(-viewY, ballX, wallMinY); viewY = -viewY * e
            }
        } else if (ballY > wallMaxY) {
            ballY = wallMaxY
            if (viewY > 0f) {
                bounce(viewY, ballX, wallMaxY); viewY = -viewY * e
            }
        }
    }

    private fun bounce(impactSpeed: Float, px: Float, py: Float) {
        val minSpeed = BallBoxTuning.minBounceSpeed * density
        if (impactSpeed < minSpeed) return
        if (!isVisibleY(py)) return
        val refSpeed = BallBoxTuning.bounceRefSpeed * density
        val impact = ((impactSpeed - minSpeed) / (refSpeed - minSpeed)).coerceIn(0f, 1f)
        val scale = BallBoxTuning.bounceMinScale + impact * (BallBoxTuning.bounceMaxScale - BallBoxTuning.bounceMinScale)
        playPrim(bouncePrimitives, scale, EFFECT_CLICK)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchX = event.x
                touchY = event.y
                if (length(event.x - ballX, event.y - ballY) <= grabRadius()) {
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
                    val dx = width * 0.5f - ballX
                    val dy = height * 0.5f - ballY
                    val d = length(dx, dy)
                    if (d > 0.001f) {
                        val cap = BallBoxTuning.launchMaxSpeed * density
                        val sp = (stretch * BallBoxTuning.launchGain).coerceAtMost(cap)
                        viewX = dx / d * sp
                        viewY = dy / d * sp
                    }
                    airborne = true
                    val prog = (stretch / maxStretch).coerceIn(0f, 1f)
                    if (isVisibleY(ballY)) {
                        if (prog > 0.66f) playPrim(launchPrimitives, 0.5f + 0.5f * prog, EFFECT_HEAVY_CLICK)
                        else playPrim(launchPrimitives, 0.4f + 0.5f * prog, EFFECT_CLICK)
                    }
                } else {
                    val maxSpeed = BallBoxTuning.maxSpeed * density
                    viewX = fingerVx.coerceIn(-maxSpeed, maxSpeed)
                    viewY = fingerVy.coerceIn(-maxSpeed, maxSpeed)
                    if (isVisibleY(ballY)) playPrim(crackPrimitives, BallBoxTuning.dropScale, EFFECT_TICK)
                }
                stretch = 0f
                startLoop()
            }
        }
        return true
    }

    private fun grabRadius(): Float = ballRadius + BallBoxTuning.grabPadding.dp

    private fun beginGrab() {
        grabbed = true
        captured = false
        isPressed = true
        grabOffsetX = ballX - touchX
        grabOffsetY = ballY - touchY
        viewX = 0f
        viewY = 0f
        stretch = 0f
        airborne = false
        lastCrackLevel = 0
        lastCrackTime = 0L
        prevBallX = ballX
        prevBallY = ballY
        fingerVx = 0f
        fingerVy = 0f
        pendingCatch = false
        parent?.requestDisallowInterceptTouchEvent(true)
        playPrim(grabPrimitives, BallBoxTuning.grabScale, EFFECT_CLICK)
        startLoop()
    }

    private fun positionGrabbed() {
        val tx = touchX + grabOffsetX
        val ty = touchY + grabOffsetY
        val ex = tx.coerceIn(wallMinX, wallMaxX)
        val ey = ty.coerceIn(wallMinY, wallMaxY)
        val ox = tx - ex
        val oy = ty - ey
        val om = length(ox, oy)
        if (om < 0.5f) {
            ballX = ex
            ballY = ey
            stretch = 0f
            lastCrackLevel = 0
        } else {
            val mag = rubberBand(om, maxStretch, BallBoxTuning.dragDamping)
            ballX = ex + ox / om * mag
            ballY = ey + oy / om * mag
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

    override fun onDraw(canvas: Canvas) {
        canvas.drawCircle(restX, restY, ringRadius, ringPaint)
        canvas.drawCircle(ballX, ballY, ballRadius * grabScale, ballPaint)
    }

    private fun playCrack(scale: Float) {
        val prim = when ((BallBoxTuning.crackPrimitive + 0.5f).toInt()) {
            0 -> primitiveLowTick
            2 -> primitiveClick
            else -> primitiveTick
        }
        val s = scale.coerceIn(0.01f, 1f)
        if (prim in supportedPrimitives) playPrimitive(prim, s) else playEffect(EFFECT_TICK)
    }

    private fun playPrim(primitives: IntArray, scale: Float, fallbackEffect: Int) {
        val safeScale = scale.coerceIn(0.01f, 1f)
        for (p in primitives) {
            if (p in supportedPrimitives) {
                playPrimitive(p, safeScale)
                return
            }
        }
        playEffect(fallbackEffect)
    }

    private fun isVisibleY(y: Float): Boolean {
        if (!getLocalVisibleRect(visibleRect)) return false
        return y >= visibleRect.top && y <= visibleRect.bottom
    }

    private fun length(x: Float, y: Float): Float = sqrt(x * x + y * y)

    private companion object {
        val EFFECT_TICK = VibrationEffect.EFFECT_TICK
        val EFFECT_CLICK = VibrationEffect.EFFECT_CLICK
        val EFFECT_HEAVY_CLICK = VibrationEffect.EFFECT_HEAVY_CLICK

        // 2nd-order Padé approximant of e^(-rate*dt)
        fun exponentialDecay(rate: Float, dt: Float): Float {
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
    var restitution = 0.90f        // wall bounce energy kept (0..1)
    var maxSpeed = 5000f           // (dp/s) hard velocity clamp

    // resting-spot magnet (felt only near the socket and only at low speed)
    var attractionRadius = 60f     // (dp) pull is felt only within this distance of the socket
    var attractionMaxSpeed = 1000f  // (dp/s) pull is felt only when slower than this
    var attractK = 3.0f            // pull stiffness near the socket (1/s^2)
    var attractMax = 600f          // (dp/s^2) cap on the pull
    var captureSpeed = 130f        // (dp/s) latch into the socket only below this speed
    var holdStiffness = 120f       // socket spring stiffness (1/s^2)
    var holdDamping = 9f           // socket spring damping (underdamped -> can resonate)
    var escapeRadiusMul = 1.8f     // escape when displacement > ringRadius * this
    var escapeSpeed = 1100f         // (dp/s) escape when shaken faster than this

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
    var grabPadding = 64f          // (dp) extra touch radius beyond the ball (bigger = easier to grab)
    var grabAttract = 20f          // how fast a grabbed ball glides to the finger (per second)
    var grabLift = 8f             // (dp) the ball rides this far above the finger

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
    var grabGrow = 1.70f           // visual ball scale while held
}

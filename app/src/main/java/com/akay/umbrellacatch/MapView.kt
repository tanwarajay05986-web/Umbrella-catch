package com.akay.umbrellacatch

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// the Stage Mode screen: 30 levels on a winding path (scroll up and down)
class MapView(ctx: Context) : View(ctx), InsetAware {

    var onBack: () -> Unit = {}
    var onPlay: (Int, Int) -> Unit = { _, _ -> }
    var onToast: (String) -> Unit = {}

    private var insetTop = (24 * resources.displayMetrics.density).toInt()
    private var insetBottom = 0
    private var animT = 0f
    private var scroll = 0f
    private var vel = 0f
    private var inited = false
    private var moved = false
    private var downY = 0f
    private var lastY = 0f
    private var vt: VelocityTracker? = null
    private val slop = ViewConfiguration.get(ctx).scaledTouchSlop
    private var panelLevel = 0
    private var alive = false
    private var last = 0L

    private val backR = RectF()
    private val closeR = RectF()
    private val panelR = RectF()
    private val rowR = Array(3) { RectF() }

    // 0 locked, 1 current, 2 done, 3 coming soon
    private val state = IntArray(Levels.TOTAL + 1)
    private val sd = Array(Levels.TOTAL + 1) { BooleanArray(3) }

    private val white = 0xFFFFFFFF.toInt()
    private val gold = 0xFFFFC21A.toInt()
    private val yellow = 0xFFFFD43B.toInt()
    private val navy = 0xFF0A2A66.toInt()

    init {
        for (l in 1..Levels.TOTAL) {
            for (s in 0..2) sd[l][s] = Store.stageDone(l, s)
            state[l] = when {
                l > Levels.PLAYABLE -> 3
                !Store.levelOpen(l) -> 0
                Store.levelDone(l) -> 2
                else -> 1
            }
        }
    }

    private val frame: Runnable = object : Runnable {
        override fun run() {
            if (!alive) return
            val now = System.nanoTime()
            var dt = (now - last) / 1_000_000_000f
            last = now
            if (dt > 0.05f) dt = 0.05f
            animT += dt
            if (vel != 0f) {
                scroll += vel * dt
                vel *= (1f - 2.6f * dt)
                if (abs(vel) < 30f) vel = 0f
                clampScroll()
            }
            invalidate()
            postOnAnimation(this)
        }
    }

    override fun setInsets(top: Int, bottom: Int) {
        insetTop = top
        insetBottom = bottom
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        alive = true
        last = System.nanoTime()
        postOnAnimation(frame)
    }

    override fun onDetachedFromWindow() {
        alive = false
        removeCallbacks(frame)
        super.onDetachedFromWindow()
    }

    // ---------- geometry ----------

    private fun kOf(): Float = min(width / 941f, (height - insetBottom) / 1672f)

    private fun gapY(k: Float): Float = 200f * k

    private fun topPad(k: Float): Float = 330f * k

    private fun contentH(k: Float): Float = topPad(k) + (Levels.TOTAL - 1) * gapY(k) + 420f * k

    private fun nodeY(l: Int, k: Float): Float = topPad(k) + (Levels.TOTAL - l) * gapY(k)

    private fun nodeX(l: Int, w: Float): Float = w * (0.5f + 0.27f * sin((l - 1) * 1.15f))

    private fun clampScroll() {
        val mx = max(0f, contentH(kOf()) - height)
        if (scroll < 0f) {
            scroll = 0f
            vel = 0f
        }
        if (scroll > mx) {
            scroll = mx
            vel = 0f
        }
    }

    private fun initialScroll(k: Float, h: Float): Float {
        var cur = 1
        while (cur < Levels.PLAYABLE && state[cur] == 2) cur++
        return (nodeY(cur, k) - h * 0.55f).coerceIn(0f, max(0f, contentH(k) - h))
    }

    // ---------- touch ----------

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downY = e.y
                lastY = e.y
                moved = false
                vel = 0f
                vt?.recycle()
                vt = VelocityTracker.obtain()
                vt?.addMovement(e)
            }
            MotionEvent.ACTION_MOVE -> {
                vt?.addMovement(e)
                if (!moved && abs(e.y - downY) > slop) moved = true
                if (moved && panelLevel == 0) {
                    scroll -= (e.y - lastY)
                    clampScroll()
                }
                lastY = e.y
            }
            MotionEvent.ACTION_UP -> {
                vt?.addMovement(e)
                if (!moved) {
                    tap(e.x, e.y)
                } else if (panelLevel == 0) {
                    vt?.computeCurrentVelocity(1000)
                    vel = -(vt?.yVelocity ?: 0f)
                }
                vt?.recycle()
                vt = null
            }
            MotionEvent.ACTION_CANCEL -> {
                vt?.recycle()
                vt = null
            }
        }
        return true
    }

    private fun tap(x: Float, y: Float) {
        if (panelLevel > 0) {
            if (closeR.contains(x, y) || !panelR.contains(x, y)) {
                panelLevel = 0
                return
            }
            for (i in 0..2) {
                if (rowR[i].contains(x, y)) {
                    val open = i == 0 || sd[panelLevel][i - 1] || Levels.TEST_UNLOCK_ALL
                    if (open) {
                        val l = panelLevel
                        panelLevel = 0
                        onPlay(l, i)
                    } else {
                        onToast("Finish Stage ${Levels.letter(i - 1)} first")
                    }
                    return
                }
            }
            return
        }
        if (backR.contains(x, y)) {
            onBack()
            return
        }
        val k = kOf()
        val w = width.toFloat()
        for (l in 1..Levels.TOTAL) {
            val nx = nodeX(l, w)
            val ny = nodeY(l, k) - scroll
            if (hypot(x - nx, y - ny) < 78f * k) {
                openLevel(l)
                return
            }
        }
    }

    private fun openLevel(l: Int) {
        if (l > Levels.PLAYABLE) {
            onToast("More levels coming soon!")
            return
        }
        if (state[l] == 0) {
            onToast("Finish Level ${l - 1} first")
            return
        }
        if (!Levels.hasStages(l)) onPlay(l, 0) else panelLevel = l
    }

    // ---------- drawing ----------

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val k = kOf()
        if (!inited) {
            inited = true
            scroll = initialScroll(k, h)
        }
        clampScroll()
        val p = D.p

        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, 0f, 0f, h, 0xFF2D9BF4.toInt(), 0xFFBDEBFF.toInt(), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null

        val span = h + 500f * k
        for (i in 0 until 7) {
            var cy = (i * 430f * k - scroll * 0.35f) % span
            if (cy < 0f) cy += span
            cy -= 250f * k
            val cx = w * (0.12f + 0.15f * ((i * 5) % 7)) + sin(animT * 0.3f + i) * 14f * k
            D.cloud(c, cx, cy, (80f + (i % 3) * 22f) * k)
        }

        drawMeadow(c, w, h, k)

        for (l in 1..Levels.TOTAL) {
            if (l % 3 != 2) continue
            val y = nodeY(l, k) - scroll + 60f * k
            if (y < -200f * k || y > h + 200f * k) continue
            val left = (l / 3) % 2 == 0
            D.pine(c, w * (if (left) 0.07f else 0.93f), y, 160f * k, 0xFF1F8A4C.toInt(), 0xFF2BA85A.toInt())
        }

        for (l in 1 until Levels.TOTAL) {
            val x0 = nodeX(l, w)
            val y0 = nodeY(l, k) - scroll
            val x1 = nodeX(l + 1, w)
            val y1 = nodeY(l + 1, k) - scroll
            if (y0 < -100f * k && y1 < -100f * k) continue
            if (y0 > h + 100f * k && y1 > h + 100f * k) continue
            val col = if (state[l] == 2) 0xFFFFE066.toInt() else 0xAAFFFFFF.toInt()
            for (t in 1..7) {
                val f = t / 8f
                D.circle(c, x0 + (x1 - x0) * f, y0 + (y1 - y0) * f, 8f * k, col)
            }
        }

        for (l in 1..Levels.TOTAL) {
            val y = nodeY(l, k) - scroll
            if (y < -140f * k || y > h + 140f * k) continue
            drawNode(c, l, nodeX(l, w), y, k)
        }

        drawHeader(c, w, k)
        if (panelLevel > 0) drawPanel(c, w, h, k)
    }

    private fun drawMeadow(c: Canvas, w: Float, h: Float, k: Float) {
        val gy = nodeY(1, k) + 120f * k - scroll
        if (gy > h) return
        val p = D.p
        D.path.reset()
        D.path.moveTo(0f, gy + 10f * k)
        D.path.quadTo(w * 0.25f, gy - 40f * k, w * 0.5f, gy + 5f * k)
        D.path.quadTo(w * 0.78f, gy + 45f * k, w, gy - 15f * k)
        D.path.lineTo(w, h + 20f)
        D.path.lineTo(0f, h + 20f)
        D.path.close()
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, gy, 0f, gy + 500f * k, 0xFF8EEA5E.toInt(), 0xFF3DB03C.toInt(), Shader.TileMode.CLAMP)
        c.drawPath(D.path, p)
        p.shader = null
        D.fence(c, 0f, w, gy + 30f * k, 60f * k, 70f * k)
        val cols = intArrayOf(0xFFFF6FA0.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFA23B.toInt(), 0xFFEA6FFF.toInt())
        for (i in 0 until 9) {
            D.flower(c, w * (0.06f + 0.11f * i), gy + (150f + (i % 3) * 45f) * k, 14f * k, cols[i % 4])
        }
    }

    private fun drawNode(c: Canvas, l: Int, x: Float, y: Float, k: Float) {
        val st = state[l]
        val rad = 62f * k
        D.circle(c, x, y + 9f * k, rad + 7f * k, 0x55102050)
        if (st == 1) {
            D.ring(c, x, y, rad + 16f * k + sin(animT * 4f) * 5f * k, 0xCCFFE066.toInt(), 6f * k)
        }
        D.circle(c, x, y, rad + 7f * k, white)
        val c1: Int
        val c2: Int
        val sc: Int
        when (st) {
            2 -> {
                c1 = 0xFF55E36F.toInt(); c2 = 0xFF1D9F39.toInt(); sc = 0xFF0F6B22.toInt()
            }
            1 -> {
                c1 = 0xFFFFD23B.toInt(); c2 = 0xFFFF8A00.toInt(); sc = 0xFF9A4A00.toInt()
            }
            else -> {
                c1 = 0xFFB9C6DC.toInt(); c2 = 0xFF7F8FAB.toInt(); sc = 0xFF4A5A78.toInt()
            }
        }
        D.gradCircle(c, x, y, rad, c1, c2)
        D.p.color = 0x40FFFFFF
        D.rf.set(x - rad * 0.62f, y - rad * 0.92f, x + rad * 0.62f, y - rad * 0.18f)
        c.drawOval(D.rf, D.p)

        val sz = if (l >= 10) 52f * k else 60f * k
        D.text(c, l.toString(), x, y + sz * 0.36f, sz, white, Paint.Align.CENTER, sc, 7f * k)

        if (st == 0 || st == 3) {
            val bx = x + rad * 0.72f
            val by = y + rad * 0.72f
            D.circle(c, bx, by, 25f * k, white)
            D.circle(c, bx, by, 20f * k, 0xFF6F7F9C.toInt())
            D.icon(c, K.LOCK, bx, by, 24f * k, white)
        }

        if (Levels.hasStages(l) && l <= Levels.PLAYABLE) {
            val dy = y + rad + 36f * k
            for (i in 0..2) {
                val dx = x + (i - 1) * 32f * k
                if (sd[l][i]) {
                    D.circle(c, dx, dy, 13f * k, white)
                    D.circle(c, dx, dy, 10f * k, 0xFF3DDB5A.toInt())
                } else {
                    D.circle(c, dx, dy, 13f * k, 0xAA3A6AB0.toInt())
                    D.circle(c, dx, dy, 10f * k, 0x88FFFFFF.toInt())
                }
            }
        }
    }

    private fun drawHeader(c: Canvas, w: Float, k: Float) {
        val top = insetTop.toFloat()
        D.p.style = Paint.Style.FILL
        D.p.shader = LinearGradient(0f, 0f, 0f, top + 150f * k, 0xDD2D9BF4.toInt(), 0x002D9BF4, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, top + 150f * k, D.p)
        D.p.shader = null

        val cy = top + 64f * k
        val bcx = w * 0.095f
        val br = 46f * k
        D.hudCircle(c, bcx, cy, br, k)
        D.icon(c, K.ARROW_L, bcx - 2f * k, cy, 34f * k, white)
        backR.set(bcx - br - 14f * k, cy - br - 14f * k, bcx + br + 14f * k, cy + br + 14f * k)

        val tl = w * 0.19f
        val tr = w * 0.69f
        D.gloss(c, tl, cy - 38f * k, tr, cy + 38f * k, 0xFFB56DFF.toInt(), 0xFF6C2BD8.toInt(), 0xFFD9B8FF.toInt(), 0x66331166, k)
        D.icon(c, K.CROWN, tl + 56f * k, cy, 46f * k, 0xFFFFD23B.toInt())
        D.text(
            c, "Stage Mode", (tl + tr) / 2f + 24f * k, cy + 16f * k,
            min(46f * k, D.fit("Stage Mode", (tr - tl) - 150f * k)), white, Paint.Align.CENTER, 0xFF3A1280.toInt(), 6f * k
        )

        var done = 0
        for (l in 1..Levels.PLAYABLE) if (state[l] == 2) done++
        val pl2 = w * 0.72f
        val pr2 = w * 0.985f
        D.hudPill(c, pl2, cy - 40f * k, pr2, cy + 40f * k, k)
        D.icon(c, K.TROPHY, pl2 + 44f * k, cy, 50f * k, gold)
        D.text(c, "$done/${Levels.PLAYABLE}", pl2 + 82f * k, cy + 15f * k, 38f * k, yellow, Paint.Align.LEFT, navy, 5f * k)
    }

    // the stage chooser of a level (A, B, C)
    private fun drawPanel(c: Canvas, w: Float, h: Float, k: Float) {
        val lv = panelLevel
        D.p.shader = null
        D.p.style = Paint.Style.FILL
        D.p.color = 0xAA0A1B3D.toInt()
        c.drawRect(0f, 0f, w, h, D.p)

        val hh = h - insetBottom
        val pl = w * 0.07f
        val pr = w * 0.93f
        val pt = hh * 0.2f
        val pb = pt + 730f * k
        panelR.set(pl, pt, pr, pb)
        D.fillRR(c, pl - 6f * k, pt - 6f * k, pr + 6f * k, pb + 6f * k, 46f * k, white)
        D.gradRR(c, pl, pt, pr, pb, 42f * k, 0xFF3F9BFF.toInt(), 0xFF1450C8.toInt())
        D.text(c, "Level $lv", w / 2f, pt + 110f * k, 70f * k, white, Paint.Align.CENTER, navy, 9f * k)
        val sub = "Finish all 3 stages to open the next level"
        D.text(c, sub, w / 2f, pt + 160f * k, min(28f * k, D.fit(sub, (pr - pl) - 60f * k)), 0xFFD6E8FF.toInt())

        val cxX = pr - 48f * k
        val cyX = pt + 48f * k
        D.circle(c, cxX, cyX, 30f * k, 0x55FFFFFF)
        D2.cross(c, cxX, cyX, 11f * k, white, 6f * k)
        closeR.set(cxX - 44f * k, cyX - 44f * k, cxX + 44f * k, cyX + 44f * k)

        for (i in 0..2) {
            val y0 = pt + 200f * k + i * 168f * k
            val y1 = y0 + 148f * k
            val rl = pl + 24f * k
            val rr = pr - 24f * k
            val done = sd[lv][i]
            val open = i == 0 || sd[lv][i - 1] || Levels.TEST_UNLOCK_ALL
            val c1: Int
            val c2: Int
            val bd: Int
            val dk: Int
            if (done) {
                c1 = 0xFF55E36F.toInt(); c2 = 0xFF1D9F39.toInt(); bd = 0xFFC9F7A8.toInt(); dk = 0xFF0F6B22.toInt()
            } else if (open) {
                c1 = 0xFFB56DFF.toInt(); c2 = 0xFF6C2BD8.toInt(); bd = 0xFFD9B8FF.toInt(); dk = 0xFF3A1280.toInt()
            } else {
                c1 = 0xFF9AA9C2.toInt(); c2 = 0xFF6F7F9C.toInt(); bd = 0xFFD0D8E8.toInt(); dk = 0xFF3F4B63.toInt()
            }
            D.fillRR(c, rl - 4f * k, y0 - 4f * k, rr + 4f * k, y1 + 4f * k, 34f * k, bd)
            D.gradRR(c, rl, y0, rr, y1, 30f * k, c1, c2)
            rowR[i].set(rl, y0, rr, y1)

            val my = (y0 + y1) / 2f
            D.circle(c, rl + 72f * k, my, 46f * k, white)
            D.text(c, Levels.letter(i), rl + 72f * k, my + 20f * k, 56f * k, dk)
            D.text(c, "Stage ${Levels.letter(i)}", rl + 140f * k, y0 + 64f * k, 44f * k, white, Paint.Align.LEFT, dk, 6f * k)
            val desc = Levels.describe(lv, i)
            val dw = (rr - 70f * k) - (rl + 140f * k) - 20f * k
            D.text(c, desc, rl + 140f * k, y0 + 112f * k, min(28f * k, D.fit(desc, dw)), white, Paint.Align.LEFT)
            val ix = rr - 56f * k
            when {
                done -> D.icon(c, K.CHECK, ix, my, 54f * k, white)
                open -> D.icon(c, K.PLAY, ix, my, 54f * k, white)
                else -> D.icon(c, K.LOCK, ix, my, 50f * k, white)
            }
        }
    }
}

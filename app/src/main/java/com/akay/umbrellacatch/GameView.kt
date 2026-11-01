package com.akay.umbrellacatch

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private class Drop(val x: Float, var y: Float, val vy: Float, val color: Int, val phase: Float)
private class Pop(val x: Float, var y: Float, var life: Float)
private class Landed(val x: Float, val y: Float, val color: Int, var life: Float)
private class Streak(var x: Float, var y: Float, val len: Float, val spd: Float)

// Classic Mode: the whole game screen is drawn by this one view
class GameView(ctx: Context) : View(ctx), InsetAware {

    var onHome: () -> Unit = {}

    private var insetTop = (24 * resources.displayMetrics.density).toInt()
    private var insetBottom = 0

    private val rnd = Random(System.nanoTime())
    private val drops = ArrayList<Drop>()
    private val pops = ArrayList<Pop>()
    private val landed = ArrayList<Landed>()
    private val rain = ArrayList<Streak>()

    private var time = 0f
    private var introT = 3.2f
    private var score = 0
    private var best = Store.best()
    private var bx = 0.5f
    private var dirL = false
    private var dirR = false
    private var dragX = -1f
    private var paused = false
    private var spawnT = 0.8f
    private var gap = 1.15f
    private var speedK = 0.2f
    private var wob = 0f
    private var last = 0L
    private var alive = false
    private var pausePressed = false

    private val pauseR = RectF()
    private val resumeR = RectF()
    private val homeR = RectF()
    private var lcx = 0f
    private var rcx = 0f
    private var lcy = 0f
    private var btnR = 0f

    private val white = 0xFFFFFFFF.toInt()
    private val gold = 0xFFFFC21A.toInt()
    private val yellow = 0xFFFFD43B.toInt()
    private val navy = 0xFF0A2A66.toInt()

    init {
        for (i in 0 until 36) {
            rain.add(Streak(rnd.nextFloat(), rnd.nextFloat(), 0.03f + rnd.nextFloat() * 0.03f, 0.9f + rnd.nextFloat() * 0.6f))
        }
    }

    private val frame: Runnable = object : Runnable {
        override fun run() {
            if (!alive) return
            val now = System.nanoTime()
            var dt = (now - last) / 1_000_000_000f
            last = now
            if (dt > 0.05f) dt = 0.05f
            update(dt)
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

    fun pauseGame() {
        paused = true
    }

    // the phone's back button: first it pauses the game, then it goes home
    fun backPressed(): Boolean {
        if (!paused) {
            paused = true
            return true
        }
        return false
    }

    private fun rimYOf(hh: Float, bw: Float): Float = hh * 0.862f - bw * 0.86f

    private fun update(dt: Float) {
        for (s in rain) {
            s.y += s.spd * dt
            s.x -= 0.05f * dt
            if (s.y > 1.05f) {
                s.y = -0.05f
                s.x = rnd.nextFloat()
            }
            if (s.x < -0.05f) s.x = 1.05f
        }
        if (paused) return
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val hh = h - insetBottom

        time += dt
        if (introT > 0f) introT -= dt
        if (wob > 0f) wob = (wob - dt * 3f).coerceAtLeast(0f)

        val bw = w * 0.24f
        val half = bw * 0.55f / w
        if (dirL && !dirR) {
            bx -= 1.2f * dt
        } else if (dirR && !dirL) {
            bx += 1.2f * dt
        } else if (dragX >= 0f) {
            bx += (dragX - bx) * min(1f, dt * 14f)
        }
        bx = bx.coerceIn(half, 1f - half)

        spawnT -= dt
        if (spawnT <= 0f) {
            spawnT = gap * (0.75f + rnd.nextFloat() * 0.5f)
            drops.add(
                Drop(
                    0.12f + rnd.nextFloat() * 0.76f, -w * 0.12f,
                    speedK * hh * (0.88f + rnd.nextFloat() * 0.24f),
                    D.UMB[rnd.nextInt(D.UMB.size)], rnd.nextFloat() * 6.28f
                )
            )
        }

        val ur = w * 0.095f
        val rimY = rimYOf(hh, bw)
        val iter = drops.iterator()
        while (iter.hasNext()) {
            val d = iter.next()
            d.y += d.vy * dt
            val dx = (d.x + sin(time * 2f + d.phase) * 0.012f) * w
            if (d.y > rimY - ur * 0.15f && d.y < rimY + bw * 0.35f && abs(dx - bx * w) < bw * 0.46f) {
                score += 10
                if (score > best) {
                    best = score
                    Store.setBest(best)
                }
                landed.add(Landed(dx, d.y, d.color, 0.28f))
                pops.add(Pop(dx, rimY - ur, 1f))
                Sfx.catch()
                wob = 1f
                gap = (gap - 0.012f).coerceAtLeast(0.5f)
                speedK = (speedK + 0.003f).coerceAtMost(0.5f)
                iter.remove()
            } else if (d.y > hh + ur * 2f) {
                iter.remove()
            }
        }

        val li = landed.iterator()
        while (li.hasNext()) {
            val ld = li.next()
            ld.life -= dt
            if (ld.life <= 0f) li.remove()
        }
        val pi = pops.iterator()
        while (pi.hasNext()) {
            val pp = pi.next()
            pp.life -= dt * 1.3f
            pp.y -= dt * h * 0.1f
            if (pp.life <= 0f) pi.remove()
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val act = e.actionMasked
        if (paused) {
            if (act == MotionEvent.ACTION_UP) {
                if (resumeR.contains(e.x, e.y)) {
                    paused = false
                } else if (homeR.contains(e.x, e.y)) {
                    onHome()
                }
            }
            return true
        }
        if (act == MotionEvent.ACTION_DOWN && pauseR.contains(e.x, e.y)) {
            pausePressed = true
            return true
        }
        if (pausePressed) {
            if (act == MotionEvent.ACTION_UP || act == MotionEvent.ACTION_CANCEL) {
                pausePressed = false
                if (act == MotionEvent.ACTION_UP && pauseR.contains(e.x, e.y)) paused = true
                dirL = false
                dirR = false
                dragX = -1f
            }
            return true
        }
        val ending = act == MotionEvent.ACTION_UP || act == MotionEvent.ACTION_CANCEL
        var l = false
        var r = false
        var drag = -1f
        val w = width.toFloat()
        if (!ending && w > 0f) {
            for (i in 0 until e.pointerCount) {
                if (act == MotionEvent.ACTION_POINTER_UP && i == e.actionIndex) continue
                val x = e.getX(i)
                val y = e.getY(i)
                if (hypot(x - lcx, y - lcy) < btnR * 1.3f) {
                    l = true
                } else if (hypot(x - rcx, y - lcy) < btnR * 1.3f) {
                    r = true
                } else if (!pauseR.contains(x, y)) {
                    drag = x / w
                }
            }
        }
        dirL = l
        dirR = r
        dragX = drag
        return true
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val hh = h - insetBottom
        val k = min(w / 941f, hh / 1672f)
        val top = insetTop.toFloat()
        val p = D.p

        D.gameSky(c, w, h, hh, k, time)

        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = 3f * k
        p.color = 0x66FFFFFF
        for (s in rain) {
            val sx = s.x * w
            val sy = s.y * hh
            c.drawLine(sx, sy, sx - s.len * hh * 0.15f, sy + s.len * hh, p)
        }
        p.style = Paint.Style.FILL

        D.gameGround(c, w, h, hh, k)

        val ur = w * 0.095f
        for (d in drops) {
            val dx = (d.x + sin(time * 2f + d.phase) * 0.012f) * w
            D.umbrella(c, dx, d.y, ur, d.color)
        }

        // bucket
        val bw = w * 0.24f
        val bh = bw * 0.86f
        val rimY = rimYOf(hh, bw)
        val bxp = bx * w
        c.save()
        c.scale(1f + wob * 0.08f, 1f - wob * 0.06f, bxp, rimY + bh)
        D.bucketBack(c, bxp, rimY, bw)
        for (ld in landed) {
            val pr = 1f - ld.life / 0.28f
            val px = ld.x + (bxp - ld.x) * pr
            val py = ld.y + (rimY + bh * 0.35f - ld.y) * pr
            val sc = 1f - 0.5f * pr
            c.save()
            c.translate(px, py)
            c.scale(sc, sc)
            D.umbrella(c, 0f, 0f, ur, ld.color)
            c.restore()
        }
        D.bucketFront(c, bxp, rimY, bw, bh)
        c.restore()

        for (pp in pops) {
            val a = (pp.life.coerceIn(0f, 1f) * 255f).toInt()
            D.text(c, "+10", pp.x, pp.y, 44f * k, D.withA(yellow, a), Paint.Align.CENTER, D.withA(navy, a), 6f * k)
        }

        // the big round buttons
        btnR = 88f * k
        lcx = w * 0.135f
        rcx = w * 0.865f
        lcy = hh * 0.915f
        D.arrowBtn(c, lcx, lcy, btnR, true, dirL, k)
        D.arrowBtn(c, rcx, lcy, btnR, false, dirR, k)

        if (introT > 0f) drawIntro(c, w, hh, k)
        drawHud(c, w, top, k)
        if (paused) drawPause(c, w, h, hh, k)
    }

    private fun drawIntro(c: Canvas, w: Float, hh: Float, k: Float) {
        val a = (min(1f, introT / 0.8f) * 255f).toInt()
        val cy = hh * 0.47f
        val cx = w / 2f
        D.text(c, "Catch", cx, cy - 40f * k, 100f * k, D.withA(white, a), Paint.Align.CENTER, D.withA(0xFF1F5FBF.toInt(), a), 14f * k)
        val s2 = min(120f * k, D.fit("Umbrellas!", 560f * k))
        D.text(c, "Umbrellas!", cx, cy + 70f * k, s2, D.withA(0xFFFFD42A.toInt(), a), Paint.Align.CENTER, D.withA(0xFF2A2A6A.toInt(), a), 14f * k)
        D.fillRR(c, cx - 300f * k, cy + 105f * k, cx + 300f * k, cy + 190f * k, 42f * k, D.withA(0xFF7A1E6E.toInt(), (a * 0.9f).toInt()))
        val s3 = min(38f * k, D.fit("Score high & beat your Best!", 520f * k))
        D.text(c, "Score high & beat your Best!", cx, cy + 160f * k, s3, D.withA(white, a))
    }

    private fun drawHud(c: Canvas, w: Float, top: Float, k: Float) {
        val hy = top + 58f * k
        val hf = 42f * k

        val l1 = w * 0.027f
        val r1 = w * 0.308f
        D.hudPill(c, l1, hy - hf, r1, hy + hf, k)
        D.icon(c, K.TROPHY, l1 + 46f * k, hy, 58f * k, gold)
        D.text(c, "Score", l1 + 92f * k, hy - 6f * k, 26f * k, white, Paint.Align.LEFT)
        D.text(c, score.toString(), l1 + 92f * k, hy + 32f * k, 42f * k, yellow, Paint.Align.LEFT, navy, 5f * k)

        val l2 = w * 0.35f
        val r2 = w * 0.632f
        D.hudPill(c, l2, hy - hf, r2, hy + hf, k)
        D.icon(c, K.CROWN, l2 + 46f * k, hy, 58f * k, gold)
        D.text(c, "Best", l2 + 92f * k, hy - 6f * k, 26f * k, white, Paint.Align.LEFT)
        D.text(c, best.toString(), l2 + 92f * k, hy + 32f * k, 42f * k, yellow, Paint.Align.LEFT, navy, 5f * k)

        val pcx = w * 0.933f
        val pr = 46f * k
        D.hudCircle(c, pcx, hy, pr, k)
        D.icon(c, K.PAUSE, pcx, hy, 40f * k, white)
        pauseR.set(pcx - pr - 14f * k, hy - pr - 14f * k, pcx + pr + 14f * k, hy + pr + 14f * k)
    }

    private fun drawPause(c: Canvas, w: Float, h: Float, hh: Float, k: Float) {
        D.p.shader = null
        D.p.style = Paint.Style.FILL
        D.p.color = 0xAA0A1B3D.toInt()
        c.drawRect(0f, 0f, w, h, D.p)

        val pl = w * 0.14f
        val pr = w * 0.86f
        val pt = hh * 0.3f
        val pb = pt + 520f * k
        D.fillRR(c, pl - 6f * k, pt - 6f * k, pr + 6f * k, pb + 6f * k, 46f * k, white)
        D.gradRR(c, pl, pt, pr, pb, 42f * k, 0xFF3F9BFF.toInt(), 0xFF1450C8.toInt())
        D.text(c, "Paused", w / 2f, pt + 125f * k, 84f * k, white, Paint.Align.CENTER, 0xFF0D2F7A.toInt(), 10f * k)

        val bl = pl + 60f * k
        val br = pr - 60f * k
        val ry = pt + 230f * k
        D.gloss(c, bl, ry - 55f * k, br, ry + 55f * k, 0xFF55E36F.toInt(), 0xFF1D9F39.toInt(), 0xFFFFD84D.toInt(), 0x66105A22, k)
        D.icon(c, K.PLAY, bl + 70f * k, ry, 56f * k, white)
        D.text(c, "Resume", w / 2f + 30f * k, ry + 18f * k, 52f * k, white, Paint.Align.CENTER, 0xFF0F6B22.toInt(), 7f * k)
        resumeR.set(bl, ry - 55f * k, br, ry + 55f * k)

        val hy = pt + 400f * k
        D.gloss(c, bl, hy - 55f * k, br, hy + 55f * k, 0xFFB56DFF.toInt(), 0xFF6C2BD8.toInt(), 0xFFD9B8FF.toInt(), 0x66331166, k)
        D.icon(c, K.HOME, bl + 70f * k, hy, 56f * k, white)
        D.text(c, "Home", w / 2f + 30f * k, hy + 18f * k, 52f * k, white, Paint.Align.CENTER, 0xFF3A1280.toInt(), 7f * k)
        homeR.set(bl, hy - 55f * k, br, hy + 55f * k)
    }
}

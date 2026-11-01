package com.akay.umbrellacatch

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.min
import kotlin.math.sin

// the whole home screen is drawn by this one view
class HomeView(ctx: Context) : View(ctx), InsetAware {

    // ids: 0 coins plus, 2 sound, 3 music, 4 settings, 5 classic, 6 stage,
    // 7 more apps, 8 shop, 9 achievements, 10 rate, 11 share, 12 about
    var onAction: (Int) -> Unit = {}

    private var insetTop = (24 * resources.displayMetrics.density).toInt()
    private var insetBottom = 0
    private var time = 0f
    private var last = 0L
    private var alive = false
    private var down = -1
    private val hitId = IntArray(24)
    private val hitR = Array(24) { RectF() }
    private var hitN = 0

    private val white = 0xFFFFFFFF.toInt()
    private val gold = 0xFFFFC21A.toInt()
    private val yellow = 0xFFFFD43B.toInt()
    private val navy = 0xFF0A2A66.toInt()

    private val frame: Runnable = object : Runnable {
        override fun run() {
            if (!alive) return
            val now = System.nanoTime()
            var dt = (now - last) / 1_000_000_000f
            last = now
            if (dt > 0.05f) dt = 0.05f
            time += dt
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

    private fun hit(id: Int, l: Float, tp: Float, r: Float, b: Float) {
        if (hitN >= hitId.size) return
        hitId[hitN] = id
        hitR[hitN].set(l, tp, r, b)
        hitN++
    }

    private fun hitAt(x: Float, y: Float): Int {
        var i = hitN - 1
        while (i >= 0) {
            if (hitR[i].contains(x, y)) return hitId[i]
            i--
        }
        return -1
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                down = hitAt(e.x, e.y)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                val id = hitAt(e.x, e.y)
                val d = down
                down = -1
                invalidate()
                if (id >= 0 && id == d) onAction(id)
            }
            MotionEvent.ACTION_CANCEL -> {
                down = -1
                invalidate()
            }
        }
        return true
    }

    private fun begin(c: Canvas, id: Int, cx: Float, cy: Float) {
        c.save()
        if (down == id) c.scale(0.95f, 0.95f, cx, cy)
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        hitN = 0
        val hh = h - insetBottom
        val k = min(w / 941f, hh / 1672f)
        val top = insetTop.toFloat()

        D.homeScene(c, w, h, hh, k, time)
        D.rainbow(c, w, hh, k)
        drawTitle(c, w, hh, k)
        D.mascot(c, w * 0.36f, hh * 0.39f + sin(time * 2.2f) * 7f * k, 300f * k)
        drawSign(c, w, hh, k)
        drawModes(c, w, hh, k)
        drawCards(c, w, hh, k)
        drawCircles(c, w, hh, k)
        drawHud(c, w, top, k)
    }

    private fun leaf(c: Canvas, x: Float, y: Float, len: Float, ang: Float) {
        c.save()
        c.rotate(ang, x, y)
        D.rf.set(x, y - len * 0.22f, x + len, y + len * 0.22f)
        D.p.shader = null
        D.p.style = Paint.Style.FILL
        D.p.color = 0xFF3DBB4A.toInt()
        c.drawOval(D.rf, D.p)
        c.restore()
    }

    private fun drawTitle(c: Canvas, w: Float, hh: Float, k: Float) {
        val cx = w / 2f
        val ty = hh * 0.215f
        D.umbrella(c, cx + 22f * k, ty - 50f * k, 175f * k, 0xFFE5322D.toInt())

        val drop = 0xFFA8E4FF.toInt()
        D.circle(c, cx - 280f * k, ty - 95f * k, 13f * k, drop)
        D.circle(c, cx - 235f * k, ty - 140f * k, 9f * k, drop)
        D.circle(c, cx + 250f * k, ty - 120f * k, 12f * k, drop)
        D.circle(c, cx + 300f * k, ty - 80f * k, 9f * k, drop)

        D.gradRR(c, cx - 335f * k, ty - 70f * k, cx + 335f * k, ty + 115f * k, 36f * k, 0xFFE3A85C.toInt(), 0xFFB06F30.toInt())
        D.strokeRR(c, cx - 335f * k, ty - 70f * k, cx + 335f * k, ty + 115f * k, 36f * k, 0xFF6E3F18.toInt(), 8f * k)
        D.p.style = Paint.Style.STROKE
        D.p.strokeWidth = 3f * k
        D.p.color = 0x22000000
        c.drawLine(cx - 300f * k, ty - 30f * k, cx + 300f * k, ty - 30f * k, D.p)
        c.drawLine(cx - 300f * k, ty + 60f * k, cx + 300f * k, ty + 60f * k, D.p)
        D.p.style = Paint.Style.FILL

        leaf(c, cx - 330f * k, ty + 90f * k, 90f * k, 165f)
        leaf(c, cx - 325f * k, ty + 70f * k, 80f * k, 205f)
        leaf(c, cx + 325f * k, ty - 55f * k, 90f * k, -25f)
        leaf(c, cx + 320f * k, ty - 40f * k, 80f * k, 15f)

        val s1 = min(150f * k, D.fit("Umbrella", 560f * k))
        D.text(c, "Umbrella", cx, ty + 44f * k, s1, 0xFF8A3E08.toInt(), Paint.Align.CENTER, 0xFF8A3E08.toInt(), 16f * k)
        D.text(c, "Umbrella", cx, ty + 36f * k, s1, 0xFFFFD42A.toInt(), Paint.Align.CENTER, 0xFFB8600F.toInt(), 12f * k)

        val s2 = min(125f * k, D.fit("Catch", 300f * k))
        val c2 = cx + 60f * k
        D.text(c, "Catch", c2, ty + 147f * k, s2, 0xFF123F8F.toInt(), Paint.Align.CENTER, 0xFF123F8F.toInt(), 14f * k)
        D.text(c, "Catch", c2, ty + 140f * k, s2, white, Paint.Align.CENTER, 0xFF2A6FD6.toInt(), 12f * k)

        val tw = 0.7f + 0.3f * sin(time * 3f)
        D.star(c, cx - 300f * k, ty - 20f * k, 18f * k * tw, 0xFFFFE066.toInt())
        D.star(c, cx + 305f * k, ty + 130f * k, 15f * k * (1.6f - tw), 0xFFFFE066.toInt())
    }

    private fun drawSign(c: Canvas, w: Float, hh: Float, k: Float) {
        val sx = w * 0.87f
        val sy = hh * 0.425f
        D.fillRR(c, sx - 14f * k, sy + 60f * k, sx + 14f * k, sy + 190f * k, 6f * k, 0xFF8A5528.toInt())
        c.save()
        c.rotate(7f, sx, sy)
        D.gradRR(c, sx - 105f * k, sy - 78f * k, sx + 105f * k, sy + 78f * k, 14f * k, 0xFFD99A52.toInt(), 0xFFA9672B.toInt())
        D.strokeRR(c, sx - 105f * k, sy - 78f * k, sx + 105f * k, sy + 78f * k, 14f * k, 0xFF6E3F18.toInt(), 5f * k)
        val dk = 0xFF5A3010.toInt()
        D.text(c, "Play", sx + 8f * k, sy - 26f * k, 38f * k, white, Paint.Align.CENTER, dk, 6f * k)
        D.text(c, "Smart", sx + 12f * k, sy + 14f * k, 38f * k, white, Paint.Align.CENTER, dk, 6f * k)
        D.text(c, "Stay Focused!", sx, sy + 52f * k, min(26f * k, D.fit("Stay Focused!", 180f * k)), white, Paint.Align.CENTER, dk, 5f * k)
        val red = 0xFFE5322D.toInt()
        val hx = sx - 66f * k
        val hy = sy + 2f * k
        D.circle(c, hx - 7f * k, hy - 5f * k, 9f * k, red)
        D.circle(c, hx + 7f * k, hy - 5f * k, 9f * k, red)
        D.path.reset()
        D.path.moveTo(hx - 15f * k, hy - 1f * k)
        D.path.lineTo(hx + 15f * k, hy - 1f * k)
        D.path.lineTo(hx, hy + 16f * k)
        D.path.close()
        c.drawPath(D.path, D.p)
        c.restore()
    }

    private fun drawModes(c: Canvas, w: Float, hh: Float, k: Float) {
        val hf = 92f * k

        // Classic Mode
        val l1 = w * 0.133f
        val r1 = w * 0.867f
        val y1 = hh * 0.52f
        begin(c, 5, w / 2f, y1)
        D.gloss(c, l1, y1 - hf, r1, y1 + hf, 0xFF55E36F.toInt(), 0xFF1D9F39.toInt(), 0xFFFFD84D.toInt(), 0x66105A22, k)
        D.icon(c, K.PLAY, l1 + 85f * k, y1, 80f * k, white)
        val tw = (r1 - l1) - 300f * k
        val dg = 0xFF0F6B22.toInt()
        D.text(c, "Classic Mode", w / 2f + 8f * k, y1 - 8f * k, min(66f * k, D.fit("Classic Mode", tw)), white, Paint.Align.CENTER, dg, 8f * k)
        D.text(
            c, "Catch Umbrellas & Score High!", w / 2f + 8f * k, y1 + 46f * k,
            min(30f * k, D.fit("Catch Umbrellas & Score High!", tw)), white, Paint.Align.CENTER, dg, 5f * k
        )
        D.icon(c, K.TROPHY, r1 - 82f * k, y1, 82f * k, 0xFFFFD23B.toInt())
        c.restore()
        hit(5, l1, y1 - hf, r1, y1 + hf)

        // Stage Mode
        val l2 = w * 0.149f
        val r2 = w * 0.855f
        val y2 = hh * 0.652f
        begin(c, 6, w / 2f, y2)
        D.gloss(c, l2, y2 - hf * 0.95f, r2, y2 + hf * 0.95f, 0xFFB56DFF.toInt(), 0xFF6C2BD8.toInt(), 0xFFD9B8FF.toInt(), 0x66331166, k)
        D.icon(c, K.CROWN, l2 + 85f * k, y2, 88f * k, 0xFFFFD23B.toInt())
        val tw2 = (r2 - l2) - 300f * k
        val dp = 0xFF3A1280.toInt()
        D.text(c, "Stage Mode", w / 2f + 8f * k, y2 - 8f * k, min(62f * k, D.fit("Stage Mode", tw2)), white, Paint.Align.CENTER, dp, 8f * k)
        D.text(c, "Complete Levels!", w / 2f + 8f * k, y2 + 44f * k, min(30f * k, D.fit("Complete Levels!", tw2)), white, Paint.Align.CENTER, dp, 5f * k)
        D.bigArrow(c, r2 - 75f * k, y2, 62f * k, white)
        c.restore()
        hit(6, l2, y2 - hf * 0.95f, r2, y2 + hf * 0.95f)
    }

    private fun drawCards(c: Canvas, w: Float, hh: Float, k: Float) {
        val cy = hh * 0.783f
        val hf = 98f * k
        val xs = floatArrayOf(0.058f, 0.367f, 0.664f)
        val xe = floatArrayOf(0.34f, 0.636f, 0.944f)
        for (i in 0..2) {
            val l = w * xs[i]
            val r = w * xe[i]
            val cx = (l + r) / 2f
            begin(c, 7 + i, cx, cy)
            if (i == 1) {
                D.card(c, l, cy - hf, r, cy + hf, 0xFF5BDB4F.toInt(), 0xFF1E9E3A.toInt(), 0xFFC9F7A8.toInt(), k)
            } else {
                D.card(c, l, cy - hf, r, cy + hf, 0xFF2F8CFF.toInt(), 0xFF1450C8.toInt(), 0xFFA7D3FF.toInt(), k)
            }
            val dk = if (i == 1) 0xFF0F6B22.toInt() else 0xFF0D2F7A.toInt()
            when (i) {
                0 -> {
                    D.appsIcon(c, cx, cy - 32f * k, 84f * k)
                    D.text(c, "More", cx, cy + 50f * k, 34f * k, white, Paint.Align.CENTER, dk, 6f * k)
                    D.text(c, "Apps", cx, cy + 86f * k, 34f * k, white, Paint.Align.CENTER, dk, 6f * k)
                    D.fillRR(c, r - 92f * k, cy - hf - 16f * k, r + 6f * k, cy - hf + 34f * k, 22f * k, 0xFFE5322D.toInt())
                    D.strokeRR(c, r - 92f * k, cy - hf - 16f * k, r + 6f * k, cy - hf + 34f * k, 22f * k, white, 3f * k)
                    D.text(c, "New", r - 43f * k, cy - hf + 20f * k, 28f * k, white)
                }
                1 -> {
                    D.shopIcon(c, cx, cy - 24f * k, 104f * k)
                    D.text(c, "Shop", cx, cy + 84f * k, 42f * k, white, Paint.Align.CENTER, dk, 7f * k)
                }
                else -> {
                    D.medalIcon(c, cx, cy - 30f * k, 96f * k)
                    D.text(
                        c, "Achievements", cx, cy + 82f * k,
                        min(34f * k, D.fit("Achievements", (r - l) - 26f * k)), white, Paint.Align.CENTER, dk, 6f * k
                    )
                }
            }
            c.restore()
            hit(7 + i, l, cy - hf, r, cy + hf)
        }
    }

    private fun drawCircles(c: Canvas, w: Float, hh: Float, k: Float) {
        val cy = hh * 0.903f
        val rad = 70f * k
        val xs = floatArrayOf(0.269f, 0.5f, 0.731f)
        val names = arrayOf("Rate Us", "Share", "About")
        val ic = intArrayOf(K.STAR, K.SHARE, K.INFO)
        for (i in 0..2) {
            val cx = w * xs[i]
            begin(c, 10 + i, cx, cy)
            D.circle(c, cx, cy + 6f * k, rad + 4f * k, 0x55102050.toInt())
            D.circle(c, cx, cy, rad + 5f * k, 0xFFB7DBFF.toInt())
            D.gradCircle(c, cx, cy, rad, 0xFF2F8CFF.toInt(), 0xFF1450C8.toInt())
            D.icon(c, ic[i], cx, cy - 20f * k, 54f * k, if (i == 0) gold else white)
            D.text(c, names[i], cx, cy + 40f * k, min(28f * k, D.fit(names[i], 120f * k)), white, Paint.Align.CENTER, 0xFF0D2F7A.toInt(), 5f * k)
            c.restore()
            hit(10 + i, cx - rad, cy - rad, cx + rad, cy + rad)
        }
    }

    private fun drawHud(c: Canvas, w: Float, top: Float, k: Float) {
        val cy = top + 58f * k
        val hf = 42f * k

        // coins
        val l1 = w * 0.027f
        val r1 = w * 0.308f
        D.hudPill(c, l1, cy - hf, r1, cy + hf, k)
        D.icon(c, K.STAR, l1 + 46f * k, cy, 60f * k, gold)
        D.text(c, Store.coins().toString(), l1 + 92f * k, cy + 16f * k, 46f * k, white, Paint.Align.LEFT, navy, 5f * k)
        val px = r1 - 40f * k
        begin(c, 0, px, cy)
        D.gradCircle(c, px, cy, 30f * k, 0xFF52E06C.toInt(), 0xFF1D9F39.toInt())
        D.ring(c, px, cy, 30f * k, white, 3f * k)
        D.icon(c, K.PLUS, px, cy, 30f * k, white)
        c.restore()
        hit(0, px - 30f * k, cy - 30f * k, px + 30f * k, cy + 30f * k)

        // best
        val l2 = w * 0.35f
        val r2 = w * 0.632f
        D.hudPill(c, l2, cy - hf, r2, cy + hf, k)
        D.icon(c, K.CROWN, l2 + 46f * k, cy, 58f * k, gold)
        D.text(c, "Best", l2 + 92f * k, cy - 6f * k, 26f * k, white, Paint.Align.LEFT)
        D.text(c, Store.best().toString(), l2 + 92f * k, cy + 32f * k, 42f * k, yellow, Paint.Align.LEFT, navy, 5f * k)

        // sound, music, settings
        val rr = 43f * k
        val xs = floatArrayOf(0.712f, 0.822f, 0.933f)
        for (i in 0..2) {
            val cx = w * xs[i]
            begin(c, 2 + i, cx, cy)
            D.hudCircle(c, cx, cy, rr, k)
            when (i) {
                0 -> D.icon(c, if (Store.sound()) K.SPEAKER_ON else K.SPEAKER_OFF, cx, cy, 46f * k, yellow)
                1 -> {
                    D.icon(c, K.MUSIC, cx, cy, 44f * k, yellow)
                    if (!Store.music()) D.slash(c, cx, cy, 30f * k, k)
                }
                else -> D.icon(c, K.GEAR, cx, cy, 46f * k, yellow)
            }
            c.restore()
            hit(2 + i, cx - rr, cy - rr, cx + rr, cy + rr)
        }
    }
}

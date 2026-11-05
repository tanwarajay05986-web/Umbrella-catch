package com.akay.umbrellacatch

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

// sky themes, fog, portals, fireworks and the small pictures of the level tips
object Fx {
    private val TOP = intArrayOf(
        0xFF2D9BF4.toInt(), 0xFF6FB6FF.toInt(), 0xFF18A5D8.toInt(),
        0xFF6B3FB0.toInt(), 0xFF0B1B4A.toInt(), 0xFF14052E.toInt()
    )
    private val BOT = intArrayOf(
        0xFFB5E8FF.toInt(), 0xFFFFE0B8.toInt(), 0xFFC9F3FF.toInt(),
        0xFFFFA86B.toInt(), 0xFF3D4E9B.toInt(), 0xFF5B1E8F.toInt()
    )

    // 0 day (levels 1-15), 1 morning, 2 noon, 3 evening, 4 night, 5 space
    fun theme(level: Int): Int = when {
        level <= 15 -> 0
        level <= 18 -> 1
        level <= 21 -> 2
        level <= 24 -> 3
        level <= 27 -> 4
        else -> 5
    }

    private fun drift(base: Float, speed: Float, t: Float, w: Float, m: Float): Float {
        val span = w + 2f * m
        var x = (base + t * speed) % span
        if (x < 0f) x += span
        return x - m
    }

    private fun blob(c: Canvas, cx: Float, cy: Float, r: Float, col: Int) {
        val p = D.p
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(cx, cy, r, col, col and 0x00FFFFFF, Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r, p)
        p.shader = null
    }

    fun sky(c: Canvas, w: Float, h: Float, hh: Float, k: Float, t: Float, th: Int) {
        val p = D.p
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, 0f, 0f, hh, TOP[th], BOT[th], Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null
        if (th == 5) {
            blob(c, w * 0.2f, hh * 0.25f, w * 0.5f, 0x44B03CFF)
            blob(c, w * 0.85f, hh * 0.45f, w * 0.5f, 0x4400C8FF)
            blob(c, w * 0.5f, hh * 0.1f, w * 0.4f, 0x44FF4DB8)
        }
        if (th >= 4) {
            for (i in 0 until 46) {
                val a = sin(i * 12.9898f) * 43758.547f
                val b = sin(i * 78.233f + 3.1f) * 12345.678f
                val fx = abs(a - floor(a))
                val fy = abs(b - floor(b))
                val tw = 0.6f + 0.4f * sin(t * 2f + i)
                D.circle(c, fx * w, fy * hh * 0.7f, (2.2f + (i % 3)) * k * tw, 0xCCFFFFFF.toInt())
            }
        }
        if (th == 4) {
            D.circle(c, w * 0.8f, hh * 0.2f, 62f * k, 0xFFFFF3C4.toInt())
            D.circle(c, w * 0.8f + 26f * k, hh * 0.2f - 10f * k, 56f * k, D.mix(TOP[th], BOT[th], 0.25f))
        }
        if (th <= 3) {
            val m = 200f * k
            D.cloud(c, drift(w * 0.15f, 8f * k, t, w, m), hh * 0.2f, 120f * k)
            D.cloud(c, drift(w * 0.85f, 6f * k, t, w, m), hh * 0.34f, 100f * k)
            D.cloud(c, drift(w * 0.05f, 9f * k, t, w, m), hh * 0.5f, 90f * k)
            D.cloud(c, drift(w * 0.95f, 7f * k, t, w, m), hh * 0.58f, 80f * k)
        }
    }

    // night and space make the ground darker
    fun nightTint(c: Canvas, w: Float, h: Float, hh: Float, th: Int) {
        if (th < 4) return
        val p = D.p
        p.shader = null
        p.style = Paint.Style.FILL
        p.color = if (th == 4) 0x66041033 else 0x77100430
        c.drawRect(0f, hh * 0.64f, w, h, p)
    }

    // dark fog with a clear circle around (cx, cy)
    fun fog(c: Canvas, w: Float, h: Float, cx: Float, cy: Float, r: Float, dark: Int) {
        val p = D.p
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(
            cx, cy, r, intArrayOf(0x00000000, 0x00000000, dark), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null
    }

    fun bolt(c: Canvas, x: Float, h: Float, k: Float, a: Int) {
        val p = D.p
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        p.strokeWidth = 9f * k
        p.color = D.withA(0xFFFFFFFF.toInt(), a)
        val path = D.path
        path.reset()
        path.moveTo(x, 0f)
        path.lineTo(x - 40f * k, h * 0.12f)
        path.lineTo(x + 20f * k, h * 0.15f)
        path.lineTo(x - 50f * k, h * 0.3f)
        path.lineTo(x + 10f * k, h * 0.34f)
        path.lineTo(x - 30f * k, h * 0.48f)
        c.drawPath(path, p)
        p.style = Paint.Style.FILL
    }

    fun portal(c: Canvas, cx: Float, cy: Float, rw: Float, rh: Float, col: Int, t: Float, pop: Float) {
        val s = pop.coerceIn(0.05f, 1f)
        val p = D.p
        p.shader = null
        p.style = Paint.Style.FILL
        p.color = D.withA(col, 70)
        D.rf.set(cx - rw * s, cy - rh * s, cx + rw * s, cy + rh * s)
        c.drawOval(D.rf, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = rw * 0.22f
        p.color = D.withA(col, 230)
        c.drawOval(D.rf, p)
        val q = 0.55f + 0.1f * sin(t * 5f)
        D.rf.set(cx - rw * q * s, cy - rh * q * s, cx + rw * q * s, cy + rh * q * s)
        p.strokeWidth = rw * 0.1f
        p.color = D.withA(0xFFFFFFFF.toInt(), 200)
        c.drawOval(D.rf, p)
        p.style = Paint.Style.FILL
    }

    // a small magnet drawn above a magnet umbrella
    fun magnet(c: Canvas, cx: Float, cy: Float, r: Float) {
        val p = D.p
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.BUTT
        p.strokeWidth = r * 0.4f
        p.color = 0xFFE5322D.toInt()
        D.rf.set(cx - r * 0.55f, cy - r * 0.3f, cx + r * 0.55f, cy + r * 0.8f)
        c.drawArc(D.rf, 180f, 180f, false, p)
        c.drawLine(cx - r * 0.55f, cy + r * 0.25f, cx - r * 0.55f, cy + r * 0.7f, p)
        c.drawLine(cx + r * 0.55f, cy + r * 0.25f, cx + r * 0.55f, cy + r * 0.7f, p)
        p.color = 0xFFFFFFFF.toInt()
        c.drawLine(cx - r * 0.55f, cy + r * 0.55f, cx - r * 0.55f, cy + r * 0.7f, p)
        c.drawLine(cx + r * 0.55f, cy + r * 0.55f, cx + r * 0.55f, cy + r * 0.7f, p)
        p.style = Paint.Style.FILL
    }

    // the picture shown on the tip panel of a stage
    fun mech(c: Canvas, icon: Int, cx: Float, cy: Float, s: Float, t: Float) {
        val p = D.p
        val white = 0xFFFFFFFF.toInt()
        val yel = 0xFFFFD43B.toInt()
        p.shader = null
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        when (icon) {
            Levels.I_WIND -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = s * 0.07f
                p.color = white
                val m = (t * 0.8f) % 1f
                for (i in 0..2) {
                    val y = cy + (i - 1) * s * 0.26f
                    val x0 = cx - s * 0.42f + m * s * 0.12f
                    val x1 = x0 + s * (0.72f - i * 0.1f)
                    c.drawLine(x0, y, x1, y, p)
                    c.drawLine(x1 - s * 0.1f, y - s * 0.08f, x1, y, p)
                    c.drawLine(x1 - s * 0.1f, y + s * 0.08f, x1, y, p)
                }
                p.style = Paint.Style.FILL
            }
            Levels.I_MAGNET -> magnet(c, cx, cy - s * 0.1f, s * 0.5f)
            Levels.I_SIZE -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = s * 0.08f
                p.color = white
                val a = s * (0.12f + 0.06f * sin(t * 4f))
                c.drawLine(cx - s * 0.45f, cy, cx - a, cy, p)
                c.drawLine(cx - a - s * 0.12f, cy - s * 0.1f, cx - a, cy, p)
                c.drawLine(cx - a - s * 0.12f, cy + s * 0.1f, cx - a, cy, p)
                c.drawLine(cx + s * 0.45f, cy, cx + a, cy, p)
                c.drawLine(cx + a + s * 0.12f, cy - s * 0.1f, cx + a, cy, p)
                c.drawLine(cx + a + s * 0.12f, cy + s * 0.1f, cx + a, cy, p)
                p.style = Paint.Style.FILL
                D.bucketBack(c, cx, cy - s * 0.15f, s * 0.26f)
                D.bucketFront(c, cx, cy - s * 0.15f, s * 0.26f, s * 0.22f)
            }
            Levels.I_FOG -> {
                D.cloud(c, cx, cy - s * 0.05f, s * 0.3f)
                for (i in 0..2) D.circle(c, cx + (i - 1) * s * 0.22f, cy + s * 0.32f, s * 0.05f, 0xAAFFFFFF.toInt())
            }
            Levels.I_BOLT -> {
                D.path.reset()
                D.path.moveTo(cx + s * 0.1f, cy - s * 0.5f)
                D.path.lineTo(cx - s * 0.22f, cy + s * 0.05f)
                D.path.lineTo(cx, cy + s * 0.05f)
                D.path.lineTo(cx - s * 0.1f, cy + s * 0.5f)
                D.path.lineTo(cx + s * 0.26f, cy - s * 0.1f)
                D.path.lineTo(cx + s * 0.05f, cy - s * 0.1f)
                D.path.close()
                p.style = Paint.Style.FILL
                p.color = yel
                c.drawPath(D.path, p)
            }
            Levels.I_CLOCK -> D2.clock(c, cx, cy, s * 0.45f, white)
            Levels.I_PORTAL -> {
                portal(c, cx - s * 0.3f, cy, s * 0.16f, s * 0.3f, 0xFF3B8CFF.toInt(), t, 1f)
                portal(c, cx + s * 0.3f, cy, s * 0.16f, s * 0.3f, 0xFFFF8A3B.toInt(), t, 1f)
                D.bigArrow(c, cx, cy, s * 0.3f, white)
            }
            Levels.I_COLOR -> {
                D.circle(c, cx - s * 0.2f, cy + s * 0.08f, s * 0.22f, D.UMB[0])
                D.circle(c, cx + s * 0.2f, cy + s * 0.08f, s * 0.22f, D.UMB[4])
                D.circle(c, cx, cy - s * 0.15f, s * 0.22f, D.UMB[1])
            }
            Levels.I_BUCKETS -> {
                D.bucketBack(c, cx - s * 0.26f, cy - s * 0.1f, s * 0.38f)
                D.bucketFront(c, cx - s * 0.26f, cy - s * 0.1f, s * 0.38f, s * 0.32f)
                D.bucketBack(c, cx + s * 0.26f, cy - s * 0.1f, s * 0.38f)
                D.bucketFront(c, cx + s * 0.26f, cy - s * 0.1f, s * 0.38f, s * 0.32f)
            }
            Levels.I_BOUNCE -> {
                D.umbrella(c, cx - s * 0.15f, cy + s * 0.05f + sin(t * 6f) * s * 0.08f, s * 0.3f, D.UMB[3])
                p.style = Paint.Style.STROKE
                p.strokeWidth = s * 0.07f
                p.color = white
                D.path.reset()
                D.path.moveTo(cx + s * 0.1f, cy + s * 0.3f)
                D.path.lineTo(cx + s * 0.22f, cy - s * 0.05f)
                D.path.lineTo(cx + s * 0.34f, cy + s * 0.2f)
                D.path.lineTo(cx + s * 0.44f, cy - s * 0.25f)
                c.drawPath(D.path, p)
                p.style = Paint.Style.FILL
            }
            Levels.I_GRAV -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = s * 0.08f
                p.color = white
                c.drawLine(cx - s * 0.2f, cy - s * 0.4f, cx - s * 0.2f, cy + s * 0.4f, p)
                c.drawLine(cx - s * 0.3f, cy + s * 0.28f, cx - s * 0.2f, cy + s * 0.4f, p)
                c.drawLine(cx - s * 0.1f, cy + s * 0.28f, cx - s * 0.2f, cy + s * 0.4f, p)
                c.drawLine(cx + s * 0.2f, cy + s * 0.4f, cx + s * 0.2f, cy - s * 0.4f, p)
                c.drawLine(cx + s * 0.1f, cy - s * 0.28f, cx + s * 0.2f, cy - s * 0.4f, p)
                c.drawLine(cx + s * 0.3f, cy - s * 0.28f, cx + s * 0.2f, cy - s * 0.4f, p)
                p.style = Paint.Style.FILL
            }
            Levels.I_ICE -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = s * 0.07f
                p.color = 0xFFD6F4FF.toInt()
                for (i in 0..2) {
                    val a = Math.toRadians(i * 60.0 + t * 20.0)
                    val dx = (cos(a) * s * 0.42f).toFloat()
                    val dy = (sin(a) * s * 0.42f).toFloat()
                    c.drawLine(cx - dx, cy - dy, cx + dx, cy + dy, p)
                }
                p.style = Paint.Style.FILL
            }
            Levels.I_X2 -> D.text(c, "x2", cx, cy + s * 0.25f, s * 0.8f, white, Paint.Align.CENTER, 0xFF3A1280.toInt(), s * 0.07f)
            Levels.I_GOLD -> {
                D.circle(c, cx, cy, s * 0.42f, 0x33FFD43B)
                D.umbrella(c, cx, cy + s * 0.18f, s * 0.42f, 0xFFFFC21A.toInt())
            }
            Levels.I_WAVE -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = s * 0.07f
                p.color = white
                for (row in 0..1) {
                    D.path.reset()
                    val y0 = cy + (row - 0.5f) * s * 0.4f
                    D.path.moveTo(cx - s * 0.45f, y0)
                    for (i in 1..12) {
                        val x = cx - s * 0.45f + i * s * 0.075f
                        D.path.lineTo(x, y0 + sin(i * 0.9f + t * 4f) * s * 0.09f)
                    }
                    c.drawPath(D.path, p)
                }
                p.style = Paint.Style.FILL
            }
            Levels.I_CROWN -> D.icon(c, K.CROWN, cx, cy, s * 0.8f, 0xFFFFD23B.toInt())
            Levels.I_HEART -> D2.heart(c, cx, cy, s * 0.4f, 0xFFFF4D5E.toInt(), 0xFFB8162B.toInt())
            Levels.I_FIRE -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = s * 0.05f
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    p.color = D.UMB[i % D.UMB.size]
                    val dx = cos(a).toFloat()
                    val dy = sin(a).toFloat()
                    c.drawLine(cx + dx * s * 0.15f, cy + dy * s * 0.15f, cx + dx * s * 0.4f, cy + dy * s * 0.4f, p)
                }
                p.style = Paint.Style.FILL
                D.circle(c, cx, cy, s * 0.08f, yel)
            }
            else -> D.umbrella(c, cx, cy + s * 0.15f, s * 0.42f, D.UMB[0])
        }
        p.style = Paint.Style.FILL
    }
}

// fireworks of the victory stage
class Fireworks {
    private class Sp(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float, val col: Int)

    private val list = ArrayList<Sp>()
    private var timer = 0.3f
    private val rnd = Random(System.nanoTime())

    fun update(dt: Float, w: Float, hh: Float) {
        timer -= dt
        if (timer <= 0f) {
            timer = 0.45f + rnd.nextFloat() * 0.5f
            val x = w * (0.12f + 0.76f * rnd.nextFloat())
            val y = hh * (0.14f + 0.3f * rnd.nextFloat())
            val col = D.UMB[rnd.nextInt(D.UMB.size)]
            for (i in 0 until 18) {
                val a = i * 0.349f
                val sp = w * (0.16f + 0.12f * rnd.nextFloat())
                list.add(Sp(x, y, cos(a) * sp, sin(a) * sp, 1f, col))
            }
        }
        val it = list.iterator()
        while (it.hasNext()) {
            val s = it.next()
            s.x += s.vx * dt
            s.y += s.vy * dt
            s.vy += w * 0.25f * dt
            s.vx *= (1f - 0.8f * dt)
            s.life -= dt * 0.9f
            if (s.life <= 0f) it.remove()
        }
    }

    fun draw(c: Canvas, k: Float) {
        for (s in list) {
            val a = (s.life.coerceIn(0f, 1f) * 255f).toInt()
            D.circle(c, s.x, s.y, 7f * k * (0.4f + s.life), D.withA(s.col, a))
        }
    }
}

package com.akay.umbrellacatch

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.cos
import kotlin.math.sin

// screens that need to know the size of the status bar and the navigation bar
interface InsetAware {
    fun setInsets(top: Int, bottom: Int)
}

// every picture of the game is drawn here in code (no image files)
object D {
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    val path = Path()
    val rf = RectF()
    val BLACK: Typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)

    val UMB = intArrayOf(
        0xFFE5322D.toInt(), 0xFFFFC21A.toInt(), 0xFF8B3FE0.toInt(),
        0xFF59C21F.toInt(), 0xFF2F84F0.toInt(), 0xFFF0388E.toInt()
    )

    private val icons = HashMap<Int, IconView>()
    private var appCtx: Context? = null

    fun init(c: Context) {
        appCtx = c.applicationContext
    }

    fun withA(color: Int, a: Int): Int = (a.coerceIn(0, 255) shl 24) or (color and 0x00FFFFFF)

    fun mix(a: Int, b: Int, f: Float): Int {
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * f).toInt()
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * f).toInt()
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * f).toInt()
        return Color.rgb(r, g, bl)
    }

    // draws one of the small icons of Icons.kt
    fun icon(c: Canvas, kind: Int, cx: Float, cy: Float, size: Float, color: Int) {
        val ctx = appCtx ?: return
        val iv = icons.getOrPut(kind) { IconView(ctx, kind, color) }
        iv.color = color
        val s = size.toInt().coerceAtLeast(2)
        iv.layout(0, 0, s, s)
        c.save()
        c.translate(cx - s / 2f, cy - s / 2f)
        iv.draw(c)
        c.restore()
    }

    // ---------- basic shapes ----------

    fun fillRR(c: Canvas, l: Float, t: Float, r: Float, b: Float, rad: Float, color: Int) {
        p.shader = null
        p.style = Paint.Style.FILL
        p.color = color
        rf.set(l, t, r, b)
        c.drawRoundRect(rf, rad, rad, p)
    }

    fun gradRR(c: Canvas, l: Float, t: Float, r: Float, b: Float, rad: Float, c1: Int, c2: Int) {
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, t, 0f, b, c1, c2, Shader.TileMode.CLAMP)
        rf.set(l, t, r, b)
        c.drawRoundRect(rf, rad, rad, p)
        p.shader = null
    }

    fun strokeRR(c: Canvas, l: Float, t: Float, r: Float, b: Float, rad: Float, color: Int, w: Float) {
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = w
        p.color = color
        rf.set(l, t, r, b)
        c.drawRoundRect(rf, rad, rad, p)
        p.style = Paint.Style.FILL
    }

    fun circle(c: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        p.shader = null
        p.style = Paint.Style.FILL
        p.color = color
        c.drawCircle(cx, cy, r, p)
    }

    fun gradCircle(c: Canvas, cx: Float, cy: Float, r: Float, c1: Int, c2: Int) {
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, cy - r, 0f, cy + r, c1, c2, Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r, p)
        p.shader = null
    }

    fun ring(c: Canvas, cx: Float, cy: Float, r: Float, color: Int, w: Float) {
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = w
        p.color = color
        c.drawCircle(cx, cy, r, p)
        p.style = Paint.Style.FILL
    }

    fun star(c: Canvas, cx: Float, cy: Float, ro: Float, color: Int) {
        path.reset()
        for (i in 0 until 10) {
            val rad = if (i % 2 == 0) ro else ro * 0.45f
            val a = Math.toRadians(-90.0 + i * 36.0)
            val x = cx + (rad * cos(a)).toFloat()
            val y = cy + (rad * sin(a)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        p.shader = null
        p.style = Paint.Style.FILL
        p.color = color
        c.drawPath(path, p)
    }

    // ---------- text ----------

    // text size that makes `s` exactly `targetW` wide
    fun fit(s: String, targetW: Float): Float {
        p.shader = null
        p.style = Paint.Style.FILL
        p.typeface = BLACK
        p.textSize = 100f
        return 100f * targetW / p.measureText(s)
    }

    fun text(
        c: Canvas, s: String, x: Float, y: Float, size: Float, fill: Int,
        align: Paint.Align = Paint.Align.CENTER, stroke: Int = 0, sw: Float = 0f
    ) {
        p.shader = null
        p.typeface = BLACK
        p.textSize = size
        p.textAlign = align
        if (sw > 0f) {
            p.style = Paint.Style.STROKE
            p.strokeWidth = sw
            p.strokeJoin = Paint.Join.ROUND
            p.color = stroke
            c.drawText(s, x, y, p)
        }
        p.style = Paint.Style.FILL
        p.color = fill
        c.drawText(s, x, y, p)
    }

    // ---------- buttons ----------

    fun gloss(c: Canvas, l: Float, t: Float, r: Float, b: Float, c1: Int, c2: Int, border: Int, shadow: Int, k: Float) {
        val rad = (b - t) / 2f
        fillRR(c, l, t + 9f * k, r, b + 9f * k, rad, shadow)
        fillRR(c, l - 6f * k, t - 6f * k, r + 6f * k, b + 6f * k, rad + 6f * k, border)
        gradRR(c, l, t, r, b, rad, c1, c2)
        fillRR(c, l + 16f * k, t + 7f * k, r - 16f * k, t + (b - t) * 0.46f, rad * 0.55f, 0x38FFFFFF.toInt())
    }

    fun card(c: Canvas, l: Float, t: Float, r: Float, b: Float, c1: Int, c2: Int, border: Int, k: Float) {
        val rad = 30f * k
        fillRR(c, l, t + 8f * k, r, b + 8f * k, rad, 0x55102050.toInt())
        fillRR(c, l - 5f * k, t - 5f * k, r + 5f * k, b + 5f * k, rad + 5f * k, border)
        gradRR(c, l, t, r, b, rad, c1, c2)
        fillRR(c, l + 10f * k, t + 8f * k, r - 10f * k, t + (b - t) * 0.4f, rad * 0.6f, 0x2EFFFFFF.toInt())
    }

    fun hudPill(c: Canvas, l: Float, t: Float, r: Float, b: Float, k: Float) {
        val rad = (b - t) / 2f
        fillRR(c, l, t + 5f * k, r, b + 5f * k, rad, 0x55081A44.toInt())
        gradRR(c, l, t, r, b, rad, 0xFF1D55A0.toInt(), 0xFF0D2B62.toInt())
        strokeRR(c, l, t, r, b, rad, 0xFF45A0F0.toInt(), 3f * k)
    }

    fun hudCircle(c: Canvas, cx: Float, cy: Float, rad: Float, k: Float) {
        circle(c, cx, cy + 5f * k, rad, 0x55081A44.toInt())
        gradCircle(c, cx, cy, rad, 0xFF1D55A0.toInt(), 0xFF0D2B62.toInt())
        ring(c, cx, cy, rad, 0xFF45A0F0.toInt(), 3f * k)
    }

    fun bigArrow(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        p.strokeWidth = s * 0.16f
        p.color = color
        c.drawLine(cx - s * 0.4f, cy, cx + s * 0.4f, cy, p)
        path.reset()
        path.moveTo(cx + s * 0.05f, cy - s * 0.35f)
        path.lineTo(cx + s * 0.42f, cy)
        path.lineTo(cx + s * 0.05f, cy + s * 0.35f)
        c.drawPath(path, p)
        p.style = Paint.Style.FILL
    }

    fun slash(c: Canvas, cx: Float, cy: Float, s: Float, k: Float) {
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = 6f * k
        p.color = 0xFFFF5A5A.toInt()
        c.drawLine(cx - s * 0.6f, cy - s * 0.6f, cx + s * 0.6f, cy + s * 0.6f, p)
        p.style = Paint.Style.FILL
    }

    // the big round left / right buttons of the game
    fun arrowBtn(c: Canvas, cx: Float, cy: Float, r: Float, left: Boolean, pressed: Boolean, k: Float) {
        circle(c, cx, cy + 7f * k, r + 6f * k, 0x66102050.toInt())
        circle(c, cx, cy, r + 6f * k, 0xFFFFFFFF.toInt())
        gradCircle(
            c, cx, cy, r,
            if (pressed) 0xFF2D7BE0.toInt() else 0xFF45A1FF.toInt(),
            if (pressed) 0xFF1145A8.toInt() else 0xFF1B5FD6.toInt()
        )
        rf.set(cx - r * 0.7f, cy - r * 0.92f, cx + r * 0.7f, cy - r * 0.25f)
        p.style = Paint.Style.FILL
        p.color = 0x30FFFFFF
        c.drawOval(rf, p)
        val a = r * 0.5f
        val d = if (left) -1f else 1f
        path.reset()
        path.moveTo(cx + d * a, cy)
        path.lineTo(cx, cy - a * 0.95f)
        path.lineTo(cx, cy - a * 0.42f)
        path.lineTo(cx - d * a, cy - a * 0.42f)
        path.lineTo(cx - d * a, cy + a * 0.42f)
        path.lineTo(cx, cy + a * 0.42f)
        path.lineTo(cx, cy + a * 0.95f)
        path.close()
        p.color = 0xFFFFFFFF.toInt()
        c.drawPath(path, p)
    }

    // ---------- small pictures for the home cards ----------

    fun appsIcon(c: Canvas, cx: Float, cy: Float, s: Float) {
        val q = s * 0.44f
        val g = s * 0.08f
        val x0 = cx - q - g / 2f
        val y0 = cy - q - g / 2f
        val cols = intArrayOf(0xFF6EDB4B.toInt(), 0xFF6EDB4B.toInt(), 0xFFFFC928.toInt(), 0xFFFFC928.toInt())
        for (i in 0 until 4) {
            val rr = i / 2
            val cc = i % 2
            val l = x0 + cc * (q + g)
            val t = y0 + rr * (q + g)
            fillRR(c, l, t, l + q, t + q, s * 0.08f, cols[i])
        }
    }

    fun shopIcon(c: Canvas, cx: Float, cy: Float, s: Float) {
        val l = cx - s * 0.45f
        val r = cx + s * 0.45f
        val top = cy - s * 0.5f
        val band = cy - s * 0.1f
        val sw = (r - l) / 5f
        for (i in 0 until 5) {
            fillRR(
                c, l + i * sw, top, l + (i + 1) * sw, band, s * 0.04f,
                if (i % 2 == 0) 0xFFE5322D.toInt() else 0xFFFFFFFF.toInt()
            )
        }
        fillRR(c, l + s * 0.05f, band, r - s * 0.05f, cy + s * 0.5f, s * 0.04f, 0xFFF6D9A8.toInt())
        fillRR(c, cx - s * 0.12f, cy + s * 0.12f, cx + s * 0.12f, cy + s * 0.5f, s * 0.03f, 0xFF3B8CFF.toInt())
        fillRR(c, l + s * 0.12f, cy + s * 0.02f, l + s * 0.32f, cy + s * 0.22f, s * 0.03f, 0xFF8ED0FF.toInt())
    }

    fun medalIcon(c: Canvas, cx: Float, cy: Float, s: Float) {
        p.shader = null
        p.style = Paint.Style.FILL
        p.color = 0xFFE5322D.toInt()
        path.reset()
        path.moveTo(cx - s * 0.24f, cy + s * 0.02f)
        path.lineTo(cx - s * 0.36f, cy + s * 0.5f)
        path.lineTo(cx - s * 0.14f, cy + s * 0.38f)
        path.lineTo(cx - s * 0.04f, cy + s * 0.5f)
        path.lineTo(cx + s * 0.04f, cy + s * 0.02f)
        path.close()
        c.drawPath(path, p)
        path.reset()
        path.moveTo(cx + s * 0.24f, cy + s * 0.02f)
        path.lineTo(cx + s * 0.36f, cy + s * 0.5f)
        path.lineTo(cx + s * 0.14f, cy + s * 0.38f)
        path.lineTo(cx + s * 0.04f, cy + s * 0.5f)
        path.lineTo(cx - s * 0.04f, cy + s * 0.02f)
        path.close()
        c.drawPath(path, p)
        circle(c, cx, cy - s * 0.1f, s * 0.36f, 0xFFE5322D.toInt())
        circle(c, cx, cy - s * 0.1f, s * 0.28f, 0xFFFFC21A.toInt())
        star(c, cx, cy - s * 0.08f, s * 0.2f, 0xFFFFF3B0.toInt())
    }

    // ---------- scenery ----------

    private fun cloudShape(c: Canvas, cx: Float, cy: Float, s: Float) {
        c.drawCircle(cx - s * 0.55f, cy + s * 0.05f, s * 0.38f, p)
        c.drawCircle(cx + s * 0.55f, cy + s * 0.05f, s * 0.38f, p)
        c.drawCircle(cx - s * 0.2f, cy - s * 0.2f, s * 0.5f, p)
        c.drawCircle(cx + s * 0.25f, cy - s * 0.12f, s * 0.42f, p)
        rf.set(cx - s * 0.95f, cy, cx + s * 0.95f, cy + s * 0.43f)
        c.drawRoundRect(rf, s * 0.2f, s * 0.2f, p)
    }

    fun cloud(c: Canvas, cx: Float, cy: Float, s: Float) {
        p.shader = null
        p.style = Paint.Style.FILL
        p.color = 0xFFBFE2F8.toInt()
        cloudShape(c, cx, cy + s * 0.07f, s)
        p.color = 0xFFFFFFFF.toInt()
        cloudShape(c, cx, cy, s)
    }

    private fun drift(base: Float, speed: Float, t: Float, w: Float, m: Float): Float {
        val span = w + 2f * m
        var x = (base + t * speed) % span
        if (x < 0f) x += span
        return x - m
    }

    fun pine(c: Canvas, cx: Float, baseY: Float, s: Float, dark: Int, light: Int) {
        p.shader = null
        p.style = Paint.Style.FILL
        p.color = 0xFF7A4A28.toInt()
        rf.set(cx - s * 0.05f, baseY - s * 0.22f, cx + s * 0.05f, baseY)
        c.drawRect(rf, p)
        for (i in 0..2) {
            val by = baseY - s * 0.14f - i * s * 0.27f
            val hw = s * (0.34f - i * 0.07f)
            path.reset()
            path.moveTo(cx - hw, by)
            path.lineTo(cx + hw, by)
            path.lineTo(cx, by - s * 0.42f)
            path.close()
            p.color = if (i % 2 == 0) dark else light
            c.drawPath(path, p)
        }
    }

    fun fence(c: Canvas, x0: Float, x1: Float, y: Float, hgt: Float, gap: Float) {
        p.shader = null
        p.style = Paint.Style.FILL
        p.color = 0xFFB9772F.toInt()
        c.drawRect(x0, y + hgt * 0.25f, x1, y + hgt * 0.37f, p)
        c.drawRect(x0, y + hgt * 0.62f, x1, y + hgt * 0.74f, p)
        p.color = 0xFFD39349.toInt()
        var x = x0
        while (x < x1) {
            rf.set(x, y, x + gap * 0.28f, y + hgt)
            c.drawRoundRect(rf, gap * 0.1f, gap * 0.1f, p)
            x += gap
        }
    }

    fun flower(c: Canvas, cx: Float, cy: Float, s: Float, col: Int) {
        p.shader = null
        p.style = Paint.Style.FILL
        p.color = col
        for (i in 0 until 5) {
            val a = Math.toRadians(i * 72.0 - 90.0)
            c.drawCircle(cx + (s * cos(a)).toFloat(), cy + (s * sin(a)).toFloat(), s * 0.62f, p)
        }
        p.color = 0xFFFFD93B.toInt()
        c.drawCircle(cx, cy, s * 0.5f, p)
    }

    private fun flowers(c: Canvas, w: Float, hh: Float, k: Float) {
        val cols = intArrayOf(0xFFFF6FA0.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFA23B.toInt(), 0xFFEA6FFF.toInt())
        val xs = floatArrayOf(0.05f, 0.12f, 0.2f, 0.82f, 0.9f, 0.96f, 0.3f, 0.7f, 0.45f, 0.58f)
        val ys = floatArrayOf(0.93f, 0.965f, 0.915f, 0.95f, 0.92f, 0.975f, 0.975f, 0.97f, 0.985f, 0.98f)
        for (i in xs.indices) flower(c, w * xs[i], hh * ys[i], 15f * k, cols[i % cols.size])
    }

    fun windmill(c: Canvas, cx: Float, baseY: Float, s: Float, t: Float) {
        p.shader = null
        p.style = Paint.Style.FILL
        path.reset()
        path.moveTo(cx - s * 0.17f, baseY)
        path.lineTo(cx + s * 0.17f, baseY)
        path.lineTo(cx + s * 0.11f, baseY - s * 0.55f)
        path.lineTo(cx - s * 0.11f, baseY - s * 0.55f)
        path.close()
        p.color = 0xFFF6E7CC.toInt()
        c.drawPath(path, p)
        path.reset()
        path.moveTo(cx - s * 0.15f, baseY - s * 0.55f)
        path.lineTo(cx + s * 0.15f, baseY - s * 0.55f)
        path.lineTo(cx, baseY - s * 0.72f)
        path.close()
        p.color = 0xFFC8452F.toInt()
        c.drawPath(path, p)
        c.save()
        c.translate(cx, baseY - s * 0.5f)
        c.rotate(t * 45f)
        for (i in 0 until 4) {
            p.color = 0xFFB67A3C.toInt()
            rf.set(-s * 0.03f, -s * 0.5f, s * 0.03f, 0f)
            c.drawRect(rf, p)
            p.color = 0xFFFFFFFF.toInt()
            rf.set(s * 0.03f, -s * 0.5f, s * 0.16f, -s * 0.15f)
            c.drawRect(rf, p)
            c.rotate(90f)
        }
        c.restore()
    }

    fun rainbow(c: Canvas, w: Float, hh: Float, k: Float) {
        val cols = intArrayOf(
            0xFFFF4D4D.toInt(), 0xFFFF9A3B.toInt(), 0xFFFFE23B.toInt(),
            0xFF4DD96B.toInt(), 0xFF4DA6FF.toInt(), 0xFFB26DFF.toInt()
        )
        val cx = w * 0.02f
        val cy = hh * 0.37f
        val band = 17f * k
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.BUTT
        p.strokeWidth = band + 1f
        for (i in cols.indices) {
            val r = w * 0.3f - i * band
            rf.set(cx - r, cy - r, cx + r, cy + r)
            p.color = cols[i]
            c.drawArc(rf, 270f, 90f, false, p)
        }
        p.style = Paint.Style.FILL
    }

    // sky, clouds, hills, trees and meadow of the home screen
    fun homeScene(c: Canvas, w: Float, h: Float, hh: Float, k: Float, t: Float) {
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, 0f, 0f, hh, 0xFF2D9BF4.toInt(), 0xFFA9E4FF.toInt(), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null

        val m = 200f * k
        cloud(c, drift(w * 0.12f, 9f * k, t, w, m), hh * 0.2f, 110f * k)
        cloud(c, drift(w * 0.66f, 7f * k, t, w, m), hh * 0.12f, 80f * k)
        cloud(c, drift(w * 0.92f, 11f * k, t, w, m), hh * 0.58f, 92f * k)
        cloud(c, drift(w * 0.2f, 8f * k, t, w, m), hh * 0.69f, 100f * k)

        path.reset()
        path.moveTo(0f, hh * 0.74f)
        path.quadTo(w * 0.25f, hh * 0.66f, w * 0.5f, hh * 0.73f)
        path.quadTo(w * 0.78f, hh * 0.8f, w, hh * 0.68f)
        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()
        p.color = 0xFF8AD66C.toInt()
        c.drawPath(path, p)

        val dark = 0xFF1F8A4C.toInt()
        val light = 0xFF2BA85A.toInt()
        pine(c, w * 0.04f, hh * 0.78f, 230f * k, dark, light)
        pine(c, w * 0.13f, hh * 0.8f, 190f * k, light, dark)
        pine(c, w * 0.96f, hh * 0.74f, 220f * k, dark, light)
        pine(c, w * 0.87f, hh * 0.78f, 180f * k, light, dark)
        windmill(c, w * 0.9f, hh * 0.74f, 150f * k, t)
        fence(c, 0f, w * 0.24f, hh * 0.78f, 70f * k, 46f * k)
        fence(c, w * 0.76f, w, hh * 0.78f, 70f * k, 46f * k)

        path.reset()
        path.moveTo(0f, hh * 0.82f)
        path.quadTo(w * 0.3f, hh * 0.79f, w * 0.55f, hh * 0.815f)
        path.quadTo(w * 0.8f, hh * 0.84f, w, hh * 0.8f)
        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()
        p.shader = LinearGradient(0f, hh * 0.78f, 0f, hh, 0xFF7FE45A.toInt(), 0xFF2E9E3A.toInt(), Shader.TileMode.CLAMP)
        c.drawPath(path, p)
        p.shader = null
        flowers(c, w, hh, k)
    }

    // sky and clouds of the game screen
    fun gameSky(c: Canvas, w: Float, h: Float, hh: Float, k: Float, t: Float) {
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, 0f, 0f, hh, 0xFF2D9BF4.toInt(), 0xFFB5E8FF.toInt(), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null
        val m = 200f * k
        cloud(c, drift(w * 0.15f, 8f * k, t, w, m), hh * 0.2f, 120f * k)
        cloud(c, drift(w * 0.85f, 6f * k, t, w, m), hh * 0.34f, 100f * k)
        cloud(c, drift(w * 0.05f, 9f * k, t, w, m), hh * 0.5f, 90f * k)
        cloud(c, drift(w * 0.95f, 7f * k, t, w, m), hh * 0.58f, 80f * k)
    }

    // mountains, trees, fence and grass of the game screen
    fun gameGround(c: Canvas, w: Float, h: Float, hh: Float, k: Float) {
        p.shader = null
        p.style = Paint.Style.FILL
        path.reset()
        path.moveTo(0f, hh * 0.8f)
        path.lineTo(0f, hh * 0.72f)
        path.lineTo(w * 0.16f, hh * 0.655f)
        path.lineTo(w * 0.34f, hh * 0.72f)
        path.lineTo(w * 0.55f, hh * 0.645f)
        path.lineTo(w * 0.78f, hh * 0.72f)
        path.lineTo(w * 0.92f, hh * 0.67f)
        path.lineTo(w, hh * 0.72f)
        path.lineTo(w, hh * 0.8f)
        path.close()
        p.color = 0xFF8EC6F0.toInt()
        c.drawPath(path, p)

        path.reset()
        path.moveTo(0f, hh * 0.76f)
        path.quadTo(w * 0.5f, hh * 0.7f, w, hh * 0.76f)
        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()
        p.color = 0xFF63C94B.toInt()
        c.drawPath(path, p)

        val dark = 0xFF1F8A4C.toInt()
        val light = 0xFF2BA85A.toInt()
        pine(c, w * 0.05f, hh * 0.795f, 250f * k, dark, light)
        pine(c, w * 0.16f, hh * 0.785f, 200f * k, light, dark)
        pine(c, w * 0.95f, hh * 0.795f, 250f * k, dark, light)
        pine(c, w * 0.84f, hh * 0.785f, 200f * k, light, dark)
        fence(c, 0f, w, hh * 0.765f, 60f * k, 70f * k)

        path.reset()
        path.moveTo(0f, hh * 0.815f)
        path.quadTo(w * 0.5f, hh * 0.77f, w, hh * 0.815f)
        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()
        p.shader = LinearGradient(0f, hh * 0.78f, 0f, hh, 0xFF8EEA5E.toInt(), 0xFF3DB03C.toInt(), Shader.TileMode.CLAMP)
        c.drawPath(path, p)
        p.shader = null

        path.reset()
        path.moveTo(0f, hh * 0.95f)
        path.quadTo(w * 0.5f, hh * 0.925f, w, hh * 0.95f)
        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()
        p.color = 0xFF9B6A3A.toInt()
        c.drawPath(path, p)
        flowers(c, w, hh * 0.92f, k)
    }

    // ---------- umbrella, bucket, mascot ----------

    fun umbrella(c: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        val light = mix(color, 0xFFFFFFFF.toInt(), 0.35f)
        val dark = mix(color, 0xFF000000.toInt(), 0.3f)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = r * 0.09f
        p.color = 0xFF6B4423.toInt()
        c.drawLine(cx, cy, cx, cy + r * 0.72f, p)
        rf.set(cx - r * 0.3f, cy + r * 0.52f, cx, cy + r * 0.92f)
        c.drawArc(rf, 0f, 180f, false, p)

        path.reset()
        path.moveTo(cx - r, cy)
        rf.set(cx - r, cy - r * 0.95f, cx + r, cy + r * 0.95f)
        path.arcTo(rf, 180f, 180f, false)
        for (i in 1..4) {
            val x1 = cx + r - i * r * 0.5f
            path.quadTo(x1 + r * 0.25f, cy - r * 0.3f, x1, cy)
        }
        path.close()
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, cy - r, 0f, cy, light, color, Shader.TileMode.CLAMP)
        c.drawPath(path, p)
        p.shader = null

        p.style = Paint.Style.STROKE
        p.strokeWidth = r * 0.035f
        p.color = withA(dark, 150)
        for (i in -2..2) c.drawLine(cx, cy - r * 0.93f, cx + i * r * 0.5f, cy, p)
        p.style = Paint.Style.FILL
        p.color = dark
        c.drawCircle(cx, cy - r * 0.95f, r * 0.06f, p)
        p.color = 0x44FFFFFF
        rf.set(cx - r * 0.72f, cy - r * 0.78f, cx - r * 0.25f, cy - r * 0.38f)
        c.drawOval(rf, p)
    }

    fun bucketBack(c: Canvas, bx: Float, rimY: Float, bw: Float) {
        rf.set(bx - bw * 0.5f, rimY - bw * 0.11f, bx + bw * 0.5f, rimY + bw * 0.11f)
        p.shader = null
        p.style = Paint.Style.FILL
        p.color = 0xFF7A1018.toInt()
        c.drawOval(rf, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = bw * 0.06f
        p.color = 0xFFFFC62E.toInt()
        c.drawArc(rf, 180f, 180f, false, p)
        p.style = Paint.Style.FILL
    }

    fun bucketFront(c: Canvas, bx: Float, rimY: Float, bw: Float, bh: Float) {
        path.reset()
        path.moveTo(bx - bw * 0.5f, rimY)
        path.lineTo(bx + bw * 0.5f, rimY)
        path.lineTo(bx + bw * 0.39f, rimY + bh)
        path.quadTo(bx, rimY + bh + bw * 0.1f, bx - bw * 0.39f, rimY + bh)
        path.close()
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, rimY, 0f, rimY + bh, 0xFFF4524B.toInt(), 0xFFC01820.toInt(), Shader.TileMode.CLAMP)
        c.drawPath(path, p)
        p.shader = null
        p.color = 0x22FFFFFF
        rf.set(bx - bw * 0.42f, rimY + bh * 0.1f, bx - bw * 0.28f, rimY + bh * 0.9f)
        c.drawRoundRect(rf, bw * 0.05f, bw * 0.05f, p)

        rf.set(bx - bw * 0.5f, rimY - bw * 0.11f, bx + bw * 0.5f, rimY + bw * 0.11f)
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = bw * 0.07f
        p.color = 0xFFFFC62E.toInt()
        c.drawArc(rf, 0f, 180f, false, p)
        p.style = Paint.Style.FILL
        star(c, bx, rimY + bh * 0.5f, bw * 0.2f, 0xFFFFFFFF.toInt())
    }

    private fun hand(c: Canvas, x: Float, y: Float, s: Float, thumbUp: Boolean) {
        val white = 0xFFFFFFFF.toInt()
        val edge = 0xFFC9CEE6.toInt()
        fillRR(c, x - s * 0.1f, y - s * 0.07f, x + s * 0.1f, y + s * 0.09f, s * 0.05f, white)
        strokeRR(c, x - s * 0.1f, y - s * 0.07f, x + s * 0.1f, y + s * 0.09f, s * 0.05f, edge, s * 0.012f)
        if (thumbUp) {
            fillRR(c, x - s * 0.09f, y - s * 0.2f, x - s * 0.01f, y - s * 0.05f, s * 0.04f, white)
            strokeRR(c, x - s * 0.09f, y - s * 0.2f, x - s * 0.01f, y - s * 0.05f, s * 0.04f, edge, s * 0.012f)
        } else {
            fillRR(c, x - s * 0.02f, y - s * 0.16f, x + s * 0.06f, y - s * 0.05f, s * 0.04f, white)
            strokeRR(c, x - s * 0.02f, y - s * 0.16f, x + s * 0.06f, y - s * 0.05f, s * 0.04f, edge, s * 0.012f)
        }
    }

    // the happy umbrella character
    fun mascot(c: Canvas, cx: Float, cy: Float, s: Float) {
        c.save()
        c.rotate(-7f, cx, cy)
        val red = 0xFFE5322D.toInt()
        val redD = 0xFFB8161E.toInt()
        val white = 0xFFFFFFFF.toInt()
        val by = cy + s * 0.06f
        val l = cx - s * 0.5f
        val r = cx + s * 0.5f

        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = s * 0.05f
        p.color = 0xFF7A4A24.toInt()
        c.drawLine(cx + s * 0.02f, by, cx + s * 0.02f, cy + s * 0.5f, p)
        p.color = redD
        path.reset()
        path.moveTo(cx - s * 0.38f, by - s * 0.04f)
        path.quadTo(cx - s * 0.56f, by + s * 0.04f, cx - s * 0.6f, by + s * 0.14f)
        c.drawPath(path, p)
        path.reset()
        path.moveTo(cx + s * 0.38f, by - s * 0.04f)
        path.quadTo(cx + s * 0.56f, by + s * 0.04f, cx + s * 0.6f, by + s * 0.16f)
        c.drawPath(path, p)

        path.reset()
        path.moveTo(l, by)
        rf.set(l, by - s * 0.56f, r, by + s * 0.56f)
        path.arcTo(rf, 180f, 180f, false)
        for (i in 1..3) {
            val x1 = r - i * (r - l) / 3f
            path.quadTo(x1 + (r - l) / 6f, by - s * 0.1f, x1, by)
        }
        path.close()
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, by - s * 0.56f, 0f, by, 0xFFFF6B5E.toInt(), red, Shader.TileMode.CLAMP)
        c.drawPath(path, p)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = s * 0.02f
        p.color = withA(redD, 120)
        for (i in -2..2) c.drawLine(cx, by - s * 0.55f, cx + i * s * 0.25f, by, p)
        p.style = Paint.Style.FILL

        val ey = by - s * 0.22f
        for (side in intArrayOf(-1, 1)) {
            val ex = cx + side * s * 0.15f
            rf.set(ex - s * 0.1f, ey - s * 0.13f, ex + s * 0.1f, ey + s * 0.13f)
            p.color = white
            c.drawOval(rf, p)
            p.color = 0xFF1E2A5A.toInt()
            c.drawCircle(ex + s * 0.015f, ey + s * 0.02f, s * 0.065f, p)
            p.color = white
            c.drawCircle(ex + s * 0.035f, ey - s * 0.02f, s * 0.022f, p)
        }
        p.color = 0x66FF5A8A
        c.drawCircle(cx - s * 0.3f, by - s * 0.1f, s * 0.05f, p)
        c.drawCircle(cx + s * 0.3f, by - s * 0.1f, s * 0.05f, p)
        rf.set(cx - s * 0.1f, by - s * 0.17f, cx + s * 0.1f, by - s * 0.02f)
        p.color = 0xFF7A1018.toInt()
        c.drawArc(rf, 0f, 180f, true, p)
        rf.set(cx - s * 0.05f, by - s * 0.07f, cx + s * 0.05f, by - s * 0.02f)
        p.color = 0xFFFF7A8A.toInt()
        c.drawOval(rf, p)

        hand(c, cx - s * 0.62f, by + s * 0.17f, s, true)
        hand(c, cx + s * 0.62f, by + s * 0.2f, s * 0.8f, false)
        c.restore()
    }
}

package com.akay.umbrellacatch

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.view.View

class RainbowView(ctx: Context) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rf = RectF()

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cols = intArrayOf(
            0xFFFF4D4D.toInt(), 0xFFFFA23B.toInt(), 0xFFFFE23B.toInt(),
            0xFF4DDC6B.toInt(), 0xFF4DA6FF.toInt(), 0xFFB66DFF.toInt()
        )
        p.style = Paint.Style.STROKE
        val sw = w / (cols.size * 1.6f)
        for (i in cols.indices) {
            p.strokeWidth = sw
            p.color = cols[i]
            val inset = sw * i
            rf.set(inset, h * 0.2f + inset, w * 2f - inset, h * 2.2f - inset)
            c.drawArc(rf, 180f, 180f, false, p)
        }
    }
}

// a friendly umbrella character with a face and two thumbs up
class MascotView(ctx: Context) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rf = RectF()

    private fun hand(c: Canvas, cx: Float, cy: Float, s: Float) {
        p.color = Color.WHITE
        rf.set(cx - s * 0.18f, cy - s * 0.5f, cx + s * 0.18f, cy + s * 0.2f)
        c.drawRoundRect(rf, s * 0.18f, s * 0.18f, p)
        c.drawCircle(cx, cy + s * 0.3f, s * 0.26f, p)
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val canopyW = w * 0.72f
        val canopyTop = h * 0.08f
        val canopyBot = h * 0.46f

        path.reset()
        path.moveTo(cx - canopyW / 2f, canopyBot)
        path.quadTo(cx, canopyTop, cx + canopyW / 2f, canopyBot)
        path.quadTo(cx + canopyW * 0.3f, canopyBot - h * 0.06f, cx + canopyW * 0.15f, canopyBot)
        path.quadTo(cx, canopyBot - h * 0.06f, cx - canopyW * 0.15f, canopyBot)
        path.quadTo(cx - canopyW * 0.3f, canopyBot - h * 0.06f, cx - canopyW / 2f, canopyBot)
        path.close()
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(cx, canopyTop, cx, canopyBot, 0xFFFF5A5A.toInt(), 0xFFE22F3F.toInt(), Shader.TileMode.CLAMP)
        c.drawPath(path, p)
        p.shader = null
        p.color = 0x33000000
        p.style = Paint.Style.STROKE
        p.strokeWidth = h * 0.012f
        for (i in -2..2) {
            c.drawLine(cx, canopyTop + h * 0.02f, cx + i * canopyW * 0.18f, canopyBot, p)
        }

        p.style = Paint.Style.FILL
        p.color = 0xFFFF5A5A.toInt()
        rf.set(cx - w * 0.22f, canopyBot - h * 0.02f, cx + w * 0.22f, h * 0.78f)
        c.drawRoundRect(rf, w * 0.08f, w * 0.08f, p)

        p.color = Color.WHITE
        c.drawCircle(cx - w * 0.1f, h * 0.56f, w * 0.07f, p)
        c.drawCircle(cx + w * 0.1f, h * 0.56f, w * 0.07f, p)
        p.color = 0xFF263056.toInt()
        c.drawCircle(cx - w * 0.08f, h * 0.55f, w * 0.035f, p)
        c.drawCircle(cx + w * 0.12f, h * 0.55f, w * 0.035f, p)

        p.style = Paint.Style.STROKE
        p.strokeWidth = h * 0.015f
        p.color = 0xFF263056.toInt()
        rf.set(cx - w * 0.1f, h * 0.6f, cx + w * 0.1f, h * 0.74f)
        c.drawArc(rf, 10f, 160f, false, p)

        p.strokeWidth = h * 0.02f
        c.drawLine(cx, h * 0.78f, cx, h * 0.96f, p)

        p.style = Paint.Style.FILL
        hand(c, cx - w * 0.36f, h * 0.62f, w * 0.18f)
        hand(c, cx + w * 0.36f, h * 0.62f, w * 0.18f)
    }
}

package com.akay.umbrellacatch

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader

// extra pictures for the level mode: stone, heart, clock and a cross
object D2 {
    private val path = Path()

    fun stone(c: Canvas, cx: Float, cy: Float, r: Float) {
        val p = D.p
        val pts = floatArrayOf(
            -0.95f, 0.15f, -0.62f, -0.62f, 0.0f, -0.92f, 0.7f, -0.58f,
            1.0f, 0.1f, 0.66f, 0.76f, -0.1f, 0.92f, -0.7f, 0.62f
        )
        path.reset()
        var i = 0
        while (i < pts.size) {
            val x = cx + pts[i] * r
            val y = cy + pts[i + 1] * r
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            i += 2
        }
        path.close()
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, cy - r, 0f, cy + r, 0xFFC3C9D6.toInt(), 0xFF6A7283.toInt(), Shader.TileMode.CLAMP)
        c.drawPath(path, p)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeJoin = Paint.Join.ROUND
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = r * 0.09f
        p.color = 0xFF454C5E.toInt()
        c.drawPath(path, p)
        p.strokeWidth = r * 0.06f
        p.color = 0x66454C5E
        c.drawLine(cx - r * 0.1f, cy - r * 0.5f, cx + r * 0.1f, cy - r * 0.05f, p)
        c.drawLine(cx + r * 0.1f, cy - r * 0.05f, cx - r * 0.15f, cy + r * 0.35f, p)
        c.drawLine(cx + r * 0.1f, cy - r * 0.05f, cx + r * 0.5f, cy + r * 0.1f, p)
        p.style = Paint.Style.FILL
        p.color = 0x55FFFFFF
        path.reset()
        path.moveTo(cx - r * 0.7f, cy - r * 0.1f)
        path.lineTo(cx - r * 0.45f, cy - r * 0.5f)
        path.lineTo(cx - r * 0.05f, cy - r * 0.62f)
        path.lineTo(cx - r * 0.3f, cy - r * 0.2f)
        path.close()
        c.drawPath(path, p)
    }

    fun heart(c: Canvas, cx: Float, cy: Float, s: Float, fill: Int, edge: Int) {
        val p = D.p
        path.reset()
        path.moveTo(cx, cy + s * 0.9f)
        path.cubicTo(cx - s * 1.5f, cy - s * 0.05f, cx - s * 0.75f, cy - s * 1.15f, cx, cy - s * 0.4f)
        path.cubicTo(cx + s * 0.75f, cy - s * 1.15f, cx + s * 1.5f, cy - s * 0.05f, cx, cy + s * 0.9f)
        path.close()
        p.shader = null
        p.style = Paint.Style.FILL
        p.color = fill
        c.drawPath(path, p)
        p.style = Paint.Style.STROKE
        p.strokeJoin = Paint.Join.ROUND
        p.strokeWidth = s * 0.14f
        p.color = edge
        c.drawPath(path, p)
        p.style = Paint.Style.FILL
        p.color = 0x66FFFFFF
        D.rf.set(cx - s * 0.62f, cy - s * 0.62f, cx - s * 0.2f, cy - s * 0.3f)
        c.drawOval(D.rf, p)
    }

    fun clock(c: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        val p = D.p
        val my = cy + r * 0.08f
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = r * 0.2f
        p.color = color
        c.drawCircle(cx, my, r * 0.8f, p)
        c.drawLine(cx, my, cx, my - r * 0.42f, p)
        c.drawLine(cx, my, cx + r * 0.36f, my + r * 0.2f, p)
        c.drawLine(cx - r * 0.25f, cy - r * 0.9f, cx + r * 0.25f, cy - r * 0.9f, p)
        p.style = Paint.Style.FILL
    }

    fun cross(c: Canvas, cx: Float, cy: Float, s: Float, color: Int, w: Float) {
        val p = D.p
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = w
        p.color = color
        c.drawLine(cx - s, cy - s, cx + s, cy + s, p)
        c.drawLine(cx + s, cy - s, cx - s, cy + s, p)
        p.style = Paint.Style.FILL
    }
}

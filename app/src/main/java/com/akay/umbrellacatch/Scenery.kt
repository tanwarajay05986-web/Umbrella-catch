package com.akay.umbrellacatch

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.view.View

// sky + clouds, used behind every screen
class SkyView(ctx: Context) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rf = RectF()

    private fun cloud(c: Canvas, cx: Float, cy: Float, s: Float) {
        p.color = 0xF2FFFFFF.toInt()
        c.drawCircle(cx - s * 0.5f, cy, s * 0.42f, p)
        c.drawCircle(cx + s * 0.5f, cy, s * 0.42f, p)
        c.drawCircle(cx, cy - s * 0.22f, s * 0.5f, p)
        rf.set(cx - s * 0.9f, cy - s * 0.1f, cx + s * 0.9f, cy + s * 0.4f)
        c.drawRoundRect(rf, s * 0.4f, s * 0.4f, p)
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        p.shader = LinearGradient(0f, 0f, 0f, h, 0xFF4FB4F5.toInt(), 0xFF9FE0FF.toInt(), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null
        cloud(c, w * 0.18f, h * 0.14f, w * 0.17f)
        cloud(c, w * 0.82f, h * 0.22f, w * 0.15f)
        cloud(c, w * 0.08f, h * 0.42f, w * 0.12f)
        cloud(c, w * 0.9f, h * 0.5f, w * 0.1f)
    }
}

// grass, fence and trees, used at the bottom of every screen
class GroundView(ctx: Context) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rf = RectF()

    private fun tree(c: Canvas, cx: Float, baseY: Float, s: Float) {
        p.color = 0xFF7A4A28.toInt()
        rf.set(cx - s * 0.08f, baseY - s * 0.5f, cx + s * 0.08f, baseY)
        c.drawRect(rf, p)
        p.color = 0xFF2E9E4F.toInt()
        c.drawCircle(cx, baseY - s * 0.62f, s * 0.38f, p)
        p.color = 0xFF3BB662.toInt()
        c.drawCircle(cx - s * 0.2f, baseY - s * 0.5f, s * 0.26f, p)
        c.drawCircle(cx + s * 0.22f, baseY - s * 0.5f, s * 0.26f, p)
    }

    private fun flower(c: Canvas, cx: Float, cy: Float, s: Float, col: Int) {
        p.color = col
        for (i in 0 until 5) {
            val a = Math.toRadians(i * 72.0)
            c.drawCircle(cx + (s * Math.cos(a)).toFloat(), cy + (s * Math.sin(a)).toFloat(), s * 0.6f, p)
        }
        p.color = 0xFFFFD93B.toInt()
        c.drawCircle(cx, cy, s * 0.5f, p)
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        p.shader = null
        p.color = 0xFF57C853.toInt()
        path.reset()
        path.moveTo(0f, h * 0.3f)
        path.quadTo(w * 0.25f, h * 0.1f, w * 0.5f, h * 0.24f)
        path.quadTo(w * 0.75f, h * 0.38f, w, h * 0.18f)
        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()
        c.drawPath(path, p)

        tree(c, w * 0.08f, h * 0.9f, w * 0.14f)
        tree(c, w * 0.94f, h * 0.86f, w * 0.12f)

        p.color = 0xFFC79256.toInt()
        for (i in 0..7) {
            val fx = w * (0.05f + i * 0.13f)
            rf.set(fx, h * 0.5f, fx + w * 0.02f, h * 0.78f)
            c.drawRect(rf, p)
        }
        rf.set(0f, h * 0.56f, w, h * 0.6f)
        c.drawRect(rf, p)

        val cols = intArrayOf(0xFFFF6FA0.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFA23B.toInt(), 0xFFEA6FFF.toInt())
        for (i in 0 until 9) {
            flower(c, w * (0.04f + i * 0.11f), h * (0.86f + (i % 3) * 0.04f), w * 0.016f, cols[i % cols.size])
        }
    }
}

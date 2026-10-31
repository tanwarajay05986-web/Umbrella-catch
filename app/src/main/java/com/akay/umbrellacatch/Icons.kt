package com.akay.umbrellacatch

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View

object K {
    const val TROPHY = 0
    const val TARGET = 1
    const val CHECK = 2
    const val SPEAKER_ON = 3
    const val SPEAKER_OFF = 4
    const val MUSIC = 5
    const val GEAR = 6
    const val CROWN = 7
    const val PAUSE = 8
    const val PLAY = 9
    const val HOME = 10
    const val FWD = 11
    const val STAR = 12
    const val PLUS = 13
    const val ARROW_L = 14
    const val ARROW_R = 15
    const val GRID = 16
    const val SHOP = 17
    const val MEDAL = 18
    const val SHARE = 19
    const val INFO = 20
    const val LOCK = 21
    const val UMBRELLA = 22
}

// every icon of the app is drawn here (no image files needed)
class IconView(ctx: Context, private val kind: Int, var color: Int) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rf = RectF()

    fun tint(c: Int) {
        color = c
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        val s = minOf(width, height).toFloat()
        val ox = (width - s) / 2f
        val oy = (height - s) / 2f
        fun x(v: Float) = ox + v * s
        fun y(v: Float) = oy + v * s
        p.shader = null
        p.color = color
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        fun stroke(w: Float) {
            p.style = Paint.Style.STROKE
            p.strokeWidth = w * s
        }
        fun fill() {
            p.style = Paint.Style.FILL
        }
        fill()

        when (kind) {
            K.TROPHY -> {
                path.reset()
                path.moveTo(x(0.3f), y(0.16f))
                path.lineTo(x(0.7f), y(0.16f))
                path.lineTo(x(0.66f), y(0.52f))
                path.lineTo(x(0.34f), y(0.52f))
                path.close()
                c.drawPath(path, p)
                stroke(0.06f)
                rf.set(x(0.08f), y(0.18f), x(0.3f), y(0.4f))
                c.drawArc(rf, 90f, 180f, false, p)
                rf.set(x(0.7f), y(0.18f), x(0.92f), y(0.4f))
                c.drawArc(rf, -90f, 180f, false, p)
                fill()
                rf.set(x(0.42f), y(0.52f), x(0.58f), y(0.68f))
                c.drawRect(rf, p)
                rf.set(x(0.3f), y(0.8f), x(0.7f), y(0.9f))
                c.drawRoundRect(rf, 0.04f * s, 0.04f * s, p)
                rf.set(x(0.36f), y(0.66f), x(0.64f), y(0.82f))
                c.drawRoundRect(rf, 0.05f * s, 0.05f * s, p)
            }
            K.TARGET -> {
                stroke(0.08f)
                c.drawCircle(x(0.5f), y(0.5f), 0.42f * s, p)
                c.drawCircle(x(0.5f), y(0.5f), 0.24f * s, p)
                fill()
                c.drawCircle(x(0.5f), y(0.5f), 0.08f * s, p)
            }
            K.CHECK -> {
                stroke(0.14f)
                path.reset()
                path.moveTo(x(0.2f), y(0.52f))
                path.lineTo(x(0.42f), y(0.74f))
                path.lineTo(x(0.8f), y(0.28f))
                c.drawPath(path, p)
            }
            K.SPEAKER_ON, K.SPEAKER_OFF -> {
                path.reset()
                path.moveTo(x(0.14f), y(0.38f))
                path.lineTo(x(0.34f), y(0.38f))
                path.lineTo(x(0.56f), y(0.18f))
                path.lineTo(x(0.56f), y(0.82f))
                path.lineTo(x(0.34f), y(0.62f))
                path.lineTo(x(0.14f), y(0.62f))
                path.close()
                c.drawPath(path, p)
                stroke(0.06f)
                if (kind == K.SPEAKER_ON) {
                    rf.set(x(0.62f), y(0.28f), x(0.9f), y(0.72f))
                    c.drawArc(rf, -50f, 100f, false, p)
                    rf.set(x(0.66f), y(0.38f), x(0.82f), y(0.62f))
                    c.drawArc(rf, -50f, 100f, false, p)
                } else {
                    c.drawLine(x(0.64f), y(0.3f), x(0.88f), y(0.7f), p)
                    c.drawLine(x(0.88f), y(0.3f), x(0.64f), y(0.7f), p)
                }
            }
            K.MUSIC -> {
                fill()
                c.drawCircle(x(0.28f), y(0.76f), 0.14f * s, p)
                c.drawCircle(x(0.64f), y(0.68f), 0.14f * s, p)
                stroke(0.07f)
                c.drawLine(x(0.4f), y(0.76f), x(0.4f), y(0.2f), p)
                c.drawLine(x(0.76f), y(0.68f), x(0.76f), y(0.14f), p)
                c.drawLine(x(0.4f), y(0.22f), x(0.76f), y(0.16f), p)
                c.drawLine(x(0.4f), y(0.36f), x(0.76f), y(0.3f), p)
            }
            K.GEAR -> {
                c.save()
                c.translate(x(0.5f), y(0.5f))
                for (i in 0 until 8) {
                    rf.set(-0.08f * s, -0.46f * s, 0.08f * s, -0.28f * s)
                    c.drawRoundRect(rf, 0.03f * s, 0.03f * s, p)
                    c.rotate(45f)
                }
                c.restore()
                path.reset()
                path.fillType = Path.FillType.EVEN_ODD
                path.addCircle(x(0.5f), y(0.5f), 0.33f * s, Path.Direction.CW)
                path.addCircle(x(0.5f), y(0.5f), 0.13f * s, Path.Direction.CW)
                c.drawPath(path, p)
                path.fillType = Path.FillType.WINDING
            }
            K.CROWN -> {
                path.reset()
                path.moveTo(x(0.12f), y(0.78f))
                path.lineTo(x(0.06f), y(0.3f))
                path.lineTo(x(0.3f), y(0.52f))
                path.lineTo(x(0.5f), y(0.18f))
                path.lineTo(x(0.7f), y(0.52f))
                path.lineTo(x(0.94f), y(0.3f))
                path.lineTo(x(0.88f), y(0.78f))
                path.close()
                c.drawPath(path, p)
                rf.set(x(0.12f), y(0.8f), x(0.88f), y(0.88f))
                c.drawRoundRect(rf, 0.02f * s, 0.02f * s, p)
            }
            K.PAUSE -> {
                rf.set(x(0.26f), y(0.2f), x(0.42f), y(0.8f))
                c.drawRoundRect(rf, 0.03f * s, 0.03f * s, p)
                rf.set(x(0.58f), y(0.2f), x(0.74f), y(0.8f))
                c.drawRoundRect(rf, 0.03f * s, 0.03f * s, p)
            }
            K.PLAY -> {
                path.reset()
                path.moveTo(x(0.28f), y(0.18f))
                path.lineTo(x(0.82f), y(0.5f))
                path.lineTo(x(0.28f), y(0.82f))
                path.close()
                c.drawPath(path, p)
            }
            K.HOME -> {
                path.reset()
                path.moveTo(x(0.5f), y(0.12f))
                path.lineTo(x(0.92f), y(0.5f))
                path.lineTo(x(0.8f), y(0.5f))
                path.lineTo(x(0.8f), y(0.88f))
                path.lineTo(x(0.2f), y(0.88f))
                path.lineTo(x(0.2f), y(0.5f))
                path.lineTo(x(0.08f), y(0.5f))
                path.close()
                c.drawPath(path, p)
            }
            K.FWD -> {
                path.reset()
                path.moveTo(x(0.14f), y(0.2f))
                path.lineTo(x(0.5f), y(0.5f))
                path.lineTo(x(0.14f), y(0.8f))
                path.close()
                c.drawPath(path, p)
                path.reset()
                path.moveTo(x(0.5f), y(0.2f))
                path.lineTo(x(0.86f), y(0.5f))
                path.lineTo(x(0.5f), y(0.8f))
                path.close()
                c.drawPath(path, p)
            }
            K.STAR -> {
                path.reset()
                for (i in 0 until 10) {
                    val rad = if (i % 2 == 0) 0.44f else 0.2f
                    val a = Math.toRadians(-90.0 + i * 36.0)
                    val px = x(0.5f) + (rad * s * Math.cos(a)).toFloat()
                    val py = y(0.54f) + (rad * s * Math.sin(a)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                c.drawPath(path, p)
            }
            K.PLUS -> {
                stroke(0.14f)
                c.drawLine(x(0.5f), y(0.18f), x(0.5f), y(0.82f), p)
                c.drawLine(x(0.18f), y(0.5f), x(0.82f), y(0.5f), p)
            }
            K.ARROW_L -> {
                path.reset()
                path.moveTo(x(0.6f), y(0.2f))
                path.lineTo(x(0.3f), y(0.5f))
                path.lineTo(x(0.6f), y(0.8f))
                path.close()
                c.drawPath(path, p)
            }
            K.ARROW_R -> {
                path.reset()
                path.moveTo(x(0.4f), y(0.2f))
                path.lineTo(x(0.7f), y(0.5f))
                path.lineTo(x(0.4f), y(0.8f))
                path.close()
                c.drawPath(path, p)
            }
            K.GRID -> {
                val q = 0.34f
                val g = 0.12f
                for (r in 0..1) for (col in 0..1) {
                    val l = 0.08f + col * (q + g)
                    val t = 0.08f + r * (q + g)
                    rf.set(x(l), y(t), x(l + q), y(t + q))
                    c.drawRoundRect(rf, 0.04f * s, 0.04f * s, p)
                }
            }
            K.SHOP -> {
                rf.set(x(0.12f), y(0.42f), x(0.88f), y(0.86f))
                c.drawRoundRect(rf, 0.04f * s, 0.04f * s, p)
                path.reset()
                path.moveTo(x(0.08f), y(0.42f))
                path.lineTo(x(0.2f), y(0.14f))
                path.lineTo(x(0.8f), y(0.14f))
                path.lineTo(x(0.92f), y(0.42f))
                path.close()
                c.drawPath(path, p)
                p.color = Color.WHITE
                rf.set(x(0.4f), y(0.58f), x(0.6f), y(0.86f))
                c.drawRect(rf, p)
            }
            K.MEDAL -> {
                stroke(0.07f)
                c.drawCircle(x(0.5f), y(0.42f), 0.26f * s, p)
                fill()
                path.reset()
                for (i in 0 until 10) {
                    val rad = if (i % 2 == 0) 0.16f else 0.07f
                    val a = Math.toRadians(-90.0 + i * 36.0)
                    val px = x(0.5f) + (rad * s * Math.cos(a)).toFloat()
                    val py = y(0.42f) + (rad * s * Math.sin(a)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                p.color = Color.WHITE
                c.drawPath(path, p)
                p.color = color
                path.reset()
                path.moveTo(x(0.34f), y(0.6f))
                path.lineTo(x(0.2f), y(0.92f))
                path.lineTo(x(0.42f), y(0.82f))
                path.lineTo(x(0.5f), y(0.94f))
                path.lineTo(x(0.58f), y(0.6f))
                path.close()
                c.drawPath(path, p)
            }
            K.SHARE -> {
                fill()
                c.drawCircle(x(0.22f), y(0.5f), 0.12f * s, p)
                c.drawCircle(x(0.78f), y(0.22f), 0.12f * s, p)
                c.drawCircle(x(0.78f), y(0.78f), 0.12f * s, p)
                stroke(0.06f)
                c.drawLine(x(0.3f), y(0.44f), x(0.7f), y(0.26f), p)
                c.drawLine(x(0.3f), y(0.56f), x(0.7f), y(0.74f), p)
            }
            K.INFO -> {
                stroke(0.07f)
                c.drawCircle(x(0.5f), y(0.5f), 0.4f * s, p)
                c.drawLine(x(0.5f), y(0.46f), x(0.5f), y(0.7f), p)
                fill()
                c.drawCircle(x(0.5f), y(0.3f), 0.05f * s, p)
            }
            K.LOCK -> {
                stroke(0.08f)
                rf.set(x(0.2f), y(0.46f), x(0.8f), y(0.9f))
                c.drawRoundRect(rf, 0.08f * s, 0.08f * s, p)
                rf.set(x(0.3f), y(0.14f), x(0.7f), y(0.58f))
                c.drawArc(rf, 180f, 180f, false, p)
                fill()
                c.drawCircle(x(0.5f), y(0.64f), 0.05f * s, p)
            }
            K.UMBRELLA -> {
                path.reset()
                path.moveTo(x(0.08f), y(0.5f))
                path.quadTo(x(0.5f), y(0.06f), x(0.92f), y(0.5f))
                path.quadTo(x(0.76f), y(0.38f), x(0.6f), y(0.5f))
                path.quadTo(x(0.5f), y(0.4f), x(0.4f), y(0.5f))
                path.quadTo(x(0.24f), y(0.38f), x(0.08f), y(0.5f))
                path.close()
                c.drawPath(path, p)
                stroke(0.045f)
                c.drawLine(x(0.5f), y(0.5f), x(0.5f), y(0.88f), p)
                rf.set(x(0.5f), y(0.8f), x(0.62f), y(0.92f))
                c.drawArc(rf, 90f, 180f, false, p)
            }
        }
    }
}

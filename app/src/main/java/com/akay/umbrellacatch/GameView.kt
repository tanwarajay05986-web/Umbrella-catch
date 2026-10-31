package com.akay.umbrellacatch

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.View
import kotlin.random.Random

private class Drop(var x: Float, var y: Float, val speed: Float, val color: Int, val size: Float)
private class Pop(var x: Float, var y: Float, var life: Float, val text: String)

class GameView(ctx: Context) : View(ctx) {

    var onScore: (Int) -> Unit = {}
    var onBest: (Int) -> Unit = {}

    private val sky = Paint(Paint.ANTI_ALIAS_FLAG)
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rf = RectF()

    private val drops = ArrayList<Drop>()
    private val pops = ArrayList<Pop>()
    private val rnd = Random(System.currentTimeMillis())
    private val colors = intArrayOf(
        0xFFE23B3B.toInt(), 0xFFFFC233.toInt(), 0xFF8A4DFF.toInt(),
        0xFF3BD15A.toInt(), 0xFF3B8CFF.toInt(), 0xFFFF4D9E.toInt()
    )

    private var bucketX = 0.5f
    private var dir = 0
    private var score = 0
    private var running = false
    private var spawnTimer = 0f
    private var spawnGap = 1.1f
    private var fallSpeed = 0.26f
    private var lastT = 0L
    private var wobble = 0f

    private val handler = Handler(Looper.getMainLooper())
    private val loop = object : Runnable {
        override fun run() {
            if (running) {
                tick()
                invalidate()
                handler.postDelayed(this, 16)
            }
        }
    }

    init {
        tp.typeface = Typeface.DEFAULT_BOLD
        tp.textAlign = Paint.Align.CENTER
    }

    fun start() {
        drops.clear()
        pops.clear()
        score = 0
        spawnGap = 1.1f
        fallSpeed = 0.26f
        onScore(0)
        lastT = System.nanoTime()
        running = true
        handler.removeCallbacks(loop)
        handler.post(loop)
    }

    fun pause() {
        running = false
        handler.removeCallbacks(loop)
    }

    fun resumeGame() {
        if (running) return
        lastT = System.nanoTime()
        running = true
        handler.post(loop)
    }

    fun setDir(d: Int) {
        dir = d
    }

    private fun tick() {
        val now = System.nanoTime()
        var dt = (now - lastT) / 1_000_000_000f
        lastT = now
        if (dt > 0.05f) dt = 0.05f
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        bucketX += dir * dt * 1.1f
        bucketX = bucketX.coerceIn(0.1f, 0.9f)

        spawnTimer -= dt
        if (spawnTimer <= 0f) {
            spawnTimer = spawnGap * (0.75f + rnd.nextFloat() * 0.5f)
            val size = h * 0.06f
            drops.add(
                Drop(
                    0.1f + rnd.nextFloat() * 0.8f, -0.08f,
                    fallSpeed * (0.85f + rnd.nextFloat() * 0.3f),
                    colors[rnd.nextInt(colors.size)], size
                )
            )
        }

        val bucketY = 0.86f
        val bucketHalf = 0.11f
        val it = drops.iterator()
        while (it.hasNext()) {
            val d = it.next()
            d.y += d.speed * dt
            if (d.y > bucketY - 0.03f && d.y < bucketY + 0.06f && Math.abs(d.x - bucketX) < bucketHalf) {
                score += 10
                onScore(score)
                if (score > Store.best()) {
                    Store.setBest(score)
                    onBest(score)
                }
                pops.add(Pop(d.x, d.y, 1f, "+10"))
                Sfx.catch()
                wobble = 1f
                it.remove()
                spawnGap = (spawnGap - 0.004f).coerceAtLeast(0.46f)
                fallSpeed = (fallSpeed + 0.0025f).coerceAtMost(0.62f)
                continue
            }
            if (d.y > 1.1f) it.remove()
        }

        val pit = pops.iterator()
        while (pit.hasNext()) {
            val pop = pit.next()
            pop.life -= dt * 1.4f
            pop.y -= dt * 0.12f
            if (pop.life <= 0f) pit.remove()
        }

        if (wobble > 0f) wobble = (wobble - dt * 3f).coerceAtLeast(0f)
    }

    private fun umbrella(c: Canvas, cx: Float, cy: Float, s: Float, col: Int) {
        path.reset()
        path.moveTo(cx - s, cy)
        path.quadTo(cx, cy - s * 1.05f, cx + s, cy)
        path.quadTo(cx + s * 0.6f, cy - s * 0.18f, cx + s * 0.3f, cy)
        path.quadTo(cx, cy - s * 0.12f, cx - s * 0.3f, cy)
        path.quadTo(cx - s * 0.6f, cy - s * 0.18f, cx - s, cy)
        path.close()
        p.style = Paint.Style.FILL
        p.color = col
        c.drawPath(path, p)
        p.color = Color.WHITE
        p.alpha = 70
        c.drawLine(cx, cy - s * 0.9f, cx, cy, p)
        p.alpha = 255
        p.color = 0xFF6B4A2A.toInt()
        p.style = Paint.Style.STROKE
        p.strokeWidth = s * 0.1f
        c.drawLine(cx, cy, cx, cy + s * 0.55f, p)
        rf.set(cx, cy + s * 0.4f, cx + s * 0.3f, cy + s * 0.7f)
        c.drawArc(rf, 90f, 180f, false, p)
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        sky.shader = android.graphics.LinearGradient(0f, 0f, 0f, h, 0xFF57B6F2.toInt(), 0xFFBDEBFF.toInt(), android.graphics.Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, sky)

        for (d in drops) umbrella(c, d.x * w, d.y * h, d.size, d.color)

        val bx = bucketX * w
        val by = h * 0.86f
        val bw = h * 0.1f * (1f + wobble * 0.18f)
        p.style = Paint.Style.FILL
        p.color = 0xFFE23B3B.toInt()
        path.reset()
        path.moveTo(bx - bw, by)
        path.lineTo(bx + bw, by)
        path.lineTo(bx + bw * 0.78f, by + bw * 1.1f)
        path.lineTo(bx - bw * 0.78f, by + bw * 1.1f)
        path.close()
        c.drawPath(path, p)
        p.color = 0xFFFFD23B.toInt()
        rf.set(bx - bw * 1.02f, by - bw * 0.22f, bx + bw * 1.02f, by + bw * 0.08f)
        c.drawRoundRect(rf, bw * 0.12f, bw * 0.12f, p)
        p.color = Color.WHITE
        val starPath = Path()
        for (i in 0 until 10) {
            val rad = if (i % 2 == 0) bw * 0.4f else bw * 0.18f
            val a = Math.toRadians(-90.0 + i * 36.0)
            val px = bx + (rad * Math.cos(a)).toFloat()
            val py = by + bw * 0.45f + (rad * Math.sin(a)).toFloat()
            if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
        }
        starPath.close()
        c.drawPath(starPath, p)

        for (pop in pops) {
            tp.color = Color.argb((pop.life * 255).toInt().coerceIn(0, 255), 255, 255, 255)
            tp.textSize = h * 0.035f
            c.drawText(pop.text, pop.x * w, pop.y * h, tp)
        }
    }
}

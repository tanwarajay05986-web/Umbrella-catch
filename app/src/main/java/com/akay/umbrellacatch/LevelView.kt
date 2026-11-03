package com.akay.umbrellacatch

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.view.MotionEvent
import android.view.View
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private class LDrop(
    val x: Float, var y: Float, val vy: Float, val color: Int,
    val phase: Float, val stone: Boolean, val special: Boolean
)

private class LPop(val x: Float, var y: Float, var life: Float, val text: String, val color: Int)
private class LLanded(val x: Float, val y: Float, val color: Int, var life: Float)
private class LStreak(var x: Float, var y: Float, val len: Float, val spd: Float)

// one level (or one stage of a level). The whole screen is drawn by this one view.
class LevelView(ctx: Context, private val spec: Spec) : View(ctx), InsetAware {

    var onNext: () -> Unit = {}
    var onRetry: () -> Unit = {}
    var onMap: () -> Unit = {}

    private var insetTop = (24 * resources.displayMetrics.density).toInt()
    private var insetBottom = 0

    private val rnd = Random(System.nanoTime())
    private val drops = ArrayList<LDrop>()
    private val pops = ArrayList<LPop>()
    private val landed = ArrayList<LLanded>()
    private val rain = ArrayList<LStreak>()
    private val clip = Path()

    private val demo = spec.special >= 0
    private var animT = 0f
    private var playT = 0f
    private var phase = 0
    private var introT = 2.8f
    private var paused = false
    private var won = false
    private var reason = ""
    private var score = 0
    private var best = Store.stageBest(spec.level, spec.stage)
    private var hearts = spec.hearts
    private var timeLeft = spec.time
    private var bx = 0.5f
    private var dirL = false
    private var dirR = false
    private var dragX = -1f
    private var spawnT = 0.6f
    private var stoneT = 2.4f
    private var wob = 0f
    private var flash = 0f
    private var last = 0L
    private var alive = false
    private var pausePressed = false

    private val pauseR = RectF()
    private val startR = RectF()
    private val resumeR = RectF()
    private val homeR = RectF()
    private val resR = Array(3) { RectF() }
    private val resAct = IntArray(3)
    private var resCount = 0
    private var lcx = 0f
    private var rcx = 0f
    private var lcy = 0f
    private var btnR = 0f

    private val white = 0xFFFFFFFF.toInt()
    private val gold = 0xFFFFC21A.toInt()
    private val yellow = 0xFFFFD43B.toInt()
    private val navy = 0xFF0A2A66.toInt()
    private val green = 0xFF6BFF8A.toInt()

    init {
        for (i in 0 until 36) {
            rain.add(LStreak(rnd.nextFloat(), rnd.nextFloat(), 0.03f + rnd.nextFloat() * 0.03f, 0.9f + rnd.nextFloat() * 0.6f))
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
        if (phase == 1) paused = true
    }

    // the phone's back button: first it pauses the game, then it goes back to the map
    fun backPressed(): Boolean {
        if (phase == 1 && !paused) {
            paused = true
            return true
        }
        return false
    }

    private fun rimYOf(hh: Float, bw: Float): Float = hh * 0.862f - bw * 0.86f

    private fun fade(color: Int): Int = D.mix(color, white, 0.6f)

    // ---------- game logic ----------

    private fun update(dt: Float) {
        animT += dt
        for (s in rain) {
            s.y += s.spd * dt
            s.x -= 0.05f * dt
            if (s.y > 1.05f) {
                s.y = -0.05f
                s.x = rnd.nextFloat()
            }
            if (s.x < -0.05f) s.x = 1.05f
        }
        if (flash > 0f) flash = (flash - dt * 2f).coerceAtLeast(0f)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        if (phase == 0) {
            if (!demo) {
                introT -= dt
                if (introT <= 0f) phase = 1
            }
            return
        }
        if (paused) return
        val hh = h - insetBottom

        if (wob > 0f) wob = (wob - dt * 3f).coerceAtLeast(0f)
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
        if (phase == 2) return

        playT += dt
        if (spec.timed) {
            timeLeft -= dt
            if (timeLeft <= 0f) {
                timeLeft = 0f
                finish(false, "Time's up!")
                return
            }
        }

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
            spawnT = spec.gap * (0.75f + rnd.nextFloat() * 0.5f)
            spawnUmbrella(w, hh)
        }
        if (spec.stones) {
            stoneT -= dt
            if (stoneT <= 0f) {
                stoneT = spec.stoneGap * (0.75f + rnd.nextFloat() * 0.5f)
                drops.add(
                    LDrop(
                        0.12f + rnd.nextFloat() * 0.76f, -w * 0.12f,
                        spec.speed * hh * 0.78f * (0.92f + rnd.nextFloat() * 0.16f),
                        0, rnd.nextFloat() * 6.28f, true, false
                    )
                )
            }
        }

        val ur = w * 0.095f
        val rimY = rimYOf(hh, bw)
        val iter = drops.iterator()
        while (iter.hasNext()) {
            val d = iter.next()
            d.y += d.vy * dt
            val dx = (d.x + sin(playT * 2f + d.phase) * 0.012f) * w
            if (d.y > rimY - ur * 0.15f && d.y < rimY + bw * 0.35f && abs(dx - bx * w) < bw * 0.46f) {
                iter.remove()
                if (d.stone) hitStone(dx, rimY, ur) else catchUmbrella(d, dx, rimY, ur)
                if (phase == 2) return
            } else if (d.y > hh + ur * 2f) {
                iter.remove()
            }
        }
    }

    private fun spawnUmbrella(w: Float, hh: Float) {
        val sp = spec.special
        val isSp = sp >= 0 && rnd.nextFloat() < 0.3f
        var ci = rnd.nextInt(D.UMB.size)
        if (sp >= 0) {
            if (isSp) {
                ci = sp
            } else {
                ci = rnd.nextInt(D.UMB.size - 1)
                if (ci >= sp) ci++
            }
        }
        drops.add(
            LDrop(
                0.12f + rnd.nextFloat() * 0.76f, -w * 0.12f,
                spec.speed * hh * (0.88f + rnd.nextFloat() * 0.24f),
                D.UMB[ci], rnd.nextFloat() * 6.28f, false, isSp
            )
        )
    }

    private fun catchUmbrella(d: LDrop, dx: Float, rimY: Float, ur: Float) {
        score += 10
        if (score > best) {
            best = score
            Store.setStageBest(spec.level, spec.stage, best)
        }
        val col = if (spec.special >= 0 && !d.special) fade(d.color) else d.color
        landed.add(LLanded(dx, d.y, col, 0.28f))
        pops.add(LPop(dx, rimY - ur, 1f, "+10", yellow))
        Sfx.catch()
        wob = 1f
        if (d.special) {
            timeLeft = min(Levels.MAX_TIME, timeLeft + Levels.BONUS_TIME)
            pops.add(LPop(dx, rimY - ur * 2.4f, 1.3f, "+3s", green))
            Sfx.bonus()
        }
        if (score >= spec.target) finish(true, "")
    }

    private fun hitStone(dx: Float, rimY: Float, ur: Float) {
        hearts -= 1
        timeLeft -= Levels.STONE_TIME
        flash = 1f
        wob = 1f
        pops.add(LPop(dx, rimY - ur, 1.3f, "-5s", 0xFFFF6B6B.toInt()))
        Sfx.hit()
        if (hearts <= 0) {
            hearts = 0
            finish(false, "No hearts left!")
        } else if (timeLeft <= 0f) {
            timeLeft = 0f
            finish(false, "Time's up!")
        }
    }

    private fun finish(win: Boolean, why: String) {
        phase = 2
        won = win
        reason = why
        dirL = false
        dirR = false
        dragX = -1f
        if (win) {
            Store.setStageDone(spec.level, spec.stage)
            Sfx.win()
        } else {
            Sfx.lose()
        }
    }

    // ---------- touch ----------

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val act = e.actionMasked
        if (phase == 0) {
            if (act == MotionEvent.ACTION_UP) {
                if (!demo) {
                    phase = 1
                } else if (startR.contains(e.x, e.y)) {
                    phase = 1
                }
            }
            return true
        }
        if (phase == 2) {
            if (act == MotionEvent.ACTION_UP) {
                for (i in 0 until resCount) {
                    if (resR[i].contains(e.x, e.y)) {
                        when (resAct[i]) {
                            0 -> onNext()
                            1 -> onRetry()
                            else -> onMap()
                        }
                        break
                    }
                }
            }
            return true
        }
        if (paused) {
            if (act == MotionEvent.ACTION_UP) {
                if (resumeR.contains(e.x, e.y)) {
                    paused = false
                } else if (homeR.contains(e.x, e.y)) {
                    onMap()
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

    // ---------- drawing ----------

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val hh = h - insetBottom
        val k = min(w / 941f, hh / 1672f)
        val top = insetTop.toFloat()
        val p = D.p

        D.gameSky(c, w, h, hh, k, animT)
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
            val dx = (d.x + sin(playT * 2f + d.phase) * 0.012f) * w
            if (d.stone) {
                D2.stone(c, dx, d.y, ur * 0.85f)
            } else if (d.special) {
                val gy = d.y - ur * 0.3f
                D.circle(c, dx, gy, ur * 1.15f, 0x33FFFFFF)
                D.ring(c, dx, gy, ur * (1.2f + 0.08f * sin(animT * 6f)), 0xCCFFFFFF.toInt(), 5f * k)
                D.umbrella(c, dx, d.y, ur, d.color)
            } else if (spec.special >= 0) {
                D.umbrella(c, dx, d.y, ur, fade(d.color))
            } else {
                D.umbrella(c, dx, d.y, ur, d.color)
            }
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
        if (flash > 0f) {
            D.fillRR(
                c, bxp - bw * 0.5f, rimY - bw * 0.08f, bxp + bw * 0.5f, rimY + bh, bw * 0.18f,
                D.withA(0xFFFF2020.toInt(), (flash * 120f).toInt())
            )
        }
        c.restore()

        for (pp in pops) {
            val a = (pp.life.coerceIn(0f, 1f) * 255f).toInt()
            D.text(c, pp.text, pp.x, pp.y, 46f * k, D.withA(pp.color, a), Paint.Align.CENTER, D.withA(navy, a), 6f * k)
        }

        // the big round buttons
        btnR = 88f * k
        lcx = w * 0.135f
        rcx = w * 0.865f
        lcy = hh * 0.915f
        D.arrowBtn(c, lcx, lcy, btnR, true, dirL, k)
        D.arrowBtn(c, rcx, lcy, btnR, false, dirR, k)

        drawHud(c, w, top, k)
        if (phase == 0) {
            if (demo) drawDemo(c, w, h, hh, k) else drawIntro(c, w, hh, k)
        }
        if (phase == 2) drawResult(c, w, h, hh, k)
        if (paused) drawPause(c, w, h, hh, k)
    }

    private fun fmtTime(t: Float): String {
        val s = ceil(t).toInt().coerceAtLeast(0)
        return String.format(Locale.US, "%d:%02d", s / 60, s % 60)
    }

    private fun pill(c: Canvas, l: Float, r: Float, hy: Float, k: Float, icon: Int, iconCol: Int, label: String, value: String) {
        D.hudPill(c, l, hy - 42f * k, r, hy + 42f * k, k)
        D.icon(c, icon, l + 46f * k, hy, 56f * k, iconCol)
        D.text(c, label, l + 86f * k, hy - 6f * k, 26f * k, white, Paint.Align.LEFT)
        D.text(c, value, l + 86f * k, hy + 32f * k, 42f * k, yellow, Paint.Align.LEFT, navy, 5f * k)
    }

    private fun drawHud(c: Canvas, w: Float, top: Float, k: Float) {
        val hy = top + 58f * k
        pill(c, w * 0.02f, w * 0.295f, hy, k, K.TROPHY, gold, "Score", score.toString())
        pill(c, w * 0.335f, w * 0.60f, hy, k, K.TARGET, 0xFFFF5A5A.toInt(), "Target", spec.target.toString())
        pill(c, w * 0.64f, w * 0.865f, hy, k, K.CROWN, gold, "Best", best.toString())

        val pcx = w * 0.935f
        val pr = 46f * k
        D.hudCircle(c, pcx, hy, pr, k)
        D.icon(c, K.PAUSE, pcx, hy, 40f * k, white)
        pauseR.set(pcx - pr - 14f * k, hy - pr - 14f * k, pcx + pr + 14f * k, hy + pr + 14f * k)

        if (!spec.timed) return
        val y2 = hy + 92f * k
        val hf = 38f * k

        // time
        val tl = w * 0.02f
        val tr = w * 0.30f
        D.hudPill(c, tl, y2 - hf, tr, y2 + hf, k)
        D2.clock(c, tl + 46f * k, y2, 24f * k, gold)
        val low = timeLeft <= 10f
        val tc = if (low && ((animT * 4f).toInt() % 2 == 0)) 0xFFFF6B6B.toInt() else yellow
        D.text(c, fmtTime(timeLeft), tl + 86f * k, y2 + 15f * k, 42f * k, tc, Paint.Align.LEFT, navy, 5f * k)

        if (spec.special >= 0) {
            val l2 = w * 0.335f
            val r2 = w * 0.60f
            D.hudPill(c, l2, y2 - hf, r2, y2 + hf, k)
            D.umbrella(c, l2 + 50f * k, y2 + 4f * k, 26f * k, D.UMB[spec.special])
            D.text(c, "+3s", l2 + 96f * k, y2 + 15f * k, 40f * k, green, Paint.Align.LEFT, navy, 5f * k)
        }
        if (spec.stones) {
            val l2 = w * 0.335f
            val r2 = w * 0.985f
            D.hudPill(c, l2, y2 - hf, r2, y2 + hf, k)
            for (i in 0 until Levels.HEARTS) {
                val hx = l2 + (r2 - l2) * (0.09f + i * 0.165f)
                val full = i < hearts
                D2.heart(
                    c, hx, y2 + 2f * k, 21f * k,
                    if (full) 0xFFFF4D5E.toInt() else 0xFF3A4F7A.toInt(),
                    if (full) 0xFFB8162B.toInt() else 0xFF22345A.toInt()
                )
            }
        }
    }

    private fun drawReach(c: Canvas, cx: Float, y: Float, k: Float, a: Int, num: String) {
        val sz = 44f * k
        val big = 60f * k
        val suffix = " Points"
        val p = D.p
        p.shader = null
        p.typeface = D.BLACK
        p.textSize = sz
        val w1 = p.measureText("Reach ")
        val w3 = p.measureText(suffix)
        p.textSize = big
        val w2 = p.measureText(num)
        val total = w1 + w2 + w3
        val maxW = 560f * k
        val s = if (total > maxW) maxW / total else 1f
        var x = cx - total * s / 2f
        D.text(c, "Reach ", x, y, sz * s, D.withA(white, a), Paint.Align.LEFT)
        x += w1 * s
        D.text(c, num, x, y, big * s, D.withA(yellow, a), Paint.Align.LEFT, D.withA(navy, a), 6f * k * s)
        x += w2 * s
        D.text(c, suffix, x, y, sz * s, D.withA(white, a), Paint.Align.LEFT)
    }

    // "Catch Umbrellas!" and what to do in this level
    private fun drawIntro(c: Canvas, w: Float, hh: Float, k: Float) {
        val a = (min(1f, introT / 0.5f) * 255f).toInt().coerceIn(0, 255)
        val cx = w / 2f
        val cy = hh * 0.42f
        val label = if (Levels.hasStages(spec.level)) {
            "Level ${spec.level} \u2022 Stage ${Levels.letter(spec.stage)}"
        } else {
            "Level ${spec.level}"
        }
        D.text(c, label, cx, cy - 165f * k, 40f * k, D.withA(white, a), Paint.Align.CENTER, D.withA(navy, a), 7f * k)
        D.text(c, "Catch", cx, cy - 40f * k, 100f * k, D.withA(white, a), Paint.Align.CENTER, D.withA(0xFF1F5FBF.toInt(), a), 14f * k)
        val s2 = min(120f * k, D.fit("Umbrellas!", 560f * k))
        D.text(c, "Umbrellas!", cx, cy + 70f * k, s2, D.withA(0xFFFFD42A.toInt(), a), Paint.Align.CENTER, D.withA(0xFF2A2A6A.toInt(), a), 14f * k)

        val ph = if (spec.stones) 200f else 150f
        D.fillRR(c, cx - 330f * k, cy + 105f * k, cx + 330f * k, cy + (105f + ph) * k, 46f * k, D.withA(0xFF7A1E6E.toInt(), (a * 0.92f).toInt()))
        drawReach(c, cx, cy + 168f * k, k, a, spec.target.toString())
        val line2 = if (spec.timed) "in ${spec.time.toInt()} seconds!" else "to Complete the Game!"
        D.text(c, line2, cx, cy + 217f * k, min(34f * k, D.fit(line2, 560f * k)), D.withA(white, a))
        if (spec.stones) {
            val l3 = "Stone: -1 heart and -5 sec!"
            D.text(c, l3, cx, cy + 262f * k, min(30f * k, D.fit(l3, 560f * k)), D.withA(yellow, a))
        }
    }

    private fun parts(c: Canvas, cx: Float, y: Float, size: Float, maxW: Float, txt: Array<String>, col: IntArray, k: Float) {
        val p = D.p
        p.shader = null
        p.typeface = D.BLACK
        p.textSize = size
        var total = 0f
        for (t in txt) total += p.measureText(t)
        val s = if (total > maxW) maxW / total else 1f
        var x = cx - total * s / 2f
        for (i in txt.indices) {
            p.textSize = size
            val wd = p.measureText(txt[i]) * s
            D.text(c, txt[i], x, y, size * s, col[i], Paint.Align.LEFT, navy, 6f * k * s)
            x += wd
        }
    }

    private fun panelBase(c: Canvas, w: Float, h: Float, pl: Float, pt: Float, pr: Float, pb: Float, k: Float) {
        D.p.shader = null
        D.p.style = Paint.Style.FILL
        D.p.color = 0xAA0A1B3D.toInt()
        c.drawRect(0f, 0f, w, h, D.p)
        D.fillRR(c, pl - 6f * k, pt - 6f * k, pr + 6f * k, pb + 6f * k, 46f * k, white)
        D.gradRR(c, pl, pt, pr, pb, 42f * k, 0xFF3F9BFF.toInt(), 0xFF1450C8.toInt())
    }

    // Stage B: a small demo that shows what to do, then a Start button
    private fun drawDemo(c: Canvas, w: Float, h: Float, hh: Float, k: Float) {
        val pl = w * 0.07f
        val pr = w * 0.93f
        val pt = hh * 0.1f
        val pb = pt + 880f * k
        panelBase(c, w, h, pl, pt, pr, pb, k)
        val cx = w / 2f
        val inner = (pr - pl) - 70f * k
        D.text(c, "Stage B", cx, pt + 105f * k, 78f * k, white, Paint.Align.CENTER, navy, 10f * k)
        D.text(c, "Level ${spec.level}", cx, pt + 158f * k, 32f * k, 0xFFBFE2FF.toInt())
        val colName = Levels.COLOR_NAMES[spec.special]
        parts(
            c, cx, pt + 238f * k, 44f * k, inner,
            arrayOf("Catch the ", colName, " umbrella!"),
            intArrayOf(white, D.UMB[spec.special], white), k
        )
        val l2 = "+3 seconds every time!"
        D.text(c, l2, cx, pt + 292f * k, min(42f * k, D.fit(l2, inner)), green, Paint.Align.CENTER, navy, 6f * k)
        val l3 = "Pale umbrellas give only points."
        D.text(c, l3, cx, pt + 338f * k, min(30f * k, D.fit(l3, inner)), 0xFFD6E8FF.toInt())

        // demo box
        val dl = pl + 40f * k
        val dr = pr - 40f * k
        val dt0 = pt + 372f * k
        val db = pt + 700f * k
        val dw = dr - dl
        clip.reset()
        D.rf.set(dl, dt0, dr, db)
        clip.addRoundRect(D.rf, 30f * k, 30f * k, Path.Direction.CW)
        c.save()
        c.clipPath(clip)
        D.p.style = Paint.Style.FILL
        D.p.shader = LinearGradient(0f, dt0, 0f, db, 0xFF57B6F2.toInt(), 0xFFBDEBFF.toInt(), Shader.TileMode.CLAMP)
        c.drawRect(dl, dt0, dr, db, D.p)
        D.p.shader = null
        D.cloud(c, dl + dw * 0.2f, dt0 + 60f * k, 40f * k)
        D.cloud(c, dl + dw * 0.82f, dt0 + 110f * k, 32f * k)
        D.fillRR(c, dl, db - 56f * k, dr, db + 20f * k, 0f, 0xFF63C94B.toInt())

        val cxD = (dl + dr) / 2f
        val bwD = 150f * k
        val bhD = bwD * 0.86f
        val rimD = db - 40f * k - bhD
        val period = 3.6f
        val tt = (animT % period) / period

        for (j in 0..1) {
            val ft = (tt + 0.2f + j * 0.5f) % 1f
            val ux = dl + dw * (0.17f + 0.66f * j)
            val uy = dt0 - 30f * k + (db - dt0 + 80f * k) * ft
            D.umbrella(c, ux, uy, 34f * k, fade(D.UMB[(spec.special + 2 + j * 2) % 6]))
        }

        val wb = if (tt >= 0.5f && tt <= 0.68f) 1f - (tt - 0.5f) / 0.18f else 0f
        c.save()
        c.scale(1f + wb * 0.08f, 1f - wb * 0.06f, cxD, rimD + bhD)
        D.bucketBack(c, cxD, rimD, bwD)
        if (tt < 0.5f) {
            val y0 = dt0 - 40f * k
            val sy = y0 + ((rimD + 25f * k) - y0) * (tt / 0.5f)
            D.circle(c, cxD, sy - 22f * k, 50f * k, 0x33FFFFFF)
            D.ring(c, cxD, sy - 22f * k, (52f + 4f * sin(animT * 6f)) * k, 0xCCFFFFFF.toInt(), 4f * k)
            D.umbrella(c, cxD, sy, 40f * k, D.UMB[spec.special])
        }
        D.bucketFront(c, cxD, rimD, bwD, bhD)
        c.restore()

        if (tt >= 0.5f) {
            val q = ((tt - 0.5f) / 0.45f).coerceIn(0f, 1f)
            val a = ((1f - q) * 255f).toInt().coerceIn(0, 255)
            val yy = rimD - 20f * k - q * 120f * k
            D2.clock(c, cxD - 85f * k, yy - 16f * k, 26f * k, D.withA(gold, a))
            D.text(c, "+3s", cxD + 20f * k, yy, 58f * k, D.withA(green, a), Paint.Align.CENTER, D.withA(navy, a), 7f * k)
        }
        c.restore()
        D.strokeRR(c, dl, dt0, dr, db, 30f * k, 0xAAFFFFFF.toInt(), 4f * k)

        // start button
        val bl = pl + 60f * k
        val br = pr - 60f * k
        val by = pt + 790f * k
        D.gloss(c, bl, by - 48f * k, br, by + 48f * k, 0xFF55E36F.toInt(), 0xFF1D9F39.toInt(), 0xFFFFD84D.toInt(), 0x66105A22, k)
        D.icon(c, K.PLAY, bl + 66f * k, by, 50f * k, white)
        D.text(c, "Start", cx + 28f * k, by + 17f * k, 50f * k, white, Paint.Align.CENTER, 0xFF0F6B22.toInt(), 7f * k)
        startR.set(bl, by - 54f * k, br, by + 54f * k)
    }

    private fun resBtn(c: Canvas, idx: Int, cy: Float, bl: Float, br: Float, k: Float, label: String, style: Int, icon: Int, act: Int) {
        val c1: Int
        val c2: Int
        val bd: Int
        val sh: Int
        val st: Int
        when (style) {
            0 -> {
                c1 = 0xFF55E36F.toInt(); c2 = 0xFF1D9F39.toInt(); bd = 0xFFFFD84D.toInt(); sh = 0x66105A22; st = 0xFF0F6B22.toInt()
            }
            1 -> {
                c1 = 0xFFFFB02E.toInt(); c2 = 0xFFFF7A00.toInt(); bd = 0xFFFFE0A0.toInt(); sh = 0x66803000; st = 0xFF9A4A00.toInt()
            }
            else -> {
                c1 = 0xFFB56DFF.toInt(); c2 = 0xFF6C2BD8.toInt(); bd = 0xFFD9B8FF.toInt(); sh = 0x66331166; st = 0xFF3A1280.toInt()
            }
        }
        D.gloss(c, bl, cy - 48f * k, br, cy + 48f * k, c1, c2, bd, sh, k)
        D.icon(c, icon, bl + 66f * k, cy, 50f * k, white)
        D.text(
            c, label, (bl + br) / 2f + 28f * k, cy + 17f * k,
            min(48f * k, D.fit(label, (br - bl) - 220f * k)), white, Paint.Align.CENTER, st, 7f * k
        )
        resR[idx].set(bl, cy - 54f * k, br, cy + 54f * k)
        resAct[idx] = act
    }

    // a simple result panel (the exact "Level Complete" screen comes in Stage 3)
    private fun drawResult(c: Canvas, w: Float, h: Float, hh: Float, k: Float) {
        val staged = Levels.hasStages(spec.level)
        val lastStage = !staged || spec.stage == 2
        val nxt = Levels.next(spec.level, spec.stage)

        val labels = ArrayList<String>()
        val styles = ArrayList<Int>()
        val icons = ArrayList<Int>()
        val acts = ArrayList<Int>()
        if (won) {
            if (nxt != null) {
                labels.add(if (nxt[0] == spec.level) "Next Stage" else "Next Level")
                styles.add(0)
                icons.add(K.FWD)
                acts.add(0)
            }
            labels.add("Play Again")
            styles.add(1)
            icons.add(K.PLAY)
            acts.add(1)
            labels.add("Levels")
            styles.add(2)
            icons.add(K.GRID)
            acts.add(2)
        } else {
            labels.add("Try Again")
            styles.add(0)
            icons.add(K.PLAY)
            acts.add(1)
            labels.add("Levels")
            styles.add(2)
            icons.add(K.GRID)
            acts.add(2)
        }
        val n = labels.size
        resCount = n

        val pl = w * 0.1f
        val pr = w * 0.9f
        val pt = hh * 0.25f
        val pb = pt + (450f + n * 135f) * k
        panelBase(c, w, h, pl, pt, pr, pb, k)
        val cx = w / 2f
        if (won) D.mascot(c, cx, pt - 40f * k, 230f * k)

        val title = if (!won) "Try Again!" else if (lastStage) "Level Complete!" else "Stage ${Levels.letter(spec.stage)} Complete!"
        D.text(
            c, title, cx, pt + 170f * k, min(66f * k, D.fit(title, (pr - pl) - 80f * k)),
            0xFFFFD42A.toInt(), Paint.Align.CENTER, 0xFF8A3E08.toInt(), 10f * k
        )

        // score and target
        val bl = pl + 45f * k
        val br = pr - 45f * k
        val bt = pt + 215f * k
        val bb = pt + 335f * k
        D.fillRR(c, bl - 5f * k, bt - 5f * k, br + 5f * k, bb + 5f * k, 34f * k, 0xFFFFC84A.toInt())
        D.gradRR(c, bl, bt, br, bb, 30f * k, 0xFF1B4C9E.toInt(), 0xFF0B2A66.toInt())
        D.fillRR(c, cx - 2f * k, bt + 18f * k, cx + 2f * k, bb - 18f * k, 2f * k, 0xAAFFC84A.toInt())
        val lc = (bl + cx) / 2f
        val rc = (cx + br) / 2f
        D.text(c, "Your Score", lc, bt + 42f * k, 28f * k, white)
        D.icon(c, K.STAR, lc - 62f * k, bt + 86f * k, 46f * k, gold)
        D.text(c, score.toString(), lc + 24f * k, bt + 100f * k, 52f * k, yellow, Paint.Align.CENTER, navy, 6f * k)
        D.text(c, "Target", rc, bt + 42f * k, 28f * k, white)
        D.icon(c, K.TARGET, rc - 62f * k, bt + 86f * k, 46f * k, 0xFFFF5A5A.toInt())
        D.text(c, spec.target.toString(), rc + 24f * k, bt + 100f * k, 52f * k, yellow, Paint.Align.CENTER, navy, 6f * k)

        if (won) {
            D.text(c, "Amazing!", cx, pt + 398f * k, 50f * k, 0xFF8BFF6B.toInt(), Paint.Align.CENTER, 0xFF1D6B22.toInt(), 8f * k)
        } else {
            D.text(c, reason, cx, pt + 398f * k, min(44f * k, D.fit(reason, (pr - pl) - 80f * k)), 0xFFFFE066.toInt(), Paint.Align.CENTER, navy, 6f * k)
        }

        for (i in 0 until n) {
            val cy = pt + 450f * k + 55f * k + i * 135f * k
            resBtn(c, i, cy, pl + 60f * k, pr - 60f * k, k, labels[i], styles[i], icons[i], acts[i])
        }
    }

    private fun drawPause(c: Canvas, w: Float, h: Float, hh: Float, k: Float) {
        val pl = w * 0.14f
        val pr = w * 0.86f
        val pt = hh * 0.3f
        val pb = pt + 520f * k
        panelBase(c, w, h, pl, pt, pr, pb, k)
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
        D.icon(c, K.GRID, bl + 70f * k, hy, 52f * k, white)
        D.text(c, "Levels", w / 2f + 30f * k, hy + 18f * k, 52f * k, white, Paint.Align.CENTER, 0xFF3A1280.toInt(), 7f * k)
        homeR.set(bl, hy - 55f * k, br, hy + 55f * k)
    }
}

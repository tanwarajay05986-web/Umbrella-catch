package com.akay.umbrellacatch

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

internal const val K_UMB = 0
internal const val K_STONE = 1
internal const val K_BONUS = 2
internal const val K_MAGNET = 3
internal const val K_REPEL = 4
internal const val K_BIG = 5
internal const val K_FREEZE = 6
internal const val K_GOLD = 7
internal const val K_MULT = 8
internal const val K_ELASTIC = 9

internal object LC {
    val white = 0xFFFFFFFF.toInt()
    val gold = 0xFFFFC21A.toInt()
    val yellow = 0xFFFFD43B.toInt()
    val navy = 0xFF0A2A66.toInt()
    val green = 0xFF6BFF8A.toInt()
    val red = 0xFFFF6B6B.toInt()
}

internal class LDrop(var x: Float, var y: Float, val vy: Float, val color: Int, val phase: Float, val kind: Int) {
    var vx = 0f
    var tp = false
    var bt = 0f
    var bounced = false
    var hue = 0
}

internal class LPop(val x: Float, var y: Float, var life: Float, val text: String, val color: Int)
internal class LLanded(val x: Float, val y: Float, val color: Int, var life: Float, val bi: Int)
internal class LStreak(var x: Float, var y: Float, val len: Float, val spd: Float)

// one level (or one stage of a level). The whole screen is drawn by this one view.
class LevelView(ctx: Context, internal val spec: Spec) : View(ctx), InsetAware {

    var onNext: () -> Unit = {}
    var onRetry: () -> Unit = {}
    var onMap: () -> Unit = {}

    internal var insetTop = (24 * resources.displayMetrics.density).toInt()
    internal var insetBottom = 0

    private val rnd = Random(System.nanoTime())
    private val drops = ArrayList<LDrop>()
    private val pops = ArrayList<LPop>()
    private val landed = ArrayList<LLanded>()
    private val rain = ArrayList<LStreak>()
    private val fireworks = Fireworks()
    private val theme = Fx.theme(spec.level)

    internal val COL3 = intArrayOf(D.UMB[0], D.UMB[4], D.UMB[1])
    internal val colorNames = arrayOf("Red", "Blue", "Yellow")
    private val goldPts = if (spec.level == 30) 30 else 20

    // levels 16+ and stage B (7-15) wait for the Start button
    internal val needStart = spec.special >= 0 || spec.level >= 16
    internal var animT = 0f
    private var playT = 0f
    internal var phase = 0
    internal var introT = 2.8f
    internal var paused = false
    internal var won = false
    internal var reason = ""
    internal var score = 0
    internal var best = Store.stageBest(spec.level, spec.stage)
    internal var hearts = spec.hearts
    internal var timeLeft = spec.time

    private var bx = 0.5f
    private var bv = 0f
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

    // effects
    internal var freezeT = 0f
    internal var multT = 0f
    internal var wideT = 0f
    internal var g = 1
    private var gravT = 8f
    private var gravWarn = false
    internal var windDir = if (spec.level % 2 == 0) 1 else -1
    private var windT = 5f
    internal var bcol = 0
    private var colT = 10f
    private var lightT = 5f
    private var lightning = 0f
    private var portalT = 10f
    private var portalPop = 1f
    private var pxL = 0f
    private var pxR = 0f
    private var spawnCount = 0
    private var surgeT = 0f
    internal var surgeState = 0
    internal var bossPhase = 1
    internal var banner = ""
    internal var bannerT = 0f

    private val bxs = FloatArray(2)
    private var nb = 1

    internal val pauseR = RectF()
    internal val startR = RectF()
    internal val resumeR = RectF()
    internal val homeR = RectF()
    internal val resR = Array(3) { RectF() }
    internal val resAct = IntArray(3)
    internal var resCount = 0
    private var lcx = 0f
    private var rcx = 0f
    private var lcy = 0f
    private var btnR = 0f

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

    // ---------- helpers ----------

    internal fun windMode(): Int = if (spec.wind != 0) spec.wind else if (spec.boss && bossPhase == 2) 1 else 0

    internal fun stonesOn(): Boolean = spec.stones || (spec.boss && bossPhase == 2)

    private fun shrinkOn(): Boolean = spec.shrink || (spec.boss && bossPhase == 2)

    private fun speedMulNow(): Float = if (spec.boss && bossPhase == 1) 1.3f else spec.speedMul

    private fun kOf(w: Float, hh: Float): Float = min(w / 941f, hh / 1672f)

    private fun fade(color: Int): Int = D.mix(color, LC.white, 0.6f)

    private fun say(t: String) {
        banner = t
        bannerT = 1.6f
    }

    private fun bwNow(w: Float): Float {
        var f = 1f
        if (shrinkOn()) f *= max(0.5f, 1f - 0.5f * score / spec.target.toFloat())
        if (wideT > 0f) f *= 1.5f
        return (if (spec.dual != 0) w * 0.17f else w * 0.24f) * f
    }

    private fun rimYOf(hh: Float, bw: Float): Float = hh * 0.862f - bw * 0.86f

    private fun rimNow(hh: Float, bw: Float, k: Float): Float =
        if (g == 1) rimYOf(hh, bw) else insetTop + 258f * k + bw * 0.86f + 8f * k

    private fun centers(w: Float) {
        when (spec.dual) {
            0 -> {
                nb = 1
                bxs[0] = bx * w
            }
            1 -> {
                nb = 2
                bxs[0] = w * (0.13f + bx * 0.34f)
                bxs[1] = w * (0.53f + bx * 0.34f)
            }
            else -> {
                nb = 2
                bxs[0] = w * (0.13f + bx * 0.34f)
                bxs[1] = w * (0.87f - bx * 0.34f)
            }
        }
    }

    private fun nearestBucket(x: Float): Float {
        var best = bxs[0]
        for (i in 1 until nb) if (abs(x - bxs[i]) < abs(x - best)) best = bxs[i]
        return best
    }

    private fun placePortals(w: Float) {
        pxL = w * (0.15f + 0.3f * rnd.nextFloat())
        pxR = w * (0.55f + 0.3f * rnd.nextFloat())
    }

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
        if (flash > 0f) flash = max(0f, flash - dt * 2f)
        if (bannerT > 0f) bannerT -= dt
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val hh = h - insetBottom
        val k = kOf(w, hh)
        if (spec.victory) fireworks.update(dt, w, hh)
        if (spec.portals != 0 && pxL == 0f) placePortals(w)

        if (phase == 0) {
            if (!needStart) {
                introT -= dt
                if (introT <= 0f) phase = 1
            }
            return
        }
        if (paused) return

        if (wob > 0f) wob = max(0f, wob - dt * 3f)
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

        // timers of the effects
        if (freezeT > 0f) freezeT = max(0f, freezeT - dt)
        if (multT > 0f) multT = max(0f, multT - dt)
        if (wideT > 0f) wideT = max(0f, wideT - dt)
        val slow = if (freezeT > 0f) 0.35f else 1f

        if (windMode() == 2) {
            windT -= dt
            if (windT <= 0f) {
                windT = 5f
                windDir = -windDir
                say(if (windDir > 0) "Wind: RIGHT" else "Wind: LEFT")
            }
        }
        if (spec.color != 0) {
            colT -= dt
            if (colT <= 0f) {
                colT = 10f
                bcol = (bcol + 1) % 3
                say("Match: " + colorNames[bcol])
            }
        }
        if (spec.fog == 2) {
            lightT -= dt
            if (lightT <= 0f) {
                lightT = 6f
                lightning = 1.2f
            }
            if (lightning > 0f) lightning = max(0f, lightning - dt)
        }
        if (spec.portals == 2) {
            portalT -= dt
            if (portalT <= 0f) {
                portalT = 10f
                placePortals(w)
                portalPop = 0f
                say("Portals moved!")
            }
        }
        if (portalPop < 1f) portalPop = min(1f, portalPop + dt * 2.5f)
        if (spec.gravity == 1 || spec.gravity == 2) {
            gravT -= dt
            if (!gravWarn && gravT <= 1f) {
                gravWarn = true
                say("Gravity flip!")
            }
            if (gravT <= 0f) {
                g = -g
                gravWarn = false
                gravT = if (spec.gravity == 2) 5f else (if (g == -1) 4f else 8f)
                wob = 1f
            }
        }
        if (spec.surge) {
            surgeT += dt
            val cyc = surgeT % 15f
            val ns = if (cyc < 6f) 0 else if (cyc < 10f) 1 else 2
            if (ns != surgeState) {
                surgeState = ns
                if (ns == 1) say("WAVE!") else if (ns == 2) say("Break")
            }
        }

        // the bucket
        val bw = bwNow(w)
        val half = bw * 0.55f / w
        val lo = if (spec.dual != 0) 0f else half
        val hi = if (spec.dual != 0) 1f else 1f - half
        if (spec.slip) {
            val target = if (dirL && !dirR) -1f else if (dirR && !dirL) 1f else 0f
            val want = if (target != 0f) target * 1.2f else if (dragX >= 0f) ((dragX - bx) * 4f).coerceIn(-1.4f, 1.4f) else 0f
            bv += (want - bv) * min(1f, dt * 2f)
            bx += bv * dt
            if (bx < lo) {
                bx = lo
                bv = 0f
            }
            if (bx > hi) {
                bx = hi
                bv = 0f
            }
        } else {
            if (dirL && !dirR) {
                bx -= 1.2f * dt
            } else if (dirR && !dirL) {
                bx += 1.2f * dt
            } else if (dragX >= 0f) {
                bx += (dragX - bx) * min(1f, dt * 14f)
            }
            bx = bx.coerceIn(lo, hi)
        }
        centers(w)
        val ur = w * 0.095f
        val rim = rimNow(hh, bw, k)

        // new umbrellas and stones
        val sg = if (spec.surge) surgeState else 0
        if (sg != 2) {
            spawnT -= dt * slow * (if (sg == 1) 3.3f else 1f)
            if (spawnT <= 0f) {
                spawnT = spec.gap * (0.75f + rnd.nextFloat() * 0.5f)
                spawnUmbrella(w, hh, ur)
            }
        }
        if (stonesOn()) {
            stoneT -= dt * slow
            if (stoneT <= 0f) {
                stoneT = spec.stoneGap * (0.75f + rnd.nextFloat() * 0.5f)
                spawnStone(w, hh, ur)
            }
        }

        // moving everything
        val bandTop = hh * 0.30f
        val bandBot = hh * 0.55f
        val wm = windMode()
        val pr = w * 0.075f
        val portalY = insetTop + 330f * k
        val iter = drops.iterator()
        while (iter.hasNext()) {
            val d = iter.next()
            val dirD = if (d.kind == K_STONE && spec.gravity == 3) -g else g
            var sgn = 1f
            if (d.bt > 0f) {
                d.bt -= dt
                sgn = -0.9f
            }
            d.y += dirD * d.vy * slow * sgn * dt
            d.x += d.vx * dt
            d.vx *= (1f - min(1f, 1.8f * dt))
            if (wm != 0 && d.y > bandTop && d.y < bandBot) d.x += windDir * w * 0.17f * dt

            val relm = (d.y - rim) * g
            if (d.kind == K_MAGNET || (d.kind == K_STONE && spec.magnetStones)) {
                if (relm > -hh * 0.24f && relm < 0f) {
                    val tgt = nearestBucket(d.x)
                    if (abs(d.x - tgt) < w * 0.34f) {
                        d.x += (tgt - d.x) * min(1f, dt * (if (d.kind == K_STONE) 2.2f else 4f))
                    }
                }
            }
            if (d.kind == K_REPEL) {
                if (relm > -hh * 0.22f && relm < bw * 0.5f) {
                    val tgt = nearestBucket(d.x)
                    if (abs(d.x - tgt) < w * 0.24f) {
                        d.x += (if (d.x >= tgt) 1f else -1f) * w * 0.9f * dt
                    }
                }
            }
            if (spec.portals != 0 && !d.tp && abs(d.y - portalY) < ur * 0.6f) {
                if (abs(d.x - pxL) < pr) {
                    d.x = pxR
                    d.tp = true
                } else if (abs(d.x - pxR) < pr) {
                    d.x = pxL
                    d.tp = true
                }
            }
            if (spec.bounce == 2 && d.bt > 0f) {
                for (o in drops) {
                    if (o === d || o.kind == K_STONE || o.bt > 0f) continue
                    if (hypot(o.x - d.x, o.y - d.y) < ur * 1.5f) {
                        o.vx = (if (o.x >= d.x) 1f else -1f) * w * 0.55f
                    }
                }
            }

            val dx = d.x + sin(playT * 2f + d.phase) * 0.012f * w
            val rel = (d.y - rim) * g
            if (d.bt <= 0f && rel > -ur * 0.15f && rel < bw * 0.35f) {
                var bi = -1
                for (i in 0 until nb) {
                    if (abs(dx - bxs[i]) < bw * 0.46f) {
                        bi = i
                        break
                    }
                }
                if (bi >= 0) {
                    if (d.kind == K_ELASTIC && !d.bounced && abs(dx - bxs[bi]) > bw * 0.27f) {
                        d.bounced = true
                        d.bt = 0.55f
                        d.vx = (if (dx >= bxs[bi]) 1f else -1f) * w * 0.35f
                    } else {
                        iter.remove()
                        if (d.kind == K_STONE) {
                            hitStone(d, dx, rim, ur)
                        } else {
                            catchDrop(d, dx, bi, rim, ur)
                        }
                        if (phase == 2) return
                        continue
                    }
                }
            }
            if (d.y > hh + ur * 4f || d.y < -ur * 4f) iter.remove()
        }
    }

    private fun spawnUmbrella(w: Float, hh: Float, ur: Float) {
        spawnCount++
        var kind = K_UMB
        var col = D.UMB[rnd.nextInt(D.UMB.size)]
        val sp = spec.special
        val r = rnd.nextFloat()
        if (spec.gold >= 1) {
            kind = K_GOLD
            col = 0xFFFFC21A.toInt()
            if (spec.gold == 2 && r < 0.3f) {
                kind = K_BONUS
                col = D.UMB[4]
            }
        } else if (sp >= 0) {
            if (r < 0.3f) {
                kind = K_BONUS
                col = D.UMB[sp]
            } else {
                var ci = rnd.nextInt(D.UMB.size - 1)
                if (ci >= sp) ci++
                col = D.UMB[ci]
            }
        } else {
            if (spec.color != 0) col = COL3[rnd.nextInt(3)]
            if (spec.freeze && r < 0.12f) {
                kind = K_FREEZE
                col = 0xFF7FD8FF.toInt()
            } else if (spec.magnet && r < 0.22f) {
                kind = K_MAGNET
                col = 0xFFFF4DA6.toInt()
            } else if (spec.repel && r < 0.32f) {
                kind = K_REPEL
                col = 0xFF5B6275.toInt()
            } else if (spec.big && r < 0.22f) {
                kind = K_BIG
            } else if (spec.bounce != 0 && r < 0.45f) {
                kind = K_ELASTIC
            } else if (spec.mult && spawnCount % 4 == 0) {
                kind = K_MULT
                col = 0xFFB26DFF.toInt()
            }
        }
        var x = w * (0.12f + rnd.nextFloat() * 0.76f)
        if (spec.portals != 0 && rnd.nextFloat() < 0.6f) {
            x = (if (rnd.nextBoolean()) pxL else pxR) + (rnd.nextFloat() - 0.5f) * w * 0.07f
        }
        val vy = spec.speed * speedMulNow() * hh * (0.88f + rnd.nextFloat() * 0.24f)
        val y = if (g == 1) -ur * 1.5f else hh + ur * 1.5f
        drops.add(LDrop(x, y, vy, col, rnd.nextFloat() * 6.28f, kind))
    }

    private fun spawnStone(w: Float, hh: Float, ur: Float) {
        var x = w * (0.12f + rnd.nextFloat() * 0.76f)
        if (spec.portals != 0 && rnd.nextFloat() < 0.6f) {
            x = (if (rnd.nextBoolean()) pxL else pxR) + (rnd.nextFloat() - 0.5f) * w * 0.07f
        }
        val up = spec.gravity == 3
        if (up) {
            var tries = 0
            while (tries < 6) {
                var near = false
                for (i in 0 until nb) if (abs(x - bxs[i]) < w * 0.2f) near = true
                if (!near) break
                x = w * (0.1f + 0.8f * rnd.nextFloat())
                tries++
            }
        }
        val dirD = if (up) -g else g
        val y = if (dirD == 1) -ur * 1.5f else hh + ur * 1.5f
        val vy = spec.speed * speedMulNow() * hh * 0.78f * (0.92f + rnd.nextFloat() * 0.16f)
        val d = LDrop(x, y, vy, 0, rnd.nextFloat() * 6.28f, K_STONE)
        d.hue = rnd.nextInt(3)
        drops.add(d)
    }

    private fun catchDrop(d: LDrop, dx: Float, bi: Int, rim: Float, ur: Float) {
        var pts = 10
        when (d.kind) {
            K_MAGNET -> pts = 30
            K_GOLD -> pts = goldPts
            K_REPEL -> pts = -10
        }
        if (spec.color != 0 && (d.kind == K_UMB || d.kind == K_ELASTIC)) {
            val match = d.color == COL3[bcol]
            if (spec.color == 2) {
                if (!match) pts = 0
            } else if (match) {
                pts = 20
            }
        }
        if (pts > 0 && multT > 0f) pts *= 2
        score = max(0, score + pts)
        if (score > best) {
            best = score
            Store.setStageBest(spec.level, spec.stage, best)
        }
        val gy = if (g == 1) d.y else 2f * rim - d.y
        val col = if (spec.special >= 0 && d.kind == K_UMB) fade(d.color) else d.color
        landed.add(LLanded(dx, gy, col, 0.28f, bi))
        val py = rim - g * ur
        val txt = if (pts > 0) "+$pts" else if (pts < 0) "$pts" else "x"
        pops.add(LPop(dx, py, 1f, txt, if (pts < 0) LC.red else LC.yellow))
        if (pts < 0) Sfx.hit() else Sfx.catch()
        wob = 1f
        when (d.kind) {
            K_BONUS -> {
                timeLeft = min(Levels.MAX_TIME, timeLeft + Levels.BONUS_TIME)
                pops.add(LPop(dx, py - ur * 1.4f, 1.3f, "+3s", LC.green))
                Sfx.bonus()
            }
            K_BIG -> {
                wideT = 7f
                pops.add(LPop(dx, py - ur * 1.4f, 1.3f, "WIDE!", LC.green))
                Sfx.bonus()
            }
            K_FREEZE -> {
                freezeT = 5f
                pops.add(LPop(dx, py - ur * 1.4f, 1.3f, "FREEZE!", 0xFF8FE3FF.toInt()))
                Sfx.bonus()
            }
            K_MULT -> {
                multT = 8f
                pops.add(LPop(dx, py - ur * 1.4f, 1.3f, "x2!", 0xFFD9A8FF.toInt()))
                Sfx.bonus()
            }
            K_MAGNET -> Sfx.bonus()
        }
        if (spec.boss && bossPhase == 1 && score >= spec.target / 2) {
            bossPhase = 2
            say("Phase 2!")
        }
        if (score >= spec.target) finish(true, "")
    }

    private fun hitStone(d: LDrop, dx: Float, rim: Float, ur: Float) {
        var hp = 1
        var tm = Levels.STONE_TIME
        if (spec.color == 3 && d.hue != bcol) {
            hp = 2
            tm = 10f
        }
        flash = 1f
        wob = 1f
        pops.add(LPop(dx, rim - g * ur, 1.3f, "-" + tm.toInt() + "s", LC.red))
        Sfx.hit()
        if (spec.heartSaver) {
            hearts = 0
            finish(false, "A stone hit!")
            return
        }
        hearts -= hp
        timeLeft -= tm
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
                if (!needStart) {
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

    private fun drawWind(c: Canvas, w: Float, hh: Float, k: Float) {
        val top = hh * 0.30f
        val bot = hh * 0.55f
        val p = D.p
        p.shader = null
        p.style = Paint.Style.FILL
        p.color = 0x16FFFFFF
        c.drawRect(0f, top, w, bot, p)
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = 5f * k
        p.color = 0x66FFFFFF
        for (i in 0 until 6) {
            val y = top + (i + 0.5f) * (bot - top) / 6f
            var fx = (animT * 0.4f * windDir + i * 0.31f) % 1f
            if (fx < 0f) fx += 1f
            val x = fx * (w + 200f * k) - 100f * k
            c.drawLine(x - windDir * 110f * k, y, x, y, p)
        }
        p.style = Paint.Style.FILL
        val cy = (top + bot) / 2f
        c.save()
        if (windDir < 0) c.scale(-1f, 1f, w / 2f, cy)
        D.bigArrow(c, w / 2f, cy, 110f * k, 0x44FFFFFF)
        c.restore()
    }

    private fun drawDrop(c: Canvas, d: LDrop, dx: Float, ur: Float, k: Float, hh: Float, rim: Float) {
        var a = 255
        if (spec.fade != 0 && d.kind != K_STONE) {
            val f = ((d.y - hh * 0.22f) / (hh * 0.42f)).coerceIn(0f, 1f)
            a = (255f * (1f - 0.92f * f)).toInt()
        }
        if (spec.fade == 2 && d.kind == K_STONE) {
            val f = (1f - abs(d.y - rim) / (hh * 0.2f)).coerceIn(0f, 1f)
            a = (255f * f).toInt()
        }
        if (a < 8) return
        if (a < 255) c.saveLayerAlpha(dx - ur * 2.2f, d.y - ur * 2.6f, dx + ur * 2.2f, d.y + ur * 2.4f, a)
        val gy = d.y - ur * 0.3f
        when (d.kind) {
            K_STONE -> {
                D2.stone(c, dx, d.y, ur * 0.85f)
                if (spec.color == 3) D.ring(c, dx, d.y, ur * 0.95f, COL3[d.hue], 6f * k)
                if (spec.goldStones) {
                    D.circle(c, dx, d.y, ur * 0.8f, 0x66FFC21A)
                    D.ring(c, dx, d.y, ur * 0.9f, 0xFFFFC21A.toInt(), 5f * k)
                }
            }
            K_BONUS -> {
                D.circle(c, dx, gy, ur * 1.15f, 0x33FFFFFF)
                D.ring(c, dx, gy, ur * (1.2f + 0.08f * sin(animT * 6f)), 0xCCFFFFFF.toInt(), 5f * k)
                D.umbrella(c, dx, d.y, ur, d.color)
            }
            K_MAGNET -> {
                D.circle(c, dx, gy, ur * 1.15f, 0x33FF4DA6)
                D.umbrella(c, dx, d.y, ur, d.color)
                Fx.magnet(c, dx, d.y - ur * 1.35f, ur * 0.55f)
            }
            K_REPEL -> {
                D.umbrella(c, dx, d.y, ur, d.color)
                D2.cross(c, dx, d.y - ur * 0.42f, ur * 0.22f, LC.white, 5f * k)
            }
            K_BIG -> {
                D.circle(c, dx, d.y - ur * 0.5f, ur * 1.7f, 0x22FFFFFF)
                D.umbrella(c, dx, d.y, ur * 1.5f, d.color)
            }
            K_FREEZE -> {
                D.circle(c, dx, gy, ur * 1.15f, 0x3380D8FF)
                D.umbrella(c, dx, d.y, ur, d.color)
                D2.clock(c, dx, d.y - ur * 0.5f, ur * 0.32f, LC.white)
            }
            K_GOLD -> {
                D.circle(c, dx, gy, ur * 1.2f + 3f * k * sin(animT * 6f + d.phase), 0x33FFD43B)
                D.umbrella(c, dx, d.y, ur, d.color)
                D.star(c, dx + ur * 0.7f, d.y - ur * 1.0f, ur * 0.22f, 0xFFFFF3B0.toInt())
            }
            K_MULT -> {
                D.umbrella(c, dx, d.y, ur, d.color)
                D.text(c, "x2", dx, d.y - ur * 0.28f, ur * 0.75f, LC.white, Paint.Align.CENTER, LC.navy, 4f * k)
            }
            K_ELASTIC -> {
                D.umbrella(c, dx, d.y, ur, d.color)
                D.ring(c, dx, gy, ur * 1.1f, 0x88FFFFFF.toInt(), 4f * k)
            }
            else -> {
                if (spec.special >= 0) {
                    D.umbrella(c, dx, d.y, ur, fade(d.color))
                } else {
                    if (spec.victory) {
                        D.circle(c, dx, gy, ur * 1.25f + 4f * k * sin(animT * 5f + d.phase), D.withA(d.color, 70))
                    }
                    D.umbrella(c, dx, d.y, ur, d.color)
                }
            }
        }
        if (a < 255) c.restore()
    }

    private fun drawBuckets(c: Canvas, bw: Float, rim: Float) {
        val bh = bw * 0.86f
        val ur = width * 0.095f
        for (i in 0 until nb) {
            val bxp = bxs[i]
            c.save()
            if (g == -1) c.scale(1f, -1f, 0f, rim)
            c.scale(1f + wob * 0.08f, 1f - wob * 0.06f, bxp, rim + bh)
            D.bucketBack(c, bxp, rim, bw)
            for (ld in landed) {
                if (ld.bi != i) continue
                val pr = 1f - ld.life / 0.28f
                val px = ld.x + (bxp - ld.x) * pr
                val py = ld.y + (rim + bh * 0.35f - ld.y) * pr
                val sc = 1f - 0.5f * pr
                c.save()
                c.translate(px, py)
                c.scale(sc, sc)
                D.umbrella(c, 0f, 0f, ur, ld.color)
                c.restore()
            }
            D.bucketFront(c, bxp, rim, bw, bh)
            if (spec.color != 0) {
                D.fillRR(c, bxp - bw * 0.5f, rim - bw * 0.04f, bxp + bw * 0.5f, rim + bh, bw * 0.16f, D.withA(COL3[bcol], 150))
            }
            if (flash > 0f) {
                D.fillRR(
                    c, bxp - bw * 0.5f, rim - bw * 0.08f, bxp + bw * 0.5f, rim + bh, bw * 0.18f,
                    D.withA(0xFFFF2020.toInt(), (flash * 120f).toInt())
                )
            }
            c.restore()
        }
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val hh = h - insetBottom
        val k = kOf(w, hh)
        val top = insetTop.toFloat()
        val p = D.p
        val ur = w * 0.095f
        val bw = bwNow(w)
        val bh = bw * 0.86f
        centers(w)
        val rim = rimNow(hh, bw, k)

        Fx.sky(c, w, h, hh, k, animT, theme)
        if (spec.victory) fireworks.draw(c, k)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = 3f * k
        p.color = if (theme >= 5) 0x22FFFFFF else 0x66FFFFFF
        for (s in rain) {
            val sx = s.x * w
            val sy = s.y * hh
            c.drawLine(sx, sy, sx - s.len * hh * 0.15f, sy + s.len * hh, p)
        }
        p.style = Paint.Style.FILL
        if (windMode() != 0) drawWind(c, w, hh, k)
        if (spec.portals != 0 && pxL > 0f) {
            val py = top + 330f * k
            Fx.portal(c, pxL, py, w * 0.075f, w * 0.11f, 0xFF3B8CFF.toInt(), animT, portalPop)
            Fx.portal(c, pxR, py, w * 0.075f, w * 0.11f, 0xFFFF8A3B.toInt(), animT, portalPop)
        }
        D.gameGround(c, w, h, hh, k)
        Fx.nightTint(c, w, h, hh, theme)

        for (d in drops) {
            val dx = d.x + sin(playT * 2f + d.phase) * 0.012f * w
            drawDrop(c, d, dx, ur, k, hh, rim)
        }
        drawBuckets(c, bw, rim)

        if (spec.fog != 0 && lightning <= 0f) {
            var fx = 0f
            for (i in 0 until nb) fx += bxs[i]
            fx /= nb
            val fr = (if (spec.fog == 2) 0.22f else 0.34f) * hh * (if (nb > 1) 1.35f else 1f)
            Fx.fog(c, w, h, fx, rim + bh * 0.1f, fr, 0xEE070B1F.toInt())
        }
        for (pp in pops) {
            val a = (pp.life.coerceIn(0f, 1f) * 255f).toInt()
            D.text(c, pp.text, pp.x, pp.y, 46f * k, D.withA(pp.color, a), Paint.Align.CENTER, D.withA(LC.navy, a), 6f * k)
        }

        // the big round buttons
        btnR = 88f * k
        lcx = w * 0.135f
        rcx = w * 0.865f
        lcy = hh * 0.915f
        D.arrowBtn(c, lcx, lcy, btnR, true, dirL, k)
        D.arrowBtn(c, rcx, lcy, btnR, false, dirR, k)

        if (freezeT > 0f) {
            p.shader = null
            p.style = Paint.Style.FILL
            p.color = 0x2260C8FF
            c.drawRect(0f, 0f, w, h, p)
        }
        if (lightning > 0.85f) {
            val la = ((lightning - 0.85f) / 0.35f * 170f).toInt().coerceIn(0, 200)
            p.shader = null
            p.style = Paint.Style.FILL
            p.color = D.withA(LC.white, la)
            c.drawRect(0f, 0f, w, h, p)
            Fx.bolt(c, w * 0.4f, hh, k, la)
        }

        drawBanner(c, w, hh, k)
        drawHud(c, w, top, k)
        if (phase == 0) {
            if (!needStart) {
                drawIntro(c, w, hh, k)
            } else if (spec.special >= 0) {
                drawDemo(c, w, h, hh, k)
            } else {
                drawTip(c, w, h, hh, k)
            }
        }
        if (phase == 2) drawResult(c, w, h, hh, k)
        if (paused) drawPause(c, w, h, hh, k)
    }
}

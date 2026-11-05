package com.akay.umbrellacatch

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private val demoClip = Path()

internal fun fmtTime(t: Float): String {
    val s = ceil(t).toInt().coerceAtLeast(0)
    return String.format(Locale.US, "%d:%02d", s / 60, s % 60)
}

private fun hudPill(c: Canvas, l: Float, r: Float, hy: Float, k: Float, icon: Int, iconCol: Int, label: String, value: String) {
    D.hudPill(c, l, hy - 42f * k, r, hy + 42f * k, k)
    D.icon(c, icon, l + 46f * k, hy, 56f * k, iconCol)
    D.text(c, label, l + 86f * k, hy - 6f * k, 26f * k, LC.white, Paint.Align.LEFT)
    D.text(c, value, l + 86f * k, hy + 32f * k, 42f * k, LC.yellow, Paint.Align.LEFT, LC.navy, 5f * k)
}

internal fun wrapLines(text: String, size: Float, maxW: Float): List<String> {
    val p = D.p
    p.shader = null
    p.typeface = D.BLACK
    p.textSize = size
    val out = ArrayList<String>()
    var cur = ""
    for (wd in text.split(" ")) {
        val t = if (cur.isEmpty()) wd else "$cur $wd"
        if (cur.isEmpty() || p.measureText(t) <= maxW) {
            cur = t
        } else {
            out.add(cur)
            cur = wd
        }
    }
    if (cur.isNotEmpty()) out.add(cur)
    return out
}

internal fun panelBase(c: Canvas, w: Float, h: Float, pl: Float, pt: Float, pr: Float, pb: Float, k: Float) {
    D.p.shader = null
    D.p.style = Paint.Style.FILL
    D.p.color = 0xAA0A1B3D.toInt()
    c.drawRect(0f, 0f, w, h, D.p)
    D.fillRR(c, pl - 6f * k, pt - 6f * k, pr + 6f * k, pb + 6f * k, 46f * k, LC.white)
    D.gradRR(c, pl, pt, pr, pb, 42f * k, 0xFF3F9BFF.toInt(), 0xFF1450C8.toInt())
}

private fun startButton(c: Canvas, w: Float, pl: Float, pr: Float, by: Float, k: Float, r: android.graphics.RectF) {
    val bl = pl + 60f * k
    val br = pr - 60f * k
    D.gloss(c, bl, by - 48f * k, br, by + 48f * k, 0xFF55E36F.toInt(), 0xFF1D9F39.toInt(), 0xFFFFD84D.toInt(), 0x66105A22, k)
    D.icon(c, K.PLAY, bl + 66f * k, by, 50f * k, LC.white)
    D.text(c, "Start", w / 2f + 28f * k, by + 17f * k, 50f * k, LC.white, Paint.Align.CENTER, 0xFF0F6B22.toInt(), 7f * k)
    r.set(bl, by - 54f * k, br, by + 54f * k)
}

// ---------- the top bar ----------

internal fun LevelView.drawHud(c: Canvas, w: Float, top: Float, k: Float) {
    val hy = top + 58f * k
    hudPill(c, w * 0.02f, w * 0.295f, hy, k, K.TROPHY, LC.gold, "Score", score.toString())
    hudPill(c, w * 0.335f, w * 0.60f, hy, k, K.TARGET, 0xFFFF5A5A.toInt(), "Target", spec.target.toString())
    hudPill(c, w * 0.64f, w * 0.865f, hy, k, K.CROWN, LC.gold, "Best", best.toString())

    val pcx = w * 0.935f
    val pr = 46f * k
    D.hudCircle(c, pcx, hy, pr, k)
    D.icon(c, K.PAUSE, pcx, hy, 40f * k, LC.white)
    pauseR.set(pcx - pr - 14f * k, hy - pr - 14f * k, pcx + pr + 14f * k, hy + pr + 14f * k)

    if (!spec.timed) return
    val y2 = hy + 92f * k
    val hf = 38f * k

    // time
    val tl = w * 0.02f
    val tr = w * 0.30f
    D.hudPill(c, tl, y2 - hf, tr, y2 + hf, k)
    D2.clock(c, tl + 46f * k, y2, 24f * k, LC.gold)
    val low = timeLeft <= 10f
    val tc = if (low && ((animT * 4f).toInt() % 2 == 0)) 0xFFFF6B6B.toInt() else LC.yellow
    D.text(c, fmtTime(timeLeft), tl + 86f * k, y2 + 15f * k, 42f * k, tc, Paint.Align.LEFT, LC.navy, 5f * k)

    if (stonesOn()) {
        val l2 = w * 0.335f
        val r2 = w * 0.985f
        D.hudPill(c, l2, y2 - hf, r2, y2 + hf, k)
        val n = max(1, spec.hearts)
        for (i in 0 until n) {
            val hx = l2 + (r2 - l2) * (0.09f + i * 0.165f)
            val full = i < hearts
            D2.heart(
                c, hx, y2 + 2f * k, 21f * k,
                if (full) 0xFFFF4D5E.toInt() else 0xFF3A4F7A.toInt(),
                if (full) 0xFFB8162B.toInt() else 0xFF22345A.toInt()
            )
        }
    } else if (spec.special >= 0 || spec.gold == 2) {
        val l2 = w * 0.335f
        val r2 = w * 0.60f
        val bc = if (spec.special >= 0) D.UMB[spec.special] else D.UMB[4]
        D.hudPill(c, l2, y2 - hf, r2, y2 + hf, k)
        D.umbrella(c, l2 + 50f * k, y2 + 4f * k, 26f * k, bc)
        D.text(c, "+3s", l2 + 96f * k, y2 + 15f * k, 40f * k, LC.green, Paint.Align.LEFT, LC.navy, 5f * k)
    }

    // one small status chip
    val y3 = y2 + 84f * k
    var txt = ""
    var col = LC.yellow
    var sw = 0
    if (freezeT > 0f) {
        txt = "FREEZE " + ceil(freezeT).toInt() + "s"
        col = 0xFF8FE3FF.toInt()
    } else if (multT > 0f) {
        txt = "x2  " + ceil(multT).toInt() + "s"
        col = 0xFFD9A8FF.toInt()
    } else if (wideT > 0f) {
        txt = "WIDE " + ceil(wideT).toInt() + "s"
        col = LC.green
    } else if (spec.color != 0) {
        txt = "MATCH " + colorNames[bcol]
        sw = COL3[bcol]
    } else if (spec.boss) {
        txt = "Phase $bossPhase"
    } else if (spec.surge) {
        txt = if (surgeState == 1) "WAVE!" else if (surgeState == 2) "BREAK" else "Wave soon"
    } else if (windMode() == 2) {
        txt = "Wind: " + (if (windDir > 0) "RIGHT" else "LEFT")
    } else if (spec.gravity == 1 || spec.gravity == 2) {
        txt = if (g == 1) "Gravity: DOWN" else "Gravity: UP"
    }
    if (txt.isNotEmpty()) {
        val p = D.p
        p.shader = null
        p.typeface = D.BLACK
        p.textSize = 30f * k
        val tw = p.measureText(txt)
        val l = w * 0.02f
        val extra = if (sw != 0) 44f * k else 0f
        val r = l + tw + 60f * k + extra
        D.hudPill(c, l, y3 - 30f * k, r, y3 + 30f * k, k)
        var tx = l + 30f * k
        if (sw != 0) {
            D.circle(c, tx + 12f * k, y3, 13f * k, sw)
            D.ring(c, tx + 12f * k, y3, 13f * k, LC.white, 3f * k)
            tx += 44f * k
        }
        D.text(c, txt, tx, y3 + 11f * k, 30f * k, col, Paint.Align.LEFT, LC.navy, 5f * k)
    }
}

internal fun LevelView.drawBanner(c: Canvas, w: Float, hh: Float, k: Float) {
    if (bannerT <= 0f || banner.isEmpty()) return
    val a = (min(1f, bannerT / 0.4f) * 255f).toInt().coerceIn(0, 255)
    val sz = min(80f * k, D.fit(banner, w * 0.8f))
    D.text(c, banner, w / 2f, hh * 0.36f, sz, D.withA(LC.yellow, a), Paint.Align.CENTER, D.withA(LC.navy, a), 10f * k)
}

// ---------- intro of levels 1 to 15 (A and C) ----------

private fun LevelView.drawReach(c: Canvas, cx: Float, y: Float, k: Float, a: Int, num: String) {
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
    D.text(c, "Reach ", x, y, sz * s, D.withA(LC.white, a), Paint.Align.LEFT)
    x += w1 * s
    D.text(c, num, x, y, big * s, D.withA(LC.yellow, a), Paint.Align.LEFT, D.withA(LC.navy, a), 6f * k * s)
    x += w2 * s
    D.text(c, suffix, x, y, sz * s, D.withA(LC.white, a), Paint.Align.LEFT)
}

internal fun LevelView.drawIntro(c: Canvas, w: Float, hh: Float, k: Float) {
    val a = (min(1f, introT / 0.5f) * 255f).toInt().coerceIn(0, 255)
    val cx = w / 2f
    val cy = hh * 0.42f
    val label = if (Levels.hasStages(spec.level)) {
        "Level ${spec.level} \u2022 Stage ${Levels.letter(spec.stage)}"
    } else {
        "Level ${spec.level}"
    }
    D.text(c, label, cx, cy - 165f * k, 40f * k, D.withA(LC.white, a), Paint.Align.CENTER, D.withA(LC.navy, a), 7f * k)
    D.text(c, "Catch", cx, cy - 40f * k, 100f * k, D.withA(LC.white, a), Paint.Align.CENTER, D.withA(0xFF1F5FBF.toInt(), a), 14f * k)
    val s2 = min(120f * k, D.fit("Umbrellas!", 560f * k))
    D.text(c, "Umbrellas!", cx, cy + 70f * k, s2, D.withA(0xFFFFD42A.toInt(), a), Paint.Align.CENTER, D.withA(0xFF2A2A6A.toInt(), a), 14f * k)

    val ph = if (spec.stones) 200f else 150f
    D.fillRR(c, cx - 330f * k, cy + 105f * k, cx + 330f * k, cy + (105f + ph) * k, 46f * k, D.withA(0xFF7A1E6E.toInt(), (a * 0.92f).toInt()))
    drawReach(c, cx, cy + 168f * k, k, a, spec.target.toString())
    val line2 = if (spec.timed) "in ${spec.time.toInt()} seconds!" else "to Complete the Game!"
    D.text(c, line2, cx, cy + 217f * k, min(34f * k, D.fit(line2, 560f * k)), D.withA(LC.white, a))
    if (spec.stones) {
        val l3 = "Stone: -1 heart and -5 sec!"
        D.text(c, l3, cx, cy + 262f * k, min(30f * k, D.fit(l3, 560f * k)), D.withA(LC.yellow, a))
    }
}

// ---------- tip panel of levels 16 to 30 ----------

internal fun LevelView.drawTip(c: Canvas, w: Float, h: Float, hh: Float, k: Float) {
    val pl = w * 0.07f
    val pr = w * 0.93f
    val pt = hh * 0.08f
    val pb = pt + 1020f * k
    panelBase(c, w, h, pl, pt, pr, pb, k)
    val cx = w / 2f
    val inner = (pr - pl) - 70f * k

    val lab = "Level ${spec.level} \u2022 Stage ${Levels.letter(spec.stage)}"
    D.text(c, lab, cx, pt + 90f * k, min(44f * k, D.fit("Level 30 \u2022 Stage C", inner)), LC.white, Paint.Align.CENTER, LC.navy, 8f * k)
    D.text(c, spec.name, cx, pt + 178f * k, min(72f * k, D.fit(spec.name, inner)), 0xFFFFD42A.toInt(), Paint.Align.CENTER, 0xFF8A3E08.toInt(), 10f * k)

    // picture
    val dl = pl + 60f * k
    val dr = pr - 60f * k
    val dt0 = pt + 225f * k
    val db = pt + 505f * k
    D.gradRR(c, dl, dt0, dr, db, 30f * k, 0xFF57B6F2.toInt(), 0xFF2A6FD6.toInt())
    D.strokeRR(c, dl, dt0, dr, db, 30f * k, 0xAAFFFFFF.toInt(), 4f * k)
    Fx.mech(c, spec.icon, cx, (dt0 + db) / 2f, 200f * k * (1f + 0.04f * sin(animT * 4f)), animT)

    // how to play
    val sz = 34f * k
    val lines = wrapLines(spec.hint, sz, inner)
    var y = pt + 575f * k
    for (ln in lines) {
        D.text(c, ln, cx, y, sz, 0xFFEAF4FF.toInt(), Paint.Align.CENTER, LC.navy, 5f * k)
        y += 46f * k
    }

    val goal = if (spec.heartSaver) {
        "Goal: ${spec.target} points \u2022 only 1 heart!"
    } else {
        "Goal: ${spec.target} points in ${spec.time.toInt()}s"
    }
    D.text(c, goal, cx, pb - 160f * k, min(40f * k, D.fit(goal, inner)), LC.yellow, Paint.Align.CENTER, LC.navy, 6f * k)
    startButton(c, w, pl, pr, pb - 80f * k, k, startR)
}

// ---------- demo of stage B (levels 7 to 15) ----------

private fun LevelView.parts(c: Canvas, cx: Float, y: Float, size: Float, maxW: Float, txt: Array<String>, col: IntArray, k: Float) {
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
        D.text(c, txt[i], x, y, size * s, col[i], Paint.Align.LEFT, LC.navy, 6f * k * s)
        x += wd
    }
}

internal fun LevelView.drawDemo(c: Canvas, w: Float, h: Float, hh: Float, k: Float) {
    val pl = w * 0.07f
    val pr = w * 0.93f
    val pt = hh * 0.1f
    val pb = pt + 880f * k
    panelBase(c, w, h, pl, pt, pr, pb, k)
    val cx = w / 2f
    val inner = (pr - pl) - 70f * k
    D.text(c, "Stage B", cx, pt + 105f * k, 78f * k, LC.white, Paint.Align.CENTER, LC.navy, 10f * k)
    D.text(c, "Level ${spec.level}", cx, pt + 158f * k, 32f * k, 0xFFBFE2FF.toInt())
    val colName = Levels.COLOR_NAMES[spec.special]
    parts(
        c, cx, pt + 238f * k, 44f * k, inner,
        arrayOf("Catch the ", colName, " umbrella!"),
        intArrayOf(LC.white, D.UMB[spec.special], LC.white), k
    )
    val l2 = "+3 seconds every time!"
    D.text(c, l2, cx, pt + 292f * k, min(42f * k, D.fit(l2, inner)), LC.green, Paint.Align.CENTER, LC.navy, 6f * k)
    val l3 = "Pale umbrellas give only points."
    D.text(c, l3, cx, pt + 338f * k, min(30f * k, D.fit(l3, inner)), 0xFFD6E8FF.toInt())

    val dl = pl + 40f * k
    val dr = pr - 40f * k
    val dt0 = pt + 372f * k
    val db = pt + 700f * k
    val dw = dr - dl
    demoClip.reset()
    D.rf.set(dl, dt0, dr, db)
    demoClip.addRoundRect(D.rf, 30f * k, 30f * k, Path.Direction.CW)
    c.save()
    c.clipPath(demoClip)
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
        D.umbrella(c, ux, uy, 34f * k, D.mix(D.UMB[(spec.special + 2 + j * 2) % 6], LC.white, 0.6f))
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
        D2.clock(c, cxD - 85f * k, yy - 16f * k, 26f * k, D.withA(LC.gold, a))
        D.text(c, "+3s", cxD + 20f * k, yy, 58f * k, D.withA(LC.green, a), Paint.Align.CENTER, D.withA(LC.navy, a), 7f * k)
    }
    c.restore()
    D.strokeRR(c, dl, dt0, dr, db, 30f * k, 0xAAFFFFFF.toInt(), 4f * k)

    startButton(c, w, pl, pr, pt + 790f * k, k, startR)
}

// ---------- result and pause ----------

private fun LevelView.resBtn(c: Canvas, idx: Int, cy: Float, bl: Float, br: Float, k: Float, label: String, style: Int, icon: Int, act: Int) {
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
    D.icon(c, icon, bl + 66f * k, cy, 50f * k, LC.white)
    D.text(
        c, label, (bl + br) / 2f + 28f * k, cy + 17f * k,
        min(48f * k, D.fit(label, (br - bl) - 220f * k)), LC.white, Paint.Align.CENTER, st, 7f * k
    )
    resR[idx].set(bl, cy - 54f * k, br, cy + 54f * k)
    resAct[idx] = act
}

// a simple result panel (the exact "Level Complete" screen comes in Stage 3)
internal fun LevelView.drawResult(c: Canvas, w: Float, h: Float, hh: Float, k: Float) {
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

    val title = if (!won) "Try Again!"
    else if (spec.level == 30 && spec.stage == 2) "Catch Master!"
    else if (lastStage) "Level Complete!"
    else "Stage ${Levels.letter(spec.stage)} Complete!"
    D.text(
        c, title, cx, pt + 170f * k, min(66f * k, D.fit(title, (pr - pl) - 80f * k)),
        0xFFFFD42A.toInt(), Paint.Align.CENTER, 0xFF8A3E08.toInt(), 10f * k
    )

    val bl = pl + 45f * k
    val br = pr - 45f * k
    val bt = pt + 215f * k
    val bb = pt + 335f * k
    D.fillRR(c, bl - 5f * k, bt - 5f * k, br + 5f * k, bb + 5f * k, 34f * k, 0xFFFFC84A.toInt())
    D.gradRR(c, bl, bt, br, bb, 30f * k, 0xFF1B4C9E.toInt(), 0xFF0B2A66.toInt())
    D.fillRR(c, cx - 2f * k, bt + 18f * k, cx + 2f * k, bb - 18f * k, 2f * k, 0xAAFFC84A.toInt())
    val lc = (bl + cx) / 2f
    val rc = (cx + br) / 2f
    D.text(c, "Your Score", lc, bt + 42f * k, 28f * k, LC.white)
    D.icon(c, K.STAR, lc - 62f * k, bt + 86f * k, 46f * k, LC.gold)
    D.text(c, score.toString(), lc + 24f * k, bt + 100f * k, 52f * k, LC.yellow, Paint.Align.CENTER, LC.navy, 6f * k)
    D.text(c, "Target", rc, bt + 42f * k, 28f * k, LC.white)
    D.icon(c, K.TARGET, rc - 62f * k, bt + 86f * k, 46f * k, 0xFFFF5A5A.toInt())
    D.text(c, spec.target.toString(), rc + 24f * k, bt + 100f * k, 52f * k, LC.yellow, Paint.Align.CENTER, LC.navy, 6f * k)

    if (won) {
        D.text(c, "Amazing!", cx, pt + 398f * k, 50f * k, 0xFF8BFF6B.toInt(), Paint.Align.CENTER, 0xFF1D6B22.toInt(), 8f * k)
    } else {
        D.text(c, reason, cx, pt + 398f * k, min(44f * k, D.fit(reason, (pr - pl) - 80f * k)), 0xFFFFE066.toInt(), Paint.Align.CENTER, LC.navy, 6f * k)
    }

    for (i in 0 until n) {
        val cy = pt + 450f * k + 55f * k + i * 135f * k
        resBtn(c, i, cy, pl + 60f * k, pr - 60f * k, k, labels[i], styles[i], icons[i], acts[i])
    }
}

internal fun LevelView.drawPause(c: Canvas, w: Float, h: Float, hh: Float, k: Float) {
    val pl = w * 0.14f
    val pr = w * 0.86f
    val pt = hh * 0.3f
    val pb = pt + 520f * k
    panelBase(c, w, h, pl, pt, pr, pb, k)
    D.text(c, "Paused", w / 2f, pt + 125f * k, 84f * k, LC.white, Paint.Align.CENTER, 0xFF0D2F7A.toInt(), 10f * k)

    val bl = pl + 60f * k
    val br = pr - 60f * k
    val ry = pt + 230f * k
    D.gloss(c, bl, ry - 55f * k, br, ry + 55f * k, 0xFF55E36F.toInt(), 0xFF1D9F39.toInt(), 0xFFFFD84D.toInt(), 0x66105A22, k)
    D.icon(c, K.PLAY, bl + 70f * k, ry, 56f * k, LC.white)
    D.text(c, "Resume", w / 2f + 30f * k, ry + 18f * k, 52f * k, LC.white, Paint.Align.CENTER, 0xFF0F6B22.toInt(), 7f * k)
    resumeR.set(bl, ry - 55f * k, br, ry + 55f * k)

    val hy = pt + 400f * k
    D.gloss(c, bl, hy - 55f * k, br, hy + 55f * k, 0xFFB56DFF.toInt(), 0xFF6C2BD8.toInt(), 0xFFD9B8FF.toInt(), 0x66331166, k)
    D.icon(c, K.GRID, bl + 70f * k, hy, 52f * k, LC.white)
    D.text(c, "Levels", w / 2f + 30f * k, hy + 18f * k, 52f * k, LC.white, Paint.Align.CENTER, 0xFF3A1280.toInt(), 7f * k)
    homeR.set(bl, hy - 55f * k, br, hy + 55f * k)
}

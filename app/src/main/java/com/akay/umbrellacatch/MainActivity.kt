package com.akay.umbrellacatch

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {

    internal val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    internal val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    internal var screen = "home"

    private var game: GameView? = null
    private var scoreTv: TextView? = null
    private var bestTv: TextView? = null

    internal fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    internal fun toast(m: String) {
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
    }

    internal fun lp(w: Int, h: Int, wt: Float = 0f): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(w, h, wt)

    internal fun lpm(w: Int, h: Int, l: Int, t: Int, r: Int, b: Int): LinearLayout.LayoutParams {
        val p = LinearLayout.LayoutParams(if (w > 0) dp(w) else w, if (h > 0) dp(h) else h)
        p.setMargins(dp(l), dp(t), dp(r), dp(b))
        return p
    }

    internal fun rr(color: Int, rDp: Int, stroke: Int = 0, strokeDp: Int = 0): GradientDrawable {
        val d = GradientDrawable()
        d.setColor(color)
        d.cornerRadius = dp(rDp).toFloat()
        if (strokeDp > 0) d.setStroke(dp(strokeDp), stroke)
        return d
    }

    internal fun grad(colors: IntArray, o: GradientDrawable.Orientation, rDp: Int): GradientDrawable {
        val d = GradientDrawable(o, colors)
        d.cornerRadius = dp(rDp).toFloat()
        return d
    }

    internal fun tv(text: String, sp: Float, color: Int, style: Int = 0): TextView {
        val t = TextView(this)
        t.text = text
        t.textSize = sp
        t.setTextColor(color)
        t.typeface = when (style) {
            2 -> Typeface.DEFAULT_BOLD
            1 -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
            else -> Typeface.DEFAULT
        }
        return t
    }

    internal fun gradText(text: String, sp: Float, c1: Int, c2: Int): TextView {
        val t = tv(text, sp, c1, 2)
        t.post {
            val w = t.paint.measureText(t.text.toString())
            t.paint.shader = LinearGradient(0f, 0f, w, 0f, c1, c2, Shader.TileMode.CLAMP)
            t.invalidate()
        }
        return t
    }

    internal fun setScreen(content: View) {
        val wrap = LinearLayout(this)
        wrap.orientation = LinearLayout.VERTICAL
        val tp = View(this)
        val bp = View(this)
        wrap.addView(tp, lp(MATCH, 0))
        wrap.addView(content, lp(MATCH, 0, 1f))
        wrap.addView(bp, lp(MATCH, 0))
        ViewCompat.setOnApplyWindowInsetsListener(wrap) { _, ins ->
            val sb = ins.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            (tp.layoutParams as LinearLayout.LayoutParams).height = sb.top
            (bp.layoutParams as LinearLayout.LayoutParams).height = sb.bottom
            tp.requestLayout()
            bp.requestLayout()
            WindowInsetsCompat.CONSUMED
        }
        setContentView(wrap)
        val c = WindowInsetsControllerCompat(window, wrap)
        c.isAppearanceLightStatusBars = false
        c.isAppearanceLightNavigationBars = false
        ViewCompat.requestApplyInsets(wrap)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        try {
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        } catch (e: Throwable) {
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (screen == "classic") exitToHome() else finish()
            }
        })
        showHome()
    }

    // ---------- shared pieces ----------

    private fun pillIcon(icon: Int, col: Int): FrameLayout {
        val f = FrameLayout(this)
        f.background = rr(Color.WHITE, 20, 0xFFD9E4F7.toInt(), 1)
        f.addView(IconView(this, icon, col), FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER))
        return f
    }

    private fun pill(icon: Int, iconCol: Int, text: String, textCol: Int): LinearLayout {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        r.gravity = Gravity.CENTER_VERTICAL
        r.background = rr(0xDD16305C.toInt(), 20)
        r.setPadding(dp(10), dp(6), dp(16), dp(6))
        val tile = FrameLayout(this)
        val td = GradientDrawable()
        td.shape = GradientDrawable.OVAL
        td.setColor(Color.WHITE)
        tile.background = td
        tile.addView(IconView(this, icon, iconCol), FrameLayout.LayoutParams(dp(18), dp(18), Gravity.CENTER))
        r.addView(tile, lp(dp(32), dp(32)))
        val t = tv(text, 15f, textCol, 2)
        t.setPadding(dp(10), 0, 0, 0)
        r.addView(t)
        return r
    }

    private fun circleBtn(icon: Int): FrameLayout {
        val f = FrameLayout(this)
        f.background = rr(0xDD16305C.toInt(), 24)
        f.addView(IconView(this, icon, Color.WHITE), FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER))
        return f
    }

    // ---------- home screen ----------

    private fun showHome() {
        screen = "home"
        val root = FrameLayout(this)
        root.addView(SkyView(this), FrameLayout.LayoutParams(MATCH, MATCH))
        root.addView(GroundView(this), FrameLayout.LayoutParams(MATCH, dp(160), Gravity.BOTTOM))

        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.setPadding(dp(16), dp(10), dp(16), dp(18))

        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        val plus = pillIcon(K.PLUS, 0xFF2DBF53.toInt())
        plus.setOnClickListener { toast("Coming in Stage 4") }
        val coinsRow = pill(K.STAR, 0xFFFFC233.toInt(), Store.coins().toString(), Color.WHITE)
        coinsRow.addView(plus, lpm(28, 28, 10, 0, 0, 0))
        top.addView(coinsRow)
        val bestRow = pill(K.CROWN, 0xFFFFC233.toInt(), "Best " + Store.best(), Color.WHITE)
        top.addView(bestRow, lpm(WRAP, WRAP, 10, 0, 0, 0))
        top.addView(View(this), lp(0, WRAP, 1f))
        val soundBtn = circleBtn(if (Store.sound()) K.SPEAKER_ON else K.SPEAKER_OFF)
        soundBtn.setOnClickListener {
            Store.setSound(!Store.sound())
            showHome()
        }
        top.addView(soundBtn, lpm(40, 40, 8, 0, 0, 0))
        val musicBtn = circleBtn(K.MUSIC)
        musicBtn.setOnClickListener {
            Store.setMusic(!Store.music())
            toast(if (Store.music()) "Music on" else "Music off")
        }
        top.addView(musicBtn, lpm(40, 40, 8, 0, 0, 0))
        val gearBtn = circleBtn(K.GEAR)
        gearBtn.setOnClickListener { toast("More settings coming soon") }
        top.addView(gearBtn, lpm(40, 40, 8, 0, 0, 0))
        c.addView(top, lp(MATCH, WRAP))

        val title = LinearLayout(this)
        title.orientation = LinearLayout.HORIZONTAL
        title.gravity = Gravity.CENTER
        title.addView(tv("Umbrella ", 30f, Color.WHITE, 2))
        title.addView(gradText("Catch", 30f, 0xFFFFE23B.toInt(), 0xFFFFA23B.toInt()))
        c.addView(title, lpm(MATCH, WRAP, 0, 18, 0, 0))

        val art = FrameLayout(this)
        art.addView(RainbowView(this), FrameLayout.LayoutParams(dp(240), dp(140), Gravity.CENTER_HORIZONTAL or Gravity.TOP))
        art.addView(MascotView(this), FrameLayout.LayoutParams(dp(150), dp(170), Gravity.CENTER))
        c.addView(art, lp(MATCH, 0, 1f))

        val classic = LinearLayout(this)
        classic.orientation = LinearLayout.HORIZONTAL
        classic.gravity = Gravity.CENTER_VERTICAL
        classic.background = grad(intArrayOf(0xFF4CDD6B.toInt(), 0xFF1FA83F.toInt()), GradientDrawable.Orientation.LEFT_RIGHT, 30)
        classic.setPadding(dp(22), 0, dp(18), 0)
        classic.addView(IconView(this, K.PLAY, Color.WHITE), lp(dp(26), dp(26)))
        val ccol = LinearLayout(this)
        ccol.orientation = LinearLayout.VERTICAL
        ccol.setPadding(dp(12), 0, 0, 0)
        ccol.addView(tv("Classic Mode", 19f, Color.WHITE, 2))
        ccol.addView(tv("Catch Umbrellas & Score High!", 12f, 0xE6FFFFFF.toInt()))
        classic.addView(ccol, lp(0, WRAP, 1f))
        classic.addView(IconView(this, K.TROPHY, Color.WHITE), lp(dp(26), dp(26)))
        classic.setOnClickListener { showClassic() }
        c.addView(classic, lpm(MATCH, 62, 0, 18, 0, 0))

        val stage = LinearLayout(this)
        stage.orientation = LinearLayout.HORIZONTAL
        stage.gravity = Gravity.CENTER_VERTICAL
        stage.background = grad(intArrayOf(0xFFB06BFF.toInt(), 0xFF7A2FE0.toInt()), GradientDrawable.Orientation.LEFT_RIGHT, 30)
        stage.setPadding(dp(22), 0, dp(18), 0)
        stage.addView(IconView(this, K.CROWN, Color.WHITE), lp(dp(26), dp(26)))
        val scol = LinearLayout(this)
        scol.orientation = LinearLayout.VERTICAL
        scol.setPadding(dp(12), 0, 0, 0)
        scol.addView(tv("Stage Mode", 19f, Color.WHITE, 2))
        scol.addView(tv("Complete Levels!", 12f, 0xE6FFFFFF.toInt()))
        stage.addView(scol, lp(0, WRAP, 1f))
        stage.addView(IconView(this, K.FWD, Color.WHITE), lp(dp(26), dp(26)))
        stage.setOnClickListener { toast("Coming in Stage 2") }
        c.addView(stage, lpm(MATCH, 62, 0, 12, 0, 0))

        val cards = LinearLayout(this)
        cards.orientation = LinearLayout.HORIZONTAL
        cards.addView(bottomCard(K.GRID, "More Apps") { showMoreApps() }, lp(0, WRAP, 1f))
        val m2 = lp(0, WRAP, 1f)
        m2.leftMargin = dp(10)
        cards.addView(bottomCard(K.SHOP, "Shop") { toast("Coming in Stage 4") }, m2)
        val m3 = lp(0, WRAP, 1f)
        m3.leftMargin = dp(10)
        cards.addView(bottomCard(K.MEDAL, "Achievements") { toast("Coming in Stage 5") }, m3)
        c.addView(cards, lpm(MATCH, WRAP, 0, 14, 0, 0))

        val circles = LinearLayout(this)
        circles.orientation = LinearLayout.HORIZONTAL
        circles.gravity = Gravity.CENTER
        val rate = circleBtn(K.STAR)
        rate.setOnClickListener { rateUs() }
        val share = circleBtn(K.SHARE)
        share.setOnClickListener { shareApp() }
        val about = circleBtn(K.INFO)
        about.setOnClickListener { showAbout() }
        circles.addView(rate, lp(dp(46), dp(46)))
        circles.addView(share, lpm(46, 46, 20, 0, 20, 0))
        circles.addView(about, lp(dp(46), dp(46)))
        c.addView(circles, lpm(MATCH, WRAP, 0, 14, 0, 0))

        root.addView(c, FrameLayout.LayoutParams(MATCH, MATCH))
        setScreen(root)
    }

    private fun bottomCard(icon: Int, title: String, onClick: () -> Unit): View {
        val b = LinearLayout(this)
        b.orientation = LinearLayout.VERTICAL
        b.gravity = Gravity.CENTER
        b.background = rr(0xDD16305C.toInt(), 18, 0xFF2E5B9E.toInt(), 1)
        b.setPadding(dp(8), dp(14), dp(8), dp(14))
        b.addView(IconView(this, icon, Color.WHITE), lp(dp(28), dp(28)))
        val t = tv(title, 12.5f, Color.WHITE, 2)
        t.gravity = Gravity.CENTER
        b.addView(t, lpm(WRAP, WRAP, 0, 6, 0, 0))
        b.setOnClickListener { onClick() }
        return b
    }

    private fun rateUs() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (e: Throwable) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
            } catch (e2: Throwable) {
                toast("Could not open Play Store")
            }
        }
    }

    private fun shareApp() {
        try {
            val i = Intent(Intent.ACTION_SEND)
            i.type = "text/plain"
            i.putExtra(Intent.EXTRA_TEXT, "Try Umbrella Catch! https://play.google.com/store/apps/details?id=$packageName")
            startActivity(Intent.createChooser(i, "Share"))
        } catch (e: Throwable) {
        }
    }

    private fun showAbout() {
        AlertDialog.Builder(this)
            .setTitle("Umbrella Catch  v1.0.0")
            .setMessage("Catch the falling umbrellas and beat your best score!\n\nFree app, no subscription.\nMade by Akay")
            .setPositiveButton("Close", null)
            .show()
    }

    // ---------- classic mode ----------

    private fun showClassic() {
        screen = "classic"
        val root = FrameLayout(this)
        val gv = GameView(this)
        game = gv
        root.addView(gv, FrameLayout.LayoutParams(MATCH, MATCH))

        val hud = LinearLayout(this)
        hud.orientation = LinearLayout.HORIZONTAL
        hud.gravity = Gravity.CENTER_VERTICAL
        hud.setPadding(dp(14), dp(10), dp(14), 0)
        val scoreRow = pill(K.TROPHY, 0xFFFFC233.toInt(), "0", Color.WHITE)
        scoreTv = scoreRow.getChildAt(1) as TextView
        hud.addView(scoreRow)
        val bestRow = pill(K.CROWN, 0xFFFFC233.toInt(), Store.best().toString(), Color.WHITE)
        bestTv = bestRow.getChildAt(1) as TextView
        hud.addView(bestRow, lpm(WRAP, WRAP, 10, 0, 0, 0))
        hud.addView(View(this), lp(0, WRAP, 1f))
        val pauseBtn = circleBtn(K.PAUSE)
        pauseBtn.setOnClickListener { showPause() }
        hud.addView(pauseBtn, lp(dp(44), dp(44)))
        root.addView(hud, FrameLayout.LayoutParams(MATCH, WRAP, Gravity.TOP))

        val leftBtn = arrowBtn(K.ARROW_L)
        val rightBtn = arrowBtn(K.ARROW_R)
        leftBtn.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> gv.setDir(-1)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> gv.setDir(0)
            }
            false
        }
        rightBtn.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> gv.setDir(1)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> gv.setDir(0)
            }
            false
        }
        val lp1 = FrameLayout.LayoutParams(dp(60), dp(60), Gravity.BOTTOM or Gravity.START)
        lp1.setMargins(dp(20), 0, 0, dp(20))
        root.addView(leftBtn, lp1)
        val lp2 = FrameLayout.LayoutParams(dp(60), dp(60), Gravity.BOTTOM or Gravity.END)
        lp2.setMargins(0, 0, dp(20), dp(20))
        root.addView(rightBtn, lp2)

        setScreen(root)

        gv.onScore = { s -> scoreTv?.text = s.toString() }
        gv.onBest = { b -> bestTv?.text = b.toString() }
        gv.post { gv.start() }
    }

    private fun arrowBtn(icon: Int): FrameLayout {
        val f = FrameLayout(this)
        val d = GradientDrawable()
        d.shape = GradientDrawable.OVAL
        d.setColor(0xDD1F6FD6.toInt())
        d.setStroke(dp(2), Color.WHITE)
        f.background = d
        f.addView(IconView(this, icon, Color.WHITE), FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER))
        return f
    }

    private fun showPause() {
        game?.pause()
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.gravity = Gravity.CENTER
        box.setPadding(dp(30), dp(10), dp(30), dp(10))
        box.addView(tv("Paused", 22f, Color.BLACK, 2))
        AlertDialog.Builder(this)
            .setView(box)
            .setCancelable(false)
            .setPositiveButton("Resume") { d, _ ->
                d.dismiss()
                game?.resumeGame()
            }
            .setNegativeButton("Home") { d, _ ->
                d.dismiss()
                exitToHome()
            }
            .show()
    }

    private fun exitToHome() {
        game?.pause()
        game = null
        showHome()
    }
}

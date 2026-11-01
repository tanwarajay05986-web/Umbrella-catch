package com.akay.umbrellacatch

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
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

    // ---------- helpers (also used by MoreUi.kt) ----------

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

    // shows a full-screen view that draws under the status bar and the navigation bar
    private fun setFull(v: View) {
        ViewCompat.setOnApplyWindowInsetsListener(v) { _, ins ->
            val sb = ins.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            if (v is InsetAware) v.setInsets(sb.top, sb.bottom)
            WindowInsetsCompat.CONSUMED
        }
        setContentView(v)
        val c = WindowInsetsControllerCompat(window, v)
        c.isAppearanceLightStatusBars = false
        c.isAppearanceLightNavigationBars = false
        ViewCompat.requestApplyInsets(v)
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        D.init(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        try {
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
            window.isNavigationBarContrastEnforced = false
        } catch (e: Throwable) {
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (screen == "classic") {
                    val g = game
                    if (g == null || !g.backPressed()) exitToHome()
                } else {
                    finish()
                }
            }
        })
        showHome()
    }

    override fun onPause() {
        super.onPause()
        game?.pauseGame()
    }

    // ---------- home ----------

    private fun showHome() {
        screen = "home"
        game = null
        val hv = HomeView(this)
        hv.onAction = { id -> onHomeAction(id) }
        setFull(hv)
    }

    private fun onHomeAction(id: Int) {
        when (id) {
            0 -> toast("Coming in Stage 4")
            2 -> Store.setSound(!Store.sound())
            3 -> {
                Store.setMusic(!Store.music())
                toast(if (Store.music()) "Music on" else "Music off")
            }
            4 -> toast("More settings coming soon")
            5 -> showClassic()
            6 -> toast("Coming in Stage 2")
            7 -> showMoreApps()
            8 -> toast("Coming in Stage 4")
            9 -> toast("Coming in Stage 5")
            10 -> rateUs()
            11 -> shareApp()
            12 -> showAbout()
        }
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
        val gv = GameView(this)
        gv.onHome = { exitToHome() }
        game = gv
        setFull(gv)
    }

    private fun exitToHome() {
        game?.pauseGame()
        showHome()
    }
}

package com.akay.umbrellacatch

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.Gravity
import android.view.Window
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

private const val MORE_LIVE = false

private class MoreApp(val name: String, val pkg: String)

private val MORE_APPS = listOf(
    MoreApp("PDF Scanner", "com.akay.pdfscanner"),
    MoreApp("Video Player", "com.akay.videoplayer"),
    MoreApp("Block", "com.akay.block"),
    MoreApp("CompressPro", "com.akay.compresspro"),
    MoreApp("Blocker", "com.akay.blocker")
)

private fun MainActivity.openMore(app: MoreApp) {
    if (!MORE_LIVE) {
        toast("Coming soon on Play Store")
        return
    }
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + app.pkg)))
    } catch (e: Throwable) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + app.pkg)))
        } catch (e2: Throwable) {
            toast("Could not open Play Store")
        }
    }
}

fun MainActivity.showMoreApps() {
    val dlg = Dialog(this)
    dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
    val c = LinearLayout(this)
    c.orientation = LinearLayout.VERTICAL
    c.setPadding(dp(20), dp(18), dp(20), dp(28))
    val bg = GradientDrawable()
    bg.setColor(Color.WHITE)
    val rad = dp(26).toFloat()
    bg.cornerRadii = floatArrayOf(rad, rad, rad, rad, 0f, 0f, 0f, 0f)
    c.background = bg

    val head = LinearLayout(this)
    head.orientation = LinearLayout.HORIZONTAL
    head.gravity = Gravity.CENTER_VERTICAL
    head.addView(tv("More Apps", 20f, 0xFF18233F.toInt(), 2), lp(0, WRAP, 1f))
    val close: TextView = tv("\u2715", 18f, 0xFF7A88A6.toInt(), 1)
    close.setPadding(dp(10), dp(4), dp(4), dp(4))
    close.setOnClickListener { dlg.dismiss() }
    head.addView(close)
    c.addView(head, lp(MATCH, WRAP))

    for (app in MORE_APPS) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.background = rr(0xFFF2F6FF.toInt(), 16, 0xFFE0E8FA.toInt(), 1)
        row.setPadding(dp(14), dp(14), dp(14), dp(14))
        val tile = FrameLayout(this)
        tile.background = grad(intArrayOf(0xFF4DA6FF.toInt(), 0xFF8A4DFF.toInt()), GradientDrawable.Orientation.TL_BR, 14)
        tile.addView(IconView(this, K.GRID, Color.WHITE), FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER))
        row.addView(tile, lp(dp(44), dp(44)))
        val name = tv(app.name, 16f, 0xFF18233F.toInt(), 2)
        name.setPadding(dp(14), 0, 0, 0)
        row.addView(name, lp(0, WRAP, 1f))
        row.addView(IconView(this, K.ARROW_R, 0xFF7A88A6.toInt()), lp(dp(16), dp(16)))
        row.setOnClickListener { openMore(app) }
        c.addView(row, lpm(MATCH, WRAP, 0, 10, 0, 0))
    }

    dlg.setContentView(c)
    val w = dlg.window
    if (w != null) {
        w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        w.setLayout(MATCH, WRAP)
        w.setGravity(Gravity.BOTTOM)
    }
    dlg.show()
}

package com.akay.umbrellacatch

import android.content.Context
import android.content.SharedPreferences

object Store {
    private var sp: SharedPreferences? = null

    fun init(c: Context) {
        if (sp == null) sp = c.applicationContext.getSharedPreferences("umbrellacatch", Context.MODE_PRIVATE)
    }

    private fun p(): SharedPreferences = sp!!

    fun best(): Int = p().getInt("best", 0)

    fun setBest(v: Int) {
        if (v > best()) p().edit().putInt("best", v).apply()
    }

    fun sound(): Boolean = p().getBoolean("sound", true)

    fun setSound(v: Boolean) {
        p().edit().putBoolean("sound", v).apply()
    }

    fun music(): Boolean = p().getBoolean("music", true)

    fun setMusic(v: Boolean) {
        p().edit().putBoolean("music", v).apply()
    }

    fun coins(): Int = p().getInt("coins", 0)

    fun addCoins(v: Int) {
        p().edit().putInt("coins", coins() + v).apply()
    }

    // ---------- level mode ----------

    // stage 0 = the only stage of levels 1-6, or stage A. 1 = B, 2 = C
    fun stageDone(level: Int, st: Int): Boolean = p().getBoolean("sd_${level}_$st", false)

    fun setStageDone(level: Int, st: Int) {
        p().edit().putBoolean("sd_${level}_$st", true).apply()
    }

    fun levelDone(level: Int): Boolean {
        if (!Levels.hasStages(level)) return stageDone(level, 0)
        return stageDone(level, 0) && stageDone(level, 1) && stageDone(level, 2)
    }

    fun levelOpen(level: Int): Boolean = Levels.TEST_UNLOCK_ALL || level <= 1 || levelDone(level - 1)

    fun stageBest(level: Int, st: Int): Int = p().getInt("sb_${level}_$st", 0)

    fun setStageBest(level: Int, st: Int, v: Int) {
        if (v > stageBest(level, st)) p().edit().putInt("sb_${level}_$st", v).apply()
    }
}

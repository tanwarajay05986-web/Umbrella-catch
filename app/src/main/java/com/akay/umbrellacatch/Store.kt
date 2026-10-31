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
}

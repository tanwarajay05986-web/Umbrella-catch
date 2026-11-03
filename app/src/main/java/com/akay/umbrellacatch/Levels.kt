package com.akay.umbrellacatch

// one level, or one stage (A / B / C) of a level
class Spec(
    val level: Int,
    val stage: Int,
    val target: Int,
    val timed: Boolean,
    val time: Float,
    val gap: Float,
    val speed: Float,
    val special: Int,
    val stones: Boolean,
    val stoneGap: Float,
    val hearts: Int
)

object Levels {
    const val TOTAL = 30
    const val PLAYABLE = 15
    const val NORMAL_MAX = 6
    const val HEARTS = 6
    const val BONUS_TIME = 3f
    const val STONE_TIME = 5f
    const val MAX_TIME = 99f

    // true = every level and every stage is open (only for testing)
    const val TEST_UNLOCK_ALL = false

    val COLOR_NAMES = arrayOf("Red", "Yellow", "Purple", "Green", "Blue", "Pink")

    // the bonus colour of stage B for levels 7 to 15 (index in D.UMB)
    private val SPECIAL = intArrayOf(2, 4, 0, 3, 5, 1, 4, 2, 0)

    fun hasStages(level: Int): Boolean = level > NORMAL_MAX

    fun letter(stage: Int): String = when (stage) {
        0 -> "A"
        1 -> "B"
        else -> "C"
    }

    fun spec(level: Int, stage: Int): Spec {
        if (!hasStages(level)) {
            val n = level - 1
            return Spec(
                level = level, stage = 0, target = 100 + 50 * n, timed = false, time = 0f,
                gap = 1.2f - 0.04f * n, speed = 0.19f + 0.012f * n, special = -1,
                stones = false, stoneGap = 0f, hearts = 0
            )
        }
        val i = (level - 7).coerceIn(0, 8)
        val gap = 0.85f - 0.015f * i
        val speed = 0.26f + 0.01f * i
        return when (stage) {
            0 -> Spec(
                level = level, stage = 0, target = 250 + 25 * i, timed = true, time = 50f + 3f * i,
                gap = gap, speed = speed, special = -1, stones = false, stoneGap = 0f, hearts = 0
            )
            1 -> Spec(
                level = level, stage = 1, target = 200 + 20 * i, timed = true, time = 25f,
                gap = gap, speed = speed, special = SPECIAL[i], stones = false, stoneGap = 0f, hearts = 0
            )
            else -> Spec(
                level = level, stage = 2, target = 230 + 20 * i, timed = true, time = 55f + 3f * i,
                gap = gap, speed = speed, special = -1, stones = true, stoneGap = 3.4f - 0.12f * i,
                hearts = HEARTS
            )
        }
    }

    // what to play after this level / stage (null = nothing more yet)
    fun next(level: Int, stage: Int): IntArray? {
        if (hasStages(level) && stage < 2) return intArrayOf(level, stage + 1)
        val nl = level + 1
        return if (nl <= PLAYABLE) intArrayOf(nl, 0) else null
    }

    fun describe(level: Int, stage: Int): String {
        val s = spec(level, stage)
        return when (stage) {
            0 -> "Reach ${s.target} points in ${s.time.toInt()}s"
            1 -> "Catch ${COLOR_NAMES[s.special]} umbrellas for +3s \u2022 ${s.target} pts"
            else -> "Avoid stones! Reach ${s.target} points in ${s.time.toInt()}s"
        }
    }
}

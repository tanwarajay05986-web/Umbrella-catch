package com.akay.umbrellacatch

// one level, or one stage (A / B / C) of a level
class Spec(val level: Int, val stage: Int) {
    var target = 100
    var timed = false
    var time = 0f
    var gap = 1f
    var speed = 0.2f
    var speedMul = 1f
    var special = -1
    var stones = false
    var stoneGap = 3f
    var hearts = 0
    var wind = 0
    var magnet = false
    var magnetStones = false
    var repel = false
    var shrink = false
    var big = false
    var fog = 0
    var freeze = false
    var portals = 0
    var color = 0
    var dual = 0
    var bounce = 0
    var gravity = 0
    var slip = false
    var mult = false
    var fade = 0
    var gold = 0
    var goldStones = false
    var surge = false
    var boss = false
    var victory = false
    var heartSaver = false
    var icon = 0
    var name = ""
    var hint = ""
}

object Levels {
    const val TOTAL = 30
    const val PLAYABLE = 30
    const val NORMAL_MAX = 6
    const val HEARTS = 6
    const val BONUS_TIME = 3f
    const val STONE_TIME = 5f
    const val MAX_TIME = 120f

    // true = every level and every stage is open (only for testing)
    const val TEST_UNLOCK_ALL = true

    const val I_UMB = 0
    const val I_WIND = 1
    const val I_MAGNET = 2
    const val I_SIZE = 3
    const val I_FOG = 4
    const val I_BOLT = 5
    const val I_CLOCK = 6
    const val I_PORTAL = 7
    const val I_COLOR = 8
    const val I_BUCKETS = 9
    const val I_BOUNCE = 10
    const val I_GRAV = 11
    const val I_ICE = 12
    const val I_X2 = 13
    const val I_GOLD = 14
    const val I_WAVE = 15
    const val I_CROWN = 16
    const val I_HEART = 17
    const val I_FIRE = 18

    val COLOR_NAMES = arrayOf("Red", "Yellow", "Purple", "Green", "Blue", "Pink")

    // the bonus colour of stage B for levels 7 to 15 (index in D.UMB)
    private val SPECIAL = intArrayOf(2, 4, 0, 3, 5, 1, 4, 2, 0)

    private fun Spec.info(n: String, h: String, i: Int) {
        name = n
        hint = h
        icon = i
    }

    fun hasStages(level: Int): Boolean = level > NORMAL_MAX

    fun letter(stage: Int): String = when (stage) {
        0 -> "A"
        1 -> "B"
        else -> "C"
    }

    fun spec(level: Int, stage: Int): Spec {
        val s = Spec(level, if (hasStages(level)) stage else 0)
        if (!hasStages(level)) {
            val n = level - 1
            s.target = 100 + 50 * n
            s.gap = 1.2f - 0.04f * n
            s.speed = 0.19f + 0.012f * n
            return s
        }
        if (level <= 15) {
            val i = level - 7
            s.timed = true
            s.gap = 0.85f - 0.015f * i
            s.speed = 0.26f + 0.01f * i
            when (stage) {
                0 -> {
                    s.target = 250 + 25 * i
                    s.time = 50f + 3f * i
                }
                1 -> {
                    s.target = 200 + 20 * i
                    s.time = 25f
                    s.special = SPECIAL[i]
                }
                else -> {
                    s.target = 230 + 20 * i
                    s.time = 55f + 3f * i
                    s.stones = true
                    s.stoneGap = 3.4f - 0.12f * i
                    s.hearts = HEARTS
                }
            }
            return s
        }
        advanced(s, level, stage)
        return s
    }

    // levels 16 to 30
    private fun advanced(s: Spec, level: Int, st: Int) {
        val i = level - 16
        val base = 240 + 6 * i
        s.timed = true
        s.speed = 0.33f + 0.004f * i
        s.gap = 0.74f - 0.006f * i
        s.stoneGap = (3.3f - 0.05f * i).coerceAtLeast(2.4f)
        s.target = if (st == 0) base else base - 20
        s.time = (if (st == 1) 55f else 60f) + i

        when (level) {
            16 -> when (st) {
                0 -> {
                    s.info("Wind Stream", "A wind current in the middle pushes the umbrellas sideways.", I_WIND)
                    s.wind = 1
                }
                1 -> {
                    s.info("Dynamic Wind", "The wind changes direction every 5 seconds. Watch the arrows!", I_WIND)
                    s.wind = 2
                }
                else -> {
                    s.info("Storm & Stones", "Wind pushes the stones too! Stone: -1 heart and -5 seconds.", I_WIND)
                    s.wind = 1
                    s.stones = true
                }
            }
            17 -> when (st) {
                0 -> {
                    s.info("Magnet Umbrella", "Pink magnet umbrellas fly into your bucket for 30 points!", I_MAGNET)
                    s.magnet = true
                }
                1 -> {
                    s.info("Repel Umbrella", "Dark umbrellas run away from the bucket. Do not catch them (-10)!", I_MAGNET)
                    s.repel = true
                }
                else -> {
                    s.info("Magnetic Trap", "Stones are magnetic too! Move away when a stone comes close.", I_MAGNET)
                    s.magnet = true
                    s.magnetStones = true
                    s.stones = true
                }
            }
            18 -> when (st) {
                0 -> {
                    s.info("Shrinking Bucket", "Your bucket gets smaller as your score grows.", I_SIZE)
                    s.shrink = true
                    s.target = base - 10
                }
                1 -> {
                    s.info("Growing Umbrella", "Catch a BIG umbrella and your bucket becomes wider for 7 seconds.", I_SIZE)
                    s.big = true
                }
                else -> {
                    s.info("Chaos Stage", "A shrinking bucket and stones. Stay sharp!", I_SIZE)
                    s.shrink = true
                    s.stones = true
                }
            }
            19 -> {
                s.time += 5f
                when (st) {
                    0 -> {
                        s.info("Fog", "Only the area around your bucket is visible.", I_FOG)
                        s.fog = 1
                        s.target = base - 30
                    }
                    1 -> {
                        s.info("Flashlight", "Lightning lights up the sky for a moment. Remember what you see!", I_BOLT)
                        s.fog = 2
                        s.target = base - 40
                    }
                    else -> {
                        s.info("Blind Survival", "Fog and stones! Spot the stones in time.", I_FOG)
                        s.fog = 1
                        s.stones = true
                        s.target = base - 40
                    }
                }
            }
            20 -> when (st) {
                0 -> {
                    s.info("Speed Rush", "The umbrellas fall much faster!", I_WAVE)
                    s.speedMul = 1.5f
                    s.target = base - 30
                }
                1 -> {
                    s.info("Time Freeze", "Catch the clock umbrella to slow everything down for 5 seconds.", I_CLOCK)
                    s.freeze = true
                }
                else -> {
                    s.info("Ultimate Test", "Fast umbrellas, stones and a timer.", I_WAVE)
                    s.speedMul = 1.5f
                    s.stones = true
                    s.target = base - 40
                }
            }
            21 -> when (st) {
                0 -> {
                    s.info("Portals", "An umbrella that enters one portal comes out of the other.", I_PORTAL)
                    s.portals = 1
                }
                1 -> {
                    s.info("Moving Portals", "The portals change place every 10 seconds.", I_PORTAL)
                    s.portals = 2
                }
                else -> {
                    s.info("Portal Trap", "Stones travel through the portals too!", I_PORTAL)
                    s.portals = 1
                    s.stones = true
                }
            }
            22 -> when (st) {
                0 -> {
                    s.info("Color Match", "The bucket color changes every 10 seconds. A matching umbrella gives double points.", I_COLOR)
                    s.color = 1
                    s.target = base + 40
                }
                1 -> {
                    s.info("Color Timer", "Only umbrellas of the bucket color give points!", I_COLOR)
                    s.color = 2
                    s.gap = 0.55f
                    s.target = 130
                }
                else -> {
                    s.info("Color Hazard", "A stone of the wrong color costs double: 2 hearts and -10 seconds.", I_COLOR)
                    s.color = 3
                    s.stones = true
                    s.target = base - 30
                }
            }
            23 -> when (st) {
                0 -> {
                    s.info("Double Bucket", "Two buckets move together. Catch with both of them.", I_BUCKETS)
                    s.dual = 1
                    s.target = base + 20
                }
                1 -> {
                    s.info("Split Control", "The two buckets move in opposite directions.", I_BUCKETS)
                    s.dual = 2
                    s.target = base
                }
                else -> {
                    s.info("Double Trouble", "Two buckets and stones. Dodge carefully!", I_BUCKETS)
                    s.dual = 1
                    s.stones = true
                    s.target = base
                }
            }
            24 -> when (st) {
                0 -> {
                    s.info("Bouncy Umbrellas", "Catch bouncy umbrellas in the middle, or they bounce away.", I_BOUNCE)
                    s.bounce = 1
                }
                1 -> {
                    s.info("Bounce Chain", "A bouncing umbrella can knock other umbrellas away.", I_BOUNCE)
                    s.bounce = 2
                }
                else -> {
                    s.info("Bounce & Dodge", "Bouncy umbrellas and stones together.", I_BOUNCE)
                    s.bounce = 2
                    s.stones = true
                }
            }
            25 -> when (st) {
                0 -> {
                    s.info("Gravity Flip", "Sometimes gravity flips and your bucket moves to the top!", I_GRAV)
                    s.gravity = 1
                }
                1 -> {
                    s.info("Gravity Switch", "Gravity keeps switching every 5 seconds.", I_GRAV)
                    s.gravity = 2
                }
                else -> {
                    s.info("Anti-Gravity", "Umbrellas fall down, but stones rise up from below!", I_GRAV)
                    s.gravity = 3
                    s.stones = true
                }
            }
            26 -> when (st) {
                0 -> {
                    s.info("Slippery Bucket", "The bucket slides like on ice. Plan your stops!", I_ICE)
                    s.slip = true
                }
                1 -> {
                    s.info("Slippery Precision", "A slippery bucket and a strict timer.", I_ICE)
                    s.slip = true
                    s.time = 40f
                    s.target = 190
                }
                else -> {
                    s.info("Slip & Stones", "A slippery bucket with stones. It will not stop at once!", I_ICE)
                    s.slip = true
                    s.stones = true
                }
            }
            27 -> when (st) {
                0 -> {
                    s.info("Multiplier Rain", "Catch the x2 umbrella to double your points for 8 seconds.", I_X2)
                    s.mult = true
                    s.target = base + 30
                }
                1 -> {
                    s.info("Fading Umbrellas", "Umbrellas fade away as they fall. Trust your instinct!", I_FOG)
                    s.fade = 1
                }
                else -> {
                    s.info("Invisible Stones", "Stones show up only when they are very close!", I_FOG)
                    s.fade = 2
                    s.stones = true
                }
            }
            28 -> when (st) {
                0 -> {
                    s.info("The Golden Hour", "Only 30 seconds. Golden umbrellas give 20 points each!", I_GOLD)
                    s.gold = 1
                    s.time = 30f
                    s.target = 300
                }
                1 -> {
                    s.info("Clock & Gold", "Golden umbrellas plus blue ones that give +3 seconds.", I_GOLD)
                    s.gold = 2
                    s.time = 35f
                    s.target = 300
                }
                else -> {
                    s.info("Golden Minefield", "Gold-colored stones hide in the golden rain!", I_GOLD)
                    s.gold = 1
                    s.goldStones = true
                    s.stones = true
                    s.time = 38f
                    s.target = 300
                }
            }
            29 -> when (st) {
                0 -> {
                    s.info("Endurance Test", "A long run. Umbrellas and stones keep on coming.", I_WAVE)
                    s.stones = true
                    s.time = 95f
                    s.target = 430
                }
                1 -> {
                    s.info("Surge Waves", "Big waves of umbrellas, then a short break.", I_WAVE)
                    s.surge = true
                    s.time = 75f
                    s.target = 380
                }
                else -> {
                    s.info("The Gauntlet", "Wind, a shrinking bucket, speed and stones all together.", I_CROWN)
                    s.wind = 1
                    s.shrink = true
                    s.speedMul = 1.25f
                    s.stones = true
                    s.time = 70f
                    s.target = 300
                }
            }
            else -> when (st) {
                0 -> {
                    s.info("Multi-Phase Boss", "Phase 1: fast umbrellas. Phase 2: wind, stones and a small bucket.", I_CROWN)
                    s.boss = true
                    s.hearts = HEARTS
                    s.time = 75f
                    s.target = 440
                }
                1 -> {
                    s.info("The Heart Saver", "Only 1 heart! One stone and it is over. Gold umbrellas give 30.", I_HEART)
                    s.heartSaver = true
                    s.hearts = 1
                    s.stones = true
                    s.gold = 2
                    s.time = 35f
                    s.target = 330
                }
                else -> {
                    s.info("Victory Rain", "No stones! Catch the glowing umbrellas and enjoy the fireworks.", I_FIRE)
                    s.victory = true
                    s.gap = 0.22f
                    s.speed = 0.3f
                    s.time = 40f
                    s.target = 450
                }
            }
        }
        if (s.stones && s.hearts == 0) s.hearts = HEARTS
    }

    // what to play after this level / stage (null = nothing more)
    fun next(level: Int, stage: Int): IntArray? {
        if (hasStages(level) && stage < 2) return intArrayOf(level, stage + 1)
        val nl = level + 1
        return if (nl <= PLAYABLE) intArrayOf(nl, 0) else null
    }

    fun describe(level: Int, stage: Int): String {
        val s = spec(level, stage)
        if (level >= 16) return s.name + " \u2022 " + s.target + " pts"
        return when (stage) {
            0 -> "Reach ${s.target} points in ${s.time.toInt()}s"
            1 -> "Catch ${COLOR_NAMES[s.special]} umbrellas for +3s \u2022 ${s.target} pts"
            else -> "Avoid stones! Reach ${s.target} points in ${s.time.toInt()}s"
        }
    }
}

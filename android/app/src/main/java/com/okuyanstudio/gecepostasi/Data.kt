package com.okuyanstudio.gecepostasi

/* Language-neutral game data, ported 1:1 from the web version. */

val KEYS = listOf("liman", "fener", "degirmen", "carsi", "yokus", "iskele")
val HOME: Map<String, Pair<String, Int>> = linkedMapOf(
    "nesrin" to ("liman" to 7), "deniz" to ("liman" to 4), "omer" to ("fener" to 1), "bekir" to ("fener" to 9),
    "selim" to ("degirmen" to 3), "hatice" to ("degirmen" to 12), "gulten" to ("carsi" to 1), "kemal" to ("carsi" to 5),
    "ayla" to ("yokus" to 2), "murat" to ("yokus" to 15), "tahsin" to ("iskele" to 6), "sevgi" to ("iskele" to 11),
    "ayla2" to ("iskele" to 3), "lidare" to ("iskele" to 1)
)
val PIDS = HOME.keys.toList()
val NOMOVE = setOf("omer", "deniz", "gulten", "lidare", "ayla", "ayla2", "bekir")

data class StampD(val icon: String, val c: String, val v: String)

val PSTAMPS = listOf(
    StampD("fish", "green", "5"), StampD("light", "blue", "10"), StampD("wave", "blue", "5"), StampD("anchor", "green", "10"),
    StampD("star", "north", "@north"), StampD("snow", "blue", "25"), StampD("drop", "green", "10"), StampD("key", "north", "5"),
    StampD("note", "blue", "10"), StampD("light", "north", "25")
)

data class Ink(val id: String, val color: Long, val need: Int)
val INKS = listOf(Ink("green", 0xFF00663F, 0), Ink("blue", 0xFF004481, 3), Ink("red", 0xFFA3242A, 6), Ink("gold", 0xFFA8781C, 10))

val CHOICES = listOf("w2", "w6", "w7")

data class StoryDef(
    val id: String, val night: Int, val kind: String, val to: String? = null, val mode: String,
    val stamp: StampD?, val target: String? = null, val why: String? = null,
    val oldKey: String? = null, val oldNo: Int = 0, val choice: Map<String, String>? = null
)

val STORY = listOf(
    StoryDef("s1", 1, "postcard", "deniz", "full", StampD("fish", "green", "5")),
    StoryDef("s2", 1, "envelope", "omer", "full", StampD("light", "blue", "10")),
    StoryDef("s3", 2, "postcard", "nesrin", "name", StampD("wave", "blue", "5")),
    StoryDef("s4", 2, "envelope", "omer", "full", StampD("anchor", "green", "5")),
    StoryDef("s5", 3, "envelope", "selim", "name", StampD("wave", "green", "5")),
    StoryDef("s6", 3, "postcard", null, "custom", null, target = "iade", why = "nostamp"),
    StoryDef("s7", 4, "postcard", "nesrin", "full", StampD("anchor", "green", "10")),
    StoryDef("s8", 4, "envelope", "gulten", "forward", StampD("light", "blue", "25"), oldKey = "iskele", oldNo = 2),
    StoryDef("s9", 5, "postcard", "deniz", "full", StampD("star", "north", "@north")),
    StoryDef("s10", 5, "envelope", "omer", "full", StampD("fish", "green", "5")),
    StoryDef("w1", 6, "envelope", "gulten", "full", StampD("anchor", "blue", "10")),
    StoryDef("w2", 6, "envelope", "omer", "full", null, choice = mapOf("fener" to "k", "iade" to "r")),
    StoryDef("w3", 7, "postcard", "omer", "wet", StampD("wave", "green", "5")),
    StoryDef("w4", 7, "postcard", "deniz", "full", StampD("light", "blue", "5")),
    StoryDef("w5", 8, "envelope", "ayla2", "twin", StampD("anchor", "green", "10")),
    StoryDef("w6", 8, "envelope", "omer", "full", StampD("anchor", "blue", "25"), choice = mapOf("fener" to "r", "carsi" to "k")),
    StoryDef("w7", 9, "parcel", "lidare", "full", StampD("star", "green", "10"), choice = mapOf("iskele" to "k", "iade" to "r")),
    StoryDef("w8", 9, "postcard", "omer", "full", StampD("snow", "blue", "5")),
    StoryDef("w9", 10, "envelope", "omer", "full", StampD("fish", "green", "10")),
    StoryDef("w10", 10, "envelope", "omer", "full", StampD("light", "north", "25"))
)

val NIGHT_COUNTS = intArrayOf(6, 8, 10, 10, 12, 12, 12, 13, 13, 14)

/** A letter on the desk. */
data class Letter(
    val kind: String,
    val story: String? = null,
    var stamp: StampD? = null,
    val paid: Boolean = false,
    var mode: String = "full",
    val person: String? = null,
    var why: String? = null,
    val choice: Map<String, String>? = null,
    var target: String? = null,
    var oldKey: String? = null,
    var oldNo: Int = 0,
    var unkI: Int = 0,
    val textI: Int = 0,
    val signI: Int = 0,
    val senderI: Int = 0,
    val billI: Int = 0,
)

data class Moved(val pid: String, val key: String, val no: Int)

/** One line of an address block. */
data class AddrLine(val t: String, val c: String = "")

package com.okuyanstudio.gecepostasi

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.Normalizer
import kotlin.random.Random

private fun ri(n: Int) = Random.nextInt(n)
private fun <T> rand(a: List<T>): T = a[ri(a.size)]

/** One night's shift. */
class Night(val n: Int, val moved: Moved?, val hard: Boolean, val replay: Boolean) {
    var q: List<Letter> = emptyList()
    var i by mutableIntStateOf(0)
    var miss by mutableIntStateOf(0)
    var ok by mutableIntStateOf(0)
    val counts = mutableStateMapOf<String, Int>()
    var burnt by mutableStateOf(false)
    var timeMax by mutableFloatStateOf(0f)
    var time by mutableFloatStateOf(0f)

    fun homeOf(p: String): Pair<String, Int> = if (moved != null && moved.pid == p) moved.key to moved.no else HOME.getValue(p)
}

data class EndInfo(val n: Int, val ok: Int, val miss: Int, val rate: Int, val burnt: Boolean, val rewardN: Int?, val rewardGold: Boolean, val rewardUp: Boolean, val ink: Int?)

/** Pure game rules — letter generation and line building. */
object Rules {
    fun rStamp() = StampD(rand(listOf("anchor", "light", "fish", "wave")), rand(listOf("green", "blue")), rand(listOf("5", "10", "25")))

    fun storyLetter(s: StoryDef, g: Night): Letter {
        val l = Letter(kind = s.kind, story = s.id, stamp = s.stamp, mode = s.mode, person = s.to, why = s.why, choice = s.choice)
        if (s.mode == "custom") { l.target = s.target; return l }
        if (s.mode in setOf("name", "wet", "twin")) l.why = s.mode
        if (s.mode == "forward") { l.why = "forward"; l.oldKey = s.oldKey; l.oldNo = s.oldNo }
        l.target = if (l.choice != null) null else g.homeOf(s.to!!).first
        return l
    }

    private fun pickPid(n: Int, g: Night): String {
        if (g.moved != null && Random.nextFloat() < .12f) return g.moved.pid
        if (n >= 8 && Random.nextFloat() < .14f) return rand(listOf("ayla", "ayla2"))
        return rand(PIDS.filter { it != "lidare" && (it != "ayla2" || n >= 8) })
    }

    private fun applyMode(l: Letter, m: String) {
        when (m) {
            "name", "wet", "twin" -> { l.mode = m; l.why = l.why ?: m }
            "nostamp" -> { l.stamp = null; l.target = "iade"; l.why = "nostamp" }
            "unknown" -> { l.mode = "unknown"; l.unkI = ri(4); l.target = "iade"; l.why = l.why ?: "unknown" }
            "forward" -> { l.mode = "forward"; l.why = l.why ?: "forward"; l.oldKey = rand(KEYS.filter { it != HOME.getValue(l.person!!).first }); l.oldNo = 3 + ri(14) }
        }
    }

    fun filler(n: Int, g: Night, pidIn: String? = null, kindIn: String? = null, mIn: String? = null, vIn: String? = null): Letter {
        val pid = pidIn ?: pickPid(n, g)
        val kind = kindIn ?: run {
            val k = Random.nextFloat()
            if (k < .22f) "bill" else if (n >= 6 && k < .36f) "parcel" else if (k < .66f) "envelope" else "postcard"
        }
        val l: Letter
        if (kind == "bill") {
            l = Letter(kind = kind, person = pid, mode = "full", paid = true, billI = ri(4))
        } else {
            var st = rStamp()
            l = Letter(kind = kind, person = pid, mode = "full", stamp = st, textI = ri(7), signI = ri(5), senderI = ri(5))
            if (kind == "parcel") {
                st = st.copy(v = vIn ?: rand(listOf("5", "10", "25", "25", "25"))); l.stamp = st
                if (st.v != "25") { l.target = "iade"; l.why = "underpaid" }
            }
            val m = mIn ?: run {
                val r = Random.nextFloat()
                when {
                    n >= 7 && r < .12f -> "wet"
                    n >= 2 && r < .26f -> "name"
                    n >= 3 && r < .36f && kind != "parcel" -> "nostamp"
                    n >= 3 && r < .44f -> "unknown"
                    n >= 4 && r < .56f -> "forward"
                    else -> "full"
                }
            }
            applyMode(l, m)
        }
        if ((pid == "ayla" || pid == "ayla2") && n >= 8 && (l.mode == "name" || l.mode == "wet")) {
            l.mode = "twin"; if (l.why == "name" || l.why == "wet") l.why = "twin"
        }
        val mv = g.moved != null && g.moved.pid == pid
        if (mv && l.mode == "forward") l.mode = "full"
        if (l.target == null) l.target = g.homeOf(pid).first
        if (mv && l.target != "iade") l.why = "moved"
        return l
    }

    fun forcedFor(n: Int, g: Night): MutableList<Letter> {
        fun plain() = rand(PIDS.filter { it !in setOf("ayla", "ayla2", "lidare") })
        return when {
            n == 6 -> listOf("5", "10", "25").map { filler(6, g, kindIn = "parcel", vIn = it, mIn = "full") }.toMutableList()
            n == 7 -> mutableListOf(filler(7, g, kindIn = "envelope", mIn = "wet", pidIn = plain()), filler(7, g, kindIn = "postcard", mIn = "wet", pidIn = plain()))
            n == 8 -> mutableListOf(filler(8, g, kindIn = "envelope", pidIn = "ayla", mIn = "twin"), filler(8, g, kindIn = "postcard", pidIn = "ayla2", mIn = "twin"))
            n == 9 && g.moved != null -> mutableListOf(filler(9, g, kindIn = "envelope", pidIn = g.moved.pid, mIn = "full"), filler(9, g, kindIn = "postcard", pidIn = g.moved.pid, mIn = "name"))
            else -> mutableListOf()
        }
    }

    fun newNight(rn: Int?, save: SaveData): Night {
        val n = rn ?: save.night
        val count = if (n <= 10) NIGHT_COUNTS[n - 1] else 12
        val fn = minOf(n, 10)
        val moved = if (n >= 9) {
            val pid = rand(PIDS.filter { it !in NOMOVE }); val old = HOME.getValue(pid).first
            Moved(pid, rand(KEYS.filter { it != old }), 2 + ri(16))
        } else null
        val g = Night(n, moved, save.hard, rn != null && rn < save.night)
        val st = STORY.filter { it.night == n }.map { storyLetter(it, g) }
        val q = forcedFor(n, g)
        while (q.size < count - st.size) q.add(filler(fn, g))
        q.shuffle()
        st.forEachIndexed { i, s -> q.add(minOf(q.size, (i + 1) * q.size / (st.size + 1) + i), s) }
        g.q = q
        if (g.hard) { g.timeMax = count * 6500f; g.time = g.timeMax }
        return g
    }

    fun mask(x: String): String = x.split(" ").joinToString(" ") { w ->
        val a = w.codePoints().toArray().map { String(Character.toChars(it)) }
        when {
            a.size <= 2 -> w
            a.size == 3 -> a[0] + "·" + a[2]
            else -> a[0] + a[1] + "·".repeat(a.size - 3) + a.last()
        }
    }

    fun linesOf(l: Letter, s: Lang): List<AddrLine> {
        if (l.mode == "custom") return s.storyLines(l.story!!).map { AddrLine(it) }
        if (l.mode == "unknown") return listOf(AddrLine(s.unknown[l.unkI]), AddrLine(s.ui("town")))
        val (st, no) = HOME.getValue(l.person!!)
        val nm = s.person(l.person).first
        return when (l.mode) {
            "name" -> listOf(AddrLine(nm), AddrLine(s.ui("town")))
            "twin" -> listOf(AddrLine(nm), AddrLine(s.no(no)), AddrLine(s.ui("town")))
            "wet" -> listOf(AddrLine(mask(nm), "wet"), AddrLine("≈≈≈≈≈≈≈", "blot"), AddrLine(s.ui("town")))
            "forward" -> listOf(AddrLine(nm), AddrLine(s.addr(s.street(l.oldKey!!), l.oldNo), "old"), AddrLine(s.ui("newAddr") + s.addr(s.street(st), no), "new"))
            else -> listOf(AddrLine(nm), AddrLine(s.addr(s.street(st), no)), AddrLine(s.ui("town")))
        }
    }

    fun textOf(l: Letter, s: Lang, e: Int) = if (l.story != null) s.storyText(l.story, e) else s.lines[l.textI]
    fun signOf(l: Letter, s: Lang) = if (l.story != null) s.storySign(l.story) else s.signs[l.signI]
    fun senderOf(l: Letter, s: Lang) = when {
        l.kind == "bill" -> s.bills[l.billI]
        l.story != null -> s.storySender(l.story)
        else -> s.senders[l.senderI]
    }
    fun noteOf(l: Letter, s: Lang) = l.story?.let { s.storyNote(it) }

    fun shown(p: String, night: Int) = when (p) { "ayla2" -> night >= 8; "lidare" -> night >= 6; else -> true }

    fun norm(x: String): String = Normalizer.normalize(x.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("[\\u0300-\\u036f·.,،]"), "").replace("ı", "i").replace(Regex("\\s+"), " ").trim()
}

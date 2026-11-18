package com.okuyanstudio.gecepostasi

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class Hint(val kind: String, val id: String)

/** All game/UI state. Animations live in the UI; rules and bookkeeping live here. */
class AppState(val save: SaveData, private val langs: Map<String, Lang>, val sound: Sound, private val scope: CoroutineScope) {
    val s: Lang get() = langs.getValue(save.lang)
    fun lang(code: String) = langs.getValue(code)

    var g by mutableStateOf<Night?>(null)
    var cur by mutableStateOf<Letter?>(null)
    var dealId by mutableIntStateOf(0)           // bumps on every new letter (drives the deal animation)
    var busy by mutableStateOf(false)
    var hint by mutableStateOf<Hint?>(null)
    var lastEnd by mutableStateOf<EndInfo?>(null)
    var startShown by mutableStateOf(true)
    var endShown by mutableStateOf(false)
    var dawn by mutableStateOf(false)
    var sheet by mutableStateOf<String?>(null)
    var pendingStart by mutableStateOf(false)
    var toast by mutableStateOf<String?>(null)
    private var toastJob: Job? = null

    fun syncAudio() {
        sound.sfxOn = save.sound; sound.musicOn = save.music; sound.vibOn = save.vib
        if (save.music || save.sound) sound.ensure()
        val n = g?.n ?: save.night
        sound.syncMusic(if (n in 6..10) "winter" else "harbor")
    }

    val inkColor: Long get() {
        val c = save.stampCount
        return (INKS.firstOrNull { it.id == save.ink && c >= it.need } ?: INKS[0]).color
    }

    fun startLabel(): String = when {
        save.night == 1 -> s.ui("start")
        save.night <= 10 -> s.startN(save.night)
        else -> s.ui("startZen")
    }
    fun nightLabel(n: Int) = if (n <= 10) s.nightN(n) else s.ui("zen")

    fun hintText(): String {
        val h = hint ?: return ""
        if (h.kind == "choice" && cur?.choice != null) return ""
        return if (h.kind == "intro") s.intro(h.id.toInt()) else s.why(h.id)
    }
    val choiceOn: Boolean get() = hint?.kind == "choice" && cur?.choice != null

    fun showStart() {
        g = null; cur = null; endShown = false; startShown = true; hint = null; dawn = false
        syncAudio()
    }

    fun onStartPressed() {
        syncAudio()
        if (!save.seenHow) { pendingStart = true; sheet = "how" } else startNight(null)
    }

    fun startNight(rn: Int?) {
        val night = Rules.newNight(rn, save)
        g = night; lastEnd = null; dawn = false
        startShown = false; endShown = false
        hint = Hint("intro", minOf(night.n, 11).toString())
        deal(); syncAudio()
    }

    private fun deal() {
        val night = g ?: return
        if (night.i >= night.q.size) { endNight(); return }
        cur = night.q[night.i]; dealId++
        if (cur?.choice != null) hint = Hint("choice", "")
        busy = false
    }

    fun canSort() = !busy && cur != null && !startShown && !endShown && sheet == null

    fun isRight(key: String): Boolean {
        val l = cur ?: return false
        return if (l.choice != null) l.choice.containsKey(key) else key == l.target
    }

    /** Called after the success animation finishes. */
    fun commit(key: String) {
        val night = g ?: return; val l = cur ?: return
        night.ok++; night.counts[key] = (night.counts[key] ?: 0) + 1
        if (l.choice != null) { save.choices[l.story!!] = l.choice.getValue(key); save.persist() }
        if (l.story != null && l.story !in save.board) {
            save.board.add(l.story); save.persist(); showToast(if (l.kind == "envelope") "toastEnv" else "toastCard")
        }
        if (hint != null && hint!!.kind != "intro") hint = null
        night.i++; deal()
    }

    fun miss() {
        val night = g ?: return; val l = cur ?: return
        night.miss++
        if (night.timeMax > 0) night.time = maxOf(0f, night.time - 3000f)
        hint = if (l.choice != null) Hint("choice", "") else Hint("why", if (night.hard) "hard" else (l.why ?: "def"))
    }

    fun showToast(k: String) {
        toast = s.ui(k); sound.pin()
        toastJob?.cancel(); toastJob = scope.launch { delay(2600); toast = null }
    }

    private fun endNight() {
        val night = g ?: return
        cur = null
        val miss = night.miss
        var rate = if (miss == 0) 3 else if (miss <= 2) 2 else 1
        if (night.burnt) rate = 1
        var rewardN: Int? = null; var gold = false; var up = false; var ink: Int? = null
        if (night.n <= 10 && rate == 3) {
            val lv = if (night.hard) 2 else 1; val prev = save.stamps[night.n] ?: 0
            if (lv > prev) {
                val before = save.stampCount; save.stamps[night.n] = lv
                rewardN = night.n; gold = lv == 2; up = prev > 0
                val after = save.stampCount
                val i = INKS.indexOfFirst { it.need > before && it.need <= after }
                if (i > 0) ink = i
            }
        }
        if (night.n <= 10) save.best[night.n] = maxOf(save.best[night.n] ?: 0, rate)
        if (!night.replay && save.night <= 10) save.night++
        save.persist()
        lastEnd = EndInfo(night.n, night.ok, miss, rate, night.burnt, rewardN, gold, up, ink)
        dawn = true
        if (night.n == 5) sound.sting("p1") else if (night.n == 10) sound.sting("w" + save.endIdx())
        scope.launch { delay(700); endShown = true }
    }

    fun tickCandle(dtMs: Float) {
        val night = g ?: return
        if (night.timeMax <= 0f || cur == null || night.burnt) return
        if (sheet != null && sheet != "rehber") return
        night.time -= dtMs
        if (night.time <= 0f) { night.time = 0f; night.burnt = true; showToast("burnt") }
    }

    fun openSheet(id: String) { sheet = id }
    fun closeSheet() {
        val was = sheet; sheet = null
        if (was == "how") {
            save.seenHow = true; save.persist()
            if (pendingStart) { pendingStart = false; startNight(null) }
        }
    }

    fun setLang(code: String) { save.lang = code; save.persist() }

    fun reset() { save.resetProgress(); sheet = null; showStart() }
}

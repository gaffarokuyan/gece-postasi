package com.okuyanstudio.gecepostasi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test
import java.io.File

class Shots {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5, maxPercentDifference = 100.0)

    private val langs by lazy { Lang.parse(File("src/main/assets/i18n.json").readText()) }

    @Composable
    private fun state(lang: String, night: Int, setup: AppState.() -> Unit): AppState {
        val scope = rememberCoroutineScope()
        return remember {
            val sv = SaveData(paparazzi.context)
            sv.lang = lang; sv.night = night; sv.seenHow = true; sv.music = false; sv.sound = false; sv.vib = false
            sv.board.addAll(STORY.filter { it.night < night }.map { it.id })
            (1 until night).forEach { sv.best[it] = 3 - it % 3; if (it % 2 == 1) sv.stamps[it] = if (it == 3) 2 else 1 }
            AppState(sv, langs, Sound(paparazzi.context), scope).apply(setup)
        }
    }

    private fun shot(name: String, lang: String, night: Int, setup: AppState.() -> Unit) = paparazzi.snapshot(name) {
        GameApp(state(lang, night, setup))
    }

    @Test fun startTr() = shot("start_tr", "tr", 3) { showStart() }
    @Test fun startAr() = shot("start_ar", "ar", 1) { showStart() }
    @Test fun startKu() = shot("start_ku", "ku", 7) { showStart() }
    @Test fun deskTr() = shot("desk_tr", "tr", 2) { startNight(null); hint = Hint("why", "name") }
    @Test fun deskKu9() = shot("desk_ku9", "ku", 9) { startNight(null) }
    @Test fun deskAr6() = shot("desk_ar6", "ar", 6) { startNight(null) }
    @Test fun choiceEn() = shot("choice_en", "en", 8) {
        startNight(null); cur = g!!.q.first { it.story == "w6" }; hint = Hint("choice", "")
    }
    @Test fun endTr() = shot("end_tr", "tr", 6) {
        startNight(5); g!!.q.forEach { _ -> }; lastEnd = EndInfo(5, 12, 0, 3, false, 5, false, false, 1); cur = null; endShown = true
    }
    @Test fun rehberKu() = shot("rehber_ku", "ku", 8) { showStart(); startShown = false; sheet = "rehber" }
    @Test fun ayarTr() = shot("ayar_tr", "tr", 3) { showStart(); startShown = false; sheet = "ayar" }
    @Test fun howEn() = shot("how_en", "en", 4) { showStart(); startShown = false; sheet = "how" }
    @Test fun panoAr() = shot("pano_ar", "ar", 7) { showStart(); startShown = false; sheet = "pano" }
    @Test fun pulKu() = shot("pul_ku", "ku", 11) { showStart(); startShown = false; sheet = "pul" }

    @Test fun letters() {
        listOf("tr", "ku", "ar").forEach { lang -> listOf(0, 1).forEach { part ->
            paparazzi.snapshot("letters_${lang}_$part") {
                val st = state(lang, 9) { startNight(null) }
                val s = langs.getValue(lang)
                val g = st.g!!
                val sample = listOf(
                    Rules.storyLetter(STORY.first { it.id == "s1" }, g),
                    Rules.storyLetter(STORY.first { it.id == "w7" }, g),
                    Rules.filler(9, g, kindIn = "bill"),
                    Rules.filler(9, g, kindIn = "envelope", mIn = "forward", pidIn = "nesrin"),
                    Rules.filler(9, g, kindIn = "postcard", mIn = "wet", pidIn = "kemal"),
                    Rules.filler(9, g, kindIn = "postcard", mIn = "twin", pidIn = "ayla2"),
                ).let { if (part == 0) it.take(3) else it.drop(3) }
                CompositionLocalProvider(LocalPal provides LightPal, LocalFonts provides if (lang == "ar") ArabicFonts else LatinFonts, LocalAppS provides s,
                    LocalLayoutDirection provides if (s.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                    Column(Modifier.background(LightPal.desk).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        sample.forEach { l -> Box(Modifier.fillMaxWidth().aspectRatio(1.5f)) { LetterCard(l, 0, Modifier.fillMaxWidth().aspectRatio(1.5f)) } }
                    }
                }
            }
        } }
    }
}

class Logic {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test fun playAllNights() {
        val langs = Lang.parse(File("src/main/assets/i18n.json").readText())
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        repeat(30) { run ->
            val sv = SaveData(paparazzi.context); sv.seenHow = true; sv.music = false; sv.sound = false
            sv.lang = Lang.ORDER[run % 4]
            val st = AppState(sv, langs, Sound(paparazzi.context), scope)
            var letters = 0
            for (n in 1..11) {
                st.startNight(null)
                val g = st.g!!
                check(g.n == n) { "night ${g.n} != $n" }
                if (n <= 10) check(g.q.size == NIGHT_COUNTS[n - 1]) { "count n=$n ${g.q.size}" }
                check(STORY.filter { it.night == n }.all { s -> g.q.any { it.story == s.id } })
                while (st.cur != null) {
                    val l = st.cur!!
                    Lang.ORDER.forEach { code -> val s = langs.getValue(code); Rules.linesOf(l, s); Rules.textOf(l, s, 0); Rules.senderOf(l, s); if (l.kind != "bill") Rules.signOf(l, s) }
                    val key = l.target ?: l.choice!!.keys.random()
                    check(key == "iade" || key in KEYS) { "bad key $key" }
                    // a wrong slot must be rejected
                    val wrong = (KEYS + "iade").first { it != key && l.choice?.containsKey(it) != true }
                    check(!st.isRight(wrong))
                    check(st.isRight(key))
                    st.commit(key); letters++
                }
                check(st.lastEnd!!.rate == 3)
            }
            check(sv.night == 11) { "night ${sv.night}" }
            check(sv.stamps.size == 10)
            println("run $run lang=${sv.lang} letters=$letters endIdx=${sv.endIdx()}")
        }
    }
}

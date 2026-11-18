package com.okuyanstudio.gecepostasi

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import org.junit.Rule
import org.junit.Test
import java.io.File
import app.cash.paparazzi.Snapshot
import app.cash.paparazzi.SnapshotHandler
import com.android.resources.ScreenOrientation
import java.lang.reflect.Proxy

/** Writes full-resolution PNGs to build/store/ (Paparazzi's own report scales images down).
 *  java.awt is not on the Android compile classpath, so the frame handler is a reflective proxy. */
class FullRes : SnapshotHandler {
    override fun newFrameHandler(snapshot: Snapshot, frameCount: Int, fps: Int): SnapshotHandler.FrameHandler {
        val dir = File("build/store").apply { mkdirs() }
        val file = File(dir, (snapshot.name ?: "shot") + ".png")
        return Proxy.newProxyInstance(javaClass.classLoader, arrayOf(SnapshotHandler.FrameHandler::class.java)) { _, m, args ->
            if (m.name == "handle") {
                Class.forName("javax.imageio.ImageIO")
                    .getMethod("write", Class.forName("java.awt.image.RenderedImage"), String::class.java, File::class.java)
                    .invoke(null, args[0], "png", file)
            }
            null
        } as SnapshotHandler.FrameHandler
    }
    override fun close() {}
}

private val langsAll by lazy { Lang.parse(File("src/main/assets/i18n.json").readText()) }

@Composable
private fun Locals(lang: String, content: @Composable () -> Unit) {
    val s = langsAll.getValue(lang)
    CompositionLocalProvider(
        LocalPal provides LightPal, LocalFonts provides if (lang == "ar") ArabicFonts else LatinFonts, LocalAppS provides s,
        LocalLayoutDirection provides if (s.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr, content = content
    )
}

@Composable
private fun deskBg() = Modifier.background(Brush.radialGradient(listOf(LightPal.desk2, LightPal.desk), radius = 1400f))

/** Phone screenshots, 1080×1920, for each store language. */
class PhoneShots {
    @get:Rule val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(screenWidth = 1080, screenHeight = 1920, xdpi = 480, ydpi = 480, density = Density.XXHIGH),
        snapshotHandler = FullRes(), useDeviceResolution = true
    )

    @Composable
    private fun state(lang: String, night: Int, setup: AppState.() -> Unit): AppState {
        val scope = rememberCoroutineScope()
        return remember {
            Table.instant = true
            val sv = SaveData(paparazzi.context)
            sv.lang = lang; sv.night = night; sv.seenHow = true; sv.music = false; sv.sound = false; sv.vib = false
            sv.board.addAll(STORY.filter { it.night < night }.map { it.id })
            (1 until night).forEach { sv.best[it] = if (it == 2) 2 else 3; if (it != 2) sv.stamps[it] = if (it == 4) 2 else 1 }
            AppState(sv, langsAll, Sound(paparazzi.context), scope).apply(setup)
        }
    }

    private fun shot(name: String, lang: String, night: Int, setup: AppState.() -> Unit) = paparazzi.snapshot(name) { GameApp(state(lang, night, setup)) }

    private fun all(f: (String) -> Unit) = Lang.ORDER.forEach(f)

    @Test fun s1_start() = all { l -> shot("1_start_$l", l, 6) { showStart() } }
    @Test fun s2_desk() = all { l -> shot("2_desk_$l", l, 4) {
        startNight(null); val g = g!!; repeat(3) { g.counts[KEYS[it * 2]] = it + 1 }; g.i = 4
        cur = g.q.first { it.story == "s8" }; hint = Hint("intro", "4")
    } }
    @Test fun s3_choice() = all { l -> shot("3_choice_$l", l, 9) {
        startNight(null); val g = g!!; g.i = 7; g.counts["iskele"] = 2; g.counts["liman"] = 3; g.counts["fener"] = 2
        cur = g.q.first { it.story == "w7" }; hint = Hint("choice", "")
    } }
    @Test fun s4_end() = all { l -> shot("4_end_$l", l, 6) {
        startNight(5); cur = null; lastEnd = EndInfo(5, 12, 0, 3, false, 5, false, false, 1); dawn = true; endShown = true
    } }
    @Test fun s5_pano() = all { l -> shot("5_pano_$l", l, 9) { showStart(); startShown = false; sheet = "pano" } }
}

/** 512×512 Play Store icon. */
class IconShot {
    @get:Rule val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(screenWidth = 512, screenHeight = 512, xdpi = 160, ydpi = 160, density = Density.MEDIUM),
        snapshotHandler = FullRes(), useDeviceResolution = true
    )
    @Test fun icon() = paparazzi.snapshot("icon_512") {
        Locals("tr") {
            Box(Modifier.size(512.dp).then(deskBg()), contentAlignment = Alignment.Center) {
                Box(Modifier.size(330.dp, 220.dp).rotate(-5f).shadow(24.dp, RoundedCornerShape(6.dp)).background(LightPal.paper, RoundedCornerShape(6.dp)).paperGrain()) {
                    Canvas(Modifier.fillMaxSize()) {
                        val c = Color(0x22072146)
                        drawLine(c, Offset(0f, 0f), Offset(size.width / 2, size.height * .55f), 5f)
                        drawLine(c, Offset(size.width, 0f), Offset(size.width / 2, size.height * .55f), 5f)
                        drawLine(LightPal.blue, Offset(size.width * .14f, size.height * .74f), Offset(size.width * .58f, size.height * .74f), 9f)
                        drawLine(LightPal.blue, Offset(size.width * .14f, size.height * .86f), Offset(size.width * .44f, size.height * .86f), 9f)
                    }
                    Box(Modifier.align(Alignment.TopEnd).padding(top = 22.dp, end = 24.dp)) { StampFace(StampD("light", "green", "5"), 1.55f) }
                }
                Postmark(langsAll.getValue("tr").pmOk, Color(0xFF00663F), 1.9f, Modifier.offset(x = 30.dp, y = (-44).dp))
            }
        }
    }
}

/** 1024×500 feature graphic (TR, EN, KU, AR). */
class FeatureShot {
    @get:Rule val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(screenWidth = 1024, screenHeight = 500, xdpi = 160, ydpi = 160, density = Density.MEDIUM, orientation = ScreenOrientation.LANDSCAPE),
        snapshotHandler = FullRes(), useDeviceResolution = true
    )
    @Test fun feature() = Lang.ORDER.forEach { lang ->
        paparazzi.snapshot("feature_$lang") {
            Locals(lang) {
                val s = LocalAppS.current; val f = LocalFonts.current
                val st = state()
                Row(Modifier.size(1024.dp, 500.dp).then(deskBg()).padding(horizontal = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        BasicText(s.title, style = TextStyle(color = LightPal.paper, fontFamily = f.cond, fontWeight = FontWeight.ExtraBold,
                            fontSize = if (s.rtl) 86.sp else 104.sp, lineHeight = if (s.rtl) 100.sp else 92.sp, shadow = Shadow(Color(0xB300915C), Offset(7f, 5f), 0f)))
                        Spacer(Modifier.height(22.dp))
                        BasicText(s.ui("intro").substringBefore(".") + ".", Modifier.width(440.dp),
                            style = TextStyle(color = LightPal.onDeskSoft, fontFamily = f.ui, fontSize = 22.sp, lineHeight = 30.sp))
                    }
                    Box(Modifier.width(430.dp).height(400.dp)) {
                        Box(Modifier.align(Alignment.Center).offset(x = 18.dp, y = 22.dp).width(400.dp).aspectRatio(1.5f).rotate(5f).background(LightPal.paper2, RoundedCornerShape(3.dp)))
                        LetterCard(st, 0, Modifier.align(Alignment.Center).width(400.dp).aspectRatio(1.5f).rotate(-4f))
                    }
                }
            }
        }
    }
    @Composable private fun state(): Letter = remember { Letter(kind = "postcard", story = "s1", stamp = StampD("fish", "green", "5"), person = "deniz", target = "liman") }
}

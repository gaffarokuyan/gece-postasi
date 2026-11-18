package com.okuyanstudio.gecepostasi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.random.Random

@Immutable
data class Pal(
    val desk: Color, val desk2: Color, val paper: Color, val paper2: Color,
) {
    val blue = Color(0xFF004481)
    val green = Color(0xFF00915C)
    val greenBtn = Color(0xFF007A4F)
    val greenInk = Color(0xFF00663F)
    val mint = Color(0xFF7FD6B0)
    val ink = Color(0xFF072146)
    val inkSoft = Color(0xFF40536F)
    val onDesk = Color(0xFFE9EEF5)
    val onDeskSoft = Color(0xFFA3B6CE)
    val north = Color(0xFF2A2F45)
    val yellow = Color(0xFFFFF3A6)
    val yellowInk = Color(0xFF3A3220)
    val note = Color(0xFFFBFAF6)
    val gold = Color(0xFFC9A227)
    fun stampColor(c: String) = when (c) { "green" -> green; "blue" -> blue; else -> north }
}

val LightPal = Pal(Color(0xFF072146), Color(0xFF0B2D5C), Color(0xFFF1EEE5), Color(0xFFDFDACB))
val DarkPal = Pal(Color(0xFF04162F), Color(0xFF082650), Color(0xFFE9E5DA), Color(0xFFD3CDBC))

@OptIn(ExperimentalTextApi::class)
private fun vf(res: Int, w: Int, wdth: Float = 100f) =
    Font(res, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w), FontVariation.width(wdth)))

@OptIn(ExperimentalTextApi::class)
private fun wf(res: Int, w: Int) = Font(res, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))

@Immutable
class Fonts(val ui: FontFamily, val cond: FontFamily, val hand: FontFamily, val handScale: Float)

private val bric = FontFamily(listOf(400, 500, 600, 650, 700, 750, 800).map { vf(R.font.bricolage, it) })
private val bricCond = FontFamily(listOf(600, 700, 750, 800).map { vf(R.font.bricolage, it, 78f) })
private val caveat = FontFamily(wf(R.font.caveat, 500), wf(R.font.caveat, 700))
private val readex = FontFamily(listOf(400, 500, 600, 650, 700, 750, 800).map { wf(R.font.readex, minOf(it, 700)) }.distinctBy { it.weight })
private val aref = FontFamily(Font(R.font.aref_regular, FontWeight.Normal), Font(R.font.aref_bold, FontWeight.Bold))

val LatinFonts = Fonts(bric, bricCond, caveat, 1f)
val ArabicFonts = Fonts(readex, readex, aref, .86f)

val LocalPal = staticCompositionLocalOf { LightPal }
val LocalFonts = staticCompositionLocalOf { LatinFonts }

/* ---------- line icons (the same SVG paths as the web version) ---------- */

class Icon(val vb: Float, val paths: List<Path>)

private fun icon(vb: Float, vararg parts: String): Icon = Icon(vb, parts.map { p ->
    if (p.startsWith("c ")) {
        val (cx, cy, r) = p.removePrefix("c ").split(" ").map { it.toFloat() }
        Path().apply { addOval(androidx.compose.ui.geometry.Rect(cx - r, cy - r, cx + r, cy + r)) }
    } else PathParser().parsePathString(p).toPath()
})

object Icons {
    val anchor = icon(32f, "c 16 7 2.6", "M16 10v16M10 14h12M7 19c1 5 5 7 9 7s8-2 9-7")
    val light = icon(32f, "M12 27l2-15h4l2 15zM13 12h6V9h-6zM16 9V6M7 8l4 2M25 8l-4 2")
    val fish = icon(32f, "M5 16c4-6 13-7 19 0-6 7-15 6-19 0zM24 16l5-4v8z", "c 10 15 .8")
    val wave = icon(32f, "M4 13c3-3 5-3 8 0s5 3 8 0 5-3 8 0M4 20c3-3 5-3 8 0s5 3 8 0 5-3 8 0")
    val star = icon(32f, "M16 5l3 7.5 8 .6-6 5.2 2 7.8-7-4.3-7 4.3 2-7.8-6-5.2 8-.6z")
    val snow = icon(32f, "M16 5v22M6.5 10.5l19 11M6.5 21.5l19-11M13 7l3 3 3-3M13 25l3-3 3 3")
    val drop = icon(32f, "M16 5c-5 7-8 11-8 15a8 8 0 0 0 16 0c0-4-3-8-8-15z")
    val key = icon(32f, "c 11 16 5", "M16 16h11M23 16v4M27 16v3")
    val note = icon(32f, "M9 6h14v20H9zM13 12h6M13 16h6M13 20h4")
    val mill = icon(32f, "M16 15L9 6M16 15l9-9M16 15l-9 9M16 15l9 9M16 15v12M12 27h8", "c 16 15 1.6")
    val market = icon(32f, "M5 12l3-6h16l3 6zM5 12c0 2 1.6 3.2 3.6 3.2s3.6-1.2 3.6-3.2c0 2 1.7 3.2 3.8 3.2s3.8-1.2 3.8-3.2c0 2 1.6 3.2 3.6 3.2S27 14 27 12M8 15.5V26h16V15.5M13.5 26v-5.5h5V26")
    val hill = icon(32f, "M4 26L13 13l5 6 3-3 7 10zM22 8a2.5 2.5 0 1 0 0 .1")
    val pier = icon(32f, "M4 14h24M9 14v9M16 14v9M23 14v9M4 26c2-1.6 4-1.6 6 0s4 1.6 6 0 4-1.6 6 0 4 1.6 6 0")
    val back = icon(24f, "M9 14L4 9l5-5M4 9h11a5 5 0 0 1 0 10h-4")
    val board = icon(24f, "M4 5h16v15H4z", "c 12 5 1.6", "M8 11h8M8 15h5")
    val sliders = icon(24f, "M4 7h10M18 7h2M4 17h2M10 17h10", "c 16 7 2", "c 8 17 2")
    val chev = icon(24f, "M9 6l6 6-6 6")
    fun stamp(n: String) = when (n) { "anchor" -> anchor; "light" -> light; "fish" -> fish; "wave" -> wave; "star" -> star; "snow" -> snow; "drop" -> drop; "key" -> key; else -> note }
    fun cubby(k: String) = when (k) { "liman" -> anchor; "fener" -> light; "degirmen" -> mill; "carsi" -> market; "yokus" -> hill; "iskele" -> pier; else -> back }
}

fun DrawScope.drawIcon(ic: Icon, color: Color, strokeW: Float, at: Offset = Offset.Zero, sz: Size = size, alpha: Float = 1f) {
    val s = minOf(sz.width, sz.height) / ic.vb
    translate(at.x + (sz.width - ic.vb * s) / 2, at.y + (sz.height - ic.vb * s) / 2) {
        scale(s, s, pivot = Offset.Zero) {
            ic.paths.forEach { drawPath(it, color, alpha = alpha, style = Stroke(strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
        }
    }
}

/** Grain texture used for the desk and paper. */
fun noiseBitmap(sz: Int, r: Int, g: Int, b: Int, maxA: Int, seed: Int): ImageBitmap {
    val rnd = Random(seed)
    val px = IntArray(sz * sz) { val a = rnd.nextInt(maxA); (a shl 24) or (r shl 16) or (g shl 8) or b }
    return Bitmap.createBitmap(px, sz, sz, Bitmap.Config.ARGB_8888).asImageBitmap()
}

@Composable
fun handSp(base: Float) = base * LocalFonts.current.handScale

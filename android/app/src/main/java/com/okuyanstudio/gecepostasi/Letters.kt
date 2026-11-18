package com.okuyanstudio.gecepostasi

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** px-like size → sp that ignores the system font scale (the card is a fixed object). */
@Composable
fun fx(px: Float): TextUnit = with(LocalDensity.current) { px.dp.toSp() }

private val paperGrain by lazy { noiseBitmap(140, 140, 132, 112, 70, 7) }

fun Modifier.paperGrain(alpha: Float = 1f) = drawBehind {
    drawRect(ShaderBrush(ImageShader(paperGrain, TileMode.Repeated, TileMode.Repeated)), alpha = alpha)
}

/* ---------------- stamps ---------------- */

@Composable
fun StampView(st: StampD?, paid: Boolean, k: Float, gold: Boolean = false) {
    val pal = LocalPal.current; val s = LocalAppS.current
    when {
        paid -> Box(
            Modifier.width((78 * k).dp).border(1.5.dp, pal.blue).padding((6 * k).dp),
            contentAlignment = Alignment.Center
        ) {
            BasicText(s.ui("paid"), style = TextStyle(color = pal.blue, fontSize = fx(11 * k), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = fx(14 * k), fontFamily = LocalFonts.current.ui))
        }
        st == null -> Canvas(Modifier.size((54 * k).dp, (64 * k).dp)) {
            drawRect(Color(0x47072146), style = Stroke(1.5.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 5f))))
        }
        else -> StampFace(st, k, gold)
    }
}

@Composable
fun StampFace(st: StampD, k: Float, gold: Boolean = false) {
    val pal = LocalPal.current; val s = LocalAppS.current
    val col = pal.stampColor(st.c)
    val tm = rememberTextMeasurer()
    val label = if (st.v == "@north") s.north else st.v
    val font = LocalFonts.current.ui
    Box(
        Modifier.size((54 * k).dp, (64 * k).dp).rotate(1.5f)
            .then(if (gold) Modifier.drawBehind {
                drawRect(pal.gold.copy(alpha = .45f), topLeft = Offset(-6.dp.toPx(), -6.dp.toPx()), size = Size(size.width + 12.dp.toPx(), size.height + 12.dp.toPx()))
                drawRect(pal.gold, topLeft = Offset(-2.dp.toPx(), -2.dp.toPx()), size = Size(size.width + 4.dp.toPx(), size.height + 4.dp.toPx()))
            } else Modifier)
    ) {
        Canvas(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
            drawRect(col)
            val step = 6.dp.toPx() * k; val r = 1.6.dp.toPx() * k
            var x = 0f
            while (x <= size.width + 1) { drawCircle(Color.Black, r, Offset(x, 0f), blendMode = BlendMode.Clear); drawCircle(Color.Black, r, Offset(x, size.height), blendMode = BlendMode.Clear); x += step }
            var y = 0f
            while (y <= size.height + 1) { drawCircle(Color.Black, r, Offset(0f, y), blendMode = BlendMode.Clear); drawCircle(Color.Black, r, Offset(size.width, y), blendMode = BlendMode.Clear); y += step }
            val inset = 4.dp.toPx() * k
            val iw = size.width - 2 * inset; val ih = size.height - 2 * inset
            val isz = iw * .66f
            val txt = tm.measure(label, TextStyle(fontSize = (10.dp.toPx() * k / (density * fontScale)).sp, fontWeight = FontWeight.Bold, color = pal.paper, fontFamily = font))
            val total = isz + txt.size.height
            val top = inset + (ih - total) / 2
            drawIcon(Icons.stamp(st.icon), pal.paper, 1.9f, Offset(inset + (iw - isz) / 2, top), Size(isz, isz))
            drawText(txt, topLeft = Offset(inset + (iw - txt.size.width) / 2, top + isz))
        }
    }
}

/* ---------------- postmark ---------------- */

@Composable
fun Postmark(label: String, ink: Color, k: Float, modifier: Modifier) {
    val s = LocalAppS.current; val tm = rememberTextMeasurer(); val font = LocalFonts.current.ui
    Canvas(modifier.size((118 * k).dp, (90 * k).dp)) {
        val sc = size.width / 130f
        val sw = 2.4f * sc
        rotate(-9f, pivot = Offset(48 * sc, 50 * sc)) {
            drawCircle(ink, 31 * sc, Offset(48 * sc, 50 * sc), style = Stroke(sw))
            drawCircle(ink, 25 * sc, Offset(48 * sc, 50 * sc), style = Stroke(sw))
            val p = Path()
            listOf(38f, 50f, 62f).forEach { yy ->
                p.moveTo(82 * sc, yy * sc)
                var x = 82f
                repeat(2) {
                    p.relativeCubicTo(7 * sc, -5 * sc, 11 * sc, 5 * sc, 18 * sc, 0f)
                    x += 18f
                }
            }
            drawPath(p, ink, style = Stroke(sw))
            val t1 = tm.measure(s.pmTown, TextStyle(fontSize = (10.5f * sc / (density * fontScale)).sp, fontWeight = FontWeight.ExtraBold, color = ink, fontFamily = font, letterSpacing = if (s.rtl) 0.sp else .4.sp))
            val t2 = tm.measure(label, TextStyle(fontSize = (12f * sc / (density * fontScale)).sp, fontWeight = FontWeight.ExtraBold, color = ink, fontFamily = font))
            drawText(t1, topLeft = Offset(48 * sc - t1.size.width / 2, 47 * sc - t1.size.height * .8f))
            drawText(t2, topLeft = Offset(48 * sc - t2.size.width / 2, 62 * sc - t2.size.height * .8f))
        }
    }
}

/* ---------------- letter card ---------------- */

@Composable
private fun AddrLines(lines: List<AddrLine>, k: Float, modifier: Modifier = Modifier) {
    val pal = LocalPal.current; val f = LocalFonts.current
    Column(modifier) {
        lines.forEach { l ->
            val color = when (l.c) { "old" -> pal.inkSoft; "new" -> pal.greenInk; "wet" -> Color(0xFF4F6280); "blot" -> Color(0x8C4F6280); else -> pal.ink }
            var m = Modifier.fillMaxWidth().drawBehind {
                drawLine(Color(0x2E072146), Offset(0f, size.height - .5f), Offset(size.width, size.height - .5f), 1f)
            }.padding(top = (2 * k).dp, bottom = (1 * k).dp)
            if ((l.c == "wet" || l.c == "blot") && Build.VERSION.SDK_INT >= 31) m = m.blur(0.6.dp)
            val style = TextStyle(
                fontFamily = f.hand, color = color,
                fontWeight = if (l.c == "new") FontWeight.Bold else FontWeight.Medium,
                fontSize = fx(22.5f * k * f.handScale), lineHeight = fx(26.5f * k * f.handScale),
                textDecoration = if (l.c == "old") TextDecoration.LineThrough else null,
                letterSpacing = if (l.c == "blot") 2.sp else 0.sp
            )
            if (l.c == "new") BasicText(l.t, m, style = style, maxLines = 3, overflow = TextOverflow.Ellipsis)
            else FitText(l.t, m, style)
        }
    }
}

/** Renders a letter. [onStampPos] reports the stamp's centre-x and top inside the card (px). */
@Composable
fun LetterCard(l: Letter, endIdx: Int, modifier: Modifier = Modifier, onStamp: (LayoutCoordinates) -> Unit = {}) {
    val pal = LocalPal.current; val s = LocalAppS.current; val f = LocalFonts.current
    val lines = remember(l, s) { Rules.linesOf(l, s) }
    BoxWithConstraints(modifier) {
        val k = (maxWidth.value / 400f).coerceIn(.72f, 1.1f)
        val stampMod = Modifier.onGloballyPositioned { c -> onStamp(c) }
        val shape = RoundedCornerShape(3.dp)
        val bg = if (l.kind == "parcel") Color(0xFFC9A574) else pal.paper
        Box(
            Modifier.fillMaxSize().shadow(18.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
                .background(bg, shape)
                .then(if (l.kind == "parcel") Modifier.drawBehind {
                    val tape = Color(0x805C401A)
                    drawRect(tape, Offset(size.width * .71f, 0f), Size(size.width * .016f, size.height))
                    drawRect(tape, Offset(0f, size.height * .68f), Size(size.width, size.height * .02f))
                } else Modifier)
                .then(if (l.kind == "envelope") Modifier.drawBehind {
                    val c = Color(0x17072146); val h = size.height * .52f
                    drawLine(c, Offset(0f, 0f), Offset(size.width / 2, h), 2f)
                    drawLine(c, Offset(size.width, 0f), Offset(size.width / 2, h), 2f)
                } else Modifier)
                .paperGrain(if (l.kind == "parcel") .5f else 1f)
        ) {
            if (l.mode == "wet") Canvas(Modifier.fillMaxSize()) {
                drawCircle(Brush.radialGradient(listOf(Color(0x29004481), Color.Transparent), Offset(size.width * .72f, size.height * .7f), size.width * .38f), size.width * .38f, Offset(size.width * .72f, size.height * .7f))
                drawCircle(Brush.radialGradient(listOf(Color(0x1F004481), Color.Transparent), Offset(size.width * .55f, size.height * .84f), size.width * .25f), size.width * .25f, Offset(size.width * .55f, size.height * .84f))
            }
            val pad = (if (l.kind == "postcard") 14 else 16) * k
            when (l.kind) {
                "postcard" -> Row(Modifier.fillMaxSize().padding(pad.dp), horizontalArrangement = Arrangement.spacedBy((12 * k).dp)) {
                    Column(
                        Modifier.weight(1.08f).fillMaxHeight().drawBehind {
                            val x = if (s.rtl) 0f else size.width
                            drawLine(Color(0x38072146), Offset(x, 0f), Offset(x, size.height), 1f)
                        }.padding(end = (12 * k).dp)
                    ) {
                        val st = TextStyle(fontFamily = f.hand, color = pal.blue, fontSize = fx(20f * k * f.handScale), lineHeight = fx(22f * k * f.handScale), fontWeight = FontWeight.Medium)
                        BasicText(Rules.textOf(l, s, endIdx), Modifier.weight(1f, fill = false), style = st, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.weight(.001f))
                        BasicText(Rules.signOf(l, s), Modifier.fillMaxWidth(), style = st.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.End), maxLines = 1)
                    }
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        Box(Modifier.align(Alignment.End).then(stampMod)) { StampView(l.stamp, l.paid, k) }
                        Spacer(Modifier.weight(1f))
                        AddrLines(lines, k)
                    }
                }
                "bill" -> Box(Modifier.fillMaxSize().padding(pad.dp)) {
                    BasicText(Rules.senderOf(l, s), Modifier.fillMaxWidth(.58f), style = TextStyle(fontFamily = f.cond, fontWeight = FontWeight(750), fontSize = fx(16 * k), color = pal.blue))
                    Box(Modifier.align(Alignment.TopEnd).then(stampMod)) { StampView(null, true, k) }
                    Column(
                        Modifier.align(Alignment.BottomStart).offset(x = (8 * k).dp, y = (-6 * k).dp).fillMaxWidth(.66f)
                            .border(1.dp, Color(0x40072146), RoundedCornerShape(4.dp))
                            .background(Color(0x61FFFFFF), RoundedCornerShape(4.dp)).padding(horizontal = (12 * k).dp, vertical = (10 * k).dp)
                    ) {
                        lines.forEach { BasicText(it.t, style = TextStyle(fontFamily = f.ui, fontSize = fx(15 * k), lineHeight = fx(21 * k), color = pal.ink), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    }
                }
                else -> Box(Modifier.fillMaxSize().padding(pad.dp)) {
                    val parcel = l.kind == "parcel"
                    BasicText(
                        s.ui("from") + Rules.senderOf(l, s), Modifier.fillMaxWidth(.58f),
                        style = TextStyle(fontFamily = f.ui, fontSize = fx(13 * k), lineHeight = fx(17.5f * k), color = if (parcel) Color(0xFF3D2E17) else pal.inkSoft), maxLines = 3
                    )
                    Box(Modifier.align(Alignment.TopEnd).then(stampMod)) { StampView(l.stamp, false, k) }
                    if (parcel) {
                        BasicText(s.ui("parcel"), Modifier.align(Alignment.BottomEnd).padding(bottom = (6 * k).dp).rotate(-4f),
                            style = TextStyle(fontFamily = f.cond, fontWeight = FontWeight.ExtraBold, fontSize = fx(24 * k), color = Color(0x803D2E17)))
                        AddrLines(lines, k, Modifier.align(Alignment.BottomStart).offset(x = (4 * k).dp).fillMaxWidth(.66f)
                            .shadow(3.dp, RoundedCornerShape(2.dp)).background(pal.note, RoundedCornerShape(2.dp)).padding(horizontal = (10 * k).dp, vertical = (6 * k).dp))
                    } else {
                        Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(bottom = (2 * k).dp)) {
                            Spacer(Modifier.weight(.3f))
                            AddrLines(lines, k, Modifier.weight(.7f))
                        }
                    }
                }
            }
            Rules.noteOf(l, s)?.let { note ->
                Box(
                    Modifier.align(Alignment.TopStart).padding(start = (14 * k).dp, top = (44 * k).dp).fillMaxWidth(.56f).rotate(-2.5f)
                        .shadow(6.dp).background(pal.yellow).padding((10 * k).dp)
                ) {
                    BasicText(note, style = TextStyle(fontFamily = f.hand, fontSize = fx(16.5f * k * f.handScale), lineHeight = fx(18.5f * k * f.handScale), color = pal.yellowInk))
                }
                Canvas(Modifier.align(Alignment.TopStart).padding(start = (28 * k).dp, top = (36 * k).dp).size((10 * k).dp, (22 * k).dp)) {
                    drawRoundRect(Color(0xFF8A93A3), cornerRadius = CornerRadius(6.dp.toPx()), style = Stroke(2.dp.toPx()))
                }
            }
        }
    }
}

@Composable
fun widthClamp(max: Dp): Modifier = Modifier.widthIn(max = max)

/** Single line that shrinks (down to 72%) to fit instead of being cut off. */
@Composable
fun FitText(text: String, modifier: Modifier, style: TextStyle) {
    val tm = rememberTextMeasurer()
    BoxWithConstraints(modifier) {
        val maxW = constraints.maxWidth
        val w = remember(text, style, maxW) { tm.measure(text, style, maxLines = 1, softWrap = false).size.width }
        val sc = if (w > maxW && w > 0) maxOf(.72f, maxW.toFloat() / w) else 1f
        BasicText(text, Modifier.fillMaxWidth(), style = style.copy(fontSize = style.fontSize * sc), maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
    }
}

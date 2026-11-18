package com.okuyanstudio.gecepostasi

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

val LocalAppS = compositionLocalOf<Lang> { error("no lang") }

private val deskGrain by lazy { noiseBitmap(180, 255, 255, 255, 34, 3) }

@Composable
fun GameApp(st: AppState) {
    val pal = if (isSystemInDarkTheme()) DarkPal else LightPal
    val s = st.s
    CompositionLocalProvider(
        LocalPal provides pal,
        LocalFonts provides if (s.code == "ar") ArabicFonts else LatinFonts,
        LocalAppS provides s,
        LocalLayoutDirection provides if (s.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) {
        Root(st)
    }
}

@Composable
private fun Root(st: AppState) {
    val pal = LocalPal.current
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()
    val table = remember { Table(st, scope, density) }

    LaunchedEffect(st.dealId) { if (st.cur != null) table.dealIn() }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) withFrameMillis { t -> if (last != 0L) st.tickCandle(minOf(100L, t - last).toFloat()); last = t }
    }
    if (androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current != null) BackHandler(enabled = st.sheet != null) { st.closeSheet() }

    Box(
        Modifier.fillMaxSize()
            .background(Brush.radialGradient(listOf(pal.desk2, pal.desk), radius = 1600f))
            .drawBehind { drawRect(ShaderBrush(ImageShader(deskGrain, TileMode.Repeated, TileMode.Repeated))) }
    ) {
        Column(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
                .align(Alignment.TopCenter).widthIn(max = 520.dp).padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 12.dp)
        ) {
            Header(st)
            Desk(st, table, Modifier.weight(1f))
            Rack(st, table)
        }
        val dawnA by animateFloatAsState(if (st.dawn) 1f else 0f, tween(1200), label = "dawn")
        if (dawnA > 0f) Box(Modifier.fillMaxSize().alpha(dawnA).background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x26004481), Color(0x597FD6B0)))))

        Overlay(st.startShown) { StartCard(st) }
        Overlay(st.endShown) { EndCard(st) }
        Sheets(st)
    }
}

/* ---------------- header ---------------- */

@Composable
fun Chip(text: String? = null, icon: Icon? = null, desc: String = "", onClick: () -> Unit) {
    val pal = LocalPal.current; val f = LocalFonts.current
    Box(
        Modifier.height(40.dp).then(if (icon != null) Modifier.width(40.dp) else Modifier)
            .clip(CircleShape).border(1.5.dp, Color(0x47E9EEF5), CircleShape)
            .clickable(onClick = onClick).semantics { contentDescription = desc.ifEmpty { text ?: "" } }
            .padding(horizontal = if (icon != null) 0.dp else 15.dp),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) Canvas(Modifier.size(19.dp)) { drawIcon(icon, pal.onDesk, 1.8f) }
        else BasicText(text!!, style = TextStyle(color = pal.onDesk, fontFamily = f.ui, fontWeight = FontWeight.SemiBold, fontSize = 15.sp), maxLines = 1)
    }
}

@Composable
private fun Header(st: AppState) {
    val pal = LocalPal.current; val f = LocalFonts.current; val s = st.s
    val g = st.g
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicText(st.nightLabel(g?.n ?: st.save.night), style = TextStyle(color = pal.onDesk, fontFamily = f.cond, fontWeight = FontWeight(750), fontSize = 22.sp), maxLines = 1)
        Row(Modifier.weight(1f).alpha(if (g != null && st.cur != null) 1f else 0f).semantics { contentDescription = if (g != null) s.left(g.q.size - g.i) else "" },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val frac = if (g != null && g.q.isNotEmpty()) g.i.toFloat() / g.q.size else 0f
            val fa by animateFloatAsState(frac, tween(350, easing = EaseOutBack), label = "prog")
            Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(Color(0x24E9EEF5))) {
                Box(Modifier.fillMaxWidth(fa).height(8.dp).clip(CircleShape).background(pal.mint))
            }
            if (g != null) BasicText("${g.i}/${g.q.size}", style = TextStyle(color = pal.onDeskSoft, fontFamily = f.ui, fontWeight = FontWeight.SemiBold, fontSize = 13.sp))
        }
        Chip(s.ui("rehber")) { st.openSheet("rehber") }
        Chip(icon = Icons.board, desc = s.ui("pano")) { st.openSheet("pano") }
        Chip(icon = Icons.sliders, desc = s.ui("settings")) { st.openSheet("ayar") }
    }
}

/* ---------------- desk ---------------- */

@Composable
private fun Desk(st: AppState, table: Table, modifier: Modifier) {
    val pal = LocalPal.current; val f = LocalFonts.current; val s = st.s
    val g = st.g; val cur = st.cur
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                // shrink the letter when the notice / choice bar need room, so nothing gets squeezed
                val extra = (if (g?.moved != null && cur != null) 84.dp else 0.dp) +
                    (if (st.choiceOn) 190.dp else 64.dp) + (if (g != null && g.timeMax > 0f) 16.dp else 0.dp)
                val w = minOf(maxWidth, 400.dp, (maxHeight - extra - 12.dp) * 1.5f).coerceAtLeast(220.dp)
                Column(Modifier.align(Alignment.Center).width(w), horizontalAlignment = Alignment.CenterHorizontally) {
                    // candle (hard mode)
                    if (g != null && cur != null && g.timeMax > 0f) {
                        val p = (g.time / g.timeMax).coerceIn(0f, 1f)
                        Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(Color(0x1FE9EEF5))) {
                            Box(Modifier.fillMaxWidth(p).height(5.dp).background(if (p < .25f) Color(0xFFE0663D) else Color(0xFFF2B24C)))
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    // night notice
                    val mv = g?.moved
                    if (mv != null && cur != null) {
                        Box(Modifier.fillMaxWidth().padding(bottom = 14.dp).rotate(-.6f).shadow(8.dp).background(pal.yellow).padding(horizontal = 12.dp, vertical = 8.dp)) {
                            BasicText(
                                androidx.compose.ui.text.buildAnnotatedString {
                                    pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight(750))); append(s.ui("notice") + "  "); pop()
                                    append(s.moved(s.person(mv.pid).first, s.addr(s.street(mv.key), mv.no)))
                                },
                                style = TextStyle(color = pal.yellowInk, fontFamily = f.ui, fontSize = 14.sp, lineHeight = 19.sp)
                            )
                        }
                    }
                    Slot(st, table, w)
                    val ht = st.hintText()
                    val warn = st.hint != null && st.hint!!.kind != "intro"
                    val hc by animateColorAsState(if (warn) pal.mint else pal.onDeskSoft, label = "hint")
                    if (!st.choiceOn) BasicText(
                        ht, Modifier.padding(top = 16.dp).heightIn(min = 44.dp).widthIn(max = 330.dp),
                        style = TextStyle(color = hc, fontFamily = f.ui, fontSize = 15.sp, lineHeight = 21.sp, textAlign = TextAlign.Center)
                    )
                    if (st.choiceOn && cur?.choice != null) ChoiceBar(st, table, cur)
                }
            }
        }
        // toast
        val tv = st.toast
        androidx.compose.animation.AnimatedVisibility(tv != null, Modifier.align(Alignment.TopCenter).padding(top = 6.dp),
            enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { -it / 4 }, exit = fadeOut(tween(250))) {
            Row(Modifier.shadow(10.dp, RoundedCornerShape(3.dp)).background(pal.paper, RoundedCornerShape(3.dp)).padding(start = 12.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(pal.green))
                Spacer(Modifier.width(9.dp))
                BasicText(tv ?: "", style = TextStyle(color = pal.ink, fontFamily = f.ui, fontWeight = FontWeight.SemiBold, fontSize = 14.sp))
            }
        }
    }
}

@Composable
private fun Slot(st: AppState, table: Table, w: Dp) {
    val pal = LocalPal.current; val s = st.s
    val g = st.g; val cur = st.cur
    val pile = g != null && cur != null && g.q.size - g.i > 1
    Box(Modifier.width(w).aspectRatio(1.5f).onGloballyPositioned { table.slotCoords = it }) {
        if (pile) {
            Box(Modifier.fillMaxSize().offset(5.dp, 7.dp).rotate(2.4f).background(pal.paper2, RoundedCornerShape(3.dp)))
            Box(Modifier.fillMaxSize().offset((-4).dp, 5.dp).rotate(-1.6f).background(Color(0xFFCFC9B8), RoundedCornerShape(3.dp)))
        }
        if (cur != null) {
            val desc = remember(cur, s) {
                val ls = Rules.linesOf(cur, s).joinToString(", ") { if (it.c == "old") s.ui("crossed") + it.t else it.t }
                "${s.ui("letter")}. $ls. ${if (cur.stamp == null && !cur.paid) s.ui("noStamp") else ""}${Rules.noteOf(cur, s)?.let { " $it" } ?: ""}"
            }
            Box(
                Modifier.fillMaxSize()
                    .graphicsLayer {
                        translationX = table.ox.value; translationY = table.oy.value
                        rotationZ = table.rot.value; scaleX = table.scl.value; scaleY = table.scl.value; alpha = table.alpha.value
                    }
                    .onGloballyPositioned { table.cardCoords = it }
                    .semantics { contentDescription = desc }
                    .pointerInput(st.dealId) {
                        detectDragGestures(
                            onDragStart = { table.dragStart(it) },
                            onDrag = { ch, d -> ch.consume(); table.drag(d) },
                            onDragEnd = { table.dragEnd() },
                            onDragCancel = { table.dragEnd() }
                        )
                    }
            ) {
                LetterCard(cur, st.save.endIdx(), Modifier.fillMaxSize()) { table.stampCoords = it }
                val label = table.pm
                if (label != null) {
                    val a = table.pmAnim.value
                    val k = (w.value / 400f).coerceIn(.72f, 1.1f)
                    Postmark(
                        label, Color(st.inkColor), k,
                        Modifier.absoluteOffset { IntOffset(table.pmPos.x.roundToInt(), table.pmPos.y.roundToInt()) }
                            .graphicsLayer { val sc = 1.6f - .6f * a; scaleX = sc; scaleY = sc; rotationZ = -7f * (1 - a); alpha = .88f * a }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChoiceBar(st: AppState, table: Table, cur: Letter) {
    val pal = LocalPal.current; val f = LocalFonts.current; val s = st.s
    val keys = cur.choice!!.keys.sortedBy { if (cur.choice[it] == "r") 0 else 1 }
    Column(Modifier.fillMaxWidth().padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicText(s.ui("choiceHint"), Modifier.fillMaxWidth(), style = TextStyle(color = pal.onDeskSoft, fontFamily = f.ui, fontSize = 14.sp, lineHeight = 19.sp, textAlign = TextAlign.Center))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            keys.forEach { k ->
                val note = cur.choice[k] == "k"
                val col = if (note) pal.yellow else pal.mint
                Column(
                    Modifier.weight(1f).heightIn(min = 54.dp).clip(RoundedCornerShape(6.dp)).background(col.copy(alpha = .08f))
                        .border(1.5.dp, col.copy(alpha = if (note) .7f else .6f), RoundedCornerShape(6.dp))
                        .clickable { table.sortTo(k) }.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                ) {
                    BasicText(if (note) s.ui("optNote") else s.ui("optRule"), style = TextStyle(color = col, fontFamily = f.ui, fontWeight = FontWeight.SemiBold, fontSize = 12.sp))
                    FitText(if (k == "iade") s.ui("iade") else s.street(k), Modifier.fillMaxWidth(), TextStyle(color = pal.onDesk, fontFamily = f.ui, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Center))
                }
            }
        }
    }
}

/* ---------------- rack ---------------- */

@Composable
private fun Rack(st: AppState, table: Table) {
    val pal = LocalPal.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp < 660
        Column(
            Modifier.fillMaxWidth().shadow(14.dp, RoundedCornerShape(6.dp)).background(pal.blue, RoundedCornerShape(6.dp)).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KEYS.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { row.forEach { Cubby(st, table, it, Modifier.weight(1f), compact) } }
            }
            Cubby(st, table, "iade", Modifier.fillMaxWidth(), compact)
        }
    }
}

@Composable
private fun Cubby(st: AppState, table: Table, key: String, modifier: Modifier, compact: Boolean) {
    val pal = LocalPal.current; val f = LocalFonts.current; val s = st.s
    val iade = key == "iade"
    val name = if (iade) s.ui("iade") else s.street(key)
    val hot = table.hot == key; val ok = table.okKey == key
    val cur = st.cur
    val pick = st.choiceOn && cur?.choice?.containsKey(key) == true
    val pickNote = pick && cur?.choice?.get(key) == "k"
    val count = st.g?.counts?.get(key) ?: 0
    val shakeN = table.shake[key] ?: 0
    val sx = remember { Animatable(0f) }
    val d = LocalDensity.current.density
    LaunchedEffect(shakeN) {
        if (shakeN > 0) sx.animateTo(0f, keyframes { durationMillis = 220; -4f * d at 55; 4f * d at 140 })
    }
    val labBg by animateColorAsState(if (ok && !iade) pal.mint else if (iade) Color.Transparent else pal.paper, tween(200), label = "lab")
    val shape = RoundedCornerShape(3.dp)
    Box(
        modifier
            .graphicsLayer { translationX = sx.value }
            .then(
                if (hot) Modifier.drawBehind { drawRoundRect(pal.mint, Offset(-4.dp.toPx(), -4.dp.toPx()), Size(size.width + 8.dp.toPx(), size.height + 8.dp.toPx()), CornerRadius(5.dp.toPx()), style = Stroke(2.dp.toPx())) }
                else if (pick) Modifier.drawBehind {
                    drawRoundRect(if (pickNote) pal.yellow else pal.mint, Offset(-4.dp.toPx(), -4.dp.toPx()), Size(size.width + 8.dp.toPx(), size.height + 8.dp.toPx()), CornerRadius(5.dp.toPx()),
                        style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))))
                } else Modifier
            )
            .heightIn(min = if (iade) 48.dp else if (compact) 60.dp else 74.dp)
            .clip(shape)
            .then(
                if (iade) Modifier.background(Color(0x5904162F)).drawBehind {
                    drawRoundRect(Color(0x66E9EEF5), cornerRadius = CornerRadius(3.dp.toPx()), style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))))
                } else Modifier.background(pal.desk).drawBehind {
                    drawRect(Brush.verticalGradient(listOf(Color(0x80000000), Color.Transparent), endY = 18.dp.toPx()))
                    if (hot) drawRect(Color(0x597FD6B0), style = Stroke(1.dp.toPx()))
                }
            )
            .onGloballyPositioned { table.cubbies[key] = it.boundsInRoot() }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { table.sortTo(key) }
            .semantics { contentDescription = s.putIn(name) }
    ) {
        if (!iade && count > 0) Column(Modifier.align(Alignment.TopCenter).padding(top = 9.dp, start = 12.dp, end = 12.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(minOf(count, 6)) { Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(1.dp)).background(pal.paper2.copy(alpha = .85f))) }
        }
        val icoCol = if (hot || ok) pal.mint else pal.onDeskSoft
        val icoA = if (hot || ok) .95f else if (iade) .75f else .4f
        if (iade) {
            Row(Modifier.align(Alignment.Center).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(20.dp)) { drawIcon(Icons.back, icoCol, 1.8f, alpha = icoA) }
                Spacer(Modifier.width(8.dp))
                BasicText(name, style = TextStyle(color = if (ok) pal.mint else pal.onDesk, fontFamily = f.ui, fontWeight = FontWeight(650), fontSize = 15.sp))
            }
        } else {
            Column(Modifier.fillMaxWidth().padding(start = 7.dp, end = 7.dp, top = 8.dp, bottom = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (!compact) Canvas(Modifier.padding(top = 6.dp, bottom = 6.dp).size(26.dp)) { drawIcon(Icons.cubby(key), icoCol, 1.8f, alpha = icoA) }
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 40.dp).shadow(1.dp, RoundedCornerShape(2.dp)).background(labBg, RoundedCornerShape(2.dp)).padding(horizontal = 3.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(name, style = TextStyle(color = pal.ink, fontFamily = f.ui, fontWeight = FontWeight(650), fontSize = 13.sp, lineHeight = 15.sp, textAlign = TextAlign.Center), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/* ---------------- overlays ---------------- */

@Composable
private fun Overlay(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(visible, enter = fadeIn(tween(250)), exit = fadeOut(tween(200))) {
        Box(
            Modifier.fillMaxSize().background(Color(0xBD030E20))
                .clickable(remember { MutableInteractionSource() }, null) {}
                .windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState()).padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.fillMaxWidth().widthIn(max = 420.dp).align(Alignment.Center)) { content() }
        }
    }
}

@Composable
fun PaperCard(content: @Composable () -> Unit) {
    val pal = LocalPal.current
    Column(Modifier.fillMaxWidth().shadow(30.dp, RoundedCornerShape(4.dp)).background(pal.paper, RoundedCornerShape(4.dp)).paperGrain(.6f).padding(start = 24.dp, end = 24.dp, top = 30.dp, bottom = 22.dp)) { content() }
}

@Composable
fun Btn(text: String, ghost: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val pal = LocalPal.current; val f = LocalFonts.current
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier.fillMaxWidth().heightIn(min = if (ghost) 48.dp else 52.dp).clip(shape)
            .then(if (ghost) Modifier.border(1.5.dp, Color(0x4D072146), shape) else Modifier.background(pal.greenBtn))
            .clickable(onClick = onClick).padding(horizontal = if (ghost) 8.dp else 18.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(text, style = TextStyle(color = if (ghost) pal.ink else Color.White, fontFamily = f.ui, fontWeight = if (ghost) FontWeight.SemiBold else FontWeight.Bold, fontSize = if (ghost) 15.5.sp else 17.sp, textAlign = TextAlign.Center))
    }
}

@Composable
fun LangSeg(st: AppState, modifier: Modifier = Modifier) {
    val pal = LocalPal.current
    Row(modifier.clip(RoundedCornerShape(8.dp)).background(Color(0x14072146)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Lang.ORDER.forEach { code ->
            val on = st.save.lang == code; val l = st.lang(code)
            Box(
                Modifier.weight(1f).heightIn(min = 38.dp).then(if (on) Modifier.shadow(2.dp, RoundedCornerShape(5.dp)).background(pal.paper, RoundedCornerShape(5.dp)) else Modifier)
                    .clip(RoundedCornerShape(5.dp)).clickable { st.setLang(code) }.padding(horizontal = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(l.name, style = TextStyle(color = if (on) pal.ink else pal.inkSoft, fontFamily = if (code == "ar") ArabicFonts.ui else LatinFonts.ui, fontWeight = FontWeight.SemiBold, fontSize = 14.sp), maxLines = 1)
            }
        }
    }
}

@Composable
private fun StartCard(st: AppState) {
    val pal = LocalPal.current; val f = LocalFonts.current; val s = st.s
    Box {
        PaperCard {
            BasicText(
                s.title, Modifier.padding(bottom = 14.dp, end = 60.dp),
                style = TextStyle(
                    color = pal.ink, fontFamily = f.cond, fontWeight = FontWeight.ExtraBold,
                    fontSize = if (s.rtl) 52.sp else 64.sp, lineHeight = if (s.rtl) 60.sp else 56.sp,
                    shadow = Shadow(Color(0x8C00915C), Offset(6f, 4f), 0f)
                )
            )
            Para(s.ui("intro"))
            NightMap(st)
            if (st.save.night >= 2) BasicText(s.ui("mapHint"), Modifier.padding(bottom = 16.dp), style = TextStyle(color = pal.inkSoft, fontFamily = f.ui, fontSize = 13.sp, lineHeight = 18.sp))
            Btn(st.startLabel()) { st.onStartPressed() }
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Btn(s.ui("how"), true, Modifier.weight(1f)) { st.openSheet("how") }
                Btn(s.ui("seeBoard"), true, Modifier.weight(1f)) { st.openSheet("pano") }
            }
            Btn(s.ui("stamps"), true, Modifier.padding(top = 10.dp)) { st.openSheet("pul") }
            LangSeg(st, Modifier.padding(top = 18.dp).fillMaxWidth())
        }
        Box(Modifier.align(Alignment.TopEnd).padding(top = 18.dp, end = 20.dp)) { StampFace(StampD("light", "green", "5"), 1f) }
    }
}

@Composable
fun Para(t: String, modifier: Modifier = Modifier, color: Color? = null, hand: Boolean = false) {
    val pal = LocalPal.current; val f = LocalFonts.current
    BasicText(
        t, modifier.padding(bottom = 20.dp).widthIn(max = 340.dp),
        style = if (hand) TextStyle(color = pal.blue, fontFamily = f.hand, fontSize = (24 * f.handScale).sp, lineHeight = (29 * f.handScale).sp)
        else TextStyle(color = color ?: pal.inkSoft, fontFamily = f.ui, fontSize = 16.sp, lineHeight = 24.sp)
    )
}

@Composable
private fun NightMap(st: AppState) {
    val pal = LocalPal.current; val f = LocalFonts.current; val s = st.s
    Column(Modifier.padding(top = 4.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(1 to s.ui("part1"), 6 to s.ui("part2")).forEach { (a, h) ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BasicText(h, style = TextStyle(color = pal.greenInk, fontFamily = f.ui, fontWeight = FontWeight.Bold, fontSize = 13.sp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (a until a + 5).forEach { n ->
                        val done = n < st.save.night; val cu = n == st.save.night; val b = st.save.best[n] ?: 0
                        val gold = st.save.stamps[n] == 2; val locked = n > st.save.night
                        val shape = RoundedCornerShape(6.dp)
                        Column(
                            Modifier.weight(1f).heightIn(min = 48.dp).alpha(if (locked) .42f else 1f).clip(shape)
                                .background(if (cu) pal.greenBtn else if (done) Color(0x1700915C) else Color.Transparent)
                                .then(if (locked) Modifier.drawBehind {
                                    drawRoundRect(Color(0x33072146), cornerRadius = CornerRadius(6.dp.toPx()), style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))))
                                } else Modifier.border(1.5.dp, if (cu) pal.greenBtn else if (done) Color(0x6100915C) else Color(0x33072146), shape))
                                .then(if (gold) Modifier.border(2.dp, pal.gold, shape) else Modifier)
                                .clickable(enabled = !locked) { if (n == st.save.night) st.onStartPressed() else st.startNight(n) }
                                .semantics { contentDescription = s.nightN(n) + if (done && b > 0) ", " + s.marksN(b) else "" }
                                .padding(2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                        ) {
                            BasicText("$n", style = TextStyle(color = if (cu) Color.White else pal.ink, fontFamily = f.ui, fontWeight = FontWeight(750), fontSize = 17.sp))
                            if (done && b > 0) Row(Modifier.padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                repeat(3) { i -> Box(Modifier.size(6.dp).clip(CircleShape).border(1.5.dp, pal.green, CircleShape).background(if (i < b) pal.green else Color.Transparent)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EndCard(st: AppState) {
    val pal = LocalPal.current; val f = LocalFonts.current; val s = st.s
    val e = st.lastEnd ?: return
    PaperCard {
        BasicText(s.ui("morning"), Modifier.padding(bottom = 10.dp), style = TextStyle(color = pal.ink, fontFamily = f.cond, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 36.sp, shadow = Shadow(Color(0x7300915C), Offset(4f, 4f), 0f)))
        Row(Modifier.padding(top = 4.dp, bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { i ->
                Canvas(Modifier.size(46.dp)) {
                    val c = if (i < e.rate) pal.green else Color(0x2E072146); val u = size.width / 50f
                    drawCircle(c, 20 * u, style = Stroke(3 * u)); drawCircle(c, 14 * u, style = Stroke(3 * u))
                }
            }
        }
        Para((if (e.miss == 0 && !e.burnt) s.perfect(e.ok) else s.stats(e.ok, e.miss)) + if (e.burnt) " " + s.ui("burnt") else "")
        if (e.n == 5) Para(s.ending, hand = true)
        if (e.n == 10) Para(s.ending2(st.save.endIdx()), hand = true)
        if (e.n == 5) Para(s.ui("part2Open"))
        if (e.rewardN != null) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 18.dp).drawBehind {
                    drawRoundRect(Color(0x47072146), cornerRadius = CornerRadius(4.dp.toPx()), style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))))
                }.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                StampFace(PSTAMPS[e.rewardN - 1], 1f, e.rewardGold)
                BasicText(
                    (if (e.rewardUp) s.goldStamp(e.rewardN) else s.newStamp(e.rewardN)) + (e.ink?.let { "\n" + s.newInk(s.inks[it]) } ?: ""),
                    style = TextStyle(color = pal.ink, fontFamily = f.ui, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp)
                )
            }
        }
        Btn(st.startLabel()) { st.startNight(null) }
        Btn(s.ui("seeBoard"), true, Modifier.padding(top = 10.dp)) { st.openSheet("pano") }
    }
}

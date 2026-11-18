package com.okuyanstudio.gecepostasi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun Sheets(st: AppState) {
    val open = st.sheet
    var shown by remember { mutableStateOf<String?>(null) }
    if (open != null) shown = open
    val scrimA by animateFloatAsState(if (open != null) 1f else 0f, tween(250), label = "scrim")
    Box(Modifier.fillMaxSize()) {
        if (scrimA > 0f) Box(
            Modifier.fillMaxSize().alpha(scrimA).background(Color(0x73000000))
                .clickable(remember { MutableInteractionSource() }, null) { st.closeSheet() }
        )
        AnimatedVisibility(
            open != null, Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(320, easing = EaseOutBack)) { it }, exit = slideOutVertically(tween(260)) { it }
        ) {
            val id = shown ?: return@AnimatedVisibility
            SheetFrame(st, id)
        }
    }
}

@Composable
private fun SheetFrame(st: AppState, id: String) {
    val pal = LocalPal.current; val f = LocalFonts.current; val s = st.s
    var dy by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(id) { dy = 0f }
    val density = LocalDensity.current.density
    val title = when (id) { "rehber" -> s.ui("rehber"); "pano" -> s.ui("pano"); "ayar" -> s.ui("settings"); "how" -> s.ui("how"); else -> s.ui("stamps") }
    Column(
        Modifier.fillMaxWidth().widthIn(max = 560.dp).fillMaxHeight(.84f)
            .windowInsetsPadding(WindowInsets.statusBars)
            .graphicsLayer { translationY = dy }
            .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)).background(pal.paper)
            .clickable(remember { MutableInteractionSource() }, null) {}
    ) {
        Column(
            Modifier.fillMaxWidth().pointerInput(id) {
                detectVerticalDragGestures(
                    onDragEnd = { if (dy > 80 * density) st.closeSheet() else dy = 0f },
                    onDragCancel = { dy = 0f }
                ) { ch, d -> ch.consume(); dy = maxOf(0f, dy + d) }
            }.padding(start = 20.dp, end = 20.dp, top = 8.dp)
        ) {
            Box(Modifier.padding(top = 6.dp, bottom = 14.dp).align(Alignment.CenterHorizontally).size(44.dp, 5.dp).clip(CircleShape).background(Color(0x33072146)))
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                BasicText(title, Modifier.weight(1f), style = TextStyle(color = pal.ink, fontFamily = f.cond, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp))
                Box(
                    Modifier.heightIn(min = 38.dp).clip(CircleShape).border(1.5.dp, Color(0x40072146), CircleShape).clickable { st.closeSheet() }.padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) { BasicText(s.ui("close"), style = TextStyle(color = pal.ink, fontFamily = f.ui, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)) }
            }
        }
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
        ) {
            when (id) {
                "rehber" -> Rehber(st)
                "pano" -> Pano(st)
                "ayar" -> Ayar(st)
                "how" -> How(st)
                else -> Pul(st)
            }
        }
    }
}

@Composable
private fun Sub(t: String) {
    val pal = LocalPal.current; val f = LocalFonts.current
    BasicText(t, Modifier.padding(bottom = 8.dp), style = TextStyle(color = pal.inkSoft, fontFamily = f.ui, fontSize = 14.5.sp, lineHeight = 21.sp))
}

@Composable
private fun Street(t: String) {
    val pal = LocalPal.current; val f = LocalFonts.current
    BasicText(t, Modifier.padding(top = 18.dp, bottom = 4.dp), style = TextStyle(color = pal.greenInk, fontFamily = f.ui, fontWeight = FontWeight(750), fontSize = 15.sp))
}

private fun Modifier.rowLine() = drawBehind { drawLine(Color(0x1F072146), Offset(0f, size.height), Offset(size.width, size.height), 1f) }

@Composable
private fun Rehber(st: AppState) {
    val pal = LocalPal.current; val f = LocalFonts.current; val s = st.s
    var q by remember { mutableStateOf("") }
    Sub(s.ui("rehberSub"))
    Box(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp).heightIn(min = 46.dp).clip(RoundedCornerShape(8.dp))
            .background(pal.note).border(1.5.dp, Color(0x38072146), RoundedCornerShape(8.dp)).padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (q.isEmpty()) BasicText(s.ui("search"), style = TextStyle(color = pal.inkSoft.copy(alpha = .7f), fontFamily = f.ui, fontSize = 16.sp))
        BasicTextField(q, { q = it }, Modifier.fillMaxWidth().semantics { contentDescription = s.ui("search") }, singleLine = true,
            textStyle = TextStyle(color = pal.ink, fontFamily = f.ui, fontSize = 16.sp), cursorBrush = SolidColor(pal.green),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search))
    }
    val nq = Rules.norm(q)
    var any = false
    KEYS.forEach { k ->
        val ps = PIDS.filter { HOME.getValue(it).first == k && Rules.shown(it, st.save.night) && (nq.isEmpty() || Rules.norm(s.person(it).first + " " + s.person(it).second).contains(nq)) }
        if (ps.isNotEmpty()) {
            any = true
            Street(s.street(k))
            ps.forEach { p ->
                val (nm, role) = s.person(p)
                Row(Modifier.fillMaxWidth().rowLine().padding(vertical = 9.dp), verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        BasicText(nm, style = TextStyle(color = pal.ink, fontFamily = f.ui, fontWeight = FontWeight.SemiBold, fontSize = 16.sp))
                        BasicText(role, style = TextStyle(color = pal.inkSoft, fontFamily = f.ui, fontSize = 13.sp))
                    }
                    BasicText(s.addr("", HOME.getValue(p).second).trim(' ', '،'), style = TextStyle(color = pal.inkSoft, fontFamily = f.ui, fontSize = 13.sp))
                }
            }
        }
    }
    if (!any) BasicText(s.ui("noMatch"), Modifier.padding(vertical = 22.dp), style = TextStyle(color = pal.inkSoft, fontFamily = f.ui, fontSize = 15.sp, lineHeight = 22.sp))
}

@Composable
private fun Pano(st: AppState) {
    val pal = LocalPal.current; val f = LocalFonts.current; val s = st.s
    Sub(s.ui("panoSub"))
    val list = STORY.filter { it.id in st.save.board }
    if (list.isEmpty()) {
        BasicText(s.ui("panoEmpty"), Modifier.padding(vertical = 22.dp), style = TextStyle(color = pal.inkSoft, fontFamily = f.ui, fontSize = 15.sp, lineHeight = 22.sp)); return
    }
    var i = 0
    @Composable fun note(t: String, sm: String) {
        val r = (if (i % 2 == 1) 1 else -1) * (0.6f + (i % 3) * .5f); i++
        Box(Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Column(Modifier.fillMaxWidth().rotate(r).shadow(6.dp).background(pal.note).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)) {
                BasicText(t, Modifier.padding(bottom = 8.dp), style = TextStyle(color = pal.blue, fontFamily = f.hand, fontSize = (23 * f.handScale).sp, lineHeight = (26 * f.handScale).sp))
                BasicText(sm, style = TextStyle(color = pal.inkSoft, fontFamily = f.ui, fontSize = 12.5.sp))
            }
            Box(Modifier.align(Alignment.TopCenter).padding(top = 0.dp).size(12.dp).shadow(2.dp, CircleShape).clip(CircleShape).background(pal.green))
        }
    }
    val e = st.save.endIdx()
    listOf(Triple(1..5, s.ui("part1"), "s10" to s.ending), Triple(6..10, s.ui("part2"), "w10" to s.ending2(e))).forEach { (rng, h, last) ->
        val l = list.filter { it.night in rng }
        if (l.isNotEmpty()) {
            Street(h)
            l.forEach { sd -> note(s.storyText(sd.id, e), "${s.storySign(sd.id)}, ${s.nightN(sd.night)}") }
            if (last.first in st.save.board && st.save.night > rng.last) note(last.second, s.ui("morningNote"))
        }
    }
}

@Composable
private fun Switch(on: Boolean, desc: String, onClick: () -> Unit) {
    val pal = LocalPal.current
    val x by animateFloatAsState(if (on) 1f else 0f, tween(200), label = "sw")
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(
        Modifier.size(52.dp, 30.dp).clip(CircleShape).background(if (on) pal.greenBtn else Color(0x33072146))
            .clickable(onClick = onClick).semantics { role = Role.Switch; contentDescription = desc; stateDescription = if (on) "on" else "off" }
    ) {
        Box(Modifier.padding(3.dp).graphicsLayer { translationX = (if (rtl) -1 else 1) * x * 22.dp.toPx() }.size(24.dp).shadow(2.dp, CircleShape).clip(CircleShape).background(Color.White))
    }
}

@Composable
private fun Ayar(st: AppState) {
    val pal = LocalPal.current; val f = LocalFonts.current; val s = st.s; val sv = st.save
    val txt = TextStyle(color = pal.ink, fontFamily = f.ui, fontSize = 16.sp)
    @Composable fun row(label: String, sub: String? = null, on: Boolean, toggle: () -> Unit) {
        Row(Modifier.fillMaxWidth().rowLine().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                BasicText(label, style = txt)
                if (sub != null) BasicText(sub, Modifier.padding(top = 3.dp), style = TextStyle(color = pal.inkSoft, fontFamily = f.ui, fontSize = 13.sp, lineHeight = 18.sp))
            }
            Switch(on, label, toggle)
        }
    }
    Column(Modifier.fillMaxWidth().rowLine().padding(vertical = 14.dp)) {
        BasicText(s.ui("lang"), Modifier.padding(bottom = 10.dp), style = txt)
        LangSeg(st, Modifier.fillMaxWidth())
    }
    row(s.ui("music"), on = sv.music) { sv.music = !sv.music; sv.persist(); st.syncAudio() }
    row(s.ui("sound"), on = sv.sound) { sv.sound = !sv.sound; sv.persist(); st.syncAudio() }
    row(s.ui("vib"), on = sv.vib) { sv.vib = !sv.vib; sv.persist(); st.syncAudio(); st.sound.buzz(15) }
    row(s.ui("hard"), s.ui("hardSub"), sv.hard) { sv.hard = !sv.hard; sv.persist() }
    @Composable fun link(label: String, go: () -> Unit) {
        Row(Modifier.fillMaxWidth().rowLine().clickable(onClick = go).padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            BasicText(label, Modifier.weight(1f), style = txt.copy(fontWeight = FontWeight.SemiBold))
            val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
            Canvas(Modifier.size(18.dp).scale(if (rtl) -1f else 1f, 1f)) { drawIcon(Icons.chev, pal.ink, 2f) }
        }
    }
    link(s.ui("stamps")) { st.openSheet("pul") }
    link(s.ui("how")) { st.openSheet("how") }
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(armed) { if (armed) { delay(3000); armed = false } }
    Box(
        Modifier.padding(top = 18.dp).fillMaxWidth().heightIn(min = 46.dp).clip(RoundedCornerShape(6.dp))
            .border(1.5.dp, Color(0x4D072146), RoundedCornerShape(6.dp))
            .clickable { if (!armed) armed = true else { armed = false; st.reset() } },
        contentAlignment = Alignment.Center
    ) { BasicText(if (armed) s.ui("resetSure") else s.ui("reset"), style = txt.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp)) }
}

@Composable
private fun Demo(i: Int, st: AppState) {
    val pal = LocalPal.current; val f = LocalFonts.current; val s = st.s
    val hand = TextStyle(fontFamily = f.hand, fontSize = (17 * f.handScale).sp, lineHeight = (19 * f.handScale).sp, color = pal.ink)
    val sel = hand.copy(color = pal.greenInk, fontWeight = FontWeight.Bold)
    val box = Modifier.size(96.dp, 66.dp)
    @Composable fun paper(content: @Composable () -> Unit) {
        Column(box.rotate(-2f).shadow(5.dp, RoundedCornerShape(3.dp)).background(pal.note, RoundedCornerShape(3.dp)).padding(horizontal = 9.dp, vertical = 8.dp), verticalArrangement = Arrangement.Center) { content() }
    }
    @Composable fun line(t: String, st2: TextStyle, under: Boolean = false) {
        BasicText(t, if (under) Modifier.drawBehind { drawLine(pal.green, Offset(0f, size.height), Offset(size.width, size.height), 2.dp.toPx()) } else Modifier, style = st2, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    val small = TextStyle(fontFamily = f.ui, fontSize = 11.sp, fontWeight = FontWeight(650), color = pal.ink)
    when (i) {
        0 -> paper { line(s.person("nesrin").first, hand); line(s.street("liman"), sel, true) }
        1 -> Column(box.clip(RoundedCornerShape(3.dp)).background(pal.blue).padding(6.dp)) {
            Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(2.dp)).background(pal.desk)) {
                Box(Modifier.align(Alignment.TopCenter).padding(top = 6.dp).size(36.dp, 12.dp).rotate(-4f).background(pal.note))
            }
            BasicText(s.street("liman"), Modifier.padding(top = 4.dp).fillMaxWidth().background(pal.paper, RoundedCornerShape(2.dp)).padding(horizontal = 3.dp, vertical = 2.dp), style = small, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        2 -> Box(box.clip(RoundedCornerShape(3.dp)).background(pal.desk), contentAlignment = Alignment.Center) {
            BasicText(s.ui("rehber"), Modifier.border(1.5.dp, Color(0x66E9EEF5), CircleShape).padding(horizontal = 12.dp, vertical = 5.dp), style = TextStyle(fontFamily = f.ui, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = pal.onDesk), maxLines = 1)
        }
        3 -> Row(box.clip(RoundedCornerShape(3.dp)).background(pal.desk).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(26.dp, 32.dp)) { drawRect(Color(0x8CE9EEF5), style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))) }
            Spacer(Modifier.width(8.dp))
            BasicText(s.ui("iade"), style = TextStyle(fontFamily = f.ui, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = pal.onDesk, lineHeight = 14.sp))
        }
        4 -> paper { line(s.street("yokus"), hand.copy(color = pal.inkSoft, textDecoration = TextDecoration.LineThrough)); line(s.street("liman"), sel, true) }
        5 -> paper {
            BasicText("—G.", Modifier.rotate(-3f).background(pal.yellow).padding(horizontal = 7.dp, vertical = 3.dp), style = hand.copy(fontSize = (15 * f.handScale).sp, color = pal.yellowInk))
            line(s.street("carsi"), sel, true)
        }
        6 -> Box(box.rotate(2f).clip(RoundedCornerShape(3.dp)).background(Color(0xFFC9A574))) {
            Box(Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 7.dp).size(22.dp, 26.dp).background(pal.green), contentAlignment = Alignment.Center) {
                BasicText("25", style = TextStyle(fontFamily = f.ui, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = pal.paper))
            }
            Box(Modifier.align(Alignment.BottomStart).padding(start = 7.dp, bottom = 7.dp).size(54.dp, 16.dp).background(pal.note))
        }
        7 -> paper { line(Rules.mask(s.person("nesrin").first), hand.copy(color = Color(0xFF4F6280))); line("≈≈≈≈≈", sel.copy(color = Color(0xFF4F6280)), true) }
        8 -> paper { line(s.person("ayla").first, hand); line(s.no(3), sel, true) }
        else -> Box(box.rotate(-2f).background(pal.yellow).padding(8.dp), contentAlignment = Alignment.Center) {
            BasicText(s.ui("notice"), style = TextStyle(fontFamily = f.ui, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = pal.yellowInk))
        }
    }
}

@Composable
private fun How(st: AppState) {
    val pal = LocalPal.current; val f = LocalFonts.current; val s = st.s
    Sub(s.ui("howSub"))
    val unlock = intArrayOf(1, 1, 2, 3, 4, 6, 6, 7, 8, 9)
    s.steps.forEachIndexed { i, (h, p) ->
        val locked = unlock[i] > st.save.night
        Row(Modifier.fillMaxWidth().rowLine().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.alpha(if (locked) .35f else 1f)) { Demo(i, st) }
            Column(Modifier.weight(1f)) {
                BasicText(h, Modifier.padding(bottom = 3.dp), style = TextStyle(color = if (locked) pal.inkSoft else pal.ink, fontFamily = f.ui, fontWeight = FontWeight.Bold, fontSize = 16.5.sp))
                BasicText(p, style = TextStyle(color = pal.inkSoft, fontFamily = f.ui, fontSize = 14.5.sp, lineHeight = 21.sp))
                if (locked) BasicText(s.lock(unlock[i]), Modifier.padding(top = 5.dp), style = TextStyle(color = pal.greenInk, fontFamily = f.ui, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp))
            }
        }
    }
    BasicText(s.ui("howFoot"), Modifier.padding(top = 16.dp, bottom = 18.dp), style = TextStyle(color = pal.inkSoft, fontFamily = f.ui, fontSize = 14.5.sp, lineHeight = 22.sp))
    Btn(if (st.pendingStart) s.ui("gotItStart") else s.ui("gotIt")) { st.closeSheet() }
}

@Composable
private fun Pul(st: AppState) {
    val pal = LocalPal.current; val f = LocalFonts.current; val s = st.s
    Sub(s.ui("stampsSub"))
    Column(Modifier.padding(top = 14.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PSTAMPS.chunked(5).forEachIndexed { r, row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEachIndexed { c, p ->
                    val n = r * 5 + c + 1; val lv = st.save.stamps[n] ?: 0
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (lv > 0) StampFace(p, .9f, lv == 2) else StampView(null, false, .9f)
                        BasicText(s.nightN(n), style = TextStyle(color = pal.inkSoft, fontFamily = f.ui, fontSize = 12.sp), maxLines = 1)
                    }
                }
            }
        }
    }
    Street(s.ui("ink"))
    val c = st.save.stampCount
    Row(Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(8.dp)).background(Color(0x14072146)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        INKS.forEachIndexed { i, ink ->
            val open = c >= ink.need; val on = st.save.ink == ink.id
            Row(
                Modifier.weight(1f).heightIn(min = 38.dp).alpha(if (open) 1f else .5f)
                    .then(if (on) Modifier.shadow(2.dp, RoundedCornerShape(5.dp)).background(pal.paper, RoundedCornerShape(5.dp)) else Modifier)
                    .clip(RoundedCornerShape(5.dp)).clickable(enabled = open) { st.save.ink = ink.id; st.save.persist() }.padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(Color(ink.color)))
                Spacer(Modifier.width(6.dp))
                BasicText(if (open) s.inks[i] else s.inkLock(ink.need), style = TextStyle(color = if (on) pal.ink else pal.inkSoft, fontFamily = f.ui, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp), maxLines = 1)
            }
        }
    }
}

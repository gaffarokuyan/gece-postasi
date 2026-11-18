package com.okuyanstudio.gecepostasi

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot

val EaseOutBack = CubicBezierEasing(.2f, .8f, .2f, 1f)
val EaseFly = CubicBezierEasing(.55f, 0f, .8f, .35f)

/** The physical desk: letter position, drag, postmark and fly-to-slot animations. */
class Table(private val st: AppState, private val scope: CoroutineScope, private val dp: Float) {
    companion object { /** for store screenshots: show letters without the deal animation */ var instant = false }
    val ox = Animatable(0f); val oy = Animatable(0f); val rot = Animatable(0f); val scl = Animatable(1f); val alpha = Animatable(1f)
    var cardCoords: LayoutCoordinates? = null
    var stampCoords: LayoutCoordinates? = null
    var slotCoords: LayoutCoordinates? = null
    val cubbies = HashMap<String, Rect>()
    var hot by mutableStateOf<String?>(null)
    var okKey by mutableStateOf<String?>(null)
    val shake = mutableStateMapOf<String, Int>()
    var pm by mutableStateOf<String?>(null)
    var pmPos by mutableStateOf(Offset.Zero)
    val pmAnim = Animatable(0f)
    private var dragRoot = Offset.Zero
    private var dragging = false

    suspend fun dealIn() {
        pm = null; pmAnim.snapTo(0f)
        if (instant) { ox.snapTo(0f); oy.snapTo(0f); rot.snapTo(0f); scl.snapTo(1f); alpha.snapTo(1f); return }
        ox.snapTo(-16 * dp); oy.snapTo(-46 * dp); rot.snapTo(-6f); scl.snapTo(1f); alpha.snapTo(0f)
        coroutineScope {
            val t = tween<Float>(340, easing = EaseOutBack)
            launch { ox.animateTo(0f, t) }; launch { oy.animateTo(0f, t) }; launch { rot.animateTo(0f, t) }; launch { alpha.animateTo(1f, t) }
        }
    }

    private fun cubbyAt(p: Offset) = cubbies.entries.firstOrNull { it.value.contains(p) }?.key

    fun dragStart(local: Offset) {
        if (st.busy || st.cur == null) { dragging = false; return }
        dragging = true; st.syncAudio()
        dragRoot = cardCoords?.takeIf { it.isAttached }?.localToRoot(local) ?: Offset.Zero
    }
    fun drag(d: Offset) {
        if (!dragging) return
        dragRoot += d
        scope.launch { ox.snapTo(ox.value + d.x); oy.snapTo(oy.value + d.y); rot.snapTo(ox.value * .04f / dp) }
        hot = cubbyAt(dragRoot)
    }
    fun dragEnd() {
        if (!dragging) return
        dragging = false; hot = null
        val key = cubbyAt(dragRoot)
        val d = hypot(ox.value, oy.value) / dp
        if (d > 12 && key != null) sortTo(key)
        else scope.launch {
            val t = tween<Float>(260, easing = EaseOutBack)
            launch { ox.animateTo(0f, t) }; launch { oy.animateTo(0f, t) }; launch { rot.animateTo(0f, t) }
        }
    }

    fun sortTo(key: String) {
        if (!st.canSort()) return
        st.busy = true
        scope.launch {
            val snd = st.sound
            if (st.isRight(key)) {
                snd.stamp(); snd.buzz(20)
                placePostmark()
                pm = if (key == "iade") st.s.pmRet else st.s.pmOk
                pmAnim.snapTo(0f); pmAnim.animateTo(1f, tween(150, easing = EaseIn))
                delay(140); snd.paper()
                fly(key)
                okKey = key
                scope.launch { delay(420); if (okKey == key) okKey = null }
                st.commit(key)
            } else {
                snd.err(); snd.buzz(25, 40, 25)
                shake[key] = (shake[key] ?: 0) + 1
                val x0 = ox.value
                coroutineScope {
                    launch {
                        ox.animateTo(0f, keyframes {
                            durationMillis = 400
                            x0 at 0; -10 * dp at 100; 8 * dp at 200; -4 * dp at 300
                        })
                    }
                    launch { oy.animateTo(0f, tween(400)) }; launch { rot.animateTo(0f, tween(400)) }
                }
                st.miss(); st.busy = false
            }
        }
    }

    private fun placePostmark() {
        val c = cardCoords ?: return; val sCo = stampCoords ?: return
        if (!c.isAttached || !sCo.isAttached) return
        val p = c.localPositionOf(sCo, Offset.Zero)
        val k = (c.size.width / dp / 400f).coerceIn(.72f, 1.1f)
        val cx = p.x + sCo.size.width / 2f - 44 * k * dp
        val left = cx.coerceIn(2 * dp, maxOf(2 * dp, c.size.width - 120 * k * dp))
        val top = maxOf(2 * dp, p.y - 12 * k * dp)
        pmPos = Offset(left, top)
    }

    private suspend fun fly(key: String) {
        val r = cubbies[key]; val slot = slotCoords
        if (r == null || slot == null || !slot.isAttached) return
        val sb = slot.localToRoot(Offset(slot.size.width / 2f, slot.size.height / 2f))
        val tx = r.center.x - sb.x; val ty = r.center.y - sb.y
        val s = minOf(.24f, r.width / slot.size.width)
        coroutineScope {
            val t = tween<Float>(380, easing = EaseFly)
            launch { ox.animateTo(tx, t) }; launch { oy.animateTo(ty, t) }; launch { scl.animateTo(s, t) }
            launch { rot.animateTo(5f, t) }; launch { alpha.animateTo(.15f, t) }
        }
    }
}

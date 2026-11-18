package com.okuyanstudio.gecepostasi

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/*
 * Sound effects and generative music, synthesised live (no audio files),
 * mirroring the Web Audio graph of the web version.
 */

private const val SR = 44100
private fun mtof(m: Double) = 440.0 * 2.0.pow((m - 69) / 12)

private class Biquad(private val type: Int, freq: Double, private val q: Double) {
    private var b0 = 0.0; private var b1 = 0.0; private var b2 = 0.0; private var a1 = 0.0; private var a2 = 0.0
    private var x1 = 0.0; private var x2 = 0.0; private var y1 = 0.0; private var y2 = 0.0
    init { set(freq) }
    fun set(freq: Double) {
        val w = 2 * PI * freq.coerceIn(20.0, SR / 2.2) / SR
        val alpha = sin(w) / (2 * q)
        val c = cos(w); val a0: Double
        when (type) {
            LP -> { b0 = (1 - c) / 2; b1 = 1 - c; b2 = (1 - c) / 2; a0 = 1 + alpha; a1 = -2 * c; a2 = 1 - alpha }
            HP -> { b0 = (1 + c) / 2; b1 = -(1 + c); b2 = (1 + c) / 2; a0 = 1 + alpha; a1 = -2 * c; a2 = 1 - alpha }
            else -> { b0 = alpha; b1 = 0.0; b2 = -alpha; a0 = 1 + alpha; a1 = -2 * c; a2 = 1 - alpha }
        }
        b0 /= a0; b1 /= a0; b2 /= a0; a1 /= a0; a2 /= a0
    }
    fun p(x: Double): Double {
        val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1; x1 = x; y2 = y1; y1 = y; return y
    }
    companion object { const val LP = 0; const val HP = 1; const val BP = 2 }
}

/** Piecewise-linear automation, like AudioParam ramps. */
private class Auto(var v: Double) {
    private val pts = ArrayList<Pair<Double, Double>>() // (time, value)
    fun cancel(t: Double) { val cur = at(t); pts.clear(); pts.add(t to cur); v = cur }
    fun set(t: Double, value: Double) { pts.clear(); pts.add(t to value); v = value }
    fun ramp(t: Double, value: Double) { pts.add(t to value) }
    fun hold(t: Double, value: Double) { pts.add(t to value) }
    fun at(t: Double): Double {
        if (pts.isEmpty()) return v
        if (t <= pts[0].first) return pts[0].second
        for (i in 1 until pts.size) {
            val (t1, v1) = pts[i]
            if (t <= t1) { val (t0, v0) = pts[i - 1]; return if (t1 == t0) v1 else v0 + (v1 - v0) * (t - t0) / (t1 - t0) }
        }
        return pts.last().second
    }
}

private abstract class Voice(val start: Long) {
    var dead = false
    /** returns sample; time in seconds since start */
    abstract fun s(t: Double): Double
}

private class ToneV(start: Long, val f: Double, val dur: Double, val g: Double, val tri: Boolean, val to: Double?) : Voice(start) {
    private var ph = 0.0
    override fun s(t: Double): Double {
        if (t >= dur) { dead = true; return 0.0 }
        val fr = if (to != null) f * (to / f).pow(t / dur) else f
        ph += fr / SR; ph -= ph.toInt()
        val o = if (tri) 1 - 4 * abs(ph - 0.5) else sin(2 * PI * ph)
        val gn = g * (0.0001 / g).pow(t / dur)
        return o * gn
    }
}

private class NoiseV(start: Long, val dur: Double, f: Double, val g: Double, q: Double) : Voice(start) {
    private val bp = Biquad(Biquad.BP, f, q)
    override fun s(t: Double): Double {
        if (t >= dur) { dead = true; return 0.0 }
        val e = (1 - t / dur).pow(2)
        return bp.p((Random.nextDouble() * 2 - 1) * e) * g
    }
}

private class BoxV(start: Long, m: Double, val v: Double) : Voice(start) {
    private val f = mtof(m); private var p1 = 0.0; private var p2 = 0.0; private var p3 = 0.0
    private val k = ln(0.0001 / v) / (2.2 - 0.006)
    override fun s(t: Double): Double {
        if (t >= 2.3) { dead = true; return 0.0 }
        val g = if (t < 0.006) v * t / 0.006 else if (t < 2.2) v * exp(k * (t - 0.006)) else 0.0
        p1 += f / SR; p2 += 3 * f / SR; p3 += 5.4 * f / SR
        return g * (sin(2 * PI * p1) + .16 * sin(2 * PI * p2) + .045 * sin(2 * PI * p3))
    }
}

private class PadV(start: Long, ms: List<Int>, val dur: Double, val v: Double) : Voice(start) {
    private val fs = ms.flatMapIndexed { i, m -> listOf(-4, 4).map { dt -> mtof(m + (dt * (if (i % 2 == 1) 1 else -1)) / 100.0) } }.toDoubleArray()
    private val ph = DoubleArray(fs.size)
    private val lp = Biquad(Biquad.LP, 900.0, 0.707)
    override fun s(t: Double): Double {
        if (t >= dur + .1) { dead = true; return 0.0 }
        val g = when { t < 2.5 -> v * t / 2.5; t < dur - 2.5 -> v; t < dur -> v * (dur - t) / 2.5; else -> 0.0 }
        var x = 0.0
        for (i in fs.indices) { ph[i] += fs[i] / SR; ph[i] -= ph[i].toInt(); x += 1 - 4 * abs(ph[i] - 0.5) }
        return lp.p(x) * g
    }
}

/** Filtered noise bed with slow LFO on level and optionally on cutoff. */
private class Layer(type: Int, val freq: Double, q: Double, val level: Double, val lr: Double, val ld: Double, val fr: Double, val fd: Double) {
    private val f = Biquad(type, freq, q); private var n = 0; private val off = Random.nextDouble() * 10
    fun s(t: Double): Double {
        if (fd > 0 && (n++ and 63) == 0) f.set(freq + fd * sin(2 * PI * fr * (t + off)))
        val g = level + (if (lr > 0) ld * sin(2 * PI * lr * (t + off)) else 0.0)
        return f.p(Random.nextDouble() * 2 - 1) * g
    }
}

class Sound(private val ctx: Context) {
    @Volatile var sfxOn = true
    @Volatile var musicOn = true
    @Volatile var vibOn = true

    private val cmds = ConcurrentLinkedQueue<() -> Unit>()
    private var track: AudioTrack? = null
    private var thread: Thread? = null
    @Volatile private var running = false
    @Volatile private var paused = false
    private var pos = 0L // samples rendered

    // audio-thread state
    private val sfx = ArrayList<Voice>()
    private val direct = ArrayList<Voice>()
    private val busV = ArrayList<Voice>()
    private val wetV = ArrayList<Voice>() // voices that also feed the delay (box notes)
    private var layers = listOf<Layer>()
    private val bus = Auto(0.0)
    private var theme: String? = null
    @Volatile private var wantTheme: String? = null
    private val delay = FloatArray(SR)
    private var dIdx = 0
    private var dLen = (SR * .33).toInt()
    private val dLp = Biquad(Biquad.LP, 2000.0, 0.707)
    private var next = 0.0; private var nextChord = 0.0; private var ci = 0; private var idx = 2; private var left = 0
    private var stopAt = -1.0

    private fun now() = pos.toDouble() / SR
    private fun at(t: Double) = (t * SR).toLong()

    fun ensure() {
        if (!sfxOn && !musicOn) return
        if (track != null) return
        val t = runCatching {
            val minBuf = AudioTrack.getMinBufferSize(SR, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT)
            AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(SR).setEncoding(AudioFormat.ENCODING_PCM_FLOAT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(maxOf(minBuf, 4096 * 4))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        }.getOrNull() ?: return
        track = t
        running = true
        t.play()
        thread = Thread({ loop() }, "gece-audio").also { it.priority = Thread.MAX_PRIORITY; it.start() }
    }

    fun pause() { paused = true; runCatching { track?.pause() } }
    fun resume() { if (track != null) { paused = false; runCatching { track?.play() } } }
    fun release() { running = false; thread?.join(300); runCatching { track?.release() }; track = null }

    fun syncMusic(themeName: String?) { wantTheme = if (musicOn) themeName else null }

    private fun post(f: () -> Unit) { cmds.add(f) }

    fun stamp() { ensure(); if (!sfxOn) return; post { val s = pos; sfx.add(ToneV(s, 120.0, .14, .55, false, 50.0)); sfx.add(NoiseV(s, .05, 700.0, .7, .7)) } }
    fun paper() { if (!sfxOn) return; post { sfx.add(NoiseV(pos, .24, 2800.0, .3, .6)) } }
    fun err() { ensure(); if (!sfxOn) return; post { sfx.add(ToneV(pos, 196.0, .12, .16, true, null)); sfx.add(ToneV(pos + at(.11), 147.0, .18, .16, true, null)) } }
    fun pin() { if (!sfxOn) return; post { sfx.add(ToneV(pos, 880.0, .18, .08, false, null)) } }

    fun sting(k: String) {
        if (!musicOn) return
        post {
            if (theme == null) return@post
            val t0 = now(); val t = t0 + .4
            bus.cancel(t0); bus.ramp(t, .3); bus.hold(t + 5, .3); bus.ramp(t + 8, 1.0)
            val sq = when (k) {
                "p1" -> listOf(74 to 0.0, 78 to .35, 81 to .7, 86 to 1.05, 81 to 1.9, 86 to 1.9)
                "w2" -> listOf(69 to 0.0, 73 to .4, 76 to .8, 81 to 1.2, 85 to 2.0, 81 to 2.0)
                "w1" -> listOf(69 to 0.0, 72 to .45, 76 to .9, 77 to 1.5, 76 to 2.3)
                "w0" -> listOf(76 to 0.0, 74 to .6, 72 to 1.2, 71 to 1.8, 69 to 2.6)
                else -> emptyList()
            }
            sq.forEach { (m, d) -> val v = BoxV(at(t + d), m.toDouble(), .13); direct.add(v) }
        }
    }

    private fun startTheme(th: String) {
        val t = now(); val w = th == "winter"; theme = th; stopAt = -1.0
        bus.set(t, 0.0); bus.ramp(t + 3, 1.0)
        dLen = (SR * (if (w) .42 else .33)).toInt(); delay.fill(0f)
        layers = if (w) listOf(
            Layer(Biquad.HP, 2600.0, .5, .016, 0.0, 0.0, 0.0, 0.0),
            Layer(Biquad.BP, 420.0, .9, .045, .07, .022, .04, 220.0)
        ) else listOf(
            Layer(Biquad.LP, 480.0, .7, .065, .085, .045, 0.0, 0.0),
            Layer(Biquad.BP, 1400.0, .6, .018, .12, .012, 0.0, 0.0)
        )
        next = t + 1.5; nextChord = t + .3; ci = 0; idx = 2; left = if (w) 4 else 6
    }

    private fun stopTheme() {
        if (theme == null) return
        val t = now(); bus.cancel(t); bus.ramp(t + 1.5, 0.0); stopAt = t + 1.6; theme = null
    }

    private fun tick() {
        if (wantTheme != theme && stopAt < 0) {
            if (theme != null) stopTheme() else if (wantTheme != null) startTheme(wantTheme!!)
        }
        if (stopAt >= 0 && now() >= stopAt) {
            busV.clear(); wetV.clear(); layers = emptyList(); stopAt = -1.0
            if (wantTheme != null) startTheme(wantTheme!!)
        }
        val th = theme ?: return
        val nowT = now(); val w = th == "winter"
        if (next < nowT) next = nowT + .1
        if (w) {
            if (nextChord < nowT) nextChord = nowT + .1
            while (nextChord < nowT + .6) {
                val ch = listOf(listOf(57, 60, 64), listOf(53, 57, 60), listOf(48, 55, 64), listOf(55, 59, 62))[ci++ % 4]
                busV.add(PadV(at(nextChord), listOf(ch[0] - 12) + ch, 9.0, .02)); nextChord += 8
            }
        }
        while (next < nowT + .6) {
            val sc = if (w) intArrayOf(69, 72, 74, 76, 79, 81, 84) else intArrayOf(74, 76, 78, 81, 83, 86, 88)
            if (left <= 0) {
                next += if (w) 6 + Random.nextDouble() * 6 else 4 + Random.nextDouble() * 5
                left = (if (w) 4 else 6) + Random.nextInt(if (w) 3 else 4)
                if (!w) wetV.add(BoxV(at(next), if (Random.nextBoolean()) 50.0 else 45.0, .045))
                continue
            }
            idx = (idx + intArrayOf(-2, -1, -1, 1, 1, 2)[Random.nextInt(6)]).coerceIn(0, sc.size - 1)
            wetV.add(BoxV(at(next), sc[idx].toDouble(), if (w) .045 else .06)); left--
            next += if (w) doubleArrayOf(1.0, 1.5, 2.0)[Random.nextInt(3)] else doubleArrayOf(.45, .45, .9, .9, 1.35)[Random.nextInt(5)]
        }
    }

    private fun mix(list: ArrayList<Voice>, p: Long): Double {
        var x = 0.0
        val it = list.iterator()
        while (it.hasNext()) {
            val v = it.next()
            if (p >= v.start) { x += v.s((p - v.start).toDouble() / SR); if (v.dead) it.remove() }
        }
        return x
    }

    private fun loop() {
        val n = 512; val buf = FloatArray(n)
        while (running) {
            if (paused) { Thread.sleep(50); continue }
            while (true) { val c = cmds.poll() ?: break; c() }
            tick()
            for (i in 0 until n) {
                val p = pos + i; val t = p.toDouble() / SR
                var out = mix(sfx, p)
                var b = 0.0
                for (l in layers) b += l.s(t)
                b += mix(busV, p)
                val wv = mix(wetV, p)
                val dv = mix(direct, p)
                b += wv
                // delay send (box notes)
                val rd = (dIdx - dLen + delay.size) % delay.size
                val y = dLp.p(delay[rd].toDouble())
                delay[dIdx] = (0.4 * (wv + dv) + y * 0.4).toFloat()
                dIdx = (dIdx + 1) % delay.size
                b += y
                out += b * bus.at(t) + dv
                buf[i] = out.coerceIn(-1.0, 1.0).toFloat()
            }
            pos += n
            val tr = track ?: break
            tr.write(buf, 0, n, AudioTrack.WRITE_BLOCKING)
        }
    }

    // ---- haptics ----
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= 31) (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        else @Suppress("DEPRECATION") (ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
    }
    fun buzz(vararg p: Long) {
        if (!vibOn) return
        val v = vibrator ?: return
        runCatching {
            if (p.size == 1) v.vibrate(VibrationEffect.createOneShot(p[0], VibrationEffect.DEFAULT_AMPLITUDE))
            else v.vibrate(VibrationEffect.createWaveform(longArrayOf(0) + p, -1))
        }
    }
}

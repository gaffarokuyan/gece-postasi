package com.okuyanstudio.gecepostasi

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** Saved progress and settings. Compose-observable; persisted as JSON in SharedPreferences. */
class SaveData(private val ctx: Context) {
    var night by mutableIntStateOf(1)
    val board = mutableStateListOf<String>()
    val choices = mutableStateMapOf<String, String>()
    val best = mutableStateMapOf<Int, Int>()
    val stamps = mutableStateMapOf<Int, Int>()
    var music by mutableStateOf(true)
    var sound by mutableStateOf(true)
    var vib by mutableStateOf(true)
    var hard by mutableStateOf(false)
    var ink by mutableStateOf("green")
    var seenHow by mutableStateOf(false)
    var lang by mutableStateOf(defaultLang())

    private fun prefs() = ctx.getSharedPreferences("gece", Context.MODE_PRIVATE)

    fun load() {
        val raw = runCatching { prefs().getString(KEY, null) }.getOrNull() ?: return
        runCatching {
            val o = JSONObject(raw)
            night = o.optInt("night", 1)
            board.clear(); o.optJSONArray("board")?.let { a -> for (i in 0 until a.length()) board.add(a.getString(i)) }
            choices.clear(); o.optJSONObject("choices")?.let { c -> c.keys().forEach { choices[it] = c.getString(it) } }
            best.clear(); o.optJSONObject("best")?.let { c -> c.keys().forEach { best[it.toInt()] = c.getInt(it) } }
            stamps.clear(); o.optJSONObject("stamps")?.let { c -> c.keys().forEach { stamps[it.toInt()] = c.getInt(it) } }
            music = o.optBoolean("music", true); sound = o.optBoolean("sound", true); vib = o.optBoolean("vib", true)
            hard = o.optBoolean("hard", false); ink = o.optString("ink", "green"); seenHow = o.optBoolean("seenHow", false)
            lang = o.optString("lang", lang).takeIf { it in Lang.ORDER } ?: lang
        }
    }

    fun persist() {
        val o = JSONObject()
        o.put("night", night)
        o.put("board", JSONArray(board.toList()))
        o.put("choices", JSONObject(choices.toMap() as Map<*, *>))
        o.put("best", JSONObject().also { j -> best.forEach { (k, v) -> j.put(k.toString(), v) } })
        o.put("stamps", JSONObject().also { j -> stamps.forEach { (k, v) -> j.put(k.toString(), v) } })
        o.put("music", music); o.put("sound", sound); o.put("vib", vib); o.put("hard", hard)
        o.put("ink", ink); o.put("seenHow", seenHow); o.put("lang", lang)
        runCatching { prefs().edit().putString(KEY, o.toString()).apply() }
    }

    /** Progress reset keeps stamps and preferences, like the web version. */
    fun resetProgress() {
        night = 1; board.clear(); choices.clear(); best.clear(); persist()
    }

    val stampCount get() = stamps.size
    fun endIdx(): Int {
        val k = CHOICES.count { choices[it] == "k" }
        return if (k == 0) 0 else if (k == 3) 2 else 1
    }

    companion object {
        const val KEY = "gece-postasi-v1"
        fun defaultLang(): String {
            val l = Locale.getDefault().language.lowercase()
            return when {
                l.startsWith("ar") -> "ar"
                l.startsWith("ku") || l.startsWith("kmr") -> "ku"
                l.startsWith("tr") -> "tr"
                else -> "en"
            }
        }
    }
}

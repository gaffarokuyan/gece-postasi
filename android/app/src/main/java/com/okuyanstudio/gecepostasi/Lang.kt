package com.okuyanstudio.gecepostasi

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** One language's texts, loaded from assets/i18n.json (same content as the web version). */
class Lang(val code: String, private val j: JSONObject) {
    val rtl = j.getString("dir") == "rtl"
    val name: String = j.getString("name")
    private val ui = j.getJSONObject("ui")
    private val streetsJ = j.getJSONObject("streets")
    private val peopleJ = j.getJSONObject("people")
    private val storiesJ = j.getJSONObject("stories")
    private val whyJ = j.getJSONObject("why")
    private val introJ = j.getJSONObject("intro")
    private val pm = j.getJSONObject("pm")

    fun ui(k: String): String = ui.optString(k, k)
    val title: String get() = ui("title").replace("<br>", "\n")
    fun street(k: String): String = streetsJ.getString(k)
    fun person(p: String): Pair<String, String> = peopleJ.getJSONArray(p).let { it.getString(0) to it.getString(1) }
    fun why(k: String): String = whyJ.optString(k, "")
    fun intro(n: Int): String = introJ.optString(n.toString(), "")
    val pmTown: String get() = pm.getString("town")
    val pmOk: String get() = pm.getString("ok")
    val pmRet: String get() = pm.getString("ret")
    val north: String get() = j.getString("north")
    val ending: String get() = j.getString("ending")
    fun ending2(i: Int): String = j.getJSONArray("ending2").getString(i)
    val lines: List<String> = strs(j.getJSONArray("lines"))
    val signs: List<String> = strs(j.getJSONArray("signs"))
    val senders: List<String> = strs(j.getJSONArray("senders"))
    val bills: List<String> = strs(j.getJSONArray("bills"))
    val unknown: List<String> = strs(j.getJSONArray("unknown"))
    val inks: List<String> = strs(j.getJSONArray("inks"))
    val steps: List<Pair<String, String>> = j.getJSONArray("steps").let { a ->
        (0 until a.length()).map { a.getJSONArray(it).let { s -> s.getString(0) to s.getString(1) } }
    }

    fun storyText(id: String, endIdx: Int): String {
        val s = storiesJ.getJSONObject(id)
        return if (s.has("texts")) s.getJSONArray("texts").getString(endIdx) else s.getString("text")
    }
    fun storySign(id: String): String = storiesJ.getJSONObject(id).getString("sign")
    fun storySender(id: String): String = storiesJ.getJSONObject(id).optString("sender", "")
    fun storyNote(id: String): String? = storiesJ.getJSONObject(id).optString("note", "").ifEmpty { null }
    fun storyLines(id: String): List<String> = strs(storiesJ.getJSONObject(id).getJSONArray("lines"))

    private fun t(k: String, vararg kv: Pair<String, Any>): String {
        var s = j.getString(k)
        kv.forEach { (a, b) -> s = s.replace("{$a}", b.toString()) }
        return s
    }
    private val en get() = code == "en"
    fun nightN(n: Int) = t("nightN", "n" to n)
    fun startN(n: Int) = t("startN", "n" to n)
    fun left(n: Int) = if (en && n == 1) "1 letter left" else t("left", "n" to n)
    fun putIn(s: String) = t("putIn", "s" to s)
    fun lock(n: Int) = t("lock", "n" to n)
    fun perfect(n: Int) = t("perfect", "n" to n)
    fun stats(o: Int, m: Int) = if (en && m == 1) "$o letters delivered. You reached for the wrong slot once." else t("stats", "o" to o, "m" to m)
    fun addr(s: String, n: Int) = t("addr", "s" to s, "n" to n)
    fun no(n: Int) = t("no", "n" to n)
    fun marksN(n: Int) = if (en && n == 1) "1 postmark" else t("marksN", "n" to n)
    fun moved(nm: String, a: String) = t("moved", "nm" to nm, "a" to a)
    fun inkLock(n: Int) = t("inkLock", "n" to n)
    fun newStamp(n: Int) = t("newStamp", "n" to n)
    fun goldStamp(n: Int) = t("goldStamp", "n" to n)
    fun newInk(x: String) = t("newInk", "x" to x)

    companion object {
        val ORDER = listOf("tr", "en", "ar", "ku")
        fun loadAll(ctx: Context): Map<String, Lang> {
            val txt = ctx.assets.open("i18n.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            return parse(txt)
        }
        fun parse(txt: String): Map<String, Lang> {
            val root = JSONObject(txt)
            return ORDER.associateWith { Lang(it, root.getJSONObject(it)) }
        }
        private fun strs(a: JSONArray) = (0 until a.length()).map { a.getString(it) }
    }
}

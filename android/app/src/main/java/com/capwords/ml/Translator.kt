package com.capwords.ml

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Both Chinese variants for one English word. */
data class Translation(val zhTw: String?, val zhCn: String?)

/**
 * Offline EN -> Chinese lookup. The full ~20k-word table (`assets/words.tsv`,
 * columns: english \t zh-TW \t zh-CN) is produced by tools/ in Phase 2 and loaded
 * lazily here. A small seed map covers the demo objects so the app is useful
 * before that table is bundled.
 */
class Translator(private val context: Context) {

    @Volatile
    private var table: Map<String, Translation>? = null

    private val seed: Map<String, Translation> = mapOf(
        "donut" to Translation("甜甜圈", "甜甜圈"),
        "doughnut" to Translation("甜甜圈", "甜甜圈"),
        "toast" to Translation("吐司", "吐司"),
        "bread" to Translation("麵包", "面包"),
        "forest" to Translation("森林", "森林"),
        "iced tea" to Translation("冰紅茶", "冰红茶"),
        "tea" to Translation("茶", "茶"),
        "clip" to Translation("夾子", "夹子"),
        "tape" to Translation("膠帶", "胶带"),
        "cup" to Translation("杯子", "杯子"),
        "mug" to Translation("馬克杯", "马克杯"),
        "milk" to Translation("牛奶", "牛奶"),
        "bagel" to Translation("貝果", "贝果"),
        "plate" to Translation("盤子", "盘子"),
        "palm tree" to Translation("棕櫚樹", "棕榈树"),
        "barrier" to Translation("柵欄", "栅栏"),
        "flower" to Translation("花", "花"),
        "plant" to Translation("植物", "植物"),
        "book" to Translation("書", "书"),
        "keyboard" to Translation("鍵盤", "键盘"),
        "bottle" to Translation("瓶子", "瓶子"),
    )

    suspend fun ensureLoaded() = withContext(Dispatchers.IO) {
        if (table != null) return@withContext
        val loaded = HashMap<String, Translation>()
        runCatching {
            context.assets.open("words.tsv").bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val cols = line.split('\t')
                    if (cols.size >= 3) {
                        loaded[cols[0].trim().lowercase()] =
                            Translation(cols[1].ifBlank { null }, cols[2].ifBlank { null })
                    }
                }
            }
            Log.i("Translator", "Loaded ${loaded.size} translations from words.tsv")
        }.onFailure {
            Log.i("Translator", "words.tsv not bundled yet; using seed map (${seed.size})")
        }
        table = loaded
    }

    fun translate(english: String): Translation? {
        val key = english.trim().lowercase()
        return table?.get(key) ?: seed[key]
    }
}

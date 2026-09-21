package com.aarvo.cart

import android.content.SharedPreferences
import org.json.JSONArray

class SaveForLaterStore(private val prefs: SharedPreferences, private val key: String = "aarvo_save_for_later_v1") {
    fun load(): Set<Int> = runCatching {
        val array = JSONArray(prefs.getString(key, "[]") ?: "[]")
        buildSet { for (i in 0 until array.length()) array.optInt(i).takeIf { it > 0 }?.let(::add) }
    }.getOrDefault(emptySet())

    fun toggle(productId: Int): Set<Int> {
        val next = load().toMutableSet()
        if (!next.add(productId)) next.remove(productId)
        persist(next)
        return next
    }

    fun remove(productId: Int): Set<Int> {
        val next = load().toMutableSet().also { it.remove(productId) }
        persist(next)
        return next
    }

    private fun persist(ids: Set<Int>) {
        prefs.edit().putString(key, JSONArray().apply { ids.sorted().forEach(::put) }.toString()).apply()
    }
}

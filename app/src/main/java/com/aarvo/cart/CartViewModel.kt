package com.aarvo.cart

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import com.aarvo.data.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class CartViewModel(
    private val prefs: SharedPreferences? = null,
    private val storageKey: String = "aarvo_cart_v1"
) : ViewModel() {
    private val _items = MutableStateFlow<List<Product>>(emptyList())
    val items: StateFlow<List<Product>> = _items.asStateFlow()

    init { loadSavedIds() }

    private fun loadSavedIds() {
        val saved = prefs?.getString(storageKey, null) ?: return
        runCatching {
            val array = JSONArray(saved)
            val ids = buildList {
                for (i in 0 until array.length()) {
                    val row = array.getJSONObject(i)
                    val id = row.optInt("productId")
                    val quantity = row.optInt("quantity")
                    if (id > 0 && quantity > 0) repeat(quantity) { add(id) }
                }
            }
            pendingIds = ids
        }
    }

    private var pendingIds: List<Int> = emptyList()

    fun restore(products: List<Product>) {
        if (pendingIds.isEmpty()) return
        val byId = products.associateBy { it.id }
        val restored = buildList {
            pendingIds.groupingBy { it }.eachCount().forEach { (id, quantity) ->
                val product = byId[id] ?: return@forEach
                repeat(quantity.coerceAtMost(product.stockQuantity)) { add(product) }
            }
        }
        pendingIds = emptyList()
        _items.value = restored
        persist()
    }

    private fun persist() {
        prefs?.edit()?.putString(storageKey, JSONArray().apply {
            distinctItems().forEach { product ->
                put(JSONObject().put("productId", product.id).put("quantity", quantity(product.id)))
            }
        }.toString())?.apply()
    }

    @Synchronized
    fun add(product: Product) {
        if (product.stockQuantity <= 0) return
        val currentQuantity = quantity(product.id)
        if (currentQuantity >= product.stockQuantity) return
        _items.value = _items.value + product
        persist()
    }

    @Synchronized
    fun remove(product: Product) {
        val index = _items.value.indexOfFirst { it.id == product.id }
        if (index >= 0) {
            _items.value = _items.value.toMutableList().also { it.removeAt(index) }
            persist()
        }
    }

    @Synchronized
    fun removeAll(productId: Int) {
        _items.value = _items.value.filterNot { it.id == productId }
        persist()
    }

    @Synchronized
    fun setQuantity(product: Product, requestedQuantity: Int) {
        val target = requestedQuantity.coerceIn(0, product.stockQuantity)
        val withoutProduct = _items.value.filterNot { it.id == product.id }
        _items.value = withoutProduct + List(target) { product }
        persist()
    }

    fun increment(product: Product) = add(product)
    fun decrement(product: Product) = remove(product)
    fun quantity(productId: Int): Int = _items.value.count { it.id == productId }
    fun distinctItems(): List<Product> = _items.value.distinctBy { it.id }

    // Each entry in _items represents one unit, so totals must include quantity.
    fun totalPaise(): Long = _items.value.sumOf { it.pricePaise }

    @Synchronized
    fun clear() {
        _items.value = emptyList()
        pendingIds = emptyList()
        prefs?.edit()?.remove(storageKey)?.apply()
    }
}

package com.aarvo.cart

import androidx.lifecycle.ViewModel
import com.aarvo.data.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CartViewModel : ViewModel() {
    private val _items = MutableStateFlow<List<Product>>(emptyList())
    val items: StateFlow<List<Product>> = _items.asStateFlow()

    @Synchronized
    fun add(product: Product) {
        if (product.stockQuantity <= 0) return
        val currentQuantity = quantity(product.id)
        if (currentQuantity >= product.stockQuantity) return
        _items.value = _items.value + product
    }

    @Synchronized
    fun remove(product: Product) {
        val index = _items.value.indexOfFirst { it.id == product.id }
        if (index >= 0) {
            _items.value = _items.value.toMutableList().also { it.removeAt(index) }
        }
    }

    @Synchronized
    fun removeAll(productId: Int) {
        _items.value = _items.value.filterNot { it.id == productId }
    }

    @Synchronized
    fun setQuantity(product: Product, requestedQuantity: Int) {
        val target = requestedQuantity.coerceIn(0, product.stockQuantity)
        val withoutProduct = _items.value.filterNot { it.id == product.id }
        _items.value = withoutProduct + List(target) { product }
    }

    fun increment(product: Product) = add(product)
    fun decrement(product: Product) = remove(product)
    fun quantity(productId: Int): Int = _items.value.count { it.id == productId }
    fun distinctItems(): List<Product> = _items.value.distinctBy { it.id }
    fun totalPaise(): Long = _items.value.sumOf { it.pricePaise }

    @Synchronized
    fun clear() {
        _items.value = emptyList()
    }
}

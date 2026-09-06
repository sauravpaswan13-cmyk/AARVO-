package com.aarvo.cart

import androidx.lifecycle.ViewModel
import com.aarvo.data.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CartViewModel : ViewModel() {
    private val _items = MutableStateFlow<List<Product>>(emptyList())
    val items: StateFlow<List<Product>> = _items.asStateFlow()

    fun add(product: Product) {
        if (product.stockQuantity <= 0) return
        val currentQuantity = _items.value.count { it.id == product.id }
        if (currentQuantity >= product.stockQuantity) return
        _items.value = _items.value + product
    }

    fun remove(product: Product) {
        val index = _items.value.indexOfFirst { it.id == product.id }
        if (index >= 0) {
            _items.value = _items.value.toMutableList().also { it.removeAt(index) }
        }
    }

    fun quantity(productId: Int): Int = _items.value.count { it.id == productId }

    fun clear() {
        _items.value = emptyList()
    }
}

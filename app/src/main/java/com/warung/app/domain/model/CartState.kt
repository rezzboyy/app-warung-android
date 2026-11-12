package com.warung.app.domain.model

data class CartState(
    val items: List<CartItem> = emptyList(),
    val discount: Double = 0.0
) {
    val totalQuantity: Int
        get() = items.sumOf { it.quantity }

    val subtotal: Double
        get() = items.sumOf { it.subtotal }

    val grandTotal: Double
        get() = (subtotal - discount).coerceAtLeast(0.0)

    val totalProfit: Double
        get() = items.sumOf { it.profit } - discount

    val isEmpty: Boolean
        get() = items.isEmpty()

    fun getItem(productId: Long): CartItem? {
        return items.find { it.product.id == productId }
    }
}

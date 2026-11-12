package com.warung.app.domain.model

import com.warung.app.data.local.entity.ProductEntity

data class CartItem(
    val product: ProductEntity,
    var quantity: Int = 1
) {
    val subtotal: Double
        get() = product.sellPrice * quantity

    val profit: Double
        get() = (product.sellPrice - product.buyPrice) * quantity

    fun hasAvailableStock(additionalQty: Int = 1): Boolean {
        return (quantity + additionalQty) <= product.stock
    }
}

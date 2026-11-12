package com.warung.app.domain.model

import com.warung.app.data.local.entity.ProductEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CartStateTest {

    private val sampleProduct1 = ProductEntity(
        id = 1,
        name = "Beras Ramos 5kg",
        buyPrice = 60000.0,
        sellPrice = 65000.0,
        stock = 10,
        unit = "Karung"
    )

    private val sampleProduct2 = ProductEntity(
        id = 2,
        name = "Minyak Goreng 1L",
        buyPrice = 14000.0,
        sellPrice = 16000.0,
        stock = 15,
        unit = "Pouch"
    )

    @Test
    fun testEmptyCartState() {
        val cart = CartState()
        assertTrue(cart.isEmpty)
        assertEquals(0, cart.totalQuantity)
        assertEquals(0.0, cart.subtotal, 0.001)
        assertEquals(0.0, cart.grandTotal, 0.001)
        assertEquals(0.0, cart.totalProfit, 0.001)
    }

    @Test
    fun testCartWithItems() {
        val item1 = CartItem(product = sampleProduct1, quantity = 2) // 65.000 * 2 = 130.000, profit = 5.000 * 2 = 10.000
        val item2 = CartItem(product = sampleProduct2, quantity = 3) // 16.000 * 3 = 48.000, profit = 2.000 * 3 = 6.000

        val cart = CartState(items = listOf(item1, item2))

        assertFalse(cart.isEmpty)
        assertEquals(5, cart.totalQuantity)
        assertEquals(178000.0, cart.subtotal, 0.001)
        assertEquals(178000.0, cart.grandTotal, 0.001)
        assertEquals(16000.0, cart.totalProfit, 0.001)
    }

    @Test
    fun testCartWithDiscount() {
        val item1 = CartItem(product = sampleProduct1, quantity = 1) // 65.000
        val cart = CartState(items = listOf(item1), discount = 5000.0)

        assertEquals(65000.0, cart.subtotal, 0.001)
        assertEquals(60000.0, cart.grandTotal, 0.001)
    }

    @Test
    fun testCartDiscountExceedingSubtotalClampedToZero() {
        val item1 = CartItem(product = sampleProduct2, quantity = 1) // 16.000
        val cart = CartState(items = listOf(item1), discount = 20000.0)

        assertEquals(0.0, cart.grandTotal, 0.001)
    }

    @Test
    fun testStockAvailabilityCheck() {
        val item = CartItem(product = sampleProduct1, quantity = 9) // stock is 10
        assertTrue(item.hasAvailableStock(1)) // 9 + 1 <= 10 -> true
        assertFalse(item.hasAvailableStock(2)) // 9 + 2 <= 10 -> false
    }
}

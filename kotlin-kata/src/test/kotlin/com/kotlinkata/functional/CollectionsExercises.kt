package com.kotlinkata.functional

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * # Exercises: Collections
 *
 * Replace each TODO() with your implementation.
 * All exercises use a realistic order/product domain.
 */
class CollectionsExercises {

    data class Product(val sku: String, val name: String, val price: Double, val category: String)
    data class Order(val id: String, val customerId: String, val items: List<OrderItem>, val status: String)
    data class OrderItem(val product: Product, val quantity: Int)

    private val electronics = Product("KBD-01", "Keyboard", 79.99, "Electronics")
    private val mouse = Product("MSE-01", "Mouse", 29.99, "Electronics")
    private val book1 = Product("BK-01", "Clean Code", 39.99, "Books")
    private val book2 = Product("BK-02", "Kotlin in Action", 49.99, "Books")
    private val headset = Product("HDT-01", "Headset", 149.99, "Electronics")

    private val orders = listOf(
        Order("ORD-001", "CUST-A", listOf(OrderItem(electronics, 1), OrderItem(mouse, 2)), "DELIVERED"),
        Order("ORD-002", "CUST-B", listOf(OrderItem(book1, 3), OrderItem(book2, 1)), "SHIPPED"),
        Order("ORD-003", "CUST-A", listOf(OrderItem(headset, 1)), "DELIVERED"),
        Order("ORD-004", "CUST-C", listOf(OrderItem(mouse, 1), OrderItem(book1, 1)), "PENDING"),
        Order("ORD-005", "CUST-B", listOf(OrderItem(electronics, 2)), "CANCELLED")
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Exercise 1: map and filter
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `map transforms each element`() {
        val prices = listOf(10.0, 20.0, 30.0, 40.0)

        // TODO: Apply a 10% discount to each price
        val discounted: List<Double> = TODO()

        assertThat(discounted).containsExactly(9.0, 18.0, 27.0, 36.0)
    }

    @Test
    fun `filter removes non-matching elements`() {
        // TODO: Get only delivered orders
        val delivered: List<Order> = TODO()

        assertThat(delivered).hasSize(2)
        assertThat(delivered.map { it.id }).containsExactlyInAnyOrder("ORD-001", "ORD-003")
    }

    @Test
    fun `get all product names from all orders`() {
        // TODO: Get a flat list of all product names across all orders (use flatMap)
        // HINT: orders.flatMap { order -> order.items.map { it.product.name } }
        val productNames: List<String> = TODO()

        assertThat(productNames).contains("Keyboard", "Mouse", "Clean Code", "Kotlin in Action", "Headset")
        assertThat(productNames.size).isGreaterThan(5)  // some appear multiple times
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Exercise 2: Aggregation
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `calculate total revenue from delivered orders`() {
        // TODO: Sum the total value (price * quantity for each item) of all DELIVERED orders
        fun Order.total(): Double = items.sumOf { it.product.price * it.quantity }

        val revenue: Double = TODO()

        // ORD-001: 79.99 * 1 + 29.99 * 2 = 139.97
        // ORD-003: 149.99 * 1 = 149.99
        assertThat(revenue).isCloseTo(289.96, org.assertj.core.data.Offset.offset(0.01))
    }

    @Test
    fun `find the most expensive product ordered`() {
        // TODO: Find the product with the highest price across all order items
        // HINT: use flatMap to get all items, then maxByOrNull
        val mostExpensive: Product? = TODO()

        assertThat(mostExpensive?.sku).isEqualTo("HDT-01")
        assertThat(mostExpensive?.price).isEqualTo(149.99)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Exercise 3: Grouping
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `group orders by status`() {
        // TODO: Group orders by their status field
        val byStatus: Map<String, List<Order>> = TODO()

        assertThat(byStatus["DELIVERED"]).hasSize(2)
        assertThat(byStatus["SHIPPED"]).hasSize(1)
        assertThat(byStatus["PENDING"]).hasSize(1)
        assertThat(byStatus["CANCELLED"]).hasSize(1)
    }

    @Test
    fun `count orders per customer`() {
        // TODO: Count how many orders each customer has
        val ordersPerCustomer: Map<String, Int> = TODO()

        assertThat(ordersPerCustomer["CUST-A"]).isEqualTo(2)
        assertThat(ordersPerCustomer["CUST-B"]).isEqualTo(2)
        assertThat(ordersPerCustomer["CUST-C"]).isEqualTo(1)
    }

    @Test
    fun `partition orders into active and inactive`() {
        val activeStatuses = setOf("PENDING", "SHIPPED", "CONFIRMED")

        // TODO: Partition into (active, inactive) using partition()
        val (active, inactive): Pair<List<Order>, List<Order>> = TODO()

        assertThat(active.map { it.id }).containsExactlyInAnyOrder("ORD-002", "ORD-004")
        assertThat(inactive.map { it.id }).containsExactlyInAnyOrder("ORD-001", "ORD-003", "ORD-005")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Exercise 4: Advanced Operations
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `get top 3 products by total quantity ordered`() {
        // TODO: Find the top 3 products by total quantity ordered across all orders
        // HINT: flatMap items, groupBy product.sku, sum quantities, sort desc, take 3
        val top3: List<String> = TODO()  // list of SKUs

        // Mouse ordered: 2 (ORD-001) + 1 (ORD-004) = 3
        // Electronics ordered: 1 (ORD-001) + 2 (ORD-005) = 3
        // Clean Code: 3 (ORD-002) + 1 (ORD-004) = 4 <-- most ordered
        assertThat(top3).contains("BK-01")  // most ordered
        assertThat(top3).hasSize(3)
    }

    @Test
    fun `calculate revenue by product category`() {
        // TODO: Calculate total revenue (price * qty) grouped by category
        // Only include DELIVERED orders
        fun Order.total(): Double = items.sumOf { it.product.price * it.quantity }

        val revenueByCategory: Map<String, Double> = TODO()

        assertThat(revenueByCategory["Electronics"]).isNotNull()
        assertThat(revenueByCategory["Books"]).isNull()  // no delivered book orders
    }

    @Test
    fun `find customers who ordered from multiple categories`() {
        // TODO: Find customer IDs who ordered products from at least 2 different categories
        // HINT: For each customer, get all their orders' items' categories, deduplicate, count
        val multiCategoryCustomers: Set<String> = TODO()

        // CUST-A: Electronics (keyboard, mouse, headset) = 1 category
        // CUST-B: Books + Electronics = 2 categories ✓
        // CUST-C: Electronics + Books = 2 categories ✓
        assertThat(multiCategoryCustomers).containsExactlyInAnyOrder("CUST-B", "CUST-C")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Exercise 5: Fold/Reduce
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `build an inventory map using fold`() {
        val orderItems = listOf(
            "SKU-1" to 5,
            "SKU-2" to 3,
            "SKU-1" to 2,  // same SKU, should be added
            "SKU-3" to 10,
            "SKU-2" to 1   // same SKU, should be added
        )

        // TODO: Build a Map<String, Int> that sums quantities per SKU using fold
        val inventory: Map<String, Int> = TODO()

        assertThat(inventory["SKU-1"]).isEqualTo(7)   // 5 + 2
        assertThat(inventory["SKU-2"]).isEqualTo(4)   // 3 + 1
        assertThat(inventory["SKU-3"]).isEqualTo(10)
    }
}

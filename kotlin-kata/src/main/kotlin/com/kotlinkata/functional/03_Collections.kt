package com.kotlinkata.functional

/**
 * # Kata 12: Collections — Kotlin vs Java Streams
 *
 * Kotlin's collection API is richer than Java's Streams and more ergonomic.
 *
 * Key differences from Java:
 *   - Collection operations are EAGER by default (vs Java Streams which are lazy)
 *   - For lazy evaluation, use Sequence<T> (Kata 14)
 *   - Kotlin has separate mutable and immutable collection interfaces
 *   - Many more built-in operations (groupBy, partition, windowed, chunked, etc.)
 *   - No .stream()/.collect() ceremony — call map/filter directly
 *
 * Java: list.stream().filter(...).map(...).collect(Collectors.toList())
 * Kotlin: list.filter { ... }.map { ... }
 *
 * Topics:
 *   - Immutable vs mutable collections
 *   - Core operations: map, filter, flatMap, fold, reduce
 *   - Grouping and partitioning
 *   - Aggregation: count, sum, min, max, average
 *   - Specialized operations: zip, windowed, chunked, distinct
 *   - Converting between collection types
 */

import java.time.LocalDate

// ─────────────────────────────────────────────────────────────────────────────
// Domain
// ─────────────────────────────────────────────────────────────────────────────

data class SaleOrder(
    val id: String,
    val customerId: String,
    val region: String,
    val items: List<SaleItem>,
    val date: LocalDate,
    val status: String
)

data class SaleItem(val sku: String, val category: String, val qty: Int, val unitPrice: Double)

val sampleOrders = listOf(
    SaleOrder("ORD-001", "CUST-1", "US-EAST", listOf(
        SaleItem("KBD-01", "Electronics", 1, 79.99),
        SaleItem("MSE-01", "Electronics", 2, 29.99)
    ), LocalDate.of(2024, 1, 15), "DELIVERED"),
    SaleOrder("ORD-002", "CUST-2", "US-WEST", listOf(
        SaleItem("BOOK-01", "Books", 3, 14.99),
        SaleItem("BOOK-02", "Books", 1, 24.99)
    ), LocalDate.of(2024, 1, 16), "SHIPPED"),
    SaleOrder("ORD-003", "CUST-1", "US-EAST", listOf(
        SaleItem("PHONE-01", "Electronics", 1, 699.99)
    ), LocalDate.of(2024, 1, 17), "DELIVERED"),
    SaleOrder("ORD-004", "CUST-3", "EU", listOf(
        SaleItem("LAPTOP-01", "Electronics", 1, 1299.99),
        SaleItem("CASE-01", "Accessories", 1, 49.99)
    ), LocalDate.of(2024, 1, 18), "PENDING"),
    SaleOrder("ORD-005", "CUST-2", "EU", listOf(
        SaleItem("KBD-01", "Electronics", 2, 79.99)
    ), LocalDate.of(2024, 1, 19), "CANCELLED")
)

// ─────────────────────────────────────────────────────────────────────────────
// 1. Immutable vs Mutable Collections
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Kotlin distinguishes between read-only and mutable collection interfaces:
 *
 *   List<T>           — read-only (can't add/remove, but the backing list might change)
 *   MutableList<T>    — read-write
 *   Set<T>            — read-only
 *   MutableSet<T>     — read-write
 *   Map<K, V>         — read-only
 *   MutableMap<K, V>  — read-write
 *
 * listOf()          → List<T> (backed by ArrayList, but exposed as read-only)
 * mutableListOf()   → MutableList<T>
 * arrayListOf()     → ArrayList<T>
 *
 * IMPORTANT: List<T> in Kotlin is NOT the same as Java's List<T> immutability!
 * Kotlin's List<T> is a READ-ONLY VIEW — the underlying list can still be mutable.
 * For true immutability, use kotlinx.collections.immutable or copy on write.
 */
fun collectionTypesExamples() {
    // Read-only list — no add/remove methods available
    val immutable: List<String> = listOf("a", "b", "c")
    // immutable.add("d")  // ❌ Compile error — no add() on List

    // Mutable list
    val mutable: MutableList<String> = mutableListOf("a", "b")
    mutable.add("c")
    mutable.removeAt(0)
    println("Mutable: $mutable")

    // Converting between them
    val snapshot: List<String> = mutable.toList()         // defensive copy
    val toMutable: MutableList<String> = immutable.toMutableList()

    // Kotlin collection builders
    val built = buildList {
        add("first")
        addAll(listOf("second", "third"))
        if (true) add("conditional")
    }
    println("Built: $built")

    val builtMap = buildMap {
        put("key1", 1)
        put("key2", 2)
        putAll(mapOf("key3" to 3))
    }
    println("Built map: $builtMap")
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Core Operations: map, filter, flatMap
// ─────────────────────────────────────────────────────────────────────────────

fun coreOperationsExamples() {
    // map — transform each element
    val orderTotals = sampleOrders.map { order ->
        order.id to order.items.sumOf { it.qty * it.unitPrice }
    }
    println("Order totals: $orderTotals")

    // filter — keep elements matching predicate
    val deliveredOrders = sampleOrders.filter { it.status == "DELIVERED" }
    println("Delivered: ${deliveredOrders.map { it.id }}")

    // filterNot — inverse of filter
    val activeOrders = sampleOrders.filterNot { it.status in listOf("CANCELLED", "DELIVERED") }
    println("Active: ${activeOrders.map { it.id }}")

    // flatMap — transform then flatten
    // Java: orders.stream().flatMap(o -> o.getItems().stream()).collect(...)
    val allItems = sampleOrders.flatMap { it.items }
    println("Total line items: ${allItems.size}")

    val allSkus = sampleOrders.flatMap { order -> order.items.map { it.sku } }.distinct()
    println("Unique SKUs: $allSkus")

    // mapNotNull — map + filter nulls in one step
    val pricesAbove100: List<Double> = sampleOrders
        .mapNotNull { order ->
            order.items.firstOrNull { it.unitPrice > 100 }?.unitPrice
        }
    println("Items priced > 100: $pricesAbove100")

    // also shows: filterIsInstance<T> — filter AND cast
    val items: List<Any> = listOf("string", 42, true, "another", 100)
    val strings = items.filterIsInstance<String>()
    println("Strings only: $strings")
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Folding and Reducing
// ─────────────────────────────────────────────────────────────────────────────

/**
 * fold  — accumulates with an INITIAL value
 * reduce — accumulates WITHOUT initial value (can fail on empty list)
 * runningFold / runningReduce — emits intermediate results
 *
 * Java:
 *   stream.reduce(0, Integer::sum)
 *   stream.collect(Collectors.summingDouble(...))
 */
fun foldReduceExamples() {
    val prices = listOf(29.99, 14.99, 9.99, 49.99)

    // fold — with initial value (safe on empty list)
    val total = prices.fold(0.0) { acc, price -> acc + price }
    println("Total (fold): $total")

    // reduce — no initial value (throws on empty!)
    val product = prices.reduce { acc, price -> acc * price }
    println("Product (reduce): $product")

    // Building a complex aggregation with fold
    data class OrderStats(
        val count: Int = 0,
        val total: Double = 0.0,
        val maxOrder: Double = 0.0
    )

    val stats = sampleOrders
        .filter { it.status == "DELIVERED" }
        .fold(OrderStats()) { acc, order ->
            val orderTotal = order.items.sumOf { it.qty * it.unitPrice }
            acc.copy(
                count = acc.count + 1,
                total = acc.total + orderTotal,
                maxOrder = maxOf(acc.maxOrder, orderTotal)
            )
        }
    println("Stats: $stats")

    // runningFold — shows progressive totals (useful for running balances)
    val runningTotals = prices.runningFold(0.0) { acc, price -> acc + price }
    println("Running totals: ${runningTotals.map { "%.2f".format(it) }}")
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Grouping and Partitioning
// ─────────────────────────────────────────────────────────────────────────────

/**
 * groupBy  — split collection into Map<K, List<V>> by a key function
 * partition — split into Pair<List, List> by predicate (true/false)
 * groupingBy — lazy grouping for more complex aggregations
 *
 * Java: Collectors.groupingBy(...), Collectors.partitioningBy(...)
 * Kotlin: same semantics but cleaner syntax
 */
fun groupingExamples() {
    // groupBy — group orders by status
    val byStatus: Map<String, List<SaleOrder>> = sampleOrders.groupBy { it.status }
    byStatus.forEach { (status, orders) ->
        println("$status: ${orders.size} orders")
    }

    // groupBy then transform values
    val totalByRegion: Map<String, Double> = sampleOrders
        .groupBy { it.region }
        .mapValues { (_, orders) -> orders.sumOf { o -> o.items.sumOf { it.qty * it.unitPrice } } }
    println("Revenue by region: $totalByRegion")

    // partition — split by predicate into (matching, not matching)
    val (premium, standard) = sampleOrders.partition { order ->
        order.items.sumOf { it.qty * it.unitPrice } > 200
    }
    println("Premium orders: ${premium.map { it.id }}")
    println("Standard orders: ${standard.map { it.id }}")

    // groupingBy with eachCount / fold (like Java's Collectors.groupingBy(..., Collectors.counting()))
    val countByCustomer = sampleOrders.groupingBy { it.customerId }.eachCount()
    println("Orders per customer: $countByCustomer")

    // Nested grouping: by region then by status
    val nested: Map<String, Map<String, List<SaleOrder>>> = sampleOrders
        .groupBy { it.region }
        .mapValues { (_, orders) -> orders.groupBy { it.status } }
    println("EU pending: ${nested["EU"]?.get("PENDING")?.map { it.id }}")
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Aggregation Functions
// ─────────────────────────────────────────────────────────────────────────────

fun aggregationExamples() {
    val totals = sampleOrders.map { order -> order.items.sumOf { it.qty * it.unitPrice } }

    println("Count: ${totals.size}")
    println("Sum: ${"%.2f".format(totals.sum())}")
    println("Min: ${"%.2f".format(totals.min())}")
    println("Max: ${"%.2f".format(totals.max())}")
    println("Average: ${"%.2f".format(totals.average())}")

    // sumOf — sum with a selector (no need for .map().sum())
    val totalItems = sampleOrders.sumOf { order -> order.items.sumOf { it.qty } }
    println("Total items sold: $totalItems")

    // minByOrNull / maxByOrNull — null-safe version
    val cheapestOrder = sampleOrders.minByOrNull { order -> order.items.sumOf { it.qty * it.unitPrice } }
    val mostExpensive = sampleOrders.maxByOrNull { order -> order.items.sumOf { it.qty * it.unitPrice } }
    println("Cheapest: ${cheapestOrder?.id}, Most expensive: ${mostExpensive?.id}")

    // count with predicate
    val deliveredCount = sampleOrders.count { it.status == "DELIVERED" }
    println("Delivered count: $deliveredCount")

    // any, all, none
    println("Any cancelled: ${sampleOrders.any { it.status == "CANCELLED" }}")
    println("All have items: ${sampleOrders.all { it.items.isNotEmpty() }}")
    println("None have zero items: ${sampleOrders.none { it.items.isEmpty() }}")
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. Specialized Operations
// ─────────────────────────────────────────────────────────────────────────────

fun specializedOperations() {
    val orderIds = (1..20).map { "ORD-${"$it".padStart(3, '0')}" }

    // chunked — split into sub-lists of given size
    val batches = orderIds.chunked(5)
    println("Batches: ${batches.map { it.size }} chunks of 5")

    // chunked with transform
    val processedBatches = orderIds.chunked(5) { batch ->
        batch.joinToString(",")  // process each batch
    }
    println("First batch joined: ${processedBatches.first()}")

    // windowed — sliding window
    val windows = orderIds.windowed(3)  // triplets
    println("First 3 windows: ${windows.take(3)}")

    // zip — pair up two lists
    val skus = listOf("SKU-1", "SKU-2", "SKU-3")
    val prices = listOf(9.99, 19.99, 29.99)
    val inventory = skus.zip(prices) { sku, price -> "$sku → \$${price}" }
    println("Inventory: $inventory")

    // zip with truncation (stops at shorter list)
    val extraPrices = listOf(9.99, 19.99)
    val truncated = skus.zip(extraPrices)  // only 2 pairs
    println("Truncated zip: $truncated")

    // distinct and distinctBy
    val allRegions = sampleOrders.map { it.region }
    println("Unique regions: ${allRegions.distinct()}")

    // distinctBy first occurrence
    val firstPerCustomer = sampleOrders.distinctBy { it.customerId }
    println("First order per customer: ${firstPerCustomer.map { "${it.customerId} → ${it.id}" }}")

    // sortedBy / sortedByDescending / sortedWith
    val byTotal = sampleOrders.sortedBy { order -> order.items.sumOf { it.qty * it.unitPrice } }
    println("Sorted by total (ascending): ${byTotal.map { it.id }}")

    // take, drop, takeLast, dropLast
    println("First 3: ${orderIds.take(3)}")
    println("Last 3: ${orderIds.takeLast(3)}")
    println("Drop first 17: ${orderIds.drop(17)}")

    // takeWhile / dropWhile
    val increasing = listOf(1, 2, 3, 4, 5, 3, 2, 1)
    println("Take while increasing: ${increasing.takeWhile { it <= 4 }}")
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. Real-World: Sales Analytics
// ─────────────────────────────────────────────────────────────────────────────

fun salesAnalytics() {
    println("\n=== Sales Analytics Report ===")

    // Top-selling categories
    val categoryRevenue = sampleOrders
        .flatMap { it.items }
        .groupBy { it.category }
        .mapValues { (_, items) -> items.sumOf { it.qty * it.unitPrice } }
        .entries
        .sortedByDescending { it.value }

    println("Revenue by category:")
    categoryRevenue.forEach { (cat, rev) -> println("  $cat: $${"%.2f".format(rev)}") }

    // Repeat customers
    val repeatCustomers = sampleOrders
        .groupBy { it.customerId }
        .filter { (_, orders) -> orders.size > 1 }
        .keys
    println("Repeat customers: $repeatCustomers")

    // Average order value by region
    val avgOrderValueByRegion = sampleOrders
        .filter { it.status != "CANCELLED" }
        .groupBy { it.region }
        .mapValues { (_, orders) ->
            orders.map { o -> o.items.sumOf { it.qty * it.unitPrice } }.average()
        }
    println("Avg order value by region:")
    avgOrderValueByRegion.forEach { (region, avg) ->
        println("  $region: $${"%.2f".format(avg)}")
    }
}

fun main() {
    println("=== Kata 12: Collections ===\n")
    println("--- Collection Types ---")
    collectionTypesExamples()
    println("\n--- Core Operations ---")
    coreOperationsExamples()
    println("\n--- Fold & Reduce ---")
    foldReduceExamples()
    println("\n--- Grouping ---")
    groupingExamples()
    println("\n--- Aggregation ---")
    aggregationExamples()
    println("\n--- Specialized Operations ---")
    specializedOperations()
    salesAnalytics()
}

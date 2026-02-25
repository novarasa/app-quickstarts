package com.kotlinkata.functional

/**
 * # Kata 11: Higher-Order Functions and Lambdas
 *
 * Higher-order functions (HOF) take functions as parameters or return functions.
 * They're fundamental to Kotlin's style and replace many patterns.
 *
 * Java equivalents:
 *   - Functional interfaces (Function<T,R>, Predicate<T>, Consumer<T>, Supplier<T>)
 *   - Strategy pattern (pass behavior as parameter)
 *   - Template Method pattern (define algorithm skeleton, delegate steps)
 *
 * Kotlin advantages over Java lambdas:
 *   - Concise syntax (trailing lambda, implicit `it`)
 *   - Can close over mutable variables (Java requires effectively final)
 *   - inline functions eliminate lambda boxing overhead
 *
 * Topics:
 *   - Function types
 *   - Lambda expressions and trailing lambda syntax
 *   - it vs named parameters
 *   - Closures (capturing mutable state)
 *   - Returning functions (function factories)
 *   - Composing functions
 *   - Common HOF patterns: Strategy, Middleware, retry, measure
 */

import kotlin.time.measureTime

// ─────────────────────────────────────────────────────────────────────────────
// 1. Function Types
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Kotlin function types:
 *   (ParamType) -> ReturnType          basic function type
 *   (A, B) -> C                        two params
 *   () -> Unit                         no params, no return (like Runnable)
 *   ((String) -> Boolean)?             nullable function type
 *   String.(Int) -> String             function type with receiver (extension lambdas)
 *
 * Java equivalents:
 *   Function<T, R>     ≈  (T) -> R
 *   Predicate<T>       ≈  (T) -> Boolean
 *   Consumer<T>        ≈  (T) -> Unit
 *   Supplier<T>        ≈  () -> T
 *   BiFunction<T,U,R>  ≈  (T, U) -> R
 *
 * Kotlin unifies ALL of these under one syntax.
 */

// Function types as parameters
fun <T> List<T>.filterWith(predicate: (T) -> Boolean): List<T> = filter(predicate)
fun <T, R> List<T>.transformWith(transform: (T) -> R): List<R> = map(transform)
fun <T> List<T>.processEach(action: (T) -> Unit) = forEach(action)

// Function types as return values (function factory)
fun createMultiplier(factor: Double): (Double) -> Double = { value -> value * factor }
fun createValidator(minLength: Int, maxLength: Int): (String) -> Boolean =
    { s -> s.length in minLength..maxLength }

data class OrderRecord(val id: String, val customerId: String, val total: Double, val status: String)

fun functionTypeExamples() {
    val orders = listOf(
        OrderRecord("ORD-001", "CUST-1", 149.99, "SHIPPED"),
        OrderRecord("ORD-002", "CUST-1", 49.99, "PENDING"),
        OrderRecord("ORD-003", "CUST-2", 299.99, "DELIVERED"),
        OrderRecord("ORD-004", "CUST-2", 9.99, "CANCELLED")
    )

    // Passing lambdas
    val shippedOrders = orders.filterWith { it.status == "SHIPPED" }
    val orderIds = orders.transformWith { it.id }
    println("Shipped: $shippedOrders")
    println("IDs: $orderIds")

    // Function factory
    val applyVAT = createMultiplier(1.20)
    val applyUSAX = createMultiplier(1.08)
    println("EU price: ${applyVAT(99.99)}")
    println("US price: ${applyUSAX(99.99)}")

    // Storing functions in variables
    val isValidSku: (String) -> Boolean = createValidator(4, 20)
    println("SKU-1 valid: ${isValidSku("SKU-1")}")
    println("A valid: ${isValidSku("A")}")

    // Function references with ::
    fun isHighValue(order: OrderRecord): Boolean = order.total > 100
    val highValueOrders = orders.filter(::isHighValue)  // method reference
    println("High value: ${highValueOrders.map { it.id }}")
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Trailing Lambda and it Conventions
// ─────────────────────────────────────────────────────────────────────────────

/**
 * If the LAST parameter is a function, the lambda can be moved OUTSIDE the parens.
 * If the lambda is the ONLY parameter, parens can be omitted entirely.
 *
 * If a lambda has ONE parameter, it can be referenced as `it` (implicit name).
 *
 * These conventions make DSL-like code possible.
 *
 * Java: collection.stream().filter(order -> order.getStatus().equals("ACTIVE"))
 * Kotlin: orders.filter { it.status == "ACTIVE" }
 */
fun trailingLambdaExamples() {
    val orders = listOf(
        OrderRecord("ORD-001", "CUST-1", 149.99, "SHIPPED"),
        OrderRecord("ORD-002", "CUST-2", 49.99, "PENDING")
    )

    // Trailing lambda syntax
    val active = orders.filter({ it.status == "PENDING" })  // old style
    val active2 = orders.filter { it.status == "PENDING" }  // trailing lambda (preferred)

    // Named lambda parameter (better for complex lambdas)
    val descriptions = orders.map { order ->
        "Order ${order.id}: $${order.total} (${order.status})"
    }
    println(descriptions)

    // Chaining
    val summary = orders
        .filter { it.total > 50 }
        .sortedByDescending { it.total }
        .map { "${it.id}: ${it.total}" }
    println("Filtered & sorted: $summary")

    // When a function has only a lambda parameter, no parens needed
    val numbers = listOf(1, 2, 3, 4, 5)
    val doubled = numbers.map { it * 2 }    // filter and map take only a lambda
    numbers.forEach { println(it) }         // no parens needed
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Closures — Capturing State
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Lambdas close over variables from the enclosing scope.
 *
 * KEY DIFFERENCE from Java: Kotlin lambdas can capture and MODIFY
 * mutable variables. Java requires effectively final.
 *
 * Java:
 *   int count = 0;
 *   list.forEach(x -> count++);  // ❌ Compile error in Java!
 *
 * Kotlin:
 *   var count = 0
 *   list.forEach { count++ }    // ✅ Works in Kotlin
 *
 * (Kotlin uses Ref wrappers under the hood, but you don't see them)
 */
fun closureExamples() {
    // Mutable closure — count orders meeting a condition
    var processedCount = 0
    var totalRevenue = 0.0

    val orders = listOf(
        OrderRecord("ORD-001", "CUST-1", 149.99, "SHIPPED"),
        OrderRecord("ORD-002", "CUST-2", 299.99, "DELIVERED"),
        OrderRecord("ORD-003", "CUST-3", 49.99, "CANCELLED")
    )

    orders
        .filter { it.status != "CANCELLED" }
        .forEach {
            processedCount++           // modifying outer variable — works!
            totalRevenue += it.total   // modifying outer variable — works!
        }

    println("Processed: $processedCount, Revenue: $totalRevenue")

    // Building accumulator using closure
    fun makeAccumulator(): () -> Int {
        var sum = 0
        return { sum += 1; sum }    // each call increments and returns
    }

    val counter = makeAccumulator()
    println("Counter: ${counter()}, ${counter()}, ${counter()}")  // 1, 2, 3
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Strategy Pattern
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Java's Strategy pattern requires an interface + multiple implementations.
 * Kotlin: pass the strategy as a function type — no interface needed.
 */
data class PricedItem(val sku: String, val price: Double, val quantity: Int)

fun calculateTotal(
    items: List<PricedItem>,
    taxStrategy: (Double) -> Double,
    discountStrategy: (Double) -> Double = { it }  // default: no discount
): Double {
    val subtotal = items.sumOf { it.price * it.quantity }
    val discounted = discountStrategy(subtotal)
    val tax = taxStrategy(discounted)
    return discounted + tax
}

fun strategyPatternExamples() {
    val items = listOf(
        PricedItem("SKU-1", 29.99, 2),
        PricedItem("SKU-2", 9.99, 5)
    )

    // Different tax strategies — just functions, no interface hierarchy!
    val usTax: (Double) -> Double = { amount -> amount * 0.08 }
    val euVat: (Double) -> Double = { amount -> amount * 0.20 }
    val taxExempt: (Double) -> Double = { 0.0 }

    val bulkDiscount: (Double) -> Double = { amount ->
        if (amount > 50) amount * 0.10 else 0.0
    }

    println("US total: ${"%.2f".format(calculateTotal(items, usTax))}")
    println("EU total: ${"%.2f".format(calculateTotal(items, euVat))}")
    println("US + bulk discount: ${"%.2f".format(calculateTotal(items, usTax, bulkDiscount))}")
    println("Tax exempt: ${"%.2f".format(calculateTotal(items, taxExempt))}")
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Middleware / Decorator Pattern
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Compose behaviors by wrapping functions.
 * This is how middleware works in frameworks (Ktor, Spring filters, etc.)
 */
typealias OrderHandler = (String) -> String

fun withLogging(handler: OrderHandler): OrderHandler = { orderId ->
    println("[LOG] Processing order: $orderId")
    val result = handler(orderId)
    println("[LOG] Completed order: $orderId → $result")
    result
}

fun withRetry(maxAttempts: Int, handler: OrderHandler): OrderHandler = { orderId ->
    var lastError: Exception? = null
    var result: String? = null
    for (attempt in 1..maxAttempts) {
        try {
            result = handler(orderId)
            break
        } catch (e: Exception) {
            lastError = e
            println("[RETRY] Attempt $attempt failed for $orderId: ${e.message}")
        }
    }
    result ?: throw lastError ?: RuntimeException("All $maxAttempts attempts failed")
}

fun withTiming(handler: OrderHandler): OrderHandler = { orderId ->
    var result: String? = null
    val elapsed = measureTime {
        result = handler(orderId)
    }
    println("[TIMING] Order $orderId took ${elapsed.inWholeMilliseconds}ms")
    result!!
}

fun middlewareExamples() {
    val baseHandler: OrderHandler = { orderId -> "Order $orderId processed" }

    // Compose middleware
    val enhancedHandler = withTiming(withLogging(withRetry(3, baseHandler)))

    val result = enhancedHandler("ORD-001")
    println("Result: $result")
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. Function Composition
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Functions can be composed — output of one becomes input of next.
 * Kotlin doesn't have built-in `andThen` like Java's Function, but it's easy to add.
 */
infix fun <A, B, C> ((A) -> B).andThen(other: (B) -> C): (A) -> C = { a -> other(this(a)) }
infix fun <A, B, C> ((B) -> C).compose(other: (A) -> B): (A) -> C = { a -> this(other(a)) }

fun compositionExamples() {
    val trim: (String) -> String = String::trim
    val uppercase: (String) -> String = String::uppercase
    val addPrefix: (String) -> String = { "ORD-$it" }

    // Compose a pipeline
    val processOrderId = trim andThen uppercase andThen addPrefix

    println(processOrderId("  00123  "))   // ORD-00123
    println(processOrderId("  456  "))     // ORD-456

    // Real use: data transformation pipeline
    val parsePrice: (String) -> Double = { it.replace("[$,]".toRegex(), "").toDouble() }
    val roundToCents: (Double) -> Long = { Math.round(it * 100) }
    val formatCents: (Long) -> String = { "$${it / 100}.${(it % 100).toString().padStart(2, '0')}" }

    val parsePriceString = parsePrice andThen roundToCents andThen formatCents
    println(parsePriceString("$19.99"))    // $19.99
    println(parsePriceString("$149.995"))  // $150.00 (rounded)
}

fun main() {
    println("=== Kata 11: Higher-Order Functions ===\n")
    functionTypeExamples()
    println()
    trailingLambdaExamples()
    println()
    closureExamples()
    println()
    strategyPatternExamples()
    println()
    middlewareExamples()
    println()
    compositionExamples()
}

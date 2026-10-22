package com.kotlinkata.functional

/**
 * # Kata 10: Extension Functions — Goodbye Utility Classes
 *
 * Extension functions let you add methods to ANY class without inheritance
 * or modifying the class. They're syntactic sugar compiled to static methods.
 *
 * Java equivalent: Utility classes (StringUtils, OrderUtils, CollectionUtils)
 * The difference: extensions look like they BELONG to the class.
 *
 *   Java: OrderUtils.calculateTotal(order)     // external, awkward
 *   Kotlin: order.calculateTotal()              // fluent, discoverable
 *
 * This is why Kotlin killed Apache Commons, Guava, and Lombok for most use cases.
 *
 * Topics:
 *   - Basic extension functions
 *   - Extension properties
 *   - Extensions on nullable types
 *   - Extensions on generic types
 *   - Extensions that return `this` (fluent builders)
 *   - Extension functions as utilities (replacing static helpers)
 *   - Scope: where extensions are visible
 */

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ─────────────────────────────────────────────────────────────────────────────
// Domain Types
// ─────────────────────────────────────────────────────────────────────────────

data class Money(val amountCents: Long, val currency: String = "USD") {
    override fun toString(): String = "${"%.2f".format(amountCents / 100.0)} $currency"
}

data class CartItem(val sku: String, val name: String, val quantity: Int, val price: Money)
data class Cart(val customerId: String, val items: List<CartItem>)
data class Shipment(val trackingNumber: String, val carrier: String, val estimatedDelivery: Instant?)

// ─────────────────────────────────────────────────────────────────────────────
// 1. Basic Extension Functions
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Syntax: fun ReceiverType.functionName(params): ReturnType { ... }
 * Inside the body, `this` refers to the receiver instance.
 *
 * IMPORTANT: Extensions are resolved STATICALLY (at compile time), not dynamically.
 * They do NOT override class methods — if a class has a method with the same
 * signature, the class method always wins.
 */

// Extend Money
fun Money.isZero(): Boolean = amountCents == 0L
fun Money.add(other: Money): Money {
    require(currency == other.currency) { "Cannot add $currency and ${other.currency}" }
    return Money(amountCents + other.amountCents, currency)
}
fun Money.multiply(factor: Int): Money = Money(amountCents * factor, currency)
fun Money.applyDiscount(percent: Double): Money = Money(
    (amountCents * (1 - percent / 100)).toLong(), currency
)

// Extend Cart with business logic (no need for CartService to do this)
fun Cart.totalItems(): Int = items.sumOf { it.quantity }
fun Cart.subtotal(): Money {
    if (items.isEmpty()) return Money(0)
    return items.map { it.price.multiply(it.quantity) }
        .reduce { acc, money -> acc.add(money) }
}
fun Cart.isEmpty(): Boolean = items.isEmpty()
fun Cart.hasItem(sku: String): Boolean = items.any { it.sku == sku }
fun Cart.itemBySku(sku: String): CartItem? = items.find { it.sku == sku }

// Extend String with domain-specific validation
fun String.isValidOrderId(): Boolean = matches(Regex("ORD-[0-9]{6,}"))
fun String.isValidSku(): Boolean = matches(Regex("[A-Z0-9]{2,8}(-[A-Z0-9]{1,8})*"))
fun String.isValidEmail(): Boolean = matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
fun String.toOrderId(): String = if (startsWith("ORD-")) this else "ORD-$this"
fun String.maskCreditCard(): String = if (length >= 4) "**** **** **** ${takeLast(4)}" else "****"

fun basicExtensionExamples() {
    val price = Money(7999, "USD")  // $79.99
    val taxAmount = Money(640, "USD")  // $6.40
    println("Price: $price")
    println("With tax: ${price.add(taxAmount)}")
    println("10% discount: ${price.applyDiscount(10.0)}")
    println("Is zero: ${Money(0).isZero()}")

    val cart = Cart("CUST-1", listOf(
        CartItem("KBD-01", "Keyboard", 1, Money(7999)),
        CartItem("MSE-01", "Mouse", 2, Money(2999))
    ))
    println("Cart total items: ${cart.totalItems()}")
    println("Cart subtotal: ${cart.subtotal()}")
    println("Has keyboard: ${cart.hasItem("KBD-01")}")

    // String extensions — look like they're part of String
    println("ORD-123456".isValidOrderId())    // true
    println("BAD".isValidOrderId())           // false
    println("john@example.com".isValidEmail()) // true
    println("4111111111111111".maskCreditCard()) // **** **** **** 1111
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Extension Properties
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Extension properties work like regular properties but cannot have a backing field.
 * They must be computed from existing state.
 */
val Money.dollars: Double get() = amountCents / 100.0
val Money.isPositive: Boolean get() = amountCents > 0
val Cart.hasExpensiveItems: Boolean get() = items.any { it.price.dollars > 100.0 }

val Shipment.isDelivered: Boolean get() = estimatedDelivery?.isBefore(Instant.now()) ?: false
val Shipment.trackingUrl: String get() = when (carrier.uppercase()) {
    "FEDEX" -> "https://fedex.com/track/$trackingNumber"
    "UPS"   -> "https://ups.com/track/$trackingNumber"
    "USPS"  -> "https://usps.com/track/$trackingNumber"
    else    -> "https://track.example.com/$trackingNumber"
}

val Instant.readableDate: String
    get() = LocalDateTime.ofInstant(this, ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))

fun extensionPropertyExamples() {
    val price = Money(7999)
    println("Dollars: ${price.dollars}")
    println("Is positive: ${price.isPositive}")

    val cart = Cart("CUST-1", listOf(
        CartItem("LAPTOP", "MacBook", 1, Money(149999))
    ))
    println("Has expensive items: ${cart.hasExpensiveItems}")

    val shipment = Shipment("1Z999AA1012345678", "UPS", Instant.now().plusSeconds(86400))
    println("Tracking URL: ${shipment.trackingUrl}")
    println("Delivered: ${shipment.isDelivered}")
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Extensions on Nullable Types
// ─────────────────────────────────────────────────────────────────────────────

/**
 * You can extend T? (nullable types)!
 * Inside, `this` is T? — you must handle null explicitly.
 *
 * This is how Kotlin's standard library isNullOrEmpty(), isNullOrBlank() work.
 */
fun Money?.orZero(): Money = this ?: Money(0)
fun Money?.isNullOrZero(): Boolean = this == null || this.amountCents == 0L
fun Cart?.isNullOrEmpty(): Boolean = this == null || this.items.isEmpty()

fun String?.toMoneyOrNull(): Money? {
    if (this == null) return null
    val cents = this.replace("[^0-9]".toRegex(), "").toLongOrNull() ?: return null
    return Money(cents)
}

fun nullableExtensionExamples() {
    val nullMoney: Money? = null
    println("Null money or zero: ${nullMoney.orZero()}")
    println("Is null or zero: ${nullMoney.isNullOrZero()}")

    val nullCart: Cart? = null
    println("Null cart is empty: ${nullCart.isNullOrEmpty()}")

    println("'\$19.99'.toMoney: ${"$19.99".toMoneyOrNull()}")
    println("'invalid'.toMoney: ${"invalid".toMoneyOrNull()}")
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Generic Extension Functions
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Extensions can be generic — extend a generic type or add generic behavior.
 * This is how Kotlin's standard library works (e.g., List.first(), List.map()).
 */

// Extend any Iterable to get paginated results
fun <T> List<T>.page(pageNumber: Int, pageSize: Int): List<T> {
    val fromIndex = (pageNumber - 1) * pageSize
    if (fromIndex >= size) return emptyList()
    return subList(fromIndex, minOf(fromIndex + pageSize, size))
}

// Extend any Map with a safe multi-get
fun <K, V> Map<K, V>.getOrThrow(key: K): V =
    this[key] ?: throw NoSuchElementException("Key not found: $key")

// Wrap a result in a Result<T> (useful for try-or-null patterns)
fun <T> (() -> T).runCatching(): Result<T> = try {
    Result.success(this())
} catch (e: Exception) {
    Result.failure(e)
}

// Convert any value to a list (useful in functional pipelines)
fun <T> T.toSingletonList(): List<T> = listOf(this)
fun <T> T?.toListOrEmpty(): List<T> = if (this != null) listOf(this) else emptyList()

fun genericExtensionExamples() {
    val orders = (1..25).map { "ORD-${"$it".padStart(6, '0')}" }

    println("Page 1 (5 items): ${orders.page(1, 5)}")
    println("Page 3 (5 items): ${orders.page(3, 5)}")
    println("Page 10 (5 items, beyond end): ${orders.page(10, 5)}")

    val statusMap = mapOf("ORD-001" to "SHIPPED", "ORD-002" to "DELIVERED")
    println("ORD-001: ${statusMap.getOrThrow("ORD-001")}")

    val single = "ORD-001".toSingletonList()
    val nullableList: String? = null
    println("Single: $single")
    println("Nullable to list: ${nullableList.toListOrEmpty()}")
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Extensions as Fluent Builders (Return this or copy)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Extensions that return a modified copy enable fluent builder pipelines
 * without the Builder pattern boilerplate.
 */
data class QueryBuilder(
    val tableName: String,
    val conditions: List<String> = emptyList(),
    val orderBy: String? = null,
    val limit: Int? = null,
    val offset: Int? = null
)

fun QueryBuilder.where(condition: String): QueryBuilder = copy(conditions = conditions + condition)
fun QueryBuilder.orderBy(column: String, direction: String = "ASC"): QueryBuilder =
    copy(orderBy = "$column $direction")
fun QueryBuilder.limit(n: Int): QueryBuilder = copy(limit = n)
fun QueryBuilder.offset(n: Int): QueryBuilder = copy(offset = n)

fun QueryBuilder.build(): String {
    val sb = StringBuilder("SELECT * FROM $tableName")
    if (conditions.isNotEmpty()) sb.append(" WHERE ${conditions.joinToString(" AND ")}")
    orderBy?.let { sb.append(" ORDER BY $it") }
    limit?.let { sb.append(" LIMIT $it") }
    offset?.let { sb.append(" OFFSET $it") }
    return sb.toString()
}

fun fluentBuilderExamples() {
    val query = QueryBuilder("orders")
        .where("status = 'ACTIVE'")
        .where("customer_id = 'CUST-001'")
        .orderBy("created_at", "DESC")
        .limit(10)
        .offset(20)
        .build()

    println("Query: $query")
}

fun main() {
    println("=== Kata 10: Extension Functions ===\n")
    println("--- Basic Extensions ---")
    basicExtensionExamples()
    println("\n--- Extension Properties ---")
    extensionPropertyExamples()
    println("\n--- Nullable Extensions ---")
    nullableExtensionExamples()
    println("\n--- Generic Extensions ---")
    genericExtensionExamples()
    println("\n--- Fluent Builder ---")
    fluentBuilderExamples()
}

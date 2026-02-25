package com.kotlinkata.realworld

import java.time.Instant
import java.util.UUID

/**
 * # Kata 21: Real-World Capstone — Order Management Domain
 *
 * This file defines a production-quality domain model using ALL Kotlin features
 * from previous katas. Read this as a synthesis of everything you've learned.
 *
 * Features used:
 *   - Data classes (Kata 06)
 *   - Sealed classes and interfaces (Kata 07)
 *   - Companion objects with factory methods (Kata 08)
 *   - Extension functions and properties (Kata 10)
 *   - Value classes for type safety (Kata 06)
 *   - when expressions for state machine (Kata 04)
 *   - Result type for error handling (Kata 07)
 */

// ─────────────────────────────────────────────────────────────────────────────
// Value Classes — Type-Safe IDs (Zero Runtime Overhead)
// ─────────────────────────────────────────────────────────────────────────────

@JvmInline value class OrderId(val value: String) {
    init { require(value.matches(Regex("ORD-[A-Z0-9]{8,}"))) { "Invalid OrderId: $value" } }
    override fun toString(): String = value
}

@JvmInline value class CustomerId(val value: String) {
    init { require(value.matches(Regex("CUST-[A-Z0-9]+"))) { "Invalid CustomerId: $value" } }
    override fun toString(): String = value
}

@JvmInline value class ProductId(val value: String) {
    init { require(value.matches(Regex("[A-Z0-9]{4,20}(-[A-Z0-9]+)*"))) { "Invalid ProductId: $value" } }
    override fun toString(): String = value
}

@JvmInline value class Money(val cents: Long) {
    init { require(cents >= 0) { "Money cannot be negative: $cents" } }
    val dollars: Double get() = cents / 100.0
    operator fun plus(other: Money): Money = Money(cents + other.cents)
    operator fun minus(other: Money): Money = Money(maxOf(0, cents - other.cents))
    operator fun times(quantity: Int): Money = Money(cents * quantity)
    operator fun compareTo(other: Money): Int = cents.compareTo(other.cents)
    override fun toString(): String = "${"%.2f".format(dollars)} USD"

    companion object {
        val ZERO = Money(0)
        fun of(dollars: Double): Money = Money(Math.round(dollars * 100))
        fun ofCents(cents: Long): Money = Money(cents)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Core Domain Entities
// ─────────────────────────────────────────────────────────────────────────────

data class Product(
    val id: ProductId,
    val name: String,
    val description: String,
    val basePrice: Money,
    val category: ProductCategory,
    val weightKg: Double,
    val inStock: Boolean = true,
    val stockCount: Int = 0
) {
    companion object {
        fun create(
            sku: String,
            name: String,
            description: String,
            priceUSD: Double,
            category: ProductCategory,
            weightKg: Double,
            stockCount: Int = 0
        ): Product = Product(
            id = ProductId(sku),
            name = name,
            description = description,
            basePrice = Money.of(priceUSD),
            category = category,
            weightKg = weightKg,
            inStock = stockCount > 0,
            stockCount = stockCount
        )
    }
}

enum class ProductCategory {
    ELECTRONICS, BOOKS, CLOTHING, HOME_GARDEN, SPORTS, FOOD, DIGITAL
}

data class OrderLineItem(
    val productId: ProductId,
    val productName: String,       // denormalized — capture at order time
    val quantity: Int,
    val unitPrice: Money,          // price at order time (not current price)
    val discountAmount: Money = Money.ZERO
) {
    init {
        require(quantity > 0) { "Quantity must be positive: $quantity" }
    }

    val lineTotal: Money get() = (unitPrice - discountAmount) * quantity
    val originalTotal: Money get() = unitPrice * quantity
    val savings: Money get() = discountAmount * quantity
}

data class ShippingAddress(
    val recipientName: String,
    val street1: String,
    val street2: String? = null,
    val city: String,
    val stateOrProvince: String,
    val postalCode: String,
    val country: String = "US"
) {
    val fullAddress: String get() = buildString {
        append(street1)
        street2?.let { append(", $it") }
        append(", $city, $stateOrProvince $postalCode, $country")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Order State Machine — Sealed Class Hierarchy
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Each state holds exactly the data relevant to that state.
 * No nullable fields with "only populated in certain states."
 *
 * States:
 *   Draft → Submitted → Confirmed → Processing → Shipped → Delivered
 *                      ↘ Cancelled
 *                                 ↘ OnHold → Processing (resume)
 *              (any non-terminal state can be cancelled)
 */
sealed class OrderStatus {
    object Draft : OrderStatus()

    data class Submitted(
        val submittedAt: Instant = Instant.now()
    ) : OrderStatus()

    data class Confirmed(
        val confirmedAt: Instant = Instant.now(),
        val estimatedShipDate: String,
        val confirmationRef: String
    ) : OrderStatus()

    data class OnHold(
        val reason: String,
        val heldAt: Instant = Instant.now(),
        val releasedAt: Instant? = null
    ) : OrderStatus()

    data class Processing(
        val startedAt: Instant = Instant.now(),
        val warehouseId: String
    ) : OrderStatus()

    data class Shipped(
        val shippedAt: Instant = Instant.now(),
        val trackingNumber: String,
        val carrier: String,
        val estimatedDelivery: String
    ) : OrderStatus()

    data class Delivered(
        val deliveredAt: Instant = Instant.now(),
        val signedBy: String? = null
    ) : OrderStatus()

    data class Cancelled(
        val cancelledAt: Instant = Instant.now(),
        val reason: String,
        val cancelledBy: String
    ) : OrderStatus()

    data class Refunded(
        val refundedAt: Instant = Instant.now(),
        val refundAmount: Money,
        val refundTransactionId: String
    ) : OrderStatus()
}

val OrderStatus.isTerminal: Boolean get() = when (this) {
    is OrderStatus.Delivered,
    is OrderStatus.Cancelled,
    is OrderStatus.Refunded -> true
    else -> false
}

val OrderStatus.isCancellable: Boolean get() = when (this) {
    is OrderStatus.Draft,
    is OrderStatus.Submitted,
    is OrderStatus.Confirmed,
    is OrderStatus.OnHold -> true
    else -> false
}

val OrderStatus.displayName: String get() = when (this) {
    is OrderStatus.Draft          -> "Draft"
    is OrderStatus.Submitted      -> "Submitted"
    is OrderStatus.Confirmed      -> "Confirmed"
    is OrderStatus.OnHold         -> "On Hold: $reason"
    is OrderStatus.Processing     -> "Processing"
    is OrderStatus.Shipped        -> "Shipped"
    is OrderStatus.Delivered      -> "Delivered"
    is OrderStatus.Cancelled      -> "Cancelled: $reason"
    is OrderStatus.Refunded       -> "Refunded: $refundAmount"
}

// ─────────────────────────────────────────────────────────────────────────────
// The Order Aggregate
// ─────────────────────────────────────────────────────────────────────────────

data class Order(
    val id: OrderId,
    val customerId: CustomerId,
    val lineItems: List<OrderLineItem>,
    val shippingAddress: ShippingAddress,
    val status: OrderStatus = OrderStatus.Draft,
    val discountCode: String? = null,
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    // Computed properties — no need for recalculation in service layer
    val subtotal: Money get() = lineItems.fold(Money.ZERO) { acc, item -> acc + item.lineTotal }
    val totalSavings: Money get() = lineItems.fold(Money.ZERO) { acc, item -> acc + item.savings }
    val itemCount: Int get() = lineItems.sumOf { it.quantity }
    val totalWeight: Double get() = 0.0  // would reference product catalog in real code

    // State machine transitions — returns new Order (immutable update)
    fun submit(): OrderResult<Order> = when (status) {
        is OrderStatus.Draft ->
            if (lineItems.isEmpty()) OrderResult.Failure("Cannot submit empty order")
            else OrderResult.Success(copy(status = OrderStatus.Submitted(), updatedAt = Instant.now()))
        else -> OrderResult.Failure("Order cannot be submitted from state: ${status.displayName}")
    }

    fun confirm(estimatedShipDate: String, ref: String): OrderResult<Order> = when (status) {
        is OrderStatus.Submitted -> OrderResult.Success(copy(
            status = OrderStatus.Confirmed(
                estimatedShipDate = estimatedShipDate,
                confirmationRef = ref
            ),
            updatedAt = Instant.now()
        ))
        else -> OrderResult.Failure("Cannot confirm from state: ${status.displayName}")
    }

    fun startProcessing(warehouseId: String): OrderResult<Order> = when (status) {
        is OrderStatus.Confirmed,
        is OrderStatus.OnHold -> OrderResult.Success(copy(
            status = OrderStatus.Processing(warehouseId = warehouseId),
            updatedAt = Instant.now()
        ))
        else -> OrderResult.Failure("Cannot process from state: ${status.displayName}")
    }

    fun ship(trackingNumber: String, carrier: String, estimatedDelivery: String): OrderResult<Order> =
        when (status) {
            is OrderStatus.Processing -> OrderResult.Success(copy(
                status = OrderStatus.Shipped(
                    trackingNumber = trackingNumber,
                    carrier = carrier,
                    estimatedDelivery = estimatedDelivery
                ),
                updatedAt = Instant.now()
            ))
            else -> OrderResult.Failure("Cannot ship from state: ${status.displayName}")
        }

    fun deliver(signedBy: String? = null): OrderResult<Order> = when (status) {
        is OrderStatus.Shipped -> OrderResult.Success(copy(
            status = OrderStatus.Delivered(signedBy = signedBy),
            updatedAt = Instant.now()
        ))
        else -> OrderResult.Failure("Cannot deliver from state: ${status.displayName}")
    }

    fun cancel(reason: String, cancelledBy: String): OrderResult<Order> = when {
        !status.isCancellable -> OrderResult.Failure("Cannot cancel from state: ${status.displayName}")
        else -> OrderResult.Success(copy(
            status = OrderStatus.Cancelled(reason = reason, cancelledBy = cancelledBy),
            updatedAt = Instant.now()
        ))
    }

    companion object {
        fun create(
            customerId: CustomerId,
            shippingAddress: ShippingAddress,
            lineItems: List<OrderLineItem>,
            discountCode: String? = null,
            notes: String? = null
        ): OrderResult<Order> {
            if (lineItems.isEmpty()) return OrderResult.Failure("Order must have at least one item")
            if (lineItems.any { it.quantity <= 0 }) return OrderResult.Failure("All quantities must be positive")

            val orderId = OrderId("ORD-${UUID.randomUUID().toString().replace("-", "").take(8).uppercase()}")
            return OrderResult.Success(Order(
                id = orderId,
                customerId = customerId,
                lineItems = lineItems,
                shippingAddress = shippingAddress,
                discountCode = discountCode,
                notes = notes
            ))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Result Type — typed error handling without exceptions
// ─────────────────────────────────────────────────────────────────────────────

sealed class OrderResult<out T> {
    data class Success<T>(val value: T) : OrderResult<T>()
    data class Failure(val reason: String, val cause: Throwable? = null) : OrderResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): T? = (this as? Success)?.value
    fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failure -> throw IllegalStateException(reason, cause)
    }

    fun getOrElse(default: @UnsafeVariance T): T = (this as? Success)?.value ?: default

    inline fun <R> map(transform: (T) -> R): OrderResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }

    inline fun onSuccess(action: (T) -> Unit): OrderResult<T> {
        if (this is Success) action(value)
        return this
    }

    inline fun onFailure(action: (String) -> Unit): OrderResult<T> {
        if (this is Failure) action(reason)
        return this
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sample Data Factory (for tests and demos)
// ─────────────────────────────────────────────────────────────────────────────

object SampleData {
    val keyboard = Product.create("KBD-01", "Mechanical Keyboard", "Cherry MX Blue switches", 79.99, ProductCategory.ELECTRONICS, 0.8, 50)
    val mouse = Product.create("MSE-01", "Wireless Mouse", "Ergonomic design", 29.99, ProductCategory.ELECTRONICS, 0.2, 100)
    val laptop = Product.create("LPT-01", "Developer Laptop", "16GB RAM, 512GB SSD", 1299.99, ProductCategory.ELECTRONICS, 2.1, 15)
    val book = Product.create("BK-KOTLIN-01", "Kotlin in Action", "2nd Edition", 49.99, ProductCategory.BOOKS, 0.5, 200)

    val standardAddress = ShippingAddress(
        recipientName = "Jane Smith",
        street1 = "123 Main Street",
        street2 = "Apt 4B",
        city = "New York",
        stateOrProvince = "NY",
        postalCode = "10001",
        country = "US"
    )

    fun createSampleOrder(): Order {
        val result = Order.create(
            customerId = CustomerId("CUST-A1B2C3"),
            shippingAddress = standardAddress,
            lineItems = listOf(
                OrderLineItem(keyboard.id, keyboard.name, 1, keyboard.basePrice),
                OrderLineItem(mouse.id, mouse.name, 2, mouse.basePrice, Money.of(5.0))
            ),
            discountCode = "JAVA-DEV-20"
        )
        return result.getOrThrow()
    }
}

fun main() {
    println("=== Kata 21: Order Domain ===\n")

    val order = SampleData.createSampleOrder()
    println("Created order: ${order.id}")
    println("Status: ${order.status.displayName}")
    println("Items: ${order.itemCount}")
    println("Subtotal: ${order.subtotal}")
    println("Savings: ${order.totalSavings}")

    println("\nOrder lifecycle:")
    order
        .also { println("  Draft: ${it.status.displayName}") }
        .submit()
        .onFailure { println("  Submit failed: $it") }
        .map { submitted ->
            submitted
                .also { println("  Submitted: ${it.status.displayName}") }
                .confirm("2024-02-15", "CONF-001")
                .getOrThrow()
                .also { println("  Confirmed: ${it.status.displayName}") }
                .startProcessing("WH-EAST-01")
                .getOrThrow()
                .also { println("  Processing: ${it.status.displayName}") }
                .ship("1Z999AA10123456784", "UPS", "2024-02-16")
                .getOrThrow()
                .also { println("  Shipped: ${it.status.displayName}") }
                .deliver("John Doe")
                .getOrThrow()
                .also { println("  Delivered: ${it.status.displayName}") }
        }
        .onFailure { println("  Pipeline failed: $it") }

    println("\nCancellation test:")
    order.submit()
        .map { it.cancel("Customer changed mind", "CUSTOMER") }
        .map { it.getOrThrow() }
        .onSuccess { println("  Cancelled: ${it.status.displayName}") }
}

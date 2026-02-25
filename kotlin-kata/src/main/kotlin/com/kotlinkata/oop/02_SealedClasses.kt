package com.kotlinkata.oop

/**
 * # Kata 07: Sealed Classes — Algebraic Data Types
 *
 * Sealed classes are Kotlin's most powerful feature for domain modeling.
 * They model a CLOSED set of subclasses — the compiler knows all possible subtypes.
 *
 * Java equivalents:
 *   - Java 17 sealed classes (similar syntax, but Kotlin is more flexible)
 *   - enum (but enums can't carry per-variant data)
 *   - abstract class + instanceof chains (verbose, not exhaustive)
 *
 * Why sealed classes over enums:
 *   - Each variant can hold DIFFERENT data (enums can't)
 *   - Object variants, data class variants, and class variants can coexist
 *   - when expressions are exhaustive — compiler enforces all cases
 *
 * Why sealed classes over abstract classes:
 *   - Closed hierarchy — no external subclassing
 *   - Compiler verifies exhaustiveness in when expressions
 *   - Better expressive power for discriminated unions
 *
 * Topics:
 *   - Sealed class vs sealed interface
 *   - Result/Error pattern (replacing exceptions in service layers)
 *   - State machines with sealed classes
 *   - Recursive sealed structures
 *   - Exhaustive when with sealed classes
 */

// ─────────────────────────────────────────────────────────────────────────────
// 1. Basic Sealed Class — The Result Pattern
// ─────────────────────────────────────────────────────────────────────────────

/**
 * THE most important pattern for sealed classes: representing operation outcomes.
 *
 * Java approach: checked exceptions (verbose) or unchecked exceptions (invisible)
 *
 * Kotlin approach: model success and failure in the TYPE SYSTEM.
 * The caller MUST handle both cases — they're enforced by the when expression.
 *
 * Kotlin actually has a built-in Result<T> type, but building your own gives
 * you richer error types. Both patterns are shown here.
 */
sealed class OrderResult<out T> {
    data class Success<T>(val value: T) : OrderResult<T>()
    sealed class Failure : OrderResult<Nothing>() {
        data class NotFound(val id: String) : Failure()
        data class ValidationError(val field: String, val message: String) : Failure()
        data class PaymentDeclined(val reason: String, val code: Int) : Failure()
        data class SystemError(val message: String, val cause: Throwable? = null) : Failure()
    }
}

// Service that returns typed results instead of throwing exceptions
object OrderService {
    fun findOrder(orderId: String): OrderResult<PersistedOrder> = when {
        orderId.isBlank() ->
            OrderResult.Failure.ValidationError("orderId", "must not be blank")
        orderId.startsWith("ORD-") ->
            OrderResult.Success(PersistedOrder(orderId, "CUST-1", 149.99))
        else ->
            OrderResult.Failure.NotFound(orderId)
    }

    fun placeOrder(customerId: String, total: Double): OrderResult<String> = when {
        customerId.isBlank() ->
            OrderResult.Failure.ValidationError("customerId", "must not be blank")
        total <= 0 ->
            OrderResult.Failure.ValidationError("total", "must be positive")
        total > 10_000 ->
            OrderResult.Failure.PaymentDeclined("Amount exceeds single-transaction limit", 4002)
        else ->
            OrderResult.Success("ORD-${System.currentTimeMillis()}")
    }
}

data class PersistedOrder(val id: String, val customerId: String, val total: Double)

fun handleOrderResult() {
    val result = OrderService.findOrder("ORD-12345")

    // Exhaustive when — the compiler ensures ALL cases are handled
    val message = when (result) {
        is OrderResult.Success         -> "Found order: ${result.value.id} (total: ${result.value.total})"
        is OrderResult.Failure.NotFound -> "Order ${result.id} not found"
        is OrderResult.Failure.ValidationError -> "Validation failed on '${result.field}': ${result.message}"
        is OrderResult.Failure.PaymentDeclined -> "Payment declined (code ${result.code}): ${result.reason}"
        is OrderResult.Failure.SystemError -> "System error: ${result.message}"
    }
    println(message)

    // Chain operations with fold-style handling
    val total = when (val r = OrderService.placeOrder("CUST-1", 299.99)) {
        is OrderResult.Success -> "Order placed: ${r.value}"
        is OrderResult.Failure -> "Failed: $r"
    }
    println(total)
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Sealed Interface — Multiple Inheritance Allowed
// ─────────────────────────────────────────────────────────────────────────────

/**
 * sealed interface is more flexible than sealed class:
 *   - Subtypes CAN extend other classes (sealed class subtypes can't extend another class)
 *   - Can implement multiple sealed interfaces
 *   - Preferred when you don't need shared state/implementation in the parent
 */
sealed interface DomainEvent {
    val occurredAt: Long get() = System.currentTimeMillis()
    val aggregateId: String
}

sealed interface OrderEvent : DomainEvent {
    data class OrderPlaced(override val aggregateId: String, val customerId: String, val total: Double) : OrderEvent
    data class OrderConfirmed(override val aggregateId: String, val estimatedDelivery: String) : OrderEvent
    data class OrderShipped(override val aggregateId: String, val trackingNumber: String) : OrderEvent
    data class OrderCancelled(override val aggregateId: String, val reason: String) : OrderEvent
    data class OrderDelivered(override val aggregateId: String) : OrderEvent
}

sealed interface PaymentEvent : DomainEvent {
    data class PaymentInitiated(override val aggregateId: String, val amount: Double) : PaymentEvent
    data class PaymentCaptured(override val aggregateId: String, val transactionId: String) : PaymentEvent
    data class PaymentFailed(override val aggregateId: String, val reason: String) : PaymentEvent
    data class RefundIssued(override val aggregateId: String, val refundId: String, val amount: Double) : PaymentEvent
}

// Event handler — exhaustive per sealed interface
fun handleOrderEvent(event: OrderEvent): String = when (event) {
    is OrderEvent.OrderPlaced    -> "New order ${event.aggregateId} by ${event.customerId} for $${event.total}"
    is OrderEvent.OrderConfirmed -> "Order ${event.aggregateId} confirmed, delivery: ${event.estimatedDelivery}"
    is OrderEvent.OrderShipped   -> "Order ${event.aggregateId} shipped, tracking: ${event.trackingNumber}"
    is OrderEvent.OrderCancelled -> "Order ${event.aggregateId} cancelled: ${event.reason}"
    is OrderEvent.OrderDelivered -> "Order ${event.aggregateId} delivered"
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Sealed Classes for State Machines
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Sealed classes are perfect for modeling finite state machines.
 * The compiler ensures all states are handled, and each state can carry
 * only the data relevant to that state.
 *
 * Compare to Java's approach:
 *   - Status enum + nullable fields = "optional" data that's hard to reason about
 *   - Each state has exactly the data it needs, no more, no less
 */
sealed class ShipmentState {
    object Preparing : ShipmentState()
    data class ReadyForPickup(val warehouseId: String) : ShipmentState()
    data class InTransit(
        val carrier: String,
        val trackingNumber: String,
        val estimatedDelivery: String
    ) : ShipmentState()
    data class OutForDelivery(val driverName: String, val estimatedTime: String) : ShipmentState()
    data class Delivered(val signedBy: String, val deliveredAt: String) : ShipmentState()
    data class DeliveryFailed(val reason: String, val attemptCount: Int) : ShipmentState()
    data class ReturnInitiated(val returnId: String, val reason: String) : ShipmentState()
}

// State transition function — only valid transitions compile
fun transition(state: ShipmentState, action: String): ShipmentState = when (state) {
    is ShipmentState.Preparing -> when (action) {
        "ready" -> ShipmentState.ReadyForPickup("WH-001")
        else    -> state  // no transition
    }
    is ShipmentState.ReadyForPickup -> when (action) {
        "picked_up" -> ShipmentState.InTransit("FedEx", "TRK-${System.currentTimeMillis()}", "2024-02-15")
        else        -> state
    }
    is ShipmentState.InTransit -> when (action) {
        "out_for_delivery" -> ShipmentState.OutForDelivery("John Driver", "14:00-18:00")
        else               -> state
    }
    is ShipmentState.OutForDelivery -> when (action) {
        "delivered"       -> ShipmentState.Delivered("Jane Smith", "2024-02-15 14:32")
        "failed"          -> ShipmentState.DeliveryFailed("No one home", 1)
        else              -> state
    }
    is ShipmentState.Delivered,
    is ShipmentState.ReturnInitiated -> state  // terminal states
    is ShipmentState.DeliveryFailed  -> when (action) {
        "retry"          -> ShipmentState.InTransit(
            "FedEx", "TRK-RETRY-${state.attemptCount + 1}", "tomorrow")
        "return"         -> ShipmentState.ReturnInitiated("RET-001", "Max attempts exceeded")
        else             -> state
    }
}

fun describeState(state: ShipmentState): String = when (state) {
    is ShipmentState.Preparing         -> "Preparing shipment"
    is ShipmentState.ReadyForPickup    -> "Ready at warehouse ${state.warehouseId}"
    is ShipmentState.InTransit         -> "In transit via ${state.carrier} (${state.trackingNumber})"
    is ShipmentState.OutForDelivery    -> "Out for delivery with ${state.driverName}"
    is ShipmentState.Delivered         -> "Delivered to ${state.signedBy} at ${state.deliveredAt}"
    is ShipmentState.DeliveryFailed    -> "Delivery failed (attempt ${state.attemptCount}): ${state.reason}"
    is ShipmentState.ReturnInitiated   -> "Return initiated: ${state.reason}"
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Sealed Classes for Validation
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Model validation results as sealed classes to accumulate ALL errors,
 * not just the first one (fail-fast vs fail-all).
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val errors: List<String>) : ValidationResult()

    operator fun plus(other: ValidationResult): ValidationResult = when {
        this is Valid && other is Valid    -> Valid
        this is Invalid && other is Valid  -> this
        this is Valid && other is Invalid  -> other
        this is Invalid && other is Invalid -> Invalid(errors + other.errors)
        else -> throw IllegalStateException("Unreachable")
    }

    val isValid: Boolean get() = this is Valid
}

data class OrderRequest(
    val customerId: String,
    val items: List<String>,
    val totalAmount: Double,
    val shippingAddress: String?
)

fun validateOrderRequest(request: OrderRequest): ValidationResult {
    val customerValidation: ValidationResult = when {
        request.customerId.isBlank() -> ValidationResult.Invalid(listOf("customerId is required"))
        !request.customerId.startsWith("CUST-") -> ValidationResult.Invalid(listOf("customerId must start with CUST-"))
        else -> ValidationResult.Valid
    }

    val itemsValidation: ValidationResult = when {
        request.items.isEmpty() -> ValidationResult.Invalid(listOf("At least one item is required"))
        request.items.size > 100 -> ValidationResult.Invalid(listOf("Maximum 100 items per order"))
        else -> ValidationResult.Valid
    }

    val amountValidation: ValidationResult = when {
        request.totalAmount <= 0 -> ValidationResult.Invalid(listOf("Total amount must be positive"))
        request.totalAmount > 1_000_000 -> ValidationResult.Invalid(listOf("Total exceeds maximum allowed"))
        else -> ValidationResult.Valid
    }

    val addressValidation: ValidationResult = when {
        request.shippingAddress.isNullOrBlank() -> ValidationResult.Invalid(listOf("Shipping address is required"))
        else -> ValidationResult.Valid
    }

    // Combine all validations — collect ALL errors (not just the first)
    return customerValidation + itemsValidation + amountValidation + addressValidation
}

fun main() {
    println("=== Kata 07: Sealed Classes ===\n")

    println("--- Result Pattern ---")
    handleOrderResult()
    println()

    println("--- Event Sourcing ---")
    val events = listOf(
        OrderEvent.OrderPlaced("ORD-001", "CUST-1", 299.99),
        OrderEvent.OrderConfirmed("ORD-001", "2024-02-15"),
        OrderEvent.OrderShipped("ORD-001", "1Z999AA10123456784")
    )
    events.forEach { println(handleOrderEvent(it)) }
    println()

    println("--- State Machine ---")
    var state: ShipmentState = ShipmentState.Preparing
    val actions = listOf("ready", "picked_up", "out_for_delivery", "delivered")
    for (action in actions) {
        state = transition(state, action)
        println("After '$action': ${describeState(state)}")
    }
    println()

    println("--- Validation ---")
    val invalid = OrderRequest("", emptyList(), -5.0, null)
    when (val result = validateOrderRequest(invalid)) {
        is ValidationResult.Valid   -> println("Valid!")
        is ValidationResult.Invalid -> result.errors.forEach { println("❌ $it") }
    }

    val valid = OrderRequest("CUST-001", listOf("SKU-1"), 49.99, "123 Main St")
    println("Valid order: ${validateOrderRequest(valid).isValid}")
}

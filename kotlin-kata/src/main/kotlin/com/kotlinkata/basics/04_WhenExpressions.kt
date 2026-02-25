package com.kotlinkata.basics

/**
 * # Kata 04: when Expressions — Kotlin's Pattern Matching
 *
 * `when` replaces Java's `switch` but is far more powerful:
 *   - Works as both a STATEMENT and an EXPRESSION (returns a value)
 *   - Exhaustive checks (no fall-through, no missing cases)
 *   - Pattern matching: ranges, types, conditions
 *   - Smart casts within branches
 *   - Works with sealed classes for compile-time exhaustiveness
 *
 * Java engineers: this is what you wished switch expressions were.
 *
 * Topics:
 *   - when as expression (returns a value)
 *   - when with ranges and conditions
 *   - when with type checks (smart casts)
 *   - when with sealed classes (exhaustive)
 *   - when replacing complex if-else chains
 */

// ─────────────────────────────────────────────────────────────────────────────
// Domain Setup
// ─────────────────────────────────────────────────────────────────────────────

enum class PaymentStatus { PENDING, AUTHORIZED, CAPTURED, REFUNDED, FAILED, CHARGEBACK }
enum class ShippingCarrier { FEDEX, UPS, USPS, DHL }

data class Product(val sku: String, val name: String, val weightKg: Double, val category: String)

// ─────────────────────────────────────────────────────────────────────────────
// 1. when as Expression
// ─────────────────────────────────────────────────────────────────────────────

/**
 * when can RETURN A VALUE — assign it to a variable, use it as a function body.
 *
 * Java switch expression (Java 14+):
 *   String label = switch(status) {
 *       case PENDING   -> "Awaiting payment";
 *       case CAPTURED  -> "Payment received";
 *       default        -> "Unknown";
 *   };
 *
 * Kotlin when expression:
 *   val label = when (status) {
 *       PENDING   -> "Awaiting payment"
 *       CAPTURED  -> "Payment received"
 *       else      -> "Unknown"
 *   }
 *
 * When used with sealed classes or enums, `else` is not needed (exhaustive).
 */
fun paymentStatusLabel(status: PaymentStatus): String = when (status) {
    PaymentStatus.PENDING     -> "Awaiting payment"
    PaymentStatus.AUTHORIZED  -> "Payment authorized"
    PaymentStatus.CAPTURED    -> "Payment received"
    PaymentStatus.REFUNDED    -> "Refund issued"
    PaymentStatus.FAILED      -> "Payment failed"
    PaymentStatus.CHARGEBACK  -> "Chargeback in progress"
    // No `else` needed — all enum values covered. Compiler enforces this!
}

fun carrierTrackingUrl(carrier: ShippingCarrier, trackingNumber: String): String = when (carrier) {
    ShippingCarrier.FEDEX -> "https://fedex.com/track/$trackingNumber"
    ShippingCarrier.UPS   -> "https://ups.com/track/$trackingNumber"
    ShippingCarrier.USPS  -> "https://usps.com/track/$trackingNumber"
    ShippingCarrier.DHL   -> "https://dhl.com/track/$trackingNumber"
}

// Multiple values in one branch
fun isTerminalStatus(status: PaymentStatus): Boolean = when (status) {
    PaymentStatus.CAPTURED,
    PaymentStatus.REFUNDED,
    PaymentStatus.CHARGEBACK -> true
    else -> false
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. when with Ranges and Conditions
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Unlike Java's switch, Kotlin's when supports arbitrary conditions.
 * No more complex if-else chains for range checks!
 */
fun shippingCost(weightKg: Double, carrier: ShippingCarrier): Double {
    val baseRate = when (carrier) {
        ShippingCarrier.FEDEX -> 8.00
        ShippingCarrier.UPS   -> 7.50
        ShippingCarrier.USPS  -> 5.00
        ShippingCarrier.DHL   -> 9.00
    }

    val weightMultiplier = when {    // when with no argument = if-else chain
        weightKg <= 0.5   -> 1.0
        weightKg <= 2.0   -> 1.5
        weightKg <= 5.0   -> 2.0
        weightKg <= 10.0  -> 3.0
        else              -> 4.5
    }

    return baseRate * weightMultiplier
}

fun discountTier(orderTotal: Double): String = when (orderTotal) {
    in 0.0..49.99    -> "No discount"
    in 50.0..99.99   -> "5% discount"
    in 100.0..249.99 -> "10% discount"
    in 250.0..499.99 -> "15% discount"
    else             -> "20% discount"
}

fun validateSku(sku: String): String = when {
    sku.isBlank()          -> "SKU cannot be blank"
    sku.length < 4         -> "SKU too short (min 4 chars)"
    sku.length > 20        -> "SKU too long (max 20 chars)"
    !sku.matches(Regex("[A-Z0-9-]+")) -> "SKU must be uppercase alphanumeric with hyphens"
    else                   -> "Valid"
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. when with Type Checks and Smart Casts
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Combining `when` with `is` gives you pattern matching with smart casts.
 * After matching a branch, the compiler knows the exact type — no cast needed.
 *
 * This is the Kotlin equivalent of Java's pattern matching for switch (Java 21).
 */
sealed interface PricingRule {
    data class FixedDiscount(val amount: Double) : PricingRule
    data class PercentDiscount(val percent: Double) : PricingRule
    data class BuyXGetY(val buy: Int, val get: Int) : PricingRule
    data class MinimumOrder(val minimum: Double, val discount: Double) : PricingRule
    object FreeShipping : PricingRule
}

fun applyPricingRule(price: Double, quantity: Int, rule: PricingRule): Double = when (rule) {
    is PricingRule.FixedDiscount    -> maxOf(0.0, price - rule.amount)
    is PricingRule.PercentDiscount  -> price * (1 - rule.percent / 100)
    is PricingRule.BuyXGetY         -> {
        // Smart cast: rule is PricingRule.BuyXGetY here, accessing rule.buy, rule.get
        val freeItems = quantity / (rule.buy + rule.get)
        val effectiveQty = quantity - freeItems
        price * effectiveQty / quantity
    }
    is PricingRule.MinimumOrder     ->
        if (price >= rule.minimum) price - rule.discount else price
    is PricingRule.FreeShipping     -> price  // no price change, handled separately
}

fun describePricingRule(rule: PricingRule): String = when (rule) {
    is PricingRule.FixedDiscount    -> "$${rule.amount} off"
    is PricingRule.PercentDiscount  -> "${rule.percent}% off"
    is PricingRule.BuyXGetY         -> "Buy ${rule.buy} get ${rule.get} free"
    is PricingRule.MinimumOrder     -> "$${ rule.discount} off orders over $${rule.minimum}"
    is PricingRule.FreeShipping     -> "Free shipping"
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. when with Sealed Classes — Exhaustive Pattern Matching
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Sealed classes + when = EXHAUSTIVE pattern matching.
 * The compiler ERRORS if you forget to handle a case.
 *
 * This is the most powerful combination in Kotlin for modeling state machines
 * and domain error types. You can't forget to handle a case — the compiler
 * won't let you.
 */
sealed class OrderState {
    object Draft : OrderState()
    data class Pending(val customerId: String, val itemCount: Int) : OrderState()
    data class Confirmed(val orderId: String, val estimatedDelivery: String) : OrderState()
    data class Shipped(val orderId: String, val trackingNumber: String, val carrier: ShippingCarrier) : OrderState()
    data class Delivered(val orderId: String, val deliveredAt: String) : OrderState()
    data class Cancelled(val reason: String) : OrderState()
    data class Refunded(val orderId: String, val refundAmount: Double) : OrderState()
}

fun orderStateMessage(state: OrderState): String = when (state) {
    is OrderState.Draft      -> "Draft order — not yet submitted"
    is OrderState.Pending    -> "Order for ${state.customerId} with ${state.itemCount} items is pending"
    is OrderState.Confirmed  -> "Order ${state.orderId} confirmed, delivery by ${state.estimatedDelivery}"
    is OrderState.Shipped    -> "Order ${state.orderId} shipped via ${state.carrier}, tracking: ${state.trackingNumber}"
    is OrderState.Delivered  -> "Order ${state.orderId} delivered at ${state.deliveredAt}"
    is OrderState.Cancelled  -> "Order cancelled: ${state.reason}"
    is OrderState.Refunded   -> "Order ${state.orderId} refunded: $${state.refundAmount}"
    // No `else` — if you add a new OrderState subclass, the compiler will force you to handle it here!
}

// Transition logic — compiler ensures all states are handled
fun canCancelOrder(state: OrderState): Boolean = when (state) {
    is OrderState.Draft,
    is OrderState.Pending,
    is OrderState.Confirmed -> true
    is OrderState.Shipped,
    is OrderState.Delivered,
    is OrderState.Cancelled,
    is OrderState.Refunded  -> false
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. when as Guard with Captured Variables (Kotlin 1.9+)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * You can capture the subject in when for use in branches.
 * A common pattern: when (val x = someNullable) { null -> ...; else -> use x }
 */
fun processProductCategory(product: Product): String {
    val shippingClass = when (val weight = product.weightKg) {
        in 0.0..0.1  -> "letter"
        in 0.1..2.0  -> "parcel"
        in 2.0..30.0 -> "heavy"
        else         -> "freight"  // weight > 30kg
    }

    return when (product.category) {
        "electronics"  -> "Handle with care, $shippingClass shipping"
        "perishable"   -> "Temperature controlled, $shippingClass shipping"
        "hazmat"       -> "Special handling required, $shippingClass shipping"
        else           -> "Standard $shippingClass shipping"
    }
}

fun main() {
    println("=== Kata 04: when Expressions ===\n")

    // Enum exhaustiveness
    PaymentStatus.entries.forEach { status ->
        println("${status.name}: ${paymentStatusLabel(status)}")
    }

    println()
    println("Weight-based shipping:")
    listOf(0.3, 1.5, 4.0, 8.0).forEach { w ->
        println("  ${w}kg via FedEx: ${"%.2f".format(shippingCost(w, ShippingCarrier.FEDEX))}")
    }

    println()
    println("Discount tiers:")
    listOf(25.0, 75.0, 150.0, 300.0, 600.0).forEach { total ->
        println("  $${total}: ${discountTier(total)}")
    }

    println()
    println("Pricing rules:")
    val rules = listOf(
        PricingRule.FixedDiscount(10.0),
        PricingRule.PercentDiscount(15.0),
        PricingRule.BuyXGetY(2, 1),
        PricingRule.FreeShipping
    )
    rules.forEach { rule ->
        println("  ${describePricingRule(rule)}: ${"%.2f".format(applyPricingRule(100.0, 3, rule))}")
    }

    println()
    println("Order states:")
    val states = listOf(
        OrderState.Pending("CUST-1", 3),
        OrderState.Confirmed("ORD-001", "2024-02-15"),
        OrderState.Shipped("ORD-001", "1Z999AA10123456784", ShippingCarrier.UPS),
        OrderState.Cancelled("Customer request")
    )
    states.forEach { state ->
        println("  ${orderStateMessage(state)}")
        println("  Can cancel: ${canCancelOrder(state)}")
    }
}

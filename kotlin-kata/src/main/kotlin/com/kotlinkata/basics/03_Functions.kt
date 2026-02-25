package com.kotlinkata.basics

/**
 * # Kata 03: Functions — Kotlin's Superpower Over Java
 *
 * Kotlin functions are significantly more expressive than Java methods.
 * Features like named parameters, defaults, extension functions, and higher-order
 * functions eliminate entire patterns (Builder, Overloaded methods, Utility classes).
 *
 * Topics:
 *   - Named parameters (eliminates most Builder patterns)
 *   - Default parameters (eliminates method overloading for defaults)
 *   - Single-expression functions
 *   - Local functions (functions inside functions)
 *   - Infix functions (DSL-like syntax)
 *   - Top-level functions (no more utility classes)
 *   - vararg (like Java's varargs)
 *   - Function return types and Unit
 */

// ─────────────────────────────────────────────────────────────────────────────
// 1. Named Parameters — Say Goodbye to Most Builder Patterns
// ─────────────────────────────────────────────────────────────────────────────

/**
 * In Java, you use the Builder pattern to avoid:
 *   new Order("ORD-1", "CUST-1", null, null, false, true, null)  // what are these booleans??
 *
 * Kotlin named parameters solve this:
 *   createOrder(id = "ORD-1", customerId = "CUST-1", expedited = true)
 *
 * Benefits:
 *   - Self-documenting call sites
 *   - Can pass in any order
 *   - Combine with defaults (see below) to eliminate most overloads
 */
data class OrderRequest(
    val customerId: String,
    val items: List<String>,
    val discountCode: String? = null,
    val expedited: Boolean = false,
    val notes: String? = null,
    val giftWrap: Boolean = false
)

fun createOrder(request: OrderRequest): String {
    val shipping = if (request.expedited) "express" else "standard"
    return "Order for ${request.customerId} via $shipping (${request.items.size} items)"
}

fun namedParameterExamples() {
    // Java-style: positional — error-prone and unreadable
    // createOrder(OrderRequest("CUST-1", listOf("SKU-1"), null, true, null, false))

    // Kotlin named params: clear intent, order doesn't matter
    val simpleOrder = createOrder(
        request = OrderRequest(
            customerId = "CUST-001",
            items = listOf("SKU-1", "SKU-2"),
            expedited = true
        )
    )

    // You can mix positional and named (positional must come first)
    val giftOrder = createOrder(
        OrderRequest(
            customerId = "CUST-002",
            items = listOf("SKU-3"),
            giftWrap = true,
            notes = "Happy Birthday!"
        )
    )

    println(simpleOrder)
    println(giftOrder)
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Default Parameters — Eliminates Overloaded Methods
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Java engineers write 4 overloaded versions to support optional parameters:
 *   void notify(String userId) { notify(userId, "default", false); }
 *   void notify(String userId, String subject) { notify(userId, subject, false); }
 *   void notify(String userId, String subject, boolean urgent) { ... }
 *
 * Kotlin replaces ALL of them with one function + default values:
 *   fun notify(userId: String, subject: String = "Notification", urgent: Boolean = false)
 *
 * @JvmOverloads generates Java-compatible overloads if you need Java interop.
 */
fun sendOrderNotification(
    customerId: String,
    subject: String = "Order Update",
    body: String = "Your order status has changed.",
    urgent: Boolean = false,
    channels: List<String> = listOf("email")
) {
    val priority = if (urgent) "[URGENT] " else ""
    val channelStr = channels.joinToString(", ")
    println("[$channelStr] → $customerId: $priority$subject")
}

fun defaultParameterExamples() {
    // All defaults
    sendOrderNotification("CUST-001")

    // Override just what you need — named params make this safe
    sendOrderNotification("CUST-002", urgent = true)

    // Custom channels
    sendOrderNotification(
        customerId = "CUST-003",
        subject = "Order Shipped!",
        channels = listOf("email", "sms", "push")
    )

    // All explicit
    sendOrderNotification(
        customerId = "CUST-004",
        subject = "Payment Failed",
        body = "Please update your payment method.",
        urgent = true,
        channels = listOf("email", "sms")
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Single-Expression Functions
// ─────────────────────────────────────────────────────────────────────────────

/**
 * When a function body is a single expression, you can use = instead of { return ... }.
 * The return type is inferred from the expression.
 *
 * Perfect for:
 *   - Simple transformations
 *   - Delegates / wrappers
 *   - Properties computed from other values
 */

// Multi-line version:
fun calculateTaxVerbose(amount: Double, rate: Double): Double {
    return amount * rate
}

// Single-expression version: cleaner, return type inferred
fun calculateTax(amount: Double, rate: Double): Double = amount * rate

fun discountedPrice(price: Double, discountPct: Double): Double = price * (1 - discountPct)

fun formatOrderId(id: Int): String = "ORD-%06d".format(id)

fun isValidOrderId(id: String): Boolean = id.startsWith("ORD-") && id.length >= 8

// Works with when too
fun orderStatusLabel(isPaid: Boolean, isShipped: Boolean): String = when {
    isPaid && isShipped -> "Complete"
    isPaid              -> "Processing"
    else                -> "Pending payment"
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Local Functions — Functions Inside Functions
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Kotlin allows functions inside functions.
 *
 * Use for:
 *   - Helper logic used only within one function (avoid polluting the class scope)
 *   - Recursive algorithms with shared state
 *   - Breaking up complex logic without exposing private helper methods
 *
 * Local functions can close over the enclosing function's variables (like lambdas).
 */
fun processOrderItems(items: List<OrderItem>): ProcessingResult {

    // Local function — not visible outside processOrderItems
    fun validateItem(item: OrderItem): String? {
        if (item.quantity <= 0) return "Item ${item.sku}: quantity must be positive"
        if (item.unitPrice < 0) return "Item ${item.sku}: price cannot be negative"
        if (item.sku.isBlank()) return "Item cannot have blank SKU"
        return null  // null = valid
    }

    // Local function captures `items` from outer scope
    fun calculateSubtotal(): Double = items.sumOf { it.quantity * it.unitPrice }

    // Use the local functions
    val errors = items.mapNotNull { validateItem(it) }
    if (errors.isNotEmpty()) {
        return ProcessingResult.Invalid(errors)
    }

    val subtotal = calculateSubtotal()
    return ProcessingResult.Valid(subtotal)
}

data class OrderItem(val sku: String, val quantity: Int, val unitPrice: Double)

sealed class ProcessingResult {
    data class Valid(val subtotal: Double) : ProcessingResult()
    data class Invalid(val errors: List<String>) : ProcessingResult()
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Infix Functions — Natural Language Syntax
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Mark a function with `infix` to call it without dot and parentheses:
 *   a.add(b)  →  a add b
 *
 * Requirements:
 *   - Must be a member function or extension function
 *   - Must have exactly one parameter
 *
 * You've already used infix functions: `to`, `until`, `downTo`, `and`, `or`, `xor`
 *   "key" to "value"  is actually  "key".to("value")
 *   1 until 10        is actually  1.until(10)
 */
data class Money(val amount: Double, val currency: String) {
    infix fun plus(other: Money): Money {
        require(currency == other.currency) { "Currency mismatch: $currency vs ${other.currency}" }
        return Money(amount + other.amount, currency)
    }

    infix fun isGreaterThan(other: Money): Boolean {
        require(currency == other.currency) { "Currency mismatch" }
        return amount > other.amount
    }
}

infix fun String.mustMatch(pattern: Regex): Boolean = pattern.matches(this)
infix fun Int.percentOf(total: Int): Double = (this.toDouble() / total) * 100

fun infixExamples() {
    val price = Money(29.99, "USD")
    val tax = Money(2.40, "USD")
    val total = price plus tax  // infix call

    println("Total: $total")
    println("Tax is ${if (tax isGreaterThan price) "more" else "less"} than price")

    // Built-in infix functions
    val range = 1 until 10      // infix
    val pair = "key" to "value" // infix (used in mapOf)

    // Custom infix for validation — reads like natural language
    val orderId = "ORD-12345"
    val isValid = orderId mustMatch Regex("ORD-\\d+")
    println("'$orderId' is valid: $isValid")

    // Percentage calculation
    val fulfilled = 75 percentOf 100
    println("$fulfilled% of orders fulfilled")
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. Top-Level Functions — No More Utility Classes
// ─────────────────────────────────────────────────────────────────────────────

/**
 * In Java you write utility classes: OrderUtils.calculateTotal(items)
 * In Kotlin, functions live at the package level — no class wrapper needed!
 *
 * Java:
 *   public class OrderUtils {
 *       public static double calculateTotal(List<OrderItem> items) { ... }
 *   }
 *   OrderUtils.calculateTotal(items);  // usage
 *
 * Kotlin:
 *   fun calculateTotal(items: List<OrderItem>): Double { ... }  // top-level
 *   calculateTotal(items)  // usage — cleaner
 *
 * From Java, top-level functions compile to static methods on a class named
 * after the file (OrderUtilsKt.calculateTotal(items)) — fully interoperable.
 */
fun calculateOrderTotal(items: List<OrderItem>, taxRate: Double = 0.08): Double {
    val subtotal = items.sumOf { it.quantity * it.unitPrice }
    return subtotal * (1 + taxRate)
}

fun generateOrderId(): String = "ORD-${System.currentTimeMillis()}"

fun isEligibleForFreeShipping(total: Double, membershipTier: String): Boolean =
    total >= 50.0 || membershipTier == "PREMIUM"

// ─────────────────────────────────────────────────────────────────────────────
// 7. vararg — Variable-Length Arguments
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Like Java's varargs (String... args), but spread operator * unpacks arrays.
 */
fun applyDiscounts(price: Double, vararg discountPcts: Double): Double {
    // discountPcts is a DoubleArray inside the function
    return discountPcts.fold(price) { acc, discount -> acc * (1 - discount) }
}

fun tagOrder(orderId: String, vararg tags: String): Map<String, List<String>> =
    mapOf(orderId to tags.toList())

fun varargExamples() {
    val original = 100.0
    println("10% off: ${applyDiscounts(original, 0.10)}")
    println("10% + 5% off: ${applyDiscounts(original, 0.10, 0.05)}")

    val discounts = doubleArrayOf(0.10, 0.05, 0.02)
    println("Multiple discounts: ${applyDiscounts(original, *discounts)}")  // spread operator

    println(tagOrder("ORD-001", "express", "gift", "fragile"))
}

fun main() {
    println("=== Kata 03: Functions ===\n")
    namedParameterExamples()
    println()
    defaultParameterExamples()
    println()
    println("Tax: ${calculateTax(100.0, 0.08)}")
    println("Discounted: ${discountedPrice(50.0, 0.20)}")
    println("Order ID: ${formatOrderId(42)}")
    println("Status: ${orderStatusLabel(isPaid = true, isShipped = false)}")
    println()
    val items = listOf(
        OrderItem("SKU-1", 2, 19.99),
        OrderItem("SKU-2", 1, 49.99)
    )
    println("Processing: ${processOrderItems(items)}")
    println("Total: ${"%.2f".format(calculateOrderTotal(items))}")
    println()
    infixExamples()
    println()
    varargExamples()
}

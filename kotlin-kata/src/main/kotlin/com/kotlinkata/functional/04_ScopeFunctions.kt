package com.kotlinkata.functional

/**
 * # Kata 13: Scope Functions — let, run, with, apply, also
 *
 * Scope functions execute a block of code in the context of an object.
 * They don't add new capabilities — they make intent clearer and reduce repetition.
 *
 * There are 5 scope functions. The differences are:
 *   - What `this` vs `it` refers to (context object reference)
 *   - What value is returned (the object itself vs the block result)
 *
 * | Function | Context object | Return value    | Is extension |
 * |----------|---------------|-----------------|--------------|
 * | let      | it            | Lambda result   | Yes          |
 * | run      | this          | Lambda result   | Yes          |
 * | with     | this          | Lambda result   | No           |
 * | apply    | this          | Context object  | Yes          |
 * | also     | it            | Context object  | Yes          |
 *
 * Java engineers: these feel unusual at first, but they eliminate a LOT of
 * temporary variable creation and make transformation pipelines cleaner.
 *
 * RULE: Choose based on INTENT, not syntax preference.
 */

// ─────────────────────────────────────────────────────────────────────────────
// Domain
// ─────────────────────────────────────────────────────────────────────────────

data class OrderDraft(
    val customerId: String,
    val items: MutableList<DraftItem> = mutableListOf(),
    var discountCode: String? = null,
    var shippingMethod: String = "STANDARD",
    var notes: String? = null,
    var isGift: Boolean = false
)

data class DraftItem(val sku: String, val quantity: Int, val unitPrice: Double)
data class ProcessedOrder(val id: String, val customerId: String, val total: Double)

// ─────────────────────────────────────────────────────────────────────────────
// 1. let — Transform or Execute on Non-Null
// ─────────────────────────────────────────────────────────────────────────────

/**
 * `let` is the most commonly used scope function.
 *
 * Primary uses:
 *   1. Null-safe operations: `nullable?.let { doSomething(it) }`
 *   2. Scoping a variable to a block (avoid polluting outer scope)
 *   3. Chaining transformations on a value
 *
 * Returns: the lambda result
 * Context object: `it` (or rename it explicitly)
 *
 * Java equivalent:
 *   T temp = getResult();
 *   if (temp != null) { doSomething(temp); }
 */
fun letExamples() {
    val rawInput: String? = "  SAVE10  "

    // Primary use 1: null-safe execution
    val discount: Double? = rawInput?.let { code ->
        code.trim().uppercase().let { cleanCode ->
            when (cleanCode) {
                "SAVE10" -> 0.10
                "SAVE20" -> 0.20
                else -> null
            }
        }
    }
    println("Discount: $discount")

    // Primary use 2: scoping — no temp variable leaked to outer scope
    val orderTotal = getOrderTotal("ORD-001").let { rawTotal ->
        val taxed = rawTotal * 1.08
        val rounded = Math.round(taxed * 100) / 100.0
        rounded
    }
    // rawTotal, taxed, rounded are all scoped inside let — not visible here
    println("Total: $orderTotal")

    // Chain transformations
    val result = "  order-001  "
        .let { it.trim() }
        .let { it.uppercase() }
        .let { if (it.startsWith("ORD")) it else "ORD-$it" }
    println("Result: $result")
}

fun getOrderTotal(orderId: String): Double = 99.99

// ─────────────────────────────────────────────────────────────────────────────
// 2. run — Configure and Return a Result
// ─────────────────────────────────────────────────────────────────────────────

/**
 * `run` is like `let` but uses `this` instead of `it` — good when calling
 * multiple methods on the same object (avoids repeating the receiver name).
 *
 * Primary uses:
 *   1. Execute multiple operations on an object and return a result
 *   2. Compute a value using local variables (like a lambda-scoped block)
 *
 * Returns: lambda result
 * Context object: `this`
 */
fun runExamples() {
    val draft = OrderDraft("CUST-001")

    // run on a receiver — `this` is draft inside the block
    val summary = draft.run {
        items.add(DraftItem("SKU-1", 2, 19.99))
        items.add(DraftItem("SKU-2", 1, 49.99))
        discountCode = "SAVE10"

        // Compute the result
        val subtotal = items.sumOf { it.quantity * it.unitPrice }
        "Draft for $customerId: $${"%.2f".format(subtotal)} (${items.size} items)"
        // `this` refers to the draft — no need for draft.items, draft.customerId
    }
    println(summary)

    // run without receiver — a lambda block returning a value
    // Great for complex initialization where you want to return a final value
    val config = run {
        val env = System.getenv("ENVIRONMENT") ?: "development"
        val debug = env == "development"
        val timeout = if (debug) 60_000L else 10_000L
        mapOf("env" to env, "debug" to debug.toString(), "timeout" to timeout.toString())
    }
    println("Config: $config")
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. with — Operate on an Object, Return Result
// ─────────────────────────────────────────────────────────────────────────────

/**
 * `with` is like `run` but is NOT an extension function — the object is passed
 * as the first argument.
 *
 * Primary use:
 *   - When you want to call multiple functions on the same NON-NULLABLE object
 *     without chaining (reads as "with this object, do...")
 *
 * Returns: lambda result
 * Context object: `this`
 *
 * NOTE: Prefer `run` when chaining (since it IS an extension function).
 * Use `with` when working with a variable you already have.
 */
fun withExamples() {
    data class ReportBuilder(
        val title: String,
        val rows: MutableList<String> = mutableListOf()
    )

    val report = ReportBuilder("Order Report")

    val rendered = with(report) {
        rows.add("Order ID | Customer | Total")
        rows.add("---------|----------|-------")
        rows.add("ORD-001  | CUST-1   | $149.99")
        rows.add("ORD-002  | CUST-2   | $299.99")

        // Build the string result
        """
            $title
            ${rows.joinToString("\n")}
        """.trimIndent()
    }
    println(rendered)

    // Useful for working with a complex object:
    val orderDraft = OrderDraft("CUST-2")
    with(orderDraft) {
        items.add(DraftItem("LAPTOP", 1, 1299.99))
        shippingMethod = "EXPRESS"
        isGift = true
        notes = "Birthday present"
    }
    println("Draft: $orderDraft")
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. apply — Configure and Return the Object
// ─────────────────────────────────────────────────────────────────────────────

/**
 * `apply` is the BUILDER function — configure an object and return IT.
 * Perfect for object initialization / builder patterns.
 *
 * Returns: the context object (this)
 * Context object: this
 *
 * Java equivalent: the Builder pattern or method chaining setters
 *
 * RULE: Use `apply` when ALL the block does is configure/initialize the object.
 * The last expression in the block is IGNORED — apply always returns the object.
 */
class OrderBuilder {
    var customerId: String = ""
    var notes: String? = null
    var expedited: Boolean = false
    val items: MutableList<DraftItem> = mutableListOf()

    fun build(): ProcessedOrder {
        require(customerId.isNotBlank()) { "customerId required" }
        val total = items.sumOf { it.quantity * it.unitPrice }
        return ProcessedOrder("ORD-${System.currentTimeMillis()}", customerId, total)
    }
}

fun applyExamples() {
    // apply returns the receiver — perfect for builder initialization
    val builder = OrderBuilder().apply {
        customerId = "CUST-001"
        items.add(DraftItem("SKU-1", 2, 19.99))
        items.add(DraftItem("SKU-2", 1, 49.99))
        expedited = true
        notes = "Handle with care"
    }
    val order = builder.build()
    println("Built order: $order")

    // Typical use: configuring a mutable data structure
    val headers = HashMap<String, String>().apply {
        put("Content-Type", "application/json")
        put("Authorization", "Bearer token123")
        put("X-Request-ID", "req-001")
        put("Accept", "application/json")
    }
    println("Headers: $headers")

    // Chaining apply calls
    val draft = OrderDraft("CUST-003")
        .apply { items.add(DraftItem("BOOK-01", 3, 14.99)) }
        .apply { discountCode = "SAVE10" }
        .apply { shippingMethod = "ECONOMY" }
    println("Draft: ${draft.items.size} items, discount: ${draft.discountCode}")
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. also — Perform Side Effects, Return the Object
// ─────────────────────────────────────────────────────────────────────────────

/**
 * `also` is like `apply` but uses `it` instead of `this`.
 * Use it when you don't need to call methods on the context object —
 * instead you want to do something WITH the object as a whole (like logging).
 *
 * Returns: the context object
 * Context object: it
 *
 * MNEMONIC: "and ALSO do this thing (as a side effect)"
 *
 * RULE: Use `also` for side effects that don't change the object
 * (logging, validation, metrics, debug printing).
 */
fun alsoExamples() {
    // Log an object as it passes through a pipeline
    val processedOrder = OrderDraft("CUST-001")
        .also { draft -> println("[AUDIT] Draft created for ${draft.customerId}") }
        .apply { items.add(DraftItem("SKU-1", 1, 99.99)) }
        .also { draft -> println("[AUDIT] Items added: ${draft.items.size} items") }
        .let { draft ->
            ProcessedOrder(
                id = "ORD-${System.currentTimeMillis()}",
                customerId = draft.customerId,
                total = draft.items.sumOf { it.quantity * it.unitPrice }
            )
        }
        .also { order -> println("[AUDIT] Order created: ${order.id}, total: ${order.total}") }

    println("Final order: $processedOrder")

    // Useful for validation side effects
    val order = ProcessedOrder("ORD-001", "CUST-1", 149.99).also { o ->
        require(o.total > 0) { "Order total must be positive" }
        require(o.customerId.isNotBlank()) { "Customer ID required" }
        // Side effect: doesn't change o, just validates
    }

    // Debug printing in a chain without breaking it
    listOf(1, 2, 3, 4, 5)
        .also { println("Before filter: $it") }
        .filter { it % 2 == 0 }
        .also { println("After filter: $it") }
        .map { it * 10 }
        .also { println("After map: $it") }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. Choosing the Right Scope Function
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Decision guide:
 *
 *   "I want to operate on a nullable value"           → ?.let { }
 *   "I want to build/configure an object"             → apply { }
 *   "I want to execute on object, return other value" → run { } or let { }
 *   "I want side effects without changing the chain"  → also { }
 *   "I want to group operations on an existing var"   → with(obj) { }
 *
 * When in doubt, ask:
 *   - Do I want the block result or the object? → result=let/run/with, object=apply/also
 *   - Do I refer to the object as `it` or `this`? → it=let/also, this=run/with/apply
 */
data class UserProfile(
    var userId: String = "",
    var email: String = "",
    var displayName: String = "",
    var locale: String = "en_US"
)

fun scopeFunctionCheatSheet() {
    val rawUserId: String? = " USER-001 "

    // let: nullable transformation + result
    val userId: String? = rawUserId?.let { it.trim().uppercase() }

    // apply: build/configure, returns the object
    val profile = UserProfile().apply {
        this.userId = userId ?: "ANONYMOUS"
        email = "user@example.com"
        displayName = "John Doe"
    }

    // also: side effect (log), return the object unchanged
    profile.also { p -> println("Creating profile for ${p.userId}") }

    // run: execute operations, returns a result
    val greeting = profile.run {
        "Hello, $displayName! Your account ($userId) is ready."
    }

    // with: grouped operations on existing non-null variable
    val summary = with(profile) {
        "User: $userId | $displayName | $email | Locale: $locale"
    }

    println(greeting)
    println(summary)
}

fun main() {
    println("=== Kata 13: Scope Functions ===\n")
    println("--- let ---")
    letExamples()
    println("\n--- run ---")
    runExamples()
    println("\n--- with ---")
    withExamples()
    println("\n--- apply ---")
    applyExamples()
    println("\n--- also ---")
    alsoExamples()
    println("\n--- Choosing the right one ---")
    scopeFunctionCheatSheet()
}

package com.kotlinkata.basics

/**
 * # Kata 02: Null Safety — The End of NullPointerException
 *
 * Java engineers live with NPE risk daily. Kotlin eliminates the entire class of
 * NullPointerException bugs by encoding nullability in the type system.
 *
 * This kata dives deep into null safety patterns you'll use constantly in production.
 *
 * Topics:
 *   - Safe call operator ?.
 *   - Elvis operator ?:
 *   - Non-null assertion !! (when it's OK vs not OK)
 *   - let for null-safe blocks
 *   - Kotlin's equivalent of Java's Optional
 *   - Interop with Java (the one source of Kotlin NPEs)
 */

// ─────────────────────────────────────────────────────────────────────────────
// Domain: Order with optional fields
// ─────────────────────────────────────────────────────────────────────────────

data class Order(
    val id: String,
    val customerId: String,
    val discountCode: String?,       // optional
    val shippingAddress: Address?,   // optional for digital orders
    val notes: String?               // optional
)

data class Address(
    val street: String,
    val city: String,
    val country: String,
    val postalCode: String?          // not all countries use postal codes
)

data class Customer(
    val id: String,
    val email: String,
    val phone: String?,
    val preferredAddress: Address?
)

// ─────────────────────────────────────────────────────────────────────────────
// 1. Safe Call Operator ?.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * `x?.method()` — calls method() only if x is non-null, otherwise returns null.
 *
 * Java equivalent (verbose):
 *   if (x != null) { return x.method(); } else { return null; }
 *
 * Kotlin:
 *   x?.method()
 *
 * Chains gracefully — null propagates through the chain (like optional chaining in JS/Swift).
 */
fun safeCallExamples() {
    val order = Order(
        id = "ORD-001",
        customerId = "CUST-1",
        discountCode = null,
        shippingAddress = Address("123 Main St", "New York", "US", "10001"),
        notes = null
    )

    // Safe call — null if discountCode is null
    val discountLength: Int? = order.discountCode?.length

    // Chained safe calls — null if any link is null
    val postalCode: String? = order.shippingAddress?.postalCode

    // Chain across multiple levels
    val cityUpper: String? = order.shippingAddress?.city?.uppercase()

    // Safe call with a method that returns a nullable
    val trimmedNotes: String? = order.notes?.trim()?.takeIf { it.isNotEmpty() }

    println("Discount code length: $discountLength")  // null
    println("Postal code: $postalCode")               // 10001
    println("City uppercase: $cityUpper")             // NEW YORK
    println("Notes: $trimmedNotes")                   // null
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Elvis Operator ?:
// ─────────────────────────────────────────────────────────────────────────────

/**
 * `x ?: default` — returns x if non-null, otherwise returns default.
 *
 * Named after the Elvis hairstyle — the ?: operator sideways looks like it.
 *
 * Java equivalents:
 *   x != null ? x : default      // ternary
 *   Optional.ofNullable(x).orElse(default)   // Optional
 *
 * Kotlin:
 *   x ?: default
 *
 * POWERFUL: The right side of ?: can be `return` or `throw` — great for early returns!
 */
fun elvisExamples(order: Order) {
    // Basic default value
    val notes = order.notes ?: "No notes provided"

    // Chained with safe call — the idiomatic null-safe access + default pattern
    val city = order.shippingAddress?.city ?: "Unknown city"

    // Elvis with throw — validates and throws on null (guard clause pattern)
    val shippingAddress = order.shippingAddress
        ?: throw IllegalStateException("Order ${order.id} requires a shipping address")

    // Elvis with return — early exit on null (common in service methods)
    fun processDiscount(o: Order): Double {
        val code = o.discountCode ?: return 0.0   // early return if no discount
        return lookupDiscount(code)
    }

    // Chaining Elvis operators
    val contact = order.notes ?: order.discountCode ?: "No contact info"

    println("Notes: $notes, City: $city, Contact: $contact")
    println("Discount: ${processDiscount(order)}")
}

fun lookupDiscount(code: String): Double = when (code) {
    "SAVE10" -> 0.10
    "SAVE20" -> 0.20
    else -> 0.0
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. let — Null-Safe Code Block
// ─────────────────────────────────────────────────────────────────────────────

/**
 * `x?.let { ... }` — execute a block only if x is non-null.
 * Inside the block, `it` (or a named param) is the non-null value.
 *
 * Java equivalent:
 *   if (x != null) { doSomethingWith(x); }
 *
 * Kotlin:
 *   x?.let { doSomethingWith(it) }
 *
 * Useful for:
 *   - Executing side effects only if value is non-null
 *   - Transforming a nullable to a non-nullable result
 *   - Scoping a nullable value to a block
 */
fun letExamples(order: Order) {
    // Execute only if non-null (no result needed)
    order.discountCode?.let { code ->
        println("Applying discount code: $code")
        // code is String (not String?) inside this block
    }

    // Transform nullable to nullable — let returns the block result
    val discountAmount: Double? = order.discountCode?.let { code ->
        lookupDiscount(code).takeIf { it > 0 }
    }

    // Useful for chaining transformations on nullable values
    val formattedAddress: String? = order.shippingAddress?.let { addr ->
        "${addr.street}, ${addr.city}, ${addr.country} ${addr.postalCode ?: ""}".trim()
    }

    // Multiple nullable values — use let or if-not-null with multiple checks
    val shippingCost: Double? = if (order.shippingAddress != null && order.discountCode != null) {
        calculateShipping(order.shippingAddress, order.discountCode)
    } else null

    println("Discount: $discountAmount, Address: $formattedAddress, Shipping: $shippingCost")
}

fun calculateShipping(address: Address, discountCode: String): Double = 9.99

// ─────────────────────────────────────────────────────────────────────────────
// 4. Non-Null Assertion !! — Use Sparingly
// ─────────────────────────────────────────────────────────────────────────────

/**
 * `x!!` asserts that x is not null — throws KotlinNullPointerException if it is.
 *
 * When it's OK:
 *   - After validation logic that guarantees non-null (but compiler can't infer it)
 *   - In tests where null would indicate a test setup bug
 *   - When interoperating with Java APIs (covered below)
 *
 * When it's NOT OK:
 *   - Routine business logic — use ?: or ?. instead
 *   - To silence compiler warnings — fix the type or logic instead
 *   - Multiple !! in a chain — you'll lose the NPE location in stack traces
 */
fun nonNullAssertionExamples() {
    // WRONG — multiple !! loses stack trace clarity
    // val city = order!!.shippingAddress!!.city!!  // which one threw?

    // BETTER — keep !! isolated and at a meaningful boundary
    fun getRequiredCustomer(id: String): Customer {
        val customer = findCustomer(id)
        return customer ?: throw NoSuchElementException("Customer $id not found")
        // Now caller has a Customer, not Customer? — no !! needed downstream
    }

    // OK in tests — null here means setup is broken
    // val result = service.processOrder("ORD-001")
    // assertNotNull(result)
    // assertEquals("Shipped", result!!.status.name)  // acceptable in tests

    // Validating then asserting
    val rawInput: String? = "  CUST-001  "
    val validated = rawInput?.trim()?.takeIf { it.isNotEmpty() }
    // At this point we have external knowledge it's valid
    val customerId: String = validated ?: throw IllegalArgumentException("Empty customer ID")
    println("Customer: $customerId")
}

fun findCustomer(id: String): Customer? = Customer(id, "customer@example.com", null, null)

// ─────────────────────────────────────────────────────────────────────────────
// 5. Kotlin vs Java Optional
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Java engineers often ask: "Where is Optional<T> in Kotlin?"
 *
 * Answer: You don't need it! Kotlin's T? IS the Optional equivalent,
 * but it's a compile-time construct, not a runtime wrapper:
 *   - No boxing overhead (T? is same memory as T for non-primitives)
 *   - No NPE if you forget to unwrap (Optional can be null itself in Java)
 *   - Works with all operators (?., ?:, let, etc.) without verbose .map().orElse()
 *   - Supported everywhere: fields, parameters, return types, generics
 *
 * Java Optional:   Optional<String> opt = Optional.ofNullable(x).map(String::toUpperCase).orElse("default");
 * Kotlin:          val result = x?.uppercase() ?: "default"
 */
fun optionalComparison() {
    // Java-style (DON'T do this in Kotlin):
    // val optional: java.util.Optional<String> = java.util.Optional.ofNullable(null)
    // val result = optional.map { it.uppercase() }.orElse("default")

    // Kotlin idiomatic — same semantics, far less ceremony:
    val maybeCode: String? = null
    val result: String = maybeCode?.uppercase() ?: "default"

    // Transform a nullable through multiple steps:
    val processedCode: String? = maybeCode
        ?.trim()
        ?.takeIf { it.length >= 4 }  // takeIf: keep value if predicate is true
        ?.uppercase()

    println("Result: $result, Processed: $processedCode")
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. Java Interop — Platform Types (The One Source of Kotlin NPEs)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * When calling Java code, Kotlin can't know if a Java method returns null or not.
 * These are called "platform types" (shown as T! in IDE tooltips).
 *
 * Java: public String getCustomerName() { ... }  // might return null
 * Kotlin sees: String!  (could be String or String? — you're responsible)
 *
 * Best practices:
 *   1. Assign to String? to be safe and handle null
 *   2. Use @NotNull/@Nullable annotations in Java to help Kotlin infer correctly
 *   3. Wrap Java API calls at the boundary and return typed Kotlin results
 */
fun javaInteropSafety() {
    // Simulating Java API call that might return null
    val javaResult: String? = getFromJavaApi()  // Treat as nullable — be safe!

    // Defensive — handle potential null from Java
    val safeResult = javaResult ?: "fallback"

    // Don't just assign to String without understanding the Java contract
    // val unsafe: String = getFromJavaApi()  // compiles, but may NPE at runtime!

    println("Result: $safeResult")
}

// Simulates a Java API (in real code, this would be a @Nullable Java method)
fun getFromJavaApi(): String? = if (System.currentTimeMillis() % 2 == 0L) "data" else null

// ─────────────────────────────────────────────────────────────────────────────
// 7. Practical Pattern: Null Object / Sentinel Values
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Sometimes you want to avoid nulls entirely — use sealed classes or defaults.
 * This is the "Null Object" pattern, Kotlin style.
 */
sealed class OrderResult {
    data class Success(val orderId: String, val total: Double) : OrderResult()
    data class Failed(val reason: String, val code: Int) : OrderResult()
    object NotFound : OrderResult()
}

fun fetchOrder(id: String): OrderResult = when {
    id.startsWith("ORD-") -> OrderResult.Success(id, 99.99)
    id.startsWith("OLD-") -> OrderResult.NotFound
    else -> OrderResult.Failed("Invalid order ID format", 400)
}

fun handleOrder(id: String) {
    when (val result = fetchOrder(id)) {
        is OrderResult.Success  -> println("Order ${result.orderId} total: ${result.total}")
        is OrderResult.Failed   -> println("Error ${result.code}: ${result.reason}")
        is OrderResult.NotFound -> println("Order $id not found")
    }
}

fun main() {
    println("=== Kata 02: Null Safety ===\n")

    val order = Order("ORD-001", "CUST-1", null, Address("1 Main St", "NYC", "US", "10001"), null)
    safeCallExamples()
    println()
    elvisExamples(order)
    println()
    letExamples(order)
    println()
    nonNullAssertionExamples()
    println()
    optionalComparison()
    println()
    javaInteropSafety()
    println()
    handleOrder("ORD-123")
    handleOrder("OLD-001")
    handleOrder("BAD-001")
}

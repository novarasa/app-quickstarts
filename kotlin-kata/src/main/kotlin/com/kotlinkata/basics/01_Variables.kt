package com.kotlinkata.basics

/**
 * # Kata 01: Variables, Types, and the Kotlin Type System
 *
 * For Java Engineers: Kotlin's type system is more expressive and safer than Java's.
 * Each concept here has a direct Java parallel — but Kotlin's approach eliminates
 * entire categories of runtime bugs at compile time.
 *
 * Topics:
 *   - val vs var (vs Java's final)
 *   - Type inference (more powerful than Java's var)
 *   - Unified type hierarchy — no primitives vs boxed types distinction
 *   - Explicit type conversions — no silent widening
 *   - lateinit for injection scenarios
 *   - const val for true compile-time constants
 */

// ─────────────────────────────────────────────────────────────────────────────
// 1. val vs var
// ─────────────────────────────────────────────────────────────────────────────

/**
 * val = immutable reference (like Java's `final`)
 * var = mutable reference
 *
 * RULE: Prefer val by default. Use var only when mutation is necessary.
 *
 * Java:
 *   final String name = "Kotlin";   // verbose final everywhere
 *   String version = "1.9";         // mutable is the default (risky)
 *
 * Kotlin:
 *   val name = "Kotlin"             // immutable is the DEFAULT (safe)
 *   var version = "1.9"             // opt-in to mutability
 */
fun valVsVar() {
    val name = "Kotlin"           // type inferred as String
    // name = "Java"              // ❌ Compile error: val cannot be reassigned

    var counter = 0
    counter++                     // ✅ var can be reassigned
    counter += 10

    // val reference is immutable, but the object it points to can be mutable
    val orders = mutableListOf("order-1", "order-2")
    orders.add("order-3")         // ✅ mutating the list is fine
    // orders = mutableListOf()   // ❌ reassigning the reference is not fine

    println("Language: $name, counter: $counter, orders: $orders")
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Type Inference
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Kotlin's type inference is far more powerful than Java's `var`.
 * Java's `var` only works for local variables.
 * Kotlin infers types for local vars, field initializers, and function return types.
 *
 * When to be explicit:
 *   - When the inferred type isn't obvious from the right-hand side
 *   - When you want a more general type (e.g., List<Number> from a mixed list)
 *   - For public API return types (good practice)
 */
fun typeInference() {
    // All types inferred — no ceremony
    val customerId = "CUST-001"     // String
    val orderCount = 42             // Int
    val totalPrice = 99.99          // Double
    val isActive = true             // Boolean
    val statusCode = 'A'            // Char

    // Complex types inferred too
    val items = listOf("SKU-1", "SKU-2", "SKU-3")   // List<String>
    val inventory = mapOf("SKU-1" to 100, "SKU-2" to 50) // Map<String, Int>

    // When to be explicit for clarity
    val parsedId: Long = "987654321".toLong()        // makes parsing intent clear
    val prices: List<Number> = listOf(9.99, 19, 4.5) // want List<Number>, not List<Any>

    println("Customer: $customerId, orders: $orderCount, items: ${items.size}")
    println("Prices type: ${prices::class.simpleName}")
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. No Implicit Type Conversions — Intentional Design
// ─────────────────────────────────────────────────────────────────────────────

/**
 * GOTCHA FOR JAVA ENGINEERS: Kotlin has NO implicit numeric widening!
 *
 * Java allows:   int i = 5; long l = i;   // silent widening
 * Kotlin requires: val l: Long = i.toLong()  // explicit, no surprises
 *
 * Why? Prevents subtle bugs where widening changes behavior silently.
 * Every numeric type has: .toByte(), .toShort(), .toInt(), .toLong(),
 *                         .toFloat(), .toDouble(), .toUInt(), etc.
 */
fun explicitConversions() {
    val quantity: Int = 100
    // val longQty: Long = quantity    // ❌ Compile error — no implicit widening
    val longQty: Long = quantity.toLong()    // ✅ Explicit

    val price: Double = 19.99
    val intPrice: Int = price.toInt()       // truncates (19), intentional
    val roundedPrice: Long = Math.round(price)  // proper rounding

    // Underscores in numeric literals for readability (like Java 7+)
    val maxOrderValue = 999_999_99L   // cents
    val timeoutMs = 30_000L

    // Operator overloading works within the same type naturally
    val subtotal = 5 * price          // Int * Double = Double (special case in arithmetic)

    println("Qty: $longQty, price: $intPrice, subtotal: $subtotal")
    println("Max order: $maxOrderValue, timeout: ${timeoutMs}ms")
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Nullable Types — NPE Safety Baked Into the Type System
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Kotlin enforces nullability IN THE TYPE SYSTEM, not by convention.
 *
 * String  — guaranteed non-null (compiler enforced)
 * String? — explicitly nullable (must handle null case)
 *
 * Java's @Nullable/@NonNull are just hints — ignored at runtime.
 * Kotlin's ? types are enforced at COMPILE TIME.
 *
 * Tony Hoare (inventor of null) called it his "billion-dollar mistake".
 * Kotlin's type system eliminates the problem by making nullability explicit.
 */
fun nullableTypes() {
    val name: String = "Kotlin"          // Never null — compiler guaranteed
    val couponCode: String? = null       // Explicitly nullable

    // To use a nullable, you MUST handle null — 4 idiomatic ways:

    // 1. Safe call operator ?. — returns null if receiver is null
    val couponLength: Int? = couponCode?.length

    // 2. Elvis operator ?: — default value if left side is null
    val discount: Double = if (couponCode != null) 0.10 else 0.0
    val discountElvis: Double = couponCode?.let { 0.10 } ?: 0.0  // same, more idiomatic

    // 3. Smart cast after null check
    if (couponCode != null) {
        println(couponCode.length)   // Smart cast: compiler knows it's String (not String?) here
    }

    // 4. Non-null assertion !! — throws KotlinNullPointerException if null
    //    Use ONLY when you have external proof it cannot be null (e.g., post-validation)
    val validated: String? = validateCoupon("SAVE10")
    val code: String = validated!!   // OK here because we know validateCoupon returns non-null for valid codes

    // Chaining safe calls — null propagates through the chain
    val address: CustomerAddress? = null
    val city: String? = address?.shipping?.city   // null if any link is null

    println("Coupon length: $couponLength, discount: $discountElvis, city: $city")
}

data class CustomerAddress(val shipping: ShippingAddress?)
data class ShippingAddress(val city: String)

fun validateCoupon(code: String): String? = if (code.isNotBlank()) code.uppercase() else null

// ─────────────────────────────────────────────────────────────────────────────
// 5. Smart Casts
// ─────────────────────────────────────────────────────────────────────────────

/**
 * After an `is` check, Kotlin automatically casts — no redundant cast needed.
 *
 * Java (pre-16):
 *   if (obj instanceof String) {
 *       String s = (String) obj;   // redundant cast
 *       System.out.println(s.length());
 *   }
 *
 * Java 16+ (pattern matching):
 *   if (obj instanceof String s) { System.out.println(s.length()); }
 *
 * Kotlin (since 1.0):
 *   if (obj is String) { println(obj.length) }   // smart cast, no cast syntax
 */
sealed interface PaymentMethod
data class CreditCard(val last4: String, val network: String) : PaymentMethod
data class BankTransfer(val iban: String) : PaymentMethod
data class Wallet(val provider: String, val balance: Double) : PaymentMethod

fun describePayment(method: PaymentMethod): String {
    // when with smart casts — fully type-safe and exhaustive
    return when (method) {
        is CreditCard    -> "Card ending ${method.last4} (${method.network})"   // smart cast to CreditCard
        is BankTransfer  -> "Bank transfer to ${method.iban}"                   // smart cast to BankTransfer
        is Wallet        -> "${method.provider} wallet, balance: ${method.balance}"  // smart cast to Wallet
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. lateinit — For Dependency Injection Scenarios
// ─────────────────────────────────────────────────────────────────────────────

/**
 * `lateinit var` is for properties that:
 *   1. Cannot be initialized in the constructor
 *   2. Are GUARANTEED to be set before first use
 *
 * Common uses: Spring @Autowired, JUnit @BeforeEach setup, Android views in onCreate()
 *
 * Better than `String?` because:
 *   - No null checks scattered everywhere
 *   - UninitializedPropertyAccessException gives a clear "who forgot to wire this?" message
 *   - Communicates intent: "this WILL be set, just not by the constructor"
 */
class OrderProcessingService {
    lateinit var orderRepository: OrderRepo       // injected by DI framework
    lateinit var paymentGateway: PaymentGatewayService  // injected by DI framework

    fun processOrder(orderId: String) {
        // Safe guard in critical paths
        check(::orderRepository.isInitialized) { "OrderRepository not wired" }
        println("Processing order $orderId")
    }
}

interface OrderRepo
interface PaymentGatewayService

// ─────────────────────────────────────────────────────────────────────────────
// 7. const val — True Compile-Time Constants
// ─────────────────────────────────────────────────────────────────────────────

/**
 * `const val` compiles to Java's `public static final` for primitives/Strings.
 * Values are INLINED at compile time, just like Java's static final constants.
 *
 * Requirements:
 *   - Top-level or inside an object/companion object
 *   - Primitive type or String
 *   - Value known at compile time (no function calls)
 *
 * Java: public static final int MAX_RETRIES = 3;
 * Kotlin: const val MAX_RETRIES = 3
 */
const val MAX_ORDER_RETRIES = 3
const val ORDER_API_VERSION = "v2"
const val PAYMENT_TIMEOUT_MS = 30_000L

object OrderLimits {
    const val MAX_ITEMS_PER_ORDER = 100
    const val MIN_ORDER_VALUE_CENTS = 1      // $0.01
    const val MAX_ORDER_VALUE_CENTS = 99_999_999  // ~$1M
}

fun main() {
    println("=== Kata 01: Variables & Types ===\n")
    valVsVar()
    typeInference()
    explicitConversions()
    nullableTypes()

    println("\nPayment descriptions:")
    listOf(
        CreditCard("4242", "Visa"),
        BankTransfer("DE89370400440532013000"),
        Wallet("PayPal", 150.00)
    ).forEach { println("  ${describePayment(it)}") }

    println("\nAPI: $ORDER_API_VERSION, Max retries: $MAX_ORDER_RETRIES")
    println("Max items per order: ${OrderLimits.MAX_ITEMS_PER_ORDER}")
}

package com.kotlinkata.oop

import java.time.Instant
import java.util.UUID

/**
 * # Kata 06: Data Classes — Kotlin's Answer to Lombok and Java Records
 *
 * A data class auto-generates: equals(), hashCode(), toString(), copy(), componentN().
 *
 * Java equivalents:
 *   - Lombok @Data/@Value
 *   - Java 16+ record (immutable, less flexible)
 *   - Hand-written POJOs (verbose and error-prone)
 *
 * Kotlin's data class advantages over Java records:
 *   - Properties can be mutable (var) — records are always immutable
 *   - Can extend classes (records can't)
 *   - copy() function for easy modification
 *   - Works naturally with sealed classes
 *   - No need for explicit constructor — primary constructor IS the data
 *
 * Topics:
 *   - Defining data classes
 *   - equals/hashCode based on properties
 *   - toString() auto-generation
 *   - copy() — immutable update pattern
 *   - Destructuring (componentN)
 *   - Data classes in collections (as Map keys)
 *   - Best practices and limitations
 */

// ─────────────────────────────────────────────────────────────────────────────
// 1. Basic Data Class
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Everything in the primary constructor is a "component" — included in
 * equals(), hashCode(), toString(), copy(), and destructuring.
 *
 * Properties defined in the body are NOT included in these generated functions.
 *
 * Java Lombok equivalent:
 *   @Data public class Product { ... }  or  @Value public class Product { ... }
 *
 * Java record equivalent:
 *   public record Product(String sku, String name, double price, String category) {}
 */
data class Product(
    val sku: String,
    val name: String,
    val price: Double,
    val category: String,
    val inStock: Boolean = true
) {
    // Properties defined here are NOT part of equals/hashCode/toString
    val displayName: String get() = "[$sku] $name"
    val formattedPrice: String get() = "${"%.2f".format(price)} USD"

    // You CAN add methods to data classes
    fun withTax(rate: Double): Double = price * (1 + rate)
    fun isOnSale(salePrice: Double): Boolean = salePrice < price
}

data class OrderItem(
    val product: Product,
    val quantity: Int,
    val unitPrice: Double,      // captured at order time (may differ from current product.price)
    val discountApplied: Double = 0.0
) {
    val lineTotal: Double get() = (unitPrice - discountApplied) * quantity
}

data class CustomerInfo(
    val id: String = UUID.randomUUID().toString(),
    val email: String,
    val firstName: String,
    val lastName: String,
    val tier: CustomerTier = CustomerTier.STANDARD
) {
    val fullName: String get() = "$firstName $lastName"
}

enum class CustomerTier { STANDARD, SILVER, GOLD, PLATINUM }

fun basicDataClassExamples() {
    val product = Product("SKU-001", "Wireless Keyboard", 79.99, "Electronics")

    // toString() — auto-generated, no boilerplate
    println(product)
    // Output: Product(sku=SKU-001, name=Wireless Keyboard, price=79.99, category=Electronics, inStock=true)

    // equals() — compares by VALUE, not reference
    val product2 = Product("SKU-001", "Wireless Keyboard", 79.99, "Electronics")
    println("Equal: ${product == product2}")  // true (structural equality)
    println("Same reference: ${product === product2}")  // false (referential equality)

    // hashCode() — consistent with equals, safe as Map key
    val map = mapOf(product to 100, product2 to 200)
    println("Map size: ${map.size}")  // 1 — they're the same key!

    // Custom method works alongside generated methods
    println(product.displayName)
    println(product.formattedPrice)
    println("Price with tax: ${"%.2f".format(product.withTax(0.08))}")
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. copy() — Immutable Update Pattern
// ─────────────────────────────────────────────────────────────────────────────

/**
 * copy() creates a NEW instance with specified properties changed.
 * All other properties keep their values from the original.
 *
 * This is the Kotlin way to implement the "wither" pattern from Java,
 * or the spread/clone pattern from JavaScript/functional languages.
 *
 * Java equivalent (verbose):
 *   return new Product(original.getSku(), newName, original.getPrice(), ...);  // error-prone
 *
 * Kotlin:
 *   original.copy(name = newName)  // clean, safe
 *
 * IMPORTANT: copy() only does SHALLOW copy. Nested objects are NOT deep copied.
 */
fun copyExamples() {
    val original = Product("SKU-001", "Keyboard", 79.99, "Electronics")

    // Modify a single field
    val onSale = original.copy(price = 59.99)

    // Modify multiple fields
    val rebranded = original.copy(name = "Mechanical Keyboard Pro", sku = "SKU-001-PRO")

    // Out of stock (copy with flag changed)
    val soldOut = original.copy(inStock = false)

    println("Original: $original")
    println("On sale: $onSale")
    println("Rebranded: $rebranded")
    println("Sold out: $soldOut")

    // Practical use: processing pipeline — each step returns a new order item
    val item = OrderItem(original, quantity = 1, unitPrice = 79.99)
    val discounted = item.copy(discountApplied = 10.0)
    val bulkOrder = item.copy(quantity = 10)
    println("Original line: ${item.lineTotal}")
    println("Discounted line: ${discounted.lineTotal}")
    println("Bulk line: ${bulkOrder.lineTotal}")

    // Practical use: applying updates from a form / PATCH request
    data class UpdateProductRequest(
        val name: String? = null,
        val price: Double? = null,
        val inStock: Boolean? = null
    )

    fun Product.applyUpdate(update: UpdateProductRequest): Product = copy(
        name = update.name ?: name,
        price = update.price ?: price,
        inStock = update.inStock ?: inStock
    )

    val update = UpdateProductRequest(price = 69.99, inStock = false)
    val updated = original.applyUpdate(update)
    println("After PATCH: $updated")
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Destructuring Declarations
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Data classes support destructuring — assign components to individual variables.
 * Each primary constructor property gets a componentN() function.
 *
 * Java has no direct equivalent (Java 21 pattern matching comes closest).
 *
 * Important: destructuring is POSITIONAL, not by name!
 * If you change the order of constructor params, destructuring SILENTLY breaks.
 * Prefer named access (product.name) unless the positions are stable.
 */
fun destructuringExamples() {
    val product = Product("SKU-001", "Keyboard", 79.99, "Electronics")

    // Destructuring — maps to primary constructor order
    val (sku, name, price, category, inStock) = product
    println("Destructured: sku=$sku, name=$name, price=$price")

    // Underscore to skip positions you don't need
    val (_, productName, productPrice) = product
    println("Name: $productName, Price: $productPrice")

    // VERY common: destructuring in loops over Map.Entry
    val inventory = mapOf(
        "SKU-001" to 50,
        "SKU-002" to 0,
        "SKU-003" to 12
    )
    for ((itemSku, count) in inventory) {
        println("$itemSku: $count ${if (count > 0) "in stock" else "OUT OF STOCK"}")
    }

    // Destructuring in lambda parameters — common with Map.forEach
    inventory.forEach { (itemSku, count) ->
        if (count == 0) println("Reorder needed: $itemSku")
    }

    // Returning multiple values — use a data class (or Pair/Triple for quick use)
    data class PriceBreakdown(val subtotal: Double, val tax: Double, val total: Double)

    fun calculatePrice(price: Double, qty: Int): PriceBreakdown {
        val subtotal = price * qty
        val tax = subtotal * 0.08
        return PriceBreakdown(subtotal, tax, subtotal + tax)
    }

    val (subtotal, tax, total) = calculatePrice(79.99, 3)
    println("Subtotal: $subtotal, Tax: $tax, Total: $total")
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Data Classes in Domain Modeling
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Data classes shine in domain modeling because they:
 *   - Are immutable by default (using val)
 *   - Have value equality (two orders with same data are "equal")
 *   - Work as Map keys and in Sets naturally
 *   - Serialize/deserialize cleanly (Jackson, kotlinx.serialization)
 */
data class Address(
    val street: String,
    val city: String,
    val state: String,
    val postalCode: String,
    val country: String = "US"
)

data class OrderHeader(
    val id: String,
    val customerId: String,
    val shippingAddress: Address,
    val billingAddress: Address,
    val createdAt: Instant = Instant.now(),
    val notes: String? = null
)

fun domainModelExample() {
    val address = Address("123 Main St", "New York", "NY", "10001")

    val order1 = OrderHeader(
        id = "ORD-001",
        customerId = "CUST-1",
        shippingAddress = address,
        billingAddress = address  // same address for both
    )

    // When shipping address changes, create a new order (immutable update)
    val shippedTo = address.copy(street = "456 Oak Ave", city = "Brooklyn", postalCode = "11201")
    val order2 = order1.copy(shippingAddress = shippedTo)

    println("Order 1 ships to: ${order1.shippingAddress.city}")
    println("Order 2 ships to: ${order2.shippingAddress.city}")

    // Data classes are perfect as Map keys — stable hashCode based on values
    val ordersByCustomer = mutableMapOf<CustomerInfo, List<OrderHeader>>()
    val customer = CustomerInfo(email = "john@example.com", firstName = "John", lastName = "Doe")
    ordersByCustomer[customer] = listOf(order1, order2)
    println("Orders for ${customer.fullName}: ${ordersByCustomer[customer]?.size}")
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Limitations and Best Practices
// ─────────────────────────────────────────────────────────────────────────────

/**
 * When NOT to use data classes:
 *   - Entities with identity (JPA entities — equals/hashCode should be id-based)
 *   - Classes that need custom equals/hashCode logic
 *   - Singletons (use object instead)
 *   - Classes with complex validation in constructors
 *
 * Best practices:
 *   - Prefer val over var in data classes (immutability)
 *   - Use copy() instead of mutation
 *   - Add init { } blocks for validation
 *   - Consider using value classes for primitive wrapping (see below)
 */

// Validation in data class
data class Email(val value: String) {
    init {
        require(value.contains("@") && value.contains(".")) {
            "Invalid email: $value"
        }
        require(value.length <= 254) { "Email too long: $value" }
    }
}

// Value classes — wrapper with zero overhead at runtime (no boxing)
@JvmInline
value class OrderId(val value: String) {
    init { require(value.startsWith("ORD-")) { "Invalid order ID format: $value" } }
}

@JvmInline
value class CustomerId(val value: String)

// Type-safe function signatures — can't accidentally swap the two IDs
fun findOrder(orderId: OrderId, customerId: CustomerId): String =
    "Order ${orderId.value} for customer ${customerId.value}"

fun bestPracticesExamples() {
    // Validation on construction
    try {
        val invalidEmail = Email("not-an-email")
    } catch (e: IllegalArgumentException) {
        println("Caught: ${e.message}")
    }

    val validEmail = Email("user@example.com")
    println("Email: ${validEmail.value}")

    // Value classes prevent mixing up parameters
    val result = findOrder(OrderId("ORD-001"), CustomerId("CUST-001"))
    println(result)

    // This would be a compile error — types are distinct:
    // findOrder(CustomerId("CUST-001"), OrderId("ORD-001"))  // ❌ type mismatch
}

fun main() {
    println("=== Kata 06: Data Classes ===\n")
    println("--- Basic Examples ---")
    basicDataClassExamples()
    println("\n--- copy() Examples ---")
    copyExamples()
    println("\n--- Destructuring ---")
    destructuringExamples()
    println("\n--- Domain Model ---")
    domainModelExample()
    println("\n--- Best Practices ---")
    bestPracticesExamples()
}

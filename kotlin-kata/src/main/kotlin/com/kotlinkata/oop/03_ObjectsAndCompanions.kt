package com.kotlinkata.oop

import java.util.concurrent.ConcurrentHashMap

/**
 * # Kata 08: Objects and Companion Objects
 *
 * Kotlin replaces Java's static members and singleton patterns with first-class
 * language constructs: `object` declarations and `companion object`.
 *
 * Java patterns replaced:
 *   - Singleton (double-checked locking, enum singleton) → object declaration
 *   - Static utility methods → companion object or top-level functions
 *   - Static factory methods → companion object
 *   - Static constants → companion object with const val
 *   - Anonymous classes → object expressions
 *
 * Topics:
 *   - object declaration (thread-safe singleton)
 *   - companion object (static members, factory methods)
 *   - object expressions (anonymous classes, adapters)
 *   - Named companion objects
 *   - @JvmStatic for Java interop
 */

// ─────────────────────────────────────────────────────────────────────────────
// 1. object Declaration — Thread-Safe Singleton
// ─────────────────────────────────────────────────────────────────────────────

/**
 * `object` creates a singleton initialized lazily and thread-safely by the JVM.
 * No double-checked locking, no enum trickery — just `object`.
 *
 * Java equivalent (verbose):
 *   public class OrderIdGenerator {
 *       private static OrderIdGenerator INSTANCE;
 *       private synchronized static OrderIdGenerator getInstance() {
 *           if (INSTANCE == null) INSTANCE = new OrderIdGenerator();
 *           return INSTANCE;
 *       }
 *       // ... methods
 *   }
 *
 * Kotlin:
 *   object OrderIdGenerator { ... }
 *
 * Compiled to: a class with a public static final INSTANCE field.
 * From Java: OrderIdGenerator.INSTANCE.generate()
 */
object OrderIdGenerator {
    private var counter = 1000

    fun next(): String = "ORD-${++counter}"

    fun nextBatch(count: Int): List<String> = (1..count).map { next() }
}

object ProductCatalogCache {
    private val cache = ConcurrentHashMap<String, CatalogProduct>()

    fun get(sku: String): CatalogProduct? = cache[sku]

    fun put(product: CatalogProduct) {
        cache[product.sku] = product
    }

    fun evict(sku: String) = cache.remove(sku)

    fun size(): Int = cache.size

    fun clear() = cache.clear()
}

data class CatalogProduct(val sku: String, val name: String, val price: Double)

// Objects can implement interfaces — useful for singletons that serve as strategies
interface TaxStrategy {
    fun calculate(amount: Double): Double
}

object USStandardTax : TaxStrategy {
    private const val RATE = 0.08
    override fun calculate(amount: Double): Double = amount * RATE
}

object EUVATTax : TaxStrategy {
    private const val RATE = 0.20
    override fun calculate(amount: Double): Double = amount * RATE
}

object NoTax : TaxStrategy {
    override fun calculate(amount: Double): Double = 0.0
}

fun objectDeclarationExamples() {
    // Objects are singletons — same instance everywhere
    val id1 = OrderIdGenerator.next()
    val id2 = OrderIdGenerator.next()
    println("IDs: $id1, $id2")

    // Use in caching
    ProductCatalogCache.put(CatalogProduct("SKU-1", "Keyboard", 79.99))
    ProductCatalogCache.put(CatalogProduct("SKU-2", "Mouse", 29.99))
    println("Cache size: ${ProductCatalogCache.size()}")
    println("SKU-1: ${ProductCatalogCache.get("SKU-1")}")

    // Polymorphic use of object singletons
    val strategies = mapOf("US" to USStandardTax, "EU" to EUVATTax, "INTL" to NoTax)
    val amount = 100.0
    strategies.forEach { (region, strategy) ->
        println("$region tax on $amount: ${strategy.calculate(amount)}")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. companion object — Static Members and Factory Methods
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A companion object lives inside a class and is accessed via the class name.
 * It replaces Java's static members.
 *
 * Java:
 *   public class Order {
 *       public static final String DEFAULT_CURRENCY = "USD";
 *       public static Order create(String customerId) { return new Order(...); }
 *   }
 *   Order.DEFAULT_CURRENCY;
 *   Order.create("CUST-1");
 *
 * Kotlin:
 *   class Order { companion object { const val DEFAULT_CURRENCY = "USD"
 *       fun create(customerId: String) = Order(...) } }
 *   Order.DEFAULT_CURRENCY
 *   Order.create("CUST-1")
 */
class Invoice private constructor(  // private constructor — force use of factory
    val id: String,
    val orderId: String,
    val customerId: String,
    val lineItems: List<InvoiceItem>,
    val currency: String,
    val notes: String?
) {
    val subtotal: Double get() = lineItems.sumOf { it.total }
    val tax: Double get() = subtotal * TAX_RATE
    val total: Double get() = subtotal + tax

    companion object {
        // Constants — compiled to static final
        const val TAX_RATE = 0.08
        const val MAX_LINE_ITEMS = 500
        private const val ID_PREFIX = "INV"

        // Factory methods — named constructors with semantic meaning
        fun create(orderId: String, customerId: String, items: List<InvoiceItem>): Invoice {
            require(items.isNotEmpty()) { "Invoice must have at least one item" }
            require(items.size <= MAX_LINE_ITEMS) { "Invoice exceeds $MAX_LINE_ITEMS line items" }
            val id = "$ID_PREFIX-${orderId.removePrefix("ORD-")}"
            return Invoice(id, orderId, customerId, items, "USD", null)
        }

        fun createWithNotes(
            orderId: String,
            customerId: String,
            items: List<InvoiceItem>,
            notes: String
        ): Invoice = create(orderId, customerId, items).copy(notes = notes)

        // "from" factory pattern — common Kotlin convention
        fun from(order: BillingOrder): Invoice = create(
            orderId = order.id,
            customerId = order.customerId,
            items = order.items.map { InvoiceItem.fromOrderItem(it) }
        )
    }

    // data class copy() won't work here since it's not a data class
    // Add explicit copy for the fields we allow to change
    fun copy(notes: String? = this.notes): Invoice =
        Invoice(id, orderId, customerId, lineItems, currency, notes)

    override fun toString(): String =
        "Invoice($id, order=$orderId, total=$${"%.2f".format(total)}, items=${lineItems.size})"
}

data class InvoiceItem(val description: String, val quantity: Int, val unitPrice: Double) {
    val total: Double get() = quantity * unitPrice

    companion object {
        fun fromOrderItem(item: BillingOrderItem): InvoiceItem =
            InvoiceItem(item.name, item.qty, item.price)
    }
}

data class BillingOrder(val id: String, val customerId: String, val items: List<BillingOrderItem>)
data class BillingOrderItem(val name: String, val qty: Int, val price: Double)

fun companionObjectExamples() {
    // Factory method — cleaner than direct constructor
    val invoice = Invoice.create(
        orderId = "ORD-001",
        customerId = "CUST-1",
        items = listOf(
            InvoiceItem("Keyboard", 1, 79.99),
            InvoiceItem("Mouse", 2, 29.99)
        )
    )
    println(invoice)
    println("Tax rate: ${Invoice.TAX_RATE}")
    println("Subtotal: ${"%.2f".format(invoice.subtotal)}, Tax: ${"%.2f".format(invoice.tax)}, Total: ${"%.2f".format(invoice.total)}")

    // From an order
    val order = BillingOrder("ORD-002", "CUST-2", listOf(
        BillingOrderItem("Laptop Stand", 1, 49.99),
        BillingOrderItem("USB Hub", 1, 34.99)
    ))
    val invoice2 = Invoice.from(order)
    println(invoice2)
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Named Companion Objects
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Companion objects can be named — useful for semantic clarity or
 * when implementing an interface as a companion.
 */
interface Deserializer<T> {
    fun deserialize(json: String): T
}

class ShippingLabel(val orderId: String, val address: String, val barcode: String) {
    companion object Factory : Deserializer<ShippingLabel> {
        // Named companion can be accessed as ShippingLabel.Factory.deserialize(...)
        // or just ShippingLabel.deserialize(...) — both work
        override fun deserialize(json: String): ShippingLabel {
            // Simplified deserialization
            return ShippingLabel("ORD-001", "123 Main St", "1234567890")
        }

        fun generate(orderId: String, address: String): ShippingLabel {
            val barcode = "BC-${orderId.hashCode().toString(16).uppercase()}"
            return ShippingLabel(orderId, address, barcode)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. object Expressions — Anonymous Classes
// ─────────────────────────────────────────────────────────────────────────────

/**
 * `object : Interface { ... }` creates an anonymous class instance.
 *
 * Unlike Java anonymous classes, Kotlin object expressions can:
 *   - Implement multiple interfaces
 *   - Access (and close over) mutable variables from enclosing scope (unlike Java!)
 *
 * Java:
 *   Comparator<Order> comp = new Comparator<Order>() {
 *       public int compare(Order a, Order b) { return a.total.compareTo(b.total); }
 *   };
 *
 * Kotlin (lambda — preferred for single-method interfaces):
 *   val comp = Comparator<OrderDto> { a, b -> a.total.compareTo(b.total) }
 *
 * Kotlin object expression (for multi-method interfaces or when state is needed):
 *   val obj = object : SomeInterface { override fun method() { ... } }
 */
interface EventListener {
    fun onOrderPlaced(orderId: String)
    fun onOrderCancelled(orderId: String, reason: String)
}

interface MetricsCollector {
    fun recordEvent(eventName: String, attributes: Map<String, String>)
}

fun setupEventHandling() {
    val processedOrders = mutableListOf<String>()  // captured by object expression

    // Anonymous class implementing multiple interfaces — captures outer state
    val listener = object : EventListener, MetricsCollector {
        override fun onOrderPlaced(orderId: String) {
            processedOrders.add(orderId)  // closes over mutableList — works unlike Java!
            recordEvent("order.placed", mapOf("orderId" to orderId))
            println("Order placed: $orderId (total processed: ${processedOrders.size})")
        }

        override fun onOrderCancelled(orderId: String, reason: String) {
            processedOrders.remove(orderId)
            println("Order cancelled: $orderId — $reason")
        }

        override fun recordEvent(eventName: String, attributes: Map<String, String>) {
            println("  [metric] $eventName: $attributes")
        }
    }

    listener.onOrderPlaced("ORD-001")
    listener.onOrderPlaced("ORD-002")
    listener.onOrderCancelled("ORD-001", "Customer request")
    println("Active orders: $processedOrders")
}

fun main() {
    println("=== Kata 08: Objects & Companions ===\n")

    println("--- Object Declarations ---")
    objectDeclarationExamples()
    println()

    println("--- Companion Objects ---")
    companionObjectExamples()
    println()

    println("--- Named Companion ---")
    val label = ShippingLabel.generate("ORD-003", "456 Oak Ave, Brooklyn NY")
    println("Label: orderId=${label.orderId}, barcode=${label.barcode}")
    println()

    println("--- Object Expressions ---")
    setupEventHandling()
}

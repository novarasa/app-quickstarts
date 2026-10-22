package com.kotlinkata.oop

import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * # Kata 09: Delegation — Composition Made Easy
 *
 * Kotlin has two forms of delegation:
 *   1. Interface delegation (`by`) — implement an interface by delegating to another object
 *   2. Property delegation (`by`) — delegate get/set of a property to a delegate object
 *
 * Java equivalents:
 *   1. Manual delegation: write forwarding methods by hand (error-prone, verbose)
 *   2. No direct equivalent for property delegates
 *
 * Delegation > Inheritance for reuse in most cases.
 * "Favor composition over inheritance" — GoF. Kotlin makes this trivial.
 *
 * Topics:
 *   - Interface delegation with `by`
 *   - Built-in delegates: lazy, observable, vetoable
 *   - Map-backed properties
 *   - Custom property delegates
 *   - Delegated properties for cross-cutting concerns
 */

// ─────────────────────────────────────────────────────────────────────────────
// 1. Interface Delegation
// ─────────────────────────────────────────────────────────────────────────────

/**
 * `class A(val b: B) : Interface by b` — A implements Interface by forwarding
 * all calls to b. Only override what you need to change.
 *
 * Java equivalent:
 *   class A implements Interface {
 *       private final B b;
 *       A(B b) { this.b = b; }
 *       void method1() { b.method1(); }   // boilerplate!
 *       void method2() { b.method2(); }   // boilerplate!
 *       // ... forwarding every method manually
 *   }
 */
interface OrderRepository {
    fun findById(id: String): PersistedOrderData?
    fun save(order: PersistedOrderData): PersistedOrderData
    fun delete(id: String): Boolean
    fun findByCustomerId(customerId: String): List<PersistedOrderData>
}

data class PersistedOrderData(val id: String, val customerId: String, val total: Double)

// Base implementation
class InMemoryOrderRepository : OrderRepository {
    private val storage = mutableMapOf<String, PersistedOrderData>()

    override fun findById(id: String): PersistedOrderData? = storage[id]
    override fun save(order: PersistedOrderData): PersistedOrderData {
        storage[order.id] = order
        return order
    }
    override fun delete(id: String): Boolean = storage.remove(id) != null
    override fun findByCustomerId(customerId: String): List<PersistedOrderData> =
        storage.values.filter { it.customerId == customerId }
}

// Decorator: adds logging WITHOUT inheriting from InMemoryOrderRepository
// Delegation handles all forwarding — only override methods you want to decorate
class LoggingOrderRepository(
    private val delegate: OrderRepository
) : OrderRepository by delegate {  // 'by delegate' generates all forwarding methods!

    // Override only the methods we want to log — others are forwarded automatically
    override fun save(order: PersistedOrderData): PersistedOrderData {
        println("[LOG] Saving order ${order.id}")
        val saved = delegate.save(order)
        println("[LOG] Saved order ${saved.id} successfully")
        return saved
    }

    override fun delete(id: String): Boolean {
        println("[LOG] Deleting order $id")
        val result = delegate.delete(id)
        println("[LOG] Delete result for $id: $result")
        return result
    }
}

// Decorator: adds caching
class CachingOrderRepository(
    private val delegate: OrderRepository
) : OrderRepository by delegate {
    private val cache = mutableMapOf<String, PersistedOrderData>()

    override fun findById(id: String): PersistedOrderData? {
        return cache.getOrPut(id) {
            delegate.findById(id) ?: return null  // don't cache nulls
        }
    }

    override fun save(order: PersistedOrderData): PersistedOrderData {
        cache[order.id] = order
        return delegate.save(order)
    }
}

fun interfaceDelegationExamples() {
    val base = InMemoryOrderRepository()
    val logged = LoggingOrderRepository(base)
    val cached = CachingOrderRepository(logged)

    // Use the fully decorated repository
    val order = PersistedOrderData("ORD-001", "CUST-1", 99.99)
    cached.save(order)
    println("Found: ${cached.findById("ORD-001")}")
    println("Found again (cached): ${cached.findById("ORD-001")}")
    cached.delete("ORD-001")
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. lazy — Initialize Once, on First Access
// ─────────────────────────────────────────────────────────────────────────────

/**
 * `val prop by lazy { ... }` — the lambda runs only on first access.
 * Thread-safe by default (LazyThreadSafetyMode.SYNCHRONIZED).
 *
 * Java equivalent: double-checked locking (verbose, error-prone).
 *
 * Perfect for:
 *   - Expensive computations you might not always need
 *   - Circular dependencies at initialization time
 *   - Config objects that need the full context to initialize
 */
class OrderAnalyticsService(private val repository: OrderRepository) {

    // Only loaded when first accessed — database query deferred
    val topCustomers: List<String> by lazy {
        println("[LAZY] Loading top customers (expensive query)...")
        listOf("CUST-001", "CUST-005", "CUST-012")  // simulated expensive load
    }

    // Different thread safety modes
    val reportMetadata: Map<String, String> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        // PUBLICATION: multiple threads may initialize, but first one wins
        mapOf("generated_at" to System.currentTimeMillis().toString(), "version" to "2.0")
    }

    // Non-thread-safe lazy (single-threaded contexts only)
    val cachedStats: List<String> by lazy(LazyThreadSafetyMode.NONE) {
        listOf("Total orders: 1234", "Revenue: $98,765")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. observable and vetoable
// ─────────────────────────────────────────────────────────────────────────────

/**
 * observable: callback on every property change
 * vetoable: callback that can REJECT a change (return false to veto)
 *
 * Java equivalent: PropertyChangeSupport (verbose) or custom setters.
 */
class OrderTracker {
    var status: String by Delegates.observable("PENDING") { prop, old, new ->
        println("[CHANGE] ${prop.name}: '$old' → '$new'")
        notifyStatusChange(old, new)
    }

    var priority: Int by Delegates.vetoable(5) { prop, old, new ->
        val allowed = new in 1..10
        if (!allowed) println("[VETO] ${prop.name}: rejected value $new (must be 1-10)")
        allowed  // return true to accept, false to reject
    }

    var itemCount: Int by Delegates.observable(0) { _, old, new ->
        if (new < old) println("[WARN] Item count decreased from $old to $new")
    }

    private fun notifyStatusChange(old: String, new: String) {
        // In real code: publish domain event, send webhook, etc.
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Map-Backed Properties
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Properties can be backed by a Map — useful for:
 *   - Dynamic configurations
 *   - Deserializing from arbitrary JSON/YAML maps
 *   - Feature flags
 */
class OrderConfiguration(private val config: Map<String, Any>) {
    // Property delegation to map — reads config["maxRetries"] automatically
    val maxRetries: Int by config
    val timeoutMs: Long by config
    val enableBulkDiscounts: Boolean by config
    val apiBaseUrl: String by config
}

class MutableOrderConfig(private val config: MutableMap<String, Any> = mutableMapOf()) {
    var maxRetries: Int by config
    var timeoutMs: Long by config
    var enableBulkDiscounts: Boolean by config
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Custom Property Delegate
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Build your own delegate by implementing ReadOnlyProperty<T, V> or ReadWriteProperty<T, V>.
 *
 * Real use cases:
 *   - Environment variable properties
 *   - Validation on set
 *   - Encrypted storage
 *   - Automatic type coercion
 */

// Delegate that reads from environment variables with a fallback
class EnvDelegate(private val envKey: String, private val default: String) :
    ReadWriteProperty<Any?, String> {

    private var overrideValue: String? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): String =
        overrideValue ?: System.getenv(envKey) ?: default

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        overrideValue = value
    }
}

fun env(key: String, default: String = "") = EnvDelegate(key, default)

// Application config using environment-backed delegates
object AppConfig {
    var databaseUrl: String by env("DATABASE_URL", "jdbc:h2:mem:testdb")
    var apiKey: String by env("API_KEY", "dev-key-not-for-production")
    var port: String by env("PORT", "8080")
}

// Delegate with validation
class ValidatedProperty<T>(
    private var value: T,
    private val validator: (T) -> Boolean,
    private val errorMessage: (T) -> String
) : ReadWriteProperty<Any?, T> {

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        require(validator(value)) { errorMessage(value) }
        this.value = value
    }
}

fun <T> validated(initial: T, validator: (T) -> Boolean, error: (T) -> String) =
    ValidatedProperty(initial, validator, error)

class OrderSettings {
    var maxItemsPerOrder: Int by validated(
        initial = 50,
        validator = { it in 1..500 },
        error = { "maxItemsPerOrder must be 1-500, got: $it" }
    )

    var taxRate: Double by validated(
        initial = 0.08,
        validator = { it in 0.0..1.0 },
        error = { "taxRate must be 0.0-1.0, got: $it" }
    )
}

fun delegationExamples() {
    println("--- Interface Delegation ---")
    interfaceDelegationExamples()

    println("\n--- lazy ---")
    val repo = InMemoryOrderRepository()
    val analytics = OrderAnalyticsService(repo)
    println("Accessing topCustomers for the first time:")
    println(analytics.topCustomers)
    println("Accessing again (cached, no log):")
    println(analytics.topCustomers)

    println("\n--- observable & vetoable ---")
    val tracker = OrderTracker()
    tracker.status = "CONFIRMED"
    tracker.status = "SHIPPED"
    tracker.priority = 3     // accepted
    tracker.priority = 15    // vetoed!
    println("Priority after veto attempt: ${tracker.priority}")

    println("\n--- Map-backed properties ---")
    val config = OrderConfiguration(mapOf(
        "maxRetries" to 3,
        "timeoutMs" to 30_000L,
        "enableBulkDiscounts" to true,
        "apiBaseUrl" to "https://api.example.com"
    ))
    println("maxRetries: ${config.maxRetries}")
    println("apiBaseUrl: ${config.apiBaseUrl}")

    println("\n--- Custom delegate ---")
    println("DB URL: ${AppConfig.databaseUrl}")
    AppConfig.databaseUrl = "jdbc:postgresql://prod/orders"
    println("DB URL (overridden): ${AppConfig.databaseUrl}")

    val settings = OrderSettings()
    settings.maxItemsPerOrder = 100
    try {
        settings.maxItemsPerOrder = 1000  // should throw
    } catch (e: IllegalArgumentException) {
        println("Caught: ${e.message}")
    }
    println("Max items: ${settings.maxItemsPerOrder}")
}

fun main() {
    println("=== Kata 09: Delegation ===\n")
    delegationExamples()
}

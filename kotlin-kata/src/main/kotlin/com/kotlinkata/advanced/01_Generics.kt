package com.kotlinkata.advanced

/**
 * # Kata 15: Generics — Variance, Reification, and Beyond
 *
 * Kotlin generics are similar to Java's but with two important improvements:
 *   1. Declaration-site variance (in/out) vs Java's use-site wildcards (? extends/? super)
 *   2. Reified type parameters — NO Class<T> parameters needed for inline functions
 *
 * Java wildcard comparison:
 *   Java: List<? extends Animal>   →   Kotlin: List<out Animal>
 *   Java: List<? super Dog>        →   Kotlin: List<in Dog>
 *
 * Kotlin's advantage: you declare variance ONCE on the class/interface,
 * not at every call site. Cleaner API, no wildcard hell.
 *
 * Topics:
 *   - Type parameters (review)
 *   - Covariance (out) — producer types
 *   - Contravariance (in) — consumer types
 *   - Invariance (neither in nor out)
 *   - Reified generics — accessing T at runtime without Class<T>
 *   - Generic constraints (where clauses)
 *   - Generic extension functions
 *   - Star projection (*)
 */

// ─────────────────────────────────────────────────────────────────────────────
// 1. Basic Generics (Review)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Generic classes, interfaces, and functions work like Java.
 * Type inference means you rarely need to specify type params explicitly.
 */
interface Repository<T, ID> {
    fun findById(id: ID): T?
    fun save(entity: T): T
    fun delete(id: ID)
    fun findAll(): List<T>
}

data class OrderEntity(val id: String, val customerId: String, val total: Double)
data class ProductEntity(val id: String, val name: String, val price: Double)

// Generic function with constraint
fun <T : Comparable<T>> List<T>.minMax(): Pair<T, T>? {
    if (isEmpty()) return null
    return Pair(minOf { it }, maxOf { it })
}

// Multiple type parameters
fun <K, V> Map<K, V>.invert(): Map<V, K> = entries.associate { (k, v) -> v to k }

// Upper bound constraint (like Java's <T extends Comparable>)
fun <T : Number> List<T>.sumAsDouble(): Double = sumOf { it.toDouble() }

fun basicGenericsExamples() {
    val prices = listOf(19.99, 49.99, 9.99, 79.99, 29.99)
    println("Min/max: ${prices.minMax()}")
    println("Sum: ${prices.sumAsDouble()}")

    val statusMap = mapOf("ORD-001" to "SHIPPED", "ORD-002" to "PENDING")
    val inverted = statusMap.invert()
    println("Inverted: $inverted")
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Covariance (out) — Producer Types
// ─────────────────────────────────────────────────────────────────────────────

/**
 * `out T` means the type is a PRODUCER of T — it only OUTPUTS T, never takes it as input.
 * A Producer<Cat> is a subtype of Producer<Animal>.
 *
 * Java: <? extends T>  (but you must write it at every call site)
 * Kotlin: out T        (declared once on the interface — declaration-site variance)
 *
 * Rule: out T = can READ T, cannot WRITE T.
 * Mnemonic: "out" means values flow OUT of the container.
 *
 * Built-in Kotlin: List<out E> (read-only list is covariant), Sequence<out T>
 */
interface Supplier<out T> {
    fun get(): T
}

interface Producer<out T> {
    fun produce(): T
    fun produceAll(): List<T>
    // fun consume(value: T): Unit  // ❌ out T cannot appear in parameter position
}

class OrderProducer : Producer<OrderEntity> {
    override fun produce(): OrderEntity = OrderEntity("ORD-001", "CUST-1", 99.99)
    override fun produceAll(): List<OrderEntity> = listOf(produce())
}

fun processOrders(producer: Producer<Any>) {  // accepts Producer<OrderEntity> because out!
    val item = producer.produce()
    println("Produced: $item")
}

fun covarianceExamples() {
    val orderProducer: Producer<OrderEntity> = OrderProducer()

    // Covariance: Producer<OrderEntity> can be assigned to Producer<Any>
    // because `out T` means Producer is covariant in T
    processOrders(orderProducer)   // ✅ Works because of out T

    // Java comparison — without declaration-site variance, you'd need:
    // processOrders(Producer<? extends Object> producer) — at every call site!

    // Kotlin's List is covariant (out E):
    val cats: List<String> = listOf("cat1", "cat2")
    val animals: List<Any> = cats   // ✅ Works — List<String> is a subtype of List<Any>
    println("Covariant list: $animals")
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Contravariance (in) — Consumer Types
// ─────────────────────────────────────────────────────────────────────────────

/**
 * `in T` means the type is a CONSUMER of T — it only takes T as input, never outputs it.
 * A Consumer<Animal> is a subtype of Consumer<Cat>.
 * (Reversed from what you might expect — that's what makes it contravariant)
 *
 * Java: <? super T>  (use-site)
 * Kotlin: in T       (declaration-site)
 *
 * Rule: in T = can WRITE T, cannot READ T (only as Any?).
 * Mnemonic: "in" means values flow INTO the container.
 *
 * Built-in Kotlin: Comparable<in T>, Comparator<in T>
 */
interface Consumer<in T> {
    fun consume(value: T)
    // fun produce(): T  // ❌ in T cannot appear in return type position
}

interface Validator<in T> {
    fun validate(value: T): Boolean
    fun validateAll(values: List<T>): List<Boolean> = values.map { validate(it) }
}

class AnyValidator : Validator<Any> {
    override fun validate(value: Any): Boolean {
        println("Validating: $value")
        return value.toString().isNotBlank()
    }
}

fun validateOrder(validator: Validator<OrderEntity>, order: OrderEntity): Boolean {
    return validator.validate(order)
}

fun contravarianceExamples() {
    val anyValidator: Validator<Any> = AnyValidator()

    // Contravariance: Validator<Any> can be used where Validator<OrderEntity> is expected
    // because `in T` means Validator is contravariant in T
    val order = OrderEntity("ORD-001", "CUST-1", 99.99)
    val result = validateOrder(anyValidator, order)  // ✅ Works because of in T
    println("Valid: $result")

    // Java comparison — without declaration-site, you'd need:
    // validateOrder(Validator<? super OrderEntity>, ...)  — every call site

    // Comparator is contravariant — built into Kotlin
    val stringComparator: Comparator<String> = compareBy { it.length }
    // You can use a Comparator<Any> where Comparator<String> is needed:
    val anyComparator: Comparator<Any> = compareBy { it.toString().length }
    val list = mutableListOf("bb", "aaa", "c")
    list.sortWith(anyComparator)   // Comparator<Any> used for List<String>
    println("Sorted: $list")
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Reified Type Parameters — The Killer Feature
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Java generics suffer from TYPE ERASURE — T is erased at runtime.
 * This means you can't do:
 *   fun <T> getInstances(list: List<Any>): List<T> {
 *       return list.filterIsInstance<T>()   // ❌ T is not known at runtime!
 *   }
 *
 * Java workaround: pass Class<T> as a parameter — ugly!
 *   public <T> List<T> getInstances(List<Object> list, Class<T> clazz) { ... }
 *
 * Kotlin solution: `inline fun <reified T>` — the type T is available at runtime!
 * Works ONLY with inline functions (because inline copies the bytecode).
 *
 * This eliminates entire categories of Class<T> parameter patterns.
 */

// Java-style (don't do this in Kotlin):
fun <T> filterByTypeJavaStyle(items: List<Any>, clazz: Class<T>): List<T> {
    @Suppress("UNCHECKED_CAST")
    return items.filter { clazz.isInstance(it) } as List<T>
}

// Kotlin reified — no Class<T> parameter needed!
inline fun <reified T> List<Any>.filterByType(): List<T> = filterIsInstance<T>()

// Access T's type at runtime
inline fun <reified T> printTypeName() = println("Type: ${T::class.simpleName}")
inline fun <reified T> isInstanceOf(value: Any): Boolean = value is T
inline fun <reified T> createEmptyList(): List<T> = emptyList<T>()

// Real-world: type-safe event bus
class EventBus {
    @PublishedApi internal val handlers = mutableMapOf<String, MutableList<(Any) -> Unit>>()

    inline fun <reified T> subscribe(noinline handler: (T) -> Unit) {
        val typeName = T::class.qualifiedName ?: error("Cannot subscribe to anonymous type")
        handlers.getOrPut(typeName) { mutableListOf() }
            .add { event -> if (event is T) handler(event) }
    }

    inline fun <reified T> publish(event: T) {
        val typeName = T::class.qualifiedName ?: return
        handlers[typeName]?.forEach { it(event as Any) }
    }
}

data class OrderPlacedEvent(val orderId: String, val total: Double)
data class PaymentReceivedEvent(val orderId: String, val transactionId: String)

fun reifiedExamples() {
    // No Class<T> parameter needed
    val mixed: List<Any> = listOf("hello", 42, true, "world", 99.99, OrderEntity("ORD-1", "C-1", 50.0))
    val strings = mixed.filterByType<String>()
    val orders = mixed.filterByType<OrderEntity>()
    println("Strings: $strings")
    println("Orders: $orders")

    printTypeName<OrderEntity>()
    printTypeName<List<String>>()
    println("Is String: ${isInstanceOf<String>("hello")}")
    println("Is Int: ${isInstanceOf<Int>("hello")}")

    // Type-safe event bus
    val bus = EventBus()
    bus.subscribe<OrderPlacedEvent> { event ->
        println("Order placed: ${event.orderId} for $${event.total}")
    }
    bus.subscribe<PaymentReceivedEvent> { event ->
        println("Payment received for ${event.orderId}: ${event.transactionId}")
    }

    bus.publish(OrderPlacedEvent("ORD-001", 149.99))
    bus.publish(PaymentReceivedEvent("ORD-001", "TXN-12345"))
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Generic Constraints
// ─────────────────────────────────────────────────────────────────────────────

/**
 * `where` clause allows multiple constraints on a type parameter.
 *
 * Java: <T extends Comparable<T> & Serializable>
 * Kotlin: fun <T> foo(x: T) where T : Comparable<T>, T : java.io.Serializable
 */
interface Identifiable { val id: String }
interface Auditable { val createdAt: java.time.Instant }

data class AuditedOrder(
    override val id: String,
    override val createdAt: java.time.Instant = java.time.Instant.now(),
    val total: Double
) : Identifiable, Auditable

// Require T to implement both interfaces
fun <T> findLatest(items: List<T>): T?
    where T : Identifiable, T : Auditable {
    return items.maxByOrNull { it.createdAt }
}

fun <T : Comparable<T>> clamp(value: T, min: T, max: T): T = when {
    value < min -> min
    value > max -> max
    else        -> value
}

fun constraintsExamples() {
    val orders = listOf(
        AuditedOrder("ORD-1", java.time.Instant.now().minusSeconds(3600), 99.99),
        AuditedOrder("ORD-2", java.time.Instant.now().minusSeconds(1800), 149.99),
        AuditedOrder("ORD-3", java.time.Instant.now(), 49.99)
    )
    println("Latest order: ${findLatest(orders)?.id}")

    println("Clamped 150 to [0,100]: ${clamp(150, 0, 100)}")
    println("Clamped -5 to [0,100]: ${clamp(-5, 0, 100)}")
    println("Clamped 'z' to ['a','m']: ${clamp('z', 'a', 'm')}")
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. Star Projection
// ─────────────────────────────────────────────────────────────────────────────

/**
 * `List<*>` = `List<out Any?>` — you can read from it but not write to it.
 * Use when you don't care about the specific type parameter.
 *
 * Java equivalent: List<?>  (unbounded wildcard)
 */
fun printCollectionInfo(collection: Collection<*>) {
    println("Size: ${collection.size}, isEmpty: ${collection.isEmpty()}")
    collection.forEach { print("$it ") }
    println()
}

fun main() {
    println("=== Kata 15: Generics ===\n")
    println("--- Basic Generics ---")
    basicGenericsExamples()
    println("\n--- Covariance (out) ---")
    covarianceExamples()
    println("\n--- Contravariance (in) ---")
    contravarianceExamples()
    println("\n--- Reified Types ---")
    reifiedExamples()
    println("\n--- Constraints ---")
    constraintsExamples()
    println("\n--- Star Projection ---")
    printCollectionInfo(listOf(1, 2, 3))
    printCollectionInfo(setOf("a", "b"))
}

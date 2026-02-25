package com.kotlinkata.advanced

/**
 * # Kata 16: Inline Functions — Performance Without Compromise
 *
 * By default, lambdas passed to higher-order functions create anonymous class
 * instances at runtime, causing performance overhead (object creation, virtual dispatch).
 *
 * `inline` copies the function body AND the lambda body to the call site.
 * Result: ZERO overhead — no lambda allocation, no virtual dispatch.
 *
 * Java comparison: Java has no `inline`. Performance-critical code uses
 * primitive streams, method handles, or manual loop unrolling.
 *
 * Additional powers of inline:
 *   - `reified` type parameters (see Generics kata)
 *   - Non-local returns from lambdas
 *   - crossinline and noinline for fine-grained control
 *
 * Topics:
 *   - Why inline exists (performance cost of lambdas)
 *   - Basic inline functions
 *   - Non-local returns
 *   - noinline — don't inline a specific lambda parameter
 *   - crossinline — allow inlining but no non-local return
 *   - Real-world patterns: timing, transactions, retry, resource management
 */

// ─────────────────────────────────────────────────────────────────────────────
// 1. The Problem — Lambda Overhead
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Without inline, this creates an object allocation on every call:
 *
 *   // Non-inline — creates a Function1 object each time
 *   fun filter(list: List<Int>, predicate: (Int) -> Boolean): List<Int> {
 *       return list.filter(predicate)
 *   }
 *
 * Compiled Java equivalent of a lambda call:
 *   Function1 predicate = new Function1() {
 *       public Object invoke(Object it) { return ((Integer)it) > 5; }
 *   };
 *   filter(list, predicate);
 *
 * Compiled Java equivalent after inline:
 *   // Lambda body copied directly — no object created!
 *   List<Integer> result = new ArrayList<>();
 *   for (Integer item : list) {
 *       if (item > 5) result.add(item);
 *   }
 */

// Regular (non-inline) — benchmarks show ~10-30ns overhead per call
fun <T> filterRegular(list: List<T>, predicate: (T) -> Boolean): List<T> =
    list.filter(predicate)

// Inline — zero lambda allocation overhead
inline fun <T> filterInline(list: List<T>, predicate: (T) -> Boolean): List<T> =
    list.filter(predicate)

// ─────────────────────────────────────────────────────────────────────────────
// 2. Basic Inline Functions
// ─────────────────────────────────────────────────────────────────────────────

/**
 * When to use inline:
 *   ✅ Higher-order functions called frequently in tight loops
 *   ✅ Any function that takes a lambda and you want reified types
 *   ✅ Resource management wrappers (withTransaction, withLock, etc.)
 *   ✅ Control flow abstractions (retry, timing, etc.)
 *
 * When NOT to use inline:
 *   ❌ Large function bodies (code bloat at call sites)
 *   ❌ Recursive functions (can't inline recursive calls)
 *   ❌ Functions with stored/callbacks lambdas (must use noinline)
 *   ❌ When the lambda can be null (use noinline)
 */

// Common pattern: resource acquisition with automatic cleanup
inline fun <T : AutoCloseable, R> T.use(block: (T) -> R): R {
    var exception: Throwable? = null
    try {
        return block(this)
    } catch (e: Throwable) {
        exception = e
        throw e
    } finally {
        when (exception) {
            null -> close()
            else -> try { close() } catch (closeException: Throwable) {
                // Suppress close exception when there's already a primary exception
            }
        }
    }
}

// Timing wrapper
inline fun <T> measureAndLog(operation: String, block: () -> T): T {
    val start = System.nanoTime()
    return try {
        block()
    } finally {
        val elapsed = (System.nanoTime() - start) / 1_000_000
        println("[$operation] took ${elapsed}ms")
    }
}

// Retry logic — inline for performance in hot paths
inline fun <T> retry(
    times: Int,
    delayMs: Long = 0,
    block: (attempt: Int) -> T
): T {
    require(times > 0) { "Retry count must be positive" }
    var lastException: Exception? = null
    repeat(times) { attempt ->
        try {
            return block(attempt + 1)  // non-local return — exits the outer function!
        } catch (e: Exception) {
            lastException = e
            if (attempt < times - 1 && delayMs > 0) {
                Thread.sleep(delayMs)
            }
        }
    }
    throw lastException ?: RuntimeException("All $times attempts failed")
}

// Transaction-like wrapper
class FakeTransaction {
    var committed = false
    var rolledBack = false
    fun commit() { committed = true; println("[TX] Committed") }
    fun rollback() { rolledBack = true; println("[TX] Rolled back") }
}

inline fun <T> withTransaction(block: (FakeTransaction) -> T): T {
    val tx = FakeTransaction()
    return try {
        val result = block(tx)
        tx.commit()
        result
    } catch (e: Exception) {
        tx.rollback()
        throw e
    }
}

fun basicInlineExamples() {
    val result = measureAndLog("fetchOrders") {
        Thread.sleep(5)  // simulate work
        listOf("ORD-001", "ORD-002", "ORD-003")
    }
    println("Orders: $result")

    // Retry with exponential backoff
    var attempts = 0
    val orderId = retry(times = 3, delayMs = 10) { attempt ->
        attempts++
        if (attempt < 3) throw RuntimeException("Transient error on attempt $attempt")
        "ORD-${System.currentTimeMillis()}"
    }
    println("Got order after $attempts attempts: $orderId")

    // Transaction
    val savedOrder = withTransaction { tx ->
        println("  Saving order...")
        "ORD-001"  // the "saved" value
    }
    println("Saved: $savedOrder")
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Non-Local Returns
// ─────────────────────────────────────────────────────────────────────────────

/**
 * GAME CHANGER: In inline lambdas, `return` returns from the ENCLOSING function,
 * not just the lambda.
 *
 * Java: You cannot `return` from a lambda — must use exceptions, flags, or Optional.
 * Kotlin inline: `return` in the lambda body returns from the outer function!
 *
 * This is WHY forEach, let, run etc. are inline — it allows natural control flow.
 *
 * Non-local returns ONLY work in lambdas passed to inline functions.
 */
fun processOrders(orders: List<String>): String {
    orders.forEach { orderId ->  // forEach is inline
        if (orderId == "ORD-INVALID") {
            return "Found invalid order: $orderId"  // returns from processOrders!
        }
        println("Processing: $orderId")
    }
    return "All orders processed"
}

// This is WHY you must qualify returns in lambdas:
// return@forEach — local return (continues to next iteration)
// return — non-local return from enclosing function (only for inline)
fun demonstrateReturnTypes() {
    val orders = listOf("ORD-001", "ORD-002", "ORD-003")

    println("With return@forEach (labeled return):")
    orders.forEach { orderId ->
        if (orderId == "ORD-002") {
            return@forEach  // skip this element, continue loop
        }
        println("  Processed: $orderId")
    }
    println("After forEach")

    println("\nWith regular return (non-local):")
    println(processOrders(listOf("ORD-001", "ORD-INVALID", "ORD-002")))
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. noinline — Don't Inline a Specific Lambda
// ─────────────────────────────────────────────────────────────────────────────

/**
 * If an inline function receives a lambda that it STORES (passes to another
 * function, stores in a variable, etc.), that lambda cannot be inlined.
 * Use `noinline` to opt that specific parameter out of inlining.
 *
 * Use cases:
 *   - Storing the lambda for later use (callbacks, handlers)
 *   - Passing the lambda to a non-inline function
 */
class EventHandler(val handler: (String) -> Unit)  // stores the lambda

inline fun setupWithCallback(
    noinline onEvent: (String) -> Unit,  // noinline because it's stored
    action: () -> Unit
): EventHandler {
    action()  // inlined
    return EventHandler(onEvent)  // onEvent is stored, must be noinline
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. crossinline — Allow Inlining but Prevent Non-Local Returns
// ─────────────────────────────────────────────────────────────────────────────

/**
 * `crossinline` marks a lambda as inlined but prevents non-local returns.
 * Use when the lambda will be called from a different execution context
 * where non-local returns would be unsafe (e.g., from another thread or Runnable).
 */
inline fun runInThread(crossinline block: () -> Unit) {
    Thread {
        block()  // inlined but crossinline prevents non-local return
    }.start()
}

inline fun runAsRunnable(crossinline block: () -> Unit): Runnable = Runnable { block() }

// ─────────────────────────────────────────────────────────────────────────────
// 6. Real-World: Production Patterns with Inline
// ─────────────────────────────────────────────────────────────────────────────

// Performance-critical filtering (no lambda object creation)
inline fun <T> List<T>.filterAndCount(predicate: (T) -> Boolean): Pair<List<T>, Int> {
    val filtered = filter(predicate)
    return filtered to filtered.size
}

// Conditional execution (no overhead compared to if/else)
inline fun executeIf(condition: Boolean, block: () -> Unit) {
    if (condition) block()
}

inline fun <T> T.applyIf(condition: Boolean, block: T.() -> T): T =
    if (condition) block() else this

// Benchmark helper
inline fun benchmark(iterations: Int = 1_000_000, block: () -> Unit): Long {
    // Warm up JIT
    repeat(1000) { block() }
    val start = System.nanoTime()
    repeat(iterations) { block() }
    return (System.nanoTime() - start) / iterations
}

// Scoped transaction with result
inline fun <T> withOrderTransaction(
    orderId: String,
    block: () -> T
): Result<T> = runCatching {
    println("[TX] Starting transaction for $orderId")
    val result = block()
    println("[TX] Committing transaction for $orderId")
    result
}.onFailure { e ->
    println("[TX] Rolling back transaction for $orderId: ${e.message}")
}

data class ProcessedOrderResult(val id: String, val status: String, val total: Double)

fun realWorldInlineExamples() {
    // Conditional order processing
    val isExpressAvailable = true
    val order = ProcessedOrderResult("ORD-001", "PENDING", 149.99)
        .applyIf(isExpressAvailable) {
            copy(status = "EXPRESS_QUEUED")
        }
    println("Order: $order")

    // Transaction-scoped operations
    val result = withOrderTransaction("ORD-001") {
        println("  Fetching inventory...")
        println("  Reserving items...")
        println("  Creating shipment...")
        "Shipment SHP-001 created"
    }
    result.fold(
        onSuccess = { println("Success: $it") },
        onFailure = { println("Failed: ${it.message}") }
    )

    // Benchmarking
    val inlineTime = benchmark(100_000) {
        listOf(1, 2, 3, 4, 5).filter { it % 2 == 0 }
    }
    println("Avg inline filter: ${inlineTime}ns")
}

fun main() {
    println("=== Kata 16: Inline Functions ===\n")
    println("--- Basic Inline ---")
    basicInlineExamples()
    println("\n--- Non-Local Returns ---")
    demonstrateReturnTypes()
    println("\n--- Real-World Patterns ---")
    realWorldInlineExamples()
}

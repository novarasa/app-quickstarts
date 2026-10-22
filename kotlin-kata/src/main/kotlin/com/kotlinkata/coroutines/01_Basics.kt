package com.kotlinkata.coroutines

import kotlinx.coroutines.*

/**
 * # Kata 18: Coroutines Basics
 *
 * Coroutines are Kotlin's approach to asynchronous programming.
 * They are lightweight threads — you can have millions of them without the
 * memory and context-switching overhead of OS threads.
 *
 * Java equivalent: CompletableFuture, Virtual Threads (Java 21), reactive (RxJava/Reactor)
 *
 * Key advantage over Java:
 *   - Sequential-looking code that is actually asynchronous
 *   - Structured concurrency — coroutines are organized in a hierarchy
 *   - Lightweight (coroutines = continuations, not OS threads)
 *   - Cancellation works throughout the hierarchy
 *
 * Topics:
 *   - suspend functions (the foundation)
 *   - runBlocking (bridge between blocking and coroutine world)
 *   - launch (fire-and-forget)
 *   - async/await (concurrent with a result)
 *   - CoroutineScope and CoroutineContext
 *   - Dispatchers (which thread pool)
 *   - Delay and timeout
 */

// ─────────────────────────────────────────────────────────────────────────────
// 1. suspend Functions — The Foundation
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A `suspend` function can be suspended (paused) and resumed later WITHOUT
 * blocking the underlying thread. The thread is FREE to do other work while
 * the coroutine is suspended (e.g., waiting for I/O).
 *
 * Java equivalent: CompletableFuture<T> return type
 *   suspend fun fetchOrder(id: String): Order  ≈  CompletableFuture<Order> fetchOrder(String id)
 *
 * But suspend functions look and act like regular functions — sequential code
 * that is actually asynchronous. No callback hell, no CompletableFuture chaining.
 *
 * RULE: suspend functions can only be called from another suspend function
 * or from a coroutine builder (launch, async, runBlocking).
 */

data class OrderData(val id: String, val customerId: String, val total: Double)
data class CustomerData(val id: String, val email: String, val name: String)
data class InventoryData(val sku: String, val available: Int)

// Simulated suspend functions (in real code: actual network/DB calls)
suspend fun fetchOrder(orderId: String): OrderData {
    delay(100)  // suspend (not block!) for 100ms — simulates I/O
    return OrderData(orderId, "CUST-001", 149.99)
}

suspend fun fetchCustomer(customerId: String): CustomerData {
    delay(80)   // suspend — free to run other coroutines while waiting
    return CustomerData(customerId, "john@example.com", "John Doe")
}

suspend fun checkInventory(sku: String): InventoryData {
    delay(50)
    return InventoryData(sku, if (sku == "SKU-OUT") 0 else 100)
}

suspend fun processPayment(orderId: String, amount: Double): String {
    delay(200)  // payment processing takes longer
    return "TXN-${orderId.hashCode().toString(16).uppercase()}"
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. runBlocking — Bridge Between Blocking and Coroutine World
// ─────────────────────────────────────────────────────────────────────────────

/**
 * `runBlocking` is the entry point from regular (blocking) code to coroutines.
 * It BLOCKS the calling thread until all coroutines inside complete.
 *
 * Use cases:
 *   - main() function in demo/test code
 *   - Tests (or use runTest from kotlinx-coroutines-test)
 *   - Bridging to legacy blocking code
 *
 * DO NOT use runBlocking inside a coroutine — it defeats the purpose!
 */
fun bridgeExample() = runBlocking {
    // Now we're in a coroutine — can call suspend functions
    val order = fetchOrder("ORD-001")
    println("Fetched: $order")

    // Can also launch child coroutines
    val job = launch {
        val customer = fetchCustomer(order.customerId)
        println("Customer: ${customer.name}")
    }
    job.join()  // wait for the child coroutine to finish
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. launch — Fire and Forget
// ─────────────────────────────────────────────────────────────────────────────

/**
 * `launch` starts a new coroutine that doesn't return a result.
 * Returns a `Job` that you can use to wait for completion or cancel.
 *
 * Java equivalent: ExecutorService.submit(Runnable)
 *
 * launch is "fire and forget" — use it when you don't need the result.
 * For results, use async/await.
 */
fun launchExamples() = runBlocking {
    println("Starting three operations concurrently...")

    // These three launch blocks run CONCURRENTLY (not sequentially)
    val job1 = launch {
        delay(200)
        println("Job 1: Audit log written")
    }

    val job2 = launch {
        delay(100)
        println("Job 2: Cache updated")
    }

    val job3 = launch {
        delay(150)
        println("Job 3: Analytics event sent")
    }

    // Wait for all to complete (structured concurrency — automatically waits)
    // These jobs finish in order: job2, job3, job1 (by delay duration)
    joinAll(job1, job2, job3)
    println("All background tasks completed")
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. async/await — Concurrent With a Result
// ─────────────────────────────────────────────────────────────────────────────

/**
 * `async` starts a coroutine that RETURNS a Deferred<T> — a future result.
 * `await()` suspends until the result is available.
 *
 * Java equivalent: CompletableFuture.supplyAsync(...)  then  .get() or .join()
 *
 * async allows you to start MULTIPLE operations CONCURRENTLY and then collect
 * all results — a major advantage over sequential await calls.
 */
fun asyncAwaitExamples() = runBlocking {
    println("=== Sequential vs Concurrent ===")

    // SEQUENTIAL (slow) — each waits for the previous to complete
    val seqStart = System.currentTimeMillis()
    val order1 = fetchOrder("ORD-001")          // 100ms
    val customer1 = fetchCustomer("CUST-001")   // 80ms
    println("Sequential time: ${System.currentTimeMillis() - seqStart}ms (expected ~180ms)")

    // CONCURRENT (fast) — both start at the same time
    val concStart = System.currentTimeMillis()
    val orderDeferred = async { fetchOrder("ORD-001") }       // starts immediately
    val customerDeferred = async { fetchCustomer("CUST-001") } // starts immediately (no waiting!)

    val order2 = orderDeferred.await()    // wait for result
    val customer2 = customerDeferred.await()  // result likely ready already
    println("Concurrent time: ${System.currentTimeMillis() - concStart}ms (expected ~100ms)")
    println("Order: ${order2.id}, Customer: ${customer2.name}")

    // coroutineScope for concurrent operations — best practice
    println("\n=== Loading order details ===")
    val startTime = System.currentTimeMillis()
    val (order, customer, inventory) = loadOrderDetails("ORD-001")
    println("Loaded in ${System.currentTimeMillis() - startTime}ms")
    println("Order: $order, Customer: $customer, Inventory: $inventory")
}

// Helper: load related data concurrently
suspend fun loadOrderDetails(orderId: String): Triple<OrderData, CustomerData, InventoryData> {
    return coroutineScope {  // creates a scope for concurrent work
        val orderD = async { fetchOrder(orderId) }
        val customerD = async { fetchCustomer("CUST-001") }
        val inventoryD = async { checkInventory("SKU-001") }

        // All three started concurrently — total time ≈ max(100, 80, 50) = 100ms
        Triple(orderD.await(), customerD.await(), inventoryD.await())
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Dispatchers — Which Thread Pool?
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Dispatchers determine which thread(s) execute the coroutine:
 *
 *   Dispatchers.Default   — CPU-intensive work (sorting, computation)
 *                           thread pool size = CPU cores
 *
 *   Dispatchers.IO        — Blocking I/O (network, disk, database calls)
 *                           thread pool can grow to 64 threads
 *
 *   Dispatchers.Main      — UI thread (Android, JavaFX)
 *
 *   Dispatchers.Unconfined — No specific thread (advanced use only)
 *
 * Java equivalent: ExecutorService / ForkJoinPool
 * But unlike Java, you can easily switch dispatchers with withContext().
 */
fun dispatcherExamples() = runBlocking {
    // Switch context for CPU-intensive work
    val result = withContext(Dispatchers.Default) {
        // This runs on the Default dispatcher (computation thread pool)
        (1..1_000_000).sumOf { it.toLong() }
    }
    println("Sum: $result (on Default dispatcher)")

    // Switch context for blocking I/O
    val fileContent = withContext(Dispatchers.IO) {
        // This runs on the IO dispatcher — safe for blocking calls
        // File("orders.csv").readText()  // would be a blocking read
        "simulated file content"
    }
    println("File: $fileContent (on IO dispatcher)")

    // Multiple dispatchers in one coroutine
    val orderSummary = withContext(Dispatchers.IO) {
        // Fetch from DB (blocking I/O)
        val raw = "raw order data"
        withContext(Dispatchers.Default) {
            // Process/compute on CPU-bound dispatcher
            raw.uppercase() + " - PROCESSED"
        }
    }
    println("Summary: $orderSummary")
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. Timeout
// ─────────────────────────────────────────────────────────────────────────────

/**
 * withTimeout throws TimeoutCancellationException if the block takes too long.
 * withTimeoutOrNull returns null instead of throwing.
 *
 * Java equivalent: Future.get(timeout, TimeUnit) — but much cleaner!
 */
fun timeoutExamples() = runBlocking {
    // withTimeoutOrNull — safe version (returns null on timeout)
    val result = withTimeoutOrNull(150) {  // 150ms timeout
        delay(100)   // this completes in time
        "Order processed"
    }
    println("Result: $result")  // "Order processed"

    val timedOut = withTimeoutOrNull(50) {   // 50ms timeout
        delay(200)  // this exceeds the timeout
        "This won't be returned"
    }
    println("Timed out: $timedOut")  // null

    // withTimeout — throws on timeout
    try {
        withTimeout(50) {
            delay(200)
            "Too slow"
        }
    } catch (e: TimeoutCancellationException) {
        println("Caught timeout: ${e.message}")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. CoroutineScope — Context Management
// ─────────────────────────────────────────────────────────────────────────────

/**
 * CoroutineScope provides the lifecycle for coroutines.
 * When a scope is cancelled, all coroutines started in it are cancelled.
 *
 * In real applications:
 *   - Android: viewModelScope, lifecycleScope
 *   - Ktor: Application scope
 *   - Spring: custom scopes or @Component with CoroutineScope
 *   - Standalone: CoroutineScope(SupervisorJob() + Dispatchers.Default)
 */
class OrderProcessingService : CoroutineScope {
    private val job = SupervisorJob()

    // SupervisorJob: if one child fails, siblings are NOT cancelled
    override val coroutineContext = Dispatchers.IO + job

    fun processOrderAsync(orderId: String) = launch {
        println("Processing $orderId...")
        val order = fetchOrder(orderId)
        val txnId = processPayment(orderId, order.total)
        println("$orderId complete: $txnId")
    }

    fun shutdown() {
        job.cancel()  // cancels ALL coroutines in this scope
    }
}

fun coroutineScopeExamples() = runBlocking {
    val service = OrderProcessingService()

    // Start multiple order processings concurrently
    val jobs = (1..3).map { i ->
        service.processOrderAsync("ORD-00$i")
    }

    // Wait for all
    jobs.forEach { it.join() }
    service.shutdown()
    println("All orders processed, service shut down")
}

fun main() {
    println("=== Kata 18: Coroutines Basics ===\n")
    println("--- Bridge Example ---")
    bridgeExample()
    println("\n--- launch ---")
    launchExamples()
    println("\n--- async/await ---")
    asyncAwaitExamples()
    println("\n--- Dispatchers ---")
    dispatcherExamples()
    println("\n--- Timeout ---")
    timeoutExamples()
    println("\n--- CoroutineScope ---")
    coroutineScopeExamples()
}

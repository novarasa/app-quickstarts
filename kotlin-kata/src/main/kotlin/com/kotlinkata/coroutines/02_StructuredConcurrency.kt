package com.kotlinkata.coroutines

import kotlinx.coroutines.*

/**
 * # Kata 19: Structured Concurrency
 *
 * Structured concurrency is the principle that coroutines form a hierarchy —
 * child coroutines cannot outlive their parent scope.
 *
 * Benefits over raw threads:
 *   - No "orphan" coroutines — if parent scope is cancelled, all children are cancelled
 *   - Errors propagate through the hierarchy naturally
 *   - Resource cleanup is guaranteed — coroutines can't leak
 *   - Easy to reason about lifetimes
 *
 * Java analogy: try-with-resources but for concurrent operations
 *
 * Topics:
 *   - Job hierarchy and parent-child relationships
 *   - Cancellation — cooperative cancellation
 *   - Exception handling — supervisorScope vs coroutineScope
 *   - Error propagation
 *   - coroutineScope vs supervisorScope
 *   - CancellationException special handling
 */

// ─────────────────────────────────────────────────────────────────────────────
// 1. Job Hierarchy
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Every coroutine has a Job. When you launch a coroutine inside a scope,
 * the new Job becomes a child of the scope's Job.
 *
 * Hierarchy rules:
 *   - Parent waits for ALL children to complete
 *   - Cancelling parent cancels all children
 *   - Child failure cancels parent (and siblings) unless SupervisorJob
 */
fun jobHierarchyExamples() = runBlocking {
    println("=== Job Hierarchy ===")

    val parentJob = launch(CoroutineName("parent")) {
        println("Parent started: ${coroutineContext[CoroutineName]?.name}")

        val child1 = launch(CoroutineName("child-1")) {
            delay(100)
            println("Child 1 completed")
        }

        val child2 = launch(CoroutineName("child-2")) {
            delay(150)
            println("Child 2 completed")
        }

        println("Parent waiting for children... children: ${coroutineContext[Job]?.children?.count()}")
        // Parent automatically waits for children — no need for joinAll!
    }

    parentJob.join()
    println("Parent and all children completed")
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Cancellation — Cooperative
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Coroutine cancellation is COOPERATIVE — the coroutine must check if it's
 * been cancelled. It doesn't happen automatically mid-execution.
 *
 * Kotlin's built-in suspension points (delay, yield, etc.) all check for
 * cancellation automatically.
 *
 * For CPU-bound loops, you must check manually with:
 *   - isActive check
 *   - yield() which checks and suspends briefly
 *   - ensureActive() which throws if cancelled
 */
suspend fun processOrderBatch(batchId: String, orders: List<String>) {
    for ((index, orderId) in orders.withIndex()) {
        currentCoroutineContext().ensureActive()  // throws CancellationException if coroutine is cancelled
        // Without this, a tight loop would ignore cancellation!

        delay(50)  // delay() also checks for cancellation
        println("Batch $batchId: processed $orderId (${index + 1}/${orders.size})")
    }
}

fun cancellationExamples() = runBlocking {
    println("=== Cancellation ===")

    val orders = (1..10).map { "ORD-${"$it".padStart(3, '0')}" }

    val processingJob = launch {
        processOrderBatch("BATCH-1", orders)
    }

    delay(175)  // let it run for 175ms (processes ~3 orders)
    println("Cancelling job...")
    processingJob.cancel()
    processingJob.join()  // wait for cancellation to complete
    println("Job cancelled. Remaining orders were NOT processed.")
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Cleanup After Cancellation
// ─────────────────────────────────────────────────────────────────────────────

/**
 * When a coroutine is cancelled, CancellationException is thrown at the
 * next suspension point. Use try/finally to guarantee cleanup.
 *
 * This is the coroutine equivalent of Java's try-with-resources.
 */
suspend fun fetchWithCleanup(resourceId: String): String {
    val resource = acquireResource(resourceId)
    try {
        delay(200)  // may be cancelled here
        return processResource(resource)
    } finally {
        // This ALWAYS runs, even on cancellation
        releaseResource(resource)
        println("Resource $resourceId released (guaranteed cleanup)")
    }
}

suspend fun acquireResource(id: String): String {
    println("Acquiring resource: $id")
    return "resource-$id"
}

suspend fun processResource(resource: String): String = "processed-$resource"

suspend fun releaseResource(resource: String) {
    // withContext(NonCancellable) if this itself is a suspend call that must complete
    println("Releasing: $resource")
}

fun cleanupExamples() = runBlocking {
    println("=== Cleanup After Cancellation ===")

    val job = launch {
        try {
            fetchWithCleanup("DB-CONNECTION-1")
        } catch (e: CancellationException) {
            println("Coroutine cancelled, but cleanup ran")
            throw e  // ALWAYS re-throw CancellationException!
        }
    }

    delay(50)
    job.cancelAndJoin()  // cancel and wait for cleanup to finish
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. coroutineScope vs supervisorScope
// ─────────────────────────────────────────────────────────────────────────────

/**
 * coroutineScope: if ANY child fails, ALL others are cancelled (fail-fast)
 * supervisorScope: if a child fails, SIBLINGS continue (fail-independently)
 *
 * Use coroutineScope when:
 *   - All operations are interdependent — failure in one means others are useless
 *   - Loading all required data (if one fails, you can't proceed anyway)
 *
 * Use supervisorScope when:
 *   - Operations are independent — one failure shouldn't affect others
 *   - Background tasks (audit logging, analytics — don't fail the main flow)
 *   - Processing a batch where partial success is OK
 */

// coroutineScope: all or nothing
suspend fun loadOrderWithAllDetails(orderId: String): String {
    return coroutineScope {
        val orderD = async { fetchOrder(orderId) }
        val customerD = async { fetchCustomer("CUST-001") }

        // If either fails, the entire scope is cancelled
        val order = orderD.await()
        val customer = customerD.await()
        "Order ${order.id} for ${customer.name}"
    }
}

// supervisorScope: independent tasks
suspend fun sendNotifications(orderId: String) {
    // Children of a supervisor must handle their own failures, otherwise they
    // reach the thread's uncaught-exception handler.
    val logFailure = CoroutineExceptionHandler { _, e -> println("  Notification failed: ${e.message}") }
    supervisorScope {
        launch {
            println("  Email sent for $orderId")
        }
        launch(logFailure) {
            // This fails but doesn't cancel the sibling (email)
            throw RuntimeException("SMS gateway down!")
        }
        launch {
            println("  Push notification sent for $orderId")
        }
    }
    // Execution continues here even though SMS failed
}

fun coroutineScopeVsSupervisorExamples() = runBlocking {
    println("=== coroutineScope vs supervisorScope ===")

    // coroutineScope — successful case
    try {
        val result = loadOrderWithAllDetails("ORD-001")
        println("coroutineScope success: $result")
    } catch (e: Exception) {
        println("coroutineScope failed: ${e.message}")
    }

    // supervisorScope — one child fails, others continue
    println("\nsupervisorScope (one fails, others continue):")
    try {
        sendNotifications("ORD-001")
        println("Notifications done (some may have failed)")
    } catch (e: Exception) {
        println("Unexpected scope failure: ${e.message}")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Exception Handling
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Exception handling in coroutines:
 *
 *   launch: exceptions are stored in the Job and re-thrown when the job completes.
 *           Use try/catch INSIDE the launch block, or CoroutineExceptionHandler.
 *
 *   async: exceptions are stored in the Deferred and re-thrown on .await().
 *          Use try/catch around the .await() call.
 *
 *   IMPORTANT: CancellationException is SPECIAL — it's NOT an error, it's
 *   normal flow. Always re-throw it, never swallow it.
 */
fun exceptionHandlingExamples() = runBlocking {
    println("=== Exception Handling ===")

    // Under a plain parent, a failing child cancels the parent even if you catch
    // the await(), and a CoroutineExceptionHandler is only honoured on root
    // coroutines. supervisorScope makes its direct children behave like roots.
    supervisorScope {
        // Exception in async — caught on await()
        val deferred = async {
            delay(50)
            throw RuntimeException("Payment service unavailable")
            "This is never returned"
        }

        try {
            val result = deferred.await()
            println("Result: $result")
        } catch (e: RuntimeException) {
            println("Caught from async: ${e.message}")
        }

        // CoroutineExceptionHandler — for uncaught exceptions in launch
        val handler = CoroutineExceptionHandler { ctx, exception ->
            println("Unhandled exception in ${ctx[CoroutineName]?.name}: ${exception.message}")
        }

        val job = launch(handler + CoroutineName("background-processor")) {
            delay(50)
            throw RuntimeException("Background processing failed")
        }
        job.join()
    }

    // try/catch inside launch — preferred for known error cases
    launch {
        try {
            delay(50)
            throw RuntimeException("Expected failure")
        } catch (e: CancellationException) {
            throw e  // ALWAYS re-throw CancellationException!
        } catch (e: Exception) {
            println("Handled gracefully: ${e.message}")
        }
    }.join()
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. Real-World: Parallel Order Processing with Error Handling
// ─────────────────────────────────────────────────────────────────────────────

data class OrderProcessingOutcome(
    val orderId: String,
    val success: Boolean,
    val result: String?,
    val error: String?
)

suspend fun processOrderSafely(orderId: String): OrderProcessingOutcome {
    return try {
        withTimeout(500) {  // each order has a 500ms budget
            val order = fetchOrder(orderId)
            val txnId = processPayment(orderId, order.total)
            OrderProcessingOutcome(orderId, true, txnId, null)
        }
    } catch (e: TimeoutCancellationException) {
        OrderProcessingOutcome(orderId, false, null, "Timed out after 500ms")
    } catch (e: Exception) {
        OrderProcessingOutcome(orderId, false, null, e.message)
    }
}

fun realWorldExamples() = runBlocking {
    println("=== Real-World: Batch Order Processing ===")

    val orderIds = listOf("ORD-001", "ORD-002", "ORD-003", "ORD-004", "ORD-005")
    val startTime = System.currentTimeMillis()

    // Process all orders concurrently with supervisorScope (individual failures ok)
    val outcomes = supervisorScope {
        orderIds.map { orderId ->
            async { processOrderSafely(orderId) }
        }.awaitAll()
    }

    val elapsed = System.currentTimeMillis() - startTime
    val successful = outcomes.count { it.success }
    val failed = outcomes.count { !it.success }

    println("Processed ${outcomes.size} orders in ${elapsed}ms")
    println("Success: $successful, Failed: $failed")
    outcomes.forEach { outcome ->
        val status = if (outcome.success) "✅ ${outcome.result}" else "❌ ${outcome.error}"
        println("  ${outcome.orderId}: $status")
    }
}

fun main() {
    println("=== Kata 19: Structured Concurrency ===\n")
    jobHierarchyExamples()
    println()
    cancellationExamples()
    println()
    cleanupExamples()
    println()
    coroutineScopeVsSupervisorExamples()
    println()
    exceptionHandlingExamples()
    println()
    realWorldExamples()
}

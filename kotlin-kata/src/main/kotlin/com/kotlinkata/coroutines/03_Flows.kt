package com.kotlinkata.coroutines

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.awaitClose

/**
 * # Kata 20: Flows — Reactive Streams the Kotlin Way
 *
 * Flow<T> is Kotlin's solution for streams of values — like Java's reactive streams
 * (RxJava/Project Reactor) but designed for coroutines and far simpler.
 *
 * Java equivalents:
 *   Flow<T>         ≈  Flux<T> (Project Reactor) or Observable<T> (RxJava)
 *   StateFlow<T>    ≈  BehaviorSubject (RxJava) or current state holder
 *   SharedFlow<T>   ≈  PublishSubject (RxJava) or event bus
 *
 * Key advantage over reactive streams:
 *   - Much simpler — no marble diagrams needed
 *   - Familiar collect() instead of subscribe()
 *   - Cancellation is automatic via coroutine scope
 *   - Sequential mental model (not callback based)
 *   - Backpressure is handled automatically (coroutine suspension)
 *
 * Topics:
 *   - Cold flows (Flow<T>) — created lazily, run for each collector
 *   - Flow operators: map, filter, transform, onEach, catch, retry
 *   - Terminal operators: collect, first, toList, fold
 *   - StateFlow — hot observable state
 *   - SharedFlow — hot event stream
 *   - callbackFlow — converting callbacks to Flow
 */

// ─────────────────────────────────────────────────────────────────────────────
// 1. Basic Flow — Cold Stream
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A Flow is COLD — the producer code runs only when someone collects.
 * Each collector gets its own fresh execution (like a Sequence).
 *
 * Compare:
 *   Sequence  — synchronous, lazy, in-memory
 *   Flow      — asynchronous, lazy, can do I/O between elements
 */

// Create a flow of order IDs (simulating a paginated database stream)
fun orderIdsFlow(startId: Int, count: Int): Flow<String> = flow {
    println("[FLOW] Starting to emit order IDs")
    for (i in startId until startId + count) {
        delay(100)  // simulate DB read of each page
        val orderId = "ORD-${"$i".padStart(6, '0')}"
        println("[FLOW] Emitting: $orderId")
        emit(orderId)  // emit one value at a time
    }
    println("[FLOW] All emitted")
}

// Flow of order data (simulating real-time updates)
fun orderUpdatesFlow(): Flow<Pair<String, String>> = flow {
    val updates = listOf(
        "ORD-001" to "CONFIRMED",
        "ORD-002" to "SHIPPED",
        "ORD-001" to "DELIVERED",
        "ORD-003" to "PENDING"
    )
    updates.forEach { update ->
        delay(50)
        emit(update)
    }
}

fun basicFlowExamples() = runBlocking {
    println("=== Basic Flow ===")

    // collect() is the terminal operator — triggers the flow
    orderIdsFlow(1, 3).collect { orderId ->
        println("  Received: $orderId")
    }

    // Cold flow — each collector gets a FRESH execution
    val flow = orderIdsFlow(10, 2)
    println("\nFirst collection:")
    flow.collect { println("  First: $it") }
    println("Second collection:")
    flow.collect { println("  Second: $it") }  // runs again from scratch
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Flow Operators — Transforming Streams
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Flow operators are INTERMEDIATE — they return a new Flow, not collecting yet.
 * Like Sequence operators, they're lazy and only execute when collected.
 *
 * Common operators (familiar from collections):
 *   map, filter, flatMap, take, drop, distinct, onEach
 *
 * Flow-specific operators:
 *   transform — like flatMap but with full emit control
 *   catch     — handle exceptions upstream
 *   retry     — retry the flow on failure
 *   debounce  — emit only after a quiet period (search-as-you-type)
 *   buffer    — decouple producer and consumer speeds
 *   conflate  — drop intermediate values (keep only latest)
 */
fun flowOperatorExamples() = runBlocking {
    println("=== Flow Operators ===")

    val orderFlow = orderIdsFlow(1, 5)

    // map + filter pipeline
    orderFlow
        .filter { it.endsWith("01") || it.endsWith("03") }
        .map { orderId -> orderId to "PROCESSED" }
        .onEach { (id, status) -> println("  onEach: $id → $status") }
        .collect { println("  Collected: $it") }

    // transform — emit multiple values per input, or skip
    println("\ntransform:")
    flowOf(1, 2, 3, 4, 5)
        .transform { n ->
            if (n % 2 != 0) {
                emit("ODD-$n")
                emit("EXTRA-$n")  // emit multiple per input
            }
            // skip even numbers (don't emit anything)
        }
        .collect { println("  $it") }

    // take and drop
    println("\ntake(3):")
    orderIdsFlow(1, 10)
        .take(3)  // cancel the flow after 3 elements
        .collect { println("  $it") }

    // flatMapConcat — map each element to a flow, concatenate
    println("\nflatMapConcat (load details for each order):")
    flowOf("ORD-001", "ORD-002")
        .flatMapConcat { orderId ->
            flow {
                emit("$orderId: loading...")
                delay(50)
                emit("$orderId: done")
            }
        }
        .collect { println("  $it") }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Error Handling in Flows
// ─────────────────────────────────────────────────────────────────────────────

fun errorHandlingFlow(): Flow<String> = flow {
    emit("ORD-001")
    emit("ORD-002")
    throw RuntimeException("Database connection lost")
    @Suppress("UNREACHABLE_CODE")
    emit("ORD-003")  // never reached
}

fun flowErrorHandlingExamples() = runBlocking {
    println("=== Flow Error Handling ===")

    // catch — handle upstream exceptions
    errorHandlingFlow()
        .catch { e ->
            println("  Caught: ${e.message}")
            emit("FALLBACK-ORDER")  // can emit recovery values in catch
        }
        .collect { println("  Got: $it") }

    // retry — retry the flow on failure
    println("\nretry:")
    var attempt = 0
    flow {
        attempt++
        println("  Attempt $attempt")
        if (attempt < 3) throw RuntimeException("Transient error")
        emit("SUCCESS after $attempt attempts")
    }
        .retry(3) { e ->
            println("  Retrying after: ${e.message}")
            delay(50)
            true  // true = retry
        }
        .collect { println("  Final: $it") }

    // onCompletion — always run when flow ends (success or failure)
    println("\nonCompletion:")
    flowOf("ORD-001", "ORD-002")
        .onCompletion { cause ->
            if (cause != null) println("  Flow failed: ${cause.message}")
            else println("  Flow completed normally")
        }
        .collect { println("  $it") }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Terminal Operators — Collecting Results
// ─────────────────────────────────────────────────────────────────────────────

fun terminalOperatorExamples() = runBlocking {
    println("=== Terminal Operators ===")

    val flow = flowOf("ORD-001", "ORD-002", "ORD-003")

    // collect — iterate all elements (most basic terminal)
    flow.collect { println("  collect: $it") }

    // first / firstOrNull — get first element and CANCEL the flow
    val first = flow.first()
    val firstMatching = flow.firstOrNull { it.contains("002") }
    println("First: $first, Matching: $firstMatching")

    // toList / toSet — materialize to a collection
    val list = flow.toList()
    val set = flow.toSet()
    println("List: $list, Set: $set")

    // fold / reduce — aggregate
    val count = flow.fold(0) { acc, _ -> acc + 1 }
    val concat = flow.reduce { a, b -> "$a,$b" }
    println("Count: $count, Concat: $concat")

    // count / any / all with predicate
    val countLong = (1..100).asFlow()
        .filter { it % 2 == 0 }
        .count()
    println("Even numbers 1-100: $countLong")
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. StateFlow — Observable State
// ─────────────────────────────────────────────────────────────────────────────

/**
 * StateFlow is a HOT observable that holds a CURRENT STATE value.
 * It always has a value and replays the latest value to new collectors.
 *
 * Java equivalent: AtomicReference + listeners (verbose)
 * Android: replaces LiveData with coroutines
 * Server: represents shared mutable state in a reactive way
 *
 * Key properties:
 *   - Always has a current value (val state = stateFlow.value)
 *   - New collectors immediately get the current value
 *   - Conflates — if state changes rapidly, only the latest is emitted
 *   - Thread-safe
 */
class OrderStatusManager {
    private val _status = MutableStateFlow("PENDING")
    val status: StateFlow<String> = _status.asStateFlow()  // expose read-only

    suspend fun confirmOrder() {
        delay(100)
        _status.value = "CONFIRMED"
    }

    suspend fun shipOrder() {
        delay(100)
        _status.value = "SHIPPED"
    }

    suspend fun deliverOrder() {
        delay(100)
        _status.value = "DELIVERED"
    }
}

fun stateFlowExamples() = runBlocking {
    println("=== StateFlow ===")

    val manager = OrderStatusManager()

    // Collect state changes (in real code: in a separate coroutine/lifecycle)
    val collector = launch {
        manager.status.collect { status ->
            println("  Status changed to: $status")
        }
    }

    // Process the order through its lifecycle
    manager.confirmOrder()
    manager.shipOrder()
    manager.deliverOrder()

    delay(50)  // let the last emission propagate
    collector.cancel()

    // Current value is always accessible
    println("Current status: ${manager.status.value}")
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. SharedFlow — Event Bus / Hot Stream
// ─────────────────────────────────────────────────────────────────────────────

/**
 * SharedFlow is a HOT stream for events — like an event bus.
 * Unlike StateFlow, it doesn't have a "current value."
 * Multiple collectors receive the same emissions.
 *
 * replay parameter: how many past values new collectors receive
 * extraBufferCapacity: buffer for slow collectors
 *
 * Java equivalent: EventBus (Guava) but type-safe and non-blocking
 */
sealed interface AppEvent {
    data class OrderPlaced(val orderId: String, val total: Double) : AppEvent
    data class PaymentReceived(val orderId: String, val txnId: String) : AppEvent
    data class OrderShipped(val orderId: String, val trackingNumber: String) : AppEvent
}

class OrderEventBus {
    private val _events = MutableSharedFlow<AppEvent>(
        replay = 0,              // no replay for new subscribers
        extraBufferCapacity = 64 // buffer events if consumers are slow
    )
    val events: SharedFlow<AppEvent> = _events.asSharedFlow()

    suspend fun publish(event: AppEvent) = _events.emit(event)
}

fun sharedFlowExamples() = runBlocking {
    println("=== SharedFlow ===")

    val bus = OrderEventBus()

    // Two independent subscribers
    val auditLog = launch {
        bus.events.collect { event ->
            println("  [AUDIT] $event")
        }
    }

    val emailService = launch {
        bus.events
            .filterIsInstance<AppEvent.OrderPlaced>()
            .collect { event ->
                println("  [EMAIL] Sending confirmation for ${event.orderId}")
            }
    }

    delay(50)  // let subscribers register

    // Publish events
    bus.publish(AppEvent.OrderPlaced("ORD-001", 149.99))
    bus.publish(AppEvent.PaymentReceived("ORD-001", "TXN-123"))
    bus.publish(AppEvent.OrderShipped("ORD-001", "1Z999AA10123456"))

    delay(100)  // let events propagate
    auditLog.cancel()
    emailService.cancel()
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. callbackFlow — Wrapping Callback APIs
// ─────────────────────────────────────────────────────────────────────────────

/**
 * callbackFlow converts callback-based APIs (listeners, observers, WebSocket handlers)
 * into a Flow. This is the bridge between legacy callback code and coroutines.
 *
 * Java equivalent: Observable.create() in RxJava
 *
 * Common uses:
 *   - WebSocket message stream
 *   - Android sensor/location updates
 *   - Database change listeners
 *   - File system watchers
 */
// Simulated legacy callback API (pretend this is a 3rd party library)
interface OrderUpdateListener {
    fun onUpdate(orderId: String, status: String)
    fun onError(error: Throwable)
    fun onComplete()
}

class LegacyOrderTracker {
    private var listener: OrderUpdateListener? = null
    fun setListener(l: OrderUpdateListener) { listener = l }

    fun startTracking(orderId: String) {
        // Simulates async callbacks from an external system
        Thread {
            Thread.sleep(50);  listener?.onUpdate(orderId, "CONFIRMED")
            Thread.sleep(50);  listener?.onUpdate(orderId, "SHIPPED")
            Thread.sleep(50);  listener?.onUpdate(orderId, "DELIVERED")
            listener?.onComplete()
        }.start()
    }
}

fun trackOrderAsFlow(orderId: String): Flow<String> = callbackFlow {
    val tracker = LegacyOrderTracker()

    tracker.setListener(object : OrderUpdateListener {
        override fun onUpdate(orderId: String, status: String) {
            trySend("$orderId: $status")  // send to flow (non-suspending)
        }
        override fun onError(error: Throwable) {
            close(error)  // close flow with error
        }
        override fun onComplete() {
            close()  // close flow normally
        }
    })

    tracker.startTracking(orderId)

    awaitClose {
        // Called when the flow collector is done or cancelled
        println("  Cleanup: unregistering tracker for $orderId")
    }
}

fun callbackFlowExamples() = runBlocking {
    println("=== callbackFlow ===")

    trackOrderAsFlow("ORD-001")
        .collect { update ->
            println("  Received: $update")
        }

    println("Done tracking")
}

fun main() {
    println("=== Kata 20: Flows ===\n")
    println("--- Basic Flow ---")
    basicFlowExamples()
    println("\n--- Flow Operators ---")
    flowOperatorExamples()
    println("\n--- Error Handling ---")
    flowErrorHandlingExamples()
    println("\n--- Terminal Operators ---")
    terminalOperatorExamples()
    println("\n--- StateFlow ---")
    stateFlowExamples()
    println("\n--- SharedFlow ---")
    sharedFlowExamples()
    println("\n--- callbackFlow ---")
    callbackFlowExamples()
}

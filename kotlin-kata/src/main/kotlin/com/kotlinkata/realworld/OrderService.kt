package com.kotlinkata.realworld

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * # Kata 22: Real-World Capstone — Order Service
 *
 * A production-quality service layer demonstrating all Kotlin features working together.
 *
 * Patterns demonstrated:
 *   - Repository pattern with extension functions
 *   - Service layer with coroutines
 *   - Event sourcing with SharedFlow
 *   - DSL for order creation
 *   - Functional error propagation with OrderResult
 *   - Sequence-based reporting
 *   - Type-safe configuration with delegation
 *   - Generics with constraints
 */

// ─────────────────────────────────────────────────────────────────────────────
// Repository Layer
// ─────────────────────────────────────────────────────────────────────────────

interface OrderStore {
    suspend fun save(order: Order): Order
    suspend fun findById(id: OrderId): Order?
    suspend fun findByCustomerId(customerId: CustomerId): List<Order>
    suspend fun findAll(): List<Order>
    fun streamAll(): Flow<Order>
}

class InMemoryOrderStore : OrderStore {
    private val store = mutableMapOf<OrderId, Order>()

    override suspend fun save(order: Order): Order {
        store[order.id] = order
        return order
    }

    override suspend fun findById(id: OrderId): Order? = store[id]

    override suspend fun findByCustomerId(customerId: CustomerId): List<Order> =
        store.values.filter { it.customerId == customerId }

    override suspend fun findAll(): List<Order> = store.values.toList()

    override fun streamAll(): Flow<Order> = store.values.asFlow()
}

// Extension functions on OrderStore — adds query capabilities without subclassing
suspend fun OrderStore.findActiveOrders(): List<Order> =
    findAll().filter { !it.status.isTerminal }

suspend fun OrderStore.findByStatus(vararg statuses: OrderStatus): List<Order> =
    findAll().filter { order -> statuses.any { order.status::class == it::class } }

suspend fun OrderStore.getTotalRevenue(): Money =
    findAll()
        .filter { it.status is OrderStatus.Delivered }
        .fold(Money.ZERO) { acc, order -> acc + order.subtotal }

fun OrderStore.streamByCustomer(customerId: CustomerId): Flow<Order> =
    streamAll().filter { it.customerId == customerId }

// ─────────────────────────────────────────────────────────────────────────────
// Event Publishing
// ─────────────────────────────────────────────────────────────────────────────

sealed interface OrderEvent {
    val orderId: OrderId
    val occurredAt: java.time.Instant get() = java.time.Instant.now()

    data class OrderCreated(override val orderId: OrderId, val customerId: CustomerId, val total: Money) : OrderEvent
    data class OrderSubmitted(override val orderId: OrderId) : OrderEvent
    data class OrderConfirmed(override val orderId: OrderId, val estimatedShipDate: String) : OrderEvent
    data class OrderShipped(override val orderId: OrderId, val trackingNumber: String) : OrderEvent
    data class OrderDelivered(override val orderId: OrderId) : OrderEvent
    data class OrderCancelled(override val orderId: OrderId, val reason: String) : OrderEvent
}

class OrderEventPublisher {
    private val _events = MutableSharedFlow<OrderEvent>(
        replay = 100,
        extraBufferCapacity = 256
    )
    val events: SharedFlow<OrderEvent> = _events.asSharedFlow()

    suspend fun publish(event: OrderEvent) {
        _events.emit(event)
        println("[EVENT] ${event::class.simpleName}: ${event.orderId}")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Order Creation DSL
// ─────────────────────────────────────────────────────────────────────────────

@DslMarker annotation class OrderBuilderDsl

@OrderBuilderDsl
class OrderBuilderScope(private val customerId: CustomerId) {
    private val lineItems = mutableListOf<OrderLineItem>()
    private var shippingAddress: ShippingAddress? = null
    private var discountCode: String? = null
    var notes: String? = null

    fun shipTo(block: AddressBuilderScope.() -> Unit) {
        shippingAddress = AddressBuilderScope().apply(block).build()
    }

    fun item(productId: String, quantity: Int, price: Double, discount: Double = 0.0) {
        lineItems.add(OrderLineItem(
            productId = ProductId(productId),
            productName = productId,  // in real code: look up from catalog
            quantity = quantity,
            unitPrice = Money.of(price),
            discountAmount = Money.of(discount)
        ))
    }

    fun discount(code: String) { discountCode = code }

    fun build(): OrderResult<Order> {
        val address = shippingAddress
            ?: return OrderResult.Failure("Shipping address is required")
        return Order.create(customerId, address, lineItems, discountCode, notes)
    }
}

@OrderBuilderDsl
class AddressBuilderScope {
    var recipientName: String = ""
    var street1: String = ""
    var street2: String? = null
    var city: String = ""
    var state: String = ""
    var postalCode: String = ""
    var country: String = "US"

    fun build(): ShippingAddress = ShippingAddress(
        recipientName = recipientName,
        street1 = street1,
        street2 = street2,
        city = city,
        stateOrProvince = state,
        postalCode = postalCode,
        country = country
    )
}

fun buildOrder(customerId: String, block: OrderBuilderScope.() -> Unit): OrderResult<Order> {
    val id = CustomerId(customerId)
    return OrderBuilderScope(id).apply(block).build()
}

// ─────────────────────────────────────────────────────────────────────────────
// Order Service — Main Business Logic
// ─────────────────────────────────────────────────────────────────────────────

class OrderService(
    private val store: OrderStore,
    private val eventPublisher: OrderEventPublisher
) {
    // Create an order using the DSL builder
    suspend fun createOrder(customerId: String, block: OrderBuilderScope.() -> Unit): OrderResult<Order> {
        val result = buildOrder(customerId, block)
        return result.map { order ->
            val saved = store.save(order)
            eventPublisher.publish(OrderEvent.OrderCreated(saved.id, saved.customerId, saved.subtotal))
            saved
        }
    }

    // Submit an order
    suspend fun submitOrder(orderId: OrderId): OrderResult<Order> =
        withOrder(orderId) { order ->
            order.submit().map { submitted ->
                store.save(submitted).also {
                    eventPublisher.publish(OrderEvent.OrderSubmitted(it.id))
                }
            }
        }

    // Confirm an order
    suspend fun confirmOrder(
        orderId: OrderId,
        estimatedShipDate: String,
        confirmationRef: String
    ): OrderResult<Order> = withOrder(orderId) { order ->
        order.confirm(estimatedShipDate, confirmationRef).map { confirmed ->
            store.save(confirmed).also {
                eventPublisher.publish(OrderEvent.OrderConfirmed(it.id, estimatedShipDate))
            }
        }
    }

    // Ship an order
    suspend fun shipOrder(
        orderId: OrderId,
        trackingNumber: String,
        carrier: String,
        estimatedDelivery: String
    ): OrderResult<Order> = withOrder(orderId) { order ->
        order.startProcessing("WH-AUTO")
            .map { it.ship(trackingNumber, carrier, estimatedDelivery) }
            .map { it.getOrThrow() }
            .map { shipped ->
                store.save(shipped).also {
                    eventPublisher.publish(OrderEvent.OrderShipped(it.id, trackingNumber))
                }
            }
    }

    // Deliver an order
    suspend fun markDelivered(orderId: OrderId, signedBy: String? = null): OrderResult<Order> =
        withOrder(orderId) { order ->
            order.deliver(signedBy).map { delivered ->
                store.save(delivered).also {
                    eventPublisher.publish(OrderEvent.OrderDelivered(it.id))
                }
            }
        }

    // Cancel an order
    suspend fun cancelOrder(orderId: OrderId, reason: String, by: String): OrderResult<Order> =
        withOrder(orderId) { order ->
            order.cancel(reason, by).map { cancelled ->
                store.save(cancelled).also {
                    eventPublisher.publish(OrderEvent.OrderCancelled(it.id, reason))
                }
            }
        }

    // Get order summary
    suspend fun getOrder(orderId: OrderId): OrderResult<Order> {
        val order = store.findById(orderId) ?: return OrderResult.Failure("Order not found: $orderId")
        return OrderResult.Success(order)
    }

    // Batch processing — process multiple orders concurrently
    suspend fun processOrderBatch(orderIds: List<OrderId>): Map<OrderId, OrderResult<Order>> =
        coroutineScope {
            orderIds
                .map { orderId ->
                    orderId to async {
                        val order = store.findById(orderId)
                            ?: return@async OrderResult.Failure("Not found: $orderId")
                        // Simulate some processing work
                        delay(50)
                        OrderResult.Success(order)
                    }
                }
                .associate { (id, deferred) -> id to deferred.await() }
        }

    // Helper: fetch order and apply a function to it
    private suspend fun <T> withOrder(
        orderId: OrderId,
        block: suspend (Order) -> OrderResult<T>
    ): OrderResult<T> {
        val order = store.findById(orderId)
            ?: return OrderResult.Failure("Order not found: $orderId")
        return block(order)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Analytics / Reporting — Sequence-Based
// ─────────────────────────────────────────────────────────────────────────────

class OrderAnalytics(private val store: OrderStore) {

    // Stream revenue by month from order history (lazy sequence)
    fun revenueByMonth(): Flow<Pair<String, Money>> = store.streamAll()
        .filter { it.status is OrderStatus.Delivered }
        .map { order ->
            val month = order.createdAt.toString().substring(0, 7)  // "2024-01"
            month to order.subtotal
        }
        .let { pairsFlow ->
            flow {
                val grouped = mutableMapOf<String, Money>()
                pairsFlow.collect { (month, amount) ->
                    grouped[month] = (grouped[month] ?: Money.ZERO) + amount
                }
                grouped.entries
                    .sortedBy { it.key }
                    .forEach { (month, total) -> emit(month to total) }
            }
        }

    // Top N customers by spend
    suspend fun topCustomers(n: Int): List<Pair<CustomerId, Money>> =
        store.findAll()
            .filter { it.status is OrderStatus.Delivered }
            .groupBy { it.customerId }
            .mapValues { (_, orders) -> orders.fold(Money.ZERO) { acc, o -> acc + o.subtotal } }
            .entries
            .sortedByDescending { it.value.cents }
            .take(n)
            .map { it.key to it.value }
}

// ─────────────────────────────────────────────────────────────────────────────
// Demo: Everything Working Together
// ─────────────────────────────────────────────────────────────────────────────

fun main() = runBlocking {
    println("=== Kata 22: Order Service Capstone ===\n")

    val store = InMemoryOrderStore()
    val events = OrderEventPublisher()
    val service = OrderService(store, events)

    // Subscribe to events
    val eventLog = launch {
        events.events.collect { event ->
            println("  [EVENT LOG] ${event::class.simpleName}")
        }
    }

    // Create orders using the DSL
    println("--- Creating orders with DSL ---")
    val order1Result = service.createOrder("CUST-A1B2C3") {
        shipTo {
            recipientName = "Jane Smith"
            street1 = "123 Main St"
            city = "New York"
            state = "NY"
            postalCode = "10001"
        }
        item("KBD-01", 1, 79.99)
        item("MSE-01", 2, 29.99, discount = 5.0)  // $5 off each
        discount("JAVA-DEV-20")
        notes = "Please leave at door"
    }

    val order2Result = service.createOrder("CUST-B4E5F6") {
        shipTo {
            recipientName = "Bob Johnson"
            street1 = "456 Oak Ave"
            city = "San Francisco"
            state = "CA"
            postalCode = "94102"
        }
        item("LPT-01", 1, 1299.99)
    }

    // Process orders through lifecycle
    println("\n--- Processing order 1 ---")
    order1Result.onSuccess { order ->
        service.submitOrder(order.id)
            .onSuccess { submitted ->
                service.confirmOrder(submitted.id, "2024-02-15", "CONF-001")
                    .onSuccess { confirmed ->
                        service.shipOrder(confirmed.id, "1Z999AA10123456784", "UPS", "2024-02-16")
                            .onSuccess { shipped ->
                                service.markDelivered(shipped.id, "Jane Smith")
                                    .onSuccess { delivered ->
                                        println("Order ${delivered.id} lifecycle complete: ${delivered.status.displayName}")
                                        println("  Subtotal: ${delivered.subtotal}")
                                        println("  Savings: ${delivered.totalSavings}")
                                    }
                            }
                    }
            }
    }

    // Cancel order 2
    println("\n--- Cancelling order 2 ---")
    order2Result.onSuccess { order ->
        service.submitOrder(order.id)
            .onSuccess { submitted ->
                service.cancelOrder(submitted.id, "Out of stock", "SYSTEM")
                    .onSuccess { cancelled -> println("Order cancelled: ${cancelled.status.displayName}") }
            }
    }

    // Batch processing
    println("\n--- Batch processing ---")
    val allOrders = store.findAll().map { it.id }
    val batchResults = service.processOrderBatch(allOrders)
    println("Batch processed ${batchResults.size} orders")
    batchResults.forEach { (id, result) ->
        println("  $id: ${if (result.isSuccess) "✅" else "❌ ${(result as OrderResult.Failure).reason}"}")
    }

    // Analytics
    println("\n--- Analytics ---")
    println("Total revenue: ${store.getTotalRevenue()}")
    println("Active orders: ${store.findActiveOrders().size}")

    delay(100)  // let events propagate
    eventLog.cancel()
}

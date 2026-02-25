package com.kotlinkata.realworld

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * # Exercises: Real-World Order Service
 *
 * These exercises tie together all the Kotlin features you've learned.
 * You'll extend the existing domain model and service layer.
 *
 * Replace each TODO() with your implementation.
 */
class OrderServiceExercises {

    private lateinit var store: InMemoryOrderStore
    private lateinit var events: OrderEventPublisher
    private lateinit var service: OrderService

    @BeforeEach
    fun setup() {
        store = InMemoryOrderStore()
        events = OrderEventPublisher()
        service = OrderService(store, events)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Exercise 1: Domain Model
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `Money arithmetic works correctly`() {
        val price = Money.of(79.99)
        val tax = Money.of(6.40)
        val discount = Money.of(10.00)

        // TODO: Calculate total as price + tax
        val total: Money = TODO()

        // TODO: Calculate discounted total as price - discount + tax
        val discountedTotal: Money = TODO()

        assertThat(total.dollars).isCloseTo(86.39, org.assertj.core.data.Offset.offset(0.01))
        assertThat(discountedTotal.dollars).isCloseTo(76.39, org.assertj.core.data.Offset.offset(0.01))
    }

    @Test
    fun `Order.create validates input`() {
        val address = SampleData.standardAddress

        // TODO: Test that Order.create fails when items list is empty
        val emptyItemsResult = Order.create(
            customerId = CustomerId("CUST-A1B2C3"),
            shippingAddress = address,
            lineItems = TODO()  // empty list
        )
        assertThat(emptyItemsResult.isFailure).isTrue()

        // TODO: Test that Order.create succeeds with valid data
        val validResult = Order.create(
            customerId = CustomerId("CUST-A1B2C3"),
            shippingAddress = address,
            lineItems = listOf(TODO())  // create a valid OrderLineItem
        )
        assertThat(validResult.isSuccess).isTrue()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Exercise 2: State Machine
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `order follows the expected lifecycle`() = runBlocking {
        // Create an order using the DSL
        val createResult = service.createOrder("CUST-A1B2C3") {
            shipTo {
                recipientName = "Test User"
                street1 = "123 Test St"
                city = "Test City"
                state = "TC"
                postalCode = "12345"
            }
            item("KBD-01", 1, 79.99)
        }

        assertThat(createResult.isSuccess).isTrue()
        val order = createResult.getOrThrow()

        // TODO: Submit the order and verify status
        val submitted = service.submitOrder(order.id).getOrThrow()
        assertThat(submitted.status).isInstanceOf(TODO<Class<*>>())

        // TODO: Confirm the order
        val confirmed = service.confirmOrder(submitted.id, "2024-02-15", "CONF-001").getOrThrow()
        assertThat(confirmed.status).isInstanceOf(TODO<Class<*>>())

        // TODO: Ship the order
        val shipped = service.shipOrder(
            confirmed.id,
            "1Z999AA10123456784",
            "UPS",
            "2024-02-16"
        ).getOrThrow()
        assertThat(shipped.status).isInstanceOf(TODO<Class<*>>())

        // Verify the shipped status has tracking info
        val shippedStatus = shipped.status as OrderStatus.Shipped
        assertThat(shippedStatus.trackingNumber).isEqualTo("1Z999AA10123456784")
    }

    @Test
    fun `cannot cancel a shipped order`() = runBlocking {
        val order = SampleData.createSampleOrder()

        // Put order in shipped state
        val shipped = order
            .submit().getOrThrow()
            .confirm("2024-02-15", "CONF-001").getOrThrow()
            .startProcessing("WH-001").getOrThrow()
            .ship("TRK-001", "UPS", "2024-02-16").getOrThrow()

        // TODO: Try to cancel the shipped order — this should fail
        val cancelResult: OrderResult<Order> = TODO()

        assertThat(cancelResult.isFailure).isTrue()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Exercise 3: Repository Extensions
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `find active orders using extension function`() = runBlocking {
        // Create and store some orders
        val order1 = SampleData.createSampleOrder()
        val order2 = SampleData.createSampleOrder()
        val order3 = SampleData.createSampleOrder()

        store.save(order1)
        store.save(order2.submit().getOrThrow())
        store.save(order3.submit().getOrThrow()
            .confirm("2024-02-15", "CONF-001").getOrThrow()
            .startProcessing("WH-001").getOrThrow()
            .ship("TRK-001", "UPS", "2024-02-16").getOrThrow()
            .deliver("Jane Smith").getOrThrow()  // terminal state
        )

        // TODO: Use the findActiveOrders() extension function
        val activeOrders: List<Order> = TODO()

        assertThat(activeOrders).hasSize(2)  // order1 (Draft) and order2 (Submitted)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Exercise 4: Using the OrderResult type
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `chain OrderResult operations with map`() {
        val order = SampleData.createSampleOrder()

        // TODO: Chain these operations using map:
        //   1. submit() the order
        //   2. confirm() with date "2024-02-15" and ref "CONF-001"
        //   3. Extract the confirmation ref from the status
        val confirmationRef: OrderResult<String> = order
            .submit()
            .map { TODO() }   // confirm step
            .map { confirmed ->
                val status = confirmed.status as? OrderStatus.Confirmed
                    ?: error("Expected Confirmed status")
                TODO() // extract the confirmationRef from status
            }

        assertThat(confirmationRef.isSuccess).isTrue()
        assertThat(confirmationRef.getOrNull()).isEqualTo("CONF-001")
    }

    @Test
    fun `onSuccess and onFailure callbacks work correctly`() {
        val order = SampleData.createSampleOrder()
        val successLog = mutableListOf<String>()
        val failureLog = mutableListOf<String>()

        // TODO: Submit the order and use onSuccess/onFailure to populate the logs
        order.submit()
            .onSuccess { TODO() }   // add order.id.value to successLog
            .onFailure { TODO() }   // add reason to failureLog

        assertThat(successLog).hasSize(1)
        assertThat(failureLog).isEmpty()

        // Now try an invalid transition
        val alreadyDelivered = order
            .submit().getOrThrow()
            .confirm("2024-02-15", "CONF-001").getOrThrow()
            .startProcessing("WH-001").getOrThrow()
            .ship("TRK-001", "UPS", "2024-02-16").getOrThrow()
            .deliver("Jane Smith").getOrThrow()

        alreadyDelivered.submit()  // can't submit a delivered order
            .onSuccess { failureLog.add("Should not succeed") }
            .onFailure { reason -> failureLog.add(reason) }

        assertThat(failureLog).hasSize(1)
    }
}

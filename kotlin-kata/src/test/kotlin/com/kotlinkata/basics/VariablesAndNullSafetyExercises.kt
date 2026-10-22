package com.kotlinkata.basics

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

/**
 * # Exercises: Variables and Null Safety
 *
 * Replace each TODO() with your implementation.
 * Run: ./gradlew test --tests "com.kotlinkata.basics.VariablesAndNullSafetyExercises"
 */
class VariablesAndNullSafetyExercises {

    // ─────────────────────────────────────────────────────────────────────────
    // Exercise 1: val vs var
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `count orders using a mutable variable`() {
        val orderIds = listOf("ORD-001", "ORD-002", "ORD-003", "ORD-004", "ORD-005")

        // TODO: Count how many orders start with "ORD-00"
        // HINT: Use a var and iterate with forEach
        val count: Int = TODO()

        assertThat(count).isEqualTo(5)
    }

    @Test
    fun `build an immutable list of discounted prices`() {
        val originalPrices = listOf(100.0, 50.0, 75.0, 200.0)
        val discountRate = 0.20

        // TODO: Create a val holding the discounted prices (20% off each)
        // HINT: Use map with { price -> price * (1 - discountRate) }
        val discounted: List<Double> = TODO()

        assertThat(discounted).containsExactly(80.0, 40.0, 60.0, 160.0)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Exercise 2: Nullable Types and Safe Calls
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `extract city from nullable address chain`() {
        data class Address(val city: String?, val country: String)
        data class User(val name: String, val address: Address?)

        val user1 = User("Alice", Address("New York", "US"))
        val user2 = User("Bob", null)
        val user3 = User("Carol", Address(null, "UK"))

        // TODO: Extract the city from each user using safe call ?.
        // Should return null if user has no address or address has no city
        fun getCity(user: User): String? = TODO()

        assertThat(getCity(user1)).isEqualTo("New York")
        assertThat(getCity(user2)).isNull()
        assertThat(getCity(user3)).isNull()
    }

    @Test
    fun `provide default shipping cost using elvis operator`() {
        data class Order(val id: String, val shippingCost: Double?)

        val orders = listOf(
            Order("ORD-001", 9.99),
            Order("ORD-002", null),      // free shipping
            Order("ORD-003", 14.99),
            Order("ORD-004", null)       // free shipping
        )

        // TODO: For each order, get shipping cost or 0.0 if null
        // HINT: Use map { it.shippingCost ?: 0.0 }
        val costs: List<Double> = TODO()

        assertThat(costs).containsExactly(9.99, 0.0, 14.99, 0.0)
    }

    @Test
    fun `execute block only when coupon code is present`() {
        val validCoupon: String? = "SAVE20"
        val noCoupon: String? = null

        val appliedDiscounts = mutableListOf<String>()

        // TODO: Apply discount only if couponCode is not null
        // HINT: use ?.let { code -> appliedDiscounts.add(code) }
        fun applyDiscount(couponCode: String?) {
            TODO()
        }

        applyDiscount(validCoupon)
        applyDiscount(noCoupon)

        assertThat(appliedDiscounts).containsExactly("SAVE20")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Exercise 3: Smart Casts
    // ─────────────────────────────────────────────────────────────────────────

    sealed interface Event
    data class OrderEvent(val orderId: String) : Event
    data class PaymentEvent(val amount: Double) : Event
    data class ErrorEvent(val message: String) : Event

    @Test
    fun `process different event types with smart casts`() {
        // TODO: Implement a function that returns a description of each event
        // Use when with smart casts — no explicit casts needed!
        fun describe(event: Event): String = TODO()

        assertThat(describe(OrderEvent("ORD-001"))).contains("ORD-001")
        assertThat(describe(PaymentEvent(99.99))).contains("99.99")
        assertThat(describe(ErrorEvent("Connection failed"))).contains("Connection failed")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Exercise 4: Null Safety in Practice
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `parse nullable user input to safe domain type`() {
        // TODO: Implement parseCustomerId that:
        //   - Returns null if input is null or blank
        //   - Trims the input
        //   - Returns uppercase version prefixed with "CUST-" if not already
        //   - Returns null if result is shorter than 8 chars
        fun parseCustomerId(input: String?): String? = TODO()

        assertThat(parseCustomerId(null)).isNull()
        assertThat(parseCustomerId("  ")).isNull()
        assertThat(parseCustomerId("  abc  ")).isNull()       // too short after trim + prefix
        assertThat(parseCustomerId("  A1B2C3  ")).isEqualTo("CUST-A1B2C3")
        assertThat(parseCustomerId("CUST-A1B2C3")).isEqualTo("CUST-A1B2C3")
    }

    @Test
    fun `aggregate prices handling nulls gracefully`() {
        // Prices from an API that may have null entries
        val prices: List<Double?> = listOf(9.99, null, 19.99, null, 29.99)

        // TODO: Sum only the non-null prices
        // HINT: filterNotNull() followed by sum()
        val total: Double = TODO()

        assertThat(total).isEqualTo(59.97)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Exercise 5: Type Conversions
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `convert string prices to Long cents`() {
        val priceStrings = listOf("9.99", "19.99", "invalid", "29.99", "bad")

        // TODO: Parse each string to Long cents (multiply by 100, round to long)
        // Skip strings that can't be parsed (return null for those, then filterNotNull)
        // HINT: use toDoubleOrNull()?.let { Math.round(it * 100) }
        val cents: List<Long> = TODO()

        assertThat(cents).containsExactly(999L, 1999L, 2999L)
    }
}

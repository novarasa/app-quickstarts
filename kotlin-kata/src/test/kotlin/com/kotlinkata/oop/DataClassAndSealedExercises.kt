package com.kotlinkata.oop

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * # Exercises: Data Classes and Sealed Classes
 *
 * Replace each TODO() with your implementation.
 */
class DataClassAndSealedExercises {

    // ─────────────────────────────────────────────────────────────────────────
    // Exercise 1: Data Classes
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `data class structural equality`() {
        data class Product(val sku: String, val price: Double, val inStock: Boolean)

        val p1 = Product("SKU-1", 9.99, true)
        val p2 = Product("SKU-1", 9.99, true)
        val p3 = Product("SKU-2", 9.99, true)

        // TODO: Fill in the expected values
        assertThat(p1 == p2).isEqualTo(TODO("true or false?"))   // same values
        assertThat(p1 == p3).isEqualTo(TODO("true or false?"))   // different sku
        assertThat(p1 === p2).isEqualTo(TODO("true or false?"))  // reference equality
    }

    @Test
    fun `copy with selective changes`() {
        data class OrderItem(
            val sku: String,
            val quantity: Int,
            val unitPrice: Double,
            val discountPct: Double = 0.0
        )

        val original = OrderItem("SKU-001", 2, 49.99)

        // TODO: Create a discounted copy using copy()
        val discounted: OrderItem = TODO()

        assertThat(discounted.sku).isEqualTo("SKU-001")        // unchanged
        assertThat(discounted.quantity).isEqualTo(2)            // unchanged
        assertThat(discounted.unitPrice).isEqualTo(49.99)       // unchanged
        assertThat(discounted.discountPct).isEqualTo(0.10)      // changed

        // TODO: Create an updated copy with more quantity
        val bulkOrder: OrderItem = TODO()
        assertThat(bulkOrder.quantity).isEqualTo(10)
        assertThat(bulkOrder.discountPct).isEqualTo(0.0)
    }

    @Test
    fun `destructuring data class`() {
        data class PriceBreakdown(val subtotal: Double, val tax: Double, val total: Double)

        fun calculatePrice(price: Double, qty: Int, taxRate: Double): PriceBreakdown {
            val subtotal = price * qty
            val tax = subtotal * taxRate
            return PriceBreakdown(subtotal, tax, subtotal + tax)
        }

        // TODO: Destructure the result and use the components
        val (subtotal, tax, total) = calculatePrice(19.99, 3, 0.08)

        assertThat(subtotal).isEqualTo(19.99 * 3)
        assertThat(tax).isCloseTo(19.99 * 3 * 0.08, org.assertj.core.data.Offset.offset(0.001))
        assertThat(total).isCloseTo(subtotal + tax, org.assertj.core.data.Offset.offset(0.001))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Exercise 2: Sealed Classes
    // ─────────────────────────────────────────────────────────────────────────

    sealed class PaymentResult {
        data class Approved(val transactionId: String, val amount: Double) : PaymentResult()
        data class Declined(val code: Int, val reason: String) : PaymentResult()
        data class Pending(val referenceId: String) : PaymentResult()
        object Cancelled : PaymentResult()
    }

    @Test
    fun `handle all states of a sealed class`() {
        // TODO: Implement describe() to return a human-readable string for each state
        // Use a when expression — it MUST be exhaustive (no else needed!)
        fun describe(result: PaymentResult): String = TODO()

        assertThat(describe(PaymentResult.Approved("TXN-001", 99.99))).contains("TXN-001")
        assertThat(describe(PaymentResult.Declined(4001, "Insufficient funds"))).contains("4001")
        assertThat(describe(PaymentResult.Pending("REF-001"))).contains("REF-001")
        assertThat(describe(PaymentResult.Cancelled)).containsIgnoringCase("cancel")
    }

    sealed class ParseResult<out T> {
        data class Success<T>(val value: T) : ParseResult<T>()
        data class Error(val message: String) : ParseResult<Nothing>()
    }

    @Test
    fun `use sealed class for error handling instead of exceptions`() {
        // TODO: Implement parseOrderId that returns ParseResult.Success or ParseResult.Error
        // Valid order IDs match pattern: ORD-[0-9]{6}
        fun parseOrderId(input: String): ParseResult<String> = TODO()

        // Test success case
        when (val result = parseOrderId("ORD-123456")) {
            is ParseResult.Success -> assertThat(result.value).isEqualTo("ORD-123456")
            is ParseResult.Error   -> org.junit.jupiter.api.Assertions.fail("Expected success")
        }

        // Test failure case
        when (val result = parseOrderId("INVALID")) {
            is ParseResult.Success -> org.junit.jupiter.api.Assertions.fail("Expected error")
            is ParseResult.Error   -> assertThat(result.message).isNotBlank()
        }
    }

    sealed class TrafficLight {
        object Red : TrafficLight()
        object Yellow : TrafficLight()
        object Green : TrafficLight()
    }

    @Test
    fun `implement state machine with sealed classes`() {
        // TODO: Implement nextState() — Red → Green → Yellow → Red
        fun nextState(current: TrafficLight): TrafficLight = TODO()

        assertThat(nextState(TrafficLight.Red)).isInstanceOf(TrafficLight.Green::class.java)
        assertThat(nextState(TrafficLight.Green)).isInstanceOf(TrafficLight.Yellow::class.java)
        assertThat(nextState(TrafficLight.Yellow)).isInstanceOf(TrafficLight.Red::class.java)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Exercise 3: Companion Objects
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `use companion object for factory method`() {
        // TODO: Add a companion object with a factory method to this class
        // The factory method create(email: String, name: String) should:
        //   - Validate email contains @ and .
        //   - Generate a random ID in the format "USER-XXXX" (4 hex chars)
        //   - Return the User if valid, or null if email is invalid
        class User private constructor(
            val id: String,
            val email: String,
            val name: String
        ) {
            // TODO: Add companion object here
        }

        // val user = User.create("john@example.com", "John Doe")
        // assertThat(user).isNotNull()
        // assertThat(user?.email).isEqualTo("john@example.com")
        // assertThat(user?.id).startsWith("USER-")

        // val invalid = User.create("not-an-email", "Bad User")
        // assertThat(invalid).isNull()

        // For now, just test the concept with a simpler approach:
        val email = "john@example.com"
        val isValid = email.contains("@") && email.contains(".")
        assertThat(isValid).isTrue()
    }
}

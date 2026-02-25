package com.kotlinkata.functional

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * # Exercises: Extension Functions and Scope Functions
 *
 * Replace each TODO() with your implementation.
 */
class ExtensionFunctionsExercises {

    // ─────────────────────────────────────────────────────────────────────────
    // Exercise 1: Writing Extension Functions
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `String extension to validate order ID format`() {
        // TODO: Write an extension function on String called isValidOrderId()
        // that returns true if the string matches ORD-[0-9]{6,}
        fun String.isValidOrderId(): Boolean = TODO()

        assertThat("ORD-123456".isValidOrderId()).isTrue()
        assertThat("ORD-00000001".isValidOrderId()).isTrue()
        assertThat("ORD-123".isValidOrderId()).isFalse()    // too short
        assertThat("CUST-001".isValidOrderId()).isFalse()   // wrong prefix
        assertThat("".isValidOrderId()).isFalse()
    }

    @Test
    fun `Double extension to convert to formatted currency string`() {
        // TODO: Write extension fun Double.toCurrencyString(currency: String = "USD"): String
        // that formats the double as e.g. "$9.99 USD"
        fun Double.toCurrencyString(currency: String = "USD"): String = TODO()

        assertThat(9.99.toCurrencyString()).isEqualTo("$9.99 USD")
        assertThat(1299.0.toCurrencyString("GBP")).isEqualTo("$1299.00 GBP")
        assertThat(0.5.toCurrencyString()).isEqualTo("$0.50 USD")
    }

    @Test
    fun `List extension to paginate results`() {
        // TODO: Write extension fun <T> List<T>.page(pageNumber: Int, pageSize: Int): List<T>
        // pageNumber is 1-based
        // Returns empty list if pageNumber is beyond the list
        fun <T> List<T>.page(pageNumber: Int, pageSize: Int): List<T> = TODO()

        val items = (1..25).toList()
        assertThat(items.page(1, 10)).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        assertThat(items.page(2, 10)).containsExactly(11, 12, 13, 14, 15, 16, 17, 18, 19, 20)
        assertThat(items.page(3, 10)).containsExactly(21, 22, 23, 24, 25)
        assertThat(items.page(4, 10)).isEmpty()
    }

    @Test
    fun `extension on nullable type to provide default`() {
        // TODO: Write extension fun String?.orUnknown(): String
        // Returns the string if not null/blank, otherwise "Unknown"
        fun String?.orUnknown(): String = TODO()

        assertThat("John".orUnknown()).isEqualTo("John")
        assertThat("  ".orUnknown()).isEqualTo("Unknown")
        val nullStr: String? = null
        assertThat(nullStr.orUnknown()).isEqualTo("Unknown")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Exercise 2: Scope Functions
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `use let for null-safe transformation`() {
        val rawInput: String? = "  SAVE20  "
        val noInput: String? = null

        // TODO: Use let to trim and uppercase rawInput; return null if input is null
        // Result should be String? (null when input is null)
        fun processCode(input: String?): String? = TODO()

        assertThat(processCode(rawInput)).isEqualTo("SAVE20")
        assertThat(processCode(noInput)).isNull()
    }

    @Test
    fun `use apply to configure a mutable object`() {
        data class EmailConfig(
            var from: String = "",
            var subject: String = "",
            var body: String = "",
            var isHtml: Boolean = false
        )

        // TODO: Use apply to configure an EmailConfig and return it
        // Set: from = "orders@shop.com", subject = "Order Confirmation", isHtml = true
        val config: EmailConfig = TODO()

        assertThat(config.from).isEqualTo("orders@shop.com")
        assertThat(config.subject).isEqualTo("Order Confirmation")
        assertThat(config.isHtml).isTrue()
    }

    @Test
    fun `use also for logging side effects`() {
        val log = mutableListOf<String>()

        // TODO: Process the order but also log each step without changing the type
        // Chain: raw string → trim → uppercase → also { log.add("Processed: $it") } → return result
        fun processOrderId(raw: String): String = TODO()

        val result = processOrderId("  ord-001  ")
        assertThat(result).isEqualTo("ORD-001")
        assertThat(log).contains("Processed: ORD-001")
    }

    @Test
    fun `use run to execute block and return result`() {
        data class Cart(val items: List<String>, val discountCode: String?)

        val cart = Cart(listOf("SKU-1", "SKU-2", "SKU-3"), "SAVE10")

        // TODO: Use run on cart to compute and return a summary string
        // Format: "Cart with N items [and discount CODE]"
        // Only include discount part if discountCode is not null
        val summary: String = cart.run {
            TODO()
        }

        assertThat(summary).isEqualTo("Cart with 3 items and discount SAVE10")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Exercise 3: Combining Extensions and Scope Functions
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `build order summary using scope functions and extensions`() {
        data class OrderItem(val sku: String, val qty: Int, val price: Double)
        data class Order(val id: String, val items: List<OrderItem>, val status: String)

        fun Order.total(): Double = items.sumOf { it.qty * it.price }
        fun Double.formatted(): String = "${"%.2f".format(this)} USD"

        val order = Order("ORD-001", listOf(
            OrderItem("SKU-1", 2, 19.99),
            OrderItem("SKU-2", 1, 49.99)
        ), "PENDING")

        // TODO: Use with(order) { } to build the summary string:
        // "Order ORD-001: 3 items, total $89.97 USD [PENDING]"
        val summary: String = with(order) {
            TODO()
        }

        assertThat(summary).isEqualTo("Order ORD-001: 3 items, total \$89.97 USD [PENDING]")
    }
}

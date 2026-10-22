package com.kotlinkata.basics

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * # Kata 05: String Templates, Multiline Strings, and Ranges
 *
 * Kotlin's string handling is cleaner and more powerful than Java's.
 *
 * Topics:
 *   - String templates (vs String.format, StringBuilder, concatenation)
 *   - Multiline strings with trimIndent/trimMargin
 *   - String operations and extensions
 *   - Ranges and progressions (IntRange, CharRange, etc.)
 *   - until, downTo, step
 *   - Checking membership with `in`
 */

// ─────────────────────────────────────────────────────────────────────────────
// 1. String Templates
// ─────────────────────────────────────────────────────────────────────────────

/**
 * String templates embed expressions directly in string literals using $ or ${}.
 *
 * Java:
 *   String.format("Order %s has %d items totaling $%.2f", id, count, total)
 *   "Order " + id + " has " + count + " items"
 *
 * Kotlin:
 *   "Order $id has $count items totaling $${"%.2f".format(total)}"
 *
 * Rules:
 *   - $variable for simple property/variable access
 *   - ${expression} for any expression (method calls, operations, etc.)
 *   - Escape literal $ with \$
 */
data class OrderSummary(
    val id: String,
    val customerId: String,
    val itemCount: Int,
    val total: Double,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

fun stringTemplateExamples() {
    val order = OrderSummary("ORD-001", "CUST-001", 3, 149.97)

    // Simple variable interpolation
    println("Order: ${order.id}")

    // Expression interpolation — any Kotlin expression works
    println("Total with tax: ${"%.2f".format(order.total * 1.08)}")

    // Method calls
    println("Customer: ${order.customerId.uppercase()}")

    // Conditional expression
    val label = "Status: ${if (order.total > 100) "Premium" else "Standard"}"
    println(label)

    // Accessing nested properties
    val date = order.createdAt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    println("Created: $date")

    // Escaping dollar sign (e.g., for currency display)
    val price = 19.99
    println("Price: \$$price")        // \$ is a literal $
    println("Price: ${"$"}$price")    // alternative: embed $ in template

    // Complex template for an order receipt
    val receipt = """
        Order ${order.id}
        Customer: ${order.customerId}
        Items: ${order.itemCount}
        Total: $${"%.2f".format(order.total)}
    """.trimIndent()
    println(receipt)
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Multiline Strings
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Triple-quoted strings: """..."""
 *   - Can span multiple lines
 *   - No escaping needed (backslashes are literal)
 *   - String templates still work inside them
 *
 * Java equivalent: messy concatenation or StringJoiner or text blocks (Java 15+)
 *
 * trimIndent() removes the common indentation from all lines.
 * trimMargin() uses a custom margin character (default |) to strip leading whitespace.
 */
fun generateOrderEmailTemplate(order: OrderSummary): String = """
    Subject: Your Order ${order.id} has been confirmed

    Dear Customer ${order.customerId},

    Thank you for your order! Here are the details:

    Order ID:   ${order.id}
    Items:      ${order.itemCount}
    Total:      $${"%.2f".format(order.total)}
    Created:    ${order.createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}

    We'll notify you when your order ships.

    Best regards,
    The Team
""".trimIndent()

fun generateJsonPayload(order: OrderSummary): String = """
    {
        "orderId": "${order.id}",
        "customerId": "${order.customerId}",
        "itemCount": ${order.itemCount},
        "total": ${order.total},
        "currency": "USD"
    }
""".trimIndent()

// trimMargin — useful inside code where indentation is important
fun generateSqlQuery(customerId: String): String = """
    |SELECT o.id, o.total, o.created_at
    |FROM orders o
    |JOIN customers c ON c.id = o.customer_id
    |WHERE c.external_id = '$customerId'
    |  AND o.status NOT IN ('CANCELLED', 'REFUNDED')
    |ORDER BY o.created_at DESC
    |LIMIT 10
""".trimMargin()

// ─────────────────────────────────────────────────────────────────────────────
// 3. String Operations (Extension Functions)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Kotlin adds many useful extension functions to String.
 * Many are familiar from Apache Commons StringUtils — now built-in.
 */
fun stringOperations() {
    val orderId = "  ORD-12345  "

    // Trimming and case
    println(orderId.trim())               // "ORD-12345"
    println("sku-abc".uppercase())        // "SKU-ABC"
    println("CATEGORY".lowercase())       // "category"

    // Checking contents
    println("ORD-001".startsWith("ORD"))  // true
    println("CUST-001".endsWith("001"))   // true
    println("ORD-12345".contains("123"))  // true

    // Padding (useful for formatting reports)
    println("ORD-1".padStart(10, '0'))    // "00000ORD-1"
    println("PAID".padEnd(10, '-'))       // "PAID------"

    // Splitting and joining
    val csvLine = "ORD-001,CUST-1,149.97,SHIPPED"
    val fields = csvLine.split(",")       // List<String>
    println(fields)

    val rejoined = fields.joinToString(" | ")  // "ORD-001 | CUST-1 | ..."
    println(rejoined)

    // takeIf / takeUnless — conditional selection
    val rawInput = "  "
    val cleanInput: String? = rawInput.trim().takeIf { it.isNotEmpty() }
    println("Clean input: $cleanInput")   // null (empty after trim)

    // Checking nullability and emptiness
    val emptyStr: String? = null
    println(emptyStr.isNullOrBlank())     // true
    println("  ".isBlank())              // true
    println("x".isNotBlank())            // true

    // Convert to number safely
    val maybeInt = "123".toIntOrNull()    // 123
    val badInt = "abc".toIntOrNull()      // null (not NumberFormatException!)
    println("Parsed: $maybeInt, bad: $badInt")

    // Regex
    val trackingPattern = Regex("[A-Z0-9]{20,22}")
    println("1Z999AA10123456784".matches(trackingPattern))   // true (UPS-like)
    val withLetters = "Order ID: ORD-001 from CUST-001".replace(Regex("\\d+"), "###")
    println(withLetters)
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Ranges and Progressions
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Ranges are first-class in Kotlin.
 *   1..10       — IntRange from 1 to 10 (inclusive)
 *   1 until 10  — IntRange from 1 to 9 (exclusive end)
 *   10 downTo 1 — IntRange from 10 to 1 (descending)
 *   1..10 step 2 — 1, 3, 5, 7, 9
 *   'a'..'z'    — CharRange
 *   "a".."z"    — NOT a range — use Comparable range
 *
 * Ranges support: iteration, membership check (`in`), contains()
 */
fun rangeExamples() {
    // Iterating with ranges
    print("Countdown: ")
    for (i in 10 downTo 1 step 2) print("$i ")
    println()

    // Char range
    print("A-E: ")
    for (c in 'A'..'E') print(c)
    println()

    // Membership check with `in` — very readable
    val orderCount = 7
    val isBulkOrder = orderCount in 5..99
    println("Is bulk order ($orderCount items): $isBulkOrder")

    // Ranges with doubles — use until-style manually
    val price = 49.99
    val tier = when (price) {
        in 0.0..24.99   -> "Basic"
        in 25.0..74.99  -> "Standard"
        in 75.0..149.99 -> "Premium"
        else            -> "Enterprise"
    }
    println("$price → $tier tier")

    // Building a sequence of order IDs
    val orderIds = (1..5).map { "ORD-%06d".format(it) }
    println("Order IDs: $orderIds")

    // Filter with range
    val quantities = listOf(0, 1, 5, 10, 20, 100, 0, 3)
    val validQuantities = quantities.filter { it in 1..50 }
    println("Valid quantities: $validQuantities")

    // Check if a value is NOT in a range
    val invalidQuantity = 0
    if (invalidQuantity !in 1..999) {
        println("Quantity $invalidQuantity is out of valid range 1..999")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Real-World: Order Report Generation
// ─────────────────────────────────────────────────────────────────────────────

data class LineItem(val sku: String, val name: String, val qty: Int, val unitPrice: Double)

fun generateOrderReport(orderId: String, customer: String, items: List<LineItem>): String {
    val separator = "─".repeat(60)
    val subtotal = items.sumOf { it.qty * it.unitPrice }
    val tax = subtotal * 0.08
    val total = subtotal + tax

    val itemLines = items.joinToString("\n") { item ->
        val lineTotal = item.qty * item.unitPrice
        "  ${item.name.padEnd(30)} ${item.qty.toString().padStart(3)} x \$${
            "%.2f".format(item.unitPrice).padStart(8)} = \$${
            "%.2f".format(lineTotal).padStart(9)}"
    }

    return """
        $separator
        ORDER RECEIPT
        $separator
        Order ID:  $orderId
        Customer:  $customer
        Date:      ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}
        $separator
        $itemLines
        $separator
        Subtotal:${" ".repeat(44)}\$${
        "%.2f".format(subtotal).padStart(9)}
        Tax (8%): ${" ".repeat(43)}\$${
        "%.2f".format(tax).padStart(9)}
        TOTAL:   ${" ".repeat(44)}\$${
        "%.2f".format(total).padStart(9)}
        $separator
    """.trimIndent()
}

fun main() {
    println("=== Kata 05: Strings, Templates & Ranges ===\n")

    stringTemplateExamples()
    println()

    val order = OrderSummary("ORD-001", "CUST-001", 3, 149.97)
    println(generateOrderEmailTemplate(order))
    println()
    println(generateJsonPayload(order))
    println()
    println("SQL Query:")
    println(generateSqlQuery("CUST-001"))
    println()

    stringOperations()
    println()

    rangeExamples()
    println()

    println(generateOrderReport(
        "ORD-001",
        "John Doe",
        listOf(
            LineItem("SKU-1", "Wireless Keyboard", 1, 79.99),
            LineItem("SKU-2", "USB-C Hub", 2, 34.99),
            LineItem("SKU-3", "Screen Cleaner", 1, 9.99)
        )
    ))
}

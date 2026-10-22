package com.kotlinkata.advanced

/**
 * # Kata 17: DSL Building — Type-Safe APIs
 *
 * Kotlin's DSL support is one of its most unique features — you can build
 * type-safe, IDE-friendly domain-specific languages within Kotlin itself.
 *
 * Key mechanism: lambdas with receivers
 *   A lambda `String.() -> Unit` has `this` bound to a String receiver.
 *   This means the lambda body can call String's methods without qualification.
 *
 * Real-world Kotlin DSLs you've seen:
 *   - Gradle build scripts (build.gradle.kts)
 *   - Ktor routing and configuration
 *   - Exposed (SQL ORM)
 *   - HTML builder
 *   - Jetpack Compose
 *
 * Topics:
 *   - Lambdas with receivers (the core mechanism)
 *   - Building a type-safe query DSL
 *   - Building an HTTP route DSL
 *   - @DslMarker — preventing accidental outer scope access
 *   - Extension functions in DSLs
 */

// ─────────────────────────────────────────────────────────────────────────────
// 1. Lambdas with Receivers — The Core Mechanism
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A regular lambda: () -> Unit
 * A lambda with receiver: String.() -> Unit  — `this` inside = a String
 *
 * This allows the lambda body to call methods on the receiver directly.
 *
 * Without lambda with receiver — explicit this/it needed everywhere:
 *   configure(builder) { builder.setName("John") }
 *
 * With lambda with receiver — methods are called directly:
 *   configure(builder) { setName("John") }
 *
 * Java has no equivalent — it requires Builder classes with method chaining.
 */

// Simple example: a scope where you can call String methods directly
fun String.configure(block: String.() -> Unit): String {
    block()  // `this` inside block is the String
    return this
}

// Building a configuration DSL
class DatabaseConfig {
    var host: String = "localhost"
    var port: Int = 5432
    var database: String = ""
    var username: String = ""
    var password: String = ""
    var maxPoolSize: Int = 10
    var connectionTimeout: Long = 30_000L

    fun toConnectionString(): String = "jdbc:postgresql://$host:$port/$database"
}

// The DSL function — takes a lambda with DatabaseConfig as receiver
fun database(block: DatabaseConfig.() -> Unit): DatabaseConfig {
    return DatabaseConfig().apply(block)
}

fun lambdaWithReceiverExamples() {
    // DSL-style configuration — reads like a config file
    val config = database {
        host = "db.example.com"
        port = 5432
        database = "orders_db"
        username = "app_user"
        password = "secret"
        maxPoolSize = 20
        connectionTimeout = 60_000L
    }
    println("Connection: ${config.toConnectionString()}")
    println("Pool size: ${config.maxPoolSize}")
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Building a Type-Safe Query DSL
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A real-world DSL example: building SQL queries type-safely.
 * This is similar to how Exposed (Kotlin SQL library) works.
 */
@DslMarker  // prevents using outer DSL functions inside inner builders
annotation class QueryDsl

@QueryDsl
class SelectBuilder {
    private val columns = mutableListOf<String>()
    private val conditions = mutableListOf<String>()
    private var tableName = ""
    private var orderByClause: String? = null
    private var limitValue: Int? = null
    private var offsetValue: Int? = null

    fun from(table: String) { tableName = table }
    fun column(name: String) { columns.add(name) }
    fun columns(vararg names: String) { columns.addAll(names) }

    fun where(block: WhereBuilder.() -> Unit) {
        val builder = WhereBuilder()
        builder.block()
        conditions.addAll(builder.conditions)
    }

    fun orderBy(column: String, direction: String = "ASC") {
        orderByClause = "$column $direction"
    }

    fun limit(n: Int) { limitValue = n }
    fun offset(n: Int) { offsetValue = n }

    fun build(): String {
        val cols = if (columns.isEmpty()) "*" else columns.joinToString(", ")
        val sb = StringBuilder("SELECT $cols FROM $tableName")
        if (conditions.isNotEmpty()) sb.append(" WHERE ${conditions.joinToString(" AND ")}")
        orderByClause?.let { sb.append(" ORDER BY $it") }
        limitValue?.let { sb.append(" LIMIT $it") }
        offsetValue?.let { sb.append(" OFFSET $it") }
        return sb.toString()
    }
}

@QueryDsl
class WhereBuilder {
    val conditions = mutableListOf<String>()

    infix fun String.eq(value: Any): Unit { conditions.add("$this = '${safeValue(value)}'") }
    infix fun String.neq(value: Any): Unit { conditions.add("$this != '${safeValue(value)}'") }
    infix fun String.gt(value: Number): Unit { conditions.add("$this > $value") }
    infix fun String.lt(value: Number): Unit { conditions.add("$this < $value") }
    infix fun String.gte(value: Number): Unit { conditions.add("$this >= $value") }
    infix fun String.lte(value: Number): Unit { conditions.add("$this <= $value") }
    infix fun String.like(pattern: String): Unit { conditions.add("$this LIKE '$pattern'") }
    fun String.isNull(): Unit { conditions.add("$this IS NULL") }
    fun String.isNotNull(): Unit { conditions.add("$this IS NOT NULL") }
    infix fun String.inList(values: List<Any>): Unit {
        conditions.add("$this IN (${values.joinToString(", ") { "'${safeValue(it)}'" }})")
    }

    private fun safeValue(v: Any): String = v.toString().replace("'", "''")
}

fun select(block: SelectBuilder.() -> Unit): String = SelectBuilder().apply(block).build()

fun queryDslExamples() {
    // Simple query
    val simple = select {
        from("orders")
        where {
            "status" eq "SHIPPED"
            "customer_id" eq "CUST-001"
        }
        orderBy("created_at", "DESC")
        limit(10)
    }
    println("Simple: $simple")

    // Complex query
    val complex = select {
        columns("id", "customer_id", "total", "created_at")
        from("orders")
        where {
            "status" inList listOf("SHIPPED", "DELIVERED")
            "total" gte 50.0
            "item_count" gt 2
        }
        orderBy("total", "DESC")
        limit(50)
        offset(100)
    }
    println("Complex: $complex")
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Building an HTTP Route DSL (Ktor-style)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Similar to how Ktor defines routes:
 *   routing {
 *       route("/orders") {
 *           get { ... }
 *           post { ... }
 *           route("/{id}") {
 *               get { ... }
 *               put { ... }
 *               delete { ... }
 *           }
 *       }
 *   }
 */
@DslMarker
annotation class RoutingDsl

data class RouteDefinition(
    val path: String,
    val method: String,
    val handler: String
)

@RoutingDsl
class RouteBuilder(private val basePath: String = "") {
    val routes = mutableListOf<RouteDefinition>()
    private val children = mutableListOf<RouteBuilder>()

    fun route(path: String, block: RouteBuilder.() -> Unit) {
        val child = RouteBuilder("$basePath$path")
        child.block()
        children.add(child)
    }

    fun get(handler: String) = routes.add(RouteDefinition("$basePath", "GET", handler))
    fun post(handler: String) = routes.add(RouteDefinition("$basePath", "POST", handler))
    fun put(handler: String) = routes.add(RouteDefinition("$basePath", "PUT", handler))
    fun delete(handler: String) = routes.add(RouteDefinition("$basePath", "DELETE", handler))
    fun patch(handler: String) = routes.add(RouteDefinition("$basePath", "PATCH", handler))

    fun collect(): List<RouteDefinition> =
        routes + children.flatMap { it.collect() }
}

fun routing(block: RouteBuilder.() -> Unit): List<RouteDefinition> {
    return RouteBuilder().apply(block).collect()
}

fun routingDslExamples() {
    val routes = routing {
        route("/api/v1") {
            route("/orders") {
                get("OrderController::listOrders")
                post("OrderController::createOrder")

                route("/{id}") {
                    get("OrderController::getOrder")
                    put("OrderController::updateOrder")
                    delete("OrderController::cancelOrder")

                    route("/items") {
                        get("OrderController::listItems")
                        post("OrderController::addItem")
                    }
                }
            }

            route("/customers") {
                get("CustomerController::list")
                post("CustomerController::create")
                route("/{id}") {
                    get("CustomerController::get")
                    patch("CustomerController::update")
                }
            }
        }
    }

    println("Registered routes:")
    routes.forEach { route ->
        println("  ${route.method.padEnd(7)} ${route.path} → ${route.handler}")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. @DslMarker — Preventing Scope Leaks
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Without @DslMarker, a nested DSL block can accidentally call methods from
 * an outer builder — leading to confusing bugs.
 *
 * @DslMarker prevents calling methods from an enclosing receiver of the same
 * DSL within a nested lambda.
 *
 * Gradle's build.gradle.kts uses @DslMarker extensively.
 */
@DslMarker
annotation class EmailDsl

@EmailDsl
class EmailBuilder {
    var from: String = ""
    var subject: String = ""
    private var body: String = ""
    private val to = mutableListOf<String>()
    private val attachments = mutableListOf<String>()

    fun to(address: String) { to.add(address) }
    fun to(vararg addresses: String) { to.addAll(addresses) }
    fun subject(text: String) { subject = text }
    fun body(block: BodyBuilder.() -> Unit) { body = BodyBuilder().apply(block).build() }
    fun attach(path: String) { attachments.add(path) }

    fun build(): Email = Email(from, to.toList(), subject, body, attachments.toList())
}

@EmailDsl
class BodyBuilder {
    private val lines = mutableListOf<String>()

    fun text(line: String) { lines.add(line) }
    fun h1(text: String) { lines.add("<h1>$text</h1>") }
    fun p(text: String) { lines.add("<p>$text</p>") }
    fun br() { lines.add("<br/>") }
    fun link(text: String, href: String) { lines.add("<a href='$href'>$text</a>") }

    fun build(): String = lines.joinToString("\n")
}

data class Email(
    val from: String,
    val to: List<String>,
    val subject: String,
    val body: String,
    val attachments: List<String>
)

fun email(block: EmailBuilder.() -> Unit): Email = EmailBuilder().apply(block).build()

fun emailDslExamples() {
    val confirmationEmail = email {
        from = "orders@shop.com"
        to("customer@example.com")
        subject = "Your Order ORD-001 has been confirmed"
        body {
            h1("Order Confirmed!")
            p("Thank you for your order.")
            p("Order ID: ORD-001")
            p("Total: \$149.99")
            br()
            link("Track your order", "https://shop.com/track/ORD-001")
        }
        attach("/tmp/invoice-ORD-001.pdf")
    }

    println("Email: ${confirmationEmail.subject}")
    println("To: ${confirmationEmail.to}")
    println("Body:\n${confirmationEmail.body}")
    println("Attachments: ${confirmationEmail.attachments}")
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Mini: Order Validation DSL
// ─────────────────────────────────────────────────────────────────────────────

@DslMarker
annotation class ValidationDsl

@ValidationDsl
class ValidationSpec<T>(private val value: T, private val fieldName: String) {
    private val errors = mutableListOf<String>()

    fun mustNotBeNull(): ValidationSpec<T> {
        if (value == null) errors.add("$fieldName must not be null")
        return this
    }

    fun errors(): List<String> = errors.toList()
    fun isValid(): Boolean = errors.isEmpty()
}

@ValidationDsl
class StringValidationSpec(private val value: String?, private val fieldName: String) {
    private val errors = mutableListOf<String>()

    fun notBlank(): StringValidationSpec {
        if (value.isNullOrBlank()) errors.add("$fieldName must not be blank")
        return this
    }
    fun minLength(min: Int): StringValidationSpec {
        if ((value?.length ?: 0) < min) errors.add("$fieldName must be at least $min characters")
        return this
    }
    fun maxLength(max: Int): StringValidationSpec {
        if ((value?.length ?: 0) > max) errors.add("$fieldName must be at most $max characters")
        return this
    }
    fun matches(pattern: Regex): StringValidationSpec {
        if (value != null && !pattern.matches(value)) errors.add("$fieldName has invalid format")
        return this
    }
    fun errors(): List<String> = errors.toList()
}

@ValidationDsl
class NumberValidationSpec(private val value: Double?, private val fieldName: String) {
    private val errors = mutableListOf<String>()

    fun positive(): NumberValidationSpec {
        if ((value ?: 0.0) <= 0) errors.add("$fieldName must be positive")
        return this
    }
    fun min(minimum: Double): NumberValidationSpec {
        if ((value ?: 0.0) < minimum) errors.add("$fieldName must be >= $minimum")
        return this
    }
    fun max(maximum: Double): NumberValidationSpec {
        if ((value ?: 0.0) > maximum) errors.add("$fieldName must be <= $maximum")
        return this
    }
    fun errors(): List<String> = errors.toList()
}

class OrderValidationBuilder(private val order: OrderInput) {
    private val allErrors = mutableListOf<String>()

    fun field(name: String, value: String?, block: StringValidationSpec.() -> Unit) {
        val spec = StringValidationSpec(value, name)
        spec.block()
        allErrors.addAll(spec.errors())
    }

    fun field(name: String, value: Double?, block: NumberValidationSpec.() -> Unit) {
        val spec = NumberValidationSpec(value, name)
        spec.block()
        allErrors.addAll(spec.errors())
    }

    fun validate(): List<String> = allErrors.toList()
}

data class OrderInput(
    val customerId: String?,
    val total: Double?,
    val shippingAddress: String?,
    val notes: String?
)

fun validateOrder(order: OrderInput, block: OrderValidationBuilder.() -> Unit): List<String> {
    return OrderValidationBuilder(order).apply(block).validate()
}

fun validationDslExamples() {
    val input = OrderInput("", -5.0, null, "x".repeat(1001))

    val errors = validateOrder(input) {
        field("customerId", input.customerId) {
            notBlank()
            minLength(5)
            matches(Regex("CUST-[A-Z0-9]+"))
        }
        field("total", input.total) {
            positive()
            min(0.01)
            max(999_999.99)
        }
        field("shippingAddress", input.shippingAddress) {
            notBlank()
            minLength(10)
        }
        field("notes", input.notes) {
            maxLength(1000)
        }
    }

    if (errors.isEmpty()) println("Valid!")
    else errors.forEach { println("❌ $it") }
}

fun main() {
    println("=== Kata 17: DSL Building ===\n")
    println("--- Lambda with Receiver ---")
    lambdaWithReceiverExamples()
    println("\n--- Query DSL ---")
    queryDslExamples()
    println("\n--- Routing DSL ---")
    routingDslExamples()
    println("\n--- Email DSL ---")
    emailDslExamples()
    println("\n--- Validation DSL ---")
    validationDslExamples()
}

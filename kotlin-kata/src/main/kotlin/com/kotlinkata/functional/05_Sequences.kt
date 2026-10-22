package com.kotlinkata.functional

/**
 * # Kata 14: Sequences — Lazy Evaluation
 *
 * `Sequence<T>` is Kotlin's equivalent of Java's `Stream<T>`.
 * Operations are evaluated LAZILY — elements are processed one at a time
 * through the entire pipeline before moving to the next element.
 *
 * Collection operations are EAGER (intermediate lists created at each step).
 * Sequence operations are LAZY (no intermediate lists, process element by element).
 *
 * Java: stream() → process one by one → collect()
 * Kotlin collections: process all → intermediate list → process all → ...
 * Kotlin sequences: process one through the whole pipeline → next one → ...
 *
 * When to use Sequence vs Collection:
 *   - Large collections (> ~1000 elements) with multiple transformation steps
 *   - Potentially infinite data sources
 *   - When you want to short-circuit (first, find, take, any, all)
 *   - When processing file lines, database cursors, network streams
 *
 * When NOT to use Sequence:
 *   - Small collections (overhead of Sequence setup not worth it)
 *   - Operations that need all elements first (sort, groupBy, last)
 *   - When you need to iterate multiple times (Sequence is consumed once)
 *
 * Topics:
 *   - Creating sequences
 *   - Sequence vs Collection performance
 *   - generateSequence for infinite sequences
 *   - sequence {} builder (suspend-like sequences)
 *   - Terminal operations
 */

// ─────────────────────────────────────────────────────────────────────────────
// 1. Creating Sequences
// ─────────────────────────────────────────────────────────────────────────────

fun creatingSequences() {
    // From existing collection
    val fromList = listOf(1, 2, 3, 4, 5).asSequence()
    val fromRange = (1..100).asSequence()

    // sequenceOf — like listOf but lazy
    val seq = sequenceOf("ORD-001", "ORD-002", "ORD-003")

    // generateSequence — potentially infinite, next element from previous
    val orderIds = generateSequence(1) { it + 1 }   // 1, 2, 3, ... (infinite)
    val first10 = orderIds.take(10).toList()
    println("First 10 IDs: $first10")

    // Fibonacci with generateSequence
    val fibonacci = generateSequence(Pair(0, 1)) { Pair(it.second, it.first + it.second) }
        .map { it.first }
        .take(15)
        .toList()
    println("Fibonacci: $fibonacci")

    // From file (conceptual — simulated here)
    val lines = generateSequence("data/orders.csv") { null }  // would be BufferedReader.readLine()
    // In real code: File("orders.csv").bufferedReader().lineSequence()

    // Sequence of dates
    val dates = generateSequence(java.time.LocalDate.of(2024, 1, 1)) { it.plusDays(1) }
    val januaryWeekdays = dates
        .takeWhile { it.monthValue == 1 }
        .filter { it.dayOfWeek.value <= 5 }  // Mon-Fri
        .toList()
    println("January weekdays: ${januaryWeekdays.size}")
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Eager vs Lazy Evaluation
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Visualizing the difference:
 *
 * EAGER (Collection):
 *   [1,2,3,4,5] → map → [10,20,30,40,50] → filter → [10,30] → first: 10
 *    5 map ops           5 filter ops
 *    2 intermediate lists created
 *
 * LAZY (Sequence):
 *   1 → map(10) → filter(pass) → first: found! Done.
 *   2 → map(20) → filter(fail)
 *   3 → map(30) → filter(pass) → first was already found, stop.
 *    Only 2 map ops, 3 filter ops — short-circuited!
 *    No intermediate lists!
 */
fun eagerVsLazyDemo() {
    val bigList = (1..1_000_000).toList()

    // EAGER — creates intermediate list for each step
    val eagerStart = System.currentTimeMillis()
    val eagerResult = bigList
        .map { it * 2 }          // creates List<Int> with 1M items
        .filter { it % 6 == 0 }  // creates another List<Int>
        .first()                  // takes first
    println("Eager result: $eagerResult, time: ${System.currentTimeMillis() - eagerStart}ms")

    // LAZY — processes elements one at a time, stops when first is found
    val lazyStart = System.currentTimeMillis()
    val lazyResult = bigList.asSequence()
        .map { it * 2 }           // no list created
        .filter { it % 6 == 0 }  // no list created
        .first()                  // finds first match and STOPS
    println("Lazy result: $lazyResult, time: ${System.currentTimeMillis() - lazyStart}ms")

    // Visualize with logging
    println("\nEager evaluation order:")
    val eagerViz = (1..5).toList()
        .map { n -> n.also { print("map($it) ") } }
        .filter { it % 2 != 0 }
    println()

    println("Lazy evaluation order:")
    val lazyViz = (1..5).asSequence()
        .map { n -> n.also { print("map($it) ") } }
        .filter { it % 2 != 0 }
        .toList()  // terminal operation — triggers evaluation
    println()
    println("Lazy result: $lazyViz")
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. generateSequence for Domain Problems
// ─────────────────────────────────────────────────────────────────────────────

/**
 * generateSequence is perfect for:
 *   - Traversing tree/graph structures
 *   - Walking up a hierarchy
 *   - Pagination (fetch next page until empty)
 *   - Retry with backoff
 */
data class Category(val id: String, val name: String, val parentId: String?)

fun getCategoryPath(categoryId: String, allCategories: Map<String, Category>): List<String> {
    return generateSequence(allCategories[categoryId]) { cat ->
        cat.parentId?.let { allCategories[it] }
    }.map { it.name }.toList().reversed()
}

fun retryWithBackoff(
    maxAttempts: Int,
    baseDelayMs: Long,
    action: () -> Boolean
): Boolean {
    return generateSequence(1) { if (it < maxAttempts) it + 1 else null }
        .any { attempt ->
            if (attempt > 1) Thread.sleep(baseDelayMs * (1L shl (attempt - 2)))
            println("Attempt $attempt...")
            action()
        }
}

// Paginated data loading
data class Page<T>(val items: List<T>, val nextPageToken: String?)

fun <T> loadAllPages(loadPage: (String?) -> Page<T>): Sequence<T> {
    return generateSequence(loadPage(null)) { page ->
        page.nextPageToken?.let { loadPage(it) }
    }.flatMap { it.items.asSequence() }
}

fun generateSequenceExamples() {
    val categories = mapOf(
        "cat-1" to Category("cat-1", "Electronics", null),
        "cat-2" to Category("cat-2", "Computers", "cat-1"),
        "cat-3" to Category("cat-3", "Laptops", "cat-2"),
        "cat-4" to Category("cat-4", "Gaming Laptops", "cat-3")
    )

    println("Category path: ${getCategoryPath("cat-4", categories)}")

    // Simulated paginated API
    val allPages = loadAllPages { token ->
        when (token) {
            null   -> Page(listOf("ORD-001", "ORD-002"), nextPageToken = "page2")
            "page2" -> Page(listOf("ORD-003", "ORD-004"), nextPageToken = "page3")
            "page3" -> Page(listOf("ORD-005"), nextPageToken = null)
            else    -> Page(emptyList(), nextPageToken = null)
        }
    }
    println("All orders from pages: ${allPages.toList()}")
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. sequence {} Builder — Coroutine-style Sequence
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The `sequence { }` builder uses coroutine-like suspension (yield/yieldAll)
 * to generate elements on demand. This is how Python generators work.
 *
 * Perfect for:
 *   - Complex state machines that produce values
 *   - Interleaved data from multiple sources
 *   - Sequences that depend on previously produced values
 */
fun orderBatchSequence(orders: List<String>, batchSize: Int): Sequence<List<String>> = sequence {
    val pending = orders.toMutableList()

    while (pending.isNotEmpty()) {
        val batch = pending.take(batchSize)
        pending.removeAll(batch)

        println("[BATCH] Preparing batch of ${batch.size} orders")
        yield(batch)  // pause and return this batch
    }
}

fun treeTraversalSequence(root: TreeNode): Sequence<TreeNode> = sequence {
    val stack = ArrayDeque<TreeNode>()
    stack.addLast(root)

    while (stack.isNotEmpty()) {
        val node = stack.removeLast()
        yield(node)  // yield the current node
        node.children.forEach { stack.addLast(it) }
    }
}

data class TreeNode(val id: String, val value: String, val children: List<TreeNode> = emptyList())

fun sequenceBuilderExamples() {
    val orders = (1..12).map { "ORD-${"$it".padStart(3, '0')}" }

    println("Processing in batches of 5:")
    orderBatchSequence(orders, 5).forEachIndexed { i, batch ->
        println("  Batch ${i + 1}: $batch")
    }

    val tree = TreeNode("root", "Root", listOf(
        TreeNode("a", "Electronics", listOf(
            TreeNode("a1", "Phones"),
            TreeNode("a2", "Laptops")
        )),
        TreeNode("b", "Books", listOf(
            TreeNode("b1", "Fiction"),
            TreeNode("b2", "Technical")
        ))
    ))

    println("Tree traversal:")
    treeTraversalSequence(tree).forEach { node ->
        println("  ${node.id}: ${node.value}")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Terminal Operations
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Sequences are lazy — nothing happens until a terminal operation is called.
 *
 * Terminal operations:
 *   toList(), toSet(), toMap()    — materialize to collection
 *   first(), firstOrNull()        — get first (short-circuits)
 *   last(), lastOrNull()          — get last (must traverse all)
 *   find { }                      — find first match (short-circuits)
 *   any { }, all { }, none { }    — check conditions (may short-circuit)
 *   count { }                     — count matching (traverses all)
 *   sum(), average(), min(), max()— aggregate (traverses all)
 *   forEach { }                   — side effects (traverses all)
 *   fold(), reduce()              — accumulate (traverses all)
 *   joinToString()                — format (traverses all)
 */
fun terminalOperations() {
    val infiniteCounter = generateSequence(1) { it + 1 }

    // Short-circuit terminal operations
    val first10k = infiniteCounter.first { it > 10_000 }   // stops at 10,001
    val has5000 = infiniteCounter.any { it == 5_000 }       // stops at 5,000
    println("First > 10000: $first10k, has 5000: $has5000")

    // Short-circuit with take
    val first100 = infiniteCounter.take(100).toList()        // stops at 100

    // Real-world: process log lines until error
    val logLines = generateSequence(1) { it + 1 }
        .map { lineNum ->
            when {
                lineNum % 10 == 0 -> "ERROR: Something failed at line $lineNum"
                else -> "INFO: Processing event $lineNum"
            }
        }

    val errorLine = logLines.find { it.startsWith("ERROR") }
    println("First error: $errorLine")

    // Collect to different containers
    val orderSeq = sequenceOf("ORD-001", "ORD-002", "ORD-001", "ORD-003")
    val uniqueOrders: Set<String> = orderSeq.toSet()
    val orderedOrders: List<String> = orderSeq.toList()
    println("Unique: $uniqueOrders (${uniqueOrders.size}), All: ${orderedOrders.size}")
}

fun main() {
    println("=== Kata 14: Sequences ===\n")
    println("--- Creating Sequences ---")
    creatingSequences()
    println("\n--- Eager vs Lazy ---")
    eagerVsLazyDemo()
    println("\n--- generateSequence ---")
    generateSequenceExamples()
    println("\n--- Sequence Builder ---")
    sequenceBuilderExamples()
    println("\n--- Terminal Operations ---")
    terminalOperations()
}

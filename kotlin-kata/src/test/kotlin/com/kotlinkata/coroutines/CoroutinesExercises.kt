package com.kotlinkata.coroutines

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * # Exercises: Coroutines
 *
 * These exercises use runTest from kotlinx-coroutines-test for deterministic testing.
 * Replace each TODO() with your implementation.
 */
class CoroutinesExercises {

    // ─────────────────────────────────────────────────────────────────────────
    // Exercise 1: Basic suspend functions
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `implement a suspend function that fetches and transforms data`() = runTest {
        // TODO: Implement fetchOrderTotal — a suspend function that:
        //   1. Simulates a delay of 100ms
        //   2. Returns the sum of the given prices
        suspend fun fetchOrderTotal(prices: List<Double>): Double = TODO()

        val result = fetchOrderTotal(listOf(19.99, 29.99, 9.99))
        assertThat(result).isCloseTo(59.97, org.assertj.core.data.Offset.offset(0.001))
    }

    @Test
    fun `run two operations sequentially vs concurrently`() = runTest {
        var callCount = 0

        suspend fun fetchData(id: Int): String {
            callCount++
            delay(100)
            return "data-$id"
        }

        // TODO: Fetch data for IDs 1 and 2 CONCURRENTLY using async/await
        // Store results in result1 and result2
        // TODO: Start both fetches concurrently, then await both
        val result1: String = TODO()
        val result2: String = TODO()

        assertThat(result1).isEqualTo("data-1")
        assertThat(result2).isEqualTo("data-2")
        assertThat(callCount).isEqualTo(2)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Exercise 2: Structured Concurrency
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `process a list of items concurrently`() = runTest {
        suspend fun processItem(id: Int): String {
            delay(50)
            return "processed-$id"
        }

        val ids = (1..10).toList()

        // TODO: Process all IDs concurrently using coroutineScope + async + awaitAll
        // HINT: coroutineScope { ids.map { id -> async { processItem(id) } }.awaitAll() }
        val results: List<String> = TODO()

        assertThat(results).hasSize(10)
        assertThat(results).contains("processed-1", "processed-5", "processed-10")
    }

    @Test
    fun `handle timeout with fallback`() = runTest {
        suspend fun slowFetch(): String {
            delay(1000)  // takes 1 second
            return "slow result"
        }

        // TODO: Use withTimeoutOrNull to get the result within 200ms
        // If it times out, return "timeout-fallback"
        val result: String = TODO()

        assertThat(result).isEqualTo("timeout-fallback")
    }

    @Test
    fun `collect results with individual error handling`() = runTest {
        data class ItemResult(val id: Int, val success: Boolean, val value: String?)

        suspend fun processItem(id: Int): String {
            delay(10)
            if (id % 3 == 0) throw RuntimeException("Failed for $id")
            return "success-$id"
        }

        val ids = (1..6).toList()

        // TODO: Process each item, wrapping errors — use supervisorScope + async
        // For each item: if successful, ItemResult(id, true, value)
        //                if failed,    ItemResult(id, false, null)
        val results: List<ItemResult> = TODO()

        assertThat(results).hasSize(6)
        assertThat(results.count { it.success }).isEqualTo(4)  // 1,2,4,5 succeed; 3,6 fail
        assertThat(results.filter { !it.success }.map { it.id }).containsExactly(3, 6)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Exercise 3: Flows
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `create and collect a basic flow`() = runTest {
        // TODO: Create a Flow<Int> that emits the square of each number from 1 to 5
        // HINT: flow { for (i in 1..5) { emit(i * i) } }
        val squaresFlow: Flow<Int> = TODO()

        val collected = mutableListOf<Int>()
        squaresFlow.collect { collected.add(it) }

        assertThat(collected).containsExactly(1, 4, 9, 16, 25)
    }

    @Test
    fun `transform a flow with operators`() = runTest {
        val orderIds = flowOf(
            "ORD-001", "ORD-002", "ORD-003", "ORD-004", "ORD-005"
        )

        // TODO: Transform the flow to:
        //   1. Filter only even-numbered orders (ORD-002, ORD-004)
        //   2. Map each to uppercase
        // HINT: orderIds.filter { it.last().digitToInt() % 2 == 0 }.map { it.uppercase() }
        val result: Flow<String> = TODO()

        val collected = result.toList()
        assertThat(collected).containsExactly("ORD-002", "ORD-004")
    }

    @Test
    fun `use StateFlow to track order status`() = runTest {
        // TODO: Create a MutableStateFlow with initial value "PENDING"
        // Then update it through: CONFIRMED → SHIPPED → DELIVERED
        // Collect all emitted values and verify the final state
        val statusFlow: MutableStateFlow<String> = TODO()
        val observed = mutableListOf<String>()

        val job = launch {
            statusFlow.collect { observed.add(it) }
        }

        // TODO: Update the status
        statusFlow.value = TODO()  // CONFIRMED
        statusFlow.value = TODO()  // SHIPPED
        statusFlow.value = TODO()  // DELIVERED

        delay(50)
        job.cancel()

        assertThat(statusFlow.value).isEqualTo("DELIVERED")
        assertThat(observed).contains("PENDING", "CONFIRMED", "SHIPPED", "DELIVERED")
    }
}

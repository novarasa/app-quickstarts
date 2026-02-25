# Kotlin Kata — From Java to Kotlin Mastery

A hands-on kata repository for **Principal Java Engineers** learning Kotlin. Every example is grounded in real production scenarios using an e-commerce order management domain.

## Philosophy

- **Real-world first**: Examples use a consistent e-commerce domain (orders, payments, products)
- **Java comparisons**: Every key concept shows the Java equivalent and explains *why* Kotlin's approach is better
- **Kata format**: Study the concept file → do the exercises → run tests to verify
- **Progressive depth**: Basics → OOP → Functional → Advanced → Coroutines → Capstone

## Project Structure

```
src/
├── main/kotlin/com/kotlinkata/
│   ├── basics/          # val/var, null safety, functions, when, strings
│   ├── oop/             # data classes, sealed classes, objects, delegation
│   ├── functional/      # extension functions, HOF, collections, scope functions, sequences
│   ├── advanced/        # generics with variance, inline functions, DSL building
│   ├── coroutines/      # suspend functions, structured concurrency, flows
│   └── realworld/       # capstone: complete order management system
│
└── test/kotlin/com/kotlinkata/
    ├── basics/          # exercises for basics
    ├── oop/             # exercises for OOP
    ├── functional/      # exercises for functional programming
    ├── advanced/        # exercises for advanced features
    ├── coroutines/      # exercises for coroutines
    └── realworld/       # capstone exercises
```

## Getting Started

### Prerequisites
- JDK 21+
- IntelliJ IDEA (recommended) **or** Gradle 8.5+ on PATH

### Setup

```bash
# Option 1: Open in IntelliJ IDEA — it handles everything automatically
# File → Open → select the kotlin-kata/ directory

# Option 2: Command line
cd kotlin-kata
gradle wrapper           # downloads the wrapper jar
./gradlew test           # run all exercises
./gradlew test --tests "com.kotlinkata.basics.*"  # run one module
```

## Learning Path

| Step | Module | File Pattern | What You'll Learn |
|------|--------|--------------|-------------------|
| 1 | `basics` | `01_Variables.kt` | val/var, type inference, nullable types, smart casts |
| 2 | `basics` | `02_NullSafety.kt` | ?.  ?: !! let, eliminating NPEs structurally |
| 3 | `basics` | `03_Functions.kt` | Named params, defaults, single-expression, local, infix |
| 4 | `basics` | `04_WhenExpressions.kt` | when as expression, exhaustive checks, smart casts |
| 5 | `basics` | `05_StringTemplates.kt` | Templates, multiline strings, ranges |
| 6 | `oop` | `01_DataClasses.kt` | Data classes, copy(), destructuring (vs Lombok/records) |
| 7 | `oop` | `02_SealedClasses.kt` | Sealed classes for algebraic types, Result pattern |
| 8 | `oop` | `03_ObjectsAndCompanions.kt` | Singletons, factory methods, companion objects |
| 9 | `oop` | `04_Delegation.kt` | Interface & property delegation, lazy, observable |
| 10 | `functional` | `01_ExtensionFunctions.kt` | Extensions (goodbye utility classes) |
| 11 | `functional` | `02_HigherOrderFunctions.kt` | Function types, lambdas, closures |
| 12 | `functional` | `03_Collections.kt` | map/filter/flatMap/groupBy/fold (vs Streams) |
| 13 | `functional` | `04_ScopeFunctions.kt` | let/run/with/apply/also — when to use which |
| 14 | `functional` | `05_Sequences.kt` | Lazy evaluation, sequence builders (vs Java Streams) |
| 15 | `advanced` | `01_Generics.kt` | Variance (in/out) vs Java wildcards, reified generics |
| 16 | `advanced` | `02_InlineFunctions.kt` | inline, noinline, crossinline, reified |
| 17 | `advanced` | `03_DSLBuilding.kt` | Type-safe DSLs with lambdas-with-receivers |
| 18 | `coroutines` | `01_Basics.kt` | suspend, launch, async, CoroutineScope |
| 19 | `coroutines` | `02_StructuredConcurrency.kt` | Job hierarchy, cancellation, error handling |
| 20 | `coroutines` | `03_Flows.kt` | Cold streams, StateFlow, SharedFlow |
| 21 | `realworld` | `OrderDomain.kt` | Complete domain model tying everything together |
| 22 | `realworld` | `OrderService.kt` | Service layer with real patterns |

## Java → Kotlin Quick Reference

| Java Pattern | Kotlin Idiomatic Equivalent |
|---|---|
| `final String s = "x"` | `val s = "x"` |
| `String s = "x"` | `var s = "x"` |
| `if (x != null) x.method()` | `x?.method()` |
| `x != null ? x : default` | `x ?: default` |
| `Optional.ofNullable(x).orElse(y)` | `x ?: y` |
| `(String) obj` after instanceof | Smart cast — no cast needed |
| `static` methods | Top-level functions or `companion object` |
| `enum` with abstract methods | `sealed class` |
| Lombok `@Data` / Java `record` | `data class` |
| Utility class (StringUtils, etc.) | Extension functions |
| Method overloading for defaults | Default parameters |
| Builder pattern | Named + default parameters |
| `Stream<T>` pipeline | Collection ops or `Sequence<T>` |
| `CompletableFuture<T>` | `suspend` function |
| `ExecutorService` | `CoroutineScope` + `Dispatchers` |
| `? extends T` / `? super T` | `out T` / `in T` |

## Domain Model

All examples use a consistent **Order Management System** domain:

```
Order ──has many──▶ OrderItem ──references──▶ Product
  │
  ├── status: OrderStatus (sealed class)
  ├── customerId: String
  └── createdAt: Instant

OrderStatus:
  Pending → Confirmed → Shipped → Delivered
                      ↘ Cancelled
```

This gives every concept a concrete, relatable context.

## Exercise Format

Exercise files (in `test/`) follow this pattern:

```kotlin
@Test
fun `map transforms each element`() {
    val prices = listOf(10.0, 20.0, 30.0)

    // TODO: Apply a 10% discount to each price
    val discounted: List<Double> = TODO()

    assertThat(discounted).containsExactly(9.0, 18.0, 27.0)
}
```

Replace `TODO()` with your implementation, then run `./gradlew test`.

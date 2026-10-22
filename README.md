# app-quickstarts

Small, self-contained starter projects you can clone, build and learn from in minutes.
Each quickstart lives in its own top-level directory with its own build, README and tests.

## 👋 For humans

**What's here?** Hands-on quickstarts that pin current, stable versions of their
languages, frameworks and tools. Pick one, `cd` into it and follow its README.

**Fastest path:**

```bash
git clone https://github.com/novarasa/app-quickstarts.git
cd app-quickstarts/kotlin-kata
./gradlew compileTestKotlin   # verify the toolchain works
./gradlew test                # start the exercises
```

You only need a JDK (17+ to launch Gradle; JDK 25 is used for compilation and is
downloaded automatically if missing). Everything else comes from the Gradle wrapper.

## 🤖 For AI agents

You are in a multi-project repository. There is **no root build**. Treat each
top-level directory listed below as an independent project.

- **Work inside one quickstart at a time.** Run commands from that quickstart's
  directory, e.g. `cd kotlin-kata && ./gradlew compileTestKotlin`.
- **Verify with:** `./gradlew compileTestKotlin` (must pass). `./gradlew test` is
  *expected* to fail on a fresh checkout because the exercises are `TODO()`
  placeholders for learners; don't "fix" them by filling in answers unless asked.
- **Versions** are centralised in each project's `gradle/libs.versions.toml`.
  Upgrade there, not inline in `build.gradle.kts`. Gradle itself is upgraded with
  `./gradlew wrapper --gradle-version <x.y.z>` (run it twice so the wrapper
  scripts and jar match), never by hand-editing `gradlew`.
- **Keep it teachable.** Code comments explain *why* (often with a Java
  comparison). Match that density and tone in any change.
- **Every concept file has a `main()`.** If you change one, run it to make sure
  the demo still executes cleanly.

See [AGENTS.md](AGENTS.md); it points back here so there is a single source of truth.

## Quickstarts

| Directory | What it teaches | Stack |
|---|---|---|
| [`kotlin-kata/`](kotlin-kata/) | Kotlin for experienced Java engineers: basics → OOP → functional → advanced → coroutines → capstone | Kotlin 2.4.21 · JDK 25 · Gradle 9.8.2 · kotlinx-coroutines 1.11.0 · JUnit 6.1.3 · AssertJ 3.27.7 · MockK 1.14.11 |

## Adding a quickstart

1. Create a new top-level directory with its own build and wrapper.
2. Pin the latest stable versions (no RCs, betas or milestones) in a version catalog.
3. Add a README with prerequisites, a one-command verify step and a learning path.
4. Add a row to the table above.

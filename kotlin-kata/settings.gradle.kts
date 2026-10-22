plugins {
    // Lets Gradle download the JDK requested by `jvmToolchain` if it isn't installed locally.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "kotlin-kata"

package com.dzaitsev.trellis.dsl

/**
 * Declares a [ModuleRegistry]:
 *
 * ```
 * val projectModules = moduleRegistry {
 *     root("app-root")
 *
 *     group("feature") {
 *         kmp("auth") { jvm(); ios() }
 *         kmp("profile") { jvm(); ios() }
 *     }
 * }
 * ```
 */
public fun moduleRegistry(block: ModuleRegistryBuilder.() -> Unit): ModuleRegistry = ModuleRegistryBuilder()
    .apply(block)
    .build()

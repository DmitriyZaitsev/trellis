package com.dzaitsev.trellis.configurator

import com.dzaitsev.trellis.Module
import com.dzaitsev.trellis.plugin.Configurator
import com.dzaitsev.trellis.plugin.TrellisProjectExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin

/**
 * Enables Kotlin's `explicitApi()` mode for a module declared with
 * [Module.hasExplicitApi] set to `true` (the default for every built-in [Module]
 * kind) — requiring explicit visibility modifiers and return types on public declarations.
 *
 * Waits for any Kotlin Gradle plugin (JVM, Multiplatform, or AGP 9's built-in Kotlin) to be
 * applied, so modules without Kotlin (e.g. [Module.Root]) are left alone.
 */
public class ExplicitApiConfigurator : Configurator<TrellisProjectExtension> {
    override fun configure(project: Project, module: Module, extension: TrellisProjectExtension) {
        if (!module.hasExplicitApi) return

        project.plugins.withType<KotlinBasePlugin>().configureEach {
            project.extensions.findByType<KotlinProjectExtension>()?.explicitApi()
        }
    }
}

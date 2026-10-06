package com.dzaitsev.gradle.moduledsl.configurator

import com.dzaitsev.gradle.moduledsl.Module
import com.dzaitsev.gradle.moduledsl.plugin.Configurator
import com.dzaitsev.gradle.moduledsl.plugin.ProjectExtension
import org.gradle.api.Project
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
public class ExplicitApiConfigurator : Configurator<Module, ProjectExtension> {
    override fun configure(project: Project, module: Module, extension: ProjectExtension) {
        if (!module.hasExplicitApi) return

        project.plugins.withType(KotlinBasePlugin::class.java).configureEach {
            project.extensions.findByType(KotlinProjectExtension::class.java)?.explicitApi()
        }
    }
}

package com.example.buildlogic

import com.dzaitsev.trellis.Module
import com.dzaitsev.trellis.plugin.Configurator
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType

class DetektConfigurator : Configurator<SampleExtension> {
    override fun configure(project: Project, module: Module, extension: SampleExtension) {
        if (module is Module.Root) return

        project.pluginManager.apply("io.gitlab.arturbosch.detekt")
        project.pluginManager.withPlugin("io.gitlab.arturbosch.detekt") {
            project.extensions.configure<DetektExtension> {
                allRules = true
                buildUponDefaultConfig = true
                config.setFrom(extension.detektConfig)
                parallel = true
                // Avoid AGP-specific variant tasks; the generic task below covers Android sources too.
                ignoredVariants = listOf(".*")
            }

            project.tasks.withType<Detekt>().configureEach {
                exclude("**/build/**", "**/generated/**")
                reports {
                    xml.required.set(true)
                    html.required.set(true)
                    txt.required.set(true)
                    sarif.required.set(true)
                }
            }

            // The default source roots omit KMP commonMain and platform source sets.
            // Analyze production Kotlin sources across all targets without type resolution.
            project.tasks.named<Detekt>("detekt") {
                setSource(project.layout.projectDirectory.dir("src"))
                include("**/*.kt", "**/*.kts")
                exclude("**/test/**", "**/*Test/**", "**/release/**", "**/androidRelease/**")
            }
        }
    }
}

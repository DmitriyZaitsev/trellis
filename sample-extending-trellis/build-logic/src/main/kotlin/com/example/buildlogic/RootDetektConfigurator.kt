package com.example.buildlogic

import com.dzaitsev.trellis.Module
import com.dzaitsev.trellis.plugin.Configurator
import org.gradle.api.Project

class RootDetektConfigurator : Configurator<SampleExtension> {
    override fun configure(project: Project, module: Module, extension: SampleExtension) {
        val aggregate = project.tasks.register("runAllDetekt") {
            group = "verification"
            description = "Analyze production Kotlin sources in every Trellis module."
        }

        project.subprojects.forEach { child ->
            child.pluginManager.withPlugin("io.gitlab.arturbosch.detekt") {
                // One source-only task per module, without variant compilation or duplicate analysis.
                val analysis = child.tasks.named("detekt")
                aggregate.configure { dependsOn(analysis) }
            }
        }
    }
}

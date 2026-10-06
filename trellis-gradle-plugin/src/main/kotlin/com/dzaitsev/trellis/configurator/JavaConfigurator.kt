package com.dzaitsev.trellis.configurator

import com.dzaitsev.trellis.Module
import com.dzaitsev.trellis.PluginIds
import com.dzaitsev.trellis.plugin.Configurator
import com.dzaitsev.trellis.plugin.ProjectExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

/**
 * Configures a plain Java module (application or library).
 *
 * Sets up group, version and test logging; the JVM target comes from [JvmTargetConfigurator].
 * If [Module.Java.mainClass] is provided, applies the `application` plugin and sets it.
 */
public class JavaConfigurator : Configurator<Module, ProjectExtension> {
    override fun configure(project: Project, module: Module, extension: ProjectExtension) {
        val javaModule = module as? Module.Java ?: return

        javaModule.group?.let { project.group = it }
        javaModule.version?.let { project.version = it }

        // Test framework (JUnit 4/5, kotlin-test) is left to the module's dependencies.
        project.tasks.withType<Test> {
            testLogging.events = TestLogEvent.entries.toSet()
        }

        // Configure application plugin if mainClass is provided
        javaModule.mainClass?.let { mainClass ->
            project.pluginManager.apply(PluginIds.APPLICATION)
            project.extensions.configure<JavaApplication> {
                this.mainClass.set(mainClass)
            }
        }
    }
}

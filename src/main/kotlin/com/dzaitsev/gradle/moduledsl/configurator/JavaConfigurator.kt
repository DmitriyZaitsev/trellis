package com.dzaitsev.gradle.moduledsl.configurator

import com.dzaitsev.gradle.moduledsl.Module
import com.dzaitsev.gradle.moduledsl.PluginIds
import com.dzaitsev.gradle.moduledsl.plugin.Configurator
import com.dzaitsev.gradle.moduledsl.plugin.ProjectExtension
import com.dzaitsev.gradle.moduledsl.plugin.getJvmTarget
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Configures a plain Java module (application or library).
 *
 * Sets up group, version, Java compiler options, and test configuration.
 * If [Module.Java.mainClass] is provided, applies the `application` plugin and sets it.
 */
public class JavaConfigurator : Configurator<Module, ProjectExtension> {
    override fun configure(project: Project, module: Module, extension: ProjectExtension) {
        val javaModule = module as? Module.Java ?: return

        project.group = javaModule.group
        project.version = javaModule.version

        project.extensions.configure<JavaPluginExtension> {
            val javaVersion = JavaVersion.toVersion(extension.javaVersion.get())
            sourceCompatibility = javaVersion
            targetCompatibility = javaVersion
        }

        // Otherwise Kotlin targets the Gradle JDK and fails KGP's JVM-target consistency check.
        project.pluginManager.withPlugin(PluginIds.KOTLIN_JVM) {
            project.extensions.configure<KotlinJvmProjectExtension> {
                compilerOptions.jvmTarget.set(extension.getJvmTarget())
            }
        }

        // Configure testing
        project.tasks.withType<Test> {
            testLogging.events = TestLogEvent.entries.toSet()
            useJUnitPlatform()
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

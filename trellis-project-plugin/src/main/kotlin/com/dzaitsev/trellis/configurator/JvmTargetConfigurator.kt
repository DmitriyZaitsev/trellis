package com.dzaitsev.trellis.configurator

import com.dzaitsev.trellis.Module
import com.dzaitsev.trellis.plugin.Configurator
import com.dzaitsev.trellis.plugin.TrellisProjectExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.HasConfigurableKotlinCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin

/**
 * Targets the bytecode of every JVM compilation at [TrellisProjectExtension.javaVersion], the same way for
 * every module kind: Kotlin/JVM, KMP `jvm()` and Android targets, and AGP 9 built-in Kotlin.
 *
 * Uses `jvmTarget`/`sourceCompatibility`/`targetCompatibility` by default, so any JDK at least as
 * new as [TrellisProjectExtension.javaVersion] can build; set [TrellisProjectExtension.useJavaToolchain] to
 * compile with a JDK toolchain of that version instead. Values
 * are set on extensions (not tasks), so a module's own `build.gradle.kts` can still override them.
 * Android `compileOptions` are set separately by the Android configurators.
 */
public class JvmTargetConfigurator : Configurator<Module, TrellisProjectExtension> {
    override fun configure(project: Project, module: Module, extension: TrellisProjectExtension) {
        val jvmTarget = extension.javaVersion.map { JvmTarget.fromTarget(JavaVersion.toVersion(it).toString()) }
        val useToolchain = extension.useJavaToolchain.getOrElse(false)

        // Also drives the TargetJvmVersion attribute of published/consumed Java variants.
        project.pluginManager.withPlugin("java") {
            project.extensions.configure<JavaPluginExtension> {
                if (useToolchain) {
                    toolchain.languageVersion.set(extension.javaVersion.map(JavaLanguageVersion::of))
                } else {
                    val javaVersion = JavaVersion.toVersion(extension.javaVersion.get())
                    sourceCompatibility = javaVersion
                    targetCompatibility = javaVersion
                }
            }
        }

        project.plugins.withType<KotlinBasePlugin>().configureEach {
            val kotlin = project.extensions.findByType(KotlinProjectExtension::class.java) ?: return@configureEach
            if (useToolchain) kotlin.jvmToolchain(extension.javaVersion.get())

            when (kotlin) {
                is KotlinMultiplatformExtension -> kotlin.targets.configureEach {
                    jvmCompilerOptions()?.jvmTarget?.set(jvmTarget)
                }

                else -> kotlin.jvmCompilerOptions()?.jvmTarget?.set(jvmTarget)
            }
        }
    }

    private fun Any.jvmCompilerOptions(): KotlinJvmCompilerOptions? =
        (this as? HasConfigurableKotlinCompilerOptions<*>)?.compilerOptions as? KotlinJvmCompilerOptions
}

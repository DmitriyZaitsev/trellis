package com.dzaitsev.trellis.configurator

import com.dzaitsev.trellis.Module
import com.dzaitsev.trellis.plugin.Configurator
import com.dzaitsev.trellis.plugin.TrellisProjectExtension
import com.dzaitsev.trellis.plugin.toKotlinVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.HasConfigurableKotlinCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin

/**
 * Sets the Kotlin language and API version of every Kotlin module (JVM, Multiplatform, and Android
 * with AGP built-in Kotlin) from [TrellisProjectExtension.kotlinVersion]. When it is unset, the
 * compiler's own default applies. Set as conventions, so a module script can still override them.
 */
public class KotlinVersionConfigurator : Configurator<TrellisProjectExtension> {
    override fun configure(project: Project, module: Module, extension: TrellisProjectExtension) {
        val version = extension.kotlinVersion.map { it.toKotlinVersion() }

        project.plugins.withType<KotlinBasePlugin>().configureEach {
            val kotlin = project.extensions.findByType<KotlinProjectExtension>() as? HasConfigurableKotlinCompilerOptions<*>
                ?: return@configureEach
            kotlin.compilerOptions.languageVersion.convention(version)
            kotlin.compilerOptions.apiVersion.convention(version)
        }
    }
}

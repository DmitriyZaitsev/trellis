package com.dzaitsev.trellis.plugin

import com.dzaitsev.trellis.configurator.defaultProjectModuleConfigurators
import org.gradle.api.Plugin
import org.gradle.api.Project

public class TrellisLocalPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val holder = project.extensions.getByType(ModuleRegistryHolder::class.java)
        val config = requireNotNull(holder.projectConventions) {
            "Declare trellis { projectConventions { ... } } in settings before applying com.dzaitsev.trellis.local"
        }
        val module = project.findModule() ?: return
        val extension = project.extensions.create("trellis", ProjectExtension::class.java)
        extension.kotlinVersion.set(config.kotlinVersion)
        extension.javaVersion.set(config.javaVersion)
        extension.androidMinSdk.set(config.androidMinSdk)
        extension.androidTargetSdk.set(config.androidTargetSdk)
        extension.useJavaToolchain.set(config.useJavaToolchain)
        defaultProjectModuleConfigurators<ProjectExtension>(module).forEach {
            it.configure(project, module, extension)
        }
    }
}
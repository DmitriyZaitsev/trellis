package com.dzaitsev.trellis.plugin

import com.dzaitsev.trellis.configurator.defaultProjectModuleConfigurators
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType

public class TrellisLocalPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val holder = project.extensions.getByType<ModuleRegistryHolder>()
        val config = requireNotNull(holder.projectConventions) {
            "Declare trellis { projectConventions { ... } } in settings before applying com.dzaitsev.trellis.local"
        }
        val module = project.findModule() ?: return
        val extension = project.extensions.create<TrellisProjectExtension>("trellis")
        extension.useSettingsConventions(config)
        defaultProjectModuleConfigurators<TrellisProjectExtension>(module).forEach {
            it.configure(project, module, extension)
        }
    }
}

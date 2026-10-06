package com.dzaitsev.trellis.plugin

import com.dzaitsev.trellis.Module
import com.dzaitsev.trellis.configurator.defaultProjectModuleConfigurators
import org.gradle.api.Project

/**
 * Project plugin `com.dzaitsev.trellis.conventions`, for builds that need only the built-in
 * configurators: apply it to the root project and set the `trellis { }` extension. Subclass
 * [ProjectPlugin] directly to add project-specific [Configurator]s.
 */
public class TrellisConventionsPlugin : ProjectPlugin<Module, ProjectExtension>(
    extensionName = "trellis",
    extensionType = ProjectExtension::class,
) {
    override fun moduleOf(project: Project): Module? = project.findModule()

    override fun immediateConfigurators(
        module: Module,
        extension: ProjectExtension,
    ): List<Configurator<Module, ProjectExtension>> =
        defaultProjectModuleConfigurators(module)
}

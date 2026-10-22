package com.dzaitsev.trellis.plugin

import com.dzaitsev.trellis.Module
import com.dzaitsev.trellis.configurator.defaultProjectModuleConfigurators
import org.gradle.api.Project

/**
 * Project plugin `com.dzaitsev.trellis.conventions`, for builds that need only the built-in
 * configurators: apply it to the root project and set the `trellis { }` extension. Subclass
 * [TrellisProjectPlugin] directly to add project-specific [Configurator]s.
 */
public class TrellisConventionsPlugin :
    TrellisProjectPlugin<Module, TrellisProjectExtension>(
        extensionName = "trellis",
        extensionType = TrellisProjectExtension::class,
    ) {
    override fun moduleOf(project: Project): Module? = project.findModule()

    override fun immediateConfigurators(
        module: Module,
        extension: TrellisProjectExtension,
    ): List<Configurator<Module, TrellisProjectExtension>> = defaultProjectModuleConfigurators()
}

package com.dzaitsev.trellis.plugin

import com.dzaitsev.trellis.ModuleModel
import org.gradle.api.Project

/**
 * One discrete configuration step applied to [project], given its resolved [module] and the
 * plugin's own [extension]. Implementations are typically stateless and reusable across
 * projects; anything project-specific should be read from [project]/[module]/[extension]
 * rather than captured at construction time.
 *
 * Generic over [M] (the module model) as well as [E] (the extension type): a consumer using
 * this library's own `Module` hierarchy writes `Configurator<Module, MyExtension>`,
 * but a consumer with a wholly different module model can implement [ModuleModel] directly and
 * write `Configurator` against that type instead.
 */
public fun interface Configurator<in M : ModuleModel, in E : TrellisProjectExtension> {
    public fun configure(project: Project, module: M, extension: E)
}

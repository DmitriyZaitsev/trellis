package com.dzaitsev.trellis.plugin

import com.dzaitsev.trellis.Module
import org.gradle.api.Project

/**
 * One discrete configuration step applied to [project], given its declared [module] and the
 * plugin's [extension]. Implementations are typically stateless and decide from [module] whether
 * they apply, returning early otherwise; read anything project-specific from the arguments rather
 * than capturing it at construction time.
 */
public fun interface Configurator<in E : TrellisProjectExtension> {
    public fun configure(project: Project, module: Module, extension: E)
}

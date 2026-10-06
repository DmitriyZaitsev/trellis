package com.dzaitsev.trellis

/**
 * The minimal shape [ProjectPlugin][com.dzaitsev.trellis.plugin.ProjectPlugin]
 * and [Configurator][com.dzaitsev.trellis.plugin.Configurator] require of a declared module.
 *
 * [Module] is this library's own built-in implementation, with the module kinds and the
 * [com.dzaitsev.trellis.dsl.ModuleRegistry]/[moduleRegistry] DSL to declare them. A consumer that needs a fundamentally
 * different module model (not Android/KMP shaped at all) can instead implement [ModuleModel]
 * directly and still reuse the plugin base and phase pipeline.
 */
public interface ModuleModel {
    public val name: String
}

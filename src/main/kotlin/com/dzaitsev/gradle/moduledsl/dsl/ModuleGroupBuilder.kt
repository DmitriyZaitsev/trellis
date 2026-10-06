package com.dzaitsev.gradle.moduledsl.dsl

import com.dzaitsev.gradle.moduledsl.Module

@ModuleDslMarker
public class ModuleGroupBuilder internal constructor(
    private val prefix: ProjectPath,
) : ModuleContainerDsl() {
    private val modules = mutableListOf<ModuleDeclaration>()

    override fun custom(module: Module) {
        require(module !is Module.Root) {
            "Root module '${module.name}' cannot be declared inside group '$prefix'"
        }
        modules += ModuleDeclaration(
            path = prefix.child(module.name),
            module = module,
        )
    }

    internal fun build(): List<ModuleDeclaration> =
        modules.toList()
}

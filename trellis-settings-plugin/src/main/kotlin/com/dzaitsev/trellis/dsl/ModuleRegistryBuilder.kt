package com.dzaitsev.trellis.dsl

import com.dzaitsev.trellis.Module

@TrellisDslMarker
public class ModuleRegistryBuilder : ModuleContainerDsl() {
    private val modules = mutableListOf<ModuleDeclaration>()
    internal var missingDirectoryPolicy: ModuleRegistryOptions.MissingDirectoryPolicy = ModuleRegistryOptions.MissingDirectoryPolicy.FAIL

    override fun custom(module: Module) {
        modules += ModuleDeclaration(
            path = if (module is Module.Root) ProjectPath.ROOT else ProjectPath(listOf(module.name)),
            module = module,
        )
    }

    public fun group(
        name: String,
        block: ModuleGroupBuilder.() -> Unit,
    ) {
        modules += ModuleGroupBuilder(
            prefix = ProjectPath(listOf(name)),
        ).apply(block).build()
    }

    public fun createMissingDirectories() {
        missingDirectoryPolicy = ModuleRegistryOptions.MissingDirectoryPolicy.CREATE
    }

    internal fun build(): ModuleRegistry =
        ModuleRegistry(
            modules = modules.toList(),
            moduleRegistryOptions = ModuleRegistryOptions(missingDirectoryPolicy),
        )
}

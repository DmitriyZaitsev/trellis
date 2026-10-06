package com.dzaitsev.gradle.moduledsl.dsl

import com.dzaitsev.gradle.moduledsl.Module

/**
 * The set of [com.dzaitsev.gradle.moduledsl.Module]s declared for a build, keyed by [com.dzaitsev.gradle.moduledsl.ModuleModel.name].
 *
 * Build one with the [moduleRegistry] DSL. Consumers typically expose their own
 * `fun Project.module(): Module = registry[name]` extension over a single top-level
 * registry instance.
 */
public class ModuleRegistry internal constructor(
    internal val modules: List<ModuleDeclaration>,
    internal val moduleRegistryOptions: ModuleRegistryOptions,
) {
    init {
        val duplicatePaths = modules
            .filterNot { it.path.isRoot }
            .groupingBy { it.path }
            .eachCount()
            .filterValues { it > 1 }
            .keys

        require(duplicatePaths.isEmpty()) {
            "Duplicate Gradle project paths: " +
                duplicatePaths.joinToString { it.gradlePath }
        }

        val duplicateRootNames = roots
            .groupingBy { it.name }
            .eachCount()
            .filterValues { it > 1 }
            .keys

        require(duplicateRootNames.isEmpty()) {
            "Duplicate root module names: " + duplicateRootNames.joinToString()
        }
    }

    /**
     * Root modules. Usually at most one; several are allowed (with distinct names) for a shared
     * module set included into different host builds, each with its own root project name.
     */
    public val roots: List<Module>
        get() = modules.filter { it.path.isRoot }.map { it.module }

    public val byPath: Map<ProjectPath, ModuleDeclaration>
        get() = modules.filterNot { it.path.isRoot }.associateBy { it.path }

    public operator fun get(value: String): Module =
        if (value.startsWith(":")) {
            getByPath(ProjectPath.parse(value))
        } else {
            getByName(value)
        }

    public operator fun get(path: ProjectPath): Module =
        getByPath(path)

    public fun getByName(name: String): Module {
        val matches = modules.filter { it.module.name == name }

        return when (matches.size) {
            0 -> error("Unknown module: $name")
            1 -> matches.single().module
            else -> error(
                "Ambiguous module name '$name'. " +
                    "Matching project paths: " +
                    matches.joinToString { it.path.gradlePath },
            )
        }
    }

    public fun getByPath(path: ProjectPath): Module =
        findByPath(path)
            ?: error("Unknown Gradle project path: ${path.gradlePath}")

    /**
     * The module declared at [path], or `null` if none is. For [ProjectPath.ROOT] this is the
     * single declared root module, if exactly one is declared; use [findRoot] to pick one of
     * several by root project name.
     */
    public fun findByPath(path: ProjectPath): Module? =
        if (path.isRoot) {
            roots.singleOrNull()
        } else {
            modules.firstOrNull { it.path == path }?.module
        }

    /** The root module named [rootProjectName], falling back to the single declared root module. */
    public fun findRoot(rootProjectName: String): Module? =
        roots.firstOrNull { it.name == rootProjectName } ?: roots.singleOrNull()

    override fun equals(other: Any?): Boolean =
        other is ModuleRegistry &&
            modules == other.modules &&
            moduleRegistryOptions == other.moduleRegistryOptions

    override fun hashCode(): Int = 31 * modules.hashCode() + moduleRegistryOptions.hashCode()
}

public data class ModuleRegistryOptions(
    public val missingDirectoryPolicy: MissingDirectoryPolicy = MissingDirectoryPolicy.FAIL,
) {
    public enum class MissingDirectoryPolicy {
        FAIL,
        CREATE,
    }
}

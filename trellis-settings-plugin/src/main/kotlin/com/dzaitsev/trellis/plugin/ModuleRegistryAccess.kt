package com.dzaitsev.trellis.plugin

import com.dzaitsev.trellis.Module
import com.dzaitsev.trellis.dsl.ModuleRegistry
import com.dzaitsev.trellis.dsl.ProjectPath
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle

internal const val MODULE_REGISTRY_HOLDER_NAME = "com.dzaitsev.trellis.moduleRegistry"

/** The [ModuleRegistry] declared in this build's `settings.gradle.kts` via [TrellisSettingsPlugin]. */
public fun Gradle.moduleRegistry(): ModuleRegistry =
    registryFrom(extensions.findByName(MODULE_REGISTRY_HOLDER_NAME), rootProject.name)

/** Like [Gradle.moduleRegistry], but Isolated Projects compatible. */
public fun Project.moduleRegistry(): ModuleRegistry =
    registryFrom(extensions.findByName(MODULE_REGISTRY_HOLDER_NAME), path)

private fun registryFrom(holder: Any?, owner: String): ModuleRegistry =
    when (holder) {
        is ModuleRegistryHolder -> holder.registry
        null -> error(
            "No module registry found for '$owner'. Apply the 'com.dzaitsev.trellis' " +
                "plugin in settings.gradle.kts.",
        )
        // Same class name, different class loader: the plugin artifact is on two classpaths.
        else -> error(
            "Module registry holder was loaded by a different class loader " +
                "(${holder.javaClass.classLoader}). Make sure the Trellis artifacts are only " +
                "added to the settings classpath, not again to build-logic/buildSrc at runtime.",
        )
    }

/**
 * The module declared for this project, or `null` for projects absent from the registry (e.g.
 * implicit intermediate projects like `:feature` or projects added with a plain `include`).
 */
public fun Project.findModule(): Module? {
    val registry = moduleRegistry()
    return if (path == ProjectPath.ROOT.gradlePath) registry.findRoot(name) else registry.findByPath(
        ProjectPath(path)
    )
}

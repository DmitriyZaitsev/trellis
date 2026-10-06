package com.dzaitsev.trellis.plugin

import com.dzaitsev.trellis.dsl.ModuleRegistry
import com.dzaitsev.trellis.dsl.ModuleRegistryOptions.MissingDirectoryPolicy
import com.dzaitsev.trellis.dsl.ProjectPath
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.kotlin.dsl.add
import org.gradle.kotlin.dsl.create
import java.nio.file.Files
import java.nio.file.Path

/** Settings plugin `com.dzaitsev.trellis`: the `trellis { }` module declarations. */
public class TrellisSettingsPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) {
        val extension = settings.extensions.create<SettingsExtension>("trellis")

        settings.gradle.settingsEvaluated {
            val registry = extension.buildRegistry()

            registerProjects(
                settings = settings,
                registry = registry,
            )

            publishRegistry(
                settings = settings,
                registry = registry,
            )

            // Projects may not read Gradle.extensions under Isolated Projects; give each its own copy.
            val holder = ModuleRegistryHolder(registry, extension.conventions?.snapshot())
            settings.gradle.lifecycle.beforeProject {
                extensions.add(ModuleRegistryHolder::class.java, MODULE_REGISTRY_HOLDER_NAME, holder)
            }
        }
    }

    private fun registerProjects(
        settings: Settings,
        registry: ModuleRegistry,
    ) {
        val rootDirectory = settings.settingsDir.toPath()
            .toAbsolutePath()
            .normalize()

        registry.modules.filterNot { it.path.isRoot }.forEach { declaration ->
            settings.include(declaration.path.gradlePath)
            val projectDirectory = settings.project(declaration.path.gradlePath).projectDir.toPath()
                .toAbsolutePath()
                .normalize()

            require(projectDirectory.startsWith(rootDirectory)) {
                "Project path escapes settings directory: " +
                    declaration.path.gradlePath
            }

            ensureDirectory(
                directory = projectDirectory,
                path = declaration.path,
                policy = registry
                    .moduleRegistryOptions
                    .missingDirectoryPolicy,
            )
        }
    }

    private fun ensureDirectory(
        directory: Path,
        path: ProjectPath,
        policy: MissingDirectoryPolicy,
    ) {
        when (policy) {
            MissingDirectoryPolicy.FAIL -> {
                require(Files.isDirectory(directory)) {
                    "Project directory does not exist for " +
                        "${path.gradlePath}: $directory"
                }
            }

            MissingDirectoryPolicy.CREATE -> {
                Files.createDirectories(directory)
            }
        }

        require(Files.isDirectory(directory)) {
            "Project path is not a directory: " +
                "${path.gradlePath}: $directory"
        }

        require(Files.isWritable(directory)) {
            "Project directory is not writable for " +
                "${path.gradlePath}: $directory"
        }
    }

    private fun publishRegistry(
        settings: Settings,
        registry: ModuleRegistry,
    ) {
        val extensions = settings.gradle.extensions
        when (val existing = extensions.findByName(MODULE_REGISTRY_HOLDER_NAME)) {
            null -> {
                extensions.add<ModuleRegistryHolder>(
                    MODULE_REGISTRY_HOLDER_NAME,
                    ModuleRegistryHolder(registry),
                )
            }

            is ModuleRegistryHolder if existing.registry == registry -> {
                // Idempotent repeated registration.
            }

            is ModuleRegistryHolder -> {
                error(
                    "A different module registry is already registered " +
                        "under '$MODULE_REGISTRY_HOLDER_NAME'",
                )
            }

            else -> {
                error(
                    "Gradle extension '$MODULE_REGISTRY_HOLDER_NAME' " +
                        "is already registered with an unexpected type: " +
                        existing::class.qualifiedName,
                )
            }
        }
    }
}

public class ModuleRegistryHolder(
    public val registry: ModuleRegistry,
) {
    public var projectConventions: ProjectConventions? = null
        private set

    public constructor(registry: ModuleRegistry, projectConventions: ProjectConventions?) : this(registry) {
        this.projectConventions = projectConventions
    }
}

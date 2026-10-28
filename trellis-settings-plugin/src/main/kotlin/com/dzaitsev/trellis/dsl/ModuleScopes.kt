package com.dzaitsev.trellis.dsl

import com.dzaitsev.trellis.android.Flavor

/**
 * Options shared by every module kind that has a block, so presets can target all of them:
 * `fun ModuleOptionsScope.uiDefaults() { usesCompose = true }`.
 */
public interface ModuleOptionsScope {
    /** Applies the Compose compiler plugin, and Compose Multiplatform on `java` and `kmp` modules. */
    public var usesCompose: Boolean

    /** Enables Kotlin `explicitApi()` mode. */
    public var hasExplicitApi: Boolean
}

/** Options of a `kmp` module, declared alongside its targets. */
@TrellisDslMarker
public class KmpModuleScope internal constructor() :
    KmpTargetsScope(),
    ModuleOptionsScope {
    override var usesCompose: Boolean = false
    override var hasExplicitApi: Boolean = false

    /** Set to `false` when the module's own script registers its targets. */
    public var autoConfigureTargets: Boolean = true

    /** Bundles the frameworks of all Apple targets into one XCFramework; they must share a base name. */
    public var xcframework: Boolean = false
}

/** Options of a `java` module. */
@TrellisDslMarker
public class JavaModuleScope internal constructor() : ModuleOptionsScope {
    public var group: String? = null
    public var version: String? = null

    /** Applies the `application` plugin with this entry point when set. */
    public var mainClass: String? = null
    override var usesCompose: Boolean = false
    override var hasExplicitApi: Boolean = false
}

/** Options of an `androidApp`/`androidLibrary` module. */
@TrellisDslMarker
public class AndroidModuleScope internal constructor() : ModuleOptionsScope {
    override var usesCompose: Boolean = false
    override var hasExplicitApi: Boolean = false

    private val flavorsScope = FlavorsScope()

    /** Product flavors; dimensions are prioritized in declaration order. */
    public fun flavors(block: FlavorsScope.() -> Unit) {
        flavorsScope.apply(block)
    }

    internal fun buildFlavors(): List<Flavor> = flavorsScope.build()
}

@TrellisDslMarker
public class FlavorsScope internal constructor() {
    private val flavors = mutableListOf<Flavor>()

    public fun dimension(name: String, block: DimensionScope.() -> Unit) {
        require(flavors.none { it.dimension == name }) { "Flavor dimension '$name' is declared more than once" }
        val declared = DimensionScope(name).apply(block).build()
        require(declared.isNotEmpty()) { "Flavor dimension '$name' declares no flavors" }
        declared.forEach { flavor ->
            require(flavors.none { it.name == flavor.name }) { "Flavor '${flavor.name}' is declared more than once" }
            flavors += flavor
        }
    }

    internal fun build(): List<Flavor> = flavors.toList()
}

@TrellisDslMarker
public class DimensionScope internal constructor(private val dimension: String) {
    private val flavors = mutableListOf<Flavor>()

    public fun flavor(name: String, isDefault: Boolean = false, block: FlavorScope.() -> Unit = {}) {
        val scope = FlavorScope().apply(block)
        flavors += Flavor(name, dimension, isDefault, scope.applicationIdSuffix)
    }

    internal fun build(): List<Flavor> = flavors.toList()
}

@TrellisDslMarker
public class FlavorScope internal constructor() {
    /** Ignored for library modules, which AGP does not allow to set it. */
    public var applicationIdSuffix: String? = null
}

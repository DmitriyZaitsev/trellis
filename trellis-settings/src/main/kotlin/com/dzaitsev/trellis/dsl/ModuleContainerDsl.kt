package com.dzaitsev.trellis.dsl

import com.dzaitsev.trellis.KmpTarget
import com.dzaitsev.trellis.KmpTarget.DesktopHost
import com.dzaitsev.trellis.KmpTarget.IosHost
import com.dzaitsev.trellis.KmpTarget.JsEnvironment
import com.dzaitsev.trellis.Module
import com.dzaitsev.trellis.Module.AndroidApp
import com.dzaitsev.trellis.Module.AndroidLibrary
import com.dzaitsev.trellis.Module.Java
import com.dzaitsev.trellis.Module.KmpLibrary
import com.dzaitsev.trellis.Module.Root
import com.dzaitsev.trellis.android.AndroidAttrs.Kmp
import com.dzaitsev.trellis.android.AndroidAttrs.Native
import com.dzaitsev.trellis.android.Flavor

@ModuleDslMarker
public abstract class ModuleContainerDsl {
    public abstract fun custom(module: Module)

    // Members rather than top-level declarations so settings scripts need no imports.

    public val jvm: KmpTarget.Jvm get() = KmpTarget.Jvm
    public val browser: JsEnvironment get() = JsEnvironment.BROWSER
    public val nodeJs: JsEnvironment get() = JsEnvironment.NODE_JS
    public val macosArm64: DesktopHost get() = DesktopHost.MACOS_ARM64
    public val linuxX64: DesktopHost get() = DesktopHost.LINUX_X64
    public val mingwX64: DesktopHost get() = DesktopHost.MINGW_X64
    public val iosArm64: IosHost get() = IosHost.IOS_ARM64
    public val iosSimulatorArm64: IosHost get() = IosHost.IOS_SIMULATOR_ARM64
    public val iosX64: IosHost get() = IosHost.IOS_X64

    public fun android(
        namespace: String,
        enableResources: Boolean = false,
        hostTests: Boolean = true,
    ): KmpTarget.Android = KmpTarget.Android(Kmp(namespace, enableResources, hostTests))

    public fun ios(framework: KmpTarget.Ios.Framework? = null): KmpTarget.Ios = KmpTarget.Ios(framework)

    /** iOS targets; no [hosts] means [IosHost.DEFAULT]. */
    public fun ios(vararg hosts: IosHost, framework: KmpTarget.Ios.Framework? = null): KmpTarget.Ios =
        KmpTarget.Ios(framework, hosts.toSet().ifEmpty { IosHost.DEFAULT })

    public fun framework(
        baseName: String,
        export: List<String> = emptyList(),
        isStatic: Boolean = true,
        freeCompilerArgs: List<String> = KmpTarget.Ios.Framework.DEFAULT_FREE_COMPILER_ARGS,
        releaseFreeCompilerArgs: List<String> = KmpTarget.Ios.Framework.DEFAULT_RELEASE_FREE_COMPILER_ARGS,
    ): KmpTarget.Ios.Framework =
        KmpTarget.Ios.Framework(baseName, export, isStatic, freeCompilerArgs, releaseFreeCompilerArgs)

    /** A Kotlin/JS target; no [environments] means all of them. */
    public fun js(vararg environments: JsEnvironment, isApplication: Boolean = false): KmpTarget.Js =
        KmpTarget.Js(isApplication, environments.toSet().ifEmpty { JsEnvironment.entries.toSet() })

    /** A Kotlin/Wasm target; no [environments] means all of them. */
    public fun wasm(vararg environments: JsEnvironment, isApplication: Boolean = false): KmpTarget.Wasm =
        KmpTarget.Wasm(isApplication, environments.toSet().ifEmpty { JsEnvironment.entries.toSet() })

    /** Desktop native targets; no [hosts] means all of them. */
    public fun desktop(vararg hosts: DesktopHost, isApplication: Boolean = false): KmpTarget.Desktop =
        KmpTarget.Desktop(isApplication, hosts.toSet().ifEmpty { DesktopHost.entries.toSet() })

    public fun root(
        name: String,
        hasExplicitApi: Boolean = false,
    ) {
        custom(
            Root(
                name = name,
                hasExplicitApi = hasExplicitApi,
            ),
        )
    }

    public fun androidApp(
        name: String,
        attrs: Native,
        hasExplicitApi: Boolean = false,
    ) {
        custom(
            AndroidApp(
                name = name,
                androidAttrs = attrs,
                hasExplicitApi = hasExplicitApi,
            ),
        )
    }

    public fun androidApp(
        name: String,
        namespace: String,
        usesCompose: Boolean = false,
        flavors: List<Flavor> = emptyList(),
        hasExplicitApi: Boolean = false,
    ) {
        androidApp(name, Native(namespace, usesCompose, flavors), hasExplicitApi)
    }

    public fun androidLibrary(
        name: String,
        namespace: String,
        usesCompose: Boolean = false,
        flavors: List<Flavor> = emptyList(),
        hasExplicitApi: Boolean = true,
    ) {
        androidLibrary(name, Native(namespace, usesCompose, flavors), hasExplicitApi)
    }

    public fun androidLibrary(
        name: String,
        attrs: Native,
        hasExplicitApi: Boolean = true,
    ) {
        custom(
            AndroidLibrary(
                name = name,
                androidAttrs = attrs,
                hasExplicitApi = hasExplicitApi,
            ),
        )
    }

    public fun java(
        name: String,
        group: String? = null,
        version: String? = null,
        mainClass: String? = null,
        hasExplicitApi: Boolean = mainClass == null,
        usesCompose: Boolean = false,
    ) {
        custom(
            Java(
                name = name,
                group = group,
                version = version,
                mainClass = mainClass,
                hasExplicitApi = hasExplicitApi,
                usesCompose = usesCompose,
            ),
        )
    }

    public fun kmp(
        name: String,
        targets: Set<KmpTarget>,
        hasExplicitApi: Boolean = true,
        autoConfigureTargets: Boolean = true,
        usesCompose: Boolean = false,
    ) {
        custom(
            KmpLibrary(
                name = name,
                targets = targets,
                hasExplicitApi = hasExplicitApi,
                autoConfigureTargets = autoConfigureTargets,
                usesCompose = usesCompose,
            ),
        )
    }

    public fun kmp(
        name: String,
        target: KmpTarget,
        hasExplicitApi: Boolean = true,
        autoConfigureTargets: Boolean = true,
        usesCompose: Boolean = false,
    ) {
        kmp(
            name = name,
            targets = setOf(target),
            hasExplicitApi = hasExplicitApi,
            autoConfigureTargets = autoConfigureTargets,
            usesCompose = usesCompose,
        )
    }
}

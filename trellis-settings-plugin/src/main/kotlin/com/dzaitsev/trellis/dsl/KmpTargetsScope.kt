package com.dzaitsev.trellis.dsl

import com.dzaitsev.trellis.KmpTarget
import com.dzaitsev.trellis.KmpTarget.Android
import com.dzaitsev.trellis.KmpTarget.Desktop
import com.dzaitsev.trellis.KmpTarget.Desktop.DesktopHost
import com.dzaitsev.trellis.KmpTarget.Ios
import com.dzaitsev.trellis.KmpTarget.Ios.Framework
import com.dzaitsev.trellis.KmpTarget.Ios.IosHost
import com.dzaitsev.trellis.KmpTarget.Js
import com.dzaitsev.trellis.KmpTarget.JsEnv
import com.dzaitsev.trellis.KmpTarget.Wasm
import com.dzaitsev.trellis.android.AndroidAttrs.Kmp

@TrellisDslMarker
public object KmpTargetsScope {
    // JVM scope
    public val jvm: KmpTarget.Jvm = KmpTarget.Jvm

    public fun android(namespace: String, enableResources: Boolean = false, hostTests: Boolean = true): Android =
        Android(Kmp(namespace, enableResources, hostTests))

    public fun ios(framework: Framework? = null): Ios = Ios(framework)

    public fun ios(framework: Framework? = null, hosts: IosTargetsScope.() -> Set<IosHost>): Ios =
        Ios(framework, IosTargetsScope.run(hosts))

    public fun js(vararg environments: JsEnv, isApplication: Boolean = false): Js = Js(
        isApplication,
        environments.toSet().ifEmpty {
            JsEnv.entries.toSet()
        },
    )

    public fun js(isApplication: Boolean = false, environments: JsTargetsScope.() -> Set<JsEnv>): Js =
        Js(isApplication, JsTargetsScope.run(environments))

    public fun wasm(vararg environments: JsEnv, isApplication: Boolean = false): Wasm = Wasm(
        isApplication,
        environments.toSet().ifEmpty {
            JsEnv.entries.toSet()
        },
    )

    public fun wasm(isApplication: Boolean = false, environments: JsTargetsScope.() -> Set<JsEnv>): Wasm =
        Wasm(isApplication, JsTargetsScope.run(environments))

    public fun desktop(vararg hosts: DesktopHost, isApplication: Boolean = false): Desktop = Desktop(
        isApplication,
        hosts.toSet().ifEmpty {
            DesktopHost.entries.toSet()
        },
    )

    public fun desktop(isApplication: Boolean = false, hosts: DesktopTargetsScope.() -> Set<DesktopHost>): Desktop =
        Desktop(isApplication, DesktopTargetsScope.run(hosts))

    @TrellisDslMarker
    public object JsTargetsScope {
        public val browser: JsEnv = JsEnv.BROWSER
        public val nodeJs: JsEnv = JsEnv.NODE_JS
    }

    @TrellisDslMarker
    public object IosTargetsScope {
        public val iosArm64: IosHost = IosHost.IOS_ARM64
        public val iosSimulatorArm64: IosHost = IosHost.IOS_SIMULATOR_ARM64
        public val iosX64: IosHost = IosHost.IOS_X64
    }

    @TrellisDslMarker
    public object DesktopTargetsScope {
        public val macosArm64: DesktopHost = DesktopHost.MACOS_ARM64
        public val linuxX64: DesktopHost = DesktopHost.LINUX_X64
        public val mingwX64: DesktopHost = DesktopHost.MINGW_X64
    }
}

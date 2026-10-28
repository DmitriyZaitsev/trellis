package com.dzaitsev.trellis.dsl

import com.dzaitsev.trellis.KmpTarget
import com.dzaitsev.trellis.KmpTarget.Framework
import com.dzaitsev.trellis.KmpTarget.Ios
import com.dzaitsev.trellis.KmpTarget.Ios.IosHost
import com.dzaitsev.trellis.KmpTarget.Js
import com.dzaitsev.trellis.KmpTarget.JsEnv
import com.dzaitsev.trellis.Module
import com.dzaitsev.trellis.android.Flavor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModuleRegistryTest {
    @Test
    fun `root path parses to ROOT`() {
        assertEquals(ProjectPath.ROOT, ProjectPath(":"))
        assertEquals(":", ProjectPath.ROOT.gradlePath)
        assertEquals(":a:b", ProjectPath(":a:b").gradlePath)
        assertEquals("a/b", ProjectPath("a:b").relativeDirectory)
    }

    @Test
    fun `invalid path segments are rejected`() {
        assertFailsWith<IllegalArgumentException> { ProjectPath(":a/b") }
        assertFailsWith<IllegalArgumentException> { ProjectPath(":a::b") }
        assertFailsWith<IllegalArgumentException> { ProjectPath(" ") }
    }

    @Test
    fun `root module maps to the root path, not a subproject`() {
        val registry =
            moduleRegistry {
                root("app-root")
                java("server")
            }

        assertEquals(
            listOf(ProjectPath.ROOT, ProjectPath(":server")),
            registry.modules.map {
                it.path
            },
        )
        assertEquals(
            Module.Root("app-root"),
            registry.findByPath(ProjectPath.ROOT),
        )
        assertEquals("app-root", registry[":"].name)
    }

    @Test
    fun `several roots resolve by root project name`() {
        val registry =
            moduleRegistry {
                root("android")
                root("ios")
            }

        assertEquals("ios", registry.findRoot("ios")?.name)
        assertNull(registry.findRoot("other"))
        assertNull(registry.findByPath(ProjectPath.ROOT))
    }

    @Test
    fun `nested groups build nested paths`() {
        val registry =
            moduleRegistry {
                group("feature") {
                    group("auth") {
                        kmp("api") { jvm() }
                    }
                    kmp("profile") { jvm() }
                }
            }

        assertEquals(
            listOf(":feature:auth:api", ":feature:profile"),
            registry.modules.map { it.path.gradlePath },
        )
        assertEquals("api", registry[":feature:auth:api"].name)
    }

    @Test
    fun `undeclared paths resolve to null`() {
        val registry =
            moduleRegistry { group("feature") { kmp("auth") { jvm() } } }

        assertNull(registry.findByPath(ProjectPath(":feature")))
        assertNull(registry.findByPath(ProjectPath.ROOT))
    }

    @Test
    fun `root inside a group is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            moduleRegistry { group("g") { root("r") } }
        }
    }

    @Test
    fun `duplicate project paths are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            moduleRegistry {
                java("a")
                kmp("a") { jvm() }
            }
        }
    }

    @Test
    fun `kmp module without targets is rejected`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                moduleRegistry { kmp("a") { } }
            }
        assertTrue("declares no targets" in error.message.orEmpty())
    }

    @Test
    fun `repeated target kinds are rejected`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                moduleRegistry {
                    kmp("a") {
                        js(browser)
                        js(nodeJs)
                    }
                }
            }
        assertTrue("js" in error.message.orEmpty())
    }

    @Test
    fun `differently configured targets of the same kind are repeated kinds`() {
        assertFailsWith<IllegalArgumentException> {
            moduleRegistry {
                kmp("a") {
                    ios()
                    ios { framework("Other") }
                }
            }
        }
    }

    @Test
    fun `hierarchy group targets are rejected`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                moduleRegistry {
                    custom(Module.KmpLibrary("a", setOf(KmpTarget.Jvm, com.dzaitsev.trellis.Apple)))
                }
            }
        assertTrue("apple" in error.message.orEmpty())
    }

    @Test
    fun `targets follow the hierarchy`() {
        val targets =
            targetsOf {
                ios()
                macos()
                linux()
                androidNative()
                tvos()
                watchos()
                js()
                wasmJs()
                jvm()
            }.associateBy { it.name }

        assertTrue(targets.getValue("ios") is KmpTarget.Apple)
        assertTrue(targets.getValue("macos") is KmpTarget.Apple)
        assertTrue(targets.getValue("linux") is KmpTarget.Native)
        assertTrue(targets.getValue("androidNative") is KmpTarget.Native)
        assertTrue(targets.getValue("tvos") is KmpTarget.Apple)
        assertTrue(targets.getValue("watchos") is KmpTarget.Apple)
        assertTrue(targets.getValue("js") is KmpTarget.Web)
        assertTrue(targets.getValue("wasmJs") is KmpTarget.Web)
        assertTrue(targets.getValue("jvm") !is KmpTarget.Native)
    }

    @Test
    fun `framework exports must be declared`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                moduleRegistry {
                    kmp("umbrella") {
                        ios(iosArm64) { framework("Shared") { export(":missing") } }
                    }
                }
            }
        assertTrue(":missing" in error.message.orEmpty())

        moduleRegistry {
            kmp("domain") { ios() }
            kmp("umbrella") {
                ios(iosArm64) { framework("Shared") { export(":domain") } }
            }
        }
    }

    @Test
    fun `framework scope builds a framework with defaults and overrides`() {
        val default = targetsOf { ios { framework("Shared") } }.single() as Ios
        assertEquals(Framework("Shared"), default.framework)

        val custom =
            targetsOf {
                ios {
                    framework("Shared") {
                        export(":a", ":b")
                        isStatic = false
                    }
                }
            }.single() as Ios
        assertEquals(Framework("Shared", export = listOf(":a", ":b"), isStatic = false), custom.framework)
        assertEquals(null, (targetsOf { ios() }.single() as Ios).framework)
        assertFailsWith<IllegalStateException> {
            targetsOf {
                ios {
                    framework("A")
                    framework("B")
                }
            }
        }
    }

    @Test
    fun `web targets default to all environments`() {
        val web = targetsOf {
            js(browser, isApplication = true)
            wasmJs()
        }.toList()
        assertEquals(Js(isApplication = true, environments = setOf(JsEnv.BROWSER)), web[0])
        assertEquals(JsEnv.DEFAULT, (web[1] as KmpTarget.WasmJs).environments)
        assertEquals(JsEnv.DEFAULT, (targetsOf { js() }.single() as Js).environments)
    }

    @Test
    fun `desktop targets default to their hosts and honor selections`() {
        val macos = targetsOf { macos() }.single() as KmpTarget.Macos
        assertEquals(setOf(KmpTarget.Macos.MacosHost.MACOS_ARM64), macos.hosts)
        val linux = targetsOf { linux(isApplication = true) }.single() as KmpTarget.Linux
        assertEquals(KmpTarget.Linux.LinuxHost.entries.toSet(), linux.hosts)
        assertEquals(true, linux.isApplication)

        assertEquals(
            setOf(KmpTarget.Macos.MacosHost.MACOS_ARM64, KmpTarget.Macos.MacosHost.MACOS_X64),
            (targetsOf { macos(macosArm64, macosX64) }.single() as KmpTarget.Macos).hosts,
        )
        assertEquals(
            setOf(KmpTarget.Linux.LinuxHost.LINUX_ARM64),
            (targetsOf { linux(linuxArm64) }.single() as KmpTarget.Linux).hosts,
        )
        assertFailsWith<IllegalArgumentException> { KmpTarget.Macos(hosts = emptySet()) }
        assertFailsWith<IllegalArgumentException> { KmpTarget.Linux(hosts = emptySet()) }
    }

    @Test
    fun `xcframework needs one shared framework name`() {
        val registry =
            moduleRegistry {
                kmp("shared") {
                    xcframework = true
                    ios { framework("Shared") }
                    macos { framework("Shared") }
                }
            }
        assertEquals(true, (registry["shared"] as Module.KmpLibrary).xcframework)

        val none =
            assertFailsWith<IllegalArgumentException> {
                moduleRegistry {
                    kmp("shared") {
                        xcframework = true
                        ios()
                    }
                }
            }
        assertTrue("no Apple target declares a framework" in none.message.orEmpty())

        val mixed =
            assertFailsWith<IllegalArgumentException> {
                moduleRegistry {
                    kmp("shared") {
                        xcframework = true
                        ios { framework("Shared") }
                        macos { framework("Mac") }
                    }
                }
            }
        assertTrue("Shared, Mac" in mixed.message.orEmpty())
    }

    @Test
    fun `every apple target can export a framework`() {
        val targets =
            targetsOf {
                macos(macosX64, isApplication = true) { framework("Mac") }
                tvos { framework("Tv") }
                watchos { framework("Watch") }
            }.associateBy { it.name }

        assertEquals(Framework("Mac"), (targets.getValue("macos") as KmpTarget.Apple).framework)
        assertEquals(Framework("Tv"), (targets.getValue("tvos") as KmpTarget.Apple).framework)
        assertEquals(Framework("Watch"), (targets.getValue("watchos") as KmpTarget.Apple).framework)
        assertEquals(null, (targetsOf { macos() }.single() as KmpTarget.Apple).framework)

        val error =
            assertFailsWith<IllegalArgumentException> {
                moduleRegistry { kmp("umbrella") { macos { framework("Shared") { export(":missing") } } } }
            }
        assertTrue(":missing" in error.message.orEmpty())
    }

    @Test
    fun `desktop declares macos linux and mingw`() {
        assertEquals(
            setOf(
                KmpTarget.Macos(true),
                KmpTarget.Linux(true),
                KmpTarget.Mingw(true),
            ),
            targetsOf { desktop(isApplication = true) },
        )
    }

    @Test
    fun `host based targets default to their DEFAULT hosts and honor selections`() {
        assertEquals(IosHost.DEFAULT, (targetsOf { ios() }.single() as Ios).hosts)
        assertEquals(KmpTarget.Tvos.TvosHost.DEFAULT, (targetsOf { tvos() }.single() as KmpTarget.Tvos).hosts)
        assertEquals(KmpTarget.Watchos.WatchosHost.DEFAULT, (targetsOf { watchos() }.single() as KmpTarget.Watchos).hosts)
        assertEquals(
            KmpTarget.AndroidNative.AndroidNativeHost.DEFAULT,
            (targetsOf { androidNative() }.single() as KmpTarget.AndroidNative).hosts,
        )

        val ios = targetsOf { ios(iosSimulatorArm64, iosX64) }.single() as Ios
        assertEquals(setOf(IosHost.IOS_SIMULATOR_ARM64, IosHost.IOS_X64), ios.hosts)
        assertEquals(
            setOf(KmpTarget.Tvos.TvosHost.TVOS_ARM64),
            (targetsOf { tvos(tvosArm64) }.single() as KmpTarget.Tvos).hosts,
        )
        assertEquals(
            setOf(KmpTarget.Watchos.WatchosHost.WATCHOS_ARM64),
            (targetsOf { watchos(watchosArm64) }.single() as KmpTarget.Watchos).hosts,
        )
        assertEquals(
            setOf(KmpTarget.AndroidNative.AndroidNativeHost.ANDROID_NATIVE_X64),
            (targetsOf { androidNative(androidNativeX64) }.single() as KmpTarget.AndroidNative).hosts,
        )

        assertFailsWith<IllegalArgumentException> { Ios(hosts = emptySet()) }
        assertFailsWith<IllegalArgumentException> { KmpTarget.Tvos(emptySet()) }
        assertFailsWith<IllegalArgumentException> { KmpTarget.Watchos(emptySet()) }
        assertFailsWith<IllegalArgumentException> { KmpTarget.AndroidNative(emptySet()) }
    }

    private fun targetsOf(block: KmpTargetsScope.() -> Unit): Set<KmpTarget> = KmpTargetsScope().apply(block).build()

    @Test
    fun `application modules default to no explicit api`() {
        val registry =
            moduleRegistry {
                androidApp("app", namespace = "com.example.app")
                java("server") { mainClass = "MainKt" }
                java("lib")
            }

        assertEquals(false, registry["app"].hasExplicitApi)
        assertEquals(false, registry["server"].hasExplicitApi)
        assertEquals(false, registry["lib"].hasExplicitApi)
    }

    @Test
    fun `module blocks carry options`() {
        val registry =
            moduleRegistry {
                kmp("shared") {
                    usesCompose = true
                    hasExplicitApi = true
                    autoConfigureTargets = false
                    jvm()
                }
                java("server") {
                    group = "com.example"
                    version = "1.0"
                    mainClass = "MainKt"
                }
                androidLibrary("ui", "com.example.ui") { usesCompose = true }
            }

        assertEquals(
            Module.KmpLibrary("shared", setOf(KmpTarget.Jvm), hasExplicitApi = true, autoConfigureTargets = false, usesCompose = true),
            registry["shared"],
        )
        assertEquals(Module.Java("server", "com.example", "1.0", "MainKt"), registry["server"])
        assertEquals(Module.AndroidLibrary("ui", "com.example.ui", usesCompose = true), registry["ui"])
    }

    @Test
    fun `defaults apply to later modules and nested groups and can be overridden`() {
        val registry =
            moduleRegistry {
                defaults { hasExplicitApi = true }
                java("lib")
                java("app") { hasExplicitApi = false }
                group("feature") {
                    defaults { usesCompose = true }
                    kmp("ui") { jvm() }
                    group("nested") { androidLibrary("widgets", "com.example.widgets") }
                }
                kmp("core") { jvm() }
            }

        assertEquals(true, registry["lib"].hasExplicitApi)
        assertEquals(false, registry["app"].hasExplicitApi)
        assertEquals(Module.KmpLibrary("ui", setOf(KmpTarget.Jvm), hasExplicitApi = true, usesCompose = true), registry["ui"])
        assertEquals(
            Module.AndroidLibrary("widgets", "com.example.widgets", usesCompose = true, hasExplicitApi = true),
            registry["widgets"],
        )
        assertEquals(Module.KmpLibrary("core", setOf(KmpTarget.Jvm), hasExplicitApi = true), registry["core"])
    }

    @Test
    fun `defaults after a module or group are rejected`() {
        assertFailsWith<IllegalStateException> {
            moduleRegistry {
                java("lib")
                defaults { hasExplicitApi = true }
            }
        }
        assertFailsWith<IllegalStateException> {
            moduleRegistry {
                group("feature") { java("lib") }
                defaults { hasExplicitApi = true }
            }
        }
    }

    @Test
    fun `option presets apply to every module kind`() {
        fun ModuleOptionsScope.uiDefaults() {
            usesCompose = true
            hasExplicitApi = true
        }

        val registry =
            moduleRegistry {
                androidApp("app", "com.example.app") { uiDefaults() }
                androidLibrary("ui", "com.example.ui") { uiDefaults() }
                java("desktop") { uiDefaults() }
                kmp("shared") {
                    uiDefaults()
                    jvm()
                }
            }

        registry.modules.forEach { (_, module) ->
            assertEquals(true, (module as Module.UsesCompose).usesCompose, module.name)
            assertEquals(true, module.hasExplicitApi, module.name)
        }
    }

    @Test
    fun `flavors keep dimension order and reject duplicates`() {
        val app =
            moduleRegistry {
                androidApp("app", "com.example.app") {
                    flavors {
                        dimension("env") {
                            flavor("dev", isDefault = true) { applicationIdSuffix = ".dev" }
                            flavor("prod")
                        }
                        dimension("market") { flavor("eu") }
                    }
                }
            }["app"] as Module.AndroidApp

        assertEquals(
            listOf(
                Flavor("dev", "env", isDefault = true, applicationIdSuffix = ".dev"),
                Flavor("prod", "env"),
                Flavor("eu", "market"),
            ),
            app.flavors,
        )

        assertFailsWith<IllegalArgumentException> {
            moduleRegistry {
                androidApp("app", "com.example.app") {
                    flavors {
                        dimension("env") { flavor("dev") }
                        dimension("market") { flavor("dev") }
                    }
                }
            }
        }
        assertFailsWith<IllegalArgumentException> {
            moduleRegistry { androidApp("app", "com.example.app") { flavors { dimension("env") { } } } }
        }
    }
}

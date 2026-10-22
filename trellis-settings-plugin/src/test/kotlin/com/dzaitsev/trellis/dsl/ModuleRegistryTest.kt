package com.dzaitsev.trellis.dsl

import com.dzaitsev.trellis.KmpTarget
import com.dzaitsev.trellis.KmpTarget.Ios
import com.dzaitsev.trellis.KmpTarget.Ios.Framework
import com.dzaitsev.trellis.KmpTarget.Ios.IosHost
import com.dzaitsev.trellis.KmpTarget.Js
import com.dzaitsev.trellis.KmpTarget.JsEnv
import com.dzaitsev.trellis.Module
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
                java("server", mainClass = "MainKt")
                java("lib")
            }

        assertEquals(false, registry["app"].hasExplicitApi)
        assertEquals(false, registry["server"].hasExplicitApi)
        assertEquals(false, registry["lib"].hasExplicitApi)
    }
}

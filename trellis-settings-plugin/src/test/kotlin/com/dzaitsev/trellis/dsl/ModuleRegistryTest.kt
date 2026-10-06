package com.dzaitsev.trellis.dsl

import com.dzaitsev.trellis.KmpTarget
import com.dzaitsev.trellis.KmpTarget.Desktop
import com.dzaitsev.trellis.KmpTarget.Desktop.DesktopHost
import com.dzaitsev.trellis.KmpTarget.Ios
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
        val registry = moduleRegistry {
            root("app-root")
            java("server")
        }

        assertEquals(listOf(ProjectPath.ROOT, ProjectPath(":server")), registry.modules.map { it.path })
        assertEquals(Module.Root("app-root"), registry.findByPath(ProjectPath.ROOT))
        assertEquals("app-root", registry[":"].name)
    }

    @Test
    fun `several roots resolve by root project name`() {
        val registry = moduleRegistry {
            root("android")
            root("ios")
        }

        assertEquals("ios", registry.findRoot("ios")?.name)
        assertNull(registry.findRoot("other"))
        assertNull(registry.findByPath(ProjectPath.ROOT))
    }

    @Test
    fun `nested groups build nested paths`() {
        val registry = moduleRegistry {
            group("feature") {
                group("auth") {
                    kmp("api") { setOf(jvm) }
                }
                kmp("profile") { setOf(jvm) }
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
        val registry = moduleRegistry { group("feature") { kmp("auth") { setOf(jvm) } } }

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
                kmp("a") { setOf(jvm) }
            }
        }
    }

    @Test
    fun `kmp module without targets is rejected`() {
        val error = assertFailsWith<IllegalArgumentException> {
            moduleRegistry { kmp("a") { emptySet() } }
        }
        assertTrue("declares no targets" in error.message.orEmpty())
    }

    @Test
    fun `repeated target kinds are rejected`() {
        val error = assertFailsWith<IllegalArgumentException> {
            moduleRegistry { kmp("a") { js { setOf(browser) } + js { setOf(nodeJs) } } }
        }
        assertTrue("Js" in error.message.orEmpty())
    }

    @Test
    fun `framework exports must be declared`() {
        val error = assertFailsWith<IllegalArgumentException> {
            moduleRegistry {
                kmp("umbrella") {
                    setOf(ios(framework = framework("Shared", export = listOf(":missing"))) {
                        setOf(iosArm64)
                    })
                }
            }
        }
        assertTrue(":missing" in error.message.orEmpty())

        moduleRegistry {
            kmp("domain") { setOf(ios()) }
            kmp("umbrella") {
                setOf(ios(framework = framework("Shared", export = listOf(":domain"))) {
                    setOf(iosArm64)
                })
            }
        }
    }

    @Test
    fun `target builders default to all environments and hosts`() {
        lateinit var targets: List<KmpTarget>
        val scope = KmpTargetsScope
        moduleRegistry {
            targets = listOf(
                scope.js(),
                scope.js(isApplication = true) { setOf(browser) },
                scope.desktop(),
                scope.desktop(isApplication = false) { setOf(macosArm64) },
            )
        }

        assertEquals(JsEnv.entries.toSet(), (targets[0] as Js).environments)
        assertEquals(Js(isApplication = true, environments = setOf(JsEnv.BROWSER)), targets[1])
        assertEquals(DesktopHost.entries.toSet(), (targets[2] as Desktop).hosts)
        assertEquals(setOf(DesktopHost.MACOS_ARM64), (targets[3] as Desktop).hosts)
    }

    @Test
    fun `ios builder defaults to device and arm64 simulator`() {
        lateinit var targets: List<Ios>
        val scope = KmpTargetsScope
        moduleRegistry {
            targets = listOf(
                scope.ios(),
                scope.ios(framework = scope.framework("Shared")) { setOf(iosArm64, iosSimulatorArm64) },
                scope.ios { setOf(iosSimulatorArm64, iosX64) },
            )
        }

        assertEquals(IosHost.DEFAULT, targets[0].hosts)
        assertEquals(IosHost.DEFAULT, targets[1].hosts)
        assertEquals("Shared", targets[1].framework?.baseName)
        assertEquals(setOf(IosHost.IOS_SIMULATOR_ARM64, IosHost.IOS_X64), targets[2].hosts)
        assertEquals(null, targets[2].framework)
        assertFailsWith<IllegalArgumentException> { Ios(hosts = emptySet()) }
    }

    @Test
    fun `application modules default to no explicit api`() {
        val registry = moduleRegistry {
            androidApp("app", namespace = "com.example.app")
            java("server", mainClass = "MainKt")
            java("lib")
        }

        assertEquals(false, registry["app"].hasExplicitApi)
        assertEquals(false, registry["server"].hasExplicitApi)
        assertEquals(false, registry["lib"].hasExplicitApi)
    }
}

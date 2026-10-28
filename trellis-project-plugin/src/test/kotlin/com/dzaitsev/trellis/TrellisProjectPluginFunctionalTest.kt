package com.dzaitsev.trellis

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Properties
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrellisProjectPluginFunctionalTest {
    @TempDir
    lateinit var dir: File

    private val repo = System.getProperty("trellis.testRepo")
    private val version = System.getProperty("trellis.version")
    private val testKitDir = File(System.getProperty("trellis.testKitDir"))

    @BeforeTest
    fun fixture() {
        file("settings.gradle.kts").writeText(
            """
            pluginManagement {
                repositories {
                    maven(url = "${repo.replace("\\", "/")}")
                    gradlePluginPortal()
                    mavenCentral()
                }
            }
            plugins { id("com.dzaitsev.trellis") version "$version" }
            dependencyResolutionManagement { repositories { mavenCentral() } }
            rootProject.name = "fixture"
            include(":plain")

            trellis {
                createMissingDirectories()
                group("libs") { group("jvm") { java("core") } }
                java("app") { mainClass = "app.MainKt" }
                kmp("multi") { jvm(); js(browser) }
            }
            """.trimIndent(),
        )
        file("build.gradle.kts").writeText(
            """
            plugins {
                id("org.jetbrains.kotlin.jvm") version "$KOTLIN_VERSION" apply false
                id("org.jetbrains.kotlin.multiplatform") version "$KOTLIN_VERSION" apply false
                id("com.dzaitsev.trellis.conventions") version "$version"
            }
            trellis {
                kotlinVersion = "$KOTLIN_VERSION"
                javaVersion = 17
                androidMinSdk = 24
                androidTargetSdk = 36
            }
            """.trimIndent(),
        )
        file("plain/build.gradle.kts").writeText("")
        file("libs/jvm/core/src/main/kotlin/core/Core.kt").writeText("package core\n\npublic fun core(): String = \"core\"\n")
        file("app/build.gradle.kts").writeText("dependencies { implementation(project(\":libs:jvm:core\")) }\n")
        file("app/src/main/kotlin/app/Main.kt").writeText("package app\n\nfun main() = println(core.core())\n")
        file("multi/src/commonMain/kotlin/Multi.kt").writeText("public fun multi(): Int = 1\n")
    }

    @Test
    fun `settings honor host project directory mappings`() {
        file("settings.gradle.kts").appendText(
            "\ninclude(\":libs:jvm:core\")\nproject(\":libs:jvm:core\").projectDir = file(\"shared/core\")\n",
        )
        file("shared/core/src/main/kotlin/core/Core.kt").writeText("package core\n\npublic fun core(): String = \"mapped\"\n")
        build(":app:compileKotlin")
        assertTrue(file("shared/core/build/classes/kotlin/main/core/CoreKt.class").isFile)
    }

    @Test
    fun `compose jvm and kmp modules apply compiler and multiplatform plugins`() {
        val settings = file("settings.gradle.kts")
        settings.writeText(
            settings
                .readText()
                .replace("repositories { mavenCentral() }", "repositories { google(); mavenCentral() }")
                .replace("java(\"core\")", "java(\"core\") { usesCompose = true }")
                .replace(
                    "kmp(\"multi\") { jvm(); js(browser) }",
                    "kmp(\"multi\") { usesCompose = true; jvm() }",
                ),
        )
        val root = file("build.gradle.kts")
        root.writeText(
            root.readText().replace(
                "plugins {",
                """
                plugins {
                    id("org.jetbrains.kotlin.plugin.compose") version "$KOTLIN_VERSION" apply false
                    id("org.jetbrains.compose") version "1.12.1" apply false
                """.trimIndent(),
            ),
        )
        listOf("libs/jvm/core", "multi").forEach { path ->
            file("$path/build.gradle.kts").writeText(
                "check(plugins.hasPlugin(\"org.jetbrains.kotlin.plugin.compose\"))\n" +
                    "check(plugins.hasPlugin(\"org.jetbrains.compose\"))\n" +
                    if (path == "multi") {
                        "kotlin { sourceSets.commonMain.dependencies { implementation(\"org.jetbrains.compose.runtime:runtime:1.12.1\") } }\n"
                    } else {
                        "dependencies { implementation(\"org.jetbrains.compose.runtime:runtime:1.12.1\") }\n"
                    },
            )
        }
        build(":libs:jvm:core:compileKotlin", ":multi:compileKotlinJvm")
    }

    @Test
    fun `android app library and kmp configure and compile with native compose compiler only`() {
        val properties = Properties()
        val localProperties = File(System.getProperty("trellis.localProperties"))
        if (localProperties.isFile) localProperties.inputStream().use(properties::load)
        val sdk =
            sequenceOf(properties.getProperty("sdk.dir"), System.getenv("ANDROID_HOME"))
                .filterNotNull()
                .map(::File)
                .firstOrNull {
                    it.resolve("platforms/android-37/android.jar").isFile ||
                        it.resolve("platforms/android-37.0/android.jar").isFile
                }
        val sdkMessage = "Android SDK with platform 37 unavailable (local.properties / ANDROID_HOME)"
        if (System.getProperty("trellis.requireAndroidSdk").toBoolean()) checkNotNull(sdk) { sdkMessage }
        assumeTrue(sdk != null, sdkMessage)
        file("local.properties").outputStream().use { output ->
            Properties().apply { setProperty("sdk.dir", sdk!!.absolutePath) }.store(output, null)
        }
        val settings = file("settings.gradle.kts")
        settings.writeText(
            settings
                .readText()
                .replace("gradlePluginPortal()", "google()\n gradlePluginPortal()")
                .replace("repositories { mavenCentral() }", "repositories { google(); mavenCentral() }") +
                """

                trellis {
                    androidApp("nativeApp", "fixture.app") { usesCompose = true }
                    androidLibrary("nativeLibrary", "fixture.library") { usesCompose = true }
                    kmp("androidMulti") { usesCompose = true; android("fixture.multi"); jvm() }
                }
                """.trimIndent(),
        )
        val root = file("build.gradle.kts")
        root.writeText(
            root
                .readText()
                .replace(
                    "plugins {",
                    """
                    plugins {
                        id("com.android.application") version "9.3.3" apply false
                        id("com.android.library") version "9.3.3" apply false
                        id("com.android.kotlin.multiplatform.library") version "9.3.3" apply false
                        id("org.jetbrains.kotlin.plugin.compose") version "$KOTLIN_VERSION" apply false
                        id("org.jetbrains.compose") version "1.12.1" apply false
                    """.trimIndent(),
                ).replace("androidTargetSdk = 36", "androidTargetSdk = 37"),
        )
        listOf("nativeApp", "nativeLibrary").forEach { path ->
            file("$path/build.gradle.kts").writeText(
                """
                check(plugins.hasPlugin("org.jetbrains.kotlin.plugin.compose"))
                check(!plugins.hasPlugin("org.jetbrains.compose"))
                dependencies { implementation("org.jetbrains.compose.runtime:runtime:1.12.1") }
                android {
                    check(namespace == "fixture.${if (path == "nativeApp") "app" else "library"}")
                    check(compileSdk == 37)
                    check(defaultConfig.minSdk == 24)
                    check(buildFeatures.compose == true)
                }
                """.trimIndent(),
            )
            file("$path/src/main/AndroidManifest.xml").writeText("<manifest />")
            file("$path/src/main/kotlin/Native.kt").writeText("public fun nativeValue(): Int = 1\n")
        }
        file("androidMulti/build.gradle.kts").writeText(
            """
            check(plugins.hasPlugin("org.jetbrains.kotlin.plugin.compose"))
            check(plugins.hasPlugin("org.jetbrains.compose"))
            kotlin {
                sourceSets.commonMain.dependencies { implementation("org.jetbrains.compose.runtime:runtime:1.12.1") }
                android {
                    check(namespace == "fixture.multi")
                    check(compileSdk == 37)
                    check(minSdk == 24)
                    check(androidResources.enable)
                }
            }
            """.trimIndent(),
        )
        file("androidMulti/src/commonMain/kotlin/Multi.kt").writeText("public fun androidValue(): Int = 1\n")
        runner(":nativeApp:assembleDebug", ":nativeLibrary:assembleDebug", ":androidMulti:assemble")
            .withGradleVersion("9.8.0")
            .build()
    }

    @Test
    fun `local conventions support isolated projects and reuse the configuration cache`() {
        val settings = file("settings.gradle.kts")
        settings.writeText(
            settings
                .readText()
                .replace("{ jvm(); js(browser) }", "{ jvm() }"),
        )
        file("settings.gradle.kts").appendText(
            "\ntrellis { projectConventions { kotlinVersion = \"$KOTLIN_VERSION\"; javaVersion = 17 } }\n",
        )

        file("build.gradle.kts").writeText(
            """
            plugins {
                id("org.jetbrains.kotlin.jvm") version "$KOTLIN_VERSION" apply false
                id("org.jetbrains.kotlin.multiplatform") version "$KOTLIN_VERSION" apply false
                id("com.dzaitsev.trellis.local") version "$version" apply false
            }
            """.trimIndent(),
        )
        listOf("libs/jvm/core", "app", "multi").forEach { path ->
            val script = file("$path/build.gradle.kts")
            script.writeText("plugins { id(\"com.dzaitsev.trellis.local\") }\n" + if (script.exists()) script.readText() else "")
        }
        val isolatedHome = File(testKitDir, "isolated-home").apply { mkdirs() }
        val arguments =
            arrayOf(
                ":app:compileKotlin",
                ":multi:compileKotlinJvm",
                "--configuration-cache",
                "-Dorg.gradle.unsafe.isolated-projects=true",
                "--gradle-user-home",
                isolatedHome.absolutePath,
            )
        build(*arguments)
        val second = build(*arguments)
        assertTrue("Configuration cache entry reused" in second.output, second.output)
        assertEquals(JAVA_17_MAJOR, classMajorVersion("app/build/classes/kotlin/main/app/MainKt.class"))
    }

    @Test
    fun `root conventions default to settings and root script values win`() {
        file("settings.gradle.kts").appendText(
            "\ntrellis { projectConventions { kotlinVersion = \"$KOTLIN_VERSION\"; javaVersion = 11 } }\n",
        )
        val root = file("build.gradle.kts")
        root.writeText(root.readText().substringBefore("trellis {"))

        build(":app:compileKotlin")
        assertEquals(JAVA_11_MAJOR, classMajorVersion("app/build/classes/kotlin/main/app/MainKt.class"))

        root.appendText("trellis { javaVersion = 17 }\n")
        build(":app:compileKotlin")
        assertEquals(JAVA_17_MAJOR, classMajorVersion("app/build/classes/kotlin/main/app/MainKt.class"))
    }

    @Test
    fun `kotlin version applies to jvm modules and is optional`() {
        file("libs/jvm/core/build.gradle.kts").writeText(
            "println(\"LANG=\" + kotlin.compilerOptions.languageVersion.orNull?.version)\n",
        )

        val expected = KOTLIN_VERSION.split('.').take(2).joinToString(".")
        assertTrue("LANG=$expected" in build("help").output)

        val root = file("build.gradle.kts")
        root.writeText(root.readText().replace("kotlinVersion = \"$KOTLIN_VERSION\"", ""))
        assertTrue("LANG=null" in build("help").output)
    }

    @Test
    fun `jvm modules build without a java version`() {
        val root = file("build.gradle.kts")
        root.writeText(root.readText().replace("javaVersion = 17", ""))

        build(":app:compileKotlin")
    }

    @Test
    fun `android modules without sdk levels fail with what to set`() {
        val settings = file("settings.gradle.kts")
        settings.writeText(
            settings.readText().replace("gradlePluginPortal()", "google()\n gradlePluginPortal()") +
                "\ntrellis { androidLibrary(\"nativeLibrary\", \"fixture.library\") }\n",
        )
        val root = file("build.gradle.kts")
        root.writeText(
            root
                .readText()
                .replace("plugins {", "plugins {\n    id(\"com.android.library\") version \"9.3.3\" apply false")
                .replace("androidMinSdk = 24", "")
                .replace("androidTargetSdk = 36", ""),
        )

        val output = runner("help").withGradleVersion("9.8.0").buildAndFail().output

        assertTrue(
            ":nativeLibrary declares an Android module or target, but androidMinSdk and androidTargetSdk are not set" in output,
            output,
        )
    }

    @Test
    fun `configures declared modules and reuses the configuration cache`() {
        val tasks = arrayOf(":app:compileKotlin", ":multi:compileKotlinJvm", ":multi:compileKotlinJs", "--configuration-cache")

        build(*tasks)
        val second = build(*tasks)

        assertTrue("Configuration cache entry reused" in second.output, second.output)
        assertEquals(JAVA_17_MAJOR, classMajorVersion("app/build/classes/kotlin/main/app/MainKt.class"))
        assertEquals(JAVA_17_MAJOR, classMajorVersion("multi/build/classes/kotlin/jvm/main/MultiKt.class"))
    }

    @Test
    fun `nested groups are included and undeclared projects are left alone`() {
        val projects = build("projects").output
        assertTrue("':libs:jvm:core'" in projects, projects)

        val plainTasks = build(":plain:tasks", "--all").output
        assertFalse("compileKotlin" in plainTasks, plainTasks)
    }

    @Test
    fun `module scripts override the configured jvm target`() {
        file("libs/jvm/core/build.gradle.kts").writeText(
            """
            java { targetCompatibility = JavaVersion.VERSION_11; sourceCompatibility = JavaVersion.VERSION_11 }
            kotlin { compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11 } }
            """.trimIndent(),
        )

        build(":libs:jvm:core:compileKotlin")

        assertEquals(JAVA_11_MAJOR, classMajorVersion("libs/jvm/core/build/classes/kotlin/main/core/CoreKt.class"))
    }

    @Test
    fun `explicit api modules reject implicit visibility`() {
        val settings = file("settings.gradle.kts")
        settings.writeText(settings.readText().replace("java(\"core\")", "java(\"core\") { hasExplicitApi = true }"))
        file("libs/jvm/core/src/main/kotlin/core/Core.kt").writeText("package core\n\nfun core(): String = \"core\"\n")

        val result = runner(":libs:jvm:core:compileKotlin").buildAndFail()

        assertTrue("Visibility must be specified in explicit API mode" in result.output, result.output)
    }

    @Test
    fun `desktop targets register only the selected hosts`() {
        val settings = file("settings.gradle.kts")
        settings.writeText(settings.readText().replace("{ jvm(); js(browser) }", "{ jvm(); macos(macosX64); linux(linuxArm64) }"))

        val tasks = build(":multi:tasks", "--all").output

        assertTrue("compileKotlinMacosX64" in tasks, tasks)
        assertTrue("compileKotlinLinuxArm64" in tasks, tasks)
        assertFalse("compileKotlinMacosArm64" in tasks, tasks)
        assertFalse("compileKotlinLinuxX64" in tasks, tasks)
    }

    @Test
    fun `apple targets beyond ios register framework binaries`() {
        val settings = file("settings.gradle.kts")
        settings.writeText(
            settings.readText().replace(
                "{ jvm(); js(browser) }",
                "{ jvm(); macos { framework(\"Shared\") }; tvos(tvosArm64) { framework(\"Shared\") } }",
            ),
        )

        val tasks = build(":multi:tasks", "--all").output

        assertTrue("linkDebugFrameworkMacosArm64" in tasks, tasks)
        assertTrue("linkReleaseFrameworkTvosArm64" in tasks, tasks)
    }

    @Test
    fun `xcframework bundles the apple frameworks`() {
        val settings = file("settings.gradle.kts")
        settings.writeText(
            settings.readText().replace(
                "{ jvm(); js(browser) }",
                "{ jvm(); xcframework = true; ios(iosArm64) { framework(\"Shared\") }; macos { framework(\"Shared\") } }",
            ),
        )

        val tasks = build(":multi:tasks", "--all").output

        assertTrue("assembleSharedXCFramework" in tasks, tasks)
    }

    @Test
    fun `browser-only js target registers no node tasks`() {
        val tasks = build(":multi:tasks", "--all").output

        assertTrue("jsBrowserTest" in tasks, tasks)
        assertFalse("jsNodeTest" in tasks, tasks)
    }

    @Test
    fun `build-logic plugin extends trellis as the readme describes`() {
        writeAcmeBuildLogic()
        val root = file("build.gradle.kts")
        root.writeText(
            root
                .readText()
                .replace("id(\"com.dzaitsev.trellis.conventions\") version \"$version\"", "id(\"com.acme.conventions\")")
                .replace("javaVersion = 17", "javaVersion = 17\n    marker = \"hello\""),
        )

        val output = build(":app:acmeMarker", ":app:compileKotlin").output

        assertTrue("acme hello :app" in output, output)
        assertEquals(JAVA_17_MAJOR, classMajorVersion("app/build/classes/kotlin/main/app/MainKt.class"))
    }

    @Test
    fun `build-logic local plugin extends trellis per module`() {
        writeAcmeBuildLogic()
        file("settings.gradle.kts").appendText(
            "\ntrellis { projectConventions { kotlinVersion = \"$KOTLIN_VERSION\"; javaVersion = 17 } }\n",
        )
        file("build.gradle.kts").writeText(
            """
            plugins {
                id("org.jetbrains.kotlin.jvm") version "$KOTLIN_VERSION" apply false
                id("org.jetbrains.kotlin.multiplatform") version "$KOTLIN_VERSION" apply false
                id("com.acme.local") apply false
            }
            """.trimIndent(),
        )
        listOf("libs/jvm/core", "app").forEach { path ->
            val script = file("$path/build.gradle.kts")
            val body = if (script.exists()) script.readText() else ""
            script.writeText("plugins { id(\"com.acme.local\") }\ntrellis { marker = \"local\" }\n$body")
        }

        val output = build(":app:acmeMarker", ":app:compileKotlin").output

        assertTrue("acme local :app" in output, output)
        assertEquals(JAVA_17_MAJOR, classMajorVersion("app/build/classes/kotlin/main/app/MainKt.class"))
    }

    private fun writeAcmeBuildLogic() {
        val repoUrl = repo.replace("\\", "/")
        val settings = file("settings.gradle.kts")
        settings.writeText(settings.readText().replace("pluginManagement {", "pluginManagement {\n    includeBuild(\"build-logic\")"))
        file("build-logic/settings.gradle.kts").writeText("rootProject.name = \"build-logic\"\n")
        file("build-logic/build.gradle.kts").writeText(
            """
            plugins { `kotlin-dsl` }
            repositories {
                maven(url = "$repoUrl")
                mavenCentral()
                gradlePluginPortal()
            }
            dependencies { implementation("com.dzaitsev.trellis:trellis-project-plugin:$version") }
            gradlePlugin {
                plugins {
                    create("acmeConventions") {
                        id = "com.acme.conventions"
                        implementationClass = "com.acme.build.AcmeConventionsPlugin"
                    }
                    create("acmeLocal") {
                        id = "com.acme.local"
                        implementationClass = "com.acme.build.AcmeLocalPlugin"
                    }
                }
            }
            """.trimIndent(),
        )
        file("build-logic/src/main/kotlin/com/acme/build/Acme.kt").writeText(
            """
            package com.acme.build

            import com.dzaitsev.trellis.Module
            import com.dzaitsev.trellis.plugin.Configurator
            import com.dzaitsev.trellis.plugin.TrellisLocalProjectPlugin
            import com.dzaitsev.trellis.plugin.TrellisProjectExtension
            import com.dzaitsev.trellis.plugin.TrellisProjectPlugin
            import org.gradle.api.Project
            import org.gradle.api.provider.Property

            interface AcmeExtension : TrellisProjectExtension {
                val marker: Property<String>
            }

            class MarkerConfigurator : Configurator<AcmeExtension> {
                override fun configure(project: Project, module: Module, extension: AcmeExtension) {
                    if (module is Module.Root) return
                    val marker = extension.marker
                    val path = project.path
                    project.tasks.register("acmeMarker") { doLast { println("acme ${'$'}{marker.get()} ${'$'}path") } }
                }
            }

            class AcmeConventionsPlugin : TrellisProjectPlugin<AcmeExtension>(AcmeExtension::class) {
                override fun configurators() = super.configurators() + MarkerConfigurator()
            }

            class AcmeLocalPlugin : TrellisLocalProjectPlugin<AcmeExtension>(AcmeExtension::class) {
                override fun configurators() = super.configurators() + MarkerConfigurator()
            }
            """.trimIndent(),
        )
    }

    private fun file(path: String): File = dir.resolve(path).apply { parentFile.mkdirs() }

    private fun runner(vararg arguments: String): GradleRunner = GradleRunner
        .create()
        .withProjectDir(dir)
        .withTestKitDir(testKitDir)
        .withArguments(*arguments, "--stacktrace")
        .forwardOutput()

    private fun build(vararg arguments: String): BuildResult = runner(*arguments).build()

    private fun classMajorVersion(path: String): Int = dir.resolve(path).readBytes().let {
        (it[6].toInt() and 0xff shl 8) or
            (it[7].toInt() and 0xff)
    }

    private companion object {
        const val KOTLIN_VERSION = "2.4.20"
        const val JAVA_11_MAJOR = 55
        const val JAVA_17_MAJOR = 61
    }
}

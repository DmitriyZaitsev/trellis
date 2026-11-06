import com.dzaitsev.trellis.dsl.KmpModuleScope

rootProject.name = "Trellis_Sample"

// pluginManagement is compiled before the rest of this script, so it cannot share declarations with it.
pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("com.dzaitsev.trellis") version "1.0.0"
}

@Suppress("PropertyName")
val PACKAGE_ID = "com.dzaitsev.trellissample"

trellis {
    createMissingDirectories()

    /*
     * In root mode (com.dzaitsev.trellis.conventions), conventions can be set here in settings.gradle.kts
     * using projectConventions { }, or in the root build.gradle.kts using trellis { }.
     * Values set in the root build script take precedence. This approach allows accessing version catalogs
     * (libs.versions.toml) if needed. Uncomment below if you prefer settings-based configuration.
     * See the sample-isolated-projects for the alternative approach.
     */
//    projectConventions {
//        kotlinVersion = "2.4.21"
//        javaVersion = 17
//        androidMinSdk = 24
//        androidTargetSdk = 37
//    }

    group("app") {
        defaults { usesCompose = true }

        androidApp("androidApp", "$PACKAGE_ID.app")

        kmp("webApp") {
            js(browser, isApplication = true)
            wasmJs(browser, isApplication = true)
        }

        java("desktopApp")
    }

    kmp("shared") {
        usesCompose = true
        ios {
            framework("Shared") {
                export(":shared:ui", ":shared:platform", ":shared:greeting")
            }
        }
    }

    group("shared") {
        defaults { hasExplicitApi = true }

        kmp("ui") {
            usesCompose = true
            sharedTargets("$PACKAGE_ID.ui")
        }

        kmp("platform") {
            sharedTargets("$PACKAGE_ID.platform")
        }

        kmp("greeting") {
            sharedTargets("$PACKAGE_ID.greeting")
        }
    }

    java("server") {
        group = PACKAGE_ID
        version = "1.0.0"
        mainClass = "$PACKAGE_ID.ApplicationKt"
    }
}

fun KmpModuleScope.sharedTargets(androidNamespace: String) {
    android(androidNamespace)
    ios()
    jvm()
    js(browser)
    wasmJs(browser)
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
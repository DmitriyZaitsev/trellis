// In root mode (com.dzaitsev.trellis.conventions), the convention plugin applies all necessary
// plugins and configures this KMP module based on its declaration in settings.gradle.kts.
// The build.gradle.kts contains only what is specific to this module: its dependencies and
// source set configuration.

kotlin {
    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
        }
    }
}
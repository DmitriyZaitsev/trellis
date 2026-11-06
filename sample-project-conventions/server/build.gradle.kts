// In root mode (com.dzaitsev.trellis.conventions), the convention plugin configures the module
// based on its declaration in settings.gradle.kts. Additional plugins beyond the base configuration
// (such as Ktor) can still be explicitly declared here when the module needs them.
plugins {
    alias(libs.plugins.ktor)
}

dependencies {
    api(projects.shared.platform)
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}
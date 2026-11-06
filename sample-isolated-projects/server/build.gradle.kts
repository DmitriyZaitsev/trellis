// In Isolated Projects mode (com.dzaitsev.trellis.local), each module must apply the local plugin
// to configure only itself. The local plugin reads conventions from settings.gradle.kts and applies
// plugins and configures the module before the rest of this script runs. Additional plugins beyond
// the base configuration (such as Ktor) can also be explicitly declared here when the module needs them.
plugins {
    alias(libs.plugins.trellisLocal)
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
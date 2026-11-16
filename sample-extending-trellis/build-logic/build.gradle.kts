plugins { `kotlin-dsl` }

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.dzaitsev.trellis:trellis-project-plugin:1.0.0")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.8")
}

gradlePlugin {
    plugins {
        create("sampleConventions") {
            id = "com.example.trellis.conventions"
            implementationClass = "com.example.buildlogic.SampleConventionsPlugin"
        }
    }
}

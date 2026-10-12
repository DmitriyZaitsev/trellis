package com.dzaitsev.trellis.plugin

public class ProjectConventions {
    public var kotlinVersion: String = "2.4"
    public var javaVersion: Int = 17
    public var androidMinSdk: Int = 24
    public var androidTargetSdk: Int = 36
    public var useJavaToolchain: Boolean = false

    internal fun snapshot(): ProjectConventions = ProjectConventions().also {
        it.kotlinVersion = kotlinVersion
        it.javaVersion = javaVersion
        it.androidMinSdk = androidMinSdk
        it.androidTargetSdk = androidTargetSdk
        it.useJavaToolchain = useJavaToolchain
    }
}

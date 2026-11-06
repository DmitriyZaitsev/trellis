package com.dzaitsev.trellissample

public class JvmPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

public actual fun getPlatform(): Platform = JvmPlatform()
package com.dzaitsev.trellissample

import web.navigator.navigator

public class JsPlatform : Platform {
    private val userAgent = navigator.userAgent
    private val browserList = listOf(
        "Chrome",
        "Firefox",
        "Safari",
        "Edge",
    )

    override val name: String = userAgent.findAnyOf(browserList, ignoreCase = true)
        ?.let { (startIndex) -> userAgent.substring(startIndex).substringBefore(" ") }
        ?: "Unknown"
}

public actual fun getPlatform(): Platform = JsPlatform()

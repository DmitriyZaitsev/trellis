package com.dzaitsev.trellissample

public interface Platform {
    public val name: String
}

public expect fun getPlatform(): Platform

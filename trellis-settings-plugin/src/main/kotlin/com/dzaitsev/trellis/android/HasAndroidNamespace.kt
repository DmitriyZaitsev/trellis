package com.dzaitsev.trellis.android

/** Anything that claims an Android namespace; namespaces must be unique across the build. */
public interface HasAndroidNamespace {
    public val namespace: String
}

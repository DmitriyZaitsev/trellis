package com.dzaitsev.trellis.android

/** Capability marker for any type ([ProjectModule] or [KmpTarget]) that carries [AndroidAttrs]. */
public interface HasAndroidAttrs {
    public val androidAttrs: AndroidAttrs
}

package com.dzaitsev.trellis.android

/**
 * Android-specific attributes attached to an Android [ProjectModule] or an [KmpTarget.Android] target.
 *
 * This interface is open: consumers may add their own implementations beyond [Native] and [Kmp]
 * if they need attribute shapes this library does not anticipate.
 */
public interface AndroidAttrs {
    public val namespace: String

    /**
     * Attributes for a plain (non-KMP) Android application or library module.
     *
     * [flavors] lives here rather than on [AndroidAttrs] because product flavors are only
     * supported for native Android app/library modules, not for a KMP module's Android target.
     * For KMP modules, Compose is a module-wide setting: `kmp(usesCompose = true)`.
     */
    public data class Native(
        override val namespace: String,
        public val usesCompose: Boolean = false,
        public val flavors: List<Flavor> = emptyList(),
    ) : AndroidAttrs

    /**
     * Attributes for the Android target of a Kotlin Multiplatform module. Flavors are not supported here.
     *
     * Set [enableResources] to compile Android resources (needed e.g. by Compose Multiplatform
     * resources) and expose them to host tests. Set [hostTests] to `false` to skip registering the
     * `androidHostTest` compilation.
     */
    public data class Kmp(
        override val namespace: String,
        public val enableResources: Boolean = false,
        public val hostTests: Boolean = true,
    ) : AndroidAttrs
}

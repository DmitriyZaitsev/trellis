package com.dzaitsev.trellis.android

/**
 * A single Android product flavor, e.g. a backend environment or a market region.
 *
 * Consumers implement this with their own `enum class` per flavor dimension and pass the
 * resulting values to [AndroidAttrs.Native.flavors].
 */
public interface Flavor {
    public val flavorName: String
    public val applicationIdSuffix: String?
    public val dimension: String
    public val isDefault: Boolean
}

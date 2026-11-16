package com.example.buildlogic

import com.dzaitsev.trellis.plugin.TrellisProjectPlugin

class SampleConventionsPlugin : TrellisProjectPlugin<SampleExtension>(SampleExtension::class) {
    // Keep Trellis's built-in configurators and append project-specific behavior.
    override fun configurators() = super.configurators() + DetektConfigurator()

    override fun rootConfigurators() = super.rootConfigurators() + RootDetektConfigurator()
}

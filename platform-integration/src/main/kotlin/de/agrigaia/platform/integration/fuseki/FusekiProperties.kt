package de.agrigaia.platform.integration.fuseki

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "agrigaia-platform.ontologies")
open class FusekiProperties(
    var agrovocURL: String? = null,
    var geonamesURL: String? = null
)

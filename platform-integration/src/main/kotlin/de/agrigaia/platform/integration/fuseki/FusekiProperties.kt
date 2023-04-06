package de.agrigaia.platform.integration.fuseki

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "agrigaia-platform.ontologies")
open class FusekiProperties(
    var agrovocUrl: String? = null,
    var geonamesUrl: String? = null
)

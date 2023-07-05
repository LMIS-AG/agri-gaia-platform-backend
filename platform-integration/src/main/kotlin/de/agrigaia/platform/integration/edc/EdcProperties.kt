package de.agrigaia.platform.integration.edc

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "agrigaia-platform.edc")
open class EdcProperties(
    var lmisConnectorUrl: String? = null
)

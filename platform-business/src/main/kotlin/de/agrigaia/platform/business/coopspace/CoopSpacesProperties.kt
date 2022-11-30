package de.agrigaia.platform.business.coopspace

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "agrigaia-platform.coop-spaces")
open class CoopSpacesProperties(
    var createCoopSpaceUrl: String? = null,
    var deleteCoopSpaceUrl: String? = null
)
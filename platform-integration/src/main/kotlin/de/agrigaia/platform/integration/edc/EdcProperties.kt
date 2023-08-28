package de.agrigaia.platform.integration.edc

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "agrigaia-platform.edc-connector-urls")
open class EdcProperties(
    var agBrain: String? = null,
    var agrotechValley: String? = null,
    var amazone: String? = null,
    var bosch: String? = null,
    var claas: String? = null,
    var dfki: String? = null,
    var hsos: String? = null,
    var kotte: String? = null,
    var krone: String? = null,
    var lmis: String? = null,
    var uos: String? = null,
    var wernsing: String? = null,
)

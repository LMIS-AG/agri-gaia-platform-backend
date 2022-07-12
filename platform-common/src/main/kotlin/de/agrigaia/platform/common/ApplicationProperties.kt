package de.agrigaia.platform.common

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "agrigaia-platform")
open class ApplicationProperties {
    /**
     * Comma-separated list of allowed CORS origins.
     */
    lateinit var allowedOrigins: Array<String>

    /**
     * Keycloak ClientID
     */
    lateinit var clientId: String
}
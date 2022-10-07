package de.agrigaia.platform.integration.keycloak


import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration


@Configuration
@ConfigurationProperties(prefix = "agrigaia-platform.keycloak")
open class KeycloakProperties {
    /**
     * Url of the authentication server
     */
    var serverUrl: String? = null
    var realm: String? = null

    /**
     * The id of the client. Often times realm-management
     */
    var clientId: String? = null

    /**
     * The secret of the client
     */
    var clientSecret: String? = null
    var userName: String? = null
    var password: String? = null
}
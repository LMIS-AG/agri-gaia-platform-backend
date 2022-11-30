package de.agrigaia.platform.integration.keycloak

import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.resource.RealmResource
import org.keycloak.admin.client.resource.UserResource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class KeycloakService @Autowired constructor(private val keycloakProperties: KeycloakProperties) {

    private var keycloak: Keycloak =  Keycloak.getInstance(
        keycloakProperties.serverUrl,
        keycloakProperties.realm,
        keycloakProperties.userName,
        keycloakProperties.password,
        keycloakProperties.clientId,
        keycloakProperties.clientSecret
    )

    private var agrigaiaRealm: RealmResource = keycloak.realm(keycloakProperties.realm)

    // TODO remove - only there for test purposes
    fun showRealm() {
        println("Realm: " + (keycloakProperties.realm ?: "no realm defined"));
    }

    fun getUserResource(id: String?): UserResource? {
        println(agrigaiaRealm.users())
        //println(kochRealm.users().count())
        println(agrigaiaRealm.users()[id])
        println(agrigaiaRealm.users().get(id).toRepresentation().username)
        println(agrigaiaRealm.users()[id].toRepresentation().username)
        return agrigaiaRealm.users()[id]
    }


}
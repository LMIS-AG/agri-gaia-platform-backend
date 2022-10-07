package de.agrigaia.platform.integration.keycloak

import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.resource.RealmResource
import org.keycloak.admin.client.resource.UserResource
import org.springframework.stereotype.Service

@Service
class KeycloakService {

    private var keycloak: Keycloak? = null
    private var agrigaiaRealm: RealmResource? = null
    private var keycloakProperties: KeycloakProperties? = null
    //private var applicationProperties: ApplicationProperties? = null

    constructor (
        keycloakProperties_: KeycloakProperties
    ) {
        keycloak = Keycloak.getInstance(
            keycloakProperties_.serverUrl,
            keycloakProperties_.realm,
            keycloakProperties_.userName,
            keycloakProperties_.password,
            keycloakProperties_.clientId,
            keycloakProperties_.clientSecret
        )
        agrigaiaRealm = keycloak!!.realm(keycloakProperties_.realm);
        keycloakProperties = keycloakProperties_;
    }

    // TODO remove - only there for tes purposes
    fun showRealm() {
        println("Realm: " + (keycloakProperties?.realm ?: "no realm defined"));
    }

    fun getUserResource(id: String?): UserResource? {
        println(agrigaiaRealm!!.users())
        //println(kochRealm!!.users().count())
        println(agrigaiaRealm!!.users()[id])
        println(agrigaiaRealm!!.users().get(id).toRepresentation().username)
        println(agrigaiaRealm!!.users()[id].toRepresentation().username)
        return agrigaiaRealm!!.users()[id]
    }


}
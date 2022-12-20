package de.agrigaia.platform.integration.keycloak

import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.resource.RealmResource
import org.keycloak.admin.client.resource.UserResource
import org.keycloak.admin.client.resource.UsersResource
import org.keycloak.representations.idm.UserRepresentation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class KeycloakConnectorService @Autowired constructor(private val keycloakProperties: KeycloakProperties) {

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

    fun getUsers(): MutableList<UserRepresentation> {
        return agrigaiaRealm.users().list()
    }

    fun getGroups(): Map<String, List<String>> {
        val groupsMap = mutableMapOf<String, List<String>>()

        val groupModels = agrigaiaRealm.groups().groups()
        groupModels.forEach { groupModel ->
            if (groupModel.name != "AgriGaia") {  // skip the "AgriGaia" group
                val usersGroupModel = groupModel.subGroups.find { it.name == "Users" }  // find the "Users" subgroup
                if (usersGroupModel != null) {
                    val groupMembers = agrigaiaRealm.groups().group(usersGroupModel.id).members()  // retrieve the members of the "Users" subgroup
                    val userNames = groupMembers.map { member ->
                        agrigaiaRealm.users().get(member.id).toRepresentation().username
                    }
                    groupsMap[groupModel.name] = userNames
                }
            }
        }

        return groupsMap
    }

}

package de.agrigaia.platform.integration.keycloak

import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.resource.RealmResource
import org.keycloak.representations.idm.GroupRepresentation
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

    fun removeUserFromGroup(username: String?, groupName: String?) {
        val user = agrigaiaRealm.users().search(username).first()
        val group = groupName?.let { findGroupByName(agrigaiaRealm.groups().groups(), it) }
        if (group != null) {
            agrigaiaRealm.users().get(user.id).leaveGroup(group.id)
        }
    }

    private fun findGroupByName(groups: List<GroupRepresentation>, groupName: String): GroupRepresentation? {
        for (group in groups) {
            if (group.name == groupName) {
                return group
            }
            val subGroup = findGroupByName(group.subGroups, groupName)
            if (subGroup != null) {
                return subGroup
            }
        }
        return null
    }
    
}

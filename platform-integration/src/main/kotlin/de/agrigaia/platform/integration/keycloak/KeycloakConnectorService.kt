package de.agrigaia.platform.integration.keycloak

import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.resource.RealmResource
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

    fun removeUserFromGroup(username: String?, role: String?, coopSpaceName: String?, companyName: String?) {
        val subGroupName = "$coopSpaceName-${role}"

        val user = agrigaiaRealm.users().search(username).first()
        val companyGroup = agrigaiaRealm.groups().groups().firstOrNull { it.name == companyName }
        val projectGroup = companyGroup?.subGroups?.firstOrNull { it.name == "Projects" }
        val targetGroup = projectGroup?.subGroups?.firstOrNull { it.name == coopSpaceName }
        val subGroup = targetGroup?.subGroups?.firstOrNull { it.name == subGroupName }

        if (subGroup != null) {
            agrigaiaRealm.users().get(user.id).leaveGroup(subGroup.id)
        }
    }
}

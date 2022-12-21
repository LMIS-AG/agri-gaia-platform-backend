package de.agrigaia.platform.business.keycloak

import de.agrigaia.platform.business.errors.BusinessException
import de.agrigaia.platform.business.errors.ErrorType
import de.agrigaia.platform.integration.keycloak.KeycloakConnectorService
import de.agrigaia.platform.model.coopspace.CoopSpaceRole
import de.agrigaia.platform.model.coopspace.Member
import org.springframework.stereotype.Service

@Service
class KeycloakService(
    private val keycloakConnectorService: KeycloakConnectorService
) {
    fun getKeycloakUsers(): List<Member> {
        val users = this.keycloakConnectorService.getUsers()
        val groups = this.keycloakConnectorService.getGroups()
        return users.mapNotNull { user ->
            val userGroup = groups.entries.find { it.value.contains(user.username) }?.key
            if (userGroup != null) {
                Member(user.firstName + " " + user.lastName, userGroup, user.email, CoopSpaceRole.USER, user.username)
            } else {
                null
            }
        }
    }

    fun findKeycloakUserByMail(mail: String): Member {
        return this.getKeycloakUsers().find { mail == it.email } ?: throw BusinessException("User with $mail does not exist", ErrorType.NOT_FOUND)
    }
}

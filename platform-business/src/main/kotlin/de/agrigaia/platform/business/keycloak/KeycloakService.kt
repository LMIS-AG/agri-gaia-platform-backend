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
        return users.map { Member(it.firstName + it.lastName, "company", it.email, CoopSpaceRole.USER, it.username) }
    }

    fun findKeycloakUserByMail(mail: String): Member {
        return this.getKeycloakUsers().find { mail == it.email } ?: throw BusinessException("User with $mail does not exist", ErrorType.NOT_FOUND)
    }
}

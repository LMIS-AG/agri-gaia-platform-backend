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
        return listOf(
            Member("Alejandro Lopez", "Bosch", "alejandro.lopez2@de.bosch.com", CoopSpaceRole.VIEWER, "alopez"),
            Member("Julian Ende", "LMIS", "julian.ende@lmis.de", CoopSpaceRole.EDITOR, "jende"),
            Member("Enis Belli", "LMIS", "enis.belli@lmis.de", CoopSpaceRole.EDITOR, "ebelli"),
            Member(
                "Katharina Beckwermert",
                "LMIS",
                "katharina.beckwermert@lmis.de",
                CoopSpaceRole.EDITOR,
                "kbeckwermert"
            ),
            Member("Henning Wuebben", "LMIS", "henning.wuebben@lmis.de", CoopSpaceRole.EDITOR, "hwuebben"),
            Member("Christoph Manß", "DFKI", "christoph.manss@dfki.de", CoopSpaceRole.EDITOR, "cmanss")
        )
    }

    fun findKeycloakUserByMail(mail: String): Member {
        return this.getKeycloakUsers().find { mail == it.email } ?: throw BusinessException("User with $mail does not exist", ErrorType.NOT_FOUND)
    }
}

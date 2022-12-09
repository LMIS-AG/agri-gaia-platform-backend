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
        for (user in users){
            println(user.firstName)
        }
        return listOf(
            Member("Alejandro Lopez", "Bosch", "alejandro.lopez2@de.bosch.com", CoopSpaceRole.GUEST, "alopez"),
            Member("Julian Ende", "LMIS", "julian.ende@lmis.de", CoopSpaceRole.USER, "jende"),
            Member("Enis Belli", "LMIS", "enis.belli@lmis.de", CoopSpaceRole.USER, "ebelli"),
            Member(
                "Katharina Beckwermert",
                "LMIS",
                "katharina.beckwermert@lmis.de",
                CoopSpaceRole.USER,
                "kbeckwermert"
            ),
            Member("Henning Wuebben", "LMIS", "henning.wuebben@lmis.de", CoopSpaceRole.USER, "hwuebben"),
            Member("Christoph Manß", "DFKI", "christoph.manss@dfki.de", CoopSpaceRole.USER, "cmanss")
        )
    }

    fun findKeycloakUserByMail(mail: String): Member {
        return this.getKeycloakUsers().find { mail == it.email } ?: throw BusinessException("User with $mail does not exist", ErrorType.NOT_FOUND)
    }
}

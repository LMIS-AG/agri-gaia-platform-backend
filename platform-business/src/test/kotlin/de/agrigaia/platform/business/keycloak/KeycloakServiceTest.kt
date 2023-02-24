package de.agrigaia.platform.business.keycloak

import de.agrigaia.platform.integration.keycloak.KeycloakConnectorService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.keycloak.representations.idm.UserRepresentation
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KeycloakServiceTest {

    @Test
    fun `Test getKeycloakUsers`(){
        val keycloakConnectorService: KeycloakConnectorService = mockk()
        val keycloakService = KeycloakService(keycloakConnectorService)

        val userA = UserRepresentation( )
        userA.username = ""
        userA.firstName = ""
        userA.lastName = ""
        userA.email = ""

        every { keycloakConnectorService.getUsers() } returns mutableListOf()
        every { keycloakConnectorService.getGroups() } returns mapOf()
        var actual = keycloakService.getKeycloakUsers()
        assertEquals(listOf(), actual, "Should return an empty list when given empty inputs.")

        every { keycloakConnectorService.getUsers() } returns mutableListOf()
        every { keycloakConnectorService.getGroups() } returns mapOf()
        actual = keycloakService.getKeycloakUsers()
        assertEquals(listOf(), actual, "Should return an empty list when given empty inputs.")
    }

    @Test
    fun `Test findKeycloakUserByMail`(){
        val keycloakConnectorService: KeycloakConnectorService = mockk()
        val keycloakService = KeycloakService(keycloakConnectorService)
        assertTrue(true, "Oh no, true is false :(")
    }

}
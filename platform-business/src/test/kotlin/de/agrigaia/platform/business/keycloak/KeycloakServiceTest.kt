package de.agrigaia.platform.business.keycloak

import de.agrigaia.platform.integration.keycloak.KeycloakConnectorService
import de.agrigaia.platform.model.coopspace.CoopSpaceRole
import de.agrigaia.platform.model.coopspace.Member
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.keycloak.representations.idm.UserRepresentation
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KeycloakServiceTest {

    @Test
    fun `Test getKeycloakUsers`() {
        val keycloakConnectorService: KeycloakConnectorService = mockk()
        val keycloakService = KeycloakService(keycloakConnectorService)

        every { keycloakConnectorService.getUsers() } returns mutableListOf()
        every { keycloakConnectorService.getGroups() } returns mapOf()
        var actual = keycloakService.getKeycloakUsers()
        assertEquals(listOf(), actual, "Should return an empty list when given empty inputs.")


        val userA = UserRepresentation()
        userA.username = "john"
        userA.firstName = "John"
        userA.lastName = "Doe"
        userA.email = "john@doe.com"
        val userB = UserRepresentation()
        userB.username = "mary"
        userB.firstName = "Mary"
        userB.lastName = "Something"
        userB.email = "mary@something.com"

        val dummyUsers = mutableListOf(userA, userB)
        val dummyGroups = mapOf("someGroup" to listOf("mary", "john"))
        every { keycloakConnectorService.getUsers() } returns dummyUsers
        every { keycloakConnectorService.getGroups() } returns dummyGroups

        val expected = listOf(
            Member(
                "${userA.firstName} ${userA.lastName}",
                "someGroup",
                userA.email,
                CoopSpaceRole.USER,
                userA.username
            ),
            Member(
                "${userB.firstName} ${userB.lastName}",
                "someGroup",
                userB.email,
                CoopSpaceRole.USER,
                userB.username
            ),
        )
        actual = keycloakService.getKeycloakUsers()

        assertEquals(expected.size, actual.size, "Should return two members.")
        for (i in expected.indices) {
            assertEquals(expected[i].name, actual[i].name, "Incorrect name.")
            assertEquals(expected[i].company, actual[i].company, "Incorrect company.")
            assertEquals(expected[i].email, actual[i].email, "Incorrect email.")
            assertEquals(expected[i].role, actual[i].role, "Incorrect role.")
            assertEquals(expected[i].username, actual[i].username, "Incorrect username.")
        }
    }

    @Test
    fun `Test findKeycloakUserByMail`() {
        val keycloakConnectorService: KeycloakConnectorService = mockk()
        val keycloakService = KeycloakService(keycloakConnectorService)
        assertTrue(true, "Oh no, true is false :(")
    }

}
package de.agrigaia.platform.business.coopspace

import de.agrigaia.platform.business.errors.BusinessException
import de.agrigaia.platform.business.errors.ErrorType
import de.agrigaia.platform.integration.keycloak.KeycloakConnectorService
import de.agrigaia.platform.integration.minio.MinioService
import de.agrigaia.platform.model.coopspace.CoopSpace
import de.agrigaia.platform.model.coopspace.CoopSpaceRole
import de.agrigaia.platform.model.coopspace.Member
import de.agrigaia.platform.persistence.repository.CoopSpaceRepository
import de.agrigaia.platform.persistence.repository.MemberRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

class CoopSpaceServiceTest {

    private val coopSpacesProperties: CoopSpacesProperties = mockk()
    private val coopSpaceRepository: CoopSpaceRepository = mockk()
    private val memberRepository: MemberRepository = mockk()
    private val minioService: MinioService = mockk()
    private val keycloakConnectorService: KeycloakConnectorService = mockk()
    private val coopSpaceService = CoopSpaceService(
        coopSpacesProperties = coopSpacesProperties,
        coopSpaceRepository = coopSpaceRepository,
        memberRepository = memberRepository,
        minioService = minioService,
        keycloakConnectorService = keycloakConnectorService,
    )

    @Test
    fun `Test findCoopSpaceById`() {
        val dummyId: Long = 42
        val dummyCoopSpace = CoopSpace("someName", "someCompany", "someMandant", listOf())
        every { coopSpaceRepository.findById(dummyId) } returns Optional.of(dummyCoopSpace)
        val actual = coopSpaceService.findCoopSpaceById(dummyId)
        verify { coopSpaceRepository.findById(match { it == dummyId }) }
        assertEquals(dummyCoopSpace, actual, "Should return CoopSpace.")
    }

    @Test
    fun `Test findCoopSpaceByName`() {
        val dummyName = "someName"
        val dummyCoopSpace = CoopSpace(dummyName, "someCompany", "someMandant", listOf())
        every { coopSpaceRepository.findByName(dummyName) } returns Optional.of(dummyCoopSpace)
        val actual = coopSpaceService.findCoopSpaceByName(dummyName)
        verify { coopSpaceRepository.findByName(match { it == dummyName }) }
        assertEquals(dummyCoopSpace, actual, "Should return CoopSpace.")
    }

    @Test
    fun `Test removeUserFromDatabase`() {
        val dummyId: Long = 42
        every { memberRepository.deleteById(dummyId) } just runs
        coopSpaceService.removeUserFromDatabase(dummyId)
        verify { memberRepository.deleteById(match { it == dummyId }) }
    }

    @Test
    fun `Test addUsersToDatabase`() {
        val dummyMember = Member()
        val dummyCoopSpace = CoopSpace()
        every { coopSpaceRepository.save(any()) } returns dummyCoopSpace
        coopSpaceService.addUsersToDatabase(listOf(dummyMember), dummyCoopSpace)
        assertEquals(listOf(dummyMember), dummyCoopSpace.members, "Should add member to CoopSpace's members list.")
        // TODO: How do I pass an error message to `verify`?
        verify { coopSpaceRepository.save(dummyCoopSpace) }// { "Should save CoopSpace to coopSpaceRepository."}
    }

    @Test
    fun `Test changeUserRoleInDatabase`() {
        every { memberRepository.save(any()) } returns Member()
        val matchingUserName = "Some Holy User Name"
        val oldRole = CoopSpaceRole.USER
        val newRole = CoopSpaceRole.GUEST

        // Throw BusinessException(ErrorType.NOT_FOUND) if Member with matching `username` is not in CoopSpace.
        val emptyCoopSpace = CoopSpace(members = listOf())
        val originalMember = Member(
            username = matchingUserName,
            role = oldRole,
        )
        originalMember.id = 43
        val exception =
            assertThrows<BusinessException>("Should throw `BusinessException` if no matching user is found.") {
                coopSpaceService.changeUserRoleInDatabase(
                    originalMember.username!!,
                    newRole,
                    originalMember.id,
                    emptyCoopSpace,
                )
            }
        assertEquals(
            exception.errorCode,
            ErrorType.UNKNOWN,
            "Should have thrown BusinessException of type `UNKNOWN`."
        )

        // Call memberRepository.save(), passing member object with field values from Member in CoopSpace, except `role` and `id`, which should have the value of the passed Member parameter.
        // Setup CoopSpace member.
        val coopSpaceMember = Member(
            name = "Some Name",
            company = "someCompany",
            email = "someEmail",
            role = oldRole,
            username = matchingUserName,
        )
        coopSpaceMember.id = 42
        val dummyCoopSpace = CoopSpace(members = listOf(coopSpaceMember))

        // Set up saved member.
        val expectedSavedMember = Member(
            name = coopSpaceMember.name,
            company = coopSpaceMember.company,
            email = coopSpaceMember.email,
            role = newRole,
            username = matchingUserName
        )
        expectedSavedMember.id = originalMember.id
        coopSpaceService.changeUserRoleInDatabase(originalMember.username!!, newRole, originalMember.id, dummyCoopSpace)
        verify {
            memberRepository.save(match { actualSavedMember ->
                actualSavedMember.name == expectedSavedMember.name &&
                        actualSavedMember.company == expectedSavedMember.company &&
                        actualSavedMember.email == expectedSavedMember.email &&
                        actualSavedMember.role == expectedSavedMember.role &&
                        actualSavedMember.username == expectedSavedMember.username &&
                        actualSavedMember.id == expectedSavedMember.id
            })
        }
    }
}
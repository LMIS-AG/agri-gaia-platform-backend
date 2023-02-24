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
import io.minio.messages.Bucket
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoopSpaceServiceTest {
    @Test
    fun `Test filterCoopSpacesByBucketAccess`() {
        val coopSpacesProperties: CoopSpacesProperties = mockk()
        val coopSpaceRepository: CoopSpaceRepository = mockk()
        val memberRepository: MemberRepository = mockk()
        val minioService: MinioService = mockk()
        val keycloakConnectorService: KeycloakConnectorService = mockk()
        val coopSpaceService = CoopSpaceService(
            coopSpacesProperties = coopSpacesProperties,
            coopSpaceRepository = coopSpaceRepository,
            memberRepository = memberRepository,
            minioService = minioService,
            keycloakConnectorService = keycloakConnectorService,
        )

        // Should return empty list if either input list is empty.
        var actual = coopSpaceService.filterCoopSpacesByBucketAccess(listOf(), listOf())
        assertEquals(listOf<CoopSpace>(), actual, "Should return an empty list when given empty lists.")
        actual = coopSpaceService.filterCoopSpacesByBucketAccess(listOf(), listOf(Bucket()))
        assertEquals(listOf<CoopSpace>(), actual, "Should return an empty list when given no coop spaces.")
        actual = coopSpaceService.filterCoopSpacesByBucketAccess(listOf(CoopSpace()), listOf())
        assertEquals(listOf<CoopSpace>(), actual, "Should return an empty list when given no buckets.")

        val a: Bucket = mockk()
        val b: Bucket = mockk()
        every { a.name() } returns "prj-lmis-inaccessibleprojectname"
        every { b.name() } returns "prj-lmis-projectname"
        val dummyBuckets = listOf(a, b)

        val c: CoopSpace = mockk()
        val d: CoopSpace = mockk()
        every { c.company } returns "lmis"
        every { c.name } returns "blub"
        every { d.company } returns "lmis"
        every { d.name } returns "projectname"
        val dummyCoopSpaces = listOf(c, d)

        val expected = listOf(d)
        actual = coopSpaceService.filterCoopSpacesByBucketAccess(dummyCoopSpaces, dummyBuckets)

        assertEquals(expected, actual, "Should remove coopSpaces the user has no access to.")
    }

    @Test
    fun `Test findCoopSpace`() {
        val coopSpacesProperties: CoopSpacesProperties = mockk()
        val coopSpaceRepository: CoopSpaceRepository = mockk()
        val memberRepository: MemberRepository = mockk()
        val minioService: MinioService = mockk()
        val keycloakConnectorService: KeycloakConnectorService = mockk()
        val coopSpaceService = CoopSpaceService(
            coopSpacesProperties = coopSpacesProperties,
            coopSpaceRepository = coopSpaceRepository,
            memberRepository = memberRepository,
            minioService = minioService,
            keycloakConnectorService = keycloakConnectorService,
        )
        val dummyId: Long = 42
        val dummyCoopSpace = CoopSpace("someName", "someCompany", "someMandant", listOf())
        every { coopSpaceRepository.findById(dummyId) } returns Optional.of(dummyCoopSpace)
        val actual = coopSpaceService.findCoopSpace(dummyId)
        verify { coopSpaceRepository.findById(match { it == dummyId }) }
        assertEquals(dummyCoopSpace, actual, "Should return CoopSpace.")
    }

    @Test
    fun `Test removeUserFromDatabase`() {
        val coopSpacesProperties: CoopSpacesProperties = mockk()
        val coopSpaceRepository: CoopSpaceRepository = mockk()
        val memberRepository: MemberRepository = mockk()
        val minioService: MinioService = mockk()
        val keycloakConnectorService: KeycloakConnectorService = mockk()
        val coopSpaceService = CoopSpaceService(
            coopSpacesProperties = coopSpacesProperties,
            coopSpaceRepository = coopSpaceRepository,
            memberRepository = memberRepository,
            minioService = minioService,
            keycloakConnectorService = keycloakConnectorService,
        )

        val dummyId: Long = 42
        every { memberRepository.deleteById(dummyId) } just runs

        coopSpaceService.removeUserFromDatabase(dummyId)
        verify { memberRepository.deleteById(match { it == dummyId }) }
    }

    @Test
    fun `Test addUsersToDatabase`() {
        val coopSpacesProperties: CoopSpacesProperties = mockk()
        val coopSpaceRepository: CoopSpaceRepository = mockk()
        val memberRepository: MemberRepository = mockk()
        val minioService: MinioService = mockk()
        val keycloakConnectorService: KeycloakConnectorService = mockk()
        val coopSpaceService = CoopSpaceService(
            coopSpacesProperties = coopSpacesProperties,
            coopSpaceRepository = coopSpaceRepository,
            memberRepository = memberRepository,
            minioService = minioService,
            keycloakConnectorService = keycloakConnectorService,
        )
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
        val coopSpacesProperties: CoopSpacesProperties = mockk()
        val coopSpaceRepository: CoopSpaceRepository = mockk()
        val memberRepository: MemberRepository = mockk()
        val minioService: MinioService = mockk()
        val keycloakConnectorService: KeycloakConnectorService = mockk()
        val coopSpaceService = CoopSpaceService(
            coopSpacesProperties = coopSpacesProperties,
            coopSpaceRepository = coopSpaceRepository,
            memberRepository = memberRepository,
            minioService = minioService,
            keycloakConnectorService = keycloakConnectorService,
        )
        every { memberRepository.save(any()) } returns Member()
        val matchingUserName = "Some Holy User Name"

        // Throw BusinessException(ErrorType.NOT_FOUND) if Member with matching `username` is not in CoopSpace.
        val memberlessCoopspace = CoopSpace(members = listOf())
        val passedMember = Member(
            username = matchingUserName,
            role = CoopSpaceRole.USER,
        )
        passedMember.id = 43
        val exception =
            assertThrows<BusinessException>("Should throw `BusinessException` if no matching user is found.") {
                coopSpaceService.changeUserRoleInDatabase(
                    passedMember,
                    memberlessCoopspace
                )
            }
        assertEquals(
            exception.errorCode,
            ErrorType.NOT_FOUND,
            "Should have thrown BusinessException of type `NOT_FOUND`."
        )

        // Call memberRepository.save(), passing member object with field values from Member in CoopSpace, except `role` and `id`, which should have the value of the passed Member parameter.
        // Setup CoopSpace member.
        val coopSpaceMember = Member(
            name = "Some Name",
            company = "someCompany",
            email = "someEmail",
            role = CoopSpaceRole.GUEST,
            username = matchingUserName
        )
        coopSpaceMember.id = 42
        val dummyCoopSpace = CoopSpace(members = listOf(coopSpaceMember))

        // Set up saved member.
        val expectedSavedMember = Member(
            name = coopSpaceMember.name,
            company = coopSpaceMember.company,
            email = coopSpaceMember.email,
            role = passedMember.role,
            username = matchingUserName
        )
        expectedSavedMember.id = passedMember.id
        coopSpaceService.changeUserRoleInDatabase(passedMember, dummyCoopSpace)
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

    @Test
    fun `Test hasAccessToCoopSpace`() {
        val coopSpacesProperties: CoopSpacesProperties = mockk()
        val coopSpaceRepository: CoopSpaceRepository = mockk()
        val memberRepository: MemberRepository = mockk()
        val minioService: MinioService = mockk()
        val keycloakConnectorService: KeycloakConnectorService = mockk()
        val coopSpaceService = CoopSpaceService(
            coopSpacesProperties = coopSpacesProperties,
            coopSpaceRepository = coopSpaceRepository,
            memberRepository = memberRepository,
            minioService = minioService,
            keycloakConnectorService = keycloakConnectorService,
        )

        val c: CoopSpace = mockk()
        every { c.members } returns listOf()
        assertFalse(coopSpaceService.hasAccessToCoopSpace("", c), "Should return false when given empty username.")
        assertFalse(
            coopSpaceService.hasAccessToCoopSpace("Antonia", c), "Cannot have access to coopSpace with no members."
        )

        every { c.members } returns listOf(Member(username = "Antonia"), Member(username = "Norbert"))
        assertFalse(coopSpaceService.hasAccessToCoopSpace("", c), "Should return false when given empty username.")
        assertTrue(
            coopSpaceService.hasAccessToCoopSpace("Antonia", c),
            "Should have access to coopSpace containing member with matching username."
        )
        assertFalse(coopSpaceService.hasAccessToCoopSpace("antonia", c), "Should be case-sensitive.")
    }
}
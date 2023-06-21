package de.agrigaia.platform.business.coopspace

import de.agrigaia.platform.business.errors.BusinessException
import de.agrigaia.platform.business.errors.ErrorType
import de.agrigaia.platform.common.HasLogger
import de.agrigaia.platform.integration.keycloak.KeycloakConnectorService
import de.agrigaia.platform.integration.minio.MinioService
import de.agrigaia.platform.model.coopspace.CoopSpace
import de.agrigaia.platform.model.coopspace.CoopSpaceRole
import de.agrigaia.platform.model.coopspace.Member
import de.agrigaia.platform.persistence.repository.CoopSpaceRepository
import de.agrigaia.platform.persistence.repository.MemberRepository
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.core.GrantedAuthority
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.body
import reactor.core.publisher.Mono

@Service
class CoopSpaceService(
    private val coopSpacesProperties: CoopSpacesProperties,
    private val coopSpaceRepository: CoopSpaceRepository,
    private val memberRepository: MemberRepository,
    private val minioService: MinioService,
    private val keycloakConnectorService: KeycloakConnectorService
) : HasLogger {
    private val webClient: WebClient = WebClient.create()

    fun getCoopSpacesWithUserAccess(coopSpaceAuthorities: List<GrantedAuthority>): List<CoopSpace> {
        val coopSpaceNames: List<String> =
            coopSpaceAuthorities.map { it.authority.substringAfter('-').substringBeforeLast('-') }
        return findAll().filter { coopSpaceNames.contains(it.name) }
    }

    fun createCoopSpace(coopSpace: CoopSpace, creator: Member): CoopSpace {
        creator.role = CoopSpaceRole.ADMIN
        val owners: MutableList<String> =
            coopSpace.members.filter { member: Member -> member.role == CoopSpaceRole.ADMIN && member.username != null }
                .map { member: Member -> member.username!! }
                .toMutableList()
        owners.add(creator.username!!)
        val editors: List<String> =
            coopSpace.members.filter { member: Member -> member.role == CoopSpaceRole.USER && member.username != null }
                .map { member: Member -> member.username!! }
        val viewers: List<String> =
            coopSpace.members.filter { member: Member -> member.role == CoopSpaceRole.GUEST && member.username != null }
                .map { member: Member -> member.username!! }


        // TODO: Abominations be gone!!
        val body = object {
            val mandant = object {
                val username = coopSpace.mandant
            }
            val coop_room = object {
                val name = coopSpace.name
                val organisation = object {
                    val name = coopSpace.company
                }
                val users_in_role = listOf<UsersInRole>(
                    UsersInRole("Admin", owners),
                    UsersInRole("User", editors),
                    UsersInRole("Guest", viewers)
                )
            }
            val delete_bucket = true
            val upload_policies = true
            val no_bucket = false
        }

        val response = webClient.post()
            .uri(this.coopSpacesProperties.createCoopSpaceUrl!!)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.TEXT_PLAIN)
            .body(Mono.just(body))
            .retrieve()
            .onStatus({ it.is4xxClientError }, ::handleClientError)
            .onStatus({ it.is5xxServerError }, ::handleClientError)
            .bodyToMono(String::class.java)
            .block()
        // FIXME Properly handle errors

        val members = coopSpace.members.toMutableList()
        members.add(creator)
        coopSpace.members = members

        return this.coopSpaceRepository.save(coopSpace)
    }

    private fun handleClientError(clientResponse: ClientResponse): Mono<out Throwable> {
        getLogger().error("Got error during REST call: ${clientResponse.statusCode()}")
        return clientResponse.createException()
    }

    fun deleteCoopSpace(jwt: String, coopSpace: CoopSpace) {
        val assetsForBucket =
            this.minioService.getAssetsForCoopspace(
                jwt,
                coopSpace.company!!.lowercase(),
                coopSpace.name!!,
                folder = "/assets"
            )
        if (assetsForBucket.isNotEmpty()) {
            throw BusinessException("Cannot delete bucket with assets inside", ErrorType.BUCKET_NOT_EMPTY)
        }

        val body = object {
            val mandant = object {
                val username = coopSpace.mandant
            }
            val coop_room = object {
                val name = coopSpace.name
                val organisation = object {
                    val name = coopSpace.company
                }
            }
            val delete_bucket = true
            val upload_policies = true
            val no_bucket = false
        }

        val response = webClient.post()
            .uri(this.coopSpacesProperties.deleteCoopSpaceUrl!!) // TODO move into config
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.TEXT_PLAIN)
            .body(Mono.just(body))
            .retrieve()
            .onStatus({ it.is4xxClientError }, ::handleClientError)
            .onStatus({ it.is5xxServerError }, ::handleClientError)
            .bodyToMono(String::class.java)
            .block()
        // FIXME Properly handle errors
        this.coopSpaceRepository.delete(coopSpace)
    }

    fun findAll(): List<CoopSpace> {
        return this.coopSpaceRepository.findAll()
    }

    fun findCoopSpaceById(id: Long): CoopSpace {
        return coopSpaceRepository
            .findById(id)
            .orElseThrow { BusinessException("CoopSpace with id $id does not exist.", ErrorType.NOT_FOUND) }
    }

    fun findCoopSpaceByName(name: String): CoopSpace {
        return coopSpaceRepository
            .findByName(name)
            .orElseThrow { BusinessException("CoopSpace with name $name does not exist.", ErrorType.NOT_FOUND) }
    }

    fun removeUserFromKeycloakGroup(username: String, role: String, companyName: String, coopSpaceName: String) {
        this.keycloakConnectorService.removeUserFromGroup(
            username,
            role,
            companyName,
            coopSpaceName,
        )
    }

    fun removeUserFromDatabase(id: Long) {
        return memberRepository
            .deleteById(id)
    }

    /**
     * add a list of users or a list containing a single user to a Keycloak subgroup by calling the "addUserToKeycloakGroup"
     * as often as necessary
     */
    fun addUsersToKeycloakGroup(memberList: List<Member> = ArrayList(), coopSpaceName: String, companyName: String) {
        for (member in memberList) {
            val username = member.username ?: throw BusinessException("Member $member has no username", ErrorType.BAD_REQUEST)
            val role = member.role ?: throw BusinessException("Member $member has no role", ErrorType.BAD_REQUEST)
            addUserToKeycloakGroup(username, role, companyName, coopSpaceName)
        }
    }

    /**
     * add a single user to a Keycloak subgroup, this function gets called directly when changing the role of a user
     */
    fun addUserToKeycloakGroup(username: String, role: CoopSpaceRole, company: String, coopSpaceName: String) {
        keycloakConnectorService.addUserToGroup(username, role.toString(), coopSpaceName, company)
    }

    fun addUsersToDatabase(memberList: List<Member> = ArrayList(), coopSpace: CoopSpace) {
        val members = coopSpace.members.toMutableList()

        members.addAll(memberList)
        coopSpace.members = members

        this.coopSpaceRepository.save(coopSpace)
    }

    /**
     * change role in the database
     */
    fun changeUserRoleInDatabase(username: String, newRole: CoopSpaceRole, id: Long, coopSpace: CoopSpace) {
        val member = coopSpace.members.find { it.username == username }
            ?: throw BusinessException("Member with username $username not found in coopSpace", ErrorType.UNKNOWN)
        member.role = newRole
        member.id = id
        this.memberRepository.save(member)
    }
}

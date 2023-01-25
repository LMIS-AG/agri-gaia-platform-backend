package de.agrigaia.platform.business.coopspace

import de.agrigaia.platform.business.errors.BusinessException
import de.agrigaia.platform.business.errors.ErrorType
import de.agrigaia.platform.common.HasLogger
import de.agrigaia.platform.integration.coopspace.UsersInRole
import de.agrigaia.platform.integration.minio.MinioService
import de.agrigaia.platform.model.coopspace.CoopSpace
import de.agrigaia.platform.model.coopspace.CoopSpaceRole
import de.agrigaia.platform.model.coopspace.Member
import de.agrigaia.platform.persistence.repository.CoopSpaceRepository
import io.minio.messages.Bucket
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.body
import reactor.core.publisher.Mono

@Service
class CoopSpaceService(
    private val coopSpacesProperties: CoopSpacesProperties,
    private val coopSpaceRepository: CoopSpaceRepository,
    private val minioService: MinioService
): HasLogger {
    private val webClient: WebClient = WebClient.create()

    /*
     * Returns only those CoopSpaces where the user has access to the corresponding bucket.
     * I.e. all they are allowed to see.
     */
    fun filterCoopSpacesByBucketAccess(coopSpaces: List<CoopSpace>, buckets: List<Bucket>): List<CoopSpace> {
        return coopSpaces.filter {
            buckets.any(fun(bucket: Bucket): Boolean {
                return bucket.name() == "prj-${it.company?.lowercase()}-${it.name}"
            })
        }
    }

    fun createCoopSpace(coopSpace: CoopSpace, creator: Member) {
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
            .onStatus(HttpStatus::is4xxClientError, ::handleClientError)
            .onStatus(HttpStatus::is5xxServerError, ::handleClientError)
            .bodyToMono(String::class.java)
            .block()
        // FIXME Properly handle errors

        val members = coopSpace.members.toMutableList()
        members.add(creator)
        coopSpace.members = members

        this.coopSpaceRepository.save(coopSpace)

    }

    private fun handleClientError(clientResponse: ClientResponse): Mono<out Throwable> {
        getLogger().error("Got error during REST call: ${clientResponse.statusCode()}")
        return clientResponse.createException()
    }

    fun deleteCoopSpace(jwt: String, coopSpace: CoopSpace) {
        val assetsForBucket =
            this.minioService.getAssetsForCoopspace(jwt, coopSpace.company!!.lowercase(), coopSpace.name!!)
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
            .onStatus(HttpStatus::is4xxClientError, ::handleClientError)
            .onStatus(HttpStatus::is5xxServerError, ::handleClientError)
            .bodyToMono(String::class.java)
            .block()
        // FIXME Properly handle errors
        this.coopSpaceRepository.delete(coopSpace)
    }

    fun findAll(): List<CoopSpace> {
        return this.coopSpaceRepository.findAll()
    }

    fun findCoopSpace(id: Long): CoopSpace {
        return coopSpaceRepository
            .findById(id)
            .orElseThrow { BusinessException("CoopSpace with id $id does not exist.", ErrorType.NOT_FOUND) }
    }

    fun deleteMember(memberId: Number) {
        // hier irgendetwas mit dem MemberRepository machen?!
    }


}

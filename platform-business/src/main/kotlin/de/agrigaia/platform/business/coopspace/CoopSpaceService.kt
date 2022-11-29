package de.agrigaia.platform.business.coopspace

import de.agrigaia.platform.business.errors.BusinessException
import de.agrigaia.platform.business.errors.ErrorType
import de.agrigaia.platform.integration.coopspace.UsersInRole
import de.agrigaia.platform.integration.minio.MinioService
import de.agrigaia.platform.model.coopspace.CoopSpace
import de.agrigaia.platform.model.coopspace.CoopSpaceRole
import de.agrigaia.platform.model.coopspace.Member
import de.agrigaia.platform.persistence.repository.CoopSpaceRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.body
import reactor.core.publisher.Mono

@Service
class CoopSpaceService @Autowired constructor(
    private val coopSpaceRepository: CoopSpaceRepository,
    private val minioService: MinioService
) {
    private val webClient: WebClient = WebClient.create();
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun createCoopSpace(coopSpace: CoopSpace) {
        val owners: List<String> =
            coopSpace.members.filter { member: Member -> member.role == CoopSpaceRole.OWNER && member.username != null }.map { member: Member -> member.username!! }
        val editors: List<String> =
            coopSpace.members.filter { member: Member -> member.role == CoopSpaceRole.EDITOR && member.username != null }.map { member: Member -> member.username!! }
        val viewers: List<String> =
            coopSpace.members.filter { member: Member -> member.role == CoopSpaceRole.VIEWER && member.username != null }.map { member: Member -> member.username!! }


        val body = object {
            val mandant = object {
                val username = coopSpace.mandant
            }
            val coop_room = object {
                val name = coopSpace.name
                val organisation = object {
                    val name = coopSpace.company
                }
                val users_in_role = listOf<UsersInRole>(UsersInRole("Admin", owners), UsersInRole("User", editors), UsersInRole("Guest", viewers))
            }
            val delete_bucket = true;
            val upload_policies = true;
            val no_bucket = false;
        }

        try {
            val response = webClient.post()
                .uri("https://create-cooperation-room-eventsource.platform.agri-gaia.com") // TODO move into config
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.TEXT_PLAIN)
                .body(Mono.just(body))
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError) { clientResponse -> handleClientError(clientResponse) }
                .onStatus(HttpStatus::is5xxServerError) { clientResponse -> handleClientError(clientResponse) }
                .bodyToMono(String::class.java)
                .block()
            logger.info(response.toString());
            this.coopSpaceRepository.save(coopSpace)
        } catch (e: Exception) {
            logger.error(e.stackTrace.toString())
        }
    }

    fun handleClientError(clientResponse: ClientResponse): Mono<out Throwable> {
        logger.info("Error " + clientResponse.rawStatusCode())
        return clientResponse.createException()
    }

    fun deleteCoopSpace(jwt: String, coopSpace: CoopSpace) {
        val assetsForBucket = this.minioService.getAssetsForBucket(jwt, coopSpace.company!!.lowercase(), coopSpace.name!!)
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
            val delete_bucket = true;
            val upload_policies = true;
            val no_bucket = false;
        }

        try {
            val response = webClient.post()
                .uri("https://delete-cooperation-room-eventsource.platform.agri-gaia.com") // TODO move into config
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.TEXT_PLAIN)
                .body(Mono.just(body))
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError) { clientResponse -> handleClientError(clientResponse) }
                .onStatus(HttpStatus::is5xxServerError) { clientResponse -> handleClientError(clientResponse) }
                .bodyToMono(String::class.java)
                .block()
            logger.info(response.toString());
            this.coopSpaceRepository.delete(coopSpace)
        } catch (e: Exception) {
            logger.error(e.stackTrace.toString())
        }
    }

    fun findAllBySomething(): List<CoopSpace> {
        return this.coopSpaceRepository.findAll()
    }

    fun findCoopSpace(id: Long): CoopSpace {
        return coopSpaceRepository
            .findById(id)
            .orElseThrow { BusinessException("Error Message", ErrorType.NOT_FOUND) }
    }

    fun getMembers(id: Long): List<Member> {
        return this.coopSpaceRepository
            .findById(id)
            .orElseThrow { BusinessException("Not Found", ErrorType.NOT_FOUND) }
            .members
    }
}
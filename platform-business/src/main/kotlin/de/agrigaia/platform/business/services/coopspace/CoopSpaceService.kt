package de.agrigaia.platform.business.services.coopspace

import de.agrigaia.platform.model.coopspace.CoopSpace
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.*
import reactor.core.publisher.Mono

@Service
class CoopSpaceService @Autowired constructor() {
    private val webClient: WebClient = WebClient.create();
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun createCoopSpace(coopSpace: CoopSpace) {

        var body = object {
            val mandant = object {
                val username = coopSpace.mandant
            }
            val coop_room = object {
                val name = coopSpace.name
                val organisation = object {
                    val name = coopSpace.company
                }
                val users_in_role = listOf<UsersInRole>(UsersInRole("Admin", listOf()), UsersInRole("User", listOf()), UsersInRole("Guest", listOf()))
            }
            val delete_bucket = true;
            val upload_policies = true;
            val no_bucket = false;
        }

        try {
            var response = webClient.post()
                .uri("https://create-cooperation-room-eventsource.platform.agri-gaia.com") // TODO move into config
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.TEXT_PLAIN)
                .body(Mono.just(body))
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError) { clientResponse -> handleClientError(clientResponse) }
                .onStatus(HttpStatus::is5xxServerError) { clientResponse -> handleClientError(clientResponse) }
                .bodyToMono(String.javaClass) // Content type 'text/html;charset=utf-8' not supported for bodyType=kotlin.jvm.internal.StringCompanionObject
                .block()
            logger.info(response.toString());
        } catch (e: Exception){
            logger.error(e.stackTrace.toString())
        }
    }

    fun handleClientError(clientResponse: ClientResponse): Mono<out Throwable> {
        logger.info("Error " + clientResponse.rawStatusCode())
        return clientResponse.createException()
    }

}
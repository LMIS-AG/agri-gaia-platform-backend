package de.agrigaia.platform.business.services.coopspace

import de.agrigaia.platform.model.coopspace.CoopSpace
import de.agrigaia.platform.persistence.repository.ExampleRepository
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

    // TODO rename
    fun log(coopSpace: CoopSpace) {

        // TODO surround with try catch block -> because text/plain throws exception. Response is always the same.

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

        var x = webClient.post()
            .uri("https://create-cooperation-room-eventsource.platform.agri-gaia.com") // TODO move into config
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.TEXT_PLAIN)
            .body(Mono.just(body)) //.bodyMono.just(coopSpace), CoopSpace::class.java)
            .retrieve()
            .onStatus(HttpStatus::is4xxClientError) { test4xx() }
            .onStatus(HttpStatus::is5xxServerError) { test5xx() }
            .bodyToMono(String.javaClass) // Content type 'text/html;charset=utf-8' not supported for bodyType=kotlin.jvm.internal.StringCompanionObject
            .block()

        logger.info(x.toString());
    }

    fun test4xx(): Mono<out Throwable> {
        logger.info("Error 4xx")
        return TODO("Provide the return value - 4xx")
    }

    fun test5xx(): Mono<out Throwable> {
        logger.info("Error 5xx")
        return TODO("Provide the return value - 5xx")
    }

    fun test(coopSpace: CoopSpace) {
        logger.info("Test CoopSpaceService") // TODO remove
    }

}

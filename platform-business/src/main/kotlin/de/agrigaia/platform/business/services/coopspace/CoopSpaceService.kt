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
import java.util.stream.Collectors

@Service
class CoopSpaceService @Autowired constructor(private val exampleRepository: ExampleRepository) {
    private val webClient: WebClient = WebClient.create();
    private val logger = LoggerFactory.getLogger(this::class.java)


    // TODO remove later
    fun log(coopSpace: CoopSpace) {

        var body = object{
            val oidc_name = coopSpace.name
            val oidc_organisation = coopSpace.company
            val members = coopSpace.members.stream().map { it -> it.name }.collect(Collectors.toList())
        }

        var x = webClient.post()
            .uri("https://kr-eventsource-argo-events.platform.agri-gaia.com/example")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.TEXT_PLAIN) // no effect!?
            .body(Mono.just(body)) //.bodyMono.just(coopSpace), CoopSpace::class.java)
            .retrieve()
            .onStatus(HttpStatus::is4xxClientError) { test4xx() }
            .onStatus(HttpStatus::is5xxServerError) { test5xx() }
            .bodyToMono(String.javaClass) // still not accepted
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

package de.agrigaia.platform.business.services.coopspace

import de.agrigaia.platform.model.coopspace.CoopSpace
import de.agrigaia.platform.persistence.repository.ExampleRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.*
import reactor.core.publisher.Mono

@Service
class CoopSpaceService @Autowired constructor(private val exampleRepository: ExampleRepository) {
    private val webClient: WebClient = WebClient.create();

    // TODO remove later
    fun log(mock: Mock) {

        println("Log CoopSpaceService")

        var x = webClient.post()
            .uri("https://kr-eventsource-argo-events.platform.agri-gaia.com/example")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.TEXT_PLAIN) // no effect!?
            .body(Mono.just(mock), Mock::class.java)
            .retrieve()
            .onStatus(HttpStatus::is4xxClientError) { test4xx() }
            .onStatus(HttpStatus::is5xxServerError) { test5xx() }
            .bodyToMono(String.javaClass) // still not accepted
            .block()

        println(x);
    }

    fun test4xx(): Mono<out Throwable> {
        println("Error 4xx")
        return TODO("Provide the return value - 4xx")
    }

    fun test5xx(): Mono<out Throwable> {
        println("Error 4xx")
        return TODO("Provide the return value - 5xx")
    }

    fun test(coopSpace: CoopSpace) {
        println("Test CoopSpaceService") // TODO remove
    }

}


data class Mock(
    var message: String?
)
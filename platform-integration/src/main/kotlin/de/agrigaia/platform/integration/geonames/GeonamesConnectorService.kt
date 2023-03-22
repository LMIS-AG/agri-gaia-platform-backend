package de.agrigaia.platform.integration.geonames

import com.fasterxml.jackson.databind.ObjectMapper
import de.agrigaia.platform.common.HasLogger
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

class GeonamesConnectorService : HasLogger {

    private val webClient: WebClient = WebClient.create()
    private val geonamesEndpoint: String = "https://fuseki.platform.agri-gaia.com/geonames"

    private fun getUriFromCoordinates(latitude: String, longitude: String): String {
        // NOTE: LIMIT is a safety measure to avoid accidentally fetching petabytes of data.
        // The query should only ever return one row.
        val query: String = """
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>
            SELECT ?sub WHERE {
                ?sub geo:lat '${latitude}' .
                ?sub geo:long '${longitude}' .
            } LIMIT 10
       """.trimIndent()
        return sendGeonamesRequest(query)
    }

    private fun sendGeonamesRequest(query: String): String {
        val body = LinkedMultiValueMap<String, String>()
        body.add("query", query)

        val request = this.webClient.post()
            .uri(geonamesEndpoint)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData(body))

        val response: String = request
            .retrieve()
            .onStatus(HttpStatus::is4xxClientError, ::handleClientError)
            .onStatus(HttpStatus::is5xxServerError, ::handleServerError)
            .bodyToMono(String::class.java)
            .block() ?: throw Exception("Response from Geonames was null.")

        return extractUriFromGeonamesResponseBody(response)
    }

    private fun extractUriFromGeonamesResponseBody(response: String): String {
        val objectMapper = ObjectMapper()
        val map = objectMapper.readValue(response, Map::class.java)
        // This abomination extracts the URI from the response returned by Geonames.
        return ((((map["results"] as Map<*, *>)["bindings"] as ArrayList<*>)[0] as Map<*, *>)["sub"] as Map<*, *>)["value"].toString()
    }

    private fun handleClientError(clientResponse: ClientResponse): Mono<out Throwable>? {
        return clientResponse.bodyToMono(String::class.java)
            .doOnNext { getLogger().error("${clientResponse.statusCode()}: $it") }
            .then(clientResponse.createException())
    }

    private fun handleServerError(clientResponse: ClientResponse): Mono<out Throwable>? =
        handleClientError(clientResponse)
}

package de.agrigaia.platform.integration.fuseki

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.web.reactive.function.client.ClientResponse
import reactor.core.publisher.Mono
import de.agrigaia.platform.common.HasLogger
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient

class FusekiConnectorService : HasLogger {

    private val webClient: WebClient = WebClient.create()
    private val agrovocEndpoint: String = "https://fuseki.platform.agri-gaia.com/ds"
    private val geonamesEndpoint: String = "https://fuseki.platform.agri-gaia.com/geonames"

    fun getConceptUriFromKeyword(keyword: String): String {
        val labelUri = getLabelUriFromKeyword(keyword.lowercase())
        return getConceptUriFromLabelUri(labelUri)
    }

    private fun getLabelUriFromKeyword(keyword: String): String {
        // NOTE: LIMIT is a safety measure to avoid accidentally fetching petabytes of data.
        // The query should only ever return one row.
        val query: String = """
            PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>
            SELECT ?sub WHERE
            {
                ?sub skosxl:literalForm ?obj
                Filter(LCASE(STR(?obj))=LCASE('${keyword}'))
            } LIMIT 10
       """.trimIndent()
        return sendRequest(query, agrovocEndpoint)
    }

    private fun getConceptUriFromLabelUri(labelUri: String): String {
        // NOTE: LIMIT is a safety measure to avoid accidentally fetching petabytes of data.
        // The query should only ever return one row.
        val query: String = """
           PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>
           SELECT ?sub WHERE {
             {
               ?sub skosxl:prefLabel <$labelUri>
             }Union{
                   ?sub skosxl:altLabel <$labelUri>
             }
           } LIMIT 10
       """.trimIndent()
        return sendRequest(query, agrovocEndpoint)
    }

    fun getUriFromCoordinates(latitude: String, longitude: String): String {
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
        return sendRequest(query, geonamesEndpoint)
    }


    private fun sendRequest(query: String, endpoint: String): String {
        val body = LinkedMultiValueMap<String, String>()
        body.add("query", query)

        val request = this.webClient.post()
            .uri(endpoint)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData(body))

        val response: String = request
            .retrieve()
            .onStatus(HttpStatus::is4xxClientError, ::handleClientError)
            .onStatus(HttpStatus::is5xxServerError, ::handleServerError)
            .bodyToMono(String::class.java)
            .block() ?: throw Exception("Response from $endpoint was null.")

        return extractUriFromResponseBody(response)
    }

    private fun extractUriFromResponseBody(response: String): String {
        val objectMapper = ObjectMapper()
        val map = objectMapper.readValue(response, Map::class.java)
        // This abomination extracts the URI from the response returned by the respective ontology.
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

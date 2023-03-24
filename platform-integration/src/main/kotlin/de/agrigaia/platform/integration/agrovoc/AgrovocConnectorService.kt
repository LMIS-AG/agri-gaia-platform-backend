package de.agrigaia.platform.integration.agrovoc

import com.fasterxml.jackson.databind.ObjectMapper
import de.agrigaia.platform.common.HasLogger
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Service
class AgrovocConnectorService : HasLogger {
    private val webClient: WebClient = WebClient.create()
    private val agrovocEndpoint: String = "https://fuseki.platform.agri-gaia.com/ds"

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
        return sendAgrovocRequest(query)
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
        return sendAgrovocRequest(query)
    }

    private fun sendAgrovocRequest(query: String): String {
        val body = LinkedMultiValueMap<String, String>()
        body.add("query", query)

        val request = this.webClient.post()
            .uri(agrovocEndpoint)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData(body))

        val response: String = request
            .retrieve()
            .onStatus(HttpStatus::is4xxClientError, ::handleClientError)
            .onStatus(HttpStatus::is5xxServerError, ::handleServerError)
            .bodyToMono(String::class.java)
            .block() ?: throw Exception("Response from Agrovoc was null.")

        return extractUriFromAgrovocResponseBody(response)
    }

    private fun extractUriFromAgrovocResponseBody(response: String): String {
        val objectMapper = ObjectMapper()
        val map = objectMapper.readValue(response, Map::class.java)
        // This abomination extracts the URI from the response returned by Agrovoc.
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
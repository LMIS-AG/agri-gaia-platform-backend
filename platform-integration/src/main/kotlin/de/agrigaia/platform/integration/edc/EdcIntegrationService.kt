package de.agrigaia.platform.integration.edc

import de.agrigaia.platform.common.HasLogger
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.body
import reactor.core.publisher.Mono

@Service
class EdcIntegrationService : HasLogger {
    private val webClient: WebClient = WebClient.create()
    private val connectorEndpoint = "https://connector-consumer-9192.platform.agri-gaia.com/api/v1/data"

    fun publishAsset(assetJson: String, policyJson: String, contractDefinitionJson: String) {
        this.sendAssetRequest(assetJson)
        this.sendPolicyRequest(policyJson)
        this.sendContractDefinitionRequest(contractDefinitionJson)
    }

    fun unpublishAsset(assetJson: String, policyJson: String, contractDefinitionJson: String) {
        this.sendContractDefinitionDeleteRequest(contractDefinitionJson)
        this.sendPolicyDeleteRequest(policyJson)
        this.sendAssetDeleteRequest(assetJson)
    }

    private fun sendAssetRequest(assetJson: String) {
        this.webClient.post()
            .uri("$connectorEndpoint/assets")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Api-Key", "password")
            .body(Mono.just(assetJson))
            .retrieve()
            .bodyToMono(String::class.java)
            .block()
    }

    private fun sendPolicyRequest(policyJson: String) {
        this.webClient.post()
            .uri("$connectorEndpoint/policydefinitions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Api-Key", "password")
            .body(Mono.just(policyJson))
            .retrieve()
            .bodyToMono(String::class.java)
            .block()
    }

    private fun sendContractDefinitionRequest(contractDefinitionJson: String) {
        this.webClient.post()
            .uri("$connectorEndpoint/contractdefinitions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Api-Key", "password")
            .body(Mono.just(contractDefinitionJson))
            .retrieve()
            .bodyToMono(String::class.java)
            .block()
    }

    private fun sendContractDefinitionDeleteRequest(contractDefinitionJson: String) {
        this.webClient.method(HttpMethod.DELETE)
            .uri("$connectorEndpoint/contractdefinitions/$contractDefinitionJson")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Api-Key", "password")
            .retrieve()
            .bodyToMono(String::class.java)
            .block()
    }

    private fun sendPolicyDeleteRequest(policyJson: String) {
        this.webClient.method(HttpMethod.DELETE)
            .uri("$connectorEndpoint/policydefinitions/$policyJson")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Api-Key", "password")
            .retrieve()
            .bodyToMono(String::class.java)
            .block()
    }

    private fun sendAssetDeleteRequest(assetJson: String) {
        this.webClient.method(HttpMethod.DELETE)
            .uri("$connectorEndpoint/assets/$assetJson")
            .header("X-Api-Key", "password")
            .retrieve()
            .bodyToMono(String::class.java)
            .block()
    }
}

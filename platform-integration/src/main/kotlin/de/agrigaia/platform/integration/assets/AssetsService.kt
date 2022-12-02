package de.agrigaia.platform.integration.assets

import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.body
import reactor.core.publisher.Mono

@Service
class AssetsService {
    private val webClient: WebClient = WebClient.create();
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val connectorEndpoint = "https://connector-consumer-9192.platform.agri-gaia.com/api/v1/data"

    fun publishAsset(assetJson: String, policyJson: String, catalogJson: String) {
        this.sendAssetRequest(fixString(assetJson));
        this.sendPolicyRequest(fixString(policyJson));
        this.sendCatalogRequest(fixString(catalogJson));
    }

    fun deleteAsset(assetJson: String, policyJson: String, catalogJson: String) {
        this.sendAssetDeleteRequest(fixString(assetJson));
        this.sendPolicyDeleteRequest(fixString(policyJson));
        this.sendCatalogDeleteRequest(fixString(catalogJson));
    }

    // TODO Please fix this, it's so bad
    private fun fixString(assetJson: String) =
        "{" + assetJson.replace("\" ", " ").replace("\",", ",").replace("\"\n", "\n").replace("\"\"", "\"")


    private fun sendAssetRequest(assetJson: String) {
        val response = this.webClient.post()
                .uri("$connectorEndpoint/assets")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Api-Key", "password")
                .body(Mono.just(assetJson))
                .retrieve()
                .bodyToMono(String::class.java)
                .block();
    }

    private fun sendPolicyRequest(policyJson: String) {
        val response = this.webClient.post()
                .uri("$connectorEndpoint/policydefinitions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Api-Key", "password")
                .body(Mono.just(policyJson))
                .retrieve()
                .bodyToMono(String::class.java)
                .block();
    }

    private fun sendCatalogRequest(catalogJson: String) {
        val response = this.webClient.post()
                .uri("$connectorEndpoint/contractdefinitions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Api-Key", "password")
                .body(Mono.just(catalogJson))
                .retrieve()
                .bodyToMono(String::class.java)
                .block();
    }

    private fun sendAssetDeleteRequest(assetJson: String) {
        val response = this.webClient.method(HttpMethod.DELETE)
            .uri("$connectorEndpoint/assets")
            .header("X-Api-Key", "password")
            .body(Mono.just(assetJson))
            .retrieve()
            .bodyToMono(String::class.java)
            .block();
    }

    private fun sendPolicyDeleteRequest(policyJson: String) {
        val response = this.webClient.method(HttpMethod.DELETE)
            .uri("$connectorEndpoint/policydefinitions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Api-Key", "password")
            .body(Mono.just(policyJson))
            .retrieve()
            .bodyToMono(String::class.java)
            .block();
    }

    private fun sendCatalogDeleteRequest(catalogJson: String) {
        val response = this.webClient.method(HttpMethod.DELETE)
            .uri("$connectorEndpoint/contractdefinitions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Api-Key", "password")
            .body(Mono.just(catalogJson))
            .retrieve()
            .bodyToMono(String::class.java)
            .block();
    }
}

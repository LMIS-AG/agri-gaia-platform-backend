package de.agrigaia.platform.integration.assets

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.body
import reactor.core.publisher.Mono

@Service
class AssetsService @Autowired constructor(){
    private val webClient: WebClient = WebClient.create();
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun publishAssets() {
        logger.info("Test AssetsService") // TODO remove
        this.sendAssetRequest();
        this.sendPolicyRequest();
        this.sendCatalogRequest();
    }

    private fun sendCatalogRequest() {
        val assetBody = object {
            val asset = object {
                val test = "test"
            }
            val dataAddress = object {
            }
        }

        val response = this.webClient.post()
            .uri("https://connector-provider-8182.platform.agri-gaia.com/api/v1/data/assets")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Api-Key", "password")
            .body(Mono.just(assetBody))
            .retrieve()
            .bodyToMono(String.javaClass)
            .block();
    }

    private fun sendPolicyRequest() {
        val policyBody = object {
            val asset = object {
                val test = "test"
            }
            val dataAddress = object {
            }
        }

        val response = this.webClient.post()
            .uri("https://connector-provider-8182.platform.agri-gaia.com/api/v1/data/policydefinitions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Api-Key", "password")
            .body(Mono.just(policyBody))
            .retrieve()
            .bodyToMono(String.javaClass)
            .block();
    }

    private fun sendAssetRequest() {
        val catalogBody = object {
            val asset = object {
                val test = "test"
            }
            val dataAddress = object {
            }
        }

        val response = this.webClient.post()
            .uri("https://connector-provider-8182.platform.agri-gaia.com/api/v1/data/contractdefinitions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Api-Key", "password")
            .body(Mono.just(catalogBody))
            .retrieve()
            .bodyToMono(String.javaClass)
            .block();
    }
}
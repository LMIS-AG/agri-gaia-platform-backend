package de.agrigaia.platform.integration.edc

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import de.agrigaia.platform.common.HasLogger
import de.agrigaia.platform.integration.minio.MinioService
import de.agrigaia.platform.model.edc.PolicyDto
import de.agrigaia.platform.model.edc.PolicyType
import io.minio.errors.ErrorResponseException
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.body
import reactor.core.publisher.Mono

/**
 * Service communicating with EDC directly.
 */
@Service
class EdcIntegrationService(private val minioService: MinioService) : HasLogger {
    private val webClient: WebClient = WebClient.create()
    private val connectorEndpoint = "https://connector-consumer-9192.platform.agri-gaia.com/api/v1/data"

    /**
     * Get names of policies in a MinIO bucket.
     * @param jwt JSON web token
     * @param bucketName name of MinIO bucket
     * @return List of strings of policy names in MinIO bucket.
     */
    private fun getAllPolicyNames(jwt: String, bucketName: String): List<String> {
        return this.minioService.getAssetsForBucket(jwt, bucketName, "policies")
            .map { policyPathToName(it.get().objectName()) }
    }

    /**
     * Return all policies in a MinIO bucket as `PolicyDto`s.
     * @param jwtTokenValue JSON web token
     * @param bucketName name of MinIO bucket
     * @return list of policies
     */
    fun getAllPolicies(jwtTokenValue: String, bucketName: String): List<PolicyDto> {
        val accessPolicies = minioService.getAssetsForBucket(
            jwtTokenValue,
            bucketName,
            "policies/access",
        ).map { PolicyDto(policyPathToName(it.get().objectName()), PolicyType.ACCESS, null) }
        val contractPolicies = minioService.getAssetsForBucket(
            jwtTokenValue,
            bucketName,
            "policies/contract",
        ).map { PolicyDto(policyPathToName(it.get().objectName()), PolicyType.CONTRACT, null) }
        return accessPolicies + contractPolicies
    }


    /**
     * Returns raw policy JSON.
     * @param jwtTokenValue JSON web token
     * @param bucketName name of MinIO bucket
     * @param policyName name of policy
     * @return String containing the policy JSON.
     */
    fun getPolicyJson(jwtTokenValue: String, bucketName: String, policyName: String): String {
        try {
            return this.minioService.downloadTextFile(jwtTokenValue, bucketName, "policies/$policyName.json")
        } catch (e: ErrorResponseException) {
            throw Exception("Policy $policyName not found in bucket $bucketName")
        }
    }

    /**
     * Get policy with field values for a certain asset
     * @param jwtTokenValue JSON web token
     * @param bucketName name of MinIO bucket
     * @param policyName name of policy
     * @param assetName name of asset
     * @return String containing the policy JSON with correct field values for asset.
     */
    fun getPolicyforAsset(jwtTokenValue: String, bucketName: String, policyName: String, assetName: String): String {
        val policyTemplate: String = getPolicyJson(jwtTokenValue, bucketName, policyName)
        return fillInPolicyTemplate(policyTemplate, assetName)
    }

    /**
     * Delete a policy from the user's MinIO bucket.
     *
     * @param jwtTokenValue JSON web token
     * @param bucketName name of MinIO bucket
     * @param policyName name of the policy
     * @return 200, even if file did not exist (MinIO is dumb)
     */
    fun deletePolicy(jwtTokenValue: String, bucketName: String, policyName: String) {
        for (p in PolicyType.values()) {
            this.minioService.deleteAsset(jwtTokenValue, bucketName, "policies/${policyTypeToDir(p)}/$policyName.json")
        }
    }

    /**
     * Add policy to user's bucket
     * @param jwtTokenValue JSON web token
     * @param bucketName name of MinIO bucket
     * @param policyName name of policy
     * @policyJson policy as JSON
     */
    fun addPolicy(
        jwtTokenValue: String,
        bucketName: String,
        policyName: String,
        policyJson: JsonNode,
        policyType: PolicyType
    ) {
        val policyNameExists: Boolean = getAllPolicyNames(jwtTokenValue, bucketName).contains(policyName)
        // TODO: Generic exceptions send 500 with useless error message.
        if (policyNameExists) {
            throw Exception("Policy with name $policyName already exists in bucket $bucketName.")
        }
        if (!isValidJson(policyJson.toString())) throw Exception("Policy content is not valid JSON.")
        // TODO: Policy must be valid (only EDC can really verify so what the heck).

        // Upload policy to user's bucket.
        val filePath = "policies/${policyTypeToDir(policyType)}/$policyName.json"
        minioService.uploadTextFile(
            jwtTokenValue,
            bucketName,
            filePath,
            policyJson.toString(),
        )
    }

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

    // TODO: These can all be moved to a more central place.
    private fun isValidJson(json: String): Boolean {
        val mapper: ObjectMapper = ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
        try {
            mapper.readTree(json)
        } catch (e: JacksonException) {
            return false
        }
        return true
    }

    /**
     * Substitute correct target value in policy template.
     * @param policyTemplate policy JSON from MinIO with placeholder values
     * @param target value to set target field to
     * @return String containing the policy JSON with correct field values for asset.
     */
    private fun fillInPolicyTemplate(policyTemplate: String, target: String): String {
        return policyTemplate.replace("<TARGET>", target)
    }

    private fun policyPathToName(policyPath: String): String {
        return policyPath.substringAfterLast('/').removeSuffix(".json")
    }

    private fun policyTypeToDir(p: PolicyType): String {
        return when (p) {
            PolicyType.ACCESS -> "access"
            PolicyType.CONTRACT -> "contract"
        }
    }
}

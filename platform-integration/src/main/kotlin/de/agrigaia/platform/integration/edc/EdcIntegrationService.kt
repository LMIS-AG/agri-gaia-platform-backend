package de.agrigaia.platform.integration.edc

import com.fasterxml.jackson.databind.JsonNode
import de.agrigaia.platform.common.HasLogger
import de.agrigaia.platform.integration.minio.MinioService
import de.agrigaia.platform.model.edc.PolicyDto
import de.agrigaia.platform.model.edc.PolicyType
import io.minio.errors.ErrorResponseException
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.body
import reactor.core.publisher.Mono

/**
 * Service communicating with EDC directly.
 */
@Service
class EdcIntegrationService(
    private val minioService: MinioService,
    private val edcProperties: EdcProperties
    ) : HasLogger {
    private val webClient: WebClient = WebClient.create()

    /**
     * Set Connector endpoint dynamically, depending on the logged-in user and its organisation
     * @return Connector endpoint as string
     */
    private fun setConnectorEndpoint(): String? {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val jwt = jwtAuthenticationToken.token
        val userGroups = listOf(jwt.claims["usergroup"])
        val actualGroup = userGroups.filterNot { group -> group == "AgriGaia" }.toString()

        val connectorEndpoint = if (actualGroup == "AgBrain") {
            edcProperties.agbrainConnectorUrl
        } else {
            edcProperties.lmisConnectorUrl
        }

        return connectorEndpoint
    }

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
        val policies: MutableList<PolicyDto> = mutableListOf()
        for (policyType in PolicyType.values()) {
            val p = minioService.getAssetsForBucket(
                jwtTokenValue,
                bucketName,
                "policies/${policyTypeToDir(policyType)}",
            ).map {
                val policyName = policyPathToName(it.get().objectName())
                PolicyDto(policyName, policyType, null)
            }
            policies.addAll(p)
        }
        return policies
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
            val policyDir = policyTypeToDir(p)
            this.minioService.deleteAsset(
                jwtTokenValue,
                bucketName,
                "policies/$policyDir/$policyName.json",
            )
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
        val connectorEndpoint = setConnectorEndpoint()

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
        val connectorEndpoint = setConnectorEndpoint()

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
        val connectorEndpoint = setConnectorEndpoint()

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
        val connectorEndpoint = setConnectorEndpoint()

        this.webClient.method(HttpMethod.DELETE)
            .uri("$connectorEndpoint/contractdefinitions/$contractDefinitionJson")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Api-Key", "password")
            .retrieve()
            .bodyToMono(String::class.java)
            .block()
    }

    private fun sendPolicyDeleteRequest(policyJson: String) {
        val connectorEndpoint = setConnectorEndpoint()

        this.webClient.method(HttpMethod.DELETE)
            .uri("$connectorEndpoint/policydefinitions/$policyJson")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Api-Key", "password")
            .retrieve()
            .bodyToMono(String::class.java)
            .block()
    }

    private fun sendAssetDeleteRequest(assetJson: String) {
        val connectorEndpoint = setConnectorEndpoint()

        this.webClient.method(HttpMethod.DELETE)
            .uri("$connectorEndpoint/assets/$assetJson")
            .header("X-Api-Key", "password")
            .retrieve()
            .bodyToMono(String::class.java)
            .block()
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

    private fun policyTypeToDir(p: PolicyType): String = p.toString().lowercase()
}

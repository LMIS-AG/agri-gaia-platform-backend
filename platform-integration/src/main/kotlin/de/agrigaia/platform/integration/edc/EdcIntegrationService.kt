package de.agrigaia.platform.integration.edc

import de.agrigaia.platform.common.HasLogger
import de.agrigaia.platform.integration.minio.MinioService
import de.agrigaia.platform.model.edc.Company
import de.agrigaia.platform.model.edc.PolicyDto
import de.agrigaia.platform.model.edc.PolicyType
import io.minio.errors.ErrorResponseException
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.body
import reactor.core.publisher.Mono
import java.util.*

/**
 * Service communicating with EDC directly.
 */
@Service
class EdcIntegrationService(
    private val minioService: MinioService,
    private val edcProperties: EdcProperties,
) : HasLogger {
    private val webClient: WebClient = WebClient.create()
    /**
     * Upload assetjson to MinIO.
     */
    fun addAssetjson(
        jwtTokenValue: String,
        bucketName: String,
        assetjsonName: String,
        assetJson: String,
    ) {
        // Upload policy to user's bucket.
        val filePath = "assetjsons/$assetjsonName.json"
        minioService.uploadTextFile(
            jwtTokenValue,
            bucketName,
            filePath,
            assetJson,
        )
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
        var policyJson: String
        for (p in PolicyType.values()) {
            val policyDir = policyTypeToDir(p)
            try {
                policyJson = this.minioService.downloadTextFile(
                    jwtTokenValue,
                    bucketName,
                    "policies/$policyDir/$policyName.json"
                )
                return policyJson
            } catch (e: ErrorResponseException) {
                continue
            }
        }
        throw Exception("Policy $policyName not found in bucket $bucketName")
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
        policyJson: String,
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
            policyJson,
        )
    }

    fun publishAsset(
        company: Company,
        assetJson: String,
        accessPolicyJson: String,
        contractPolicyJson: String,
        contractDefinitionJson: String
    ) {
        val connectorUrl: String = connectorUrlByCompany(company)
        this.sendAssetRequest(connectorUrl, assetJson)
        this.sendPolicyRequest(connectorUrl, accessPolicyJson)
        this.sendPolicyRequest(connectorUrl, contractPolicyJson)
        this.sendContractDefinitionRequest(connectorUrl, contractDefinitionJson)
    }

    fun unpublishAsset(
        company: Company,
        assetJson: String,
        accessPolicyJson: String,
        contractPolicyJson: String,
        contractDefinitionJson: String
    ) {
        val connectorUrl: String = connectorUrlByCompany(company)
        this.sendContractDefinitionDeleteRequest(connectorUrl, contractDefinitionJson)
        this.sendPolicyDeleteRequest(connectorUrl, accessPolicyJson)
        this.sendPolicyDeleteRequest(connectorUrl, contractPolicyJson)
        this.sendAssetDeleteRequest(connectorUrl, assetJson)
    }

    private fun connectorUrlByCompany(company: Company): String {
        val connectorUrl: String? = when (company) {
            Company.agbrain -> edcProperties.agBrain
            Company.agrotechvalley -> edcProperties.agrotechValley
            Company.amazone -> edcProperties.amazone
            Company.bosch -> edcProperties.bosch
            Company.claas -> edcProperties.claas
            Company.dfki -> edcProperties.dfki
            Company.hsos -> edcProperties.hsos
            Company.kotte -> edcProperties.kotte
            Company.krone -> edcProperties.krone
            Company.lmis -> edcProperties.lmis
            Company.uos -> edcProperties.uos
            Company.wernsing -> edcProperties.wernsing
        }
        return connectorUrl ?: throw Exception("No connector URL found for company $company")
    }

    private fun sendAssetRequest(connectorUrl: String, assetJson: String) {
        val request = this.webClient.post()
            .uri("${connectorUrl}/assets")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Api-Key", "password")
            .body(Mono.just(assetJson))

        request.retrieve()
            .onStatus({ it.is4xxClientError }, ::handleClientError)
            .onStatus({ it.is5xxServerError }, ::handleServerError)
            .bodyToMono(String::class.java)
            .block()
    }

    private fun handleClientError(clientResponse: ClientResponse): Mono<out Throwable>? {
        return clientResponse.bodyToMono(String::class.java)
            .doOnNext { getLogger().error("${clientResponse.statusCode()}: $it") }
            .then(clientResponse.createException())
    }

    private fun handleServerError(clientResponse: ClientResponse): Mono<out Throwable>? =
        handleClientError(clientResponse)

    private fun sendPolicyRequest(connectorUrl: String, policyJson: String) {
        this.webClient.post()
            .uri("${connectorUrl}/policydefinitions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Api-Key", "password")
            .body(Mono.just(policyJson))
            .retrieve()
            .bodyToMono(String::class.java)
            .block()
    }

    private fun sendContractDefinitionRequest(connectorUrl: String, contractDefinitionJson: String) {
        this.webClient.post()
            .uri("${connectorUrl}/contractdefinitions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Api-Key", "password")
            .body(Mono.just(contractDefinitionJson))
            .retrieve()
            .bodyToMono(String::class.java)
            .block()
    }

    private fun sendContractDefinitionDeleteRequest(connectorUrl: String, contractDefinitionJson: String) {
        this.webClient.method(HttpMethod.DELETE)
            .uri("${connectorUrl}/contractdefinitions/$contractDefinitionJson")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Api-Key", "password")
            .retrieve()
            .bodyToMono(String::class.java)
            .block()
    }

    private fun sendPolicyDeleteRequest(connectorUrl: String, policyJson: String) {
        this.webClient.method(HttpMethod.DELETE)
            .uri("${connectorUrl}/policydefinitions/$policyJson")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Api-Key", "password")
            .retrieve()
            .bodyToMono(String::class.java)
            .block()
    }

    private fun sendAssetDeleteRequest(connectorUrl: String, assetJson: String) {
        this.webClient.method(HttpMethod.DELETE)
            .uri("${connectorUrl}/assets/$assetJson")
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
        return policyTemplate
            .replace("<TARGET>", target)
            .replace("<UUID>", UUID.randomUUID().toString())
    }

    private fun policyPathToName(policyPath: String): String {
        return policyPath.substringAfterLast('/').removeSuffix(".json")
    }

    private fun policyTypeToDir(p: PolicyType): String = p.toString().lowercase()
}

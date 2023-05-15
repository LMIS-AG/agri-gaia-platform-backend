package de.agrigaia.platform.business.edc

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import de.agrigaia.platform.business.errors.BusinessException
import de.agrigaia.platform.business.errors.ErrorType
import de.agrigaia.platform.common.HasLogger
import de.agrigaia.platform.integration.fuseki.FusekiConnectorService
import de.agrigaia.platform.integration.minio.MinioService
import io.minio.errors.ErrorResponseException
import org.springframework.stereotype.Service

/**
 * Service for EDC related actions not communicating with EDC directly.
 */
@Service
class EdcBusinessService(
    private val fusekiConnectorService: FusekiConnectorService,
    private val minioService: MinioService,
) : HasLogger {

    /**
     * Return all policies in a MinIO bucket.
     * @param jwt JSON web token
     * @param bucketName name of MinIO bucket
     * @return list of policies
     */
    fun getAllPolicies(jwt: String, bucketName: String): List<String> {
        return this.minioService.getAssetsForBucket(jwt, bucketName, "policies")
            .map { it.get().objectName() }
            .map {
                try {
                    minioService.downloadTextFile(jwt, bucketName, it)
                } catch (e: ErrorResponseException) {
                    throw BusinessException("Policy $it not found in bucket $bucketName", ErrorType.NOT_FOUND)
                }
            }
    }

    /**
     * Get policy from MinIO bucket.
     * @param jwt JSON web token
     * @param bucketName name of MinIO bucket
     * @param policyName name of policy
     * @return String containing the policy JSON.
     */
    fun getPolicy(jwt: String, bucketName: String, policyName: String): String {
        try {
            return this.minioService.downloadTextFile(jwt, bucketName, "policies/$policyName.json")
        } catch (e: ErrorResponseException) {
            throw BusinessException("Policy $policyName not found in bucket $bucketName", ErrorType.NOT_FOUND)
        }
    }

    /**
     * Get policy with field values for a certain asset
     * @param jwt JSON web token
     * @param bucketName name of MinIO bucket
     * @param policyName name of policy
     * @param assetName name of asset
     * @return String containing the policy JSON with correct field values for asset.
     */
    fun getPolicyforAsset(jwt: String, bucketName: String, policyName: String, assetName: String): String {
        val policyTemplate: String = getPolicy(jwt, bucketName, policyName)
        return fillInPolicyTemplate(policyTemplate, assetName)
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

    /**
     * Add policy to user's bucket
     * @param jwtTokenValue JSON web token
     * @param bucketName name of MinIO bucket
     * @param policyName name of policy
     * @policyJson policy as JSON
     * @return TODO
     */
    fun addPolicy(jwtTokenValue: String, bucketName: String, policyName: String, policyJson: String) {
        val policyNameExists: Boolean = getPolicyNames(jwtTokenValue, bucketName).contains(policyName)
        if (policyNameExists) {
            throw BusinessException(
                "Policy with name $policyName already exists in bucket $bucketName.",
                ErrorType.BAD_REQUEST
            )
        }
        if (!isValidJson(policyJson)) {
            throw BusinessException("Request body is not valid JSON.", ErrorType.BAD_REQUEST)
        }
        // TODO: Policy must be valid (only EDC can really verify).

        // Upload policy to user's bucket.
        minioService.uploadTextFile(jwtTokenValue, bucketName, "policies/$policyName.json", policyJson)
    }

    // TODO: This can be moved to a more central place.
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


    private fun getPolicyNames(jwtTokenValue: String, bucketName: String): List<String> {
        return this.minioService.getAssetsForBucket(jwtTokenValue, bucketName, "policies")
            .map { it.get().objectName().removePrefix("policies/").removeSuffix(".json") }
    }

    fun createAssetJson(
        assetPropName: String,
        assetPropId: String,
        bucketName: String,
        assetName: String,
        assetPropDescription: String?,
        assetPropContentType: String?,
        assetPropVersion: String?,
        agrovocKeywords: List<String>?,
        latitude: String?,
        longitude: String?,
        dateRange: String?,
        dataAddressKeyName: String?,
    ): String {
        val spatial = if (latitude.isNullOrEmpty() || longitude.isNullOrEmpty()) {
            "null"
        } else {
            "\"${this.fusekiConnectorService.getUriFromCoordinates(latitude, longitude)}\""
        }
        return """
            {
              "asset": {
                "properties": {
                  "asset:prop:name": "$assetPropName",
                  "asset:prop:byteSize": null,
                  "asset:prop:description": "${assetPropDescription ?: ""}",
                  "asset:prop:contenttype": "${assetPropContentType ?: ""}",
                  "asset:prop:version": "${assetPropVersion ?: ""}",
                  "asset:prop:id": "$assetPropId",
                  "theme": ${agrovocKeywords?.map { w -> "\"${this.fusekiConnectorService.getConceptUriFromKeyword(w)}\"" }},
                  "spatial": ${spatial},
                  "temporal": "${dateRange ?: ""}"
                },
                "id": "$assetPropId"
              },
              "dataAddress": {
                "properties": {
                  "type": "AmazonS3",
                  "region": "us-east-1",
                  "bucketName": "$bucketName",
                  "assetName": "$assetName",
                  "keyName": "${dataAddressKeyName ?: ""}"
                }
              }
            }"""
    }

    fun createContractDefinitionJson(assetId: String, policyUUID: String, catalogUUID: String): String = """{
  "accessPolicyId": "$policyUUID",
  "contractPolicyId": "$policyUUID",
  "id": "$catalogUUID",
  "criteria": [
    {
      "operandLeft": "asset:prop:id",
      "operator": "=",
      "operandRight": "$assetId"
    }
  ]
}
    """

    /**
     * Extract UUID from policy JSON.
     */
    fun extractIdfromPolicy(policyJson: String): String {
        val idLine: String = policyJson.lines().firstOrNull { it.contains("\"id\": ") }
            ?: throw BusinessException("Could not extract id from policy.", ErrorType.UNKNOWN)
        return idLine.substringBeforeLast("\"").substringAfterLast("\"")
    }


}

package de.agrigaia.platform.business.edc

import de.agrigaia.platform.business.errors.BusinessException
import de.agrigaia.platform.business.errors.ErrorType
import de.agrigaia.platform.integration.fuseki.FusekiConnectorService
import de.agrigaia.platform.integration.minio.MinioService
import io.minio.messages.Item
import org.springframework.stereotype.Service

/**
 * Service for EDC related actions not communicating with EDC directly.
 */
@Service
class EdcBusinessService(
    private val fusekiConnectorService: FusekiConnectorService,
    private val minioService: MinioService,
) {

    /**
     * Return all policy `Item`s in a MinIO bucket.
     * @param jwt JSON web token
     * @param bucketName name of MinIO bucket
     * @return List of policy `Item` objects.
     */
    private fun getPolicyItems(jwt: String, bucketName: String): List<Item> {
        return this.minioService.getAssetsForBucket(jwt, bucketName, "policies").map { it.get() }
    }

    /**
     * Get names of policies in a MinIO bucket.
     * @param jwt JSON web token
     * @param bucketName name of MinIO bucket
     * @return List of strings of policy names in MinIO bucket.
     */
    fun getPolicyNames(jwt: String, bucketName: String): List<String> {
        return this.getPolicyItems(jwt, bucketName)
            .map { it.objectName().removePrefix("policies/").removeSuffix(".json") }
    }

    /**
     * Get policy from MinIO bucket with correct values for asset.
     * @param jwt JSON web token
     * @param bucketName name of MinIO bucket
     * @param policyName name of policy
     * @param assetName name of asset
     * @return String containing the policy JSON with correct field values for asset.
     */
    fun getPolicy(jwt: String, bucketName: String, policyName: String, assetName: String): String {
        val policyTemplate: String = this.minioService.getFileContent(jwt, bucketName, "policies/$policyName.json")
        return preparePolicyTemplate(policyTemplate, assetName)
    }

    /**
     * Substitute correct target value in policy template.
     * @param policyTemplate policy JSON from MinIO with placeholder values
     * @param target value to set target field to
     * @return String containing the policy JSON with correct field values for asset.
     */
    private fun preparePolicyTemplate(policyTemplate: String, target: String): String {
        return policyTemplate.replace("<TARGET>", target)
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
    fun extractUUIDfromPolicy(policyJson: String): String {
        val uuidLine: String = policyJson.lines().firstOrNull { it.contains("\"id\": ") }
            ?: throw BusinessException("Could not extract id from policy.", ErrorType.UNKNOWN)
        return uuidLine.substringBeforeLast("\"").substringAfterLast("\"")
    }
}

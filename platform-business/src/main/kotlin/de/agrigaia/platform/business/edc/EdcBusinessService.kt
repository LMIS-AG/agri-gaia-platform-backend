package de.agrigaia.platform.business.edc

import de.agrigaia.platform.integration.fuseki.FusekiConnectorService
import de.agrigaia.platform.integration.minio.MinioService
import io.minio.messages.Item
import org.springframework.stereotype.Service

@Service
class EdcBusinessService(
    private val fusekiConnectorService: FusekiConnectorService,
    private val minioService: MinioService,
) {

    private fun getPolicyItems(jwt: String, bucketName: String): List<Item> {
        return this.minioService.getAssetsForBucket(jwt, bucketName, "policies").map { it.get() }
    }

    fun getPolicyNames(jwt: String, bucketName: String): List<String> {
        return this.getPolicyItems(jwt, bucketName).map { it.objectName().removePrefix("policies/").removeSuffix(".json") }
    }

    fun getPolicy(jwt: String, bucketName: String, policyName: String, assetName: String): String {
        val policyTemplate: String = this.minioService.getFileContent(jwt, bucketName, "policies/$policyName.json")
        return preparePolicyTemplate(policyTemplate, assetName)
    }

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

    fun createPolicyJson(target: String, policyUUID: String): String = """{
  "uid": "use-eu",
  "id": "$policyUUID",
  "policy": {
    "permissions": [
      {
        "edctype": "dataspaceconnector:permission",
        "uid": null,
        "target": "$target",
        "action": {
          "type": "USE",
          "includedIn": null,
          "constraint": null
        },
        "assignee": null,
        "assigner": null,
        "constraints": [],
        "duties": []
      }
    ],
    "prohibitions": [],
    "obligations": [],
    "extensibleProperties": {},
    "inheritsFrom": null,
    "assigner": null,
    "assignee": null,
    "target": "",
    "type": {
      "type": "set"
    }
  }
}
    """

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
}

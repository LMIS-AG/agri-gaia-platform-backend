package de.agrigaia.platform.business.edc

import de.agrigaia.platform.integration.agrovoc.AgrovocConnectorService
import de.agrigaia.platform.integration.fuseki.FusekiConnectorService
import org.springframework.stereotype.Service

@Service
class EdcService(private val fusekiConnectorService: FusekiConnectorService) {
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
    ) = """
        {
          "asset": {
            "properties": {
              "asset:prop:name": "$assetPropName",
              "asset:prop:byteSize": null,
              "asset:prop:description": "${assetPropDescription?:""}",
              "asset:prop:contenttype": "${assetPropContentType?:""}",
              "asset:prop:version": "${assetPropVersion?:""}",
              "asset:prop:id": "$assetPropId",
              "theme": ${agrovocKeywords?.map { w -> "\"${this.fusekiConnectorService.getConceptUriFromKeyword(w)}\""}},
              "spatial": "\"${this.fusekiConnectorService.getUriFromCoordinates(longitude!!, latitude!!)}\""
              "temporal": "${dateRange?:""}"
            },
            "id": "$assetPropId"
          },
          "dataAddress": {
            "properties": {
              "type": "AmazonS3",
              "region": "us-east-1",
              "bucketName": "$bucketName",
              "assetName": "$assetName",
              "keyName": "${dataAddressKeyName?:""}"
            }
          }
        }"""

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

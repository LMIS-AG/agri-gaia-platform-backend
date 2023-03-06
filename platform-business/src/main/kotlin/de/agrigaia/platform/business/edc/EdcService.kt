package de.agrigaia.platform.business.edc

import de.agrigaia.platform.integration.agrovoc.AgrovocService
import org.springframework.stereotype.Service

@Service
class EdcService(private val agrovocService: AgrovocService) {
    fun createAssetJson(
        assetPropName: String,
        assetPropId: String,
        bucketName: String,
        assetName: String,
        assetPropDescription: String?,
        assetPropContentType: String?,
        assetPropVersion: String?,
        agrovocKeywords: List<String>?,
        geonamesUri: String?,
        dateRange: String?,
        dataAddressKeyName: String?,
    ) = """
        {
          "asset": {
            "properties": {
              "asset:prop:name": "$assetPropName",
              "asset:prop:byteSize": null,
              "asset:prop:description": "$assetPropDescription",
              "asset:prop:contenttype": "$assetPropContentType",
              "asset:prop:version": "$assetPropVersion",
              "asset:prop:id": "$assetPropId",
              "theme": "${agrovocKeywords?.map { w: String -> this.agrovocService.getConceptUriFromKeyword(w) }}",
              "spatial": "$geonamesUri",
              "temporal": "$dateRange"
            },
            "id": "$assetPropId"
          },
          "dataAddress": {
            "properties": {
              "type": "AmazonS3",
              "region": "us-east-1",
              "bucketName": "$bucketName",
              "assetName": "$assetName",
              "keyName": "$dataAddressKeyName"
            }
          }
        }"""

    fun createPolicyJson(target: String): String = """{
  "uid": "use-eu",
  "id": "3a75736e-001d-4364-8bd4-9888490edb59",
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

    fun createCatalogJson(assetId: String): String = """{
  "accessPolicyId": "3a75736e-001d-4364-8bd4-9888490edb59",
  "contractPolicyId": "3a75736e-001d-4364-8bd4-9888490edb59",
  "id": "3a75736e-001d-4364-8bd4-9888490edb58",
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
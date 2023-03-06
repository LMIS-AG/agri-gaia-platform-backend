package de.agrigaia.platform.api.assets


import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.agrigaia.platform.api.BaseController
import de.agrigaia.platform.business.errors.BusinessException
import de.agrigaia.platform.business.errors.ErrorType
import de.agrigaia.platform.common.HasLogger
import de.agrigaia.platform.integration.agrovoc.AgrovocService
import de.agrigaia.platform.integration.edc.EdcService
import de.agrigaia.platform.integration.minio.MinioService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/assets")
class AssetsController @Autowired constructor(
    private val edcService: EdcService,
    private val agrovocService: AgrovocService,
    private val minioService: MinioService
) : HasLogger, BaseController() {

    @PostMapping("publish/{bucket}/{name}")
    @ResponseStatus(HttpStatus.CREATED)
    fun publishAsset(
        @PathVariable bucket: String,
        @PathVariable name: String,
        @RequestBody assetJsonDto: AssetJsonDto,
    ) {
        val assetId: String = assetJsonDto.assetPropId
            ?: throw BusinessException("No asset id in AssetJsonDto.", ErrorType.NOT_FOUND)
        val assetJson = createAssetJson(assetJsonDto, bucket, name)
        val policyJson = createPolicyJson(name)
        val catalogJson = createCatalogJson(assetId)

        this.edcService.publishAsset(assetJson, policyJson, catalogJson)
    }


    // TODO: Move all three functions someplace else. Idc where but this is not their home.
    private fun createAssetJson(assetJsonDto: AssetJsonDto, bucketName: String, assetName: String) = """
        {
          "asset": {
            "properties": {
              "asset:prop:name": "${assetJsonDto.assetPropName}",
              "asset:prop:byteSize": null,
              "asset:prop:description": "${assetJsonDto.assetPropDescription}",
              "asset:prop:contenttype": "${assetJsonDto.assetPropContentType}",
              "asset:prop:version": "${assetJsonDto.assetPropVersion}",
              "asset:prop:id": "${assetJsonDto.assetPropId}",
              "theme": "${ assetJsonDto.agrovocKeywords?.map { w -> this.agrovocService.getConceptUriFromKeyword(w) } }",
              "spatial": "${assetJsonDto.geonamesUri}",
              "temporal": "${assetJsonDto.dateRange}"
            },
            "id": "${assetJsonDto.assetPropId}"
          },
          "dataAddress": {
            "properties": {
              "type": "AmazonS3",
              "region": "us-east-1",
              "bucketName": "$bucketName",
              "assetName": "$assetName",
              "keyName": "${assetJsonDto.dataAddressKeyName}"
            }
          }
        }"""

    private fun createPolicyJson(target: String): String = """{
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

    private fun createCatalogJson(assetId: String): String = """{
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

    @DeleteMapping("unpublish/{bucket}/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unpublishAsset(@PathVariable bucket: String, @PathVariable name: String) {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val jwt = jwtAuthenticationToken.token.tokenValue

        val assetJson = this.minioService.getFileContent(jwt, bucket, "config/${name}/asset.json")
        val policyJson = this.minioService.getFileContent(jwt, bucket, "config/${name}/policy.json")
        val catalogJson = this.minioService.getFileContent(jwt, bucket, "config/${name}/catalog.json")

        val assetMap = ObjectMapper().readValue<MutableMap<String, MutableMap<String, Any>>>(assetJson)
        val policyMap = ObjectMapper().readValue<MutableMap<String, Any>>(policyJson)
        val catalogMap = ObjectMapper().readValue<MutableMap<String, Any>>(catalogJson)

        val assetId = assetMap.get("asset")!!.get("id") as String
        val policyId = policyMap.get("id") as String
        val contractId = catalogMap.get("id") as String

        this.edcService.unpublishAsset(assetId, policyId, contractId)
    }
}

package de.agrigaia.platform.api.edc


import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.agrigaia.platform.api.BaseController
import de.agrigaia.platform.business.edc.EdcService
import de.agrigaia.platform.business.errors.BusinessException
import de.agrigaia.platform.business.errors.ErrorType
import de.agrigaia.platform.common.HasLogger
import de.agrigaia.platform.integration.edc.EdcConnectorService
import de.agrigaia.platform.integration.minio.MinioService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/assets")
class EdcController @Autowired constructor(
    private val businessEdcService: EdcService,
    private val edcConnectorService: EdcConnectorService,
    private val minioService: MinioService
) : HasLogger, BaseController() {

    @PostMapping("publish/{bucket}/{name}")
    @ResponseStatus(HttpStatus.CREATED)
    fun publishAsset(
        @PathVariable bucket: String,
        @PathVariable name: String,
        @RequestBody assetJsonDto: AssetJsonDto,
    ) {
        val assetPropName: String =
            assetJsonDto.assetPropName ?: throw BusinessException("No asset name in AssetJsonDto.", ErrorType.NOT_FOUND)
        val assetPropId: String =
            assetJsonDto.assetPropId ?: throw BusinessException("No asset id in AssetJsonDto.", ErrorType.NOT_FOUND)

        val assetJson = businessEdcService.createAssetJson(
            assetPropName,
            assetPropId,
            bucket,
            name,
            assetJsonDto.assetPropDescription,
            assetJsonDto.assetPropContentType,
            assetJsonDto.assetPropVersion,
            assetJsonDto.agrovocKeywords,
            assetJsonDto.geonamesUri,
            assetJsonDto.dateRange,
            assetJsonDto.dataAddressKeyName
        )
        val policyJson = businessEdcService.createPolicyJson(name)
        val contractDefinitionJson = businessEdcService.createContractDefinitionJson(assetPropId)

        this.edcConnectorService.publishAsset(assetJson, policyJson, contractDefinitionJson)
    }


    @DeleteMapping("unpublish/{bucket}/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unpublishAsset(@PathVariable bucket: String, @PathVariable name: String) {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val jwt = jwtAuthenticationToken.token.tokenValue

        val assetJson = this.minioService.getFileContent(jwt, bucket, "config/${name}/asset.json")
        val policyJson = this.minioService.getFileContent(jwt, bucket, "config/${name}/policy.json")
        val contractDefinitionJson = this.minioService.getFileContent(jwt, bucket, "config/${name}/catalog.json")

        val assetMap = ObjectMapper().readValue<MutableMap<String, MutableMap<String, Any>>>(assetJson)
        val policyMap = ObjectMapper().readValue<MutableMap<String, Any>>(policyJson)
        val contractDefinitionMap = ObjectMapper().readValue<MutableMap<String, Any>>(contractDefinitionJson)

        val assetId = assetMap.get("asset")!!.get("id") as String
        val policyId = policyMap.get("id") as String
        val contractId = contractDefinitionMap.get("id") as String

        this.edcConnectorService.unpublishAsset(assetId, policyId, contractId)
    }
}

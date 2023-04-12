package de.agrigaia.platform.api.edc

import de.agrigaia.platform.api.BaseController
import de.agrigaia.platform.business.edc.EdcService
import de.agrigaia.platform.business.errors.BusinessException
import de.agrigaia.platform.business.errors.ErrorType
import de.agrigaia.platform.common.HasLogger
import de.agrigaia.platform.integration.edc.EdcConnectorService
import de.agrigaia.platform.model.edc.Asset
import de.agrigaia.platform.persistence.repository.AssetRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/assets")
class EdcController @Autowired constructor(
    private val businessEdcService: EdcService,
    private val edcConnectorService: EdcConnectorService,
    private val assetRepository: AssetRepository,
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
            assetJsonDto.latitude,
            assetJsonDto.longitude,
            assetJsonDto.dateRange,
            assetJsonDto.dataAddressKeyName
        )

        val policyUUID = UUID.randomUUID().toString()
        val contractUUID = UUID.randomUUID().toString()

        val policyJson = businessEdcService.createPolicyJson(name, policyUUID)
        val contractDefinitionJson = businessEdcService.createContractDefinitionJson(assetPropId, policyUUID, contractUUID)

        this.edcConnectorService.publishAsset(assetJson, policyJson, contractDefinitionJson)

        val publishedAsset = Asset()

        publishedAsset.bucket = bucket
        publishedAsset.name = name
        publishedAsset.assetId = assetPropId
        publishedAsset.policyId = policyUUID
        publishedAsset.contractId = contractUUID

        assetRepository.save(publishedAsset)
    }

    @DeleteMapping("unpublish/{bucket}/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unpublishAsset(@PathVariable bucket: String, @PathVariable name: String) {

        val asset = assetRepository.findByBucketAndName(bucket, name)

        val assetId = asset?.assetId
        val policyId = asset?.policyId
        val contractId = asset?.contractId

        assetRepository.delete(asset)
        this.edcConnectorService.unpublishAsset(assetId!!, policyId!!, contractId!!)

    }
}

package de.agrigaia.platform.api.edc

import de.agrigaia.platform.api.BaseController
import de.agrigaia.platform.business.edc.EdcBusinessService
import de.agrigaia.platform.business.errors.BusinessException
import de.agrigaia.platform.business.errors.ErrorType
import de.agrigaia.platform.common.HasLogger
import de.agrigaia.platform.integration.edc.EdcIntegrationService
import de.agrigaia.platform.model.edc.Asset
import de.agrigaia.platform.persistence.repository.AssetRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/assets")
class EdcController @Autowired constructor(
    private val edcBusinessService: EdcBusinessService,
    private val edcIntegrationService: EdcIntegrationService,
    private val assetRepository: AssetRepository,
) : HasLogger, BaseController() {


    @PostMapping("publish/{bucketName}/{assetName}/{policyName}")
    @ResponseStatus(HttpStatus.CREATED)
    fun publishAsset(
        @PathVariable bucketName: String,
        @PathVariable assetName: String,
        @PathVariable policyName: String,
        @RequestBody assetJsonDto: AssetJsonDto,
    ) {
        val assetPropName: String =
            assetJsonDto.assetPropName ?: throw BusinessException("No asset name in AssetJsonDto.", ErrorType.NOT_FOUND)
        val assetPropId: String =
            assetJsonDto.assetPropId ?: throw BusinessException("No asset id in AssetJsonDto.", ErrorType.NOT_FOUND)

        val assetJson = edcBusinessService.createAssetJson(
            assetPropName,
            assetPropId,
            bucketName,
            assetName,
            assetJsonDto.assetPropDescription,
            assetJsonDto.assetPropContentType,
            assetJsonDto.assetPropVersion,
            assetJsonDto.agrovocKeywords,
            assetJsonDto.latitude,
            assetJsonDto.longitude,
            assetJsonDto.dateRange,
            assetJsonDto.dataAddressKeyName
        )

        val jwt = getJwt()
        val policyJson: String = this.edcBusinessService.getPolicy(jwt, bucketName, policyName, assetName)
        val policyUUID: String = this.edcBusinessService.extractIdfromPolicy(policyJson)
        val contractUUID = UUID.randomUUID().toString()
        val contractDefinitionJson =
            edcBusinessService.createContractDefinitionJson(assetPropId, policyUUID, contractUUID)

        this.edcIntegrationService.publishAsset(assetJson, policyJson, contractDefinitionJson)

        val publishedAsset = Asset()

        publishedAsset.bucket = bucketName
        publishedAsset.name = assetName
        publishedAsset.assetId = assetPropId
        publishedAsset.policyId = policyUUID
        publishedAsset.contractId = contractUUID

        assetRepository.save(publishedAsset)
    }

    @DeleteMapping("unpublish/{bucketName}/{assetName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unpublishAsset(@PathVariable bucketName: String, @PathVariable assetName: String) {

        val asset: Asset = assetRepository.findByBucketAndName(bucketName, assetName)
            ?: throw BusinessException("Asset was null", ErrorType.BAD_REQUEST)

        val assetId = asset.assetId
            ?: throw BusinessException("assetId was null", ErrorType.BAD_REQUEST)
        val policyId = asset.policyId
            ?: throw BusinessException("policyId was null", ErrorType.BAD_REQUEST)
        val contractId = asset.contractId
            ?: throw BusinessException("contractId was null", ErrorType.BAD_REQUEST)

        assetRepository.delete(asset)
        this.edcIntegrationService.unpublishAsset(assetId, policyId, contractId)
    }


    /**
     * Returns a list of policy names from a Minio bucket.
     *
     * @param bucketName name of the MinIO bucket
     * @return list of names of the policies in MinIO bucket `bucketName`
     */
    @GetMapping("policies/{bucketName}")
    fun getPolicyNames(@PathVariable bucketName: String): ResponseEntity<List<String>> {
        val jwt = getJwt()
        val policyNames: List<String> = this.edcBusinessService.getPolicyNames(jwt, bucketName)
        return ResponseEntity.ok(policyNames)
    }


    /**
     * Save a policy to a MinioBucket.
     *
     * @param bucketName name of the MinIO bucket
     * @param policyName name of the policy
     * @return TODO
     */
    @PostMapping("policies/{bucketName}/{policyName}")
    fun addPolicy(@PathVariable bucketName: String, @PathVariable policyName: String, @RequestBody policyJson: String) {
        TODO("Not yet implemented")
    }


    /**
     * Delete a policy from a MinIO bucket.
     *
     * @param bucketName name of the MinIO bucket
     * @param policyName name of the policy
     * @return TODO
     */
    @DeleteMapping("policies/{bucketName}/{policyName}")
    fun deletePolicy(@PathVariable bucketName: String, @PathVariable policyName: String) {
        TODO("Not yet implemented")
    }


    /**
     * Get a policy from a MinIO bucket.
     *
     * @param bucketName name of the MinIO bucket
     * @param policyName name of the policy
     * @return TODO
     */
    @GetMapping("policies/{bucketName}/{policyName}")
    fun getPolicy(@PathVariable bucketName: String, @PathVariable policyName: String) {
        TODO("Not yet implemented")
    }

    private fun getJwt(): String {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        return jwtAuthenticationToken.token.tokenValue
    }
}
